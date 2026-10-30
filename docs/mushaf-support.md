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

## IndoPak, visible but unavailable

The old option called API mushaf 3 a "16-line" edition. The official page table distinguishes generic IndoPak (3, 604 pages), IndoPak 15-line (6, 610 pages), and IndoPak 16-line (7, 548 pages). The app does not contain a complete alternate 610/548-page lookup/word pack or compatible font. Pretending these layouts are the same would corrupt page navigation and planner progress.

The bundled legacy `text_indopak` uses private-use characters U+E003, U+E004, U+E01A, U+E01B, U+E01C, U+E01E, U+E01F, U+E021 and U+E022. None of the bundled fonts contains these nine characters. They must not be deleted or guessed: several encode Quranic pause/annotation signs. An unsupported saved IndoPak selection therefore resolves to the bundled Madani Standard before rendering; the picker keeps the unavailable choice visible with its reason rather than displaying fabricated preview text.

For a faithful future release, Quran Foundation specifies `text_indopak_nastaleeq` and this exact compatible font:

https://verses.quran.foundation/fonts/quran/hafs/nastaleeq/indopak/indopak-nastaleeq-waqf-lazim-v4.2.1.ttf

The official font-rendering guide allows integrated font caching/bundling when the app owner maintains an active Quran Foundation Developer Console account and credits Quran Foundation. The app owner confirmed that they do not have an active Developer Console account, so those conditions are not currently met; public API access alone does not establish entitlement. The font was inspected transiently for glyph coverage and has not been added to the APK. Its embedded older restrictive license is not presented as a blanket prohibition: the current official developer terms are the authority for qualifying use. Enable IndoPak only after establishing those conditions and shipping the matching complete text/layout, not merely by installing an arbitrary Nastaliq font.

## Rendering and cache rules

The preview must read the same bundled source field and use the same selected edition/font/Tajweed shaping path as the real reader. Never synthesize a printed-page preview from placeholder bars. Unavailable editions get an explanatory non-Arabic placeholder instead of a misleading sample.

A selection change clears in-memory chapter and page caches. Page/Planner context is preserved: changing an edition in the planner reloads the current page, not its entire chapter. The dedicated Page view observes the shared setting so back-stack screens cannot keep a stale script or font. All selectable presentations continue to use the bundled Quran before any network refresh.

The web picker uses the same bundled Al-Fatihah 2–3 word records, the normal reader text selector, and existing Tajweed CSS. Its Uthmani default font is a WOFF2 conversion of the Android Amiri regular font; the Amiri Project’s [OFL 1.1 license](https://github.com/aliftype/amiri/blob/main/OFL.txt) is included at `public/fonts/Amiri-OFL.txt`. Both preview font faces are served locally and precached for offline use. Browser verification blocked external network requests and confirmed all three available previews, Tajweed spans, disabled IndoPak and selection persistence.
