package com.revalclan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;
import org.junit.Test;

public class HardRevalNightLegionContractTest
{
	private static final Path ROOT = Paths.get("").toAbsolutePath();

	private static String source(String relative) throws IOException
	{
		return Files.readString(ROOT.resolve(relative), StandardCharsets.UTF_8);
	}

	@Test
	public void copiedPublicRevalStructureIsTheOnlyClientImplementation() throws Exception
	{
		Path javaRoot = ROOT.resolve("src/main/java/com/revalclan");
		try (Stream<Path> files = Files.walk(javaRoot))
		{
			assertEquals(129L, files.filter(path -> path.toString().endsWith(".java")).count());
		}
		assertTrue(Files.exists(javaRoot.resolve("ui/ProfilePanel.java")));
		assertTrue(Files.exists(javaRoot.resolve("ui/AchievementsPanel.java")));
		assertTrue(Files.exists(javaRoot.resolve("ui/EventsPanel.java")));
		assertTrue(Files.exists(javaRoot.resolve("ui/CompetitionsPanel.java")));
		assertTrue(Files.exists(javaRoot.resolve("ui/DiaryPanel.java")));
		assertTrue(Files.exists(javaRoot.resolve("playercards/PlayerCardManager.java")));
		assertFalse(Files.exists(ROOT.resolve("src/main/java/com/liveon")));
		assertFalse(Files.exists(ROOT.resolve("src/main/java/com/nightlegion/livexp")));
	}

	@Test
	public void runtimeUsesOnlyNightLegionTransportAndCurrentLocalPlayer() throws Exception
	{
		String transport = source("src/main/java/com/revalclan/api/NightLegionTransport.java");
		String api = source("src/main/java/com/revalclan/api/RevalApiService.java");
		String webhook = source("src/main/java/com/revalclan/util/WebhookService.java");
		assertTrue(transport.contains("https://nightlegion-livexp.onrender.com"));
		assertTrue(transport.contains("runeLiteClient.getLocalPlayer().getName()"));
		assertTrue(transport.contains("volatile String authoritativeRsn"));
		assertTrue(transport.contains("return authoritativeRsn"));
		assertTrue(transport.contains("X-NightLegion-Token"));
		assertTrue(api.contains("community_reval_api"));
		assertTrue(webhook.contains("community_reval_event"));
		for (String text : new String[] {transport, api, webhook})
		{
			assertFalse(text.contains("api.revalosrs.ee"));
			assertFalse(text.contains("discord.gg/reval"));
		}
	}

	@Test
	public void brandingConfigurationAndClanGateAreNightLegion() throws Exception
	{
		String descriptor = source("runelite-plugin.properties");
		String config = source("src/main/java/com/revalclan/RevalClanConfig.java");
		String validator = source("src/main/java/com/revalclan/util/ClanValidator.java");
		String overlay = source("src/main/java/com/revalclan/playercards/PlayerCardOverlay.java");
		assertTrue(descriptor.contains("displayName=NightLegion"));
		assertTrue(descriptor.contains("plugins=com.revalclan.RevalClanPlugin"));
		assertTrue(config.contains("@ConfigGroup(\"nightlegion\")"));
		assertTrue(config.contains("Personal Link Token"));
		assertTrue(validator.contains("REQUIRED_CLAN_NAME = \"NightLegion\""));
		assertTrue(overlay.contains("NIGHTLEGION POINTS"));
		assertFalse(Files.exists(ROOT.resolve("src/main/resources/com/revalclan/ui/assets/reval.png")));
		assertTrue(Files.exists(ROOT.resolve("src/main/resources/com/revalclan/ui/assets/nightlegion.png")));
	}
}
