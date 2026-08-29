package com.nightlegion.livexp;

import com.google.gson.JsonObject;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;

/** Rank/profile UI. Rank Secret Key is configured separately in RuneLite config;
 * this view reads the linked Discord profile through the normal companion link. */
class NightLegionRankPanel extends JPanel
{
    private final Client client;
    private final NightLegionApi api;

    private final JLabel status = new JLabel("● Loading rank profile");
    private final JLabel rank = valueLabel("—");
    private final JLabel points = valueLabel("0");
    private final JLabel next = valueLabel("—");
    private final JLabel activity7 = valueLabel("0.00");
    private final JLabel activity30 = valueLabel("0.00");
    private final JLabel activity90 = valueLabel("0.00");
    private final JLabel weekly = new JLabel("Waiting for activity data", SwingConstants.LEFT);
    private final JLabel rsn = new JLabel("RSN: not linked", SwingConstants.LEFT);
    private final JProgressBar progress = new JProgressBar(0, 1000);

    NightLegionRankPanel(Client client, NightLegionApi api)
    {
        this.client = client;
        this.api = api;
        setLayout(new BorderLayout());
        setBackground(NightLegionTheme.BACKGROUND);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(NightLegionTheme.BACKGROUND);
        content.setBorder(BorderFactory.createEmptyBorder(10, 9, 10, 9));

        JLabel title = new JLabel("NIGHTLEGION RANK");
        title.setForeground(NightLegionTheme.PURPLE_BRIGHT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        title.setAlignmentX(LEFT_ALIGNMENT);
        content.add(title);
        content.add(Box.createVerticalStrut(3));

        JLabel subtitle = new JLabel("Clan contribution · permanent points");
        subtitle.setForeground(NightLegionTheme.MUTED);
        subtitle.setAlignmentX(LEFT_ALIGNMENT);
        content.add(subtitle);
        content.add(Box.createVerticalStrut(8));

        status.setForeground(NightLegionTheme.MUTED);
        status.setAlignmentX(LEFT_ALIGNMENT);
        content.add(status);
        content.add(Box.createVerticalStrut(10));

        JPanel top = new JPanel(new GridLayout(1, 2, 6, 0));
        top.setOpaque(false);
        top.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));
        top.setAlignmentX(LEFT_ALIGNMENT);
        top.add(tile("CURRENT RANK", rank));
        top.add(tile("CLAN POINTS", points));
        content.add(top);
        content.add(Box.createVerticalStrut(7));

        JPanel nextCard = card();
        nextCard.setLayout(new BoxLayout(nextCard, BoxLayout.Y_AXIS));
        nextCard.add(smallTitle("NEXT RANK"));
        nextCard.add(Box.createVerticalStrut(3));
        nextCard.add(next);
        nextCard.add(Box.createVerticalStrut(6));
        progress.setValue(0);
        progress.setStringPainted(true);
        progress.setString("0%");
        progress.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
        nextCard.add(progress);
        content.add(nextCard);
        content.add(Box.createVerticalStrut(7));

        JPanel activity = new JPanel(new GridLayout(1, 3, 5, 0));
        activity.setOpaque(false);
        activity.setMaximumSize(new Dimension(Integer.MAX_VALUE, 68));
        activity.setAlignmentX(LEFT_ALIGNMENT);
        activity.add(tile("7 DAYS", activity7));
        activity.add(tile("30 DAYS", activity30));
        activity.add(tile("90 DAYS", activity90));
        content.add(activity);
        content.add(Box.createVerticalStrut(7));

        JPanel weekCard = card();
        weekCard.setLayout(new BoxLayout(weekCard, BoxLayout.Y_AXIS));
        weekCard.add(smallTitle("THIS WEEK"));
        weekCard.add(Box.createVerticalStrut(4));
        weekly.setForeground(NightLegionTheme.SILVER);
        weekCard.add(weekly);
        content.add(weekCard);
        content.add(Box.createVerticalStrut(7));

        rsn.setForeground(NightLegionTheme.MUTED);
        rsn.setAlignmentX(LEFT_ALIGNMENT);
        content.add(rsn);
        content.add(Box.createVerticalStrut(8));

        JButton refresh = new JButton("REFRESH RANK");
        NightLegionTheme.styleButton(refresh, true, false);
        refresh.setAlignmentX(LEFT_ALIGNMENT);
        refresh.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        refresh.addActionListener(e -> refresh());
        content.add(refresh);
        content.add(Box.createVerticalStrut(8));

        JLabel help = new JLabel("Need linking? Discord: /rank link · /rank set-rsn");
        help.setForeground(NightLegionTheme.MUTED);
        help.setFont(help.getFont().deriveFont(9f));
        help.setAlignmentX(LEFT_ALIGNMENT);
        content.add(help);

        add(content, BorderLayout.NORTH);
        refresh();
    }

    void refresh()
    {
        status.setText("● Refreshing...");
        status.setForeground(NightLegionTheme.MUTED);
        api.action("overview", currentRsn(), new JsonObject(), json ->
            SwingUtilities.invokeLater(() -> render(json)), error ->
            SwingUtilities.invokeLater(() ->
            {
                status.setForeground(NightLegionTheme.MUTED);
                status.setText("● " + error);
            }));
    }

    private void render(JsonObject overview)
    {
        if (!overview.has("rank") || overview.get("rank").isJsonNull() || !overview.get("rank").isJsonObject())
        {
            status.setText("● Rank profile not linked yet");
            status.setForeground(NightLegionTheme.MUTED);
            rank.setText("—");
            next.setText("Use /rank set-rsn in Discord");
            return;
        }

        JsonObject profile = overview.getAsJsonObject("rank");
        JsonObject rankData = object(profile, "rank");
        JsonObject activityData = object(profile, "activity");
        JsonObject week = object(profile, "weekly");

        int level = integer(rankData, "level", 1);
        String title = text(rankData, "title", "Quester");
        double currentPoints = decimal(profile, "points", 0);
        rank.setText("L" + level + " · " + title);
        points.setText(format(currentPoints));

        status.setText("● Rank system connected");
        status.setForeground(NightLegionTheme.PURPLE_BRIGHT);
        rsn.setText("RSN: " + text(profile, "rsn", currentRsn()));

        activity7.setText(String.format("%.2f", decimal(activityData, "7d", 0)));
        activity30.setText(String.format("%.2f", decimal(activityData, "30d", 0)));
        activity90.setText(String.format("%.2f", decimal(activityData, "90d", 0)));

        int nextLevel = integer(rankData, "next_level", 0);
        if (nextLevel <= 0)
        {
            next.setText("MAX RANK");
            progress.setValue(1000);
            progress.setString("100%");
        }
        else
        {
            String nextTitle = text(rankData, "next_title", "Next rank");
            double threshold = decimal(rankData, "threshold", 0);
            double nextThreshold = decimal(rankData, "next_threshold", threshold + 1);
            double remaining = decimal(rankData, "remaining", 0);
            next.setText("L" + nextLevel + " · " + nextTitle + " · " + format(remaining) + " pts left");
            double span = Math.max(1.0, nextThreshold - threshold);
            double pct = Math.max(0.0, Math.min(1.0, (currentPoints - threshold) / span));
            progress.setValue((int) Math.round(pct * 1000));
            progress.setString((int) Math.round(pct * 100) + "%");
        }

        int messages = integer(week, "discord_messages", 0);
        int messagePoints = integer(week, "discord_message_points", 0);
        int clanMessages = integer(week, "clan_messages", 0);
        long xp = longValue(week, "wom_xp_gained", 0L);
        weekly.setText("Discord " + messages + " msgs (" + messagePoints + "/5 pts) · Clan "
            + clanMessages + " msgs · OSRS +" + String.format("%,d", xp) + " XP");
    }

    private static JPanel tile(String label, JLabel value)
    {
        JPanel card = card();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.add(smallTitle(label));
        card.add(Box.createVerticalStrut(4));
        card.add(value);
        return card;
    }

    private static JPanel card()
    {
        JPanel panel = new JPanel();
        panel.setBackground(NightLegionTheme.SURFACE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(NightLegionTheme.SURFACE_ALT.brighter()),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        panel.setAlignmentX(LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
        return panel;
    }

    private static JLabel smallTitle(String value)
    {
        JLabel label = new JLabel(value);
        label.setForeground(NightLegionTheme.MUTED);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 9f));
        label.setAlignmentX(LEFT_ALIGNMENT);
        return label;
    }

    private static JLabel valueLabel(String value)
    {
        JLabel label = new JLabel(value);
        label.setForeground(NightLegionTheme.SILVER);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 12f));
        label.setAlignmentX(LEFT_ALIGNMENT);
        return label;
    }

    private String currentRsn()
    {
        return client.getLocalPlayer() == null || client.getLocalPlayer().getName() == null
            ? ""
            : client.getLocalPlayer().getName().trim();
    }

    private static JsonObject object(JsonObject parent, String key)
    {
        return parent != null && parent.has(key) && parent.get(key).isJsonObject()
            ? parent.getAsJsonObject(key)
            : new JsonObject();
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

    private static double decimal(JsonObject object, String key, double fallback)
    {
        try
        {
            return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsDouble() : fallback;
        }
        catch (Exception ignored)
        {
            return fallback;
        }
    }

    private static String format(double value)
    {
        if (Math.rint(value) == value)
        {
            return String.format("%,.0f", value);
        }
        return String.format("%,.1f", value);
    }
}
