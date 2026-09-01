package com.nightlegion.livexp;

import com.google.gson.Gson;
import java.util.concurrent.ScheduledExecutorService;
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

    public NightLegionExtrasBridge(Client client, OkHttpClient httpClient, ScheduledExecutorService executor,
        Supplier<String> tokenSupplier, Gson gson, ItemManager itemManager)
    {
        NightLegionApi api = new NightLegionApi(httpClient, executor, tokenSupplier, gson);
        events = new NightLegionPanel(client, api, itemManager);
        events.putClientProperty("nightlegionOwnScroll", Boolean.TRUE);
        groups = new NightLegionGroupFinderPanel(client, api, itemManager);
        groups.putClientProperty("nightlegionOwnScroll", Boolean.TRUE);
    }

    public JPanel eventsPanel() { return events; }
    public JPanel groupsPanel() { return groups; }

    public void refreshAll()
    {
        events.refresh();
        groups.refresh();
    }
}
