package com.nightlegion.livexp;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.LinkBrowser;

/** Live On home panel copied to NightLegion and translated to English. */
final class NightLegionHomePanel extends PluginPanel
{
    private static final Pattern RECORD_TIME_PATTERN = Pattern.compile(
        "(?:\\s*[·•|-]\\s*|\\s+)(\\d{1,2}(?::\\d{2}){1,2}(?:\\.\\d{1,2})?)$");

    private final Client client;
    private final NightLegionApi api;
    private final JPanel onlineChannels = new JPanel();
    private final JLabel pinnedNotice = new JLabel("<html><b>ANNOUNCEMENTS</b><br><font color='#aaaaaa'>No pinned announcement.</font></html>");
    private final JPanel recentActivities = new JPanel();
    private final Set<String> expandedActivities = new HashSet<>();
    private List<Activity> currentActivities = Collections.emptyList();

    NightLegionHomePanel(Client client, NightLegionApi api)
    {
        super(false);
        this.client = client;
        this.api = api;
        setLayout(new BorderLayout(6, 8));
        setBorder(BorderFactory.createEmptyBorder(7, 7, 7, 7));

        JPanel home = new JPanel();
        home.setLayout(new BoxLayout(home, BoxLayout.Y_AXIS));

        JPanel notice = new JPanel(new BorderLayout());
        notice.setAlignmentX(LEFT_ALIGNMENT);
        notice.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
        notice.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 3, 0, 0, new Color(75, 170, 235)),
            BorderFactory.createEmptyBorder(7, 8, 7, 6)));
        notice.add(pinnedNotice);
        home.add(notice);
        home.add(Box.createVerticalStrut(8));
        home.add(createSectionDivider());
        home.add(Box.createVerticalStrut(8));

        JLabel title = new JLabel("ONLINE ON TWITCH");
        title.setForeground(new Color(70, 220, 100));
        title.setAlignmentX(LEFT_ALIGNMENT);
        home.add(title);
        home.add(Box.createVerticalStrut(5));
        onlineChannels.setLayout(new BoxLayout(onlineChannels, BoxLayout.Y_AXIS));
        onlineChannels.setAlignmentX(LEFT_ALIGNMENT);
        onlineChannels.setMinimumSize(new Dimension(0, 0));
        onlineChannels.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        home.add(onlineChannels);
        home.add(Box.createVerticalStrut(8));
        home.add(createSectionDivider());
        home.add(Box.createVerticalStrut(8));

        JLabel recentTitle = new JLabel("RECENT CLAN ACTIVITY");
        recentTitle.setForeground(new Color(255, 152, 0));
        recentTitle.setAlignmentX(LEFT_ALIGNMENT);
        home.add(recentTitle);
        recentActivities.setLayout(new BoxLayout(recentActivities, BoxLayout.Y_AXIS));
        recentActivities.setAlignmentX(LEFT_ALIGNMENT);
        home.add(recentActivities);

        JScrollPane scroll = new JScrollPane(home);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        updatePinnedNotice(null);
        updateOnline(Collections.emptyList());
        updateRecent(Collections.emptyList());
    }

    void refresh()
    {
        api.action("community_snapshot", rsn(), new JsonObject(), json -> SwingUtilities.invokeLater(() ->
        {
            JsonArray notices = array(json, "notices");
            String notice = null;
            if (notices.size() > 0 && notices.get(0).isJsonObject())
            {
                notice = text(notices.get(0).getAsJsonObject(), "text", "");
            }
            updatePinnedNotice(notice);

            List<Stream> streams = new ArrayList<>();
            for (JsonElement element : array(json, "streams"))
            {
                if (!element.isJsonObject()) continue;
                JsonObject row = element.getAsJsonObject();
                if (!bool(row, "is_live")) continue;
                Stream stream = new Stream();
                stream.playerName = text(row, "player_name", text(row, "rsn", text(row, "display_name", "Streamer")));
                stream.url = text(row, "url", "https://www.twitch.tv/" + text(row, "channel_login", ""));
                stream.viewerCount = integer(row, "viewer_count", 0);
                stream.game = text(row, "game_name", "");
                streams.add(stream);
            }
            updateOnline(streams);

            List<Activity> activities = new ArrayList<>();
            for (JsonElement element : array(json, "recent_activity"))
            {
                if (!element.isJsonObject()) continue;
                JsonObject row = element.getAsJsonObject();
                Activity activity = new Activity();
                activity.type = text(row, "type", "");
                activity.playerName = text(row, "player_name", "");
                activity.title = text(row, "title", "Clan activity");
                activities.add(activity);
            }
            updateRecent(activities);
        }), error -> SwingUtilities.invokeLater(() ->
        {
            updatePinnedNotice("Could not load NightLegion: " + error);
            updateOnline(Collections.emptyList());
            updateRecent(Collections.emptyList());
        }));
    }

    private void updateOnline(List<Stream> channels)
    {
        SwingUtilities.invokeLater(() ->
        {
            onlineChannels.removeAll();
            List<Stream> displayed = channels == null ? new ArrayList<>() : new ArrayList<>(channels);
            if (displayed.isEmpty())
            {
                JLabel empty = new JLabel("No active streams right now.");
                empty.setHorizontalAlignment(JLabel.CENTER);
                empty.setForeground(new Color(155, 155, 155));
                empty.setBorder(BorderFactory.createEmptyBorder(18, 4, 18, 4));
                onlineChannels.add(empty);
            }
            else
            {
                for (Stream channel : displayed)
                {
                    onlineChannels.add(createLiveCard(channel));
                }
            }
            onlineChannels.revalidate();
            onlineChannels.repaint();
        });
    }

    private void updatePinnedNotice(String message)
    {
        SwingUtilities.invokeLater(() -> pinnedNotice.setText(message == null || message.trim().isEmpty()
            ? "<html><b>ANNOUNCEMENTS</b><br><font color='#aaaaaa'>No pinned announcement.</font></html>"
            : "<html><b>ANNOUNCEMENTS</b><br><div style='width:150px'>" + escapeHtml(message.trim()) + "</div></html>"));
    }

    private void updateRecent(List<Activity> activities)
    {
        SwingUtilities.invokeLater(() ->
        {
            recentActivities.removeAll();
            List<Activity> displayed = activities == null ? new ArrayList<>() : new ArrayList<>(activities);
            displayed.removeIf(activity -> activity == null || !isFeedActivity(activity.type));
            if (displayed.size() > 10)
            {
                displayed = new ArrayList<>(displayed.subList(0, 10));
            }
            currentActivities = new ArrayList<>(displayed);
            Set<String> visibleKeys = new HashSet<>();
            for (Activity activity : displayed)
            {
                visibleKeys.add(activityKey(activity));
            }
            expandedActivities.retainAll(visibleKeys);

            if (displayed.isEmpty())
            {
                JLabel empty = new JLabel("No recent clan activity.");
                empty.setForeground(new Color(155, 155, 155));
                empty.setBorder(BorderFactory.createEmptyBorder(8, 4, 8, 4));
                recentActivities.add(empty);
            }
            else
            {
                for (Activity activity : displayed)
                {
                    JPanel row = new JPanel(new BorderLayout(5, 0));
                    row.setAlignmentX(LEFT_ALIGNMENT);
                    Color accent = "CLAN_RECORD".equals(activity.type) ? new Color(90, 190, 245)
                        : new Color(235, 185, 45);
                    row.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 3, 1, 0, accent),
                        BorderFactory.createEmptyBorder(5, 7, 5, 4)));

                    String icon = "CLAN_RECORD".equals(activity.type) ? "◷"
                        : "MVP_LEADER".equals(activity.type) || "MVP_WINNER".equals(activity.type) ? "♛" : "★";
                    JLabel marker = new JLabel(icon);
                    marker.setForeground(accent);

                    String player = activity.playerName == null ? "" : activity.playerName;
                    String detail = activity.title == null ? "Clan activity" : activity.title;
                    String recordTime = "";
                    if ("CLAN_RECORD".equals(activity.type))
                    {
                        Matcher timeMatcher = RECORD_TIME_PATTERN.matcher(detail);
                        if (timeMatcher.find())
                        {
                            recordTime = timeMatcher.group(1);
                            detail = detail.substring(0, timeMatcher.start()).trim();
                        }
                        detail = detail.replaceFirst("(?i)^new clan best time in\\s+", "New clan best time in ");
                    }

                    boolean collective = player.isEmpty();
                    boolean clanRecord = "CLAN_RECORD".equals(activity.type);
                    String key = activityKey(activity);
                    boolean expanded = expandedActivities.contains(key);
                    boolean expandable = (!collective && (player.length() > 22 || detail.length() > 29))
                        || (collective && detail.length() > 50);
                    JPanel text = createActivityText(player, detail, recordTime, collective, clanRecord, expanded);
                    int collapsedHeight = clanRecord ? 59 : 43;
                    int rowHeight = expanded ? Math.max(collapsedHeight, text.getPreferredSize().height + 10) : collapsedHeight;
                    row.setPreferredSize(new Dimension(210, rowHeight));
                    row.setMaximumSize(new Dimension(Integer.MAX_VALUE, rowHeight));
                    row.add(marker, BorderLayout.WEST);
                    row.add(text, BorderLayout.CENTER);

                    if (expandable)
                    {
                        Runnable toggleActivity = () ->
                        {
                            if (!expandedActivities.remove(key))
                            {
                                expandedActivities.add(key);
                            }
                            updateRecent(currentActivities);
                        };
                        JButton toggle = new JButton(new ActivityToggleIcon(expanded));
                        toggle.setToolTipText(expanded ? "Collapse activity" : "Expand activity");
                        toggle.setMargin(new java.awt.Insets(0, 1, 0, 1));
                        toggle.setFocusable(false);
                        toggle.setPreferredSize(new Dimension(22, 22));
                        toggle.addActionListener(event -> toggleActivity.run());
                        row.add(toggle, BorderLayout.EAST);
                        makeClickable(row, toggleActivity);
                    }
                    recentActivities.add(row);
                }
            }
            recentActivities.revalidate();
            recentActivities.repaint();
        });
    }

    private static void makeClickable(Component component, Runnable action)
    {
        if (!(component instanceof AbstractButton))
        {
            component.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            component.addMouseListener(new MouseAdapter()
            {
                @Override
                public void mouseClicked(MouseEvent event)
                {
                    if (event.getButton() == MouseEvent.BUTTON1)
                    {
                        action.run();
                    }
                }
            });
        }
        if (component instanceof Container)
        {
            for (Component child : ((Container) component).getComponents())
            {
                makeClickable(child, action);
            }
        }
    }

    private static JPanel createActivityText(String player, String detail, String recordTime,
        boolean collective, boolean clanRecord, boolean expanded)
    {
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        String timeSuffix = recordTime.isEmpty() ? "" : " · " + recordTime;
        String tooltip = collective ? escapeHtml(detail + timeSuffix)
            : "<html><b>" + escapeHtml(player) + "</b><br>" + escapeHtml(detail + timeSuffix) + "</html>";

        if (clanRecord && !recordTime.isEmpty())
        {
            JPanel heading = new JPanel(new BorderLayout(4, 0));
            heading.setOpaque(false);
            heading.setAlignmentX(LEFT_ALIGNMENT);
            JLabel name = new JLabel("<html><b>" + escapeHtml(abbreviate(player, 16)) + "</b></html>");
            name.setToolTipText(tooltip);
            heading.add(name, BorderLayout.CENTER);
            JLabel time = new JLabel(recordTime);
            time.setForeground(new Color(90, 190, 245));
            time.setToolTipText(tooltip);
            heading.add(time, BorderLayout.EAST);
            text.add(heading);

            JLabel recordLabel = new JLabel("New clan best time");
            recordLabel.setToolTipText(tooltip);
            text.add(recordLabel);
            String boss = detail.replaceFirst("(?i)^new clan best time in\\s+", "").trim();
            JLabel bossLabel = expanded
                ? new JLabel("<html><div style='width:125px'><b>" + escapeHtml(boss) + "</b></div></html>")
                : new JLabel("<html><b>" + escapeHtml(abbreviate(boss, 24)) + "</b></html>");
            bossLabel.setForeground(new Color(185, 205, 220));
            bossLabel.setToolTipText(tooltip);
            text.add(bossLabel);
            return text;
        }

        if (expanded)
        {
            JLabel full = new JLabel("<html><div style='width:125px'>"
                + (collective ? "<b>" + escapeHtml(detail) + "</b>"
                    : "<b>" + escapeHtml(player) + "</b><br>" + escapeHtml(detail))
                + "</div></html>");
            full.setToolTipText(tooltip);
            text.add(full);
            return text;
        }

        if (!collective)
        {
            JLabel name = new JLabel("<html><b>" + escapeHtml(abbreviate(player, 22)) + "</b></html>");
            name.setToolTipText(tooltip);
            text.add(name);
        }
        JLabel summary = new JLabel(abbreviate(detail, collective ? 50 : 29));
        summary.setToolTipText(tooltip);
        text.add(summary);
        return text;
    }

    private static String activityKey(Activity activity)
    {
        return String.valueOf(activity.type) + '\u0000' + String.valueOf(activity.playerName)
            + '\u0000' + String.valueOf(activity.title);
    }

    private static JPanel createSectionDivider()
    {
        JPanel divider = new JPanel();
        divider.setAlignmentX(LEFT_ALIGNMENT);
        divider.setBackground(new Color(62, 62, 62));
        divider.setPreferredSize(new Dimension(210, 1));
        divider.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        return divider;
    }

    private static boolean isFeedActivity(String type)
    {
        return "PROMOTION".equals(type) || "CLAN_RECORD".equals(type)
            || "MVP_LEADER".equals(type) || "MVP_WINNER".equals(type)
            || "DROP_MILESTONE".equals(type);
    }

    private static String abbreviate(String value, int maximumLength)
    {
        if (value == null || value.length() <= maximumLength)
        {
            return value;
        }
        return value.substring(0, Math.max(0, maximumLength - 1)).trim() + "…";
    }

    private static JPanel createLiveCard(Stream channel)
    {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setAlignmentX(LEFT_ALIGNMENT);
        card.setPreferredSize(new Dimension(210, 54));
        card.setMinimumSize(new Dimension(0, 54));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 54));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 3, 1, 0, new Color(40, 200, 80)),
            BorderFactory.createEmptyBorder(4, 7, 4, 5)));

        JPanel heading = new JPanel(new BorderLayout(5, 0));
        heading.setOpaque(false);
        heading.setAlignmentX(LEFT_ALIGNMENT);
        heading.setMaximumSize(new Dimension(Integer.MAX_VALUE, 21));
        String playerName = channel.playerName == null ? "Streamer" : channel.playerName;
        JLabel name = new JLabel("●  " + abbreviate(playerName, 20));
        name.setToolTipText(playerName);
        name.setForeground(new Color(70, 220, 100));
        heading.add(name, BorderLayout.CENTER);
        JLabel liveBadge = new JLabel("LIVE");
        liveBadge.setForeground(new Color(85, 225, 110));
        liveBadge.setFont(liveBadge.getFont().deriveFont(java.awt.Font.BOLD, 9f));
        heading.add(liveBadge, BorderLayout.EAST);
        card.add(heading);

        JPanel details = new JPanel(new BorderLayout(5, 0));
        details.setOpaque(false);
        details.setAlignmentX(LEFT_ALIGNMENT);
        details.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        String detailText = channel.game == null || channel.game.isEmpty() ? "Twitch" : channel.game;
        if (channel.viewerCount > 0)
        {
            detailText += " · " + channel.viewerCount + " viewers";
        }
        JLabel detail = new JLabel(abbreviate(detailText, 22));
        detail.setForeground(new Color(165, 165, 165));
        detail.setToolTipText(channel.url);
        details.add(detail, BorderLayout.CENTER);
        JButton open = new JButton("Open");
        open.setBackground(new Color(190, 104, 0));
        open.setForeground(Color.WHITE);
        open.setMargin(new java.awt.Insets(1, 8, 1, 8));
        open.setPreferredSize(new Dimension(54, 22));
        open.setToolTipText("Open " + channel.url);
        open.addActionListener(event -> LinkBrowser.browse(channel.url));
        details.add(open, BorderLayout.EAST);
        card.add(details);
        return card;
    }

    private String rsn()
    {
        return client.getLocalPlayer() == null || client.getLocalPlayer().getName() == null
            ? "" : client.getLocalPlayer().getName().trim();
    }

    private static String escapeHtml(String value)
    {
        if (value == null)
        {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static JsonArray array(JsonObject parent, String key)
    {
        try
        {
            return parent != null && parent.has(key) && parent.get(key).isJsonArray()
                ? parent.getAsJsonArray(key) : new JsonArray();
        }
        catch (Exception ignored)
        {
            return new JsonArray();
        }
    }

    private static String text(JsonObject object, String key, String fallback)
    {
        try
        {
            return object != null && object.has(key) && !object.get(key).isJsonNull()
                ? object.get(key).getAsString() : fallback;
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
            return object != null && object.has(key) && !object.get(key).isJsonNull()
                ? object.get(key).getAsInt() : fallback;
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
            return object != null && object.has(key) && object.get(key).getAsBoolean();
        }
        catch (Exception ignored)
        {
            return false;
        }
    }

    private static final class Stream
    {
        String playerName;
        String url;
        String game;
        int viewerCount;
    }

    private static final class Activity
    {
        String type;
        String playerName;
        String title;
    }

    private static final class ActivityToggleIcon implements javax.swing.Icon
    {
        private final boolean expanded;

        private ActivityToggleIcon(boolean expanded)
        {
            this.expanded = expanded;
        }

        @Override
        public void paintIcon(Component component, java.awt.Graphics graphics, int x, int y)
        {
            graphics.setColor(component.isEnabled() ? new Color(190, 190, 190) : new Color(105, 105, 105));
            int[] xs = {x, x + 8, x + 4};
            int[] ys = expanded ? new int[]{y + 6, y + 6, y + 1} : new int[]{y + 1, y + 1, y + 6};
            graphics.fillPolygon(xs, ys, 3);
        }

        @Override public int getIconWidth() { return 9; }
        @Override public int getIconHeight() { return 8; }
    }
}
