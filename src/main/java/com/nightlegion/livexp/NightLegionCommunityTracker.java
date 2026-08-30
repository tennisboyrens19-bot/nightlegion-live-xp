package com.nightlegion.livexp;

import com.google.gson.JsonObject;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;
import net.runelite.client.util.Text;

/** Optional community telemetry: PB notices and valuable NPC drops. */
class NightLegionCommunityTracker
{
    private static final Pattern PB_PATTERN = Pattern.compile(
        "(?i)(?:duration|completion time|fight duration|challenge duration|lap time|subdued in)\\s*:?\\s*([0-9]+(?::[0-9]{1,2})?(?:\\.[0-9]+)?)?.*new personal best"
    );

    private final Client client;
    private final NightLegionApi api;
    private final NightLegionLiveXpConfig config;
    private final ItemManager itemManager;
    private final Set<String> recentPbSignatures = ConcurrentHashMap.newKeySet();

    NightLegionCommunityTracker(Client client, NightLegionApi api, NightLegionLiveXpConfig config, ItemManager itemManager)
    {
        this.client = client;
        this.api = api;
        this.config = config;
        this.itemManager = itemManager;
    }

    void onChatMessage(ChatMessage event)
    {
        if (!config.communityPbTracking() || event.getType() != ChatMessageType.GAMEMESSAGE)
        {
            return;
        }
        String message = Text.removeTags(event.getMessage() == null ? "" : event.getMessage()).trim();
        if (message.isEmpty() || !message.toLowerCase().contains("new personal best"))
        {
            return;
        }
        Matcher matcher = PB_PATTERN.matcher(message);
        if (!matcher.find() || matcher.group(1) == null)
        {
            return;
        }
        double seconds = parseTime(matcher.group(1));
        if (seconds <= 0)
        {
            return;
        }
        String signature = rsn() + "|" + message.toLowerCase();
        if (!recentPbSignatures.add(signature))
        {
            return;
        }
        if (recentPbSignatures.size() > 100)
        {
            recentPbSignatures.clear();
            recentPbSignatures.add(signature);
        }

        JsonObject data = new JsonObject();
        data.addProperty("category", inferCategory(message));
        data.addProperty("label", message.length() > 120 ? message.substring(0, 120) : message);
        data.addProperty("seconds", seconds);
        api.action("community_pb_report", rsn(), data, ignored -> { }, ignored -> { });
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
            api.action("community_drop_report", rsn(), data, ignored -> { }, ignored -> { });
        }
    }

    private String rsn()
    {
        return client.getLocalPlayer() == null || client.getLocalPlayer().getName() == null
            ? ""
            : client.getLocalPlayer().getName().trim();
    }

    private static double parseTime(String value)
    {
        try
        {
            String[] parts = value.split(":");
            if (parts.length == 1)
            {
                return Double.parseDouble(parts[0]);
            }
            if (parts.length == 2)
            {
                return Double.parseDouble(parts[0]) * 60.0 + Double.parseDouble(parts[1]);
            }
        }
        catch (Exception ignored)
        {
        }
        return -1;
    }

    private static String inferCategory(String message)
    {
        String clean = message.replaceAll("(?i)\\s*\\(new personal best\\).*", "").trim();
        int colon = clean.indexOf(':');
        if (colon > 0)
        {
            String left = clean.substring(0, colon).trim();
            if (!left.isEmpty() && left.length() <= 80)
            {
                return left;
            }
        }
        return "In-game PB";
    }
}
