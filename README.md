# NightLegion RuneLite plugin

This candidate uses the published Reval client source at
`179faa521b14e4541151effd0f5d28f12ec89597`, with NightLegion branding, endpoints,
and Personal Link Token authentication. The original collection managers,
notifiers, session handling, sync triggers, retry logic and panel loading are
preserved. The previous custom NightLegion sync coordinator is not used.

## Connect

1. Use `/runelite_link` in the NightLegion Discord.
2. Paste the private token into RuneLite > NightLegion > Personal Link Token.
3. Log into a character in the NightLegion clan.

Keep the token private. Existing valid tokens continue to identify the same
Discord member; changing the token re-enters the original login flow.
The user's own server handles the original client HTTP contract. No Reval
production account, token, private database or member data is bundled or used.

Website: https://nightlegion-web.vercel.app/

## Preserved client behaviour

Collection Log syncing uses the original in-game Sync button. Login/logout,
heartbeat/session acknowledgements, event notifications, lazy panel loading and
manual refresh behave according to the pinned original client. This does not
add a custom every-minute full-account scanner or automatically open game UI.
Evidence unavailable to the original client cannot be inferred: bank-only items
may require moving the item to inventory/equipment or available collection-log
history. A first-time historical ownership import does not establish the number
of previous duplicate drops.

The matching NightLegion backend removes exactly `CLAN_ACTIVITY`,
`DISCORD_ACTIVITY`, `CLAN_EVENTS` and `WOM_ACTIVITY` as point systems. Normal
Events and registrations remain. Pets, milestones, collection-log, combat
achievements, capes, Misc and untradeable-drop sources remain.

## Reproducible verification

`python3 tools/reval_source.py` checks every runtime file against the pinned
upstream archive after the narrowly defined identity/authentication changes.
It rejects missing files, unexpected runtime code and unapproved changes.
Of 157 upstream Java files, 135 are byte-identical, 17 have only literal
branding/destination changes, and five have token-authentication wiring. One
small authentication class is added. There is no replacement data collector.

`gradle --no-daemon clean test jar` runs the upstream tests plus the token
boundary tests. The paired backend includes an original-Java-client to real
local HTTP/SQLite integration test, with external game/Discord inputs replaced
by test fixtures. Those tests are not a live in-game acceptance test.

## Release status

Candidate version: `2.20.1-nightlegion.1`. Native `/plugin/...`,
`/reval-webhook` and `/event-filters` routes on the paired NightLegion server are
required BEFORE this client is released. The separate historical-activity-data
migration requires a verified private backup and explicit application; code
submission does not run it.

The LICENSE and LICENSES directory retain the upstream BSD and third-party
notices. Older LIVEON/TRANSLATION/migration documents describe historical
candidates, not the current runtime. See PROFILE_FIXES.md for current scope.
