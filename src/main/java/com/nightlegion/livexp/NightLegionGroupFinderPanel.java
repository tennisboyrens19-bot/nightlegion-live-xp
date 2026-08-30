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
import net.runelite.api.Client;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.AsyncBufferedImage;

/**
 * NightLegion Group Finder using the RaidMates visual layout and the existing
 * NightLegion Discord-backed Group Finder API.
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

        JButton requests = new JButton("Requests");
        styleButton(requests, false, false);
        requests.addActionListener(event -> showRequests());

        JButton myGroup = new JButton("My Group");
        styleButton(myGroup, false, false);
        myGroup.addActionListener(event -> showMyGroups());

        JPanel buttons = new JPanel(new GridLayout(2, 2, 6, 4));
        buttons.setBackground(HEADER);
        buttons.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        buttons.setAlignmentX(LEFT_ALIGNMENT);
        buttons.add(refresh);
        buttons.add(create);
        buttons.add(requests);
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
        List<JsonObject> out = new ArrayList<>();
        if (latest == null || !latest.has("groups") || !latest.get("groups").isJsonArray())
        {
            return out;
        }

        String wanted = filter.getSelectedItem() == null ? "All activities" : String.valueOf(filter.getSelectedItem());
        for (JsonElement element : latest.getAsJsonArray("groups"))
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
        JsonObject data = new JsonObject();
        data.addProperty("group_id", text(group, "id", ""));
        status.setText("Sending request...");
        api.action("group_join", rsn(), data, result -> SwingUtilities.invokeLater(() ->
        {
            String message = text(result, "message", "Request sent.");
            JOptionPane.showMessageDialog(this, message, "NightLegion", JOptionPane.INFORMATION_MESSAGE);
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
        if (latest == null || !latest.has("groups") || !latest.get("groups").isJsonArray())
        {
            refresh();
            return;
        }

        List<JsonObject> mine = new ArrayList<>();
        for (JsonElement element : latest.getAsJsonArray("groups"))
        {
            if (element.isJsonObject() && bool(element.getAsJsonObject(), "is_host"))
            {
                mine.add(element.getAsJsonObject());
            }
        }

        if (mine.isEmpty())
        {
            JOptionPane.showMessageDialog(this, "You have no active groups.", "NightLegion", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        for (JsonObject group : mine)
        {
            JPanel card = new JPanel();
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            styleCard(card);

            JLabel title = new JLabel(text(group, "title", text(group, "activity", "My Group")));
            title.setForeground(BRAND_GOLD);
            title.setFont(title.getFont().deriveFont(Font.BOLD));
            card.add(title);

            JButton close = new JButton("Close group");
            styleButton(close, false, true);
            close.addActionListener(event -> closeGroup(group));
            card.add(Box.createVerticalStrut(5));
            card.add(close);
            list.add(card);
            list.add(Box.createVerticalStrut(6));
        }
        JOptionPane.showMessageDialog(this, list, "My Group", JOptionPane.PLAIN_MESSAGE);
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
}
