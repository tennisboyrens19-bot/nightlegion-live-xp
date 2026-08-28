package com.nightlegion.livexp;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.callback.ClientThread;

class NightLegionNotifier
{
    private static final long POLL_SECONDS = 20L;
    private static final String PURPLE = "7b16c9";
    private static final String MUTED = "2f1638";

    private final Client client;
    private final ClientThread clientThread;
    private final NightLegionApi api;
    private final NightLegionLiveXpConfig config;
    private final ScheduledExecutorService executor;

    private final Set<String> seenGroups = new HashSet<>();
    private final Map<String, Set<Long>> hostMembers = new HashMap<>();
    private final Map<String, Set<Long>> hostPending = new HashMap<>();

    private boolean sessionActive;
    private boolean primed;
    private String seenBotwId = "";
    private String seenSotwId = "";
    private String seenGiveawayId = "";

    NightLegionNotifier(
        Client client,
        ClientThread clientThread,
        NightLegionApi api,
        NightLegionLiveXpConfig config,
        ScheduledExecutorService executor)
    {
        this.client = client;
        this.clientThread = clientThread;
        this.api = api;
        this.config = config;
        this.executor = executor;
    }

    void start()
    {
        executor.scheduleWithFixedDelay(this::poll, 3, POLL_SECONDS, TimeUnit.SECONDS);
    }

    void onLoggedIn()
    {
        if (!sessionActive)
        {
            sessionActive = true;
            resetTracking();
        }
        executor.execute(this::poll);
    }

    void onLoggedOut()
    {
        sessionActive = false;
        resetTracking();
    }

    private void poll()
    {
        String token = config.token() == null ? "" : config.token().trim();
        if (token.isEmpty())
        {
            resetAll();
            return;
        }

        String currentRsn = rsn();
        if (client.getGameState() != GameState.LOGGED_IN || currentRsn.isEmpty())
        {
            return;
        }

        if (!sessionActive)
        {
            sessionActive = true;
            resetTracking();
        }

        api.action("overview", currentRsn, new JsonObject(), this::handleOverview, ignored -> { });
    }

    private void resetAll()
    {
        sessionActive = false;
        resetTracking();
    }

    private void resetTracking()
    {
        primed = false;
        seenBotwId = "";
        seenSotwId = "";
        seenGiveawayId = "";
        seenGroups.clear();
        hostMembers.clear();
        hostPending.clear();
    }

    private void handleOverview(JsonObject overview)
    {
        JsonObject botw = object(overview, "botw");
        JsonObject sotw = object(overview, "sotw");
        JsonObject giveaway = object(overview, "giveaway");

        String currentBotw = id(botw);
        String currentSotw = id(sotw);
        String currentGiveaway = id(giveaway);

        JsonArray groups = overview.has("groups") && overview.get("groups").isJsonArray()
            ? overview.getAsJsonArray("groups")
            : new JsonArray();
        Set<String> currentGroups = groupIds(groups);

        if (!primed)
        {
            if (config.eventAlerts())
            {
                announceEvent(botw, "BOTW", "⚔", false);
                announceEvent(sotw, "SOTW", "🏆", false);
            }
            if (config.giveawayAlerts())
            {
                announceGiveaway(giveaway, false);
            }

            seenBotwId = currentBotw;
            seenSotwId = currentSotw;
            seenGiveawayId = currentGiveaway;
            seenGroups.clear();
            seenGroups.addAll(currentGroups);
            primeHostState(groups);
            primed = true;
            return;
        }

        if (config.eventAlerts())
        {
            if (botw != null && !currentBotw.isEmpty() && !currentBotw.equals(seenBotwId))
            {
                announceEvent(botw, "BOTW", "⚔", true);
            }
            if (sotw != null && !currentSotw.isEmpty() && !currentSotw.equals(seenSotwId))
            {
                announceEvent(sotw, "SOTW", "🏆", true);
            }
        }
        seenBotwId = currentBotw;
        seenSotwId = currentSotw;

        if (config.giveawayAlerts() && giveaway != null && !currentGiveaway.isEmpty() && !currentGiveaway.equals(seenGiveawayId))
        {
            announceGiveaway(giveaway, true);
        }
        seenGiveawayId = currentGiveaway;

        boolean canUseGroupFinder = !overview.has("groupfinder_access") || overview.get("groupfinder_access").getAsBoolean();
        if (config.groupFinderAlerts() && canUseGroupFinder)
        {
            broadcastNewGroups(groups);
            notifyHosts(groups);
        }
        else
        {
            primeHostState(groups);
        }

        seenGroups.retainAll(currentGroups);
        seenGroups.addAll(currentGroups);
    }

    private void announceEvent(JsonObject event, String section, String emoji, boolean isNew)
    {
        if (event == null || id(event).isEmpty())
        {
            return;
        }

        String label = text(event, "label");
        boolean entered = bool(event, "entered");
        boolean pending = bool(event, "pending_buyin");
        String action;
        if (entered)
        {
            action = "You're entered — open NightLegion → " + section + " for progress.";
        }
        else if (pending)
        {
            action = "Your buy-in is pending — open NightLegion → " + section + ".";
        }
        else
        {
            action = "Open NightLegion → " + section + " to join.";
        }

        chat(emoji + " " + (isNew ? "New " : "") + section + " active: "
            + (label.isEmpty() ? section : label) + " — " + action);
    }

    private void announceGiveaway(JsonObject giveaway, boolean isNew)
    {
        if (giveaway == null || id(giveaway).isEmpty())
        {
            return;
        }

        String prize = text(giveaway, "prize");
        String rank = text(giveaway, "required_role_name");
        boolean entered = bool(giveaway, "entered");
        boolean eligible = !giveaway.has("eligible") || giveaway.get("eligible").getAsBoolean();

        String suffix;
        if (entered)
        {
            suffix = "You're already entered.";
        }
        else if (eligible)
        {
            suffix = "Open NightLegion → Giveaway to enter.";
        }
        else
        {
            suffix = rank.isEmpty()
                ? "Open NightLegion → Giveaway for details."
                : "Requires Discord rank: " + rank + ".";
        }

        chat("🎁 " + (isNew ? "New Giveaway" : "Giveaway active") + ": "
            + (prize.isEmpty() ? "NightLegion giveaway" : prize) + " — " + suffix);
    }

    private void broadcastNewGroups(JsonArray groups)
    {
        for (JsonElement element : groups)
        {
            if (!element.isJsonObject())
            {
                continue;
            }
            JsonObject group = element.getAsJsonObject();
            String groupId = text(group, "id");
            boolean isHost = bool(group, "is_host");
            if (groupId.isEmpty() || seenGroups.contains(groupId) || isHost)
            {
                continue;
            }

            String activity = text(group, "activity");
            String host = text(group, "host_name");
            String world = group.has("world") && !group.get("world").isJsonNull()
                ? " · World " + group.get("world").getAsInt()
                : "";
            int members = group.has("members") && group.get("members").isJsonArray()
                ? group.getAsJsonArray("members").size()
                : 1;
            int max = group.has("max_players") && !group.get("max_players").isJsonNull()
                ? group.get("max_players").getAsInt()
                : 1;

            chat("👥 New " + (activity.isEmpty() ? "Group Finder" : activity)
                + " group by " + (host.isEmpty() ? "a clan member" : host)
                + " — " + members + "/" + max + world
                + ". Open NightLegion → Group Finder.");
        }
    }

    private void notifyHosts(JsonArray groups)
    {
        Set<String> activeHostGroups = new HashSet<>();

        for (JsonElement element : groups)
        {
            if (!element.isJsonObject())
            {
                continue;
            }
            JsonObject group = element.getAsJsonObject();
            boolean isHost = bool(group, "is_host");
            String groupId = text(group, "id");
            if (!isHost || groupId.isEmpty())
            {
                continue;
            }

            activeHostGroups.add(groupId);
            long hostId = longValue(group, "host_id", 0L);
            Set<Long> currentMembers = ids(group, "members");
            Set<Long> currentPending = ids(group, "pending");
            Set<Long> previousMembers = hostMembers.getOrDefault(groupId, new HashSet<>());
            Set<Long> previousPending = hostPending.getOrDefault(groupId, new HashSet<>());
            Map<Long, String> memberNames = names(group, "member_details");
            Map<Long, String> pendingNames = names(group, "pending_details");
            String activity = text(group, "activity");
            String title = activity.isEmpty() ? "Group Finder" : activity;

            for (Long userId : difference(currentPending, previousPending))
            {
                chat("👤 " + pendingNames.getOrDefault(userId, "A clan member")
                    + " requested to join your " + title
                    + " group — open NightLegion → Group Finder → Requests.");
            }

            for (Long userId : difference(currentMembers, previousMembers))
            {
                if (userId == hostId)
                {
                    continue;
                }
                int max = group.has("max_players") && !group.get("max_players").isJsonNull()
                    ? group.get("max_players").getAsInt()
                    : currentMembers.size();
                chat("✅ " + memberNames.getOrDefault(userId, "A clan member")
                    + " joined your " + title + " group — "
                    + currentMembers.size() + "/" + max + " players.");
            }

            hostMembers.put(groupId, new HashSet<>(currentMembers));
            hostPending.put(groupId, new HashSet<>(currentPending));
        }

        hostMembers.keySet().retainAll(activeHostGroups);
        hostPending.keySet().retainAll(activeHostGroups);
    }

    private void primeHostState(JsonArray groups)
    {
        hostMembers.clear();
        hostPending.clear();
        for (JsonElement element : groups)
        {
            if (!element.isJsonObject())
            {
                continue;
            }
            JsonObject group = element.getAsJsonObject();
            if (!bool(group, "is_host"))
            {
                continue;
            }
            String groupId = text(group, "id");
            if (!groupId.isEmpty())
            {
                hostMembers.put(groupId, ids(group, "members"));
                hostPending.put(groupId, ids(group, "pending"));
            }
        }
    }

    private static Set<String> groupIds(JsonArray groups)
    {
        Set<String> out = new HashSet<>();
        for (JsonElement element : groups)
        {
            if (element.isJsonObject())
            {
                String groupId = text(element.getAsJsonObject(), "id");
                if (!groupId.isEmpty())
                {
                    out.add(groupId);
                }
            }
        }
        return out;
    }

    private static Set<Long> ids(JsonObject object, String key)
    {
        Set<Long> out = new HashSet<>();
        if (!object.has(key) || !object.get(key).isJsonArray())
        {
            return out;
        }
        for (JsonElement element : object.getAsJsonArray(key))
        {
            try
            {
                out.add(element.getAsLong());
            }
            catch (Exception ignored)
            {
            }
        }
        return out;
    }

    private static Map<Long, String> names(JsonObject object, String key)
    {
        Map<Long, String> out = new HashMap<>();
        if (!object.has(key) || !object.get(key).isJsonArray())
        {
            return out;
        }
        for (JsonElement element : object.getAsJsonArray(key))
        {
            if (!element.isJsonObject())
            {
                continue;
            }
            JsonObject detail = element.getAsJsonObject();
            long userId = longValue(detail, "user_id", 0L);
            if (userId > 0)
            {
                out.put(userId, text(detail, "display_name"));
            }
        }
        return out;
    }

    private static Set<Long> difference(Set<Long> current, Set<Long> previous)
    {
        Set<Long> out = new HashSet<>(current);
        out.removeAll(previous);
        return out;
    }

    private String rsn()
    {
        return client.getLocalPlayer() == null || client.getLocalPlayer().getName() == null
            ? ""
            : client.getLocalPlayer().getName().trim();
    }

    private void chat(String message)
    {
        clientThread.invokeLater(() -> client.addChatMessage(
            ChatMessageType.GAMEMESSAGE,
            "",
            "<col=" + PURPLE + ">[NightLegion]</col> <col=" + MUTED + ">" + escapeTags(message) + "</col>",
            null));
    }

    private static JsonObject object(JsonObject parent, String key)
    {
        return parent.has(key) && parent.get(key).isJsonObject() ? parent.getAsJsonObject(key) : null;
    }

    private static String id(JsonObject object)
    {
        return object == null ? "" : text(object, "id");
    }

    private static boolean bool(JsonObject object, String key)
    {
        try
        {
            return object.has(key) && !object.get(key).isJsonNull() && object.get(key).getAsBoolean();
        }
        catch (Exception ignored)
        {
            return false;
        }
    }

    private static long longValue(JsonObject object, String key, long fallback)
    {
        try
        {
            return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsLong() : fallback;
        }
        catch (Exception ignored)
        {
            return fallback;
        }
    }

    private static String text(JsonObject object, String key)
    {
        try
        {
            return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : "";
        }
        catch (Exception ignored)
        {
            return "";
        }
    }

    private static String escapeTags(String value)
    {
        return value == null ? "" : value.replace("<", "[").replace(">", "]");
    }
}
