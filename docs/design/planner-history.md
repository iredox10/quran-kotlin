# Planner history screen — reference lock

Designing Android completed-plan history for Quran Nur readers. Goal: review the reading
journey, saved Quran highlights and daily reflections without losing content after restart.
Direct build against the existing Quran Nur planner design system, not a new visual brand.

Live Refero tools are unavailable. References inspected before implementation:

- Existing PlannerScreen / PlanTodayTab: primary canvas, classic title face, dark/light
  tokens, gold reserved for reading progress and meaningful emphasis, Lucide/Nur icons.
- Existing AnalyticsFlowChart: secondary reference for readable, truthful time graphics.
- Existing PlanJournalTab: secondary reference for verse identity and journal context.
- Refero bundled typography, icons, craft-details and visual-workflow guides: readable
  text, restrained hierarchy, labeled controls, touch targets, and rendered visual QA.

| Decision | Source | Rule / role |
| --- | --- | --- |
| Real navigation destination and back behavior | User request; existing immersive routes | No modal, close button, or enclosing card |
| Quiet full-width canvas with separated list rows | Existing planner; typography guide | Text and spacing provide hierarchy; borders separate content |
| Three labeled detail sections | User request; journal reference | Overview, highlights, reflections are immediately discoverable |
| Gold daily bars and an accumulated-time line | Analytics reference; user request | Actual recorded values only; unknown historical dates stay unknown |
| Arabic verse, meaning, reference and saved note | Journal reference; user request | Offline source text; complete long content remains readable |
| 16sp journal prose, 24sp line height, 44–48dp actions | Typography / craft guides | Readability and accessible targets before decoration |
| Existing outline icons and theme tokens | NurIcons; icons guide | Icons reinforce labels; no new icon family or decorative gradients |

Media strategy: code-native data charts and existing icons; no bitmap imagery required.
QA: populated/empty list, completed and ended plans, long notes and Arabic, light/dark,
actual offline highlight loading, Android back, and a small-width reading layout.

## Validation

- Offline debug and instrumentation APK builds passed.
- Unit suite: 528 tests, 527 passed, one existing live-service test skipped; no failures.
- Final emulator run: five tests passed, covering empty/populated history, daily and
  accumulated charts, long reflections and Quran highlights, restart/cold-load retention,
  and local selected-translation loading with content network requests blocked.
- Visually inspected light overview and dark long-ayah/reflection screens at 320dp
  width and 1.3 font scale. Tab labels fit; long content remains vertically scrollable.
- Matching bundled QPC word scripts are restored locally for highlight text; legacy
  text uses its compatible fallback font. No content download is required by history.
- Test planner fixtures and translation preferences were restored; emulator size and
  font scale returned to their original settings. No physical phone installation.
- Final artifacts: `build/planner-history-qa-check.log`,
  `build/planner-history-qa-final-device.log`, and `build/planner-history-*-final.png`.
