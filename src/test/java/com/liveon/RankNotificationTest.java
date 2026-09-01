package com.liveon;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RankNotificationTest
{
	@Test
	public void formatsAvailableRankMessageInEnglishWithNightLegionBranding()
	{
		assertEquals(
			"[NightLegion] Rank promotion available: Sergeant! Request it through the clan plugin.",
			ClanMessagesPlugin.rankNotificationMessage("Sergeant"));
	}

	@Test
	public void notifiesImmediatelyForHigherRank()
	{
		assertEquals(true, ClanMessagesPlugin.shouldNotifyAvailableRank(1, 2, false, false));
	}

	@Test
	public void notifiesOnlyOncePerSessionForSameRank()
	{
		assertEquals(false, ClanMessagesPlugin.shouldNotifyAvailableRank(1, 2, true, false));
	}

	@Test
	public void suppressesCurrentAndPendingRanks()
	{
		assertEquals(false, ClanMessagesPlugin.shouldNotifyAvailableRank(2, 2, false, false));
		assertEquals(false, ClanMessagesPlugin.shouldNotifyAvailableRank(1, 2, false, true));
	}
}
