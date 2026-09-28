"""Exact acceptance bug-fix hunks, applied after the original scoped fixes.

Every old context must match once; all unlisted source still matches upstream.
These fixed literals are reviewed adaptations, never runtime-derived exemptions.
"""
FIXES = {
    'src/main/java/com/revalclan/RevalClanPlugin.java': 'Clear Collection Log ownership and API caches at logout.',
    'src/main/java/com/revalclan/api/RevalApiService.java': 'Invalidate in-flight account/catalog responses when their cache generation changes.',
    'src/main/java/com/revalclan/api/account/AccountResponse.java': 'Retain fractional account balances, category totals and ledger deltas.',
    'src/main/java/com/revalclan/notifiers/DiaryNotifier.java': 'Reject queued diary completions after reset, logout or account change.',
    'src/main/java/com/revalclan/notifiers/LootNotifier.java': 'Give each queued receipt its own stable event ID; clear all pending receipt evidence at session reset.',
    'src/main/java/com/revalclan/notifiers/SyncNotifier.java': 'Recognize strict native accepted acknowledgements and legacy ok acknowledgements while rejecting false or malformed success flags.',
    'src/main/java/com/revalclan/session/SessionTracker.java': 'Release failed HTTP session deliveries from in-flight state so persisted replay can retry.',
    'src/main/java/com/revalclan/ui/PointsAlbumWindow.java': 'Preserve fractional ledger values while grouping, totaling and sorting cards.',
    'src/main/java/com/revalclan/ui/ProfilePanel.java': 'Invalidate catalog/UI state across accounts, restart cancelled profile loads during refresh, and preserve four-decimal points in totals, progress and categories.',
    'src/main/java/com/revalclan/ui/AchievementsPanel.java': 'Clear account-specific achievement data at logout and reject stale response callbacks.',
    'src/main/java/com/revalclan/ui/DiaryPanel.java': 'Clear account-specific diary data at logout and reject stale response callbacks.',
    'src/main/java/com/revalclan/ui/RankingPanel.java': 'Reject stale catalog callbacks and clear account-specific ranking views at session reset.',
    'src/main/java/com/revalclan/ui/RevalPanel.java': 'Reset and reload the ranking view across login/logout boundaries.',
    'src/main/java/com/revalclan/util/NumberFmt.java': 'Display up to four decimal places for points without rounding valid fractional adjustments away.',
}


def adapt_acceptance(path, source, one):
    if path == 'src/main/java/com/revalclan/RevalClanPlugin.java':
        source = one(source, r'''				break;

			case LOGIN_SCREEN: {
				eventFilterManager.resetSession();
				revalApiService.resetEventsSession();
				boolean wasInClan = clanMembership.isMember();
''', r'''				break;

			case LOGIN_SCREEN: {
				collectionLogManager.clearObtainedItems();
				revalApiService.clearCache();
				eventFilterManager.resetSession();
				revalApiService.resetEventsSession();
				boolean wasInClan = clanMembership.isMember();
''')
    if path == 'src/main/java/com/revalclan/api/RevalApiService.java':
        source = one(source, r'''    // Cached responses
    private PointsResponse cachedPoints;
    private long lastPointsFetch = 0;
    private AccountResponse cachedAccount;
    private String cachedAccountIdentifier;
    private long lastAccountFetch = 0;
''', r'''    // Cached responses
    private PointsResponse cachedPoints;
    private long lastPointsFetch = 0;
    private long pointsGeneration;
    private long accountGeneration;
    private AccountResponse cachedAccount;
    private String cachedAccountIdentifier;
    private long lastAccountFetch = 0;
''')
        source = one(source, r'''
    // ==================== POINTS API ====================

    public void fetchPoints(Consumer<PointsResponse> onSuccess, Consumer<Exception> onError) {
        if (cachedPoints != null && System.currentTimeMillis() - lastPointsFetch < CACHE_DURATION_MS) {
            onSuccess.accept(cachedPoints);
            return;
        }
        get(ApiEndpoints.POINTS, PointsResponse.class, response -> {
            cachedPoints = response;
            lastPointsFetch = System.currentTimeMillis();
            onSuccess.accept(response);
        }, onError);
    }

    // ==================== ACCOUNT API ====================

    public void fetchAccount(long accountHash, Consumer<AccountResponse> onSuccess, Consumer<Exception> onError) {
        String identifier = String.valueOf(accountHash);
        if (cachedAccount != null && identifier.equals(cachedAccountIdentifier)
            && System.currentTimeMillis() - lastAccountFetch < ACCOUNT_CACHE_DURATION_MS) {
            onSuccess.accept(cachedAccount);
            return;
        }
        get(ApiEndpoints.ACCOUNT + "?accountHash=" + accountHash, AccountResponse.class, response -> {
            cachedAccount = response;
            cachedAccountIdentifier = identifier;
            lastAccountFetch = System.currentTimeMillis();
            onSuccess.accept(response);
        }, onError);
    }

    public void refreshAccount(long accountHash, Consumer<AccountResponse> onSuccess, Consumer<Exception> onError) {
''', r'''
    // ==================== POINTS API ====================

    public synchronized void fetchPoints(Consumer<PointsResponse> onSuccess, Consumer<Exception> onError) {
        if (cachedPoints != null && System.currentTimeMillis() - lastPointsFetch < CACHE_DURATION_MS) {
            onSuccess.accept(cachedPoints);
            return;
        }
        long generation = pointsGeneration;
        get(ApiEndpoints.POINTS, PointsResponse.class, response -> {
            synchronized (this) {
                if (generation != pointsGeneration) return;
                cachedPoints = response;
                lastPointsFetch = System.currentTimeMillis();
                onSuccess.accept(response);
            }
        }, error -> {
            synchronized (this) {
                if (generation == pointsGeneration) onError.accept(error);
            }
        });
    }

    // ==================== ACCOUNT API ====================

    public synchronized void fetchAccount(long accountHash, Consumer<AccountResponse> onSuccess, Consumer<Exception> onError) {
        String identifier = String.valueOf(accountHash);
        if (cachedAccount != null && identifier.equals(cachedAccountIdentifier)
            && System.currentTimeMillis() - lastAccountFetch < ACCOUNT_CACHE_DURATION_MS) {
            onSuccess.accept(cachedAccount);
            return;
        }
        long generation = accountGeneration;
        get(ApiEndpoints.ACCOUNT + "?accountHash=" + accountHash, AccountResponse.class, response -> {
            synchronized (this) {
                if (generation != accountGeneration) return;
                cachedAccount = response;
                cachedAccountIdentifier = identifier;
                lastAccountFetch = System.currentTimeMillis();
                onSuccess.accept(response);
            }
        }, error -> {
            synchronized (this) {
                if (generation == accountGeneration) onError.accept(error);
            }
        });
    }

    public void refreshAccount(long accountHash, Consumer<AccountResponse> onSuccess, Consumer<Exception> onError) {
''')
        source = one(source, r'''
    // ==================== CACHE MANAGEMENT ====================

    public void clearCache() {
        cachedPoints = null;
        lastPointsFetch = 0;
        cachedAccount = null;
''', r'''
    // ==================== CACHE MANAGEMENT ====================

    public synchronized void clearCache() {
        pointsGeneration++;
        accountGeneration++;
        cachedPoints = null;
        lastPointsFetch = 0;
        cachedAccount = null;
''')
        source = one(source, r'''        cachedLeaguesConfig = null;
    }

    public void clearAccountCache() {
        cachedAccount = null;
        cachedAccountIdentifier = null;
        lastAccountFetch = 0;
''', r'''        cachedLeaguesConfig = null;
    }

    public synchronized void clearAccountCache() {
        accountGeneration++;
        cachedAccount = null;
        cachedAccountIdentifier = null;
        lastAccountFetch = 0;
''')
    if path == 'src/main/java/com/revalclan/api/account/AccountResponse.java':
        source = one(source, r'''        private String womRank;
        private Double ehp;
        private Double ehb;
        private Integer activityPoints;
        private Integer maintenancePoints;
        private String clanRank;
        private String lastSyncedAt;
        private String rankUpdatedAt;
''', r'''        private String womRank;
        private Double ehp;
        private Double ehb;
        private Double activityPoints;
        private Double maintenancePoints;
        private String clanRank;
        private String lastSyncedAt;
        private String rankUpdatedAt;
''')
        source = one(source, r'''     */
    @Data
    public static class PointsBreakdown {
        private int drops;
        private int pets;
        private int milestones;
        private int events;
        private int revalDiaries;
        private int revalChallenges;
        private int total;
    }

    /**
''', r'''     */
    @Data
    public static class PointsBreakdown {
        private double drops;
        private double pets;
        private double milestones;
        private double events;
        private double revalDiaries;
        private double revalChallenges;
        private double total;
    }

    /**
''')
        source = one(source, r'''        private Integer id;
        private Integer osrsAccountId;
        private String pointType; // 'activity' | 'maintenance'
        private Integer pointsChange;
        private Integer pointsAfter;
        private String sourceType; // 'drop' | 'pet' | 'milestone' | 'manual' | 'event' | 'decay' | 'misc'
        private Integer sourceId;
        private Integer itemId;
''', r'''        private Integer id;
        private Integer osrsAccountId;
        private String pointType; // 'activity' | 'maintenance'
        private Double pointsChange;
        private Double pointsAfter;
        private String sourceType; // 'drop' | 'pet' | 'milestone' | 'manual' | 'event' | 'decay' | 'misc'
        private Integer sourceId;
        private Integer itemId;
''')
    if path == 'src/main/java/com/revalclan/notifiers/DiaryNotifier.java':
        source = one(source, r'''
	private final Map<Integer, Integer> diaryCompletionById = new ConcurrentHashMap<>();
	private int initDelayTicks = 0;

	@Override
	public boolean isEnabled() {
''', r'''
	private final Map<Integer, Integer> diaryCompletionById = new ConcurrentHashMap<>();
	private int initDelayTicks = 0;
	private int generation;

	@Override
	public boolean isEnabled() {
''')
        source = one(source, r'''			diaryCompletionById.put(id, value);

			if (isComplete(id, value)) {
				clientThread.invokeLater(() -> {
					handleDiaryCompletion(diaryInfo, id);
					return true;
				});
''', r'''			diaryCompletionById.put(id, value);

			if (isComplete(id, value)) {
				long accountHash = client.getAccountHash();
				int completionGeneration = generation;
				clientThread.invokeLater(() -> {
					if (completionGeneration != generation || client.getGameState() != GameState.LOGGED_IN
						|| client.getAccountHash() != accountHash) return true;
					handleDiaryCompletion(diaryInfo, id);
					return true;
				});
''')
        source = one(source, r'''	}

	public void reset() {
		diaryCompletionById.clear();
		initDelayTicks = 0;
	}
''', r'''	}

	public void reset() {
		generation++;
		diaryCompletionById.clear();
		initDelayTicks = 0;
	}
''')
    if path == 'src/main/java/com/revalclan/notifiers/LootNotifier.java':
        source = one(source, r'''
	/** Forget everything tied to the session that just ended. */
	public void reset() {
		lastInventory.clear();
		lastInventoryKnown = false;
		recentRealLootSources.clear();
''', r'''
	/** Forget everything tied to the session that just ended. */
	public void reset() {
		pendingLoot.clear();
		recentClogItems.clear();
		recentSelfDrops.clear();
		recentUnequips.clear();
		equipmentSnapshot.clear();
		tickCounter = 0;
		lastInventory.clear();
		lastInventoryKnown = false;
		recentRealLootSources.clear();
''')
        source = one(source, r'''		if (itemsList.isEmpty()) return;

		Map<String, Object> lootData = new HashMap<>();
		lootData.put("source", source);
		lootData.put("sourceType", sourceType);
		if (sourceId != null) {
''', r'''		if (itemsList.isEmpty()) return;

		Map<String, Object> lootData = new HashMap<>();
		// Distinct drops can flush in the same millisecond; retries retain this identity.
		lootData.put("eventId", UUID.randomUUID().toString());
		lootData.put("source", source);
		lootData.put("sourceType", sourceType);
		if (sourceId != null) {
''')
    if path == 'src/main/java/com/revalclan/notifiers/SyncNotifier.java':
        source = one(source, r'''
	private static SyncResult resultFor(JsonObject response, Object fingerprint) {
		try {
			if (!response.has("ok") || !response.get("ok").getAsBoolean()
				|| !response.has("status") || !"success".equals(response.get("status").getAsString())) {
				return SyncResult.FAILED;
			}
''', r'''
	private static SyncResult resultFor(JsonObject response, Object fingerprint) {
		try {
			boolean accepted = isTrue(response, "accepted");
			boolean legacyOk = isTrue(response, "ok");
			if ((!accepted && !legacyOk) || (response.has("accepted") && !accepted)
				|| (response.has("ok") && !legacyOk)
				|| !response.has("status") || !"success".equals(response.get("status").getAsString())) {
				return SyncResult.FAILED;
			}
''')
        source = one(source, r'''		}
	}

	private static boolean matchesFingerprint(JsonObject response, Object fingerprint) {
		try {
			return fingerprint != null && response.has("sync") && response.get("sync").isJsonObject()
''', r'''		}
	}

	private static boolean isTrue(JsonObject response, String key) {
		return response.has(key) && response.get(key).isJsonPrimitive()
			&& response.getAsJsonPrimitive(key).isBoolean() && response.get(key).getAsBoolean();
	}

	private static boolean matchesFingerprint(JsonObject response, Object fingerprint) {
		try {
			return fingerprint != null && response.has("sync") && response.get("sync").isJsonObject()
''')
    if path == 'src/main/java/com/revalclan/session/SessionTracker.java':
        source = one(source, r'''		inFlight.add(id);
		Map<String, Object> payload = envelope("SESSION_SUMMARY", persisted.username, persisted.accountHash, persisted.world, persisted.worldFlags);
		payload.put("sessionSummary", persisted.summary);
		webhookService.sendDataAsync(payload, response -> confirmDelivered(id, response));
		log.info("Sending session {} ({})", id, persisted.summary.get("endReason"));
	}

''', r'''		inFlight.add(id);
		Map<String, Object> payload = envelope("SESSION_SUMMARY", persisted.username, persisted.accountHash, persisted.world, persisted.worldFlags);
		payload.put("sessionSummary", persisted.summary);
		webhookService.sendDataAsync(payload, null, response -> confirmDelivered(id, response),
			error -> confirmDelivered(id, null));
		log.info("Sending session {} ({})", id, persisted.summary.get("endReason"));
	}

''')
    if path == 'src/main/java/com/revalclan/ui/PointsAlbumWindow.java':
        source = one(source, r'''		String origin = " ";
		Integer itemId;
		String sourceType;
		int points;
		int count;
		String latestDate = "";
	}
''', r'''		String origin = " ";
		Integer itemId;
		String sourceType;
		double points;
		int count;
		String latestDate = "";
	}
''')
        source = one(source, r'''		String query = searchField.getText() != null ? searchField.getText().trim().toLowerCase() : "";

		List<CardData> cards = new ArrayList<>();
		long totalPts = 0;
		int totalEntries = 0;
		Map<String, CardData> grouped = new LinkedHashMap<>();

''', r'''		String query = searchField.getText() != null ? searchField.getText().trim().toLowerCase() : "";

		List<CardData> cards = new ArrayList<>();
		double totalPts = 0;
		int totalEntries = 0;
		Map<String, CardData> grouped = new LinkedHashMap<>();

''')
        source = one(source, r'''		}

		Comparator<CardData> byDate = Comparator.comparing(c -> c.latestDate);
		Comparator<CardData> byPoints = Comparator.comparingInt(c -> c.points);
		switch (sortCombo.getSelectedIndex()) {
			case 1: cards.sort(byDate); break;
			case 2: cards.sort(byPoints.reversed()); break;
''', r'''		}

		Comparator<CardData> byDate = Comparator.comparing(c -> c.latestDate);
		Comparator<CardData> byPoints = Comparator.comparingDouble(c -> c.points);
		switch (sortCombo.getSelectedIndex()) {
			case 1: cards.sort(byDate); break;
			case 2: cards.sort(byPoints.reversed()); break;
''')
    if path == 'src/main/java/com/revalclan/ui/ProfilePanel.java':
        source = one(source, r'''
	private void fetchRanks() {
		if (apiService == null) return;
		apiService.fetchPoints(
			response -> {
				if (response.getData() != null) {
					pointsData = response.getData();
					if (response.getData().getRanks() != null) {
						ranks = response.getData().getRanks();
					}
					SwingUtilities.invokeLater(() -> {
						if (currentAccount != null) buildProfile();
					});
				}
			},
			error -> {}
		);
	}
''', r'''
	private void fetchRanks() {
		if (apiService == null) return;
		long generation = accountRequestGeneration.get();
		apiService.fetchPoints(
			response -> SwingUtilities.invokeLater(() -> {
				if (generation != accountRequestGeneration.get()) return;
				if (response.getData() != null) {
					pointsData = response.getData();
					if (response.getData().getRanks() != null) {
						ranks = response.getData().getRanks();
					}
					if (currentAccount != null) buildProfile();
				}
			}),
			error -> {}
		);
	}
''')
        source = one(source, r'''		isLoading = false;
		currentAccount = null;
		pointsLog = null;
		disposeAlbum();
		showNotLoggedIn();
	}
''', r'''		isLoading = false;
		currentAccount = null;
		pointsLog = null;
		pointsData = null;
		ranks = new java.util.ArrayList<>();
		disposeAlbum();
		showNotLoggedIn();
	}
''')
        source = one(source, r'''
	public void refresh() {
		if (apiService != null && client != null) {
			apiService.clearAccountCache();
			loadCurrentAccount();
		}
''', r'''
	public void refresh() {
		if (apiService != null && client != null) {
			isLoading = false;
			apiService.clearAccountCache();
			loadCurrentAccount();
		}
''')
        source = one(source, r'''		pointsDisplay.setLayout(new BoxLayout(pointsDisplay, BoxLayout.Y_AXIS));
		pointsDisplay.setOpaque(false);

		int points = account.getActivityPoints() != null ? account.getActivityPoints() : 0;
		JLabel pointsValue = new JLabel(NumberFmt.group(points));
		pointsValue.setFont(FontManager.getRunescapeBoldFont());
		pointsValue.setForeground(UIConstants.ACCENT_GOLD);
''', r'''		pointsDisplay.setLayout(new BoxLayout(pointsDisplay, BoxLayout.Y_AXIS));
		pointsDisplay.setOpaque(false);

		double points = account.getActivityPoints() != null ? account.getActivityPoints() : 0;
		JLabel pointsValue = new JLabel(NumberFmt.group(points));
		pointsValue.setFont(FontManager.getRunescapeBoldFont());
		pointsValue.setForeground(UIConstants.ACCENT_GOLD);
''')
        source = one(source, r'''	private JPanel buildRankProgressBar(AccountResponse.OsrsAccount account) {
		if (ranks == null || ranks.isEmpty()) return null;

		int currentPoints = account.getActivityPoints() != null ? account.getActivityPoints() : 0;
		String currentRank = account.getClanRank();

		PointsResponse.Rank nextRank = null;
''', r'''	private JPanel buildRankProgressBar(AccountResponse.OsrsAccount account) {
		if (ranks == null || ranks.isEmpty()) return null;

		double currentPoints = account.getActivityPoints() != null ? account.getActivityPoints() : 0;
		String currentRank = account.getClanRank();

		PointsResponse.Rank nextRank = null;
''')
        source = one(source, r'''		}

		int pointsNeeded = nextRank.getPointsRequired() - previousRankPoints;
		int pointsProgress = currentPoints - previousRankPoints;
		double progress = Math.min(1.0, Math.max(0.0, pointsNeeded > 0 ? (double) pointsProgress / pointsNeeded : 0));
		int pointsRemaining = nextRank.getPointsRequired() - currentPoints;
		boolean needsRankUp = pointsRemaining < 0;

		// Clickable sub-panel: hover highlight, opens the ranks page on the website
''', r'''		}

		int pointsNeeded = nextRank.getPointsRequired() - previousRankPoints;
		double pointsProgress = currentPoints - previousRankPoints;
		double progress = Math.min(1.0, Math.max(0.0, pointsNeeded > 0 ? (double) pointsProgress / pointsNeeded : 0));
		double pointsRemaining = nextRank.getPointsRequired() - currentPoints;
		boolean needsRankUp = pointsRemaining < 0;

		// Clickable sub-panel: hover highlight, opens the ranks page on the website
''')
        source = one(source, r'''		bottomRow.add(createStatCard(formatNumber(breakdown.getRevalChallenges()), "Challenges", UIConstants.ACCENT_GREEN, "reval_challenge"));
		bottomRow.add(createStatCard(formatNumber(breakdown.getEvents()), "Events", UIConstants.ACCENT_BLUE, "event"));

		long miscPoints = breakdown.getTotal()
			- breakdown.getDrops() - breakdown.getPets() - breakdown.getMilestones()
			- breakdown.getEvents() - breakdown.getRevalDiaries() - breakdown.getRevalChallenges();

''', r'''		bottomRow.add(createStatCard(formatNumber(breakdown.getRevalChallenges()), "Challenges", UIConstants.ACCENT_GREEN, "reval_challenge"));
		bottomRow.add(createStatCard(formatNumber(breakdown.getEvents()), "Events", UIConstants.ACCENT_BLUE, "event"));

		double miscPoints = breakdown.getTotal()
			- breakdown.getDrops() - breakdown.getPets() - breakdown.getMilestones()
			- breakdown.getEvents() - breakdown.getRevalDiaries() - breakdown.getRevalChallenges();

''')
        source = one(source, r'''		contentPanel.repaint();
	}

	private String formatNumber(long num) {
		if (num >= 1_000_000) return new DecimalFormat("#.#M").format(num / 1_000_000.0);
		if (num >= 1_000) return new DecimalFormat("#.#K").format(num / 1_000.0);
		return String.valueOf(num);
	}

	private String formatDecimal(double num) {
''', r'''		contentPanel.repaint();
	}

	private String formatNumber(double num) {
		return NumberFmt.group(num);
	}

	private String formatDecimal(double num) {
''')
    if path == 'src/main/java/com/revalclan/ui/AchievementsPanel.java':
        source = one(source, r'''	private final JPanel contentPanel;
	private RefreshButton refreshBtn;
	private List<AchievementsResponse.Achievement> achievements = new ArrayList<>();

	public AchievementsPanel() {
		setLayout(new BorderLayout());
''', r'''	private final JPanel contentPanel;
	private RefreshButton refreshBtn;
	private List<AchievementsResponse.Achievement> achievements = new ArrayList<>();
	private long requestGeneration;

	public AchievementsPanel() {
		setLayout(new BorderLayout());
''')
        source = one(source, r'''	}

	public void onLoggedOut() {
		showNotLoggedIn();
	}

''', r'''	}

	public void onLoggedOut() {
		requestGeneration++;
		achievements = new ArrayList<>();
		refreshBtn = null;
		showNotLoggedIn();
	}

''')
        source = one(source, r'''	}

	private void loadData() {
		if (client == null || client.getAccountHash() == -1 || apiService == null) {
			showNotLoggedIn();
			return;
''', r'''	}

	private void loadData() {
		long generation = ++requestGeneration;
		if (client == null || client.getAccountHash() == -1 || apiService == null) {
			showNotLoggedIn();
			return;
''')
        source = one(source, r'''
		apiService.fetchAchievementDefinitions(client.getAccountHash(),
			response -> SwingUtilities.invokeLater(() -> {
				if (refreshBtn != null) refreshBtn.setLoading(false);
				achievements = (response != null && response.isSuccess() && response.getData() != null)
					? response.getData().getAchievements() : new ArrayList<>();
''', r'''
		apiService.fetchAchievementDefinitions(client.getAccountHash(),
			response -> SwingUtilities.invokeLater(() -> {
				if (generation != requestGeneration) return;
				if (refreshBtn != null) refreshBtn.setLoading(false);
				achievements = (response != null && response.isSuccess() && response.getData() != null)
					? response.getData().getAchievements() : new ArrayList<>();
''')
        source = one(source, r'''				buildUI();
			}),
			error -> SwingUtilities.invokeLater(() -> {
				if (refreshBtn != null) refreshBtn.setLoading(false);
				buildUI();
			})
''', r'''				buildUI();
			}),
			error -> SwingUtilities.invokeLater(() -> {
				if (generation != requestGeneration) return;
				if (refreshBtn != null) refreshBtn.setLoading(false);
				buildUI();
			})
''')
    if path == 'src/main/java/com/revalclan/ui/DiaryPanel.java':
        source = one(source, r'''	private DiariesResponse.Diary selectedDiary = null;
	private final Set<String> expandedTiers = new HashSet<>();
	private RefreshButton refreshButton;

	public DiaryPanel() {
		setLayout(new BorderLayout());
''', r'''	private DiariesResponse.Diary selectedDiary = null;
	private final Set<String> expandedTiers = new HashSet<>();
	private RefreshButton refreshButton;
	private long requestGeneration;

	public DiaryPanel() {
		setLayout(new BorderLayout());
''')
        source = one(source, r'''		showNotLoggedIn();
	}

	public void onLoggedOut() { showNotLoggedIn(); }
	public void refresh() { loadData(); }

	private void loadData() {
		if (apiService == null) return;
		if (refreshButton != null) refreshButton.setLoading(true);

''', r'''		showNotLoggedIn();
	}

	public void onLoggedOut() {
		requestGeneration++;
		allDiaries = new ArrayList<>();
		selectedDiary = null;
		expandedTiers.clear();
		refreshButton = null;
		while (mainContainer.getComponentCount() > 1) mainContainer.remove(1);
		cardLayout.show(mainContainer, "LIST");
		showNotLoggedIn();
	}
	public void refresh() { loadData(); }

	private void loadData() {
		long generation = ++requestGeneration;
		if (apiService == null) return;
		if (refreshButton != null) refreshButton.setLoading(true);

''')
        source = one(source, r'''
		apiService.fetchDiaries(accountHash,
			response -> SwingUtilities.invokeLater(() -> {
				if (refreshButton != null) refreshButton.setLoading(false);
				allDiaries = (response != null && response.getData() != null && response.getData().getDiaries() != null)
					? response.getData().getDiaries() : new ArrayList<>();
''', r'''
		apiService.fetchDiaries(accountHash,
			response -> SwingUtilities.invokeLater(() -> {
				if (generation != requestGeneration) return;
				if (refreshButton != null) refreshButton.setLoading(false);
				allDiaries = (response != null && response.getData() != null && response.getData().getDiaries() != null)
					? response.getData().getDiaries() : new ArrayList<>();
''')
        source = one(source, r'''				}
			}),
			error -> SwingUtilities.invokeLater(() -> {
				if (refreshButton != null) refreshButton.setLoading(false);
				allDiaries = new ArrayList<>();
				buildUI();
''', r'''				}
			}),
			error -> SwingUtilities.invokeLater(() -> {
				if (generation != requestGeneration) return;
				if (refreshButton != null) refreshButton.setLoading(false);
				allDiaries = new ArrayList<>();
				buildUI();
''')
    if path == 'src/main/java/com/revalclan/ui/RankingPanel.java':
        source = one(source, r'''	private final JLabel loadingLabel;
	private final GridBagConstraints gbc;
	private int gridY = 0;

	private RevalApiService apiService;
	private ItemManager itemManager;
''', r'''	private final JLabel loadingLabel;
	private final GridBagConstraints gbc;
	private int gridY = 0;
	private long requestGeneration;

	private RevalApiService apiService;
	private ItemManager itemManager;
''')
        source = one(source, r'''
	public void load() {
		loadData();
	}

	public void refresh() {
''', r'''
	public void load() {
		loadData();
	}

	public void resetSession() {
		requestGeneration++;
		showPlaceholder();
	}

	public void refresh() {
''')
        source = one(source, r'''	}

	private void loadData() {
		showLoading();
		apiService.fetchPoints(
			response -> SwingUtilities.invokeLater(() -> buildContent(response)),
			error -> SwingUtilities.invokeLater(this::showError)
		);
	}

''', r'''	}

	private void loadData() {
		long generation = ++requestGeneration;
		showLoading();
		apiService.fetchPoints(
			response -> SwingUtilities.invokeLater(() -> {
				if (generation == requestGeneration) buildContent(response);
			}),
			error -> SwingUtilities.invokeLater(() -> {
				if (generation == requestGeneration) showError();
			})
		);
	}

''')
    if path == 'src/main/java/com/revalclan/ui/RevalPanel.java':
        source = one(source, r'''		SwingUtilities.invokeLater(() -> {
			clanValidated = true;
			memberTabsLoaded.clear();
			profilePanel.refresh();
			eventsPanel.onLoginReady();
			loadSelectedTab(false);
''', r'''		SwingUtilities.invokeLater(() -> {
			clanValidated = true;
			memberTabsLoaded.clear();
			rankingPanel.resetSession();
			publicLoads.put("RANKING", rankingPanel::load);
			profilePanel.refresh();
			eventsPanel.onLoginReady();
			loadSelectedTab(false);
''')
        source = one(source, r'''		SwingUtilities.invokeLater(() -> {
			clanValidated = false;
			memberTabsLoaded.clear();
			competitionsPanel.onLoggedOut();
			profilePanel.onLoggedOut();
			achievementsPanel.onLoggedOut();
''', r'''		SwingUtilities.invokeLater(() -> {
			clanValidated = false;
			memberTabsLoaded.clear();
			rankingPanel.resetSession();
			publicLoads.put("RANKING", rankingPanel::load);
			competitionsPanel.onLoggedOut();
			profilePanel.onLoggedOut();
			achievementsPanel.onLoggedOut();
''')
    if path == 'src/main/java/com/revalclan/util/NumberFmt.java':
        source = one(source, r'''import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Formats integers with space grouping ("12 345") independent of the default locale. */
public final class NumberFmt {
	private NumberFmt() {
	}
''', r'''import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Formats numbers with space grouping ("12 345") independent of the default locale. */
public final class NumberFmt {
	private NumberFmt() {
	}
''')
        source = one(source, r'''		symbols.setGroupingSeparator(' ');
		return new DecimalFormat("#,##0", symbols).format(value);
	}
}
''', r'''		symbols.setGroupingSeparator(' ');
		return new DecimalFormat("#,##0", symbols).format(value);
	}

	/** Point balances and ledger deltas retain the backend's four-decimal precision. */
	public static String group(double value) {
		DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.ROOT);
		symbols.setGroupingSeparator(' ');
		// Arithmetic on category totals can leave a tiny negative remainder at zero.
		return new DecimalFormat("#,##0.####", symbols).format(Math.abs(value) < 0.00005 ? 0 : value);
	}
}
''')
    return source
