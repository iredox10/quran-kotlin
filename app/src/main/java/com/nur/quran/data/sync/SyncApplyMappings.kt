package com.nur.quran.data.sync

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.nur.quran.data.mushaf.Mushaf
import com.nur.quran.data.mushaf.fontIdToName
import com.nur.quran.data.planner.PRAYER_NAMES

/**
 * Web → Android key mappings for the sync APPLY side.
 *
 * These are the inverse of the `SyncSnapshot.kt` Android → web helpers: they
 * translate the flat keys web's `getSyncableState` emits
 * (quran-app/src/store/useAppStore.js L860-918) into the values Android's
 * stores keep. Every function is pure (no Context / prefs / DAO) so it is JVM
 * unit testable — see `SyncApplyMappingsTest`; `SyncService` only wires the
 * results into prefs and Room.
 *
 * Value coercion is deliberately lenient because remote values reach us either
 * as JSON scalars (parsed `stateData`) or as strings (Appwrite attributes), and
 * a value we cannot read must be dropped rather than written as a default.
 */

/** Web `ARABIC_FONT_OPTIONS` ids (quran-app/src/config/mushaf.js). */
private val WEB_ARABIC_FONT_IDS = setOf(
    "kfgqpc-hafs", "uthman-taha-naskh", "amiri-quran",
    "noto-naskh-arabic", "scheherazade-new"
)

/** Android `word_tap_behavior` values (SettingsDrawer "Word Hover Action"). */
private val ANDROID_WORD_TAP_BEHAVIORS = setOf("tajweed", "translation", "none")

/** Web `prayerSettings.readPreference` values. */
private val WEB_READ_PREFERENCES = setOf("before", "after", "split")

/** Android's font-scale slider range (SurahViewModel: ±0.25 steps, 0.5..3.0). */
private const val ANDROID_FONT_SCALE_MIN = 0.5f
private const val ANDROID_FONT_SCALE_MAX = 3.0f

/** Web font index whose scale equals Android's neutral 1.0, and the scale per step. */
private const val WEB_FONT_INDEX_ANCHOR = 2
private const val WEB_FONT_INDEX_SCALE_STEP = 0.25f

private val gson = Gson()
private val mapType = object : TypeToken<Map<String, Any?>>() {}.type
private val listType = object : TypeToken<List<Any?>>() {}.type

// ------------------------------------------------------------- value coercion ---

/** Non-blank strings only, so an empty remote scalar never overwrites local state. */
internal fun asString(value: Any?): String? = (value as? String)?.trim()?.takeIf { it.isNotEmpty() }

internal fun asBoolean(value: Any?): Boolean? = when (value) {
    is Boolean -> value
    is String -> when (value.trim().lowercase()) {
        "true" -> true
        "false" -> false
        else -> null
    }
    else -> null
}

internal fun asInt(value: Any?): Int? = when (value) {
    is Number -> value.toInt()
    is String -> value.trim().toIntOrNull() ?: value.trim().toDoubleOrNull()?.toInt()
    else -> null
}

internal fun asFloat(value: Any?): Float? = when (value) {
    is Number -> value.toFloat()
    is String -> value.trim().toFloatOrNull()
    else -> null
}

internal fun asLong(value: Any?): Long = when (value) {
    is Number -> value.toLong()
    is String -> value.trim().toLongOrNull() ?: value.trim().toDoubleOrNull()?.toLong() ?: 0L
    else -> 0L
}

/**
 * Lists pass through; a JSON string is parsed, because Appwrite returns a
 * JSON-string attribute as a string even when the merged value was an array.
 */
internal fun asList(value: Any?): List<Any?>? = when (value) {
    is List<*> -> value
    is String -> parseJsonArray(value)
    else -> null
}

internal fun asMap(value: Any?): Map<String, Any?>? = when (value) {
    is Map<*, *> -> @Suppress("UNCHECKED_CAST") (value as Map<String, Any?>)
    is String -> parseJsonObject(value)
    else -> null
}

private fun parseJsonArray(raw: String): List<Any?>? =
    runCatching { JsonParser.parseString(raw) }.getOrNull()
        ?.takeIf { it.isJsonArray }
        ?.let { runCatching<List<Any?>> { gson.fromJson(it, listType) }.getOrNull() }

private fun parseJsonObject(raw: String): Map<String, Any?>? =
    runCatching { JsonParser.parseString(raw) }.getOrNull()
        ?.takeIf { it.isJsonObject }
        ?.let { runCatching { gson.fromJson<Map<String, Any?>>(it, mapType) }.getOrNull() }

// -------------------------------------------------------- settings → prefs ----

/**
 * Web `fontSize` / `translationFontSize` index (1..8) → Android `*_scale`
 * (0.5..3.0). Index 2 is both stores' neutral 1.0 and each step is 0.25, which
 * is the exact inverse of `fontScaleToWebFontSizeIndex` for un-clamped
 * scales; boundaries only exist on the web side's clamping.
 */
internal fun webFontIndexToScale(value: Any?): Float? {
    val index = asInt(value) ?: return null
    val scale = 1f + (index - WEB_FONT_INDEX_ANCHOR) * WEB_FONT_INDEX_SCALE_STEP
    return scale.coerceIn(ANDROID_FONT_SCALE_MIN, ANDROID_FONT_SCALE_MAX)
}

/** Web `arabicFontId` → Android `arabic_font` display name; ids web does not know are dropped. */
internal fun webArabicFontName(value: Any?): String? {
    val id = asString(value)?.lowercase() ?: return null
    return id.takeIf { it in WEB_ARABIC_FONT_IDS }?.let { fontIdToName(it) }
}

/** Web `wordTooltipBehavior` → Android `word_tap_behavior`; unknown values are dropped. */
internal fun webWordTapBehavior(value: Any?): String? {
    val raw = asString(value)?.lowercase() ?: return null
    return raw.takeIf { it in ANDROID_WORD_TAP_BEHAVIORS }
}

/** Web `mushafId` → Android `mushaf_preset` (canonical Mushaf id). */
internal fun webMushafPreset(value: Any?): String? = asString(value)?.let { Mushaf.fromId(it).id }

/** Web `dailyReadingGoal` → Settings `daily_reading_goal` (minutes per day). */
internal fun webDailyReadingGoal(value: Any?): Int? = asInt(value)?.coerceAtLeast(0)

/**
 * Web `prayerSettings` → PlannerSettings values. Either half may be absent;
 * a totally unreadable object yields null so neither pref is touched.
 */
data class WebPrayerSettings(
    val activePrayers: List<String>?,
    val readPreference: String?
)

internal fun webPrayerSettings(value: Any?): WebPrayerSettings? {
    val map = asMap(value) ?: return null
    val prayers = asList(map["activePrayers"])
        ?.mapNotNull { asString(it) }
        ?.filter { it in PRAYER_NAMES }
        ?.sortedBy { PRAYER_NAMES.indexOf(it) }
        ?.takeIf { it.isNotEmpty() }
    val read = asString(map["readPreference"])?.lowercase()?.takeIf { it in WEB_READ_PREFERENCES }
    if (prayers == null && read == null) return null
    return WebPrayerSettings(prayers, read)
}

// ------------------------------------------------- hifdh settings → prefs ----

/** Web `memorizedSurahs` (chapter ids) → Android `memorized_surahs` string set. */
internal fun webMemorizedSurahSet(value: Any?): Set<String>? =
    asList(value)?.mapNotNull { asInt(it) }?.map { it.toString() }?.toSet()

/** Web `memorizedAyahs` (verse keys) → Android `memorized_ayahs` string set. */
internal fun webMemorizedAyahSet(value: Any?): Set<String>? =
    asList(value)?.mapNotNull { asString(it) }?.toSet()

// ----------------------------------------------- tours / coachmarks / visits ---

/** Web `completedTours` / `dismissedCoachmarks` ids → `tour_`/`coachmark_` pref ids. */
internal fun webFlagIds(value: Any?): List<String>? =
    asList(value)?.mapNotNull { asString(it) }?.distinct()

/** Web `pageVisitCounts` → `tour_prefs` `visit_<pageId>` counters; non-positive counts are dropped. */
internal fun webPageVisits(value: Any?): Map<String, Int>? {
    val map = asMap(value) ?: return null
    return map.mapNotNull { (key, raw) ->
        val pageId = key.trim().takeIf { it.isNotEmpty() } ?: return@mapNotNull null
        val count = asInt(raw)?.takeIf { it > 0 } ?: return@mapNotNull null
        pageId to count
    }.toMap()
}

// ------------------------------------------------------- Room row mappings ----

/** Web `bookmarks` entry: { verseKey, surahName, chapterId, createdAt }. */
data class WebBookmarkRow(
    val verseKey: String,
    val chapterId: Int,
    val surahName: String,
    val createdAt: Long
)

internal fun webBookmarkRows(value: Any?): List<WebBookmarkRow>? =
    asList(value)?.mapNotNull { asMap(it)?.let(::webBookmarkRow) }

private fun webBookmarkRow(raw: Map<String, Any?>): WebBookmarkRow? {
    val verseKey = asString(raw["verseKey"]) ?: return null
    val chapterId = asInt(raw["chapterId"])
        ?: verseKey.substringBefore(":").toIntOrNull()
        ?: return null
    // Web stamps `createdAt`; Android's own blob names the same field `timestamp`.
    val createdAt = listOf(raw["createdAt"], raw["timestamp"])
        .map { asLong(it) }
        .firstOrNull { it > 0 }
        ?: 0L
    return WebBookmarkRow(
        verseKey = verseKey,
        chapterId = chapterId,
        surahName = asString(raw["surahName"]) ?: "",
        createdAt = createdAt
    )
}

/** Web `collections` item: { verseKey, surahName, chapterId, addedAt }. */
data class WebCollectionItemRow(
    val verseKey: String,
    val chapterId: Int,
    val surahName: String,
    val addedAt: Long
)

/** Web `collections` entry: { id, name, createdAt, items: [...] }. */
data class WebCollectionRow(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val items: List<WebCollectionItemRow>
)

internal fun webCollectionRows(value: Any?): List<WebCollectionRow>? =
    asList(value)?.mapNotNull { asMap(it)?.let(::webCollectionRow) }

private fun webCollectionRow(raw: Map<String, Any?>): WebCollectionRow? {
    val id = asLong(raw["id"]).takeIf { it > 0 }
        ?: asString(raw["id"])?.hashCode()?.toLong()
        ?: return null
    val createdAt = asLong(raw["createdAt"]).takeIf { it > 0 } ?: 0L
    // Web collection items carry no stamp of their own, so they inherit the
    // collection's `createdAt` when they need one.
    val items = asList(raw["items"])
        ?.mapNotNull { asMap(it)?.let(::webCollectionItemRow) }
        ?.map { if (it.addedAt > 0) it else it.copy(addedAt = createdAt) }
        ?: emptyList()
    return WebCollectionRow(
        id = id,
        name = asString(raw["name"]) ?: "Collection",
        createdAt = createdAt,
        items = items
    )
}

private fun webCollectionItemRow(raw: Map<String, Any?>): WebCollectionItemRow? {
    val verseKey = asString(raw["verseKey"]) ?: return null
    val chapterId = asInt(raw["chapterId"])
        ?: verseKey.substringBefore(":").toIntOrNull()
        ?: return null
    // Web omits `addedAt` on collection items; Android's own blob names it
    // `addedAt`, and the collection stamp is applied afterwards as a fallback.
    val addedAt = listOf(raw["addedAt"], raw["timestamp"])
        .map { asLong(it) }
        .firstOrNull { it > 0 }
        ?: 0L
    return WebCollectionItemRow(verseKey, chapterId, asString(raw["surahName"]) ?: "", addedAt)
}

/** Web `readingSessions` entry; `timestamp` is the identity key on both stores. */
data class WebReadingSessionRow(
    val date: String,
    val duration: Long,
    val type: String,
    val chapterId: Int?,
    val timestamp: Long
)

internal fun webReadingSessionRows(value: Any?): List<WebReadingSessionRow>? =
    asList(value)?.mapNotNull { asMap(it)?.let(::webReadingSessionRow) }

private fun webReadingSessionRow(raw: Map<String, Any?>): WebReadingSessionRow? {
    val timestamp = asLong(raw["timestamp"]).takeIf { it > 0 } ?: return null
    return WebReadingSessionRow(
        date = asString(raw["date"]) ?: "",
        duration = asLong(raw["duration"]),
        type = asString(raw["type"]) ?: "reading",
        chapterId = asInt(raw["chapterId"]),
        timestamp = timestamp
    )
}

/** Web `recentlyRead` entry; `chapterId` is the union key, latest `timestamp` wins. */
data class WebRecentlyReadRow(
    val chapterId: Int,
    val chapterName: String,
    val verseKey: String?,
    val timestamp: Long
)

internal fun webRecentlyReadRows(value: Any?): List<WebRecentlyReadRow>? =
    asList(value)?.mapNotNull { asMap(it)?.let(::webRecentlyReadRow) }

private fun webRecentlyReadRow(raw: Map<String, Any?>): WebRecentlyReadRow? {
    val chapterId = asInt(raw["chapterId"]) ?: return null
    return WebRecentlyReadRow(
        chapterId = chapterId,
        chapterName = asString(raw["chapterName"]) ?: "",
        verseKey = asString(raw["verseKey"]),
        timestamp = asLong(raw["timestamp"])
    )
}

// ------------------------------------------------- planner / per-plan extras ---

/** Plan ids behind a web per-plan map (`plannerBookmarks` / `plannerSessionTimers` / …). */
internal fun webPlannerExtraEntries(value: Any?): List<Pair<String, Any?>>? =
    asMap(value)?.mapNotNull { (id, entry) ->
        id.trim().takeIf { it.isNotEmpty() }?.let { it to entry }
    }

/**
 * Web `plannerBookmarks[planId]` entries → Android's persisted highlight rows.
 * Web's `createdAt` is an ISO string Android has no field for (it defaults on
 * read), so it is dropped instead of breaking the parse.
 */
data class WebPlannerBookmarkRow(
    val verseKey: String,
    val surahName: String,
    val note: String
)

internal fun webPlannerBookmarkRows(value: Any?): List<WebPlannerBookmarkRow>? =
    asList(value)?.mapNotNull { asMap(it)?.let(::webPlannerBookmarkRow) }

private fun webPlannerBookmarkRow(raw: Map<String, Any?>): WebPlannerBookmarkRow? {
    val verseKey = asString(raw["verseKey"]) ?: return null
    return WebPlannerBookmarkRow(
        verseKey = verseKey,
        surahName = asString(raw["surahName"]) ?: "",
        note = asString(raw["note"]) ?: ""
    )
}

/**
 * Web `plannerSessionTimers[planId]` ({ dayNumber: { totalSeconds } }) →
 * Android's persisted day → seconds map.
 */
internal fun webPlannerSessionTotals(value: Any?): Map<Int, Long>? {
    val days = asMap(value) ?: return null
    return days.mapNotNull { (day, entry) ->
        val dayNumber = day.trim().toIntOrNull() ?: return@mapNotNull null
        val seconds = asMap(entry)?.let { asLong(it["totalSeconds"]) } ?: 0L
        (dayNumber to seconds).takeIf { seconds >= 0 }
    }.toMap()
}

/**
 * Web `plannerReflections[planId]` ({ dayNumber: { text } }) → Android's
 * plan-embedded `assignmentReflections` (day → note text).
 */
internal fun webPlannerReflections(value: Any?): Map<Int, String>? {
    val days = asMap(value) ?: return null
    return days.mapNotNull { (day, entry) ->
        val dayNumber = day.trim().toIntOrNull() ?: return@mapNotNull null
        val text = asMap(entry)?.let { asString(it["text"]) } ?: asString(entry)
        text?.let { dayNumber to it }
    }.toMap()
}

/** Web `plannerReflections` → { planId: { day → text } }, skipping unusable plans. */
internal fun webPlannerReflectionsByPlan(value: Any?): Map<String, Map<Int, String>>? =
    asMap(value)?.mapNotNull { (id, entry) ->
        val planId = id.trim().takeIf { it.isNotEmpty() } ?: return@mapNotNull null
        val days = webPlannerReflections(entry)?.takeIf { it.isNotEmpty() }
        days?.let { planId to it }
    }?.toMap()

/**
 * Folds [reflections] into a raw plan map's `assignmentReflections`, the only
 * home Android has for per-day planner notes (web keeps a separate keyed map).
 */
internal fun applyWebReflectionsToPlan(
    plan: Map<String, Any?>,
    reflections: Map<Int, String>?
): Map<String, Any?> {
    if (reflections.isNullOrEmpty()) return plan
    val out = plan.toMutableMap()
    val existing = asMap(out["assignmentReflections"])?.toMutableMap() ?: mutableMapOf()
    reflections.forEach { (day, text) -> existing[day.toString()] = text }
    out["assignmentReflections"] = existing
    return out
}

/**
 * Union local and remote plan lists by id (remote wins per plan). The merge
 * side owns this for remote state; the apply side repeats it so a payload that
 * arrived without the local `planners` key can never drop locally known plans.
 */
internal fun mergePlansById(local: Any?, remote: Any?): List<Any?>? {
    val localPlans = asList(local) ?: return asList(remote)
    val remotePlans = asList(remote) ?: return localPlans
    val out = LinkedHashMap<String, Any?>()
    localPlans.forEach { out[planIdentity(it)] = it }
    remotePlans.forEach { out[planIdentity(it)] = it }
    return out.values.toList()
}

private fun planIdentity(plan: Any?): String {
    val map = asMap(plan) ?: return plan?.toString() ?: "null"
    val id = asString(map["id"]) ?: return gson.toJson(map)
    return "id:$id"
}
