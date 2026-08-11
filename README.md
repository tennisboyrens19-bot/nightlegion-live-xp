# NightLegion

RuneLite companion plugin for NightLegion Skill of the Week (SOTW).

The plugin sends live XP changes to the NightLegion SOTW service so the Discord leaderboard can update while the player remains logged in. The existing Jagex Hiscores / Wise Old Man sync remains the fallback for players who do not use RuneLite, including mobile players.

## Setup

1. Install **NightLegion** from RuneLite Plugin Hub.
2. In the NightLegion Discord, run `/sotw_runelite_link`.
3. Copy the newly generated **Personal Link Token**.
4. Open the **NightLegion** RuneLite settings and paste the token.
5. Play normally.

The server endpoint is built into the plugin; clan members do not need to configure a URL.

## Data sent

When a Personal Link Token is configured, the plugin may send:

- RuneScape display name
- the name of a skill whose XP changed
- current XP for that skill
- the NightLegion Personal Link Token used to associate the update with the correct Discord account

It does **not** send chat messages, bank contents, inventory contents, RuneScape/Jagex passwords, or Jagex account credentials.

## Scoring and verification

Live RuneLite data is used to make the in-progress Discord display more responsive. It is not treated as the sole authority for prize results. NightLegion can reconcile or verify final SOTW results against Jagex Hiscores / Wise Old Man.

## Token safety

Keep the Personal Link Token private. Running `/sotw_runelite_link` again rotates it and invalidates the previous token.

## Development

Requires Java 11. The project follows the standard RuneLite Plugin Hub layout and has no additional runtime dependencies beyond RuneLite/JDK APIs.
