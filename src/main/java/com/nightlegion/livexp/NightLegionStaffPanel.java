package com.nightlegion.livexp;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.client.ui.PluginPanel;

/** Owner-only Live On-style staff workspace. */
final class NightLegionStaffPanel extends PluginPanel
{
    private static final Color ORANGE = new Color(190, 104, 0);
    private static final Color MUTED = new Color(155, 155, 155);
    private final Client client;
    private final NightLegionApi api;
    private final JPanel announcements = vertical();
    private final JPanel mvpBadges = vertical();
    private final JPanel twitch = vertical();
    private final JLabel status = new JLabel(" ");
    private JsonObject snapshot;

    NightLegionStaffPanel(Client client, NightLegionApi api)
    {
        super(false);
        this.client = client;
        this.api = api;
        setLayout(new BorderLayout(0, 5));
        setBorder(BorderFactory.createEmptyBorder(6, 5, 5, 5));

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Announcements", scroll(announcements));
        tabs.addTab("MVP Badge", scroll(mvpBadges));
        tabs.addTab("Twitch", scroll(twitch));
        add(tabs, BorderLayout.CENTER);
        status.setForeground(MUTED);
        add(status, BorderLayout.SOUTH);
    }

    void refresh()
    {
        api.action("community_snapshot", rsn(), new JsonObject(), json -> SwingUtilities.invokeLater(() ->
        {
            snapshot = json;
            if (!bool(json, "is_owner"))
            {
                renderDenied();
                return;
            }
            status.setText("Owner access verified");
            renderAnnouncements();
            renderMvpBadges();
            renderTwitch();
        }), error -> SwingUtilities.invokeLater(() -> status.setText(error)));
    }

    private void renderDenied()
    {
        announcements.removeAll();
        announcements.add(message("Owner only."));
        mvpBadges.removeAll();
        mvpBadges.add(message("Owner only."));
        twitch.removeAll();
        twitch.add(message("Owner only."));
        status.setText("Owner only");
        finish(announcements); finish(mvpBadges); finish(twitch);
    }

    private void renderAnnouncements()
    {
        announcements.removeAll();
        JLabel title = heading("CLAN ANNOUNCEMENTS");
        announcements.add(title);
        announcements.add(Box.createVerticalStrut(5));

        JTextArea composer = new JTextArea(4, 20);
        composer.setLineWrap(true);
        composer.setWrapStyleWord(true);
        composer.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(58, 58, 58)),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        composer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 86));
        announcements.add(composer);
        announcements.add(Box.createVerticalStrut(4));
        JButton send = action("Send announcement");
        send.addActionListener(e ->
        {
            String value = composer.getText().trim();
            if (value.isEmpty()) return;
            JsonObject data = new JsonObject();
            data.addProperty("text", value);
            call("community_notice_post", data);
        });
        announcements.add(send);
        announcements.add(Box.createVerticalStrut(8));
        announcements.add(heading("SENT ANNOUNCEMENTS"));
        announcements.add(Box.createVerticalStrut(4));

        JsonArray rows = array(snapshot, "notices");
        if (rows.size() == 0)
        {
            announcements.add(message("No announcements yet."));
        }
        for (JsonElement element : rows)
        {
            if (!element.isJsonObject()) continue;
            JsonObject row = element.getAsJsonObject();
            JPanel card = card();
            JLabel author = new JLabel(text(row, "author", "NightLegion"));
            author.setForeground(MUTED);
            card.add(author, BorderLayout.NORTH);
            JLabel body = new JLabel("<html><div style='width:165px'>" + escape(text(row, "text", "")) + "</div></html>");
            card.add(body, BorderLayout.CENTER);
            JPanel controls = new JPanel(new GridLayout(1, 2, 3, 0));
            controls.setOpaque(false);
            JButton edit = new JButton("Edit");
            JButton delete = new JButton("Delete");
            edit.addActionListener(e -> editAnnouncement(row));
            delete.addActionListener(e -> deleteAnnouncement(row));
            controls.add(edit);
            controls.add(delete);
            card.add(controls, BorderLayout.SOUTH);
            announcements.add(card);
            announcements.add(Box.createVerticalStrut(4));
        }
        finish(announcements);
    }

    private void editAnnouncement(JsonObject row)
    {
        String current = text(row, "text", "");
        String value = (String) JOptionPane.showInputDialog(this, "Announcement text", "Edit Announcement",
            JOptionPane.PLAIN_MESSAGE, null, null, current);
        if (value == null || value.trim().isEmpty()) return;
        JsonObject data = new JsonObject();
        data.addProperty("id", text(row, "id", ""));
        data.addProperty("text", value.trim());
        call("community_notice_update", data);
    }

    private void deleteAnnouncement(JsonObject row)
    {
        if (JOptionPane.showConfirmDialog(this, "Delete this announcement?", "NightLegion",
            JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        JsonObject data = new JsonObject();
        data.addProperty("id", text(row, "id", ""));
        call("community_notice_delete", data);
    }

    private void renderMvpBadges()
    {
        mvpBadges.removeAll();
        mvpBadges.add(heading("MVP BADGE"));
        JLabel help = new JLabel("<html><div style='width:170px'>Members here receive the gold <b>MVP</b> badge in clan chat and the native clan member list.</div></html>");
        help.setForeground(MUTED);
        mvpBadges.add(help);
        mvpBadges.add(Box.createVerticalStrut(6));
        JTextField rsn = new JTextField();
        rsn.setToolTipText("RuneScape name");
        rsn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        mvpBadges.add(rsn);
        mvpBadges.add(Box.createVerticalStrut(4));
        JButton add = action("Add MVP");
        add.addActionListener(e ->
        {
            String value = rsn.getText().trim();
            if (value.isEmpty()) return;
            JsonObject data = new JsonObject();
            data.addProperty("player_name", value);
            call("community_mvp_badge_add", data);
        });
        mvpBadges.add(add);
        mvpBadges.add(Box.createVerticalStrut(8));

        JsonArray rows = array(snapshot, "mvp_badges");
        for (JsonElement element : rows)
        {
            if (!element.isJsonObject()) continue;
            JsonObject row = element.getAsJsonObject();
            String player = text(row, "player_name", "");
            JPanel card = card();
            JLabel name = new JLabel(player + "   MVP");
            name.setForeground(new Color(255, 198, 40));
            card.add(name, BorderLayout.CENTER);
            JButton remove = new JButton("Remove");
            remove.addActionListener(e ->
            {
                JsonObject data = new JsonObject();
                data.addProperty("player_name", player);
                call("community_mvp_badge_remove", data);
            });
            card.add(remove, BorderLayout.EAST);
            mvpBadges.add(card);
            mvpBadges.add(Box.createVerticalStrut(4));
        }
        finish(mvpBadges);
    }

    private void renderTwitch()
    {
        twitch.removeAll();
        twitch.add(heading("LIVE ON TWITCH"));
        JLabel help = new JLabel("<html><div style='width:170px'>Link a NightLegion RSN to a Twitch login. When that channel is online, the player receives the green <b>LIVE</b> badge.</div></html>");
        help.setForeground(MUTED);
        twitch.add(help);
        twitch.add(Box.createVerticalStrut(6));
        JTextField player = new JTextField();
        player.setToolTipText("RuneScape name");
        JTextField channel = new JTextField();
        channel.setToolTipText("Twitch login");
        player.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        channel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        twitch.add(player);
        twitch.add(Box.createVerticalStrut(4));
        twitch.add(channel);
        twitch.add(Box.createVerticalStrut(4));
        JButton add = action("Save Twitch member");
        add.addActionListener(e ->
        {
            if (player.getText().trim().isEmpty() || channel.getText().trim().isEmpty()) return;
            JsonObject data = new JsonObject();
            data.addProperty("player_name", player.getText().trim());
            data.addProperty("channel", channel.getText().trim());
            call("community_stream_add", data);
        });
        twitch.add(add);
        twitch.add(Box.createVerticalStrut(8));

        JsonArray rows = array(snapshot, "streams");
        for (JsonElement element : rows)
        {
            if (!element.isJsonObject()) continue;
            JsonObject row = element.getAsJsonObject();
            String rsn = text(row, "player_name", "");
            String login = text(row, "channel_login", text(row, "channel", ""));
            boolean live = bool(row, "is_live");
            JPanel card = card();
            JLabel name = new JLabel("<html><b>" + escape(rsn) + "</b><br><font color='#999999'>" + escape(login) + "</font></html>");
            card.add(name, BorderLayout.CENTER);
            JLabel badge = new JLabel(live ? "LIVE" : "OFFLINE");
            badge.setForeground(live ? new Color(150, 255, 170) : MUTED);
            card.add(badge, BorderLayout.NORTH);
            JButton remove = new JButton("Remove");
            remove.addActionListener(e ->
            {
                JsonObject data = new JsonObject();
                data.addProperty("player_name", rsn);
                data.addProperty("channel", login);
                call("community_stream_remove", data);
            });
            card.add(remove, BorderLayout.EAST);
            twitch.add(card);
            twitch.add(Box.createVerticalStrut(4));
        }
        finish(twitch);
    }

    private void call(String action, JsonObject data)
    {
        status.setText("Saving...");
        api.action(action, rsn(), data, result -> SwingUtilities.invokeLater(this::refresh),
            error -> SwingUtilities.invokeLater(() -> status.setText(error)));
    }

    private static JPanel vertical()
    {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(7, 5, 7, 5));
        return panel;
    }

    private static JScrollPane scroll(JPanel panel)
    {
        JScrollPane scroll = new JScrollPane(panel);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(12);
        return scroll;
    }

    private static JLabel heading(String text)
    {
        JLabel label = new JLabel(text);
        label.setForeground(new Color(255, 152, 0));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private static JButton action(String text)
    {
        JButton button = new JButton(text);
        button.setBackground(ORANGE);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 29));
        return button;
    }

    private static JPanel card()
    {
        JPanel panel = new JPanel(new BorderLayout(5, 4));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(58, 58, 58)),
            BorderFactory.createEmptyBorder(5, 6, 5, 6)));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 115));
        return panel;
    }

    private static JLabel message(String value)
    {
        JLabel label = new JLabel(value);
        label.setForeground(MUTED);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private static void finish(JPanel panel)
    {
        panel.revalidate();
        panel.repaint();
    }

    private String rsn()
    {
        return client.getLocalPlayer() == null || client.getLocalPlayer().getName() == null
            ? "" : client.getLocalPlayer().getName().trim();
    }

    private static JsonArray array(JsonObject parent, String key)
    {
        try { return parent != null && parent.has(key) && parent.get(key).isJsonArray() ? parent.getAsJsonArray(key) : new JsonArray(); }
        catch (Exception ignored) { return new JsonArray(); }
    }

    private static String text(JsonObject object, String key, String fallback)
    {
        try { return object != null && object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : fallback; }
        catch (Exception ignored) { return fallback; }
    }

    private static boolean bool(JsonObject object, String key)
    {
        try { return object != null && object.has(key) && object.get(key).getAsBoolean(); }
        catch (Exception ignored) { return false; }
    }

    private static String escape(String value)
    {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
