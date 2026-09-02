package com.nightlegion.livexp;

import com.google.gson.Gson;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.JPanel;
import net.runelite.api.Client;
import net.runelite.client.game.ItemManager;
import okhttp3.OkHttpClient;

/** Keeps NightLegion-only Events and Groups inside the active Live On shell. */
public final class NightLegionExtrasBridge
{
    private final NightLegionPanel events;
    private final NightLegionGroupFinderPanel groups;
    private final NightLegionRankTracker rankTracker;
    private Consumer<com.google.gson.JsonObject> rankProfileConsumer = ignored -> { };

    public NightLegionExtrasBridge(Client client, OkHttpClient httpClient, ScheduledExecutorService executor,
        Supplier<String> tokenSupplier, BooleanSupplier connectionEnabled, Supplier<String> currentClanRankSupplier,
        Gson gson, ItemManager itemManager)
    {
        NightLegionApi api = new NightLegionApi(httpClient, executor, tokenSupplier, gson);
        events = new NightLegionPanel(client, api, itemManager);
        events.putClientProperty("nightlegionOwnScroll", Boolean.TRUE);
        groups = new NightLegionGroupFinderPanel(client, api, itemManager);
        groups.putClientProperty("nightlegionOwnScroll", Boolean.TRUE);
        rankTracker = new NightLegionRankTracker(client, executor, api, connectionEnabled, tokenSupplier,
            currentClanRankSupplier, profile -> rankProfileConsumer.accept(profile));
        rankTracker.start();
    }

    public JPanel eventsPanel() { return events; }
    public JPanel groupsPanel() { return groups; }

    public void setRankProfileConsumer(Consumer<com.google.gson.JsonObject> consumer)
    {
        rankProfileConsumer = consumer == null ? ignored -> { } : consumer;
    }

    public void onChatMessage(net.runelite.api.events.ChatMessage event) { rankTracker.onChatMessage(event); }
    public void onGameStateChanged(net.runelite.api.GameState state) { rankTracker.onGameStateChanged(state); }
    public void shutDown() { rankTracker.stop(); }

    public void refreshAll()
    {
        events.refresh();
        groups.refresh();
    }
}
