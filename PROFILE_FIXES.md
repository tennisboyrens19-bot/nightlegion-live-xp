# Current candidate: published Reval client + NightLegion token/data

This document supersedes the earlier adapted-sync candidate. It is not a claim
that any version has already been installed or deployed.

## Client source

Source: `revalOSRS/reval-cc-plugin@179faa521b14e4541151effd0f5d28f12ec89597`,
as published by Plugin Hub PR #16622. All 157 source files are retained.
Collectors, collection-log scanning, original event detection, session
persistence/replay, heartbeat timing, sync fingerprints, gzip/multipart sending,
response parsers, caches, buttons and panel loading come from that source.

Allowed differences: NightLegion name/logo/website/Discord/server addresses,
configuration namespace, and Personal Link Token HTTP authentication. The token
is obtained through the existing `/runelite_link`; no new Discord registration
UI is required. Authentication sends headers to the configured NightLegion
origin only and strips them from cross-origin redirects.

Removed from the previous candidate: custom ProgressSyncService/SyncSchedule,
custom bank-evidence collector, client companion-queue polling, custom MVP UI,
and custom automatic cross-tab refresh. Upstream code, not those replacements,
is now the baseline. No new runScript calls were invented; the scripts are the
ones present in the published upstream version.

## Server requirements

The matching backend resolves the existing token, binds the observed account
hash to its Discord owner, handles the original HTTP paths and response DTOs,
and acknowledges saved data/session results instead of returning a queue ID to
RuneLite. It understands original gzip JSON and separate screenshot multipart,
filter/notification versions, full-versus-slim fingerprints and session replay.
It retains earlier fixes to partial-state merging and idempotent scoring.

Only the four owner-retired activity point systems are removed. The catalogue
contains all remaining point-source categories and retains Prefect at 500.
Normal events and registrations are independent of the retired Clan Events
participation-point system and must remain operational.

Historical activity-data removal is a separate tested backup-first migration.
It must not be described as applied until the actual production database is
inspected, backed up and migrated. It never deletes current link tokens or
ordinary event registrations.

## Verification and limits

The source comparison proves client-code preservation after explicit branding
and authentication changes. It does not prove equivalence with Reval's private
backend or their private custom tasks. Tests use our own data and synthetic
Discord/game boundaries, never Reval's private records. Preserve this distinction
when reporting completion.

The public catalogue does not establish a conversion from EHP/EHB to plugin
points. The supplied Reval screenshot describes a separate Discord-rank rule;
do not revive retired WOM Activity points or invent a conversion.

Before production release: native backend deployed, privacy warning reviewed,
authenticated in-game token/initial-sync/relogin acceptance completed, and the
historical-activity migration explicitly resolved. The client must not be
published ahead of its required server routes.
