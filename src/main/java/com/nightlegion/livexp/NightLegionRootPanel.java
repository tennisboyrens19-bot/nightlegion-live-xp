package com.nightlegion.livexp;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.PluginPanel;

/**
 * NightLegion shell intentionally mirrors the compact Live On Clan layout:
 * member gate first, then a 2-column navigation grid and one content page.
 * Group Finder remains part of the Clan page as the RaidMates-style extra.
 */
class NightLegionRootPanel extends PluginPanel
{
    private final Client client;
    private final NightLegionPanel clanPanel;
    private final NightLegionRankPanel rankPanel;
    private final NightLegionCommunityPanel communityPanel;

    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final JPanel navigationGrid = new JPanel(new GridBagLayout());
    private final java.util.List<JButton> navigationButtons = new java.util.ArrayList<>();
    private final JPanel access = new JPanel(new BorderLayout());
    private final JLabel accessMessage = new JLabel("", SwingConstants.CENTER);
    private final JLabel footerStatus = new JLabel("Checking NightLegion membership...", SwingConstants.CENTER);
    private final JButton verifyButton = new JButton("Verify now");
    private String selected = "ACCESS";

    NightLegionRootPanel(Client client, NightLegionApi api, ItemManager itemManager)
    {
        super(false);
        this.client = client;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        setBackground(NightLegionTheme.BACKGROUND);

        clanPanel = new NightLegionPanel(client, api, itemManager);
        rankPanel = new NightLegionRankPanel(client, api);
        communityPanel = new NightLegionCommunityPanel(client, api);

        buildAccess();
        buildAuthenticatedPages();

        add(navigationGrid, BorderLayout.NORTH);
        add(content, BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        verifyButton.addActionListener(event -> refresh());
        showAccess("Checking your linked account...");
        refresh();
    }

    private void buildAccess()
    {
        access.setBorder(BorderFactory.createEmptyBorder(18, 8, 12, 8));
        access.setBackground(NightLegionTheme.BACKGROUND);

        JPanel welcome = new JPanel();
        welcome.setOpaque(false);
        welcome.setLayout(new BoxLayout(welcome, BoxLayout.Y_AXIS));
        welcome.add(Box.createVerticalGlue());

        JLabel logo = new JLabel(NightLegionTheme.markIcon(150, NightLegionTheme.PURPLE_BRIGHT));
        logo.setHorizontalAlignment(JLabel.CENTER);
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        welcome.add(logo);
        welcome.add(Box.createVerticalStrut(10));

        JLabel title = new JLabel("Welcome to NightLegion", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        welcome.add(title);
        welcome.add(Box.createVerticalStrut(12));

        NightLegionTheme.styleButton(verifyButton, true, false);
        verifyButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        verifyButton.setMaximumSize(new Dimension(190, 30));
        welcome.add(verifyButton);
        welcome.add(Box.createVerticalStrut(8));

        accessMessage.setAlignmentX(Component.CENTER_ALIGNMENT);
        accessMessage.setForeground(NightLegionTheme.GOLD);
        welcome.add(accessMessage);
        welcome.add(Box.createVerticalStrut(14));

        JLabel description = new JLabel(
            "<html><center>Receive clan announcements and events.<br>"
                + "Track your NightLegion rank and progress.<br>"
                + "Follow MVPs, PBs, drops and clan activity.<br>"
                + "Find PvM teams with Group Finder.</center></html>",
            SwingConstants.CENTER);
        description.setForeground(new java.awt.Color(185, 185, 185));
        description.setAlignmentX(Component.CENTER_ALIGNMENT);
        welcome.add(description);
        welcome.add(Box.createVerticalStrut(9));

        JLabel membersOnly = new JLabel("Exclusive to NightLegion members", SwingConstants.CENTER);
        membersOnly.setForeground(NightLegionTheme.GOLD);
        membersOnly.setAlignmentX(Component.CENTER_ALIGNMENT);
        welcome.add(membersOnly);
        welcome.add(Box.createVerticalGlue());

        access.add(welcome, BorderLayout.CENTER);
        content.add(access, "ACCESS");
    }

    private void buildAuthenticatedPages()
    {
        addPage("HOME", "Home", communityPanel, "Clan dashboard");
        addPage("RANK", "Ranks", rankPanel, "NightLegion rank progress");
        addPage("EVENTS", "Events", clanPanel, "BOTW, SOTW, giveaways and Group Finder");
    }

    private void addPage(String key, String title, JPanel panel, String tooltip)
    {
        JScrollPane scroll = new JScrollPane(panel);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        content.add(scroll, key);

        JButton button = new JButton(title);
        button.setName(key);
        button.setToolTipText(tooltip);
        button.setFocusPainted(false);
        button.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        button.addActionListener(event -> showPage(key));
        navigationButtons.add(button);

        int index = navigationButtons.size() - 1;
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = index % 2;
        c.gridy = index / 2;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new java.awt.Insets(0, index % 2 == 1 ? 2 : 0, 3, index % 2 == 0 ? 2 : 0);
        navigationGrid.add(button, c);
    }

    private JPanel buildFooter()
    {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, NightLegionTheme.BORDER),
            BorderFactory.createEmptyBorder(6, 10, 2, 10)));

        JPanel links = new JPanel(new GridLayout(1, 2, 5, 0));
        JButton discord = new JButton("Discord");
        JButton refresh = new JButton("Refresh");
        discord.addActionListener(event -> net.runelite.client.util.LinkBrowser.browse("https://discord.gg/EyAvTDmE3"));
        refresh.addActionListener(event -> refresh());
        links.add(discord);
        links.add(refresh);

        footerStatus.setForeground(NightLegionTheme.MUTED);
        footerStatus.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        footer.add(links, BorderLayout.CENTER);
        footer.add(footerStatus, BorderLayout.SOUTH);
        return footer;
    }

    private void showAccess(String message)
    {
        selected = "ACCESS";
        navigationGrid.setVisible(false);
        accessMessage.setText("<html><center>" + message + "</center></html>");
        cards.show(content, "ACCESS");
        footerStatus.setText("Checking NightLegion membership...");
        revalidate();
        repaint();
    }

    private void showPage(String page)
    {
        selected = page;
        navigationGrid.setVisible(true);
        cards.show(content, page);
        for (JButton button : navigationButtons)
        {
            boolean active = page.equals(button.getName());
            button.setForeground(active ? NightLegionTheme.GOLD : new java.awt.Color(210, 210, 210));
            button.setBackground(active ? new java.awt.Color(50, 50, 50) : new java.awt.Color(35, 35, 35));
            button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, active ? 2 : 1, 0,
                    active ? NightLegionTheme.GOLD : NightLegionTheme.BORDER),
                BorderFactory.createEmptyBorder(3, 3, active ? 2 : 3, 3)));
        }
        if ("RANK".equals(page)) rankPanel.refresh();
        else if ("HOME".equals(page)) communityPanel.refresh();
        else clanPanel.refresh();
        footerStatus.setText(currentRsn().isEmpty() ? "NightLegion" : "Authenticated as " + currentRsn());
        revalidate();
        repaint();
    }

    void refresh()
    {
        String rsn = currentRsn();
        if (client.getGameState() != GameState.LOGGED_IN || rsn.isEmpty())
        {
            showAccess("Log in to RuneScape to verify your NightLegion membership.");
            return;
        }

        // Membership/account data is already validated by the existing NightLegion
        // companion endpoints. Render the same member-first flow as Live On while
        // preserving the existing API and features.
        navigationGrid.setVisible(true);
        if ("ACCESS".equals(selected))
        {
            showPage("HOME");
        }
        else
        {
            showPage(selected);
        }
    }

    private String currentRsn()
    {
        Player player = client.getLocalPlayer();
        return player == null || player.getName() == null ? "" : player.getName().trim();
    }
}
