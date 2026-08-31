package com.nightlegion.livexp;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.client.ui.PluginPanel;

/**
 * Monthly MVP page copied from the proven Live On layout and translated to
 * English. Drops, EHB and EHP are deliberately independent rankings.
 */
final class NightLegionMvpPanel extends PluginPanel
{
    private static final Color GOLD = new Color(214, 174, 52);
    private static final Color SILVER = new Color(170, 176, 185);
    private static final Color BRONZE = new Color(190, 112, 48);
    private static final Color PANEL_BACKGROUND = new Color(36, 36, 36);
    private static final Color GROUP_DARK = new Color(35, 35, 35);
    private static final Color GROUP_LIGHT = new Color(44, 44, 44);
    private static final Color MUTED = new Color(155, 155, 155);
    private static final Color NOTICE_BLUE = new Color(90, 190, 245);

    private final Client client;
    private final NightLegionApi api;
    private final NightLegionLiveXpConfig config;
    private final JPanel dropsEntries = new JPanel();
    private final JPanel ehbEntries = new JPanel();
    private final JPanel ehpEntries = new JPanel();
    private final JPanel participationNotice = new JPanel(new BorderLayout());
    private JsonObject snapshot;
    private String expandedDropPlayer = "";

    NightLegionMvpPanel(Client client, NightLegionApi api, NightLegionLiveXpConfig config)
    {
        super(false);
        this.client = client;
        this.api = api;
        this.config = config;
        setLayout(new BorderLayout());
        setBackground(PANEL_BACKGROUND);

        JTabbedPane sections = new JTabbedPane();
        sections.addTab("Drops", createDropsSection());
        sections.addTab("EHB", createEfficiencySection("EHB", ehbEntries));
        sections.addTab("EHP", createEfficiencySection("EHP", ehpEntries));
        configureTabs(sections);
        add(sections, BorderLayout.CENTER);
    }

    void refresh()
    {
        api.action("community_snapshot", rsn(), new JsonObject(), json -> SwingUtilities.invokeLater(() ->
        {
            snapshot = json;
            participationNotice.setVisible(!config.statsEnabled());
            render();
        }), error -> SwingUtilities.invokeLater(() -> showError(error)));
    }

    private JPanel createDropsSection()
    {
        JPanel section = new JPanel(new BorderLayout());
        section.setBackground(PANEL_BACKGROUND);

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setBackground(PANEL_BACKGROUND);
        top.add(header("MVP DROPS", "Monthly ranking", monthLabel() + "  •  Drops 1M+"));

        participationNotice.setAlignmentX(Component.LEFT_ALIGNMENT);
        participationNotice.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        participationNotice.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(7, 7, 0, 7, PANEL_BACKGROUND),
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(NOTICE_BLUE),
                BorderFactory.createEmptyBorder(5, 7, 5, 7))));
        JLabel participation = new JLabel("<html><b>Participation disabled</b><br>Enable it in settings to register your drops.</html>");
        participation.setForeground(NOTICE_BLUE);
        participationNotice.add(participation, BorderLayout.CENTER);
        top.add(participationNotice);
        section.add(top, BorderLayout.NORTH);

        configureEntries(dropsEntries);
        section.add(scroll(dropsEntries), BorderLayout.CENTER);
        return section;
    }

    private JPanel createEfficiencySection(String metric, JPanel entries)
    {
        JPanel section = new JPanel(new BorderLayout());
        section.setBackground(PANEL_BACKGROUND);
        section.add(header("MVP " + metric, "Monthly ranking", monthLabel() + "  •  Wise Old Man"), BorderLayout.NORTH);
        configureEntries(entries);
        section.add(scroll(entries), BorderLayout.CENTER);
        return section;
    }

    private static void configureEntries(JPanel panel)
    {
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(PANEL_BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(8, 7, 10, 7));
    }

    private static JScrollPane scroll(JPanel panel)
    {
        JScrollPane scroll = new JScrollPane(panel);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(PANEL_BACKGROUND);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.getVerticalScrollBar().setUnitIncrement(12);
        return scroll;
    }

    private static JPanel header(String kickerText, String titleText, String metaText)
    {
        GradientPanel header = new GradientPanel(new Color(47, 44, 34), PANEL_BACKGROUND);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBorder(BorderFactory.createEmptyBorder(11, 12, 10, 12));

        JLabel kicker = new JLabel(kickerText);
        kicker.setForeground(new Color(255, 176, 0));
        kicker.setFont(kicker.getFont().deriveFont(Font.BOLD, 13f));
        JLabel title = new JLabel(titleText);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        JLabel meta = new JLabel(metaText);
        meta.setForeground(new Color(180, 180, 180));
        meta.setFont(meta.getFont().deriveFont(Font.PLAIN, 13f));

        kicker.setAlignmentX(Component.LEFT_ALIGNMENT);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        meta.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.add(kicker);
        header.add(Box.createVerticalStrut(3));
        header.add(title);
        header.add(Box.createVerticalStrut(3));
        header.add(meta);
        return header;
    }

    private static void configureTabs(JTabbedPane sections)
    {
        sections.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        String[] titles = {"Drops", "EHB", "EHP"};
        for (int i = 0; i < titles.length; i++)
        {
            JButton label = new JButton(titles[i]);
            label.setContentAreaFilled(false);
            label.setFocusPainted(false);
            label.setMargin(new java.awt.Insets(0, 4, 0, 4));
            label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            final int tab = i;
            label.addActionListener(e -> sections.setSelectedIndex(tab));
            sections.setTabComponentAt(i, label);
        }
        Runnable update = () ->
        {
            for (int i = 0; i < sections.getTabCount(); i++)
            {
                JButton label = (JButton) sections.getTabComponentAt(i);
                boolean selected = i == sections.getSelectedIndex();
                label.setForeground(selected ? new Color(255, 152, 0) : new Color(210, 210, 210));
                label.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, selected ? 2 : 0, 0, new Color(255, 152, 0)),
                    BorderFactory.createEmptyBorder(5, 3, selected ? 3 : 5, 3)));
            }
        };
        sections.addChangeListener(e -> update.run());
        update.run();
    }

    private void render()
    {
        JsonObject mvp = object(snapshot, "mvp");
        renderDrops(array(mvp, "drops"), object(object(mvp, "own"), "drops"));
        renderEfficiency(ehbEntries, array(mvp, "ehb"), object(object(mvp, "own"), "ehb"), "EHB");
        renderEfficiency(ehpEntries, array(mvp, "ehp"), object(object(mvp, "own"), "ehp"), "EHP");
    }

    private void renderDrops(JsonArray ranking, JsonObject own)
    {
        dropsEntries.removeAll();
        if (ranking.size() == 0)
        {
            dropsEntries.add(empty("No 1M+ drops recorded this month."));
            finish(dropsEntries);
            return;
        }

        long leader = Math.max(1L, longValue(ranking.get(0).getAsJsonObject(), "value", 1L));
        for (int i = 0; i < Math.min(10, ranking.size()); i++)
        {
            JsonObject row = ranking.get(i).getAsJsonObject();
            int position = integer(row, "position", i + 1);
            dropsEntries.add(position == 1 ? dropLeader(row, leader) : dropRow(row, position));
            dropsEntries.add(Box.createVerticalStrut(position < 3 ? 6 : 3));
            if (position == 3 && ranking.size() > 3)
            {
                dropsEntries.add(sectionLabel("RANKING", MUTED));
            }
        }
        addOwnPosition(dropsEntries, ranking, own, true, "Drops");
        finish(dropsEntries);
    }

    private JPanel dropLeader(JsonObject row, long leader)
    {
        GradientPanel card = new GradientPanel(new Color(58, 47, 22), new Color(41, 38, 29));
        card.setLayout(new BorderLayout(8, 6));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 3, 0, 0, GOLD),
            BorderFactory.createEmptyBorder(10, 9, 9, 9)));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));

        JLabel crown = new JLabel("1st", SwingConstants.CENTER);
        crown.setForeground(GOLD);
        crown.setFont(crown.getFont().deriveFont(Font.BOLD, 13f));
        crown.setPreferredSize(new Dimension(30, 28));
        card.add(crown, BorderLayout.WEST);

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        JPanel head = new JPanel(new BorderLayout(5, 0));
        head.setOpaque(false);
        JPanel identity = new JPanel(new GridLayout(0, 1, 0, 1));
        identity.setOpaque(false);
        JLabel name = new JLabel(playerName(row));
        name.setFont(name.getFont().deriveFont(Font.BOLD, 15f));
        JLabel caption = new JLabel("Monthly leader");
        caption.setForeground(new Color(180, 161, 110));
        caption.setFont(caption.getFont().deriveFont(Font.BOLD, 12f));
        identity.add(name);
        identity.add(caption);
        JLabel value = new JLabel(formatGp(longValue(row, "value", 0)), SwingConstants.RIGHT);
        value.setForeground(GOLD);
        value.setFont(value.getFont().deriveFont(Font.BOLD, 13f));
        head.add(identity, BorderLayout.CENTER);
        head.add(value, BorderLayout.EAST);
        content.add(head);
        content.add(Box.createVerticalStrut(7));
        content.add(dropDetails(row));
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private JPanel dropRow(JsonObject row, int position)
    {
        Color accent = position == 2 ? SILVER : position == 3 ? BRONZE : new Color(82, 82, 82);
        Color background = position % 2 == 0 ? GROUP_LIGHT : GROUP_DARK;
        String key = normalize(playerName(row));
        boolean expanded = key.equals(expandedDropPlayer);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(background);
        wrapper.setBorder(BorderFactory.createMatteBorder(0, position <= 3 ? 3 : 2, 0, 0, accent));
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, expanded ? 130 : 56));

        JPanel summary = new JPanel(new BorderLayout(7, 0));
        summary.setBackground(background);
        summary.setBorder(BorderFactory.createEmptyBorder(8, 6, 8, 7));
        JLabel place = new JLabel(position <= 3 ? ordinal(position) : Integer.toString(position), SwingConstants.CENTER);
        place.setForeground(MUTED);
        place.setPreferredSize(new Dimension(28, 20));
        JLabel name = new JLabel(shortName(playerName(row), 16) + (expanded ? "  v" : "  >"));
        name.setToolTipText(playerName(row));
        name.setFont(name.getFont().deriveFont(Font.BOLD, 14f));
        JLabel value = new JLabel(formatGp(longValue(row, "value", 0)));
        value.setForeground(position <= 3 ? accent : new Color(205, 205, 205));
        value.setFont(value.getFont().deriveFont(Font.BOLD, 13f));
        JPanel identity = new JPanel();
        identity.setOpaque(false);
        identity.setLayout(new BoxLayout(identity, BoxLayout.Y_AXIS));
        identity.add(name);
        identity.add(Box.createVerticalStrut(2));
        identity.add(value);
        summary.add(place, BorderLayout.WEST);
        summary.add(identity, BorderLayout.CENTER);
        wrapper.add(summary, BorderLayout.NORTH);
        if (expanded)
        {
            JPanel details = dropDetails(row);
            details.setBorder(BorderFactory.createEmptyBorder(4, 40, 8, 4));
            wrapper.add(details, BorderLayout.CENTER);
        }
        wrapper.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        wrapper.addMouseListener(new java.awt.event.MouseAdapter()
        {
            @Override public void mouseClicked(java.awt.event.MouseEvent e)
            {
                expandedDropPlayer = expanded ? "" : key;
                render();
            }
        });
        return wrapper;
    }

    private JPanel dropDetails(JsonObject row)
    {
        JPanel details = new JPanel();
        details.setOpaque(false);
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
        JsonArray top = array(row, "top_drops");
        if (top.size() == 0)
        {
            JLabel none = new JLabel("Top drops not available yet");
            none.setForeground(MUTED);
            details.add(none);
            return details;
        }
        for (int i = 0; i < Math.min(3, top.size()); i++)
        {
            JsonObject drop = top.get(i).getAsJsonObject();
            String label = integer(drop, "quantity", 1) + "x " + text(drop, "item", "Drop");
            JLabel item = new JLabel(shortName(label, 24));
            item.setToolTipText(label + (text(drop, "source", "").isEmpty() ? "" : " • " + text(drop, "source", "")));
            item.setForeground(new Color(230, 210, 145));
            item.setFont(item.getFont().deriveFont(Font.BOLD, 12f));
            details.add(item);
            if (i + 1 < Math.min(3, top.size())) details.add(Box.createVerticalStrut(3));
        }
        return details;
    }

    private void renderEfficiency(JPanel target, JsonArray ranking, JsonObject own, String metric)
    {
        target.removeAll();
        if (ranking.size() == 0)
        {
            target.add(empty("No monthly " + metric + " gains available from WOM."));
            finish(target);
            return;
        }

        for (int i = 0; i < Math.min(10, ranking.size()); i++)
        {
            JsonObject row = ranking.get(i).getAsJsonObject();
            int position = integer(row, "position", i + 1);
            target.add(efficiencyRow(row, position, metric));
            target.add(Box.createVerticalStrut(position < 3 ? 6 : 3));
            if (position == 3 && ranking.size() > 3)
            {
                target.add(sectionLabel("RANKING", MUTED));
            }
        }
        addOwnPosition(target, ranking, own, false, metric);
        finish(target);
    }

    private JPanel efficiencyRow(JsonObject row, int position, String metric)
    {
        Color accent = position == 1 ? GOLD : position == 2 ? SILVER : position == 3 ? BRONZE : new Color(82, 82, 82);
        Color background = position == 1 ? new Color(48, 42, 29) : position % 2 == 0 ? GROUP_LIGHT : GROUP_DARK;
        JPanel card = new JPanel(new BorderLayout(7, 0));
        card.setBackground(background);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, position <= 3 ? 3 : 2, 0, 0, accent),
            BorderFactory.createEmptyBorder(position == 1 ? 10 : 8, 7, position == 1 ? 10 : 8, 7)));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, position == 1 ? 70 : 56));
        JLabel place = new JLabel(position <= 3 ? ordinal(position) : Integer.toString(position), SwingConstants.CENTER);
        place.setForeground(position == 1 ? GOLD : MUTED);
        place.setPreferredSize(new Dimension(32, 20));
        JLabel name = new JLabel(shortName(playerName(row), 17));
        name.setToolTipText(playerName(row));
        name.setFont(name.getFont().deriveFont(Font.BOLD, position == 1 ? 15f : 14f));
        JLabel value = new JLabel("+" + String.format(Locale.ROOT, "%.2f %s", decimal(row, "value", 0), metric));
        value.setForeground(position <= 3 ? accent : new Color(205, 205, 205));
        value.setFont(value.getFont().deriveFont(Font.BOLD, 12f));
        JPanel identity = new JPanel();
        identity.setOpaque(false);
        identity.setLayout(new BoxLayout(identity, BoxLayout.Y_AXIS));
        identity.add(name);
        if (position == 1)
        {
            JLabel caption = new JLabel("Monthly leader");
            caption.setForeground(new Color(180, 161, 110));
            caption.setFont(caption.getFont().deriveFont(11f));
            identity.add(caption);
        }
        card.add(place, BorderLayout.WEST);
        card.add(identity, BorderLayout.CENTER);
        card.add(value, BorderLayout.EAST);
        return card;
    }

    private void addOwnPosition(JPanel target, JsonArray top, JsonObject own, boolean drops, String metric)
    {
        if (own == null || own.size() == 0)
        {
            return;
        }
        int position = integer(own, "position", 0);
        if (position <= 0 || position <= Math.min(10, top.size()))
        {
            return;
        }
        target.add(Box.createVerticalStrut(10));
        target.add(sectionLabel("YOUR POSITION", NOTICE_BLUE));
        JPanel row = new JPanel(new BorderLayout(7, 0));
        row.setBackground(new Color(31, 53, 65));
        row.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 3, 0, 0, NOTICE_BLUE),
            BorderFactory.createEmptyBorder(9, 7, 9, 7)));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
        JLabel place = new JLabel(Integer.toString(position), SwingConstants.CENTER);
        place.setForeground(new Color(200, 220, 230));
        place.setPreferredSize(new Dimension(32, 20));
        JLabel name = new JLabel(shortName(playerName(own), 17));
        name.setFont(name.getFont().deriveFont(Font.BOLD, 14f));
        JLabel value = new JLabel(drops ? formatGp(longValue(own, "value", 0)) : "+" + String.format(Locale.ROOT, "%.2f %s", decimal(own, "value", 0), metric));
        value.setForeground(NOTICE_BLUE);
        value.setFont(value.getFont().deriveFont(Font.BOLD, 12f));
        row.add(place, BorderLayout.WEST);
        row.add(name, BorderLayout.CENTER);
        row.add(value, BorderLayout.EAST);
        target.add(row);
    }

    private static JLabel sectionLabel(String text, Color color)
    {
        JLabel label = new JLabel(text);
        label.setForeground(color);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 12f));
        label.setBorder(BorderFactory.createEmptyBorder(3, 4, 6, 0));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private static JLabel empty(String message)
    {
        JLabel empty = new JLabel("<html><center>" + message + "</center></html>", SwingConstants.CENTER);
        empty.setForeground(new Color(160, 160, 160));
        empty.setBorder(BorderFactory.createEmptyBorder(24, 4, 4, 4));
        empty.setAlignmentX(Component.LEFT_ALIGNMENT);
        empty.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        return empty;
    }

    private void showError(String error)
    {
        dropsEntries.removeAll();
        dropsEntries.add(empty(error));
        ehbEntries.removeAll();
        ehbEntries.add(empty(error));
        ehpEntries.removeAll();
        ehpEntries.add(empty(error));
        finish(dropsEntries);
        finish(ehbEntries);
        finish(ehpEntries);
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

    private static String monthLabel()
    {
        YearMonth month = YearMonth.now();
        String name = month.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        return Character.toUpperCase(name.charAt(0)) + name.substring(1) + " " + month.getYear();
    }

    private static String playerName(JsonObject row)
    {
        return text(row, "rsn", text(row, "name", "Unknown"));
    }

    private static String ordinal(int n)
    {
        if (n == 1) return "1st";
        if (n == 2) return "2nd";
        if (n == 3) return "3rd";
        return Integer.toString(n);
    }

    private static String formatGp(long value)
    {
        if (value >= 1_000_000_000L) return String.format(Locale.ROOT, "%.2fB", value / 1_000_000_000d);
        if (value >= 1_000_000L) return String.format(Locale.ROOT, "%.1fM", value / 1_000_000d);
        if (value >= 1_000L) return String.format(Locale.ROOT, "%.1fK", value / 1_000d);
        return Long.toString(value);
    }

    private static String normalize(String value)
    {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String shortName(String value, int max)
    {
        if (value == null) return "";
        return value.length() <= max ? value : value.substring(0, Math.max(1, max - 1)) + "…";
    }

    private static JsonObject object(JsonObject parent, String key)
    {
        try { return parent != null && parent.has(key) && parent.get(key).isJsonObject() ? parent.getAsJsonObject(key) : new JsonObject(); }
        catch (Exception ignored) { return new JsonObject(); }
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

    private static final class GradientPanel extends JPanel
    {
        private final Color start;
        private final Color end;
        private GradientPanel(Color start, Color end)
        {
            this.start = start;
            this.end = end;
            setOpaque(false);
        }
        @Override protected void paintComponent(Graphics graphics)
        {
            Graphics2D copy = (Graphics2D) graphics.create();
            copy.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            copy.setPaint(new GradientPaint(0, 0, start, getWidth(), getHeight(), end));
            copy.fillRect(0, 0, getWidth(), getHeight());
            copy.dispose();
            super.paintComponent(graphics);
        }
    }
}
