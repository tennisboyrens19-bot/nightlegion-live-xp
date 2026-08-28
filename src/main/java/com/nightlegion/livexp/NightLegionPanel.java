package com.nightlegion.livexp;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.Window;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JViewport;
import javax.swing.ScrollPaneConstants;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.AsyncBufferedImage;

class NightLegionPanel extends PluginPanel
{
    private static final int ROW_HEIGHT = 30;

    private final Client client;
    private final NightLegionApi api;
    private final ItemManager itemManager;
    private final Map<Integer, ImageIcon> itemIcons = new HashMap<>();

    private final JComboBox<String> section = new JComboBox<>(new String[]{
        "BOTW", "SOTW", "GIVEAWAY", "GROUP FINDER"
    });
    private final JComboBox<String> activityFilter = new JComboBox<>();
    private final JLabel connection = new JLabel("● Not connected");
    private final ViewportPanel body = new ViewportPanel();

    private JsonObject latest;
    private List<String> activities = new ArrayList<>();
    private boolean syncingActivities;

    NightLegionPanel(Client client, NightLegionApi api, ItemManager itemManager)
    {
        this.client = client;
        this.api = api;
        this.itemManager = itemManager;

        setLayout(new BorderLayout());
        setBackground(NightLegionTheme.BACKGROUND);

        section.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        section.setAlignmentX(Component.LEFT_ALIGNMENT);
        NightLegionTheme.styleCombo(section);
        configureSectionCombo();

        activityFilter.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        activityFilter.setAlignmentX(Component.LEFT_ALIGNMENT);
        NightLegionTheme.styleCombo(activityFilter);
        configureActivityCombo(activityFilter);

        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(NightLegionTheme.BACKGROUND);
        body.setBorder(BorderFactory.createEmptyBorder(7, 7, 7, 7));

        JScrollPane scroll = new JScrollPane(body);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getViewport().setBackground(NightLegionTheme.BACKGROUND);

        add(buildHeader(), BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);

        section.addActionListener(e -> render());
        activityFilter.addActionListener(e ->
        {
            if (!syncingActivities && "GROUP FINDER".equals(String.valueOf(section.getSelectedItem())))
            {
                render();
            }
        });

        refresh();
    }

    private JPanel buildHeader()
    {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(NightLegionTheme.HEADER);
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 2, 0, NightLegionTheme.PURPLE),
            BorderFactory.createEmptyBorder(8, 9, 7, 9)));

        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 7, 0));
        brand.setBackground(NightLegionTheme.HEADER);
        brand.setAlignmentX(Component.LEFT_ALIGNMENT);
        brand.setMaximumSize(new Dimension(Integer.MAX_VALUE, 37));
        brand.add(new JLabel(NightLegionTheme.markIcon(25, NightLegionTheme.PURPLE_BRIGHT)));

        JPanel names = new JPanel();
        names.setLayout(new BoxLayout(names, BoxLayout.Y_AXIS));
        names.setBackground(NightLegionTheme.HEADER);
        JLabel title = new JLabel("NightLegion");
        title.setForeground(NightLegionTheme.PURPLE_BRIGHT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        JLabel subtitle = new JLabel("Own the night");
        subtitle.setForeground(NightLegionTheme.MUTED);
        subtitle.setFont(subtitle.getFont().deriveFont(9.5f));
        names.add(title);
        names.add(subtitle);
        brand.add(names);

        connection.setForeground(NightLegionTheme.MUTED);
        connection.setFont(connection.getFont().deriveFont(Font.BOLD, 10f));
        connection.setAlignmentX(Component.LEFT_ALIGNMENT);

        header.add(brand);
        header.add(Box.createVerticalStrut(5));
        header.add(section);
        header.add(Box.createVerticalStrut(5));
        header.add(connection);
        return header;
    }

    void refresh()
    {
        setStatus("● Refreshing...");
        api.action("overview", rsn(), new JsonObject(), json ->
            SwingUtilities.invokeLater(() ->
            {
                latest = json;
                String playerRsn = rsn();
                connection.setForeground(NightLegionTheme.PURPLE_BRIGHT);
                connection.setText(playerRsn.isEmpty() ? "● Connected" : "● Connected as " + playerRsn);

                activities = new ArrayList<>();
                if (json.has("activities") && json.get("activities").isJsonArray())
                {
                    for (JsonElement e : json.getAsJsonArray("activities"))
                    {
                        if (!e.isJsonNull() && !e.getAsString().trim().isEmpty())
                        {
                            activities.add(e.getAsString());
                        }
                    }
                }
                syncActivityFilter();
                render();
            }), this::showError);
    }

    private void syncActivityFilter()
    {
        String previous = activityFilter.getSelectedItem() == null ? null : String.valueOf(activityFilter.getSelectedItem());
        syncingActivities = true;
        activityFilter.removeAllItems();

        String[] preferred = new String[]{
            "Chambers of Xeric", "Theatre of Blood", "Tombs of Amascut", "Nex",
            "Corporeal Beast", "Barbarian Assault", "Soul Wars", "Pest Control"
        };
        for (String choice : preferred)
        {
            addActivityIfPresent(choice);
        }
        for (String choice : activities)
        {
            if (!containsActivity(choice))
            {
                activityFilter.addItem(choice);
            }
        }

        if (activityFilter.getItemCount() == 0)
        {
            activityFilter.addItem("Chambers of Xeric");
        }

        if (previous != null && containsActivity(previous))
        {
            selectActivity(previous);
        }
        else if (containsActivity("Chambers of Xeric"))
        {
            selectActivity("Chambers of Xeric");
        }
        syncingActivities = false;
    }

    private void addActivityIfPresent(String wanted)
    {
        for (String activity : activities)
        {
            if (wanted.equalsIgnoreCase(activity))
            {
                activityFilter.addItem(activity);
                return;
            }
        }
    }

    private boolean containsActivity(String wanted)
    {
        for (int i = 0; i < activityFilter.getItemCount(); i++)
        {
            if (wanted.equalsIgnoreCase(activityFilter.getItemAt(i)))
            {
                return true;
            }
        }
        return false;
    }

    private void selectActivity(String wanted)
    {
        for (int i = 0; i < activityFilter.getItemCount(); i++)
        {
            if (wanted.equalsIgnoreCase(activityFilter.getItemAt(i)))
            {
                activityFilter.setSelectedIndex(i);
                return;
            }
        }
    }

    private void render()
    {
        body.removeAll();
        if (latest == null)
        {
            addCenteredEmptyState("Waiting for NightLegion...", "Connecting to your linked account.", true);
            repaintBody();
            return;
        }

        String selected = String.valueOf(section.getSelectedItem());
        if ("BOTW".equals(selected))
        {
            renderEvent("botw", "JOIN BOTW", "Boss of the Week");
        }
        else if ("SOTW".equals(selected))
        {
            renderEvent("sotw", "JOIN SOTW", "Skill of the Week");
        }
        else if ("GIVEAWAY".equals(selected))
        {
            renderGiveaway();
        }
        else
        {
            renderGroups();
        }
        repaintBody();
    }

    private void renderEvent(String key, String joinText, String subtitle)
    {
        if (!latest.has(key) || latest.get(key).isJsonNull())
        {
            addCenteredEmptyState("No active " + subtitle.toLowerCase(), "NightLegion will show it here when one starts.", true);
            return;
        }

        JsonObject event = latest.getAsJsonObject(key);
        String label = safeString(event, "label", key.toUpperCase());
        boolean entered = event.has("entered") && event.get("entered").getAsBoolean();
        boolean pending = event.has("pending_buyin") && event.get("pending_buyin").getAsBoolean();

        JPanel hero = heroCard();
        hero.add(eventTitle(label, subtitle, key));
        hero.add(Box.createVerticalStrut(6));

        JPanel stats = new JPanel(new GridLayout(2, 2, 5, 5));
        stats.setOpaque(false);
        stats.setAlignmentX(Component.LEFT_ALIGNMENT);
        stats.setMaximumSize(new Dimension(Integer.MAX_VALUE, 76));
        stats.add(statTile("ENTRY", formatGp(safeLong(event, "entry_fee_gp", 0))));
        stats.add(statTile("ENDS", formatTime(safeLong(event, "end_time", 0))));
        stats.add(statTile("PLAYERS", String.valueOf(safeInt(event, "participants", 0))));
        stats.add(statTile("STATUS", entered ? "ENTERED" : pending ? "BUY-IN" : "NOT ENTERED"));
        hero.add(stats);

        if (event.has("personal") && event.get("personal").isJsonObject())
        {
            hero.add(Box.createVerticalStrut(6));
            hero.add(personalStrip(event.getAsJsonObject("personal")));
        }

        body.add(hero);
        body.add(Box.createVerticalStrut(6));

        if (event.has("prizes") && event.get("prizes").isJsonArray() && event.getAsJsonArray("prizes").size() > 0)
        {
            body.add(compactLines("PRIZES", event.getAsJsonArray("prizes"), 3));
            body.add(Box.createVerticalStrut(6));
        }

        if (event.has("leaderboard") && event.get("leaderboard").isJsonArray() && event.getAsJsonArray("leaderboard").size() > 0)
        {
            body.add(leaderboardPreview(event));
            body.add(Box.createVerticalStrut(6));
        }

        JButton refresh = new JButton("Refresh");
        JButton join = new JButton(entered ? "✓ Entered" : pending ? "Buy-in Pending" : joinText);
        JButton progress = new JButton("Progress");
        JButton leaderboard = new JButton("Leaderboard");
        NightLegionTheme.styleButton(refresh, false, false);
        NightLegionTheme.styleButton(join, true, false);
        NightLegionTheme.styleButton(progress, false, false);
        NightLegionTheme.styleButton(leaderboard, false, false);
        join.setEnabled(!entered && !pending);
        progress.setEnabled(event.has("personal") && event.get("personal").isJsonObject());
        leaderboard.setEnabled(event.has("leaderboard") && event.get("leaderboard").isJsonArray());

        refresh.addActionListener(e -> refresh());
        join.addActionListener(e ->
        {
            join.setEnabled(false);
            setStatus("● Sending...");
            api.action("botw".equals(key) ? "join_botw" : "join_sotw", rsn(), new JsonObject(), result ->
                SwingUtilities.invokeLater(() ->
                {
                    showMessage(result);
                    refresh();
                }), this::showError);
        });
        progress.addActionListener(e -> showProgress(event, subtitle));
        leaderboard.addActionListener(e -> showLeaderboard(event, subtitle));

        body.add(buttonGrid(refresh, join, progress, leaderboard));
    }

    private JPanel eventTitle(String label, String subtitle, String key)
    {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        ImageIcon icon = activityIcon(label);
        if (icon == null)
        {
            icon = itemIcon(NightLegionTheme.sectionItemId(key.toUpperCase()));
        }
        if (icon != null)
        {
            JLabel iconLabel = new JLabel(icon);
            iconLabel.setVerticalAlignment(SwingConstants.TOP);
            row.add(iconLabel, BorderLayout.WEST);
        }

        JPanel text = new JPanel();
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.setOpaque(false);
        JLabel heading = new JLabel(shorten(label, 28));
        heading.setForeground(NightLegionTheme.PURPLE_BRIGHT);
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 15f));
        JLabel sub = new JLabel(subtitle);
        sub.setForeground(NightLegionTheme.MUTED);
        sub.setFont(sub.getFont().deriveFont(10f));
        text.add(heading);
        text.add(sub);
        row.add(text, BorderLayout.CENTER);
        return row;
    }

    private JPanel personalStrip(JsonObject p)
    {
        JPanel strip = new JPanel(new BorderLayout());
        strip.setBackground(NightLegionTheme.SURFACE_ALT);
        strip.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(NightLegionTheme.PURPLE.darker()),
            BorderFactory.createEmptyBorder(4, 6, 4, 6)));
        strip.setAlignmentX(Component.LEFT_ALIGNMENT);
        strip.setMaximumSize(new Dimension(Integer.MAX_VALUE, 29));
        JLabel mine = new JLabel(shorten(personalSummary(p), 38));
        mine.setForeground(NightLegionTheme.SILVER);
        mine.setFont(mine.getFont().deriveFont(Font.BOLD, 10f));
        strip.add(mine, BorderLayout.CENTER);
        return strip;
    }

    private JPanel leaderboardPreview(JsonObject event)
    {
        JPanel panel = card();
        JLabel title = sectionHeading("TOP 3");
        panel.add(title);
        panel.add(Box.createVerticalStrut(3));

        JsonArray rows = event.getAsJsonArray("leaderboard");
        int limit = Math.min(3, rows.size());
        for (int i = 0; i < limit; i++)
        {
            JsonObject row = rows.get(i).getAsJsonObject();
            String rank = row.has("rank") ? "#" + row.get("rank").getAsInt() : "#" + (i + 1);
            String player = shorten(safeString(row, "rsn", "Unknown"), 16);
            String gain = formatGain(safeLong(row, "gain", 0), safeString(event, "unit", ""));

            JPanel line = new JPanel(new BorderLayout(5, 0));
            line.setOpaque(false);
            line.setAlignmentX(Component.LEFT_ALIGNMENT);
            line.setMaximumSize(new Dimension(Integer.MAX_VALUE, 19));
            JLabel left = new JLabel(rank + "  " + player);
            JLabel right = new JLabel(gain);
            boolean you = row.has("is_you") && row.get("is_you").getAsBoolean();
            left.setForeground(you ? NightLegionTheme.PURPLE_BRIGHT : NightLegionTheme.SILVER);
            right.setForeground(you ? NightLegionTheme.PURPLE_BRIGHT : NightLegionTheme.MUTED);
            line.add(left, BorderLayout.CENTER);
            line.add(right, BorderLayout.EAST);
            panel.add(line);
        }
        return panel;
    }

    private void showProgress(JsonObject event, String title)
    {
        if (!event.has("personal") || !event.get("personal").isJsonObject())
        {
            JOptionPane.showMessageDialog(this, "You are not entered in this event.", "NightLegion", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JsonObject p = event.getAsJsonObject("personal");
        JPanel panel = dialogPanel();
        addDialogLine(panel, "RSN", safeString(p, "rsn", rsn()));
        addDialogLine(panel, "Rank", p.has("rank") && !p.get("rank").isJsonNull() ? "#" + p.get("rank").getAsInt() : "Unranked");
        addDialogLine(panel, "Gained", formatGain(safeLong(p, "gain", 0), safeString(p, "unit", safeString(event, "unit", ""))));
        addDialogLine(panel, "Starting", formatRawStat(p, "starting", safeString(event, "unit", "")));
        addDialogLine(panel, "Current", formatRawStat(p, "current", safeString(event, "unit", "")));
        addDialogLine(panel, "Gap to next", formatGain(safeLong(p, "gap_to_next", 0), safeString(event, "unit", "")));
        if (p.has("xp_per_hour") && !p.get("xp_per_hour").isJsonNull())
        {
            addDialogLine(panel, "XP / hour", formatNumber(p.get("xp_per_hour").getAsLong()));
        }
        JOptionPane.showMessageDialog(this, panel, title + " — My Progress", JOptionPane.PLAIN_MESSAGE);
    }

    private void showLeaderboard(JsonObject event, String title)
    {
        if (!event.has("leaderboard") || !event.get("leaderboard").isJsonArray())
        {
            JOptionPane.showMessageDialog(this, "Leaderboard data is not available yet.", "NightLegion", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JPanel panel = dialogPanel();
        JsonArray rows = event.getAsJsonArray("leaderboard");
        if (rows.size() == 0)
        {
            addDialogLine(panel, "", "No ranked players yet.");
        }
        else
        {
            for (JsonElement element : rows)
            {
                JsonObject row = element.getAsJsonObject();
                String left = "#" + safeInt(row, "rank", 0) + "  " + safeString(row, "rsn", "Unknown");
                String right = formatGain(safeLong(row, "gain", 0), safeString(event, "unit", ""));
                addDialogLine(panel, left, right);
            }
        }
        JScrollPane scroll = new JScrollPane(panel);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        JOptionPane.showMessageDialog(this, scroll, title + " — Leaderboard", JOptionPane.PLAIN_MESSAGE);
    }

    private String personalSummary(JsonObject p)
    {
        String rank = p.has("rank") && !p.get("rank").isJsonNull() ? "#" + p.get("rank").getAsInt() : "Unranked";
        String gain = formatGain(safeLong(p, "gain", 0), safeString(p, "unit", ""));
        String pace = p.has("xp_per_hour") && !p.get("xp_per_hour").isJsonNull()
            ? " • " + formatNumber(p.get("xp_per_hour").getAsLong()) + " XP/h"
            : "";
        return "YOU  " + rank + " • " + gain + pace;
    }

    private void renderGiveaway()
    {
        if (!latest.has("giveaway") || latest.get("giveaway").isJsonNull())
        {
            addCenteredEmptyState("No active giveaway", "You'll get a NightLegion alert when a new giveaway starts.", true);
            return;
        }

        JsonObject g = latest.getAsJsonObject("giveaway");
        boolean entered = g.has("entered") && g.get("entered").getAsBoolean();
        boolean eligible = !g.has("eligible") || g.get("eligible").getAsBoolean();

        JPanel hero = heroCard();
        hero.add(eventTitle(safeString(g, "prize", "Giveaway"), "NightLegion Giveaway", "GIVEAWAY"));
        hero.add(Box.createVerticalStrut(6));

        JPanel stats = new JPanel(new GridLayout(2, 2, 5, 5));
        stats.setOpaque(false);
        stats.setMaximumSize(new Dimension(Integer.MAX_VALUE, 76));
        stats.add(statTile("ENTRIES", String.valueOf(safeInt(g, "entries", 0))));
        stats.add(statTile("ENDS", formatTime(safeLong(g, "end_time", 0))));
        stats.add(statTile("RANK", g.has("required_role_name") && !g.get("required_role_name").isJsonNull()
            ? shorten(g.get("required_role_name").getAsString(), 13) : "None"));
        stats.add(statTile("STATUS", entered ? "ENTERED" : eligible ? "ELIGIBLE" : "LOCKED"));
        hero.add(stats);
        body.add(hero);
        body.add(Box.createVerticalStrut(6));

        JPanel info = card();
        info.add(sectionHeading("HOW IT WORKS"));
        info.add(Box.createVerticalStrut(3));
        info.add(wrappedText("One entry per linked Discord member. Rank requirements are checked by NightLegion.", NightLegionTheme.MUTED, 2));
        body.add(info);
        body.add(Box.createVerticalStrut(6));

        JButton refresh = new JButton("Refresh");
        JButton enter = new JButton(entered ? "✓ Entered" : eligible ? "Enter Giveaway" : "Rank Required");
        JButton details = new JButton("Details");
        JButton status = new JButton("My Status");
        NightLegionTheme.styleButton(refresh, false, false);
        NightLegionTheme.styleButton(enter, true, false);
        NightLegionTheme.styleButton(details, false, false);
        NightLegionTheme.styleButton(status, false, false);
        enter.setEnabled(!entered && eligible);

        refresh.addActionListener(e -> refresh());
        enter.addActionListener(e ->
        {
            JsonObject data = new JsonObject();
            data.addProperty("giveaway_id", safeString(g, "id", ""));
            api.action("enter_giveaway", rsn(), data, result ->
                SwingUtilities.invokeLater(() ->
                {
                    showMessage(result);
                    refresh();
                }), this::showError);
        });
        details.addActionListener(e -> showGiveawayDetails(g));
        status.addActionListener(e -> JOptionPane.showMessageDialog(
            this,
            entered ? "You are entered in this giveaway." : eligible ? "You are eligible but not entered yet." : "Your linked Discord rank does not meet this giveaway's requirement.",
            "NightLegion — Giveaway Status",
            JOptionPane.INFORMATION_MESSAGE));

        body.add(buttonGrid(refresh, enter, details, status));
    }

    private void showGiveawayDetails(JsonObject g)
    {
        JPanel panel = dialogPanel();
        addDialogLine(panel, "Prize", safeString(g, "prize", "Giveaway"));
        addDialogLine(panel, "Entries", String.valueOf(safeInt(g, "entries", 0)));
        addDialogLine(panel, "Ends", formatTime(safeLong(g, "end_time", 0)));
        addDialogLine(panel, "Required rank", g.has("required_role_name") && !g.get("required_role_name").isJsonNull()
            ? g.get("required_role_name").getAsString() : "None");
        JOptionPane.showMessageDialog(this, panel, "NightLegion — Giveaway", JOptionPane.PLAIN_MESSAGE);
    }

    private void renderGroups()
    {
        body.add(activityFilter);
        body.add(Box.createVerticalStrut(6));

        JButton refresh = new JButton("Refresh");
        JButton create = new JButton("Create Listing");
        JButton requests = new JButton("Requests");
        JButton mine = new JButton("My Group");
        NightLegionTheme.styleButton(refresh, false, false);
        NightLegionTheme.styleButton(create, true, false);
        NightLegionTheme.styleButton(requests, false, false);
        NightLegionTheme.styleButton(mine, false, false);
        refresh.addActionListener(e -> refresh());
        create.addActionListener(e -> createListing());
        requests.addActionListener(e -> showRequests());
        mine.addActionListener(e -> showMyGroups());

        body.add(buttonGrid(refresh, create, requests, mine));
        body.add(Box.createVerticalStrut(6));

        List<JsonObject> groups = filteredGroups();
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setOpaque(false);
        statusBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        statusBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
        JLabel status = new JLabel(groups.size() + (groups.size() == 1 ? " open listing" : " open listings"));
        status.setForeground(NightLegionTheme.PURPLE_BRIGHT);
        status.setFont(status.getFont().deriveFont(Font.BOLD, 10f));
        statusBar.add(status, BorderLayout.WEST);
        body.add(statusBar);
        body.add(Box.createVerticalStrut(2));
        body.add(wrappedText("RSNs are client-observed. Listings sync with NightLegion Discord.", NightLegionTheme.MUTED, 2));
        body.add(Box.createVerticalStrut(7));

        if (groups.isEmpty())
        {
            JPanel empty = card();
            empty.setBorder(BorderFactory.createEmptyBorder(20, 8, 20, 8));
            JLabel title = new JLabel("No open listings found", SwingConstants.CENTER);
            title.setForeground(NightLegionTheme.SILVER);
            title.setAlignmentX(Component.CENTER_ALIGNMENT);
            JLabel hint = new JLabel("Create one and your clan can join from RuneLite.", SwingConstants.CENTER);
            hint.setForeground(NightLegionTheme.MUTED);
            hint.setFont(hint.getFont().deriveFont(10f));
            hint.setAlignmentX(Component.CENTER_ALIGNMENT);
            empty.add(title);
            empty.add(Box.createVerticalStrut(4));
            empty.add(hint);
            body.add(empty);
            body.add(Box.createVerticalGlue());
            return;
        }

        for (JsonObject group : groups)
        {
            body.add(buildGroupCard(group));
            body.add(Box.createVerticalStrut(6));
        }
    }

    private List<JsonObject> filteredGroups()
    {
        List<JsonObject> out = new ArrayList<>();
        if (!latest.has("groups") || !latest.get("groups").isJsonArray())
        {
            return out;
        }

        String wanted = activityFilter.getSelectedItem() == null ? "" : String.valueOf(activityFilter.getSelectedItem());
        for (JsonElement element : latest.getAsJsonArray("groups"))
        {
            if (!element.isJsonObject())
            {
                continue;
            }
            JsonObject group = element.getAsJsonObject();
            String activity = safeString(group, "activity", "");
            if (wanted.isEmpty() || wanted.equalsIgnoreCase(activity))
            {
                out.add(group);
            }
        }
        return out;
    }

    private JPanel buildGroupCard(JsonObject group)
    {
        JPanel card = card();
        String activity = safeString(group, "activity", "Group");
        String host = safeString(group, "host_name", "Unknown");

        JPanel headingRow = new JPanel(new BorderLayout(6, 0));
        headingRow.setOpaque(false);
        headingRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        headingRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        ImageIcon icon = activityIcon(activity);
        if (icon != null)
        {
            headingRow.add(new JLabel(icon), BorderLayout.WEST);
        }
        JLabel heading = new JLabel(shorten(activity + " — " + host, 30));
        heading.setForeground(NightLegionTheme.PURPLE_BRIGHT);
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 11f));
        headingRow.add(heading, BorderLayout.CENTER);
        card.add(headingRow);

        int members = group.has("members") && group.get("members").isJsonArray()
            ? group.getAsJsonArray("members").size() : 0;
        String details = members + "/" + safeInt(group, "max_players", 1)
            + " · " + safeString(group, "region", "ANY")
            + " · " + safeString(group, "language", "EN");
        JLabel detailLabel = new JLabel(details);
        detailLabel.setForeground(NightLegionTheme.MUTED);
        detailLabel.setFont(detailLabel.getFont().deriveFont(10f));
        card.add(detailLabel);

        String middle = "Role: " + safeString(group, "role", "ANY");
        String kc = safeString(group, "kc", "");
        if (!kc.isEmpty())
        {
            middle += " · KC: " + kc;
        }
        if (group.has("world") && !group.get("world").isJsonNull())
        {
            middle += " · W" + group.get("world").getAsInt();
        }
        JLabel roleKc = new JLabel(shorten(middle, 38));
        roleKc.setForeground(NightLegionTheme.SILVER);
        roleKc.setFont(roleKc.getFont().deriveFont(10f));
        card.add(roleKc);

        String note = safeString(group, "note", "");
        if (!note.isEmpty())
        {
            card.add(Box.createVerticalStrut(2));
            card.add(wrappedText(note, NightLegionTheme.MUTED, 2));
        }

        boolean joined = group.has("joined") && group.get("joined").getAsBoolean();
        boolean requested = group.has("requested") && group.get("requested").getAsBoolean();
        boolean hostGroup = group.has("is_host") && group.get("is_host").getAsBoolean();
        boolean open = "open".equalsIgnoreCase(safeString(group, "status", "open"));

        JButton join = new JButton(hostGroup ? "YOUR GROUP" : joined ? "✓ JOINED" : requested ? "REQUEST PENDING" : "REQUEST TO JOIN");
        NightLegionTheme.styleButton(join, true, false);
        join.setEnabled(!hostGroup && !joined && !requested && open);
        join.setAlignmentX(Component.LEFT_ALIGNMENT);
        join.setMaximumSize(new Dimension(Integer.MAX_VALUE, ROW_HEIGHT));
        join.addActionListener(e ->
        {
            JsonObject data = new JsonObject();
            data.addProperty("group_id", safeString(group, "id", ""));
            api.action("group_join", rsn(), data, result ->
                SwingUtilities.invokeLater(() ->
                {
                    showMessage(result);
                    refresh();
                }), this::showError);
        });
        card.add(Box.createVerticalStrut(5));
        card.add(join);
        return card;
    }

    private void createListing()
    {
        if (activities.isEmpty())
        {
            showError("Activities are still loading. Press Refresh.");
            return;
        }
        Window owner = SwingUtilities.getWindowAncestor(this);
        String selectedActivity = activityFilter.getSelectedItem() == null
            ? "Chambers of Xeric"
            : String.valueOf(activityFilter.getSelectedItem());
        JsonObject data = GroupListingDialog.show(owner, activities, client.getWorld(), itemManager, selectedActivity);
        if (data == null)
        {
            return;
        }
        setStatus("● Creating listing...");
        api.action("group_create", rsn(), data, result ->
            SwingUtilities.invokeLater(() ->
            {
                showMessage(result);
                refresh();
            }), this::showError);
    }

    private void showRequests()
    {
        api.action("group_requests", rsn(), new JsonObject(), result ->
            SwingUtilities.invokeLater(() ->
            {
                JsonArray rows = result.has("requests") && result.get("requests").isJsonArray()
                    ? result.getAsJsonArray("requests") : new JsonArray();
                if (rows.size() == 0)
                {
                    JOptionPane.showMessageDialog(this, "No pending requests.", "NightLegion", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }

                JPanel list = dialogPanel();
                for (JsonElement element : rows)
                {
                    JsonObject row = element.getAsJsonObject();
                    JPanel line = card();
                    line.add(wrappedText(safeString(row, "display_name", "Player") + " → " + safeString(row, "group_title", "Group"), NightLegionTheme.SILVER, 2));

                    JButton accept = new JButton("Accept");
                    JButton decline = new JButton("Decline");
                    NightLegionTheme.styleButton(accept, true, false);
                    NightLegionTheme.styleButton(decline, false, true);
                    accept.addActionListener(e -> decide(row, true));
                    decline.addActionListener(e -> decide(row, false));
                    JPanel actions = new JPanel(new GridLayout(1, 2, 5, 0));
                    actions.setOpaque(false);
                    actions.setMaximumSize(new Dimension(Integer.MAX_VALUE, ROW_HEIGHT));
                    actions.add(accept);
                    actions.add(decline);
                    line.add(Box.createVerticalStrut(4));
                    line.add(actions);
                    list.add(line);
                    list.add(Box.createVerticalStrut(5));
                }
                JScrollPane scroll = new JScrollPane(list);
                scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
                JOptionPane.showMessageDialog(this, scroll, "Group Requests", JOptionPane.PLAIN_MESSAGE);
            }), this::showError);
    }

    private void decide(JsonObject row, boolean accept)
    {
        JsonObject data = new JsonObject();
        data.addProperty("group_id", safeString(row, "group_id", ""));
        data.addProperty("user_id", row.has("user_id") ? row.get("user_id").getAsLong() : 0L);
        data.addProperty("accept", accept);
        api.action("group_request_decide", rsn(), data, result ->
            SwingUtilities.invokeLater(this::refresh), this::showError);
    }

    private void showMyGroups()
    {
        if (latest == null || !latest.has("groups") || !latest.get("groups").isJsonArray())
        {
            return;
        }
        List<JsonObject> mine = new ArrayList<>();
        for (JsonElement e : latest.getAsJsonArray("groups"))
        {
            JsonObject g = e.getAsJsonObject();
            if (g.has("is_host") && g.get("is_host").getAsBoolean())
            {
                mine.add(g);
            }
        }
        if (mine.isEmpty())
        {
            JOptionPane.showMessageDialog(this, "You have no active groups.", "NightLegion", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JPanel list = dialogPanel();
        for (JsonObject g : mine)
        {
            JPanel row = card();
            JLabel title = new JLabel(shorten(safeString(g, "title", "My Group"), 34));
            title.setForeground(NightLegionTheme.PURPLE_BRIGHT);
            row.add(title);
            JButton close = new JButton("Close Group");
            NightLegionTheme.styleButton(close, false, true);
            close.setMaximumSize(new Dimension(Integer.MAX_VALUE, ROW_HEIGHT));
            close.addActionListener(e ->
            {
                JsonObject data = new JsonObject();
                data.addProperty("group_id", safeString(g, "id", ""));
                api.action("group_close", rsn(), data, result ->
                    SwingUtilities.invokeLater(() ->
                    {
                        showMessage(result);
                        refresh();
                    }), this::showError);
            });
            row.add(Box.createVerticalStrut(4));
            row.add(close);
            list.add(row);
            list.add(Box.createVerticalStrut(5));
        }
        JOptionPane.showMessageDialog(this, list, "My Group", JOptionPane.PLAIN_MESSAGE);
    }

    private JPanel heroCard()
    {
        JPanel card = card();
        card.setBackground(NightLegionTheme.HEADER);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 2, 0, NightLegionTheme.PURPLE),
            BorderFactory.createEmptyBorder(7, 8, 7, 8)));
        return card;
    }

    private JPanel card()
    {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(NightLegionTheme.SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(NightLegionTheme.SURFACE_ALT.brighter()),
            BorderFactory.createEmptyBorder(6, 7, 6, 7)));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 210));
        return card;
    }

    private JPanel statTile(String label, String value)
    {
        JPanel tile = new JPanel();
        tile.setLayout(new BoxLayout(tile, BoxLayout.Y_AXIS));
        tile.setBackground(NightLegionTheme.SURFACE_ALT);
        tile.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        JLabel top = new JLabel(label);
        top.setForeground(NightLegionTheme.MUTED);
        top.setFont(top.getFont().deriveFont(Font.BOLD, 8.5f));
        JLabel bottom = new JLabel(shorten(value, 15));
        bottom.setForeground(NightLegionTheme.SILVER);
        bottom.setFont(bottom.getFont().deriveFont(Font.BOLD, 10f));
        tile.add(top);
        tile.add(bottom);
        return tile;
    }

    private JPanel compactLines(String heading, JsonArray lines, int maxLines)
    {
        JPanel panel = card();
        panel.add(sectionHeading(heading));
        panel.add(Box.createVerticalStrut(3));
        int limit = Math.min(maxLines, lines.size());
        for (int i = 0; i < limit; i++)
        {
            panel.add(wrappedText(lines.get(i).getAsString(), NightLegionTheme.SILVER, 1));
        }
        return panel;
    }

    private JLabel sectionHeading(String text)
    {
        JLabel title = new JLabel(text);
        title.setForeground(NightLegionTheme.PURPLE_BRIGHT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 10f));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        return title;
    }

    private JTextArea wrappedText(String text, Color color, int maxRows)
    {
        JTextArea area = new JTextArea(text == null ? "" : text);
        area.setEditable(false);
        area.setFocusable(false);
        area.setOpaque(false);
        area.setForeground(color);
        area.setFont(area.getFont().deriveFont(10f));
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setRows(Math.max(1, maxRows));
        area.setColumns(1);
        area.setBorder(BorderFactory.createEmptyBorder());
        area.setAlignmentX(Component.LEFT_ALIGNMENT);
        area.setMaximumSize(new Dimension(Integer.MAX_VALUE, Math.max(18, 17 * maxRows)));
        return area;
    }

    private JPanel buttonGrid(JButton a, JButton b, JButton c, JButton d)
    {
        JPanel buttons = new JPanel(new GridLayout(2, 2, 5, 4));
        buttons.setOpaque(false);
        buttons.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttons.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        buttons.add(a);
        buttons.add(b);
        buttons.add(c);
        buttons.add(d);
        return buttons;
    }

    private JPanel dialogPanel()
    {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(NightLegionTheme.BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        return panel;
    }

    private void addDialogLine(JPanel panel, String left, String right)
    {
        JLabel line = new JLabel((left == null || left.isEmpty() ? "" : left + ": ") + right);
        line.setForeground(NightLegionTheme.SILVER);
        line.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
        panel.add(line);
    }

    private void addCenteredEmptyState(String titleText, String hintText, boolean withRefresh)
    {
        body.add(Box.createVerticalGlue());
        JPanel empty = new JPanel();
        empty.setLayout(new BoxLayout(empty, BoxLayout.Y_AXIS));
        empty.setOpaque(false);
        empty.setAlignmentX(Component.LEFT_ALIGNMENT);
        empty.setMaximumSize(new Dimension(Integer.MAX_VALUE, 125));

        JLabel icon = new JLabel(NightLegionTheme.markIcon(28, NightLegionTheme.PURPLE));
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel title = new JLabel(titleText, SwingConstants.CENTER);
        title.setForeground(NightLegionTheme.SILVER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 12f));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel hint = new JLabel("<html><div style='text-align:center'>" + escapeHtml(hintText) + "</div></html>", SwingConstants.CENTER);
        hint.setForeground(NightLegionTheme.MUTED);
        hint.setFont(hint.getFont().deriveFont(10f));
        hint.setAlignmentX(Component.CENTER_ALIGNMENT);

        empty.add(icon);
        empty.add(Box.createVerticalStrut(5));
        empty.add(title);
        empty.add(Box.createVerticalStrut(4));
        empty.add(hint);

        if (withRefresh)
        {
            JButton refresh = new JButton("Refresh");
            NightLegionTheme.styleButton(refresh, false, false);
            refresh.setAlignmentX(Component.CENTER_ALIGNMENT);
            refresh.addActionListener(e -> refresh());
            empty.add(Box.createVerticalStrut(8));
            empty.add(refresh);
        }

        body.add(empty);
        body.add(Box.createVerticalGlue());
    }

    private void configureSectionCombo()
    {
        section.setRenderer(new DefaultListCellRenderer()
        {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus)
            {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focus);
                String text = value == null ? "" : String.valueOf(value);
                label.setText(text);
                label.setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 6));
                label.setBackground(selected ? NightLegionTheme.PURPLE : NightLegionTheme.SURFACE_ALT);
                label.setForeground(Color.WHITE);
                label.setIcon(itemIcon(NightLegionTheme.sectionItemId(text)));
                label.setIconTextGap(7);
                return label;
            }
        });
    }

    private void configureActivityCombo(JComboBox<String> combo)
    {
        combo.setRenderer(new DefaultListCellRenderer()
        {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus)
            {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focus);
                String text = value == null ? "" : String.valueOf(value);
                label.setText(shorten(text, 26));
                label.setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 6));
                label.setBackground(selected ? NightLegionTheme.PURPLE : NightLegionTheme.SURFACE_ALT);
                label.setForeground(Color.WHITE);
                label.setIcon(activityIcon(text));
                label.setIconTextGap(7);
                return label;
            }
        });
    }

    private ImageIcon activityIcon(String activity)
    {
        return itemIcon(NightLegionTheme.activityItemId(activity));
    }

    private ImageIcon itemIcon(int itemId)
    {
        if (itemId <= 0 || itemManager == null)
        {
            return null;
        }
        return itemIcons.computeIfAbsent(itemId, id ->
        {
            AsyncBufferedImage image = itemManager.getImage(id);
            image.onLoaded(() -> SwingUtilities.invokeLater(this::repaint));
            return new ImageIcon(image);
        });
    }

    private String rsn()
    {
        return client.getLocalPlayer() == null || client.getLocalPlayer().getName() == null
            ? ""
            : client.getLocalPlayer().getName().trim();
    }

    private void showMessage(JsonObject result)
    {
        String message = result.has("message") && !result.get("message").isJsonNull()
            ? result.get("message").getAsString()
            : "Done.";
        JOptionPane.showMessageDialog(this, message, "NightLegion", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showError(String error)
    {
        SwingUtilities.invokeLater(() ->
        {
            connection.setForeground(new Color(236, 112, 112));
            connection.setText("● " + shorten(error, 34));
            JOptionPane.showMessageDialog(this, error, "NightLegion", JOptionPane.ERROR_MESSAGE);
        });
    }

    private void setStatus(String text)
    {
        SwingUtilities.invokeLater(() ->
        {
            connection.setForeground(NightLegionTheme.MUTED);
            connection.setText(text);
        });
    }

    private void repaintBody()
    {
        body.revalidate();
        body.repaint();
    }

    private static String formatRawStat(JsonObject object, String key, String unit)
    {
        if (!object.has(key) || object.get(key).isJsonNull())
        {
            return "Pending";
        }
        return formatNumber(object.get(key).getAsLong()) + (unit.isEmpty() ? "" : " " + unit);
    }

    private static String safeString(JsonObject object, String key, String fallback)
    {
        try
        {
            return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : fallback;
        }
        catch (Exception ignored)
        {
            return fallback;
        }
    }

    private static int safeInt(JsonObject object, String key, int fallback)
    {
        try
        {
            return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsInt() : fallback;
        }
        catch (Exception ignored)
        {
            return fallback;
        }
    }

    private static long safeLong(JsonObject object, String key, long fallback)
    {
        try
        {
            return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsLong() : fallback;
        }
        catch (Exception ignored)
        {
            return fallback;
        }
    }

    private static String formatGp(long gp)
    {
        if (gp <= 0)
        {
            return "FREE";
        }
        return formatNumber(gp) + " GP";
    }

    private static String formatGain(long gain, String unit)
    {
        return "+" + formatNumber(gain) + (unit == null || unit.isEmpty() ? "" : " " + unit);
    }

    private static String formatNumber(long value)
    {
        long absolute = Math.abs(value);
        if (absolute >= 1_000_000_000L)
        {
            return trimDecimal(value / 1_000_000_000.0) + "B";
        }
        if (absolute >= 1_000_000L)
        {
            return trimDecimal(value / 1_000_000.0) + "M";
        }
        if (absolute >= 1_000L)
        {
            return trimDecimal(value / 1_000.0) + "K";
        }
        return String.valueOf(value);
    }

    private static String trimDecimal(double value)
    {
        String formatted = String.format("%.1f", value);
        return formatted.endsWith(".0") ? formatted.substring(0, formatted.length() - 2) : formatted;
    }

    private static String formatTime(long unix)
    {
        if (unix <= 0)
        {
            return "Unknown";
        }
        long seconds = Math.max(0, unix - System.currentTimeMillis() / 1000L);
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long mins = (seconds % 3600) / 60;
        if (days > 0)
        {
            return days + "d " + hours + "h";
        }
        if (hours > 0)
        {
            return hours + "h " + mins + "m";
        }
        return mins + "m";
    }

    private static String shorten(String text, int max)
    {
        if (text == null)
        {
            return "";
        }
        String clean = text.replace('\n', ' ').replace('\r', ' ').trim();
        if (clean.length() <= max)
        {
            return clean;
        }
        return clean.substring(0, Math.max(1, max - 1)).trim() + "…";
    }

    private static String escapeHtml(String value)
    {
        if (value == null)
        {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static final class ViewportPanel extends JPanel implements Scrollable
    {
        @Override
        public Dimension getPreferredScrollableViewportSize()
        {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction)
        {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction)
        {
            return Math.max(48, visibleRect.height - 48);
        }

        @Override
        public boolean getScrollableTracksViewportWidth()
        {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight()
        {
            if (getParent() instanceof JViewport)
            {
                return ((JViewport) getParent()).getHeight() > getPreferredSize().height;
            }
            return false;
        }
    }
}
