# Memorization implementation TODO

Source: [research and code audit](research/memorization-improvement-review.md).
Work in small verified commits. Preserve existing memorized material, Hafs verse ownership,
offline operation and web-compatible scheduler cards. No progress is teacher verified without
an explicit listener assessment. Speech recognition remains an opt-in evaluated extension,
not a prerequisite for offline memorization.

## 1. Trustworthy recall and review
- [ ] Correct rating semantics: a prompt is not an independent pass; distinguish effort from failure.
- [ ] Record cue usage and assessment mode; distinguish cue-based transition tests from unprompted recall.
- [ ] Correct failures and requeue them after intervening practice without endless loops.
- [ ] Refresh due work as time passes and when returning to the app.

## 2. Evidence and scheduling
- [ ] Add append-only review events with unique IDs, passage identity, rating, cues and timestamps.
- [ ] Persist acquisition and independent-pass dates; migrate old material as imported/unknown.
- [ ] Classify recent revision by learning dates, not last-opened surah.
- [ ] Preserve web/mobile sync compatibility; merge events idempotently and retain existing cards.
- [ ] Show learning, independent recall, delayed retention and repair states separately from teacher assessment.

## 3. Guided daily work
- [ ] Add Start Today with time budget and New / Recent / Older revision sections.
- [ ] Protect older-portion coverage and prioritise overdue/weak work within the budget.
- [ ] Add a manageable missed-day recovery plan with visible outstanding work.
- [ ] Guide listen/read → hidden recall → correction → spaced retest → later-day review.
- [ ] Support long ayahs without inventing recitation stops; let users/teachers select valid practice portions.

## 4. Quran-specific practice
- [ ] Add ayah-to-ayah and page-boundary transition drills.
- [ ] Add continuous passage assessment alongside random-start tests.
- [ ] Add mistake notebook: exact location, type, notes, recurrence and later repair.
- [ ] Add optional local recordings with replay and deletion; never upload automatically.
- [ ] Add offline listener/tasmiʿ mode and explicit human assessment provenance.
- [ ] Add similar-passage candidate comparison and user/teacher-confirmed confusion pairs.
- [ ] Pin each memorization plan to its own edition and expose supported scope accurately.

## 5. History, validation and handoff
- [ ] Add recall/retention history, unresolved mistakes, review coverage and workload.
- [ ] Separate listening/practice time from assessed retention.
- [ ] Verify migrations, clock handling, duplicate events and offline behavior with meaningful tests.
- [ ] Verify core flows on emulator; keep TODO updated and make micro commits.
- [ ] Document teacher-pilot criteria and optional speech-assistance evaluation requirements.

## Later extension requiring real evaluation
- [ ] Optional automatic speech feedback: validate against consenting teacher-labelled recordings
      per riwayah, accents/noise and self-corrections before enabling automatic grading.
      Never imply that generic transcription certifies tajweed or makharij.
