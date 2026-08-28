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
        description = "Paste the private token from Discord /runelite_link (the old /sotw_runelite_link command also works)",
        warning = "NightLegion sends your RuneScape display name, SOTW XP updates, event join actions, Giveaway entries and Group Finder listing/request data to the NightLegion service. It does not send passwords, Jagex credentials, bank contents, inventory contents or private chat.",
        secret = true
    )
    default String token()
    {
        return "";
    }
}
