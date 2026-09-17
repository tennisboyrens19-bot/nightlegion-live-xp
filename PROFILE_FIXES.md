# NightLegion profile/MVP repair candidate

Version: **2.18.7-nightlegion.2**. Unreleased until approved and submitted through Plugin Hub.

## Client changes

- Restore the existing website globe button for https://nightlegion-web.vercel.app/.
- Refresh authenticated data automatically after a Personal Link Token change; invalidate old transport completions and reset profile/admin state. Wrap connection errors and provide Retry instead of clipping the message.
- Display backend-verified milestone completion, explicit unknown efficiency values, and full signed point-breakdown fields.
- Show one automatic overall monthly MVP on the existing leaderboard page with gold badges on the profile, leaderboard entry and player card. Standings use Drops/EHP/EHB and the existing 3/2/1 top-three scoring; incomplete data never assigns a winner.
- Preserve the official **Prefect** name/icon. The backend keeps Mentor 0, Prefect 500, Leader 1,000 and all other thresholds/maintenance requirements.
- Remove the obsolete CHAT telemetry collector/setting. Keep actual game-event detection, clan profile cards, event team colours, announcements and notifications.
- Reject the four retired activity-source sections in old server responses. This is only backward-compatibility defense: the matching backend update removes their collectors, awards and data rather than just hiding the UI.
- Give the Competitions tab more room and keep the MVP layout compact at narrow sidebar widths.

## Backend prerequisite

Deploy the matching NightLegionBot profile/retirement changes and complete its reviewed backup-first activity-data migration. It reconciles removed historical activity credits; it does not delete real events, registrations, legitimate game achievements, current Personal Link Tokens or actual in-game staff ranks. The client does not perform that database migration.

## Tests and remaining acceptance

The full project test suite compiles the real plugin, tests DTO compatibility, actual Swing milestone/MVP rendering, 180/220/250-pixel layouts, token save/poll/error/cancellation behavior against a mock HTTP server, and removal of retired chat telemetry. Headless screenshots and test reports are retained by CI. The package version is read from the same metadata used for the plugin user agent.

These are not an authenticated in-game acceptance test. Before Plugin Hub submission, verify the current live account's profile, milestone sync, MVP standings, website click, token change without restart, event functions and staff panel in RuneLite. The reviewed runScript calls are outside this change scope and remain unchanged.
