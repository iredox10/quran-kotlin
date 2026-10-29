package com.nur.quran.ui.viewmodels

import com.nur.quran.data.api.ApiTafsirVerse
import com.nur.quran.data.api.TafsirResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Shared by all Quran readers; cache identity includes both chapter and selected edition. */
internal class TafsirReader(
    private val scope: CoroutineScope,
    private val loadChapter: suspend (tafsirId: Int, chapterId: Int) -> TafsirResponse,
) {
    private val mutableState = MutableStateFlow<TafsirUiState>(TafsirUiState.Hidden)
    val state: StateFlow<TafsirUiState> = mutableState.asStateFlow()
    private val cache = object : LinkedHashMap<Pair<Int, Int>, List<ApiTafsirVerse>>(8, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Pair<Int, Int>, List<ApiTafsirVerse>>?) = size > 8
    }
    private var job: Job? = null
    @Volatile private var generation = 0

    fun open(verseKey: String, tafsirId: Int) {
        job?.cancel()
        val request = ++generation
        val chapterId = verseKey.substringBefore(':').toIntOrNull() ?: return
        mutableState.value = TafsirUiState.Loading
        job = scope.launch(Dispatchers.IO) {
            try {
                val key = tafsirId to chapterId
                val rows = synchronized(cache) { cache[key] } ?: loadChapter(tafsirId, chapterId).tafsirs
                    .also { synchronized(cache) { cache[key] = it } }
                currentCoroutineContext().ensureActive()
                val verse = rows.firstOrNull { it.verse_key == verseKey && !it.text.isNullOrBlank() }
                if (request != generation) return@launch
                mutableState.value = if (verse == null) {
                    TafsirUiState.Error("Tafsir is not available for this verse.")
                } else {
                    TafsirUiState.Visible(verseKey, verseKey.substringAfter(':'), verse.text.orEmpty(), verse.resource_id)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (request == generation) mutableState.value = TafsirUiState.Error(e.localizedMessage ?: "Failed to load tafsir")
            }
        }
    }

    fun dismiss() {
        ++generation
        job?.cancel()
        mutableState.value = TafsirUiState.Hidden
    }

    fun clear() {
        dismiss()
        synchronized(cache) { cache.clear() }
    }
}
