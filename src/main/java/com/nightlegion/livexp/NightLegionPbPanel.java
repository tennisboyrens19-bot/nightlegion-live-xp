package com.nightlegion.livexp;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.client.ui.PluginPanel;

/** Live On PB page, translated to English and backed by NightLegion PB data. */
final class NightLegionPbPanel extends PluginPanel
{
    private static final Color ORANGE = new Color(255, 152, 0);
    private static final Color BLUE = new Color(90, 190, 245);
    private static final Color GOLD = new Color(214, 174, 52);
    private static final Color SILVER = new Color(170, 176, 185);
    private static final Color BRONZE = new Color(190, 112, 48);
    private static final Color MUTED = new Color(145, 145, 145);

    private final Client client;
    private final NightLegionApi api;
    private final NightLegionLiveXpConfig config;
    private final JButton tutorialToggle = new JButton("▸ How to register your PBs?");
    private final JPanel tutorial = new JPanel();
    private final JPanel participationNotice = new JPanel(new BorderLayout());
    private final JTextField search = new JTextField("Search");
    private final JComboBox<String> raids = new JComboBox<>();
    private final JComboBox<String> bosses = new JComboBox<>();
    private final JPanel ranking = new JPanel();
    private final JLabel ownPb = new JLabel(" ", SwingConstants.CENTER);
    private JsonArray boards = new JsonArray();
    private boolean updating;

    NightLegionPbPanel(Client client, NightLegionApi api, NightLegionLiveXpConfig config)
    {
        super(false);
        this.client = client;
        this.api = api;
        this.config = config;
        setLayout(new BorderLayout(0, 7));
        setBorder(BorderFactory.createEmptyBorder(7, 5, 5, 5));

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        configureTutorial();
        top.add(tutorialToggle);
        top.add(tutorial);
        top.add(Box.createVerticalStrut(7));
        configureParticipationNotice();
        top.add(participationNotice);
        top.add(Box.createVerticalStrut(7));

        JPanel titleRow = new JPanel(new BorderLayout(4, 0));
        titleRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        JLabel title = new JLabel("BEST CLAN TIMES");
        title.setForeground(ORANGE);
        titleRow.add(title, BorderLayout.CENTER);
        JButton refresh = new JButton("Refresh");
        refresh.setMargin(new java.awt.Insets(1, 7, 1, 7));
        refresh.addActionListener(e -> refresh());
        titleRow.add(refresh, BorderLayout.EAST);
        top.add(titleRow);
        top.add(Box.createVerticalStrut(6));

        configureSearchPrompt();
        search.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        search.setAlignmentX(Component.LEFT_ALIGNMENT);
        search.setToolTipText("Search bosses, challenges and raids");
        search.addActionListener(e -> selectFromSearch());
        top.add(search);
        top.add(Box.createVerticalStrut(6));

        JLabel selection = new JLabel("Or select from the menus below");
        selection.setForeground(MUTED);
        selection.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.add(selection);
        top.add(Box.createVerticalStrut(3));

        configureCombo(raids, "Raids");
        configureCombo(bosses, "Bosses & challenges");
        raids.addActionListener(e ->
        {
            if (updating || raids.getSelectedItem() == null) return;
            updating = true;
            bosses.setSelectedItem(null);
            updating = false;
            renderSelected(String.valueOf(raids.getSelectedItem()));
        });
        bosses.addActionListener(e ->
        {
            if (updating || bosses.getSelectedItem() == null) return;
            updating = true;
            raids.setSelectedItem(null);
            updating = false;
            renderSelected(String.valueOf(bosses.getSelectedItem()));
        });
        top.add(raids);
        top.add(Box.createVerticalStrut(6));
        top.add(bosses);

        ownPb.setForeground(BLUE);
        ownPb.setAlignmentX(Component.LEFT_ALIGNMENT);
        ownPb.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        ownPb.setBorder(BorderFactory.createEmptyBorder(5, 2, 4, 2));
        top.add(ownPb);

        tutorialToggle.addActionListener(e ->
        {
            tutorial.setVisible(!tutorial.isVisible());
            tutorialToggle.setText((tutorial.isVisible() ? "▾ " : "▸ ") + "How to register your PBs?");
            revalidate();
        });
        add(top, BorderLayout.NORTH);

        ranking.setLayout(new BoxLayout(ranking, BoxLayout.Y_AXIS));
        JScrollPane scroll = new JScrollPane(ranking);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.getVerticalScrollBar().setUnitIncrement(12);
        add(scroll, BorderLayout.CENTER);
    }

    void refresh()
    {
        api.action("community_snapshot", rsn(), new JsonObject(), json -> SwingUtilities.invokeLater(() ->
        {
            boards = array(json, "pbs");
            participationNotice.setVisible(!config.pbRankingEnabled());
            rebuildMenus();
        }), error -> SwingUtilities.invokeLater(() ->
        {
            ranking.removeAll();
            ranking.add(empty(error));
            ranking.revalidate();
            ranking.repaint();
        }));
    }

    private void configureTutorial()
    {
        tutorialToggle.setAlignmentX(Component.LEFT_ALIGNMENT);
        tutorialToggle.setMaximumSize(new Dimension(Integer.MAX_VALUE, 27));
        tutorialToggle.setHorizontalAlignment(SwingConstants.LEFT);
        tutorial.setLayout(new BoxLayout(tutorial, BoxLayout.Y_AXIS));
        tutorial.setAlignmentX(Component.LEFT_ALIGNMENT);
        tutorial.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(62, 62, 62)),
            BorderFactory.createEmptyBorder(6, 7, 6, 7)));
        tutorial.setMaximumSize(new Dimension(Integer.MAX_VALUE, 105));
        JLabel text = new JLabel("<html>Keep <b>Participate in PB rankings</b> enabled.<br>"
            + "NightLegion reads your visible personal-best times from supported RuneLite/game surfaces and submits improvements automatically.<br><br>"
            + "Open a supported boss/raid PB screen if a time has not synced yet.</html>");
        text.setForeground(new Color(190, 190, 190));
        tutorial.add(text);
        tutorial.setVisible(false);
    }

    private void configureParticipationNotice()
    {
        participationNotice.setAlignmentX(Component.LEFT_ALIGNMENT);
        participationNotice.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
        participationNotice.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BLUE),
            BorderFactory.createEmptyBorder(5, 7, 5, 7)));
        JLabel message = new JLabel("<html><b>Participation disabled</b><br>Enable it in settings to register your PBs.</html>");
        message.setForeground(BLUE);
        participationNotice.add(message, BorderLayout.CENTER);
    }

    private static void configureCombo(JComboBox<String> combo, String prompt)
    {
        combo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        combo.setAlignmentX(Component.LEFT_ALIGNMENT);
        combo.setRenderer((list, value, index, selected, focused) ->
        {
            JLabel label = (JLabel) new javax.swing.DefaultListCellRenderer()
                .getListCellRendererComponent(list, value, index, selected, focused);
            if (value == null)
            {
                label.setText(prompt);
                label.setForeground(new Color(155, 155, 155));
            }
            return label;
        });
    }

    private void configureSearchPrompt()
    {
        Color normal = search.getForeground();
        Color prompt = new Color(145, 145, 145);
        search.setForeground(prompt);
        search.addFocusListener(new java.awt.event.FocusAdapter()
        {
            @Override public void focusGained(java.awt.event.FocusEvent e)
            {
                if ("Search".equals(search.getText()))
                {
                    search.setText("");
                    search.setForeground(normal);
                }
            }
            @Override public void focusLost(java.awt.event.FocusEvent e)
            {
                if (search.getText().trim().isEmpty())
                {
                    search.setText("Search");
                    search.setForeground(prompt);
                }
            }
        });
    }

    private void rebuildMenus()
    {
        Set<String> raidNames = new LinkedHashSet<>();
        Set<String> bossNames = new LinkedHashSet<>();
        for (JsonElement element : boards)
        {
            if (!element.isJsonObject()) continue;
            String name = text(element.getAsJsonObject(), "category", "");
            if (name.isEmpty()) continue;
            if (isRaid(name)) raidNames.add(name); else bossNames.add(name);
        }

        updating = true;
        raids.setModel(new DefaultComboBoxModel<>(raidNames.toArray(new String[0])));
        bosses.setModel(new DefaultComboBoxModel<>(bossNames.toArray(new String[0])));
        raids.setSelectedItem(null);
        bosses.setSelectedItem(null);
        updating = false;
        ownPb.setText(" ");
        ranking.removeAll();
        ranking.add(empty(boards.size() == 0 ? "No clan PBs have synced yet." : "Choose a boss, challenge or raid above."));
        ranking.revalidate();
        ranking.repaint();
    }

    private void selectFromSearch()
    {
        String query = normalize(search.getText());
        if (query.isEmpty() || "search".equals(query)) return;
        String best = "";
        for (JsonElement element : boards)
        {
            if (!element.isJsonObject()) continue;
            String category = text(element.getAsJsonObject(), "category", "");
            if (normalize(category).contains(query))
            {
                best = category;
                break;
            }
        }
        if (best.isEmpty()) return;
        updating = true;
        if (isRaid(best))
        {
            raids.setSelectedItem(best);
            bosses.setSelectedItem(null);
        }
        else
        {
            bosses.setSelectedItem(best);
            raids.setSelectedItem(null);
        }
        updating = false;
        renderSelected(best);
    }

    private void renderSelected(String category)
    {
        JsonObject board = findBoard(category);
        ranking.removeAll();
        ownPb.setText(" ");
        if (board == null)
        {
            ranking.add(empty("No times for this category."));
            finish();
            return;
        }
        JsonArray rows = array(board, "rows");
        List<JsonObject> values = new ArrayList<>();
        for (JsonElement element : rows)
        {
            if (element.isJsonObject()) values.add(element.getAsJsonObject());
        }
        values.sort(Comparator.comparingDouble(v -> decimal(v, "seconds", Double.MAX_VALUE)));
        if (values.isEmpty())
        {
            ranking.add(empty("No times for this category."));
            finish();
            return;
        }
        String local = normalize(rsn());
        for (int i = 0; i < values.size(); i++)
        {
            JsonObject row = values.get(i);
            ranking.add(rankRow(i + 1, row));
            if (i + 1 < values.size()) ranking.add(Box.createVerticalStrut(3));
            if (normalize(text(row, "rsn", "")).equals(local))
            {
                ownPb.setText("Your PB: " + formatTime(decimal(row, "seconds", 0)) + " · #" + (i + 1));
            }
        }
        if (ownPb.getText().trim().isEmpty()) ownPb.setText("No PB synced for this category");
        finish();
    }

    private JPanel rankRow(int position, JsonObject row)
    {
        Color accent = position == 1 ? GOLD : position == 2 ? SILVER : position == 3 ? BRONZE : new Color(82, 82, 82);
        JPanel panel = new JPanel(new BorderLayout(7, 0));
        panel.setBackground(position % 2 == 0 ? new Color(44, 44, 44) : new Color(35, 35, 35));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, position <= 3 ? 3 : 2, 0, 0, accent),
            BorderFactory.createEmptyBorder(8, 6, 8, 7)));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        JLabel place = new JLabel(position <= 3 ? medalLabel(position) : Integer.toString(position), SwingConstants.CENTER);
        place.setForeground(position <= 3 ? accent : MUTED);
        place.setPreferredSize(new Dimension(32, 20));
        String name = text(row, "rsn", text(row, "name", "Unknown"));
        JLabel player = new JLabel(shortName(name, 17));
        player.setToolTipText(name);
        player.setFont(player.getFont().deriveFont(Font.BOLD, 13f));
        JLabel value = new JLabel(formatTime(decimal(row, "seconds", 0)));
        value.setForeground(position <= 3 ? accent : new Color(205, 205, 205));
        value.setFont(value.getFont().deriveFont(Font.BOLD, 12f));
        panel.add(place, BorderLayout.WEST);
        panel.add(player, BorderLayout.CENTER);
        panel.add(value, BorderLayout.EAST);
        return panel;
    }

    private JsonObject findBoard(String category)
    {
        for (JsonElement element : boards)
        {
            if (element.isJsonObject() && text(element.getAsJsonObject(), "category", "").equals(category))
                return element.getAsJsonObject();
        }
        return null;
    }

    private static boolean isRaid(String value)
    {
        String v = normalize(value);
        return v.contains("chambers of xeric") || v.contains("cox")
            || v.contains("theatre of blood") || v.contains("tob")
            || v.contains("tombs of amascut") || v.contains("toa")
            || v.contains("raid");
    }

    private static String medalLabel(int position)
    {
        if (position == 1) return "1st";
        if (position == 2) return "2nd";
        if (position == 3) return "3rd";
        return Integer.toString(position);
    }

    private static String formatTime(double seconds)
    {
        long cs = Math.round(Math.max(0, seconds) * 100.0);
        long hours = cs / 360000;
        long minutes = (cs / 6000) % 60;
        long secs = (cs / 100) % 60;
        long fraction = cs % 100;
        return hours > 0
            ? String.format(Locale.ROOT, "%d:%02d:%02d.%02d", hours, minutes, secs, fraction)
            : String.format(Locale.ROOT, "%d:%02d.%02d", minutes, secs, fraction);
    }

    private static JLabel empty(String message)
    {
        JLabel label = new JLabel("<html><center>" + message + "</center></html>", SwingConstants.CENTER);
        label.setForeground(new Color(155, 155, 155));
        label.setBorder(BorderFactory.createEmptyBorder(18, 5, 5, 5));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));
        return label;
    }

    private void finish()
    {
        ranking.revalidate();
        ranking.repaint();
    }

    private String rsn()
    {
        return client.getLocalPlayer() == null || client.getLocalPlayer().getName() == null
            ? "" : client.getLocalPlayer().getName().trim();
    }

    private static String shortName(String value, int max)
    {
        if (value == null) return "";
        return value.length() <= max ? value : value.substring(0, Math.max(1, max - 1)) + "…";
    }

    private static String normalize(String value)
    {
        return value == null ? "" : value.replace('_', ' ').trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
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

    private static double decimal(JsonObject object, String key, double fallback)
    {
        try { return object != null && object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsDouble() : fallback; }
        catch (Exception ignored) { return fallback; }
    }
}
