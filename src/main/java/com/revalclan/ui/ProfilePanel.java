package com.revalclan.ui;

import com.revalclan.RevalClanConfig;
import com.revalclan.api.RevalApiService;
import com.revalclan.api.account.AccountResponse;
import com.revalclan.api.points.PointsResponse;
import com.revalclan.ui.components.ChecklistItem;
import com.revalclan.ui.components.LoginPrompt;
import com.revalclan.ui.constants.UIConstants;
import com.revalclan.ui.components.ArrowIcon;
import com.revalclan.ui.components.BlockButton;
import com.revalclan.ui.components.Badge;
import com.revalclan.ui.components.Clickable;
import com.revalclan.util.ClanRankIconResolver;
import com.revalclan.util.NumberFmt;
import com.revalclan.util.UIAssetLoader;
import net.runelite.api.Client;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.FontManager;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

public class ProfilePanel extends JPanel {
	private final JPanel contentPanel;
	private final GridBagConstraints gbc;
	private int gridY = 0;

	private RevalApiService apiService;
	private Client client;
	private UIAssetLoader assetLoader;
	private RevalClanConfig config;
	private ItemManager itemManager;
	private ClanRankIconResolver rankIconResolver;
	private Runnable onOpenRanks;
	private Runnable onSyncGuide;
    private Runnable onResync;
    public void setOnResync(Runnable action) { onResync = action; }
	private Consumer<AccountResponse.AccountData> onAccountLoaded;

	private AccountResponse.AccountData currentAccount;
	private PointsAlbumWindow albumWindow;
	private List<PointsResponse.Rank> ranks;
	private PointsResponse.PointsData pointsData;
	private List<AccountResponse.PointsLogEntry> pointsLog;
	private boolean isLoading = false;
    private volatile long loadGeneration = 0;

	public ProfilePanel() {
		setLayout(new BorderLayout());
		setBackground(UIConstants.BACKGROUND);

		contentPanel = new JPanel(new GridBagLayout());
		contentPanel.setBackground(UIConstants.BACKGROUND);
		contentPanel.setBorder(new EmptyBorder(4, 2, 4, 2));

		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0;
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.insets = new Insets(2, 0, 2, 0);

		JPanel wrapper = new JPanel(new BorderLayout()) {
			@Override
			public Dimension getPreferredSize() {
				Dimension size = super.getPreferredSize();
				if (getParent() != null) size.width = getParent().getWidth();
				return size;
			}
		};
		wrapper.setBackground(UIConstants.BACKGROUND);
		wrapper.add(contentPanel, BorderLayout.NORTH);

		JScrollPane scrollPane = new JScrollPane(wrapper);
		scrollPane.setBackground(UIConstants.BACKGROUND);
		scrollPane.setBorder(null);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.getVerticalScrollBar().setUnitIncrement(16);
		scrollPane.getViewport().setBackground(UIConstants.BACKGROUND);

		showNotLoggedIn();
		add(scrollPane, BorderLayout.CENTER);
	}

	private void addComponent(JComponent comp) {
		gbc.gridy = gridY++;
		contentPanel.add(comp, gbc);
	}

	private void addSpacing(int height) {
		gbc.gridy = gridY++;
		contentPanel.add(Box.createVerticalStrut(height), gbc);
	}

	public void init(RevalApiService apiService, Client client, UIAssetLoader assetLoader, RevalClanConfig config,
					 ItemManager itemManager, ClanRankIconResolver rankIconResolver) {
		this.apiService = apiService;
		this.client = client;
		this.assetLoader = assetLoader;
		this.config = config;
		this.itemManager = itemManager;
		this.rankIconResolver = rankIconResolver;
		fetchRanks();
	}

	/** Where the rank-up bar navigates (the Ranking side-panel view) */
	public void setOnOpenRanks(Runnable onOpenRanks) {
		this.onOpenRanks = onOpenRanks;
	}

	/** Arms the in-game collection log sync guide (points album button) */
	public void setOnSyncGuide(Runnable onSyncGuide) {
		this.onSyncGuide = onSyncGuide;
	}

	public void setOnAccountLoaded(Consumer<AccountResponse.AccountData> callback) {
		this.onAccountLoaded = callback;
	}

    private void fetchRanks() {
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

    public void resetConnection() {
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
    }

	public void loadCurrentAccount() {
		loadCurrentAccount(false);
	}

	public void loadCurrentAccount(boolean retry) {
		if (client == null || apiService == null) {
			showNotLoggedIn();
			return;
		}

		long accountHash = client.getAccountHash();
		if (accountHash == -1) {
			if (retry) {
				showError("Not logged into RuneLite account");
			} else {
				Timer timer = new Timer(2000, e -> loadCurrentAccount(true));
				timer.setRepeats(false);
				timer.start();
				showLoading();
			}
			return;
		}
		loadAccount(accountHash);
	}

	public void loadAccount(long accountHash) {
		if (isLoading) return;
		isLoading = true;
		showLoading();

        final long requestGeneration = loadGeneration;
		apiService.fetchAccount(accountHash,
			response -> {
                if (requestGeneration != loadGeneration) return;
				isLoading = false;
				SwingUtilities.invokeLater(() -> {
                    if (requestGeneration != loadGeneration) return;
					currentAccount = response.getData();
					if (currentAccount != null) {
						pointsLog = currentAccount.getPointsLog();
						if (onAccountLoaded != null) onAccountLoaded.accept(currentAccount);
					}
					if (pointsData != null) buildProfile();
					if (ranks == null || ranks.isEmpty() || pointsData == null) fetchRanks();
				});
			},
			error -> {
                if (requestGeneration != loadGeneration) return;
				isLoading = false;
				SwingUtilities.invokeLater(() -> { if (requestGeneration == loadGeneration) showError(error.getMessage() != null ? error.getMessage() : "Failed to fetch account data"); });
			}
		);
	}

	public void loadAccountById(int osrsAccountId) {
		if (isLoading) return;
		isLoading = true;
		showLoading();

        final long requestGeneration = loadGeneration;
		apiService.fetchAccountById(osrsAccountId,
			response -> {
                if (requestGeneration != loadGeneration) return;
				isLoading = false;
				SwingUtilities.invokeLater(() -> {
                    if (requestGeneration != loadGeneration) return;
					currentAccount = response.getData();
					if (currentAccount != null) pointsLog = currentAccount.getPointsLog();
					if (pointsData != null) buildProfile();
					if (ranks == null || ranks.isEmpty() || pointsData == null) fetchRanks();
				});
			},
			error -> {
                if (requestGeneration != loadGeneration) return;
				isLoading = false;
				SwingUtilities.invokeLater(() -> { if (requestGeneration == loadGeneration) showError(error.getMessage() != null ? error.getMessage() : "Player not found"); });
			}
		);
	}

	public void refresh() {
		if (apiService != null && client != null) {
			apiService.clearAccountCache();
			loadCurrentAccount();
		}
	}

	/**
	 * Rebuilds the profile UI from cached data without re-fetching from the API.
	 * Used when config changes (e.g. hide completed items toggle).
	 */
	public void rebuild() {
		if (currentAccount != null && pointsData != null) {
			SwingUtilities.invokeLater(this::buildProfile);
		}
	}

	public String getClanRank() {
		return (currentAccount != null && currentAccount.getOsrsAccount() != null) 
			? currentAccount.getOsrsAccount().getClanRank() : null;
	}

	public boolean isAccountLoaded() {
		return currentAccount != null && currentAccount.getOsrsAccount() != null;
	}

	private void showNotLoggedIn() {
		contentPanel.removeAll();
		gridY = 0;
		addComponent(new LoginPrompt("Profile"));
		revalidateAndRepaint();
	}

	private void showLoading() {
		contentPanel.removeAll();
		gridY = 0;

		JPanel placeholder = createCenteredPanel();
		placeholder.setBorder(new EmptyBorder(50, 20, 20, 20));

		JLabel loading = new JLabel("Loading profile...");
		loading.setFont(FontManager.getRunescapeSmallFont());
		loading.setForeground(UIConstants.TEXT_SECONDARY);
		loading.setAlignmentX(Component.CENTER_ALIGNMENT);

		placeholder.add(loading);
		addComponent(placeholder);
		revalidateAndRepaint();
	}

	private void showError(String message) {
		contentPanel.removeAll();
		gridY = 0;

		JPanel placeholder = createCenteredPanel();
		placeholder.setBorder(new EmptyBorder(30, 20, 20, 20));

		JLabel errorIcon = new JLabel("!");
		errorIcon.setFont(FontManager.getRunescapeBoldFont());
		errorIcon.setForeground(UIConstants.ERROR_COLOR);
		errorIcon.setAlignmentX(Component.CENTER_ALIGNMENT);

		JTextArea errorLabel = new JTextArea(message);
        errorLabel.setEditable(false);
        errorLabel.setFocusable(false);
        errorLabel.setOpaque(false);
        errorLabel.setLineWrap(true);
        errorLabel.setWrapStyleWord(true);
        errorLabel.setColumns(0);
        errorLabel.setRows(4);
        errorLabel.setMinimumSize(new Dimension(0, 56));
		errorLabel.setFont(FontManager.getRunescapeSmallFont());
		errorLabel.setForeground(UIConstants.ERROR_COLOR);
		errorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

		JTextArea hint = new JTextArea("Check the connection settings, then press Retry.");
        hint.setEditable(false);
        hint.setFocusable(false);
        hint.setOpaque(false);
        hint.setLineWrap(true);
        hint.setWrapStyleWord(true);
        hint.setColumns(0);
        hint.setRows(3);
        hint.setMinimumSize(new Dimension(0, 42));
		hint.setFont(FontManager.getRunescapeSmallFont());
		hint.setForeground(UIConstants.TEXT_SECONDARY);
		hint.setAlignmentX(Component.CENTER_ALIGNMENT);

		placeholder.add(errorIcon);
		placeholder.add(Box.createRigidArea(new Dimension(0, 6)));
		placeholder.add(errorLabel);
		placeholder.add(Box.createRigidArea(new Dimension(0, 6)));
		placeholder.add(hint);
        JButton retry = new BlockButton("Retry", UIConstants.ACCENT_GOLD, 26);
        retry.addActionListener(event -> refresh());
        placeholder.add(Box.createRigidArea(new Dimension(0, 8)));
        placeholder.add(retry);

		addComponent(placeholder);
		revalidateAndRepaint();
	}

	private void buildProfile() {
		contentPanel.removeAll();
		gridY = 0;

		if (currentAccount == null || currentAccount.getOsrsAccount() == null) {
			showError("No profile data");
			return;
		}

		AccountResponse.OsrsAccount account = currentAccount.getOsrsAccount();

		addComponent(buildHeaderSection(account));
		addSpacing(6);

		if (currentAccount.getPointsBreakdown() != null) {
			addComponent(buildPointsSection(currentAccount.getPointsBreakdown()));
			addSpacing(6);
		}

		addComponent(buildStatsSection(account));
		addSpacing(6);
		addComponent(buildMilestonesSection(currentAccount.getMilestones()));
		addSpacing(6);
		addComponent(buildCombatAchievementsSection());
		addSpacing(6);
		addComponent(buildCollectionLogSection());
		addSpacing(6);
		addComponent(buildItemChecklistSection("Monkey Backpacks", "MONKEY_BACKPACKS"));
		addSpacing(6);
		addComponent(buildItemChecklistSection("ToA Capes", "TOA_CAPES"));
		addSpacing(6);
		addComponent(buildItemChecklistSection("ToB Capes", "TOB_CAPES"));
		addSpacing(6);
		addComponent(buildItemChecklistSection("CM Capes", "CM_CAPES"));
		addSpacing(16);

		// Add filler to absorb extra space and keep content at top
		gbc.gridy = gridY;
		gbc.weighty = 1.0;
		contentPanel.add(Box.createGlue(), gbc);

		revalidateAndRepaint();
	}

	private JPanel buildHeaderSection(AccountResponse.OsrsAccount account) {
		JPanel header = new JPanel();
		header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
		header.setBackground(UIConstants.CARD_BG);
		header.setBorder(new EmptyBorder(10, 12, 10, 12));

		// Top row: rank icon + nickname on the left, points on the right
		JPanel topRow = new JPanel(new BorderLayout(8, 0));
		topRow.setOpaque(false);
		topRow.setAlignmentX(Component.LEFT_ALIGNMENT);

		JPanel namePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
		namePanel.setOpaque(false);

		JLabel rankIcon = new JLabel();
		rankIcon.setPreferredSize(new Dimension(20, 20));
		rankIcon.setToolTipText(getRankDisplayName(account.getClanRank()));
		if (rankIconResolver != null && account.getClanRank() != null) {
			rankIconResolver.apply(account.getClanRank(), rankIcon, 20);
		}
		namePanel.add(rankIcon);

		JLabel nameLabel = new JLabel(account.getOsrsNickname());
		nameLabel.setFont(FontManager.getRunescapeBoldFont());
		nameLabel.setForeground(UIConstants.TEXT_PRIMARY);
		namePanel.add(nameLabel);

		JPanel pointsDisplay = new JPanel();
		pointsDisplay.setLayout(new BoxLayout(pointsDisplay, BoxLayout.Y_AXIS));
		pointsDisplay.setOpaque(false);

		int points = account.getActivityPoints() != null ? account.getActivityPoints() : 0;
		JLabel pointsValue = new JLabel(NumberFmt.group(points));
		pointsValue.setFont(FontManager.getRunescapeBoldFont());
		pointsValue.setForeground(UIConstants.ACCENT_GOLD);
		pointsValue.setAlignmentX(Component.RIGHT_ALIGNMENT);

		JLabel pointsLabel = new JLabel("NightLegion Points");
		pointsLabel.setFont(FontManager.getRunescapeSmallFont());
		pointsLabel.setForeground(UIConstants.TEXT_SECONDARY);
		pointsLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);

		pointsDisplay.add(pointsValue);
		pointsDisplay.add(pointsLabel);

		topRow.add(namePanel, BorderLayout.CENTER);
		topRow.add(pointsDisplay, BorderLayout.EAST);
		header.add(topRow);
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

		JPanel rankProgress = buildRankProgressBar(account);
		if (rankProgress != null) {
			header.add(Box.createRigidArea(new Dimension(0, 8)));
			rankProgress.setAlignmentX(Component.LEFT_ALIGNMENT);
			header.add(rankProgress);
		}

		// Leaderboard profiles have no client to refresh from
		if (client != null) {
			header.add(Box.createRigidArea(new Dimension(0, 8)));
			JButton refreshButton = buildRefreshProfileButton();
			refreshButton.setAlignmentX(Component.LEFT_ALIGNMENT);
			header.add(refreshButton);
            if (currentAccount.getSyncNotes() != null) {
                refreshButton.setToolTipText(String.join(" ", currentAccount.getSyncNotes()));
            }
		}

		if (onSyncGuide != null) {
			header.add(Box.createRigidArea(new Dimension(0, 6)));
			JButton syncButton = buildSyncGuideButton();
			syncButton.setAlignmentX(Component.LEFT_ALIGNMENT);
			header.add(syncButton);
		}

		return wrapInRoundedPanel(header);
	}

	/** Arms the in-game collection log sync guide */
	private JButton buildSyncGuideButton() {
		JButton btn = new BlockButton("Sync missing points", UIConstants.TEXT_SECONDARY, 24);
		btn.setToolTipText("Resync available progress, then open Collection Log / All Pets for historical pets. Open your bank for stored milestone items.");
		btn.addActionListener(e -> {
			if (onResync != null) onResync.run();
            if (onSyncGuide != null) onSyncGuide.run();
			btn.setText("Check your Collection Log in-game");
			Timer reset = new Timer(4000, ev -> btn.setText("Sync missing points"));
			reset.setRepeats(false);
			reset.start();
		});
		return btn;
	}

	/** Full-width block button so the refresh action stands out */
	private JButton buildRefreshProfileButton() {
		JButton btn = new BlockButton("Refresh Profile", UIConstants.ACCENT_GOLD, 26);
		btn.addActionListener(e -> {
			btn.setText("Refreshing...");
			btn.setEnabled(false);
			refresh();
		});
		return btn;
	}

	private JPanel buildRankProgressBar(AccountResponse.OsrsAccount account) {
		if (ranks == null || ranks.isEmpty()) return null;

		int currentPoints = account.getActivityPoints() != null ? account.getActivityPoints() : 0;
		String currentRank = account.getCalculatedRank() != null ? account.getCalculatedRank() : account.getClanRank();

		PointsResponse.Rank nextRank = null;
		int previousRankPoints = 0;

		List<PointsResponse.Rank> sortedRanks = new ArrayList<>(ranks);
		sortedRanks.sort(Comparator.comparingInt(PointsResponse.Rank::getPointsRequired));

		for (int i = 0; i < sortedRanks.size(); i++) {
			PointsResponse.Rank rank = sortedRanks.get(i);
			if (rank.getName().equalsIgnoreCase(currentRank) ||
				(rank.getDisplayName() != null && rank.getDisplayName().equalsIgnoreCase(currentRank))) {
				previousRankPoints = rank.getPointsRequired();
				if (i + 1 < sortedRanks.size()) nextRank = sortedRanks.get(i + 1);
				break;
			}
		}

		if (nextRank == null) {
			for (PointsResponse.Rank rank : sortedRanks) {
				if (rank.getPointsRequired() > currentPoints) {
					nextRank = rank;
					break;
				}
				previousRankPoints = rank.getPointsRequired();
			}
		}

		if (nextRank == null) {
			JPanel maxRank = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
			maxRank.setOpaque(false);
			JLabel maxLabel = new JLabel("Max Rank!");
			maxLabel.setFont(FontManager.getRunescapeSmallFont());
			maxLabel.setForeground(UIConstants.ACCENT_GOLD);
			maxRank.add(maxLabel);
			return maxRank;
		}

		int pointsNeeded = nextRank.getPointsRequired() - previousRankPoints;
		int pointsProgress = currentPoints - previousRankPoints;
		double progress = Math.min(1.0, Math.max(0.0, pointsNeeded > 0 ? (double) pointsProgress / pointsNeeded : 0));
		int pointsRemaining = nextRank.getPointsRequired() - currentPoints;
		boolean needsRankUp = pointsRemaining < 0;

		// Clickable sub-panel: hover highlight, opens the ranks page on the website
		final boolean[] hovered = { false };
		JPanel progressPanel = new JPanel() {
			@Override
			protected void paintComponent(Graphics g) {
				Graphics2D g2d = (Graphics2D) g.create();
				g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2d.setColor(hovered[0] ? UIConstants.CARD_HOVER : UIConstants.BACKGROUND);
				g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
				g2d.dispose();
				super.paintComponent(g);
			}
		};
		progressPanel.setLayout(new BoxLayout(progressPanel, BoxLayout.Y_AXIS));
		progressPanel.setOpaque(false);
		progressPanel.setBorder(new EmptyBorder(6, 8, 8, 8));
		if (onOpenRanks != null) {
			progressPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			progressPanel.setToolTipText("View all ranks");
		}

		JPanel labelRow = new JPanel(new BorderLayout(4, 0));
		labelRow.setOpaque(false);
		labelRow.setAlignmentX(Component.LEFT_ALIGNMENT);

		JPanel labelLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
		labelLeft.setOpaque(false);

		JLabel progressLabel = new JLabel(NumberFmt.group(pointsRemaining) + " pts to");
		progressLabel.setFont(FontManager.getRunescapeSmallFont());
		progressLabel.setForeground(UIConstants.TEXT_SECONDARY);
		labelLeft.add(progressLabel);

		JLabel nextRankIcon = new JLabel();
		nextRankIcon.setPreferredSize(new Dimension(14, 14));
		if (rankIconResolver != null) {
			rankIconResolver.apply(nextRank.getName(), nextRankIcon, 14);
		}
		labelLeft.add(nextRankIcon);

		String nextRankName = nextRank.getDisplayName() != null ? nextRank.getDisplayName() : nextRank.getName();
		JLabel nextRankLabel = new JLabel(nextRankName);
		nextRankLabel.setFont(FontManager.getRunescapeSmallFont());
		nextRankLabel.setForeground(UIConstants.ACCENT_GOLD);
		labelLeft.add(nextRankLabel);

		if (needsRankUp) {
			ImageIcon infoIcon = assetLoader != null ? assetLoader.getIcon("info.png", 12) : null;
			if (infoIcon != null) {
				JLabel infoIconLabel = new JLabel(infoIcon);
				infoIconLabel.setToolTipText("Waiting for a staff member to give you the correct rank");
				labelLeft.add(infoIconLabel);
			}
		}

		labelRow.add(labelLeft, BorderLayout.WEST);
		labelRow.add(new JLabel(new ArrowIcon(11, UIConstants.TEXT_MUTED)), BorderLayout.EAST);

		JProgressBar bar = new JProgressBar(0, 100);
		bar.setValue((int) (progress * 100));
		bar.setBackground(UIConstants.PROGRESS_BG);
		bar.setForeground(UIConstants.ACCENT_GOLD);
		bar.setBorderPainted(false);
		bar.setPreferredSize(new Dimension(100, 5));
		bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 5));
		bar.setAlignmentX(Component.LEFT_ALIGNMENT);

		progressPanel.add(labelRow);
		progressPanel.add(Box.createRigidArea(new Dimension(0, 4)));
		progressPanel.add(bar);
		progressPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, progressPanel.getPreferredSize().height));

		if (onOpenRanks != null) {
			Clickable.onPress(progressPanel, onOpenRanks, h -> hovered[0] = h);
		}

		return progressPanel;
	}

	private JPanel buildPointsSection(AccountResponse.PointsBreakdown breakdown) {
		JPanel section = new JPanel();
		section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
		section.setOpaque(false);

		JPanel labelHeader = new JPanel(new BorderLayout());
		labelHeader.setOpaque(false);
		labelHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
		labelHeader.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));

		JLabel pointsFromLabel = new JLabel("Points from:");
		pointsFromLabel.setFont(FontManager.getRunescapeBoldFont());
		pointsFromLabel.setForeground(UIConstants.ACCENT_GOLD);
		labelHeader.add(pointsFromLabel, BorderLayout.WEST);

		section.add(labelHeader);
		section.add(Box.createRigidArea(new Dimension(0, 6)));

		JPanel topRow = new JPanel(new GridLayout(1, 3, 4, 0));
		topRow.setOpaque(false);
		topRow.setAlignmentX(Component.LEFT_ALIGNMENT);
		topRow.add(createStatCard(formatNumber(breakdown.getDrops()), "Drops", UIConstants.ACCENT_GREEN, "drop"));
		topRow.add(createStatCard(formatNumber(breakdown.getPets()), "Pets", UIConstants.ACCENT_PURPLE, "pet"));
		topRow.add(createStatCard(formatNumber(breakdown.getMilestones()), "Milestones", UIConstants.ACCENT_BLUE, "milestone"));

		JPanel bottomRow = new JPanel(new GridLayout(1, 3, 4, 0));
		bottomRow.setOpaque(false);
		bottomRow.setAlignmentX(Component.LEFT_ALIGNMENT);
		bottomRow.add(createStatCard(formatNumber(breakdown.getRevalDiaries()), "Diaries", UIConstants.ACCENT_GOLD, "reval_diary"));
		bottomRow.add(createStatCard(formatNumber(breakdown.getRevalChallenges()), "Challenges", UIConstants.ACCENT_GREEN, "reval_challenge"));
		bottomRow.add(createStatCard(formatNumber(breakdown.getEvents()), "Events", UIConstants.ACCENT_BLUE, "event"));

		long miscPoints = breakdown.getMisc() != null ? breakdown.getMisc() : breakdown.getTotal()
			- breakdown.getDrops() - breakdown.getPets() - breakdown.getMilestones()
			- breakdown.getEvents() - breakdown.getRevalDiaries() - breakdown.getRevalChallenges();

		JPanel miscRow = new JPanel(new GridLayout(1, 1));
		miscRow.setOpaque(false);
		miscRow.setAlignmentX(Component.LEFT_ALIGNMENT);
		JPanel miscCard = createStatCard(formatNumber(miscPoints), "Misc", UIConstants.TEXT_SECONDARY, "misc");
        if (breakdown.getUnreconciledPoints() != null && breakdown.getUnreconciledPoints() != 0) {
            miscCard.setToolTipText("Includes " + breakdown.getUnreconciledPoints()
                + " points not reconciled to the ledger. No points were changed.");
        }
        miscRow.add(miscCard);

		section.add(topRow);
		section.add(Box.createRigidArea(new Dimension(0, 4)));
		section.add(bottomRow);
		section.add(Box.createRigidArea(new Dimension(0, 4)));
		section.add(miscRow);

		return section;
	}

    static String formatEfficiency(Double value) {
        return value == null || !Double.isFinite(value) || value < 0
            ? "--" : String.format(java.util.Locale.ROOT, "%.1f", value);
    }

	private JPanel buildStatsSection(AccountResponse.OsrsAccount account) {
		JPanel section = new JPanel(new GridLayout(1, 2, 4, 4));
		section.setOpaque(false);
		JPanel ehp = createStatCard(formatEfficiency(account.getEhp()), "EHP", UIConstants.ACCENT_GREEN, null);
        JPanel ehb = createStatCard(formatEfficiency(account.getEhb()), "EHB", UIConstants.ACCENT_BLUE, null);
        ehp.setToolTipText("--".equals(formatEfficiency(account.getEhp())) ? "EHP unavailable: no verified player statistic was returned." : "Wise Old Man EHP for this character");
        ehb.setToolTipText("--".equals(formatEfficiency(account.getEhb())) ? "EHB unavailable: no verified player statistic was returned." : "Wise Old Man EHB for this character");
        section.add(ehp);
        section.add(ehb);
		return section;
	}

	private JPanel buildMilestonesSection(List<AccountResponse.Milestone> completedMilestones) {
		boolean hideCompleted = config != null && config.hideCompletedItems();

		JPanel wrapper = new JPanel(new BorderLayout());
		wrapper.setBackground(UIConstants.CARD_BG);
		wrapper.setBorder(new EmptyBorder(10, 12, 10, 12));

		JLabel title = new JLabel("Milestones");
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(UIConstants.TEXT_PRIMARY);
		title.setBorder(new EmptyBorder(0, 0, 8, 0));

		JPanel list = new JPanel();
		list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
		list.setOpaque(false);

		List<PointsResponse.PointSource> definitions = new ArrayList<>();
		if (pointsData != null && pointsData.getPointSources() != null) {
			List<PointsResponse.PointSource> sources = pointsData.getPointSources().get("MILESTONES");
			if (sources != null) definitions = sources;
		}

		Map<String, AccountResponse.Milestone> completedMap = new HashMap<>();
		if (completedMilestones != null) {
			for (AccountResponse.Milestone m : completedMilestones) {
				if (m.getType() != null) completedMap.put(m.getType(), m);
			}
		}

		int itemCount = 0;
		if (definitions.isEmpty() && completedMilestones != null) {
			if (!hideCompleted) {
				for (AccountResponse.Milestone m : completedMilestones) {
					list.add(new ChecklistItem(m.getDescription(), true, m.getPointsAwarded(), assetLoader));
					list.add(Box.createRigidArea(new Dimension(0, 4)));
					itemCount++;
				}
			}
		} else {
			for (PointsResponse.PointSource def : definitions) {
				AccountResponse.Milestone completed = completedMap.get(def.getId());
				boolean isCompleted = completed != null && completed.getAchievedAt() != null && !completed.getAchievedAt().isEmpty();
				if (hideCompleted && isCompleted) continue;
				String desc = def.getDescription() != null ? def.getDescription() : def.getName();
				Integer pts = isCompleted && completed.getPointsAwarded() != null ? completed.getPointsAwarded() : def.getPointsValue();
				list.add(new ChecklistItem(desc, isCompleted, pts, assetLoader));
				list.add(Box.createRigidArea(new Dimension(0, 4)));
				itemCount++;
			}
		}

		if (itemCount == 0) {
			JLabel allDone = new JLabel("All milestones completed!");
			allDone.setFont(FontManager.getRunescapeSmallFont());
			allDone.setForeground(UIConstants.ACCENT_GREEN);
			list.add(allDone);
		}

		wrapper.add(title, BorderLayout.NORTH);
		wrapper.add(list, BorderLayout.CENTER);
		return wrapInRoundedPanel(wrapper);
	}

	private JPanel buildCombatAchievementsSection() {
		return buildTierSection("Combat Achievements", "COMBAT_ACHIEVEMENTS",
			currentAccount != null ? currentAccount.getCombatAchievementPoints() : 0);
	}

	private JPanel buildCollectionLogSection() {
		return buildTierSection("Collection Log", "COLLECTION_LOG",
			currentAccount != null ? currentAccount.getCollectionLogUniqueObtained() : 0);
	}

	private JPanel buildItemChecklistSection(String titleText, String sourceKey) {
		boolean hideCompleted = config != null && config.hideCompletedItems();

		JPanel wrapper = new JPanel(new BorderLayout());
		wrapper.setBackground(UIConstants.CARD_BG);
		wrapper.setBorder(new EmptyBorder(10, 12, 10, 12));

		JLabel title = new JLabel(titleText);
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(UIConstants.TEXT_PRIMARY);
		title.setBorder(new EmptyBorder(0, 0, 8, 0));

		JPanel list = new JPanel();
		list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
		list.setOpaque(false);

		List<PointsResponse.PointSource> definitions = new ArrayList<>();
		if (pointsData != null && pointsData.getPointSources() != null) {
			List<PointsResponse.PointSource> sources = pointsData.getPointSources().get(sourceKey);
			if (sources != null) definitions = sources;
		}

		Map<String, AccountResponse.Milestone> completedMap = new HashMap<>();
		if (currentAccount != null && currentAccount.getMilestones() != null) {
			for (AccountResponse.Milestone m : currentAccount.getMilestones()) {
				if (m.getType() != null) completedMap.put(m.getType(), m);
			}
		}

		if (definitions.isEmpty()) {
			JLabel empty = new JLabel("No items available");
			empty.setFont(FontManager.getRunescapeSmallFont());
			empty.setForeground(UIConstants.TEXT_SECONDARY);
			list.add(empty);
		} else {
			int itemCount = 0;
			for (PointsResponse.PointSource def : definitions) {
				AccountResponse.Milestone completed = completedMap.get(def.getId());
				boolean isCompleted = completed != null && completed.getAchievedAt() != null && !completed.getAchievedAt().isEmpty();
				if (hideCompleted && isCompleted) continue;
				String desc = def.getDescription() != null ? def.getDescription() : def.getName();
				Integer pts = isCompleted && completed.getPointsAwarded() != null ? completed.getPointsAwarded() : def.getPointsValue();
				list.add(new ChecklistItem(desc, isCompleted, pts, assetLoader));
				list.add(Box.createRigidArea(new Dimension(0, 4)));
				itemCount++;
			}

			if (itemCount == 0) {
				JLabel allDone = new JLabel("All " + titleText.toLowerCase() + " completed!");
				allDone.setFont(FontManager.getRunescapeSmallFont());
				allDone.setForeground(UIConstants.ACCENT_GREEN);
				list.add(allDone);
			}
		}

		wrapper.add(title, BorderLayout.NORTH);
		wrapper.add(list, BorderLayout.CENTER);
		return wrapInRoundedPanel(wrapper);
	}

	private JPanel buildTierSection(String titleText, String sourceKey, Integer currentProgress) {
		boolean hideCompleted = config != null && config.hideCompletedItems();

		JPanel wrapper = new JPanel(new BorderLayout());
		wrapper.setBackground(UIConstants.CARD_BG);
		wrapper.setBorder(new EmptyBorder(10, 12, 10, 12));

		JLabel title = new JLabel(titleText);
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(UIConstants.TEXT_PRIMARY);
		title.setBorder(new EmptyBorder(0, 0, 8, 0));

		JPanel list = new JPanel();
		list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
		list.setOpaque(false);

		int progress = currentProgress != null ? currentProgress : 0;
		List<PointsResponse.PointSource> tiers = new ArrayList<>();

		if (pointsData != null && pointsData.getPointSources() != null) {
			List<PointsResponse.PointSource> sources = pointsData.getPointSources().get(sourceKey);
			if (sources != null) {
				tiers = sources;
				tiers.sort(Comparator.comparingInt(t -> t.getThreshold() != null ? t.getThreshold() : 0));
			}
		}

		if (tiers.isEmpty()) {
			JLabel empty = new JLabel("No tiers available");
			empty.setFont(FontManager.getRunescapeSmallFont());
			empty.setForeground(UIConstants.TEXT_SECONDARY);
			list.add(empty);
		} else {
            int itemCount = 0;
            int previousPoints = 0;
            for (PointsResponse.PointSource tier : tiers) {
                Integer threshold = tier.getThreshold();
                if ("COMBAT_ACHIEVEMENTS".equals(sourceKey) && currentAccount != null
                    && currentAccount.getCombatAchievementThresholds() != null && tier.getId() != null) {
                    Integer live = currentAccount.getCombatAchievementThresholds().get(
                        tier.getId().replace("combat_achievement_", ""));
                    if (live != null && live > 0) threshold = live;
                }
                boolean completed = threshold != null && progress >= threshold;
                int cumulative = tier.getPointsValue();
                int additional = Math.max(0, cumulative - previousPoints);
                previousPoints = Math.max(previousPoints, cumulative);
                if (hideCompleted && completed) continue;
                String desc = tier.getDescription() != null ? tier.getDescription() : tier.getName();
                list.add(new ChecklistItem(desc, completed, additional, assetLoader));
				list.add(Box.createRigidArea(new Dimension(0, 4)));
				itemCount++;
			}

			if (itemCount == 0) {
				JLabel allDone = new JLabel("All " + titleText.toLowerCase() + " completed!");
				allDone.setFont(FontManager.getRunescapeSmallFont());
				allDone.setForeground(UIConstants.ACCENT_GREEN);
				list.add(allDone);
			}
		}

		wrapper.add(title, BorderLayout.NORTH);
		wrapper.add(list, BorderLayout.CENTER);
		return wrapInRoundedPanel(wrapper);
	}

	private JPanel createStatCard(String value, String label, Color accentColor, String sourceType) {
		JPanel card = new JPanel() {
			private boolean hovered = false;
			{
				setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
				setOpaque(false);
				setBorder(new EmptyBorder(8, 8, 8, 8));

				if (sourceType != null) {
					Clickable.onPress(this, () -> showPointsBreakdown(sourceType, label), h -> hovered = h);
				}
			}
			@Override
			protected void paintComponent(Graphics g) {
				Graphics2D g2d = (Graphics2D) g.create();
				g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				Color bg = hovered ? UIConstants.CARD_BG.brighter() : UIConstants.CARD_BG;
				g2d.setColor(bg);
				g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
				g2d.dispose();
			}
		};

		JLabel valueLabel = new JLabel(value);
		valueLabel.setFont(FontManager.getRunescapeBoldFont());
		valueLabel.setForeground(accentColor != null ? accentColor : UIConstants.TEXT_PRIMARY);
		valueLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

		JLabel nameLabel = new JLabel(label);
		nameLabel.setFont(FontManager.getRunescapeSmallFont());
		nameLabel.setForeground(UIConstants.TEXT_SECONDARY);
		nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

		card.add(valueLabel);
		card.add(nameLabel);
		return card;
	}

	private JPanel createCenteredPanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBackground(UIConstants.BACKGROUND);
		return panel;
	}

	private JPanel wrapInRoundedPanel(JPanel content) {
		JPanel wrapper = new JPanel(new BorderLayout()) {
			@Override
			protected void paintComponent(Graphics g) {
				Graphics2D g2d = (Graphics2D) g.create();
				g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2d.setColor(UIConstants.CARD_BG);
				g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
				g2d.dispose();
			}
		};
		wrapper.setOpaque(false);
		wrapper.add(content, BorderLayout.CENTER);
		return wrapper;
	}

	private void revalidateAndRepaint() {
		contentPanel.revalidate();
		contentPanel.repaint();
	}

	private String formatNumber(long num) {
		if (num >= 1_000_000) return new DecimalFormat("#.#M").format(num / 1_000_000.0);
		if (num >= 1_000) return new DecimalFormat("#.#K").format(num / 1_000.0);
		return String.valueOf(num);
	}

	private String formatDecimal(double num) {
		return new DecimalFormat("#,##0.0").format(num);
	}

	private String getRankDisplayName(String rankName) {
		if (ranks != null && rankName != null) {
			for (PointsResponse.Rank rank : ranks) {
				if (rank.getName().equalsIgnoreCase(rankName)) {
					return rank.getDisplayName();
				}
			}
		}
		return rankName;
	}

	private void showPointsBreakdown(String sourceType, String title) {
		if (pointsLog == null || pointsLog.isEmpty()) {
			JOptionPane.showMessageDialog(this, "No points log data available.", "Points Log", JOptionPane.INFORMATION_MESSAGE);
			return;
		}

		if (albumWindow != null) {
			albumWindow.dispose();
		}
		String playerName = (currentAccount != null && currentAccount.getOsrsAccount() != null)
			? currentAccount.getOsrsAccount().getOsrsNickname() : null;
		albumWindow = new PointsAlbumWindow(playerName, pointsLog, itemManager);
		albumWindow.selectSource(sourceType);
		albumWindow.setVisible(true);
		albumWindow.toFront();
	}

	/** Closes the points album if open; called when the plugin shuts down. */
	public void disposeAlbum() {
		if (albumWindow != null) {
			albumWindow.dispose();
			albumWindow = null;
		}
	}
}
