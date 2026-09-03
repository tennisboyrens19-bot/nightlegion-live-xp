# NightLegion

NightLegion is a hard clan-specific adaptation of the public Reval RuneLite
plugin pinned at commit `6033d3188b18d34f4bd4c28e6cf7986c8b95f0f9`.
The public Reval client structure, navigation, layout, styling, interaction
model, profile cards, point catalogue, sync flow and notifiers are retained.
Only clan-specific branding, configuration, identity and backend transport are
replaced.

## Setup

1. Install **NightLegion** from RuneLite Plugin Hub.
2. Generate a **Personal Link Token** in the NightLegion Discord.
3. Paste the token into the NightLegion RuneLite settings.
4. Log into a character in the NightLegion clan.

The token identifies the NightLegion Discord member. The currently logged-in
RuneLite player name remains the authoritative RSN for every request and event.

## NightLegion-only runtime

The plugin does not connect to Reval services. Reval production users, member
records, Discord data, databases, URLs, credentials, tokens and secrets are not
copied, queried or bundled. All client requests go through the NightLegion
companion relay and are fulfilled by NightLegionBot using NightLegion data.

Depending on enabled settings, the copied public notifier system sends the
current RSN and event-specific RuneLite state needed for clan profiles, points,
achievements, events, competitions and synchronization. This can include item
IDs/quantities/prices, inventory/equipment snapshots attached to an event,
quests, levels, kill counts, achievement diaries, combat achievements,
collection-log observations, personal bests and configured system messages.
It never sends RuneScape/Jagex passwords or Jagex account credentials.

## Development

The project targets Java 11. Runtime HTTP uses RuneLite's injected
`OkHttpClient`. See `REVAL_HARD_MIGRATION.md` for the source-to-NightLegion
contract inventory and `THIRD_PARTY_NOTICES.md` for licenses and attribution.
