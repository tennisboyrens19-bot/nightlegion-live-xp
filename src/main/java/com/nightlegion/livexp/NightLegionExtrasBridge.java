package com.nightlegion.livexp;

import com.google.gson.Gson;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;
import javax.swing.JPanel;
import net.runelite.api.Client;
import net.runelite.client.game.ItemManager;
import okhttp3.OkHttpClient;

/** Keeps NightLegion-only BOTW/SOTW/Giveaway/Group Finder inside the exact Live On shell. */
public final class NightLegionExtrasBridge
{
    private final NightLegionPanel botw;
    private final NightLegionPanel sotw;
    private final NightLegionPanel giveaway;
    private final NightLegionPanel groups;

    public NightLegionExtrasBridge(Client client, OkHttpClient httpClient, ScheduledExecutorService executor,
        Supplier<String> tokenSupplier, Gson gson, ItemManager itemManager)
    {
        NightLegionApi api = new NightLegionApi(httpClient, executor, tokenSupplier, gson);
        botw = panel(client, api, itemManager, "BOTW");
        sotw = panel(client, api, itemManager, "SOTW");
        giveaway = panel(client, api, itemManager, "GIVEAWAY");
        groups = panel(client, api, itemManager, "GROUP FINDER");
    }

    private static NightLegionPanel panel(Client client, NightLegionApi api, ItemManager itemManager, String section)
    {
        NightLegionPanel panel = new NightLegionPanel(client, api, itemManager, section);
        panel.putClientProperty("nightlegionOwnScroll", Boolean.TRUE);
        return panel;
    }

    public JPanel botwPanel() { return botw; }
    public JPanel sotwPanel() { return sotw; }
    public JPanel giveawayPanel() { return giveaway; }
    public JPanel groupsPanel() { return groups; }

    public void refreshAll()
    {
        botw.refresh();
        sotw.refresh();
        giveaway.refresh();
        groups.refresh();
    }
}
