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
        secret = true,
        position = 0
    )
    default String token()
    {
        return "";
    }

    @ConfigItem(
        keyName = "eventAlerts",
        name = "BOTW / SOTW chat alerts",
        description = "Show NightLegion chatbox messages for active BOTW/SOTW on login and when a new event starts",
        position = 1
    )
    default boolean eventAlerts()
    {
        return true;
    }

    @ConfigItem(
        keyName = "giveawayAlerts",
        name = "Giveaway chat alerts",
        description = "Show NightLegion chatbox messages for active giveaways on login and when a new giveaway appears",
        position = 2
    )
    default boolean giveawayAlerts()
    {
        return true;
    }

    @ConfigItem(
        keyName = "groupFinderAlerts",
        name = "Group Finder chat alerts",
        description = "Show an in-game NightLegion chatbox message for new Group Finder listings when your linked Discord account has the Group Finder rank",
        position = 3
    )
    default boolean groupFinderAlerts()
    {
        return true;
    }
}
