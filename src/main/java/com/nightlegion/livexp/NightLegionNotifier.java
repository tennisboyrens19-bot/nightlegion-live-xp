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
import net.runelite.client.callback.ClientThread;

class NightLegionNotifier
{
    private static final long POLL_SECONDS = 20L;
    private static final String PURPLE = "b86cff";
    private static final String MUTED = "d4c4df";

    private final Client client;
    private final ClientThread clientThread;
    private final NightLegionApi api;
    private final NightLegionLiveXpConfig config;
    private final ScheduledExecutorService executor;

    private final Set<String> seenGroups = new HashSet<>();
    private final Map<String, Set<Long>> hostMembers = new HashMap<>();
    private final Map<String, Set<Long>> hostPending = new HashMap<>();
    private boolean primed;
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
        executor.scheduleWithFixedDelay(this::poll, 8, POLL_SECONDS, TimeUnit.SECONDS);
    }

    private void poll()
    {
        String token = config.token() == null ? "" : config.token().trim();
        if (token.isEmpty())
        {
            reset();
            return;
        }

        api.action("overview", rsn(), new JsonObject(), this::handleOverview, ignored -> { });
    }

    private void reset()
    {
        primed = false;
        seenGiveawayId = "";
        seenGroups.clear();
        hostMembers.clear();
        hostPending.clear();
    }

    private void handleOverview(JsonObject overview)
    {
        String currentGiveaway = "";
        JsonObject giveaway = null;
        if (overview.has("giveaway") && overview.get("giveaway").isJsonObject())
        {
            giveaway = overview.getAsJsonObject("giveaway");
            currentGiveaway = text(giveaway, "id");
        }

        JsonArray groups = overview.has("groups") && overview.get("groups").isJsonArray()
            ? overview.getAsJsonArray("groups")
            : new JsonArray();
        Set<String> currentGroups = groupIds(groups);

        if (!primed)
        {
            seenGiveawayId = currentGiveaway;
            seenGroups.clear();
            seenGroups.addAll(currentGroups);
            primeHostState(groups);
            primed = true;
            return;
        }

        if (config.giveawayAlerts() && giveaway != null && !currentGiveaway.isEmpty() && !currentGiveaway.equals(seenGiveawayId))
        {
            String prize = text(giveaway, "prize");
            String rank = text(giveaway, "required_role_name");
            boolean eligible = !giveaway.has("eligible") || giveaway.get("eligible").getAsBoolean();
            String suffix = eligible
                ? "Open NightLegion → Giveaway to enter."
                : (rank.isEmpty() ? "Open NightLegion → Giveaway for details." : "Requires Discord rank: " + rank + ".");
            chat("🎁 New Giveaway: " + (prize.isEmpty() ? "NightLegion giveaway" : prize) + " — " + suffix);
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

    private void broadcastNewGroups(JsonArray groups)
    {
        for (JsonElement element : groups)
        {
            if (!element.isJsonObject())
            {
                continue;
            }
            JsonObject group = element.getAsJsonObject();
            String id = text(group, "id");
            boolean isHost = group.has("is_host") && group.get("is_host").getAsBoolean();
            if (id.isEmpty() || seenGroups.contains(id) || isHost)
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
            boolean isHost = group.has("is_host") && group.get("is_host").getAsBoolean();
            String id = text(group, "id");
            if (!isHost || id.isEmpty())
            {
                continue;
            }

            activeHostGroups.add(id);
            long hostId = longValue(group, "host_id", 0L);
            Set<Long> currentMembers = ids(group, "members");
            Set<Long> currentPending = ids(group, "pending");
            Set<Long> previousMembers = hostMembers.getOrDefault(id, new HashSet<>());
            Set<Long> previousPending = hostPending.getOrDefault(id, new HashSet<>());
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

            hostMembers.put(id, new HashSet<>(currentMembers));
            hostPending.put(id, new HashSet<>(currentPending));
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
            if (!group.has("is_host") || !group.get("is_host").getAsBoolean())
            {
                continue;
            }
            String id = text(group, "id");
            if (!id.isEmpty())
            {
                hostMembers.put(id, ids(group, "members"));
                hostPending.put(id, ids(group, "pending"));
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
                String id = text(element.getAsJsonObject(), "id");
                if (!id.isEmpty())
                {
                    out.add(id);
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
