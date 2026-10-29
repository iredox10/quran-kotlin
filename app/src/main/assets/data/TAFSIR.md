The Ibn Kathir bundle stores commentary for a group of ayahs on its first row;
following rows in the group are empty. `completeBundledTafsir` resolves those
references in memory within the same chapter and edition. It does not duplicate
the commentary in the asset or copy across surah boundaries.

Grouping reference: https://api-docs.quran.com/docs/api/field-reference/
Example: https://quran.com/al-baqarah/9/tafsirs/en-tafisr-ibn-kathir (2:8–9).

The original bundle has no commentary for Al-Fil (105:1–5).
`tafsir_ibn_kathir_supplement.json` provides an explicitly labeled, short offline
summary based on https://quran.com/al-fil/1/tafsirs/en-tafisr-ibn-kathir,
with a link to the full commentary. The summary is not presented as the original
translation. Downloaded full commentary takes precedence over this fallback.
