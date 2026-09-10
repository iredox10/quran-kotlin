package com.nur.quran.data.sync

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.nur.quran.data.db.dao.QuranDao
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
            translationId = if (hifdh.contains("translation_id")) hifdh.getInt("translation_id", 85) else null,
            tafsirId = if (hifdh.contains("tafsir_id")) hifdh.getInt("tafsir_id", 169) else null,
            reciterId = if (hifdh.contains("reciter_id")) hifdh.getInt("reciter_id", 7) else null,
            tajweedEnabled = if (hifdh.contains("is_tajweed_enabled")) hifdh.getBoolean("is_tajweed_enabled", false) else null,
            wordTapBehavior = hifdh.getString("word_tap_behavior", null),
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
