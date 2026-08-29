package com.nightlegion.livexp;

import java.awt.BorderLayout;
import javax.swing.JTabbedPane;
import net.runelite.api.Client;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.PluginPanel;

/** Root NightLegion sidebar: existing clan tools plus the rank-system view. */
class NightLegionRootPanel extends PluginPanel
{
    private final NightLegionPanel clanPanel;
    private final NightLegionRankPanel rankPanel;

    NightLegionRootPanel(Client client, NightLegionApi api, ItemManager itemManager)
    {
        super(false);
        setLayout(new BorderLayout());
        setBackground(NightLegionTheme.BACKGROUND);

        clanPanel = new NightLegionPanel(client, api, itemManager);
        rankPanel = new NightLegionRankPanel(client, api);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(NightLegionTheme.HEADER);
        tabs.setForeground(NightLegionTheme.SILVER);
        tabs.addTab("CLAN", clanPanel);
        tabs.addTab("RANK", rankPanel);
        tabs.addChangeListener(event ->
        {
            if (tabs.getSelectedComponent() == rankPanel)
            {
                rankPanel.refresh();
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
    }
}
