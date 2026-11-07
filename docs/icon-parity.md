# Web and Android icons

Android's `NurIcons` is generated from the **installed web app's Lucide exports**, currently `lucide-react` 0.577.0. The web repository is the visual reference. Paths are copied verbatim; SVG circles, ellipses, lines, polygons and rounded rectangles are converted to equivalent paths without moving their centers. Every icon uses the web's 24-unit viewport, 2-unit round stroke and round joins. Filled play/pause/bookmark/stop variants use that same geometry and stroke with a solid fill.

The shared library covers the header, settings, home, reading actions, navigation and player. Material back/search/close/chevron substitutions have been replaced. The audio player uses the web's SkipBack, SkipForward and Settings2 glyphs, a filled stop square and the matching icon sizes. Header glyphs remain 18dp, bottom navigation 22dp, player transport/settings/close 16dp, play/pause 18dp and stop 14dp. The play glyph's 1dp optical shift matches the web's centered flex item with a 2px left margin. Theme toolbar icons stay neutral in both themes. Existing RTL mirroring for navigation arrows is retained.

Regenerate after intentionally updating the web icon dependency:

```sh
node scripts/sync-web-icons.cjs ../quran-app
node scripts/sync-web-icons.cjs ../quran-app --check
```

The generator also writes the independent original SVG-node fixture to Android test assets and includes the upstream ISC/Feather MIT license at `assets/licenses/lucide-ISC.txt`. Android does not load JavaScript, web assets or remote icon fonts at runtime.

`WebIconRenderingTest` renders every Android icon at 72 physical pixels with fixed density/layout direction and records its screen bounds. Pull `cache/lucide-native-grid.png` and `cache/lucide-native-bounds.json` from the app into `build/`, then run:

```sh
python3 scripts/compare-web-icon-rendering.py
```

This rasterizes the web's original SVG primitives with an independent SVG renderer and compares each icon to Android's actual screen output. It allows engine antialiasing differences (mean channel error at most 2.5% and ink bounds at most 1px apart). All 73 icons passed; the largest mean channel difference was 1.203%. The SVG-node check is exact; the raster tolerance does not permit reshaping or recentering icons.
