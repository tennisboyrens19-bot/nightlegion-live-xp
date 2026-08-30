package com.nightlegion.livexp;

import java.awt.BorderLayout;
import javax.swing.JTabbedPane;
import net.runelite.api.Client;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.PluginPanel;

/** Root NightLegion sidebar: clan tools, rank system, and community/staff hub. */
class NightLegionRootPanel extends PluginPanel
{
    private final NightLegionPanel clanPanel;
    private final NightLegionRankPanel rankPanel;
    private final NightLegionCommunityPanel communityPanel;

    NightLegionRootPanel(Client client, NightLegionApi api, ItemManager itemManager)
    {
        super(false);
        setLayout(new BorderLayout());
        setBackground(NightLegionTheme.BACKGROUND);

        clanPanel = new NightLegionPanel(client, api, itemManager);
        rankPanel = new NightLegionRankPanel(client, api);
        communityPanel = new NightLegionCommunityPanel(client, api);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(NightLegionTheme.HEADER);
        tabs.setForeground(NightLegionTheme.SILVER);
        tabs.addTab("CLAN", clanPanel);
        tabs.addTab("RANK", rankPanel);
        tabs.addTab("HUB", communityPanel);
        tabs.addChangeListener(event ->
        {
            if (tabs.getSelectedComponent() == rankPanel)
            {
                rankPanel.refresh();
            }
            else if (tabs.getSelectedComponent() == communityPanel)
            {
                communityPanel.refresh();
            }
            else
            {
                clanPanel.refresh();
            }
        });
        add(tabs, BorderLayout.CENTER);
    }

    void refresh()
    {
        clanPanel.refresh();
        rankPanel.refresh();
        communityPanel.refresh();
    }
}
