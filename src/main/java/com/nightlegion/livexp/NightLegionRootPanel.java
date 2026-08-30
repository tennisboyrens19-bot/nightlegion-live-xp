package com.nightlegion.livexp;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.LinkBrowser;

/**
 * NightLegion member panel based directly on the permitted Live On Clan UI.
 * Only clan names/content/hooks are changed; the visual shell intentionally
 * keeps the same spacing, navigation, footer and access flow.
 */
class NightLegionRootPanel extends PluginPanel
{
    private static final Color SELECTED = new Color(255, 152, 0);
    private static final Color NAV_TEXT = new Color(210, 210, 210);
    private static final Color NAV_SELECTED_BG = new Color(50, 50, 50);
    private static final Color NAV_BG = new Color(35, 35, 35);
    private static final Color NAV_BORDER = new Color(55, 55, 55);

    private final Client client;
    private final NightLegionApi api;
    private final NightLegionLiveXpConfig config;
    private final ConfigManager configManager;

    private final NightLegionCommunityPanel homePanel;
    private final NightLegionRankPanel rankPanel;
    private final NightLegionCommunityPanel mvpPanel;
    private final NightLegionCommunityPanel pbPanel;
    private final NightLegionPanel eventsPanel;
    private final NightLegionGroupFinderPanel groupsPanel;

    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final JPanel navigationGrid = new JPanel(new GridBagLayout());
    private final java.util.List<JButton> navigationButtons = new java.util.ArrayList<>();
    private final JPanel access = new JPanel(new BorderLayout(5, 5));
    private final JLabel accessMessage = new JLabel("", SwingConstants.CENTER);
    private final JLabel footerStatus = new JLabel("Disconnected", SwingConstants.CENTER);
    private final JButton verifyButton = new JButton("Verify now");

    private String selected = "ACCESS";

    NightLegionRootPanel(
        Client client,
        NightLegionApi api,
        ItemManager itemManager,
        NightLegionLiveXpConfig config,
        ConfigManager configManager)
    {
        super(false);
        this.client = client;
        this.api = api;
        this.config = config;
        this.configManager = configManager;

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH, 650));

        homePanel = new NightLegionCommunityPanel(client, api, "ANNOUNCEMENTS");
        rankPanel = new NightLegionRankPanel(client, api);
        mvpPanel = new NightLegionCommunityPanel(client, api, "MVP");
        pbPanel = new NightLegionCommunityPanel(client, api, "PB LEADERBOARD");
        eventsPanel = new NightLegionPanel(client, api, itemManager);
        groupsPanel = new NightLegionGroupFinderPanel(client, api, itemManager);

        createAccessTab();
        createAuthenticatedPages();

        add(navigationGrid, BorderLayout.NORTH);
        add(content, BorderLayout.CENTER);
        add(createLinksFooter(), BorderLayout.SOUTH);

        verifyButton.addActionListener(event -> verifyMembership(true));
        navigationGrid.setVisible(false);
        cards.show(content, "ACCESS");
        refresh();
    }

    private void createAccessTab()
    {
        // These values are intentionally the same as Live On Clan.
        access.setBorder(BorderFactory.createEmptyBorder(18, 8, 12, 8));

        JPanel welcome = new JPanel();
        welcome.setLayout(new BoxLayout(welcome, BoxLayout.Y_AXIS));
        welcome.add(Box.createVerticalGlue());

        JLabel logo = new JLabel(NightLegionTheme.markIcon(180, NightLegionTheme.PURPLE_BRIGHT));
        logo.setHorizontalAlignment(JLabel.CENTER);
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        welcome.add(logo);
        welcome.add(Box.createVerticalStrut(8));

        JLabel title = new JLabel("Welcome to NightLegion", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        welcome.add(title);
        welcome.add(Box.createVerticalStrut(12));

        verifyButton.setBackground(new Color(190, 104, 0));
        verifyButton.setForeground(Color.WHITE);
        verifyButton.setFocusPainted(false);
        verifyButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        verifyButton.setMaximumSize(new Dimension(190, 30));
        welcome.add(verifyButton);
        welcome.add(Box.createVerticalStrut(8));

        accessMessage.setHorizontalAlignment(JLabel.CENTER);
        accessMessage.setForeground(new Color(235, 178, 55));
        accessMessage.setAlignmentX(Component.CENTER_ALIGNMENT);
        welcome.add(accessMessage);
        welcome.add(Box.createVerticalStrut(14));

        JLabel description = new JLabel(
            "<html><center>Receive NightLegion announcements and events.<br>"
                + "Track your rank and clan progress.<br>"
                + "Follow MVPs, PBs and clan activity.<br>"
                + "Find your next PvM team.</center></html>",
            SwingConstants.CENTER);
        description.setForeground(new Color(185, 185, 185));
        description.setAlignmentX(Component.CENTER_ALIGNMENT);
        welcome.add(description);
        welcome.add(Box.createVerticalStrut(8));

        JLabel membersOnly = new JLabel("Exclusive to NightLegion members", SwingConstants.CENTER);
        membersOnly.setForeground(new Color(210, 160, 55));
        membersOnly.setAlignmentX(Component.CENTER_ALIGNMENT);
        welcome.add(membersOnly);
        welcome.add(Box.createVerticalGlue());

        access.add(welcome, BorderLayout.CENTER);
        content.add(access, "ACCESS");
    }

    private void createAuthenticatedPages()
    {
        addNavigationButton("Home", "home", homePanel, "HOME", "NightLegion clan dashboard");
        addNavigationButton("Ranks", "ranks", rankPanel, "RANK", "NightLegion rank progress");
        addNavigationButton("MVPs", "crown", mvpPanel, "MVP", "NightLegion MVP rankings");
        addNavigationButton("PBs", "trophy", pbPanel, "PBS", "NightLegion personal-best leaderboards");
        addNavigationButton("Events", "star", eventsPanel, "EVENTS", "BOTW, SOTW and Giveaways");
        addNavigationButton("Groups", "groups", groupsPanel, "GROUPS", "NightLegion Group Finder");
    }

    private void addNavigationButton(String title, String iconType, JPanel component, String pageKey, String tooltip)
    {
        component.setMinimumSize(new Dimension(0, 0));

        JScrollPane pageScroll = new JScrollPane(component);
        pageScroll.setBorder(BorderFactory.createEmptyBorder());
        pageScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        pageScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        pageScroll.getVerticalScrollBar().setUnitIncrement(16);
        pageScroll.setMinimumSize(new Dimension(0, 0));
        content.add(pageScroll, pageKey);

        JButton button = new JButton(title, createUiIcon(iconType));
        button.setName(pageKey);
        button.setToolTipText(tooltip);
        button.setIconTextGap(5);
        button.setMargin(new java.awt.Insets(5, 4, 5, 4));
        button.setFocusPainted(false);
        button.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        button.addActionListener(event -> selectPage(pageKey));
        navigationButtons.add(button);

        int index = navigationButtons.size() - 1;
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = index % 2;
        constraints.gridy = index / 2;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new java.awt.Insets(
            0,
            index % 2 == 1 ? 2 : 0,
            3,
            index % 2 == 0 ? 2 : 0);
        navigationGrid.add(button, constraints);
    }

    private JPanel createLinksFooter()
    {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(55, 55, 55)),
            BorderFactory.createEmptyBorder(6, 10, 2, 10)));

        JPanel links = new JPanel(new GridLayout(1, 2, 5, 0));

        JButton discord = new JButton("Discord", createUiIcon("message"));
        discord.setToolTipText("Open NightLegion Discord");
        discord.setMargin(new java.awt.Insets(3, 6, 3, 6));
        discord.addActionListener(event -> LinkBrowser.browse("https://discord.gg/EyAvTDmE3"));

        JButton wom = new JButton("WOM", createUiIcon("trophy"));
        wom.setToolTipText("Open Wise Old Man");
        wom.setMargin(new java.awt.Insets(3, 6, 3, 6));
        wom.addActionListener(event -> LinkBrowser.browse("https://wiseoldman.net/groups"));

        links.add(discord);
        links.add(wom);

        footerStatus.setForeground(new Color(165, 165, 165));
        footerStatus.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        footer.add(links, BorderLayout.CENTER);
        footer.add(footerStatus, BorderLayout.SOUTH);
        return footer;
    }

    private void selectPage(String pageKey)
    {
        selected = pageKey;
        cards.show(content, pageKey);
        for (JButton button : navigationButtons)
        {
            boolean active = pageKey.equals(button.getName());
            button.setForeground(active ? SELECTED : NAV_TEXT);
            button.setBackground(active ? NAV_SELECTED_BG : NAV_BG);
            button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, active ? 2 : 1, 0, active ? SELECTED : NAV_BORDER),
                BorderFactory.createEmptyBorder(3, 3, active ? 2 : 3, 3)));
        }
        refreshPage(pageKey);
        footerStatus.setText(currentRsn().isEmpty() ? "NightLegion" : "Authenticated as " + currentRsn());
    }

    private void refreshPage(String pageKey)
    {
        switch (pageKey)
        {
            case "HOME":
                homePanel.refresh();
                break;
            case "RANK":
                rankPanel.refresh();
                break;
            case "MVP":
                mvpPanel.refresh();
                break;
            case "PBS":
                pbPanel.refresh();
                break;
            case "GROUPS":
                groupsPanel.refresh();
                break;
            default:
                eventsPanel.refresh();
                break;
        }
    }

    void refresh()
    {
        verifyMembership(false);
    }

    private void verifyMembership(boolean allowPrompt)
    {
        String rsn = currentRsn();
        if (!config.enabled())
        {
            showAccess("Enable <b>Connect to clan</b><br>in the NightLegion settings.");
            footerStatus.setText("Connection disabled");
            return;
        }

        if (client.getGameState() != GameState.LOGGED_IN || rsn.isEmpty())
        {
            showAccess("Log in to RuneScape to verify your NightLegion membership.");
            return;
        }

        String token = config.token() == null ? "" : config.token().trim();
        if (token.isEmpty())
        {
            showAccess("Member not identified.<br>Click <b>Verify now</b> to link NightLegion.");
            footerStatus.setText("Not authenticated");
            if (!allowPrompt)
            {
                return;
            }

            JPasswordField field = new JPasswordField();
            field.setPreferredSize(new Dimension(220, 26));
            JPanel prompt = new JPanel(new BorderLayout(0, 6));
            prompt.add(new JLabel(
                "<html>Run <b>/runelite_link</b> in NightLegion Discord,<br>"
                    + "then paste the Personal Link Token here.</html>"),
                BorderLayout.NORTH);
            prompt.add(field, BorderLayout.CENTER);

            int choice = JOptionPane.showConfirmDialog(
                this,
                prompt,
                "Link NightLegion",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
            if (choice != JOptionPane.OK_OPTION)
            {
                return;
            }

            char[] password = field.getPassword();
            try
            {
                token = new String(password).trim();
            }
            finally
            {
                java.util.Arrays.fill(password, '\0');
            }
            if (token.isEmpty())
            {
                return;
            }
            configManager.setConfiguration("nightlegionlivexp", "token", token);
        }

        verifyButton.setEnabled(false);
        verifyButton.setText("Checking...");
        accessMessage.setText("<html><center>Checking NightLegion membership...</center></html>");

        api.action("overview", rsn, new com.google.gson.JsonObject(), result ->
            javax.swing.SwingUtilities.invokeLater(() ->
            {
                verifyButton.setEnabled(true);
                verifyButton.setText("Verify now");
                navigationGrid.setVisible(true);
                if ("ACCESS".equals(selected))
                {
                    selectPage("HOME");
                }
                else
                {
                    selectPage(selected);
                }
                revalidate();
                repaint();
            }), error -> javax.swing.SwingUtilities.invokeLater(() ->
            {
                verifyButton.setEnabled(true);
                verifyButton.setText("Verify now");
                showAccess("Member not identified.<br>" + escapeHtml(error));
                footerStatus.setText("Not authenticated");
            }));
    }

    private void showAccess(String message)
    {
        selected = "ACCESS";
        navigationGrid.setVisible(false);
        accessMessage.setText("<html><center>" + message + "</center></html>");
        cards.show(content, "ACCESS");
        revalidate();
        repaint();
    }

    private String currentRsn()
    {
        Player player = client.getLocalPlayer();
        return player == null || player.getName() == null ? "" : player.getName().trim();
    }

    private static String escapeHtml(String value)
    {
        if (value == null)
        {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    // Copied from the Live On navigation icon approach, with a Group Finder icon
    // added for the RaidMates-style page.
    private ImageIcon createUiIcon(String type)
    {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setColor("groups".equals(type) ? new Color(50, 210, 90)
            : ("home".equals(type) || "crown".equals(type) || "trophy".equals(type)
                || "star".equals(type) || "ranks".equals(type))
                ? new Color(225, 170, 45)
                : new Color(210, 210, 210));
        graphics.setStroke(new java.awt.BasicStroke(
            1.7f,
            java.awt.BasicStroke.CAP_ROUND,
            java.awt.BasicStroke.JOIN_ROUND));

        if ("trophy".equals(type))
        {
            graphics.drawRect(5, 2, 6, 7);
            graphics.drawArc(1, 3, 6, 6, 80, 200);
            graphics.drawArc(9, 3, 6, 6, -100, 200);
            graphics.drawLine(8, 9, 8, 13);
            graphics.drawLine(5, 14, 11, 14);
        }
        else if ("crown".equals(type))
        {
            graphics.fillPolygon(
                new int[]{2, 4, 7, 8, 9, 12, 14, 13, 3},
                new int[]{5, 9, 4, 9, 4, 9, 5, 12, 12},
                9);
            graphics.fillRect(3, 12, 10, 2);
        }
        else if ("home".equals(type))
        {
            graphics.drawPolygon(new int[]{2, 8, 14}, new int[]{8, 2, 8}, 3);
            graphics.drawRect(4, 7, 8, 7);
            graphics.drawRect(7, 10, 3, 4);
        }
        else if ("ranks".equals(type))
        {
            graphics.drawPolygon(
                new int[]{8, 14, 12, 8, 4, 2},
                new int[]{1, 4, 11, 15, 11, 4},
                6);
            graphics.drawLine(5, 6, 11, 6);
            graphics.drawLine(6, 9, 10, 9);
        }
        else if ("star".equals(type))
        {
            graphics.fillPolygon(
                new int[]{8, 10, 15, 11, 12, 8, 4, 5, 1, 6},
                new int[]{1, 6, 6, 9, 14, 11, 14, 9, 6, 6},
                10);
        }
        else if ("groups".equals(type))
        {
            graphics.fillOval(2, 3, 5, 5);
            graphics.fillOval(9, 3, 5, 5);
            graphics.drawArc(1, 8, 7, 6, 20, 140);
            graphics.drawArc(8, 8, 7, 6, 20, 140);
        }
        else
        {
            graphics.drawRoundRect(2, 3, 12, 8, 2, 2);
            graphics.drawLine(5, 11, 4, 14);
            graphics.drawLine(5, 11, 8, 11);
        }

        graphics.dispose();
        return new ImageIcon(image);
    }
}
