package com.nightlegion.livexp;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("nightlegionlivexp")
public interface NightLegionLiveXpConfig extends Config
{
	@ConfigItem(
		keyName = "token",
		name = "Personal Link Token",
		description = "Paste the private token from Discord /sotw_runelite_link",
		warning = "This sends your RuneScape display name, changed skill name, current XP for that skill, and NightLegion Personal Link Token to the NightLegion SOTW server. It does not send chat, bank, inventory, password, or Jagex account credentials.",
		secret = true
	)
	default String token()
	{
		return "";
	}
}
