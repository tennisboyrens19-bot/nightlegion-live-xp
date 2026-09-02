package com.liveon;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MvpChatDecorationTest
{
	private static final String MVP = " <col=ffc628>MVP</col>";
	private static final String LIVE = " <col=96ffaa>LIVE</col>";

	@Test
	public void decoratesOnlyTheAutomaticMvpSender()
	{
		assertEquals("Hoag" + MVP + ":",
			ClanMessagesPlugin.decorateClanChatSender("Hoag:", true, false, ""));
		assertEquals("Other Player:",
			ClanMessagesPlugin.decorateClanChatSender("Other Player:", false, false, ""));
	}

	@Test
	public void mvpAndLiveMarkersCoexistInUpstreamOrder()
	{
		assertEquals("Hoag" + MVP + LIVE + ":",
			ClanMessagesPlugin.decorateClanChatSender("Hoag:", true, true, ""));
		assertEquals("Hoag" + LIVE + ":",
			ClanMessagesPlugin.decorateClanChatSender("Hoag:", false, true, ""));
	}

	@Test
	public void changingWinnerRemovesAndAddsMarkerWithoutChangingMessageText()
	{
		String message = "Normal clan message text";
		assertEquals("Old Winner:",
			ClanMessagesPlugin.decorateClanChatSender("Old Winner:", false, false, ""));
		assertEquals("New Winner" + MVP + ":",
			ClanMessagesPlugin.decorateClanChatSender("New Winner:", true, false, ""));
		assertEquals("Normal clan message text", message);
	}
}
