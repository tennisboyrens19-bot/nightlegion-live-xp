package com.nightlegion.livexp;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("nightlegionlivexp")
public interface NightLegionLiveXpConfig extends Config
{
    String THIRD_PARTY_WARNING =
        "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers";

    // Visible settings intentionally mirror the approved Live On Clan plugin.
    @ConfigItem(
        keyName = "enabled",
        name = "Connect to clan",
        description = "Enable NightLegion clan features and automatic member verification",
        warning = THIRD_PARTY_WARNING,
        position = 0
    )
    default boolean enabled()
    {
        return true;
    }

    @ConfigItem(
        keyName = "liveStatusEnabled",
        name = "Show live members",
        description = "Show configured NightLegion members who are live",
        warning = THIRD_PARTY_WARNING,
        position = 1
    )
    default boolean liveStatusEnabled()
    {
        return true;
    }

    @ConfigItem(
        keyName = "statsEnabled",
        name = "Participate in monthly MVP",
        description = "Enable NightLegion activity features used by the monthly MVP views",
        warning = THIRD_PARTY_WARNING,
        position = 2
    )
    default boolean statsEnabled()
    {
        return true;
    }

    @ConfigItem(
        keyName = "discordDropsEnabled",
        name = "Send drops to Discord",
        description = "Report qualifying drops to the NightLegion Discord drop feed",
        warning = THIRD_PARTY_WARNING,
        position = 3
    )
    default boolean discordDropsEnabled()
    {
        return false;
    }

    @Range(min = -20, max = 20)
    @ConfigItem(
        keyName = "sidebarIconPriority",
        name = "Sidebar icon position",
        description = "Use the arrows to move the NightLegion icon on the RuneLite sidebar",
        position = 4
    )
    default int sidebarIconPriority()
    {
        return 0;
    }

    // Existing NightLegion credentials/features are preserved but hidden from
    // the normal settings screen so the visible configuration matches Live On.
    @ConfigItem(
        keyName = "token",
        name = "Personal Link Token",
        description = "NightLegion Discord/RuneLite link token",
        warning = THIRD_PARTY_WARNING,
        secret = true,
        hidden = true
    )
    default String token()
    {
        return "";
    }

    @ConfigItem(
        keyName = "eventAlerts",
        name = "BOTW / SOTW chat alerts",
        description = "Show NightLegion in-game event notifications",
        hidden = true
    )
    default boolean eventAlerts()
    {
        return true;
    }

    @ConfigItem(
        keyName = "giveawayAlerts",
        name = "Giveaway chat alerts",
        description = "Show NightLegion in-game giveaway notifications",
        hidden = true
    )
    default boolean giveawayAlerts()
    {
        return true;
    }

    @ConfigItem(
        keyName = "groupFinderAlerts",
        name = "Group Finder chat alerts",
        description = "Show NightLegion in-game Group Finder notifications",
        hidden = true
    )
    default boolean groupFinderAlerts()
    {
        return true;
    }

    @ConfigItem(
        keyName = "rankTrackingEnabled",
        name = "Enable rank tracking",
        description = "Send optional NightLegion rank telemetry",
        hidden = true
    )
    default boolean rankTrackingEnabled()
    {
        return true;
    }

    @ConfigItem(
        keyName = "rankToken",
        name = "Rank Secret Key",
        description = "Private NightLegion rank-system key",
        warning = THIRD_PARTY_WARNING,
        secret = true,
        hidden = true
    )
    default String rankToken()
    {
        return "";
    }

    @ConfigItem(
        keyName = "communityPbTracking",
        name = "Submit PBs",
        description = "Submit detected NightLegion personal bests",
        hidden = true
    )
    default boolean communityPbTracking()
    {
        return true;
    }

    @ConfigItem(
        keyName = "communityDropTracking",
        name = "Report valuable drops",
        description = "Report valuable NPC drops",
        hidden = true
    )
    default boolean communityDropTracking()
    {
        return discordDropsEnabled();
    }

    @Range(min = 0, max = 2000000000)
    @ConfigItem(
        keyName = "communityDropThreshold",
        name = "Minimum drop value",
        description = "Minimum GE value before a drop is submitted",
        hidden = true
    )
    default int communityDropThreshold()
    {
        return 5_000_000;
    }
}
