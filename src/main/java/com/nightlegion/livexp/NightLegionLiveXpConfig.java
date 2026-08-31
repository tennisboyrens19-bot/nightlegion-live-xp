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

    // Visible settings mirror Live On Clan, translated to English.
    @ConfigItem(
        keyName = "enabled",
        name = "Connect to clan server",
        description = "Enable NightLegion online clan features",
        warning = THIRD_PARTY_WARNING,
        position = 0
    )
    default boolean enabled()
    {
        return true;
    }

    @ConfigItem(
        keyName = "liveStatusEnabled",
        name = "Show live streams",
        description = "Check linked Twitch channels and show clan members who are live",
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
        description = "Submit eligible 1M+ drops to the monthly clan MVP ranking",
        warning = THIRD_PARTY_WARNING,
        position = 2
    )
    default boolean statsEnabled()
    {
        return true;
    }

    @ConfigItem(
        keyName = "pbRankingEnabled",
        name = "Participate in PB rankings",
        description = "Submit detected PB times to the private NightLegion leaderboard",
        warning = THIRD_PARTY_WARNING,
        position = 3
    )
    default boolean pbRankingEnabled()
    {
        return true;
    }

    @ConfigItem(
        keyName = "discordDropsEnabled",
        name = "Send drops to Discord",
        description = "Post your eligible rare drops to the NightLegion Discord",
        warning = THIRD_PARTY_WARNING,
        position = 4
    )
    default boolean discordDropsEnabled()
    {
        return false;
    }

    @Range(min = -20, max = 20)
    @ConfigItem(
        keyName = "sidebarIconPriority",
        name = "Sidebar position",
        description = "Use the arrows to move the NightLegion icon on the RuneLite sidebar",
        position = 5
    )
    default int sidebarIconPriority()
    {
        return 0;
    }

    // Existing NightLegion credentials/features remain compatible but are not
    // exposed in the normal Live On-style settings screen.
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
        description = "Track in-game clan-chat activity for NightLegion contribution ranks",
        hidden = true
    )
    default boolean rankTrackingEnabled()
    {
        return true;
    }

    // Kept only so an existing install does not lose its stored value. New
    // builds use the normal Personal Link Token for rank telemetry as well.
    @ConfigItem(
        keyName = "rankToken",
        name = "Legacy Rank Secret Key",
        description = "Legacy NightLegion rank-system key",
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
        return pbRankingEnabled();
    }

    @ConfigItem(
        keyName = "communityDropTracking",
        name = "Track valuable drops",
        description = "Detect valuable NPC drops for MVP and/or Discord",
        hidden = true
    )
    default boolean communityDropTracking()
    {
        return statsEnabled() || discordDropsEnabled();
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
        return 1_000_000;
    }
}
