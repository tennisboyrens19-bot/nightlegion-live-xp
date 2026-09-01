# Live On divergence record

## Baseline

The active NightLegion implementation was compared file-by-file against:

- Upstream repository: `https://github.com/MilicoOSRS/live-on-clan`
- Pinned commit: `8f69ae9b1906f75fe2896d780fb6858d4a969139`
- Upstream source root: `src/main/java/com/liveon`
- NightLegion production entrypoint: `com.liveon.ClanMessagesPlugin`

The `com.liveon` package and the `live-on-clan-messages` RuneLite configuration
group are retained as compatibility identifiers. They are not runtime clan data
or user-visible NightLegion branding.

## Code/behavior versus runtime data

### Copied code and behavior

The pinned repository is used only as the licensed implementation source for
the Live On panels, calculations, parsers, interactions, message presentation,
and badge placement. Source attribution, the pinned commit, compatibility class
names, and compatible request/response field names may therefore retain the
`Live On`/`liveon` identifier where they are not user-visible runtime data.

### NightLegion runtime data and infrastructure

All live requests use NightLegion services and NightLegion identifiers. All
membership checks, current-player actions, announcements, leaderboards, PBs,
rank records, Twitch mappings, Discord links, and stored records are populated
only from the current NightLegion environment. The upstream repository is never
queried as a runtime data source.

No upstream clan members, RSNs, WOM group, Discord guild/channel identifiers,
announcements, MVP/PB history, Twitch channels, rank/member records, database or
storage contents, backend service URL, API credentials, tokens, or secrets are
included, seeded, migrated, imported, or referenced by the runtime. The static
logo resource keeps its compatibility filename but contains NightLegion artwork.

## Intentional differences

### Branding and English text

- All visible plugin branding is NightLegion.
- Visible Portuguese UI/status text is translated to English without changing
  the corresponding interaction or calculation.
- NightLegion artwork, rank display aliases, Discord invite, WOM link, and
  backend service replace the upstream presentation/infrastructure values.
- These changes affect `ClanMessagesConfig`, `ClanMessagesPanel`,
  `ClanMessagesPlugin`, `ClanTagsPanel`, `LiveOnPanel`, `MvpPanel`, `PbCategory`,
  `PbPanel`, `RankRequestsPanel`, `RanksPanel`, `RankVisuals`, and
  `WomMembership`.

### NightLegion identity and authorization

- The current local RuneLite player name remains the active RSN and is sent in
  `X-Live-On-Player`; the Personal Link Token identifies only the linked Discord
  member.
- The hidden second staff-secret UI/config path is removed. The backend
  independently resolves NightLegion WOM membership and owner/deputy/staff/
  broadcast capabilities for the current RSN.
- Local NightLegion clan titles map Major/General/Deputy Owner/Owner into the
  upstream staff presentation while preserving the administrator broadcast
  restriction and deputy/owner-only clan-tag management.

### NightLegion extras

- A narrow bridge adds the existing NightLegion BOTW, SOTW, Giveaway, and Groups
  surfaces around the upstream panel.
- The Events dropdown is exactly `BOTW`, `SOTW`, `GIVEAWAY`.
- Group Finder is absent only from Events. The standalone top-level Groups page
  and its existing backend/bot behavior remain.
- The previous rewritten NightLegion Home/Ranks/MVP/PB/Staff/root plugin stack
  was removed so it cannot become an alternative production implementation.

### Announcements and clan messages

- The upstream Staff/message flow is backed by NightLegion storage and delivered
  through RuneLite's in-game clan/system message polling.
- Panel announcements also create the corresponding in-game clan message.
- No announcement create/edit/delete path calls Discord, stores a Discord
  announcement message ID, or requires an announcement channel.

### Automatic single Overall MVP

- The upstream Drops/EHB/EHP panels, ranking presentation, gold markup, green
  LIVE markup, native member-list decoration, and clan-chat decoration remain.
- Manual MVP controls and `MvpManagementPanel` are removed. Manual add/remove
  backend actions are rejected and stored manual badge lists are not read as an
  authority.
- The backend awards 3/2/1 points for 1st/2nd/3rd on each monthly Drops/EHB/EHP
  board and returns exactly one Overall MVP.
- Ties are resolved by Drops position, EHB position, EHP position, then
  normalized RSN alphabetically.
- MVP and LIVE are evaluated independently, so both upstream badges can appear
  on the same player.

### PB synchronization

- The upstream `PbPanel` and upstream PB payload/parsing functions remain the
  active implementation.
- POH Adventure Log import remains supported as an optional historical import.
- Supported Combat Achievement boss pages, boss statistics boards, scoreboard/
  stat interfaces, and chat PB messages submit exposed PB times immediately;
  successful submissions refresh PB categories.
- PB storage, ranking ownership, and drop/MVP ownership use normalized current
  RSN rather than the Discord member ID or a previously linked RSN.

### Build and package integration

- `runelite-plugin.properties`, the Gradle `run` task, and the developer launcher
  all load `com.liveon.ClanMessagesPlugin`.
- Lombok-generated logging/accessors/value methods were replaced with equivalent
  explicit Java methods, removing an unnecessary annotation-processor build
  dependency without changing runtime behavior.
- Focused upstream-derived tests cover WOM permissions, rank notifications,
  loot filtering, Adventure Log/CA/boss-stat PB parsing, and the approved Events
  dropdown. Backend tests cover identity separation, server authorization,
  in-game-only messages, and the single deterministic Overall MVP.

Any future source change under `src/main/java/com/liveon` must either preserve
the pinned behavior, fit one of the categories above, or be added to this file
with a concrete technical reason before release.
