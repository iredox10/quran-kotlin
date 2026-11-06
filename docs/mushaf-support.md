# Offline mushaf support

Reviewed 30 October 2026 against Quran Foundation's primary documentation:

- [Font rendering, compatible text fields and font bundling terms](https://api-docs.quran.com/docs/tutorials/fonts/font-rendering/)
- [Page layouts and page counts](https://api-docs.quran.com/docs/tutorials/fonts/page-layout/)
- [QuranWBW's IndoPak source project and font credits](https://github.com/marwan/indopak-quran-text)

## Available in this Android build

| Choice | Quran text | Bundled default font | Navigation | Display |
| --- | --- | --- | --- | --- |
| Madani Standard | `text_qpc_hafs` | KFGQPC Hafs | 604 pages | Flowing, adjustable Unicode |
| Madani Tajweed | `text_qpc_hafs` plus the existing word-aligned Tajweed source | KFGQPC Hafs | 604 pages | Same Arabic base, colored rules |
| Uthmani Hafs | `text_uthmani` | Amiri Quran | 604 pages | Flowing, adjustable Unicode |

These are Hafs text presentations, not different qira'at. Font changes do not turn Hafs into Warsh or Qalun. Existing downloaded Quran text, verse keys, bookmarks and planner page assignments retain their canonical 604-page navigation.

QCF V1/V2/V4 are glyph-font editions: each page requires the matching font and `code_v1`/`code_v2` data. The prior Tajweed preset incorrectly described its Unicode renderer as an exact 15-line QCF facsimile. It now explicitly uses flowing QPC text. Unicode lines may wrap with screen width or font size; no exact printed line count is promised.

The four bundled general Quran fonts KFGQPC Hafs, Amiri, Noto Naskh and Scheherazade cover every non-formatting character in the bundled QPC/Uthmani word packs. `uthman_taha_naskh.otf` lacks Alef Wasla and many Quranic annotation marks, so it is no longer offered as a fully compatible Quran font. Its legacy display-name mapping is retained and incompatible saved choices are coerced to the selected edition's complete default.

## Legacy IndoPak text compatibility

The old option called API mushaf 3 a "16-line" edition. The official page table distinguishes generic IndoPak (3, 604 pages), IndoPak 15-line (6, 610 pages), and IndoPak 16-line (7, 548 pages). The app does not contain a complete alternate 610/548-page lookup/word pack or compatible font. Pretending these layouts are the same would corrupt page navigation and planner progress.

The bundled legacy `text_indopak` uses private-use characters U+E003, U+E004, U+E01A, U+E01B, U+E01C, U+E01E, U+E01F, U+E021 and U+E022. None of the bundled fonts contains these nine characters. They must not be deleted or guessed: several encode Quranic pause/annotation signs. An unsupported saved IndoPak selection therefore resolves to the bundled Madani Standard before rendering; the generic text option remains disabled. The complete 13-line printed IndoPak edition is available through a separate explicit download and reader, described below.

For integrating a Unicode IndoPak option into the shared Hafs reader, Quran Foundation specifies `text_indopak_nastaleeq` and this exact compatible font:

https://verses.quran.foundation/fonts/quran/hafs/nastaleeq/indopak/indopak-nastaleeq-waqf-lazim-v4.2.1.ttf

The official font-rendering guide allows integrated font caching/bundling when the app owner maintains an active Quran Foundation Developer Console account and credits Quran Foundation. The app owner confirmed that they do not have an active Developer Console account, so those conditions are not currently met; public API access alone does not establish entitlement. The font was inspected transiently for glyph coverage and has not been added to the APK. Its embedded older restrictive license is not presented as a blanket prohibition: the current official developer terms are the authority for qualifying use. Enable IndoPak only after establishing those conditions and shipping the matching complete text/layout, not merely by installing an arbitrary Nastaliq font.

## Rendering and cache rules

The preview must read the same bundled source field and use the same selected edition/font/Tajweed shaping path as the real reader. Never synthesize a printed-page preview from placeholder bars. Separate editions use actual Quran previews and identify their source.

A selection change clears in-memory chapter and page caches. Page/Planner context is preserved: changing an edition in the planner reloads the current page, not its entire chapter. The dedicated Page view observes the shared setting so back-stack screens cannot keep a stale script or font. Normal Quran reading uses bundled and downloaded content only. Selected translation packs, word packs and Tajweed do not trigger HTTP fallbacks; extra downloads or manual refresh require a user action. Upcoming pages are prepared locally in memory.

The web picker uses the same bundled Al-Fatihah 2–3 word records, the normal reader text selector, and existing Tajweed CSS. Its Uthmani default font is a WOFF2 conversion of the Android Amiri regular font; the Amiri Project’s [OFL 1.1 license](https://github.com/aliftype/amiri/blob/main/OFL.txt) is included at `public/fonts/Amiri-OFL.txt`. Both preview font faces are served locally and precached for offline use. Browser verification blocked external network requests and confirmed all three available previews, Tajweed spans, disabled IndoPak and selection persistence.

## November 2026: native Warsh/Qaloon readers and authentic IndoPak preview

Warsh and Qaloon are available from the mushaf picker as dedicated offline readers.
Each bundles its complete 6,214-ayah KFGQPC corpus and its unmodified matching font.
The native readers have their own 604-page navigation, surah chooser and saved position.
They do not write into the Hafs verse database or attach Hafs audio, translations,
tajweed rules, bookmarks or planner completion to incompatible native verse numbers.
See `app/src/main/assets/data/mushafs/SOURCES.md` for provenance, validation and licenses.

Warsh and Qaloon flow continuously across ayahs, with native paired fonts, justified RTL text, adjustable type, surah separation and an unnumbered basmala (taken verbatim from each corpus at 27:30), except at At-Tawbah. They use native page membership without fabricating word-level printed line breaks. Ayahs spanning two pages remain intact on both pages.

## Explicit IndoPak download

The picker offers the complete Quraan Al Majeed **13-line printed edition** linked by the [DigitalKhatt font author](https://github.com/DigitalKhatt/indopakfont). Download source: [Internet Archive original PDF](https://archive.org/details/QuranAl-majeed-13Line/1-QuraanAlMajeed13Line-Complete/). This is a separate printed edition, not an arbitrary font applied to the incompatible bundled private-use script. It does not require a Quran Foundation developer account.

The user explicitly downloads 34,113,171 bytes. Downloads resume partial bytes using HTTP Range, enforce the expected size and Archive SHA-1 `3819c14fe04efa0596a159d4a6f277570453ec41`, validate PDF page count, and atomically install into private files. Incomplete files cannot be opened. Android PdfRenderer renders local pages; page navigation, zoom, page jump and saved position require no network. Its own pagination stays independent of Madani planner progress. The preview is rendered directly from the downloaded original Al-Fatihah page, preserving its printed letters and line breaks. The reader exposes printed Quran pages 2–848 (PDF pages 4–850); cover/index/appendix pages are not treated as Quran. Adjacent pages are rendered locally into a small bitmap cache.

Only the Al-Fatihah preview is bundled. No complete PDF is bundled or redistributed in the APK; the user downloads the original from its source. The installed edition and incomplete download live in app-private storage. Uninstalling/clearing app data removes them.

### Reading gestures

IndoPak opens as an immersive page, with scanner whitespace trimmed from its local bitmap without changing the printed letters. The page fits the viewport with its original proportions; there is no bottom navigation panel. Tap the page to show or hide back, page-jump and zoom controls. Swipe right for the next printed page and left for the previous. At increased zoom, dragging pans the enlarged page instead of turning it. First/last pages are bounded. Warsh and Qaloon use the same page-swipe direction alongside their existing footer; vertical scrolling remains independent.
