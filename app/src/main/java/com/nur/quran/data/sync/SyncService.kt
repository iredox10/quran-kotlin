package com.nur.quran.data.sync

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nur.quran.data.db.dao.QuranDao
import com.nur.quran.data.mushaf.fontNameToId
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

private const val SYNC_PREFS = "sync_state"
private const val KEY_LAST_SYNC_AT = "last_sync_at"
private const val TAG = "NurSync"

/**
 * Cloud sync orchestration for the shared Heroku backend. Snapshot construction
 * belongs to [SnapshotBuilder]. Each account has one server-owned sync document
 * with `userId` and JSON-string `stateData`; server timestamps are the watermark.
 * Legacy flattened documents are read for migration compatibility.
 *
 * Merge rules (mirror of quran-app/src/utils/syncMerge.js `mergeStateInto`):
 * - Settings / scalars: LAST-WRITE-WINS (incoming wins; absent local keys serialize as
 *   null and are omitted from the map, so a merge never clobbers remote with defaults).
 * - JSON-string sections are parsed, then unioned by identity key —
 *   bookmarks by `verseKey`; collections by `id` (items unioned by `verseKey`);
 *   plans by `id` (planner bookmarks/session maps deep-merged); hifdh goals by `id`,
 *   history deep-merged, memorized ayahs/transition links deduped; reading sessions by
 *   `timestamp` (cap 500); recentlyRead by `chapterId`, latest timestamp wins, cap 20 —
 *   then re-serialized.
 * - Trade-off (same as web): deletions don't propagate.
 * - The flat web keys (web's `getSyncableState` names: `bookmarks`, `collections`,
 *   `planners`, `readingSessions`, `recentlyRead`, …) merge generically through
 *   [mergeValue]: lists union, keyed maps deep-merge, scalars LWW. The
 *   `pageVisitCounts` per-key max lives with the merge-rules work, not here.
 *
 * Apply side ([applyState]): every web flat key that has an Android home is
 * written to that home, and the legacy Android blobs are accepted alongside the
 * web shape for the transition — `hifdhJson`/`bookmarksJson`/`collectionsJson`/
 * `plansJson`/`readingStatsJson` apply FIRST, so the web-named keys win when a
 * document carries both. Destinations: scalars → `hifdh_settings` / `Settings` /
 * `PlannerSettings` / `tour_prefs` prefs (see [applyDisplaySettings],
 * [applySourceSettings], [applyPlannerSettings], [applyHifdhState],
 * [applyTourState]); data → Room + planner cache rows (see [applyLibraryData],
 * [applyReadingStats], [applyPlannerData]). Each section is isolated
 * (`runSuspendCatching`) and every mapper drops a value it cannot read, so a
 * malformed or unknown key never wipes local state — the same rule web follows.
 * The key → store value translations are pure and unit tested in
 * [SyncApplyMappings] (see SyncApplyMappingsTest).
 *
 * [push]/[pull] return Boolean and [sync] returns a status string. Coroutine cancellation propagates.
 */
@Singleton
class SyncService @Inject constructor(
    private val rnApiClient: com.nur.quran.data.api.rn.RnApiClient,
    private val snapshotBuilder: SnapshotBuilder?,
    private val dao: QuranDao?,
    @ApplicationContext private val context: Context,
) {
    private val gson = Gson()
    private val syncMutex = Mutex()

    /** Like [runSuspendCatching] but names the section so a silent apply failure is visible in logcat. */
    private suspend fun applySection(name: String, block: suspend () -> Unit) {
        try {
            block()
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            Log.w(TAG, "apply section '$name' failed", e)
        }
    }

    /**
     * Emitted after [applyState] writes a pulled remote snapshot to prefs/DB, so
     * ViewModels holding prefs-snapshot StateFlows (SurahViewModel's hifdh data)
     * can re-read them. Without this, a restore pull updates prefs but the UI keeps
     * showing the empty snapshot taken at construction until app restart.
     */
    private val _syncApplied = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val syncApplied: SharedFlow<Unit> = _syncApplied.asSharedFlow()

    // ------------------------------------------------------------------ public API ---

    /** Build local snapshot and upload (merge local over remote, update-or-create). */
    suspend fun push(): Boolean = syncMutex.withLock {
        try {
            val userId = currentUserId() ?: return@withLock false
            pushInternal(userId)
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Download remote doc; if remote is newer than local, union-merge into local state and
     * apply (prefs via `apply()`, DAO via existing inserts). True when ok or already current.
     */
    suspend fun pull(): Boolean = syncMutex.withLock {
        try {
            val userId = currentUserId() ?: run {
                Log.w(TAG, "pull: no userId (signed out)")
                return@withLock false
            }
            val beforeRead = localStateMap()
            val remote = fetchRemote(userId) ?: run {
                Log.w(TAG, "pull: no remote doc for user $userId")
                return@withLock false
            }
            // Web parity (CloudSync.jsx:102): gate ONLY on the persisted watermark.
            // The old maxOf() with the snapshot build clock (≈ now) could never fire.
            // lastSyncAt()==0 (fresh login/reset) force-applies like web's login pull.
            checkOwner(userId)
            val gate = remote.updatedAt > lastSyncAt()
            Log.d(TAG, "pull: remote.updatedAt=${remote.updatedAt} lastSyncAt=${lastSyncAt()} gate=$gate stateKeys=${remote.state.keys}")
            if (gate) {
                applyIncoming(userId, remote.state, remote.updatedAt, beforeRead)
            }
            true
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            Log.w(TAG, "pull failed", e)
            false
        }
    }

    /**
     * One-shot bidirectional sync. Returns "pushed" / "pulled" / "up-to-date" /
     * "signed-out" / "error:<message>".
     */
    suspend fun sync(): String = syncMutex.withLock {
        try {
            val userId = currentUserId() ?: return@withLock "signed-out"
            val beforeRead = localStateMap()
            val remote = fetchRemote(userId)
            val lastSync = lastSyncAt()
            val local = localStateMap()
            checkOwner(userId)
            Log.d(
                TAG,
                "sync: user=$userId remote=${if (remote == null) "null" else "ts=${remote.updatedAt} keys=${remote.state.keys}"} lastSync=$lastSync localKeys=${local.keys}"
            )
            if (remote != null && remote.updatedAt > lastSync) {
                applyIncoming(userId, remote.state, remote.updatedAt, beforeRead)
                // Web parity (CloudSync.jsx:116-139): push back when the merge
                // changed anything, so the cloud converges to the union.
                if (syncDelta(remote.state,localStateMap()).isNotEmpty() && !pushInternal(userId)) {
                    return@withLock "error:push failed"
                }
                "pulled"
            } else if (remote == null) {
                if (pushInternal(userId)) "pushed" else "error:push failed"
            } else {
                if (syncDelta(remote.state,local).isNotEmpty()) {
                    if (pushInternal(userId)) "pushed" else "error:push failed"
                } else "up-to-date"
            }
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            Log.w(TAG, "sync failed", e)
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
                // Flat web keys use the exact web merge rules (per-key max,
                // dayNumber unions, deep maps) instead of plain LWW.
                else -> out[field] = mergeField(field, out[field], inc)
            }
        }
        return out
    }

    // ------------------------------------------------------------------ push/pull ---

    /** runCatching variant for blocks that call suspend functions. */
    private suspend fun <T> runSuspendCatching(block: suspend () -> T): Result<T> {
        return try {
            Result.success(block())
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun checkOwner(userId: String) {
        check(currentUserId() == userId) { "Account changed during sync" }
    }

    private fun sameState(first: Map<String, Any?>, second: Map<String, Any?>): Boolean =
        gson.toJsonTree(SyncContract.stripStateMetadata(first)) ==
            gson.toJsonTree(SyncContract.stripStateMetadata(second))

    /** Persisted identity from the Heroku session, shared by web and mobile. */
    private fun currentUserId(): String? = rnApiClient.storedUserId()

    private fun outbox() = context.getSharedPreferences("sync_outbox", Context.MODE_PRIVATE)
    private fun syncKey(userId: String, suffix: String) = "${rnApiClient.baseUrl}:$userId:$suffix"
    private fun acknowledged(userId: String): Map<String, Any?> =
        outbox().getString(syncKey(userId,"baseline"),null)?.let { parseJsonObject(it) } ?: emptyMap()
    private fun pending(userId: String): Map<String, Any?>? =
        outbox().getString(syncKey(userId,"pending"),null)?.let { parseJsonObject(it) }

    private suspend fun applyIncoming(userId: String, state: Map<String, Any?>, stamp: Long, before: Map<String, Any?>) {
        val edited = syncDelta(before,localStateMap())
        @Suppress("UNCHECKED_CAST")
        val waiting = pending(userId)?.get("changes") as? Map<String, Any?> ?: emptyMap()
        var merged = mergeStateInto(localStateMap(),state)
        merged = mergeStateInto(mergeStateInto(merged,waiting),edited)
        checkOwner(userId)
        applyState(merged)
        checkOwner(userId)
        outbox().edit().putString(syncKey(userId,"baseline"),gson.toJson(state)).apply()
        saveLastSync(maxOf(stamp,lastSyncAt()))
    }

    /** Upload directly; changed sections merge under the backend's per-account lock. */
    private suspend fun pushInternal(userId: String): Boolean {
        try {
            repeat(3) {
                val local = localStateMap()
                val saved = pending(userId)
                val changes = if (saved != null) {
                    @Suppress("UNCHECKED_CAST")
                    (saved["changes"] as? Map<String, Any?>) ?: error("Invalid pending sync")
                } else syncDelta(acknowledged(userId),local)
                if (changes.isEmpty()) return true
                val mutationId = saved?.get("id")?.toString() ?: java.util.UUID.randomUUID().toString()
                @Suppress("UNCHECKED_CAST")
                val sent = (saved?.get("snapshot") as? Map<String, Any?>) ?: local
                checkOwner(userId)
                // commit() makes the retry packet durable before it leaves this device.
                check(outbox().edit().putString(syncKey(userId,"pending"),gson.toJson(mapOf(
                    "id" to mutationId,"changes" to changes,"snapshot" to sent
                ))).commit()) { "Could not save pending backup" }
                val doc = try {
                    rnApiClient.patchSyncDoc(userId,changes,mutationId)
                } catch (error: retrofit2.HttpException) {
                    if (error.code() !in listOf(404,405)) throw error
                    // Safe fallback during a rolling backend deployment.
                    val current = fetchRemote(userId)
                    val union = mergeStateInto(current?.state ?: emptyMap(),changes)
                    val stamp = rnApiClient.putSyncDoc(SyncContract.buildPushPayload(userId,gson.toJson(union)) +
                        ("baseUpdatedAt" to (current?.updatedAt ?: 0L))) ?: error("Missing sync timestamp")
                    com.nur.quran.data.api.rn.RnSyncDoc(userId,gson.toJson(union),stamp,emptyMap())
                }
                checkOwner(userId)
                val canonical = doc.stateData?.let { parseJsonObject(it) }
                val expected = acknowledged(userId) + sent
                outbox().edit().remove(syncKey(userId,"pending"))
                    .putString(syncKey(userId,"baseline"),gson.toJson(canonical ?: sent)).commit()
                if (canonical != null && !sameState(canonical,expected)) applyIncoming(userId,canonical,doc.updatedAtMs,sent)
                else saveLastSync(maxOf(doc.updatedAtMs,lastSyncAt()))
            }
            return true
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (_: Exception) { return false }
    }

    /** Failed reads propagate so a blind upload cannot replace the remote union. */
    private suspend fun fetchRemote(userId: String): RemoteDoc? {
        val doc = rnApiClient.getSyncDoc()
        checkOwner(userId)
        doc ?: return null
        check(doc.updatedAtMs > 0) { "Sync document is missing its server timestamp" }
        if (doc.userId != null && doc.userId.isNotBlank() && doc.userId != userId) {
            // Fail closed: treating a protocol mismatch as empty could overwrite remote data.
            error("Sync document belongs to a different account")
        }
        return RemoteDoc(
            state = SyncContract.stateFromDoc(
                doc.legacyFields + mapOf(
                    SyncContract.FIELD_STATE_DATA to (doc.stateData ?: ""),
                ),
            ) { parseJsonObject(it) },
            updatedAt = doc.updatedAtMs,
            id = "rn-sync",
        )
    }

    /**
     * Web-shaped docs carry `stateData` (a JSON string); legacy Android docs predate it and
     * keep their state in flattened attributes, which are read until the next push writes
     * the migrated state back behind `stateData`.
     */
    private fun extractState(data: Map<String, Any?>): Map<String, Any?> =
        SyncContract.stateFromDoc(data) { parseJsonObject(it) }

    // ----------------------------------------------------------------- local state ---

    private suspend fun localStateMap(): Map<String, Any?> {
        val base: Map<String, Any?> = if (snapshotBuilder != null) {
            runSuspendCatching {
                snapshotBuilder.snapshotToMap(snapshotBuilder.build())
            }.getOrNull() ?: emptyMap()
        } else {
            buildLocalStateMap()
        }
        // Flat web keys (web getSyncableState) ride alongside the legacy blobs so
        // the web client reads everything Android knows. Absent data is omitted.
        return runSuspendCatching {
            SyncContract.stripStateMetadata(base.toMutableMap().also { it.putAll(flatWebKeys()) })
        }.getOrNull() ?: SyncContract.stripStateMetadata(base)
    }

    /**
     * Web-named flat keys derived from local stores. Mirrors the apply side
     * (applyDisplaySettings/applyHifdhState/...) so push and pull speak the
     * same shapes. Every section is best-effort and omitted on failure.
     */
    private suspend fun flatWebKeys(): Map<String, Any?> {
        val out = mutableMapOf<String, Any?>()
        val hifdh = prefs("hifdh_settings")
        val settings = prefs("Settings")
        val plannerPrefs = prefs("PlannerSettings")
        val tours = prefs("tour_prefs")
        // Settings scalars.
        if (hifdh.contains("arabic_scale")) {
            out["fontSize"] = fontScaleToWebFontSizeIndex(hifdh.getFloat("arabic_scale", 1f))
        }
        if (hifdh.contains("translation_scale")) {
            out["translationFontSize"] = fontScaleToWebFontSizeIndex(hifdh.getFloat("translation_scale", 1f))
        }
        normalizeWordTooltipBehavior(hifdh.getString("word_tap_behavior", null))?.let {
            out["wordTooltipBehavior"] = it
        }
        hifdh.getString("arabic_font", null)?.let { out["arabicFontId"] = fontNameToId(it) }
        if (settings.contains("daily_reading_goal")) {
            out["dailyReadingGoal"] = settings.getInt("daily_reading_goal", 20)
        }
        if (plannerPrefs.contains("show_intention_prompt")) {
            out["intentionPromptEnabled"] = plannerPrefs.getBoolean("show_intention_prompt", true)
        }
        val activePrayers = plannerPrefs.getStringSet("active_prayers", null)
        val readPreference = plannerPrefs.getString("reading_preference", null)
        if (activePrayers != null || readPreference != null) {
            out["prayerSettings"] = mapOf(
                "activePrayers" to (activePrayers?.toList() ?: emptyList<String>()),
                "readPreference" to (readPreference ?: "after"),
            )
        }
        // Tours / visits.
        tours.all.mapNotNull { (key, value) ->
            if (key.startsWith("tour_") && value == true) key.removePrefix("tour_") else null
        }.takeIf { it.isNotEmpty() }?.let { out["completedTours"] = it }
        tours.all.mapNotNull { (key, value) ->
            if (key.startsWith("coachmark_") && value == true) key.removePrefix("coachmark_") else null
        }.takeIf { it.isNotEmpty() }?.let { out["dismissedCoachmarks"] = it }
        tours.all.mapNotNull { (key, value) ->
            if (key.startsWith("visit_")) key.removePrefix("visit_") to ((value as? Number)?.toInt() ?: 0) else null
        }.toMap().takeIf { it.isNotEmpty() }?.let { out["pageVisitCounts"] = it }
        // Library arrays (web shapes; timestamp is epoch ms like web createdAt).
        runSuspendCatching { daoBookmarks() }.getOrNull()?.let { rows ->
            out["bookmarks"] = rows.map {
                mapOf(
                    "verseKey" to it["verseKey"], "surahName" to it["surahName"],
                    "chapterId" to it["chapterId"], "createdAt" to it["timestamp"],
                )
            }
        }
        runSuspendCatching {
            val collections = daoCollections()
            val itemsByCollection = daoCollectionItems().groupBy { it["collectionId"] }
            collections.map { collection ->
                val items = (itemsByCollection[collection["id"]] ?: emptyList()).map {
                    mapOf(
                        "verseKey" to it["verseKey"], "surahName" to it["surahName"],
                        "chapterId" to it["chapterId"],
                    )
                }
                mapOf(
                    "id" to collection["id"], "name" to collection["name"],
                    "createdAt" to collection["createdAt"], "items" to items,
                )
            }
        }.getOrNull()?.let { out["collections"] = it }
        // Reading stats.
        runSuspendCatching { daoSessions() }.getOrNull()?.let { out["readingSessions"] = it }
        runSuspendCatching { daoRecent() }.getOrNull()?.let { out["recentlyRead"] = it }
        // Hifdh (web top-level shapes; history/goals stay verbatim for lenient parsers).
        hifdh.getStringSet("memorized_ayahs", null)?.toList()?.sorted()?.let { out["memorizedAyahs"] = it }
        hifdh.getStringSet("memorized_surahs", null)?.mapNotNull { it.toIntOrNull() }?.sorted()?.let {
            out["memorizedSurahs"] = it
        }
        hifdh.getString("hifdh_history", null)?.let { parseJsonAny(it) }?.let { out["hifdhHistory"] = it }
        hifdh.getString("hifdh_goals", null)?.let { parseJsonAny(it) }?.let { out["hifdhGoals"] = it }
        // Planner (parsed structures so they merge as objects, not strings).
        val plans = runSuspendCatching {
            dao?.getCacheEntry("planner_all_plans")?.dataJson?.takeIf { it.isNotBlank() }?.let { parseJsonAny(it) }
        }.getOrNull()
        @Suppress("UNCHECKED_CAST")
        val planList = (plans as? List<*>)?.filterIsInstance<Map<String, Any?>>() ?: emptyList()
        if (planList.isNotEmpty()) out["planners"] = planList
        runSuspendCatching {
            dao?.getCacheEntry("planner_active_id")?.dataJson?.takeIf { it.isNotBlank() }
        }.getOrNull()?.let { out["activePlannerId"] = it }
        runSuspendCatching {
            dao?.getCacheEntry("planner_archived_plans")?.dataJson?.takeIf { it.isNotBlank() }?.let { parseJsonAny(it) }
        }.getOrNull()?.let { out["archivedPlanners"] = it }
        if (planList.isNotEmpty()) {
            val planIds = planList.mapNotNull { it["id"]?.toString() }
            val bookmarksByPlan = mutableMapOf<String, Any?>()
            val timersByPlan = mutableMapOf<String, Any?>()
            val reflectionsByPlan = mutableMapOf<String, Any?>()
            for (planId in planIds) {
                runSuspendCatching {
                    dao?.getCacheEntry("planner_bookmarks_$planId")?.dataJson?.takeIf { it.isNotBlank() }?.let { parseJsonAny(it) }
                }.getOrNull()?.let { bookmarksByPlan[planId] = it }
                runSuspendCatching {
                    dao?.getCacheEntry("planner_sessions_$planId")?.dataJson?.takeIf { it.isNotBlank() }?.let {
                        com.nur.quran.data.planner.PlannerWire.sessionTimersFromJson(it)[planId]
                    }
                }.getOrNull()?.let { days ->
                    timersByPlan[planId] = days.mapValues { (_, timer) ->
                        mapOf("startedAt" to timer.startedAt, "totalSeconds" to timer.totalSeconds)
                    }.mapKeys { (day, _) -> day.toString() }
                }
                runSuspendCatching {
                    dao?.getCacheEntry("planner_reflections_$planId")?.dataJson?.takeIf { it.isNotBlank() }?.let {
                        com.nur.quran.data.planner.PlannerWire.reflectionsFromJson(it)[planId]
                    }
                }.getOrNull()?.let { days ->
                    reflectionsByPlan[planId] = days.mapValues { (_, reflection) ->
                        mapOf("text" to reflection.text, "createdAt" to com.nur.quran.data.planner.PlannerWire.isoFromEpochMs(reflection.createdAt))
                    }.mapKeys { (day, _) -> day.toString() }
                }
            }
            if (bookmarksByPlan.isNotEmpty()) out["plannerBookmarks"] = bookmarksByPlan
            if (timersByPlan.isNotEmpty()) out["plannerSessionTimers"] = timersByPlan
            if (reflectionsByPlan.isNotEmpty()) {
                out["plannerReflections"] = reflectionsByPlan
            } else {
                // Fall back to plan-embedded notes so reflections still cross devices.
                plannerReflectionsFromPlans(planList).takeIf { it.isNotEmpty() }?.let {
                    out["plannerReflections"] = it
                }
            }
        }
        return out
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
        if (hifdh.contains("translation_id")) out["translationId"] = hifdh.getInt("translation_id", 20)
        if (hifdh.contains("tafsir_id")) out["tafsirId"] = hifdh.getInt("tafsir_id", 169)
        // reciter_id deliberately omitted: web uses string reciter keys and an
        // int here would clobber them under last-write-wins merge.
        if (hifdh.contains("is_tajweed_enabled")) {
            out["tajweedEnabled"] = hifdh.getBoolean("is_tajweed_enabled", false)
        }
        if (hifdh.contains("mushaf_preset")) {
            hifdh.getString("mushaf_preset", null)?.let {
                out["mushafPreset"] = it
                out["mushafId"] = it
            }
        }
        if (hifdh.contains("is_reading_mode")) {
            out["readingMode"] = hifdh.getBoolean("is_reading_mode", false)
        } else if (hifdh.contains("is_translation_enabled")) {
            out["readingMode"] = !hifdh.getBoolean("is_translation_enabled", true)
        }
        if (hifdh.contains("is_translation_enabled")) {
            out["translationEnabled"] = hifdh.getBoolean("is_translation_enabled", true)
        } else if (hifdh.contains("is_reading_mode")) {
            out["translationEnabled"] = !hifdh.getBoolean("is_reading_mode", false)
        }
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

    /**
     * @suppress
     * Test seam: runs [applyState] outside `pull()`, so JVM tests can drive a remote
     * payload through the apply path (prefs + DAO writes) without a transport round trip.
     */
    @VisibleForTesting
    suspend fun applyRemoteState(state: Map<String, Any?>) = applyState(state)

    private suspend fun applyState(state: Map<String, Any?>) {
        // Legacy Android blobs apply first, so the web-named flat keys
        // (web's `getSyncableState`) win when a document carries both.
        applySection("hifdhJson") { applyHifdhJson(state) }
        applySection("displaySettings") { applyDisplaySettings(state) }
        applySection("sourceSettings") { applySourceSettings(state) }
        applySection("plannerSettings") { applyPlannerSettings(state) }
        applySection("hifdhState") { applyHifdhState(state) }
        applySection("tourState") { applyTourState(state) }
        applySection("libraryJson") { applyLibraryJson(state) }
        applySection("libraryData") { applyLibraryData(state) }
        applySection("readingStats") { applyReadingStats(state) }
        applySection("plannerData") { applyPlannerData(state) }
        Log.d(TAG, "applyState: done for keys=${state.keys}")
        // Nudge prefs-snapshot ViewModels to re-read what we just wrote, so a
        // restore shows up immediately instead of waiting for an app restart.
        runCatching { _syncApplied.tryEmit(Unit) }
    }

    /**
     * Web's flat hifdh keys → `hifdh_settings`. History and goals are written
     * back verbatim (web's own snake_case card / ISO-date shape), which is what
     * `HifdhStore` and its lenient parsers read.
     */
    private suspend fun applyHifdhState(state: Map<String, Any?>) {
        val hifdh = mutableMapOf<String, Any?>()
        webMemorizedAyahSet(state["memorizedAyahs"])?.let { hifdh["memorized_ayahs"] = it }
        webMemorizedSurahSet(state["memorizedSurahs"])?.let { hifdh["memorized_surahs"] = it }
        asMap(state["hifdhHistory"])?.let { hifdh["hifdh_history"] = gson.toJson(it) }
        asList(state["hifdhGoals"])?.let { hifdh["hifdh_goals"] = gson.toJson(it) }
        applyPrefs("hifdh_settings", hifdh)
    }

    /** Legacy Android `hifdhJson` blob → hifdh_settings keys. */
    private fun applyHifdhJson(state: Map<String, Any?>) {
        val hifdh = mutableMapOf<String, Any?>()
        (state["hifdhJson"] as? String)?.let { raw ->
            parseJsonObject(raw)?.let { h ->
                (h["history"] as? Map<String, Any?>)?.let {
                    hifdh["hifdh_history"] = gson.toJson(it)
                }
                (h["goals"] as? List<*>)?.let { hifdh["hifdh_goals"] = gson.toJson(it) }
                (h["memorizedAyahs"] as? List<*>)?.let { list ->
                    hifdh["memorized_ayahs"] = list.mapNotNull { it?.toString() }.toSet()
                }
                (h["transitionLinks"] as? List<*>)?.let { list ->
                    hifdh["transition_links"] = list.mapNotNull { it?.toString() }.toSet()
                }
            }
        }
        applyPrefs("hifdh_settings", hifdh)
    }

    /**
     * Web's `completedTours` / `dismissedCoachmarks` / `pageVisitCounts` →
     * `tour_prefs` `tour_` / `coachmark_` / `visit_` keys. Only flags the
     * remote actually carries are written, so local tours survive (web does not
     * propagate deletions either).
     */
    private suspend fun applyTourState(state: Map<String, Any?>) {
        val tours = mutableMapOf<String, Any?>()
        webFlagIds(state["completedTours"])?.forEach { tours["tour_$it"] = true }
        webFlagIds(state["dismissedCoachmarks"])?.forEach { tours["coachmark_$it"] = true }
        webPageVisits(state["pageVisitCounts"])?.forEach { (page, count) ->
            tours["visit_$page"] = count
        }
        applyPrefs("tour_prefs", tours)
    }

    /**
     * Web's flat `readingSessions` / `recentlyRead` arrays → Room. Sessions are
     * deduped by `timestamp` (the identity key syncMerge unions on) and capped
     * at 500, recently-read at 5 per chapter, latest stamp wins — web parity.
     */
    private suspend fun applyReadingStats(state: Map<String, Any?>) {
        val d = dao ?: return
        webReadingSessionRows(state["readingSessions"])?.let { rows ->
            val seen = daoSessions().mapNotNullTo(HashSet()) {
                longOf(it["timestamp"]).takeIf { t -> t > 0 }
            }
            rows.forEach { row ->
                if (!seen.add(row.timestamp)) return@forEach // already applied
                runSuspendCatching {
                    d.insertReadingSession(
                        ReadingSessionEntity(
                            date = row.date,
                            duration = row.duration,
                            type = row.type,
                            chapterId = row.chapterId,
                            timestamp = row.timestamp,
                        ),
                    )
                }
            }
            runSuspendCatching { d.pruneReadingSessions() } // keep newest 500 (web parity)
        }
        webRecentlyReadRows(state["recentlyRead"])?.let { rows ->
            rows.forEach { row ->
                runSuspendCatching {
                    d.upsertRecentlyRead(
                        RecentlyReadEntity(
                            chapterId = row.chapterId,
                            chapterName = row.chapterName,
                            verseKey = row.verseKey,
                            timestamp = row.timestamp.takeIf { it > 0 }
                                ?: System.currentTimeMillis(),
                        ),
                    )
                }
            }
            runSuspendCatching { d.pruneRecentlyRead() } // keep newest 20 (web store parity)
        }
    }

    /**
     * Web's flat `planners` / `activePlannerId` / `archivedPlanners` plus the
     * per-plan keyed maps (`plannerBookmarks`, `plannerSessionTimers`,
     * `plannerReflections`) → `api_responses` cache rows. Plan lists are
     * unioned with the locally known ones, so a payload that arrived without
     * the local `planners` key cannot drop plans this device owns.
     */
    private suspend fun applyPlannerData(state: Map<String, Any?>) {
        val d = dao ?: return
        val reflections = webPlannerReflectionsByPlan(state["plannerReflections"]).orEmpty()
        val localPlans = runSuspendCatching { d.getCacheEntry("planner_all_plans")?.dataJson }.getOrNull()
        mergePlansById(localPlans, state["planners"])
            ?.map { plan -> planWithReflections(plan, reflections) }
            ?.let { plans ->
                runSuspendCatching {
                    d.insertCacheEntry(
                        ApiResponseCacheEntity("planner_all_plans", gson.toJson(plans)),
                    )
                }
            }
        asString(state["activePlannerId"])?.let { id ->
            runSuspendCatching { d.insertCacheEntry(ApiResponseCacheEntity("planner_active_id", id)) }
        }
        val localArchived = runSuspendCatching {
            d.getCacheEntry("planner_archived_plans")?.dataJson
        }.getOrNull()
        mergePlansById(localArchived, state["archivedPlanners"])?.let { archived ->
            runSuspendCatching {
                d.insertCacheEntry(
                    ApiResponseCacheEntity("planner_archived_plans", gson.toJson(archived)),
                )
            }
        }
        webPlannerExtraEntries(state["plannerBookmarks"])?.forEach { (id, entry) ->
            webPlannerBookmarkRows(entry)?.let { rows ->
                runSuspendCatching {
                    d.insertCacheEntry(
                        ApiResponseCacheEntity("planner_bookmarks_$id", gson.toJson(rows)),
                    )
                }
            }
        }
        // Web keeps { dayNumber: { totalSeconds, startedAt } }; Android persists
        // day → seconds only (startedAt is session state it never re-reads).
        webPlannerExtraEntries(state["plannerSessionTimers"])?.forEach { (id, entry) ->
            webPlannerSessionTotals(entry)?.let { totals ->
                runSuspendCatching {
                    d.insertCacheEntry(
                        ApiResponseCacheEntity("planner_sessions_$id", gson.toJson(totals)),
                    )
                }
            }
        }
    }

    /** Folds web `plannerReflections[planId]` into a plan's `assignmentReflections`. */
    private fun planWithReflections(
        plan: Any?,
        reflections: Map<String, Map<Int, String>>,
    ): Any? {
        if (reflections.isEmpty()) return plan
        val map = asMap(plan) ?: return plan
        return applyWebReflectionsToPlan(map, reflections[asString(map["id"])])
    }

    /**
     * Web's flat `bookmarks` / `collections` arrays → Room. A row that cannot
     * be read (no verse key, no usable id) is skipped instead of being written
     * with defaults, so one malformed entry cannot corrupt the local union.
     */
    private suspend fun applyLibraryData(state: Map<String, Any?>) {
        val d = dao ?: return
        webBookmarkRows(state["bookmarks"])?.forEach { row ->
            runSuspendCatching {
                d.insertBookmark(
                    BookmarkEntity(
                        // BookmarkEntity PK defaults to 1 (single-row); derive a stable id
                        // per verse so unioned bookmarks don't clobber each other.
                        id = row.verseKey.hashCode(),
                        verseKey = row.verseKey,
                        chapterId = row.chapterId,
                        surahName = row.surahName,
                        timestamp = row.createdAt.takeIf { it > 0 } ?: System.currentTimeMillis(),
                    ),
                )
            }
        }
        webCollectionRows(state["collections"])?.forEach { col ->
            runSuspendCatching {
                d.insertCollection(
                    CollectionEntity(
                        id = col.id,
                        name = col.name,
                        createdAt = col.createdAt.takeIf { it > 0 } ?: System.currentTimeMillis(),
                    ),
                )
            }
            col.items.forEach { item ->
                runSuspendCatching {
                    d.insertCollectionItem(
                        CollectionItemEntity(
                            collectionId = col.id,
                            verseKey = item.verseKey,
                            chapterId = item.chapterId,
                            surahName = item.surahName,
                            addedAt = item.addedAt.takeIf { it > 0 } ?: System.currentTimeMillis(),
                        ),
                    )
                }
            }
        }
    }

    /**
     * Legacy Android blobs (`bookmarksJson` / `collectionsJson` / `plansJson` /
     * `readingStatsJson`) → Room. Invoked by [applyState] BEFORE the flat web
     * sections, so a document carrying both applies the web shape last.
     * Restores use existing insert methods only; union semantics (never delete,
     * matching web "deletions don't propagate").
     */
    private suspend fun applyLibraryJson(state: Map<String, Any?>) {
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
            runSuspendCatching { dao.pruneRecentlyRead() } // keep newest 20 (web store parity)
        }
    }

    /**
     * Theme + typography. Web's canonical keys are `theme`, `fontSize` /
     * `translationFontSize` (1..8), `arabicFontId` and `wordTooltipBehavior`;
     * the Android blob's own names (`arabicFont`, `arabicScale`,
     * `translationScale`, `wordTapBehavior`) are still read, so a payload
     * written by either side of the transition applies.
     */
    private suspend fun applyDisplaySettings(state: Map<String, Any?>) {
        val hifdh = mutableMapOf<String, Any?>()
        val settings = mutableMapOf<String, Any?>()
        asString(state["theme"])?.takeIf { it == "dark" || it == "light" }?.let { theme ->
            val dark = theme == "dark"
            hifdh["is_dark_theme"] = dark
            settings["is_dark_theme"] = dark
        }
        (webFontIndexToScale(state["fontSize"]) ?: asFloat(state["arabicScale"]))?.let {
            hifdh["arabic_scale"] = it
        }
        (webFontIndexToScale(state["translationFontSize"]) ?: asFloat(state["translationScale"]))?.let {
            hifdh["translation_scale"] = it
        }
        (webArabicFontName(state["arabicFontId"]) ?: asString(state["arabicFont"]))?.let {
            hifdh["arabic_font"] = it
        }
        (
            webWordTapBehavior(state["wordTooltipBehavior"])
                ?: webWordTapBehavior(state["wordTapBehavior"])
            )?.let { hifdh["word_tap_behavior"] = it }
        applyPrefs("hifdh_settings", hifdh)
        applyPrefs("Settings", settings)
    }

    /**
     * Source, mushaf and reading mode. Web's canonical keys are `translationId`,
     * `tafsirId`, `reciterId`, `tajweedEnabled`, `mushafId` and `readingMode`;
     * the Android blob's `mushafPreset` / `translationEnabled` are still read.
     */
    private suspend fun applySourceSettings(state: Map<String, Any?>) {
        val hifdh = mutableMapOf<String, Any?>()
        asInt(state["translationId"])?.let { hifdh["translation_id"] = it }
        asInt(state["tafsirId"])?.let { hifdh["tafsir_id"] = it }
        asInt(state["reciterId"])?.let { hifdh["reciter_id"] = it }
        asBoolean(state["tajweedEnabled"])?.let { hifdh["is_tajweed_enabled"] = it }
        (webMushafPreset(state["mushafId"]) ?: webMushafPreset(state["mushafPreset"]))?.let {
            hifdh["mushaf_preset"] = it
        }
        // Reading mode (web `readingMode`, true = arabic-only) and its inverse
        // `translationEnabled`: translation-first, readingMode-last so the
        // canonical web key wins when both are present. Either way both prefs
        // keys stay in sync (readingMode == !translationEnabled).
        asBoolean(state["translationEnabled"])?.let { on ->
            hifdh["is_translation_enabled"] = on
            hifdh["is_reading_mode"] = !on
        }
        asBoolean(state["readingMode"])?.let { on ->
            hifdh["is_reading_mode"] = on
            hifdh["is_translation_enabled"] = !on
        }
        applyPrefs("hifdh_settings", hifdh)
    }

    /**
     * Settings the UI owns outside the reading prefs: web `dailyReadingGoal`
     * (ProfileScreen `daily_reading_goal` in `Settings`), and the planner's
     * `intentionPromptEnabled` + `prayerSettings` (PlannerSettings).
     */
    private suspend fun applyPlannerSettings(state: Map<String, Any?>) {
        val app = mutableMapOf<String, Any?>()
        val planner = mutableMapOf<String, Any?>()
        webDailyReadingGoal(state["dailyReadingGoal"])?.let { app["daily_reading_goal"] = it }
        asBoolean(state["intentionPromptEnabled"])?.let { planner["show_intention_prompt"] = it }
        webPrayerSettings(state["prayerSettings"])?.let { prayer ->
            prayer.activePrayers?.let { planner["active_prayers"] = it.toSet() }
            prayer.readPreference?.let { planner["reading_preference"] = it }
        }
        applyPrefs("Settings", app)
        applyPrefs("PlannerSettings", planner)
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
            // Full history (home keeps a short LIMIT-5 flow; web parity cap is 20).
            dao.getRecentlyReadHistory().first().map {
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

    /**
     * Applies type-tagged values to one prefs file. An empty map (nothing the
     * remote actually carried) is skipped, so a partial payload never rewrites
     * a file it has nothing to say about.
     */
    private fun applyPrefs(file: String, writes: Map<String, Any?>) {
        if (writes.isEmpty()) return
        val edit = prefs(file).edit()
        writes.forEach { (key, value) -> putPrefValue(edit, key, value) }
        edit.apply()
    }

    private fun putPrefValue(edit: SharedPreferences.Editor, key: String, value: Any?) {
        when (value) {
            null -> return
            is Boolean -> edit.putBoolean(key, value)
            is Int -> edit.putInt(key, value)
            is Long -> edit.putLong(key, value)
            is Float -> edit.putFloat(key, value)
            is Set<*> -> edit.putStringSet(key, value.mapNotNull { it?.toString() }.toSet())
            else -> edit.putString(key, value.toString())
        }
    }

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
        // recentlyRead: latest timestamp wins per chapter, most-recent-first, cap 20.
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
            .take(20) // Web parity: recentlyRead history cap (syncMerge.js slice(0, 20)).
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

    /**
     * Accepts epoch-ms numbers and Appwrite ISO-8601 strings; 0 when unparseable.
     *
     * Covers both the classic `...Z` form and offset-style `...+00:00` (Appwrite
     * 2.x servers), plus fractional seconds of any length via [java.time.Instant]
     * on API 26+ — falls back to SimpleDateFormat patterns below that.
     */
    private fun parseUpdatedAt(v: Any?): Long {
        return when (v) {
            is Number -> v.toLong()
            is String -> {
                v.toLongOrNull()
                    ?: runCatching { java.time.Instant.parse(v).toEpochMilli() }.getOrNull()
                    ?: isoFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", v)
                    ?: isoFormat("yyyy-MM-dd'T'HH:mm:ssXXX", v)
                    ?: isoFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", v)
                    ?: isoFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", v)
                    ?: 0L
            }
            else -> 0L
        }
    }

    private fun isoFormat(pattern: String, v: String): Long? = runCatching {
        val fmt = SimpleDateFormat(pattern, Locale.US)
        fmt.timeZone = TimeZone.getTimeZone("UTC")
        fmt.parse(v)?.time
    }.getOrNull()

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

    private data class RemoteDoc(
        val state: Map<String, Any?>,
        val updatedAt: Long,
        /** Id of the account's sync document — the update target, never a guess. */
        val id: String,
    )
}
