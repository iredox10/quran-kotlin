# Interface languages

Quran Nur bundles interface translations for 21 languages: English (`en`), Arabic
(`ar`), French (`fr`), Spanish (`es`), Portuguese (`pt`), German (`de`), Italian
(`it`), Russian (`ru`), simplified Chinese (`zh-Hans`), Japanese (`ja`), Korean
(`ko`), Hindi (`hi`), Bengali (`bn`), Urdu (`ur`), Indonesian (`id`), Malay (`ms`),
Turkish (`tr`), Persian (`fa`), Swahili (`sw`), Hausa (`ha`), and Yoruba (`yo`).
The picker also offers the system default.

Change the interface language in **Settings → General → App language**. Android
13 and later use the platform's per-app locale setting; older versions persist
an app preference and recreate the Activity with a localized context. Arabic,
Persian and Urdu use RTL layouts. Navigation arrows mirror automatically, while
Quran text keeps its own Arabic reading direction. Locale recreation restores the
navigation state rather than replaying an earlier deep link.

The choice applies to app navigation, home, reader actions, settings, account
forms, library, memorization, statistics, planner/history, dialogs and
accessibility labels. Reading times use Android ICU/CLDR grammatical relative-time formats, and weekday
labels and display dates use the chosen locale;
stored date keys, routes, preferences, API identifiers and sync fields remain
stable. The UI preference is local to the device. It does not change the Quran
script, selected verse translation, tafsir source, reciter, or user-written notes.
Language switching does not require a network connection. Generated translations need native-speaker review before they should be described as professionally localized.

## Resource ownership

- `values/` contains the canonical English strings.
- `ui_strings.xml` contains shared app UI and parameterized messages.
- `settings_strings.xml`, `planner_strings.xml`, and
  `mushaf_picker_strings.xml` keep their component-specific copy together.
- Locale directories bundle translated resources; simplified Chinese uses
  Android's `values-b+zh+Hans` qualifier.
- `values-ar/reviewed_strings.xml` contains reviewed Arabic interface and Quran
  terminology. Other hand-written Arabic files also override generated entries.
- Hausa and Yoruba have hand-written core settings/planner labels in `reviewed_strings.xml`.
- `generated_strings.xml` files contain build-time assisted translations. There
  is no runtime translation service or API key in the app.

## Updating translations

After editing English copy, run the offline coverage and format-token audit:

```sh
python scripts/generate_app_locales.py --check
```

To fill missing translations, run the generator (requires network access):

```sh
python scripts/generate_app_locales.py
# Or update only selected languages:
python scripts/generate_app_locales.py --languages ar fr ha yo
```

The generator caches by source text, preserves existing hand-written resources,
and protects numbered Android placeholders, escaped newlines, and selected Quran
proper names. Keep placeholder positions and types intact when editing a
translation. For a reviewed override, put the entry in a separate locale file and
remove the same key from that locale's generated file; Android disallows duplicate
resource names. A later generation pass automatically excludes reviewed keys.

Always localize display labels at their UI boundaries. Do not translate stable
preference filenames/keys, navigation routes, prayer IDs, storage keys, enum IDs,
or Quran/translation content. Resource calls in non-Compose callbacks must use a
captured localized `Context.getString`; `stringResource` belongs in composition.
