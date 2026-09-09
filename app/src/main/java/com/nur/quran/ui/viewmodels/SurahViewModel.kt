package com.nur.quran.ui.viewmodels

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.nur.quran.data.api.ApiTafsirVerse
import com.nur.quran.data.audio.AudioDownloadManager
import com.nur.quran.data.audio.LinkedAudioStore
import com.nur.quran.data.audio.PlaybackSettings
import com.nur.quran.data.audio.Reciters
import com.nur.quran.data.audio.TimingImporter
import com.nur.quran.data.tafsir.TafsirPackManager
import com.nur.quran.data.words.WordPackManager
import com.nur.quran.ui.components.audio.PackUiState
import com.nur.quran.data.db.entities.BookmarkEntity
import com.nur.quran.data.db.entities.ChapterEntity
import com.nur.quran.data.db.entities.CollectionEntity
import com.nur.quran.data.db.entities.CollectionItemEntity
import com.nur.quran.data.db.entities.RecentlyReadEntity
import com.nur.quran.data.db.entities.VerseEntity
import com.nur.quran.data.db.entities.WordEntity
import com.nur.quran.data.hifdh.FsrsCard
import com.nur.quran.data.hifdh.FsrsScheduler
import com.nur.quran.data.hifdh.HifdhGoal
import com.nur.quran.data.hifdh.HifdhHistoryEntry
import com.nur.quran.data.hifdh.HifdhStore
import com.nur.quran.data.repository.QuranRepository
import com.nur.quran.utils.TajweedProcessor
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SurahUiState {
    object Loading : SurahUiState
    data class Success(
        val chapter: ChapterEntity,
        val verses: List<VerseEntity>,
        val wordsMap: Map<Int, List<WordEntity>> = emptyMap(),
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
    @ApplicationContext private val context: Context,
    private val linkedAudioStore: LinkedAudioStore,
    private val timingImporter: TimingImporter,
    private val tafsirPackManager: TafsirPackManager,
    private val wordPackManager: WordPackManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<SurahUiState>(SurahUiState.Loading)
    val uiState: StateFlow<SurahUiState> = _uiState.asStateFlow()

    private val hifdhPrefs = context.getSharedPreferences("hifdh_settings", Context.MODE_PRIVATE)

    private val _isTajweedEnabled = MutableStateFlow(hifdhPrefs.getBoolean("is_tajweed_enabled", false))
    val isTajweedEnabled: StateFlow<Boolean> = _isTajweedEnabled.asStateFlow()

    private val _isTranslationEnabled = MutableStateFlow(hifdhPrefs.getBoolean("is_translation_enabled", true))
    val isTranslationEnabled: StateFlow<Boolean> = _isTranslationEnabled.asStateFlow()

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

    private val _selectedArabicFontName = MutableStateFlow(hifdhPrefs.getString("arabic_font", "Scheherazade New") ?: "Scheherazade New")
    val selectedArabicFontName: StateFlow<String> = _selectedArabicFontName.asStateFlow()

    private val _wordTapBehavior = MutableStateFlow(hifdhPrefs.getString("word_tap_behavior", "translation") ?: "translation")
    val wordTapBehavior: StateFlow<String> = _wordTapBehavior.asStateFlow()

    private val _mushafPreset = MutableStateFlow(
        com.nur.quran.data.mushaf.Mushaf.fromPresetKey(
            hifdhPrefs.getString("mushaf_preset", "uthmani")
        ).id
    )
    val mushafPreset: StateFlow<String> = _mushafPreset.asStateFlow()

    /** Canonical mushaf derived from the stored preset (web: getMushafById). */
    val currentMushaf: StateFlow<com.nur.quran.data.mushaf.Mushaf> = _mushafPreset
        .map { com.nur.quran.data.mushaf.Mushaf.fromId(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, com.nur.quran.data.mushaf.Mushaf.fromId(_mushafPreset.value))

    /** Web: isTajweedEnabledForMushaf — Indopak never, Tajweed preset always. */
    val isTajweedEffective: StateFlow<Boolean> = kotlinx.coroutines.flow.combine(
        _mushafPreset, _isTajweedEnabled
    ) { preset, toggle ->
        com.nur.quran.data.mushaf.Mushaf.isTajweedEffective(
            com.nur.quran.data.mushaf.Mushaf.fromId(preset), toggle
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    /** Tracks which mushaf each loaded chapter was fetched with (forces refetch on switch). */
    private val loadedMushafByChapter = mutableMapOf<Int, String>()

    private val _currentReciterId = MutableStateFlow(hifdhPrefs.getInt("reciter_id", 7))
    val currentReciterId: StateFlow<Int> = _currentReciterId.asStateFlow()

    fun setReciterId(id: Int) {
        _currentReciterId.value = id
        hifdhPrefs.edit().putInt("reciter_id", id).apply()
    }

    val bookmarkedVerses: StateFlow<Set<String>> = repository.getBookmarkedVerseKeysFlow()
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

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

    /** Linked GreenTech/quran_android surahs: reciterId → linked surah numbers. */
    val linkedState: StateFlow<Map<Int, Set<Int>>> = linkedAudioStore.linkedState

    // ── Offline packs (tafsir + word translations) ─────────────────────
    /** Tafsir packs mapped for Settings UI rows. */
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

    /** Chapters with cached word translations. */
    val wordPackCached: StateFlow<Set<Int>> =
        wordPackManager.packStates
            .map { states ->
                states.filter { (_, s) -> !s.isDownloading && s.downloaded >= s.total && s.total > 0 }.keys +
                    wordPackManager.getCachedChapters()
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val wordPackDownloading: StateFlow<Boolean> =
        wordPackManager.packStates
            .map { states -> states.values.any { it.isDownloading } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun downloadTafsirPack(tafsirId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try { tafsirPackManager.downloadPack(tafsirId) } catch (_: Exception) {}
        }
    }

    fun cancelTafsirPack(tafsirId: Int) = tafsirPackManager.cancelPack(tafsirId)

    fun deleteTafsirPack(tafsirId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try { tafsirPackManager.deletePack(tafsirId) } catch (_: Exception) {}
        }
    }

    /** Non-suspend entry for the word tooltip button; runs in its own scope. */
    fun downloadChapterWords(chapterId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try { wordPackManager.downloadChapterWords(chapterId) } catch (_: Exception) {}
        }
    }

    /** Downloads word translations for every chapter missing them. */
    fun downloadAllMissingWordPacks() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                (1..114).filter { !wordPackManager.isChapterCached(it) }.forEach { id ->
                    try { wordPackManager.downloadChapterWords(id) } catch (_: Exception) {}
                }
            } catch (_: Exception) {}
        }
    }

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    private var currentChapterId: Int = 0
    private var currentChapterName: String = ""
    private var currentTafsirId: Int = 169
    private val _currentTranslationId = MutableStateFlow(hifdhPrefs.getInt("translation_id", 85))
    val currentTranslationId: StateFlow<Int> = _currentTranslationId.asStateFlow()
    private var cachedTafsirVerses: List<ApiTafsirVerse> = emptyList()

    // Reading-session tracking (mirrors the web Surah page's start/unmount timer)
    private var readingSessionStart: Long = 0L
    private var readingSessionChapterId: Int = 0

    // ── Hifdh (memorization) tracking ──────────────────────────────────
    private val hifdhStore = HifdhStore(context)
    private val fsrsScheduler = FsrsScheduler()

    private val _hifdhHistory = MutableStateFlow(hifdhStore.loadHifdhHistory())
    val hifdhHistory: StateFlow<Map<String, HifdhHistoryEntry>> = _hifdhHistory.asStateFlow()

    private val _transitionLinks = MutableStateFlow(hifdhStore.loadTransitionLinks())
    val transitionLinks: StateFlow<Set<String>> = _transitionLinks.asStateFlow()

    private val _hifdhGoals = MutableStateFlow(hifdhStore.loadHifdhGoals())
    val hifdhGoals: StateFlow<List<HifdhGoal>> = _hifdhGoals.asStateFlow()

    /** Web: logHifdhReview(key, rating) — schedule a new review and persist it. */
    fun logHifdhReview(verseKey: String, rating: Int) {
        val now = System.currentTimeMillis()
        val current = _hifdhHistory.value.toMutableMap()
        val prevCard = current[verseKey]?.card
        val card = if (prevCard != null) {
            fsrsScheduler.next(prevCard, now, rating)
        } else {
            fsrsScheduler.next(fsrsScheduler.createEmptyCard(now), now, rating)
        }
        val strength = when (rating) {
            4 -> "strong"
            1 -> "weak"
            else -> "medium"
        }
        current[verseKey] = HifdhHistoryEntry(card = card, lastReviewed = now, strength = strength)
        _hifdhHistory.value = current
        hifdhStore.saveHifdhHistory(current)

        // Web: Again flags the transition link; Easy/Good clears it (Hard leaves it).
        val links = _transitionLinks.value.toMutableSet()
        when (rating) {
            1 -> links.add(verseKey)
            3, 4 -> links.remove(verseKey)
        }
        _transitionLinks.value = links
        hifdhStore.saveTransitionLinks(links)
    }

    fun getHifdhCard(verseKey: String): FsrsCard? = _hifdhHistory.value[verseKey]?.card

    /** Web: addHifdhGoal — persisted; targetDate is epoch millis. */
    fun addHifdhGoal(targetId: Int, targetDateMillis: Long) {
        val goal = HifdhGoal(
            id = System.currentTimeMillis().toString(),
            targetType = "surah",
            targetId = targetId,
            targetDate = targetDateMillis,
            createdAt = System.currentTimeMillis()
        )
        _hifdhGoals.value = _hifdhGoals.value + goal
        hifdhStore.saveHifdhGoals(_hifdhGoals.value)
    }

    fun deleteHifdhGoal(id: String) {
        _hifdhGoals.value = _hifdhGoals.value.filterNot { it.id == id }
        hifdhStore.saveHifdhGoals(_hifdhGoals.value)
    }

    /** Cached verse lookups used by the hifdh test modals. */
    private val verseTextCache = mutableMapOf<String, VerseEntity>()

    suspend fun getVerseTexts(keys: List<String>): Map<String, VerseEntity> {
        val missing = keys.filterNot { it in verseTextCache }
        if (missing.isNotEmpty()) {
            repository.getVersesByKey(missing).forEach { verseTextCache[it.verseKey] = it }
        }
        return keys.mapNotNull { key -> verseTextCache[key]?.let { key to it } }.toMap()
    }

    // Per-surah scroll positions (mirrors the web's surahScrollPositions map)
    val scrollPositions = mutableMapOf<Int, Pair<Int, Int>>()

    // ── Audio playback (ExoPlayer) ──────────────────────────────────────
    private var exoPlayer: ExoPlayer? = null
    private var playlist: List<VerseEntity> = emptyList()
    private var playlistChapterId: Int = 0

    // Hifdh chunk loop (web: Memorization.jsx handleAudioEnded / toggleAudio)
    private var hifdhLoopActive = false
    private var hifdhAyahRepeat = 1        // -1 = infinite ayah repeat
    private var hifdhDelayMs = 0L
    private var hifdhRangeLoop = 1         // -1 = infinite range loop
    private var hifdhAyahPlayCount = 0
    private var hifdhRangeCount = 0
    private var hifdhPlaylist: List<VerseEntity> = emptyList()
    private var hifdhDelayJob: Job? = null

    private val _playingVerseKey = MutableStateFlow<String?>(null)
    val playingVerseKey: StateFlow<String?> = _playingVerseKey.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    // ── Playback settings (repeat / delay / speed, persisted) ─────────────
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

    fun setStreamOnly(v: Boolean) {
        _streamOnly.value = v
        hifdhPrefs.edit().putBoolean("pb_stream_only", v).apply()
        _playbackSettings.value = _playbackSettings.value.copy(streamOnly = v)
    }

    private val _scrollWhilePlaying = MutableStateFlow(hifdhPrefs.getBoolean("pb_scroll_while_playing", true))
    /** Web: scrollWhilePlaying — follow/highlight each ayah while playing. */
    val scrollWhilePlaying: StateFlow<Boolean> = _scrollWhilePlaying.asStateFlow()

    fun setScrollWhilePlaying(v: Boolean) {
        _scrollWhilePlaying.value = v
        hifdhPrefs.edit().putBoolean("pb_scroll_while_playing", v).apply()
        _playbackSettings.value = _playbackSettings.value.copy(scrollWhilePlaying = v)
    }

    /**
     * Web parity: in-play settings adjust (GlobalAudioPlayer drawer). Applies
     * speed/repeats/delay/streamOnly live without restarting; a reciter change
     * rebuilds the current playlist preserving the active verse index.
     */
    fun applyInPlaySettings(
        reciterId: Int,
        ayahRepeat: Int,
        rangeRepeat: Int,
        delayMs: Long,
        speed: Float,
        streamOnly: Boolean
    ) {
        setAyahRepeat(ayahRepeat)
        setRangeRepeat(rangeRepeat)
        setDelayMs(delayMs)
        setSpeed(speed)
        setStreamOnly(streamOnly)
        if (reciterId == _currentReciterId.value) return
        setReciterId(reciterId)
        val player = exoPlayer ?: return
        if (playlist.isEmpty()) return
        val currentKey = _playingVerseKey.value
        val wasPlaying = player.isPlaying
        val (playable, items) = buildPlaylistItems(playlist, playlistChapterId)
        if (playable.isEmpty() || items.size != playable.size) return
        val idx = playable.indexOfFirst { it.verseKey == currentKey }.coerceAtLeast(0)
        repeatGeneration++
        repeatDelayJob?.cancel()
        repeatAyahCount = 0
        repeatRangeCount = 0
        repeatSeekPending = false
        playlist = playable
        lastMediaIndex = idx
        player.setMediaItems(items)
        player.seekTo(idx, 0L)
        player.prepare()
        _playingVerseKey.value = playable.getOrNull(idx)?.verseKey
        if (wasPlaying) player.play() else player.pause()
    }

    /** In-flight auto-cache guards keyed `"$reciterId:$chapterId"` (fire-and-forget dedupe). */
    private val autoCacheInFlight = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    /**
     * Auto-download-on-play: after streaming starts, cache the chapter in the
     * background for offline next time. Skipped when stream-only mode is on,
     * the chapter is linked (gapless file), already fully downloaded, or a
     * download for the same key is already running. Never throws.
     */
    private fun autoCacheChapter(reciterId: Int, chapterId: Int, verses: List<VerseEntity>) {
        if (verses.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (_streamOnly.value) return@launch
                if (runCatching { linkedAudioStore.isLinked(reciterId, chapterId) }.getOrDefault(false)) return@launch
                val key = "$reciterId:$chapterId"
                if (!autoCacheInFlight.add(key)) return@launch
                try {
                    if (_isDownloading.value) return@launch
                    if (audioDownloadManager.isDownloading(reciterId, chapterId)) return@launch
                    if (runCatching { audioDownloadManager.isFullyDownloaded(reciterId, chapterId, verses) }.getOrDefault(false)) return@launch
                    val onProgress: (Int, Int) -> Unit = { done, total ->
                        _downloadProgress.value = if (total > 0) done.toFloat() / total else 1f
                    }
                    // Download only the missing verses; falls back to full
                    // chapter download if anything goes wrong.
                    val missingCount: Int? = runCatching {
                        audioDownloadManager.downloadMissing(reciterId, verses, chapterId, onProgress)
                    }.getOrNull()
                    if (missingCount != null) {
                        if (missingCount >= 0) {
                            _downloadedChapters.value = audioDownloadManager.getDownloadedChapters()
                        }
                    } else {
                        val ok = runCatching {
                            audioDownloadManager.downloadChapter(reciterId, chapterId, verses, onProgress)
                        }.getOrDefault(false)
                        if (ok) {
                            _downloadedChapters.value = audioDownloadManager.getDownloadedChapters()
                            _downloadProgress.value = 1f
                        }
                    }
                } finally {
                    autoCacheInFlight.remove(key)
                }
            } catch (_: Exception) {
                // Fire-and-forget: playback already started, never surface errors.
            }
        }
    }

    // Unified repeat state for the general (non-hifdh) playlist path.
    private var repeatAyahCount = 0
    private var repeatRangeCount = 0
    private var repeatDelayJob: Job? = null
    private var repeatGeneration = 0
    private var lastMediaIndex = -1
    private var repeatSeekPending = false

    fun setAyahRepeat(repeat: Int) {
        _playbackSettings.value = _playbackSettings.value.copy(ayahRepeat = repeat)
        hifdhPrefs.edit().putInt("pb_ayah_repeat", repeat).apply()
        repeatAyahCount = 0
    }

    fun setRangeRepeat(repeat: Int) {
        _playbackSettings.value = _playbackSettings.value.copy(rangeRepeat = repeat)
        hifdhPrefs.edit().putInt("pb_range_repeat", repeat).apply()
        repeatRangeCount = 0
    }

    fun setDelayMs(delayMs: Long) {
        val safe = delayMs.coerceAtLeast(0L)
        _playbackSettings.value = _playbackSettings.value.copy(delayMs = safe)
        hifdhPrefs.edit().putLong("pb_delay_ms", safe).apply()
    }

    fun setSpeed(speed: Float) {
        val safe = speed.coerceIn(0.25f, 3f)
        _playbackSettings.value = _playbackSettings.value.copy(speed = safe)
        hifdhPrefs.edit().putFloat("pb_speed", safe).apply()
        exoPlayer?.setPlaybackSpeed(safe)
    }

    private fun reciterName(reciterId: Int): String = Reciters.nameOf(reciterId)

    /**
     * Local-first media item: the linked gapless file URI wins, then the
     * downloaded file URI, otherwise the remote URL.
     * Null when the verse has neither.
     */
    private fun buildMediaItem(verse: VerseEntity, chapterId: Int): MediaItem? {
        // Linked gapless fast path: synchronous prefs check only; the detailed
        // timing check happens in the seek path (seekToAyah / getAyahAtPosition).
        val linkedUriString: String? = try {
            if (linkedAudioStore.isLinked(_currentReciterId.value, chapterId)) {
                linkedUriSync(_currentReciterId.value, chapterId)?.toString()
            } else null
        } catch (_: Exception) {
            null
        }
        val uriString = linkedUriString
            ?: audioDownloadManager.localFile(verse.verseKey)?.let { Uri.fromFile(it).toString() }
            ?: audioDownloadManager.remoteUrl(_currentReciterId.value, verse)
            ?: return null
        val metadata = MediaMetadata.Builder()
            .setTitle("Surah $chapterId Ayah ${verse.verseNumber}")
            .setArtist(reciterName(_currentReciterId.value))
            .build()
        return MediaItem.Builder()
            .setUri(uriString)
            .setMediaId(verse.verseKey)
            .setMediaMetadata(metadata)
            .build()
    }

    /** Pairs verses with their media items so playlist indices always line up. */
    private fun buildPlaylistItems(verses: List<VerseEntity>, chapterId: Int): Pair<List<VerseEntity>, List<MediaItem>> {
        val pairs = verses.mapNotNull { verse -> buildMediaItem(verse, chapterId)?.let { verse to it } }
        return pairs.map { it.first } to pairs.map { it.second }
    }

    // ── Linked gapless playback ─────────────────────────────────────────
    private val linkedSurahUriCache = mutableMapOf<Pair<Int, Int>, Uri?>()

    /** Synchronous fast-path lookup of the persisted gapless file (prefs + DocumentFile). */
    private fun linkedUriSync(reciterId: Int, surah: Int): Uri? {
        val key = reciterId to surah
        if (linkedSurahUriCache.containsKey(key)) return linkedSurahUriCache[key]
        val uri = try {
            if (!linkedAudioStore.isLinked(reciterId, surah)) {
                linkedSurahUriCache[key] = null
                return null
            }
            val fileName = "%03d.mp3".format(surah)
            // 1. Qari subfolders mapped to this reciter (scanner's setFolderReciter).
            val viaFolder = linkedAudioStore.getFolderUrisForReciter(reciterId).firstNotNullOfOrNull { folder ->
                runCatching {
                    val doc = DocumentFile.fromSingleUri(context, Uri.parse(folder))
                    if (doc?.isDirectory == true) doc.findFile(fileName)?.uri else null
                }.getOrNull()
            }
            if (viaFolder != null) return viaFolder.also { linkedSurahUriCache[key] = it }
            // 2. Fallback: tree roots holding SSS.mp3 directly (user picked the qari folder).
            linkedAudioStore.getTrees().firstNotNullOfOrNull { tree ->
                runCatching {
                    DocumentFile.fromTreeUri(context, Uri.parse(tree))?.findFile(fileName)?.uri
                }.getOrNull()
            }
        } catch (_: Exception) {
            null
        }
        linkedSurahUriCache[key] = uri
        return uri
    }

    /** Resolve the persisted gapless surah file Uri, or null on any failure. */
    suspend fun resolveLinkedSurahUri(reciterId: Int, surah: Int): Uri? {
        val key = reciterId to surah
        if (linkedSurahUriCache.containsKey(key)) return linkedSurahUriCache[key]
        return try {
            linkedUriSync(reciterId, surah)
        } catch (_: Exception) {
            null
        }
    }

    /** Seek the gapless surah file to the start of [ayah] using imported timings. */
    fun seekToAyah(surah: Int, ayah: Int) {
        viewModelScope.launch {
            try {
                val ms = timingImporter.getStartMs(_currentReciterId.value, surah, ayah)
                exoPlayer?.seekTo(ms ?: 0L)
            } catch (_: Exception) {
            }
        }
    }

    /** Ayah active at [posMs] within [surah], or null on any failure. */
    suspend fun getAyahAtPosition(surah: Int, posMs: Long): Int? {
        return try {
            timingImporter.getTimings(_currentReciterId.value, surah)
                .findLast { it.startMs <= posMs }?.ayah
        } catch (_: Exception) {
            null
        }
    }

    // Position-poll hook for gapless follow mode. Existing repeat/delay handlers untouched.
    private var followJob: Job? = null

    /** Poll ExoPlayer position and emit the current ayah until stopped. */
    fun startFollowAyah(surah: Int, onAyah: (Int) -> Unit) {
        followJob?.cancel()
        followJob = viewModelScope.launch {
            try {
                while (_isPlaying.value) {
                    try {
                        val pos = exoPlayer?.currentPosition ?: 0L
                        getAyahAtPosition(surah, pos)?.let { onAyah(it) }
                    } catch (_: Exception) {
                    }
                    delay(100)
                }
            } catch (_: Exception) {
            }
        }
    }

    fun stopFollow() {
        followJob?.cancel()
        followJob = null
    }

    /** Runs [action] after the configured settings delay; cancelled on pause/stop/seek. */
    private fun scheduleRepeatDelay(action: () -> Unit) {
        repeatDelayJob?.cancel()
        hifdhDelayJob?.cancel()
        val delayMs = _playbackSettings.value.delayMs
        if (delayMs > 0) {
            val job = viewModelScope.launch {
                delay(delayMs)
                action()
            }
            repeatDelayJob = job
            hifdhDelayJob = job
        } else {
            action()
        }
    }

    private fun rangeStartIndex(): Int {
        val key = _playbackSettings.value.rangeStart ?: return 0
        return playlist.indexOfFirst { it.verseKey == key }.coerceAtLeast(0)
    }

    private fun resetPlaybackState(player: ExoPlayer? = exoPlayer) {
        _isPlaying.value = false
        _playingVerseKey.value = null
        player?.seekTo(0, 0L)
        player?.pause()
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(playing: Boolean) {
            _isPlaying.value = playing
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val player = exoPlayer ?: return
            val index = player.currentMediaItemIndex
            val list = if (hifdhLoopActive) hifdhPlaylist else playlist
            _playingVerseKey.value = list.getOrNull(index)?.verseKey
            if (hifdhLoopActive) {
                lastMediaIndex = index
            } else {
                handlePlaylistTransition(player, index, reason)
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                if (hifdhLoopActive) {
                    handleHifdhChunkEnded()
                } else {
                    handlePlaylistEnded()
                }
            }
        }
    }

    /**
     * Per-ayah repeat for the general playlist: when ExoPlayer auto-advances,
     * replay the finished ayah until [PlaybackSettings.ayahRepeat] is reached
     * (-1 = infinite), honouring [PlaybackSettings.delayMs] between plays.
     */
    private fun handlePlaylistTransition(player: ExoPlayer, index: Int, reason: Int) {
        if (repeatSeekPending) {
            repeatSeekPending = false
            lastMediaIndex = index
            return
        }
        if (reason != Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
            // Manual seek / playlist change: drop pending repeats.
            repeatGeneration++
            repeatDelayJob?.cancel()
            repeatAyahCount = 0
            lastMediaIndex = index
            return
        }
        val finishedIdx = lastMediaIndex
        lastMediaIndex = index
        val ayahRepeat = _playbackSettings.value.ayahRepeat
        if (finishedIdx !in playlist.indices || finishedIdx == index) return
        if (ayahRepeat != -1 && repeatAyahCount + 1 >= ayahRepeat) {
            repeatAyahCount = 0
            return
        }
        if (ayahRepeat != -1) repeatAyahCount++
        repeatSeekPending = true
        lastMediaIndex = finishedIdx
        _playingVerseKey.value = playlist.getOrNull(finishedIdx)?.verseKey
        val gen = repeatGeneration
        player.pause()
        player.seekTo(finishedIdx, 0L)
        scheduleRepeatDelay {
            if (gen != repeatGeneration) return@scheduleRepeatDelay
            player.seekTo(finishedIdx, 0L)
            player.play()
        }
    }

    /**
     * End-of-playlist repeat for the general path: final-ayah repeat first,
     * then loop back to the range start while [PlaybackSettings.rangeRepeat]
     * allows (-1 = infinite). With 1/1 settings this is the plain stop.
     */
    private fun handlePlaylistEnded() {
        val player = exoPlayer ?: return
        if (playlist.isEmpty()) {
            resetPlaybackState(player)
            return
        }
        val settings = _playbackSettings.value
        val lastIdx = playlist.size - 1
        val endedIdx = player.currentMediaItemIndex.coerceIn(0, lastIdx)
        // 1) Final ayah repeat.
        if (settings.ayahRepeat == -1 || repeatAyahCount + 1 < settings.ayahRepeat) {
            if (settings.ayahRepeat != -1) repeatAyahCount++
            val gen = repeatGeneration
            player.pause()
            player.seekTo(endedIdx, 0L)
            _playingVerseKey.value = playlist.getOrNull(endedIdx)?.verseKey
            scheduleRepeatDelay {
                if (gen != repeatGeneration) return@scheduleRepeatDelay
                player.play()
            }
            return
        }
        repeatAyahCount = 0
        // 2) Range loop back to the range start (whole playlist when unset).
        if (settings.rangeRepeat == -1 || repeatRangeCount + 1 < settings.rangeRepeat) {
            if (settings.rangeRepeat != -1) repeatRangeCount++
            val startIdx = rangeStartIndex()
            val gen = repeatGeneration
            player.pause()
            player.seekTo(startIdx, 0L)
            _playingVerseKey.value = playlist.getOrNull(startIdx)?.verseKey
            scheduleRepeatDelay {
                if (gen != repeatGeneration) return@scheduleRepeatDelay
                player.play()
            }
            return
        }
        repeatRangeCount = 0
        resetPlaybackState(player)
    }

    private fun getOrCreatePlayer(): ExoPlayer {
        return exoPlayer ?: ExoPlayer.Builder(context).build().also { player ->
            player.addListener(playerListener)
            player.setPlaybackSpeed(_playbackSettings.value.speed)
            exoPlayer = player
        }
    }

    /** True when the loaded playlist belongs to the given chapter (web: isCurrentSurahPlaying). */
    private fun isCurrentChapterPlaylist(chapterId: Int) =
        playlist.isNotEmpty() && playlistChapterId == chapterId

    /** Web: handlePlayClick — toggle when this surah is loaded, else start from verse 1. */
    fun playPauseChapter(verses: List<VerseEntity>, chapterId: Int) {
        if (isCurrentChapterPlaylist(chapterId)) {
            val wasPlaying = exoPlayer?.isPlaying == true
            togglePlayPause()
            // Fire-and-forget background cache on resume; never on pause.
            if (!wasPlaying) autoCacheChapter(_currentReciterId.value, chapterId, verses.ifEmpty { playlist })
        } else {
            startPlaylist(verses, chapterId, 0)
            autoCacheChapter(_currentReciterId.value, chapterId, verses)
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
                _playingVerseKey.value = verse.verseKey
                _isPlaying.value = true
                autoCacheChapter(_currentReciterId.value, chapterId, verses.ifEmpty { playlist })
                return
            }
        }
        val startIndex = verses.indexOfFirst { it.verseKey == verse.verseKey }.coerceAtLeast(0)
        startPlaylist(verses, chapterId, startIndex)
        autoCacheChapter(_currentReciterId.value, chapterId, verses)
    }

    private fun togglePlayPause() {
        val player = getOrCreatePlayer()
        if (player.isPlaying) {
            hifdhDelayJob?.cancel()
            repeatDelayJob?.cancel()
            player.pause()
        } else {
            if (player.playbackState == Player.STATE_ENDED && player.currentMediaItemIndex >= 0) {
                player.seekTo(player.currentMediaItemIndex, 0L)
            }
            player.play()
        }
    }

    /** Web: Memorization.jsx toggleAudio — play the visible hifdh chunk with repeat/delay/range options. */
    fun playPauseHifdhChunk(verses: List<VerseEntity>, chapterId: Int, ayahRepeat: Int, delaySec: Int, rangeLoop: Int) {
        val (playable, items) = buildPlaylistItems(verses, chapterId)
        if (playable.isEmpty()) return
        val player = getOrCreatePlayer()
        val sameChunk = hifdhLoopActive && playlistChapterId == chapterId &&
            hifdhPlaylist.map { it.verseKey } == playable.map { it.verseKey }
        if (sameChunk) {
            togglePlayPause()
            return
        }
        hifdhLoopActive = true
        hifdhAyahRepeat = ayahRepeat
        hifdhDelayMs = delaySec * 1000L
        hifdhRangeLoop = rangeLoop
        hifdhAyahPlayCount = 0
        hifdhRangeCount = 0
        hifdhPlaylist = playable
        playlistChapterId = chapterId
        // Mirror into the shared settings/counters so the listener stays consistent.
        _playbackSettings.value = _playbackSettings.value.copy(
            ayahRepeat = ayahRepeat,
            rangeRepeat = rangeLoop,
            delayMs = delaySec * 1000L
        )
        hifdhPrefs.edit()
            .putInt("pb_ayah_repeat", ayahRepeat)
            .putInt("pb_range_repeat", rangeLoop)
            .putLong("pb_delay_ms", delaySec * 1000L)
            .apply()
        repeatGeneration++
        repeatDelayJob?.cancel()
        repeatAyahCount = 0
        repeatRangeCount = 0
        repeatSeekPending = false
        lastMediaIndex = 0
        player.setPlaybackSpeed(_playbackSettings.value.speed)
        player.setMediaItems(items)
        player.prepare()
        player.play()
    }

    /** Web: Memorization.jsx effect on currentVerseIndex — keep playing, follow the visible chunk. */
    fun followHifdhChunk(verses: List<VerseEntity>, chapterId: Int, ayahRepeat: Int, delaySec: Int, rangeLoop: Int) {
        if (!hifdhLoopActive || !_isPlaying.value) return
        val (playable, items) = buildPlaylistItems(verses, chapterId)
        if (playable.isEmpty()) return
        if (hifdhPlaylist.map { it.verseKey } == playable.map { it.verseKey }) return
        val player = getOrCreatePlayer()
        hifdhPlaylist = playable
        playlistChapterId = chapterId
        hifdhAyahRepeat = ayahRepeat
        hifdhDelayMs = delaySec * 1000L
        hifdhRangeLoop = rangeLoop
        hifdhAyahPlayCount = 0
        hifdhRangeCount = 0
        _playbackSettings.value = _playbackSettings.value.copy(
            ayahRepeat = ayahRepeat,
            rangeRepeat = rangeLoop,
            delayMs = delaySec * 1000L
        )
        repeatGeneration++
        repeatDelayJob?.cancel()
        repeatAyahCount = 0
        repeatRangeCount = 0
        repeatSeekPending = false
        lastMediaIndex = 0
        player.setPlaybackSpeed(_playbackSettings.value.speed)
        player.setMediaItems(items)
        player.prepare()
        player.play()
    }

    /** Web: Memorization.jsx handleAudioEnded — ayah repeat, then next ayah, then range loop. */
    private fun handleHifdhChunkEnded() {
        val player = exoPlayer ?: return
        val currentIdx = player.currentMediaItemIndex
        if (currentIdx < 0 || currentIdx >= hifdhPlaylist.size) {
            hifdhLoopActive = false
            return
        }
        if (hifdhAyahRepeat == -1 || hifdhAyahPlayCount + 1 < hifdhAyahRepeat) {
            if (hifdhAyahRepeat != -1) hifdhAyahPlayCount++
            hifdhDelayThen { player.seekTo(currentIdx, 0L); player.play() }
            return
        }
        hifdhAyahPlayCount = 0
        if (currentIdx < hifdhPlaylist.size - 1) {
            hifdhDelayThen { player.seekTo(currentIdx + 1, 0L); player.play() }
            return
        }
        if (hifdhRangeLoop == -1 || hifdhRangeCount + 1 < hifdhRangeLoop) {
            if (hifdhRangeLoop != -1) hifdhRangeCount++
            hifdhDelayThen { player.seekTo(0, 0L); player.play() }
            return
        }
        hifdhLoopActive = false
        hifdhAyahPlayCount = 0
        hifdhRangeCount = 0
        _isPlaying.value = false
        _playingVerseKey.value = null
        player.pause()
    }

    private fun hifdhDelayThen(action: () -> Unit) {
        hifdhDelayJob?.cancel()
        if (hifdhDelayMs > 0) {
            hifdhDelayJob = viewModelScope.launch {
                delay(hifdhDelayMs)
                if (_isPlaying.value) action()
            }
        } else {
            action()
        }
    }

    /** Shared local-first starter: metadata items, repeat reset, stored speed. */
    private fun startPlayback(verses: List<VerseEntity>, chapterId: Int, startIndex: Int) {
        val (playable, items) = buildPlaylistItems(verses, chapterId)
        if (playable.isEmpty()) return
        hifdhLoopActive = false
        repeatGeneration++
        repeatDelayJob?.cancel()
        hifdhDelayJob?.cancel()
        hifdhAyahPlayCount = 0
        hifdhRangeCount = 0
        repeatAyahCount = 0
        repeatRangeCount = 0
        repeatSeekPending = false
        playlist = playable
        playlistChapterId = chapterId
        val player = getOrCreatePlayer()
        player.setPlaybackSpeed(_playbackSettings.value.speed)
        player.setMediaItems(items)
        val idx = startIndex.coerceIn(0, playable.size - 1)
        lastMediaIndex = idx
        player.seekTo(idx, 0L)
        player.prepare()
        // Optimistic state so the mini-player (web: GlobalAudioPlayer pill)
        // appears instantly; the player listener keeps it in sync after.
        _playingVerseKey.value = playable.getOrNull(idx)?.verseKey
        _isPlaying.value = true
        player.play()
        // Gapless linked file (one surah Uri shared by all items): drive the
        // active verse from timing data so highlight/follow stay ayah-precise.
        maybeStartGaplessFollow(items, chapterId)
    }

    /**
     * Starts position polling when the playlist is a single gapless surah file
     * with imported timings; no-op for normal per-ayah playlists.
     */
    private fun maybeStartGaplessFollow(items: List<MediaItem>, chapterId: Int) {
        stopFollow()
        try {
            val uris = items.mapNotNull { it.localConfiguration?.uri?.toString() }.distinct()
            if (uris.size != 1) return
            val reciterId = _currentReciterId.value
            viewModelScope.launch {
                val hasTimings = runCatching { timingImporter.hasTimings(reciterId, chapterId) }.getOrDefault(false)
                if (!hasTimings) return@launch
                startFollowAyah(chapterId) { ayah ->
                    _playingVerseKey.value = "$chapterId:$ayah"
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun startPlaylist(verses: List<VerseEntity>, chapterId: Int, startIndex: Int) {
        _playbackSettings.value = _playbackSettings.value.copy(rangeStart = null, rangeEnd = null)
        startPlayback(verses, chapterId, startIndex)
    }

    /** Plays a closed verse range with the configured ayah/range repeat applied. */
    fun playRange(verses: List<VerseEntity>, chapterId: Int, startVerseKey: String, endVerseKey: String) {
        if (verses.isEmpty()) return
        val fromKey = verses.indexOfFirst { it.verseKey == startVerseKey }.coerceAtLeast(0)
        val toKey = verses.indexOfFirst { it.verseKey == endVerseKey }.takeIf { it >= 0 } ?: (verses.size - 1)
        val from = minOf(fromKey, toKey)
        val to = maxOf(fromKey, toKey)
        val sub = verses.subList(from, to + 1)
        if (sub.isEmpty()) return
        _playbackSettings.value = _playbackSettings.value.copy(
            rangeStart = sub.first().verseKey,
            rangeEnd = sub.last().verseKey
        )
        startPlayback(sub, chapterId, 0)
        // Cache the full chapter (not just the range) for offline next time.
        autoCacheChapter(_currentReciterId.value, chapterId, verses)
    }

    fun stopPlaying() {
        repeatGeneration++
        repeatDelayJob?.cancel()
        hifdhDelayJob?.cancel()
        stopFollow()
        hifdhLoopActive = false
        hifdhPlaylist = emptyList()
        hifdhAyahPlayCount = 0
        hifdhRangeCount = 0
        repeatAyahCount = 0
        repeatRangeCount = 0
        repeatSeekPending = false
        lastMediaIndex = -1
        exoPlayer?.stop()
        exoPlayer?.clearMediaItems()
        playlist = emptyList()
        playlistChapterId = 0
        _isPlaying.value = false
        _playingVerseKey.value = null
    }

    // ── Download ────────────────────────────────────────────────────────
    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()

    fun downloadChapterAudio(chapterId: Int, verses: List<VerseEntity>) {
        if (_isDownloading.value) return
        viewModelScope.launch {
            _isDownloading.value = true
            _downloadProgress.value = 0f
            try {
                val ok = audioDownloadManager.downloadChapter(chapterId, verses) { done, total ->
                    _downloadProgress.value = if (total > 0) done.toFloat() / total else 1f
                }
                if (ok) {
                    _downloadedChapters.value = audioDownloadManager.getDownloadedChapters()
                    _downloadProgress.value = 1f
                }
            } finally {
                _isDownloading.value = false
            }
        }
    }

    /**
     * Reciter-aware entry point: resolves the chapter verses locally, then
     * downloads into that reciter's folder (web: per-reciter pack).
     */
    fun downloadChapterAudio(reciterId: Int, chapterId: Int) {
        if (_isDownloading.value) return
        viewModelScope.launch {
            _isDownloading.value = true
            _downloadProgress.value = 0f
            try {
                val verses = repository.getVersesByChapterDirect(chapterId)
                val ok = audioDownloadManager.downloadChapter(reciterId, chapterId, verses) { done, total ->
                    _downloadProgress.value = if (total > 0) done.toFloat() / total else 1f
                }
                if (ok) {
                    _downloadedChapters.value = audioDownloadManager.getDownloadedChapters()
                    _downloadProgress.value = 1f
                }
            } finally {
                _isDownloading.value = false
            }
        }
    }

    private val chapterMemoryCache = java.util.concurrent.ConcurrentHashMap<Int, SurahUiState.Success>()
    private val surahScrollPositions = java.util.concurrent.ConcurrentHashMap<Int, Pair<Int, Int>>()

    fun saveSurahScrollPosition(chapterId: Int, index: Int, offset: Int) {
        if (chapterId > 0) {
            surahScrollPositions[chapterId] = Pair(index, offset)
        }
    }

    fun getSurahScrollPosition(chapterId: Int): Pair<Int, Int>? {
        return surahScrollPositions[chapterId]
    }

    // ── Chapter loading ─────────────────────────────────────────────────

    private var loadChapterJob: Job? = null

    fun loadChapterDetails(chapterId: Int) {
        val requestedMushaf = _mushafPreset.value
        if (currentChapterId == chapterId && _uiState.value is SurahUiState.Success
            && loadedMushafByChapter[chapterId] == requestedMushaf
        ) {
            return
        }
        currentChapterId = chapterId
        _tafsirState.value = TafsirUiState.Hidden
        cachedTafsirVerses = emptyList()

        val memoryCached = chapterMemoryCache[chapterId]
        if (memoryCached != null) {
            _uiState.value = memoryCached
            currentChapterName = memoryCached.chapter.nameSimple
        }

        loadChapterJob?.cancel()
        loadChapterJob = viewModelScope.launch(Dispatchers.IO) {
            val t0 = System.currentTimeMillis()
            // Chapter metadata offline-first (one indexed query when already seeded).
            repository.ensureChaptersFromAssets()
            var localChapter = repository.getChapterById(chapterId)
            var cachedVerses = if (localChapter != null) repository.getVersesByChapterDirect(localChapter.id) else emptyList()

            if (loadedMushafByChapter[chapterId] != null
                && loadedMushafByChapter[chapterId] != _mushafPreset.value
            ) {
                // Bust the in-memory cache when the mushaf changed (line numbers differ).
                chapterMemoryCache.remove(chapterId)
            }

            if (cachedVerses.isEmpty()) {
                // Offline-first: bundled assets paint instantly, no network.
                repository.ensureOfflineChapter(chapterId)
                if (localChapter == null) localChapter = repository.getChapterById(chapterId)
                if (localChapter != null) cachedVerses = repository.getVersesByChapterDirect(localChapter.id)
            }

            val chapter = localChapter
            if (chapter != null && cachedVerses.isNotEmpty()) {
                currentChapterName = chapter.nameSimple
                repository.addRecentlyRead(chapter.id, chapter.nameSimple)
                val wordsMap = repository.getWordsForVerses(cachedVerses.map { it.id })
                val existingTajweed = (_uiState.value as? SurahUiState.Success)?.tajweedMap ?: emptyMap()
                val successState = SurahUiState.Success(chapter, cachedVerses, wordsMap, existingTajweed)
                chapterMemoryCache[chapterId] = successState
                if (currentChapterId == chapterId) _uiState.value = successState
                android.util.Log.d("SurahPerf", "surah $chapterId first paint in ${System.currentTimeMillis() - t0}ms (offline)")
                // Background upgrade: selected translation + line numbers + tajweed cache.
                // Verse ids/order never change, so scroll position is preserved.
                // Child of loadChapterJob: a fast chapter switch cancels it.
                try {
                    repository.refreshVersesByChapter(chapterId, _currentTranslationId.value, _mushafPreset.value)
                    val updatedVerses = repository.getVersesByChapterDirect(chapterId)
                    if (updatedVerses.isNotEmpty() && currentChapterId == chapterId) {
                        val updatedWords = repository.getWordsForVerses(updatedVerses.map { it.id })
                        val tajweed = (_uiState.value as? SurahUiState.Success)?.tajweedMap ?: emptyMap()
                        val upgraded = SurahUiState.Success(chapter, updatedVerses, updatedWords, tajweed)
                        chapterMemoryCache[chapterId] = upgraded
                        loadedMushafByChapter[chapterId] = _mushafPreset.value
                        _uiState.value = upgraded
                    } else if (updatedVerses.isNotEmpty()) {
                        loadedMushafByChapter[chapterId] = _mushafPreset.value
                    }
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                }
                try {
                    if (currentChapterId == chapterId) loadTajweed(chapterId)
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                }
                android.util.Log.d("SurahPerf", "surah $chapterId background upgrade done in ${System.currentTimeMillis() - t0}ms total")
            } else {
                _uiState.value = SurahUiState.Error("Chapter $chapterId not found")
            }
        }
    }

    fun loadPageVerses(pageNumber: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = SurahUiState.Loading
            try {
                val verses = repository.getVersesByPage(pageNumber)
                if (verses.isEmpty()) {
                    _uiState.value = SurahUiState.Error("No verses found for page $pageNumber")
                    return@launch
                }
                val chapterId = verses.firstOrNull()?.chapterId ?: 1
                val chapter = repository.getChapterById(chapterId)
                    ?: ChapterEntity(chapterId, "Surah $chapterId", "سورة", "Surah $chapterId", "Chapter", "makkah", 1, 10, pageNumber, pageNumber)
                val wordsMap = repository.getWordsForVerses(verses.map { it.id })
                _uiState.value = SurahUiState.Success(chapter, verses, wordsMap)
            } catch (e: Exception) {
                _uiState.value = SurahUiState.Error(e.localizedMessage ?: "Failed to load page verses")
            }
        }
    }

    private suspend fun loadVerses(chapter: ChapterEntity) {
        val cachedVerses = repository.getVersesByChapterDirect(chapter.id)
        if (cachedVerses.isNotEmpty()) {
            val wordsMap = repository.getWordsForVerses(cachedVerses.map { it.id })
            val tajweed = (_uiState.value as? SurahUiState.Success)?.tajweedMap ?: emptyMap()
            _uiState.value = SurahUiState.Success(chapter, cachedVerses, wordsMap, tajweed)

            val hasTranslations = cachedVerses.any { !it.translation.isNullOrBlank() }
            if (!hasTranslations) {
                try {
                    repository.refreshVersesByChapter(chapter.id, _currentTranslationId.value, _mushafPreset.value)
                    val updatedVerses = repository.getVersesByChapterDirect(chapter.id)
                    val updatedWords = repository.getWordsForVerses(updatedVerses.map { it.id })
                    _uiState.value = SurahUiState.Success(chapter, updatedVerses, updatedWords, tajweed)
                } catch (_: Exception) {
                    // Silent fallback — already showing cached verses
                }
            }
        } else {
            try {
                repository.refreshVersesByChapter(chapter.id, _currentTranslationId.value, _mushafPreset.value)
                val downloadedVerses = repository.getVersesByChapterDirect(chapter.id)
                if (downloadedVerses.isEmpty()) {
                    _uiState.value = SurahUiState.Error("Failed to load Surah ${chapter.nameSimple}.")
                    return
                }
                val downloadedWords = repository.getWordsForVerses(downloadedVerses.map { it.id })
                _uiState.value = SurahUiState.Success(chapter, downloadedVerses, downloadedWords, emptyMap())
            } catch (e: Exception) {
                _uiState.value = SurahUiState.Error("Failed to load Surah ${chapter.nameSimple}.")
            }
        }
    }

    /** Public entry for screens (planner reader) that need tajweed without a full chapter load. */
    fun ensureTajweedForChapter(chapterId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try { loadTajweed(chapterId) } catch (_: Exception) {}
        }
    }

    private suspend fun loadTajweed(chapterId: Int) {
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
            // Tajweed data not available — UI will show plain text
        }
    }

    /** Web: VerseItem inView → addRecentlyRead(chapter.id, name, verse.verse_key). */
    fun updateRecentlyReadVerse(verseKey: String) {
        if (currentChapterId <= 0 || currentChapterName.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.addRecentlyRead(currentChapterId, currentChapterName, verseKey)
        }
    }

    suspend fun getRecentlyReadForChapter(chapterId: Int): RecentlyReadEntity? {
        return repository.getRecentlyReadForChapter(chapterId)
    }

    /**
     * Called when the Surah screen becomes visible. Mirrors the web app's
     * `useEffect(() => { const startTime = Date.now(); ... }, [id])`.
     */
    private var currentSessionType: String = "reading"

    fun startReadingSession(chapterId: Int, sessionType: String = "reading") {
        endReadingSession(currentSessionType)
        readingSessionChapterId = chapterId
        currentSessionType = sessionType
        readingSessionStart = System.currentTimeMillis()
    }

    /**
     * Called when the Surah screen is disposed. Logs a session if the user
     * stayed at least 10 seconds (same threshold as the web app).
     */
    fun endReadingSession(sessionType: String = currentSessionType) {
        if (readingSessionStart == 0L) return
        val duration = (System.currentTimeMillis() - readingSessionStart) / 1000
        readingSessionStart = 0L
        if (duration >= 10) {
            val chapterId = if (readingSessionChapterId > 0) readingSessionChapterId else null
            viewModelScope.launch {
                repository.logReadingSession(duration, sessionType, chapterId)
            }
        }
    }

    fun toggleTajweed() {
        // Web: setTajweed blocked unless supportsTajweedToggle (Indopak never).
        val mushaf = com.nur.quran.data.mushaf.Mushaf.fromId(_mushafPreset.value)
        if (!mushaf.supportsTajweedToggle) return
        _isTajweedEnabled.value = !_isTajweedEnabled.value
        hifdhPrefs.edit().putBoolean("is_tajweed_enabled", _isTajweedEnabled.value).apply()
    }

    fun toggleTranslation() {
        _isTranslationEnabled.value = !_isTranslationEnabled.value
        hifdhPrefs.edit().putBoolean("is_translation_enabled", _isTranslationEnabled.value).apply()
    }

    fun toggleBookmark(verseKey: String, chapterId: Int, surahName: String) {
        viewModelScope.launch {
            val isCurrentlyBookmarked = verseKey in bookmarkedVerses.value
            if (isCurrentlyBookmarked) {
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

    fun isBookmarked(verseKey: String): Boolean {
        return verseKey in bookmarkedVerses.value
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
        _currentTranslationId.value = translationId
        hifdhPrefs.edit().putInt("translation_id", translationId).apply()
        viewModelScope.launch {
            try {
                repository.refreshVersesByChapter(currentChapterId, translationId, _mushafPreset.value)
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

    fun setSelectedArabicFontName(name: String) {
        // Web: setArabicFont coerced to mushaf-compatible font.
        val mushaf = com.nur.quran.data.mushaf.Mushaf.fromId(_mushafPreset.value)
        val requestedId = com.nur.quran.data.mushaf.fontNameToId(name)
        val coercedId = com.nur.quran.data.mushaf.Mushaf.compatibleFontId(mushaf, requestedId)
        val coercedName = com.nur.quran.data.mushaf.fontIdToName(coercedId)
        _selectedArabicFontName.value = coercedName
        hifdhPrefs.edit().putString("arabic_font", coercedName).apply()
    }

    fun setWordTapBehavior(behavior: String) {
        _wordTapBehavior.value = behavior
        hifdhPrefs.edit().putString("word_tap_behavior", behavior).apply()
    }

    fun setMushafPreset(preset: String) {
        // Web: setSelectedMushaf — canonicalize, coerce font, force tajweed rules, reload.
        val mushaf = com.nur.quran.data.mushaf.Mushaf.fromId(preset)
        _mushafPreset.value = mushaf.id
        hifdhPrefs.edit().putString("mushaf_preset", mushaf.id).apply()
        // Coerce font to a mushaf-compatible one (web: getCompatibleArabicFontId).
        val currentFontId = com.nur.quran.data.mushaf.fontNameToId(_selectedArabicFontName.value)
        val coercedId = com.nur.quran.data.mushaf.Mushaf.compatibleFontId(mushaf, currentFontId)
        val coercedName = com.nur.quran.data.mushaf.fontIdToName(coercedId)
        if (coercedName != _selectedArabicFontName.value) {
            _selectedArabicFontName.value = coercedName
            hifdhPrefs.edit().putString("arabic_font", coercedName).apply()
        }
        // Web: Indopak disables tajweed; Tajweed preset forces it on.
        if (!mushaf.supportsTajweedToggle && _isTajweedEnabled.value) {
            _isTajweedEnabled.value = false
            hifdhPrefs.edit().putBoolean("is_tajweed_enabled", false).apply()
        } else if (mushaf.forcesTajweed && !_isTajweedEnabled.value) {
            _isTajweedEnabled.value = true
            hifdhPrefs.edit().putBoolean("is_tajweed_enabled", true).apply()
        }
        // Bust caches so line numbers + script are refetched for the new mushaf.
        chapterMemoryCache.clear()
        loadedMushafByChapter.clear()
        val chapterToReload = currentChapterId
        if (chapterToReload > 0) {
            currentChapterId = 0
            loadChapterDetails(chapterToReload)
        }
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
