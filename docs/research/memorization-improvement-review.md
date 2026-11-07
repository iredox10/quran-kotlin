# Quran Nur memorization: source audit and improvement proposal

Research date: 2026-11-07. Scope: current Android source, Quran teaching practice, learning research, FSRS documentation and current product documentation. This is a proposal; no memorization behavior was changed during the review.

## Recommendation

Build a guided, assessable cycle: prepare accurately → practise manageable portions → recite without cues → correct mistakes → recall after a delay → maintain continuous revision. Reuse the existing audio, masking, scheduler and revision screens. Add reliable evidence of independent recall before adding more progress counters.

There is no single universal Quran memorization quota, repeat count or promotion threshold. School practices vary. General memory research supports retrieval and spacing, but does not validate a particular FSRS retention target or school timetable for complete Quran recitation. Numerical defaults below are product proposals to pilot with qualified teachers, not religious or experimentally established standards.

## What is already implemented

- `HifdhReaderScreen.kt`: one/three/five-ayah or page chunks; visible/blur/word/first-letter modes; repeated recitation, range loops and delays; optional translation; memorized toggles.
- `HifdhTestModal.kt`: randomly selected individual-ayah recall, reveal, four self-ratings and previous-ayah cues for weak transitions.
- `FsrsScheduler.kt`: FSRS-6 port aligned with web ts-fsrs defaults; short learning/relearning steps and 90% requested retention.
- `HifdhQueues.kt`: Sabaq/Sabqi/Manzil buckets and due filtering.
- `HifdhStore.kt` and `SurahViewModel.kt`: goals, latest per-ayah card/strength, memorized sets and transition flags, with web-compatible persistence/sync.

## Gaps confirmed in source

1. **Prompted recall counts as scheduler success.** `HifdhTestModal.kt` maps “Needed prompt” to Hard (2). FSRS interprets Hard as recalled correctly with difficulty; failing to recall without the answer should not be represented as a successful independent test. This can produce inappropriate intervals. Separate cue use from latency/hesitation and give the scheduler honest outcomes.
2. **Revision buckets do not measure acquisition age.** `classifyHifdhQueue()` puts new/learning/reps<3 cards in Sabaq, then all remaining ayahs in the most recent memorization surah in Sabqi. An old ayah can become “recent” just because that surah was last opened, while recent material in another surah becomes Manzil. Add learned-at and last-independent-pass timestamps, configurable recent-window policy, and explicit learning sessions.
3. **Memorized status is a toggle.** `toggleMemorizedAyah()` and full-surah toggles do not require a recall assessment or create a successful review record. Preserve manual import of previously learned material, but distinguish self-declared coverage from tested retention and teacher verification.
4. **Tests are random one-pass samples.** The modal removes a tested key and completes after the queue is exhausted; a failed item is not reinserted into the current test. FSRS can schedule later relearning, but the session does not guide the learner through correcting and retesting that failure.
5. **Strength describes only the latest rating.** `HifdhHistoryEntry` retains the current card, lastReviewed and a coarse strength; Easy becomes strong, Again weak, others medium. It is not a longitudinal evidence trail or pronunciation assessment.
6. **No structured error/recording/teacher workflow was found in the inspected memorization components.** A transition flag cannot explain omitted words, substitutions, confusing parallels, prompting or tajweed corrections.
7. **Due counts use a captured time.** MemorizeScreen computes the due queues inside remember keyed to memorized/history/session changes. Due status can stay stale while the app is idle past a due time; refresh on clock boundaries/resume.
8. **Edition scope matters.** Ordinary Warsh/Qaloon/IndoPak reading now supports separate native navigation, but the current memorization model remains Hafs-keyed. Do not advertise cross-riwayah memorization support until text, numbering, audio and scheduling identities are edition-aware.

## Priority 1: trustworthy learning and revision

### Daily guided session

Show one Start Today action with named sections: New lesson (Sabaq), Recent revision (Sabqi), Older revision (Manzil). Preserve teacher-defined quantities. Determine recent revision by the acquisition window or selected current portion, not last-opened surah. Protect a rotation through older material even when individual cards are not due; use FSRS to prioritise weak items inside the allotted review budget.

Let users specify available minutes and days. Estimate realistic work from measured practice duration/portion length, not ayah counts alone. Reduce optional new material when old review exceeds the budget. Provide a gentle recovery mode after missed days, with no loss of previously learned status and an explicit backlog indication.

### Guided recall and promotion

Offer Listen/read → Recite with limited hints → Recite hidden → Check/correct → Retest after intervening practice → Delayed test. Use manageable phrases for long ayahs; boundaries need Quran-teacher review for suitable waqf/ibtida. Word/phrase counts are only workload estimates, never a license to invent recitation stops.

Proposed states: Learning, Recalled today, Retained on later days, Needs repair; Teacher checked is a separate provenance field. A self-declared imported ayah should be labelled as such, not automatically teacher verified. Do not require an arbitrary universal number of repeats. A pilot may try two independent clean recalls and later-day checks, but the threshold must be configurable and tested.

### Honest rating rubric

- Again: could not complete independently, wrong/missing text, or required an answer-revealing prompt.
- Hard: correct independently, with substantial effort/hesitation.
- Good: correct independently with ordinary effort.
- Easy: correct independently with little effort; does not alone prove durable mastery.

Track displayed cue/hint level and assessment source. A previous-ayah cue can be a valid **transition test**; record that test separately from an unprompted ayah-start test. Keep the audio paused and the answer hidden during independent recall. After grading, let learners inspect the answer as corrective feedback. In correction practice, requeue failures after other material; do not mistake immediate echoing for a delayed retention test.

## Priority 2: Quran-specific robustness

- **Rabt:** dedicated last-phrase→next-ayah, ayah→ayah and page→page drills. Test both arbitrary starts and uninterrupted passages; never replace sequential recitation with shuffled single-ayah cards alone.
- **Mutashabihat:** reviewed pairs/groups of similar passages, exact differences highlighted, with surrounding context and optional translation. Build candidates offline from the verified text, then curate with a teacher. Do not generate mnemonic explanations or theological distinctions as authoritative content.
- **Mistake notebook:** word/range, error type, intended text, what was recited, prompt usage, timestamp, source, optional local audio and correction note. Prioritise recurring issues without assuming every Again means a transition failure. Show unresolved mistakes and clean later recalls.
- **Tasmiʿ/listener mode:** hidden text for the learner, visible reference for the listener, tap to mark an error, replay and notes. Allow explicit teacher sign-off separately from self-assessment. A basic offline listener mode is valuable without a teacher marketplace or online account.
- **Stable visual reference:** pin the learning mushaf within a memorization plan. Flowing adjustable text must not be presented as identical fixed printed lines. Supported edition+riwayah+text version+verse identity belong in learning records.
- **Understanding:** optional local meaning/tafsir before practice and brief context reminders; these support the memorization workflow, but no promise that one specific meaning-based routine outperforms all alternatives.

## Priority 3: feedback, history and optional speech assistance

Store an append-only review-event log alongside the scheduler snapshot: event ID, edition-qualified item/portion, time, practice/test mode, cues, rating, errors, duration and assessor. Use idempotent sync/replay with event IDs, clock handling and migration of existing cards. The current latest-card map alone cannot reconstruct a credible full practice history.

Show first-attempt unaided recall, later-day recall, unresolved errors, review coverage/oldest untouched portion, workload, and **estimated** recall probability. Separate reading/listening minutes from memorization tests. Report unavailable evidence as unknown; do not equate total minutes, streaks, a green checkbox, or an FSRS probability with certified recitation.

Start speech features with optional local recording and reference playback. Later, evaluate speech alignment/error detection against consenting, teacher-labelled recordings, including accents, noise, self-corrections, repeated words, children where appropriate, and each supported riwayah. Measure false accusations and missed errors; permit dismissal and “uncertain” outcomes. Automatic word recognition is not evidence of complete tajweed/makhraj competence.

All core practice, schedules, error notes, bundled text and downloaded audio should operate offline. Download extra packs explicitly. Keep recordings local by default; obtain separate consent for cloud analysis/sharing, show pending uploads, provide deletion and a storage cap. Avoid synchronizing audio binaries through the compact state JSON. School/guardian features need their own consent and access design.

## Suggested delivery order and validation

1. Repair ratings, due refresh, acquisition metadata and event logging; migrate without deleting existing learned material. Test web/mobile sync, duplicate events, time zones and clock/resume boundaries.
2. Build Start Today, correction/retest, delayed checks and realistic recovery scheduling; test with adult beginners and experienced memorizers.
3. Add transition/continuous passage tests, error notebook and offline recording/listener assessment.
4. Add curated mutashabihat, mastery reporting and edition-aware memorization.
5. Pilot optional speech feedback after a teacher-reviewed accuracy evaluation; retain human verification.

Evaluate learning with delayed unaided recitation, including page transitions and random starts. Compare baseline and new routines over multiple weeks, not only same-session completion. Assess usability, hint dependence, review burden and adherence as well as recall. Specific success thresholds should be agreed with teachers and revised after the pilot.

## Evidence and limits

- [Roediger & Karpicke, 2006, original retrieval-practice experiment](https://pubmed.ncbi.nlm.nih.gov/16507066/): delayed retention benefits from testing versus restudy in the studied material. Supports testing independent recall; does not establish a complete Quran curriculum.
- [Carpenter, Pan & Butler, 2022, Nature Reviews Psychology](https://doi.org/10.1038/s44159-022-00089-1): review of spacing/retrieval and learners' strategy judgments. [Author manuscript](https://sc-pan.github.io/pdf/NRP_2022.pdf).
- [Australian Education Research Organisation: spacing and retrieval practice](https://www.edresearch.edu.au/guides-resources/practice-guides/spacing-and-retrieval-practice-guide-full-publication): practical educational guidance; school learning is not identical to Quran oral assessment.
- [Online Islamic Institute: its Sabaq/Sabqi/Manzil teaching practice](https://onlineislamicinstitute.com/hifz-revision-schedule/): describes recent-vs-old revision, rotation and teacher-adjusted new portions. A provider's practice account, not a controlled trial or universal religious standard.
- [Madrasat El Quran: its revision system](https://madrasatelquran.com/sabaq-sabqi-manzil/): describes listener assessment, mistake tracking and review rotation. Commercial teaching guidance; numerical quotas vary.
- [Anki: answer buttons](https://docs.ankiweb.net/manual/studying): Hard is a successful difficult recall; Again is failure. [FSRS options](https://docs.ankiweb.net/manual/deck-options): desired retention/workload trade-offs, correct ratings and optimisation from review history. The app's 90% target is not “90% Quran mastery”.
- [Tarteel: current tashkeel detection](https://support.tarteel.ai/en/articles/12414420-how-to-turn-on-tashkeel-mistake-detection): current documentation includes diacritic detection and acknowledges missed errors. Its 2022 launch article is outdated for feature comparison.
- [Tarteel: supported riwayat](https://support.tarteel.ai/en/articles/12414396-does-tarteel-support-different-quran-riwayat-qira-at): describes Hafs support; demonstrates why alternate-recitation validation cannot be assumed from text display support.
- [Tarteel: mutashabihat tools](https://support.tarteel.ai/en/articles/17239528-how-to-find-similar-verses-and-phrases-mutashabihat): an existing implementation pattern, not proof of comparative learning effectiveness.

Research was conducted through source review and published material; no live teacher interviews, learner study or new on-device usability session was conducted for this proposal.
