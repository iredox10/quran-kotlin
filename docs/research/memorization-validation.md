# Memorization validation and teacher pilot

The Android workshop supports Hafs using its own Madani reference. Selecting another
reading mushaf does not reinterpret memorization records as a different riwayah.
A selected “teacher” assessor records the user's declared assessment context; it is
not an authenticated teacher credential, ijazah or certification of recitation.

## Before broader release

Run a consenting pilot with a qualified Quran teacher and beginners, intermediate
students and established huffaz. Include short and long ayahs, page transitions,
similar passages, missed days and imported memorization. Compare independent
recitation on a later day with the app's predicted review needs. Record prompts,
self-corrections, omitted/changed words and transition failures separately. Teachers
must decide appropriate stops and acquisition portions; app-generated search
similarity is only a candidate, never a claim about Quran interpretation.

Evaluate at least these outcomes over several weeks:

- Independent recall on a later day, rather than number of taps or repetitions.
- Coverage and retention of older material while learning new material.
- Whether daily estimates fit actual time without producing excessive backlog.
- Whether prompted answers, corrections and listening inflate retained counts.
- Whether notes and confusion pairs lead to successful later repair.
- Accessibility of long Arabic passages, labels, dark mode and small screens.
- Cold offline use, process recreation, clock/time-zone changes and two-device merges.

Record implementation defects separately from teaching-policy feedback. Start with
adjustable defaults; the current recent-revision window and time estimates are
product heuristics, not religiously mandated or experimentally universal quotas.

## Data and current sync scope

Review evidence is append-only and account scoped in local SQLite. Existing scheduler
cards and manually marked memorization remain compatible. At most 500 recent local
events are sent per ordinary sync; older local events remain on the original device.
Only explicit new-learning assessments establish an acquisition date; reviewing imported material does not invent one. A restored device without the original acquisition event keeps that date unknown.
This is not a complete cloud archive of lifetime history. Server-side historical
pagination and equivalent web workshop UI are separate follow-up work. Ordinary
web sync must preserve the additional evidence section even though it has no UI for it.

Microphone permission is requested only when the user starts a recording. Recordings
are private local files, limited to three minutes per take and 50 MiB per account.
They stop when the screen goes into the background, are excluded from Android automatic backup and device transfer, can be replayed/deleted, and
are never uploaded by the sync engine. A recording reference in a review does not
make the audio available on a second device. App-data removal deletes local history
and recordings; copying private files is not a supported backup mechanism.

## Optional speech assistance: evaluation gate

Do not enable automatic grading by connecting a general transcription API. First
obtain consent for a teacher-labelled evaluation corpus and define permitted data
retention and deletion. Evaluate separately for each supported riwayah, age group,
accent, microphone quality, background noise and self-correction behavior. Use
speaker-disjoint train/development/test splits. Report missed errors, false alarms,
latency and confidence calibration per error class, especially omissions and
wrong-word substitutions. Generic word transcription does not establish tajweed,
makharij, waqf or teacher certification.

Provide explicit opt-in for any network audio processing, an independent offline
path and a clear uncertainty state. A teacher should adjudicate disagreements.
Automatic feedback stays disabled until the labelled evaluation and teacher review
justify it. No paid speech service is required for the current workshop.

## Release prerequisite

The matching backend change is committed in `quran-backend` as `4fdd786`. It caps
the event window at 500 without rewriting event IDs or scheduler cards. Deploy that
backend change before publicly releasing this Android feature. These implementation
commits do not deploy production or add the workshop interface to the web app.
