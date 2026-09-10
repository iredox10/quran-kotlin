package com.nur.quran.data.translation

import android.content.Context
import com.google.gson.Gson
import com.nur.quran.data.api.ApiVerse
import com.nur.quran.data.api.QuranApi
import com.nur.quran.data.db.dao.QuranDao
import com.nur.quran.data.db.entities.TranslationTextEntity
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

data class TranslationPackStatus(
    val downloaded: Int,
    val total: Int = 114,
    val isDownloading: Boolean = false,
    val error: String? = null
)

/**
 * Downloads / deletes whole-translation offline packs.
 *
 * Storage contract (the `verses` table holds only ONE translation, so packs
 * live in their own table):
 * - one [TranslationTextEntity] row per verse per translation edition
 * - table: `translation_texts(translationId, verseKey, text)`
 *
 * Completion is tracked in SharedPreferences file `"translation_packs"`
 * (StringSet of downloaded translation ids), mirroring `TafsirPackManager`.
 *
 * Progress is chapter-granular (downloaded/total chapters out of 114), like
 * tafsir packs. `downloadPack` never throws for network/IO errors (surfaced
 * in [packStates]); only coroutine [CancellationException] from [cancelPack]
 * propagates.
 */
@Singleton
class TranslationPackManager @Inject constructor(
    private val quranApi: QuranApi,
    private val quranDao: QuranDao,
    gson: Gson,
    @ApplicationContext private val context: Context
) {
    // Kept for constructor-signature parity (build fixer aligns call sites);
    // persistence needs no JSON — texts are stored as plain rows.
    @Suppress("unused")
    private val gson: Gson = gson

    /**
     * Supported editions, mapped from `TRANSLATION_EDITIONS:
     * List<TranslationEdition(id, name, language)>` in this package.
     */
    val supported: List<Pair<Int, String>> =
        TRANSLATION_EDITIONS.map { it.id to it.name }

    private val prefs get() =
        context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)

    private val _packStates = MutableStateFlow(
        supported.associate { (id, _) ->
            id to TranslationPackStatus(
                downloaded = if (isDownloaded(id)) TOTAL else 0
            )
        }
    )
    val packStates: StateFlow<Map<Int, TranslationPackStatus>> = _packStates.asStateFlow()

    private val jobs = mutableMapOf<Int, Job>()
    private val lock = Any()

    fun isDownloaded(translationId: Int): Boolean =
        prefs.getStringSet(PREFS_KEY, emptySet())?.contains(translationId.toString()) == true

    fun getDownloaded(): Set<Int> =
        prefs.getStringSet(PREFS_KEY, emptySet())
            ?.mapNotNull { it.toIntOrNull() }
            ?.toSet() ?: emptySet()

    /**
     * Downloads chapters 1..114 for [translationId] into `translation_texts`.
     * One `getVersesByChapter(translations=tid, perPage=300)` page-set per
     * chapter, following `next_page` like `WordPackManager` (a single page
     * covers the longest chapter, the loop is just a safety net). Polite:
     * 100ms delay every 10 chapters. Upserts are idempotent (REPLACE), so an
     * interrupted run can simply restart. Marks prefs only on clean
     * completion. Never throws for network/IO errors (surfaced in
     * [packStates]); only coroutine [CancellationException] from [cancelPack]
     * propagates.
     */
    suspend fun downloadPack(translationId: Int) {
        synchronized(lock) {
            jobs[translationId]?.takeIf { it.isActive }?.let { return }
        }
        val job = currentCoroutineContext()[Job]
        if (job != null) synchronized(lock) { jobs[translationId] = job }
        try {
            if (isDownloaded(translationId)) {
                _packStates.update { it + (translationId to TranslationPackStatus(TOTAL, TOTAL, false, null)) }
                return
            }
            _packStates.update { it + (translationId to TranslationPackStatus(0, TOTAL, true, null)) }
            for (c in 1..TOTAL) {
                currentCoroutineContext().ensureActive()
                try {
                    val verses = mutableListOf<ApiVerse>()
                    var page = 1
                    var pagesFetched = 0
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val response = quranApi.getVersesByChapter(
                            chapterId = c,
                            translations = translationId.toString(),
                            words = false,
                            page = page,
                            perPage = PER_PAGE
                        )
                        verses += response.verses
                        pagesFetched++
                        val next = response.pagination.next_page
                        if (next == null || pagesFetched >= MAX_PAGES) break
                        page = next
                    }
                    val rows = verses.mapNotNull { apiVerse ->
                        val text = apiVerse.translations
                            ?.firstOrNull { it.resource_id == translationId }?.text
                            ?: apiVerse.translations?.firstOrNull()?.text
                        if (text.isNullOrBlank()) null
                        else TranslationTextEntity(
                            translationId = translationId,
                            verseKey = apiVerse.verse_key,
                            text = text
                        )
                    }
                    if (rows.isNotEmpty()) quranDao.upsertTranslationTexts(rows)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _packStates.update {
                        it + (translationId to TranslationPackStatus(c - 1, TOTAL, false, e.message ?: "Download failed"))
                    }
                    return
                }
                _packStates.update { it + (translationId to TranslationPackStatus(c, TOTAL, true, null)) }
                if (c % 10 == 0) delay(100)
            }
            markDownloaded(translationId, true)
            _packStates.update { it + (translationId to TranslationPackStatus(TOTAL, TOTAL, false, null)) }
        } catch (e: CancellationException) {
            _packStates.update { current ->
                val done = current[translationId]?.downloaded ?: 0
                current + (translationId to TranslationPackStatus(done, TOTAL, false, null))
            }
            throw e
        } finally {
            synchronized(lock) {
                if (jobs[translationId] === job) jobs.remove(translationId)
            }
        }
    }

    /** Cancels an in-flight [downloadPack]. No-op when idle. */
    fun cancelPack(translationId: Int) {
        synchronized(lock) { jobs[translationId] }?.cancel()
    }

    /** Deletes all cached rows for [translationId] + unmarks prefs. Never throws. */
    suspend fun deletePack(translationId: Int) {
        try {
            cancelPack(translationId)
            quranDao.deleteTranslationPack(translationId)
            markDownloaded(translationId, false)
            _packStates.update { it + (translationId to TranslationPackStatus(0, TOTAL, false, null)) }
        } catch (e: Exception) {
            _packStates.update { current ->
                val done = current[translationId]?.downloaded ?: 0
                current + (translationId to TranslationPackStatus(done, TOTAL, false, e.message ?: "Delete failed"))
            }
        }
    }

    private fun markDownloaded(translationId: Int, downloaded: Boolean) {
        val current = prefs.getStringSet(PREFS_KEY, emptySet())?.toMutableSet() ?: mutableSetOf()
        if (downloaded) current.add(translationId.toString()) else current.remove(translationId.toString())
        prefs.edit().putStringSet(PREFS_KEY, current).apply()
    }

    companion object {
        const val TOTAL = 114
        /** Total verses in the mushaf; informational (completion uses the prefs mark, like tafsir). */
        const val TOTAL_VERSES = 6236
        const val PREFS_FILE = "translation_packs"
        const val PREFS_KEY = "downloaded_ids"

        /** Longest chapter is 286 verses; one page normally covers a chapter. */
        private const val PER_PAGE = 300
        private const val MAX_PAGES = 10
    }
}
