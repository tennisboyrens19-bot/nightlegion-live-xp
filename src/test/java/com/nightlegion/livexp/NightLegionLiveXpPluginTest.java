package com.nightlegion.livexp;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class NightLegionLiveXpPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(NightLegionLiveXpPlugin.class);
		RuneLite.main(args);
	}
}
