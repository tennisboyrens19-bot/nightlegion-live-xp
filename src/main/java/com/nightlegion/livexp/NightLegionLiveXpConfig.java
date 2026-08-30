package com.nightlegion.livexp;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

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

    @ConfigSection(
        name = "Rank Tracking",
        description = "Separate NightLegion rank-system connection and telemetry",
        position = 10
    )
    String rankTrackingSection = "rankTrackingSection";

    @ConfigItem(
        keyName = "rankTrackingEnabled",
        name = "Enable rank tracking",
        description = "Send optional desktop telemetry to the NightLegion ranking system. Mobile players are tracked through Discord/clan activity and Wise Old Man instead.",
        section = rankTrackingSection,
        position = 0
    )
    default boolean rankTrackingEnabled()
    {
        return true;
    }

    @ConfigItem(
        keyName = "rankToken",
        name = "Rank Secret Key",
        description = "Paste the separate secret key created in Discord with /rank link",
        warning = "Rank tracking sends your RuneScape display name, total XP, total level, quest points, tracked session time, your own clan-chat activity counts, and detected boss-kill activity to NightLegion. RuneLite-only telemetry does not give desktop players extra rank points. Message contents, passwords, Jagex credentials, bank contents, inventory contents and private chat are not sent.",
        secret = true,
        section = rankTrackingSection,
        position = 1
    )
    default String rankToken()
    {
        return "";
    }

    @ConfigSection(
        name = "Community Hub",
        description = "Optional community features such as PB and valuable-drop reporting",
        position = 20
    )
    String communitySection = "communitySection";

    @ConfigItem(
        keyName = "communityPbTracking",
        name = "Submit new PB messages",
        description = "When RuneLite sees a game message containing a new personal best time, submit the time and your display name to the NightLegion clan PB board.",
        warning = "This sends the detected personal-best game message, parsed time, and your RuneScape display name to the NightLegion third-party service. It does not send private chat.",
        section = communitySection,
        position = 0
    )
    default boolean communityPbTracking()
    {
        return false;
    }

    @ConfigItem(
        keyName = "communityDropTracking",
        name = "Report valuable NPC drops",
        description = "Submit qualifying NPC drops to the NightLegion recent-drop feed and optional Discord drop channel.",
        warning = "This sends the item name, quantity, approximate GE value, NPC source, and your RuneScape display name to the NightLegion third-party service. Inventory and bank contents are not scanned or transmitted.",
        section = communitySection,
        position = 1
    )
    default boolean communityDropTracking()
    {
        return false;
    }

    @Range(min = 0, max = 2000000000)
    @ConfigItem(
        keyName = "communityDropThreshold",
        name = "Minimum drop value",
        description = "Minimum approximate GE value in coins before an NPC drop is submitted",
        section = communitySection,
        position = 2
    )
    default int communityDropThreshold()
    {
        return 5_000_000;
    }
}
