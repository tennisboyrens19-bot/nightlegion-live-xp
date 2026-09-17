# NightLegion profile and sync repair candidate

Version: **2.18.7-nightlegion.2**. Unreleased until approved and submitted through Plugin Hub. Paired backend: NightLegionBot PR #4; client PR #6. No live database or Plugin Hub manifest was changed by this work.

## Connection, profile and presentation

Restore the existing globe button for https://nightlegion-web.vercel.app/. Token changes invalidate old transport responses, reset authenticated/admin state and retry after clan validation. Errors wrap instead of clipping and include Retry. EHP/EHB unknown values remain unknown, and profiles consume character-scoped milestone evidence and full signed ledger totals. One automatic overall monthly MVP remains on the leaderboard with gold profile/player-card badges.

The original Reval navigation arrangement is restored: Profile / Achievements; Events / trophy / Competitions; Diary. This supersedes the earlier candidate's moved trophy button. Prefect remains the official name/icon, with Mentor 0 / Prefect 500 / Leader 1,000 and unchanged remaining rank policy. The four retired activity-point sections and CHAT collector remain removed; ordinary events, registrations, game-event detection, team colours and profile cards are preserved.

## Automatic sync and refresh

A single-flight coordinator performs an initial snapshot after the logged-in account and clan are ready (3-second warm-up), coalesces observed state changes with a 5-second minimum request spacing, and performs a 60-second periodic reconciliation. Failures retry with a 15-second backoff using the same observation identity. Logout, account/token changes and shutdown reset the generation so stale callbacks cannot affect the new context.

Snapshots read existing client/cache state only. They do NOT open bank/collection-log interfaces or run a new client script. The bank reader uploads only allowlisted one-time milestone IDs, not the full bank; only positive-quantity, non-placeholder items are eligible. The server retains evidence after the item disappears from the currently visible containers.

After a successful server acknowledgement, the plugin invalidates the account cache and refreshes Profile, Achievements and Diaries together, plus the leaderboard when visible. Refresh work is coalesced, selected tabs/diary detail are preserved, and stale/error responses do not erase known diary completion. The manual Sync missing points button is a resync fallback and retains the existing collection-log guide.

## Historical evidence and scoring limits

For a bank-only Fire cape, open the bank normally once, or expose the item in inventory/equipment. The backend recognizes an actual Avernic defender as evidence of the earlier Dragon defender milestone, but not a tradeable hilt. Historical pets require available All Pets collection-log evidence. Open that page and use the existing collection-log sync when necessary. The plugin cannot reconstruct arbitrary old sold/lost items or unknown duplicate-pet counts without evidence.

CA and collection-log point labels now show incremental rewards rather than adding cumulative targets repeatedly, matching the relevant public Reval change. Available live game CA thresholds are passed through to display/scoring. The preserved reviewed CollectionLogSyncButton.java and DiaryNotifier.java files remain byte-identical to the published release.

EHP/EHB statistics and monthly MVP are supported. The fetched public Reval catalogue does not define an EHP/EHB-to-clan-point rate or ordinary OSRS diary-tier rewards. Those conversions remain unconfigured pending an explicit rule; no rates were invented and the retired WOM Activity points were not reinstated. The all-48-diaries milestone remains 500 points. Third-party efficiency values reflect the provider's stored snapshot, with the existing bounded cache; they are not guaranteed instant hiscores updates.

## Reval source cross-check

Original reference: revalOSRS/reval-cc-plugin at 6033d3188b18d34f4bd4c28e6cf7986c8b95f0f9. Compared Plugin Hub PR #16404 (heartbeat/session preservation) and #16622, source 179faa521b14e4541151effd0f5d28f12ec89597 (incremental tier labels, lazy panels and other changes). Reval's custom Clan Diaries must not be confused with ordinary OSRS achievement diaries. This focused repair is not a claim that all functionality of their private backend or every latest PR change was copied.

## Tests and release acceptance

The complete Gradle build exercises real Java classes, DTOs, Swing rendering and mock HTTP, with additional coordinator timing/retry/logout, bank evidence/placeholder and cross-tab refresh tests. Backend tests exercise service -> state merge -> real SQLite -> scoring -> tab responses using disposable fixtures, including restarts and repeated/concurrent deliveries. Final read-only CI packages the JAR and retains XML reports. These are NOT authenticated live-client tests.

Before release, deploy/test the paired backend in an approved environment, complete its separately reviewed backup-first retirement migration as appropriate, then verify fresh-existing accounts, actual bank and All Pets observations, new CA/diary/item/pet updates, loss/retry/relogin, points consistency, and unchanged collection-log sync in RuneLite. Do not publish an untested live candidate solely because CI is green.
