package com.liveon;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.IntConsumer;
import javax.swing.JLabel;
import net.runelite.api.Client;
import net.runelite.api.EnumComposition;
import net.runelite.api.EnumID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.SpriteManager;

/**
 * Direct adaptation of Reval's CLAN_RANK_NAME -> CLAN_RANK_GRAPHIC resolver.
 * NightLegion's approved Precept spelling intentionally maps to Prefect's icon.
 */
final class ClanRankIconResolver
{
	private static final Map<String, Integer> FALLBACK = new LinkedHashMap<>();
	static
	{
		FALLBACK.put("mentor", 3137); FALLBACK.put("precept", 3138);
		FALLBACK.put("prefect", 3138); FALLBACK.put("leader", 3139);
		FALLBACK.put("supervisor", 3140); FALLBACK.put("superior", 3141);
		FALLBACK.put("executive", 3142); FALLBACK.put("senator", 3143);
		FALLBACK.put("monarch", 3144); FALLBACK.put("red_topaz", 3129);
		FALLBACK.put("sapphire", 3130); FALLBACK.put("emerald", 3131);
		FALLBACK.put("ruby", 3132); FALLBACK.put("diamond", 3133);
		FALLBACK.put("dragonstone", 3134); FALLBACK.put("onyx", 3135);
		FALLBACK.put("zenyte", 3136); FALLBACK.put("marshal", 3111);
	}

	private final Client client;
	private final ClientThread clientThread;
	private final SpriteManager spriteManager;
	private volatile Map<String, Integer> titleToSprite;

	ClanRankIconResolver(Client client, ClientThread clientThread, SpriteManager spriteManager)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.spriteManager = spriteManager;
	}

	void apply(String rankName, JLabel target, int size)
	{
		resolve(rankName, spriteId -> SpriteIcons.apply(spriteManager, spriteId, target, size));
	}

	private void resolve(String rankName, IntConsumer callback)
	{
		if (rankName == null) return;
		String key = normalize(rankName);
		Map<String, Integer> cached = titleToSprite;
		if (cached != null)
		{
			deliver(cached, key, callback);
			return;
		}
		clientThread.invoke(() ->
		{
			Map<String, Integer> map = titleToSprite;
			if (map == null)
			{
				map = buildMap();
				if (!map.isEmpty()) titleToSprite = map;
			}
			deliver(map, key, callback);
		});
	}

	private void deliver(Map<String, Integer> map, String key, IntConsumer callback)
	{
		Integer sprite = map.get(key.replace('_', ' '));
		if (sprite == null) sprite = FALLBACK.get(key);
		if (sprite != null) callback.accept(sprite);
	}

	private Map<String, Integer> buildMap()
	{
		Map<String, Integer> map = new HashMap<>();
		try
		{
			EnumComposition names = client.getEnum(EnumID.CLAN_RANK_NAME);
			EnumComposition graphics = client.getEnum(EnumID.CLAN_RANK_GRAPHIC);
			for (int key : names.getKeys())
			{
				String title = names.getStringValue(key);
				int sprite = graphics.getIntValue(key);
				if (title != null && !title.isEmpty() && sprite > 0)
					map.put(title.toLowerCase(Locale.ROOT), sprite);
			}
		}
		catch (Exception ignored) { }
		return map;
	}

	private static String normalize(String value)
	{
		return value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
	}
}
