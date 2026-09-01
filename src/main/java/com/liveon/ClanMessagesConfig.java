package com.liveon;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("live-on-clan-messages")
public interface ClanMessagesConfig extends Config
{
    String THIRD_PARTY_WARNING =
        "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers";

    @ConfigItem(
        keyName = "personalLinkToken",
        name = "Personal Link Token",
        description = "Your NightLegion Discord/RuneLite link token",
        warning = THIRD_PARTY_WARNING,
        secret = true,
        position = 0
    )
    default String personalLinkToken() { return ""; }

    @ConfigItem(
        keyName = "enabled",
        name = "Connect to clan server",
        description = "Enable NightLegion online clan features",
        warning = THIRD_PARTY_WARNING,
        position = 1
    )
    default boolean enabled() { return false; }

    @ConfigItem(
        keyName = "liveStatusEnabled",
        name = "Show live streams",
        description = "Show NightLegion members who are live on Twitch",
        warning = THIRD_PARTY_WARNING,
        position = 2
    )
    default boolean liveStatusEnabled() { return false; }

    @ConfigItem(
        keyName = "statsEnabled",
        name = "Participate in monthly MVP",
        description = "Submit eligible 1M+ drops to the monthly NightLegion MVP ranking",
        warning = THIRD_PARTY_WARNING,
        position = 3
    )
    default boolean statsEnabled() { return false; }

    @ConfigItem(
        keyName = "pbRankingEnabled",
        name = "Participate in PB rankings",
        description = "Submit detected PB times to the private NightLegion leaderboard",
        warning = THIRD_PARTY_WARNING,
        position = 4
    )
    default boolean pbRankingEnabled() { return false; }

    @ConfigItem(
        keyName = "discordDropsEnabled",
        name = "Send drops to Discord",
        description = "Post eligible rare drops to the NightLegion Hall of Fame",
        warning = THIRD_PARTY_WARNING,
        position = 5
    )
    default boolean discordDropsEnabled() { return false; }

    @Range(min = -20, max = 20)
    @ConfigItem(
        keyName = "sidebarIconPriority",
        name = "Sidebar position",
        description = "Move the NightLegion icon on the RuneLite sidebar",
        position = 6
    )
    default int sidebarIconPriority() { return 0; }

    @ConfigItem(
        keyName = "discordDropMinimumValue",
        name = "Minimum drop value",
        description = "Minimum GE value for a Discord drop notification",
        hidden = true
    )
    default int discordDropMinimumValue() { return 3_000_000; }

    @ConfigItem(
        keyName = "serverUrl",
        name = "Server",
        description = "NightLegion API address",
        hidden = true
    )
    default String serverUrl() { return "https://nightlegion-livexp.onrender.com/"; }

    @ConfigItem(
        keyName = "pollIntervalSeconds",
        name = "Refresh interval",
        description = "How often to check for new clan messages",
        hidden = true
    )
    default int pollIntervalSeconds() { return 30; }
}
