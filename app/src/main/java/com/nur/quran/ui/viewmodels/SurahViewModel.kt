package com.nur.quran.ui.viewmodels

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.nur.quran.data.api.ApiTafsirVerse
import com.nur.quran.data.audio.AudioDownloadManager
import com.nur.quran.data.db.entities.BookmarkEntity
import com.nur.quran.data.db.entities.ChapterEntity
import com.nur.quran.data.db.entities.CollectionEntity
import com.nur.quran.data.db.entities.CollectionItemEntity
import com.nur.quran.data.db.entities.VerseEntity
import com.nur.quran.data.db.entities.WordEntity
import com.nur.quran.data.repository.QuranRepository
import com.nur.quran.utils.TajweedProcessor
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SurahUiState {
    object Loading : SurahUiState
    data class Success(
        val chapter: ChapterEntity,
        val verses: List<VerseEntity>,
        val wordsMap: Map<Int, List<WordEntity>>,
        val tajweedMap: Map<String, String> = emptyMap()
    ) : SurahUiState
    data class Error(val message: String) : SurahUiState
}

sealed interface TafsirUiState {
    object Hidden : TafsirUiState
    object Loading : TafsirUiState
    data class Visible(
        val verseKey: String,
        val verseNumber: String,
        val text: String
    ) : TafsirUiState
    data class Error(val message: String) : TafsirUiState
}

@HiltViewModel
class SurahViewModel @Inject constructor(
    private val repository: QuranRepository,
    private val audioDownloadManager: AudioDownloadManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<SurahUiState>(SurahUiState.Loading)
    val uiState: StateFlow<SurahUiState> = _uiState.asStateFlow()

    private val _isTajweedEnabled = MutableStateFlow(true)
    val isTajweedEnabled: StateFlow<Boolean> = _isTajweedEnabled.asStateFlow()

    private val _isTranslationEnabled = MutableStateFlow(true)
    val isTranslationEnabled: StateFlow<Boolean> = _isTranslationEnabled.asStateFlow()

    private val hifdhPrefs = context.getSharedPreferences("hifdh_settings", Context.MODE_PRIVATE)

    private val _isMemorizeModeEnabled = MutableStateFlow(false)
    val isMemorizeModeEnabled: StateFlow<Boolean> = _isMemorizeModeEnabled.asStateFlow()

    private val _memorizedAyahs = MutableStateFlow<Set<String>>(hifdhPrefs.getStringSet("memorized_ayahs", emptySet()) ?: emptySet())
    val memorizedAyahs: StateFlow<Set<String>> = _memorizedAyahs.asStateFlow()

    private val _arabicFontScale = MutableStateFlow(1.0f)
    val arabicFontScale: StateFlow<Float> = _arabicFontScale.asStateFlow()

    private val _translationFontScale = MutableStateFlow(1.0f)
    val translationFontScale: StateFlow<Float> = _translationFontScale.asStateFlow()

    private val _isSaukaCompleting = MutableStateFlow(false)
    val isSaukaCompleting: StateFlow<Boolean> = _isSaukaCompleting.asStateFlow()

    private val _tafsirState = MutableStateFlow<TafsirUiState>(TafsirUiState.Hidden)
    val tafsirState: StateFlow<TafsirUiState> = _tafsirState.asStateFlow()

    private val _bookmarkedVerses = MutableStateFlow<Set<String>>(emptySet())
    val bookmarkedVerses: StateFlow<Set<String>> = _bookmarkedVerses.asStateFlow()

    /** All chapters, used for prev/next navigation names (web: allChapters). */
    val allChapters: StateFlow<List<ChapterEntity>> = repository.getChaptersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Collections ─────────────────────────────────────────────────────
    val collections: StateFlow<List<CollectionEntity>> = repository.getCollectionsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val collectionItems: StateFlow<List<CollectionItemEntity>> = repository.getAllCollectionItemsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Audio downloads ─────────────────────────────────────────────────
    private val _downloadedChapters = MutableStateFlow(audioDownloadManager.getDownloadedChapters())
    val downloadedChapters: StateFlow<Set<Int>> = _downloadedChapters.asStateFlow()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    private var currentChapterId: Int = 0
    private var currentChapterName: String = ""
    private var currentTafsirId: Int = 169
    private var currentTranslationId: Int = 131
    private var cachedTafsirVerses: List<ApiTafsirVerse> = emptyList()

    // Reading-session tracking (mirrors the web Surah page's start/unmount timer)
    private var readingSessionStart: Long = 0L
    private var readingSessionChapterId: Int = 0

    // Per-surah scroll positions (mirrors the web's surahScrollPositions map)
    val scrollPositions = mutableMapOf<Int, Pair<Int, Int>>()

    // ── Audio playback (ExoPlayer) ──────────────────────────────────────
    private var exoPlayer: ExoPlayer? = null
    private var playlist: List<VerseEntity> = emptyList()
    private var playlistChapterId: Int = 0

    private val _playingVerseKey = MutableStateFlow<String?>(null)
    val playingVerseKey: StateFlow<String?> = _playingVerseKey.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(playing: Boolean) {
            _isPlaying.value = playing
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val index = exoPlayer?.currentMediaItemIndex ?: return
            _playingVerseKey.value = playlist.getOrNull(index)?.verseKey
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                _isPlaying.value = false
                _playingVerseKey.value = null
                exoPlayer?.seekTo(0, 0L)
                exoPlayer?.pause()
            }
        }
    }

    private fun getOrCreatePlayer(): ExoPlayer {
        return exoPlayer ?: ExoPlayer.Builder(context).build().also { player ->
            player.addListener(playerListener)
            exoPlayer = player
        }
    }

    /** True when the loaded playlist belongs to the given chapter (web: isCurrentSurahPlaying). */
    private fun isCurrentChapterPlaylist(chapterId: Int) =
        playlist.isNotEmpty() && playlistChapterId == chapterId

    /** Web: handlePlayClick — toggle when this surah is loaded, else start from verse 1. */
    fun playPauseChapter(verses: List<VerseEntity>, chapterId: Int) {
        if (isCurrentChapterPlaylist(chapterId)) {
            togglePlayPause()
        } else {
            startPlaylist(verses, chapterId, 0)
        }
    }

    /** Web: handlePlayVerse — play from the tapped verse, or toggle if it's the active one. */
    fun playVerse(verses: List<VerseEntity>, chapterId: Int, verse: VerseEntity) {
        if (isCurrentChapterPlaylist(chapterId) && _playingVerseKey.value == verse.verseKey) {
            togglePlayPause()
            return
        }
        if (isCurrentChapterPlaylist(chapterId)) {
            val index = playlist.indexOfFirst { it.verseKey == verse.verseKey }
            if (index >= 0) {
                getOrCreatePlayer().seekTo(index, 0L)
                getOrCreatePlayer().play()
                return
            }
        }
        val startIndex = verses.indexOfFirst { it.verseKey == verse.verseKey }.coerceAtLeast(0)
        startPlaylist(verses, chapterId, startIndex)
    }

    private fun togglePlayPause() {
        val player = getOrCreatePlayer()
        if (player.isPlaying) player.pause() else player.play()
    }

    private fun startPlaylist(verses: List<VerseEntity>, chapterId: Int, startIndex: Int) {
        val playable = verses.filter { audioDownloadManager.playableSource(it) != null }
        if (playable.isEmpty()) return
        playlist = playable
        playlistChapterId = chapterId
        val player = getOrCreatePlayer()
        player.setMediaItems(playable.map { MediaItem.fromUri(audioDownloadManager.playableSource(it)!!) })
        player.seekTo(startIndex.coerceIn(0, playable.size - 1), 0L)
        player.prepare()
        player.play()
    }

    fun stopPlaying() {
        exoPlayer?.stop()
        exoPlayer?.clearMediaItems()
        playlist = emptyList()
        playlistChapterId = 0
        _isPlaying.value = false
        _playingVerseKey.value = null
    }

    // ── Download ────────────────────────────────────────────────────────
    fun downloadChapterAudio(chapterId: Int, verses: List<VerseEntity>) {
        if (_isDownloading.value) return
        viewModelScope.launch {
            _isDownloading.value = true
            try {
                val ok = audioDownloadManager.downloadChapter(chapterId, verses)
                if (ok) _downloadedChapters.value = audioDownloadManager.getDownloadedChapters()
            } finally {
                _isDownloading.value = false
            }
        }
    }

    // ── Chapter loading ─────────────────────────────────────────────────
    init {
        viewModelScope.launch {
            repository.getBookmarkedVerseKeysFlow().collect { keys ->
                _bookmarkedVerses.value = keys.toSet()
            }
        }
    }

    fun loadChapterDetails(chapterId: Int) {
        currentChapterId = chapterId
        _tafsirState.value = TafsirUiState.Hidden

        viewModelScope.launch {
            _uiState.value = SurahUiState.Loading
            try {
                repository.getChaptersFlow().collect { chapters ->
                    val chapter = chapters.find { it.id == chapterId }
                    if (chapter != null) {
                        currentChapterName = chapter.nameSimple
                        repository.addRecentlyRead(chapter.id, chapter.nameSimple)
                        loadVerses(chapter)
                        loadTajweed(chapter.id)
                    } else {
                        repository.refreshChapters()
                    }
                }
            } catch (e: Exception) {
                _uiState.value = SurahUiState.Error(e.localizedMessage ?: "Failed to load chapter metadata")
            }
        }
    }

    private fun loadVerses(chapter: ChapterEntity) {
        viewModelScope.launch {
            repository.getVersesByChapterFlow(chapter.id).collect { cachedVerses ->
                if (cachedVerses.isNotEmpty()) {
                    val wordsMap = mutableMapOf<Int, List<WordEntity>>()
                    var totalWordsCount = 0
                    cachedVerses.forEach { verse ->
                        val words = repository.getWordsForVerse(verse.id)
                        wordsMap[verse.id] = words
                        totalWordsCount += words.size
                    }
                    if (totalWordsCount > 0) {
                        val tajweed = (_uiState.value as? SurahUiState.Success)?.tajweedMap ?: emptyMap()
                        _uiState.value = SurahUiState.Success(chapter, cachedVerses, wordsMap, tajweed)
                    } else {
                        try {
                            repository.refreshVersesByChapter(chapter.id, currentTranslationId)
                        } catch (e: Exception) {
                            _uiState.value = SurahUiState.Error(e.localizedMessage ?: "Network error. Please try again.")
                        }
                    }
                } else {
                    try {
                        repository.refreshVersesByChapter(chapter.id, currentTranslationId)
                    } catch (e: Exception) {
                        _uiState.value = SurahUiState.Error(e.localizedMessage ?: "Network error. Please try again.")
                    }
                }
            }
        }
    }

    private fun loadTajweed(chapterId: Int) {
        viewModelScope.launch {
            try {
                val tajweedResponse = repository.getTajweedHtmlForChapter(chapterId)
                val map = mutableMapOf<String, String>()
                tajweedResponse.verses.forEach { v ->
                    map[v.verse_key] = TajweedProcessor.sanitizeTajweedHtml(v.text_uthmani_tajweed)
                }
                val current = _uiState.value
                if (current is SurahUiState.Success) {
                    _uiState.value = current.copy(tajweedMap = map)
                }
            } catch (_: Exception) {
            }
        }
    }

    /** Web: VerseItem inView → addRecentlyRead(chapter.id, name, verse.verse_key). */
    fun updateRecentlyReadVerse(verseKey: String) {
        if (currentChapterId <= 0 || currentChapterName.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.addRecentlyRead(currentChapterId, currentChapterName, verseKey)
        }
    }

    /**
     * Called when the Surah screen becomes visible. Mirrors the web app's
     * `useEffect(() => { const startTime = Date.now(); ... }, [id])`.
     */
    fun startReadingSession(chapterId: Int) {
        endReadingSession()
        readingSessionChapterId = chapterId
        readingSessionStart = System.currentTimeMillis()
    }

    /**
     * Called when the Surah screen is disposed. Logs a session if the user
     * stayed at least 10 seconds (same threshold as the web app).
     */
    fun endReadingSession() {
        if (readingSessionStart == 0L) return
        val duration = (System.currentTimeMillis() - readingSessionStart) / 1000
        readingSessionStart = 0L
        if (duration >= 10) {
            val chapterId = if (readingSessionChapterId > 0) readingSessionChapterId else null
            viewModelScope.launch {
                repository.logReadingSession(duration, "reading", chapterId)
            }
        }
    }

    fun toggleTajweed() {
        _isTajweedEnabled.value = !_isTajweedEnabled.value
    }

    fun toggleTranslation() {
        _isTranslationEnabled.value = !_isTranslationEnabled.value
    }

    fun toggleBookmark(verseKey: String, chapterId: Int, surahName: String) {
        viewModelScope.launch {
            val current = _bookmarkedVerses.value
            if (verseKey in current) {
                repository.deleteBookmark(verseKey)
            } else {
                repository.insertBookmark(
                    BookmarkEntity(
                        verseKey = verseKey,
                        chapterId = chapterId,
                        surahName = surahName
                    )
                )
            }
        }
    }

    fun isBookmarked(verseKey: String): Boolean {
        return verseKey in _bookmarkedVerses.value
    }

    fun loadTafsir(verseKey: String, chapterId: Int) {
        viewModelScope.launch {
            _tafsirState.value = TafsirUiState.Loading
            try {
                if (cachedTafsirVerses.isEmpty()) {
                    val response = repository.getTafsirForChapter(currentTafsirId, chapterId)
                    cachedTafsirVerses = response.tafsirs
                }
                val tafsir = cachedTafsirVerses.find { it.verse_key == verseKey }
                _tafsirState.value = if (tafsir != null && !tafsir.text.isNullOrEmpty()) {
                    TafsirUiState.Visible(
                        verseKey = verseKey,
                        verseNumber = verseKey.split(":")[1],
                        text = tafsir.text
                    )
                } else {
                    TafsirUiState.Error("Tafsir is not available for this verse in the selected source.")
                }
            } catch (e: Exception) {
                _tafsirState.value = TafsirUiState.Error(
                    e.localizedMessage ?: "Failed to load tafsir"
                )
            }
        }
    }

    fun dismissTafsir() {
        _tafsirState.value = TafsirUiState.Hidden
    }

    /** Fetches a translation footnote's HTML text (web: getFootnote), cached in-memory. */
    private val footnoteCache = mutableMapOf<String, String>()

    suspend fun getFootnoteText(footnoteId: String): String {
        footnoteCache[footnoteId]?.let { return it }
        val text = try {
            repository.getFootnote(footnoteId).foot_note.text
        } catch (e: Exception) {
            "Footnote not available."
        }
        footnoteCache[footnoteId] = text
        return text
    }

    // ── Collections ─────────────────────────────────────────────────────
    fun addCollection(name: String, onCreated: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = repository.addCollection(name)
            onCreated(id)
        }
    }

    fun addToCollection(collectionId: Long, verseKey: String, chapterId: Int, surahName: String) {
        viewModelScope.launch {
            repository.addToCollection(collectionId, verseKey, chapterId, surahName)
        }
    }

    fun shareVerse(verseKey: String, surahName: String) {
        val shareText = "Read $verseKey ($surahName) on Quran Nur: https://quran.com/${
            verseKey.replace(":", "/")
        }"

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share Verse")
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(shareIntent)
    }

    fun setTafsirId(tafsirId: Int) {
        currentTafsirId = tafsirId
        cachedTafsirVerses = emptyList()
    }

    fun setTranslationId(translationId: Int) {
        currentTranslationId = translationId
        viewModelScope.launch {
            try {
                repository.refreshVersesByChapter(currentChapterId, translationId)
            } catch (_: Exception) {
            }
        }
    }

    fun toggleMemorizeMode() {
        _isMemorizeModeEnabled.value = !_isMemorizeModeEnabled.value
    }

    fun toggleMemorizedAyah(verseKey: String) {
        val current = _memorizedAyahs.value.toMutableSet()
        if (current.contains(verseKey)) {
            current.remove(verseKey)
        } else {
            current.add(verseKey)
        }
        hifdhPrefs.edit().putStringSet("memorized_ayahs", current).apply()
        _memorizedAyahs.value = current
    }

    fun updateArabicFontScale(delta: Float) {
        _arabicFontScale.value = (_arabicFontScale.value + delta).coerceIn(0.5f, 3.0f)
    }

    fun updateTranslationFontScale(delta: Float) {
        _translationFontScale.value = (_translationFontScale.value + delta).coerceIn(0.5f, 3.0f)
    }

    fun completeSaukaJuz(assignmentId: String, backToSauka: String, onDone: () -> Unit) {
        viewModelScope.launch {
            _isSaukaCompleting.value = true
            delay(1000)
            _isSaukaCompleting.value = false
            onDone()
        }
    }

    override fun onCleared() {
        // Flush the reading session synchronously (viewModelScope is cancelled here)
        if (readingSessionStart != 0L) {
            val duration = (System.currentTimeMillis() - readingSessionStart) / 1000
            readingSessionStart = 0L
            if (duration >= 10) {
                val chapterId = if (readingSessionChapterId > 0) readingSessionChapterId else null
                kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
                    repository.logReadingSession(duration, "reading", chapterId)
                }
            }
        }
        exoPlayer?.removeListener(playerListener)
        exoPlayer?.release()
        exoPlayer = null
        super.onCleared()
    }
}
