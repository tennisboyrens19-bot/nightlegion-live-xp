package com.nightlegion.livexp;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
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
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Separate rank telemetry channel. This does not use the normal NightLegion
 * companion token and cannot award points client-side; the Discord bot decides
 * all points and rank changes.
 */
class NightLegionRankTracker
{
    private static final Logger log = LoggerFactory.getLogger(NightLegionRankTracker.class);
    private static final String RANK_ENDPOINT = "https://nightlegion-livexp.onrender.com/rank/sync";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final Pattern KILL_COUNT = Pattern.compile("(?i)^Your (.+?) kill count is: [0-9,]+\\.?$");
    private static final Pattern GZ = Pattern.compile("(?i)^g+z+[!1.]*$");

    private final Client client;
    private final OkHttpClient http;
    private final ScheduledExecutorService executor;
    private final NightLegionLiveXpConfig config;
    private final Gson gson;
    private final Map<String, Integer> bossKills = new HashMap<>();

    private String sessionId = "";
    private long sessionStartedAt;
    private int clanMessages;
    private int clanGzMessages;

    NightLegionRankTracker(Client client, OkHttpClient http, ScheduledExecutorService executor, NightLegionLiveXpConfig config, Gson gson)
    {
        this.client = client;
        this.http = http;
        this.executor = executor;
        this.config = config;
        this.gson = gson;
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
        bossKills.clear();
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
            bossKills.clear();
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
                    bossKills.merge(boss, 1, Integer::sum);
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
        bossKills.clear();
    }

    private boolean enabled()
    {
        String token = config.rankToken() == null ? "" : config.rankToken().trim();
        return config.rankTrackingEnabled() && !token.isEmpty();
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

        String token = config.rankToken().trim();
        Request request = new Request.Builder()
            .url(RANK_ENDPOINT)
            .header("X-NightLegion-Rank-Token", token)
            .post(RequestBody.create(JSON, gson.toJson(body)))
            .build();

        http.newCall(request).enqueue(new Callback()
        {
            @Override
            public void onFailure(Call call, IOException e)
            {
                log.debug("NightLegion rank upload failed", e);
            }

            @Override
            public void onResponse(Call call, Response response)
            {
                try (Response ignored = response)
                {
                    if (response.code() == 401)
                    {
                        log.warn("NightLegion Rank Secret Key was rejected. Create a new one with /rank_link.");
                    }
                    else if (response.code() < 200 || response.code() >= 300)
                    {
                        log.debug("NightLegion rank service returned HTTP {}", response.code());
                    }
                }
            }
        });
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
