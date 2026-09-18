# NightLegion update

This release branch preserves the newer NightLegion candidate
`9f65e42b6116c06d9b0ddbca09ada84dd97f796e`, including the maintainer-requested
RuneLite Filepath changes. It must not be replaced with the older published
`990f550` client, and it must not create another duplicate Plugin Hub PR.

The current source baseline is the published Reval client at
`179faa521b14e4541151effd0f5d28f12ec89597`. Runtime changes are restricted to
NightLegion branding/destinations, existing Personal Link Token authentication,
and the documented Filepath compatibility required by RuneLite review.

## Owner checklist

- Website globe: https://nightlegion-web.vercel.app/.
- Keep the plugin display name NightLegion; use NightLegion update for the
  release/PR title, not a second installable plugin.
- Exact approved rank ladder: Mentor 0; Prefect 500; Leader 1,000;
  Supervisor 2,000; Superior 4,000; Executive 7,500; Senator 10,000;
  Monarch 15,000; Red Topaz 17,500; Sapphire 20,000; Emerald 22,500;
  Ruby 25,000; Diamond 27,500; Dragonstone 30,000; Onyx 35,000;
  Zenyte 40,000; Marshal 50,000. Only Marshal has 50/month maintenance.
  Ranks and scoring are server-owned; verify the paired backend catalogue.
- Retire CLAN_ACTIVITY, DISCORD_ACTIVITY, CLAN_EVENTS and WOM_ACTIVITY point
  systems only. Do not delete ordinary event registrations or existing link
  tokens. Do not run historical-data migrations as part of this code update.
- Preserve upstream profile, achievements, events, competitions, diary,
  collection log, session and notifier behavior. No replacement full-account
  scanner or undocumented synthetic point awarding.
- Explicitly report whether MVP exists in the pinned upstream source; do not
  describe copying a version without MVP as an MVP fix.

The audit workflow produces complete source differences, an exact-source
verification report and Java test reports for the precise branch commit.
Neither these tests nor a Plugin Hub merge prove live in-game acceptance or
parity with Reval's private backend. Backend deployment and in-game checks
must be reported separately, with no fabricated completion claims.
