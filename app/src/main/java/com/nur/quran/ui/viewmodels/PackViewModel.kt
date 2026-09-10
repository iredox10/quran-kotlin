package com.nur.quran.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nur.quran.data.tafsir.TafsirPackManager
import com.nur.quran.data.words.WordPackManager
import com.nur.quran.ui.components.audio.PackUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Shared offline-packs ViewModel wired to every [SettingsDrawer] host.
 *
 * Single source of truth for tafsir packs ([TafsirPackManager]) and
 * word-translation packs ([WordPackManager]). All work launches in
 * [viewModelScope] on IO and never throws (failures land in the managers'
 * status flows, which are mapped here for the UI).
 */
@HiltViewModel
class PackViewModel @Inject constructor(
    private val tafsirPackManager: TafsirPackManager,
    private val wordPackManager: WordPackManager
) : ViewModel() {

    /** Tafsir packs mapped for Settings UI rows (mirrors the former SurahViewModel bridge). */
    val tafsirPacks: StateFlow<List<PackUiState>> =
        tafsirPackManager.packStates
            .map { states ->
                tafsirPackManager.supported.map { (id, title) ->
                    val s = states[id]
                    PackUiState(
                        id = id,
                        title = title,
                        downloaded = s?.downloaded ?: 0,
                        total = s?.total ?: 114,
                        isDownloading = s?.isDownloading == true,
                        error = s?.error
                    )
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val initialWordCached: Int = wordPackManager.getCachedChapters().size

    /** Number of chapters with cached word translations. */
    val wordCachedCount: StateFlow<Int> =
        wordPackManager.packStates
            .map { states ->
                (states.filter { (_, s) -> !s.isDownloading && s.downloaded >= s.total && s.total > 0 }.keys +
                    wordPackManager.getCachedChapters()).size
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialWordCached)

    /** True while any word-pack chapter download is in flight. */
    val wordIsDownloading: StateFlow<Boolean> =
        wordPackManager.packStates
            .map { states -> states.values.any { it.isDownloading } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /**
     * Combined-row progress: every tafsir shares the global word progress
     * (cachedCount/114), keyed by tafsir id.
     */
    val wordProgressByTafsir: StateFlow<Map<Int, Pair<Int, Int>>> =
        wordCachedCount
            .map { cached ->
                tafsirPackManager.supported.associate { (id, _) -> id to (cached to 114) }
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                tafsirPackManager.supported.associate { (id, _) -> id to (initialWordCached to 114) }
            )

    /** Combined pack: full tafsir + word-by-word translations for all chapters. */
    fun downloadTafsirPack(tafsirId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { tafsirPackManager.downloadPackWithWords(tafsirId) }
        }
    }

    fun cancelTafsirPack(tafsirId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { tafsirPackManager.cancelPack(tafsirId) }
        }
    }

    fun deleteTafsirPack(tafsirId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { tafsirPackManager.deletePack(tafsirId) }
        }
    }

    /** Downloads word translations for every chapter missing them, sequentially. */
    fun downloadAllMissingWordPacks() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                (1..114).filter { !wordPackManager.isChapterCached(it) }.forEach { id ->
                    activeWordChapter = id
                    runCatching { wordPackManager.downloadChapterWords(id) }
                }
            }
            activeWordChapter = -1
        }
    }

    /** Cancels an in-flight word-pack run. */
    fun cancelAllWordPacks() {
        val id = activeWordChapter
        if (id > 0) runCatching { wordPackManager.cancelDownload(id) }
    }

    private var activeWordChapter: Int = -1
}
