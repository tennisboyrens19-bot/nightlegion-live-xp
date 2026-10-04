# Pinned Reval source verification

Baseline: `revalOSRS/reval-cc-plugin` commit
`179faa521b14e4541151effd0f5d28f12ec89597`.
Archive SHA-256:
`65972faa05010ecf641772ea3f25490235d35f13da8c2da6648decd7638c6f8a`.

## Reviewed adaptations

`tools/reval_source.py` reproduces the expected text from the pinned archive.
Each bug-fix transformation requires its exact upstream anchor to occur once.
The complete resulting file must then match the working tree. A listed filename
is never an exemption from content verification.

| File | Necessary difference |
| --- | --- |
| `RevalClanPlugin.java` | Existing identity/authentication and RuneLite Filepath wiring. Login/logout notifiers retain their original collector injection. Consume fingerprint-repair requests only for the active account; clear obtained Collection Log items and API caches at logout. |
| `api/RevalApiService.java` | Use account/catalog generations to prevent responses already in flight from restoring cleared cache data or invoking callbacks for an old account. |
| `collectionlog/CollectionLogManager.java` | Reject zero/invalid item counts, guest POH books and temporary-world evidence; apply cache replacement IDs before storing ownership. Temporary-world flags use the actual RuneLite names, including `TOURNAMENT_WORLD`, and cover seasonal, Deadman, beta, no-save, speedrunning, PvP Arena and Last Man Standing worlds. Ordinary PvP, high-risk and fresh-start worlds remain allowed. Resolve exact pet names only within the cache's All Pets category, rejecting ambiguity. |
| `notifiers/PetNotifier.java` | Include that cache-resolved pet item ID in the existing event, allowing a correct sprite without speculative name matching. |
| `notifiers/DiaryNotifier.java` | Correct 41 completion varbit constants to match all 48 entries in the full-sync diary manager. Reject a queued completion after reset, logout or account change; preserve the original Karamja completion semantics. |
| `notifiers/LootNotifier.java` | Assign a stable UUID to each queued receipt so separate identical drops remain distinct even at the same send timestamp. Clear pending loot and account-specific receipt evidence on session reset. Existing raid reward duplicate suppression remains intact. Accept RuneLite 1.13.1 long item prices without narrowing, preserving unit-price filters and stack-value arithmetic. |
| `notifiers/ClueNotifier.java` | Accept RuneLite 1.13.1 long item prices without narrowing, preserving clue quantity arithmetic and payload fields. |
| `notifiers/DeathNotifier.java` | Carry RuneLite 1.13.1 long item prices through valuation and descending price sorting, preserving item selection and quantity arithmetic. |
| `api/account/AccountResponse.java` | Parse account-specific combat achievement thresholds. Preserve fractional balances, category totals and ledger deltas. |
| `ui/ProfilePanel.java` | Use positive account-specific CA thresholds, falling back to the catalogue for missing/invalid thresholds; retain catalogue reward ordering. Clear account/history/album/catalog state on logout; reject stale account/catalog callbacks. Allow refresh to restart a cancelled request. Preserve fractional points in the profile, rank progress and category totals. |
| `ui/PointsAlbumWindow.java` | Preserve fractional ledger values when grouping, totaling and sorting existing cards. |
| `ui/RankingPanel.java` | Reset account-specific ranking data and ignore responses from an obsolete request generation. |
| `ui/RevalPanel.java` | Reset and reload the ranking view at login/logout boundaries. |
| `ui/AchievementsPanel.java` | Clear account-specific achievement data at logout; reject late success/error callbacks after logout or a newer request. |
| `ui/DiaryPanel.java` | Clear diary data, expanded tiers and selected detail view at logout; reject late success/error callbacks after logout or a newer request. |
| `util/NumberFmt.java` | Display point balances and deltas to the same four-decimal precision as the backend. |
| `util/UIAssetLoader.java` | Apply the Plugin Hub maintainer's requested `Class.getResourceAsStream()` loading in both bundled-image paths, closing each stream with try-with-resources. Preserve resource paths, normalization, caches, scaling and null/error fallbacks. |
| `session/SessionTracker.java` | Clear the in-flight marker after HTTP/transport failure so the existing persisted session replay can retry. |
| `collectionlog/CollectionLogSyncButton.java` | Show success only after an acknowledged sync, marshal UI updates to the client thread, and discard pending syncs/callbacks across account boundaries. |
| `notifiers/SyncNotifier.java` | Distinguish successful, incomplete and failed acknowledgements. Accept strict native `accepted: true` or legacy `ok: true`; reject explicit false/malformed flags. Only matching fingerprints can be stored; preserve the original no-argument call. |
| `notifiers/BaseNotifier.java` | Propagate clan-gate rejection to the optional sync completion callback. |
| `util/WebhookService.java` | Report transport, HTTP and malformed-response failure through an optional callback; preserve existing overloads. |
| `util/SyncStateManager.java` | Bind queued fingerprint-repair requests to their originating account. Keep a concurrent set of account hashes so a delayed old-account acknowledgement cannot overwrite a current-account repair request. |

The changes use exact context-matched transformations in
`tools/reval_account_fixes.py`, `tools/reval_sync_fixes.py` and
`tools/reval_acceptance_fixes.py`. Every replacement is a fixed reviewed literal;
the checker does not generate expectations from the candidate tree. The complete
transformed file must match, so these helpers do not permit arbitrary edits.
Some files contain both existing authentication wiring and scoped bug fixes;
the generated report identifies both boundaries.

The 157 upstream Java files comprise 120 identical after line-ending
normalization, 11 with only branding/destination changes, 2 with only
authentication wiring, 1 exact Filepath adaptation and 23 with scoped bug fixes.

Existing literal branding/destination substitutions, token-authentication seams,
the authentication class, the NightLegion icon and the exact Filepath
`SessionStore` hash remain explicit boundaries. The added authentication class
and custom icon are now also checksum-verified; the old checker only verified
their presence. Licenses are preserved. This
check does not establish backend behavior or replace live acceptance tests.

## Regression of the check itself

`tools/test_reval_source.py` first loads the same checksum-verified archive. It
copies the candidate runtime tree to a temporary directory and invokes the real
`reval_source.main()` entry point. Negative tests require rejection of:

- An unrelated Java field in preserved `QuestNotifier.java`.
- An unrelated Java field in adapted `CollectionLogManager.java`.
- A changed zero-count ownership guard inside the approved adaptation.
- A removed stale-account callback guard.
- A removed sync-acknowledgement fingerprint guard.
- Unrelated code inside the newly adapted loot notifier.
- A constant receipt ID, missing pending-loot reset or missing logout ownership reset.
- A removed diary account guard, session failure callback or API generation guard.
- A changed fractional ledger model or point precision formatter.
- A removed native acknowledgement rejection guard.
- A removed stale-response guard in either the achievements or diary panel.
- Unrelated code, removed stream closure or removed missing-resource fallback in the bundled-image loader.
- Narrowed clue/loot/death prices, changed clue quantity arithmetic, changed loot unit-price thresholds, narrowed death sorting or unrelated death-notifier code in the RuneLite 1.13.1 compatibility adaptation.
- Any change to the pinned authentication boundary or NightLegion icon.
- A missing runtime source file.
- An extra runtime source file.

The 33 tests include positive checks for the current candidate and Windows CRLF
checkout and negative checks for every mutation above.
Only CRLF-to-LF normalization is applied to Java/license text. Other whitespace,
comments, code and binary resources remain significant. Both existing parity
workflows retain the source check and additionally execute these tests.
