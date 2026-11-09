# Outside reading — planner reference lock

Direct build for the existing Android Quran Nur planner. Refero MCP is unavailable;
use the existing product plus bundled form/icon craft references.

| Decision | Reference | Role |
| --- | --- | --- |
| Full-screen scrollable form with fixed Save and Continue action | PlannerHistoryScreen; user flow | Reliable back, keyboard and long per-page time inputs |
| Existing cream/dark tokens, classic title, outline NurIcons | Planner dashboard and history | Match existing planner identity |
| Fixed current page, count/last-page input and reading date before timing | User request; form craft | Establish exactly what is credited before estimating |
| Estimated versus entered time controls | User request | Never present an estimate as measured time |
| Common per-page time with optional individual overrides | Form craft; user request | Fast common path, precise exception handling |
| Start at the active planner’s current page; bound count/end to assigned pages | Existing PlannerReadingProgress | Avoid duplicate progress/time; canonical 604-page planner numbering |
| Save then resume the next unread page using existing planner reader | User clarification | No alternate reading experience |
| History source totals and dated outside-reading entries | Existing history charts; user request | Measured in app, entered outside, estimated outside remain distinguishable |

Data design: immutable ID-keyed outside logs embedded in each run, additive sync
merge, progress + log + analytics session in one Room transaction. Measured timers
remain unchanged. Estimates use only measured in-app planner time and completed
in-app pages; estimated logs never train the estimate. No arbitrary pace when no
measured sample exists. Restarts preserve the old run and clear the new run's logs.

QA: range validation, duplicate saves, overlap, past/future dates, cross-day ranges,
item plans, resume, manual page overrides, estimate exclusion, cold load and sync,
transaction rollback, light/dark and long/small-screen form scrolling.

User refinement: starting page is read-only. Only page count or last page is entered; both are validated against the latest active plan before saving.

## Validation

- Full unit suite: 542 tests, 541 passed, one existing live-service test skipped.
  Fourteen new domain checks cover active-position/range constraints, cross-day
  continuation, overlap, shared page boundaries, legacy item progress, manual
  durations, weighted pace, history provenance, JSON restoration, restart, and sync.
- Nine emulator checks passed at 320dp width and 1.3 font scale: both input modes,
  disabled invalid inputs, estimated time, per-page overrides, source totals,
  cold-load/retry protection, restart retention, transaction failure rollback,
  and existing history/highlight/reflection screens.
- Visual review found long floating minute labels crowded the page headings at
  enlarged font sizes. Compact localized minute labels and extra field spacing
  fixed this; all four form checks were rerun successfully after that change.
- Localization coverage/format audit: 974 resources in 21 app languages; action
  names, time-source distinctions and ambiguous minute-unit translations reviewed.
- Espresso's input strategy is incompatible with the installed API-37 emulator.
  The new UI tests drive the real Compose semantic actions through the host view,
  without adding production dependencies or bypassing the form's callbacks.
- Test plans, archived runs, session fixtures and resume preferences restored.
  Emulator dimensions/font scale restored after responsive checks.
- No HTTP is required by logging. Measured planner timers remain separate; outside
  time enters analytics once in the same Room transaction as progress and its log.
- Personal pace uses completed pages with measured time from planner runs. Surah
  sessions without page counts cannot provide a reliable per-page sample; with no
  measured sample, the form requests an entered time instead of inventing a pace.

Artifacts: `build/outside-reading-current-page-final.log`,
`build/outside-reading-final-device.log`, `build/outside-reading-visual-final-device.log`,
`build/outside-reading-visual-final-build.log` and `build/outside-reading-verified-dark.png`.
