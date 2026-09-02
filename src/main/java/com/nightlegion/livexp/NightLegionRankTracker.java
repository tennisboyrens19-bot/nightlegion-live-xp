package com.nightlegion.livexp;

import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.VarPlayer;
import net.runelite.api.events.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Restored NightLegion contribution telemetry from the last working tracker.
 * It sends only the currently logged-in player's cumulative session counters;
 * the Discord-token-owned backend applies the existing caps and weights.
 */
final class NightLegionRankTracker
{
    private static final Logger log = LoggerFactory.getLogger(NightLegionRankTracker.class);
    private static final Pattern KILL_COUNT = Pattern.compile("(?i)^Your (.+?) kill count is: [0-9,]+\\.?$");
    private static final Pattern GZ = Pattern.compile("(?i)^g+z+[z\\s!?.1]*$");

    private final Client client;
    private final ScheduledExecutorService executor;
    private final NightLegionApi api;
    private final BooleanSupplier connectionEnabled;
    private final Supplier<String> tokenSupplier;
    private final Supplier<String> currentClanRankSupplier;
    private final Consumer<JsonObject> profileConsumer;
    private final Map<String, Integer> bossKills = new HashMap<>();

    private ScheduledFuture<?> uploadTask;
    private String sessionId = "";
    private long sessionStartedAt;
    private int clanMessages;
    private int clanGzMessages;

    NightLegionRankTracker(Client client, ScheduledExecutorService executor, NightLegionApi api,
        BooleanSupplier connectionEnabled, Supplier<String> tokenSupplier,
        Supplier<String> currentClanRankSupplier, Consumer<JsonObject> profileConsumer)
    {
        this.client = client;
        this.executor = executor;
        this.api = api;
        this.connectionEnabled = connectionEnabled;
        this.tokenSupplier = tokenSupplier;
        this.currentClanRankSupplier = currentClanRankSupplier;
        this.profileConsumer = profileConsumer;
    }

    void start()
    {
        if (uploadTask != null && !uploadTask.isCancelled())
        {
            return;
        }
        if (client.getGameState() == GameState.LOGGED_IN)
        {
            beginSession();
        }
        uploadTask = executor.scheduleWithFixedDelay(this::sendSnapshotSafe, 15, 60, TimeUnit.SECONDS);
    }

    void stop()
    {
        if (uploadTask != null)
        {
            uploadTask.cancel(false);
            uploadTask = null;
        }
        resetSession();
    }

    void onGameStateChanged(GameState state)
    {
        if (state == GameState.LOGGED_IN && sessionId.isEmpty())
        {
            beginSession();
        }
        else if (state == GameState.LOGIN_SCREEN)
        {
            resetSession();
        }
    }

    void onChatMessage(ChatMessage event)
    {
        if (!enabled() || client.getGameState() != GameState.LOGGED_IN)
        {
            return;
        }
        if ((event.getType() == ChatMessageType.CLAN_CHAT || event.getType() == ChatMessageType.CLAN_MESSAGE)
            && isLocalPlayer(event.getName()))
        {
            String message = plain(event.getMessage()).trim();
            if (isCongratulation(message))
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
            Matcher matcher = KILL_COUNT.matcher(plain(event.getMessage()).trim());
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

    private void resetSession()
    {
        sessionId = "";
        sessionStartedAt = 0;
        clanMessages = 0;
        clanGzMessages = 0;
        synchronized (bossKills)
        {
            bossKills.clear();
        }
    }

    private boolean enabled()
    {
        String token = tokenSupplier == null ? "" : tokenSupplier.get();
        return connectionEnabled.getAsBoolean() && token != null && !token.trim().isEmpty();
    }

    private void sendSnapshotSafe()
    {
        try
        {
            sendSnapshot();
        }
        catch (Exception exception)
        {
            log.debug("NightLegion rank snapshot failed", exception);
        }
    }

    private void sendSnapshot()
    {
        if (!enabled() || client.getGameState() != GameState.LOGGED_IN)
        {
            return;
        }
        String rsn = currentRsn();
        if (rsn.isEmpty())
        {
            return;
        }
        if (sessionId.isEmpty())
        {
            beginSession();
        }

        int totalLevel = 0;
        for (Skill skill : Skill.values())
        {
            if (skill != Skill.OVERALL)
            {
                totalLevel += Math.max(0, client.getRealSkillLevel(skill));
            }
        }

        JsonObject body = new JsonObject();
        body.addProperty("session_id", sessionId);
        body.addProperty("rsn", rsn);
        body.addProperty("current_clan_rank", safe(currentClanRankSupplier == null ? "" : currentClanRankSupplier.get()));
        body.addProperty("total_xp", client.getOverallExperience());
        body.addProperty("total_level", totalLevel);
        body.addProperty("quest_points", Math.max(0, client.getVarpValue(VarPlayer.QUEST_POINTS)));
        body.addProperty("session_seconds", Math.max(0L,
            (System.currentTimeMillis() - sessionStartedAt) / 1000L));
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

        log.debug("Uploading NightLegion contribution snapshot for currentRsn={}", rsn);
        api.action("community_rank_snapshot", rsn, body, result ->
        {
            if (result.has("profile") && result.get("profile").isJsonObject())
            {
                profileConsumer.accept(result.getAsJsonObject("profile"));
            }
        }, error -> log.debug("NightLegion contribution upload failed: {}", error));
    }

    private boolean isLocalPlayer(String chatName)
    {
        return !currentRsn().isEmpty() && plain(chatName).trim().equalsIgnoreCase(currentRsn());
    }

    static boolean isCongratulation(String message)
    {
        return GZ.matcher(message == null ? "" : message.trim()).matches();
    }

    static String killCountBoss(String message)
    {
        Matcher matcher = KILL_COUNT.matcher(message == null ? "" : plain(message).trim());
        return matcher.matches() ? matcher.group(1).trim() : null;
    }

    private String currentRsn()
    {
        Player player = client.getLocalPlayer();
        return player == null ? "" : safe(player.getName());
    }

    private static String safe(String value)
    {
        return value == null ? "" : value.replace('\u00A0', ' ').trim();
    }

    private static String plain(String value)
    {
        return value == null ? "" : value.replaceAll("<[^>]*>", "").replace('\u00A0', ' ');
    }
}
