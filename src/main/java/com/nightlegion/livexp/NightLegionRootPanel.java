package com.nightlegion.livexp;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import net.runelite.api.Client;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.PluginPanel;

/** Root NightLegion sidebar: clan tools, rank system, and community/staff hub. */
class NightLegionRootPanel extends PluginPanel
{
    private final NightLegionPanel clanPanel;
    private final NightLegionRankPanel rankPanel;
    private final NightLegionCommunityPanel communityPanel;
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final JPanel nav = new JPanel(new GridLayout(1, 3, 5, 0));
    private final JButton clanButton = navButton("CLAN", "BOTW · SOTW · GROUPS");
    private final JButton rankButton = navButton("RANK", "POINTS · PROGRESS");
    private final JButton hubButton = navButton("HUB", "NEWS · MVP · LIVE");
    private String selected = "CLAN";

    NightLegionRootPanel(Client client, NightLegionApi api, ItemManager itemManager)
    {
        super(false);
        setLayout(new BorderLayout());
        setBackground(NightLegionTheme.BACKGROUND);

        clanPanel = new NightLegionPanel(client, api, itemManager);
        rankPanel = new NightLegionRankPanel(client, api);
        communityPanel = new NightLegionCommunityPanel(client, api);

        content.setBackground(NightLegionTheme.BACKGROUND);
        content.add(clanPanel, "CLAN");
        content.add(rankPanel, "RANK");
        content.add(communityPanel, "HUB");

        add(buildTopShell(), BorderLayout.NORTH);
        add(content, BorderLayout.CENTER);
        showPage("CLAN");
    }

    private JPanel buildTopShell()
    {
        JPanel shell = new JPanel();
        shell.setLayout(new BoxLayout(shell, BoxLayout.Y_AXIS));
        shell.setBackground(NightLegionTheme.HEADER);
        shell.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 2, 0, NightLegionTheme.PURPLE),
            BorderFactory.createEmptyBorder(9, 9, 8, 9)));

        JPanel brand = new JPanel(new BorderLayout(8, 0));
        brand.setOpaque(false);
        brand.setAlignmentX(Component.LEFT_ALIGNMENT);
        brand.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        brand.add(new JLabel(NightLegionTheme.markIcon(27, NightLegionTheme.PURPLE_BRIGHT)), BorderLayout.WEST);

        JPanel names = new JPanel();
        names.setLayout(new BoxLayout(names, BoxLayout.Y_AXIS));
        names.setOpaque(false);
        JLabel title = new JLabel("NIGHTLEGION");
        title.setForeground(NightLegionTheme.PURPLE_BRIGHT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        JLabel subtitle = new JLabel("Clan command center");
        subtitle.setForeground(NightLegionTheme.MUTED);
        subtitle.setFont(subtitle.getFont().deriveFont(9.5f));
        names.add(title);
        names.add(subtitle);
        brand.add(names, BorderLayout.CENTER);

        JLabel badge = new JLabel("LIVE", SwingConstants.CENTER);
        badge.setOpaque(true);
        badge.setBackground(NightLegionTheme.SURFACE_ALT);
        badge.setForeground(NightLegionTheme.SILVER);
        badge.setFont(badge.getFont().deriveFont(Font.BOLD, 9f));
        badge.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(NightLegionTheme.PURPLE.darker()),
            BorderFactory.createEmptyBorder(3, 7, 3, 7)));
        brand.add(badge, BorderLayout.EAST);

        nav.setOpaque(false);
        nav.setAlignmentX(Component.LEFT_ALIGNMENT);
        nav.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        nav.add(clanButton);
        nav.add(rankButton);
        nav.add(hubButton);
        clanButton.addActionListener(event -> showPage("CLAN"));
        rankButton.addActionListener(event -> showPage("RANK"));
        hubButton.addActionListener(event -> showPage("HUB"));

        shell.add(brand);
        shell.add(Box.createVerticalStrut(7));
        shell.add(nav);
        return shell;
    }

    private static JButton navButton(String title, String subtitle)
    {
        JButton button = new JButton("<html><center><b>" + title + "</b><br><font size='2'>" + subtitle + "</font></center></html>");
        button.setFocusable(false);
        button.setHorizontalAlignment(SwingConstants.CENTER);
        button.setMargin(new java.awt.Insets(3, 3, 3, 3));
        button.setPreferredSize(new Dimension(70, 40));
        NightLegionTheme.styleButton(button, false, false);
        return button;
    }

    private void showPage(String page)
    {
        selected = page;
        cards.show(content, page);
        NightLegionTheme.styleButton(clanButton, "CLAN".equals(page), false);
        NightLegionTheme.styleButton(rankButton, "RANK".equals(page), false);
        NightLegionTheme.styleButton(hubButton, "HUB".equals(page), false);

        if ("RANK".equals(page))
        {
            rankPanel.refresh();
        }
        else if ("HUB".equals(page))
        {
            communityPanel.refresh();
        }
        else
        {
            clanPanel.refresh();
        }
        revalidate();
        repaint();
    }

    void refresh()
    {
        if ("RANK".equals(selected))
        {
            rankPanel.refresh();
        }
        else if ("HUB".equals(selected))
        {
            communityPanel.refresh();
        }
        else
        {
            clanPanel.refresh();
        }
    }
}
