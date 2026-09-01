package com.liveon;

import java.lang.reflect.Proxy;
import net.runelite.api.Client;
import net.runelite.api.widgets.Widget;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ClanLiveBadgeDecoratorTest
{
	@Test
	public void rendersMvpAndLiveTogetherAndClearsBoth()
	{
		String[] text = {"Plant Lover"};
		Widget widget = (Widget) Proxy.newProxyInstance(
			Widget.class.getClassLoader(),
			new Class<?>[]{Widget.class},
			(proxy, method, args) ->
			{
				switch (method.getName())
				{
					case "getText": return text[0];
					case "setText": text[0] = (String) args[0]; return proxy;
					case "isHidden": return false;
					case "getChildren":
					case "getDynamicChildren":
					case "getStaticChildren":
					case "getNestedChildren": return null;
					default: return defaultValue(method.getReturnType());
				}
			});
		Client client = (Client) Proxy.newProxyInstance(
			Client.class.getClassLoader(),
			new Class<?>[]{Client.class},
			(proxy, method, args) -> "getWidget".equals(method.getName())
				? widget : defaultValue(method.getReturnType()));
		ClanLiveBadgeDecorator decorator = new ClanLiveBadgeDecorator(client, new BadgePlugin());

		decorator.refresh();
		assertEquals("Plant Lover <col=ffc628>MVP</col> <col=96ffaa>LIVE</col>", text[0]);

		decorator.clearDecorations();
		assertEquals("Plant Lover", text[0]);
	}

	private static Object defaultValue(Class<?> type)
	{
		if (!type.isPrimitive()) return null;
		if (type == boolean.class) return false;
		if (type == byte.class) return (byte) 0;
		if (type == short.class) return (short) 0;
		if (type == int.class) return 0;
		if (type == long.class) return 0L;
		if (type == float.class) return 0F;
		if (type == double.class) return 0D;
		if (type == char.class) return '\0';
		return null;
	}

	private static final class BadgePlugin extends ClanMessagesPlugin
	{
		@Override boolean isLiveStatusVisible() { return true; }
		@Override boolean isPlayerLive(String playerName) { return true; }
		@Override boolean isPlayerMvp(String playerName) { return true; }
		@Override String clanTagBadges(String playerName) { return ""; }
		@Override String removeKnownClanTagMarkup(String text) { return text; }
		@Override String decoratedPlayerNameIn(String text)
		{
			return "Plant Lover".equals(text) ? text : null;
		}
	}
}
