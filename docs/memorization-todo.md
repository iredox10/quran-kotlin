# Memorization implementation TODO

Source: [research and code audit](research/memorization-improvement-review.md).
Work in small verified commits. Preserve existing memorized material, Hafs verse ownership,
offline operation and web-compatible scheduler cards. Assessor labels record explicit self/listener/teacher context;
they do not claim independent teacher certification. Speech recognition remains an opt-in evaluated extension,
not a prerequisite for offline memorization.

## 1. Trustworthy recall and review
- [x] Correct rating semantics: a prompt is not an independent pass; distinguish effort from failure.
- [x] Record cue usage and assessment mode; distinguish cue-based transition tests from unprompted recall.
- [x] Correct failures and requeue them after intervening practice without endless loops.
- [x] Refresh due work as time passes and when returning to the app.

## 2. Evidence and scheduling
- [x] Add append-only review events with unique IDs, passage identity, rating, cues and timestamps.
- [x] Persist acquisition and independent-pass dates; migrate old material as imported/unknown.
- [x] Classify recent revision by learning dates, not last-opened surah.
- [x] Preserve web/mobile sync compatibility; merge events idempotently and retain existing cards.
- [x] Show learning, independent recall, delayed retention and repair states separately from teacher assessment.

## 3. Guided daily work
- [x] Add Start Today with time budget and New / Recent / Older revision sections.
- [x] Protect older-portion coverage and prioritise overdue/weak work within the budget.
- [x] Add a manageable missed-day recovery plan with visible outstanding work.
- [x] Guide listen/read → hidden recall → correction → spaced retest → later-day review.
- [x] Support long ayahs without inventing recitation stops; let users/teachers select valid practice portions.

## 4. Quran-specific practice
- [x] Add ayah-to-ayah and page-boundary transition drills.
- [x] Add continuous passage assessment alongside random-start tests.
- [x] Add mistake notebook: exact location, type, notes, recurrence and later repair.
- [x] Add optional local recordings with replay and deletion; never upload automatically.
- [x] Add offline listener/tasmiʿ mode and explicit human assessment provenance.
- [x] Add similar-passage candidate comparison and user/teacher-confirmed confusion pairs.
- [x] Pin each memorization plan to its own edition and expose supported scope accurately.

## 5. History, validation and handoff
- [x] Add recall/retention history, unresolved mistakes, review coverage and workload.
- [x] Separate listening/practice time from assessed retention.
- [x] Verify migrations, clock handling, duplicate events and offline behavior with meaningful tests.
- [x] Verify core flows on emulator; keep TODO updated and make micro commits.
- [x] Document teacher-pilot criteria and optional speech-assistance evaluation requirements.

## Later extension requiring real evaluation
- [ ] Optional automatic speech feedback: validate against consenting teacher-labelled recordings
      per riwayah, accents/noise and self-corrections before enabling automatic grading.
      Never imply that generic transcription certifies tajweed or makharij.

## Delivery notes — 2026-11-07

Open **Memorization → Start today** for the Android workshop. It uses a pinned Hafs /
Madani reference independently of the ordinary reading mushaf. Existing reader,
manual memorization and scheduler data remain available.

Verification completed:

- Offline Gradle unit-test, debug APK and instrumentation APK builds passed.
- 514 JVM tests passed; one live Heroku integration test was skipped.
- Seven emulator tests passed: workshop navigation/reference preservation,
  prompted recall and bounded correction, bundled similar passages, translation
  cleanup, account-scoped journal merge, explicit recording/replay/deletion and
  offline content reads with HTTP calls blocked.
- The matching backend compiled and 11 targeted merge/planner tests passed.

The backend event-window cap is committed in `quran-backend` as `4fdd786` and must be
deployed before public release. Nothing in this delivery pushes or deploys production.
The event transport contains at most 500 recent events; older records remain local.
A lifetime cloud archive and equivalent web workshop interface are separate future
work. This implementation preserves the shared scheduler/sync contract.

See [validation and teacher pilot](research/memorization-validation.md) for supported
scope, recording privacy, release prerequisites and the speech-feedback evaluation gate.
