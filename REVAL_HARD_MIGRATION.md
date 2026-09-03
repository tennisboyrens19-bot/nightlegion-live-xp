# Hard Reval-to-NightLegion migration inventory

Pinned public source: `revalOSRS/reval-cc-plugin` at
`6033d3188b18d34f4bd4c28e6cf7986c8b95f0f9` (BSD-2-Clause).

The complete pinned `com.revalclan` client tree is the implementation source of
truth. Package/class names are retained where doing so preserves the literal
public structure; they are not runtime clan data or visible branding.

| Reval component | Public source files | NightLegion replacement/redirect | NightLegionBot contract |
|---|---|---|---|
| Main shell, header, navigation, styling | `RevalClanPlugin`, `ui/RevalPanel`, `ui/constants`, `ui/components` | NightLegion name/logo/Discord; Reval layout unchanged | Authenticated companion transport |
| Settings and session lifecycle | `RevalClanConfig`, `session/*`, `LoginNotifier`, `LogoutNotifier`, `SyncNotifier` | `nightlegion` config group plus Personal Link Token | `community_reval_event` with persisted login/logout/sync and fingerprint ack |
| Profile and point history | `ui/ProfilePanel`, `api/account/*` | Current RuneLite RSN plus NightLegion points ledger | `/account`, `/account/{id}` |
| Ranks and point sources | `ui/RankingPanel`, `api/points/*`, icon resolvers | Audited pinned public catalogue with approved NightLegion rank naming | `/points`, rank ledger/state tables |
| Achievements | `ui/AchievementsPanel`, `api/achievements/*` | NightLegion member progress derived from verified ledger/state | `/achievements` |
| Events, registration and team colours | `ui/EventsPanel`, `events/*`, `teams/*`, `api/events/*` | NightLegion BOTW/SOTW data and registrations | `/events`, `/events/{id}/register`, `/events/{id}/registration-status`, `/events/active-teams` |
| Competitions, progress, side quests and votes | `ui/CompetitionsPanel`, `api/competitions/*` | NightLegion BOTW/SOTW competition records and NightLegion vote persistence | `/competitions*`, `/competitions/votes*` |
| Diary | `ui/DiaryPanel`, `diaries/*`, `api/diaries/*` | Current character's official in-game diary state | `/diaries` |
| Leaderboard and profile cards | `ui/LeaderboardPanel`, `playercards/*`, `api/leaderboard/*`, `api/playercards/*` | NightLegion rank DB and current/past RSN mapping | `/leaderboard`, `/players/profile-card` |
| Collection Log and Sync Missing Points | `collectionlog/*`, `PlayerDataCollector` | Pinned public capture/UI; no fabricated boundary scan | `community_reval_event` SYNC persistence and idempotent point reconciliation |
| Loot, pets, quests, levels, kills, clues, diaries, CAs, deaths, emotes, chat, music and leagues | `notifiers/*`, `combat/*`, `pbs/*` | Public event capture preserved; current local RSN overwritten server-side as authority | `community_reval_event`, `community_reval_filters`, raw event ledger and points processor |
| Announcements and notifications | `util/AnnouncementService`, `api/announcements/*`, `api/notifications/*` | NightLegion in-game notices and member notifications | `/announcements`, `/notifications`, `/notifications/ack` |
| Staff rank review | `ui/admin/*`, `api/admin/*` | Linked NightLegion Discord roles Major, General, Deputy Owner or Owner; no client code authorization and no automatic OSRS rank mutation | `/admin/auth/login`, `/admin/rank-changes/pending`, `/admin/rank-changes/{id}/actualize` |
| API/webhook/filter services | `api/RevalApiService`, `util/WebhookService`, `util/EventFilterManager` | All direct Reval networking removed and replaced with NightLegion companion calls | `/companion/request`, `/companion/result/{id}` plus the three `community_reval_*` actions |

No Reval production endpoint, database, member/user record, Discord identifier,
secret or token is used at runtime.
