package com.nightlegion.livexp;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.LinkBrowser;

/**
 * NightLegion community/staff hub. The server remains authoritative: this panel
 * can request or review actions but cannot grant clan points or directly change
 * an OSRS clan rank.
 */
class NightLegionCommunityPanel extends PluginPanel
{
    private final Client client;
    private final NightLegionApi api;
    private final JComboBox<String> section = new JComboBox<>(new String[]{
        "ANNOUNCEMENTS", "RANK REQUESTS", "MVP", "PB LEADERBOARD", "LIVE", "TAGS", "DROPS"
    });
    private final JPanel body = new JPanel();
    private final JLabel status = new JLabel("● Loading community hub");
    private JsonObject snapshot;
    private final String fixedSection;

    NightLegionCommunityPanel(Client client, NightLegionApi api)
    {
        this(client, api, null);
    }

    NightLegionCommunityPanel(Client client, NightLegionApi api, String fixedSection)
    {
        super(false);
        this.client = client;
        this.api = api;
        this.fixedSection = fixedSection;
        setLayout(new BorderLayout());
        setBackground(NightLegionTheme.BACKGROUND);

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(NightLegionTheme.HEADER);
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 2, 0, NightLegionTheme.PURPLE),
            BorderFactory.createEmptyBorder(8, 9, 7, 9)));

        JLabel title = new JLabel("NIGHTLEGION HUB");
        title.setForeground(NightLegionTheme.PURPLE_BRIGHT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 15f));
        title.setIcon(NightLegionTheme.markIcon(18, NightLegionTheme.PURPLE_BRIGHT));
        title.setIconTextGap(7);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.add(title);
        header.add(Box.createVerticalStrut(5));

        NightLegionTheme.styleCombo(section);
        section.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        section.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.add(section);
        header.add(Box.createVerticalStrut(5));

        status.setForeground(NightLegionTheme.MUTED);
        status.setFont(status.getFont().deriveFont(Font.BOLD, 10f));
        status.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.add(status);
        if (fixedSection == null)
        {
            add(header, BorderLayout.NORTH);
        }
        else
        {
            section.setSelectedItem(fixedSection);
        }

        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(NightLegionTheme.BACKGROUND);
        body.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JScrollPane scroll = new JScrollPane(body);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getViewport().setBackground(NightLegionTheme.BACKGROUND);
        add(scroll, BorderLayout.CENTER);

        section.addActionListener(e -> render());
    }

    void refresh()
    {
        status.setText("● Refreshing...");
        status.setForeground(NightLegionTheme.MUTED);
        api.action("community_snapshot", rsn(), new JsonObject(), json -> SwingUtilities.invokeLater(() ->
        {
            snapshot = json;
            status.setText("● Community hub connected" + (isStaff() ? " · STAFF" : ""));
            status.setForeground(NightLegionTheme.PURPLE_BRIGHT);
            render();
        }), error -> SwingUtilities.invokeLater(() ->
        {
            status.setText("● " + error);
            status.setForeground(NightLegionTheme.MUTED);
        }));
    }

    private void render()
    {
        body.removeAll();
        if (snapshot == null)
        {
            body.add(empty("Waiting for NightLegion community data..."));
            repaintBody();
            return;
        }
        String selected = fixedSection == null ? String.valueOf(section.getSelectedItem()) : fixedSection;
        switch (selected)
        {
            case "ANNOUNCEMENTS":
                renderAnnouncements();
                break;
            case "RANK REQUESTS":
                renderRanks();
                break;
            case "MVP":
                renderMvp();
                break;
            case "PB LEADERBOARD":
                renderPbs();
                break;
            case "LIVE":
                renderStreams();
                break;
            case "TAGS":
                renderTags();
                break;
            default:
                renderDrops();
                break;
        }
        repaintBody();
    }

    private void renderAnnouncements()
    {
        title("CLAN ANNOUNCEMENTS", "Pinned NightLegion notices from staff");
        JsonArray notices = array(snapshot, "notices");
        if (notices.size() == 0)
        {
            body.add(empty("No active announcements."));
        }
        for (JsonElement element : notices)
        {
            JsonObject notice = element.getAsJsonObject();
            JPanel card = card();
            JLabel author = small(text(notice, "author", "NightLegion Staff"));
            card.add(author);
            card.add(Box.createVerticalStrut(4));
            JTextArea message = textArea(text(notice, "text", ""));
            card.add(message);
            body.add(card);
            body.add(Box.createVerticalStrut(6));
        }
        if (isStaff())
        {
            JButton post = button("POST ANNOUNCEMENT", true);
            post.addActionListener(e ->
            {
                String text = JOptionPane.showInputDialog(this, "Announcement text", "NightLegion Announcement", JOptionPane.PLAIN_MESSAGE);
                if (text == null || text.trim().isEmpty()) return;
                JsonObject data = new JsonObject();
                data.addProperty("text", text.trim());
                data.addProperty("hours", 72);
                call("community_notice_post", data);
            });
            body.add(post);
        }
        body.add(refreshButton());
    }

    private void renderRanks()
    {
        title("RANK REVIEW", "Automatic eligibility + staff promotion review");
        JsonObject rank = object(snapshot, "rank");
        JsonObject self = object(rank, "self");
        if (self.size() > 0)
        {
            JPanel mine = card();
            mine.add(value("Your rank: L" + integer(self, "level", 1) + " · " + text(self, "title", "Quester")));
            mine.add(Box.createVerticalStrut(3));
            mine.add(small(format(decimal(self, "points", 0)) + " Clan Points"));
            if (bool(self, "promotion_ready"))
            {
                mine.add(Box.createVerticalStrut(4));
                JLabel ready = new JLabel("✓ Promotion threshold reached — staff review ready");
                ready.setForeground(NightLegionTheme.PURPLE_BRIGHT);
                mine.add(ready);
            }
            body.add(mine);
            body.add(Box.createVerticalStrut(6));
        }

        if (!isStaff())
        {
            JsonObject own = object(rank, "own_request");
            if (own.size() > 0 && "pending".equalsIgnoreCase(text(own, "status", "")))
            {
                body.add(empty("Your promotion review is already pending."));
            }
            else
            {
                JButton request = button("REQUEST PROMOTION REVIEW", true);
                request.addActionListener(e ->
                {
                    JsonObject data = new JsonObject();
                    String reason = JOptionPane.showInputDialog(this, "Optional note for staff", "Promotion Review", JOptionPane.PLAIN_MESSAGE);
                    if (reason != null) data.addProperty("reason", reason.trim());
                    call("community_rank_request", data);
                });
                body.add(request);
            }
            body.add(refreshButton());
            return;
        }

        JsonArray pending = array(rank, "pending");
        if (pending.size() == 0)
        {
            body.add(empty("No pending rank reviews."));
        }
        for (JsonElement element : pending)
        {
            JsonObject row = element.getAsJsonObject();
            JPanel card = card();
            card.add(value(text(row, "display_name", "Member") + " · L" + integer(row, "level", 1) + " " + text(row, "title", "")));
            card.add(Box.createVerticalStrut(3));
            card.add(small((text(row, "rsn", "").isEmpty() ? "No RSN" : text(row, "rsn", "")) + " · " + format(decimal(row, "points", 0)) + " pts · " + text(row, "source", "automatic")));
            card.add(Box.createVerticalStrut(6));
            JPanel actions = new JPanel(new GridLayout(1, 2, 5, 0));
            actions.setOpaque(false);
            JButton accept = button("ACCEPT", true);
            JButton decline = button("DECLINE", false);
            long uid = longValue(row, "user_id", 0L);
            accept.addActionListener(e -> decideRank(uid, true));
            decline.addActionListener(e -> decideRank(uid, false));
            actions.add(accept);
            actions.add(decline);
            card.add(actions);
            body.add(card);
            body.add(Box.createVerticalStrut(6));
        }

        JsonArray actions = array(rank, "recent_actions");
        if (actions.size() > 0)
        {
            JPanel history = card();
            history.add(heading("RECENT STAFF ACTIONS"));
            for (JsonElement element : actions)
            {
                JsonObject row = element.getAsJsonObject();
                history.add(small(text(row, "display_name", "Member") + " · " + text(row, "decision", "") + " by " + text(row, "staff_name", "Staff")));
            }
            body.add(history);
        }
        body.add(refreshButton());
    }

    private void decideRank(long uid, boolean accept)
    {
        JsonObject data = new JsonObject();
        data.addProperty("user_id", uid);
        data.addProperty("accept", accept);
        call("community_rank_decide", data);
    }

    private void renderMvp()
    {
        title("MONTHLY MVP", "Contribution, PvM and consistency leaders");
        JsonObject mvp = object(snapshot, "mvp");
        board("CONTRIBUTION", array(mvp, "contribution"), " pts");
        board("PVM", array(mvp, "pvm"), " kills");
        board("ACTIVITY", array(mvp, "activity"), " days");
        body.add(refreshButton());
    }

    private void board(String name, JsonArray rows, String suffix)
    {
        JPanel card = card();
        card.add(heading(name));
        if (rows.size() == 0)
        {
            card.add(small("No monthly data yet."));
        }
        int i = 1;
        for (JsonElement element : rows)
        {
            JsonObject row = element.getAsJsonObject();
            card.add(small("#" + i++ + "  " + text(row, "name", "Unknown") + " — " + format(decimal(row, "value", 0)) + suffix));
        }
        body.add(card);
        body.add(Box.createVerticalStrut(6));
    }

    private void renderPbs()
    {
        title("CLAN PB LEADERBOARDS", "Best submitted personal-best times");
        JsonArray boards = array(snapshot, "pbs");
        if (boards.size() == 0)
        {
            body.add(empty("No PBs submitted yet."));
        }
        for (JsonElement element : boards)
        {
            JsonObject board = element.getAsJsonObject();
            JPanel card = card();
            card.add(heading(text(board, "category", "PB")));
            JsonArray rows = array(board, "rows");
            int i = 1;
            for (JsonElement rowElement : rows)
            {
                JsonObject row = rowElement.getAsJsonObject();
                card.add(small("#" + i++ + "  " + (text(row, "rsn", "").isEmpty() ? text(row, "name", "Unknown") : text(row, "rsn", "Unknown")) + " — " + time(decimal(row, "seconds", 0))));
            }
            body.add(card);
            body.add(Box.createVerticalStrut(6));
        }
        body.add(refreshButton());
    }

    private void renderStreams()
    {
        title("CLAN LIVE", "NightLegion Twitch channels");
        JsonArray streams = array(snapshot, "streams");
        if (streams.size() == 0) body.add(empty("No clan streams configured yet."));
        for (JsonElement element : streams)
        {
            JsonObject stream = element.getAsJsonObject();
            JPanel card = card();
            String channel = text(stream, "channel", "stream");
            card.add(value(channel));
            card.add(Box.createVerticalStrut(4));
            JButton open = button("OPEN TWITCH", true);
            open.addActionListener(e -> LinkBrowser.browse(text(stream, "url", "https://www.twitch.tv/" + channel)));
            card.add(open);
            body.add(card);
            body.add(Box.createVerticalStrut(6));
        }
        if (isStaff())
        {
            JButton add = button("ADD TWITCH CHANNEL", false);
            add.addActionListener(e ->
            {
                String channel = JOptionPane.showInputDialog(this, "Twitch channel name", "Add Clan Stream", JOptionPane.PLAIN_MESSAGE);
                if (channel == null || channel.trim().isEmpty()) return;
                JsonObject data = new JsonObject();
                data.addProperty("channel", channel.trim());
                call("community_stream_add", data);
            });
            body.add(add);
        }
        body.add(refreshButton());
    }

    private void renderTags()
    {
        title("CLAN TAGS", "Custom NightLegion member badges/groups");
        JsonArray tags = array(snapshot, "tags");
        if (tags.size() == 0) body.add(empty(isStaff() ? "No clan tags configured yet." : "You have no clan tags yet."));
        for (JsonElement element : tags)
        {
            JsonObject tag = element.getAsJsonObject();
            JPanel card = card();
            card.add(value("◆ " + text(tag, "label", text(tag, "key", "Tag"))));
            if (isStaff()) card.add(small(array(tag, "members").size() + " members"));
            body.add(card);
            body.add(Box.createVerticalStrut(5));
        }
        if (isStaff())
        {
            JButton manage = button("ASSIGN / REMOVE TAG", false);
            manage.addActionListener(e -> manageTag());
            body.add(manage);
        }
        body.add(refreshButton());
    }

    private void manageTag()
    {
        String uid = JOptionPane.showInputDialog(this, "Discord user ID", "Clan Tag", JOptionPane.PLAIN_MESSAGE);
        if (uid == null || uid.trim().isEmpty()) return;
        String tag = JOptionPane.showInputDialog(this, "Tag key/name", "Clan Tag", JOptionPane.PLAIN_MESSAGE);
        if (tag == null || tag.trim().isEmpty()) return;
        int result = JOptionPane.showConfirmDialog(this, "Assign this tag? Choose No to remove it.", "Clan Tag", JOptionPane.YES_NO_CANCEL_OPTION);
        if (result == JOptionPane.CANCEL_OPTION || result == JOptionPane.CLOSED_OPTION) return;
        try
        {
            JsonObject data = new JsonObject();
            data.addProperty("user_id", Long.parseLong(uid.trim()));
            data.addProperty("tag", tag.trim());
            data.addProperty("label", tag.trim());
            data.addProperty("enabled", result == JOptionPane.YES_OPTION);
            call("community_tag_set", data);
        }
        catch (NumberFormatException ignored)
        {
            JOptionPane.showMessageDialog(this, "Invalid Discord user ID.", "NightLegion", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void renderDrops()
    {
        title("RECENT DROPS", "Optional NightLegion drop feed");
        JsonArray drops = array(snapshot, "drops");
        if (drops.size() == 0) body.add(empty("No qualifying drops reported yet."));
        for (JsonElement element : drops)
        {
            JsonObject drop = element.getAsJsonObject();
            JPanel card = card();
            String who = text(drop, "rsn", "").isEmpty() ? text(drop, "name", "Unknown") : text(drop, "rsn", "Unknown");
            card.add(value(who + " · " + integer(drop, "quantity", 1) + "× " + text(drop, "item", "Item")));
            long gp = longValue(drop, "total_value", 0);
            card.add(small((gp > 0 ? String.format("%,d gp", gp) : "Value unavailable") + (text(drop, "source", "").isEmpty() ? "" : " · " + text(drop, "source", ""))));
            body.add(card);
            body.add(Box.createVerticalStrut(5));
        }
        body.add(refreshButton());
    }

    private void call(String action, JsonObject data)
    {
        status.setText("● Sending...");
        api.action(action, rsn(), data, result -> SwingUtilities.invokeLater(() -> refresh()), error -> SwingUtilities.invokeLater(() ->
        {
            status.setText("● " + error);
            JOptionPane.showMessageDialog(this, error, "NightLegion", JOptionPane.ERROR_MESSAGE);
        }));
    }

    private JButton refreshButton()
    {
        JButton button = button("REFRESH", false);
        button.addActionListener(e -> refresh());
        return button;
    }

    private void title(String name, String subtitle)
    {
        JLabel heading = new JLabel(name);
        heading.setForeground(NightLegionTheme.PURPLE_BRIGHT);
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 14f));
        heading.setIcon(NightLegionTheme.markIcon(14, NightLegionTheme.PURPLE_BRIGHT));
        heading.setIconTextGap(6);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(heading);
        JLabel sub = new JLabel(subtitle);
        sub.setForeground(NightLegionTheme.MUTED);
        sub.setFont(sub.getFont().deriveFont(9.5f));
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(sub);
        body.add(Box.createVerticalStrut(8));
    }

    private JPanel card()
    {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(NightLegionTheme.SURFACE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 3, 0, 0, NightLegionTheme.PURPLE),
                BorderFactory.createLineBorder(NightLegionTheme.BORDER)),
            BorderFactory.createEmptyBorder(8, 9, 8, 8)));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));
        return panel;
    }

    private JPanel empty(String text)
    {
        JPanel panel = card();
        panel.add(small(text));
        return panel;
    }

    private JButton button(String text, boolean primary)
    {
        JButton button = new JButton(text);
        NightLegionTheme.styleButton(button, primary, false);
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        return button;
    }

    private JLabel heading(String text)
    {
        JLabel label = new JLabel(text);
        label.setForeground(NightLegionTheme.PURPLE_BRIGHT);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 10f));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private JLabel value(String text)
    {
        JLabel label = new JLabel(text);
        label.setForeground(NightLegionTheme.SILVER);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 11f));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private JLabel small(String text)
    {
        JLabel label = new JLabel(text);
        label.setForeground(NightLegionTheme.MUTED);
        label.setFont(label.getFont().deriveFont(9.5f));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private JTextArea textArea(String text)
    {
        JTextArea area = new JTextArea(text);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setOpaque(false);
        area.setForeground(NightLegionTheme.SILVER);
        area.setFont(area.getFont().deriveFont(10f));
        area.setAlignmentX(Component.LEFT_ALIGNMENT);
        area.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        return area;
    }

    private boolean isStaff()
    {
        return bool(snapshot, "is_staff");
    }

    private String rsn()
    {
        return client.getLocalPlayer() == null || client.getLocalPlayer().getName() == null ? "" : client.getLocalPlayer().getName().trim();
    }

    private void repaintBody()
    {
        body.revalidate();
        body.repaint();
    }

    private static JsonObject object(JsonObject parent, String key)
    {
        return parent != null && parent.has(key) && parent.get(key).isJsonObject() ? parent.getAsJsonObject(key) : new JsonObject();
    }

    private static JsonArray array(JsonObject parent, String key)
    {
        return parent != null && parent.has(key) && parent.get(key).isJsonArray() ? parent.getAsJsonArray(key) : new JsonArray();
    }

    private static String text(JsonObject object, String key, String fallback)
    {
        try { return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : fallback; }
        catch (Exception ignored) { return fallback; }
    }

    private static int integer(JsonObject object, String key, int fallback)
    {
        try { return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsInt() : fallback; }
        catch (Exception ignored) { return fallback; }
    }

    private static long longValue(JsonObject object, String key, long fallback)
    {
        try { return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsLong() : fallback; }
        catch (Exception ignored) { return fallback; }
    }

    private static double decimal(JsonObject object, String key, double fallback)
    {
        try { return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsDouble() : fallback; }
        catch (Exception ignored) { return fallback; }
    }

    private static boolean bool(JsonObject object, String key)
    {
        try { return object.has(key) && !object.get(key).isJsonNull() && object.get(key).getAsBoolean(); }
        catch (Exception ignored) { return false; }
    }

    private static String format(double value)
    {
        return Math.rint(value) == value ? String.format("%,.0f", value) : String.format("%,.1f", value);
    }

    private static String time(double seconds)
    {
        long ms = Math.round(seconds * 1000.0);
        long minutes = ms / 60000;
        double rest = (ms % 60000) / 1000.0;
        return minutes > 0 ? String.format("%d:%05.2f", minutes, rest) : String.format("%.2fs", rest);
    }
}
