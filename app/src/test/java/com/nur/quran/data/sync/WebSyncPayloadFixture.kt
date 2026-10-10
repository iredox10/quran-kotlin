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

/**
 * Hifdh sections. `hifdhHistory` values carry the ts-fsrs card as the web persists it
 * (snake_case keys, ISO `due` / `last_review`); `hifdhGoals` dates are `YYYY-MM-DD`.
 */
internal const val WEB_HIFDH_STATE = """{
  "hifdhHistory": {
    "2:255": {
      "card": {
        "due": "2026-05-20T09:00:00.000Z",
        "stability": 12.4,
        "difficulty": 5.1,
        "elapsed_days": 3,
        "scheduled_days": 12,
        "reps": 3,
        "lapses": 1,
        "learning_steps": 0,
        "state": 2,
        "last_review": "2026-05-08T09:00:00.000Z"
      },
      "lastReviewed": 1778154000000,
      "strength": "strong"
    },
    "112:1": {
      "card": {
        "due": "2026-05-14T09:00:00.000Z",
        "stability": 4.1,
        "difficulty": 6.7,
        "elapsed_days": 1,
        "scheduled_days": 4,
        "reps": 2,
        "lapses": 0,
        "learning_steps": 1,
        "state": 1,
        "last_review": "2026-05-10T09:00:00.000Z"
      },
      "lastReviewed": 1778413200000,
      "strength": "medium"
    }
  },
  "hifdhGoals": [
    { "id": 1778300000000, "targetType": "surah", "targetId": 112, "targetDate": "2026-06-30", "createdAt": 1778300000000 },
    { "id": 1778300000001, "targetType": "ayah", "targetId": "2:255", "targetDate": "2026-05-31", "createdAt": 1778300000001 }
  ]
}"""

/** Planner sections: plans with per-day progress/assignments, plus their keyed extras. */
internal const val WEB_PLANNER_STATE = """{
  "planners": [
    {
      "id": "plan-1778200000000",
      "createdAt": "2026-05-07T06:00:00.000Z",
      "unitType": "ayah",
      "title": "Al-Ikhlas review",
      "durationDays": 3,
      "startDate": "2026-05-07",
      "startUnit": 1,
      "endUnit": 4,
      "assignmentProgress": { "1": 4, "2": 1 },
      "assignmentCompletedItems": { "1": [1, 2, 3, 4] },
      "assignmentCompletedAt": { "1": "2026-05-07T07:00:00.000Z" },
      "completedDays": [1],
      "assignments": [
        { "dayNumber": 1, "date": "2026-05-07", "title": "Ayahs 1-4", "updatedAt": 190 },
        { "dayNumber": 2, "date": "2026-05-08", "title": "Ayahs 5-6", "updatedAt": 80 }
      ],
      "excludeDays": [5]
    }
  ],
  "activePlannerId": "plan-1778200000000",
  "archivedPlanners": [],
  "plannerReflections": {
    "plan-1778200000000": {
      "1": { "text": "First pass felt solid", "createdAt": "2026-05-07T08:00:00.000Z" },
      "2": { "text": "Verse 5 slipped", "createdAt": "2026-05-08T08:00:00.000Z" }
    }
  },
  "plannerBookmarks": {
    "plan-1778200000000": [
      { "verseKey": "112:1", "surahName": "Al-Ikhlas", "note": "tarteel check", "createdAt": "2026-05-07T09:00:00.000Z" }
    ]
  },
  "plannerSessionTimers": {
    "plan-1778200000000": {
      "1": { "startedAt": "2026-05-07T06:10:00.000Z", "totalSeconds": 1500 }
    }
  }
}"""

/** Onboarding / page-visit / misc device-shared keys. */
internal const val WEB_ACCOUNT_STATE = """{
  "completedTours": ["home-tour", "surah-tour"],
  "dismissedCoachmarks": ["planner-coach"],
  "dismissedGestureTips": ["swipe-tip"],
  "pageVisitCounts": { "home": 12, "surah": 45, "analytics": 3, "planner": 7 },
  "saukaProgress": { "sauka-1": 12 },
  "pomodoroProfiles": [
    { "id": "classic", "name": "Classic", "focusMinutes": 25, "breakMinutes": 5 }
  ],
  "activePomodoroProfileId": "classic",
  "pomodoroHistory": [],
  "pomodoroMode": "focus",
  "pomodoroIsRunning": false,
  "pomodoroSecondsLeft": 1500,
  "pomodoroCompletedFocusCount": 4,
  "audioSettings": {
    "startRange": null, "endRange": null, "reciterId": 3, "repeatSelection": 1,
    "repeatAya": 1, "delayBetweenAyas": 0, "playbackSpeed": 1.0, "scrollWhilePlaying": true
  }
}"""

private val MAP_TYPE = object : com.google.gson.reflect.TypeToken<Map<String, Any?>>() {}.type

/** Mirrors the web's `JSON.stringify`, which keeps `null` members. */
private val GSON: com.google.gson.Gson = com.google.gson.GsonBuilder().serializeNulls().create()

/** The whole getSyncableState-shaped payload, as one map (numbers arrive as Double). */
fun webState(): Map<String, Any?> = listOf(
    WEB_SETTINGS_STATE,
    WEB_READING_STATE,
    WEB_HIFDH_STATE,
    WEB_PLANNER_STATE,
    WEB_ACCOUNT_STATE,
).map { section -> GSON.fromJson<Map<String, Any?>>(section, MAP_TYPE) }
    .reduce { acc, section ->
        LinkedHashMap(acc).apply { putAll(section) }
    }

/** The exact string the web client stores in the document's `stateData` attribute. */
fun webStateData(): String = GSON.toJson(webState())

/** Parses a JSON string the way the sync engine does (null on unparseable input). */
fun parseState(json: String?): Map<String, Any?>? = json?.takeIf { it.isNotBlank() }
    ?.let { runCatching { GSON.fromJson<Map<String, Any?>>(it, MAP_TYPE) }.getOrNull() }

/** Numbers in both payloads are compared as Double: Gson hands back Double for JSON numbers. */
fun numberOf(value: Any?): Double? = (value as? Number)?.toDouble()
