package com.nur.quran.data.tafsir

import android.content.Context
import com.google.gson.Gson
import com.nur.quran.data.api.QuranApi
import com.nur.quran.data.db.dao.QuranDao
import com.nur.quran.data.db.entities.ApiResponseCacheEntity
import com.nur.quran.data.words.WordPackManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/** Top-level alias for the supported packs (mirrors [TafsirPackManager.supported]). */
val SUPPORTED_TAFSIRS = listOf(
    169 to "Ibn Kathir (Abridged)",
    168 to "Ibn Kathir (Full)",
    817 to "Al-Muyassar",
    16 to "Tafsir al-Jalalayn"
)

data class TafsirPackStatus(
    val downloaded: Int,
    val total: Int = 114,
    val isDownloading: Boolean = false,
    val error: String? = null
)

/**
 * Downloads / deletes whole-tafsir offline packs.
 *
 * Storage contract (matches `QuranRepository.getTafsirForChapter`):
 * - one [ApiResponseCacheEntity] row per chapter
 * - key format: `"tafsir_${tafsirId}_chapter_$chapterId"`
 * - value: `gson.toJson(TafsirResponse)` — same type the repository caches.
 *
 * Completion is tracked in SharedPreferences file `"tafsir_packs"`
 * (StringSet of downloaded tafsir ids).
 */
@Singleton
class TafsirPackManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val quranDao: QuranDao,
    private val quranApi: QuranApi,
    private val gson: Gson,
    private val wordPackManager: WordPackManager
) {
    val supported: List<Pair<Int, String>> = SUPPORTED_TAFSIRS

    private val prefs get() =
        context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)

    private val _packStates = MutableStateFlow(
        supported.associate { (id, _) ->
            id to TafsirPackStatus(
                downloaded = if (isDownloaded(id)) TOTAL else 0
            )
        }
    )
    val packStates: StateFlow<Map<Int, TafsirPackStatus>> = _packStates.asStateFlow()

    private val jobs = mutableMapOf<Int, Job>()
    private val lock = Any()
    /** Tafsir id -> word chapter currently downloading in [downloadPackWithWords]. */
    private val activeWordChapters = mutableMapOf<Int, Int>()

    fun isDownloaded(tafsirId: Int): Boolean =
        prefs.getStringSet(PREFS_KEY, emptySet())?.contains(tafsirId.toString()) == true

    fun getDownloaded(): Set<Int> =
        prefs.getStringSet(PREFS_KEY, emptySet())
            ?.mapNotNull { it.toIntOrNull() }
            ?.toSet() ?: emptySet()

    /**
     * Downloads chapters 1..114 for [tafsirId] into the API-response cache.
     * Polite: 100ms delay every 10 chapters. Resumes: cached chapters are skipped.
     * Never throws for network/IO errors (surfaced in [packStates]); only
     * coroutine [CancellationException] from [cancelPack] propagates.
     */
    suspend fun downloadPack(tafsirId: Int) {
        synchronized(lock) {
            jobs[tafsirId]?.takeIf { it.isActive }?.let { return }
        }
        val job = currentCoroutineContext()[Job]
        if (job != null) synchronized(lock) { jobs[tafsirId] = job }
        try {
            _packStates.update { it + (tafsirId to TafsirPackStatus(0, TOTAL, true, null)) }
            for (c in 1..TOTAL) {
                currentCoroutineContext().ensureActive()
                val key = cacheKey(tafsirId, c)
                try {
                    if (quranDao.getCacheEntry(key) == null) {
                        val response = quranApi.getTafsirByChapter(tafsirId, c)
                        quranDao.insertCacheEntry(ApiResponseCacheEntity(key, gson.toJson(response)))
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _packStates.update {
                        it + (tafsirId to TafsirPackStatus(c - 1, TOTAL, false, e.message ?: "Download failed"))
                    }
                    return
                }
                _packStates.update { it + (tafsirId to TafsirPackStatus(c, TOTAL, true, null)) }
                if (c % 10 == 0) delay(100)
            }
            markDownloaded(tafsirId, true)
            _packStates.update { it + (tafsirId to TafsirPackStatus(TOTAL, TOTAL, false, null)) }
        } catch (e: CancellationException) {
            _packStates.update { current ->
                val done = current[tafsirId]?.downloaded ?: 0
                current + (tafsirId to TafsirPackStatus(done, TOTAL, false, null))
            }
            throw e
        } finally {
            synchronized(lock) {
                if (jobs[tafsirId] === job) jobs.remove(tafsirId)
            }
        }
    }

    /**
     * Downloads the whole tafsir pack for [tafsirId] and then the word-by-word
     * translations for chapters 1..114.
     *
     * Runs [downloadPack] first; only when the tafsir part completes without
     * error do the word chapters follow, sequentially via
     * [WordPackManager.downloadChapterWords] with per-chapter `runCatching`
     * (a failed chapter is recorded in [WordPackManager.packStates] and
     * skipped, never aborting the run). Tafsir progress is reported in
     * [packStates] exactly as [downloadPack] does; word progress is visible in
     * [WordPackManager.packStates] (UI reads both).
     *
     * Cooperative cancellation: the combined job is registered under the same
     * tafsir key in the jobs map during the word phase, with
     * [ensureActive] checked each chapter, so [cancelPack] stops both parts.
     * Network/IO errors land in [packStates]; only coroutine
     * [CancellationException] from [cancelPack] propagates.
     */
    suspend fun downloadPackWithWords(tafsirId: Int) {
        synchronized(lock) {
            jobs[tafsirId]?.takeIf { it.isActive }?.let { return }
        }
        // NB: the combined job is deliberately NOT registered before
        // downloadPack — it registers this same Job itself, and pre-registering
        // would trip its already-in-flight guard. It is (re-)registered below
        // for the word phase after downloadPack's finally block removes it.
        try {
            downloadPack(tafsirId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _packStates.update { current ->
                val done = current[tafsirId]?.downloaded ?: 0
                current + (tafsirId to TafsirPackStatus(done, TOTAL, false, e.message ?: "Download failed"))
            }
            return
        }
        // Tafsir part must have completed without error; otherwise stop here.
        if (_packStates.value[tafsirId]?.error != null) return
        val job = currentCoroutineContext()[Job]
        if (job != null) synchronized(lock) { jobs[tafsirId] = job }
        try {
            for (c in 1..TOTAL) {
                currentCoroutineContext().ensureActive()
                synchronized(lock) { activeWordChapters[tafsirId] = c }
                try {
                    runCatching { wordPackManager.downloadChapterWords(c) }
                        .onFailure { e -> if (e is CancellationException) throw e }
                } finally {
                    synchronized(lock) {
                        if (activeWordChapters[tafsirId] == c) activeWordChapters.remove(tafsirId)
                    }
                }
            }
        } catch (e: CancellationException) {
            _packStates.update { current ->
                val done = current[tafsirId]?.downloaded ?: 0
                current + (tafsirId to TafsirPackStatus(done, TOTAL, false, null))
            }
            throw e
        } catch (e: Exception) {
            _packStates.update { current ->
                val done = current[tafsirId]?.downloaded ?: 0
                current + (tafsirId to TafsirPackStatus(done, TOTAL, false, e.message ?: "Download failed"))
            }
        } finally {
            synchronized(lock) {
                if (jobs[tafsirId] === job) jobs.remove(tafsirId)
                activeWordChapters.remove(tafsirId)
            }
        }
    }

    /** Cancels an in-flight [downloadPack] or [downloadPackWithWords]. No-op when idle. */
    fun cancelPack(tafsirId: Int) {
        synchronized(lock) { activeWordChapters[tafsirId] }?.let { chapterId ->
            runCatching { wordPackManager.cancelDownload(chapterId) }
        }
        synchronized(lock) { jobs[tafsirId] }?.cancel()
    }

    /** Deletes all cached chapters for [tafsirId] + unmarks prefs. Never throws. */
    suspend fun deletePack(tafsirId: Int) {
        try {
            cancelPack(tafsirId)
            quranDao.clearCacheByPrefix("tafsir_${tafsirId}_chapter_")
            markDownloaded(tafsirId, false)
            _packStates.update { it + (tafsirId to TafsirPackStatus(0, TOTAL, false, null)) }
        } catch (e: Exception) {
            _packStates.update { current ->
                val done = current[tafsirId]?.downloaded ?: 0
                current + (tafsirId to TafsirPackStatus(done, TOTAL, false, e.message ?: "Delete failed"))
            }
        }
    }

    private fun markDownloaded(tafsirId: Int, downloaded: Boolean) {
        val current = prefs.getStringSet(PREFS_KEY, emptySet())?.toMutableSet() ?: mutableSetOf()
        if (downloaded) current.add(tafsirId.toString()) else current.remove(tafsirId.toString())
        prefs.edit().putStringSet(PREFS_KEY, current).apply()
    }

    companion object {
        const val TOTAL = 114
        const val PREFS_FILE = "tafsir_packs"
        const val PREFS_KEY = "downloaded_ids"
        fun cacheKey(tafsirId: Int, chapterId: Int): String =
            "tafsir_${tafsirId}_chapter_$chapterId"
    }
}
