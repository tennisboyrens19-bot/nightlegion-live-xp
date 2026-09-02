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
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import net.runelite.api.Client;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.AsyncBufferedImage;

/**
 * NightLegion Group Finder adapted from the BSD-2-Clause RaidMates behavior/UI
 * at commit b53eedb656310791e481310ed3413eadf7a3960b. Runtime traffic uses only
 * the NightLegion Discord-backed Group Finder API. See THIRD_PARTY_NOTICES.md.
 */
class NightLegionGroupFinderPanel extends PluginPanel
{
    private static final Color BACKGROUND = new Color(14, 22, 18);
    private static final Color HEADER = new Color(10, 38, 27);
    private static final Color SURFACE = new Color(24, 39, 31);
    private static final Color SURFACE_ALT = new Color(30, 50, 39);
    private static final Color BRAND_GREEN = new Color(42, 132, 82);
    private static final Color BRAND_GOLD = new Color(232, 199, 105);
    private static final Color MUTED_TEXT = new Color(172, 188, 178);
    private static final Color DANGER = new Color(145, 58, 58);

    private final Client client;
    private final NightLegionApi api;
    private final ItemManager itemManager;
    private final Map<Integer, ImageIcon> activityIcons = new HashMap<>();
    private final JComboBox<String> filter = new JComboBox<>();
    private final JLabel status = new JLabel("0 open listing(s) · auto-refresh 10s");
    private final JPanel listings = new JPanel();
    private final Timer refreshTimer;
    private final JButton requestsButton = new JButton("Requests");
    private final java.util.Set<String> knownRequestKeys = new java.util.HashSet<>();

    private JsonObject latest;
    private List<String> activities = new ArrayList<>();
    private boolean syncingFilter;

    NightLegionGroupFinderPanel(Client client, NightLegionApi api, ItemManager itemManager)
    {
        super(false);
        this.client = client;
        this.api = api;
        this.itemManager = itemManager;

        setLayout(new BorderLayout());
        setBackground(BACKGROUND);
        add(buildHeader(), BorderLayout.NORTH);

        listings.setLayout(new BoxLayout(listings, BoxLayout.Y_AXIS));
        listings.setBackground(BACKGROUND);
        listings.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JScrollPane scroll = new JScrollPane(listings);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getViewport().setBackground(BACKGROUND);
        add(scroll, BorderLayout.CENTER);

        configureActivityCombo(filter);
        filter.addActionListener(event ->
        {
            if (!syncingFilter)
            {
                renderListings();
            }
        });

        refreshTimer = new Timer(10_000, event -> refresh());
        refreshTimer.setRepeats(true);
        showEmpty();
    }

    private JPanel buildHeader()
    {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(HEADER);
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 2, 0, BRAND_GOLD),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)));

        JLabel title = new JLabel("NightLegion");
        title.setForeground(BRAND_GOLD);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));

        JLabel subtitle = new JLabel("Find your next PvM team");
        subtitle.setForeground(MUTED_TEXT);
        subtitle.setFont(subtitle.getFont().deriveFont(11f));

        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        brand.setBackground(HEADER);
        brand.add(new JLabel(NightLegionTheme.markIcon(22, BRAND_GOLD)));

        JPanel names = new JPanel();
        names.setLayout(new BoxLayout(names, BoxLayout.Y_AXIS));
        names.setBackground(HEADER);
        names.add(title);
        names.add(subtitle);
        brand.add(names);
        brand.setAlignmentX(LEFT_ALIGNMENT);

        filter.setMaximumSize(new Dimension(Integer.MAX_VALUE, filter.getPreferredSize().height));
        filter.setAlignmentX(LEFT_ALIGNMENT);
        filter.setBackground(SURFACE_ALT);
        filter.setForeground(Color.WHITE);

        JButton refresh = new JButton("Refresh");
        styleButton(refresh, false, false);
        refresh.addActionListener(event -> refresh());

        JButton create = new JButton("Create Listing");
        styleButton(create, true, false);
        create.addActionListener(event -> createListing());

        styleButton(requestsButton, false, false);
        requestsButton.addActionListener(event -> showRequests());

        JButton myGroup = new JButton("My Group");
        styleButton(myGroup, false, false);
        myGroup.addActionListener(event -> showMyGroups());

        JPanel buttons = new JPanel(new GridLayout(2, 2, 6, 4));
        buttons.setBackground(HEADER);
        buttons.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        buttons.setAlignmentX(LEFT_ALIGNMENT);
        buttons.add(refresh);
        buttons.add(create);
        buttons.add(requestsButton);
        buttons.add(myGroup);

        status.setForeground(BRAND_GOLD);
        status.setFont(status.getFont().deriveFont(Font.BOLD, 11f));
        status.setAlignmentX(LEFT_ALIGNMENT);

        JLabel identityNotice = new JLabel(
            "<html><small>RSNs are client-observed, not Jagex-verified.</small></html>");
        identityNotice.setForeground(MUTED_TEXT);
        identityNotice.setAlignmentX(LEFT_ALIGNMENT);

        header.add(brand);
        header.add(Box.createVerticalStrut(8));
        header.add(filter);
        header.add(Box.createVerticalStrut(8));
        header.add(buttons);
        header.add(Box.createVerticalStrut(6));
        header.add(status);
        header.add(Box.createVerticalStrut(3));
        header.add(identityNotice);
        return header;
    }

    void refresh()
    {
        if (!refreshTimer.isRunning())
        {
            refreshTimer.start();
        }

        status.setText("Refreshing...");
        api.action("overview", rsn(), new JsonObject(), result -> SwingUtilities.invokeLater(() ->
        {
            latest = result;
            activities = new ArrayList<>();
            if (result.has("activities") && result.get("activities").isJsonArray())
            {
                for (JsonElement element : result.getAsJsonArray("activities"))
                {
                    if (!element.isJsonNull())
                    {
                        String value = element.getAsString().trim();
                        if (!value.isEmpty())
                        {
                            activities.add(value);
                        }
                    }
                }
            }
            rebuildFilter();
            renderListings();
            refreshRequestCount();
        }), error -> SwingUtilities.invokeLater(() -> status.setText(error)));
    }

    private void rebuildFilter()
    {
        String previous = filter.getSelectedItem() == null ? "All activities" : String.valueOf(filter.getSelectedItem());
        syncingFilter = true;
        filter.removeAllItems();
        filter.addItem("All activities");
        for (String activity : activities)
        {
            filter.addItem(activity);
        }
        filter.setSelectedItem(containsItem(previous) ? previous : "All activities");
        syncingFilter = false;
    }

    private boolean containsItem(String value)
    {
        for (int i = 0; i < filter.getItemCount(); i++)
        {
            if (value.equalsIgnoreCase(filter.getItemAt(i)))
            {
                return true;
            }
        }
        return false;
    }

    private void renderListings()
    {
        listings.removeAll();
        List<JsonObject> rows = filteredGroups();
        status.setText(rows.size() + " open listing(s) · auto-refresh 10s");

        if (rows.isEmpty())
        {
            showEmpty();
            return;
        }

        for (JsonObject row : rows)
        {
            listings.add(buildCard(row));
            listings.add(Box.createVerticalStrut(6));
        }
        listings.revalidate();
        listings.repaint();
    }

    private void showEmpty()
    {
        listings.removeAll();
        JLabel empty = new JLabel("No open listings found");
        empty.setForeground(Color.GRAY);
        empty.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        listings.add(empty);
        listings.revalidate();
        listings.repaint();
    }

    private List<JsonObject> filteredGroups()
    {
        String wanted = filter.getSelectedItem() == null ? "All activities" : String.valueOf(filter.getSelectedItem());
        return filterGroups(latest, wanted);
    }

    static List<JsonObject> filterGroups(JsonObject overview, String wantedActivity)
    {
        List<JsonObject> out = new ArrayList<>();
        if (overview == null || !overview.has("groups") || !overview.get("groups").isJsonArray())
        {
            return out;
        }
        String wanted = wantedActivity == null ? "All activities" : wantedActivity;
        for (JsonElement element : overview.getAsJsonArray("groups"))
        {
            if (!element.isJsonObject())
            {
                continue;
            }
            JsonObject group = element.getAsJsonObject();
            if (!"open".equalsIgnoreCase(text(group, "status", "open")))
            {
                continue;
            }
            String activity = text(group, "activity", "");
            if ("All activities".equalsIgnoreCase(wanted) || wanted.equalsIgnoreCase(activity))
            {
                out.add(group);
            }
        }
        return out;
    }

    private JPanel buildCard(JsonObject group)
    {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        styleCard(card);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));

        String activity = text(group, "activity", "Group");
        String host = text(group, "host_name", "Unknown");

        JLabel heading = new JLabel(activity + " — " + host);
        addActivityIcon(heading, activity);
        heading.setForeground(BRAND_GOLD);
        heading.setFont(heading.getFont().deriveFont(Font.BOLD));

        int current = group.has("members") && group.get("members").isJsonArray()
            ? group.getAsJsonArray("members").size() : 0;
        int max = integer(group, "max_players", 1);
        JLabel details = new JLabel(current + "/" + max
            + " · " + text(group, "region", "ANY")
            + " · " + text(group, "language", "EN"));
        details.setForeground(MUTED_TEXT);

        card.add(heading);
        card.add(details);

        String note = text(group, "note", "");
        if (!note.isEmpty())
        {
            JLabel noteLabel = new JLabel(note);
            noteLabel.setForeground(MUTED_TEXT.darker());
            card.add(noteLabel);
        }

        boolean joined = bool(group, "joined");
        boolean requested = bool(group, "requested");
        boolean isHost = bool(group, "is_host");

        JButton join = new JButton(isHost ? "Your group" : joined ? "Joined" : requested ? "Request pending" : "Request to join");
        styleButton(join, true, false);
        join.setEnabled(!isHost && !joined && !requested);
        join.addActionListener(event -> requestJoin(group));
        card.add(Box.createVerticalStrut(5));
        card.add(join);
        return card;
    }

    private void createListing()
    {
        if (activities.isEmpty())
        {
            status.setText("Activities are still loading");
            return;
        }
        Window owner = SwingUtilities.getWindowAncestor(this);
        String selected = filter.getSelectedItem() == null || "All activities".equals(filter.getSelectedItem())
            ? "Chambers of Xeric"
            : String.valueOf(filter.getSelectedItem());
        JsonObject data = GroupListingDialog.show(owner, activities, client.getWorld(), itemManager, selected);
        if (data == null)
        {
            return;
        }

        status.setText("Creating listing...");
        api.action("group_create", rsn(), data, result -> SwingUtilities.invokeLater(this::refresh),
            error -> SwingUtilities.invokeLater(() -> status.setText(error)));
    }

    private void requestJoin(JsonObject group)
    {
        JComboBox<String> role = new JComboBox<>(new String[]{"ANY", "DPS", "Tank", "Support", "Learner"});
        JTextField experience = new JTextField();
        JTextField message = new JTextField();
        JPanel form = new JPanel(new GridLayout(0, 1, 3, 3));
        form.add(new JLabel("Preferred role"));
        form.add(role);
        form.add(new JLabel("Experience / kill count (optional)"));
        form.add(experience);
        form.add(new JLabel("Message to host (optional)"));
        form.add(message);
        int choice = JOptionPane.showConfirmDialog(this, form, "Request to Join",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION)
        {
            return;
        }
        JsonObject data = new JsonObject();
        data.addProperty("group_id", text(group, "id", ""));
        data.addProperty("role", String.valueOf(role.getSelectedItem()));
        try
        {
            data.addProperty("experience_kc", Math.max(0, Integer.parseInt(experience.getText().trim())));
        }
        catch (NumberFormatException ignored)
        {
            data.addProperty("experience_kc", 0);
        }
        data.addProperty("message", message.getText().trim());
        status.setText("Sending request...");
        api.action("group_join", rsn(), data, result -> SwingUtilities.invokeLater(() ->
        {
            String responseMessage = text(result, "message", "Request sent.");
            JOptionPane.showMessageDialog(this, responseMessage, "NightLegion", JOptionPane.INFORMATION_MESSAGE);
            refresh();
        }), error -> SwingUtilities.invokeLater(() -> status.setText(error)));
    }

    private void showRequests()
    {
        status.setText("Loading requests...");
        api.action("group_requests", rsn(), new JsonObject(), result -> SwingUtilities.invokeLater(() ->
        {
            JsonArray rows = result.has("requests") && result.get("requests").isJsonArray()
                ? result.getAsJsonArray("requests") : new JsonArray();
            if (rows.size() == 0)
            {
                JOptionPane.showMessageDialog(this, "No pending requests.", "NightLegion", JOptionPane.INFORMATION_MESSAGE);
                refresh();
                return;
            }

            JPanel list = new JPanel();
            list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
            for (JsonElement element : rows)
            {
                JsonObject row = element.getAsJsonObject();
                JPanel card = new JPanel();
                card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
                styleCard(card);
                JLabel title = new JLabel(text(row, "display_name", "Player") + " — "
                    + text(row, "group_title", "Group"));
                title.setForeground(BRAND_GOLD);
                card.add(title);
                JLabel details = new JLabel(text(row, "requester_rsn", "Player") + " · "
                    + text(row, "role", "ANY") + " · " + integer(row, "experience_kc", 0) + " KC");
                details.setForeground(MUTED_TEXT);
                card.add(details);
                String requestMessage = text(row, "message", "");
                if (!requestMessage.isEmpty())
                {
                    JLabel note = new JLabel(requestMessage);
                    note.setForeground(MUTED_TEXT);
                    card.add(note);
                }

                JButton accept = new JButton("Accept");
                JButton reject = new JButton("Reject");
                styleButton(accept, true, false);
                styleButton(reject, false, true);
                accept.addActionListener(e -> decide(row, true));
                reject.addActionListener(e -> decide(row, false));

                JPanel actions = new JPanel(new GridLayout(1, 2, 5, 0));
                actions.setBackground(SURFACE);
                actions.add(accept);
                actions.add(reject);
                card.add(Box.createVerticalStrut(5));
                card.add(actions);
                list.add(card);
                list.add(Box.createVerticalStrut(6));
            }
            JOptionPane.showMessageDialog(this, new JScrollPane(list), "Group Requests", JOptionPane.PLAIN_MESSAGE);
            refresh();
        }), error -> SwingUtilities.invokeLater(() -> status.setText(error)));
    }

    private void refreshRequestCount()
    {
        api.action("group_requests", rsn(), new JsonObject(), result -> SwingUtilities.invokeLater(() ->
        {
            JsonArray rows = result.has("requests") && result.get("requests").isJsonArray()
                ? result.getAsJsonArray("requests") : new JsonArray();
            java.util.Set<String> current = new java.util.HashSet<>();
            for (JsonElement element : rows)
            {
                if (!element.isJsonObject()) continue;
                JsonObject row = element.getAsJsonObject();
                current.add(text(row, "group_id", "") + ":" + longValue(row, "user_id", 0L));
            }
            if (!knownRequestKeys.isEmpty())
            {
                java.util.Set<String> added = new java.util.HashSet<>(current);
                added.removeAll(knownRequestKeys);
                if (!added.isEmpty())
                {
                    status.setText(added.size() + " new join request(s)");
                }
            }
            knownRequestKeys.clear();
            knownRequestKeys.addAll(current);
            requestsButton.setText(rows.size() == 0 ? "Requests" : "Requests (" + rows.size() + ")");
        }), ignored -> { });
    }

    private void decide(JsonObject row, boolean accept)
    {
        JsonObject data = new JsonObject();
        data.addProperty("group_id", text(row, "group_id", ""));
        data.addProperty("user_id", longValue(row, "user_id", 0L));
        data.addProperty("accept", accept);
        api.action("group_request_decide", rsn(), data, result -> SwingUtilities.invokeLater(this::refresh),
            error -> SwingUtilities.invokeLater(() -> status.setText(error)));
    }

    private void showMyGroups()
    {
        status.setText("Loading your group...");
        api.action("group_my", rsn(), new JsonObject(), result -> SwingUtilities.invokeLater(() ->
        {
            if (!result.has("group") || result.get("group").isJsonNull()
                || !result.get("group").isJsonObject())
            {
                JOptionPane.showMessageDialog(this, "You have no active group.", "NightLegion",
                    JOptionPane.INFORMATION_MESSAGE);
                refresh();
                return;
            }
            showGroupLobby(result.getAsJsonObject("group"));
        }), error -> SwingUtilities.invokeLater(() -> status.setText(error)));
    }

    private void showGroupLobby(JsonObject group)
    {
        JPanel lobby = new JPanel();
        lobby.setLayout(new BoxLayout(lobby, BoxLayout.Y_AXIS));

        JLabel title = new JLabel(text(group, "title", text(group, "activity", "My Group")));
        title.setFont(title.getFont().deriveFont(Font.BOLD, 15f));
        title.setForeground(BRAND_GOLD);
        lobby.add(title);
        lobby.add(new JLabel(text(group, "activity", "Group") + " · World "
            + text(group, "world", "Any") + " · expires " + expiryLabel(longValue(group, "expires_at", 0L))));
        lobby.add(Box.createVerticalStrut(7));

        JLabel membersTitle = new JLabel("Members");
        membersTitle.setFont(membersTitle.getFont().deriveFont(Font.BOLD));
        lobby.add(membersTitle);
        JsonArray members = group.has("members") && group.get("members").isJsonArray()
            ? group.getAsJsonArray("members") : new JsonArray();
        for (JsonElement element : members)
        {
            if (!element.isJsonObject()) continue;
            JsonObject member = element.getAsJsonObject();
            String ready = bool(member, "ready") ? "Ready" : "Not ready";
            JLabel row = new JLabel((bool(member, "is_host") ? "★ " : "• ")
                + text(member, "rsn", "Player") + " · " + text(member, "role", "ANY") + " · " + ready);
            row.setForeground(bool(member, "ready") ? BRAND_GREEN.brighter() : MUTED_TEXT);
            lobby.add(row);
        }

        lobby.add(Box.createVerticalStrut(8));
        JButton ready = new JButton(bool(group, "viewer_ready") ? "Mark Not Ready" : "Mark Ready");
        styleButton(ready, true, false);
        ready.addActionListener(event -> setReady(!bool(group, "viewer_ready")));
        JButton chat = new JButton("Lobby Chat");
        styleButton(chat, false, false);
        chat.addActionListener(event -> showLobbyChat());
        JButton exit = new JButton(bool(group, "is_host") ? "Close Group" : "Leave Group");
        styleButton(exit, false, true);
        exit.addActionListener(event ->
        {
            if (bool(group, "is_host")) closeGroup(group);
            else leaveGroup(group);
        });
        JPanel actions = new JPanel(new GridLayout(1, 3, 4, 0));
        actions.add(ready);
        actions.add(chat);
        actions.add(exit);
        lobby.add(actions);
        JOptionPane.showMessageDialog(this, lobby, "My Group", JOptionPane.PLAIN_MESSAGE);
        refresh();
    }

    private void setReady(boolean ready)
    {
        JsonObject data = new JsonObject();
        data.addProperty("ready", ready);
        status.setText(ready ? "Marking you ready..." : "Updating ready status...");
        api.action("group_ready", rsn(), data, result -> SwingUtilities.invokeLater(this::showMyGroups),
            error -> SwingUtilities.invokeLater(() -> status.setText(error)));
    }

    private void leaveGroup(JsonObject group)
    {
        JsonObject data = new JsonObject();
        data.addProperty("group_id", text(group, "id", ""));
        api.action("group_leave", rsn(), data, result -> SwingUtilities.invokeLater(this::refresh),
            error -> SwingUtilities.invokeLater(() -> status.setText(error)));
    }

    private void showLobbyChat()
    {
        status.setText("Loading lobby chat...");
        api.action("group_chat_get", rsn(), new JsonObject(), result -> SwingUtilities.invokeLater(() ->
        {
            JsonArray messages = result.has("messages") && result.get("messages").isJsonArray()
                ? result.getAsJsonArray("messages") : new JsonArray();
            JTextArea history = new JTextArea(12, 28);
            history.setEditable(false);
            history.setLineWrap(true);
            history.setWrapStyleWord(true);
            StringBuilder text = new StringBuilder();
            for (JsonElement element : messages)
            {
                if (!element.isJsonObject()) continue;
                JsonObject row = element.getAsJsonObject();
                text.append(text(row, "sender_rsn", "Player")).append(": ")
                    .append(text(row, "body", "")).append('\n');
            }
            history.setText(text.toString());
            history.setCaretPosition(history.getDocument().getLength());
            JTextField message = new JTextField();
            JPanel chat = new JPanel(new BorderLayout(4, 4));
            chat.add(new JScrollPane(history), BorderLayout.CENTER);
            chat.add(message, BorderLayout.SOUTH);
            int choice = JOptionPane.showConfirmDialog(this, chat, "NightLegion Lobby Chat",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (choice == JOptionPane.OK_OPTION && !message.getText().trim().isEmpty())
            {
                sendLobbyChat(message.getText().trim());
            }
        }), error -> SwingUtilities.invokeLater(() -> status.setText(error)));
    }

    private void sendLobbyChat(String message)
    {
        JsonObject data = new JsonObject();
        data.addProperty("message", message);
        api.action("group_chat_send", rsn(), data, result -> SwingUtilities.invokeLater(this::showLobbyChat),
            error -> SwingUtilities.invokeLater(() -> status.setText(error)));
    }

    private void closeGroup(JsonObject group)
    {
        JsonObject data = new JsonObject();
        data.addProperty("group_id", text(group, "id", ""));
        api.action("group_close", rsn(), data, result -> SwingUtilities.invokeLater(this::refresh),
            error -> SwingUtilities.invokeLater(() -> status.setText(error)));
    }

    private static void styleCard(JPanel card)
    {
        card.setBackground(SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(48, 82, 63)),
            BorderFactory.createEmptyBorder(9, 9, 9, 9)));
    }

    private static void styleButton(JButton button, boolean primary, boolean danger)
    {
        Color background = danger ? DANGER : (primary ? BRAND_GREEN : SURFACE_ALT);
        Color foreground = primary || danger ? Color.WHITE : BRAND_GOLD;
        button.setBackground(background);
        button.setForeground(foreground);
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(primary ? BRAND_GREEN.brighter() : background.brighter()),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)));
    }

    private void configureActivityCombo(JComboBox<String> comboBox)
    {
        comboBox.setRenderer(new DefaultListCellRenderer()
        {
            @Override
            public Component getListCellRendererComponent(
                JList<?> list,
                Object value,
                int index,
                boolean isSelected,
                boolean cellHasFocus)
            {
                JLabel label = (JLabel) super.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus);
                String activity = value == null ? "" : String.valueOf(value);
                label.setText(activity);
                addActivityIcon(label, activity);
                label.setIconTextGap(7);
                return label;
            }
        });
    }

    private void addActivityIcon(JLabel label, String activity)
    {
        int itemId = NightLegionTheme.activityItemId(activity);
        if (itemId <= 0 || itemManager == null)
        {
            label.setIcon(null);
            return;
        }

        ImageIcon icon = activityIcons.computeIfAbsent(itemId, id ->
        {
            AsyncBufferedImage image = itemManager.getImage(id);
            image.onLoaded(() -> SwingUtilities.invokeLater(this::repaint));
            return new ImageIcon(image);
        });
        label.setIcon(icon);
    }

    private String rsn()
    {
        return client.getLocalPlayer() == null || client.getLocalPlayer().getName() == null
            ? ""
            : client.getLocalPlayer().getName().trim();
    }

    private static String text(JsonObject object, String key, String fallback)
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

    private static int integer(JsonObject object, String key, int fallback)
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

    private static long longValue(JsonObject object, String key, long fallback)
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

    private static boolean bool(JsonObject object, String key)
    {
        try
        {
            return object.has(key) && !object.get(key).isJsonNull() && object.get(key).getAsBoolean();
        }
        catch (Exception ignored)
        {
            return false;
        }
    }

    private static String expiryLabel(long expiresAt)
    {
        long seconds = Math.max(0L, expiresAt - System.currentTimeMillis() / 1000L);
        if (seconds <= 0) return "now";
        long minutes = Math.max(1L, (seconds + 59L) / 60L);
        return minutes + (minutes == 1 ? " minute" : " minutes");
    }
}
