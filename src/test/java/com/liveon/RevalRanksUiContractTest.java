package com.liveon;

import com.google.gson.JsonObject;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.MouseEvent;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RevalRanksUiContractTest
{
	private static final List<String> RANKS = Arrays.asList(
		"Mentor", "Precept", "Leader", "Supervisor", "Superior", "Executive",
		"Senator", "Monarch", "Red Topaz", "Sapphire", "Emerald", "Ruby",
		"Diamond", "Dragonstone", "Onyx", "Zenyte", "Marshal");
	private static final List<Integer> THRESHOLDS = Arrays.asList(
		0, 500, 1_000, 2_000, 4_000, 7_500, 10_000, 15_000, 17_500,
		20_000, 22_500, 25_000, 27_500, 30_000, 35_000, 40_000, 50_000);

	@Test
	public void rendersExactRankOrderAndCollapsedPointSources()
	{
		PointsResponse response = response();
		assertEquals(RANKS, names(response));
		assertEquals(THRESHOLDS, thresholds(response));

		RankingPanel panel = new RankingPanel(null, null, null);
		panel.setData(response);
		List<String> labels = labels(panel);
		assertTrue(labels.contains("RANKS"));
		assertTrue(labels.contains("Clan rank progression"));
		assertTrue(labels.contains("POINT SOURCES"));
		assertTrue(labels.contains("Ways to earn points"));
		for (String rank : RANKS) assertTrue("missing rank " + rank, labels.contains(rank));
		assertTrue(labels.contains("50.0k (150/mo)"));

		List<CollapsibleSection> sections = components(panel, CollapsibleSection.class);
		assertEquals(2, sections.size());
		for (CollapsibleSection section : sections) assertFalse(section.isExpanded());
		sections.get(0).setExpanded(true);
		assertTrue(sections.get(0).isExpanded());
	}

	@Test
	public void profileSeparatesActualClanRankFromProgression()
	{
		RevalProfilePanel panel = new RevalProfilePanel(null, () -> { });
		JsonObject profile = new JsonObject();
		profile.addProperty("rsn", "Plant Lover");
		profile.addProperty("actual_clan_rank", "Owner");
		profile.addProperty("points", 28_100);
		JsonObject progression = new JsonObject();
		progression.addProperty("title", "Diamond");
		progression.addProperty("threshold", 27_500);
		progression.addProperty("next_title", "Dragonstone");
		progression.addProperty("next_threshold", 30_000);
		profile.add("progression", progression);
		JsonObject maintenance = new JsonObject();
		maintenance.addProperty("points", 90);
		maintenance.addProperty("requirement", 150);
		profile.add("maintenance", maintenance);
		panel.setProfile(profile);
		List<String> labels = labels(panel);
		assertTrue(labels.contains("Plant Lover"));
		assertTrue(labels.contains("Actual clan rank: Owner"));
		assertTrue(labels.contains("Progression: Diamond"));
		assertTrue(labels.contains("28,100 pts"));
		assertTrue(labels.contains("Next: Dragonstone at 30,000 · 1,900 remaining"));
		assertTrue(labels.contains("Monthly maintenance: 90 / 150"));
	}

	@Test
	public void colorsAndMousePressBehaviorMatchPinnedRevalComponents()
	{
		assertEquals(15, RevalUiConstants.BACKGROUND.getRed());
		assertEquals(17, RevalUiConstants.BACKGROUND.getGreen());
		assertEquals(17, RevalUiConstants.BACKGROUND.getBlue());
		assertEquals(218, RevalUiConstants.ACCENT_GOLD.getRed());
		assertEquals(165, RevalUiConstants.ACCENT_GOLD.getGreen());
		assertEquals(32, RevalUiConstants.ACCENT_GOLD.getBlue());
		assertEquals(139, RevalUiConstants.ACCENT_GREEN.getRed());

		JPanel target = new JPanel();
		int[] presses = {0};
		Clickable.onPress(target, () -> presses[0]++,
			RevalUiConstants.CARD_HOVER, RevalUiConstants.CARD_BG);
		MouseEvent press = new MouseEvent(target, MouseEvent.MOUSE_PRESSED,
			System.currentTimeMillis(), 0, 1, 1, 1, false, MouseEvent.BUTTON1);
		for (java.awt.event.MouseListener listener : target.getMouseListeners())
			listener.mousePressed(press);
		assertEquals(1, presses[0]);
	}

	@Test
	public void activeClientUsesNightLegionTransportAndPinnedRevalEventCoverage() throws Exception
	{
		Path plugin = Paths.get("src", "main", "java", "com", "liveon", "ClanMessagesPlugin.java");
		Path tracker = Paths.get("src", "main", "java", "com", "nightlegion", "livexp",
			"NightLegionLootPointTracker.java");
		String pluginSource = new String(Files.readAllBytes(plugin), StandardCharsets.UTF_8);
		String trackerSource = new String(Files.readAllBytes(tracker), StandardCharsets.UTF_8);
		assertTrue(pluginSource.contains("public class ClanMessagesPlugin extends Plugin"));
		assertTrue(pluginSource.contains("currentLocalRsn()"));
		assertTrue(pluginSource.contains("client.getLocalPlayer().getName()"));
		assertTrue(pluginSource.contains("community_rank_full_sync"));
		assertTrue(pluginSource.contains("community_rank_event"));
		for (String event : Arrays.asList(
			"ServerNpcLoot", "NpcLootReceived", "PlayerLootReceived", "LootReceived",
			"ItemContainerChanged", "MenuOptionClicked"))
			assertTrue("missing event " + event, trackerSource.contains(event));
		assertTrue(trackerSource.contains("New item added to your collection log:"));
		assertTrue(trackerSource.contains("self-drop"));
		assertTrue(trackerSource.contains("gear-swap"));
		assertFalse(pluginSource.contains("api.revalosrs.ee"));
		assertFalse(trackerSource.contains("api.revalosrs.ee"));
	}

	private static PointsResponse response()
	{
		PointsResponse response = new PointsResponse();
		response.status = "success";
		response.sourceCommit = "6033d3188b18d34f4bd4c28e6cf7986c8b95f0f9";
		response.data = new PointsResponse.PointsData();
		response.data.ranks = new ArrayList<>();
		for (int i = 0; i < RANKS.size(); i++)
		{
			PointsResponse.Rank rank = new PointsResponse.Rank();
			rank.name = RANKS.get(i).toLowerCase(java.util.Locale.ROOT).replace(' ', '_');
			rank.displayName = RANKS.get(i);
			rank.pointsRequired = THRESHOLDS.get(i);
			rank.maintenancePerMonth = i == RANKS.size() - 1 ? 150 : 0;
			response.data.ranks.add(rank);
		}
		response.data.pointSources = new LinkedHashMap<>();
		PointsResponse.PointSource valuable = new PointsResponse.PointSource();
		valuable.id = "valuable_drop";
		valuable.name = "Valuable Drop";
		valuable.description = "Single item must be worth 1M+ GP";
		valuable.pointsDescription = "1 point per 1M GP";
		response.data.pointSources.put("VALUABLE_DROPS", Arrays.asList(valuable));
		PointsResponse.PointSource fireCape = new PointsResponse.PointSource();
		fireCape.id = "fire_cape";
		fireCape.name = "Fire Cape";
		fireCape.pointsDescription = "50 points";
		fireCape.metadata = new PointsResponse.PointSourceMetadata();
		fireCape.metadata.itemId = 6570;
		fireCape.metadata.category = "Milestones";
		fireCape.metadata.source = "Fight Caves";
		response.data.pointSources.put("UNTRADEABLE_DROPS", Arrays.asList(fireCape));
		return response;
	}

	private static List<String> names(PointsResponse response)
	{
		List<String> values = new ArrayList<>();
		for (PointsResponse.Rank rank : response.data.ranks) values.add(rank.displayName);
		return values;
	}

	private static List<Integer> thresholds(PointsResponse response)
	{
		List<Integer> values = new ArrayList<>();
		for (PointsResponse.Rank rank : response.data.ranks) values.add(rank.pointsRequired);
		return values;
	}

	private static List<String> labels(Component root)
	{
		List<String> values = new ArrayList<>();
		if (root instanceof JLabel) values.add(((JLabel) root).getText());
		if (root instanceof Container)
			for (Component child : ((Container) root).getComponents()) values.addAll(labels(child));
		return values;
	}

	private static <T> List<T> components(Component root, Class<T> type)
	{
		List<T> values = new ArrayList<>();
		if (type.isInstance(root)) values.add(type.cast(root));
		if (root instanceof Container)
			for (Component child : ((Container) root).getComponents()) values.addAll(components(child, type));
		return values;
	}
}
