package com.nightlegion.livexp;

import com.google.gson.JsonObject;
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
import java.util.ArrayList;
import java.util.List;
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
 * NightLegion shell copied from Live On's proven RuneLite layout, translated to
 * English. NightLegion's BOTW, SOTW, Giveaways and Groups remain intact.
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

    private final NightLegionHomePanel homePanel;
    private final NightLegionRankPanel rankPanel;
    private final NightLegionMvpPanel mvpPanel;
    private final NightLegionPbPanel pbPanel;
    private final NightLegionPanel eventsPanel;
    private final NightLegionGroupFinderPanel groupsPanel;
    private final NightLegionStaffPanel staffPanel;

    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final JPanel navigationGrid = new JPanel(new GridBagLayout());
    private final List<JButton> navigationButtons = new ArrayList<>();
    private final JPanel access = new JPanel(new BorderLayout(5, 5));
    private final JLabel accessMessage = new JLabel("", SwingConstants.CENTER);
    private final JLabel footerStatus = new JLabel("Disconnected", SwingConstants.CENTER);
    private final JButton verifyButton = new JButton("Verify now");
    private JButton staffButton;

    private String selected = "ACCESS";

    NightLegionRootPanel(Client client, NightLegionApi api, ItemManager itemManager,
        NightLegionLiveXpConfig config, ConfigManager configManager)
    {
        super(false);
        this.client = client;
        this.api = api;
        this.config = config;
        this.configManager = configManager;

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH, 650));

        homePanel = new NightLegionHomePanel(client, api);
        rankPanel = new NightLegionRankPanel(client, api);
        mvpPanel = new NightLegionMvpPanel(client, api, config);
        pbPanel = new NightLegionPbPanel(client, api, config);
        eventsPanel = new NightLegionPanel(client, api, itemManager);
        groupsPanel = new NightLegionGroupFinderPanel(client, api, itemManager);
        staffPanel = new NightLegionStaffPanel(client, api);

        createAccessTab();
        createAuthenticatedPages();
        add(navigationGrid, BorderLayout.NORTH);
        add(content, BorderLayout.CENTER);
        add(createLinksFooter(), BorderLayout.SOUTH);

        verifyButton.addActionListener(event -> verifyMembership(true));
        navigationGrid.setVisible(false);
        if (staffButton != null)
        {
            staffButton.setVisible(false);
        }
        cards.show(content, "ACCESS");
        refresh();
    }

    private void createAccessTab()
    {
        access.setBorder(BorderFactory.createEmptyBorder(18, 8, 12, 8));
        JPanel welcome = new JPanel();
        welcome.setLayout(new BoxLayout(welcome, BoxLayout.Y_AXIS));
        welcome.add(Box.createVerticalGlue());

        JLabel logo = new JLabel(NightLegionArtwork.welcomeIcon(205));
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
            "<html><center>NightLegion announcements and clan activity.<br>"
                + "Ranks, Monthly MVPs and PB leaderboards.<br>"
                + "BOTW, SOTW, Giveaways and Group Finder.</center></html>", SwingConstants.CENTER);
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
        addNavigationButton("Home", "home", homePanel, "HOME", "NightLegion clan dashboard", false, false);
        addNavigationButton("Ranks", "ranks", rankPanel, "RANK", "NightLegion rank progress", false, false);
        addNavigationButton("MVPs", "crown", mvpPanel, "MVP", "Monthly NightLegion MVP rankings", false, false);
        addNavigationButton("PBs", "trophy", pbPanel, "PBS", "NightLegion personal-best leaderboards", false, false);
        addNavigationButton("Events", "star", eventsPanel, "EVENTS", "BOTW, SOTW and Giveaways", true, false);
        addNavigationButton("Groups", "groups", groupsPanel, "GROUPS", "NightLegion Group Finder", true, false);
        staffButton = addNavigationButton("Staff", "key", staffPanel, "STAFF", "Owner controls", false, true);
    }

    private JButton addNavigationButton(String title, String iconType, JPanel component, String pageKey,
        String tooltip, boolean wrapInScroll, boolean fullWidth)
    {
        component.setMinimumSize(new Dimension(0, 0));
        if (wrapInScroll)
        {
            JScrollPane pageScroll = new JScrollPane(component);
            pageScroll.setBorder(BorderFactory.createEmptyBorder());
            pageScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            pageScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
            pageScroll.getVerticalScrollBar().setUnitIncrement(16);
            pageScroll.setMinimumSize(new Dimension(0, 0));
            content.add(pageScroll, pageKey);
        }
        else
        {
            content.add(component, pageKey);
        }

        JButton button = new JButton(title, createUiIcon(iconType));
        button.setName(pageKey);
        button.setToolTipText(tooltip);
        button.setIconTextGap(5);
        button.setMargin(new java.awt.Insets(5, 4, 5, 4));
        button.setFocusPainted(false);
        button.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        button.addActionListener(event -> selectPage(pageKey));
        navigationButtons.add(button);

        int normalIndex = 0;
        for (JButton candidate : navigationButtons)
        {
            if (!"STAFF".equals(candidate.getName()))
            {
                normalIndex++;
            }
        }
        normalIndex--;

        GridBagConstraints c = new GridBagConstraints();
        if (fullWidth)
        {
            c.gridx = 0;
            c.gridy = 3;
            c.gridwidth = 2;
        }
        else
        {
            c.gridx = normalIndex % 2;
            c.gridy = normalIndex / 2;
            c.gridwidth = 1;
        }
        c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new java.awt.Insets(0, c.gridx == 1 ? 2 : 0, 3, c.gridx == 0 && !fullWidth ? 2 : 0);
        navigationGrid.add(button, c);
        styleNavigationButton(button, false);
        return button;
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
        discord.addActionListener(event -> LinkBrowser.browse("https://discord.gg/AP2aK742SZ"));

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
            styleNavigationButton(button, pageKey.equals(button.getName()));
        }
        refreshPage(pageKey);
        footerStatus.setText(currentRsn().isEmpty() ? "NightLegion" : "Authenticated as " + currentRsn());
    }

    private static void styleNavigationButton(JButton button, boolean active)
    {
        button.setForeground(active ? SELECTED : NAV_TEXT);
        button.setBackground(active ? NAV_SELECTED_BG : NAV_BG);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, active ? 2 : 1, 0, active ? SELECTED : NAV_BORDER),
            BorderFactory.createEmptyBorder(3, 3, active ? 2 : 3, 3)));
    }

    private void refreshPage(String pageKey)
    {
        switch (pageKey)
        {
            case "HOME": homePanel.refresh(); break;
            case "RANK": rankPanel.refresh(); break;
            case "MVP": mvpPanel.refresh(); break;
            case "PBS": pbPanel.refresh(); break;
            case "GROUPS": groupsPanel.refresh(); break;
            case "STAFF": staffPanel.refresh(); break;
            default: eventsPanel.refresh(); break;
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
            showAccess("Enable <b>Connect to clan server</b><br>in NightLegion settings.");
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
            prompt.add(new JLabel("<html>Run <b>/runelite_link</b> in NightLegion Discord,<br>then paste the Personal Link Token here.</html>"), BorderLayout.NORTH);
            prompt.add(field, BorderLayout.CENTER);
            int choice = JOptionPane.showConfirmDialog(this, prompt, "Link NightLegion",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
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
        api.action("overview", rsn, new JsonObject(), result -> javax.swing.SwingUtilities.invokeLater(() ->
        {
            verifyButton.setEnabled(true);
            verifyButton.setText("Verify now");
            navigationGrid.setVisible(true);
            setOwnerNavigation(isOwner(result));
            selectPage("ACCESS".equals(selected) ? "HOME" : selected);
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

    private boolean isOwner(JsonObject overview)
    {
        try
        {
            JsonObject community = overview.has("community") && overview.get("community").isJsonObject()
                ? overview.getAsJsonObject("community") : new JsonObject();
            return community.has("is_owner") && community.get("is_owner").getAsBoolean();
        }
        catch (Exception ignored)
        {
            return false;
        }
    }

    private void setOwnerNavigation(boolean owner)
    {
        if (staffButton != null)
        {
            staffButton.setVisible(owner);
        }
        if (!owner && "STAFF".equals(selected))
        {
            selected = "HOME";
            cards.show(content, "HOME");
        }
        navigationGrid.revalidate();
        navigationGrid.repaint();
    }

    private void showAccess(String message)
    {
        selected = "ACCESS";
        navigationGrid.setVisible(false);
        if (staffButton != null)
        {
            staffButton.setVisible(false);
        }
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

    private ImageIcon createUiIcon(String type)
    {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setColor("groups".equals(type) ? new Color(50, 210, 90)
            : ("home".equals(type) || "crown".equals(type) || "trophy".equals(type)
                || "star".equals(type) || "ranks".equals(type) || "key".equals(type))
                ? new Color(225, 170, 45) : new Color(210, 210, 210));
        graphics.setStroke(new java.awt.BasicStroke(1.7f, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));

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
            graphics.fillPolygon(new int[]{2, 4, 7, 8, 9, 12, 14, 13, 3},
                new int[]{5, 9, 4, 9, 4, 9, 5, 12, 12}, 9);
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
            graphics.drawPolygon(new int[]{8, 14, 12, 8, 4, 2}, new int[]{1, 4, 11, 15, 11, 4}, 6);
            graphics.drawLine(5, 6, 11, 6);
            graphics.drawLine(6, 9, 10, 9);
        }
        else if ("star".equals(type))
        {
            graphics.fillPolygon(new int[]{8, 10, 15, 11, 12, 8, 4, 5, 1, 6},
                new int[]{1, 6, 6, 9, 14, 11, 14, 9, 6, 6}, 10);
        }
        else if ("groups".equals(type))
        {
            graphics.fillOval(2, 3, 5, 5);
            graphics.fillOval(9, 3, 5, 5);
            graphics.drawArc(1, 8, 7, 6, 20, 140);
            graphics.drawArc(8, 8, 7, 6, 20, 140);
        }
        else if ("key".equals(type))
        {
            graphics.drawOval(2, 2, 7, 7);
            graphics.drawLine(8, 8, 14, 14);
            graphics.drawLine(11, 11, 13, 9);
            graphics.drawLine(13, 13, 15, 11);
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
