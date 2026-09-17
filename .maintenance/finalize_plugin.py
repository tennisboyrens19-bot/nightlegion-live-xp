from pathlib import Path
root = Path('.')
base = root / 'src/main/java/com/revalclan'

def edit(path, old, new):
    p = base / path
    s = p.read_text()
    assert old in s, (path, old[:70])
    p.write_text(s.replace(old, new))

p = base / 'ui/ProfilePanel.java'
s = p.read_text().replace('private long loadGeneration = 0;', 'private volatile long loadGeneration = 0;')
a = s.index('\tprivate void fetchRanks() {')
b = s.index('\n    public void resetConnection()', a)
s = s[:a] + '''    private void fetchRanks() {
        if (apiService == null) return;
        final long requestGeneration = loadGeneration;
        apiService.fetchPoints(response -> SwingUtilities.invokeLater(() -> {
            if (requestGeneration != loadGeneration) return;
            if (response != null && response.getData() != null) {
                pointsData = response.getData();
                ranks = pointsData.getRanks();
                if (currentAccount != null) buildProfile();
            }
        }), error -> { });
    }
''' + s[b:]
s = s.replace('errorLabel.setColumns(22);', 'errorLabel.setColumns(0);\n        errorLabel.setRows(4);\n        errorLabel.setMinimumSize(new Dimension(0, 56));')
s = s.replace('hint.setColumns(22);', 'hint.setColumns(0);\n        hint.setRows(3);\n        hint.setMinimumSize(new Dimension(0, 42));')
s = s.replace('long miscPoints = breakdown.getTotal()\n', 'long miscPoints = breakdown.getMisc() != null ? breakdown.getMisc() : breakdown.getTotal()\n')
s = s.replace('miscRow.add(createStatCard(formatNumber(miscPoints), "Misc", UIConstants.TEXT_SECONDARY, "misc"));', '''JPanel miscCard = createStatCard(formatNumber(miscPoints), "Misc", UIConstants.TEXT_SECONDARY, "misc");
        if (breakdown.getUnreconciledPoints() != null && breakdown.getUnreconciledPoints() != 0) {
            miscCard.setToolTipText("Includes " + breakdown.getUnreconciledPoints()
                + " points not reconciled to the ledger. No points were changed.");
        }
        miscRow.add(miscCard);''')
s = s.replace('section.add(createStatCard(formatEfficiency(account.getEhp()), "EHP", UIConstants.ACCENT_GREEN, null));\n\t\tsection.add(createStatCard(formatEfficiency(account.getEhb()), "EHB", UIConstants.ACCENT_BLUE, null));', '''JPanel ehp = createStatCard(formatEfficiency(account.getEhp()), "EHP", UIConstants.ACCENT_GREEN, null);
        JPanel ehb = createStatCard(formatEfficiency(account.getEhb()), "EHB", UIConstants.ACCENT_BLUE, null);
        ehp.setToolTipText("--".equals(formatEfficiency(account.getEhp())) ? "EHP unavailable: no verified player statistic was returned." : "Wise Old Man EHP for this character");
        ehb.setToolTipText("--".equals(formatEfficiency(account.getEhb())) ? "EHB unavailable: no verified player statistic was returned." : "Wise Old Man EHB for this character");
        section.add(ehp);
        section.add(ehb);''')
p.write_text(s)
edit('ui/RankingPanel.java', '\tpublic void refresh() {', '''    public void resetConnection() {
        showPlaceholder();
    }

\tpublic void refresh() {''')
edit('ui/CompetitionsPanel.java', '\tpublic void refresh() {', '''    public void resetConnection() {
        activeVotes = new ArrayList<>();
        activeCompetitions = new ArrayList<>();
        scheduledCompetitions = new ArrayList<>();
        myVotes.clear();
        detailViewPanel.removeAll();
        cardLayout.show(cardContainer, "LIST");
        contentPanel.removeAll();
        contentPanel.add(new com.revalclan.ui.components.LoginPrompt("Competitions"));
        contentPanel.revalidate();
        contentPanel.repaint();
        if (onIndicatorUpdate != null) onIndicatorUpdate.accept(false);
    }

\tpublic void refresh() {''')
p = base / 'ui/RevalPanel.java'
s = p.read_text()
a = s.index('    public void onConnectionChanged()')
b = s.index('\n\tpublic void onLoggedOut()', a)
s = s[:a] + '''    public void onConnectionChanged() {
        profilePanel.resetConnection();
        rankingPanel.resetConnection();
        leaderboardPanel.resetConnection();
        competitionsPanel.resetConnection();
        achievementsPanel.onLoggedOut();
        eventsPanel.onLoggedOut();
        diaryPanel.onLoggedOut();
        if (adminManager != null) adminManager.logout();
        if (adminLoginPanel != null) contentPanel.remove(adminLoginPanel);
        if (adminDashboardPanel != null) contentPanel.remove(adminDashboardPanel);
        if (pendingRankupsPanel != null) contentPanel.remove(pendingRankupsPanel);
        adminManager = new AdminManager();
        adminLoginPanel = null;
        adminDashboardPanel = null;
        pendingRankupsPanel = null;
        if (adminButton != null) adminButton.setAdmin(false);
        setEventsIndicator(false);
        setCompetitionsIndicator(false);
        selectTab("PROFILE");
    }
''' + s[b:]
p.write_text(s)
p = base / 'ui/LeaderboardPanel.java'
s = p.read_text().replace('import com.revalclan.api.RevalApiService;', 'import com.revalclan.api.RevalApiService;\nimport com.revalclan.api.common.MonthlyMvp;\nimport com.revalclan.ui.components.Badge;')
s = s.replace('private RefreshButton refreshButton;', 'private RefreshButton refreshButton;\n    private MonthlyMvp monthlyMvp;\n    private volatile long loadGeneration;')
s = s.replace('JPanel contentWrapper = new JPanel(new BorderLayout());', '''JPanel contentWrapper = new JPanel(new BorderLayout()) {
            @Override public Dimension getPreferredSize() {
                Dimension size = super.getPreferredSize();
                if (getParent() != null) size.width = getParent().getWidth();
                return size;
            }
        };''')
s = s.replace('\tpublic void refresh() {', '''    public void resetConnection() {
        loadGeneration++;
        monthlyMvp = null;
        allEntries = new ArrayList<>();
        filteredEntries = new ArrayList<>();
        profileViewPanel.removeAll();
        showListView();
        showMessage("Connect to load the leaderboard");
    }

\tpublic void refresh() {''')
a = s.index('\tprivate void loadLeaderboard()')
b = s.index('\n\tprivate void filterLeaderboard()', a)
s = s[:a] + '''    private void loadLeaderboard() {
        if (apiService == null) return;
        final long requestGeneration = ++loadGeneration;
        refreshButton.setLoading(true);
        showLoading();
        apiService.fetchLeaderboard(response -> SwingUtilities.invokeLater(() -> {
            if (requestGeneration != loadGeneration) return;
            refreshButton.setLoading(false);
            if (response != null && response.getData() != null) {
                monthlyMvp = response.getData().getMonthlyMvp();
                allEntries = response.getData().getLeaderboard() != null
                    ? response.getData().getLeaderboard() : new ArrayList<>();
                filterLeaderboard();
            } else {
                showMessage("No leaderboard data");
            }
        }), error -> SwingUtilities.invokeLater(() -> {
            if (requestGeneration != loadGeneration) return;
            refreshButton.setLoading(false);
            showMessage("Leaderboard unavailable. Press Refresh to retry.");
        }));
    }
''' + s[b:]
s = s.replace('searchField.getText().toLowerCase().trim()', 'searchField.getText().toLowerCase(java.util.Locale.ROOT).trim()')
s = s.replace('e.getOsrsNickname().toLowerCase().contains(query)', 'e.getOsrsNickname() != null && e.getOsrsNickname().toLowerCase(java.util.Locale.ROOT).contains(query)')
s = s.replace('''		if (filteredEntries.isEmpty()) {
			showMessage(allEntries.isEmpty() ? "No players found" : "No matching players");
			return;
		}
''', '''        contentPanel.add(new MonthlyMvpPanel(monthlyMvp));
        contentPanel.add(Box.createVerticalStrut(10));
''')
s = s.replace('''		contentPanel.add(countRow);
		contentPanel.add(Box.createVerticalStrut(4));''', '''		contentPanel.add(countRow);
		contentPanel.add(Box.createVerticalStrut(4));
        if (filteredEntries.isEmpty()) {
            JLabel empty = new JLabel(allEntries.isEmpty() ? "No players found" : "No matching players");
            empty.setForeground(UIConstants.TEXT_SECONDARY);
            empty.setFont(FontManager.getRunescapeSmallFont());
            contentPanel.add(empty);
        }''')
a = s.index('\tprivate void showMessage(String message) {')
b = s.index('\n\tprivate void buildLeaderboard()', a)
s = s[:a] + s[a:b].replace('contentPanel.add(label);', 'contentPanel.add(label);\n        contentPanel.add(refreshButton);') + s[b:]
s = s.replace('row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));', 'row.setMaximumSize(new Dimension(Integer.MAX_VALUE, entry.isMonthlyMvpWinner() ? 62 : 44));')
s = s.replace('''		JPanel leftPanel = new JPanel();
		leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.X_AXIS));''', '\t\tJPanel leftPanel = new JPanel(new BorderLayout(4, 0));')
s = s.replace('rankLabel.setPreferredSize(new Dimension(42, 20));', 'rankLabel.setPreferredSize(new Dimension(28, 20));')
s = s.replace('''		leftPanel.add(rankLabel);
		leftPanel.add(Box.createRigidArea(new Dimension(6, 0)));
		leftPanel.add(nameLabel);''', '''        nameLabel.setToolTipText(entry.getOsrsNickname());
        JPanel nameStack = new JPanel(new BorderLayout());
        nameStack.setOpaque(false);
        nameStack.add(nameLabel, BorderLayout.CENTER);
        if (entry.isMonthlyMvpWinner()) {
            Badge badge = new Badge("MVP", UIConstants.ACCENT_GOLD);
            nameStack.add(badge, BorderLayout.SOUTH);
        }
        leftPanel.add(rankLabel, BorderLayout.WEST);
        leftPanel.add(nameStack, BorderLayout.CENTER);''')
s = s.replace('row.add(leftPanel, BorderLayout.WEST);', 'row.add(leftPanel, BorderLayout.CENTER);')
p.write_text(s)
p = base / 'util/ClanRankIconResolver.java'
s = p.read_text().replace('Map.entry("prefect", 3138),', 'Map.entry("prefect", 3138),\n        Map.entry("precept", 3138),')
p.write_text(s)
p = root / 'build.gradle'
s = p.read_text().replace("version = '1.0.0'", "def pluginProperties = new Properties()\nfile('runelite-plugin.properties').withInputStream { pluginProperties.load(it) }\nversion = pluginProperties.getProperty('version')")
s = s.replace("testImplementation 'junit:junit:4.13.2'", "testImplementation 'junit:junit:4.13.2'\n    testImplementation 'org.mockito:mockito-core:4.11.0'\n    testImplementation 'com.squareup.okhttp3:mockwebserver:3.14.9'")
p.write_text(s)
p = root / 'runelite-plugin.properties'
p.write_text(p.read_text().replace('2.18.7-nightlegion.1', '2.18.7-nightlegion.2'))
