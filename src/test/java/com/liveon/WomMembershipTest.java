package com.liveon;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WomMembershipTest
{
	@Test
	public void nonAuthorizedRanksDoNotReceiveStaffAccess()
	{
		assertFalse(WomMembership.isStaffRole("administrator"));
		assertFalse(WomMembership.canPublishBroadcast("administrator"));
		assertFalse(WomMembership.isStaffRole("moderator"));
	}

	@Test
	public void onlyApprovedInGameStaffRanksCanPublishBroadcasts()
	{
		assertTrue(WomMembership.canPublishBroadcast("major"));
		assertTrue(WomMembership.canPublishBroadcast("general"));
		assertTrue(WomMembership.canPublishBroadcast("deputy_owner"));
		assertTrue(WomMembership.canPublishBroadcast("owner"));
		assertFalse(WomMembership.canPublishBroadcast("member"));
	}
}
