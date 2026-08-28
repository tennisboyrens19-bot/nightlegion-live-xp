package com.nightlegion.livexp;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.HashSet;
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
            primed = false;
            seenGiveawayId = "";
            seenGroups.clear();
            return;
        }

        api.action("overview", rsn(), new JsonObject(), this::handleOverview, ignored -> { });
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

        Set<String> currentGroups = new HashSet<>();
        JsonArray groups = overview.has("groups") && overview.get("groups").isJsonArray()
            ? overview.getAsJsonArray("groups")
            : new JsonArray();
        for (JsonElement element : groups)
        {
            if (element.isJsonObject())
            {
                String id = text(element.getAsJsonObject(), "id");
                if (!id.isEmpty())
                {
                    currentGroups.add(id);
                }
            }
        }

        if (!primed)
        {
            seenGiveawayId = currentGiveaway;
            seenGroups.clear();
            seenGroups.addAll(currentGroups);
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
            for (JsonElement element : groups)
            {
                if (!element.isJsonObject())
                {
                    continue;
                }
                JsonObject group = element.getAsJsonObject();
                String id = text(group, "id");
                if (id.isEmpty() || seenGroups.contains(id))
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

        seenGroups.retainAll(currentGroups);
        seenGroups.addAll(currentGroups);
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
