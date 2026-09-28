# NightLegion RuneLite plugin

This candidate uses the published Reval client source at
`179faa521b14e4541151effd0f5d28f12ec89597`, with NightLegion branding, endpoints,
and Personal Link Token authentication. The pinned implementation remains the
source baseline, with reviewed ownership, pet-ID, diary, receipt identity,
acknowledgement, account/session boundary and point precision
corrections documented in SOURCE_PARITY.md. The previous custom NightLegion
sync coordinator is not used.

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

Collection Log syncing uses the original in-game Sync button. It reports success
after a matching server acknowledgement, distinguishes incomplete scoring and
failures, and drops callbacks belonging to a previous account. Login/logout,
heartbeat/session acknowledgements, event notifications, lazy panel loading and
manual refresh retain the pinned client's structure. This does not
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

`python3 tools/reval_source.py` checks the complete upstream runtime file set
against a checksum-pinned archive after exact identity/authentication and
documented bug-fix transformations. It rejects missing files, unexpected runtime
code and unapproved changes, including additional edits inside adapted files.
Windows CRLF is normalized to LF in Java/license text; other whitespace and code
remain significant. The generated counts and adaptation list are written to
`build/source-parity.json`. There is no replacement data collector.

`python3 tools/test_reval_source.py` runs the same CLI implementation on disposable
runtime-tree copies. Its negative tests prove that unrelated code, altered
ownership guards, missing source and extra source still fail the check. They
also cover receipt identity, native sync acknowledgements, session retry,
account guards and decimal accounting. Both
commands accept `--archive path/to/reval-reference.tar.gz`; the pinned SHA-256 is
always verified. CI runs both commands as required steps.

`gradle --no-daemon clean test jar` runs the upstream tests plus authentication,
ownership, sync, points-display and account-boundary regressions. The paired
backend includes an original-Java-client to real
local HTTP/SQLite integration test, with external game/Discord inputs replaced
by test fixtures. Those tests are not a live in-game acceptance test.

## Release status

Candidate version: `2.20.1-nightlegion.2`. Native `/plugin/...`,
`/reval-webhook` and `/event-filters` routes on the paired NightLegion server are
required BEFORE this client is released. The separate historical-activity-data
migration requires a verified private backup and explicit application; code
submission does not run it.

The LICENSE and LICENSES directory retain the upstream BSD and third-party
notices. Older LIVEON/TRANSLATION/migration documents describe historical
candidates, not the current runtime. See PROFILE_FIXES.md for current scope.
