package com.nightlegion.livexp;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.VarPlayer;
import net.runelite.api.events.ChatMessage;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NightLegion contribution telemetry.
 *
 * Discord messages are counted by the Discord bot. In-game Clan Chat messages
 * are counted here and submitted using the same Personal Link Token as the rest
 * of the plugin. Monthly MVP is a completely separate system.
 */
class NightLegionRankTracker
{
    private static final Logger log = LoggerFactory.getLogger(NightLegionRankTracker.class);
    private static final Pattern KILL_COUNT = Pattern.compile("(?i)^Your (.+?) kill count is: [0-9,]+\\.?$");
    private static final Pattern GZ = Pattern.compile("(?i)^g+z+[z\\s!?.1]*$");

    private final Client client;
    private final ScheduledExecutorService executor;
    private final NightLegionLiveXpConfig config;
    private final NightLegionApi api;
    private final Map<String, Integer> bossKills = new HashMap<>();

    private String sessionId = "";
    private long sessionStartedAt;
    private int clanMessages;
    private int clanGzMessages;

    NightLegionRankTracker(Client client, OkHttpClient http, ScheduledExecutorService executor,
        NightLegionLiveXpConfig config, Gson gson)
    {
        this.client = client;
        this.executor = executor;
        this.config = config;
        this.api = new NightLegionApi(http, executor, config, gson);
    }

    void start()
    {
        if (client.getGameState() == GameState.LOGGED_IN)
        {
            beginSession();
        }
        executor.scheduleWithFixedDelay(this::sendSnapshotSafe, 15, 60, TimeUnit.SECONDS);
    }

    void stop()
    {
        sessionId = "";
        synchronized (bossKills)
        {
            bossKills.clear();
        }
        clanMessages = 0;
        clanGzMessages = 0;
    }

    void onGameStateChanged(GameState state)
    {
        if (state == GameState.LOGGED_IN)
        {
            if (sessionId.isEmpty())
            {
                beginSession();
            }
        }
        else if (state == GameState.LOGIN_SCREEN)
        {
            sessionId = "";
            synchronized (bossKills)
            {
                bossKills.clear();
            }
            clanMessages = 0;
            clanGzMessages = 0;
        }
    }

    void onChatMessage(ChatMessage event)
    {
        if (!enabled() || client.getGameState() != GameState.LOGGED_IN)
        {
            return;
        }

        // Only count the linked player's own outgoing clan messages. Every
        // member's plugin reports their own activity, preventing duplicates
        // from multiple RuneLite clients seeing the same clan message.
        if (event.getType() == ChatMessageType.CLAN_CHAT && isLocalPlayer(event.getName()))
        {
            String message = plain(event.getMessage()).trim();
            if (GZ.matcher(message).matches())
            {
                clanGzMessages++;
            }
            else if (message.length() >= 2)
            {
                clanMessages++;
            }
            return;
        }

        if (event.getType() == ChatMessageType.GAMEMESSAGE)
        {
            String message = plain(event.getMessage()).trim();
            Matcher matcher = KILL_COUNT.matcher(message);
            if (matcher.matches())
            {
                String boss = matcher.group(1).trim();
                if (!boss.isEmpty() && boss.length() <= 80)
                {
                    synchronized (bossKills)
                    {
                        bossKills.merge(boss, 1, Integer::sum);
                    }
                }
            }
        }
    }

    private void beginSession()
    {
        sessionId = UUID.randomUUID().toString();
        sessionStartedAt = System.currentTimeMillis();
        clanMessages = 0;
        clanGzMessages = 0;
        synchronized (bossKills)
        {
            bossKills.clear();
        }
    }

    private boolean enabled()
    {
        String token = config.token() == null ? "" : config.token().trim();
        return config.enabled() && config.rankTrackingEnabled() && !token.isEmpty();
    }

    private void sendSnapshotSafe()
    {
        try
        {
            sendSnapshot();
        }
        catch (Exception ex)
        {
            log.debug("NightLegion rank snapshot failed", ex);
        }
    }

    private void sendSnapshot()
    {
        if (!enabled() || client.getGameState() != GameState.LOGGED_IN)
        {
            return;
        }

        Player player = client.getLocalPlayer();
        if (player == null || player.getName() == null || player.getName().trim().isEmpty())
        {
            return;
        }
        if (sessionId.isEmpty())
        {
            beginSession();
        }

        long totalXp = client.getOverallExperience();
        int totalLevel = 0;
        for (Skill skill : Skill.values())
        {
            if (skill == Skill.OVERALL)
            {
                continue;
            }
            int level = client.getRealSkillLevel(skill);
            if (level > 0)
            {
                totalLevel += level;
            }
        }

        int questPoints = Math.max(0, client.getVarpValue(VarPlayer.QUEST_POINTS));
        long elapsed = Math.max(0L, (System.currentTimeMillis() - sessionStartedAt) / 1000L);

        JsonObject body = new JsonObject();
        body.addProperty("session_id", sessionId);
        body.addProperty("rsn", player.getName().trim());
        body.addProperty("total_xp", totalXp);
        body.addProperty("total_level", totalLevel);
        body.addProperty("quest_points", questPoints);
        body.addProperty("session_seconds", elapsed);
        body.addProperty("clan_messages", clanMessages);
        body.addProperty("clan_gz_messages", clanGzMessages);

        JsonObject kills = new JsonObject();
        synchronized (bossKills)
        {
            for (Map.Entry<String, Integer> entry : bossKills.entrySet())
            {
                kills.addProperty(entry.getKey(), entry.getValue());
            }
        }
        body.add("boss_kills", kills);

        api.action("community_rank_snapshot", player.getName().trim(), body,
            ignored -> { },
            error -> log.debug("NightLegion contribution upload failed: {}", error));
    }

    private boolean isLocalPlayer(String chatName)
    {
        Player player = client.getLocalPlayer();
        if (player == null || player.getName() == null)
        {
            return false;
        }
        return plain(chatName).trim().equalsIgnoreCase(player.getName().trim());
    }

    private static String plain(String value)
    {
        if (value == null)
        {
            return "";
        }
        return value.replaceAll("<[^>]*>", "").replace('\u00A0', ' ');
    }
}
