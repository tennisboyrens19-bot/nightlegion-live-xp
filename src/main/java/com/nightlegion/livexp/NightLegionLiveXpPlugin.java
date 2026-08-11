package com.nightlegion.livexp;

import com.google.inject.Provides;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PluginDescriptor(
	name = "NightLegion",
	description = "Updates the NightLegion SOTW leaderboard while you stay logged in",
	tags = {"nightlegion", "sotw", "xp", "skilling"},
	enabledByDefault = true
)
public class NightLegionLiveXpPlugin extends Plugin
{
	private static final Logger log = LoggerFactory.getLogger(NightLegionLiveXpPlugin.class);
	private static final URI ENDPOINT = URI.create("https://nightlegion-livexp.onrender.com/sotw/live-xp");
	private static final long PUSH_INTERVAL_MS = 5_000L;

	@Inject
	private Client client;

	@Inject
	private NightLegionLiveXpConfig config;

	private final HttpClient httpClient = HttpClient.newBuilder()
		.connectTimeout(Duration.ofSeconds(8))
		.build();

	private final Map<Skill, Integer> latestXp = new ConcurrentHashMap<>();
	private final Map<Skill, Integer> lastSentXp = new ConcurrentHashMap<>();
	private final Map<Skill, Long> lastAttemptAt = new ConcurrentHashMap<>();

	private volatile String currentRsn = "";
	private volatile String lastToken = "";
	private ScheduledExecutorService sender;

	@Override
	protected void startUp()
	{
		sender = Executors.newSingleThreadScheduledExecutor(r ->
		{
			Thread thread = new Thread(r, "nightlegion-live-xp");
			thread.setDaemon(true);
			return thread;
		});
		sender.scheduleWithFixedDelay(this::flushPending, 2, 2, TimeUnit.SECONDS);
		log.info("NightLegion started");
	}

	@Override
	protected void shutDown()
	{
		if (sender != null)
		{
			sender.shutdownNow();
			sender = null;
		}
		latestXp.clear();
		lastSentXp.clear();
		lastAttemptAt.clear();
		currentRsn = "";
		lastToken = "";
		log.info("NightLegion stopped");
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		refreshRsn();
	}

	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		Skill skill = event.getSkill();
		if (skill == null || skill == Skill.OVERALL || client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		refreshRsn();
		if (currentRsn.isEmpty())
		{
			return;
		}

		int xp = event.getXp();
		if (xp >= 0)
		{
			latestXp.put(skill, xp);
		}
	}

	private void refreshRsn()
	{
		Player player = client.getLocalPlayer();
		if (player == null || player.getName() == null)
		{
			return;
		}

		String nextRsn = player.getName().trim();
		if (!nextRsn.equals(currentRsn))
		{
			currentRsn = nextRsn;
			latestXp.clear();
			lastSentXp.clear();
			lastAttemptAt.clear();
		}
	}

	private void flushPending()
	{
		try
		{
			String token = config.token() == null ? "" : config.token().trim();
			String rsn = currentRsn == null ? "" : currentRsn.trim();

			if (!token.equals(lastToken))
			{
				lastToken = token;
				lastSentXp.clear();
				lastAttemptAt.clear();
			}

			if (token.isEmpty() || rsn.isEmpty())
			{
				return;
			}

			long now = System.currentTimeMillis();
			for (Map.Entry<Skill, Integer> entry : latestXp.entrySet())
			{
				Skill skill = entry.getKey();
				int xp = entry.getValue();
				Integer sentXp = lastSentXp.get(skill);

				if (sentXp != null && sentXp == xp)
				{
					continue;
				}

				long previousAttempt = lastAttemptAt.getOrDefault(skill, 0L);
				if (now - previousAttempt < PUSH_INTERVAL_MS)
				{
					continue;
				}

				lastAttemptAt.put(skill, now);
				send(token, rsn, skill, xp);
			}
		}
		catch (Exception ex)
		{
			log.debug("NightLegion flush failed", ex);
		}
	}

	private void send(String token, String rsn, Skill skill, int xp)
	{
		String json = "{" +
			"\"rsn\":\"" + escapeJson(rsn) + "\"," +
			"\"skill\":\"" + escapeJson(skill.getName()) + "\"," +
			"\"xp\":" + xp +
			"}";

		HttpRequest request = HttpRequest.newBuilder()
			.uri(ENDPOINT)
			.timeout(Duration.ofSeconds(10))
			.header("Content-Type", "application/json")
			.header("X-NightLegion-Token", token)
			.POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
			.build();

		httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding())
			.whenComplete((response, throwable) ->
			{
				if (throwable != null)
				{
					log.debug("NightLegion upload failed", throwable);
					return;
				}

				if (!token.equals(lastToken) || !rsn.equals(currentRsn))
				{
					return;
				}

				if (response.statusCode() >= 200 && response.statusCode() < 300)
				{
					lastSentXp.put(skill, xp);
					log.debug("NightLegion sent {} {} XP", skill.getName(), xp);
				}
				else if (response.statusCode() == 401)
				{
					log.warn("NightLegion token was rejected. Create a new one with /sotw_runelite_link.");
				}
				else
				{
					log.debug("NightLegion server returned HTTP {}", response.statusCode());
				}
			});
	}

	private static String escapeJson(String value)
	{
		return value
			.replace("\\", "\\\\")
			.replace("\"", "\\\"")
			.replace("\r", "\\r")
			.replace("\n", "\\n");
	}

	@Provides
	NightLegionLiveXpConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(NightLegionLiveXpConfig.class);
	}
}
