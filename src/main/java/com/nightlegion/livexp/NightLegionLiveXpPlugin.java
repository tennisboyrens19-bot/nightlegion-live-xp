package com.nightlegion.livexp;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.Text;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PluginDescriptor(
    name = "NightLegion",
    description = "NightLegion clan tools, events, Group Finder, ranks and community hub",
    tags = {"nightlegion", "sotw", "botw", "giveaway", "group finder", "rank", "clan", "xp", "bossing", "skilling", "pb", "drops"},
    enabledByDefault = true
)
public class NightLegionLiveXpPlugin extends Plugin
{
    private static final Logger log = LoggerFactory.getLogger(NightLegionLiveXpPlugin.class);
    private static final String ENDPOINT = "https://nightlegion-livexp.onrender.com/sotw/live-xp";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final long PUSH_INTERVAL_MS = 5_000L;

    // Same RuneLite chatbox script used by the Live On implementation. It runs
    // after the native clan rank/icon has been resolved, so our text badge sits
    // after the player's name without replacing RuneLite's rank icon.
    private static final int ADD_CHATBOX_MESSAGE_SCRIPT = 4483;

    @Inject private Client client;
    @Inject private ClientThread clientThread;
    @Inject private NightLegionLiveXpConfig config;
    @Inject private OkHttpClient okHttpClient;
    @Inject private Gson gson;
    @Inject private ClientToolbar clientToolbar;
    @Inject private ConfigManager configManager;
    @Inject private ItemManager itemManager;

    private final Map<Skill, Integer> latestXp = new ConcurrentHashMap<>();
    private final Map<Skill, Integer> lastSentXp = new ConcurrentHashMap<>();
    private final Map<Skill, Long> lastAttemptAt = new ConcurrentHashMap<>();
    private final Set<String> mvpBadgePlayers = ConcurrentHashMap.newKeySet();
    private final Set<String> liveBadgePlayers = ConcurrentHashMap.newKeySet();

    private volatile String currentRsn = "";
    private volatile String lastToken = "";
    private ScheduledExecutorService sender;
    private NavigationButton navButton;
    private NightLegionRootPanel panel;
    private NightLegionNotifier notifier;
    private NightLegionRankTracker rankTracker;
    private NightLegionCommunityTracker communityTracker;
    private NightLegionApi communityApi;
    private NightLegionClanBadgeDecorator clanBadgeDecorator;

    @Override
    protected void startUp()
    {
        sender = Executors.newSingleThreadScheduledExecutor(r ->
        {
            Thread thread = new Thread(r, "nightlegion-companion");
            thread.setDaemon(true);
            return thread;
        });
        sender.scheduleWithFixedDelay(this::flushPending, 2, 2, TimeUnit.SECONDS);
        sender.scheduleWithFixedDelay(this::refreshBadgeSnapshotSafe, 5, 60, TimeUnit.SECONDS);

        communityApi = new NightLegionApi(okHttpClient, sender, config, gson);
        notifier = new NightLegionNotifier(client, clientThread, communityApi, config, sender);
        notifier.start();

        rankTracker = new NightLegionRankTracker(client, okHttpClient, sender, config, gson);
        rankTracker.start();
        communityTracker = new NightLegionCommunityTracker(client, communityApi, config, itemManager);
        clanBadgeDecorator = new NightLegionClanBadgeDecorator(client, this);

        SwingUtilities.invokeLater(() ->
        {
            panel = new NightLegionRootPanel(client, communityApi, itemManager, config, configManager);
            navButton = NavigationButton.builder()
                .tooltip("NightLegion")
                .icon(createIcon())
                .priority(config.sidebarIconPriority())
                .panel(panel)
                .build();
            clientToolbar.addNavigation(navButton);
        });

        log.info("NightLegion companion started");
    }

    @Override
    protected void shutDown()
    {
        NightLegionClanBadgeDecorator decorator = clanBadgeDecorator;
        if (decorator != null)
        {
            clientThread.invokeLater(decorator::clearDecorations);
        }

        if (navButton != null)
        {
            clientToolbar.removeNavigation(navButton);
            navButton = null;
        }
        panel = null;
        notifier = null;
        communityTracker = null;
        communityApi = null;
        clanBadgeDecorator = null;

        if (rankTracker != null)
        {
            rankTracker.stop();
            rankTracker = null;
        }

        if (sender != null)
        {
            sender.shutdownNow();
            sender = null;
        }
        latestXp.clear();
        lastSentXp.clear();
        lastAttemptAt.clear();
        mvpBadgePlayers.clear();
        liveBadgePlayers.clear();
        currentRsn = "";
        lastToken = "";
        log.info("NightLegion companion stopped");
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        NightLegionNotifier currentNotifier = notifier;
        NightLegionRankTracker currentRankTracker = rankTracker;
        NightLegionCommunityTracker currentCommunityTracker = communityTracker;
        if (currentRankTracker != null)
        {
            currentRankTracker.onGameStateChanged(event.getGameState());
        }

        if (event.getGameState() == GameState.LOGIN_SCREEN)
        {
            if (currentNotifier != null)
            {
                currentNotifier.onLoggedOut();
            }
            if (currentCommunityTracker != null)
            {
                currentCommunityTracker.onLoggedOut();
            }
            mvpBadgePlayers.clear();
            liveBadgePlayers.clear();
            NightLegionClanBadgeDecorator decorator = clanBadgeDecorator;
            if (decorator != null)
            {
                decorator.clearDecorations();
            }
            return;
        }

        if (event.getGameState() != GameState.LOGGED_IN)
        {
            return;
        }

        refreshRsn();
        refreshBadgeSnapshotSafe();
        if (currentNotifier != null)
        {
            currentNotifier.onLoggedIn();
        }

        NightLegionRootPanel currentPanel = panel;
        if (currentPanel != null)
        {
            SwingUtilities.invokeLater(currentPanel::refresh);
        }
    }

    @Subscribe
    public void onChatMessage(ChatMessage event)
    {
        NightLegionRankTracker currentRankTracker = rankTracker;
        if (currentRankTracker != null)
        {
            currentRankTracker.onChatMessage(event);
        }
        NightLegionCommunityTracker currentCommunityTracker = communityTracker;
        if (currentCommunityTracker != null)
        {
            currentCommunityTracker.onChatMessage(event);
        }
    }

    @Subscribe
    public void onWidgetLoaded(WidgetLoaded event)
    {
        NightLegionCommunityTracker currentCommunityTracker = communityTracker;
        if (currentCommunityTracker != null)
        {
            currentCommunityTracker.onWidgetLoaded(event);
        }
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        NightLegionCommunityTracker currentCommunityTracker = communityTracker;
        if (currentCommunityTracker != null)
        {
            currentCommunityTracker.onGameTick();
        }
        NightLegionClanBadgeDecorator decorator = clanBadgeDecorator;
        if (decorator != null)
        {
            decorator.refresh();
        }
    }

    /**
     * Exact Live On chat badge behaviour: MVP in gold, LIVE in green, and both
     * can appear at the same time immediately before the sender colon.
     */
    @Subscribe
    public void onScriptPreFired(ScriptPreFired event)
    {
        if (event.getScriptId() != ADD_CHATBOX_MESSAGE_SCRIPT || !badgesEnabled())
        {
            return;
        }

        Object[] objectStack = client.getObjectStack();
        int objectStackSize = client.getObjectStackSize();
        if (objectStackSize < 2 || !(objectStack[1] instanceof String))
        {
            return;
        }

        String sender = NightLegionClanBadgeDecorator.removeOwnMarkup((String) objectStack[1]);
        String plainSender = Text.removeTags(sender).replace('\u00A0', ' ').trim();
        if (!plainSender.endsWith(":"))
        {
            return;
        }

        String playerKey = normalizePlayerName(plainSender.substring(0, plainSender.length() - 1));
        boolean isMvp = mvpBadgePlayers.contains(playerKey);
        boolean isLive = config.liveStatusEnabled() && liveBadgePlayers.contains(playerKey);
        if (!isMvp && !isLive)
        {
            return;
        }

        StringBuilder badges = new StringBuilder();
        if (isMvp)
        {
            badges.append(NightLegionClanBadgeDecorator.MVP_MARKUP);
        }
        if (isLive)
        {
            badges.append(NightLegionClanBadgeDecorator.LIVE_MARKUP);
        }

        int colonIndex = sender.lastIndexOf(':');
        if (colonIndex >= 0)
        {
            objectStack[1] = sender.substring(0, colonIndex) + badges + sender.substring(colonIndex);
        }
    }

    @Subscribe
    public void onNpcLootReceived(NpcLootReceived event)
    {
        NightLegionCommunityTracker currentCommunityTracker = communityTracker;
        if (currentCommunityTracker != null)
        {
            currentCommunityTracker.onNpcLootReceived(event);
        }
    }

    @Subscribe
    public void onStatChanged(StatChanged event)
    {
        Skill skill = event.getSkill();
        if (skill == null || skill == Skill.OVERALL || client.getGameState() != GameState.LOGGED_IN)
        {
            return;
        }

        refreshRsn();
        if (currentRsn.isEmpty())
        {
            return;
        }

        int xp = event.getXp();
        if (xp >= 0)
        {
            latestXp.put(skill, xp);
        }
    }

    boolean badgesEnabled()
    {
        return config.enabled() && config.token() != null && !config.token().trim().isEmpty();
    }

    boolean isPlayerMvp(String playerName)
    {
        return mvpBadgePlayers.contains(normalizePlayerName(playerName));
    }

    boolean isPlayerLive(String playerName)
    {
        return config.liveStatusEnabled() && liveBadgePlayers.contains(normalizePlayerName(playerName));
    }

    /** Resolve a player name inside the native clan-list text. */
    String badgePlayerNameIn(String displayedText)
    {
        String normalized = normalizePlayerName(displayedText);
        if (mvpBadgePlayers.contains(normalized) || liveBadgePlayers.contains(normalized))
        {
            return normalized;
        }

        // Some RuneLite revisions expose small extra text in the same widget.
        // Match only whole normalized names to avoid decorating unrelated rows.
        Set<String> candidates = new HashSet<>(mvpBadgePlayers);
        candidates.addAll(liveBadgePlayers);
        for (String candidate : candidates)
        {
            if (normalized.equals(candidate)
                || normalized.startsWith(candidate + " ")
                || normalized.endsWith(" " + candidate))
            {
                return candidate;
            }
        }
        return null;
    }

    private void refreshBadgeSnapshotSafe()
    {
        try
        {
            NightLegionApi api = communityApi;
            String token = config.token() == null ? "" : config.token().trim();
            String rsn = currentRsn == null ? "" : currentRsn.trim();
            if (api == null || token.isEmpty() || rsn.isEmpty() || !config.enabled())
            {
                return;
            }

            api.action("community_snapshot", rsn, new JsonObject(), this::applyBadgeSnapshot,
                error -> log.debug("NightLegion badge refresh failed: {}", error));
        }
        catch (Exception ex)
        {
            log.debug("NightLegion badge refresh failed", ex);
        }
    }

    private void applyBadgeSnapshot(JsonObject snapshot)
    {
        Set<String> nextMvp = new HashSet<>();
        Set<String> nextLive = new HashSet<>();

        if (snapshot != null && snapshot.has("mvp_badges") && snapshot.get("mvp_badges").isJsonArray())
        {
            for (JsonElement element : snapshot.getAsJsonArray("mvp_badges"))
            {
                String player = "";
                try
                {
                    if (element.isJsonPrimitive())
                    {
                        player = element.getAsString();
                    }
                    else if (element.isJsonObject())
                    {
                        JsonObject row = element.getAsJsonObject();
                        player = string(row, "player_name", string(row, "rsn", ""));
                    }
                }
                catch (Exception ignored)
                {
                }
                player = normalizePlayerName(player);
                if (!player.isEmpty())
                {
                    nextMvp.add(player);
                }
            }
        }

        JsonArray streams = snapshot != null && snapshot.has("streams") && snapshot.get("streams").isJsonArray()
            ? snapshot.getAsJsonArray("streams") : new JsonArray();
        for (JsonElement element : streams)
        {
            if (!element.isJsonObject())
            {
                continue;
            }
            JsonObject row = element.getAsJsonObject();
            boolean online = false;
            try
            {
                online = row.has("is_live") && row.get("is_live").getAsBoolean();
            }
            catch (Exception ignored)
            {
            }
            if (!online)
            {
                continue;
            }
            String player = normalizePlayerName(string(row, "player_name", string(row, "rsn", "")));
            if (!player.isEmpty())
            {
                nextLive.add(player);
            }
        }

        mvpBadgePlayers.clear();
        mvpBadgePlayers.addAll(nextMvp);
        liveBadgePlayers.clear();
        liveBadgePlayers.addAll(nextLive);

        NightLegionClanBadgeDecorator decorator = clanBadgeDecorator;
        if (decorator != null)
        {
            clientThread.invokeLater(decorator::refresh);
        }
    }

    private void refreshRsn()
    {
        Player player = client.getLocalPlayer();
        if (player == null || player.getName() == null)
        {
            return;
        }

        String nextRsn = player.getName().trim();
        if (!nextRsn.equals(currentRsn))
        {
            currentRsn = nextRsn;
            latestXp.clear();
            lastSentXp.clear();
            lastAttemptAt.clear();
        }
    }

    private void flushPending()
    {
        try
        {
            String token = config.token() == null ? "" : config.token().trim();
            String rsn = currentRsn == null ? "" : currentRsn.trim();

            if (!token.equals(lastToken))
            {
                lastToken = token;
                lastSentXp.clear();
                lastAttemptAt.clear();
            }

            if (token.isEmpty() || rsn.isEmpty())
            {
                return;
            }

            long now = System.currentTimeMillis();
            for (Map.Entry<Skill, Integer> entry : latestXp.entrySet())
            {
                Skill skill = entry.getKey();
                int xp = entry.getValue();
                Integer sentXp = lastSentXp.get(skill);

                if (sentXp != null && sentXp == xp)
                {
                    continue;
                }

                long previousAttempt = lastAttemptAt.getOrDefault(skill, 0L);
                if (now - previousAttempt < PUSH_INTERVAL_MS)
                {
                    continue;
                }

                lastAttemptAt.put(skill, now);
                sendXp(token, rsn, skill, xp);
            }
        }
        catch (Exception ex)
        {
            log.debug("NightLegion XP flush failed", ex);
        }
    }

    private void sendXp(String token, String rsn, Skill skill, int xp)
    {
        String json = "{" +
            "\"rsn\":\"" + escapeJson(rsn) + "\"," +
            "\"skill\":\"" + escapeJson(skill.getName()) + "\"," +
            "\"xp\":" + xp +
            "}";

        Request request = new Request.Builder()
            .url(ENDPOINT)
            .header("X-NightLegion-Token", token)
            .post(RequestBody.create(JSON, json))
            .build();

        okHttpClient.newCall(request).enqueue(new Callback()
        {
            @Override
            public void onFailure(Call call, IOException e)
            {
                log.debug("NightLegion XP upload failed", e);
            }

            @Override
            public void onResponse(Call call, Response response)
            {
                try (Response ignored = response)
                {
                    if (!token.equals(lastToken) || !rsn.equals(currentRsn))
                    {
                        return;
                    }

                    int statusCode = response.code();
                    if (statusCode >= 200 && statusCode < 300)
                    {
                        lastSentXp.put(skill, xp);
                    }
                    else if (statusCode == 401)
                    {
                        log.warn("NightLegion token was rejected. Create a new one with /runelite_link.");
                    }
                    else
                    {
                        log.debug("NightLegion XP server returned HTTP {}", statusCode);
                    }
                }
            }
        });
    }

    private static String normalizePlayerName(String value)
    {
        if (value == null)
        {
            return "";
        }
        return Text.removeTags(value)
            .replace('\u00A0', ' ')
            .trim()
            .replaceAll("\\s+", " ")
            .toLowerCase(Locale.ROOT);
    }

    private static String string(JsonObject object, String key, String fallback)
    {
        try
        {
            return object != null && object.has(key) && !object.get(key).isJsonNull()
                ? object.get(key).getAsString() : fallback;
        }
        catch (Exception ignored)
        {
            return fallback;
        }
    }

    private static BufferedImage createIcon()
    {
        return NightLegionTheme.markImage(16, NightLegionTheme.PURPLE_BRIGHT);
    }

    private static String escapeJson(String value)
    {
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\r", "\\r")
            .replace("\n", "\\n");
    }

    @Provides
    NightLegionLiveXpConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(NightLegionLiveXpConfig.class);
    }
}
