package com.liveon;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class PbPayloadTest
{
	@Test
	public void recognizesRaidAndAwakenedModeVariants()
	{
		assertPayload("Theatre of Blood: Hard Mode", "Theatre of Blood", "Hard Mode");
		assertPayload("Theatre of Blood HM", "Theatre of Blood", "Hard Mode");
		assertPayload("Tombs of Amascut: Expert Mode", "Tombs of Amascut", "Expert Mode");
		assertPayload("Chambers of Xeric CM", "Chambers of Xeric", "Challenge Mode");
		assertPayload("The Whisperer - Awakened", "The Whisperer", "Awakened");
		assertPayload("Duke Sucellus", "Duke Sucellus", "Normal");
	}

	@Test
	public void unifiesCombatAchievementAndAdventureLogNames()
	{
		assertPayload("TzTok-Jad", "TzHaar Fight Cave", "");
		assertPayload("TzKal-Zuk", "Inferno", "");
	}

	@Test
	public void readsRaidRoomAndOverallTimesFromAdventureLog()
	{
		List<Map<String, Object>> records = ClanMessagesPlugin.parseAdventureLogPbs(Arrays.asList(
			"Theatre of Blood - Hard",
			"Fastest Room time - (Team size: 3 player hard mode):", "20:21.60",
			"Fastest Overall time - (Team size: 3 player hard mode):", "22:19.30"));
		assertEquals(2, records.size());
		assertAdventureRecord(records.get(0), "Hard Mode", 3, "ROOM", 1221.60);
		assertAdventureRecord(records.get(1), "Hard Mode", 3, "OVERALL", 1339.30);
	}

	@Test
	public void readsCombatAchievementBossPagesAndWidgets()
	{
		Map<String, Object> page = ClanMessagesPlugin.parseCombatAchievementBossPb(Arrays.asList(
			"Combat Achievements - Maggot King",
			"Combat Level: 741 Kill Count: 313 Personal Best: 1:03.00"));
		assertEquals("Maggot King", page.get("boss"));
		assertEquals(63.0, (Double) page.get("seconds"), 0.001);

		Map<String, Object> widget = ClanMessagesPlugin.parseCombatAchievementBossWidgets(
			"TzTok-Jad", Arrays.asList("Combat Level: 702", "Kill Count: 1", "Personal Best: 1:06:13.20"));
		assertEquals("TzTok-Jad", widget.get("boss"));
		assertEquals(3973.20, (Double) widget.get("seconds"), 0.001);
		assertNull(ClanMessagesPlugin.parseCombatAchievementBossPb(Arrays.asList(
			"Combat Achievements - Maggot King", "Kill Count: 313")));
	}

	@Test
	public void readsPhysicalBossStatisticsBoardsIncludingAwakenedTimes()
	{
		Map<String, Object> board = ClanMessagesPlugin.parseBossStatisticsBoardPb(Arrays.asList(
			"Maggot King Statistics", "Personal Killcount<br>313", "Personal Best Time<br>1:03.00"));
		assertEquals("Maggot King", board.get("boss"));
		assertEquals(63.0, (Double) board.get("seconds"), 0.001);

		List<Map<String, Object>> records = ClanMessagesPlugin.parseBossStatisticsBoardPbs(Arrays.asList(
			"The Whisperer Statistics", "Personal Best Time<br>2:05.40",
			"Awakened Personal Best Time<br>3:48.20"));
		assertEquals(2, records.size());
		assertEquals("", records.get(0).get("mode"));
		assertEquals("Awakened", records.get(1).get("mode"));
		assertEquals(228.2, (Double) records.get(1).get("seconds"), 0.001);
	}

	@Test
	public void readsSupportedChatPbMessages()
	{
		assertEquals("Vorkath", ClanMessagesPlugin.parsePbChatBoss("Your Vorkath kill count is: 1,234"));
		Map<String, Object> raid = ClanMessagesPlugin.parsePbChatTime(
			"Team size: 3 players Duration: <col=ff0000>18:42.60</col> (new personal best)");
		assertNotNull(raid);
		assertEquals(3, raid.get("teamSize"));
		assertEquals(1122.6, (Double) raid.get("seconds"), 0.001);
		Map<String, Object> boss = ClanMessagesPlugin.parsePbChatTime(
			"Fight duration: <col=ff0000>1:03.20</col> (new personal best)");
		assertNotNull(boss);
		assertEquals(63.2, (Double) boss.get("seconds"), 0.001);
	}

	@Test
	public void collectionLogNeverFabricatesAPb()
	{
		assertNull(ClanMessagesPlugin.parsePbChatBoss("New item added to your collection log: Pet"));
		assertNull(ClanMessagesPlugin.parsePbChatTime("New item added to your collection log: Pet"));
		assertNull(ClanMessagesPlugin.parseCombatAchievementBossPb(Arrays.asList(
			"Collection Log", "Vorkath", "Obtained: 12/12")));
	}

	@Test
	public void localRuneLiteRsnIsTheAuthoritativeSubmissionIdentity()
	{
		assertEquals("Plant Lover", ClanMessagesPlugin.authoritativeRsn(" Plant\u00A0Lover "));
		assertEquals("", ClanMessagesPlugin.authoritativeRsn(null));
	}

	private static void assertPayload(String recorded, String boss, String mode)
	{
		Map<String, Object> payload = ClanMessagesPlugin.pbPayload(recorded, 2, 123.45);
		assertEquals(boss, payload.get("boss"));
		assertEquals(mode, payload.get("mode"));
		assertEquals(2, payload.get("teamSize"));
	}

	private static void assertAdventureRecord(Map<String, Object> record, String mode, int teamSize,
		String timeType, double seconds)
	{
		assertEquals("Theatre of Blood", record.get("boss"));
		assertEquals(mode, record.get("mode"));
		assertEquals(teamSize, record.get("teamSize"));
		assertEquals(timeType, record.get("timeType"));
		assertEquals(seconds, (Double) record.get("seconds"), 0.001);
	}
}
