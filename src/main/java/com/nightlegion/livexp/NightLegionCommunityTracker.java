package com.nightlegion.livexp;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;
import net.runelite.client.util.Text;

/** Optional community telemetry: PBs from official game surfaces and valuable NPC drops. */
class NightLegionCommunityTracker
{
    private static final Pattern ADVENTURE_OWNER = Pattern.compile("(?i)the exploits of\\s+(.+)");

    private final Client client;
    private final NightLegionApi api;
    private final NightLegionLiveXpConfig config;
    private final ItemManager itemManager;
    private final Set<String> recentPbSignatures = ConcurrentHashMap.newKeySet();
    private final LinkedHashSet<Integer> recentlyLoadedGroups = new LinkedHashSet<>();

    private boolean adventureMenuLoaded;
    private boolean adventureJournalLoaded;
    private String adventureOwner = "";
    private int combatAchievementScanTicks;
    private int statisticsBoardScanTicks;

    NightLegionCommunityTracker(Client client, NightLegionApi api, NightLegionLiveXpConfig config, ItemManager itemManager)
    {
        this.client = client;
        this.api = api;
        this.config = config;
        this.itemManager = itemManager;
    }

    void onLoggedOut()
    {
        adventureMenuLoaded = false;
        adventureJournalLoaded = false;
        adventureOwner = "";
        combatAchievementScanTicks = 0;
        statisticsBoardScanTicks = 0;
        recentlyLoadedGroups.clear();
        recentPbSignatures.clear();
    }

    void onChatMessage(ChatMessage event)
    {
        if (!config.communityPbTracking() || event.getType() != ChatMessageType.GAMEMESSAGE)
        {
            return;
        }
        NightLegionPbParser.PbRecord record = NightLegionPbParser.parseChat(event.getMessage());
        if (record != null)
        {
            submitPb(record);
        }
    }

    void onWidgetLoaded(WidgetLoaded event)
    {
        if (!config.communityPbTracking() || event == null)
        {
            return;
        }
        int groupId = event.getGroupId();
        recentlyLoadedGroups.add(groupId);
        while (recentlyLoadedGroups.size() > 8)
        {
            Iterator<Integer> iterator = recentlyLoadedGroups.iterator();
            iterator.next();
            iterator.remove();
        }
        statisticsBoardScanTicks = 4;

        if (groupId == InterfaceID.MENU || groupId == InterfaceID.MENU_NEW)
        {
            adventureMenuLoaded = true;
        }
        else if (groupId == InterfaceID.JOURNALSCROLL)
        {
            adventureJournalLoaded = true;
        }

        if (groupId == InterfaceID.CA_OVERVIEW || groupId == InterfaceID.CA_TASKS
            || groupId == InterfaceID.CA_REWARDS || groupId == InterfaceID.CA_BOSSES
            || groupId == InterfaceID.CA_BOSS)
        {
            combatAchievementScanTicks = 6;
        }
    }

    void onGameTick()
    {
        if (!config.communityPbTracking())
        {
            return;
        }
        processAdventureLog();
        processCombatAchievement();
        processBossStatisticsBoards();
    }

    void onNpcLootReceived(NpcLootReceived event)
    {
        if (!config.communityDropTracking() || event == null || event.getItems() == null)
        {
            return;
        }
        long threshold = Math.max(0L, config.communityDropThreshold());
        String source = event.getNpc() == null || event.getNpc().getName() == null ? "NPC" : event.getNpc().getName();
        for (ItemStack stack : event.getItems())
        {
            if (stack == null || stack.getQuantity() <= 0)
            {
                continue;
            }
            int itemId = stack.getId();
            int quantity = stack.getQuantity();
            long price = Math.max(0, itemManager.getItemPrice(itemId));
            long total = price * (long) quantity;
            if (total < threshold)
            {
                continue;
            }
            String item = itemManager.getItemComposition(itemId).getName();
            if (item == null || item.trim().isEmpty())
            {
                continue;
            }
            JsonObject data = new JsonObject();
            data.addProperty("item", item);
            data.addProperty("quantity", quantity);
            data.addProperty("total_value", total);
            data.addProperty("source", source);
            data.addProperty("count_for_mvp", config.statsEnabled());
            data.addProperty("send_to_discord", config.discordDropsEnabled());
            api.action("community_drop_report", rsn(), data, ignored -> { }, ignored -> { });
        }
    }

    private void processAdventureLog()
    {
        if (client.getLocalPlayer() == null || client.getLocalPlayer().getName() == null)
        {
            return;
        }

        if (adventureMenuLoaded)
        {
            adventureMenuLoaded = false;
            Widget menu = client.getWidget(InterfaceID.Menu.LJ_LAYER2);
            List<String> menuTexts = new ArrayList<>();
            collectVisibleTexts(menu, menuTexts);
            for (String text : menuTexts)
            {
                Matcher matcher = ADVENTURE_OWNER.matcher(cleanWidgetText(text));
                if (matcher.find())
                {
                    adventureOwner = matcher.group(1).trim();
                    break;
                }
            }
        }

        if (!adventureJournalLoaded)
        {
            return;
        }
        adventureJournalLoaded = false;
        if (adventureOwner.isEmpty() || !sameRsn(adventureOwner, rsn()))
        {
            return;
        }

        Widget parent = client.getWidget(InterfaceID.Journalscroll.TEXTLAYER);
        if (parent == null)
        {
            return;
        }
        List<String> lines = new ArrayList<>();
        collectVisibleTexts(parent, lines);
        for (NightLegionPbParser.PbRecord record : NightLegionPbParser.parseAdventureLog(lines))
        {
            submitPb(record);
        }
    }

    private void processCombatAchievement()
    {
        if (combatAchievementScanTicks <= 0 || (client.getTickCount() & 1) != 0)
        {
            return;
        }
        combatAchievementScanTicks--;
        Widget bossName = client.getWidget(InterfaceID.CaBoss.BOSS_NAME);
        Widget bossStats = client.getWidget(InterfaceID.CaBoss.CA_BOSS_STATS);
        if (!isVisible(bossName) || !isVisible(bossStats))
        {
            return;
        }
        List<String> title = new ArrayList<>();
        List<String> stats = new ArrayList<>();
        collectVisibleTexts(bossName, title);
        collectVisibleTexts(bossStats, stats);
        String boss = firstUsefulText(title);
        NightLegionPbParser.PbRecord record = NightLegionPbParser.parseCombatAchievement(boss, stats);
        if (record != null)
        {
            combatAchievementScanTicks = 0;
            submitPb(record);
        }
    }

    private void processBossStatisticsBoards()
    {
        if (statisticsBoardScanTicks <= 0 || (client.getTickCount() & 1) != 0 || combatAchievementScanTicks > 0)
        {
            return;
        }
        statisticsBoardScanTicks--;
        Widget[] roots = client.getWidgetRoots();
        if (roots == null || recentlyLoadedGroups.isEmpty())
        {
            return;
        }

        List<String> texts = new ArrayList<>();
        Set<Widget> visited = Collections.newSetFromMap(new IdentityHashMap<Widget, Boolean>());
        for (Widget root : roots)
        {
            collectTextsForGroups(root, recentlyLoadedGroups, texts, visited);
        }

        List<NightLegionPbParser.PbRecord> records = NightLegionPbParser.parseBossStatistics(texts);
        if (!records.isEmpty())
        {
            statisticsBoardScanTicks = 0;
            recentlyLoadedGroups.clear();
            for (NightLegionPbParser.PbRecord record : records)
            {
                submitPb(record);
            }
        }
        else if (statisticsBoardScanTicks <= 0)
        {
            recentlyLoadedGroups.clear();
        }
    }

    private void submitPb(NightLegionPbParser.PbRecord record)
    {
        String player = rsn();
        if (player.isEmpty() || record == null || record.seconds <= 0)
        {
            return;
        }
        String signature = player.toLowerCase(Locale.ROOT) + "|" + record.category().toLowerCase(Locale.ROOT)
            + "|" + String.format(Locale.ROOT, "%.3f", record.seconds);
        if (!recentPbSignatures.add(signature))
        {
            return;
        }
        if (recentPbSignatures.size() > 500)
        {
            recentPbSignatures.clear();
            recentPbSignatures.add(signature);
        }

        JsonObject data = new JsonObject();
        data.addProperty("category", record.category());
        data.addProperty("label", record.label.length() > 120 ? record.label.substring(0, 120) : record.label);
        data.addProperty("seconds", record.seconds);
        data.addProperty("boss", record.boss);
        data.addProperty("mode", record.mode);
        data.addProperty("team_size", record.teamSize);
        data.addProperty("time_type", record.timeType);
        data.addProperty("source", record.source);
        api.action("community_pb_report", player, data, ignored -> { }, ignored -> { });
    }

    private String rsn()
    {
        return client.getLocalPlayer() == null || client.getLocalPlayer().getName() == null
            ? ""
            : client.getLocalPlayer().getName().trim();
    }

    private static boolean sameRsn(String a, String b)
    {
        return normalizeRsn(a).equals(normalizeRsn(b));
    }

    private static String normalizeRsn(String value)
    {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private static boolean isVisible(Widget widget)
    {
        return widget != null && !widget.isHidden();
    }

    private static String firstUsefulText(List<String> texts)
    {
        for (String value : texts)
        {
            String clean = cleanWidgetText(value);
            if (!clean.isEmpty())
            {
                return clean;
            }
        }
        return "";
    }

    private static String cleanWidgetText(String raw)
    {
        if (raw == null)
        {
            return "";
        }
        String prepared = raw.replaceAll("(?i)<br\\s*/?>", ": ");
        return Text.removeTags(prepared).replaceAll("\\s+", " ").trim();
    }

    private static void collectVisibleTexts(Widget widget, List<String> out)
    {
        collectVisibleTexts(widget, out, Collections.newSetFromMap(new IdentityHashMap<Widget, Boolean>()));
    }

    private static void collectVisibleTexts(Widget widget, List<String> out, Set<Widget> visited)
    {
        if (widget == null || widget.isHidden() || !visited.add(widget))
        {
            return;
        }
        String text = cleanWidgetText(widget.getText());
        if (!text.isEmpty())
        {
            out.add(text);
        }
        collectVisibleTexts(widget.getChildren(), out, visited);
        collectVisibleTexts(widget.getDynamicChildren(), out, visited);
        collectVisibleTexts(widget.getStaticChildren(), out, visited);
        collectVisibleTexts(widget.getNestedChildren(), out, visited);
    }

    private static void collectVisibleTexts(Widget[] widgets, List<String> out, Set<Widget> visited)
    {
        if (widgets == null)
        {
            return;
        }
        for (Widget widget : widgets)
        {
            collectVisibleTexts(widget, out, visited);
        }
    }

    private static void collectTextsForGroups(Widget widget, Set<Integer> groups, List<String> out, Set<Widget> visited)
    {
        if (widget == null || widget.isHidden() || !visited.add(widget))
        {
            return;
        }
        if (groups.contains(widget.getId() >>> 16))
        {
            collectVisibleTexts(widget, out);
            return;
        }
        collectTextsForGroups(widget.getChildren(), groups, out, visited);
        collectTextsForGroups(widget.getDynamicChildren(), groups, out, visited);
        collectTextsForGroups(widget.getStaticChildren(), groups, out, visited);
        collectTextsForGroups(widget.getNestedChildren(), groups, out, visited);
    }

    private static void collectTextsForGroups(Widget[] widgets, Set<Integer> groups, List<String> out, Set<Widget> visited)
    {
        if (widgets == null)
        {
            return;
        }
        for (Widget widget : widgets)
        {
            collectTextsForGroups(widget, groups, out, visited);
        }
    }
}
