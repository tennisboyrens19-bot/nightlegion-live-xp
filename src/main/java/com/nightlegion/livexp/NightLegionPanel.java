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
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.AsyncBufferedImage;

class NightLegionPanel extends PluginPanel
{
    private final Client client;
    private final NightLegionApi api;
    private final ItemManager itemManager;
    private final Map<Integer, ImageIcon> activityIcons = new HashMap<>();

    private final JComboBox<String> section = new JComboBox<>(new String[]{
        "BOTW", "SOTW", "GIVEAWAY", "GROUP FINDER"
    });
    private final JComboBox<String> activityFilter = new JComboBox<>();
    private final JLabel connection = new JLabel("● Not connected");
    private final JPanel body = new JPanel();

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

        section.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        section.setAlignmentX(Component.LEFT_ALIGNMENT);
        NightLegionTheme.styleCombo(section);

        activityFilter.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        activityFilter.setAlignmentX(Component.LEFT_ALIGNMENT);
        NightLegionTheme.styleCombo(activityFilter);
        configureActivityCombo(activityFilter);

        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(NightLegionTheme.BACKGROUND);
        body.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JScrollPane scroll = new JScrollPane(body);
        scroll.setBorder(BorderFactory.createEmptyBorder());
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
            BorderFactory.createEmptyBorder(10, 10, 10, 10)));

        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        brand.setBackground(NightLegionTheme.HEADER);
        brand.setAlignmentX(Component.LEFT_ALIGNMENT);
        brand.add(new JLabel(NightLegionTheme.markIcon(28, NightLegionTheme.PURPLE_BRIGHT)));

        JPanel names = new JPanel();
        names.setLayout(new BoxLayout(names, BoxLayout.Y_AXIS));
        names.setBackground(NightLegionTheme.HEADER);
        JLabel title = new JLabel("NightLegion");
        title.setForeground(NightLegionTheme.PURPLE_BRIGHT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        JLabel subtitle = new JLabel("Clan companion");
        subtitle.setForeground(NightLegionTheme.MUTED);
        subtitle.setFont(subtitle.getFont().deriveFont(11f));
        names.add(title);
        names.add(subtitle);
        brand.add(names);

        connection.setForeground(NightLegionTheme.MUTED);
        connection.setFont(connection.getFont().deriveFont(Font.BOLD, 11f));
        connection.setAlignmentX(Component.LEFT_ALIGNMENT);

        header.add(brand);
        header.add(Box.createVerticalStrut(8));
        header.add(section);
        header.add(Box.createVerticalStrut(7));
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
            JLabel waiting = new JLabel("Waiting for NightLegion...");
            waiting.setForeground(NightLegionTheme.MUTED);
            body.add(waiting);
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
            addEmptyState("No active " + subtitle.toLowerCase());
            addRefreshButton();
            return;
        }

        JsonObject event = latest.getAsJsonObject(key);
        JPanel card = card();

        JLabel heading = new JLabel(safeString(event, "label", key.toUpperCase()));
        heading.setForeground(NightLegionTheme.PURPLE_BRIGHT);
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 16f));
        card.add(heading);

        JLabel sub = new JLabel(subtitle);
        sub.setForeground(NightLegionTheme.MUTED);
        card.add(sub);
        card.add(Box.createVerticalStrut(8));

        addCardLine(card, "Entry", formatGp(safeLong(event, "entry_fee_gp", 0)));
        addCardLine(card, "Participants", String.valueOf(safeInt(event, "participants", 0)));
        addCardLine(card, "Ends", formatTime(safeLong(event, "end_time", 0)));

        if (event.has("prizes") && event.get("prizes").isJsonArray())
        {
            JsonArray prizes = event.getAsJsonArray("prizes");
            for (int i = 0; i < prizes.size(); i++)
            {
                addCardLine(card, i == 0 ? "Prizes" : "", prizes.get(i).getAsString());
            }
        }

        boolean entered = event.has("entered") && event.get("entered").getAsBoolean();
        boolean pending = event.has("pending_buyin") && event.get("pending_buyin").getAsBoolean();

        JButton refresh = new JButton("Refresh");
        JButton join = new JButton(entered ? "✓ ENTERED" : pending ? "BUY-IN PENDING" : joinText);
        NightLegionTheme.styleButton(refresh, false, false);
        NightLegionTheme.styleButton(join, true, false);
        join.setEnabled(!entered && !pending);
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

        JPanel buttons = new JPanel(new GridLayout(1, 2, 6, 0));
        buttons.setOpaque(false);
        buttons.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        buttons.add(refresh);
        buttons.add(join);
        card.add(Box.createVerticalStrut(10));
        card.add(buttons);
        body.add(card);
    }

    private void renderGiveaway()
    {
        if (!latest.has("giveaway") || latest.get("giveaway").isJsonNull())
        {
            addEmptyState("No active giveaway");
            addRefreshButton();
            return;
        }

        JsonObject g = latest.getAsJsonObject("giveaway");
        JPanel card = card();
        JLabel heading = new JLabel(safeString(g, "prize", "Giveaway"));
        heading.setForeground(NightLegionTheme.PURPLE_BRIGHT);
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 16f));
        card.add(heading);

        JLabel sub = new JLabel("NightLegion Giveaway");
        sub.setForeground(NightLegionTheme.MUTED);
        card.add(sub);
        card.add(Box.createVerticalStrut(8));

        addCardLine(card, "Entries", String.valueOf(safeInt(g, "entries", 0)));
        addCardLine(card, "Ends", formatTime(safeLong(g, "end_time", 0)));
        addCardLine(card, "Required rank",
            g.has("required_role_name") && !g.get("required_role_name").isJsonNull()
                ? g.get("required_role_name").getAsString()
                : "None");

        boolean entered = g.has("entered") && g.get("entered").getAsBoolean();
        boolean eligible = !g.has("eligible") || g.get("eligible").getAsBoolean();
        JButton refresh = new JButton("Refresh");
        JButton enter = new JButton(entered ? "✓ ENTERED" : eligible ? "ENTER GIVEAWAY" : "RANK REQUIRED");
        NightLegionTheme.styleButton(refresh, false, false);
        NightLegionTheme.styleButton(enter, true, false);
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

        JPanel buttons = new JPanel(new GridLayout(1, 2, 6, 0));
        buttons.setOpaque(false);
        buttons.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        buttons.add(refresh);
        buttons.add(enter);
        card.add(Box.createVerticalStrut(10));
        card.add(buttons);
        body.add(card);
    }

    private void renderGroups()
    {
        activityFilter.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        body.add(activityFilter);
        body.add(Box.createVerticalStrut(8));

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

        JPanel buttons = new JPanel(new GridLayout(2, 2, 6, 4));
        buttons.setOpaque(false);
        buttons.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        buttons.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttons.add(refresh);
        buttons.add(create);
        buttons.add(requests);
        buttons.add(mine);
        body.add(buttons);
        body.add(Box.createVerticalStrut(7));

        List<JsonObject> groups = filteredGroups();
        JLabel status = new JLabel(groups.size() + (groups.size() == 1 ? " open listing" : " open listings"));
        status.setForeground(NightLegionTheme.PURPLE_BRIGHT);
        status.setFont(status.getFont().deriveFont(Font.BOLD, 11f));
        status.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(status);

        JLabel notice = new JLabel("<html><small>RSNs are client-observed. Listings sync with NightLegion Discord.</small></html>");
        notice.setForeground(NightLegionTheme.MUTED);
        notice.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(Box.createVerticalStrut(3));
        body.add(notice);
        body.add(Box.createVerticalStrut(10));

        if (groups.isEmpty())
        {
            JLabel empty = new JLabel("No open listings found");
            empty.setForeground(Color.GRAY);
            empty.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            empty.setAlignmentX(Component.LEFT_ALIGNMENT);
            body.add(empty);
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

        JLabel heading = new JLabel(activity + " — " + host);
        heading.setForeground(NightLegionTheme.PURPLE_BRIGHT);
        heading.setFont(heading.getFont().deriveFont(Font.BOLD));
        heading.setIcon(activityIcon(activity));
        heading.setIconTextGap(7);
        card.add(heading);

        int members = group.has("members") && group.get("members").isJsonArray()
            ? group.getAsJsonArray("members").size() : 0;
        String details = members + "/" + safeInt(group, "max_players", 1)
            + " · " + safeString(group, "region", "ANY")
            + " · " + safeString(group, "language", "EN");
        JLabel detailLabel = new JLabel(details);
        detailLabel.setForeground(NightLegionTheme.MUTED);
        card.add(detailLabel);

        if (group.has("world") && !group.get("world").isJsonNull())
        {
            JLabel world = new JLabel("World " + group.get("world").getAsInt());
            world.setForeground(NightLegionTheme.SILVER);
            card.add(world);
        }

        String role = safeString(group, "role", "ANY");
        String kc = safeString(group, "kc", "");
        JLabel roleKc = new JLabel("Role: " + role + (kc.isEmpty() ? "" : " · KC: " + kc));
        roleKc.setForeground(NightLegionTheme.SILVER);
        card.add(roleKc);

        String note = safeString(group, "note", "");
        if (!note.isEmpty())
        {
            JLabel noteLabel = new JLabel(note);
            noteLabel.setForeground(NightLegionTheme.MUTED);
            card.add(noteLabel);
        }

        boolean joined = group.has("joined") && group.get("joined").getAsBoolean();
        boolean requested = group.has("requested") && group.get("requested").getAsBoolean();
        boolean hostGroup = group.has("is_host") && group.get("is_host").getAsBoolean();
        boolean open = "open".equalsIgnoreCase(safeString(group, "status", "open"));

        JButton join = new JButton(hostGroup ? "YOUR GROUP" : joined ? "✓ JOINED" : requested ? "REQUEST PENDING" : "REQUEST TO JOIN");
        NightLegionTheme.styleButton(join, true, false);
        join.setEnabled(!hostGroup && !joined && !requested && open);
        join.setAlignmentX(Component.LEFT_ALIGNMENT);
        join.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
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
        card.add(Box.createVerticalStrut(6));
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

                JPanel list = new JPanel();
                list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
                list.setBackground(NightLegionTheme.BACKGROUND);
                for (JsonElement element : rows)
                {
                    JsonObject row = element.getAsJsonObject();
                    JPanel line = card();
                    JLabel who = new JLabel(safeString(row, "display_name", "Player") + " → " + safeString(row, "group_title", "Group"));
                    who.setForeground(NightLegionTheme.SILVER);
                    line.add(who);

                    JPanel actions = new JPanel(new GridLayout(1, 2, 6, 0));
                    actions.setOpaque(false);
                    JButton accept = new JButton("Accept");
                    JButton decline = new JButton("Decline");
                    NightLegionTheme.styleButton(accept, true, false);
                    NightLegionTheme.styleButton(decline, false, true);
                    accept.addActionListener(e -> decide(row, true));
                    decline.addActionListener(e -> decide(row, false));
                    actions.add(accept);
                    actions.add(decline);
                    line.add(Box.createVerticalStrut(5));
                    line.add(actions);
                    list.add(line);
                    list.add(Box.createVerticalStrut(5));
                }
                JOptionPane.showMessageDialog(this, new JScrollPane(list), "Group Requests", JOptionPane.PLAIN_MESSAGE);
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

        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBackground(NightLegionTheme.BACKGROUND);
        for (JsonObject g : mine)
        {
            JPanel row = card();
            JLabel title = new JLabel(safeString(g, "title", "My Group"));
            title.setForeground(NightLegionTheme.PURPLE_BRIGHT);
            row.add(title);
            JButton close = new JButton("Close Group");
            NightLegionTheme.styleButton(close, false, true);
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
            row.add(Box.createVerticalStrut(5));
            row.add(close);
            list.add(row);
            list.add(Box.createVerticalStrut(5));
        }
        JOptionPane.showMessageDialog(this, list, "My Group", JOptionPane.PLAIN_MESSAGE);
    }

    private JPanel card()
    {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(NightLegionTheme.SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(NightLegionTheme.SURFACE_ALT.brighter()),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 220));
        return card;
    }

    private void addRefreshButton()
    {
        JButton refresh = new JButton("Refresh");
        NightLegionTheme.styleButton(refresh, false, false);
        refresh.setAlignmentX(Component.LEFT_ALIGNMENT);
        refresh.addActionListener(e -> refresh());
        body.add(Box.createVerticalStrut(8));
        body.add(refresh);
    }

    private void addEmptyState(String text)
    {
        JLabel empty = new JLabel(text);
        empty.setForeground(NightLegionTheme.MUTED);
        empty.setBorder(BorderFactory.createEmptyBorder(10, 4, 10, 4));
        empty.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(empty);
    }

    private void addCardLine(JPanel card, String left, String right)
    {
        JLabel line = new JLabel((left == null || left.isEmpty() ? "" : left + ": ") + right);
        line.setForeground(NightLegionTheme.SILVER);
        card.add(line);
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
                label.setText(text);
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
        int itemId = NightLegionTheme.activityItemId(activity);
        if (itemId <= 0 || itemManager == null)
        {
            return null;
        }
        return activityIcons.computeIfAbsent(itemId, id ->
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
            connection.setText("● " + error);
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
        if (gp <= 0) return "FREE";
        if (gp >= 1_000_000)
        {
            return (gp % 1_000_000 == 0 ? String.valueOf(gp / 1_000_000) : String.format("%.1f", gp / 1_000_000.0)) + "M GP";
        }
        if (gp >= 1_000) return (gp / 1_000) + "K GP";
        return gp + " GP";
    }

    private static String formatTime(long unix)
    {
        long seconds = Math.max(0, unix - System.currentTimeMillis() / 1000L);
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long mins = (seconds % 3600) / 60;
        if (days > 0) return days + "d " + hours + "h";
        if (hours > 0) return hours + "h " + mins + "m";
        return mins + "m";
    }
}
