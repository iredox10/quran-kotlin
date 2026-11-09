# Mobile scrolling and reader stability

## Problems fixed

- The native Quran renderer reassigned its spannable text and line metrics during
  unrelated playback/timer recompositions. It now changes text and metrics only
  when their inputs change, caches matching font instances and palette mappings,
  and keeps touch callbacks current without reconstructing spans.
- Wrap correction posted repeated layout work, including while a view was not
  ready. Corrections now coalesce on animation frames and retain stable spans.
  Actual width/font/text changes still trigger the bounded correction.
- Surah restoration had three competing effects, two keyed on every Success
  object. Translation/overlay updates could replay an opening ayah mid-scroll.
  One restoration now runs per navigation request, capturing the saved position
  before the scroll observer can overwrite it with the initial zero position.
- Planner reading-position tracking observed lazy scroll indices in composition.
  It now observes them in snapshotFlow, retaining debounced persistence without
  recomposing the whole screen for each item boundary.
- Prayer-time HTTP and planner-cache parsing now run off the UI thread. Prayer
  connections are released even after successful early returns.
- Startup no longer eagerly decodes the entire 11MB tafsir asset. Explicit tafsir
  opens still read bundled content locally. Verse and tafsir caches have separate
  locks so a tafsir parse does not lock the Quran reader's cache.
- Library bookmarks and collections are individually keyed lazy rows rather than
  two giant eager list items. Phone/tablet column layouts and existing actions
  remain available.
- Memorization counts are indexed once per saved-set update; weekly analytics
  constructs its date lookup once instead of once for each reading session.
  Home statistics aggregation runs on a background dispatcher.

## Verification

- Unit suite: 528 tests, no failures; one existing live-service test skipped.
- Device regressions cover 30 callback/color updates without text or layout
  replacement; fresh word-tap actions; actual text/font updates; Arabic glyph
  silhouette preservation; metadata updates preserving a settled reading position;
  planner prayer resume and serialized progress persistence; offline content and
  first-open bundled tafsir with content HTTP requests blocked.
- Emulator smoke checks: home, memorization, planner dashboard, analytics, profile,
  and library (12 existing collections), scrolling both directions.
- Initial test synchronization failures were corrected to await Compose frames
  and settled flings rather than treating main-looper idleness as render completion.
- Debug frame samples collected while Gradle and the emulator shared the host are
  not a controlled performance benchmark. No frame-rate or percentage improvement
  is claimed. Smoothness on the physical phone still needs a release-build check.

Build/device logs are retained under ignored `build/scroll-*.log`.
