package com.nur.quran.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nur.quran.data.audio.AudioDownloadManager
import com.nur.quran.data.audio.AudioDownloadWorker
import com.nur.quran.data.audio.DownloadProgress
import com.nur.quran.data.audio.Reciters
import com.nur.quran.data.db.entities.ChapterEntity
import com.nur.quran.data.repository.QuranRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Downloads-manager ViewModel: one flat row per reciter.
 *
 * Verse keys per chapter are derived from the chapters flow
 * (`ChapterEntity.versesCount`, itself from `chapters.json` `verses_count`)
 * as `"$chapterId:$ayah"` for ayah 1..versesCount — never hardcoded.
 *
 * All work runs IO-scoped and never throws (every body is `runCatching`).
 * There is no `setPaused` on [AudioDownloadManager], so pause/resume are
 * best-effort: pause cancels in-flight work (manager jobs + worker), resume
 * clears stale worker state and refreshes so the user can restart.
 */
@HiltViewModel
class AudioPacksViewModel @Inject constructor(
    private val audioDownloadManager: AudioDownloadManager,
    private val repository: QuranRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    data class ReciterDlState(
        val reciterId: Int,
        val name: String,
        val style: String,
        val downloadedSurahs: Int,
        val totalSurahs: Int = 114,
        val bytes: Long,
        val isBusy: Boolean,
    )

    private val _reciterStates = MutableStateFlow(
        Reciters.ALL.map { r ->
            ReciterDlState(
                reciterId = r.id,
                name = r.name,
                style = r.style,
                downloadedSurahs = 0,
                bytes = 0L,
                isBusy = false,
            )
        }
    )
    val reciterStates: StateFlow<List<ReciterDlState>> = _reciterStates.asStateFlow()

    /** Live per-`"reciterId:chapterId"` progress, straight from the manager. */
    val downloadState: StateFlow<Map<String, DownloadProgress>> =
        audioDownloadManager.downloadState

    private val _totalBytes = MutableStateFlow(0L)
    val totalBytes: StateFlow<Long> = _totalBytes.asStateFlow()

    /** All 114 chapters (names + ayah counts) for the per-qari surah screen. */
    val chapters: StateFlow<List<ChapterEntity>> =
        repository.getChaptersFlow()
            .map { list -> list.sortedBy { it.id } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** chapterId -> versesCount, from the chapters flow. Never hardcoded. */
    @Volatile
    private var versesCountByChapter: Map<Int, Int> = emptyMap()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                repository.getChaptersFlow().collect { chapters ->
                    versesCountByChapter =
                        chapters.associate { it.id to it.versesCount }
                    refreshInternal()
                }
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                audioDownloadManager.downloadState.collect { refreshInternal() }
            }
        }
        refresh()
    }

    /** Recomputes per-reciter downloaded counts, bytes and busy flags. */
    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { refreshInternal() }
        }
    }

    private fun refreshInternal() {
        val live = audioDownloadManager.downloadState.value
        val states = Reciters.ALL.map { r ->
            val downloaded = runCatching {
                audioDownloadManager.getDownloadedChapters(r.id)
            }.getOrDefault(emptySet())
            val bytes = runCatching {
                audioDownloadManager.getStorageBytes(r.id)
            }.getOrDefault(0L)
            val busy = live.any { (key, p) ->
                p.isDownloading && key.startsWith("${r.id}:")
            }
            ReciterDlState(
                reciterId = r.id,
                name = r.name,
                style = r.style,
                downloadedSurahs = downloaded.size,
                bytes = bytes,
                isBusy = busy,
            )
        }
        _reciterStates.value = states
        _totalBytes.value = runCatching {
            audioDownloadManager.getStorageBytes(null)
        }.getOrDefault(states.sumOf { it.bytes })
    }

    /** Verse keys `"$chapterId:$ayah"` for ayah 1..versesCount (from chapters flow). */
    fun verseKeysFor(chapterId: Int): List<String> {
        val count = versesCountByChapter[chapterId] ?: 0
        if (count <= 0) return emptyList()
        return (1..count).map { "$chapterId:$it" }
    }

    /** True when the surah file set is fully downloaded for the reciter. */
    fun isSurahDownloaded(reciterId: Int, chapterId: Int): Boolean =
        runCatching { audioDownloadManager.getDownloadedChapters(reciterId).contains(chapterId) }
            .getOrDefault(false)

    private suspend fun ensureVerseCounts() {
        if (versesCountByChapter.isNotEmpty()) return
        versesCountByChapter = runCatching {
            repository.getChaptersFlow().firstOrNull()
        }.getOrNull()?.associate { it.id to it.versesCount } ?: emptyMap()
    }

    fun downloadSurah(
        reciterId: Int,
        chapterId: Int,
        verseKeys: List<String> = emptyList(),
        wifiOnly: Boolean = false,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val keys = verseKeys.ifEmpty { verseKeysFor(chapterId) }
                if (keys.isEmpty()) return@runCatching
                AudioDownloadWorker.enqueue(context, reciterId, chapterId, keys, wifiOnly)
                refreshInternal()
            }
        }
    }

    /** Enqueues every not-yet-downloaded chapter for the reciter as one chain. */
    fun downloadMushaf(reciterId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                ensureVerseCounts()
                val downloaded = runCatching {
                    audioDownloadManager.getDownloadedChapters(reciterId)
                }.getOrDefault(emptySet())
                val all = (1..114).mapNotNull { c ->
                    if (downloaded.contains(c)) return@mapNotNull null
                    val count = versesCountByChapter[c] ?: return@mapNotNull null
                    c to (1..count).map { "$c:$it" }
                }.toMap()
                if (all.isEmpty()) return@runCatching
                AudioDownloadWorker.enqueueMushaf(context, reciterId, all)
                refreshInternal()
            }
        }
    }

    /** Best-effort pause: cancels in-flight manager jobs + worker per chapter. */
    fun pauseReciter(reciterId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                inFlightChapters(reciterId).forEach { chapterId ->
                    runCatching {
                        audioDownloadManager.cancelDownload(reciterId, chapterId)
                    }
                    runCatching {
                        AudioDownloadWorker.cancel(context, reciterId, chapterId)
                    }
                }
                refreshInternal()
            }
        }
    }

    /**
     * Best-effort resume: no `setPaused` exists, so this clears any stale
     * worker state and refreshes; the user restarts via download actions.
     */
    fun resumeReciter(reciterId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                inFlightChapters(reciterId).forEach { chapterId ->
                    runCatching {
                        AudioDownloadWorker.cancel(context, reciterId, chapterId)
                    }
                }
                refreshInternal()
            }
        }
    }

    fun cancelSurah(reciterId: Int, chapterId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                audioDownloadManager.cancelDownload(reciterId, chapterId)
                runCatching {
                    AudioDownloadWorker.cancel(context, reciterId, chapterId)
                }
                refreshInternal()
            }
        }
    }

    fun deleteSurah(reciterId: Int, chapterId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                runCatching {
                    audioDownloadManager.cancelDownload(reciterId, chapterId)
                }
                runCatching {
                    AudioDownloadWorker.cancel(context, reciterId, chapterId)
                }
                runCatching {
                    audioDownloadManager.deleteChapter(reciterId, chapterId)
                }
                refreshInternal()
            }
        }
    }

    fun deleteReciter(reciterId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                inFlightChapters(reciterId).forEach { chapterId ->
                    runCatching {
                        AudioDownloadWorker.cancel(context, reciterId, chapterId)
                    }
                }
                runCatching { audioDownloadManager.deleteReciter(reciterId) }
                refreshInternal()
            }
        }
    }

    private fun inFlightChapters(reciterId: Int): List<Int> {
        val prefix = "$reciterId:"
        return audioDownloadManager.downloadState.value
            .filter { (key, p) -> p.isDownloading && key.startsWith(prefix) }
            .mapNotNull { (key, _) -> key.substringAfter(prefix).toIntOrNull() }
    }
}
