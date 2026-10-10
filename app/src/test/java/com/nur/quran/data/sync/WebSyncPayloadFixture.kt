package com.nur.quran.data.sync

/**
 * Web truth: quran-app/src/services/appwrite.js reads the sync document's
 * `stateData` as `slimStatePayload(getSyncPayload(useAppStore.getState()))` — the
 * `getSyncableState` keys (useAppStore.js) minus `lastSyncAt` and the derived
 * `planner`. These constants are those keys verbatim, with the values a real
 * account carries, so the round-trip tests assert data the web client can produce.
 */
internal const val WEB_SETTINGS_STATE = """{
  "theme": "dark",
  "translationId": 131,
  "reciterId": 3,
  "fontSize": 3,
  "translationFontSize": 2,
  "readingMode": true,
  "mushafId": "madani-standard",
  "arabicFontId": "amiri-quran",
  "arabicFont": "Amiri Quran",
  "tajweedEnabled": true,
  "wordTooltipBehavior": "translation",
  "tafsirId": 169,
  "dailyReadingGoal": 35,
  "intentionPromptEnabled": false,
  "customAudioBaseUrl": "https://audio.example.com/quran",
  "prayerSettings": { "activePrayers": ["Fajr", "Isha"], "readPreference": "before" },
  "prayerTimes": null,
  "location": null
}"""

/** Bookmark / collection / reading-history sections, in the web's field names. */
internal const val WEB_READING_STATE = """{
  "bookmark": { "verseKey": "2:255", "surahName": "Al-Baqarah" },
  "bookmarks": [
    { "verseKey": "2:255", "surahName": "Al-Baqarah", "chapterId": 2 },
    { "verseKey": "112:1", "surahName": "Al-Ikhlas", "chapterId": 112 }
  ],
  "collections": [
    {
      "id": "col-1",
      "name": "Tajweed notes",
      "items": [
        { "verseKey": "2:17", "surahName": "Al-Baqarah", "chapterId": 2 },
        { "verseKey": "114:5", "surahName": "An-Nas", "chapterId": 114 }
      ]
    }
  ],
  "recentlyRead": [
    { "chapterId": 112, "chapterName": "Al-Ikhlas", "verseKey": "112:4", "timestamp": 1778161500000 },
    { "chapterId": 2, "chapterName": "Al-Baqarah", "verseKey": "2:285", "timestamp": 1778158200000 }
  ],
  "readingSessions": [
    { "date": "2026-05-06", "duration": 1800, "type": "reading", "chapterId": 2, "timestamp": 1778161200000 },
    { "date": "2026-05-07", "duration": 420, "type": "memorizing", "chapterId": 112, "timestamp": 1778164800000 }
  ],
  "memorizedAyahs": ["112:1", "112:2", "112:3", "112:4", "2:255"],
  "memorizedSurahs": [112],
  "downloadedSurahs": [1, 112],
  "offlineDataStatus": "completed",
  "offlinePackStatus": { "1": { "status": "ready" } }
}"""
