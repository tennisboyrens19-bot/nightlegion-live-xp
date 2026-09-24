# Pinned Reval source verification

Baseline: `revalOSRS/reval-cc-plugin` commit
`179faa521b14e4541151effd0f5d28f12ec89597`.
Archive SHA-256:
`65972faa05010ecf641772ea3f25490235d35f13da8c2da6648decd7638c6f8a`.

## Confirmed PR #9 failure

The logs for candidate `17857eeb427a98d585ef2fed056a9980aa381eab` were read:

- [Exact Reval release tests, job 107597292980](https://github.com/tennisboyrens19-bot/nightlegion-live-xp/actions/runs/35988679379/job/107597292980)
- [NightLegion update audit and tests, job 107597292553](https://github.com/tennisboyrens19-bot/nightlegion-live-xp/actions/runs/35988679454/job/107597292553)

Both jobs reported `BUILD SUCCESSFUL` before the source check failed with
`Unapproved upstream deviations` for `RevalClanPlugin.java`,
`collectionlog/CollectionLogManager.java` and `notifiers/PetNotifier.java`.
The checks were correctly detecting changes that the old identity/authentication
transformations did not describe.

## Reviewed adaptations

`tools/reval_source.py` reproduces the expected text from the pinned archive.
Each bug-fix transformation requires its exact upstream anchor to occur once.
The complete resulting file must then match the working tree. A listed filename
is never an exemption from content verification.

| File | Necessary difference |
| --- | --- |
| `RevalClanPlugin.java` | Existing identity/authentication and RuneLite Filepath wiring. The extra `PlayerDataCollector` field was unused and has been removed; login/logout notifiers already inject that collector. Consume fingerprint-repair requests only for the active account. |
| `collectionlog/CollectionLogManager.java` | Reject zero/invalid item counts, guest POH books and temporary-world evidence; apply cache replacement IDs before storing ownership. Temporary-world flags use the actual RuneLite names, including `TOURNAMENT_WORLD`, and cover seasonal, Deadman, beta, no-save, speedrunning, PvP Arena and Last Man Standing worlds. Ordinary PvP, high-risk and fresh-start worlds remain allowed. Resolve exact pet names only within the cache's All Pets category, rejecting ambiguity. |
| `notifiers/PetNotifier.java` | Include that cache-resolved pet item ID in the existing event, allowing a correct sprite without speculative name matching. |
| `notifiers/DiaryNotifier.java` | Correct 41 completion varbit constants to match all 48 entries in the existing full-sync diary manager. Event handling and Karamja completion semantics remain upstream. |
| `api/account/AccountResponse.java` | Parse the backend's existing account-specific combat achievement thresholds. |
| `ui/ProfilePanel.java` | Use positive account-specific CA thresholds for completion. Fall back to the catalogue for missing/invalid thresholds; retain catalogue reward ordering. Clear cached account/history/album state on logout; ignore success/error callbacks from the previous account generation. These anchored changes are separated in `tools/reval_account_fixes.py`. |
| `collectionlog/CollectionLogSyncButton.java` | Show success only after an acknowledged sync, marshal UI updates to the client thread, and discard pending syncs/callbacks across account boundaries. |
| `notifiers/SyncNotifier.java` | Distinguish successful, incomplete and failed acknowledgements. Only matching fingerprints can be stored; preserve the original no-argument call. |
| `notifiers/BaseNotifier.java` | Propagate clan-gate rejection to the optional sync completion callback. |
| `util/WebhookService.java` | Report transport, HTTP and malformed-response failure through an optional callback; preserve existing overloads. |
| `util/SyncStateManager.java` | Bind queued fingerprint-repair requests to their originating account. Keep a concurrent set of account hashes so a delayed old-account acknowledgement cannot overwrite a current-account repair request. |

The sync changes use exact context-matched transformations in
`tools/reval_sync_fixes.py`. They do not allow arbitrary edits to those files.
Some files contain both existing authentication wiring and scoped bug fixes;
the generated report identifies both boundaries.

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
- Any change to the pinned authentication boundary or NightLegion icon.
- A missing runtime source file.
- An extra runtime source file.

Positive tests require the current candidate and Windows CRLF checkout to pass.
Only CRLF-to-LF normalization is applied to Java/license text. Other whitespace,
comments, code and binary resources remain significant. Both existing parity
workflows retain the source check and additionally execute these tests.
