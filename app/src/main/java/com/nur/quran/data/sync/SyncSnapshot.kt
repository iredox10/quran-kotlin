package com.nur.quran.data.sync

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.nur.quran.data.db.dao.QuranDao
import com.nur.quran.data.db.entities.BookmarkEntity
import com.nur.quran.data.db.entities.CollectionEntity
import com.nur.quran.data.db.entities.CollectionItemEntity
import com.nur.quran.data.db.entities.ReadingSessionEntity
import com.nur.quran.data.db.entities.RecentlyReadEntity
import com.nur.quran.data.mushaf.fontNameToId
import com.nur.quran.data.planner.PRAYER_NAMES
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cross-device sync payload, mirroring the web app's `getSyncableState`
 * (quran-app/src/store/useAppStore.js ~L843-900) as closely as Android
 * stores allow.
 *
 * Web → Android source mapping:
 * - theme ("light"/"dark") ← `is_dark_theme` boolean in `hifdh_settings`
 *   (SurahScreen) with fallback to the `Settings` file (MainActivity).
 * - translationId / tafsirId / reciterId ← `translation_id` / `tafsir_id` /
 *   `reciter_id` ints in `hifdh_settings` (SurahViewModel).
 * - arabicFont (display name, e.g. "Scheherazade New") ← `arabic_font` in
 *   `hifdh_settings`. Web `arabicFontId` has no Android equivalent.
 * - arabicScale / translationScale ← `arabic_scale` / `translation_scale`
 *   floats in `hifdh_settings` (web `fontSize` / `translationFontSize`).
 * - tajweedEnabled ← `is_tajweed_enabled` in `hifdh_settings`.
 * - wordTapBehavior ← `word_tap_behavior` in `hifdh_settings`
 *   (web `wordTooltipBehavior`).
 * - mushafPreset / mushafId ← `mushaf_preset` in `hifdh_settings`
 *   (web `mushafId`; both carry the canonical id, e.g. "madani-standard").
 * - readingMode ← `is_reading_mode` in `hifdh_settings` (web `readingMode`;
 *   true = arabic-only). Absent `is_reading_mode` falls back to the inverse
 *   of `is_translation_enabled` so legacy installs still sync.
 * - translationEnabled ← `is_translation_enabled` in `hifdh_settings`
 *   (inverse of `readingMode`; absent key falls back to inverse of
 *   `is_reading_mode`).
 * - bookmarksJson ← Room `bookmarks` table (web `bookmarks`/`bookmark`;
 *   Android keeps a single bookmark row, serialized as a list).
 * - collectionsJson ← Room `collections` + `collection_items` tables
 *   (web `collections`), shaped as {"collections":[...],"items":[...]}.
 * - plansJson ← `api_responses` cache rows `planner_all_plans`,
 *   `planner_active_id`, `planner_archived_plans` (+ legacy
 *   `planner_active_plan`), plus per-plan `planner_bookmarks_<id>` /
 *   `planner_sessions_<id>` extras for plans known locally. Per-plan extras
 *   for unknown ids cannot be enumerated — [QuranDao] has no
 *   list-by-prefix query — and are therefore best-effort only.
 * - hifdhJson ← `hifdh_settings` keys `hifdh_history`, `hifdh_goals`,
 *   `memorized_ayahs`, `transition_links`
 *   (web `hifdhHistory` / `hifdhGoals` / `memorizedAyahs`), shaped as
 *   {"history":{...},"goals":[...],"memorizedAyahs":[...],"transitionLinks":[...]}.
 * - readingStatsJson ← Room `reading_sessions` + `recently_read` tables
 *   (web `readingSessions` / `recentlyRead`), shaped as
 *   {"sessions":[...],"recentlyRead":[...]}.
 *
 * Deliberately excluded (device-local, mirrors web exclusions):
 * - Audio downloads / pack caches (`audio_downloads`, `tafsir_packs`,
 *   word packs, `translation_texts`, `linked_timings`) — like web
 *   `downloadedSurahs` / `offlinePackStatus`, these are per-device.
 * - Quran text/tajweed/tafsir `api_responses` cache rows — content cache,
 *   re-fetchable, not user state.
 * - Playback `pb_*` prefs, prayer timings/location, tours & coachmarks,
 *   pomodoro state — no matching snapshot field / device-local.
 */
data class SyncSnapshot(
    val version: Int = 1,
    val updatedAt: Long = System.currentTimeMillis(),
    val theme: String? = null,
    val arabicFont: String? = null,
    val arabicScale: Float? = null,
    val translationScale: Float? = null,
    val translationId: Int? = null,
    val tafsirId: Int? = null,
    val reciterId: Int? = null,
    val tajweedEnabled: Boolean? = null,
    val wordTapBehavior: String? = null,
    val mushafPreset: String? = null,
    val mushafId: String? = null,
    val readingMode: Boolean? = null,
    val translationEnabled: Boolean? = null,
    val bookmarksJson: String? = null,
    val collectionsJson: String? = null,
    val plansJson: String? = null,
    val hifdhJson: String? = null,
    val readingStatsJson: String? = null
)

/**
 * Builds [SyncSnapshot] from local stores. Absent prefs keys yield null
 * (never defaults) so a merge never clobbers remote state with local
 * fallbacks. DAO reads are best-effort: a failed section yields null
 * rather than failing the whole snapshot.
 */
@Singleton
class SnapshotBuilder @Inject constructor(
    private val gson: Gson,
    @ApplicationContext private val context: Context,
    private val dao: QuranDao
) {
    suspend fun build(): SyncSnapshot {
        val hifdh = context.getSharedPreferences("hifdh_settings", Context.MODE_PRIVATE)
        val settings = context.getSharedPreferences("Settings", Context.MODE_PRIVATE)

        val theme: String? = when {
            hifdh.contains("is_dark_theme") ->
                if (hifdh.getBoolean("is_dark_theme", false)) "dark" else "light"
            settings.contains("is_dark_theme") ->
                if (settings.getBoolean("is_dark_theme", false)) "dark" else "light"
            else -> null
        }

        return SyncSnapshot(
            updatedAt = System.currentTimeMillis(),
            theme = theme,
            arabicFont = hifdh.getString("arabic_font", null),
            arabicScale = if (hifdh.contains("arabic_scale")) hifdh.getFloat("arabic_scale", 1f) else null,
            translationScale = if (hifdh.contains("translation_scale")) hifdh.getFloat("translation_scale", 1f) else null,
            translationId = if (hifdh.contains("translation_id")) hifdh.getInt("translation_id", 20) else null,
            tafsirId = if (hifdh.contains("tafsir_id")) hifdh.getInt("tafsir_id", 169) else null,
            reciterId = if (hifdh.contains("reciter_id")) hifdh.getInt("reciter_id", 7) else null,
            tajweedEnabled = if (hifdh.contains("is_tajweed_enabled")) hifdh.getBoolean("is_tajweed_enabled", false) else null,
            wordTapBehavior = hifdh.getString("word_tap_behavior", null),
            mushafPreset = if (hifdh.contains("mushaf_preset")) hifdh.getString("mushaf_preset", null) else null,
            mushafId = if (hifdh.contains("mushaf_preset")) hifdh.getString("mushaf_preset", null) else null,
            readingMode = when {
                hifdh.contains("is_reading_mode") -> hifdh.getBoolean("is_reading_mode", false)
                hifdh.contains("is_translation_enabled") -> !hifdh.getBoolean("is_translation_enabled", true)
                else -> null
            },
            translationEnabled = when {
                hifdh.contains("is_translation_enabled") -> hifdh.getBoolean("is_translation_enabled", true)
                hifdh.contains("is_reading_mode") -> !hifdh.getBoolean("is_reading_mode", false)
                else -> null
            },
            bookmarksJson = readBookmarksJson(),
            collectionsJson = readCollectionsJson(),
            plansJson = readPlansJson(),
            hifdhJson = readHifdhJson(hifdh),
            readingStatsJson = readReadingStatsJson()
        )
    }

    private suspend fun readBookmarksJson(): String? = runCatching {
        gson.toJson(dao.getAllBookmarks().first())
    }.getOrNull()

    private suspend fun readCollectionsJson(): String? = runCatching {
        val collections = dao.getAllCollections().first()
        val items = dao.getAllCollectionItems().first()
        gson.toJson(mapOf("collections" to collections, "items" to items))
    }.getOrNull()

    private suspend fun readPlansJson(): String? = runCatching {
        val allPlansRaw = dao.getCacheEntry("planner_all_plans")?.dataJson
        val activeId = dao.getCacheEntry("planner_active_id")?.dataJson?.takeIf { it.isNotBlank() }
        val archivedRaw = dao.getCacheEntry("planner_archived_plans")?.dataJson
        val legacyActiveRaw = dao.getCacheEntry("planner_active_plan")?.dataJson

        // Collect known plan ids so per-plan extras can be attached.
        val knownIds = mutableSetOf<String>()
        activeId?.let { knownIds.add(it) }
        knownIds.addAll(extractPlanIds(allPlansRaw))
        knownIds.addAll(extractPlanIds(archivedRaw))

        val bookmarks = mutableMapOf<String, Any?>()
        val sessionTotals = mutableMapOf<String, Any?>()
        for (id in knownIds) {
            dao.getCacheEntry("planner_bookmarks_$id")?.dataJson
                ?.takeIf { it.isNotBlank() }?.let { bookmarks[id] = rawElement(it) }
            dao.getCacheEntry("planner_sessions_$id")?.dataJson
                ?.takeIf { it.isNotBlank() }?.let { sessionTotals[id] = rawElement(it) }
        }

        gson.toJson(
            mapOf(
                "allPlans" to allPlansRaw?.let(::rawElement),
                "activePlannerId" to activeId,
                "archivedPlans" to archivedRaw?.let(::rawElement),
                "activePlan" to legacyActiveRaw?.takeIf { it.isNotBlank() }?.let(::rawElement),
                "bookmarks" to bookmarks,
                "sessionTotals" to sessionTotals
            )
        )
    }.getOrNull()

    private fun readHifdhJson(hifdh: android.content.SharedPreferences): String? = runCatching {
        gson.toJson(
            mapOf(
                "history" to hifdh.getString("hifdh_history", null)?.let(::rawElement),
                "goals" to hifdh.getString("hifdh_goals", null)?.let(::rawElement),
                "memorizedAyahs" to (hifdh.getStringSet("memorized_ayahs", null)?.toList() ?: emptyList<String>()),
                "transitionLinks" to (hifdh.getStringSet("transition_links", null)?.toList() ?: emptyList<String>())
            )
        )
    }.getOrNull()

    private suspend fun readReadingStatsJson(): String? = runCatching {
        val sessions = dao.getAllReadingSessions().first()
        val recentlyRead = dao.getRecentlyRead().first()
        gson.toJson(mapOf("sessions" to sessions, "recentlyRead" to recentlyRead))
    }.getOrNull()

    /** Parses stored JSON verbatim so re-serialization never double-escapes it. */
    private fun rawElement(json: String): Any? = runCatching {
        gson.fromJson(json, Any::class.java)
    }.getOrNull()

    private fun extractPlanIds(plansRaw: String?): List<String> {
        if (plansRaw.isNullOrBlank()) return emptyList()
        return runCatching {
            val type = object : TypeToken<List<Map<String, Any?>>>() {}.type
            val plans: List<Map<String, Any?>> = gson.fromJson(plansRaw, type) ?: return emptyList()
            plans.mapNotNull { it["id"]?.toString() }
        }.getOrNull() ?: emptyList()
    }

    /**
     * Flattens a snapshot to a plain `Map<String, Any>` for Appwrite
     * document I/O (`Databases.createDocument` / `updateDocument` data
     * param). Null fields are omitted so partial snapshots never wipe
     * remote keys; `version` and `updatedAt` are always present.
     */
    fun snapshotToMap(snapshot: SyncSnapshot): Map<String, Any> {
        val obj = gson.toJsonTree(snapshot).asJsonObject
        val out = mutableMapOf<String, Any>()
        for ((key, el) in obj.entrySet()) {
            if (el == null || el.isJsonNull) continue
            out[key] = when {
                el.isJsonPrimitive -> {
                    val p = el.asJsonPrimitive
                    when {
                        p.isBoolean -> p.asBoolean
                        p.isNumber -> p.asNumber
                        else -> p.asString
                    }
                }
                else -> el.toString()
            }
        }
        out.putIfAbsent("version", 1)
        out.putIfAbsent("updatedAt", System.currentTimeMillis())
        return out
    }

    companion object {
        /** Lenient parse of stored JSON, for tests / debugging. */
        fun parseLenient(gson: Gson, json: String?) =
            json?.takeIf { it.isNotBlank() }?.let {
                runCatching { JsonParser.parseString(it) }.getOrNull()
            }
    }
}

// ─────────────────────────── Web-compatible mappings ───────────────────────────
// Pure functions translating Android stores into the flat keys web's
// `getSyncableState` expects. Kept free of Context/prefs so they are unit
// testable on the JVM (see SyncSnapshotMappingsTest).

/** Web's `fontSize` / `translationFontSize` slider range (SettingsDrawer, 1..8). */
private const val WEB_FONT_INDEX_MIN = 1
private const val WEB_FONT_INDEX_MAX = 8

/**
 * Web's default index (2) lines up with Android's default scale (1.0), and
 * Android's 0.5..3.0 slider covers the index range at 0.25 of scale per step.
 */
private const val WEB_FONT_INDEX_DEFAULT = 2
private const val WEB_FONT_INDEX_SCALE_STEP = 0.25f

/** Web `ARABIC_FONT_OPTIONS` ids (quran-app/src/config/mushaf.js). */
private val WEB_ARABIC_FONT_IDS = setOf(
    "kfgqpc-hafs", "uthman-taha-naskh", "amiri-quran",
    "noto-naskh-arabic", "scheherazade-new"
)

/** Web `wordTooltipBehavior` values ("Word Hover Action" options). */
private val WEB_WORD_TOOLTIP_BEHAVIORS = setOf("tajweed", "translation", "none")

/**
 * Android's continuous `arabic_scale` / `translation_scale` (0.5..3.0) onto
 * web's discrete `fontSize` / `translationFontSize` index (1..8). Index 2 is
 * both stores' default, so the mapping is anchored there and clamped at the
 * ends; inverse is `index -> 1f + (index - 2) * 0.25f`.
 */
internal fun fontScaleToWebFontSizeIndex(scale: Float): Int {
    val raw = WEB_FONT_INDEX_DEFAULT +
        (scale - WEB_FONT_INDEX_DEFAULT) / WEB_FONT_INDEX_SCALE_STEP
    return Math.round(
        raw.coerceIn(WEB_FONT_INDEX_MIN.toFloat(), WEB_FONT_INDEX_MAX.toFloat())
    )
}

/** Web `wordTooltipBehavior` ← Android `word_tap_behavior`; unknown values are dropped. */
internal fun normalizeWordTooltipBehavior(raw: String?): String? {
    val value = raw?.trim()?.lowercase()?.takeIf { it.isNotEmpty() } ?: return null
    return value.takeIf { it in WEB_WORD_TOOLTIP_BEHAVIORS }
}

/**
 * Web `arabicFontId` ← Android `arabic_font` display name. Reuses the
 * display-name map in [com.nur.quran.data.mushaf] and only emits ids web
 * knows, so an unmapped Android font never writes garbage into web state.
 */
internal fun arabicFontIdFromName(name: String?): String? =
    name?.takeIf { it.isNotBlank() }
        ?.let { fontNameToId(it) }
        ?.takeIf { it in WEB_ARABIC_FONT_IDS }

/** Web `prayerSettings.readPreference` values ('before' | 'after' | 'split'). */
private val WEB_READ_PREFERENCES = setOf("before", "after", "split")

/**
 * Web `prayerSettings.readPreference` ← PlannerSettings `reading_preference`.
 * Mirrors PlannerViewModel's own coercion so a legacy/garbage stored value is
 * dropped instead of pushed.
 */
internal fun normalizeReadPreference(raw: String?): String? {
    val value = raw?.trim()?.lowercase()?.takeIf { it.isNotEmpty() } ?: return null
    return value.takeIf { it in WEB_READ_PREFERENCES }
}

/**
 * Web `prayerSettings.activePrayers` ← PlannerSettings `active_prayers`:
 * names are ordered by [PRAYER_NAMES] (web renders Fajr → Isha) and unknown
 * names are dropped; an empty selection means "not set" so it is omitted and
 * the remote preference survives the merge.
 */
internal fun normalizeActivePrayers(raw: Set<String>?): List<String>? {
    val ordered = raw.orEmpty()
        .filter { it in PRAYER_NAMES }
        .sortedBy { PRAYER_NAMES.indexOf(it) }
    return ordered.takeIf { it.isNotEmpty() }
}

/** Prefs key prefix for the `tour_prefs` page-visit counters. */
private const val PAGE_VISIT_PREFIX = "visit_"

/** Web `memorizedSurahs` (chapter ids) ← hifdh_settings `memorized_surahs` string set. */
internal fun memorizedSurahIds(raw: Set<String>?): List<Int> =
    raw.orEmpty().mapNotNull { it.trim().toIntOrNull() }.distinct().sorted()

/** Web `memorizedAyahs` (verse keys) ← hifdh_settings `memorized_ayahs` string set. */
internal fun memorizedAyahKeys(raw: Set<String>?): List<String> =
    raw.orEmpty().map { it.trim() }.filter { it.isNotEmpty() }.distinct().sorted()

/**
 * Ids behind a `tour_prefs` boolean flag, e.g. `tour_home` → "home" for web
 * `completedTours` and `coachmark_home` → "home" for `dismissedCoachmarks`.
 */
internal fun flagIds(prefs: Map<String, Any?>, prefix: String): List<String> =
    prefs.mapNotNull { (key, value) ->
        if (key.startsWith(prefix) && value == true) {
            key.removePrefix(prefix).takeIf { it.isNotEmpty() }
        } else {
            null
        }
    }.sorted()

/** Web `pageVisitCounts` ← `tour_prefs` `visit_<pageId>` int counters. */
internal fun pageVisitCounts(prefs: Map<String, Any?>): Map<String, Int> =
    prefs.mapNotNull { (key, value) ->
        if (!key.startsWith(PAGE_VISIT_PREFIX)) return@mapNotNull null
        val pageId = key.removePrefix(PAGE_VISIT_PREFIX).takeIf { it.isNotEmpty() }
        val count = (value as? Number)?.toInt()?.takeIf { it > 0 }
        if (pageId != null && count != null) pageId to count else null
    }.toMap()

/** Web `bookmarks` entry: { verseKey, surahName, chapterId, createdAt }. */
internal fun webBookmark(bookmark: BookmarkEntity): Map<String, Any?> = mapOf(
    "verseKey" to bookmark.verseKey,
    "surahName" to bookmark.surahName,
    "chapterId" to bookmark.chapterId,
    "createdAt" to bookmark.timestamp
)

/**
 * Web `readingSessions` entry. `timestamp` is the identity key syncMerge
 * unions on and `duration` is seconds on both stores.
 */
internal fun webReadingSession(session: ReadingSessionEntity): Map<String, Any?> = mapOf(
    "date" to session.date,
    "duration" to session.duration,
    "type" to session.type,
    "chapterId" to session.chapterId,
    "timestamp" to session.timestamp
)

/** Web `recentlyRead` entry; `chapterId` is the union key, latest `timestamp` wins. */
internal fun webRecentlyRead(entry: RecentlyReadEntity): Map<String, Any?> = mapOf(
    "chapterId" to entry.chapterId,
    "chapterName" to entry.chapterName,
    "verseKey" to entry.verseKey,
    "timestamp" to entry.timestamp
)

/** Web `collections` entry: { id, name, createdAt, items: [{ verseKey, surahName, chapterId, addedAt }] }. */
internal fun webCollections(
    collections: List<CollectionEntity>,
    items: List<CollectionItemEntity>
): List<Map<String, Any?>> = collections.map { collection ->
    mapOf(
        "id" to collection.id,
        "name" to collection.name,
        "createdAt" to collection.createdAt,
        "items" to items.filter { it.collectionId == collection.id }.map { item ->
            mapOf(
                "verseKey" to item.verseKey,
                "surahName" to item.surahName,
                "chapterId" to item.chapterId,
                "addedAt" to item.addedAt
            )
        }
    )
}
