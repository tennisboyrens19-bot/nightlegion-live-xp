package com.nightlegion.livexp;

import com.google.gson.JsonObject;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.Locale;
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

/** Live On-style rank page with NightLegion Clan Points/activity data. */
class NightLegionRankPanel extends JPanel
{
    private static final Color ORANGE = new Color(190, 104, 0);
    private static final Color ACCENT = new Color(255, 152, 0);
    private static final Color MUTED = new Color(155, 155, 155);
    private static final Color BORDER = new Color(58, 58, 58);

    private final Client client;
    private final NightLegionApi api;
    private final JPanel body = new JPanel();
    private final JLabel status = new JLabel("Loading rank profile...");

    NightLegionRankPanel(Client client, NightLegionApi api)
    {
        this.client = client;
        this.api = api;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(body, BorderLayout.NORTH);
        renderLoading();
    }

    void refresh()
    {
        status.setText("Refreshing...");
        api.action("community_snapshot", currentRsn(), new JsonObject(), json ->
            SwingUtilities.invokeLater(() -> render(json)), error ->
            SwingUtilities.invokeLater(() -> renderError(error)));
    }

    private void renderLoading()
    {
        body.removeAll();
        JPanel identity = card();
        JLabel name = new JLabel(currentRsn().isEmpty() ? "NightLegion member" : currentRsn());
        name.setFont(name.getFont().deriveFont(Font.BOLD, 14f));
        identity.add(name, BorderLayout.NORTH);
        status.setForeground(MUTED);
        identity.add(status, BorderLayout.SOUTH);
        body.add(identity);
        finish();
    }

    private void render(JsonObject snapshot)
    {
        body.removeAll();
        JsonObject rankRoot = object(snapshot, "rank");
        JsonObject profile = object(rankRoot, "profile");
        if (profile.size() == 0)
        {
            renderError("Rank profile is not linked yet.");
            return;
        }

        JsonObject rank = object(profile, "rank");
        JsonObject activity = object(profile, "activity");
        JsonObject weekly = object(profile, "weekly");
        String rsn = text(profile, "rsn", currentRsn());
        double points = decimal(profile, "points", 0);
        int level = integer(rank, "level", 1);
        String title = text(rank, "title", "Quester");

        JPanel identity = card();
        identity.setBorder(BorderFactory.createEmptyBorder(5, 7, 6, 7));
        JLabel player = new JLabel(rsn.isEmpty() ? "NightLegion member" : rsn);
        player.setFont(player.getFont().deriveFont(Font.BOLD, 14f));
        JLabel current = new JLabel("Current rank • L" + level + " · " + title);
        current.setForeground(new Color(170, 170, 170));
        identity.add(player, BorderLayout.NORTH);
        identity.add(current, BorderLayout.SOUTH);
        body.add(identity);
        body.add(Box.createVerticalStrut(3));

        JPanel top = new JPanel(new GridLayout(1, 2, 4, 0));
        top.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.setMaximumSize(new Dimension(Integer.MAX_VALUE, 67));
        top.add(statCard("CURRENT RANK", "L" + level + " · " + title));
        top.add(statCard("CLAN POINTS", format(points)));
        body.add(top);
        body.add(Box.createVerticalStrut(4));

        JPanel nextCard = card();
        nextCard.setLayout(new BoxLayout(nextCard, BoxLayout.Y_AXIS));
        JLabel nextTitle = small("NEXT RANK");
        nextCard.add(nextTitle);
        nextCard.add(Box.createVerticalStrut(3));
        int nextLevel = integer(rank, "next_level", 0);
        if (nextLevel <= 0)
        {
            JLabel max = value("Maximum normal rank reached");
            nextCard.add(max);
        }
        else
        {
            String nextName = text(rank, "next_title", "Next rank");
            double remaining = decimal(rank, "remaining", 0);
            JLabel line = value("L" + nextLevel + " · " + nextName);
            nextCard.add(line);
            JLabel left = new JLabel(format(remaining) + " points remaining");
            left.setForeground(MUTED);
            nextCard.add(left);
            nextCard.add(Box.createVerticalStrut(5));
            double threshold = decimal(rank, "threshold", 0);
            double nextThreshold = decimal(rank, "next_threshold", threshold + 1);
            double span = Math.max(1.0, nextThreshold - threshold);
            double pct = Math.max(0, Math.min(1, (points - threshold) / span));
            JProgressBar progress = new JProgressBar(0, 1000);
            progress.setValue((int) Math.round(pct * 1000));
            progress.setStringPainted(true);
            progress.setString((int) Math.round(pct * 100) + "%");
            progress.setForeground(ACCENT);
            progress.setBackground(new Color(55, 55, 55));
            progress.setBorderPainted(false);
            progress.setMaximumSize(new Dimension(Integer.MAX_VALUE, 17));
            nextCard.add(progress);
        }
        body.add(nextCard);
        body.add(Box.createVerticalStrut(4));

        JPanel activityRow = new JPanel(new GridLayout(1, 2, 4, 0));
        activityRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        activityRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 61));
        activityRow.add(statCard("7 DAYS", String.format(Locale.ROOT, "%.2f", decimal(activity, "7d", 0))));
        activityRow.add(statCard("30 DAYS", String.format(Locale.ROOT, "%.2f", decimal(activity, "30d", 0))));
        body.add(activityRow);
        body.add(Box.createVerticalStrut(4));

        // Do not put this on one long line. This is the exact clipping problem
        // in the previous NightLegion build.
        JPanel week = card();
        week.setLayout(new BoxLayout(week, BoxLayout.Y_AXIS));
        week.add(small("THIS WEEK"));
        week.add(Box.createVerticalStrut(5));
        int discordMessages = integer(weekly, "discord_messages", 0);
        int discordPoints = integer(weekly, "discord_message_points", 0);
        int clanMessages = integer(weekly, "clan_messages", 0);
        int clanPoints = integer(weekly, "clan_message_points", 0);
        JLabel discord = new JLabel("Discord messages:  " + discordMessages + "   (" + discordPoints + "/5 pts)");
        JLabel clan = new JLabel("Clan chat messages:  " + clanMessages + "   (" + clanPoints + "/5 pts)");
        discord.setForeground(new Color(215, 215, 215));
        clan.setForeground(new Color(215, 215, 215));
        week.add(discord);
        week.add(Box.createVerticalStrut(3));
        week.add(clan);
        long xp = longValue(weekly, "wom_xp_gained", 0);
        if (xp > 0)
        {
            week.add(Box.createVerticalStrut(3));
            JLabel osrs = new JLabel("OSRS XP gained:  +" + String.format(Locale.ROOT, "%,d", xp));
            osrs.setForeground(MUTED);
            week.add(osrs);
        }
        body.add(week);
        body.add(Box.createVerticalStrut(6));

        JButton refresh = new JButton("Refresh rank");
        refresh.setBackground(ORANGE);
        refresh.setForeground(Color.WHITE);
        refresh.setFocusPainted(false);
        refresh.setAlignmentX(Component.LEFT_ALIGNMENT);
        refresh.setMaximumSize(new Dimension(Integer.MAX_VALUE, 29));
        refresh.addActionListener(e -> refresh());
        body.add(refresh);
        body.add(Box.createVerticalStrut(5));

        JPanel account = card();
        account.setLayout(new BoxLayout(account, BoxLayout.Y_AXIS));
        account.add(small("ACCOUNT LINK"));
        JLabel linked = new JLabel("Discord + RuneLite linked as " + (rsn.isEmpty() ? currentRsn() : rsn));
        linked.setForeground(MUTED);
        linked.setFont(linked.getFont().deriveFont(10f));
        account.add(linked);
        body.add(account);
        finish();
    }

    private void renderError(String error)
    {
        body.removeAll();
        JPanel card = card();
        JLabel title = new JLabel("Rank profile unavailable");
        title.setFont(title.getFont().deriveFont(Font.BOLD));
        JLabel detail = new JLabel("<html><div style='width:175px'>" + escape(error) + "</div></html>");
        detail.setForeground(MUTED);
        card.add(title, BorderLayout.NORTH);
        card.add(detail, BorderLayout.CENTER);
        body.add(card);
        finish();
    }

    private static JPanel statCard(String label, String text)
    {
        JPanel card = card();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.add(small(label));
        card.add(Box.createVerticalStrut(4));
        card.add(value(text));
        return card;
    }

    private static JPanel card()
    {
        JPanel panel = new JPanel(new BorderLayout(4, 3));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setBackground(new Color(43, 43, 43));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER),
            BorderFactory.createEmptyBorder(6, 7, 6, 7)));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));
        return panel;
    }

    private static JLabel small(String value)
    {
        JLabel label = new JLabel(value);
        label.setForeground(MUTED);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 10f));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private static JLabel value(String value)
    {
        JLabel label = new JLabel(value);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 12f));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private void finish()
    {
        body.revalidate();
        body.repaint();
    }

    private String currentRsn()
    {
        return client.getLocalPlayer() == null || client.getLocalPlayer().getName() == null
            ? "" : client.getLocalPlayer().getName().trim();
    }

    private static JsonObject object(JsonObject parent, String key)
    {
        try { return parent != null && parent.has(key) && parent.get(key).isJsonObject() ? parent.getAsJsonObject(key) : new JsonObject(); }
        catch (Exception ignored) { return new JsonObject(); }
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

    private static long longValue(JsonObject object, String key, long fallback)
    {
        try { return object != null && object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsLong() : fallback; }
        catch (Exception ignored) { return fallback; }
    }

    private static double decimal(JsonObject object, String key, double fallback)
    {
        try { return object != null && object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsDouble() : fallback; }
        catch (Exception ignored) { return fallback; }
    }

    private static String format(double value)
    {
        return Math.rint(value) == value ? String.format(Locale.ROOT, "%,.0f", value) : String.format(Locale.ROOT, "%,.1f", value);
    }

    private static String escape(String value)
    {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
