# Live On HTTP call inventory

Generated from exact vendored upstream source.

## `<class>` around line 81

```java
0063: import net.runelite.client.events.ConfigChanged;
0064: import javax.swing.JOptionPane;
0065: import javax.swing.Icon;
0066: import javax.swing.ImageIcon;
0067: import javax.swing.SwingUtilities;
0068: import net.runelite.client.events.NpcLootReceived;
0069: import net.runelite.client.game.ItemManager;
0070: import net.runelite.client.game.ItemStack;
0071: import net.runelite.client.plugins.Plugin;
0072: import net.runelite.client.plugins.PluginDescriptor;
0073: import net.runelite.client.plugins.loottracker.LootReceived;
0074: import net.runelite.client.plugins.loottracker.LootTrackerConfig;
0075: import net.runelite.client.ui.DrawManager;
0076: import net.runelite.client.ui.ClientToolbar;
0077: import net.runelite.client.ui.NavigationButton;
0078: import net.runelite.client.Notifier;
0079: import net.runelite.client.util.LinkBrowser;
0080: import net.runelite.client.util.Text;
0081: import okhttp3.HttpUrl;
0082: import okhttp3.MediaType;
0083: import okhttp3.OkHttpClient;
0084: import okhttp3.Request;
0085: import okhttp3.RequestBody;
0086: import okhttp3.Response;
0087: import okhttp3.MultipartBody;
0088: import net.runelite.http.api.loottracker.LootRecordType;
0089: 
0090: @Slf4j
0091: @PluginDescriptor(name = "Live On Clan")
0092: public class ClanMessagesPlugin extends Plugin
0093: {
0094: 	private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
0095: 	private static final Pattern RANK_REQUEST_MESSAGE_PATTERN = Pattern.compile("(?<player>.+) solicitou um rank: (?<rank>.+)");
0096: 	private static final Pattern PROMOTION_MESSAGE_PATTERN = Pattern.compile("(?:Promo\u00E7\u00E3o: )?(?<player>.+?) foi promovido para (?<rank>.+)!");
0097: 	private static final Pattern URL_PATTERN = Pattern.compile("(?i)\\b(?:https?://|twitch\\.tv/)[^\\s<>]+");
0098: 	private static final Pattern PET_TRIGGER_PATTERN = Pattern.compile(
0099: 		"You (?:have a funny feeling like you|feel something weird sneaking).*", Pattern.CASE_INSENSITIVE);
0100: 	private static final Pattern PET_CLAN_PATTERN = Pattern.compile(
0101: 		"\\b(?<user>[\\w\\s]+) (?:has a funny feeling like .+ followed|feels something weird sneaking into .+ backpack|feels like .+ acquired something special): (?:(?<pet>.+) at (?<milestone>.+)|(?<petOnly>.+))",
0102: 		Pattern.CASE_INSENSITIVE);
0103: 	private static final Pattern PET_UNTRADEABLE_PATTERN = Pattern.compile("Untradeable drop: (.+)", Pattern.CASE_INSENSITIVE);
0104: 	private static final Pattern PET_COLLECTION_PATTERN = Pattern.compile(
0105: 		"(?:New item added to your collection log|Collection log):\\s*(.+)", Pattern.CASE_INSENSITIVE);
0106: 	private static final Pattern VALUABLE_DROP_PATTERN = Pattern.compile(
0107: 		"(?:Valuable drop|Untradeable drop):\\s*(?:(\\d+)\\s*x\\s*)?(.+?)\\s*\\(([0-9,]+)\\s+coins?\\)\\s*\\.?$",
0108: 		Pattern.CASE_INSENSITIVE);
0109: 	private static final String PB_TEAM_SIZE = "(?<teamsize>\\d+(?:\\+|-\\d+)? players?|Solo)";
```

## `validChatUrl` around line 470

```java
0452: 			}
0453: 			addedUrls.add(url);
0454: 			client.getMenu().createMenuEntry(menuPosition++)
0455: 				.setOption("Open link")
0456: 				.setTarget(url)
0457: 				.setType(MenuAction.RUNELITE)
0458: 				.onClick(entry -> LinkBrowser.browse(url));
0459: 		}
0460: 	}
0461: 
0462: 	private static String validChatUrl(String candidate)
0463: 	{
0464: 		String url = candidate;
0465: 		while (!url.isEmpty() && ".,;:!?)]}".indexOf(url.charAt(url.length() - 1)) >= 0)
0466: 		{
0467: 			url = url.substring(0, url.length() - 1);
0468: 		}
0469: 		String navigable = url.regionMatches(true, 0, "twitch.tv/", 0, 10) ? "https://" + url : url;
0470: 		HttpUrl parsed = HttpUrl.parse(navigable);
0471: 		return parsed != null && ("http".equals(parsed.scheme()) || "https".equals(parsed.scheme())) ? navigable : null;
0472: 	}
0473: 
0474: 	@Subscribe
0475: 	public void onChatMessage(ChatMessage event)
0476: 	{
0477: 		if (event.getType() != ChatMessageType.GAMEMESSAGE
0478: 			&& event.getType() != ChatMessageType.FRIENDSCHATNOTIFICATION
0479: 			&& event.getType() != ChatMessageType.CLAN_MESSAGE)
0480: 		{
0481: 			return;
0482: 		}
0483: 		String message = Text.removeTags(event.getMessage()).replace('\u00A0', ' ').trim();
0484: 		if (event.getType() == ChatMessageType.GAMEMESSAGE)
0485: 		{
0486: 			if (isPbParticipationEnabled())
0487: 			{
0488: 				capturePersonalBest(event.getMessage(), message);
0489: 			}
0490: 			PendingAllowlistedDrop valuableDrop = allowlistedValuableDrop(message);
0491: 			if (valuableDrop != null)
0492: 			{
0493: 				scheduleAllowlistedDropFallback(valuableDrop);
0494: 			}
0495: 			else
0496: 			{
0497: 				String collectionItem = allowlistedCollectionItem(message);
0498: 				if (collectionItem != null)
```

## `discordWikiLink` around line 902

```java
0884: 				"COLLECTION_LOG", null, drop.totalValue);
0885: 		});
0886: 	}
0887: 
0888: 	private ItemStack resolveCollectionLogItem(String itemName)
0889: 	{
0890: 		for (net.runelite.http.api.item.ItemPrice candidate : itemManager.search(itemName))
0891: 		{
0892: 			if (candidate.getName() != null && candidate.getName().trim().equalsIgnoreCase(itemName))
0893: 			{
0894: 				return new ItemStack(candidate.getId(), 1);
0895: 			}
0896: 		}
0897: 		return null;
0898: 	}
0899: 
0900: 	private static String discordWikiLink(String label, String search)
0901: 	{
0902: 		String url = HttpUrl.parse("https://oldschool.runescape.wiki/")
0903: 			.newBuilder()
0904: 			.addPathSegments("w/Special:Search")
0905: 			.addQueryParameter("search", search)
0906: 			.build()
0907: 			.toString()
0908: 			.replace(")", "\\)");
0909: 		return "[" + label + "](" + url + ")";
0910: 	}
0911: 
0912: 	private boolean isTemporaryLootWorld()
0913: 	{
0914: 		java.util.EnumSet<WorldType> worldTypes = client.getWorldType();
0915: 		return worldTypes.contains(WorldType.SEASONAL)
0916: 			|| worldTypes.contains(WorldType.DEADMAN)
0917: 			|| worldTypes.contains(WorldType.PVP)
0918: 			|| worldTypes.contains(WorldType.BOUNTY)
0919: 			|| worldTypes.contains(WorldType.PVP_ARENA)
0920: 			|| worldTypes.contains(WorldType.HIGH_RISK)
0921: 			|| worldTypes.contains(WorldType.BETA_WORLD)
0922: 			|| worldTypes.contains(WorldType.TOURNAMENT_WORLD)
0923: 			|| worldTypes.contains(WorldType.NOSAVE_MODE)
0924: 			|| worldTypes.contains(WorldType.QUEST_SPEEDRUNNING)
0925: 			|| worldTypes.contains(WorldType.LAST_MAN_STANDING)
0926: 			|| worldTypes.contains(WorldType.FRESH_START_WORLD);
0927: 	}
0928: 
0929: 	private void refreshRanks()
0930: 	{
```

## `discordWikiLink` around line 905

```java
0887: 
0888: 	private ItemStack resolveCollectionLogItem(String itemName)
0889: 	{
0890: 		for (net.runelite.http.api.item.ItemPrice candidate : itemManager.search(itemName))
0891: 		{
0892: 			if (candidate.getName() != null && candidate.getName().trim().equalsIgnoreCase(itemName))
0893: 			{
0894: 				return new ItemStack(candidate.getId(), 1);
0895: 			}
0896: 		}
0897: 		return null;
0898: 	}
0899: 
0900: 	private static String discordWikiLink(String label, String search)
0901: 	{
0902: 		String url = HttpUrl.parse("https://oldschool.runescape.wiki/")
0903: 			.newBuilder()
0904: 			.addPathSegments("w/Special:Search")
0905: 			.addQueryParameter("search", search)
0906: 			.build()
0907: 			.toString()
0908: 			.replace(")", "\\)");
0909: 		return "[" + label + "](" + url + ")";
0910: 	}
0911: 
0912: 	private boolean isTemporaryLootWorld()
0913: 	{
0914: 		java.util.EnumSet<WorldType> worldTypes = client.getWorldType();
0915: 		return worldTypes.contains(WorldType.SEASONAL)
0916: 			|| worldTypes.contains(WorldType.DEADMAN)
0917: 			|| worldTypes.contains(WorldType.PVP)
0918: 			|| worldTypes.contains(WorldType.BOUNTY)
0919: 			|| worldTypes.contains(WorldType.PVP_ARENA)
0920: 			|| worldTypes.contains(WorldType.HIGH_RISK)
0921: 			|| worldTypes.contains(WorldType.BETA_WORLD)
0922: 			|| worldTypes.contains(WorldType.TOURNAMENT_WORLD)
0923: 			|| worldTypes.contains(WorldType.NOSAVE_MODE)
0924: 			|| worldTypes.contains(WorldType.QUEST_SPEEDRUNNING)
0925: 			|| worldTypes.contains(WorldType.LAST_MAN_STANDING)
0926: 			|| worldTypes.contains(WorldType.FRESH_START_WORLD);
0927: 	}
0928: 
0929: 	private void refreshRanks()
0930: 	{
0931: 		rankSyncCompleted = true;
0932: 		clientThread.invoke(() -> refreshRanksOnClientThread(true));
0933: 	}
```

## `sendDiscordDrop` around line 2040

```java
2022: 		extra.put("killCount", killCount);
2023: 		extra.put("rarestProbability", rarestProbability);
2024: 		extra.put("npcId", npcId);
2025: 		payload.put("extra", extra);
2026: 		try
2027: 		{
2028: 			byte[] screenshotBytes = null;
2029: 			if (screenshot != null)
2030: 			{
2031: 				Map<String, Object> image = new LinkedHashMap<>();
2032: 				image.put("url", discordAttachmentUrl(DISCORD_LOOT_ATTACHMENT));
2033: 				embed.put("image", image);
2034: 				ByteArrayOutputStream output = new ByteArrayOutputStream();
2035: 				ImageIO.write((BufferedImage) screenshot, "png", output);
2036: 				screenshotBytes = output.toByteArray();
2037: 			}
2038: 			// Serialize only after attachment://loot.png has been added to the
2039: 			// embed, otherwise Discord renders the PNG as a separate attachment.
2040: 			MultipartBody.Builder multipart = new MultipartBody.Builder().setType(MultipartBody.FORM)
2041: 				.addFormDataPart("payload_json", gson.toJson(payload));
2042: 			if (screenshotBytes != null)
2043: 			{
2044: 				multipart.addFormDataPart("file", DISCORD_LOOT_ATTACHMENT,
2045: 					RequestBody.create(MediaType.parse("image/png"), screenshotBytes));
2046: 			}
2047: 			Request request = discordNotificationRequest(multipart.build());
2048: 			if (request == null)
2049: 			{
2050: 				return;
2051: 			}
2052: 			okHttpClient.newCall(request).enqueue(new okhttp3.Callback()
2053: 			{
2054: 				@Override public void onFailure(okhttp3.Call call, IOException exception) { log.debug("Unable to send Discord drop notification", exception); }
2055: 				@Override public void onResponse(okhttp3.Call call, Response response) throws IOException
2056: 				{
2057: 					try (Response ignored = response) { if (!response.isSuccessful()) log.debug("Discord notification relay returned {}", response.code()); }
2058: 				}
2059: 			});
2060: 		}
2061: 		catch (IOException exception)
2062: 		{
2063: 			log.debug("Unable to prepare Discord drop screenshot", exception);
2064: 		}
2065: 	}
2066: 
2067: 	private static Map<String, Object> embedField(String name, String value, boolean inline)
2068: 	{
```

## `sendPetNotification` around line 2315

```java
2297: 		payload.put("dinkAccountHash", liveOnAccountHash(playerName));
2298: 		payload.put("world", client.getWorld());
2299: 		Map<String, Object> extra = new LinkedHashMap<>();
2300: 		if (petName != null && !petName.trim().isEmpty())
2301: 		{
2302: 			extra.put("petName", petName.trim());
2303: 		}
2304: 		if (milestone != null && !milestone.trim().isEmpty())
2305: 		{
2306: 			extra.put("milestone", milestone.trim());
2307: 		}
2308: 		extra.put("duplicate", duplicate);
2309: 		extra.put("previouslyOwned", previouslyOwned);
2310: 		if (petRarity != null) extra.put("rarity", petRarity);
2311: 		extra.put("gameMessage", gameMessage);
2312: 		payload.put("extra", extra);
2313: 		try
2314: 		{
2315: 			MultipartBody.Builder multipart = new MultipartBody.Builder().setType(MultipartBody.FORM).addFormDataPart("payload_json", gson.toJson(payload));
2316: 			if (screenshot != null)
2317: 			{
2318: 				ByteArrayOutputStream output = new ByteArrayOutputStream();
2319: 				ImageIO.write((BufferedImage) screenshot, "png", output);
2320: 				multipart.addFormDataPart("file", DISCORD_PET_ATTACHMENT,
2321: 					RequestBody.create(MediaType.parse("image/png"), output.toByteArray()));
2322: 			}
2323: 			Request request = discordNotificationRequest(multipart.build());
2324: 			if (request == null)
2325: 			{
2326: 				return;
2327: 			}
2328: 			okHttpClient.newCall(request).enqueue(new SilentCallback());
2329: 		}
2330: 		catch (IOException exception) { log.debug("Unable to prepare pet notification payload", exception); }
2331: 	}
2332: 
2333: 	private Request discordNotificationRequest(RequestBody body)
2334: 	{
2335: 		HttpUrl base = serverBaseUrl();
2336: 		if (base == null || authenticatedPlayerName == null || authenticatedPlayerName.isEmpty())
2337: 		{
2338: 			log.debug("Discord notification skipped because the clan server is not authenticated");
2339: 			return null;
2340: 		}
2341: 		HttpUrl url = base.newBuilder().addPathSegments("notifications/discord").build();
2342: 		return requestBuilder(url).post(body).build();
2343: 	}
```

## `discordNotificationRequest` around line 2335

```java
2317: 			{
2318: 				ByteArrayOutputStream output = new ByteArrayOutputStream();
2319: 				ImageIO.write((BufferedImage) screenshot, "png", output);
2320: 				multipart.addFormDataPart("file", DISCORD_PET_ATTACHMENT,
2321: 					RequestBody.create(MediaType.parse("image/png"), output.toByteArray()));
2322: 			}
2323: 			Request request = discordNotificationRequest(multipart.build());
2324: 			if (request == null)
2325: 			{
2326: 				return;
2327: 			}
2328: 			okHttpClient.newCall(request).enqueue(new SilentCallback());
2329: 		}
2330: 		catch (IOException exception) { log.debug("Unable to prepare pet notification payload", exception); }
2331: 	}
2332: 
2333: 	private Request discordNotificationRequest(RequestBody body)
2334: 	{
2335: 		HttpUrl base = serverBaseUrl();
2336: 		if (base == null || authenticatedPlayerName == null || authenticatedPlayerName.isEmpty())
2337: 		{
2338: 			log.debug("Discord notification skipped because the clan server is not authenticated");
2339: 			return null;
2340: 		}
2341: 		HttpUrl url = base.newBuilder().addPathSegments("notifications/discord").build();
2342: 		return requestBuilder(url).post(body).build();
2343: 	}
2344: 
2345: 	private static Map<String, Object> footer()
2346: 	{
2347: 		Map<String, Object> footer = new LinkedHashMap<>();
2348: 		footer.put("text", "Enviado pelo Live ON Clan Plugin");
2349: 		footer.put("icon_url", "https://raw.githubusercontent.com/MilicoOSRS/live-on-clan/master/src/main/resources/live-on-logo.png");
2350: 		return footer;
2351: 	}
2352: 
2353: 	private Map<String, Object> author(String playerName)
2354: 	{
2355: 		Map<String, Object> author = new LinkedHashMap<>();
2356: 		author.put("name", playerName);
2357: 		author.put("url", "https://wiseoldman.net/players/" + playerName.replace(" ", "%20"));
2358: 		String badgeUrl = accountBadgeUrl(String.valueOf(client.getAccountType()),
2359: 			client.getWorldType().contains(WorldType.SEASONAL));
2360: 		if (badgeUrl != null)
2361: 		{
2362: 			author.put("icon_url", badgeUrl);
2363: 		}
```

## `discordNotificationRequest` around line 2341

```java
2323: 			Request request = discordNotificationRequest(multipart.build());
2324: 			if (request == null)
2325: 			{
2326: 				return;
2327: 			}
2328: 			okHttpClient.newCall(request).enqueue(new SilentCallback());
2329: 		}
2330: 		catch (IOException exception) { log.debug("Unable to prepare pet notification payload", exception); }
2331: 	}
2332: 
2333: 	private Request discordNotificationRequest(RequestBody body)
2334: 	{
2335: 		HttpUrl base = serverBaseUrl();
2336: 		if (base == null || authenticatedPlayerName == null || authenticatedPlayerName.isEmpty())
2337: 		{
2338: 			log.debug("Discord notification skipped because the clan server is not authenticated");
2339: 			return null;
2340: 		}
2341: 		HttpUrl url = base.newBuilder().addPathSegments("notifications/discord").build();
2342: 		return requestBuilder(url).post(body).build();
2343: 	}
2344: 
2345: 	private static Map<String, Object> footer()
2346: 	{
2347: 		Map<String, Object> footer = new LinkedHashMap<>();
2348: 		footer.put("text", "Enviado pelo Live ON Clan Plugin");
2349: 		footer.put("icon_url", "https://raw.githubusercontent.com/MilicoOSRS/live-on-clan/master/src/main/resources/live-on-logo.png");
2350: 		return footer;
2351: 	}
2352: 
2353: 	private Map<String, Object> author(String playerName)
2354: 	{
2355: 		Map<String, Object> author = new LinkedHashMap<>();
2356: 		author.put("name", playerName);
2357: 		author.put("url", "https://wiseoldman.net/players/" + playerName.replace(" ", "%20"));
2358: 		String badgeUrl = accountBadgeUrl(String.valueOf(client.getAccountType()),
2359: 			client.getWorldType().contains(WorldType.SEASONAL));
2360: 		if (badgeUrl != null)
2361: 		{
2362: 			author.put("icon_url", badgeUrl);
2363: 		}
2364: 		return author;
2365: 	}
2366: 
2367: 	private static String accountBadgeUrl(String accountType, boolean seasonal)
2368: 	{
2369: 		final String wikiImages = "https://oldschool.runescape.wiki/images/";
```

## `fetchMessages` around line 3031

```java
3013: 		}
3014: 		long interval = Math.max(5, config.pollIntervalSeconds());
3015: 		pollingTask = executor.scheduleAtFixedRate(this::fetchMessages, 0, interval, TimeUnit.SECONDS);
3016: 		mvpDropsPollingTask = executor.scheduleAtFixedRate(this::fetchMvpRankings, 2, 60, TimeUnit.SECONDS);
3017: 		if (isStaff && hasStaffAccessKey())
3018: 		{
3019: 			rankRequestsPollingTask = executor.scheduleAtFixedRate(this::fetchRankRequests, 10, interval, TimeUnit.SECONDS);
3020: 		}
3021: 	}
3022: 
3023: 	private void fetchMessages()
3024: 	{
3025: 		if (client.getGameState() != GameState.LOGGED_IN || authenticatedPlayerName == null
3026: 			|| authenticatedPlayerName.isEmpty()) return;
3027: 		if (!messageFetchInFlight.compareAndSet(false, true))
3028: 		{
3029: 			return;
3030: 		}
3031: 		HttpUrl base = serverBaseUrl();
3032: 		if (base == null)
3033: 		{
3034: 			panel.setStatus("URL inválida");
3035: 			messageFetchInFlight.set(false);
3036: 			return;
3037: 		}
3038: 		boolean initializeSession = !messageSessionInitialized;
3039: 		long requestGeneration = messageSessionGeneration.get();
3040: 		HttpUrl.Builder urlBuilder = base.newBuilder()
3041: 			.addPathSegment("messages")
3042: 			.addQueryParameter("after", lastMessageId);
3043: 		if (initializeSession)
3044: 		{
3045: 			urlBuilder.addQueryParameter("sessionStart", "1");
3046: 		}
3047: 		HttpUrl url = urlBuilder.build();
3048: 		okHttpClient.newCall(requestBuilder(url).get().build()).enqueue(new okhttp3.Callback()
3049: 		{
3050: 			@Override public void onFailure(okhttp3.Call call, IOException exception)
3051: 			{
3052: 				if (requestGeneration != messageSessionGeneration.get())
3053: 				{
3054: 					messageFetchInFlight.set(false);
3055: 					return;
3056: 				}
3057: 				log.debug("Unable to fetch clan messages", exception);
3058: 				if (panel != null) panel.setStatus("Sem conexão");
3059: 				messageFetchInFlight.set(false);
```

## `fetchMessages` around line 3040

```java
3022: 
3023: 	private void fetchMessages()
3024: 	{
3025: 		if (client.getGameState() != GameState.LOGGED_IN || authenticatedPlayerName == null
3026: 			|| authenticatedPlayerName.isEmpty()) return;
3027: 		if (!messageFetchInFlight.compareAndSet(false, true))
3028: 		{
3029: 			return;
3030: 		}
3031: 		HttpUrl base = serverBaseUrl();
3032: 		if (base == null)
3033: 		{
3034: 			panel.setStatus("URL inválida");
3035: 			messageFetchInFlight.set(false);
3036: 			return;
3037: 		}
3038: 		boolean initializeSession = !messageSessionInitialized;
3039: 		long requestGeneration = messageSessionGeneration.get();
3040: 		HttpUrl.Builder urlBuilder = base.newBuilder()
3041: 			.addPathSegment("messages")
3042: 			.addQueryParameter("after", lastMessageId);
3043: 		if (initializeSession)
3044: 		{
3045: 			urlBuilder.addQueryParameter("sessionStart", "1");
3046: 		}
3047: 		HttpUrl url = urlBuilder.build();
3048: 		okHttpClient.newCall(requestBuilder(url).get().build()).enqueue(new okhttp3.Callback()
3049: 		{
3050: 			@Override public void onFailure(okhttp3.Call call, IOException exception)
3051: 			{
3052: 				if (requestGeneration != messageSessionGeneration.get())
3053: 				{
3054: 					messageFetchInFlight.set(false);
3055: 					return;
3056: 				}
3057: 				log.debug("Unable to fetch clan messages", exception);
3058: 				if (panel != null) panel.setStatus("Sem conexão");
3059: 				messageFetchInFlight.set(false);
3060: 			}
3061: 
3062: 			@Override public void onResponse(okhttp3.Call call, Response response) throws IOException
3063: 			{
3064: 				try (Response ignored = response)
3065: 				{
3066: 					if (requestGeneration != messageSessionGeneration.get())
3067: 					{
3068: 						return;
```

## `fetchMessages` around line 3041

```java
3023: 	private void fetchMessages()
3024: 	{
3025: 		if (client.getGameState() != GameState.LOGGED_IN || authenticatedPlayerName == null
3026: 			|| authenticatedPlayerName.isEmpty()) return;
3027: 		if (!messageFetchInFlight.compareAndSet(false, true))
3028: 		{
3029: 			return;
3030: 		}
3031: 		HttpUrl base = serverBaseUrl();
3032: 		if (base == null)
3033: 		{
3034: 			panel.setStatus("URL inválida");
3035: 			messageFetchInFlight.set(false);
3036: 			return;
3037: 		}
3038: 		boolean initializeSession = !messageSessionInitialized;
3039: 		long requestGeneration = messageSessionGeneration.get();
3040: 		HttpUrl.Builder urlBuilder = base.newBuilder()
3041: 			.addPathSegment("messages")
3042: 			.addQueryParameter("after", lastMessageId);
3043: 		if (initializeSession)
3044: 		{
3045: 			urlBuilder.addQueryParameter("sessionStart", "1");
3046: 		}
3047: 		HttpUrl url = urlBuilder.build();
3048: 		okHttpClient.newCall(requestBuilder(url).get().build()).enqueue(new okhttp3.Callback()
3049: 		{
3050: 			@Override public void onFailure(okhttp3.Call call, IOException exception)
3051: 			{
3052: 				if (requestGeneration != messageSessionGeneration.get())
3053: 				{
3054: 					messageFetchInFlight.set(false);
3055: 					return;
3056: 				}
3057: 				log.debug("Unable to fetch clan messages", exception);
3058: 				if (panel != null) panel.setStatus("Sem conexão");
3059: 				messageFetchInFlight.set(false);
3060: 			}
3061: 
3062: 			@Override public void onResponse(okhttp3.Call call, Response response) throws IOException
3063: 			{
3064: 				try (Response ignored = response)
3065: 				{
3066: 					if (requestGeneration != messageSessionGeneration.get())
3067: 					{
3068: 						return;
3069: 					}
```

## `fetchMessages` around line 3042

```java
3024: 	{
3025: 		if (client.getGameState() != GameState.LOGGED_IN || authenticatedPlayerName == null
3026: 			|| authenticatedPlayerName.isEmpty()) return;
3027: 		if (!messageFetchInFlight.compareAndSet(false, true))
3028: 		{
3029: 			return;
3030: 		}
3031: 		HttpUrl base = serverBaseUrl();
3032: 		if (base == null)
3033: 		{
3034: 			panel.setStatus("URL inválida");
3035: 			messageFetchInFlight.set(false);
3036: 			return;
3037: 		}
3038: 		boolean initializeSession = !messageSessionInitialized;
3039: 		long requestGeneration = messageSessionGeneration.get();
3040: 		HttpUrl.Builder urlBuilder = base.newBuilder()
3041: 			.addPathSegment("messages")
3042: 			.addQueryParameter("after", lastMessageId);
3043: 		if (initializeSession)
3044: 		{
3045: 			urlBuilder.addQueryParameter("sessionStart", "1");
3046: 		}
3047: 		HttpUrl url = urlBuilder.build();
3048: 		okHttpClient.newCall(requestBuilder(url).get().build()).enqueue(new okhttp3.Callback()
3049: 		{
3050: 			@Override public void onFailure(okhttp3.Call call, IOException exception)
3051: 			{
3052: 				if (requestGeneration != messageSessionGeneration.get())
3053: 				{
3054: 					messageFetchInFlight.set(false);
3055: 					return;
3056: 				}
3057: 				log.debug("Unable to fetch clan messages", exception);
3058: 				if (panel != null) panel.setStatus("Sem conexão");
3059: 				messageFetchInFlight.set(false);
3060: 			}
3061: 
3062: 			@Override public void onResponse(okhttp3.Call call, Response response) throws IOException
3063: 			{
3064: 				try (Response ignored = response)
3065: 				{
3066: 					if (requestGeneration != messageSessionGeneration.get())
3067: 					{
3068: 						return;
3069: 					}
3070: 					String clearMarker = response.header("X-Live-On-Cleared-At", "");
```

## `fetchMessages` around line 3045

```java
3027: 		if (!messageFetchInFlight.compareAndSet(false, true))
3028: 		{
3029: 			return;
3030: 		}
3031: 		HttpUrl base = serverBaseUrl();
3032: 		if (base == null)
3033: 		{
3034: 			panel.setStatus("URL inválida");
3035: 			messageFetchInFlight.set(false);
3036: 			return;
3037: 		}
3038: 		boolean initializeSession = !messageSessionInitialized;
3039: 		long requestGeneration = messageSessionGeneration.get();
3040: 		HttpUrl.Builder urlBuilder = base.newBuilder()
3041: 			.addPathSegment("messages")
3042: 			.addQueryParameter("after", lastMessageId);
3043: 		if (initializeSession)
3044: 		{
3045: 			urlBuilder.addQueryParameter("sessionStart", "1");
3046: 		}
3047: 		HttpUrl url = urlBuilder.build();
3048: 		okHttpClient.newCall(requestBuilder(url).get().build()).enqueue(new okhttp3.Callback()
3049: 		{
3050: 			@Override public void onFailure(okhttp3.Call call, IOException exception)
3051: 			{
3052: 				if (requestGeneration != messageSessionGeneration.get())
3053: 				{
3054: 					messageFetchInFlight.set(false);
3055: 					return;
3056: 				}
3057: 				log.debug("Unable to fetch clan messages", exception);
3058: 				if (panel != null) panel.setStatus("Sem conexão");
3059: 				messageFetchInFlight.set(false);
3060: 			}
3061: 
3062: 			@Override public void onResponse(okhttp3.Call call, Response response) throws IOException
3063: 			{
3064: 				try (Response ignored = response)
3065: 				{
3066: 					if (requestGeneration != messageSessionGeneration.get())
3067: 					{
3068: 						return;
3069: 					}
3070: 					String clearMarker = response.header("X-Live-On-Cleared-At", "");
3071: 					if (!clearMarker.isEmpty() && !clearMarker.equals(lastClearMarker))
3072: 					{
3073: 						lastClearMarker = clearMarker;
```

## `fetchMessages` around line 3047

```java
3029: 			return;
3030: 		}
3031: 		HttpUrl base = serverBaseUrl();
3032: 		if (base == null)
3033: 		{
3034: 			panel.setStatus("URL inválida");
3035: 			messageFetchInFlight.set(false);
3036: 			return;
3037: 		}
3038: 		boolean initializeSession = !messageSessionInitialized;
3039: 		long requestGeneration = messageSessionGeneration.get();
3040: 		HttpUrl.Builder urlBuilder = base.newBuilder()
3041: 			.addPathSegment("messages")
3042: 			.addQueryParameter("after", lastMessageId);
3043: 		if (initializeSession)
3044: 		{
3045: 			urlBuilder.addQueryParameter("sessionStart", "1");
3046: 		}
3047: 		HttpUrl url = urlBuilder.build();
3048: 		okHttpClient.newCall(requestBuilder(url).get().build()).enqueue(new okhttp3.Callback()
3049: 		{
3050: 			@Override public void onFailure(okhttp3.Call call, IOException exception)
3051: 			{
3052: 				if (requestGeneration != messageSessionGeneration.get())
3053: 				{
3054: 					messageFetchInFlight.set(false);
3055: 					return;
3056: 				}
3057: 				log.debug("Unable to fetch clan messages", exception);
3058: 				if (panel != null) panel.setStatus("Sem conexão");
3059: 				messageFetchInFlight.set(false);
3060: 			}
3061: 
3062: 			@Override public void onResponse(okhttp3.Call call, Response response) throws IOException
3063: 			{
3064: 				try (Response ignored = response)
3065: 				{
3066: 					if (requestGeneration != messageSessionGeneration.get())
3067: 					{
3068: 						return;
3069: 					}
3070: 					String clearMarker = response.header("X-Live-On-Cleared-At", "");
3071: 					if (!clearMarker.isEmpty() && !clearMarker.equals(lastClearMarker))
3072: 					{
3073: 						lastClearMarker = clearMarker;
3074: 						if (panel != null) panel.clearMessages();
3075: 					}
```

## `removePanelNotice` around line 3265

```java
3247: 				if (panel != null) panel.setPanelNoticeStatus("Falha ao publicar aviso");
3248: 			}
3249: 
3250: 			@Override public void onResponse(okhttp3.Call call, Response response)
3251: 			{
3252: 				try (Response ignored = response)
3253: 				{
3254: 					if (panel != null) panel.setPanelNoticeStatus(
3255: 						response.isSuccessful() ? "Aviso publicado no Painel" : "Erro " + response.code());
3256: 					if (response.isSuccessful()) fetchPanelNotice();
3257: 				}
3258: 			}
3259: 		});
3260: 	}
3261: 
3262: 	private void removePanelNotice()
3263: 	{
3264: 		if (!isStaff) return;
3265: 		HttpUrl base = serverBaseUrl();
3266: 		if (base == null) return;
3267: 		HttpUrl url = base.newBuilder().addPathSegments("admin/panel-notice").build();
3268: 		okHttpClient.newCall(requestBuilder(url).delete().build()).enqueue(new okhttp3.Callback()
3269: 		{
3270: 			@Override public void onFailure(okhttp3.Call call, IOException exception)
3271: 			{
3272: 				log.debug("Unable to remove panel notice", exception);
3273: 				if (panel != null) panel.setPanelNoticeStatus("Falha ao remover aviso");
3274: 			}
3275: 
3276: 			@Override public void onResponse(okhttp3.Call call, Response response)
3277: 			{
3278: 				try (Response ignored = response)
3279: 				{
3280: 					if (panel != null) panel.setPanelNoticeStatus(
3281: 						response.isSuccessful() ? "Aviso removido" : "Erro " + response.code());
3282: 					if (response.isSuccessful() && panel != null) panel.updatePanelNotice("");
3283: 				}
3284: 			}
3285: 		});
3286: 	}
3287: 
3288: 	private void fetchRecentActivities()
3289: 	{
3290: 		if (client.getGameState() != GameState.LOGGED_IN || authenticatedPlayerName == null
3291: 			|| authenticatedPlayerName.isEmpty()) return;
3292: 		long generation = connectionSessionGeneration.get();
3293: 		String account = authenticatedPlayerName;
```

## `removePanelNotice` around line 3267

```java
3249: 
3250: 			@Override public void onResponse(okhttp3.Call call, Response response)
3251: 			{
3252: 				try (Response ignored = response)
3253: 				{
3254: 					if (panel != null) panel.setPanelNoticeStatus(
3255: 						response.isSuccessful() ? "Aviso publicado no Painel" : "Erro " + response.code());
3256: 					if (response.isSuccessful()) fetchPanelNotice();
3257: 				}
3258: 			}
3259: 		});
3260: 	}
3261: 
3262: 	private void removePanelNotice()
3263: 	{
3264: 		if (!isStaff) return;
3265: 		HttpUrl base = serverBaseUrl();
3266: 		if (base == null) return;
3267: 		HttpUrl url = base.newBuilder().addPathSegments("admin/panel-notice").build();
3268: 		okHttpClient.newCall(requestBuilder(url).delete().build()).enqueue(new okhttp3.Callback()
3269: 		{
3270: 			@Override public void onFailure(okhttp3.Call call, IOException exception)
3271: 			{
3272: 				log.debug("Unable to remove panel notice", exception);
3273: 				if (panel != null) panel.setPanelNoticeStatus("Falha ao remover aviso");
3274: 			}
3275: 
3276: 			@Override public void onResponse(okhttp3.Call call, Response response)
3277: 			{
3278: 				try (Response ignored = response)
3279: 				{
3280: 					if (panel != null) panel.setPanelNoticeStatus(
3281: 						response.isSuccessful() ? "Aviso removido" : "Erro " + response.code());
3282: 					if (response.isSuccessful() && panel != null) panel.updatePanelNotice("");
3283: 				}
3284: 			}
3285: 		});
3286: 	}
3287: 
3288: 	private void fetchRecentActivities()
3289: 	{
3290: 		if (client.getGameState() != GameState.LOGGED_IN || authenticatedPlayerName == null
3291: 			|| authenticatedPlayerName.isEmpty()) return;
3292: 		long generation = connectionSessionGeneration.get();
3293: 		String account = authenticatedPlayerName;
3294: 		getJson("stats/recent-activity", new okhttp3.Callback()
3295: 		{
```

## `deleteLiveChannel` around line 3462

```java
3444: 				{
3445: 					if (panel != null) panel.setLivesStatus(response.isSuccessful() ? "Canal associado" : "Erro " + response.code());
3446: 					if (response.isSuccessful())
3447: 					{
3448: 						if (panel != null) panel.clearLiveFields();
3449: 						fetchLives();
3450: 					}
3451: 				}
3452: 			}
3453: 		});
3454: 	}
3455: 
3456: 	private void deleteLiveChannel(LiveChannel channel)
3457: 	{
3458: 		if (!isStaff || channel == null)
3459: 		{
3460: 			return;
3461: 		}
3462: 		HttpUrl base = serverBaseUrl();
3463: 		if (base == null) return;
3464: 		HttpUrl url = base.newBuilder().addPathSegment("admin").addPathSegment("live-channels")
3465: 			.addPathSegment(Integer.toString(channel.id)).build();
3466: 		okHttpClient.newCall(requestBuilder(url).delete().build()).enqueue(new okhttp3.Callback()
3467: 		{
3468: 			@Override public void onFailure(okhttp3.Call call, IOException exception)
3469: 			{
3470: 				log.debug("Unable to remove Twitch channel", exception);
3471: 				if (panel != null) panel.setLivesStatus("Falha ao remover canal");
3472: 			}
3473: 			@Override public void onResponse(okhttp3.Call call, Response response) throws IOException
3474: 			{
3475: 				try (Response ignored = response)
3476: 				{
3477: 					if (panel != null) panel.setLivesStatus(response.isSuccessful() ? "Canal removido" : "Erro " + response.code());
3478: 					if (response.isSuccessful()) fetchLives();
3479: 				}
3480: 			}
3481: 		});
3482: 	}
3483: 
3484: 	private void fetchMvpMembers()
3485: 	{
3486: 		if (authenticatedPlayerName == null || authenticatedPlayerName.isEmpty())
3487: 		{
3488: 			mvpMembers.clear();
3489: 			return;
3490: 		}
```

## `deleteLiveChannel` around line 3464

```java
3446: 					if (response.isSuccessful())
3447: 					{
3448: 						if (panel != null) panel.clearLiveFields();
3449: 						fetchLives();
3450: 					}
3451: 				}
3452: 			}
3453: 		});
3454: 	}
3455: 
3456: 	private void deleteLiveChannel(LiveChannel channel)
3457: 	{
3458: 		if (!isStaff || channel == null)
3459: 		{
3460: 			return;
3461: 		}
3462: 		HttpUrl base = serverBaseUrl();
3463: 		if (base == null) return;
3464: 		HttpUrl url = base.newBuilder().addPathSegment("admin").addPathSegment("live-channels")
3465: 			.addPathSegment(Integer.toString(channel.id)).build();
3466: 		okHttpClient.newCall(requestBuilder(url).delete().build()).enqueue(new okhttp3.Callback()
3467: 		{
3468: 			@Override public void onFailure(okhttp3.Call call, IOException exception)
3469: 			{
3470: 				log.debug("Unable to remove Twitch channel", exception);
3471: 				if (panel != null) panel.setLivesStatus("Falha ao remover canal");
3472: 			}
3473: 			@Override public void onResponse(okhttp3.Call call, Response response) throws IOException
3474: 			{
3475: 				try (Response ignored = response)
3476: 				{
3477: 					if (panel != null) panel.setLivesStatus(response.isSuccessful() ? "Canal removido" : "Erro " + response.code());
3478: 					if (response.isSuccessful()) fetchLives();
3479: 				}
3480: 			}
3481: 		});
3482: 	}
3483: 
3484: 	private void fetchMvpMembers()
3485: 	{
3486: 		if (authenticatedPlayerName == null || authenticatedPlayerName.isEmpty())
3487: 		{
3488: 			mvpMembers.clear();
3489: 			return;
3490: 		}
3491: 		getJson("mvp-members", new okhttp3.Callback()
3492: 		{
```

## `deleteLiveChannel` around line 3465

```java
3447: 					{
3448: 						if (panel != null) panel.clearLiveFields();
3449: 						fetchLives();
3450: 					}
3451: 				}
3452: 			}
3453: 		});
3454: 	}
3455: 
3456: 	private void deleteLiveChannel(LiveChannel channel)
3457: 	{
3458: 		if (!isStaff || channel == null)
3459: 		{
3460: 			return;
3461: 		}
3462: 		HttpUrl base = serverBaseUrl();
3463: 		if (base == null) return;
3464: 		HttpUrl url = base.newBuilder().addPathSegment("admin").addPathSegment("live-channels")
3465: 			.addPathSegment(Integer.toString(channel.id)).build();
3466: 		okHttpClient.newCall(requestBuilder(url).delete().build()).enqueue(new okhttp3.Callback()
3467: 		{
3468: 			@Override public void onFailure(okhttp3.Call call, IOException exception)
3469: 			{
3470: 				log.debug("Unable to remove Twitch channel", exception);
3471: 				if (panel != null) panel.setLivesStatus("Falha ao remover canal");
3472: 			}
3473: 			@Override public void onResponse(okhttp3.Call call, Response response) throws IOException
3474: 			{
3475: 				try (Response ignored = response)
3476: 				{
3477: 					if (panel != null) panel.setLivesStatus(response.isSuccessful() ? "Canal removido" : "Erro " + response.code());
3478: 					if (response.isSuccessful()) fetchLives();
3479: 				}
3480: 			}
3481: 		});
3482: 	}
3483: 
3484: 	private void fetchMvpMembers()
3485: 	{
3486: 		if (authenticatedPlayerName == null || authenticatedPlayerName.isEmpty())
3487: 		{
3488: 			mvpMembers.clear();
3489: 			return;
3490: 		}
3491: 		getJson("mvp-members", new okhttp3.Callback()
3492: 		{
3493: 			@Override public void onFailure(okhttp3.Call call, IOException exception)
```

## `deleteMvpMember` around line 3566

```java
3548: 				{
3549: 					if (panel != null) panel.setMvpMembersStatus(response.isSuccessful() ? "MVP adicionado" : "Erro " + response.code());
3550: 					if (response.isSuccessful())
3551: 					{
3552: 						if (panel != null) panel.clearMvpMemberField();
3553: 						fetchMvpMembers();
3554: 					}
3555: 				}
3556: 			}
3557: 		});
3558: 	}
3559: 
3560: 	private void deleteMvpMember(MvpMember member)
3561: 	{
3562: 		if (!isDeputyOwner || member == null)
3563: 		{
3564: 			return;
3565: 		}
3566: 		HttpUrl base = serverBaseUrl();
3567: 		if (base == null) return;
3568: 		HttpUrl url = base.newBuilder().addPathSegment("admin").addPathSegment("mvp-members")
3569: 			.addPathSegment(Integer.toString(member.id)).build();
3570: 		okHttpClient.newCall(requestBuilder(url).delete().build()).enqueue(new okhttp3.Callback()
3571: 		{
3572: 			@Override public void onFailure(okhttp3.Call call, IOException exception)
3573: 			{
3574: 				log.debug("Unable to remove MVP member", exception);
3575: 				if (panel != null) panel.setMvpMembersStatus("Falha ao remover MVP");
3576: 			}
3577: 
3578: 			@Override public void onResponse(okhttp3.Call call, Response response) throws IOException
3579: 			{
3580: 				try (Response ignored = response)
3581: 				{
3582: 					if (panel != null) panel.setMvpMembersStatus(response.isSuccessful() ? "MVP removido" : "Erro " + response.code());
3583: 					if (response.isSuccessful()) fetchMvpMembers();
3584: 				}
3585: 			}
3586: 		});
3587: 	}
3588: 
3589: 	private void fetchClanTags()
3590: 	{
3591: 		if (authenticatedPlayerName == null || authenticatedPlayerName.isEmpty())
3592: 		{
3593: 			clanTagsByPlayer.clear();
3594: 			return;
```

## `deleteMvpMember` around line 3568

```java
3550: 					if (response.isSuccessful())
3551: 					{
3552: 						if (panel != null) panel.clearMvpMemberField();
3553: 						fetchMvpMembers();
3554: 					}
3555: 				}
3556: 			}
3557: 		});
3558: 	}
3559: 
3560: 	private void deleteMvpMember(MvpMember member)
3561: 	{
3562: 		if (!isDeputyOwner || member == null)
3563: 		{
3564: 			return;
3565: 		}
3566: 		HttpUrl base = serverBaseUrl();
3567: 		if (base == null) return;
3568: 		HttpUrl url = base.newBuilder().addPathSegment("admin").addPathSegment("mvp-members")
3569: 			.addPathSegment(Integer.toString(member.id)).build();
3570: 		okHttpClient.newCall(requestBuilder(url).delete().build()).enqueue(new okhttp3.Callback()
3571: 		{
3572: 			@Override public void onFailure(okhttp3.Call call, IOException exception)
3573: 			{
3574: 				log.debug("Unable to remove MVP member", exception);
3575: 				if (panel != null) panel.setMvpMembersStatus("Falha ao remover MVP");
3576: 			}
3577: 
3578: 			@Override public void onResponse(okhttp3.Call call, Response response) throws IOException
3579: 			{
3580: 				try (Response ignored = response)
3581: 				{
3582: 					if (panel != null) panel.setMvpMembersStatus(response.isSuccessful() ? "MVP removido" : "Erro " + response.code());
3583: 					if (response.isSuccessful()) fetchMvpMembers();
3584: 				}
3585: 			}
3586: 		});
3587: 	}
3588: 
3589: 	private void fetchClanTags()
3590: 	{
3591: 		if (authenticatedPlayerName == null || authenticatedPlayerName.isEmpty())
3592: 		{
3593: 			clanTagsByPlayer.clear();
3594: 			return;
3595: 		}
3596: 		getJson("clan-tags", new okhttp3.Callback()
```

## `deleteMvpMember` around line 3569

```java
3551: 					{
3552: 						if (panel != null) panel.clearMvpMemberField();
3553: 						fetchMvpMembers();
3554: 					}
3555: 				}
3556: 			}
3557: 		});
3558: 	}
3559: 
3560: 	private void deleteMvpMember(MvpMember member)
3561: 	{
3562: 		if (!isDeputyOwner || member == null)
3563: 		{
3564: 			return;
3565: 		}
3566: 		HttpUrl base = serverBaseUrl();
3567: 		if (base == null) return;
3568: 		HttpUrl url = base.newBuilder().addPathSegment("admin").addPathSegment("mvp-members")
3569: 			.addPathSegment(Integer.toString(member.id)).build();
3570: 		okHttpClient.newCall(requestBuilder(url).delete().build()).enqueue(new okhttp3.Callback()
3571: 		{
3572: 			@Override public void onFailure(okhttp3.Call call, IOException exception)
3573: 			{
3574: 				log.debug("Unable to remove MVP member", exception);
3575: 				if (panel != null) panel.setMvpMembersStatus("Falha ao remover MVP");
3576: 			}
3577: 
3578: 			@Override public void onResponse(okhttp3.Call call, Response response) throws IOException
3579: 			{
3580: 				try (Response ignored = response)
3581: 				{
3582: 					if (panel != null) panel.setMvpMembersStatus(response.isSuccessful() ? "MVP removido" : "Erro " + response.code());
3583: 					if (response.isSuccessful()) fetchMvpMembers();
3584: 				}
3585: 			}
3586: 		});
3587: 	}
3588: 
3589: 	private void fetchClanTags()
3590: 	{
3591: 		if (authenticatedPlayerName == null || authenticatedPlayerName.isEmpty())
3592: 		{
3593: 			clanTagsByPlayer.clear();
3594: 			return;
3595: 		}
3596: 		getJson("clan-tags", new okhttp3.Callback()
3597: 		{
```

## `fetchPbRanking` around line 3677

```java
3659: 						? java.util.Collections.emptyList() : java.util.Arrays.asList(values));
3660: 				}
3661: 				finally { finishPbCategoriesFetch(); }
3662: 			}
3663: 		});
3664: 	}
3665: 
3666: 	private void finishPbCategoriesFetch()
3667: 	{
3668: 		pbCategoriesFetchInFlight.set(false);
3669: 		if (panel != null) panel.setPbRefreshEnabled(true);
3670: 	}
3671: 
3672: 	private void fetchPbRanking(PbCategory category)
3673: 	{
3674: 		if (category == null || authenticatedPlayerName == null || authenticatedPlayerName.isEmpty()) return;
3675: 		long requestGeneration = pbRankingRequestGeneration.incrementAndGet();
3676: 		if (panel != null) panel.beginPbRankingRequest(requestGeneration);
3677: 		HttpUrl base = serverBaseUrl();
3678: 		if (base == null) return;
3679: 		HttpUrl url = base.newBuilder().addPathSegments("stats/pb-ranking")
3680: 			.addQueryParameter("boss", category.boss)
3681: 			.addQueryParameter("mode", category.mode == null ? "" : category.mode)
3682: 			.addQueryParameter("teamSize", Integer.toString(category.team_size))
3683: 			.addQueryParameter("timeType", category.time_type == null ? "" : category.time_type).build();
3684: 		okHttpClient.newCall(requestBuilder(url).get().build()).enqueue(new okhttp3.Callback()
3685: 		{
3686: 			@Override public void onFailure(okhttp3.Call call, IOException exception)
3687: 			{
3688: 				if (requestGeneration != pbRankingRequestGeneration.get()) return;
3689: 				log.debug("Unable to fetch PB ranking", exception);
3690: 			}
3691: 			@Override public void onResponse(okhttp3.Call call, Response response) throws IOException
3692: 			{
3693: 				try (Response ignored = response)
3694: 				{
3695: 					if (requestGeneration != pbRankingRequestGeneration.get()) return;
3696: 					if (!response.isSuccessful() || response.body() == null)
3697: 					{
3698: 						log.debug("PB ranking returned HTTP {}", response.code());
3699: 						return;
3700: 					}
3701: 					PbRankingResponse parsed = gson.fromJson(response.body().string(), PbRankingResponse.class);
3702: 					if (parsed != null && panel != null) panel.updatePbRanking(parsed, requestGeneration);
3703: 				}
3704: 			}
3705: 		});
```

## `fetchPbRanking` around line 3679

```java
3661: 				finally { finishPbCategoriesFetch(); }
3662: 			}
3663: 		});
3664: 	}
3665: 
3666: 	private void finishPbCategoriesFetch()
3667: 	{
3668: 		pbCategoriesFetchInFlight.set(false);
3669: 		if (panel != null) panel.setPbRefreshEnabled(true);
3670: 	}
3671: 
3672: 	private void fetchPbRanking(PbCategory category)
3673: 	{
3674: 		if (category == null || authenticatedPlayerName == null || authenticatedPlayerName.isEmpty()) return;
3675: 		long requestGeneration = pbRankingRequestGeneration.incrementAndGet();
3676: 		if (panel != null) panel.beginPbRankingRequest(requestGeneration);
3677: 		HttpUrl base = serverBaseUrl();
3678: 		if (base == null) return;
3679: 		HttpUrl url = base.newBuilder().addPathSegments("stats/pb-ranking")
3680: 			.addQueryParameter("boss", category.boss)
3681: 			.addQueryParameter("mode", category.mode == null ? "" : category.mode)
3682: 			.addQueryParameter("teamSize", Integer.toString(category.team_size))
3683: 			.addQueryParameter("timeType", category.time_type == null ? "" : category.time_type).build();
3684: 		okHttpClient.newCall(requestBuilder(url).get().build()).enqueue(new okhttp3.Callback()
3685: 		{
3686: 			@Override public void onFailure(okhttp3.Call call, IOException exception)
3687: 			{
3688: 				if (requestGeneration != pbRankingRequestGeneration.get()) return;
3689: 				log.debug("Unable to fetch PB ranking", exception);
3690: 			}
3691: 			@Override public void onResponse(okhttp3.Call call, Response response) throws IOException
3692: 			{
3693: 				try (Response ignored = response)
3694: 				{
3695: 					if (requestGeneration != pbRankingRequestGeneration.get()) return;
3696: 					if (!response.isSuccessful() || response.body() == null)
3697: 					{
3698: 						log.debug("PB ranking returned HTTP {}", response.code());
3699: 						return;
3700: 					}
3701: 					PbRankingResponse parsed = gson.fromJson(response.body().string(), PbRankingResponse.class);
3702: 					if (parsed != null && panel != null) panel.updatePbRanking(parsed, requestGeneration);
3703: 				}
3704: 			}
3705: 		});
3706: 	}
3707: 
```

## `fetchPbRanking` around line 3680

```java
3662: 			}
3663: 		});
3664: 	}
3665: 
3666: 	private void finishPbCategoriesFetch()
3667: 	{
3668: 		pbCategoriesFetchInFlight.set(false);
3669: 		if (panel != null) panel.setPbRefreshEnabled(true);
3670: 	}
3671: 
3672: 	private void fetchPbRanking(PbCategory category)
3673: 	{
3674: 		if (category == null || authenticatedPlayerName == null || authenticatedPlayerName.isEmpty()) return;
3675: 		long requestGeneration = pbRankingRequestGeneration.incrementAndGet();
3676: 		if (panel != null) panel.beginPbRankingRequest(requestGeneration);
3677: 		HttpUrl base = serverBaseUrl();
3678: 		if (base == null) return;
3679: 		HttpUrl url = base.newBuilder().addPathSegments("stats/pb-ranking")
3680: 			.addQueryParameter("boss", category.boss)
3681: 			.addQueryParameter("mode", category.mode == null ? "" : category.mode)
3682: 			.addQueryParameter("teamSize", Integer.toString(category.team_size))
3683: 			.addQueryParameter("timeType", category.time_type == null ? "" : category.time_type).build();
3684: 		okHttpClient.newCall(requestBuilder(url).get().build()).enqueue(new okhttp3.Callback()
3685: 		{
3686: 			@Override public void onFailure(okhttp3.Call call, IOException exception)
3687: 			{
3688: 				if (requestGeneration != pbRankingRequestGeneration.get()) return;
3689: 				log.debug("Unable to fetch PB ranking", exception);
3690: 			}
3691: 			@Override public void onResponse(okhttp3.Call call, Response response) throws IOException
3692: 			{
3693: 				try (Response ignored = response)
3694: 				{
3695: 					if (requestGeneration != pbRankingRequestGeneration.get()) return;
3696: 					if (!response.isSuccessful() || response.body() == null)
3697: 					{
3698: 						log.debug("PB ranking returned HTTP {}", response.code());
3699: 						return;
3700: 					}
3701: 					PbRankingResponse parsed = gson.fromJson(response.body().string(), PbRankingResponse.class);
3702: 					if (parsed != null && panel != null) panel.updatePbRanking(parsed, requestGeneration);
3703: 				}
3704: 			}
3705: 		});
3706: 	}
3707: 
3708: 	private void submitPb(String boss, String mode, int teamSize, double seconds)
```

## `fetchPbRanking` around line 3681

```java
3663: 		});
3664: 	}
3665: 
3666: 	private void finishPbCategoriesFetch()
3667: 	{
3668: 		pbCategoriesFetchInFlight.set(false);
3669: 		if (panel != null) panel.setPbRefreshEnabled(true);
3670: 	}
3671: 
3672: 	private void fetchPbRanking(PbCategory category)
3673: 	{
3674: 		if (category == null || authenticatedPlayerName == null || authenticatedPlayerName.isEmpty()) return;
3675: 		long requestGeneration = pbRankingRequestGeneration.incrementAndGet();
3676: 		if (panel != null) panel.beginPbRankingRequest(requestGeneration);
3677: 		HttpUrl base = serverBaseUrl();
3678: 		if (base == null) return;
3679: 		HttpUrl url = base.newBuilder().addPathSegments("stats/pb-ranking")
3680: 			.addQueryParameter("boss", category.boss)
3681: 			.addQueryParameter("mode", category.mode == null ? "" : category.mode)
3682: 			.addQueryParameter("teamSize", Integer.toString(category.team_size))
3683: 			.addQueryParameter("timeType", category.time_type == null ? "" : category.time_type).build();
3684: 		okHttpClient.newCall(requestBuilder(url).get().build()).enqueue(new okhttp3.Callback()
3685: 		{
3686: 			@Override public void onFailure(okhttp3.Call call, IOException exception)
3687: 			{
3688: 				if (requestGeneration != pbRankingRequestGeneration.get()) return;
3689: 				log.debug("Unable to fetch PB ranking", exception);
3690: 			}
3691: 			@Override public void onResponse(okhttp3.Call call, Response response) throws IOException
3692: 			{
3693: 				try (Response ignored = response)
3694: 				{
3695: 					if (requestGeneration != pbRankingRequestGeneration.get()) return;
3696: 					if (!response.isSuccessful() || response.body() == null)
3697: 					{
3698: 						log.debug("PB ranking returned HTTP {}", response.code());
3699: 						return;
3700: 					}
3701: 					PbRankingResponse parsed = gson.fromJson(response.body().string(), PbRankingResponse.class);
3702: 					if (parsed != null && panel != null) panel.updatePbRanking(parsed, requestGeneration);
3703: 				}
3704: 			}
3705: 		});
3706: 	}
3707: 
3708: 	private void submitPb(String boss, String mode, int teamSize, double seconds)
3709: 	{
```

## `fetchPbRanking` around line 3682

```java
3664: 	}
3665: 
3666: 	private void finishPbCategoriesFetch()
3667: 	{
3668: 		pbCategoriesFetchInFlight.set(false);
3669: 		if (panel != null) panel.setPbRefreshEnabled(true);
3670: 	}
3671: 
3672: 	private void fetchPbRanking(PbCategory category)
3673: 	{
3674: 		if (category == null || authenticatedPlayerName == null || authenticatedPlayerName.isEmpty()) return;
3675: 		long requestGeneration = pbRankingRequestGeneration.incrementAndGet();
3676: 		if (panel != null) panel.beginPbRankingRequest(requestGeneration);
3677: 		HttpUrl base = serverBaseUrl();
3678: 		if (base == null) return;
3679: 		HttpUrl url = base.newBuilder().addPathSegments("stats/pb-ranking")
3680: 			.addQueryParameter("boss", category.boss)
3681: 			.addQueryParameter("mode", category.mode == null ? "" : category.mode)
3682: 			.addQueryParameter("teamSize", Integer.toString(category.team_size))
3683: 			.addQueryParameter("timeType", category.time_type == null ? "" : category.time_type).build();
3684: 		okHttpClient.newCall(requestBuilder(url).get().build()).enqueue(new okhttp3.Callback()
3685: 		{
3686: 			@Override public void onFailure(okhttp3.Call call, IOException exception)
3687: 			{
3688: 				if (requestGeneration != pbRankingRequestGeneration.get()) return;
3689: 				log.debug("Unable to fetch PB ranking", exception);
3690: 			}
3691: 			@Override public void onResponse(okhttp3.Call call, Response response) throws IOException
3692: 			{
3693: 				try (Response ignored = response)
3694: 				{
3695: 					if (requestGeneration != pbRankingRequestGeneration.get()) return;
3696: 					if (!response.isSuccessful() || response.body() == null)
3697: 					{
3698: 						log.debug("PB ranking returned HTTP {}", response.code());
3699: 						return;
3700: 					}
3701: 					PbRankingResponse parsed = gson.fromJson(response.body().string(), PbRankingResponse.class);
3702: 					if (parsed != null && panel != null) panel.updatePbRanking(parsed, requestGeneration);
3703: 				}
3704: 			}
3705: 		});
3706: 	}
3707: 
3708: 	private void submitPb(String boss, String mode, int teamSize, double seconds)
3709: 	{
3710: 		submitPb(boss, mode, teamSize, seconds, null);
```

## `fetchPbRanking` around line 3683

```java
3665: 
3666: 	private void finishPbCategoriesFetch()
3667: 	{
3668: 		pbCategoriesFetchInFlight.set(false);
3669: 		if (panel != null) panel.setPbRefreshEnabled(true);
3670: 	}
3671: 
3672: 	private void fetchPbRanking(PbCategory category)
3673: 	{
3674: 		if (category == null || authenticatedPlayerName == null || authenticatedPlayerName.isEmpty()) return;
3675: 		long requestGeneration = pbRankingRequestGeneration.incrementAndGet();
3676: 		if (panel != null) panel.beginPbRankingRequest(requestGeneration);
3677: 		HttpUrl base = serverBaseUrl();
3678: 		if (base == null) return;
3679: 		HttpUrl url = base.newBuilder().addPathSegments("stats/pb-ranking")
3680: 			.addQueryParameter("boss", category.boss)
3681: 			.addQueryParameter("mode", category.mode == null ? "" : category.mode)
3682: 			.addQueryParameter("teamSize", Integer.toString(category.team_size))
3683: 			.addQueryParameter("timeType", category.time_type == null ? "" : category.time_type).build();
3684: 		okHttpClient.newCall(requestBuilder(url).get().build()).enqueue(new okhttp3.Callback()
3685: 		{
3686: 			@Override public void onFailure(okhttp3.Call call, IOException exception)
3687: 			{
3688: 				if (requestGeneration != pbRankingRequestGeneration.get()) return;
3689: 				log.debug("Unable to fetch PB ranking", exception);
3690: 			}
3691: 			@Override public void onResponse(okhttp3.Call call, Response response) throws IOException
3692: 			{
3693: 				try (Response ignored = response)
3694: 				{
3695: 					if (requestGeneration != pbRankingRequestGeneration.get()) return;
3696: 					if (!response.isSuccessful() || response.body() == null)
3697: 					{
3698: 						log.debug("PB ranking returned HTTP {}", response.code());
3699: 						return;
3700: 					}
3701: 					PbRankingResponse parsed = gson.fromJson(response.body().string(), PbRankingResponse.class);
3702: 					if (parsed != null && panel != null) panel.updatePbRanking(parsed, requestGeneration);
3703: 				}
3704: 			}
3705: 		});
3706: 	}
3707: 
3708: 	private void submitPb(String boss, String mode, int teamSize, double seconds)
3709: 	{
3710: 		submitPb(boss, mode, teamSize, seconds, null);
3711: 	}
```

## `deleteClanTagPath` around line 3852

```java
3834: 			}
3835: 			@Override public void onResponse(okhttp3.Call call, Response response) throws IOException
3836: 			{
3837: 				try (Response ignored = response)
3838: 				{
3839: 					if (panel != null) panel.setClanTagsStatus(response.isSuccessful() ? success : "Erro " + response.code());
3840: 					if (response.isSuccessful())
3841: 					{
3842: 						if (clearAction != null) clearAction.run();
3843: 						fetchClanTags();
3844: 					}
3845: 				}
3846: 			}
3847: 		};
3848: 	}
3849: 
3850: 	private void deleteClanTagPath(String path, String success)
3851: 	{
3852: 		HttpUrl base = serverBaseUrl();
3853: 		if (base == null) return;
3854: 		HttpUrl.Builder builder = base.newBuilder();
3855: 		for (String segment : path.split("/")) builder.addPathSegment(segment);
3856: 		okHttpClient.newCall(requestBuilder(builder.build()).delete().build()).enqueue(clanTagWriteCallback(success, null));
3857: 	}
3858: 
3859: 	boolean isLiveStatusVisible()
3860: 	{
3861: 		return (config.liveStatusEnabled() && !onlineLiveChannels.isEmpty()) || !mvpMembers.isEmpty()
3862: 			|| !clanTagsByPlayer.isEmpty();
3863: 	}
3864: 
3865: 	boolean isPlayerLive(String playerName)
3866: 	{
3867: 		return config.liveStatusEnabled()
3868: 			&& onlineLiveChannels.containsKey(normalizeChatPlayerName(playerName));
3869: 	}
3870: 
3871: 	boolean isPlayerMvp(String playerName)
3872: 	{
3873: 		return mvpMembers.contains(normalizeChatPlayerName(playerName));
3874: 	}
3875: 
3876: 	String clanTagBadges(String playerName)
3877: 	{
3878: 		java.util.List<ClanTag> tags = clanTagsByPlayer.get(normalizeChatPlayerName(playerName));
3879: 		if (tags == null || tags.isEmpty()) return "";
3880: 		StringBuilder badges = new StringBuilder();
```

## `deleteClanTagPath` around line 3854

```java
3836: 			{
3837: 				try (Response ignored = response)
3838: 				{
3839: 					if (panel != null) panel.setClanTagsStatus(response.isSuccessful() ? success : "Erro " + response.code());
3840: 					if (response.isSuccessful())
3841: 					{
3842: 						if (clearAction != null) clearAction.run();
3843: 						fetchClanTags();
3844: 					}
3845: 				}
3846: 			}
3847: 		};
3848: 	}
3849: 
3850: 	private void deleteClanTagPath(String path, String success)
3851: 	{
3852: 		HttpUrl base = serverBaseUrl();
3853: 		if (base == null) return;
3854: 		HttpUrl.Builder builder = base.newBuilder();
3855: 		for (String segment : path.split("/")) builder.addPathSegment(segment);
3856: 		okHttpClient.newCall(requestBuilder(builder.build()).delete().build()).enqueue(clanTagWriteCallback(success, null));
3857: 	}
3858: 
3859: 	boolean isLiveStatusVisible()
3860: 	{
3861: 		return (config.liveStatusEnabled() && !onlineLiveChannels.isEmpty()) || !mvpMembers.isEmpty()
3862: 			|| !clanTagsByPlayer.isEmpty();
3863: 	}
3864: 
3865: 	boolean isPlayerLive(String playerName)
3866: 	{
3867: 		return config.liveStatusEnabled()
3868: 			&& onlineLiveChannels.containsKey(normalizeChatPlayerName(playerName));
3869: 	}
3870: 
3871: 	boolean isPlayerMvp(String playerName)
3872: 	{
3873: 		return mvpMembers.contains(normalizeChatPlayerName(playerName));
3874: 	}
3875: 
3876: 	String clanTagBadges(String playerName)
3877: 	{
3878: 		java.util.List<ClanTag> tags = clanTagsByPlayer.get(normalizeChatPlayerName(playerName));
3879: 		if (tags == null || tags.isEmpty()) return "";
3880: 		StringBuilder badges = new StringBuilder();
3881: 		for (ClanTag clanTag : tags) badges.append(clanTagMarkup(clanTag));
3882: 		return badges.toString();
```

## `deleteClanTagPath` around line 3855

```java
3837: 				try (Response ignored = response)
3838: 				{
3839: 					if (panel != null) panel.setClanTagsStatus(response.isSuccessful() ? success : "Erro " + response.code());
3840: 					if (response.isSuccessful())
3841: 					{
3842: 						if (clearAction != null) clearAction.run();
3843: 						fetchClanTags();
3844: 					}
3845: 				}
3846: 			}
3847: 		};
3848: 	}
3849: 
3850: 	private void deleteClanTagPath(String path, String success)
3851: 	{
3852: 		HttpUrl base = serverBaseUrl();
3853: 		if (base == null) return;
3854: 		HttpUrl.Builder builder = base.newBuilder();
3855: 		for (String segment : path.split("/")) builder.addPathSegment(segment);
3856: 		okHttpClient.newCall(requestBuilder(builder.build()).delete().build()).enqueue(clanTagWriteCallback(success, null));
3857: 	}
3858: 
3859: 	boolean isLiveStatusVisible()
3860: 	{
3861: 		return (config.liveStatusEnabled() && !onlineLiveChannels.isEmpty()) || !mvpMembers.isEmpty()
3862: 			|| !clanTagsByPlayer.isEmpty();
3863: 	}
3864: 
3865: 	boolean isPlayerLive(String playerName)
3866: 	{
3867: 		return config.liveStatusEnabled()
3868: 			&& onlineLiveChannels.containsKey(normalizeChatPlayerName(playerName));
3869: 	}
3870: 
3871: 	boolean isPlayerMvp(String playerName)
3872: 	{
3873: 		return mvpMembers.contains(normalizeChatPlayerName(playerName));
3874: 	}
3875: 
3876: 	String clanTagBadges(String playerName)
3877: 	{
3878: 		java.util.List<ClanTag> tags = clanTagsByPlayer.get(normalizeChatPlayerName(playerName));
3879: 		if (tags == null || tags.isEmpty()) return "";
3880: 		StringBuilder badges = new StringBuilder();
3881: 		for (ClanTag clanTag : tags) badges.append(clanTagMarkup(clanTag));
3882: 		return badges.toString();
3883: 	}
```

## `publishDraft` around line 4100

```java
4082: return false;
4083: }
4084: 
4085: private static void appendChatText(ChatMessageBuilder builder, Color color, String text)
4086: 	{
4087: 		if (color == null)
4088: 		{
4089: 			builder.append(text);
4090: 			return;
4091: 		}
4092: 		builder.append(color, text);
4093: 	}
4094: 
4095: 	private void publishDraft(String mode)
4096: 	{
4097: 		String message = panel.getDraft();
4098: 		if (message.isEmpty()) return;
4099: 		panel.setPublishing(true);
4100: 		HttpUrl base = serverBaseUrl();
4101: 		if (base == null)
4102: 		{
4103: 			panel.setStatus("URL inválida");
4104: 			panel.setPublishing(false);
4105: 			return;
4106: 		}
4107: 		String rsnName = authenticatedPlayerName;
4108: 		if (rsnName == null || rsnName.isEmpty())
4109: 		{
4110: 			panel.setStatus("Valide sua conta pelo WOM");
4111: 			panel.setPublishing(false);
4112: 			return;
4113: 		}
4114: 		java.util.Map<String, Object> bodyMap = new java.util.LinkedHashMap<>();
4115: 		bodyMap.put("message", message);
4116: 		bodyMap.put("mode", mode);
4117: 		bodyMap.put("playerName", rsnName);
4118: 		// Include pinned flag when publishing a BROADCAST if the panel checkbox is selected (server enforces staff requirement)
4119: 		final boolean pinned = panel != null && panel.isPinSelected();
4120: 		bodyMap.put("pinned", pinned);
4121: 		String jsonBody = gson.toJson(bodyMap);
4122: 				log.debug("Publish payload: {}", jsonBody);
4123: 				RequestBody body = RequestBody.create(JSON, jsonBody);
4124: 				Request request = requestBuilder(base.newBuilder().addPathSegment("messages").build()).post(body).build();
4125: 		okHttpClient.newCall(request).enqueue(new okhttp3.Callback()
4126: 		{
4127: 			@Override public void onFailure(okhttp3.Call call, IOException exception)
4128: 			{
```

## `publishDraft` around line 4124

```java
4106: 		}
4107: 		String rsnName = authenticatedPlayerName;
4108: 		if (rsnName == null || rsnName.isEmpty())
4109: 		{
4110: 			panel.setStatus("Valide sua conta pelo WOM");
4111: 			panel.setPublishing(false);
4112: 			return;
4113: 		}
4114: 		java.util.Map<String, Object> bodyMap = new java.util.LinkedHashMap<>();
4115: 		bodyMap.put("message", message);
4116: 		bodyMap.put("mode", mode);
4117: 		bodyMap.put("playerName", rsnName);
4118: 		// Include pinned flag when publishing a BROADCAST if the panel checkbox is selected (server enforces staff requirement)
4119: 		final boolean pinned = panel != null && panel.isPinSelected();
4120: 		bodyMap.put("pinned", pinned);
4121: 		String jsonBody = gson.toJson(bodyMap);
4122: 				log.debug("Publish payload: {}", jsonBody);
4123: 				RequestBody body = RequestBody.create(JSON, jsonBody);
4124: 				Request request = requestBuilder(base.newBuilder().addPathSegment("messages").build()).post(body).build();
4125: 		okHttpClient.newCall(request).enqueue(new okhttp3.Callback()
4126: 		{
4127: 			@Override public void onFailure(okhttp3.Call call, IOException exception)
4128: 			{
4129: 				log.debug("Unable to publish clan message", exception);
4130: 				if (panel != null) panel.setStatus("Falha ao publicar");
4131: 				if (panel != null) panel.setPublishing(false);
4132: 			}
4133: 
4134: 			@Override public void onResponse(okhttp3.Call call, Response response) throws IOException
4135: 			{
4136: 				try (Response ignored = response)
4137: 				{
4138: 					String body = response.body() == null ? "" : response.body().string();
4139: 					log.debug("Publish response: code={} body={}", response.code(), body);
4140: 					if (panel != null)
4141: 					{
4142: 						if (response.isSuccessful())
4143: 						{
4144: 							panel.clearDraft();
4145: 							panel.setStatus("Publicado");
4146: 							displayPublishedMessage(body, "Staff", message, mode, pinned);
4147: 						}
4148: 						else if (response.code() == 403 && body.contains("staff_required"))
4149: 						{
4150: 							panel.setStatus("Acesso staff necessário para publicar");
4151: 						}
4152: 						else if (response.code() == 403 && body.contains("broadcast_role_required"))
```

## `deleteSentMessage` around line 4258

```java
4240: 						response.body().string(), ClanMessagesPanel.StaffSentMessage[].class);
4241: 					if (panel != null)
4242: 					{
4243: 						panel.updateSentMessages(sent == null
4244: 							? java.util.Collections.emptyList()
4245: 							: java.util.Arrays.asList(sent));
4246: 					}
4247: 				}
4248: 			}
4249: 		});
4250: 	}
4251: 
4252: 	private void deleteSentMessage(ClanMessagesPanel.StaffSentMessage sentMessage)
4253: 	{
4254: 		if (!isStaff || sentMessage == null || sentMessage.id == null)
4255: 		{
4256: 			return;
4257: 		}
4258: 		HttpUrl base = serverBaseUrl();
4259: 		if (base == null) return;
4260: 		HttpUrl url = base.newBuilder().addPathSegment("admin").addPathSegment("messages")
4261: 			.addPathSegment(sentMessage.id).build();
4262: 		okHttpClient.newCall(requestBuilder(url).delete().build()).enqueue(new okhttp3.Callback()
4263: 		{
4264: 			@Override public void onFailure(okhttp3.Call call, IOException exception)
4265: 			{
4266: 				log.debug("Unable to delete sent message", exception);
4267: 				if (panel != null) panel.setSentMessagesStatus("Falha ao remover");
4268: 			}
4269: 			@Override public void onResponse(okhttp3.Call call, Response response) throws IOException
4270: 			{
4271: 				try (Response ignored = response)
4272: 				{
4273: 					if (panel != null) panel.setSentMessagesStatus(response.isSuccessful() ? "Mensagem removida" : "Erro " + response.code());
4274: 					if (response.isSuccessful()) fetchSentMessages();
4275: 				}
4276: 			}
4277: 		});
4278: 	}
4279: 
4280: 	private void resendSentMessage(ClanMessagesPanel.StaffSentMessage sentMessage)
4281: 	{
4282: 		if (sentMessage == null || panel == null)
4283: 		{
4284: 			return;
4285: 		}
4286: 		panel.setDraft(sentMessage.message, sentMessage.isPinned());
```

## `deleteSentMessage` around line 4260

```java
4242: 					{
4243: 						panel.updateSentMessages(sent == null
4244: 							? java.util.Collections.emptyList()
4245: 							: java.util.Arrays.asList(sent));
4246: 					}
4247: 				}
4248: 			}
4249: 		});
4250: 	}
4251: 
4252: 	private void deleteSentMessage(ClanMessagesPanel.StaffSentMessage sentMessage)
4253: 	{
4254: 		if (!isStaff || sentMessage == null || sentMessage.id == null)
4255: 		{
4256: 			return;
4257: 		}
4258: 		HttpUrl base = serverBaseUrl();
4259: 		if (base == null) return;
4260: 		HttpUrl url = base.newBuilder().addPathSegment("admin").addPathSegment("messages")
4261: 			.addPathSegment(sentMessage.id).build();
4262: 		okHttpClient.newCall(requestBuilder(url).delete().build()).enqueue(new okhttp3.Callback()
4263: 		{
4264: 			@Override public void onFailure(okhttp3.Call call, IOException exception)
4265: 			{
4266: 				log.debug("Unable to delete sent message", exception);
4267: 				if (panel != null) panel.setSentMessagesStatus("Falha ao remover");
4268: 			}
4269: 			@Override public void onResponse(okhttp3.Call call, Response response) throws IOException
4270: 			{
4271: 				try (Response ignored = response)
4272: 				{
4273: 					if (panel != null) panel.setSentMessagesStatus(response.isSuccessful() ? "Mensagem removida" : "Erro " + response.code());
4274: 					if (response.isSuccessful()) fetchSentMessages();
4275: 				}
4276: 			}
4277: 		});
4278: 	}
4279: 
4280: 	private void resendSentMessage(ClanMessagesPanel.StaffSentMessage sentMessage)
4281: 	{
4282: 		if (sentMessage == null || panel == null)
4283: 		{
4284: 			return;
4285: 		}
4286: 		panel.setDraft(sentMessage.message, sentMessage.isPinned());
4287: 		SwingUtilities.invokeLater(() -> publishDraft(
4288: 			"CLAN".equalsIgnoreCase(sentMessage.mode) ? "CLAN" : "BROADCAST"));
```

## `deleteSentMessage` around line 4261

```java
4243: 						panel.updateSentMessages(sent == null
4244: 							? java.util.Collections.emptyList()
4245: 							: java.util.Arrays.asList(sent));
4246: 					}
4247: 				}
4248: 			}
4249: 		});
4250: 	}
4251: 
4252: 	private void deleteSentMessage(ClanMessagesPanel.StaffSentMessage sentMessage)
4253: 	{
4254: 		if (!isStaff || sentMessage == null || sentMessage.id == null)
4255: 		{
4256: 			return;
4257: 		}
4258: 		HttpUrl base = serverBaseUrl();
4259: 		if (base == null) return;
4260: 		HttpUrl url = base.newBuilder().addPathSegment("admin").addPathSegment("messages")
4261: 			.addPathSegment(sentMessage.id).build();
4262: 		okHttpClient.newCall(requestBuilder(url).delete().build()).enqueue(new okhttp3.Callback()
4263: 		{
4264: 			@Override public void onFailure(okhttp3.Call call, IOException exception)
4265: 			{
4266: 				log.debug("Unable to delete sent message", exception);
4267: 				if (panel != null) panel.setSentMessagesStatus("Falha ao remover");
4268: 			}
4269: 			@Override public void onResponse(okhttp3.Call call, Response response) throws IOException
4270: 			{
4271: 				try (Response ignored = response)
4272: 				{
4273: 					if (panel != null) panel.setSentMessagesStatus(response.isSuccessful() ? "Mensagem removida" : "Erro " + response.code());
4274: 					if (response.isSuccessful()) fetchSentMessages();
4275: 				}
4276: 			}
4277: 		});
4278: 	}
4279: 
4280: 	private void resendSentMessage(ClanMessagesPanel.StaffSentMessage sentMessage)
4281: 	{
4282: 		if (sentMessage == null || panel == null)
4283: 		{
4284: 			return;
4285: 		}
4286: 		panel.setDraft(sentMessage.message, sentMessage.isPinned());
4287: 		SwingUtilities.invokeLater(() -> publishDraft(
4288: 			"CLAN".equalsIgnoreCase(sentMessage.mode) ? "CLAN" : "BROADCAST"));
4289: 	}
```

## `togglePinnedMessage` around line 4302

```java
4284: 			return;
4285: 		}
4286: 		panel.setDraft(sentMessage.message, sentMessage.isPinned());
4287: 		SwingUtilities.invokeLater(() -> publishDraft(
4288: 			"CLAN".equalsIgnoreCase(sentMessage.mode) ? "CLAN" : "BROADCAST"));
4289: 	}
4290: 
4291: 	private void togglePinnedMessage(ClanMessagesPanel.StaffSentMessage sentMessage)
4292: 	{
4293: 		if (!isStaff || sentMessage == null || sentMessage.id == null)
4294: 		{
4295: 			return;
4296: 		}
4297: 		if (!"BROADCAST".equalsIgnoreCase(sentMessage.mode))
4298: 		{
4299: 			if (panel != null) panel.setSentMessagesStatus("Somente broadcasts podem ser fixados");
4300: 			return;
4301: 		}
4302: 		HttpUrl base = serverBaseUrl();
4303: 		if (base == null) return;
4304: 		boolean newPinnedValue = !sentMessage.isPinned();
4305: 		java.util.Map<String, Object> payload = new java.util.LinkedHashMap<>();
4306: 		payload.put("pinned", newPinnedValue);
4307: 		payload.put("playerName", authenticatedPlayerName);
4308: 		RequestBody body = RequestBody.create(JSON, gson.toJson(payload));
4309: 		HttpUrl url = base.newBuilder().addPathSegment("admin").addPathSegment("messages")
4310: 			.addPathSegment(sentMessage.id).addPathSegment("pin").build();
4311: 		okHttpClient.newCall(requestBuilder(url).post(body).build()).enqueue(new okhttp3.Callback()
4312: 		{
4313: 			@Override public void onFailure(okhttp3.Call call, IOException exception)
4314: 			{
4315: 				log.debug("Unable to change pinned message", exception);
4316: 				if (panel != null) panel.setSentMessagesStatus("Falha ao alterar mensagem fixada");
4317: 			}
4318: 			@Override public void onResponse(okhttp3.Call call, Response response) throws IOException
4319: 			{
4320: 				try (Response ignored = response)
4321: 				{
4322: 					if (panel != null)
4323: 					{
4324: 						panel.setSentMessagesStatus(response.isSuccessful()
4325: 							? (newPinnedValue ? "Mensagem fixada" : "Mensagem desfixada")
4326: 							: "Erro " + response.code());
4327: 					}
4328: 					if (response.isSuccessful()) fetchSentMessages();
4329: 				}
4330: 			}
```

## `togglePinnedMessage` around line 4309

```java
4291: 	private void togglePinnedMessage(ClanMessagesPanel.StaffSentMessage sentMessage)
4292: 	{
4293: 		if (!isStaff || sentMessage == null || sentMessage.id == null)
4294: 		{
4295: 			return;
4296: 		}
4297: 		if (!"BROADCAST".equalsIgnoreCase(sentMessage.mode))
4298: 		{
4299: 			if (panel != null) panel.setSentMessagesStatus("Somente broadcasts podem ser fixados");
4300: 			return;
4301: 		}
4302: 		HttpUrl base = serverBaseUrl();
4303: 		if (base == null) return;
4304: 		boolean newPinnedValue = !sentMessage.isPinned();
4305: 		java.util.Map<String, Object> payload = new java.util.LinkedHashMap<>();
4306: 		payload.put("pinned", newPinnedValue);
4307: 		payload.put("playerName", authenticatedPlayerName);
4308: 		RequestBody body = RequestBody.create(JSON, gson.toJson(payload));
4309: 		HttpUrl url = base.newBuilder().addPathSegment("admin").addPathSegment("messages")
4310: 			.addPathSegment(sentMessage.id).addPathSegment("pin").build();
4311: 		okHttpClient.newCall(requestBuilder(url).post(body).build()).enqueue(new okhttp3.Callback()
4312: 		{
4313: 			@Override public void onFailure(okhttp3.Call call, IOException exception)
4314: 			{
4315: 				log.debug("Unable to change pinned message", exception);
4316: 				if (panel != null) panel.setSentMessagesStatus("Falha ao alterar mensagem fixada");
4317: 			}
4318: 			@Override public void onResponse(okhttp3.Call call, Response response) throws IOException
4319: 			{
4320: 				try (Response ignored = response)
4321: 				{
4322: 					if (panel != null)
4323: 					{
4324: 						panel.setSentMessagesStatus(response.isSuccessful()
4325: 							? (newPinnedValue ? "Mensagem fixada" : "Mensagem desfixada")
4326: 							: "Erro " + response.code());
4327: 					}
4328: 					if (response.isSuccessful()) fetchSentMessages();
4329: 				}
4330: 			}
4331: 		});
4332: 	}
4333: 
4334: 	private void clearMessages()
4335: 	{
4336: 		if (!isStaff)
4337: 		{
```

## `togglePinnedMessage` around line 4310

```java
4292: 	{
4293: 		if (!isStaff || sentMessage == null || sentMessage.id == null)
4294: 		{
4295: 			return;
4296: 		}
4297: 		if (!"BROADCAST".equalsIgnoreCase(sentMessage.mode))
4298: 		{
4299: 			if (panel != null) panel.setSentMessagesStatus("Somente broadcasts podem ser fixados");
4300: 			return;
4301: 		}
4302: 		HttpUrl base = serverBaseUrl();
4303: 		if (base == null) return;
4304: 		boolean newPinnedValue = !sentMessage.isPinned();
4305: 		java.util.Map<String, Object> payload = new java.util.LinkedHashMap<>();
4306: 		payload.put("pinned", newPinnedValue);
4307: 		payload.put("playerName", authenticatedPlayerName);
4308: 		RequestBody body = RequestBody.create(JSON, gson.toJson(payload));
4309: 		HttpUrl url = base.newBuilder().addPathSegment("admin").addPathSegment("messages")
4310: 			.addPathSegment(sentMessage.id).addPathSegment("pin").build();
4311: 		okHttpClient.newCall(requestBuilder(url).post(body).build()).enqueue(new okhttp3.Callback()
4312: 		{
4313: 			@Override public void onFailure(okhttp3.Call call, IOException exception)
4314: 			{
4315: 				log.debug("Unable to change pinned message", exception);
4316: 				if (panel != null) panel.setSentMessagesStatus("Falha ao alterar mensagem fixada");
4317: 			}
4318: 			@Override public void onResponse(okhttp3.Call call, Response response) throws IOException
4319: 			{
4320: 				try (Response ignored = response)
4321: 				{
4322: 					if (panel != null)
4323: 					{
4324: 						panel.setSentMessagesStatus(response.isSuccessful()
4325: 							? (newPinnedValue ? "Mensagem fixada" : "Mensagem desfixada")
4326: 							: "Erro " + response.code());
4327: 					}
4328: 					if (response.isSuccessful()) fetchSentMessages();
4329: 				}
4330: 			}
4331: 		});
4332: 	}
4333: 
4334: 	private void clearMessages()
4335: 	{
4336: 		if (!isStaff)
4337: 		{
4338: 			panel.setStatus("Acesso staff necessário");
```

## `requestRank` around line 4366

```java
4348: 		});
4349: 	}
4350: 
4351: 	private void requestRank()
4352: 	{
4353: 		String playerName = authenticatedPlayerName;
4354: 		if (playerName == null || playerName.isEmpty())
4355: 		{
4356: 			if (panel != null) panel.setStatus("Valide sua conta pelo WOM");
4357: 			return;
4358: 		}
4359: 		String currentRank = panel.getCurrentRank();
4360: 		if (currentRank.isEmpty() || currentRank.contains("não sincronizado") || currentRank.contains("em análise"))
4361: 		{
4362: 			if (panel != null) panel.setStatus("Sincronize o rank antes de solicitar");
4363: 			return;
4364: 		}
4365: 		String message = playerName + " solicitou um rank: " + currentRank;
4366: 		HttpUrl base = serverBaseUrl();
4367: 		if (base == null)
4368: 		{
4369: 			if (panel != null) panel.setStatus("URL inválida");
4370: 			return;
4371: 		}
4372: 		java.util.Map<String, Object> requestPayload = new java.util.LinkedHashMap<>();
4373: 		requestPayload.put("message", message);
4374: 		requestPayload.put("mode", "STAFF");
4375: 		requestPayload.put("playerName", playerName);
4376: 		RequestBody body = RequestBody.create(JSON, gson.toJson(requestPayload));
4377: 		Request request = requestBuilder(base.newBuilder().addPathSegment("messages").build()).post(body).build();
4378: 		okHttpClient.newCall(request).enqueue(new okhttp3.Callback()
4379: 		{
4380: 			@Override public void onFailure(okhttp3.Call call, IOException exception)
4381: 			{
4382: 				log.debug("Unable to send rank request", exception);
4383: 				if (panel != null) panel.setStatus("Falha ao solicitar rank");
4384: 			}
4385: 
4386: 			@Override public void onResponse(okhttp3.Call call, Response response) throws IOException
4387: 			{
4388: 				try (Response ignored = response)
4389: 				{
4390: 					String responseBody = response.body() == null ? "" : response.body().string();
4391: 					log.debug("Rank request response: code={} body={}", response.code(), responseBody);
4392: 					if (panel != null)
4393: 					{
4394: 						if (response.isSuccessful())
```

## `requestRank` around line 4377

```java
4359: 		String currentRank = panel.getCurrentRank();
4360: 		if (currentRank.isEmpty() || currentRank.contains("não sincronizado") || currentRank.contains("em análise"))
4361: 		{
4362: 			if (panel != null) panel.setStatus("Sincronize o rank antes de solicitar");
4363: 			return;
4364: 		}
4365: 		String message = playerName + " solicitou um rank: " + currentRank;
4366: 		HttpUrl base = serverBaseUrl();
4367: 		if (base == null)
4368: 		{
4369: 			if (panel != null) panel.setStatus("URL inválida");
4370: 			return;
4371: 		}
4372: 		java.util.Map<String, Object> requestPayload = new java.util.LinkedHashMap<>();
4373: 		requestPayload.put("message", message);
4374: 		requestPayload.put("mode", "STAFF");
4375: 		requestPayload.put("playerName", playerName);
4376: 		RequestBody body = RequestBody.create(JSON, gson.toJson(requestPayload));
4377: 		Request request = requestBuilder(base.newBuilder().addPathSegment("messages").build()).post(body).build();
4378: 		okHttpClient.newCall(request).enqueue(new okhttp3.Callback()
4379: 		{
4380: 			@Override public void onFailure(okhttp3.Call call, IOException exception)
4381: 			{
4382: 				log.debug("Unable to send rank request", exception);
4383: 				if (panel != null) panel.setStatus("Falha ao solicitar rank");
4384: 			}
4385: 
4386: 			@Override public void onResponse(okhttp3.Call call, Response response) throws IOException
4387: 			{
4388: 				try (Response ignored = response)
4389: 				{
4390: 					String responseBody = response.body() == null ? "" : response.body().string();
4391: 					log.debug("Rank request response: code={} body={}", response.code(), responseBody);
4392: 					if (panel != null)
4393: 					{
4394: 						if (response.isSuccessful())
4395: 						{
4396: 							rankRequestStatusKnown = true;
4397: 							rankRequestPending = true;
4398: 							panel.setRankRequestState(true, 0);
4399: 							panel.setStatusSuccess("Solicita\u00E7\u00E3o enviada para a staff.");
4400: 							// Rank requests are staff-only. Do not simulate the STAFF
4401: 							// message for the requesting member.
4402: 							scheduleMessageRefresh();
4403: 							if (isStaff) fetchRankRequests();
4404: 						}
4405: 						else if (response.code() == 409)
```

## `fetchRankRequestStatus` around line 4428

```java
4410: 							panel.setStatus("Você já possui uma solicitação pendente");
4411: 						}
4412: 						else if (response.code() == 429)
4413: 						{
4414: 							int retryAfter = rankRetryAfter(responseBody, response.header("Retry-After"));
4415: 							panel.setRankRequestState(false, retryAfter);
4416: 							panel.setStatus("Aguarde " + Math.max(1, (retryAfter + 59) / 60) + " min para solicitar novamente");
4417: 						}
4418: 						else panel.setStatus("Erro " + response.code());
4419: 					}
4420: 				}
4421: 			}
4422: 		});
4423: 	}
4424: 
4425: 	private void fetchRankRequestStatus()
4426: 	{
4427: 		if (authenticatedPlayerName == null || authenticatedPlayerName.isEmpty() || panel == null) return;
4428: 		HttpUrl base = serverBaseUrl();
4429: 		if (base == null) return;
4430: 		Request request = requestBuilder(base.newBuilder().addPathSegments("rank-request/status").build()).get().build();
4431: 		okHttpClient.newCall(request).enqueue(new okhttp3.Callback()
4432: 		{
4433: 			@Override public void onFailure(okhttp3.Call call, IOException exception)
4434: 			{
4435: 				log.debug("Unable to fetch rank request status", exception);
4436: 			}
4437: 
4438: 			@Override public void onResponse(okhttp3.Call call, Response response) throws IOException
4439: 			{
4440: 				try (Response ignored = response)
4441: 				{
4442: 					if (!response.isSuccessful() || response.body() == null) return;
4443: 					com.google.gson.JsonObject state = gson.fromJson(response.body().string(), com.google.gson.JsonObject.class);
4444: 					boolean pending = state != null && state.has("pending") && state.get("pending").getAsBoolean();
4445: 					int cooldown = state != null && state.has("cooldownRemaining")
4446: 						? Math.max(0, state.get("cooldownRemaining").getAsInt()) : 0;
4447: 					rankRequestStatusKnown = true;
4448: 					rankRequestPending = pending;
4449: 					panel.setRankRequestState(pending, cooldown);
4450: 				}
4451: 			}
4452: 		});
4453: 	}
4454: 
4455: 	private int rankRetryAfter(String responseBody, String retryHeader)
4456: 	{
```

## `verifyToken` around line 4555

```java
4537: 				}
4538: 				else
4539: 				{
4540: 					authenticatedPlayerName = "";
4541: 					isStaff = false;
4542: 					isDeputyOwner = false;
4543: 					canPublishBroadcast = false;
4544: 					if (panel != null) { panel.setStatus("Não é membro do clã (WOM)"); panel.setAccessMessage("Membro não identificado. Este plugin é exclusivo para membros do Live On."); panel.startVerifyCooldown(VERIFY_COOLDOWN_SECONDS); }
4545: 					return;
4546: 				}
4547: 			}
4548: 			// Not cached: perform network check. Disable verify button and show status.
4549: 			if (panel != null) { panel.setVerifyEnabled(false); panel.setAccessMessage("Verificando..."); }
4550: 			okhttp3.Call previousCall = currentWomCall;
4551: 			if (previousCall != null)
4552: 			{
4553: 				previousCall.cancel();
4554: 			}
4555: 			HttpUrl womUrl = new HttpUrl.Builder()
4556: 				.scheme("https")
4557: 				.host("api.wiseoldman.net")
4558: 				.addPathSegment("v2")
4559: 				.addPathSegment("players")
4560: 				.addPathSegment(rsn)
4561: 				.addPathSegment("groups")
4562: 				.addQueryParameter("limit", "50")
4563: 				.build();
4564: 			okhttp3.Call call = okHttpClient.newCall(new Request.Builder()
4565: 				.url(womUrl)
4566: 				.header("Accept", "application/json")
4567: 				.header("User-Agent", WOM_USER_AGENT)
4568: 				.get()
4569: 				.build());
4570: 			currentWomCall = call;
4571: 			call.enqueue(new okhttp3.Callback()
4572: 			{
4573: 				@Override public void onFailure(okhttp3.Call call, IOException exception)
4574: 				{
4575: 					if (call.isCanceled())
4576: 					{
4577: 						return;
4578: 					}
4579: 					if (currentWomCall != call)
4580: 					{
4581: 						return;
4582: 					}
4583: 					log.debug("WOM membership check failed", exception);
```

## `verifyToken` around line 4558

```java
4540: 					authenticatedPlayerName = "";
4541: 					isStaff = false;
4542: 					isDeputyOwner = false;
4543: 					canPublishBroadcast = false;
4544: 					if (panel != null) { panel.setStatus("Não é membro do clã (WOM)"); panel.setAccessMessage("Membro não identificado. Este plugin é exclusivo para membros do Live On."); panel.startVerifyCooldown(VERIFY_COOLDOWN_SECONDS); }
4545: 					return;
4546: 				}
4547: 			}
4548: 			// Not cached: perform network check. Disable verify button and show status.
4549: 			if (panel != null) { panel.setVerifyEnabled(false); panel.setAccessMessage("Verificando..."); }
4550: 			okhttp3.Call previousCall = currentWomCall;
4551: 			if (previousCall != null)
4552: 			{
4553: 				previousCall.cancel();
4554: 			}
4555: 			HttpUrl womUrl = new HttpUrl.Builder()
4556: 				.scheme("https")
4557: 				.host("api.wiseoldman.net")
4558: 				.addPathSegment("v2")
4559: 				.addPathSegment("players")
4560: 				.addPathSegment(rsn)
4561: 				.addPathSegment("groups")
4562: 				.addQueryParameter("limit", "50")
4563: 				.build();
4564: 			okhttp3.Call call = okHttpClient.newCall(new Request.Builder()
4565: 				.url(womUrl)
4566: 				.header("Accept", "application/json")
4567: 				.header("User-Agent", WOM_USER_AGENT)
4568: 				.get()
4569: 				.build());
4570: 			currentWomCall = call;
4571: 			call.enqueue(new okhttp3.Callback()
4572: 			{
4573: 				@Override public void onFailure(okhttp3.Call call, IOException exception)
4574: 				{
4575: 					if (call.isCanceled())
4576: 					{
4577: 						return;
4578: 					}
4579: 					if (currentWomCall != call)
4580: 					{
4581: 						return;
4582: 					}
4583: 					log.debug("WOM membership check failed", exception);
4584: 					if (panel != null) panel.setAuthenticated(false, false);
4585: 					if (panel != null) { panel.setStatus("Falha ao verificar grupo (WOM)"); panel.setAccessMessage("Falha ao verificar grupo (WOM)"); panel.startVerifyCooldown(VERIFY_COOLDOWN_SECONDS); }
4586: 					authenticatedPlayerName = "";
```

## `verifyToken` around line 4559

```java
4541: 					isStaff = false;
4542: 					isDeputyOwner = false;
4543: 					canPublishBroadcast = false;
4544: 					if (panel != null) { panel.setStatus("Não é membro do clã (WOM)"); panel.setAccessMessage("Membro não identificado. Este plugin é exclusivo para membros do Live On."); panel.startVerifyCooldown(VERIFY_COOLDOWN_SECONDS); }
4545: 					return;
4546: 				}
4547: 			}
4548: 			// Not cached: perform network check. Disable verify button and show status.
4549: 			if (panel != null) { panel.setVerifyEnabled(false); panel.setAccessMessage("Verificando..."); }
4550: 			okhttp3.Call previousCall = currentWomCall;
4551: 			if (previousCall != null)
4552: 			{
4553: 				previousCall.cancel();
4554: 			}
4555: 			HttpUrl womUrl = new HttpUrl.Builder()
4556: 				.scheme("https")
4557: 				.host("api.wiseoldman.net")
4558: 				.addPathSegment("v2")
4559: 				.addPathSegment("players")
4560: 				.addPathSegment(rsn)
4561: 				.addPathSegment("groups")
4562: 				.addQueryParameter("limit", "50")
4563: 				.build();
4564: 			okhttp3.Call call = okHttpClient.newCall(new Request.Builder()
4565: 				.url(womUrl)
4566: 				.header("Accept", "application/json")
4567: 				.header("User-Agent", WOM_USER_AGENT)
4568: 				.get()
4569: 				.build());
4570: 			currentWomCall = call;
4571: 			call.enqueue(new okhttp3.Callback()
4572: 			{
4573: 				@Override public void onFailure(okhttp3.Call call, IOException exception)
4574: 				{
4575: 					if (call.isCanceled())
4576: 					{
4577: 						return;
4578: 					}
4579: 					if (currentWomCall != call)
4580: 					{
4581: 						return;
4582: 					}
4583: 					log.debug("WOM membership check failed", exception);
4584: 					if (panel != null) panel.setAuthenticated(false, false);
4585: 					if (panel != null) { panel.setStatus("Falha ao verificar grupo (WOM)"); panel.setAccessMessage("Falha ao verificar grupo (WOM)"); panel.startVerifyCooldown(VERIFY_COOLDOWN_SECONDS); }
4586: 					authenticatedPlayerName = "";
4587: 					isStaff = false;
```

## `verifyToken` around line 4560

```java
4542: 					isDeputyOwner = false;
4543: 					canPublishBroadcast = false;
4544: 					if (panel != null) { panel.setStatus("Não é membro do clã (WOM)"); panel.setAccessMessage("Membro não identificado. Este plugin é exclusivo para membros do Live On."); panel.startVerifyCooldown(VERIFY_COOLDOWN_SECONDS); }
4545: 					return;
4546: 				}
4547: 			}
4548: 			// Not cached: perform network check. Disable verify button and show status.
4549: 			if (panel != null) { panel.setVerifyEnabled(false); panel.setAccessMessage("Verificando..."); }
4550: 			okhttp3.Call previousCall = currentWomCall;
4551: 			if (previousCall != null)
4552: 			{
4553: 				previousCall.cancel();
4554: 			}
4555: 			HttpUrl womUrl = new HttpUrl.Builder()
4556: 				.scheme("https")
4557: 				.host("api.wiseoldman.net")
4558: 				.addPathSegment("v2")
4559: 				.addPathSegment("players")
4560: 				.addPathSegment(rsn)
4561: 				.addPathSegment("groups")
4562: 				.addQueryParameter("limit", "50")
4563: 				.build();
4564: 			okhttp3.Call call = okHttpClient.newCall(new Request.Builder()
4565: 				.url(womUrl)
4566: 				.header("Accept", "application/json")
4567: 				.header("User-Agent", WOM_USER_AGENT)
4568: 				.get()
4569: 				.build());
4570: 			currentWomCall = call;
4571: 			call.enqueue(new okhttp3.Callback()
4572: 			{
4573: 				@Override public void onFailure(okhttp3.Call call, IOException exception)
4574: 				{
4575: 					if (call.isCanceled())
4576: 					{
4577: 						return;
4578: 					}
4579: 					if (currentWomCall != call)
4580: 					{
4581: 						return;
4582: 					}
4583: 					log.debug("WOM membership check failed", exception);
4584: 					if (panel != null) panel.setAuthenticated(false, false);
4585: 					if (panel != null) { panel.setStatus("Falha ao verificar grupo (WOM)"); panel.setAccessMessage("Falha ao verificar grupo (WOM)"); panel.startVerifyCooldown(VERIFY_COOLDOWN_SECONDS); }
4586: 					authenticatedPlayerName = "";
4587: 					isStaff = false;
4588: 					if (currentWomCall == call) currentWomCall = null;
```

## `verifyToken` around line 4561

```java
4543: 					canPublishBroadcast = false;
4544: 					if (panel != null) { panel.setStatus("Não é membro do clã (WOM)"); panel.setAccessMessage("Membro não identificado. Este plugin é exclusivo para membros do Live On."); panel.startVerifyCooldown(VERIFY_COOLDOWN_SECONDS); }
4545: 					return;
4546: 				}
4547: 			}
4548: 			// Not cached: perform network check. Disable verify button and show status.
4549: 			if (panel != null) { panel.setVerifyEnabled(false); panel.setAccessMessage("Verificando..."); }
4550: 			okhttp3.Call previousCall = currentWomCall;
4551: 			if (previousCall != null)
4552: 			{
4553: 				previousCall.cancel();
4554: 			}
4555: 			HttpUrl womUrl = new HttpUrl.Builder()
4556: 				.scheme("https")
4557: 				.host("api.wiseoldman.net")
4558: 				.addPathSegment("v2")
4559: 				.addPathSegment("players")
4560: 				.addPathSegment(rsn)
4561: 				.addPathSegment("groups")
4562: 				.addQueryParameter("limit", "50")
4563: 				.build();
4564: 			okhttp3.Call call = okHttpClient.newCall(new Request.Builder()
4565: 				.url(womUrl)
4566: 				.header("Accept", "application/json")
4567: 				.header("User-Agent", WOM_USER_AGENT)
4568: 				.get()
4569: 				.build());
4570: 			currentWomCall = call;
4571: 			call.enqueue(new okhttp3.Callback()
4572: 			{
4573: 				@Override public void onFailure(okhttp3.Call call, IOException exception)
4574: 				{
4575: 					if (call.isCanceled())
4576: 					{
4577: 						return;
4578: 					}
4579: 					if (currentWomCall != call)
4580: 					{
4581: 						return;
4582: 					}
4583: 					log.debug("WOM membership check failed", exception);
4584: 					if (panel != null) panel.setAuthenticated(false, false);
4585: 					if (panel != null) { panel.setStatus("Falha ao verificar grupo (WOM)"); panel.setAccessMessage("Falha ao verificar grupo (WOM)"); panel.startVerifyCooldown(VERIFY_COOLDOWN_SECONDS); }
4586: 					authenticatedPlayerName = "";
4587: 					isStaff = false;
4588: 					if (currentWomCall == call) currentWomCall = null;
4589: 				}
```

## `verifyToken` around line 4562

```java
4544: 					if (panel != null) { panel.setStatus("Não é membro do clã (WOM)"); panel.setAccessMessage("Membro não identificado. Este plugin é exclusivo para membros do Live On."); panel.startVerifyCooldown(VERIFY_COOLDOWN_SECONDS); }
4545: 					return;
4546: 				}
4547: 			}
4548: 			// Not cached: perform network check. Disable verify button and show status.
4549: 			if (panel != null) { panel.setVerifyEnabled(false); panel.setAccessMessage("Verificando..."); }
4550: 			okhttp3.Call previousCall = currentWomCall;
4551: 			if (previousCall != null)
4552: 			{
4553: 				previousCall.cancel();
4554: 			}
4555: 			HttpUrl womUrl = new HttpUrl.Builder()
4556: 				.scheme("https")
4557: 				.host("api.wiseoldman.net")
4558: 				.addPathSegment("v2")
4559: 				.addPathSegment("players")
4560: 				.addPathSegment(rsn)
4561: 				.addPathSegment("groups")
4562: 				.addQueryParameter("limit", "50")
4563: 				.build();
4564: 			okhttp3.Call call = okHttpClient.newCall(new Request.Builder()
4565: 				.url(womUrl)
4566: 				.header("Accept", "application/json")
4567: 				.header("User-Agent", WOM_USER_AGENT)
4568: 				.get()
4569: 				.build());
4570: 			currentWomCall = call;
4571: 			call.enqueue(new okhttp3.Callback()
4572: 			{
4573: 				@Override public void onFailure(okhttp3.Call call, IOException exception)
4574: 				{
4575: 					if (call.isCanceled())
4576: 					{
4577: 						return;
4578: 					}
4579: 					if (currentWomCall != call)
4580: 					{
4581: 						return;
4582: 					}
4583: 					log.debug("WOM membership check failed", exception);
4584: 					if (panel != null) panel.setAuthenticated(false, false);
4585: 					if (panel != null) { panel.setStatus("Falha ao verificar grupo (WOM)"); panel.setAccessMessage("Falha ao verificar grupo (WOM)"); panel.startVerifyCooldown(VERIFY_COOLDOWN_SECONDS); }
4586: 					authenticatedPlayerName = "";
4587: 					isStaff = false;
4588: 					if (currentWomCall == call) currentWomCall = null;
4589: 				}
4590: 
```

## `verifyToken` around line 4564

```java
4546: 				}
4547: 			}
4548: 			// Not cached: perform network check. Disable verify button and show status.
4549: 			if (panel != null) { panel.setVerifyEnabled(false); panel.setAccessMessage("Verificando..."); }
4550: 			okhttp3.Call previousCall = currentWomCall;
4551: 			if (previousCall != null)
4552: 			{
4553: 				previousCall.cancel();
4554: 			}
4555: 			HttpUrl womUrl = new HttpUrl.Builder()
4556: 				.scheme("https")
4557: 				.host("api.wiseoldman.net")
4558: 				.addPathSegment("v2")
4559: 				.addPathSegment("players")
4560: 				.addPathSegment(rsn)
4561: 				.addPathSegment("groups")
4562: 				.addQueryParameter("limit", "50")
4563: 				.build();
4564: 			okhttp3.Call call = okHttpClient.newCall(new Request.Builder()
4565: 				.url(womUrl)
4566: 				.header("Accept", "application/json")
4567: 				.header("User-Agent", WOM_USER_AGENT)
4568: 				.get()
4569: 				.build());
4570: 			currentWomCall = call;
4571: 			call.enqueue(new okhttp3.Callback()
4572: 			{
4573: 				@Override public void onFailure(okhttp3.Call call, IOException exception)
4574: 				{
4575: 					if (call.isCanceled())
4576: 					{
4577: 						return;
4578: 					}
4579: 					if (currentWomCall != call)
4580: 					{
4581: 						return;
4582: 					}
4583: 					log.debug("WOM membership check failed", exception);
4584: 					if (panel != null) panel.setAuthenticated(false, false);
4585: 					if (panel != null) { panel.setStatus("Falha ao verificar grupo (WOM)"); panel.setAccessMessage("Falha ao verificar grupo (WOM)"); panel.startVerifyCooldown(VERIFY_COOLDOWN_SECONDS); }
4586: 					authenticatedPlayerName = "";
4587: 					isStaff = false;
4588: 					if (currentWomCall == call) currentWomCall = null;
4589: 				}
4590: 
4591: 				@Override public void onResponse(okhttp3.Call call, Response response) throws IOException
4592: 				{
```

## `verifyToken` around line 4565

```java
4547: 			}
4548: 			// Not cached: perform network check. Disable verify button and show status.
4549: 			if (panel != null) { panel.setVerifyEnabled(false); panel.setAccessMessage("Verificando..."); }
4550: 			okhttp3.Call previousCall = currentWomCall;
4551: 			if (previousCall != null)
4552: 			{
4553: 				previousCall.cancel();
4554: 			}
4555: 			HttpUrl womUrl = new HttpUrl.Builder()
4556: 				.scheme("https")
4557: 				.host("api.wiseoldman.net")
4558: 				.addPathSegment("v2")
4559: 				.addPathSegment("players")
4560: 				.addPathSegment(rsn)
4561: 				.addPathSegment("groups")
4562: 				.addQueryParameter("limit", "50")
4563: 				.build();
4564: 			okhttp3.Call call = okHttpClient.newCall(new Request.Builder()
4565: 				.url(womUrl)
4566: 				.header("Accept", "application/json")
4567: 				.header("User-Agent", WOM_USER_AGENT)
4568: 				.get()
4569: 				.build());
4570: 			currentWomCall = call;
4571: 			call.enqueue(new okhttp3.Callback()
4572: 			{
4573: 				@Override public void onFailure(okhttp3.Call call, IOException exception)
4574: 				{
4575: 					if (call.isCanceled())
4576: 					{
4577: 						return;
4578: 					}
4579: 					if (currentWomCall != call)
4580: 					{
4581: 						return;
4582: 					}
4583: 					log.debug("WOM membership check failed", exception);
4584: 					if (panel != null) panel.setAuthenticated(false, false);
4585: 					if (panel != null) { panel.setStatus("Falha ao verificar grupo (WOM)"); panel.setAccessMessage("Falha ao verificar grupo (WOM)"); panel.startVerifyCooldown(VERIFY_COOLDOWN_SECONDS); }
4586: 					authenticatedPlayerName = "";
4587: 					isStaff = false;
4588: 					if (currentWomCall == call) currentWomCall = null;
4589: 				}
4590: 
4591: 				@Override public void onResponse(okhttp3.Call call, Response response) throws IOException
4592: 				{
4593: 					try (Response resp = response)
```

## `postJson` around line 4664

```java
4646: 								authenticatedPlayerName = "";
4647: 								isStaff = false;
4648: 								isDeputyOwner = false;
4649: 								canPublishBroadcast = false;
4650: 								if (panel != null) panel.setAuthenticated(false, false);
4651: 								if (panel != null) { panel.setStatus("Não é membro do clã (WOM)"); panel.setAccessMessage("Membro não identificado. Este plugin é exclusivo para membros do Live On."); }
4652: 							}
4653: 							// start cooldown so user cannot spam immediately
4654: 							if (panel != null) panel.startVerifyCooldown(VERIFY_COOLDOWN_SECONDS);
4655: 							if (currentWomCall == call) currentWomCall = null;
4656: 					}
4657: 				}
4658: 			});
4659: 		});
4660: 	}
4661: 
4662: 	private void postJson(String path, String json, okhttp3.Callback callback)
4663: 	{
4664: 		HttpUrl base = serverBaseUrl();
4665: 		if (base == null)
4666: 		{
4667: 			panel.setStatus("URL inválida");
4668: 			return;
4669: 		}
4670: 		RequestBody body = RequestBody.create(JSON, json);
4671: 		Request request = requestBuilder(base.newBuilder().addPathSegments(path).build())
4672: 			.post(body)
4673: 			.build();
4674: 		okHttpClient.newCall(request).enqueue(callback);
4675: 	}
4676: 
4677: 	private void getJson(String path, okhttp3.Callback callback)
4678: 	{
4679: 		HttpUrl base = serverBaseUrl();
4680: 		if (base == null)
4681: 		{
4682: 			panel.setStatus("URL inválida");
4683: 			return;
4684: 		}
4685: 		okHttpClient.newCall(requestBuilder(base.newBuilder().addPathSegments(path).build()).get().build()).enqueue(callback);
4686: 	}
4687: 
4688: 	private Request.Builder requestBuilder(HttpUrl url)
4689: 	{
4690: 		Request.Builder builder = new Request.Builder().url(url);
4691: 		String playerName = authenticatedPlayerName;
4692: 		if (playerName != null && !playerName.isEmpty())
```

## `getJson` around line 4679

```java
4661: 
4662: 	private void postJson(String path, String json, okhttp3.Callback callback)
4663: 	{
4664: 		HttpUrl base = serverBaseUrl();
4665: 		if (base == null)
4666: 		{
4667: 			panel.setStatus("URL inválida");
4668: 			return;
4669: 		}
4670: 		RequestBody body = RequestBody.create(JSON, json);
4671: 		Request request = requestBuilder(base.newBuilder().addPathSegments(path).build())
4672: 			.post(body)
4673: 			.build();
4674: 		okHttpClient.newCall(request).enqueue(callback);
4675: 	}
4676: 
4677: 	private void getJson(String path, okhttp3.Callback callback)
4678: 	{
4679: 		HttpUrl base = serverBaseUrl();
4680: 		if (base == null)
4681: 		{
4682: 			panel.setStatus("URL inválida");
4683: 			return;
4684: 		}
4685: 		okHttpClient.newCall(requestBuilder(base.newBuilder().addPathSegments(path).build()).get().build()).enqueue(callback);
4686: 	}
4687: 
4688: 	private Request.Builder requestBuilder(HttpUrl url)
4689: 	{
4690: 		Request.Builder builder = new Request.Builder().url(url);
4691: 		String playerName = authenticatedPlayerName;
4692: 		if (playerName != null && !playerName.isEmpty())
4693: 		{
4694: 			builder.header("X-Live-On-Player", playerName);
4695: 			String staffAccessKey = config.staffAccessKey() == null
4696: 				? ""
4697: 				: config.staffAccessKey().trim();
4698: 			if (isStaff && !staffAccessKey.isEmpty())
4699: 			{
4700: 				builder.header("Authorization", "Bearer " + staffAccessKey);
4701: 			}
4702: 			else
4703: 			{
4704: 				builder.header("Authorization", "LiveOnPlayer " + playerName);
4705: 			}
4706: 		}
4707: 		return builder;
```

## `requestBuilder` around line 4688

```java
4670: 		RequestBody body = RequestBody.create(JSON, json);
4671: 		Request request = requestBuilder(base.newBuilder().addPathSegments(path).build())
4672: 			.post(body)
4673: 			.build();
4674: 		okHttpClient.newCall(request).enqueue(callback);
4675: 	}
4676: 
4677: 	private void getJson(String path, okhttp3.Callback callback)
4678: 	{
4679: 		HttpUrl base = serverBaseUrl();
4680: 		if (base == null)
4681: 		{
4682: 			panel.setStatus("URL inválida");
4683: 			return;
4684: 		}
4685: 		okHttpClient.newCall(requestBuilder(base.newBuilder().addPathSegments(path).build()).get().build()).enqueue(callback);
4686: 	}
4687: 
4688: 	private Request.Builder requestBuilder(HttpUrl url)
4689: 	{
4690: 		Request.Builder builder = new Request.Builder().url(url);
4691: 		String playerName = authenticatedPlayerName;
4692: 		if (playerName != null && !playerName.isEmpty())
4693: 		{
4694: 			builder.header("X-Live-On-Player", playerName);
4695: 			String staffAccessKey = config.staffAccessKey() == null
4696: 				? ""
4697: 				: config.staffAccessKey().trim();
4698: 			if (isStaff && !staffAccessKey.isEmpty())
4699: 			{
4700: 				builder.header("Authorization", "Bearer " + staffAccessKey);
4701: 			}
4702: 			else
4703: 			{
4704: 				builder.header("Authorization", "LiveOnPlayer " + playerName);
4705: 			}
4706: 		}
4707: 		return builder;
4708: 	}
4709: 
4710: 	private boolean hasStaffAccessKey()
4711: 	{
4712: 		return config.staffAccessKey() != null && !config.staffAccessKey().trim().isEmpty();
4713: 	}
4714: 
4715: 	private void saveStaffAccessKey(String staffAccessKey)
4716: 	{
```

## `requestBuilder` around line 4690

```java
4672: 			.post(body)
4673: 			.build();
4674: 		okHttpClient.newCall(request).enqueue(callback);
4675: 	}
4676: 
4677: 	private void getJson(String path, okhttp3.Callback callback)
4678: 	{
4679: 		HttpUrl base = serverBaseUrl();
4680: 		if (base == null)
4681: 		{
4682: 			panel.setStatus("URL inválida");
4683: 			return;
4684: 		}
4685: 		okHttpClient.newCall(requestBuilder(base.newBuilder().addPathSegments(path).build()).get().build()).enqueue(callback);
4686: 	}
4687: 
4688: 	private Request.Builder requestBuilder(HttpUrl url)
4689: 	{
4690: 		Request.Builder builder = new Request.Builder().url(url);
4691: 		String playerName = authenticatedPlayerName;
4692: 		if (playerName != null && !playerName.isEmpty())
4693: 		{
4694: 			builder.header("X-Live-On-Player", playerName);
4695: 			String staffAccessKey = config.staffAccessKey() == null
4696: 				? ""
4697: 				: config.staffAccessKey().trim();
4698: 			if (isStaff && !staffAccessKey.isEmpty())
4699: 			{
4700: 				builder.header("Authorization", "Bearer " + staffAccessKey);
4701: 			}
4702: 			else
4703: 			{
4704: 				builder.header("Authorization", "LiveOnPlayer " + playerName);
4705: 			}
4706: 		}
4707: 		return builder;
4708: 	}
4709: 
4710: 	private boolean hasStaffAccessKey()
4711: 	{
4712: 		return config.staffAccessKey() != null && !config.staffAccessKey().trim().isEmpty();
4713: 	}
4714: 
4715: 	private void saveStaffAccessKey(String staffAccessKey)
4716: 	{
4717: 		configManager.setConfiguration(
4718: 			"live-on-clan-messages",
```

## `serverBaseUrl` around line 4770

```java
4752: 		pbRankingRequestGeneration.incrementAndGet();
4753: 		messageSessionInitialized = false;
4754: 		messageSessionGeneration.incrementAndGet();
4755: 		rankRequestsSessionInitialized = false;
4756: 		String storedCursor = configManager.getConfiguration(
4757: 			"live-on-clan-messages",
4758: 			messageCursorConfigKey(accountKey));
4759: 		lastMessageId = messageCursorByAccount.getOrDefault(
4760: 			accountKey,
4761: 			storedCursor == null ? "" : storedCursor);
4762: 		lastClearMarker = "";
4763: 		displayedPendingRankRequests.clear();
4764: 		if (panel != null)
4765: 		{
4766: 			panel.clearMessages();
4767: 		}
4768: 	}
4769: 
4770: 	private HttpUrl serverBaseUrl()
4771: 	{
4772: 		String configuredUrl = config.serverUrl() == null ? "" : config.serverUrl().trim();
4773: 		if (configuredUrl.isEmpty() || configuredUrl.contains("example.invalid"))
4774: 		{
4775: 			configuredUrl = "http://127.0.0.1:8080";
4776: 		}
4777: 		return HttpUrl.parse(configuredUrl);
4778: 	}
4779: 
4780: 	private void rebuildNavigationButton()
4781: 	{
4782: 		if (panel == null) return;
4783: 		if (navigationButton != null) clientToolbar.removeNavigation(navigationButton);
4784: 		navigationButton = NavigationButton.builder()
4785: 			.tooltip("Live on clan")
4786: 			.icon(createIcon())
4787: 			.panel(panel)
4788: 			.priority(config.sidebarIconPriority())
4789: 			.build();
4790: 		clientToolbar.addNavigation(navigationButton);
4791: 	}
4792: 
4793: 	private BufferedImage createIcon()
4794: 	{
4795: 		BufferedImage icon = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
4796: 		Graphics2D graphics = icon.createGraphics();
4797: 		graphics.setColor(new Color(25, 90, 25));
4798: 		graphics.fillOval(3, 3, 26, 26);
```

## `serverBaseUrl` around line 4777

```java
4759: 		lastMessageId = messageCursorByAccount.getOrDefault(
4760: 			accountKey,
4761: 			storedCursor == null ? "" : storedCursor);
4762: 		lastClearMarker = "";
4763: 		displayedPendingRankRequests.clear();
4764: 		if (panel != null)
4765: 		{
4766: 			panel.clearMessages();
4767: 		}
4768: 	}
4769: 
4770: 	private HttpUrl serverBaseUrl()
4771: 	{
4772: 		String configuredUrl = config.serverUrl() == null ? "" : config.serverUrl().trim();
4773: 		if (configuredUrl.isEmpty() || configuredUrl.contains("example.invalid"))
4774: 		{
4775: 			configuredUrl = "http://127.0.0.1:8080";
4776: 		}
4777: 		return HttpUrl.parse(configuredUrl);
4778: 	}
4779: 
4780: 	private void rebuildNavigationButton()
4781: 	{
4782: 		if (panel == null) return;
4783: 		if (navigationButton != null) clientToolbar.removeNavigation(navigationButton);
4784: 		navigationButton = NavigationButton.builder()
4785: 			.tooltip("Live on clan")
4786: 			.icon(createIcon())
4787: 			.panel(panel)
4788: 			.priority(config.sidebarIconPriority())
4789: 			.build();
4790: 		clientToolbar.addNavigation(navigationButton);
4791: 	}
4792: 
4793: 	private BufferedImage createIcon()
4794: 	{
4795: 		BufferedImage icon = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
4796: 		Graphics2D graphics = icon.createGraphics();
4797: 		graphics.setColor(new Color(25, 90, 25));
4798: 		graphics.fillOval(3, 3, 26, 26);
4799: 		graphics.setColor(new Color(80, 220, 80));
4800: 		graphics.fillOval(5, 5, 22, 22);
4801: 		graphics.dispose();
4802: 		return icon;
4803: 	}
4804: 
4805: 
```

## `resolveRankRequest` around line 5000

```java
4982: 		Matcher matcher = RANK_REQUEST_MESSAGE_PATTERN.matcher(message);
4983: 		return matcher.matches() ? rankRequestKey(matcher.group("player"), matcher.group("rank")) : null;
4984: 	}
4985: 
4986: 	private static String rankRequestKey(String playerName, String rankName)
4987: 	{
4988: 		return (playerName == null ? "" : playerName.trim().toLowerCase(java.util.Locale.ROOT))
4989: 			+ '\u0000'
4990: 			+ (rankName == null ? "" : rankName.trim().toLowerCase(java.util.Locale.ROOT));
4991: 	}
4992: 
4993: 	private void resolveRankRequest(RankRequestsPanel.RankRequest request, String decision)
4994: 	{
4995: 		if (request == null || !isStaff)
4996: 		{
4997: 			if (panel != null) panel.setRankRequestsStatus("Acesso staff ausente");
4998: 			return;
4999: 		}
5000: 		HttpUrl base = serverBaseUrl();
5001: 		if (base == null)
5002: 		{
5003: 			if (panel != null) panel.setRankRequestsStatus("URL inválida");
5004: 			return;
5005: 		}
5006: 		java.util.Map<String, Object> payload = new java.util.LinkedHashMap<>();
5007: 		payload.put("decision", decision);
5008: 		payload.put("playerName", authenticatedPlayerName);
5009: 		RequestBody body = RequestBody.create(JSON, gson.toJson(payload));
5010: 		HttpUrl url = base.newBuilder()
5011: 			.addPathSegment("admin")
5012: 			.addPathSegment("rank-requests")
5013: 			.addPathSegment(Integer.toString(request.id))
5014: 			.addPathSegment("decision")
5015: 			.build();
5016: 		okHttpClient.newCall(requestBuilder(url).post(body).build()).enqueue(new okhttp3.Callback()
5017: 		{
5018: 			@Override public void onFailure(okhttp3.Call call, IOException exception)
5019: 			{
5020: 				log.debug("Unable to resolve rank request", exception);
5021: 				if (panel != null) panel.setRankRequestsStatus("Falha ao atualizar solicitação");
5022: 			}
5023: 
5024: 			@Override public void onResponse(okhttp3.Call call, Response response) throws IOException
5025: 			{
5026: 				try (Response ignored = response)
5027: 				{
5028: 					String responseBody = response.body() == null ? "" : response.body().string();
```

## `resolveRankRequest` around line 5010

```java
4992: 
4993: 	private void resolveRankRequest(RankRequestsPanel.RankRequest request, String decision)
4994: 	{
4995: 		if (request == null || !isStaff)
4996: 		{
4997: 			if (panel != null) panel.setRankRequestsStatus("Acesso staff ausente");
4998: 			return;
4999: 		}
5000: 		HttpUrl base = serverBaseUrl();
5001: 		if (base == null)
5002: 		{
5003: 			if (panel != null) panel.setRankRequestsStatus("URL inválida");
5004: 			return;
5005: 		}
5006: 		java.util.Map<String, Object> payload = new java.util.LinkedHashMap<>();
5007: 		payload.put("decision", decision);
5008: 		payload.put("playerName", authenticatedPlayerName);
5009: 		RequestBody body = RequestBody.create(JSON, gson.toJson(payload));
5010: 		HttpUrl url = base.newBuilder()
5011: 			.addPathSegment("admin")
5012: 			.addPathSegment("rank-requests")
5013: 			.addPathSegment(Integer.toString(request.id))
5014: 			.addPathSegment("decision")
5015: 			.build();
5016: 		okHttpClient.newCall(requestBuilder(url).post(body).build()).enqueue(new okhttp3.Callback()
5017: 		{
5018: 			@Override public void onFailure(okhttp3.Call call, IOException exception)
5019: 			{
5020: 				log.debug("Unable to resolve rank request", exception);
5021: 				if (panel != null) panel.setRankRequestsStatus("Falha ao atualizar solicitação");
5022: 			}
5023: 
5024: 			@Override public void onResponse(okhttp3.Call call, Response response) throws IOException
5025: 			{
5026: 				try (Response ignored = response)
5027: 				{
5028: 					String responseBody = response.body() == null ? "" : response.body().string();
5029: 					if (!response.isSuccessful())
5030: 					{
5031: 						log.debug("Rank request decision failed: code={} body={}", response.code(), responseBody);
5032: 						if (panel != null) panel.setRankRequestsStatus("Erro " + response.code());
5033: 						return;
5034: 					}
5035: 					if (panel != null)
5036: 					{
5037: 						panel.setRankRequestsStatus("DECLINED".equals(decision)
5038: 							? "Solicitação recusada"
```

## `resolveRankRequest` around line 5011

```java
4993: 	private void resolveRankRequest(RankRequestsPanel.RankRequest request, String decision)
4994: 	{
4995: 		if (request == null || !isStaff)
4996: 		{
4997: 			if (panel != null) panel.setRankRequestsStatus("Acesso staff ausente");
4998: 			return;
4999: 		}
5000: 		HttpUrl base = serverBaseUrl();
5001: 		if (base == null)
5002: 		{
5003: 			if (panel != null) panel.setRankRequestsStatus("URL inválida");
5004: 			return;
5005: 		}
5006: 		java.util.Map<String, Object> payload = new java.util.LinkedHashMap<>();
5007: 		payload.put("decision", decision);
5008: 		payload.put("playerName", authenticatedPlayerName);
5009: 		RequestBody body = RequestBody.create(JSON, gson.toJson(payload));
5010: 		HttpUrl url = base.newBuilder()
5011: 			.addPathSegment("admin")
5012: 			.addPathSegment("rank-requests")
5013: 			.addPathSegment(Integer.toString(request.id))
5014: 			.addPathSegment("decision")
5015: 			.build();
5016: 		okHttpClient.newCall(requestBuilder(url).post(body).build()).enqueue(new okhttp3.Callback()
5017: 		{
5018: 			@Override public void onFailure(okhttp3.Call call, IOException exception)
5019: 			{
5020: 				log.debug("Unable to resolve rank request", exception);
5021: 				if (panel != null) panel.setRankRequestsStatus("Falha ao atualizar solicitação");
5022: 			}
5023: 
5024: 			@Override public void onResponse(okhttp3.Call call, Response response) throws IOException
5025: 			{
5026: 				try (Response ignored = response)
5027: 				{
5028: 					String responseBody = response.body() == null ? "" : response.body().string();
5029: 					if (!response.isSuccessful())
5030: 					{
5031: 						log.debug("Rank request decision failed: code={} body={}", response.code(), responseBody);
5032: 						if (panel != null) panel.setRankRequestsStatus("Erro " + response.code());
5033: 						return;
5034: 					}
5035: 					if (panel != null)
5036: 					{
5037: 						panel.setRankRequestsStatus("DECLINED".equals(decision)
5038: 							? "Solicitação recusada"
5039: 							: "Solicitação aceita");
```

## `resolveRankRequest` around line 5012

```java
4994: 	{
4995: 		if (request == null || !isStaff)
4996: 		{
4997: 			if (panel != null) panel.setRankRequestsStatus("Acesso staff ausente");
4998: 			return;
4999: 		}
5000: 		HttpUrl base = serverBaseUrl();
5001: 		if (base == null)
5002: 		{
5003: 			if (panel != null) panel.setRankRequestsStatus("URL inválida");
5004: 			return;
5005: 		}
5006: 		java.util.Map<String, Object> payload = new java.util.LinkedHashMap<>();
5007: 		payload.put("decision", decision);
5008: 		payload.put("playerName", authenticatedPlayerName);
5009: 		RequestBody body = RequestBody.create(JSON, gson.toJson(payload));
5010: 		HttpUrl url = base.newBuilder()
5011: 			.addPathSegment("admin")
5012: 			.addPathSegment("rank-requests")
5013: 			.addPathSegment(Integer.toString(request.id))
5014: 			.addPathSegment("decision")
5015: 			.build();
5016: 		okHttpClient.newCall(requestBuilder(url).post(body).build()).enqueue(new okhttp3.Callback()
5017: 		{
5018: 			@Override public void onFailure(okhttp3.Call call, IOException exception)
5019: 			{
5020: 				log.debug("Unable to resolve rank request", exception);
5021: 				if (panel != null) panel.setRankRequestsStatus("Falha ao atualizar solicitação");
5022: 			}
5023: 
5024: 			@Override public void onResponse(okhttp3.Call call, Response response) throws IOException
5025: 			{
5026: 				try (Response ignored = response)
5027: 				{
5028: 					String responseBody = response.body() == null ? "" : response.body().string();
5029: 					if (!response.isSuccessful())
5030: 					{
5031: 						log.debug("Rank request decision failed: code={} body={}", response.code(), responseBody);
5032: 						if (panel != null) panel.setRankRequestsStatus("Erro " + response.code());
5033: 						return;
5034: 					}
5035: 					if (panel != null)
5036: 					{
5037: 						panel.setRankRequestsStatus("DECLINED".equals(decision)
5038: 							? "Solicitação recusada"
5039: 							: "Solicitação aceita");
5040: 					}
```

## `resolveRankRequest` around line 5013

```java
4995: 		if (request == null || !isStaff)
4996: 		{
4997: 			if (panel != null) panel.setRankRequestsStatus("Acesso staff ausente");
4998: 			return;
4999: 		}
5000: 		HttpUrl base = serverBaseUrl();
5001: 		if (base == null)
5002: 		{
5003: 			if (panel != null) panel.setRankRequestsStatus("URL inválida");
5004: 			return;
5005: 		}
5006: 		java.util.Map<String, Object> payload = new java.util.LinkedHashMap<>();
5007: 		payload.put("decision", decision);
5008: 		payload.put("playerName", authenticatedPlayerName);
5009: 		RequestBody body = RequestBody.create(JSON, gson.toJson(payload));
5010: 		HttpUrl url = base.newBuilder()
5011: 			.addPathSegment("admin")
5012: 			.addPathSegment("rank-requests")
5013: 			.addPathSegment(Integer.toString(request.id))
5014: 			.addPathSegment("decision")
5015: 			.build();
5016: 		okHttpClient.newCall(requestBuilder(url).post(body).build()).enqueue(new okhttp3.Callback()
5017: 		{
5018: 			@Override public void onFailure(okhttp3.Call call, IOException exception)
5019: 			{
5020: 				log.debug("Unable to resolve rank request", exception);
5021: 				if (panel != null) panel.setRankRequestsStatus("Falha ao atualizar solicitação");
5022: 			}
5023: 
5024: 			@Override public void onResponse(okhttp3.Call call, Response response) throws IOException
5025: 			{
5026: 				try (Response ignored = response)
5027: 				{
5028: 					String responseBody = response.body() == null ? "" : response.body().string();
5029: 					if (!response.isSuccessful())
5030: 					{
5031: 						log.debug("Rank request decision failed: code={} body={}", response.code(), responseBody);
5032: 						if (panel != null) panel.setRankRequestsStatus("Erro " + response.code());
5033: 						return;
5034: 					}
5035: 					if (panel != null)
5036: 					{
5037: 						panel.setRankRequestsStatus("DECLINED".equals(decision)
5038: 							? "Solicitação recusada"
5039: 							: "Solicitação aceita");
5040: 					}
5041: 					fetchRankRequests();
```

## `resolveRankRequest` around line 5014

```java
4996: 		{
4997: 			if (panel != null) panel.setRankRequestsStatus("Acesso staff ausente");
4998: 			return;
4999: 		}
5000: 		HttpUrl base = serverBaseUrl();
5001: 		if (base == null)
5002: 		{
5003: 			if (panel != null) panel.setRankRequestsStatus("URL inválida");
5004: 			return;
5005: 		}
5006: 		java.util.Map<String, Object> payload = new java.util.LinkedHashMap<>();
5007: 		payload.put("decision", decision);
5008: 		payload.put("playerName", authenticatedPlayerName);
5009: 		RequestBody body = RequestBody.create(JSON, gson.toJson(payload));
5010: 		HttpUrl url = base.newBuilder()
5011: 			.addPathSegment("admin")
5012: 			.addPathSegment("rank-requests")
5013: 			.addPathSegment(Integer.toString(request.id))
5014: 			.addPathSegment("decision")
5015: 			.build();
5016: 		okHttpClient.newCall(requestBuilder(url).post(body).build()).enqueue(new okhttp3.Callback()
5017: 		{
5018: 			@Override public void onFailure(okhttp3.Call call, IOException exception)
5019: 			{
5020: 				log.debug("Unable to resolve rank request", exception);
5021: 				if (panel != null) panel.setRankRequestsStatus("Falha ao atualizar solicitação");
5022: 			}
5023: 
5024: 			@Override public void onResponse(okhttp3.Call call, Response response) throws IOException
5025: 			{
5026: 				try (Response ignored = response)
5027: 				{
5028: 					String responseBody = response.body() == null ? "" : response.body().string();
5029: 					if (!response.isSuccessful())
5030: 					{
5031: 						log.debug("Rank request decision failed: code={} body={}", response.code(), responseBody);
5032: 						if (panel != null) panel.setRankRequestsStatus("Erro " + response.code());
5033: 						return;
5034: 					}
5035: 					if (panel != null)
5036: 					{
5037: 						panel.setRankRequestsStatus("DECLINED".equals(decision)
5038: 							? "Solicitação recusada"
5039: 							: "Solicitação aceita");
5040: 					}
5041: 					fetchRankRequests();
5042: 				}
```

## `deleteRankRequest` around line 5054

```java
5036: 					{
5037: 						panel.setRankRequestsStatus("DECLINED".equals(decision)
5038: 							? "Solicitação recusada"
5039: 							: "Solicitação aceita");
5040: 					}
5041: 					fetchRankRequests();
5042: 				}
5043: 			}
5044: 		});
5045: 	}
5046: 
5047: 	private void deleteRankRequest(int id)
5048: 	{
5049: 		if (!isStaff)
5050: 		{
5051: 			log.debug("Not staff, skipping rank request delete");
5052: 			return;
5053: 		}
5054: 		HttpUrl base = serverBaseUrl();
5055: 		if (base == null) return;
5056: 		okHttpClient.newCall(requestBuilder(base.newBuilder().addPathSegments("admin/rank-requests/" + id).build()).delete().build()).enqueue(new okhttp3.Callback()
5057: 		{
5058: 			@Override public void onFailure(okhttp3.Call call, IOException exception)
5059: 			{
5060: 				log.debug("Unable to delete rank request", exception);
5061: 			}
5062: 
5063: 			@Override public void onResponse(okhttp3.Call call, Response response) throws IOException
5064: 			{
5065: 				try (Response ignored = response)
5066: 				{
5067: 					if (response.isSuccessful())
5068: 					{
5069: 						fetchRankRequests();
5070: 					}
5071: 					else log.debug("Failed to delete rank request: " + response.code());
5072: 				}
5073: 			}
5074: 		});
5075: 	}
5076: 
5077: 	private void confirmRankRequest(RankRequestsPanel.RankRequest request)
5078: 	{
5079: 		if (request == null || request.playerName == null || request.rankName == null)
5080: 		{
5081: 			if (panel != null) panel.setRankRequestsStatus("Solicita\u00E7\u00E3o inv\u00E1lida");
5082: 			return;
```

## `confirmRankRequest` around line 5089

```java
5071: 					else log.debug("Failed to delete rank request: " + response.code());
5072: 				}
5073: 			}
5074: 		});
5075: 	}
5076: 
5077: 	private void confirmRankRequest(RankRequestsPanel.RankRequest request)
5078: 	{
5079: 		if (request == null || request.playerName == null || request.rankName == null)
5080: 		{
5081: 			if (panel != null) panel.setRankRequestsStatus("Solicita\u00E7\u00E3o inv\u00E1lida");
5082: 			return;
5083: 		}
5084: 		if (!isStaff)
5085: 		{
5086: 			if (panel != null) panel.setRankRequestsStatus("Acesso staff ausente");
5087: 			return;
5088: 		}
5089: 		HttpUrl base = serverBaseUrl();
5090: 		if (base == null)
5091: 		{
5092: 			if (panel != null) panel.setRankRequestsStatus("URL inválida");
5093: 			return;
5094: 		}
5095: 		String message = request.playerName + " foi promovido para " + request.rankName + "!";
5096: 		String rsnName = authenticatedPlayerName;
5097: 		if (rsnName == null || rsnName.isEmpty())
5098: 		{
5099: 			if (panel != null) panel.setRankRequestsStatus("Valide sua conta pelo WOM");
5100: 			return;
5101: 		}
5102: 		java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
5103: 		map.put("message", message);
5104: 		map.put("mode", "CLAN");
5105: 		map.put("playerName", rsnName);
5106: 		RequestBody body = RequestBody.create(JSON, gson.toJson(map));
5107: 		Request publishRequest = requestBuilder(base.newBuilder().addPathSegment("messages").build()).post(body).build();
5108: 		okHttpClient.newCall(publishRequest).enqueue(new okhttp3.Callback()
5109: 		{
5110: 			@Override public void onFailure(okhttp3.Call call, IOException exception)
5111: 			{
5112: 				log.debug("Unable to publish promotion message", exception);
5113: 				if (panel != null) panel.setRankRequestsStatus("Falha ao publicar promoção");
5114: 			}
5115: 
5116: 			@Override public void onResponse(okhttp3.Call call, Response response) throws IOException
5117: 			{
```

## `confirmRankRequest` around line 5107

```java
5089: 		HttpUrl base = serverBaseUrl();
5090: 		if (base == null)
5091: 		{
5092: 			if (panel != null) panel.setRankRequestsStatus("URL inválida");
5093: 			return;
5094: 		}
5095: 		String message = request.playerName + " foi promovido para " + request.rankName + "!";
5096: 		String rsnName = authenticatedPlayerName;
5097: 		if (rsnName == null || rsnName.isEmpty())
5098: 		{
5099: 			if (panel != null) panel.setRankRequestsStatus("Valide sua conta pelo WOM");
5100: 			return;
5101: 		}
5102: 		java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
5103: 		map.put("message", message);
5104: 		map.put("mode", "CLAN");
5105: 		map.put("playerName", rsnName);
5106: 		RequestBody body = RequestBody.create(JSON, gson.toJson(map));
5107: 		Request publishRequest = requestBuilder(base.newBuilder().addPathSegment("messages").build()).post(body).build();
5108: 		okHttpClient.newCall(publishRequest).enqueue(new okhttp3.Callback()
5109: 		{
5110: 			@Override public void onFailure(okhttp3.Call call, IOException exception)
5111: 			{
5112: 				log.debug("Unable to publish promotion message", exception);
5113: 				if (panel != null) panel.setRankRequestsStatus("Falha ao publicar promoção");
5114: 			}
5115: 
5116: 			@Override public void onResponse(okhttp3.Call call, Response response) throws IOException
5117: 			{
5118: 				try (Response ignored = response)
5119: 				{
5120: 					String responseBody = response.body() == null ? "" : response.body().string();
5121: 					if (!response.isSuccessful())
5122: 					{
5123: 						if (panel != null) panel.setRankRequestsStatus("Erro " + response.code());
5124: 						log.debug("Unable to publish promotion message: code={} body={}", response.code(), responseBody);
5125: 						return;
5126: 					}
5127: 					if (panel != null) panel.setRankRequestsStatus("Promo\u00E7\u00E3o publicada no clan channel");
5128: 					displayPublishedMessage(responseBody, "Staff", message, "CLAN", false);
5129: 					resolveRankRequest(request, "ACCEPTED");
5130: 					scheduleMessageRefresh();
5131: 					fetchSentMessages();
5132: 				}
5133: 			}
5134: 		});
5135: 	}
```
