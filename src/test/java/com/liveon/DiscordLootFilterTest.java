package com.liveon;

import java.util.Arrays;
import java.util.Map;
import net.runelite.client.game.ItemStack;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DiscordLootFilterTest
{
	@Test
	public void usesMatchingDiscordAttachmentReferences()
	{
		assertEquals("attachment://loot.png", ClanMessagesPlugin.discordAttachmentUrl(
			ClanMessagesPlugin.DISCORD_LOOT_ATTACHMENT));
		assertEquals("attachment://pet.png", ClanMessagesPlugin.discordAttachmentUrl(
			ClanMessagesPlugin.DISCORD_PET_ATTACHMENT));
	}

	@Test
	public void limitsDiscordDescriptionsWithoutChangingNormalDrops()
	{
		String normal = "1x Oathplate legs (89.0M)\nYama";
		assertEquals(normal, ClanMessagesPlugin.limitDiscordDescription(normal));
		char[] characters = new char[5000];
		Arrays.fill(characters, 'x');
		String limited = ClanMessagesPlugin.limitDiscordDescription(new String(characters));
		assertEquals(4096, limited.length());
		assertTrue(limited.endsWith("..."));
	}

	@Test
	public void matchesExactAndWildcardNames()
	{
		assertTrue(ClanMessagesPlugin.matchesDiscordFilter(Arrays.asList("crimson kisten"), "Crimson kisten"));
		assertFalse(ClanMessagesPlugin.matchesDiscordFilter(Arrays.asList("crimson kisten"), "Crimson kisten (broken)"));
		assertTrue(ClanMessagesPlugin.matchesDiscordFilter(Arrays.asList("elder venator*"), "Elder venator bow"));
		assertFalse(ClanMessagesPlugin.matchesDiscordFilter(Arrays.asList("elder venator*"), "Amulet of rancour"));
	}

	@Test
	public void extractsOnlyAllowlistedCollectionLogAndValuableDrops()
	{
		assertEquals("Crimson kisten", ClanMessagesPlugin.allowlistedCollectionItem(
			"New item added to your collection log: Crimson kisten"));
		assertNull(ClanMessagesPlugin.allowlistedCollectionItem(
			"New item added to your collection log: Big bones"));
		ClanMessagesPlugin.PendingAllowlistedDrop drop = ClanMessagesPlugin.allowlistedValuableDrop(
			"Valuable drop: 2 x Crimson kisten (7,200,000 coins)");
		assertEquals("Crimson kisten", drop.itemName);
		assertEquals(2, drop.quantity);
		assertEquals(Long.valueOf(7_200_000L), drop.totalValue);
		assertNull(ClanMessagesPlugin.allowlistedValuableDrop(
			"Valuable drop: 1 x Big bones (4,000,000 coins)"));
	}

	@Test
	public void usesInclusiveOneMillionMvpThresholdAndStackTotalValue()
	{
		assertFalse(ClanMessagesPlugin.qualifiesForMvpDrop(999_999L));
		assertTrue(ClanMessagesPlugin.qualifiesForMvpDrop(1_000_000L));
		assertTrue(ClanMessagesPlugin.qualifiesForMvpDrop(2_000_000L));
		assertEquals(1_200_000L, ClanMessagesPlugin.totalDropValue(400_000L, 3L));
		assertTrue(ClanMessagesPlugin.qualifiesForMvpDrop(
			ClanMessagesPlugin.totalDropValue(400_000L, 3L)));
		Map<Integer, Long> quantities = ClanMessagesPlugin.aggregateDropQuantities(Arrays.asList(
			new ItemStack(123, 1), new ItemStack(123, 2), new ItemStack(456, 1)));
		assertEquals(Long.valueOf(3L), quantities.get(123));
		assertEquals(Long.valueOf(1L), quantities.get(456));
	}

	@Test
	public void extractsAnyUntradeableDropWithItsGameReportedTotalValue()
	{
		ClanMessagesPlugin.PendingAllowlistedDrop drop = ClanMessagesPlugin.allowlistedValuableDrop(
			"Untradeable drop: Araxyte fang (18,400,000 coins)");
		assertEquals("Araxyte fang", drop.itemName);
		assertEquals(1, drop.quantity);
		assertEquals(Long.valueOf(18_400_000L), drop.totalValue);

		ClanMessagesPlugin.PendingAllowlistedDrop future = ClanMessagesPlugin.allowlistedValuableDrop(
			"Untradeable drop: Future untradeable reward (2,500,000 coins)");
		assertEquals("Future untradeable reward", future.itemName);
		assertEquals(Long.valueOf(2_500_000L), future.totalValue);
	}
}
