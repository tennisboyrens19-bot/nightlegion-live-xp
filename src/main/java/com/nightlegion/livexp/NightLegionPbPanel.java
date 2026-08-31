package com.nightlegion.livexp;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.JTextField;
import net.runelite.api.Client;
import net.runelite.client.ui.PluginPanel;

/**
 * Live On PB panel ported directly to NightLegion. The layout, search/filter
 * flow and narrow-sidebar sizing follow the original source; labels are English.
 */
final class NightLegionPbPanel extends PluginPanel
{
    private static final Color ORANGE = new Color(255, 152, 0);
    private static final Color BLUE = new Color(90, 190, 245);

    private final Client client;
    private final NightLegionApi api;
    private final NightLegionLiveXpConfig config;

    private final JTextField globalSearch = new JTextField("Search");
    private final JButton clearSearch = new JButton("×");
    private final JPopupMenu searchSuggestions = new JPopupMenu();
    private final JComboBox<String> raids = new JComboBox<>();
    private final JComboBox<String> bosses = new JComboBox<>();
    private final JComboBox<String> modes = new JComboBox<>();
    private final JComboBox<String> teams = new JComboBox<>();
    private final JComboBox<String> timeTypes = new JComboBox<>();
    private final JPanel filters = new JPanel(new GridLayout(1, 2, 5, 0));
    private final JPanel filterStack = new JPanel();
    private final JPanel raidsGroup = new JPanel();
    private final JPanel bossesGroup = new JPanel();
    private final List<Category> availableCategories = new ArrayList<>();
    private final JPanel ranking = new VerticalRankingPanel();
    private final JScrollPane rankingScroll = new JScrollPane(ranking);
    private final JLabel ownPb = new JLabel("No PB synced", SwingConstants.CENTER);
    private final JPanel tutorial = new JPanel();
    private final JButton tutorialToggle = new JButton("▾ How to register your PBs?");
    private final JPanel participationNotice = new JPanel(new BorderLayout());
    private final JButton refresh = new JButton("↻");

    private boolean updatingFilters;
    private boolean updatingGlobalSearch;

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

        JLabel title = new JLabel("BEST CLAN TIMES");
        title.setForeground(ORANGE);
        JPanel titleRow = new JPanel(new BorderLayout(4, 0));
        titleRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        titleRow.add(title, BorderLayout.CENTER);
        refresh.setToolTipText("Refresh ranking");
        refresh.setMargin(new java.awt.Insets(1, 7, 1, 7));
        refresh.setPreferredSize(new Dimension(32, 26));
        refresh.addActionListener(event -> refresh());
        titleRow.add(refresh, BorderLayout.EAST);
        top.add(titleRow);
        top.add(Box.createVerticalStrut(6));

        globalSearch.setToolTipText("Search bosses, challenges and raids");
        configureSearchPrompt(globalSearch, "Search");
        configureGlobalAutocomplete();
        clearSearch.setToolTipText("Clear search");
        clearSearch.setMargin(new java.awt.Insets(1, 7, 1, 7));
        clearSearch.setPreferredSize(new Dimension(30, 28));
        clearSearch.setVisible(false);
        clearSearch.addActionListener(event -> clearGlobalSearch());
        JPanel searchRow = new JPanel(new BorderLayout(3, 0));
        searchRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        searchRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        searchRow.add(globalSearch, BorderLayout.CENTER);
        searchRow.add(clearSearch, BorderLayout.EAST);
        top.add(searchRow);
        top.add(Box.createVerticalStrut(6));

        JLabel selectionLabel = new JLabel("Or select from the menus below");
        selectionLabel.setForeground(new Color(145, 145, 145));
        selectionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.add(selectionLabel);
        top.add(Box.createVerticalStrut(3));

        raids.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        raids.setAlignmentX(Component.LEFT_ALIGNMENT);
        raids.setToolTipText("Select a raid");
        configureComboPlaceholder(raids, "Raids");
        raids.addActionListener(event ->
        {
            if (updatingFilters || raids.getSelectedItem() == null) return;
            updatingFilters = true;
            bosses.setSelectedItem(null);
            updatingFilters = false;
            clearSearchAfterMenuSelection();
            positionFilters(true);
            rebuildCategoryFilters();
        });
        raidsGroup.setLayout(new BoxLayout(raidsGroup, BoxLayout.Y_AXIS));
        raidsGroup.setAlignmentX(Component.LEFT_ALIGNMENT);
        raidsGroup.add(raids);
        top.add(raidsGroup);
        top.add(Box.createVerticalStrut(6));

        javax.swing.JSeparator categorySeparator = new javax.swing.JSeparator();
        categorySeparator.setForeground(new Color(62, 62, 62));
        categorySeparator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        categorySeparator.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.add(categorySeparator);
        top.add(Box.createVerticalStrut(6));

        bosses.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        bosses.setAlignmentX(Component.LEFT_ALIGNMENT);
        bosses.setToolTipText("Select a boss or challenge");
        configureComboPlaceholder(bosses, "Bosses & challenges");
        bosses.addActionListener(event ->
        {
            if (updatingFilters || bosses.getSelectedItem() == null) return;
            updatingFilters = true;
            raids.setSelectedItem(null);
            updatingFilters = false;
            clearSearchAfterMenuSelection();
            positionFilters(false);
            rebuildCategoryFilters();
        });

        modes.addActionListener(event ->
        {
            if (!updatingFilters)
            {
                rebuildTeamFilter();
                renderSelection();
            }
        });
        teams.addActionListener(event ->
        {
            if (updatingFilters) return;
            String boss = selectedActivity();
            String mode = modes.isVisible() ? selectedText(modes) : singleMode(boss);
            rebuildTimeTypeFilter(boss, mode, parseTeamLabel(selectedText(teams)));
            renderSelection();
        });
        timeTypes.addActionListener(event ->
        {
            if (!updatingFilters) renderSelection();
        });

        bossesGroup.setLayout(new BoxLayout(bossesGroup, BoxLayout.Y_AXIS));
        bossesGroup.setAlignmentX(Component.LEFT_ALIGNMENT);
        bossesGroup.add(bosses);
        top.add(bossesGroup);

        filterStack.setLayout(new BoxLayout(filterStack, BoxLayout.Y_AXIS));
        filterStack.setAlignmentX(Component.LEFT_ALIGNMENT);
        filterStack.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        filterStack.setMaximumSize(new Dimension(Integer.MAX_VALUE, 66));
        filters.setAlignmentX(Component.LEFT_ALIGNMENT);
        filters.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        filters.add(modes);
        filters.add(teams);
        filters.setVisible(false);
        timeTypes.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        timeTypes.setAlignmentX(Component.LEFT_ALIGNMENT);
        timeTypes.setVisible(false);
        filterStack.add(filters);
        filterStack.add(Box.createVerticalStrut(5));
        filterStack.add(timeTypes);
        filterStack.setVisible(false);
        bossesGroup.add(filterStack);

        ownPb.setForeground(BLUE);
        ownPb.setAlignmentX(Component.LEFT_ALIGNMENT);
        ownPb.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        ownPb.setBorder(BorderFactory.createEmptyBorder(5, 2, 4, 2));
        top.add(ownPb);

        tutorialToggle.addActionListener(event ->
        {
            tutorial.setVisible(!tutorial.isVisible());
            tutorialToggle.setText((tutorial.isVisible() ? "▾ " : "▸ ") + "How to register your PBs?");
            revalidate();
        });
        add(top, BorderLayout.NORTH);

        ranking.setLayout(new BoxLayout(ranking, BoxLayout.Y_AXIS));
        rankingScroll.setBorder(BorderFactory.createEmptyBorder());
        rankingScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        rankingScroll.getVerticalScrollBar().setUnitIncrement(24);
        add(rankingScroll, BorderLayout.CENTER);
    }

    void refresh()
    {
        refresh.setEnabled(false);
        api.action("community_snapshot", rsn(), new JsonObject(), json -> SwingUtilities.invokeLater(() ->
        {
            refresh.setEnabled(true);
            participationNotice.setVisible(!config.pbRankingEnabled());
            availableCategories.clear();
            for (JsonElement element : array(json, "pbs"))
            {
                if (!element.isJsonObject()) continue;
                JsonObject raw = element.getAsJsonObject();
                Category category = new Category();
                category.category = text(raw, "category", "PB");
                category.boss = text(raw, "boss", category.category);
                category.mode = text(raw, "mode", "");
                category.teamSize = integer(raw, "team_size", 0);
                category.timeType = text(raw, "time_type", "");
                category.rows = array(raw, "rows");
                category.own = object(raw, "own");
                availableCategories.add(category);
            }
            rebuildBossLists();
        }), error -> SwingUtilities.invokeLater(() ->
        {
            refresh.setEnabled(true);
            ranking.removeAll();
            ranking.add(centered("Could not load PBs: " + escape(error)));
            ranking.revalidate();
            ranking.repaint();
        }));
    }

    private void configureParticipationNotice()
    {
        participationNotice.setAlignmentX(Component.LEFT_ALIGNMENT);
        participationNotice.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
        participationNotice.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BLUE),
            BorderFactory.createEmptyBorder(5, 7, 5, 7)));
        JLabel message = new JLabel("<html><b>Participation disabled</b><br>Enable it in settings<br>to register your PBs.</html>");
        message.setForeground(BLUE);
        participationNotice.add(message, BorderLayout.CENTER);
    }

    private void configureTutorial()
    {
        tutorial.setLayout(new BoxLayout(tutorial, BoxLayout.Y_AXIS));
        tutorial.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 3, 0, 0, BLUE),
            BorderFactory.createEmptyBorder(6, 8, 6, 5)));
        JLabel instructions = new JLabel("<html><div style='width:160px'>"
            + "1. Open your POH <b>Adventure Log</b> to import your times.<br><br>"
            + "2. In <b>Combat Achievements</b>, open the boss page you want to register.<br><br>"
            + "3. Supported scoreboards are also detected.<br><br>"
            + "With <b>Participate in PB rankings</b> enabled, new PBs are submitted automatically."
            + "</div></html>");
        instructions.setAlignmentX(Component.LEFT_ALIGNMENT);
        instructions.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        tutorial.add(instructions);
        tutorial.setVisible(true);
        tutorialToggle.setAlignmentX(Component.LEFT_ALIGNMENT);
        tutorialToggle.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
    }

    private static void configureSearchPrompt(JTextField field, String prompt)
    {
        Color normalColor = field.getForeground();
        Color promptColor = new Color(145, 145, 145);
        field.addFocusListener(new java.awt.event.FocusAdapter()
        {
            @Override
            public void focusGained(java.awt.event.FocusEvent event)
            {
                if (prompt.equals(field.getText()))
                {
                    field.setText("");
                    field.setForeground(normalColor);
                }
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent event)
            {
                if (field.getText().trim().isEmpty())
                {
                    field.setText(prompt);
                    field.setForeground(promptColor);
                }
            }
        });
        field.setForeground(promptColor);
        field.setText(prompt);
    }

    private static void configureComboPlaceholder(JComboBox<String> combo, String prompt)
    {
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

    private void configureGlobalAutocomplete()
    {
        searchSuggestions.setFocusable(false);
        globalSearch.getDocument().addDocumentListener(new javax.swing.event.DocumentListener()
        {
            private void changed()
            {
                if (updatingGlobalSearch) return;
                String query = globalSearch.getText();
                if ("Search".equals(query)) return;
                SwingUtilities.invokeLater(() -> filterGlobalSuggestions(query));
            }

            @Override public void insertUpdate(javax.swing.event.DocumentEvent event) { changed(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent event) { changed(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent event) { changed(); }
        });
        globalSearch.addActionListener(event -> selectGlobalSearchResult(globalSearch.getText()));
    }

    private void filterGlobalSuggestions(String query)
    {
        if (updatingGlobalSearch) return;
        String normalized = normalizeSearch(query);
        searchSuggestions.setVisible(false);
        searchSuggestions.removeAll();
        clearSearch.setVisible(!normalized.isEmpty());
        if (normalized.isEmpty()) return;

        String alias = bossAlias(normalized);
        Set<String> suggestions = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (Category category : availableCategories)
        {
            String candidate = safe(category.boss);
            String normalizedCandidate = normalizeSearch(candidate);
            if (normalizedCandidate.contains(normalized) || normalizedCandidate.contains(alias))
            {
                suggestions.add(candidate);
            }
            if (suggestions.size() >= 5) break;
        }
        for (String suggestion : suggestions)
        {
            JMenuItem item = new JMenuItem(suggestion);
            item.setFocusable(false);
            item.setRequestFocusEnabled(false);
            item.addActionListener(event -> selectGlobalSearchResult(suggestion));
            searchSuggestions.add(item);
        }
        if (!suggestions.isEmpty() && globalSearch.isShowing())
        {
            searchSuggestions.show(globalSearch, 0, globalSearch.getHeight());
            globalSearch.requestFocusInWindow();
        }
    }

    private void selectGlobalSearchResult(String query)
    {
        if (updatingGlobalSearch) return;
        String resolved = resolveBossName(query);
        if (resolved.isEmpty() || !containsBoss(resolved)) return;
        updatingGlobalSearch = true;
        globalSearch.setForeground(javax.swing.UIManager.getColor("TextField.foreground"));
        globalSearch.setText(resolved);
        updatingGlobalSearch = false;
        clearSearch.setVisible(true);
        searchSuggestions.setVisible(false);

        updatingFilters = true;
        if (isRaid(resolved))
        {
            raids.setSelectedItem(resolved);
            bosses.setSelectedItem(null);
        }
        else
        {
            bosses.setSelectedItem(resolved);
            raids.setSelectedItem(null);
        }
        updatingFilters = false;
        positionFilters(isRaid(resolved));
        rebuildCategoryFilters();
    }

    private void positionFilters(boolean belowRaids)
    {
        java.awt.Container parent = filterStack.getParent();
        if (parent != null) parent.remove(filterStack);
        JPanel target = belowRaids ? raidsGroup : bossesGroup;
        target.add(filterStack);
        target.revalidate();
        target.repaint();
    }

    private void clearGlobalSearch()
    {
        updatingGlobalSearch = true;
        globalSearch.setText("");
        globalSearch.requestFocusInWindow();
        updatingGlobalSearch = false;
        clearSearch.setVisible(false);
        searchSuggestions.setVisible(false);
    }

    private void clearSearchAfterMenuSelection()
    {
        updatingGlobalSearch = true;
        if (globalSearch.hasFocus())
        {
            globalSearch.setText("");
            globalSearch.setForeground(javax.swing.UIManager.getColor("TextField.foreground"));
        }
        else
        {
            globalSearch.setText("Search");
            globalSearch.setForeground(new Color(145, 145, 145));
        }
        updatingGlobalSearch = false;
        clearSearch.setVisible(false);
        searchSuggestions.setVisible(false);
    }

    private boolean containsBoss(String boss)
    {
        for (Category category : availableCategories)
        {
            if (safe(category.boss).equalsIgnoreCase(boss)) return true;
        }
        return false;
    }

    private void rebuildBossLists()
    {
        String previousBoss = selectedText(bosses);
        String previousRaid = selectedText(raids);
        Set<String> bossNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        Set<String> raidNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (Category value : availableCategories)
        {
            if (value.boss == null) continue;
            if (isRaid(value.boss)) raidNames.add(value.boss);
            else bossNames.add(value.boss);
        }
        updatingFilters = true;
        bosses.setModel(new DefaultComboBoxModel<>(bossNames.toArray(new String[0])));
        raids.setModel(new DefaultComboBoxModel<>(raidNames.toArray(new String[0])));
        bosses.setSelectedItem(bossNames.contains(previousBoss) ? previousBoss : null);
        raids.setSelectedItem(raidNames.contains(previousRaid) ? previousRaid : null);
        updatingFilters = false;

        if (bossNames.isEmpty() && raidNames.isEmpty())
        {
            ranking.removeAll();
            ranking.add(centered("No PBs synced yet."));
            setTutorialExpanded(true);
            ownPb.setText("<html><div style='text-align:center'>Open the Adventure Log<br>to import your PBs</div></html>");
            ranking.revalidate();
            ranking.repaint();
            return;
        }

        if (selectedActivity().isEmpty())
        {
            ranking.removeAll();
            ranking.add(centered("Choose a raid, boss or challenge above."));
            ranking.revalidate();
            ranking.repaint();
        }
        else
        {
            rebuildCategoryFilters();
        }
    }

    private static boolean isRaid(String boss)
    {
        String normalized = safe(boss).toLowerCase(Locale.ROOT);
        return normalized.contains("chambers of xeric")
            || normalized.contains("theatre of blood")
            || normalized.contains("tombs of amascut");
    }

    private void rebuildCategoryFilters()
    {
        String boss = selectedActivity();
        Set<String> values = new LinkedHashSet<>();
        for (Category category : availableCategories)
        {
            if (safe(category.boss).equalsIgnoreCase(boss)) values.add(safe(category.mode));
        }
        updatingFilters = true;
        modes.setModel(new DefaultComboBoxModel<>(values.toArray(new String[0])));
        modes.setVisible(values.size() > 1 || (values.size() == 1 && !values.iterator().next().isEmpty()));
        if (values.size() == 1) modes.setSelectedIndex(0);
        updatingFilters = false;
        rebuildTeamFilter();
        filterStack.setVisible(filters.isVisible() || timeTypes.isVisible());
        renderSelection();
        revalidate();
    }

    private void rebuildTeamFilter()
    {
        String boss = selectedActivity();
        String mode = modes.isVisible() ? selectedText(modes) : singleMode(boss);
        Set<Integer> sizes = new TreeSet<>();
        for (Category category : availableCategories)
        {
            if (safe(category.boss).equalsIgnoreCase(boss) && safe(category.mode).equalsIgnoreCase(mode))
                sizes.add(category.teamSize);
        }
        List<String> labels = new ArrayList<>();
        for (Integer size : sizes)
        {
            if (size > 0) labels.add(size == 1 ? "Solo" : size + " players");
        }
        updatingFilters = true;
        teams.setModel(new DefaultComboBoxModel<>(labels.toArray(new String[0])));
        teams.setVisible(labels.size() > 1);
        if (!labels.isEmpty()) teams.setSelectedIndex(0);
        updatingFilters = false;
        filters.setVisible(modes.isVisible() || teams.isVisible());
        rebuildTimeTypeFilter(boss, mode, teams.isVisible() ? parseTeamLabel(selectedText(teams)) : singleTeam(boss, mode));
        filterStack.setVisible(filters.isVisible() || timeTypes.isVisible());
    }

    private void rebuildTimeTypeFilter(String boss, String mode, int teamSize)
    {
        Set<String> types = new LinkedHashSet<>();
        for (Category category : availableCategories)
        {
            if (safe(category.boss).equalsIgnoreCase(boss)
                && safe(category.mode).equalsIgnoreCase(mode)
                && category.teamSize == teamSize
                && !safe(category.timeType).isEmpty())
            {
                types.add(category.timeType.toUpperCase(Locale.ROOT));
            }
        }
        List<String> labels = new ArrayList<>();
        if (types.contains("ROOM")) labels.add("Room time");
        if (types.contains("OVERALL")) labels.add("Overall time");
        updatingFilters = true;
        timeTypes.setModel(new DefaultComboBoxModel<>(labels.toArray(new String[0])));
        timeTypes.setVisible("Theatre of Blood".equalsIgnoreCase(boss) && labels.size() > 1);
        if (!labels.isEmpty()) timeTypes.setSelectedIndex(0);
        updatingFilters = false;
    }

    private void renderSelection()
    {
        Category selected = selectedCategory();
        if (selected == null)
        {
            ranking.removeAll();
            ranking.add(centered("No PB ranking is available for this selection."));
            ownPb.setText("No PB synced for this category");
            ranking.revalidate();
            ranking.repaint();
            return;
        }

        ranking.removeAll();
        JsonArray rows = selected.rows == null ? new JsonArray() : selected.rows;
        if (rows.size() == 0)
        {
            ranking.add(centered("No PBs exist in this category yet."));
        }
        else
        {
            for (JsonElement element : rows)
            {
                if (element.isJsonObject()) ranking.add(rankingRow(element.getAsJsonObject()));
            }
        }

        if (selected.own == null || selected.own.size() == 0)
        {
            ownPb.setText("<html><div style='text-align:center'>You do not have a PB<br>in this category yet</div></html>");
        }
        else
        {
            ownPb.setText("<html><div style='text-align:center'>Your PB: "
                + formatTime(decimal(selected.own, "seconds", 0)) + " · #"
                + integer(selected.own, "position", 0) + "</div></html>");
        }
        ranking.revalidate();
        ranking.repaint();
        rankingScroll.getViewport().revalidate();
    }

    private Category selectedCategory()
    {
        String boss = selectedActivity();
        String mode = modes.isVisible() ? selectedText(modes) : singleMode(boss);
        int teamSize = teams.isVisible() ? parseTeamLabel(selectedText(teams)) : singleTeam(boss, mode);
        String timeType = timeTypes.isVisible() ? selectedTimeType() : singleTimeType(boss, mode, teamSize);
        for (Category value : availableCategories)
        {
            if (safe(value.boss).equalsIgnoreCase(boss)
                && safe(value.mode).equalsIgnoreCase(mode)
                && value.teamSize == teamSize
                && safe(value.timeType).equalsIgnoreCase(timeType))
            {
                return value;
            }
        }
        return null;
    }

    private JPanel rankingRow(JsonObject entry)
    {
        int position = integer(entry, "position", 0);
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, position <= 3 ? 38 : 30));
        row.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, position <= 3 ? 3 : 1, 1, 0, medalColor(position)),
            BorderFactory.createEmptyBorder(5, 6, 5, 6)));
        JLabel place = new JLabel(position + getOrdinalSuffix(position));
        place.setPreferredSize(new Dimension(position <= 3 ? 48 : 30, 24));
        if (position <= 3)
        {
            place.setIcon(medalIcon(position));
            place.setIconTextGap(3);
            place.setForeground(medalColor(position));
        }
        String playerName = text(entry, "rsn", text(entry, "name", "—"));
        JLabel player = new JLabel(playerName);
        player.setToolTipText(playerName);
        JLabel time = new JLabel(formatTime(decimal(entry, "seconds", 0)), SwingConstants.RIGHT);
        if (position <= 3) time.setForeground(medalColor(position));
        row.add(place, BorderLayout.WEST);
        row.add(player, BorderLayout.CENTER);
        row.add(time, BorderLayout.EAST);
        return row;
    }

    private static String getOrdinalSuffix(int n)
    {
        if (n == 1) return "st";
        if (n == 2) return "nd";
        if (n == 3) return "rd";
        return "th";
    }

    private void setTutorialExpanded(boolean expanded)
    {
        tutorial.setVisible(expanded);
        tutorialToggle.setText((expanded ? "▾ " : "▸ ") + "How to register your PBs?");
    }

    private String selectedActivity()
    {
        String raid = selectedText(raids);
        return raid.isEmpty() ? selectedText(bosses) : raid;
    }

    private String resolveBossName(String query)
    {
        String normalizedQuery = normalizeSearch(query);
        if (normalizedQuery.isEmpty()) return "";
        String alias = bossAlias(normalizedQuery);
        for (Category category : availableCategories)
        {
            String candidate = safe(category.boss);
            String normalizedCandidate = normalizeSearch(candidate);
            if (normalizedCandidate.equals(normalizedQuery)
                || normalizedCandidate.equals(alias)
                || normalizedCandidate.contains(alias)
                || normalizedCandidate.contains(normalizedQuery)) return candidate;
        }
        return query;
    }

    private static String bossAlias(String query)
    {
        switch (query)
        {
            case "dusk": case "dawn": case "gargs": case "ggs": case "gg": return "grotesque guardians";
            case "jad": case "tztok jad": return "tzhaar fight cave";
            case "zuk": case "tzkal zuk": return "inferno";
            case "cox": case "xeric": case "chambers": case "olm": case "raids": return "chambers of xeric";
            case "tob": case "theatre": case "verzik": case "verzik vitur": case "raids 2": return "theatre of blood";
            case "toa": case "tombs": case "amascut": case "warden": case "wardens": case "raids 3": return "tombs of amascut";
            case "cg": case "cgaunt": case "cgauntlet": case "the corrupted gauntlet": return "corrupted gauntlet";
            case "gaunt": case "gauntlet": case "the gauntlet": return "gauntlet";
            case "sire": return "abyssal sire";
            case "cerb": return "cerberus";
            case "thermy": case "smoke devil": return "thermonuclear smoke devil";
            case "hydra": return "alchemical hydra";
            case "kbd": return "king black dragon";
            case "corp": return "corporeal beast";
            case "kq": return "kalphite queen";
            case "vork": return "vorkath";
            case "mole": return "giant mole";
            case "phantom": case "muspah": case "pm": return "phantom muspah";
            case "sara": case "saradomin": case "zily": case "zilyana": return "commander zilyana";
            case "zammy": case "zamorak": case "kril": return "k ril tsutsaroth";
            case "arma": case "kree": case "kreearra": case "armadyl": return "kree arra";
            case "bandos": case "bando": case "graardor": return "general graardor";
            case "duke": case "duke awakened": return "duke sucellus";
            case "levi": case "levi awakened": return "the leviathan";
            case "vard": case "vard awakened": return "vardorvis";
            case "wisp": case "whisp": case "whisperer awakened": return "the whisperer";
            case "sol": case "colo": case "colosseum": return "sol heredit";
            default: return query;
        }
    }

    private static String normalizeSearch(String value)
    {
        return safe(value).trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
    }

    private String singleMode(String boss)
    {
        for (Category value : availableCategories)
            if (safe(value.boss).equalsIgnoreCase(boss)) return safe(value.mode);
        return "";
    }

    private int singleTeam(String boss, String mode)
    {
        for (Category value : availableCategories)
            if (safe(value.boss).equalsIgnoreCase(boss) && safe(value.mode).equalsIgnoreCase(mode)) return value.teamSize;
        return 0;
    }

    private String singleTimeType(String boss, String mode, int teamSize)
    {
        for (Category value : availableCategories)
            if (safe(value.boss).equalsIgnoreCase(boss) && safe(value.mode).equalsIgnoreCase(mode)
                && value.teamSize == teamSize) return safe(value.timeType);
        return "";
    }

    private String selectedTimeType()
    {
        return "Overall time".equals(selectedText(timeTypes)) ? "OVERALL" : "ROOM";
    }

    private static String selectedText(JComboBox<String> combo)
    {
        return combo.getSelectedItem() == null ? "" : combo.getSelectedItem().toString();
    }

    private static int parseTeamLabel(String label)
    {
        if ("Solo".equalsIgnoreCase(label)) return 1;
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("^(\\d+)").matcher(label);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
    }

    private static JLabel centered(String text)
    {
        JLabel label = new JLabel("<html><div style='text-align:center;width:190px'>" + text + "</div></html>", SwingConstants.CENTER);
        label.setBorder(BorderFactory.createEmptyBorder(18, 3, 18, 3));
        return label;
    }

    private String rsn()
    {
        return client.getLocalPlayer() == null || client.getLocalPlayer().getName() == null
            ? "" : client.getLocalPlayer().getName().trim();
    }

    private static String safe(String value) { return value == null ? "" : value; }

    private static Color medalColor(int position)
    {
        return position == 1 ? new Color(235, 190, 45)
            : position == 2 ? new Color(175, 185, 190)
            : position == 3 ? new Color(190, 110, 55)
            : new Color(70, 70, 70);
    }

    private static ImageIcon medalIcon(int position)
    {
        java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(16, 18,
            java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
            java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        Color color = medalColor(position);
        graphics.setColor(color.darker());
        graphics.fillPolygon(new int[]{3, 7, 7, 5}, new int[]{1, 1, 9, 11}, 4);
        graphics.fillPolygon(new int[]{9, 13, 11, 9}, new int[]{1, 1, 11, 9}, 4);
        graphics.setColor(color);
        graphics.fillOval(3, 6, 10, 10);
        graphics.setColor(color.brighter());
        graphics.drawOval(4, 7, 8, 8);
        graphics.dispose();
        return new ImageIcon(image);
    }

    static String formatTime(double seconds)
    {
        long centiseconds = Math.round(seconds * 100.0);
        long hours = centiseconds / 360000;
        long minutes = (centiseconds / 6000) % 60;
        long secs = (centiseconds / 100) % 60;
        long fraction = centiseconds % 100;
        return hours > 0 ? String.format(Locale.ROOT, "%d:%02d:%02d.%02d", hours, minutes, secs, fraction)
            : String.format(Locale.ROOT, "%02d:%02d.%02d", minutes, secs, fraction);
    }

    private static JsonArray array(JsonObject parent, String key)
    {
        try { return parent != null && parent.has(key) && parent.get(key).isJsonArray() ? parent.getAsJsonArray(key) : new JsonArray(); }
        catch (Exception ignored) { return new JsonArray(); }
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

    private static double decimal(JsonObject object, String key, double fallback)
    {
        try { return object != null && object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsDouble() : fallback; }
        catch (Exception ignored) { return fallback; }
    }

    private static String escape(String value)
    {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static final class Category
    {
        String category;
        String boss;
        String mode;
        int teamSize;
        String timeType;
        JsonArray rows = new JsonArray();
        JsonObject own = new JsonObject();
    }

    private static final class VerticalRankingPanel extends JPanel implements Scrollable
    {
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) { return 24; }
        @Override public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) { return Math.max(24, visibleRect.height - 24); }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }
}
