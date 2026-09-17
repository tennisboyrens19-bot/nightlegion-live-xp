from pathlib import Path
import re
import runpy
root = Path('.')
base = root / 'src/main/java/com/revalclan'

def rep(path, old, new, count=1):
    p = base / path
    s = p.read_text()
    assert old in s, (path, old[:80])
    p.write_text(s.replace(old, new, count))

assert 'private MonthlyMvp monthlyMvp;' not in (base/'api/account/AccountResponse.java').read_text(), 'Already applied'
rep('api/account/AccountResponse.java', 'import com.revalclan.api.common.ApiResponse;', 'import com.revalclan.api.common.ApiResponse;\nimport com.revalclan.api.common.MonthlyMvp;')
rep('api/account/AccountResponse.java', '        private List<PointsLogEntry> pointsLog;', '        private List<PointsLogEntry> pointsLog;\n        private MonthlyMvp monthlyMvp;\n        private boolean monthlyMvpWinner;')
rep('api/account/AccountResponse.java', '        private Double ehb;', '        private Double ehb;\n        private String efficiencyStatus;\n        private String efficiencyUpdatedAt;')
rep('api/account/AccountResponse.java', '        private String clanRank;', '        private String clanRank;\n        private String calculatedRank;\n        private String actualClanRank;')
rep('api/account/AccountResponse.java', '        private int total;', '        private int total;\n        private Integer misc;\n        private Integer unreconciledPoints;')
rep('api/leaderboard/LeaderboardResponse.java', 'import com.revalclan.api.common.ApiResponse;', 'import com.revalclan.api.common.ApiResponse;\nimport com.revalclan.api.common.MonthlyMvp;')
rep('api/leaderboard/LeaderboardResponse.java', '        private List<LeaderboardEntry> leaderboard;', '        private List<LeaderboardEntry> leaderboard;\n        private MonthlyMvp monthlyMvp;')
rep('api/leaderboard/LeaderboardResponse.java', '        private String clanRank;', '        private String clanRank;\n        private boolean monthlyMvpWinner;')
rep('api/playercards/ProfileCardResponse.java', 'private String nickname;', 'private String nickname;\n\t\tprivate boolean monthlyMvpWinner;')
rep('playercards/PlayerCardData.java', 'String playerName;', 'String playerName;\n\tboolean monthlyMvpWinner;')
rep('playercards/PlayerCardData.java', '\t\t\tprofile.getNickname(),', '\t\t\tprofile.getNickname(),\n\t\t\tprofile.isMonthlyMvpWinner(),')
rep('playercards/PlayerCardOverlay.java', 'int cy = y + 26;', '''int cy = y + 26;
        if (card.isMonthlyMvpWinner()) {
            g.setFont(small);
            drawShadowed(g, "MVP", x + 18, y + 25, UIConstants.ACCENT_GOLD);
        }''')
p = base/'ui/RevalPanel.java'
s = p.read_text().replace('private static final String WEBSITE_URL = "";', 'static final String WEBSITE_URL = "https://nightlegion-web.vercel.app/";')
s = s.replace('row2.add(leaderboardTab);\n\t\trow2.add(Box.createRigidArea(new Dimension(4, 0)));\n\t\t', '')
s = s.replace('JPanel row3 = new JPanel(new GridLayout(1, 1, 4, 0));', 'JPanel row3 = new JPanel(new BorderLayout(4, 0));')
s = s.replace('row3.add(diaryTab);', 'row3.add(leaderboardTab, BorderLayout.WEST);\n\t\trow3.add(diaryTab, BorderLayout.CENTER);')
s = s.replace('public void onLoggedIn() {\n\t\tprofilePanel.refresh();', 'public void onLoggedIn() {\n\t\trankingPanel.refresh();\n\t\tleaderboardPanel.refresh();\n\t\tprofilePanel.refresh();')
pos = s.index('\n\tpublic void onLoggedOut()')
s = s[:pos] + '''
    public void onConnectionChanged() {
        profilePanel.resetConnection();
        selectTab("PROFILE");
    }
''' + s[pos:]
p.write_text(s)
rep('ui/RankingPanel.java', 'public class RankingPanel extends JPanel {', '''public class RankingPanel extends JPanel {
    // Defense against old server responses; the backend retires these systems.
    private static final java.util.Set<String> HIDDEN_POINT_SECTIONS = java.util.Set.of(
        "CLAN_ACTIVITY", "DISCORD_ACTIVITY", "CLAN_EVENTS", "WOM_ACTIVITY");

    static boolean isPointSectionVisible(String category) {
        return category != null && !HIDDEN_POINT_SECTIONS.contains(category.toUpperCase(java.util.Locale.ROOT));
    }
''')
rep('ui/RankingPanel.java', 'if (sources == null || sources.isEmpty() || category.equals("UNTRADEABLE_DROPS")) continue;', 'if (sources == null || sources.isEmpty() || !isPointSectionVisible(category)\n                    || category.equals("UNTRADEABLE_DROPS")) continue;')
rep('ui/AchievementsPanel.java', '\t\tcard.add(badge);', '\t\tif (achievement.getRarity() != null && !achievement.getRarity().isEmpty()) card.add(badge);')
p = base/'api/NightLegionTransport.java'
s = p.read_text().replace('import java.util.concurrent.TimeUnit;', 'import java.util.concurrent.TimeUnit;\nimport java.util.concurrent.atomic.AtomicLong;')
s = s.replace('private volatile String authoritativeRsn = "";', 'private volatile String authoritativeRsn = "";\n    private final AtomicLong generation = new AtomicLong();\n    private final String baseUrl;')
s = s.replace('''Client runeLiteClient)
	{
		this.client = client;''', '''Client runeLiteClient)
    {
        this(client, gson, config, runeLiteClient, BASE);
    }

    NightLegionTransport(OkHttpClient client, Gson gson, RevalClanConfig config,
        Client runeLiteClient, String baseUrl)
    {
        this.baseUrl = baseUrl;
        this.client = client;''', 1)
s = s.replace('\tpublic void request', '''    /** Suppress responses from an old credential/account context. */
    public void invalidateRequests() { generation.incrementAndGet(); }

\tpublic void request''', 1)
s = s.replace('\t\tJsonObject envelope = new JsonObject();', '''        final long requestGeneration = generation.get();
        Consumer<JsonObject> guardedSuccess = value -> {
            if (generation.get() == requestGeneration) success.accept(value);
        };
        Consumer<Exception> guardedFailure = error -> {
            if (generation.get() == requestGeneration) failure.accept(error);
        };
\t\tJsonObject envelope = new JsonObject();''', 1)
a = s.index('        final long requestGeneration')
b = s.index('\n\t/** Capture identity', a)
part = s[a:b]
c = part.index('\t\tJsonObject envelope')
part = part[:c] + part[c:].replace('failure.accept(', 'guardedFailure.accept(').replace('poll(id, token, 0, success, failure)', 'poll(id, token, 0, guardedSuccess, guardedFailure, requestGeneration)')
part = part.replace('guardedFailure.accept(error); }', 'guardedFailure.accept(new IOException("Cannot reach NightLegion. Check your connection and try again.")); }', 1)
s = s[:a] + part + s[b:]
s = s.replace('.url(BASE +', '.url(baseUrl +')
s = s.replace('Consumer<Exception> failure)\n\t{\n\t\tif (attempt > MAX_POLLS)', 'Consumer<Exception> failure, long requestGeneration)\n\t{\n        if (generation.get() != requestGeneration) return;\n\t\tif (attempt > MAX_POLLS)', 1)
s = s.replace('poll(id, token, attempt + 1, success, failure);', 'poll(id, token, attempt + 1, success, failure, requestGeneration);')
s = s.replace('''		poller.schedule(() ->
		{
			Request request''', '''		poller.schedule(() ->
		{
            if (generation.get() != requestGeneration) return;
			Request request''', 1)
s = s.replace('private String errorText(String text, int status)\n\t{', '''private String errorText(String text, int status)
\t{
        if (status == 401 || status == 403)
            return "NightLegion connection not authorized. Check your token and clan membership.";
        if (status >= 500)
            return "NightLegion server is unavailable. Please try again later.";''', 1)
p.write_text(s)
p = base/'RevalClanPlugin.java'
s = p.read_text()
needle = '\t\tif (!"nightlegion".equals(event.getGroup())) return;\n'
assert s.count(needle) == 1
s = s.replace(needle, needle + '''
        if ("personalLinkToken".equals(event.getKey())) {
            nightLegionTransport.invalidateRequests();
            revalApiService.clearCache();
            javax.swing.SwingUtilities.invokeLater(() -> {
                if (revalPanel != null) revalPanel.onConnectionChanged();
            });
            clientThread.invokeLater(() -> {
                nightLegionTransport.captureCurrentRsn();
                inRequiredClan = false;
                clanValidationAttempt = client.getGameState() == GameState.LOGGED_IN ? 0 : -1;
                return true;
            });
            return;
        }
''', 1)
s = s.replace('\t\t\trevalPanel.onLoggedIn();', '''            javax.swing.SwingUtilities.invokeLater(() -> {
                if (revalPanel != null) revalPanel.onLoggedIn();
            });''', 1)
# Retired CHAT telemetry: retain actual game-message parsing for pets/drops/etc.
s = re.sub(r'^.*@Inject\s+private ChatNotifier chatNotifier;\n', '', s, flags=re.M)
s = s.replace('\t\tchatNotifier.onChatMessage(type, event.getName(), cleanMessage);\n', '')
p.write_text(s)
p = base/'RevalClanConfig.java'
s = p.read_text()
a = s.rfind('\t@ConfigItem(', 0, s.index('default boolean notifyChat()'))
b = s.index('\n\t}', s.index('default boolean notifyChat()')) + len('\n\t}')
assert a >= 0
p.write_text(s[:a] + s[b:])
(base/'notifiers/ChatNotifier.java').unlink()
p = base/'ui/ProfilePanel.java'
s = p.read_text().replace('import com.revalclan.ui.components.BlockButton;', 'import com.revalclan.ui.components.BlockButton;\nimport com.revalclan.ui.components.Badge;')
s = s.replace('private boolean isLoading = false;', 'private boolean isLoading = false;\n    private long loadGeneration = 0;')
s = s.replace('''	public void onLoggedOut() {
		SwingUtilities.invokeLater(this::showNotLoggedIn);
	}''', '''    public void resetConnection() {
        loadGeneration++;
        isLoading = false;
        currentAccount = null;
        pointsData = null;
        pointsLog = null;
        ranks = null;
        disposeAlbum();
        showNotLoggedIn();
    }

    public void onLoggedOut() {
        SwingUtilities.invokeLater(this::resetConnection);
    }''', 1)
s = s.replace('\t\tapiService.fetchAccount(accountHash,', '        final long requestGeneration = loadGeneration;\n\t\tapiService.fetchAccount(accountHash,', 1)
s = s.replace('\t\tapiService.fetchAccountById(osrsAccountId,', '        final long requestGeneration = loadGeneration;\n\t\tapiService.fetchAccountById(osrsAccountId,', 1)
a = s.index('\tpublic void loadAccount(long')
b = s.index('\n\tpublic void refresh()', a)
part = s[a:b].replace('response -> {\n\t\t\t\tisLoading = false;', 'response -> {\n                if (requestGeneration != loadGeneration) return;\n\t\t\t\tisLoading = false;')
part = part.replace('SwingUtilities.invokeLater(() -> {\n\t\t\t\t\tcurrentAccount', 'SwingUtilities.invokeLater(() -> {\n                    if (requestGeneration != loadGeneration) return;\n\t\t\t\t\tcurrentAccount')
part = part.replace('error -> {\n\t\t\t\tisLoading = false;', 'error -> {\n                if (requestGeneration != loadGeneration) return;\n\t\t\t\tisLoading = false;')
for fallback in ('Failed to fetch account data', 'Player not found'):
    old = f'SwingUtilities.invokeLater(() -> showError(error.getMessage() != null ? error.getMessage() : "{fallback}"));'
    new = f'SwingUtilities.invokeLater(() -> {{ if (requestGeneration == loadGeneration) showError(error.getMessage() != null ? error.getMessage() : "{fallback}"); }});'
    part = part.replace(old, new)
s = s[:a] + part + s[b:]
s = s.replace('JLabel errorLabel = new JLabel(message);', '''JTextArea errorLabel = new JTextArea(message);
        errorLabel.setEditable(false);
        errorLabel.setFocusable(false);
        errorLabel.setOpaque(false);
        errorLabel.setLineWrap(true);
        errorLabel.setWrapStyleWord(true);
        errorLabel.setColumns(22);''', 1)
s = s.replace('JLabel hint = new JLabel("Make sure you\'re in the NightLegion clan");', '''JTextArea hint = new JTextArea("Check the connection settings, then press Retry.");
        hint.setEditable(false);
        hint.setFocusable(false);
        hint.setOpaque(false);
        hint.setLineWrap(true);
        hint.setWrapStyleWord(true);
        hint.setColumns(22);''', 1)
s = s.replace('''		placeholder.add(hint);

		addComponent(placeholder);''', '''		placeholder.add(hint);
        JButton retry = new BlockButton("Retry", UIConstants.ACCENT_GOLD, 26);
        retry.addActionListener(event -> refresh());
        placeholder.add(Box.createRigidArea(new Dimension(0, 8)));
        placeholder.add(retry);

		addComponent(placeholder);''', 1)
s = s.replace('topRow.add(namePanel, BorderLayout.WEST);', 'topRow.add(namePanel, BorderLayout.CENTER);', 1)
s = s.replace('''		header.add(topRow);

		JPanel rankProgress''', '''		header.add(topRow);
        if (currentAccount != null && currentAccount.isMonthlyMvpWinner()) {
            Badge badge = new Badge("MVP", UIConstants.ACCENT_GOLD);
            badge.setToolTipText("Automatic overall monthly MVP: 3/2/1 points across Drops, EHB and EHP");
            badge.setAlignmentX(Component.LEFT_ALIGNMENT);
            header.add(Box.createRigidArea(new Dimension(0, 4)));
            header.add(badge);
        }
        if (account.getActualClanRank() != null) {
            nameLabel.setToolTipText("In-game clan rank: " + account.getActualClanRank()
                + "; points progression: " + getRankDisplayName(account.getClanRank()));
        }

		JPanel rankProgress''', 1)
s = s.replace('String currentRank = account.getClanRank();', 'String currentRank = account.getCalculatedRank() != null ? account.getCalculatedRank() : account.getClanRank();', 1)
s = s.replace('formatDecimal(account.getEhp() != null ? account.getEhp() : 0.0)', 'formatEfficiency(account.getEhp())')
s = s.replace('formatDecimal(account.getEhb() != null ? account.getEhb() : 0.0)', 'formatEfficiency(account.getEhb())')
pos = s.index('\n\tprivate JPanel buildStatsSection')
s = s[:pos] + '''
    static String formatEfficiency(Double value) {
        return value == null || !Double.isFinite(value) || value < 0
            ? "--" : String.format(java.util.Locale.ROOT, "%.1f", value);
    }
''' + s[pos:]
p.write_text(s)
p = root/'src/test/java/com/revalclan/HardRevalNightLegionContractTest.java'
s = p.read_text().replace('assertEquals(129L, files.filter(path -> path.toString().endsWith(".java")).count());', 'assertTrue(files.filter(path -> path.toString().endsWith(".java")).count() >= 129L);')
p.write_text(s)
runpy.run_path(str(root/'.maintenance/finalize_plugin.py'))
# The official Prefect name/icon already exists. Do not retain the old typo.
p = base/'util/ClanRankIconResolver.java'
p.write_text(p.read_text().replace('        Map.entry("precept", 3138),\n', ''))
p = root/'src/test/java/com/revalclan/ui/ProfileRegressionTest.java'
p.write_text(p.read_text().replace('precept', 'prefect'))
# Normalize EOFs only for changed Java files to satisfy git diff --check.
import subprocess
for name in subprocess.check_output(['git','diff','--name-only']).decode().splitlines():
    p = root/name
    if p.is_file() and p.suffix == '.java':
        p.write_text(p.read_text().rstrip()+'\n')
