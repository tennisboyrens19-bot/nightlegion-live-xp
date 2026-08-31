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
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.LinkBrowser;

/** Home page intentionally follows Live On Clan's panel composition. */
final class NightLegionHomePanel extends PluginPanel
{
    private static final Color ORANGE = new Color(255, 152, 0);
    private static final Color GREEN = new Color(70, 220, 100);
    private static final Color BLUE = new Color(90, 190, 245);
    private static final Color MUTED = new Color(155, 155, 155);

    private final Client client;
    private final NightLegionApi api;
    private final JPanel body = new JPanel();
    private JsonObject snapshot;

    NightLegionHomePanel(Client client, NightLegionApi api)
    {
        super(false);
        this.client = client;
        this.api = api;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(7, 7, 7, 7));
        setBackground(NightLegionTheme.BACKGROUND);

        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(NightLegionTheme.BACKGROUND);
        JScrollPane scroll = new JScrollPane(body);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getViewport().setBackground(NightLegionTheme.BACKGROUND);
        add(scroll, BorderLayout.CENTER);
    }

    void refresh()
    {
        api.action("community_snapshot", rsn(), new JsonObject(), json -> SwingUtilities.invokeLater(() ->
        {
            snapshot = json;
            render();
        }), error -> SwingUtilities.invokeLater(() ->
        {
            body.removeAll();
            JLabel label = new JLabel("Could not load NightLegion: " + error);
            label.setForeground(MUTED);
            body.add(label);
            repaintBody();
        }));
    }

    private void render()
    {
        body.removeAll();
        renderNotice();
        body.add(Box.createVerticalStrut(8));
        body.add(divider());
        body.add(Box.createVerticalStrut(8));
        renderLive();
        body.add(Box.createVerticalStrut(8));
        body.add(divider());
        body.add(Box.createVerticalStrut(8));
        renderRecent();
        repaintBody();
    }

    private void renderNotice()
    {
        JsonArray notices = array(snapshot, "notices");
        JsonObject notice = notices.size() > 0 && notices.get(0).isJsonObject()
            ? notices.get(0).getAsJsonObject() : null;

        JPanel card = new JPanel(new BorderLayout(5, 5));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, isOwner() ? 100 : 64));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 3, 0, 0, BLUE),
            BorderFactory.createEmptyBorder(7, 8, 7, 6)));

        String message = notice == null ? "No pinned announcement." : escape(text(notice, "text", ""));
        JLabel label = new JLabel("<html><b>ANNOUNCEMENTS</b><br><font color='#aaaaaa'><div style='width:160px'>" + message + "</div></font></html>");
        card.add(label, BorderLayout.CENTER);

        if (isOwner())
        {
            JPanel controls = new JPanel(new GridLayout(1, notice == null ? 1 : 3, 3, 0));
            controls.setOpaque(false);
            JButton post = smallButton(notice == null ? "Post" : "New");
            post.addActionListener(e -> postNotice());
            controls.add(post);
            if (notice != null)
            {
                JButton edit = smallButton("Edit");
                edit.addActionListener(e -> editNotice(notice));
                JButton remove = smallButton("Delete");
                remove.addActionListener(e -> deleteNotice(notice));
                controls.add(edit);
                controls.add(remove);
            }
            card.add(controls, BorderLayout.SOUTH);
        }
        body.add(card);
    }

    private void renderLive()
    {
        JLabel title = new JLabel("LIVE ON TWITCH");
        title.setForeground(GREEN);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(title);
        body.add(Box.createVerticalStrut(5));

        JsonArray streams = array(snapshot, "streams");
        int liveCount = 0;
        for (JsonElement element : streams)
        {
            if (!element.isJsonObject()) continue;
            JsonObject stream = element.getAsJsonObject();
            if (!bool(stream, "is_live")) continue;
            liveCount++;
            body.add(liveCard(stream));
            body.add(Box.createVerticalStrut(4));
        }
        if (liveCount == 0)
        {
            JLabel empty = new JLabel("No active streams right now.");
            empty.setForeground(MUTED);
            empty.setHorizontalAlignment(JLabel.CENTER);
            empty.setAlignmentX(Component.LEFT_ALIGNMENT);
            empty.setBorder(BorderFactory.createEmptyBorder(18, 4, 18, 4));
            empty.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
            body.add(empty);
        }
    }

    private JPanel liveCard(JsonObject stream)
    {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setPreferredSize(new Dimension(210, 54));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 54));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 3, 1, 0, new Color(40, 200, 80)),
            BorderFactory.createEmptyBorder(4, 7, 4, 5)));

        JPanel heading = new JPanel(new BorderLayout(5, 0));
        heading.setOpaque(false);
        String name = text(stream, "display_name", text(stream, "channel_login", text(stream, "channel", "Streamer")));
        JLabel member = new JLabel("●  " + abbreviate(name, 20));
        member.setForeground(GREEN);
        heading.add(member, BorderLayout.CENTER);
        JLabel badge = new JLabel("LIVE");
        badge.setForeground(GREEN);
        badge.setFont(badge.getFont().deriveFont(java.awt.Font.BOLD, 9f));
        heading.add(badge, BorderLayout.EAST);
        card.add(heading);

        JPanel details = new JPanel(new BorderLayout(5, 0));
        details.setOpaque(false);
        String game = text(stream, "game_name", "");
        int viewers = integer(stream, "viewer_count", 0);
        JLabel info = new JLabel(abbreviate((game.isEmpty() ? "Twitch" : game) + " · " + viewers + " viewers", 26));
        info.setForeground(MUTED);
        details.add(info, BorderLayout.CENTER);
        JButton open = new JButton("Open");
        open.setBackground(new Color(190, 104, 0));
        open.setForeground(Color.WHITE);
        open.setMargin(new java.awt.Insets(1, 8, 1, 8));
        String url = text(stream, "url", "https://www.twitch.tv/" + text(stream, "channel_login", ""));
        open.addActionListener(e -> LinkBrowser.browse(url));
        details.add(open, BorderLayout.EAST);
        card.add(details);
        return card;
    }

    private void renderRecent()
    {
        JLabel title = new JLabel("RECENT CLAN ACTIVITY");
        title.setForeground(ORANGE);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(title);
        body.add(Box.createVerticalStrut(5));

        JsonArray rows = array(snapshot, "recent_activity");
        if (rows.size() == 0)
        {
            JLabel empty = new JLabel("No recent clan activity.");
            empty.setForeground(MUTED);
            empty.setBorder(BorderFactory.createEmptyBorder(8, 4, 8, 4));
            body.add(empty);
            return;
        }
        int shown = 0;
        for (JsonElement element : rows)
        {
            if (!element.isJsonObject() || shown++ >= 10) break;
            JsonObject row = element.getAsJsonObject();
            body.add(activityRow(row));
            body.add(Box.createVerticalStrut(2));
        }
    }

    private JPanel activityRow(JsonObject row)
    {
        String type = text(row, "type", "");
        Color accent = "CLAN_RECORD".equals(type) ? BLUE : ORANGE;
        JPanel panel = new JPanel(new BorderLayout(5, 0));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 3, 1, 0, accent),
            BorderFactory.createEmptyBorder(5, 7, 5, 4)));
        JLabel marker = new JLabel("CLAN_RECORD".equals(type) ? "◷" : "★");
        marker.setForeground(accent);
        panel.add(marker, BorderLayout.WEST);
        String player = text(row, "player_name", "");
        String detail = text(row, "title", "Clan activity");
        JLabel text = new JLabel("<html>" + (player.isEmpty() ? "" : "<b>" + escape(player) + "</b><br>") + escape(detail) + "</html>");
        panel.add(text, BorderLayout.CENTER);
        return panel;
    }

    private void postNotice()
    {
        String value = JOptionPane.showInputDialog(this, "Announcement text", "NightLegion Announcement", JOptionPane.PLAIN_MESSAGE);
        if (value == null || value.trim().isEmpty()) return;
        JsonObject data = new JsonObject();
        data.addProperty("text", value.trim());
        call("community_notice_post", data);
    }

    private void editNotice(JsonObject notice)
    {
        String current = text(notice, "text", "");
        String value = (String) JOptionPane.showInputDialog(this, "Announcement text", "Edit Announcement",
            JOptionPane.PLAIN_MESSAGE, null, null, current);
        if (value == null || value.trim().isEmpty()) return;
        JsonObject data = new JsonObject();
        data.addProperty("id", text(notice, "id", ""));
        data.addProperty("text", value.trim());
        call("community_notice_update", data);
    }

    private void deleteNotice(JsonObject notice)
    {
        if (JOptionPane.showConfirmDialog(this, "Delete this announcement?", "NightLegion",
            JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        JsonObject data = new JsonObject();
        data.addProperty("id", text(notice, "id", ""));
        call("community_notice_delete", data);
    }

    private void call(String action, JsonObject data)
    {
        api.action(action, rsn(), data, ignored -> SwingUtilities.invokeLater(this::refresh),
            error -> SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, error, "NightLegion", JOptionPane.ERROR_MESSAGE)));
    }

    private boolean isOwner()
    {
        return snapshot != null && bool(snapshot, "is_owner");
    }

    private String rsn()
    {
        return client.getLocalPlayer() == null || client.getLocalPlayer().getName() == null
            ? "" : client.getLocalPlayer().getName().trim();
    }

    private static JPanel divider()
    {
        JPanel divider = new JPanel();
        divider.setAlignmentX(Component.LEFT_ALIGNMENT);
        divider.setBackground(new Color(62, 62, 62));
        divider.setPreferredSize(new Dimension(210, 1));
        divider.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        return divider;
    }

    private static JButton smallButton(String text)
    {
        JButton button = new JButton(text);
        button.setMargin(new java.awt.Insets(1, 5, 1, 5));
        return button;
    }

    private void repaintBody()
    {
        body.revalidate();
        body.repaint();
    }

    private static JsonArray array(JsonObject parent, String key)
    {
        return parent != null && parent.has(key) && parent.get(key).isJsonArray()
            ? parent.getAsJsonArray(key) : new JsonArray();
    }

    private static String text(JsonObject object, String key, String fallback)
    {
        try { return object != null && object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : fallback; }
        catch (Exception ignored) { return fallback; }
    }

    private static int integer(JsonObject object, String key, int fallback)
    {
        try { return object != null && object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsInt() : fallback; }
        catch (Exception ignored) { return fallback; }
    }

    private static boolean bool(JsonObject object, String key)
    {
        try { return object != null && object.has(key) && object.get(key).getAsBoolean(); }
        catch (Exception ignored) { return false; }
    }

    private static String abbreviate(String value, int max)
    {
        if (value == null || value.length() <= max) return value == null ? "" : value;
        return value.substring(0, Math.max(0, max - 1)).trim() + "…";
    }

    private static String escape(String value)
    {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
