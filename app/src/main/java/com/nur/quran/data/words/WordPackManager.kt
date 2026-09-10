package com.nur.quran.data.words

import android.content.Context
import com.google.gson.Gson
import com.nur.quran.data.api.ApiVerse
import com.nur.quran.data.api.ApiWord
import com.nur.quran.data.api.QuranApi
import com.nur.quran.data.db.dao.QuranDao
import com.nur.quran.data.db.entities.WordEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Per-chapter download progress snapshot, keyed in [WordPackManager.packStates]
 * by chapter id.
 */
data class WordPackStatus(
    val downloaded: Int,
    val total: Int,
    val isDownloading: Boolean = false,
    val error: String? = null
)

/**
 * Offline-first word-translation packs: proactively fetches a chapter's words
 * WITH translations and persists them into Room, so
 * `WordTranslationTooltipDrawer` can read Room first and work offline.
 *
 * API used: `GET verses/by_chapter/{chapterId}` with `translations="20"`,
 * `words=true`, `mushaf=null` (API default) and explicit
 * [WORD_FIELDS] ([WORD_FIELDS] value below). The explicit list is required
 * because [com.nur.quran.data.mushaf.Mushaf.wordFields] omits
 * `transliteration` (and `char_type_name`), which [ApiWord] parses as
 * `transliteration?.text` — without it the tooltip has no transliteration
 * offline. Verse/word mapping mirrors
 * `QuranRepository.refreshVersesByChapter`: `translation =
 * apiWord.translation?.text`, `transliteration = apiWord.transliteration?.text`.
 *
 * Persistence notes (DAO has no update-translation or count method — only
 * `getWordsForVerse`, `getWordsForVerses` and REPLACE `insertWords`):
 * - Never touches the `verses` table (no deletes, no verse writes).
 * - Merges per verse: rows are matched by id when the stored words already
 *   carry API ids; otherwise (offline synthetic seeds whose ids are
 *   `verseId * 100 + index`) they are matched by position so existing rows
 *   are UPDATED in place instead of duplicated. Network
 *   translation/transliteration fill stored nulls; existing text/line fields
 *   are preserved unless blank/zero.
 * - [isChapterCached] is prefs-based: no synchronous word-count DAO method
 *   exists to sanity-check from a non-suspend function.
 *
 * `downloadChapterWords` never throws on network/DB failures (they land in
 * [WordPackStatus.error]); [CancellationException] is re-thrown so
 * [cancelDownload] stays cooperative with structured concurrency.
 */
@Singleton
class WordPackManager @Inject constructor(
    private val quranApi: QuranApi,
    private val quranDao: QuranDao,
    gson: Gson,
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _packStates = MutableStateFlow<Map<Int, WordPackStatus>>(emptyMap())
    /** Live word-pack progress per chapter id. */
    val packStates: StateFlow<Map<Int, WordPackStatus>> = _packStates.asStateFlow()

    private val jobs = ConcurrentHashMap<Int, Job>()

    /**
     * Fetches every verse of [chapterId] with word translations and upserts
     * the words into Room, reporting progress per verse in [packStates].
     * Marks the chapter cached in prefs only when all verses persist without
     * error. No-op when a download for the chapter is already in flight.
     * Cancel via [cancelDownload]. Never throws (except cooperative
     * [CancellationException]).
     */
    suspend fun downloadChapterWords(chapterId: Int) {
        if (_packStates.value[chapterId]?.isDownloading == true) return
        val callerJob = currentCoroutineContext()[Job]
        if (callerJob != null) jobs[chapterId] = callerJob
        try {
            withContext(Dispatchers.IO) {
                _packStates.update { it + (chapterId to WordPackStatus(0, 0, true, null)) }

                val verses = mutableListOf<ApiVerse>()
                var page = 1
                var pagesFetched = 0
                while (true) {
                    ensureActive()
                    val response = quranApi.getVersesByChapter(
                        chapterId = chapterId,
                        translations = TRANSLATION_ID,
                        words = true,
                        wordFields = WORD_FIELDS,
                        mushaf = null,
                        page = page,
                        perPage = PER_PAGE
                    )
                    verses += response.verses
                    pagesFetched++
                    val next = response.pagination.next_page
                    if (next == null || pagesFetched >= MAX_PAGES) break
                    page = next
                }

                val total = verses.size
                if (total == 0) {
                    _packStates.update {
                        it + (chapterId to WordPackStatus(0, 0, false, "empty_response"))
                    }
                    return@withContext
                }
                _packStates.update { it + (chapterId to WordPackStatus(0, total, true, null)) }

                var done = 0
                var lastError: String? = null
                for (verse in verses) {
                    ensureActive()
                    try {
                        upsertVerseWords(verse)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        lastError = e.message ?: e::class.simpleName ?: "persist_failed"
                    }
                    done++
                    _packStates.update {
                        it + (chapterId to WordPackStatus(done, total, true, lastError))
                    }
                }

                if (lastError == null) {
                    markCached(chapterId)
                    _packStates.update {
                        it + (chapterId to WordPackStatus(total, total, false, null))
                    }
                } else {
                    _packStates.update {
                        it + (chapterId to WordPackStatus(done, total, false, lastError))
                    }
                }
            }
        } catch (e: CancellationException) {
            _packStates.update { current ->
                val prev = current[chapterId]
                current + (chapterId to (prev?.copy(isDownloading = false, error = null)
                    ?: WordPackStatus(0, 0, false, null)))
            }
            throw e
        } catch (e: Exception) {
            _packStates.update { current ->
                val prev = current[chapterId]
                val total = prev?.total ?: 0
                val done = prev?.downloaded ?: 0
                current + (chapterId to WordPackStatus(
                    downloaded = done,
                    total = total,
                    isDownloading = false,
                    error = e.message ?: e::class.simpleName ?: "download_failed"
                ))
            }
        } finally {
            if (callerJob != null) jobs.remove(chapterId, callerJob) else jobs.remove(chapterId)
        }
    }

    /** Cancels an in-flight [downloadChapterWords] for the given chapter. */
    fun cancelDownload(chapterId: Int) {
        jobs[chapterId]?.cancel()
    }

    /**
     * True when the chapter was fully downloaded before (prefs mark).
     * Prefs-only: [QuranDao] exposes no synchronous word-count method usable
     * from this non-suspend function.
     */
    fun isChapterCached(chapterId: Int): Boolean =
        prefs.getStringSet(KEY_CACHED, emptySet())?.contains(chapterId.toString()) == true

    /** Ids of all chapters fully downloaded before. */
    fun getCachedChapters(): Set<Int> =
        prefs.getStringSet(KEY_CACHED, emptySet())?.mapNotNull { it.toIntOrNull() }?.toSet()
            ?: emptySet()

    // ── Merge + upsert ──────────────────────────────────────────────────────

    /**
     * Upserts one verse's network words into Room without touching the
     * `verses` table. Matches stored rows by id when they already carry API
     * ids, else by position (offline synthetic seeds) so rows are updated in
     * place and never duplicated.
     */
    private suspend fun upsertVerseWords(apiVerse: ApiVerse) {
        val apiWords = apiVerse.words
        if (apiWords.isNullOrEmpty()) return
        val existing = try {
            quranDao.getWordsForVerse(apiVerse.id)
        } catch (_: Exception) {
            emptyList()
        }
        val byId = existing.associateBy { it.id }
        val matchById = apiWords.any { byId.containsKey(it.id) }
        val byPosition = existing.associateBy { it.position }
        val merged = apiWords.map { apiWord ->
            val current = if (matchById) byId[apiWord.id] else byPosition[apiWord.position]
            if (current == null) {
                newWordEntity(apiVerse.id, apiWord)
            } else {
                current.copy(
                    translation = apiWord.translation?.text ?: current.translation,
                    transliteration = apiWord.transliteration?.text ?: current.transliteration,
                    textUthmani = current.textUthmani ?: apiWord.text_uthmani,
                    textIndopak = current.textIndopak ?: apiWord.text_indopak,
                    textQpcHafs = current.textQpcHafs ?: apiWord.text_qpc_hafs,
                    textUthmaniTajweed = current.textUthmaniTajweed ?: apiWord.text_uthmani_tajweed,
                    lineNumber = if (current.lineNumber == 0) apiWord.line_number ?: 0 else current.lineNumber
                )
            }
        }
        quranDao.insertWords(merged)
    }

    private fun newWordEntity(verseId: Int, apiWord: ApiWord) = WordEntity(
        id = apiWord.id,
        verseId = verseId,
        position = apiWord.position,
        textUthmani = apiWord.text_uthmani,
        textIndopak = apiWord.text_indopak,
        textQpcHafs = apiWord.text_qpc_hafs,
        textUthmaniTajweed = apiWord.text_uthmani_tajweed,
        translation = apiWord.translation?.text,
        transliteration = apiWord.transliteration?.text,
        charTypeName = apiWord.char_type_name.ifBlank { "word" },
        lineNumber = apiWord.line_number ?: 0
    )

    private fun markCached(chapterId: Int) {
        val current = prefs.getStringSet(KEY_CACHED, emptySet())?.toMutableSet() ?: mutableSetOf()
        if (current.add(chapterId.toString())) {
            prefs.edit().putStringSet(KEY_CACHED, current).apply()
        }
    }

    companion object {
        private const val PREFS_NAME = "word_packs"
        private const val KEY_CACHED = "cached_chapters"

        /** Verse-level translation resource, same as QuranRepository ("20"). */
        private const val TRANSLATION_ID = "20"

        /**
         * Explicit word fields: Mushaf.wordFields() lacks `transliteration`
         * (parsed by ApiWord as `transliteration?.text`) and `char_type_name`,
         * so the full set is requested directly. `mushaf` stays null (API
         * default); line/page numbers are still requested when available.
         */
        const val WORD_FIELDS =
            "text_uthmani,text_indopak,text_qpc_hafs,translation,transliteration,char_type_name,line_number,page_number"

        /** Longest chapter is 286 verses; one page normally covers a chapter. */
        private const val PER_PAGE = 300
        private const val MAX_PAGES = 10
    }
}
