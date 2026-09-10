package com.nur.quran.data.sync

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nur.quran.data.db.dao.QuranDao
import com.nur.quran.data.db.entities.ApiResponseCacheEntity
import com.nur.quran.data.db.entities.BookmarkEntity
import com.nur.quran.data.db.entities.CollectionEntity
import com.nur.quran.data.db.entities.CollectionItemEntity
import com.nur.quran.data.db.entities.ReadingSessionEntity
import com.nur.quran.data.db.entities.RecentlyReadEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

private const val SYNC_PREFS = "sync_state"
private const val KEY_LAST_SYNC_AT = "last_sync_at"

/**
 * Cloud sync engine. Owns ONLY orchestration + merge; snapshot construction belongs to
 * [SnapshotBuilder] and transport constants/client to [AppwriteClient].
 *
 * Transport (mirrors quran-app/src/services/appwrite.js): one doc per user in
 * `AppwriteClient.USER_SYNC_COLLECTION_ID`, document id == userId (from `account.get()`),
 * create-if-missing (try update, on 404 create). Payload is [SnapshotBuilder.snapshotToMap]
 * output: scalar settings + JSON-string section fields (bookmarksJson, collectionsJson,
 * plansJson, hifdhJson, readingStatsJson) + `version` / `updatedAt`.
 *
 * Merge rules (mirror of quran-app/src/utils/syncMerge.js `mergeStateInto`):
 * - Settings / scalars: LAST-WRITE-WINS (incoming wins; absent local keys serialize as
 *   null and are omitted from the map, so a merge never clobbers remote with defaults).
 * - JSON-string sections are parsed, then unioned by identity key —
 *   bookmarks by `verseKey`; collections by `id` (items unioned by `verseKey`);
 *   plans by `id` (planner bookmarks/session maps deep-merged); hifdh goals by `id`,
 *   history deep-merged, memorized ayahs/transition links deduped; reading sessions by
 *   `timestamp` (cap 500); recentlyRead by `chapterId`, latest timestamp wins, cap 5 —
 *   then re-serialized.
 * - Trade-off (same as web): deletions don't propagate.
 *
 * All entry points never throw: [push]/[pull] return Boolean, [sync] returns a status string.
 */
@Singleton
class SyncService @Inject constructor(
    private val appwriteClient: AppwriteClient,
    private val snapshotBuilder: SnapshotBuilder?,
    private val dao: QuranDao?,
    @ApplicationContext private val context: Context,
) {
    private val gson = Gson()

    // ------------------------------------------------------------------ public API ---

    /** Build local snapshot and upload (merge local over remote, update-or-create). */
    suspend fun push(): Boolean {
        return try {
            val userId = currentUserId() ?: return false
            pushInternal(userId)
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Download remote doc; if remote is newer than local, union-merge into local state and
     * apply (prefs via `apply()`, DAO via existing inserts). True when ok or already current.
     */
    suspend fun pull(): Boolean {
        return try {
            val userId = currentUserId() ?: return false
            val remote = fetchRemote(userId) ?: return false
            val watermark = maxOf(lastSyncAt(), localUpdatedAt())
            if (remote.updatedAt > watermark) {
                applyState(mergeStateInto(localStateMap(), remote.state))
                saveLastSync(remote.updatedAt)
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * One-shot bidirectional sync. Returns "pushed" / "pulled" / "up-to-date" /
     * "signed-out" / "error:<message>".
     */
    suspend fun sync(): String {
        return try {
            val userId = currentUserId() ?: return "signed-out"
            val remote = fetchRemote(userId)
            val lastSync = lastSyncAt()
            val local = localStateMap()
            val localTs = (local["updatedAt"] as? Number)?.toLong() ?: 0L
            if (remote != null && remote.updatedAt > maxOf(lastSync, localTs)) {
                applyState(mergeStateInto(local, remote.state))
                saveLastSync(remote.updatedAt)
                "pulled"
            } else if (localTs > lastSync || remote == null) {
                if (pushInternal(userId)) "pushed" else "error:push failed"
            } else {
                "up-to-date"
            }
        } catch (e: Exception) {
            "error:${e.message}"
        }
    }

    // ------------------------------------------------- merge (mirrors syncMerge.js) ---

    /**
     * Merge `incoming` into `base`. JSON-string section fields are parsed, unioned by
     * identity key and re-serialized; keyed maps deep-merge; scalars/settings
     * incoming-wins (LWW). Pure function (no I/O).
     */
    fun mergeStateInto(
        base: Map<String, Any?>,
        incoming: Map<String, Any?>,
    ): Map<String, Any?> {
        val out = base.toMutableMap()
        for ((field, inc) in incoming) {
            if (inc == null) continue
            when (field) {
                "bookmarksJson" -> out[field] = mergeJsonList(
                    out[field] as? String, inc as? String, "verseKey",
                )
                "collectionsJson" -> out[field] = mergeCollectionsJson(
                    out[field] as? String, inc as? String,
                )
                "plansJson" -> out[field] = mergePlansJson(
                    out[field] as? String, inc as? String,
                )
                "hifdhJson" -> out[field] = mergeHifdhJson(
                    out[field] as? String, inc as? String,
                )
                "readingStatsJson" -> out[field] = mergeReadingStatsJson(
                    out[field] as? String, inc as? String,
                )
                else -> out[field] = mergeValue(out[field], inc)
            }
        }
        return out
    }

    // ------------------------------------------------------------------ push/pull ---

    /** runCatching variant for blocks that call suspend functions. */
    private suspend fun <T> runSuspendCatching(block: suspend () -> T): Result<T> {
        return try {
            Result.success(block())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** userId from account.get(); null when signed out (or on transport failure). */
    private suspend fun currentUserId(): String? {
        return runSuspendCatching { appwriteClient.account.get().id }.getOrNull()
    }

    private suspend fun pushInternal(userId: String): Boolean {
        return try {
            val local = localStateMap().toMutableMap()
            val now = System.currentTimeMillis()
            local["userId"] = userId
            local["version"] = 1
            // LWW stamp for settings; section lists merge by union regardless of this stamp.
            local["updatedAt"] = now
            val existing = fetchRemote(userId)
            val payload: Map<String, Any> = if (existing != null) {
                mergeStateInto(existing.state, local).toMutableMap().also {
                    it["userId"] = userId
                    it["version"] = 1
                    it["updatedAt"] = now
                }.withoutNulls()
            } else {
                local.withoutNulls()
            }
            // Create-if-missing: try update, on 404 (or any update failure) create.
            val remoteTs = try {
                val updated = appwriteClient.databases.updateDocument(
                    AppwriteClient.DATABASE_ID,
                    AppwriteClient.USER_SYNC_COLLECTION_ID,
                    userId,
                    payload,
                )
                parseUpdatedAt(updated.updatedAt)
            } catch (_: Exception) {
                val created = appwriteClient.databases.createDocument(
                    AppwriteClient.DATABASE_ID,
                    AppwriteClient.USER_SYNC_COLLECTION_ID,
                    userId,
                    payload,
                )
                parseUpdatedAt(created.updatedAt)
            }
            saveLastSync(if (remoteTs > 0) remoteTs else now)
            true
        } catch (_: Exception) {
            false
        }
    }

    private suspend fun fetchRemote(userId: String): RemoteDoc? {
        return try {
            val doc = appwriteClient.databases.getDocument(
                AppwriteClient.DATABASE_ID,
                AppwriteClient.USER_SYNC_COLLECTION_ID,
                userId,
            )
            RemoteDoc(extractState(doc.data), parseUpdatedAt(doc.updatedAt))
        } catch (_: Exception) {
            null // missing doc OR transport failure — caller treats as absent
        }
    }

    /** Remote doc may carry web-style `stateData` JSON string; normalize to a state map. */
    private fun extractState(data: Map<String, Any?>): Map<String, Any?> {
        val out = data.toMutableMap()
        (out.remove("stateData") as? String)?.let { raw ->
            parseJsonObject(raw)?.let { parsed -> for ((k, v) in parsed) out.putIfAbsent(k, v) }
        }
        out.remove("userId")
        return out
    }

    // ----------------------------------------------------------------- local state ---

    private suspend fun localStateMap(): Map<String, Any?> {
        if (snapshotBuilder != null) {
            return runSuspendCatching {
                snapshotBuilder.snapshotToMap(snapshotBuilder.build())
            }.getOrNull() ?: emptyMap()
        }
        return buildLocalStateMap()
    }

    private suspend fun localUpdatedAt(): Long {
        return (runSuspendCatching { localStateMap()["updatedAt"] }.getOrNull() as? Number)?.toLong() ?: 0L
    }

    /**
     * Fallback snapshot (same key/JSON-string shape as [SnapshotBuilder]) for when the
     * builder is absent. Best-effort per section; a failed section is omitted (never
     * defaults) so it can't clobber remote state on merge.
     */
    private suspend fun buildLocalStateMap(): Map<String, Any?> {
        val out = mutableMapOf<String, Any?>(
            "version" to 1,
            "updatedAt" to System.currentTimeMillis(),
        )
        val hifdh = prefs("hifdh_settings")
        val settings = prefs("Settings")
        if (hifdh.contains("is_dark_theme") || settings.contains("is_dark_theme")) {
            val dark = if (hifdh.contains("is_dark_theme")) hifdh.getBoolean("is_dark_theme", false)
            else settings.getBoolean("is_dark_theme", false)
            out["theme"] = if (dark) "dark" else "light"
        }
        hifdh.getString("arabic_font", null)?.let { out["arabicFont"] = it }
        if (hifdh.contains("arabic_scale")) out["arabicScale"] = hifdh.getFloat("arabic_scale", 1f)
        if (hifdh.contains("translation_scale")) {
            out["translationScale"] = hifdh.getFloat("translation_scale", 1f)
        }
        if (hifdh.contains("translation_id")) out["translationId"] = hifdh.getInt("translation_id", 20)
        if (hifdh.contains("tafsir_id")) out["tafsirId"] = hifdh.getInt("tafsir_id", 169)
        if (hifdh.contains("reciter_id")) out["reciterId"] = hifdh.getInt("reciter_id", 7)
        if (hifdh.contains("is_tajweed_enabled")) {
            out["tajweedEnabled"] = hifdh.getBoolean("is_tajweed_enabled", false)
        }
        hifdh.getString("word_tap_behavior", null)?.let { out["wordTapBehavior"] = it }
        runSuspendCatching { daoBookmarks() }.getOrNull()
            ?.let { out["bookmarksJson"] = gson.toJson(it) }
        runSuspendCatching {
            gson.toJson(mapOf("collections" to daoCollections(), "items" to daoCollectionItems()))
        }.getOrNull()?.let { out["collectionsJson"] = it }
        runCatching { daoHifdhJson(hifdh) }.getOrNull()?.let { out["hifdhJson"] = it }
        runSuspendCatching {
            gson.toJson(mapOf("sessions" to daoSessions(), "recentlyRead" to daoRecent()))
        }.getOrNull()?.let { out["readingStatsJson"] = it }
        return out
    }

    // ---------------------------------------------------------------- apply remote ---

    private suspend fun applyState(state: Map<String, Any?>) {
        // Scalar settings → hifdh_settings / Settings prefs (single apply() per file).
        val hifdhEdit = prefs("hifdh_settings").edit()
        var hifdhTouched = false
        (state["theme"] as? String)?.let {
            hifdhEdit.putBoolean("is_dark_theme", it == "dark"); hifdhTouched = true
            prefs("Settings").edit().putBoolean("is_dark_theme", it == "dark").apply()
        }
        (state["arabicFont"] as? String)?.let {
            hifdhEdit.putString("arabic_font", it); hifdhTouched = true
        }
        (state["arabicScale"] as? Number)?.let {
            hifdhEdit.putFloat("arabic_scale", it.toFloat()); hifdhTouched = true
        }
        (state["translationScale"] as? Number)?.let {
            hifdhEdit.putFloat("translation_scale", it.toFloat()); hifdhTouched = true
        }
        (state["translationId"] as? Number)?.let {
            hifdhEdit.putInt("translation_id", it.toInt()); hifdhTouched = true
        }
        (state["tafsirId"] as? Number)?.let {
            hifdhEdit.putInt("tafsir_id", it.toInt()); hifdhTouched = true
        }
        (state["reciterId"] as? Number)?.let {
            hifdhEdit.putInt("reciter_id", it.toInt()); hifdhTouched = true
        }
        (state["tajweedEnabled"] as? Boolean)?.let {
            hifdhEdit.putBoolean("is_tajweed_enabled", it); hifdhTouched = true
        }
        (state["wordTapBehavior"] as? String)?.let {
            hifdhEdit.putString("word_tap_behavior", it); hifdhTouched = true
        }

        // hifdhJson section → hifdh_settings keys.
        (state["hifdhJson"] as? String)?.let { raw ->
            parseJsonObject(raw)?.let { h ->
                (h["history"] as? Map<String, Any?>)?.let {
                    hifdhEdit.putString("hifdh_history", gson.toJson(it)); hifdhTouched = true
                }
                (h["goals"] as? List<*>)?.let {
                    hifdhEdit.putString("hifdh_goals", gson.toJson(it)); hifdhTouched = true
                }
                (h["memorizedAyahs"] as? List<*>)?.let { list ->
                    hifdhEdit.putStringSet(
                        "memorized_ayahs",
                        list.mapNotNull { it?.toString() }.toMutableSet(),
                    )
                    hifdhTouched = true
                }
                (h["transitionLinks"] as? List<*>)?.let { list ->
                    hifdhEdit.putStringSet(
                        "transition_links",
                        list.mapNotNull { it?.toString() }.toMutableSet(),
                    )
                    hifdhTouched = true
                }
            }
        }
        if (hifdhTouched) hifdhEdit.apply()

        // DAO restores — existing insert methods only; union semantics (never delete,
        // matching web "deletions don't propagate").
        val dao = dao ?: return
        parseJsonArray(state["bookmarksJson"] as? String)?.forEach { raw ->
            (raw as? Map<String, Any?>) ?: return@forEach
            val verseKey = raw["verseKey"]?.toString() ?: return@forEach
            runSuspendCatching {
                dao.insertBookmark(
                    BookmarkEntity(
                        // BookmarkEntity PK defaults to 1 (single-row); derive a stable id
                        // per verse so unioned bookmarks don't clobber each other.
                        id = verseKey.hashCode(),
                        verseKey = verseKey,
                        chapterId = raw["chapterId"]?.toString()?.toIntOrNull()
                            ?: verseKey.substringBefore(":").toIntOrNull() ?: 0,
                        surahName = raw["surahName"]?.toString() ?: "",
                        timestamp = longOf(raw["timestamp"]).takeIf { it > 0 }
                            ?: System.currentTimeMillis(),
                    ),
                )
            }
        }
        parseJsonObject(state["collectionsJson"] as? String)?.let { c ->
            @Suppress("UNCHECKED_CAST")
            val collections = (c["collections"] as? List<Map<String, Any?>>) ?: emptyList()
            @Suppress("UNCHECKED_CAST")
            val items = (c["items"] as? List<Map<String, Any?>>) ?: emptyList()
            collections.forEach { col ->
                val id = col["id"]?.toString()?.toLongOrNull()
                    ?: col["id"]?.toString().hashCode().toLong()
            runSuspendCatching {
                dao.insertCollection(
                        CollectionEntity(
                            id = id,
                            name = col["name"]?.toString() ?: "Collection",
                            createdAt = longOf(col["createdAt"]).takeIf { it > 0 }
                                ?: System.currentTimeMillis(),
                        ),
                    )
                }
                items.filter {
                    it["collectionId"]?.toString()?.toLongOrNull() == id ||
                        it["collectionId"]?.toString() == col["id"]?.toString()
                }.forEach { item ->
                    val verseKey = item["verseKey"]?.toString() ?: return@forEach
                runSuspendCatching {
                    dao.insertCollectionItem(
                            CollectionItemEntity(
                                collectionId = id,
                                verseKey = verseKey,
                                chapterId = item["chapterId"]?.toString()?.toIntOrNull()
                                    ?: verseKey.substringBefore(":").toIntOrNull() ?: 0,
                                surahName = item["surahName"]?.toString() ?: "",
                                addedAt = longOf(item["addedAt"]).takeIf { it > 0 }
                                    ?: System.currentTimeMillis(),
                            ),
                        )
                    }
                }
            }
        }
        parseJsonObject(state["plansJson"] as? String)?.let { p ->
            suspend fun putCache(key: String, value: Any?) {
                if (value == null) return
                runSuspendCatching {
                    dao.insertCacheEntry(ApiResponseCacheEntity(key, gson.toJson(value)))
                }
            }
            putCache("planner_all_plans", p["allPlans"])
            (p["activePlannerId"] as? String)?.takeIf { it.isNotBlank() }?.let {
                runSuspendCatching { dao.insertCacheEntry(ApiResponseCacheEntity("planner_active_id", it)) }
            }
            putCache("planner_archived_plans", p["archivedPlans"])
            putCache("planner_active_plan", p["activePlan"])
            (p["bookmarks"] as? Map<String, Any?>)?.forEach { (id, v) ->
                putCache("planner_bookmarks_$id", v)
            }
            (p["sessionTotals"] as? Map<String, Any?>)?.forEach { (id, v) ->
                putCache("planner_sessions_$id", v)
            }
        }
        parseJsonObject(state["readingStatsJson"] as? String)?.let { r ->
            @Suppress("UNCHECKED_CAST")
            val sessions = (r["sessions"] as? List<Map<String, Any?>>) ?: emptyList()
            val seen = daoSessions().mapNotNullTo(HashSet()) {
                longOf(it["timestamp"]).takeIf { t -> t > 0 }
            }
            sessions.forEach { s ->
                val ts = longOf(s["timestamp"])
                if (ts > 0 && !seen.add(ts)) return@forEach // dedupe by timestamp
                runSuspendCatching {
                    dao.insertReadingSession(
                        ReadingSessionEntity(
                            date = s["date"]?.toString() ?: "",
                            duration = longOf(s["duration"]),
                            type = s["type"]?.toString() ?: "reading",
                            chapterId = s["chapterId"]?.toString()?.toIntOrNull(),
                            timestamp = ts.takeIf { it > 0 } ?: System.currentTimeMillis(),
                        ),
                    )
                }
            }
            runSuspendCatching { dao.pruneReadingSessions() } // keep newest 500 (web parity)
            @Suppress("UNCHECKED_CAST")
            val recent = (r["recentlyRead"] as? List<Map<String, Any?>>) ?: emptyList()
            recent.forEach { item ->
                val chapterId = item["chapterId"]?.toString()?.toIntOrNull() ?: return@forEach
                runSuspendCatching {
                    dao.upsertRecentlyRead(
                        RecentlyReadEntity(
                            chapterId = chapterId,
                            chapterName = item["chapterName"]?.toString() ?: "",
                            verseKey = item["verseKey"]?.toString(),
                            timestamp = longOf(item["timestamp"]).takeIf { it > 0 }
                                ?: System.currentTimeMillis(),
                        ),
                    )
                }
            }
            runSuspendCatching { dao.pruneRecentlyRead() } // keep newest 5 (web parity)
        }
    }

    // ------------------------------------------------------------------ DAO dumps ---

    private suspend fun daoBookmarks(): List<Map<String, Any?>> {
        val dao = dao ?: return emptyList()
        return dao.getAllBookmarks().first().map {
            mapOf(
                "verseKey" to it.verseKey,
                "chapterId" to it.chapterId,
                "surahName" to it.surahName,
                "timestamp" to it.timestamp,
            )
        }
    }

    private suspend fun daoCollections(): List<Map<String, Any?>> {
        val dao = dao ?: return emptyList()
        return dao.getAllCollections().first().map {
            mapOf("id" to it.id, "name" to it.name, "createdAt" to it.createdAt)
        }
    }

    private suspend fun daoCollectionItems(): List<Map<String, Any?>> {
        val dao = dao ?: return emptyList()
        return dao.getAllCollectionItems().first().map {
            mapOf(
                "collectionId" to it.collectionId,
                "verseKey" to it.verseKey,
                "chapterId" to it.chapterId,
                "surahName" to it.surahName,
                "addedAt" to it.addedAt,
            )
        }
    }

    private fun daoHifdhJson(hifdh: SharedPreferences): String {
        return gson.toJson(
            mapOf(
                "history" to hifdh.getString("hifdh_history", null)?.let { parseJsonAny(it) },
                "goals" to hifdh.getString("hifdh_goals", null)?.let { parseJsonAny(it) },
                "memorizedAyahs" to (
                    hifdh.getStringSet("memorized_ayahs", null)?.toList() ?: emptyList<String>()
                    ),
                "transitionLinks" to (
                    hifdh.getStringSet("transition_links", null)?.toList() ?: emptyList<String>()
                    ),
            ),
        )
    }

    private suspend fun daoSessions(): List<Map<String, Any?>> {
        val dao = dao ?: return emptyList()
        return runSuspendCatching {
            dao.getAllReadingSessions().first().map {
                mapOf(
                    "date" to it.date,
                    "duration" to it.duration,
                    "type" to it.type,
                    "chapterId" to it.chapterId,
                    "timestamp" to it.timestamp,
                )
            }
        }.getOrNull() ?: emptyList()
    }

    private suspend fun daoRecent(): List<Map<String, Any?>> {
        val dao = dao ?: return emptyList()
        return runSuspendCatching {
            dao.getRecentlyRead().first().map {
                mapOf(
                    "chapterId" to it.chapterId,
                    "chapterName" to it.chapterName,
                    "verseKey" to it.verseKey,
                    "timestamp" to it.timestamp,
                )
            }
        }.getOrNull() ?: emptyList()
    }

    // ------------------------------------------------------------------ prefs util ---

    private fun prefs(name: String): SharedPreferences =
        context.getSharedPreferences(name, Context.MODE_PRIVATE)

    private fun lastSyncAt(): Long =
        runCatching { prefs(SYNC_PREFS).getLong(KEY_LAST_SYNC_AT, 0L) }.getOrDefault(0L)

    /** Last successful sync timestamp (0 = never). For UI status rows. */
    fun lastSyncAtPublic(): Long = lastSyncAt()

    private fun saveLastSync(ts: Long) {
        runCatching { prefs(SYNC_PREFS).edit().putLong(KEY_LAST_SYNC_AT, ts).apply() }
    }

    // ------------------------------------------------------------- section merges ---

    /** Union two JSON arrays of objects by identity key; unparseable input → incoming wins. */
    private fun mergeJsonList(baseRaw: String?, incomingRaw: String?, key: String): String? {
        val incoming = parseJsonArray(incomingRaw) ?: return incomingRaw
        val base = parseJsonArray(baseRaw) ?: emptyList()
        val map = LinkedHashMap<String, Any?>()
        base.forEach { map[identityOf(it, key)] = it }
        incoming.forEach { map[identityOf(it, key)] = it }
        return gson.toJson(map.values.toList())
    }

    private fun mergeCollectionsJson(baseRaw: String?, incomingRaw: String?): String? {
        val incoming = parseJsonObject(incomingRaw) ?: return incomingRaw
        val base = parseJsonObject(baseRaw)
        @Suppress("UNCHECKED_CAST")
        val baseCols = (base?.get("collections") as? List<Map<String, Any?>>) ?: emptyList()
        @Suppress("UNCHECKED_CAST")
        val incCols = (incoming["collections"] as? List<Map<String, Any?>>) ?: emptyList()
        @Suppress("UNCHECKED_CAST")
        val baseItems = (base?.get("items") as? List<Map<String, Any?>>) ?: emptyList()
        @Suppress("UNCHECKED_CAST")
        val incItems = (incoming["items"] as? List<Map<String, Any?>>) ?: emptyList()
        // Union collections by id (incoming wins per collection)…
        val cols = LinkedHashMap<String, Any?>()
        baseCols.forEach { cols[it["id"]?.toString() ?: canonical(it)] = it }
        incCols.forEach { cols[it["id"]?.toString() ?: canonical(it)] = it }
        // …and items by (collectionId, verseKey).
        val items = LinkedHashMap<String, Any?>()
        (baseItems + incItems).forEach { item ->
            items["${item["collectionId"]}|${item["verseKey"]}" ] = item
        }
        return gson.toJson(mapOf("collections" to cols.values.toList(), "items" to items.values.toList()))
    }

    private fun mergePlansJson(baseRaw: String?, incomingRaw: String?): String? {
        val incoming = parseJsonObject(incomingRaw) ?: return incomingRaw
        val base = parseJsonObject(baseRaw) ?: emptyMap()
        val out = base.toMutableMap()
        // allPlans / archivedPlans: union by id; active ids/plans: incoming wins (LWW).
        out["allPlans"] = unionById(
            base["allPlans"] as? List<*>,
            incoming["allPlans"] as? List<*>,
        ) ?: incoming["allPlans"]
        out["archivedPlans"] = unionById(
            base["archivedPlans"] as? List<*>,
            incoming["archivedPlans"] as? List<*>,
        ) ?: incoming["archivedPlans"]
        for (k in listOf("activePlannerId", "activePlan")) {
            if (incoming.containsKey(k)) out[k] = incoming[k]
        }
        out["bookmarks"] = mergeValue(
            base["bookmarks"] as? Map<String, Any?> ?: emptyMap<String, Any?>(),
            incoming["bookmarks"],
        )
        out["sessionTotals"] = mergeValue(
            base["sessionTotals"] as? Map<String, Any?> ?: emptyMap<String, Any?>(),
            incoming["sessionTotals"],
        )
        return gson.toJson(out)
    }

    private fun mergeHifdhJson(baseRaw: String?, incomingRaw: String?): String? {
        val incoming = parseJsonObject(incomingRaw) ?: return incomingRaw
        val base = parseJsonObject(baseRaw) ?: emptyMap()
        val out = base.toMutableMap()
        out["history"] = mergeValue(
            base["history"] as? Map<String, Any?> ?: emptyMap<String, Any?>(),
            incoming["history"],
        )
        val goals = unionById(base["goals"] as? List<*>, incoming["goals"] as? List<*>)
        if (goals != null || incoming.containsKey("goals")) out["goals"] = goals
        out["memorizedAyahs"] = unionLists(
            base["memorizedAyahs"] as? List<*> ?: emptyList<Any?>(),
            incoming["memorizedAyahs"] as? List<*> ?: emptyList<Any?>(),
        )
        out["transitionLinks"] = unionLists(
            base["transitionLinks"] as? List<*> ?: emptyList<Any?>(),
            incoming["transitionLinks"] as? List<*> ?: emptyList<Any?>(),
        )
        return gson.toJson(out)
    }

    private fun mergeReadingStatsJson(baseRaw: String?, incomingRaw: String?): String? {
        val incoming = parseJsonObject(incomingRaw) ?: return incomingRaw
        val base = parseJsonObject(baseRaw) ?: emptyMap()
        // sessions: union by timestamp (incoming wins), keep newest 500.
        val sessions = LinkedHashMap<String, Any?>()
        @Suppress("UNCHECKED_CAST")
        ((base["sessions"] as? List<Map<String, Any?>>) ?: emptyList()).forEach {
            sessions[longOf(it["timestamp"]).toString()] = it
        }
        @Suppress("UNCHECKED_CAST")
        ((incoming["sessions"] as? List<Map<String, Any?>>) ?: emptyList()).forEach {
            sessions[longOf(it["timestamp"]).toString()] = it
        }
        // recentlyRead: latest timestamp wins per chapter, most-recent-first, cap 5.
        val recent = LinkedHashMap<String, Map<String, Any?>>()
        @Suppress("UNCHECKED_CAST")
        val all = ((base["recentlyRead"] as? List<Map<String, Any?>>) ?: emptyList()) +
            ((incoming["recentlyRead"] as? List<Map<String, Any?>>) ?: emptyList())
        all.forEach { item ->
            val id = item["chapterId"]?.toString() ?: canonical(item)
            val prev = recent[id]
            recent[id] = if (prev == null || longOf(item["timestamp"]) >= longOf(prev["timestamp"])) {
                item
            } else {
                prev
            }
        }
        val recentMerged = recent.values
            .sortedByDescending { longOf(it["timestamp"]) }
            .take(5)
        return gson.toJson(
            mapOf(
                "sessions" to sessions.values.toList().takeLast(500),
                "recentlyRead" to recentMerged,
            ),
        )
    }

    // ---------------------------------------------------------------- generic merge ---

    @Suppress("UNCHECKED_CAST")
    private fun mergeValue(base: Any?, incoming: Any?): Any? {
        if (incoming == null) return base
        if (base == null) return incoming
        if (base is List<*> && incoming is List<*>) return unionLists(base, incoming)
        if (base is Map<*, *> && incoming is Map<*, *>) {
            val out = (base as Map<String, Any?>).toMutableMap()
            for ((k, v) in (incoming as Map<String, Any?>)) out[k] = mergeValue(out[k], v)
            return out
        }
        return incoming // scalar / position: incoming wins (LWW)
    }

    private fun unionById(base: List<*>?, incoming: List<*>?): List<Any?>? {
        if (base == null && incoming == null) return null
        val map = LinkedHashMap<String, Any?>()
        base?.forEach { map[(it as? Map<*, *>)?.get("id")?.toString() ?: canonical(it)] = it }
        incoming?.forEach { map[(it as? Map<*, *>)?.get("id")?.toString() ?: canonical(it)] = it }
        return map.values.toList()
    }

    private fun unionLists(base: List<*>, incoming: List<*>): List<Any?> {
        val map = LinkedHashMap<String, Any?>()
        base.forEach { map[canonical(it)] = it }
        incoming.forEach { map[canonical(it)] = it }
        return map.values.toList()
    }

    private fun identityOf(item: Any?, key: String): String {
        return (item as? Map<*, *>)?.get(key)?.toString() ?: canonical(item)
    }

    private fun canonical(v: Any?): String {
        return when (v) {
            null -> "null"
            is String -> "\"$v\""
            is Number, is Boolean -> v.toString()
            is Map<*, *> -> v.entries.sortedBy { it.key.toString() }
                .joinToString(",", "{", "}") { (k, item) -> "\"$k\":${canonical(item)}" }
            is List<*> -> v.joinToString(",", "[", "]") { canonical(it) }
            is Set<*> -> v.map { canonical(it) }.sorted().joinToString(",", "[", "]")
            else -> "\"$v\""
        }
    }

    // ------------------------------------------------------------------ json/time ---

    private val mapType = object : TypeToken<Map<String, Any?>>() {}.type
    private val listType = object : TypeToken<List<Any?>>() {}.type

    private fun parseJsonObject(raw: String?): Map<String, Any?>? {
        if (raw.isNullOrBlank()) return null
        return runCatching { gson.fromJson<Map<String, Any?>>(raw, mapType) }.getOrNull()
    }

    private fun parseJsonArray(raw: String?): List<Any?>? {
        if (raw.isNullOrBlank()) return null
        return runCatching { gson.fromJson<List<Any?>>(raw, listType) }.getOrNull()
    }

    private fun parseJsonAny(raw: String): Any? {
        return runCatching { gson.fromJson(raw, Any::class.java) }.getOrNull()
    }

    /** Accepts epoch-ms numbers and Appwrite ISO-8601 strings; 0 when unparseable. */
    private fun parseUpdatedAt(v: Any?): Long {
        return when (v) {
            is Number -> v.toLong()
            is String -> {
                v.toLongOrNull()
                    ?: runCatching {
                        val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
                        fmt.timeZone = TimeZone.getTimeZone("UTC")
                        fmt.parse(v)?.time
                    }.getOrNull()
                    ?: runCatching {
                        val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
                        fmt.timeZone = TimeZone.getTimeZone("UTC")
                        fmt.parse(v)?.time
                    }.getOrNull() ?: 0L
            }
            else -> 0L
        }
    }

    private fun longOf(v: Any?): Long {
        return when (v) {
            is Number -> v.toLong()
            is String -> v.toLongOrNull() ?: v.toDoubleOrNull()?.toLong() ?: 0L
            else -> 0L
        }
    }

    private fun Map<String, Any?>.withoutNulls(): Map<String, Any> {
        val out = LinkedHashMap<String, Any>(size)
        for ((k, v) in this) if (v != null) out[k] = v
        return out
    }

    private data class RemoteDoc(val state: Map<String, Any?>, val updatedAt: Long)
}
