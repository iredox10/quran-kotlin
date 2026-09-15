package com.nur.quran.ui.viewmodels

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.nur.quran.data.audio.AudioDownloadManager
import com.nur.quran.data.audio.PlaybackSettings
import com.nur.quran.data.audio.Reciters
import com.nur.quran.data.db.entities.BookmarkEntity
import com.nur.quran.data.db.entities.ChapterEntity
import com.nur.quran.data.db.entities.VerseEntity
import com.nur.quran.data.db.entities.WordEntity
import com.nur.quran.data.mushaf.ChapterMetadata
import com.nur.quran.data.mushaf.Mushaf
import com.nur.quran.data.repository.QuranRepository
import com.nur.quran.services.QuranAudioService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface PageUiState {
    object Loading : PageUiState
    data class Success(
        val verses: List<VerseEntity>,
        val wordsMap: Map<Int, List<WordEntity>>,
        val chapterMap: Map<Int, ChapterEntity>,
        val tajweedMap: Map<String, String>,
        val pageNumber: Int,
        val totalPages: Int = 604
    ) : PageUiState
    data class Error(val message: String) : PageUiState
}

@HiltViewModel
class PageViewModel @Inject constructor(
    private val repository: QuranRepository,
    private val audioDownloadManager: AudioDownloadManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<PageUiState>(PageUiState.Loading)
    val uiState: StateFlow<PageUiState> = _uiState.asStateFlow()

    private val _currentPageNumber = MutableStateFlow(1)
    val currentPageNumber: StateFlow<Int> = _currentPageNumber.asStateFlow()

    val juzInfo = _currentPageNumber.map { com.nur.quran.data.getJuzByPage(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.nur.quran.data.getJuzByPage(1))

    val hizbInfo = _currentPageNumber.map { com.nur.quran.data.getHizbByPage(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.nur.quran.data.getHizbByPage(1))

    private val hifdhPrefs = context.getSharedPreferences("hifdh_settings", Context.MODE_PRIVATE)

    private val _isTranslationEnabled = MutableStateFlow(hifdhPrefs.getBoolean("is_translation_enabled", true))
    val isTranslationEnabled: StateFlow<Boolean> = _isTranslationEnabled.asStateFlow()

    private val _arabicFontScale = MutableStateFlow(hifdhPrefs.getFloat("arabic_scale", 1f))
    val arabicFontScale: StateFlow<Float> = _arabicFontScale.asStateFlow()

    private val _translationFontScale = MutableStateFlow(hifdhPrefs.getFloat("translation_scale", 1f))
    val translationFontScale: StateFlow<Float> = _translationFontScale.asStateFlow()

    private val _selectedArabicFontName = MutableStateFlow(hifdhPrefs.getString("arabic_font", "KFGQPC Hafs") ?: "KFGQPC Hafs")
    val selectedArabicFontName: StateFlow<String> = _selectedArabicFontName.asStateFlow()

    private val _mushafPreset = MutableStateFlow(
        Mushaf.fromPresetKey(hifdhPrefs.getString("mushaf_preset", "uthmani")).id
    )
    val currentMushaf: StateFlow<Mushaf> = _mushafPreset
        .map { Mushaf.fromId(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, Mushaf.fromId(_mushafPreset.value))

    private val _isTajweedEnabled = MutableStateFlow(hifdhPrefs.getBoolean("is_tajweed_enabled", false))
    val isTajweedEffective: StateFlow<Boolean> = combine(_mushafPreset, _isTajweedEnabled) { preset, toggle ->
        Mushaf.isTajweedEffective(Mushaf.fromId(preset), toggle)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val bookmarkedVerses: StateFlow<Set<String>> = repository.getBookmarkedVerseKeysFlow()
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    fun toggleBookmark(verseKey: String, chapterId: Int, surahName: String) {
        viewModelScope.launch {
            if (bookmarkedVerses.value.contains(verseKey)) {
                repository.clearBookmarks()
            } else {
                repository.setSingleBookmark(
                    BookmarkEntity(
                        verseKey = verseKey,
                        chapterId = chapterId,
                        surahName = surahName,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    private var loadJob: Job? = null

    fun loadPage(pageNumber: Int) {
        if (pageNumber !in 1..604) return
        loadJob?.cancel()
        _currentPageNumber.value = pageNumber
        
        loadJob = viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = PageUiState.Loading
            try {
                val verses = repository.getVersesByPage(pageNumber, _mushafPreset.value)
                if (verses.isEmpty()) {
                    _uiState.value = PageUiState.Error("No verses found for page $pageNumber")
                    return@launch
                }
                
                val chapterIds = verses.map { it.chapterId }.distinct()
                val chapterMap = mutableMapOf<Int, ChapterEntity>()
                val tajweedMap = mutableMapOf<String, String>()
                
                for (cId in chapterIds) {
                    repository.getChapterById(cId)?.let { chapterMap[cId] = it }
                    if (isTajweedEffective.value) {
                        try {
                            val taj = repository.getTajweedHtmlForChapter(cId)
                            taj.verses.forEach { tajweedMap[it.verse_key] = it.text_uthmani_tajweed }
                        } catch (_: Exception) {}
                    }
                }
                
                val wordsMap = repository.getWordsForVerses(verses.map { it.id })
                
                _uiState.value = PageUiState.Success(
                    verses = verses,
                    wordsMap = wordsMap,
                    chapterMap = chapterMap,
                    tajweedMap = tajweedMap,
                    pageNumber = pageNumber
                )
                
                // Track recently read using the first chapter on the page
                val firstChapterId = verses.first().chapterId
                val firstChapterName = chapterMap[firstChapterId]?.nameSimple ?: "Surah $firstChapterId"
                repository.addRecentlyRead(firstChapterId, firstChapterName)
                
                // Prefetch next 3 pages
                prefetchPages(pageNumber + 1, pageNumber + 3)
                
            } catch (e: Exception) {
                _uiState.value = PageUiState.Error(e.localizedMessage ?: "Failed to load page $pageNumber")
            }
        }
    }

    private fun prefetchPages(startPage: Int, endPage: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            for (p in startPage..endPage) {
                if (p <= 604) {
                    try {
                        repository.getVersesByPage(p, _mushafPreset.value)
                    } catch (_: Exception) {}
                }
            }
        }
    }

    fun nextPage() {
        if (_currentPageNumber.value < 604) {
            loadPage(_currentPageNumber.value + 1)
        }
    }

    fun prevPage() {
        if (_currentPageNumber.value > 1) {
            loadPage(_currentPageNumber.value - 1)
        }
    }

    // --- Audio Player ---
    private var mediaController: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playingVerseKey = MutableStateFlow<String?>(null)
    val playingVerseKey: StateFlow<String?> = _playingVerseKey.asStateFlow()
    
    private val _currentReciterId = MutableStateFlow(hifdhPrefs.getInt("reciter_id", 7))
    val currentReciterId: StateFlow<Int> = _currentReciterId.asStateFlow()

    private val _playbackSettings = MutableStateFlow(
        PlaybackSettings(
            ayahRepeat = hifdhPrefs.getInt("pb_ayah_repeat", 1),
            rangeRepeat = hifdhPrefs.getInt("pb_range_repeat", 1),
            delayMs = hifdhPrefs.getLong("pb_delay_ms", 0L),
            speed = hifdhPrefs.getFloat("pb_speed", 1f),
            streamOnly = hifdhPrefs.getBoolean("pb_stream_only", false),
            scrollWhilePlaying = hifdhPrefs.getBoolean("pb_scroll_while_playing", true)
        )
    )
    val playbackSettings: StateFlow<PlaybackSettings> = _playbackSettings.asStateFlow()

    private val _streamOnly = MutableStateFlow(hifdhPrefs.getBoolean("pb_stream_only", false))
    val streamOnly: StateFlow<Boolean> = _streamOnly.asStateFlow()

    init {
        ensureController()
    }

    private fun ensureController(): MediaController? {
        mediaController?.let { return it }
        if (controllerFuture == null) {
            val sessionToken = SessionToken(
                context,
                ComponentName(context, QuranAudioService::class.java)
            )
            val future = MediaController.Builder(context, sessionToken).buildAsync()
            controllerFuture = future
            Futures.addCallback(
                future,
                object : FutureCallback<MediaController> {
                    override fun onSuccess(result: MediaController?) {
                        result ?: return
                        result.removeListener(playerListener)
                        result.addListener(playerListener)
                        result.setPlaybackSpeed(_playbackSettings.value.speed)
                        mediaController = result
                        _isPlaying.value = result.isPlaying
                        _playingVerseKey.value = result.currentMediaItem?.mediaId
                    }
                    override fun onFailure(t: Throwable) {
                        controllerFuture = null
                    }
                },
                MoreExecutors.directExecutor()
            )
        }
        return mediaController
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
        }
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            _playingVerseKey.value = mediaItem?.mediaId
        }
    }

    fun startPlayback(verses: List<VerseEntity>, startIndex: Int = 0) {
        val player = mediaController ?: return
        val items = verses.mapNotNull { buildMediaItem(it) }
        if (items.isEmpty()) return
        player.setMediaItems(items)
        player.seekTo(startIndex.coerceIn(0, items.size - 1), 0L)
        player.prepare()
        player.play()
    }

    fun togglePlayPause() {
        val player = mediaController ?: return
        if (player.isPlaying) player.pause() else player.play()
    }

    fun stopPlaying() {
        mediaController?.stop()
        mediaController?.clearMediaItems()
        _playingVerseKey.value = null
    }

    private fun buildMediaItem(verse: VerseEntity): MediaItem? {
        val reciterId = _currentReciterId.value
        val surahName = ChapterMetadata.nameById(verse.chapterId)
        val title = "$surahName ${verse.chapterId}:${verse.verseNumber}"
        
        val uriString = audioDownloadManager.localFile(verse.verseKey)?.let { Uri.fromFile(it).toString() }
            ?: audioDownloadManager.remoteUrl(reciterId, verse)
            ?: return null

        val metadata = MediaMetadata.Builder().setTitle(title).setDisplayTitle(title).build()
        return MediaItem.Builder().setUri(uriString).setMediaId(verse.verseKey).setMediaMetadata(metadata).build()
    }
}
