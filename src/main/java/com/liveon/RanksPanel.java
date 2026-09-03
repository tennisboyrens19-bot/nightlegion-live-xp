package com.liveon;

import com.google.gson.JsonObject;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.List;
import javax.swing.Icon;
import javax.swing.JPanel;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;

/** NightLegion shell around the pinned Reval rank/profile presentation. */
final class RanksPanel extends JPanel
{
	private final Runnable refresh;
	private RankingPanel ranking;
	private RevalProfilePanel profile;
	private JsonObject pendingProfile;
	private String currentProgression = "";

	RanksPanel(Runnable refreshAction, Runnable ignoredResetAction, Runnable ignoredRequestAction)
	{
		refresh = refreshAction;
		setLayout(new BorderLayout(0, 2));
		setBackground(RevalUiConstants.BACKGROUND);
		setPreferredSize(new Dimension(225, 650));
	}

	void initialize(ItemManager itemManager, SpriteManager spriteManager, Client client, ClientThread clientThread)
	{
		removeAll();
		ClanRankIconResolver resolver = new ClanRankIconResolver(client, clientThread, spriteManager);
		profile = new RevalProfilePanel(resolver, refresh);
		ranking = new RankingPanel(itemManager, spriteManager, resolver);
		add(profile, BorderLayout.NORTH);
		add(ranking, BorderLayout.CENTER);
		if (pendingProfile != null) profile.setProfile(pendingProfile);
		revalidate();
		repaint();
	}

	void setCatalogue(PointsResponse response)
	{
		if (ranking != null) ranking.setData(response);
	}

	void setProfile(JsonObject value)
	{
		pendingProfile = value;
		if (value != null)
		{
			JsonObject rank = value.has("progression") && value.get("progression").isJsonObject()
				? value.getAsJsonObject("progression")
				: value.has("rank") && value.get("rank").isJsonObject() ? value.getAsJsonObject("rank") : null;
			if (rank != null && rank.has("title")) currentProgression = rank.get("title").getAsString();
		}
		if (profile != null) profile.setProfile(value);
	}

	// Compatibility methods retained for the active Live On shell while the old
	// manual rank-request content is intentionally no longer authoritative.
	void update(String accountName, String clanRankName, Icon clanRankIcon,
		String evaluatedRank, Icon evaluatedRankIcon, String nextRankName, Icon nextRankIcon,
		List<String> nextChecks, List<String> overviewChecks, String advice) { }
	void updateActivityProfile(double points, String rank, String nextRank,
		Double nextThreshold, String currentClanRank) { }
	void clearDetails() { }
	void reset() { refresh.run(); }
	void setStatus(String ignored) { }
	void setStatusSuccess(String ignored) { }
	void setRankRequestState(boolean pending, int cooldownSeconds) { }

	String getCurrentRank() { return currentProgression; }
	String getNextRank() { return currentProgression; }
	String getDisplayedAvailableRank() { return currentProgression; }
	String getDisplayedNextRank() { return currentProgression; }
	String getDisplayedNextRequirements() { return ""; }
	String getDisplayedCurrentClanRank() { return profile == null ? "" : profile.actualRankText(); }
	boolean isSpecialNoticeVisible() { return false; }
	boolean isProgressionVisible() { return true; }
	String getRequirementsDescription() { return ""; }
}
