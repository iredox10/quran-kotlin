package com.nur.quran.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Check
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.font.Font
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.core.text.HtmlCompat
import com.nur.quran.data.db.entities.ChapterEntity
import com.nur.quran.data.db.entities.VerseEntity
import com.nur.quran.data.db.entities.WordEntity
import com.nur.quran.data.audio.Reciters
import com.nur.quran.data.getHizbByPage
import com.nur.quran.data.getJuzByPage
import com.nur.quran.ui.components.ColoredArabicText
import com.nur.quran.ui.components.audio.AudioSetupSheet
import com.nur.quran.ui.components.audio.MiniPlayer
import com.nur.quran.ui.components.TajweedSegment
import com.nur.quran.ui.components.NurIcons
import com.nur.quran.ui.components.ShareVerseDialog
import com.nur.quran.ui.components.AutoScrollerBar
import com.nur.quran.ui.components.SettingsDrawer
import com.nur.quran.ui.viewmodels.SurahUiState
import com.nur.quran.ui.viewmodels.SurahViewModel
import com.nur.quran.ui.viewmodels.TafsirUiState
import com.nur.quran.utils.TajweedProcessor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import androidx.compose.ui.text.font.FontLoadingStrategy

val fontAmiri = FontFamily(
    Font(com.nur.quran.R.font.amiri_regular, weight = FontWeight.Normal, loadingStrategy = FontLoadingStrategy.OptionalLocal),
    Font(com.nur.quran.R.font.amiri_bold, weight = FontWeight.Bold, loadingStrategy = FontLoadingStrategy.OptionalLocal)
)

val fontKfgqpcHafs = FontFamily(
    Font(com.nur.quran.R.font.kfgqpc_hafs, loadingStrategy = FontLoadingStrategy.OptionalLocal)
)

val fontUthmanTahaNaskh = FontFamily(
    Font(com.nur.quran.R.font.uthman_taha_naskh, loadingStrategy = FontLoadingStrategy.OptionalLocal)
)

val fontNoto = FontFamily(
    Font(com.nur.quran.R.font.noto_regular, weight = FontWeight.Normal, loadingStrategy = FontLoadingStrategy.OptionalLocal),
    Font(com.nur.quran.R.font.noto_bold, weight = FontWeight.Bold, loadingStrategy = FontLoadingStrategy.OptionalLocal)
)

val fontScheherazade = FontFamily(
    Font(com.nur.quran.R.font.scheherazade_regular, weight = FontWeight.Normal, loadingStrategy = FontLoadingStrategy.OptionalLocal),
    Font(com.nur.quran.R.font.scheherazade_bold, weight = FontWeight.Bold, loadingStrategy = FontLoadingStrategy.OptionalLocal)
)

val fontSystemDefault = FontFamily.Default

fun getArabicFontFamily(name: String): FontFamily {
    return try {
        when (name.trim().lowercase()) {
            "kfgqpc-hafs", "kfgqpc hafs" -> fontKfgqpcHafs
            "uthman-taha-naskh", "uthman taha naskh" -> fontUthmanTahaNaskh
            "amiri-quran", "amiri quran" -> fontAmiri
            "noto-naskh-arabic", "noto naskh arabic" -> fontNoto
            "scheherazade-new", "scheherazade new" -> fontScheherazade
            "system default" -> fontSystemDefault
            else -> fontScheherazade
        }
    } catch (e: Throwable) {
        FontFamily.Default
    }
}

fun formatArabicDigits(number: Int): String {
    val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
    return number.toString().map { arabicDigits[it - '0'] }.joinToString("")
}

// The KFGQPC Hafs font draws the full end-of-ayah medallion from the digits alone
// (its GSUB substitutes each digit with the ornament composite), so emitting the
// U+06DD mark before the digits would render TWO medallions.
fun usesEmbeddedEndMarker(fontName: String): Boolean {
    val name = fontName.trim().lowercase()
    return name == "kfgqpc-hafs" || name == "kfgqpc hafs"
}

fun formatArabicVerseEndMarker(verseNumber: Int, fontName: String = "Scheherazade New"): String {
    val digits = formatArabicDigits(verseNumber)
    val mark = if (usesEmbeddedEndMarker(fontName)) digits else "\u06dd$digits"
    return " <tajweed class='end'>$mark</tajweed>"
}

fun formatCleanEndMarker(endWord: WordEntity?, verseNumber: Int, fontName: String = "Scheherazade New"): String {
    val digits = if (endWord != null) {
        val raw = endWord.textUthmani ?: endWord.textQpcHafs ?: ""
        val cleaned = raw.replace("﴿", "").replace("﴾", "").replace("{", "").replace("}", "").replace("\u06dd", "").trim()
        val parsedInt = cleaned.toIntOrNull()
        if (parsedInt != null) {
            formatArabicDigits(parsedInt)
        } else {
            formatArabicDigits(verseNumber)
        }
    } else {
        formatArabicDigits(verseNumber)
    }
    val mark = if (usesEmbeddedEndMarker(fontName)) digits else "\u06dd$digits"
    return " <tajweed class='end'>$mark</tajweed>"
}

fun buildCleanVerseTajweedHtml(fullVerseHtml: String?, words: List<WordEntity>, verseNumber: Int, fontName: String = "Scheherazade New"): String {
    val endWord = words.firstOrNull { it.charTypeName == "end" }
    val cleanEndMarker = formatCleanEndMarker(endWord, verseNumber, fontName)

    val cleanInput = if (!fullVerseHtml.isNullOrBlank()) {
        fullVerseHtml
    } else {
        words.filter { it.charTypeName != "end" }.joinToString(" ") { word ->
            word.textUthmaniTajweed ?: (word.textUthmani ?: "")
        }
    }

    val baseHtml = cleanInput
        .replace("<(span|tajweed|rule)\\s+class=['\"]?end[^'\">]*['\"]?>.*?</(span|tajweed|rule)>".toRegex(RegexOption.IGNORE_CASE), "")
        .replace("<(span|tajweed|rule)\\s+class=['\"]?end[^'\">]*['\"]?>".toRegex(RegexOption.IGNORE_CASE), "")
        .replace("[\u06dd\u06de\u06df\u06e0\u06e2\u06ea-\u06ec\u25cc]".toRegex(), "")
        .replace("&#1757;|&#x0*6dd;|&#x0*6DD;".toRegex(RegexOption.IGNORE_CASE), "")
        .replace("﴿.*?﴾".toRegex(), "")
        .replace("\\{.*?\\}".toRegex(), "")
        .replace("[﴿﴾{}]".toRegex(), "")

    return baseHtml.trim() + cleanEndMarker
}

private val ORNAMENT_EMBEDDED_REGEX = "[\u06dd\u06de\u06df\u06e0\u06e2\u06ea-\u06ec\u25cc﴿﴾{}]".toRegex()
private val ORNAMENT_PLAIN_REGEX = "[\u06df\u06e0\u06e2\u06ea-\u06ec\u25cc]".toRegex()

/**
 * Display text per word, index-aligned with [words] (no reordering/removal,
 * so tap-target indices stay valid).
 *
 * - Picks the mushaf script (QPC Hafs vs Indopak).
 * - KFGQPC Hafs shapes bare digits into ONE ayah medallion via GSUB, so any
 *   U+06DD/ornament chars are stripped — otherwise TWO medallions render
 *   (empty frame + numbered medallion).
 * - Other fonts need the U+06DD frame to draw the medallion, so it is kept
 *   (and ensured) on the single end marker.
 * - Extra `end` words (e.g. stale offline rows surviving alongside network
 *   rows) collapse to "" so exactly one ayah marker renders.
 */
fun mushafPlainWordTexts(words: List<WordEntity>, mushafId: String, fontName: String): List<String> {
    val mushaf = com.nur.quran.data.mushaf.Mushaf.fromId(mushafId)
    val embedded = usesEmbeddedEndMarker(fontName)
    var endSeen = false
    return words.map { word ->
        var t = com.nur.quran.data.mushaf.wordTextForMushaf(mushaf, word.textUthmani, word.textIndopak, word.textQpcHafs)
        t = if (embedded) t.replace(ORNAMENT_EMBEDDED_REGEX, "") else t.replace(ORNAMENT_PLAIN_REGEX, "")
        if (word.charTypeName == "end") {
            if (endSeen) return@map ""
            endSeen = true
            t = t.trim()
            if (!embedded && t.isNotBlank() && !t.contains("\u06dd")) t = "\u06dd$t"
        }
        t
    }
}

/** Verse-level fallback (words empty): same per-font ornament rules. */
fun mushafPlainVerseText(verse: VerseEntity, mushafId: String, fontName: String): String {
    val mushaf = com.nur.quran.data.mushaf.Mushaf.fromId(mushafId)
    var t = com.nur.quran.data.mushaf.verseTextForMushaf(mushaf, verse.textUthmani, verse.textIndopak, verse.textQpcHafs)
    t = if (usesEmbeddedEndMarker(fontName)) t.replace(ORNAMENT_EMBEDDED_REGEX, "") else t.replace(ORNAMENT_PLAIN_REGEX, "")
    return t
}

// Colors matching the web app CSS variables (index.css) with dynamic dark mode mapping
var isDarkThemeGlobal by mutableStateOf(false)
    internal set

// Web: --h-cream      light: #FAF7F0  dark: #2D2D2A
val hCream     get() = if (isDarkThemeGlobal) Color(0xFF2D2D2A) else Color(0xFFFAF7F0)
// Web: --h-bone       light: #EDE8DA  dark: #3A3A36
val hBone      get() = if (isDarkThemeGlobal) Color(0xFF3A3A36) else Color(0xFFEDE8DA)
// Web: --h-bone-dark  light: #DDD7C7  dark: #4A4A45
val hBoneDark  get() = if (isDarkThemeGlobal) Color(0xFF4A4A45) else Color(0xFFDDD7C7)
// Web: --h-ink        light: #2B3F3C  dark: #EFECE4
val hInk       get() = if (isDarkThemeGlobal) Color(0xFFEFECE4) else Color(0xFF2B3F3C)
// Web: --h-ink-mid    light: #4D5F5C  dark: #B0ABA5
val hInkMid    get() = if (isDarkThemeGlobal) Color(0xFFB0ABA5) else Color(0xFF4D5F5C)
// Web: --h-ink-muted  light: #8E9B97  dark: #5C5855
val hInkMuted  get() = if (isDarkThemeGlobal) Color(0xFF5C5855) else Color(0xFF8E9B97)
// Web: --h-gold       light: #B8924A  dark: #C6A87C
val hGold      get() = if (isDarkThemeGlobal) Color(0xFFC6A87C) else Color(0xFFB8924A)
// Web: --h-gold-soft  light: rgba(184,146,74,0.18) dark: rgba(198,168,124,0.1)
val hGoldSoft  get() = if (isDarkThemeGlobal) Color(0x1AC6A87C) else Color(0x2EB8924A)
// Web: --accent-light light: rgba(198,168,124,0.15) dark: rgba(198,168,124,0.1)
val hGoldLight get() = if (isDarkThemeGlobal) Color(0x1AC6A87C) else Color(0x26C6A87C)
// Web: --h-teal       light: #2E4F4A  dark: #4A7A72
val hTeal      get() = if (isDarkThemeGlobal) Color(0xFF4A7A72) else Color(0xFF2E4F4A)
// Web: --h-teal-soft  light: rgba(46,79,74,0.08)  dark: rgba(46,79,74,0.15)
val hTealSoft  get() = if (isDarkThemeGlobal) Color(0x262E4F4A) else Color(0x142E4F4A)
// Web: --h-white      light: #FAFAF5  dark: #1A1A18
val hWhite     get() = if (isDarkThemeGlobal) Color(0xFF1A1A18) else Color(0xFFFAFAF5)
// Web: --bg-surface   light: #EFECE4  dark: #2D2D2A
val hSurface   get() = if (isDarkThemeGlobal) Color(0xFF2D2D2A) else Color(0xFFEFECE4)
// Web: --border-color  (transparent in both, but we use bone-dark for visible borders)
val hBorderColor get() = if (isDarkThemeGlobal) Color(0xFF4A4A45) else Color(0xFFDDD7C7)
val hGreen     get() = if (isDarkThemeGlobal) Color(0xFF10b981) else Color(0xFF10b981)
val hRed       get() = if (isDarkThemeGlobal) Color(0xFFEF4444) else Color(0xFFEF4444)
val hTealMid   get() = if (isDarkThemeGlobal) Color(0xFF3D6560) else Color(0xFF3D6560)
val glassBg    get() = if (isDarkThemeGlobal) Color(0x802D2D2A) else Color(0x99EFECE4)


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SurahScreen(
    viewModel: SurahViewModel,
    chapterId: Int,
    targetVerseKey: String? = null,
    onBackClick: () -> Unit,
    onNavigateToSurah: (Int, String?) -> Unit = { _, _ -> },
    saukaAssignmentId: String? = null,
    backToSauka: String? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val isTajweedEffective by viewModel.isTajweedEffective.collectAsState()
    val currentMushaf by viewModel.currentMushaf.collectAsState()
    val isTranslationEnabled by viewModel.isTranslationEnabled.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val playingVerseKey by viewModel.playingVerseKey.collectAsState()
    val currentReciterId by viewModel.currentReciterId.collectAsState()
    val playbackSettings by viewModel.playbackSettings.collectAsState()
    val streamOnly by viewModel.streamOnly.collectAsState()
    val tafsirState by viewModel.tafsirState.collectAsState()
    val bookmarkedVerses by viewModel.bookmarkedVerses.collectAsState()
    val allChapters by viewModel.allChapters.collectAsState()
    val downloadedChapters by viewModel.downloadedChapters.collectAsState()
    // Link-in-place state (reciterId -> linked surah ids). Provided by the
    // audio-link agent; the build fixer aligns the ViewModel property.
    val linkedMap by viewModel.linkedState.collectAsState()
    val isDownloading by viewModel.isDownloading.collectAsState()
    val collections by viewModel.collections.collectAsState()
    val collectionItems by viewModel.collectionItems.collectAsState()

    // New states for Memorize, Font Scales, and Sauka
    val isMemorizeModeEnabled by viewModel.isMemorizeModeEnabled.collectAsState()
    val memorizedAyahs by viewModel.memorizedAyahs.collectAsState()
    val arabicFontScale by viewModel.arabicFontScale.collectAsState()
    val translationFontScale by viewModel.translationFontScale.collectAsState()
    val isSaukaCompleting by viewModel.isSaukaCompleting.collectAsState()
    val activeTranslationId by viewModel.currentTranslationId.collectAsState()
    val wordTapBehavior by viewModel.wordTapBehavior.collectAsState()
    val selectedArabicFontName by viewModel.selectedArabicFontName.collectAsState(initial = "Scheherazade New")
    val fontFamilyArabic = remember(selectedArabicFontName) {
        getArabicFontFamily(selectedArabicFontName)
    }

    var isReadingMode by remember { mutableStateOf(false) }
    var pendingScrollTarget by remember { mutableStateOf<Int?>(null) }
    var selectedWordForTooltip by remember { mutableStateOf<WordEntity?>(null) }
    var collectionVerse by remember { mutableStateOf<VerseEntity?>(null) }
    var shareVerseDialogTarget by remember { mutableStateOf<VerseEntity?>(null) }
    var showSettingsDrawer by remember { mutableStateOf(false) }
    var showFontSettingsDialog by remember { mutableStateOf(false) }
    var showAudioSetupDialog by remember { mutableStateOf(false) }
    var showNavigationDialog by remember { mutableStateOf(false) }
    var isAutoScrollActive by remember { mutableStateOf(false) }
    var settingsSubView by remember { mutableStateOf(SettingsSubView.ROOT) }
    var settingsActiveTab by remember { mutableStateOf("general") }
    var selectedTajweedRule by remember { mutableStateOf<TajweedRule?>(null) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("hifdh_settings", android.content.Context.MODE_PRIVATE) }
    var showTour by remember {
        mutableStateOf(!prefs.getBoolean("has_seen_surah_tour", false))
    }
    var showSwipeTip by remember {
        mutableStateOf(!prefs.getBoolean("has_seen_swipe_tip", false))
    }

    // Persist dark mode preference (web: Zustand persist middleware)
    LaunchedEffect(Unit) {
        isDarkThemeGlobal = prefs.getBoolean("is_dark_theme", false)
    }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    LaunchedEffect(pendingScrollTarget) {
        pendingScrollTarget?.let { target ->
            listState.scrollToItem(target)
            pendingScrollTarget = null
        }
    }

    // Follow-along: scroll the now-playing ayah into view while auto-scroll is on.
    // Header offset mirrors the targetVerseKey effect above (header + optional
    // banners + basmala). No-op in reading mode (page items, not verse items).
    LaunchedEffect(playingVerseKey) {
        val key = playingVerseKey ?: return@LaunchedEffect
        if (!isAutoScrollActive || isReadingMode) return@LaunchedEffect
        val state = uiState
        if (state !is SurahUiState.Success || state.chapter.id != chapterId) return@LaunchedEffect
        val verseIndex = state.verses.indexOfFirst { it.verseKey == key }
        if (verseIndex < 0) return@LaunchedEffect
        var hOffset = 1
        if (isMemorizeModeEnabled) hOffset += 1
        if (showSwipeTip) hOffset += 1
        if (saukaAssignmentId != null && backToSauka != null) hOffset += 1
        if (isMemorizeModeEnabled) hOffset += 1
        if (chapterId != 1 && chapterId != 9) hOffset += 1
        try {
            listState.animateScrollToItem(verseIndex + hOffset)
        } catch (_: Exception) {
            // List not laid out yet — next key change will retry
        }
    }

    var isAutoScrollPaused by remember { mutableStateOf(false) }
    var autoScrollSpeed by remember { mutableIntStateOf(3) }
    val speedMap = remember { mapOf(1 to 12f, 2 to 24f, 3 to 45f, 4 to 90f, 5 to 150f, 6 to 270f, 7 to 450f) }

    LaunchedEffect(isAutoScrollActive, isAutoScrollPaused, autoScrollSpeed) {
        if (isAutoScrollActive && !isAutoScrollPaused) {
            val pxPerSec = speedMap[autoScrollSpeed] ?: 45f
            var lastFrameTimeNanos = 0L
            var remainder = 0f

            while (isAutoScrollActive && !isAutoScrollPaused) {
                withFrameNanos { frameTimeNanos ->
                    if (lastFrameTimeNanos != 0L) {
                        val deltaSec = (frameTimeNanos - lastFrameTimeNanos) / 1_000_000_000f
                        val nextDistance = remainder + pxPerSec * deltaSec
                        val wholePixels = nextDistance.toInt()
                        remainder = nextDistance - wholePixels
                        if (wholePixels > 0) {
                            if (listState.canScrollForward) {
                                coroutineScope.launch {
                                    listState.scrollBy(wholePixels.toFloat())
                                }
                            } else {
                                isAutoScrollActive = false
                                isAutoScrollPaused = false
                            }
                        }
                    }
                    lastFrameTimeNanos = frameTimeNanos
                }
            }
        }
    }

    remember(chapterId) {
        viewModel.loadChapterDetails(chapterId)
        true
    }

    LaunchedEffect(chapterId, listState) {
        snapshotFlow { Pair(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) }
            .collect { (index, offset) ->
                if (index >= 0) {
                    viewModel.saveSurahScrollPosition(chapterId, index, offset)
                }
            }
    }

    LaunchedEffect(chapterId, targetVerseKey, uiState) {
        val state = uiState
        if (state is SurahUiState.Success && state.chapter.id == chapterId) {
            if (!targetVerseKey.isNullOrBlank()) {
                val index = state.verses.indexOfFirst { it.verseKey == targetVerseKey }
                if (index >= 0) {
                    var hOffset = 1
                    if (isMemorizeModeEnabled) hOffset += 1
                    if (showSwipeTip) hOffset += 1
                    if (saukaAssignmentId != null && backToSauka != null) hOffset += 1
                    if (isMemorizeModeEnabled) hOffset += 1
                    if (chapterId != 1 && chapterId != 9) hOffset += 1
                    listState.scrollToItem(index + hOffset)
                } else {
                    val saved = viewModel.getSurahScrollPosition(chapterId)
                    if (saved != null) {
                        listState.scrollToItem(saved.first, saved.second)
                    } else {
                        listState.scrollToItem(0)
                    }
                }
            } else {
                val saved = viewModel.getSurahScrollPosition(chapterId)
                if (saved != null) {
                    listState.scrollToItem(saved.first, saved.second)
                } else {
                    listState.scrollToItem(0)
                }
            }
        }
    }

    // Reading-session timer: starts when the screen opens, logs on exit
    DisposableEffect(chapterId) {
        viewModel.startReadingSession(chapterId)
        onDispose {
            viewModel.endReadingSession()
        }
    }

    // Save scroll position per surah (web: surahScrollPositions)
    DisposableEffect(chapterId) {
        onDispose {
            viewModel.scrollPositions[chapterId] =
                listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        }
    }

    // Swipe left/right to move between surahs (web: useSwipeable, delta 50)
    var swipeTotal by remember { mutableStateOf(0f) }
    val swipeThreshold = with(androidx.compose.ui.platform.LocalDensity.current) { 60.dp.toPx() }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    val state = uiState
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showNavigationDialog = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (state is SurahUiState.Success) state.chapter.nameSimple else "Quran Nur",
                            fontFamily = fontFamilyUi,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = hInk,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = NurIcons.ChevronDown,
                            contentDescription = "Select Surah",
                            tint = hInkMuted,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = NurIcons.ArrowLeft,
                            contentDescription = "Back",
                            tint = hInk,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                actions = {
                    TopBarIconBtn(
                        icon = NurIcons.ChevronsDown,
                        active = isAutoScrollActive,
                        label = "Auto-scroll"
                    ) { isAutoScrollActive = !isAutoScrollActive }
                    TopBarIconBtn(
                        icon = NurIcons.BookOpen,
                        active = isReadingMode,
                        label = "Reading mode"
                    ) {
                        val state = uiState
                        val verses = if (state is SurahUiState.Success) state.verses else emptyList()
                        val chapter = if (state is SurahUiState.Success) state.chapter else null
                        var hOffset = 1
                        if (chapter != null && chapter.id != 1 && chapter.id != 9) hOffset += 1
                        val visibleItem = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.offset >= -it.size / 2 }
                        
                        if (!isReadingMode) {
                            val verseIndex = (visibleItem?.index ?: hOffset) - hOffset
                            isReadingMode = true
                            if (verseIndex >= 0 && verseIndex < verses.size) {
                                val pageToScroll = verses[verseIndex].pageNumber
                                val pagesList = verses.groupBy { it.pageNumber }.keys.toList().sorted()
                                val pageIndex = pagesList.indexOf(pageToScroll)
                                if (pageIndex >= 0) {
                                    pendingScrollTarget = pageIndex + hOffset
                                }
                            }
                        } else {
                            val pageIndex = (visibleItem?.index ?: hOffset) - hOffset
                            isReadingMode = false
                            val pagesList = verses.groupBy { it.pageNumber }.keys.toList().sorted()
                            if (pageIndex >= 0 && pageIndex < pagesList.size) {
                                val pageNumber = pagesList[pageIndex]
                                val firstVerseOnPage = verses.firstOrNull { it.pageNumber == pageNumber }
                                if (firstVerseOnPage != null) {
                                    val verseIndex = verses.indexOf(firstVerseOnPage)
                                    if (verseIndex >= 0) {
                                        pendingScrollTarget = verseIndex + hOffset
                                    }
                                }
                            }
                        }
                    }
                    TopBarIconBtn(
                        icon = NurIcons.Volume2,
                        active = isPlaying,
                        label = "Audio options"
                    ) { showAudioSetupDialog = true }
                    TopBarIconBtn(
                        icon = if (isDarkThemeGlobal) NurIcons.Sun else NurIcons.Moon,
                        active = isDarkThemeGlobal,
                        label = "Toggle theme"
                    ) { isDarkThemeGlobal = !isDarkThemeGlobal; prefs.edit().putBoolean("is_dark_theme", isDarkThemeGlobal).apply() }

                    TopBarIconBtn(
                        icon = Icons.Default.Settings,
                        active = showSettingsDrawer,
                        label = "Settings"
                    ) { showSettingsDrawer = true }
                    Spacer(modifier = Modifier.width(8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = glassBg,
                    scrolledContainerColor = glassBg
                ),
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(hWhite)
                .padding(paddingValues)
                .pointerInput(chapterId) {
                    detectHorizontalDragGestures(
                        onDragStart = { swipeTotal = 0f },
                        onDragEnd = {
                            when {
                                swipeTotal > swipeThreshold && chapterId < 114 ->
                                    onNavigateToSurah(chapterId + 1, null)
                                swipeTotal < -swipeThreshold && chapterId > 1 ->
                                    onNavigateToSurah(chapterId - 1, null)
                            }
                            swipeTotal = 0f
                        },
                        onHorizontalDrag = { _, dragAmount -> swipeTotal += dragAmount }
                    )
                }
        ) {
            when (val state = uiState) {
                is SurahUiState.Loading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = hGold,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Loading Ayahs...",
                            fontFamily = fontFamilyUi,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = hInkMuted
                        )
                    }
                }
                is SurahUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = state.message,
                            color = Color.Red,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                is SurahUiState.Success -> {
                    val chapter = state.chapter
                    val verses = state.verses
                    val wordsMap = state.wordsMap
                    val tajweedMap = state.tajweedMap

                    // Restore scroll position once verses are ready
                    LaunchedEffect(chapterId, verses.isNotEmpty()) {
                        if (verses.isNotEmpty()) {
                            val inMemoryPos = viewModel.scrollPositions[chapterId]
                            if (inMemoryPos != null) {
                                listState.scrollToItem(inMemoryPos.first, inMemoryPos.second)
                            } else {
                                val recentEntry = viewModel.getRecentlyReadForChapter(chapterId)
                                val savedVerseKey = recentEntry?.verseKey
                                if (!savedVerseKey.isNullOrEmpty()) {
                                    val verseIndex = verses.indexOfFirst { it.verseKey == savedVerseKey }
                                    if (verseIndex >= 0) {
                                        var headerOffset = 1
                                        if (chapter.id != 1 && chapter.id != 9) {
                                            headerOffset += 1
                                        }
                                        val targetListIndex = (headerOffset + verseIndex).coerceIn(0, (verses.size + headerOffset - 1).coerceAtLeast(0))
                                        listState.scrollToItem(targetListIndex)
                                    }
                                }
                            }
                        }
                    }

                    // Track the verse in view → recently read with verseKey (web: VerseItem inView)
                    LaunchedEffect(chapterId, verses.isNotEmpty()) {
                        if (verses.isEmpty()) return@LaunchedEffect
                        snapshotFlow { listState.layoutInfo.visibleItemsInfo }
                            .collect { visibleItems ->
                                val verseId = visibleItems
                                    .firstOrNull { it.key is Int && it.offset >= -it.size / 2 }
                                    ?.key as? Int ?: return@collect
                                val verseKey = verses.find { it.id == verseId }?.verseKey ?: return@collect
                                delay(600)
                                viewModel.updateRecentlyReadVerse(verseKey)
                            }
                    }

                    val versesByPage = remember(verses) { verses.groupBy { it.pageNumber }.toSortedMap() }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 120.dp)
                    ) {
                        item {
                            // Linked (link-in-place) surah count for the current reciter.
                            val linkedCount = linkedMap[currentReciterId]?.size ?: 0
                            SurahHeader(
                                chapter = chapter,
                                versesStartPage = verses.firstOrNull()?.pageNumber ?: 0,
                                isPlaying = isPlaying,
                                isDownloaded = chapter.id in downloadedChapters,
                                isDownloading = isDownloading,
                                reciterName = Reciters.nameOf(currentReciterId),
                                downloadedCount = if (chapter.id in downloadedChapters) verses.size else 0,
                                totalCount = verses.size,
                                linkedCount = linkedCount,
                                onPlayClick = { showAudioSetupDialog = true },
                                onDownloadClick = { viewModel.downloadChapterAudio(chapter.id, verses) },
                                onRelinkClick = { showSettingsDrawer = true }
                            )
                        }

                        // Memorize progress banner (web matched)
                        if (isMemorizeModeEnabled) {
                            item {
                                val memorizedInSurah = remember(memorizedAyahs, chapter.id) {
                                    memorizedAyahs.filter { it.startsWith("${chapter.id}:") }.size
                                }
                                val totalInSurah = chapter.versesCount
                                val progress = if (totalInSurah > 0) memorizedInSurah.toFloat() / totalInSurah else 0f

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    colors = CardDefaults.cardColors(containerColor = hGold.copy(alpha = 0.15f)),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, hGold.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = NurIcons.Brain,
                                            contentDescription = null,
                                            tint = hGold,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "Memorization progress",
                                                    fontFamily = fontFamilyUi,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = hInk
                                                )
                                                Text(
                                                    text = "$memorizedInSurah/$totalInSurah ayahs",
                                                    fontFamily = fontFamilyUi,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = hInk
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(8.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(hWhite.copy(alpha = 0.6f))
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxHeight()
                                                        .fillMaxWidth(progress)
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(hGold)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Swipe to navigate tip banner (web matched)
                        if (showSwipeTip) {
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    colors = CardDefaults.cardColors(containerColor = hSurface),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, hBorderColor)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = NurIcons.ArrowLeft,
                                            contentDescription = null,
                                            tint = hGold,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Swipe to Navigate",
                                                fontFamily = fontFamilyUi,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = hInk
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Swipe left or right anywhere on the screen to move between Surahs.",
                                                fontFamily = fontFamilyBody,
                                                fontSize = 12.sp,
                                                color = hInkMid
                                            )
                                        }
                                        IconButton(onClick = {
                                            showSwipeTip = false
                                            prefs.edit().putBoolean("has_seen_swipe_tip", true).apply()
                                        }) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Dismiss",
                                                tint = hInkMuted,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Sauka completion card (mirroring web app completion block)
                        if (saukaAssignmentId != null && backToSauka != null) {
                            item {
                                var isCompleted by remember { mutableStateOf(false) }
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = hSurface),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, hBorderColor)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Sauka Group Reading Completed?",
                                                fontFamily = fontFamilyUi,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = hInk
                                            )
                                            Text(
                                                text = "Tap below to submit your progress to the group.",
                                                fontFamily = fontFamilyBody,
                                                fontSize = 12.sp,
                                                color = hInkMid
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Button(
                                            onClick = {
                                                viewModel.completeSaukaJuz(saukaAssignmentId, backToSauka) {
                                                    isCompleted = true
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = if (isCompleted) Color.Gray else hGold),
                                            enabled = !isCompleted && !isSaukaCompleting
                                        ) {
                                            if (isSaukaCompleting) {
                                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                                            } else {
                                                Text(if (isCompleted) "Completed" else "Mark Done", color = Color.White)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Memorization progress card
                        if (isMemorizeModeEnabled) {
                            item {
                                val memorizedInSurah = verses.filter { it.verseKey in memorizedAyahs }.size
                                val totalInSurah = verses.size
                                val progress = if (totalInSurah > 0) memorizedInSurah.toFloat() / totalInSurah else 0f
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    colors = CardDefaults.cardColors(containerColor = hGoldLight),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, hGold.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(NurIcons.Brain, contentDescription = null, tint = hGold, modifier = Modifier.size(24.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Memorization progress", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = hInk)
                                                Text("$memorizedInSurah/$totalInSurah ayahs", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = hInk)
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            LinearProgressIndicator(
                                                progress = progress,
                                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(100)),
                                                color = hGold,
                                                trackColor = hWhite
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (chapter.id != 1 && chapter.id != 9) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp, horizontal = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ",
                                        fontSize = 28.sp,
                                        fontFamily = fontFamilyArabic,
                                        color = hGold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        if (!isReadingMode) {
                            itemsIndexed(verses, key = { _, verse -> verse.id }) { index, verse ->
                                val prevVerse = if (index > 0) verses[index - 1] else null
                                val showPageDivider = verse.pageNumber != 0 &&
                                    (prevVerse == null || prevVerse.pageNumber != verse.pageNumber)

                                if (showPageDivider) {
                                    WebPageDivider(pageNumber = verse.pageNumber)
                                }
                                WebVerseDivider()

                                val words = wordsMap[verse.id] ?: emptyList()
                                VerseItem(
                                    verse = verse,
                                    words = words,
                                    tajweedMap = tajweedMap,
                                    isTranslationEnabled = isTranslationEnabled,
                                    isTajweedEnabled = isTajweedEffective,
                                    mushafId = currentMushaf.id,
                                    isCurrentlyPlaying = playingVerseKey == verse.verseKey && isPlaying,
                                    isBookmarked = verse.verseKey in bookmarkedVerses,
                                    isTafsirOpen = (tafsirState as? TafsirUiState.Visible)?.verseKey == verse.verseKey,
                                    onPlayClick = { viewModel.playVerse(verses, chapter.id, verse) },
                                    onBookmarkClick = {
                                        viewModel.toggleBookmark(verse.verseKey, chapter.id, chapter.nameSimple)
                                    },
                                    onTafsirClick = {
                                        if ((tafsirState as? TafsirUiState.Visible)?.verseKey == verse.verseKey) {
                                            viewModel.dismissTafsir()
                                        } else {
                                            viewModel.loadTafsir(verse.verseKey, chapter.id)
                                        }
                                    },
                                    onShareClick = { shareVerseDialogTarget = verse },
                                    onAddToCollection = { collectionVerse = verse },
                                    onWordClick = { word ->
                                        if (wordTapBehavior != "none") {
                                            selectedWordForTooltip = word
                                        }
                                    },
                                    onTajweedClick = { selectedTajweedRule = it },
                                    onLoadFootnote = { id -> viewModel.getFootnoteText(id) },
                                    isMemorizeModeEnabled = isMemorizeModeEnabled,
                                    isMemorized = verse.verseKey in memorizedAyahs,
                                    onToggleMemorized = { viewModel.toggleMemorizedAyah(verse.verseKey) },
                                    arabicFontScale = arabicFontScale,
                                    translationFontScale = translationFontScale,
                                    fontFamilyArabic = fontFamilyArabic,
                                    selectedArabicFontName = selectedArabicFontName
                                )
                            }
                        } else {
                            items(versesByPage.entries.toList(), key = { (page, _) -> "page_$page" }) { (page, pageVerses) ->
                                ContinuousReadingPageItem(
                                    page = page,
                                    pageVerses = pageVerses,
                                    wordsMap = wordsMap,
                                    tajweedMap = tajweedMap,
                                    isTajweedEnabled = isTajweedEffective,
                                    mushafId = currentMushaf.id,
                                    onWordClick = { word ->
                                        if (wordTapBehavior != "none") {
                                            selectedWordForTooltip = word
                                        }
                                    },
                                    onTajweedClick = { selectedTajweedRule = it },
                                    arabicFontScale = arabicFontScale,
                                    fontFamilyArabic = fontFamilyArabic,
                                    selectedArabicFontName = selectedArabicFontName
                                )
                            }
                        }

                        if (backToSauka != null && saukaAssignmentId != null) {
                            item {
                                SaukaCompletionBanner(
                                    assignmentId = saukaAssignmentId,
                                    backToSauka = backToSauka,
                                    isCompleting = isSaukaCompleting,
                                    onComplete = {
                                        viewModel.completeSaukaJuz(saukaAssignmentId, backToSauka) {
                                            onBackClick()
                                        }
                                    }
                                )
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(48.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .height(1.dp)
                                    .background(hBorderColor)
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            WebSurahNavButtons(
                                chapter = chapter,
                                allChapters = allChapters,
                                onNavigateToSurah = onNavigateToSurah
                            )
                        }
                    }
                }
            }

            if (showSettingsDrawer) {
                SettingsDrawer(
                    viewModel = viewModel,
                    onDismiss = { showSettingsDrawer = false }
                )
            }

            selectedWordForTooltip?.let { word ->
                WordTranslationTooltipDrawer(
                    word = word,
                    onDismiss = { selectedWordForTooltip = null }
                )
            }

            when (val tafsir = tafsirState) {
                is TafsirUiState.Loading -> TafsirLoadingOverlay()
                is TafsirUiState.Visible -> {
                    TafsirBottomSheet(
                        verseNumber = tafsir.verseNumber,
                        text = tafsir.text,
                        onDismiss = { viewModel.dismissTafsir() }
                    )
                }
                is TafsirUiState.Error -> {
                    TafsirErrorOverlay(
                        message = tafsir.message,
                        onDismiss = { viewModel.dismissTafsir() }
                    )
                }
                TafsirUiState.Hidden -> {}
            }

            // Tajweed rule tooltip BottomSheet
            selectedTajweedRule?.let { rule ->
                TajweedRuleBottomSheet(
                    rule = rule,
                    onDismiss = { selectedTajweedRule = null }
                )
            }

            if (showTour) {
                SurahTourDialog(
                    onDismiss = {
                        showTour = false
                        prefs.edit().putBoolean("has_seen_surah_tour", true).apply()
                    }
                )
            }

            AutoScrollerBar(
                isAutoScrollActive = isAutoScrollActive,
                isAutoScrollPaused = isAutoScrollPaused,
                autoScrollSpeed = autoScrollSpeed,
                onPauseToggle = { isAutoScrollPaused = !isAutoScrollPaused },
                onSpeedChange = { autoScrollSpeed = it },
                onJumpUp = {
                    coroutineScope.launch {
                        listState.scrollBy(-300f)
                    }
                },
                onJumpDown = {
                    coroutineScope.launch {
                        listState.scrollBy(300f)
                    }
                },
                onClose = {
                    isAutoScrollActive = false
                    isAutoScrollPaused = false
                }
            )

            // Bottom mini-player: overlays above BottomNav padding, visible while
            // a verse is loaded in the player. togglePlayPause() is private in
            // the ViewModel, so play/pause goes through playPauseChapter (toggles
            // when this chapter's playlist is loaded, else starts at verse 1);
            // prev/next seek via playVerse on the neighbouring playlist item.
            val playerVerses = (uiState as? SurahUiState.Success)?.verses ?: emptyList()
            MiniPlayer(
                verseKey = playingVerseKey ?: "",
                isPlaying = isPlaying,
                visible = playingVerseKey != null,
                onPlayPause = {
                    if (playerVerses.isNotEmpty()) viewModel.playPauseChapter(playerVerses, chapterId)
                },
                onPrevious = {
                    val idx = playerVerses.indexOfFirst { it.verseKey == playingVerseKey }
                    if (idx > 0) viewModel.playVerse(playerVerses, chapterId, playerVerses[idx - 1])
                },
                onNext = {
                    val idx = playerVerses.indexOfFirst { it.verseKey == playingVerseKey }
                    if (idx >= 0 && idx < playerVerses.size - 1) {
                        viewModel.playVerse(playerVerses, chapterId, playerVerses[idx + 1])
                    }
                },
                onClose = { viewModel.stopPlaying() }
            )

            // Save-to-Collection modal
            collectionVerse?.let { verse ->
                val state = uiState
                CollectionModal(
                    verse = verse,
                    chapterId = (state as? SurahUiState.Success)?.chapter?.id ?: 0,
                    surahName = (state as? SurahUiState.Success)?.chapter?.nameSimple ?: "",
                    collections = collections,
                    collectionItems = collectionItems,
                    onAddToCollection = { collectionId ->
                        viewModel.addToCollection(
                            collectionId,
                            verse.verseKey,
                            (state as? SurahUiState.Success)?.chapter?.id ?: 0,
                            (state as? SurahUiState.Success)?.chapter?.nameSimple ?: ""
                        )
                        collectionVerse = null
                    },
                    onCreateCollection = { name ->
                        viewModel.addCollection(name) { newId ->
                            viewModel.addToCollection(
                                newId,
                                verse.verseKey,
                                (state as? SurahUiState.Success)?.chapter?.id ?: 0,
                                (state as? SurahUiState.Success)?.chapter?.nameSimple ?: ""
                            )
                        }
                        collectionVerse = null
                    },
                    onDismiss = { collectionVerse = null }
                )
            }

            // Share-verse modal
            shareVerseDialogTarget?.let { verse ->
                val state = uiState
                val surahName = (state as? SurahUiState.Success)?.chapter?.nameSimple ?: ""
                ShareVerseDialog(
                    verse = verse,
                    chapterName = surahName,
                    onDismiss = { shareVerseDialogTarget = null }
                )
            }

            // Settings Side Drawer Overlay (matching web SettingsDrawer)
            if (showFontSettingsDialog) {
                // Background dark scrim overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                        .clickable { showFontSettingsDialog = false }
                )

                // Slide-in drawer panel aligned to the right edge
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(320.dp)
                        .align(Alignment.CenterEnd)
                        .background(hWhite)
                        .clickable(enabled = false) {} // prevent click propagation
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(hSurface)
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (settingsSubView != SettingsSubView.ROOT) {
                                    IconButton(
                                        onClick = { settingsSubView = SettingsSubView.ROOT },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowLeft,
                                            contentDescription = "Back",
                                            tint = hInk
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(
                                    text = when (settingsSubView) {
                                        SettingsSubView.TRANSLATION -> "Choose Translation"
                                        SettingsSubView.ARABIC_FONT -> "Choose Arabic Font"
                                        else -> "Settings"
                                    },
                                    fontFamily = fontFamilyUi,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = hInk
                                )
                            }
                            IconButton(
                                onClick = { showFontSettingsDialog = false },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(imageVector = NurIcons.X, contentDescription = "Close settings", tint = hInkMid)
                            }
                        }

                        Divider(color = hBorderColor, thickness = 1.dp)

                        // Content
                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            if (settingsSubView == SettingsSubView.TRANSLATION) {
                                val translationsList = listOf(
                                    85 to "English - M.A.S. Abdel Haleem",
                                    20 to "English - Saheeh International",
                                    22 to "English - A. Yusuf Ali",
                                    84 to "English - Mufti Taqi Usmani",
                                    32 to "Hausa - Abubakar Mahmoud Gumi",
                                    234 to "Urdu - Fatah Muhammad Jalandhari"
                                )
                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                    items(translationsList) { (id, name) ->
                                        val isSelected = id == activeTranslationId
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    viewModel.setTranslationId(id)
                                                    settingsSubView = SettingsSubView.ROOT
                                                }
                                                .then(
                                                    if (isSelected) Modifier.background(hGoldSoft)
                                                    else Modifier
                                                )
                                                .padding(vertical = 14.dp, horizontal = 16.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                name,
                                                fontFamily = fontFamilyUi,
                                                fontSize = 14.sp,
                                                color = if (isSelected) hGold else hInk,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Selected",
                                                    tint = hGold,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        Divider(color = hBorderColor, thickness = 0.5.dp)
                                    }
                                }
                            } else if (settingsSubView == SettingsSubView.ARABIC_FONT) {
                                val fontsList = listOf(
                                    "KFGQPC Hafs",
                                    "Uthman Taha Naskh",
                                    "Amiri Quran",
                                    "Noto Naskh Arabic",
                                    "Scheherazade New",
                                    "System Default"
                                )
                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                    items(fontsList) { fontName ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    viewModel.setSelectedArabicFontName(fontName)
                                                    settingsSubView = SettingsSubView.ROOT
                                                }
                                                .padding(vertical = 14.dp, horizontal = 16.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(fontName, fontFamily = fontFamilyUi, fontSize = 14.sp, color = hInk)
                                        }
                                        Divider(color = hBorderColor, thickness = 0.5.dp)
                                    }
                                }
                            } else {
                                // Root settings
                                Column(modifier = Modifier.fillMaxSize()) {
                                    // Tab selector (General / Reading / Data)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                            .background(hSurface, RoundedCornerShape(12.dp))
                                            .padding(4.dp)
                                    ) {
                                        val tabs = listOf("general" to "General", "reading" to "Reading", "data" to "Data")
                                        tabs.forEach { (tabId, label) ->
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (settingsActiveTab == tabId) hGold else Color.Transparent)
                                                    .clickable { settingsActiveTab = tabId }
                                                    .padding(vertical = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = label,
                                                    color = if (settingsActiveTab == tabId) Color.White else hInkMid,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    fontFamily = fontFamilyUi
                                                )
                                            }
                                        }
                                    }

                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp)
                                    ) {
                                        if (settingsActiveTab == "general") {
                                            Text("Appearance", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = hInkMuted, modifier = Modifier.padding(vertical = 8.dp))
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(hSurface, RoundedCornerShape(12.dp))
                                                    .padding(4.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(if (!isDarkThemeGlobal) hGoldSoft else Color.Transparent)
                                                        .clickable { isDarkThemeGlobal = false; prefs.edit().putBoolean("is_dark_theme", false).apply() }
                                                        .padding(vertical = 8.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text("Light", color = if (!isDarkThemeGlobal) hGold else hInkMid, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(if (isDarkThemeGlobal) hGoldSoft else Color.Transparent)
                                                        .clickable { isDarkThemeGlobal = true; prefs.edit().putBoolean("is_dark_theme", true).apply() }
                                                        .padding(vertical = 8.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text("Dark", color = if (isDarkThemeGlobal) hGold else hInkMid, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(16.dp))
                                            Text("Essentials", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = hInkMuted, modifier = Modifier.padding(vertical = 8.dp))
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(hSurface, RoundedCornerShape(12.dp))
                                            ) {
                                                SettingsSelectionRow(
                                                    label = "Translation",
                                                    value = "English / Other translations"
                                                ) {
                                                    settingsSubView = SettingsSubView.TRANSLATION
                                                }
                                            }
                                        }

                                        if (settingsActiveTab == "reading") {
                                            Text("Text Preferences", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = hInkMuted, modifier = Modifier.padding(vertical = 8.dp))
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(hSurface, RoundedCornerShape(12.dp))
                                                    .padding(8.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                SettingsSelectionRow(
                                                    label = "Arabic Font",
                                                    value = selectedArabicFontName,
                                                    onClick = { settingsSubView = SettingsSubView.ARABIC_FONT }
                                                )
                                                Divider(color = hBorderColor, thickness = 0.5.dp)
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text("Translation Mode", fontSize = 13.sp, color = hInk)
                                                    Switch(
                                                        checked = isTranslationEnabled,
                                                        onCheckedChange = { viewModel.toggleTranslation() },
                                                        colors = SwitchDefaults.colors(checkedThumbColor = hGold, checkedTrackColor = hGoldSoft)
                                                    )
                                                }
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        "Tajweed Color Rules" + if (!currentMushaf.supportsTajweedToggle) " (N/A for IndoPak)" else "",
                                                        fontSize = 13.sp,
                                                        color = if (!currentMushaf.supportsTajweedToggle) hInkMuted else hInk
                                                    )
                                                    Switch(
                                                        checked = isTajweedEffective,
                                                        enabled = currentMushaf.supportsTajweedToggle,
                                                        onCheckedChange = { viewModel.toggleTajweed() },
                                                        colors = SwitchDefaults.colors(checkedThumbColor = hGold, checkedTrackColor = hGoldSoft)
                                                    )
                                                }
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text("Memorize Mode", fontSize = 13.sp, color = hInk)
                                                    Switch(
                                                        checked = isMemorizeModeEnabled,
                                                        onCheckedChange = { viewModel.toggleMemorizeMode() },
                                                        colors = SwitchDefaults.colors(checkedThumbColor = hGold, checkedTrackColor = hGoldSoft)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(16.dp))
                                            Text("Text Sizes", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = hInkMuted, modifier = Modifier.padding(vertical = 8.dp))
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(hSurface, RoundedCornerShape(12.dp))
                                                    .padding(12.dp),
                                                verticalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Column {
                                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                        Text("Arabic Size", fontSize = 13.sp, color = hInk)
                                                        Text("${(arabicFontScale * 100).toInt()}%", fontSize = 12.sp, color = hInkMuted)
                                                    }
                                                    Slider(
                                                        value = arabicFontScale,
                                                        onValueChange = { viewModel.updateArabicFontScale(it - arabicFontScale) },
                                                        valueRange = 0.5f..3.0f,
                                                        colors = SliderDefaults.colors(thumbColor = hGold, activeTrackColor = hGold)
                                                    )
                                                }
                                                Column {
                                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                        Text("Translation Size", fontSize = 13.sp, color = hInk)
                                                        Text("${(translationFontScale * 100).toInt()}%", fontSize = 12.sp, color = hInkMuted)
                                                    }
                                                    Slider(
                                                        value = translationFontScale,
                                                        onValueChange = { viewModel.updateTranslationFontScale(it - translationFontScale) },
                                                        valueRange = 0.5f..3.0f,
                                                        colors = SliderDefaults.colors(thumbColor = hGold, activeTrackColor = hGold)
                                                    )
                                                }
                                            }
                                        }

                                        if (settingsActiveTab == "data") {
                                            Text("Offline Audio Downloads", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = hInkMuted, modifier = Modifier.padding(vertical = 8.dp))
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(hSurface, RoundedCornerShape(12.dp))
                                                    .padding(16.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text("Local Offline Audio connected.", fontSize = 13.sp, color = hInk, textAlign = TextAlign.Center)
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Button(
                                                    onClick = {},
                                                    colors = ButtonDefaults.buttonColors(containerColor = hGold),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text("Folder Connected", color = Color.White)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (showNavigationDialog) {
                SurahNavigationModal(
                    isOpen = showNavigationDialog,
                    onDismiss = { showNavigationDialog = false },
                    allChapters = allChapters,
                    currentChapterId = chapterId,
                    onNavigateToSurah = { surahId ->
                        showNavigationDialog = false
                        onNavigateToSurah(surahId, null)
                    },
                    onJumpToAyah = { targetSurahId, ayahNum ->
                        showNavigationDialog = false
                        val targetKey = "$targetSurahId:$ayahNum"
                        if (targetSurahId != chapterId) {
                            onNavigateToSurah(targetSurahId, targetKey)
                        } else {
                            val state = uiState
                            if (state is SurahUiState.Success) {
                                val verses = state.verses
                                val verseIndex = verses.indexOfFirst { it.verseNumber == ayahNum }
                                if (verseIndex >= 0) {
                                    var hOffset = 1
                                    if (isMemorizeModeEnabled) hOffset += 1
                                    if (showSwipeTip) hOffset += 1
                                    if (saukaAssignmentId != null && backToSauka != null) hOffset += 1
                                    if (isMemorizeModeEnabled) hOffset += 1
                                    if (chapterId != 1 && chapterId != 9) hOffset += 1
                                    coroutineScope.launch {
                                        listState.animateScrollToItem(verseIndex + hOffset)
                                    }
                                }
                            }
                        }
                    },
                    onJumpToPage = { pageNum ->
                        showNavigationDialog = false
                        val targetChapter = allChapters.firstOrNull { pageNum in it.pagesStart..it.pagesEnd }
                        if (targetChapter != null && targetChapter.id != chapterId) {
                            val state = uiState
                            val firstVerseKeyOnPage = if (state is SurahUiState.Success) {
                                state.verses.firstOrNull { it.pageNumber == pageNum }?.verseKey
                            } else null
                            onNavigateToSurah(targetChapter.id, firstVerseKeyOnPage)
                        } else {
                            val state = uiState
                            if (state is SurahUiState.Success) {
                                val verses = state.verses
                                val verseIndex = verses.indexOfFirst { it.pageNumber == pageNum }
                                if (verseIndex >= 0) {
                                    var hOffset = 1
                                    if (isMemorizeModeEnabled) hOffset += 1
                                    if (showSwipeTip) hOffset += 1
                                    if (saukaAssignmentId != null && backToSauka != null) hOffset += 1
                                    if (isMemorizeModeEnabled) hOffset += 1
                                    if (chapterId != 1 && chapterId != 9) hOffset += 1
                                    coroutineScope.launch {
                                        listState.animateScrollToItem(verseIndex + hOffset)
                                    }
                                }
                            }
                        }
                    },
                    fontFamilyUi = fontFamilyUi,
                    fontFamilyArabic = fontFamilyArabic
                )
            }

            if (showAudioSetupDialog && uiState is SurahUiState.Success) {
                val verses = (uiState as SurahUiState.Success).verses
                AudioSetupSheet(
                    chapterId = chapterId,
                    versesCount = verses.size,
                    initialReciterId = currentReciterId,
                    initialAyahRepeat = playbackSettings.ayahRepeat,
                    initialRangeRepeat = playbackSettings.rangeRepeat,
                    initialDelayMs = playbackSettings.delayMs,
                    initialSpeed = playbackSettings.speed,
                    initialStreamOnly = streamOnly,
                    onDismiss = { showAudioSetupDialog = false },
                    onPlayRange = { reciterId, startKey, endKey, ayahRepeat, rangeRepeat, delayMs, speed, streamOnly ->
                        viewModel.setReciterId(reciterId)
                        viewModel.setAyahRepeat(ayahRepeat)
                        viewModel.setRangeRepeat(rangeRepeat)
                        viewModel.setDelayMs(delayMs)
                        viewModel.setSpeed(speed)
                        viewModel.setStreamOnly(streamOnly)
                        viewModel.playRange(verses, chapterId, startKey, endKey)
                        showAudioSetupDialog = false
                    },
                    onPlayAll = { reciterId, ayahRepeat, rangeRepeat, delayMs, speed, streamOnly ->
                        viewModel.setReciterId(reciterId)
                        viewModel.setAyahRepeat(ayahRepeat)
                        viewModel.setRangeRepeat(rangeRepeat)
                        viewModel.setDelayMs(delayMs)
                        viewModel.setSpeed(speed)
                        viewModel.setStreamOnly(streamOnly)
                        if (verses.isNotEmpty()) {
                            viewModel.playRange(
                                verses,
                                chapterId,
                                verses.first().verseKey,
                                verses.last().verseKey
                            )
                        }
                        showAudioSetupDialog = false
                    }
                )
            }
        }
    }
}

// ── Top bar icon button (web Layout IconBtn style) ──────────────────────
@Composable
private fun TopBarIconBtn(
    icon: ImageVector,
    active: Boolean,
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .padding(horizontal = 2.dp)
            .size(36.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (active) hGoldSoft else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (active) hGold else hInkMuted,
            modifier = Modifier.size(18.dp)
        )
    }
}

// ── Surah Header (web-matched) ──────────────────────────────────────────
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SurahHeader(
    chapter: ChapterEntity,
    versesStartPage: Int = 0,
    isPlaying: Boolean,
    isDownloaded: Boolean,
    isDownloading: Boolean,
    reciterName: String = "",
    downloadedCount: Int = 0,
    totalCount: Int = 0,
    linkedCount: Int = 0,
    onPlayClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onRelinkClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 32.dp, bottom = 32.dp, start = 16.dp, end = 16.dp)
            .drawBehind {
                drawLine(
                    color = hBorderColor,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = chapter.nameSimple,
                fontSize = 35.sp,
                fontWeight = FontWeight.ExtraBold,
                color = hInk,
                fontFamily = fontFamilyUi,
                letterSpacing = (-0.5).sp,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = chapter.nameArabic,
                fontSize = 40.sp,
                color = hGold,
                fontFamily = fontFamilyArabic,
                modifier = Modifier.offset(y = (-6).dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Meta row: SURAH N ● {Translated} ● {N} AYAHS ● {Place} ● STARTS JUZ X • HIZB Y
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.Center
        ) {
            val metaItems = buildList {
                add("Surah ${chapter.id}" to true)
                if (chapter.translatedName.isNotBlank()) add(chapter.translatedName to false)
                add("${chapter.versesCount} Ayahs" to false)
                add(chapter.revelationPlace to false)
                if (versesStartPage > 0) {
                    val juz = getJuzByPage(versesStartPage)
                    val hizb = getHizbByPage(versesStartPage)
                    add("Starts Juz ${juz.id} • Hizb ${hizb.id}" to false)
                }
            }
            metaItems.forEachIndexed { index, (text, isAccent) ->
                if (index > 0) {
                    Text(
                        text = "  ●  ",
                        fontSize = 9.sp,
                        color = hInkMuted.copy(alpha = 0.5f),
                        fontFamily = fontFamilyMono
                    )
                }
                Text(
                    text = text.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = if (isAccent) FontWeight.Bold else FontWeight.Medium,
                    color = if (isAccent) hGold else hInkMid,
                    fontFamily = fontFamilyMono,
                    letterSpacing = 1.5.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Play / download pill
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(100))
                .background(hWhite)
                .border(1.dp, hBorderColor, RoundedCornerShape(100))
                .padding(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(hGold)
                    .clickable(onClick = onPlayClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) NurIcons.PauseFilled else NurIcons.PlayFilled,
                    contentDescription = if (isPlaying) "Pause Audio" else "Play Audio",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
            if (!isDownloaded) {
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable(enabled = !isDownloading, onClick = onDownloadClick),
                    contentAlignment = Alignment.Center
                ) {
                    if (isDownloading) {
                        val transition = rememberInfiniteTransition(label = "spin")
                        val angle by transition.animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec = infiniteRepeatable(tween(1000)),
                            label = "angle"
                        )
                        Icon(
                            imageVector = NurIcons.Loader2,
                            contentDescription = "Downloading...",
                            tint = hInkMid,
                            modifier = Modifier.size(18.dp).rotate(angle)
                        )
                    } else {
                        Icon(
                            imageVector = NurIcons.Download,
                            contentDescription = "Download Audio",
                            tint = hInkMid,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Reciter + offline-count subtitle (keeps the pill above untouched)
        if (reciterName.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (linkedCount > 0) {
                    "$reciterName • Linked • $linkedCount/114"
                } else if (totalCount > 0) {
                    "$reciterName • $downloadedCount/$totalCount offline"
                } else {
                    reciterName
                },
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = hInkMuted,
                fontFamily = fontFamilyMono,
                letterSpacing = 0.5.sp,
                textAlign = TextAlign.Center
            )
            if (linkedCount > 0) {
                TextButton(onClick = onRelinkClick) {
                    Text(
                        text = "Re-link",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = hGold,
                        fontFamily = fontFamilyMono
                    )
                }
            }
        }
    }
}

// ── Web-style gradient verse divider ────────────────────────────────────
@Composable
fun WebVerseDivider() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, hGoldLight, hBorderColor, hGoldLight, Color.Transparent)
                    )
                )
        )
        Box(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(hGold.copy(alpha = 0.4f))
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, hGoldLight, hBorderColor, hGoldLight, Color.Transparent)
                    )
                )
        )
    }
}

// ── Web-style Page Divider ──────────────────────────────────────────────
@Composable
fun WebPageDivider(pageNumber: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(Brush.horizontalGradient(colors = listOf(Color.Transparent, hGold, Color.Transparent)))
        )
        Box(
            modifier = Modifier
                .padding(horizontal = 10.dp)
                .clip(RoundedCornerShape(100))
                .background(hGoldLight)
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                text = "Page $pageNumber",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = hGold,
                fontFamily = fontFamilyBody
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(Brush.horizontalGradient(colors = listOf(Color.Transparent, hGold, Color.Transparent)))
        )
    }
}

// ── Verse Item (web-matched actions, footnotes, playing highlight) ──────
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VerseItem(
    verse: VerseEntity,
    words: List<WordEntity>,
    tajweedMap: Map<String, String>?,
    isTranslationEnabled: Boolean,
    isTajweedEnabled: Boolean,
    mushafId: String = "madani-standard",
    isCurrentlyPlaying: Boolean,
    isBookmarked: Boolean,
    isTafsirOpen: Boolean,
    onPlayClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    onTafsirClick: () -> Unit,
    onShareClick: () -> Unit,
    onAddToCollection: () -> Unit,
    onWordClick: (WordEntity) -> Unit,
    onTajweedClick: (TajweedRule) -> Unit = {},
    onLoadFootnote: suspend (String) -> String,
    isMemorizeModeEnabled: Boolean = false,
    isMemorized: Boolean = false,
    onToggleMemorized: () -> Unit = {},
    arabicFontScale: Float = 1.0f,
    translationFontScale: Float = 1.0f,
    fontFamilyArabic: FontFamily = fontScheherazade,
    selectedArabicFontName: String = "Scheherazade New"
) {
    var activeFootnoteId by remember { mutableStateOf<String?>(null) }
    var footnoteText by remember { mutableStateOf("") }
    var isFootnoteLoading by remember { mutableStateOf(false) }
    var isRevealed by remember(isMemorizeModeEnabled) { mutableStateOf(false) }
    val isHidden = isMemorizeModeEnabled && !isRevealed

    LaunchedEffect(activeFootnoteId) {
        val id = activeFootnoteId ?: return@LaunchedEffect
        isFootnoteLoading = true
        footnoteText = onLoadFootnote(id)
        isFootnoteLoading = false
    }

    val playScale by animateFloatAsState(if (isCurrentlyPlaying) 1.01f else 1f, label = "playScale")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = playScale; scaleY = playScale }
            .then(
                if (isCurrentlyPlaying) {
                    Modifier
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(hGoldLight)
                } else Modifier
            )
            .padding(horizontal = if (isCurrentlyPlaying) 24.dp else 40.dp, vertical = 20.dp)
    ) {
        // Verse key + actions row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Plain gold bold text on mobile (web sm:hidden pill)
            Text(
                text = verse.verseKey,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = hGold,
                fontFamily = fontFamilyMono,
                letterSpacing = 0.65.sp
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (isMemorizeModeEnabled) {
                    VerseActionIcon(
                        icon = NurIcons.CheckCircle2,
                        tint = if (isMemorized) hGold else hInkMuted,
                        label = "Toggle Memorized",
                        onClick = onToggleMemorized
                    )
                    VerseActionIcon(
                        icon = if (isRevealed) NurIcons.EyeOff else NurIcons.Eye,
                        tint = if (isRevealed) hGold else hInkMuted,
                        label = "Toggle Reveal",
                        onClick = { isRevealed = !isRevealed }
                    )
                }
                VerseActionIcon(icon = NurIcons.Plus, tint = hInkMuted, label = "Add to Collection", onClick = onAddToCollection)
                VerseActionIcon(
                    icon = if (isBookmarked) NurIcons.BookmarkFilled else NurIcons.Bookmark,
                    tint = if (isBookmarked) hGold else hInkMuted,
                    label = "Bookmark Verse",
                    onClick = onBookmarkClick
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (isCurrentlyPlaying) hGoldLight else Color.Transparent)
                        .clickable(onClick = onPlayClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isCurrentlyPlaying) NurIcons.PauseFilled else NurIcons.PlayFilled,
                        contentDescription = if (isCurrentlyPlaying) "Playing" else "Play this Ayah",
                        tint = if (isCurrentlyPlaying) hGold else hInkMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
                VerseActionIcon(
                    icon = NurIcons.Info,
                    tint = if (isTafsirOpen) hGold else hInkMuted,
                    label = "Read Tafsir",
                    onClick = onTafsirClick
                )
                VerseActionIcon(icon = NurIcons.Share2, tint = hInkMuted, label = "Share verse", onClick = onShareClick)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Arabic words (RTL)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = isHidden) { isRevealed = true }
                .graphicsLayer { alpha = if (isHidden) 0.05f else 1.0f }
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                val fullVerseHtml = tajweedMap?.get(verse.verseKey)
                val isVerseTajweedAvailable = !fullVerseHtml.isNullOrBlank()

                if (isTajweedEnabled && (isVerseTajweedAvailable || words.any { it.textUthmaniTajweed != null })) {
                    val finalTajweedHtml = remember(verse.verseKey, fullVerseHtml, selectedArabicFontName) {
                        buildCleanVerseTajweedHtml(fullVerseHtml, words, verse.verseNumber, selectedArabicFontName)
                    }
                    val tajweedPlainText = remember(verse.verseKey, words, mushafId, selectedArabicFontName) {
                        if (words.isNotEmpty()) {
                            val sb = StringBuilder()
                            val ranges = mutableListOf<Pair<IntRange, Int>>()
                            val displayWords = mushafPlainWordTexts(words, mushafId, selectedArabicFontName)
                            words.forEachIndexed { wordIndex, word ->
                                if (wordIndex > 0) sb.append(" ")
                                val rawText = displayWords.getOrElse(wordIndex) { "" }
                                val start = sb.length
                                sb.append(rawText)
                                ranges.add(Pair(start..sb.length, wordIndex))
                            }
                            sb.toString() to ranges
                        } else {
                            val rawText = mushafPlainVerseText(verse, mushafId, selectedArabicFontName)
                            rawText to emptyList<Pair<IntRange, Int>>()
                        }
                    }
                    val tajweedSegments = remember(finalTajweedHtml, tajweedPlainText.first, isDarkThemeGlobal) {
                        val textColorHex = if (isDarkThemeGlobal) "#EFECE4" else "#2B3F3C"
                        TajweedProcessor.getWordTajweedSegments(tajweedPlainText.first, finalTajweedHtml, textColorHex)
                    }

                    SurahTajweedText(
                        text = tajweedPlainText.first,
                        wordRanges = tajweedPlainText.second,
                        segments = tajweedSegments,
                        fontFamily = fontFamilyArabic,
                        fontSize = (26 * arabicFontScale).sp,
                        lineHeight = (52 * arabicFontScale).sp,
                        isInteractive = !isHidden,
                        onWordClick = { idx ->
                            if (idx >= 0 && idx < words.size) {
                                onWordClick(words[idx])
                            }
                        },
                        onTajweedClick = { ruleClass ->
                            val rule = TajweedRules.RULES[ruleClass]
                            if (rule != null) {
                                onTajweedClick(rule)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    // Non-tajweed: simple verse text as single AnnotatedString
                    val verseAnnotated = remember(verse, words, fontFamilyArabic, arabicFontScale, isDarkThemeGlobal, mushafId, selectedArabicFontName) {
                        buildAnnotatedString {
                            if (words.isNotEmpty()) {
                                val displayWords = mushafPlainWordTexts(words, mushafId, selectedArabicFontName)
                                words.forEachIndexed { wordIndex, word ->
                                    if (wordIndex > 0) append(" ")
                                    val wordStart = length
                                    val isEndMarker = word.charTypeName == "end"
                                    val rawText = displayWords.getOrElse(wordIndex) { "" }
                                    val plainText = rawText
                                    val displayText = plainText
                                    append(displayText)
                                    if (isEndMarker) {
                                        addStyle(
                                            SpanStyle(color = hGold),
                                            wordStart, length
                                        )
                                    }
                                    addStringAnnotation("WORD_INDEX", wordIndex.toString(), wordStart, length)
                                }
                            } else {
                                val rawText = mushafPlainVerseText(verse, mushafId, selectedArabicFontName)
                                val plainText = rawText
                                append(plainText)
                            }
                        }
                    }

                    ClickableText(
                        text = verseAnnotated,
                        modifier = Modifier.fillMaxWidth(),
                        style = TextStyle(
                            fontSize = (26 * arabicFontScale).sp,
                            color = hInk,
                            fontFamily = fontFamilyArabic,
                            textAlign = TextAlign.Right,
                            lineHeight = (52 * arabicFontScale).sp
                        ),
                        onClick = { offset ->
                            if (!isHidden) {
                                val wordAnn = verseAnnotated.getStringAnnotations("WORD_INDEX", offset, offset).firstOrNull()
                                if (wordAnn != null) {
                                    val idx = wordAnn.item.toIntOrNull()
                                    if (idx != null && idx < words.size) {
                                        onWordClick(words[idx])
                                    }
                                }
                            }
                        }
                    )
                }
            }
            if (isHidden) {
                Text(
                    text = "Tap to reveal text",
                    fontSize = 12.sp,
                    color = hInkMuted,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .background(hSurface.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        // Translation with clickable footnotes
        if (isTranslationEnabled && !verse.translation.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(20.dp))
            if (isHidden) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .border(BorderStroke(1.dp, hBorderColor), RoundedCornerShape(10.dp))
                        .background(hSurface)
                        .clickable { isRevealed = true }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text("Tap to reveal translation & meaning", fontSize = 13.sp, color = hInkMuted)
                }
            } else {
                TranslationText(
                    html = verse.translation,
                    onFootnoteClick = { footnoteId ->
                        activeFootnoteId = if (activeFootnoteId == footnoteId) null else footnoteId
                    },
                    translationFontScale = translationFontScale
                )
            }
        }

        // Inline footnote card (web style: left gold border, bg-secondary)
        AnimatedVisibility(
            visible = activeFootnoteId != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            FootnoteCard(
                text = footnoteText,
                isLoading = isFootnoteLoading,
                onClose = { activeFootnoteId = null }
            )
        }
    }
}

@Composable
private fun VerseActionIcon(
    icon: ImageVector,
    tint: Color,
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
    }
}

// ── Footnote card (web: bg-secondary, left accent border) ───────────────
@Composable
private fun FootnoteCard(
    text: String,
    isLoading: Boolean,
    onClose: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .clip(shape)
            .background(hSurface)
            .drawBehind {
                drawRect(
                    color = hGold,
                    topLeft = Offset(0f, 0f),
                    size = androidx.compose.ui.geometry.Size(3.dp.toPx(), size.height)
                )
            }
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = NurIcons.Info,
                    contentDescription = null,
                    tint = hGold,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Footnote",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = hInk
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (isLoading) {
                Text(
                    text = "Loading...",
                    fontSize = 14.sp,
                    color = hInkMuted
                )
            } else {
                val cleanText = remember(text) {
                    HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_LEGACY).toString().trim()
                }
                Text(
                    text = cleanText,
                    fontSize = 14.sp,
                    color = hInk,
                    lineHeight = 22.sp
                )
            }
        }
        Icon(
            imageVector = NurIcons.X,
            contentDescription = "Close footnote",
            tint = hInkMuted,
            modifier = Modifier
                .size(16.dp)
                .clickable(onClick = onClose)
        )
    }
}

// ── Translation text with clickable <sup foot_note> markers ─────────────
private val footnoteSupRegex = Regex("<sup[^>]*foot_note=[\"']?(\\d+)[\"']?[^>]*>\\s*(\\d+)\\s*</sup>")

@Composable
private fun TranslationText(
    html: String,
    onFootnoteClick: (String) -> Unit,
    translationFontScale: Float = 1.0f
) {
    val annotated = remember(html) { buildTranslationAnnotatedString(html) }
    ClickableText(
        text = annotated,
        style = TextStyle(
            fontSize = (15 * translationFontScale).sp,
            color = hInkMid,
            fontFamily = fontFamilyBody,
            lineHeight = (24 * translationFontScale).sp,
            textAlign = TextAlign.Start
        ),
        onClick = { offset ->
            annotated.getStringAnnotations("footnote", offset, offset)
                .firstOrNull()?.let { onFootnoteClick(it.item) }
        }
    )
}

private fun buildTranslationAnnotatedString(html: String): AnnotatedString {
    if (!html.contains("<sup")) {
        val plain = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
        return AnnotatedString(plain)
    }

    val matches = mutableListOf<Pair<String, String>>()
    val tokenizedHtml = footnoteSupRegex.replace(html) { match ->
        val id = match.groupValues[1]
        val digits = match.groupValues[2]
        matches.add(id to digits)
        "⟦FN:${matches.size - 1}⟧"
    }

    val plain = HtmlCompat.fromHtml(tokenizedHtml, HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
    val tokenRegex = Regex("⟦FN:(\\d+)⟧")

    return buildAnnotatedString {
        var lastIndex = 0
        tokenRegex.findAll(plain).forEach { match ->
            val start = match.range.first
            val end = match.range.last + 1
            if (start > lastIndex) {
                append(plain.substring(lastIndex, start))
            }
            val fnIndex = match.groupValues[1].toIntOrNull()
            if (fnIndex != null && fnIndex in matches.indices) {
                val (id, digits) = matches[fnIndex]
                pushStringAnnotation(tag = "footnote", annotation = id)
                withStyle(
                    SpanStyle(
                        color = hGold,
                        fontSize = 10.sp,
                        baselineShift = BaselineShift.Superscript
                    )
                ) {
                    append(digits)
                }
                pop()
            }
            lastIndex = end
        }
        if (lastIndex < plain.length) {
            append(plain.substring(lastIndex))
        }
    }
}

// ── Save to Collection Modal (web-matched) ──────────────────────────────
@Composable
fun CollectionModal(
    verse: VerseEntity,
    chapterId: Int,
    surahName: String,
    collections: List<com.nur.quran.data.db.entities.CollectionEntity>,
    collectionItems: List<com.nur.quran.data.db.entities.CollectionItemEntity>,
    onAddToCollection: (Long) -> Unit,
    onCreateCollection: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newCollectionName by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        // Backdrop
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(onClick = onDismiss)
        )
        // Card
        Surface(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(16.dp)
                .widthIn(max = 420.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = hWhite,
            shadowElevation = 24.dp
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(hGoldLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = NurIcons.BookmarkFilled,
                                contentDescription = null,
                                tint = hGold.copy(alpha = 0.6f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Save to Collection",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = hInk,
                            fontFamily = fontFamilyUi
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(hSurface)
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = NurIcons.X,
                            contentDescription = "Close",
                            tint = hInkMid,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Collections list
                Column(
                    modifier = Modifier
                        .heightIn(max = 260.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    collections.forEach { collection ->
                        val isIn = collectionItems.any {
                            it.collectionId == collection.id && it.verseKey == verse.verseKey
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isIn) hGoldLight else hSurface)
                                .border(
                                    1.dp,
                                    if (isIn) hGold else Color.Transparent,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable(enabled = !isIn) { onAddToCollection(collection.id) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isIn) hGold else hBorderColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isIn) NurIcons.CheckCircle2 else NurIcons.Bookmark,
                                    contentDescription = null,
                                    tint = if (isIn) Color.White else hInkMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = collection.name,
                                fontSize = 15.sp,
                                fontWeight = if (isIn) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (isIn) hGold else hInk
                            )
                        }
                    }
                    if (collections.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = NurIcons.Bookmark,
                                contentDescription = null,
                                tint = hInkMuted,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(text = "No collections yet", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = hInk)
                            Text(text = "Create your first one below", fontSize = 13.sp, color = hInkMuted)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(hBorderColor.copy(alpha = 0.6f))
                )
                Spacer(modifier = Modifier.height(20.dp))

                // New collection
                Text(
                    text = "NEW COLLECTION",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = hInkMuted,
                    fontFamily = fontFamilyMono,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newCollectionName,
                        onValueChange = { newCollectionName = it },
                        placeholder = { Text("Enter name...", color = hInkMuted, fontSize = 14.sp) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = hInk,
                            unfocusedTextColor = hInk,
                            focusedBorderColor = hGold,
                            unfocusedBorderColor = hBorderColor,
                            focusedContainerColor = hWhite,
                            unfocusedContainerColor = hSurface,
                            cursorColor = hGold
                        ),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (newCollectionName.isNotBlank()) hGold else hBorderColor)
                            .clickable(enabled = newCollectionName.isNotBlank()) {
                                onCreateCollection(newCollectionName.trim())
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = NurIcons.Plus,
                            contentDescription = "Create collection",
                            tint = if (newCollectionName.isNotBlank()) Color.White else hInkMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

// ── Web-style Prev/Next Surah Navigation ────────────────────────────────
@Composable
fun WebSurahNavButtons(
    chapter: ChapterEntity,
    allChapters: List<ChapterEntity>,
    onNavigateToSurah: (Int, String?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Next surah (left, ArrowLeft) — web navigates to id+1
        if (chapter.id < 114) {
            val nextName = allChapters.find { it.id == chapter.id + 1 }?.nameSimple ?: "${chapter.id + 1}"
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, hBorderColor, RoundedCornerShape(20.dp))
                    .clickable { onNavigateToSurah(chapter.id + 1, null) }
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(hSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = NurIcons.ArrowLeft,
                            contentDescription = null,
                            tint = hInkMid,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Surah $nextName",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = hInk,
                        fontFamily = fontFamilyUi,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        // Previous surah (right, ArrowRight) — web navigates to id-1
        if (chapter.id > 1) {
            val prevName = allChapters.find { it.id == chapter.id - 1 }?.nameSimple ?: "${chapter.id - 1}"
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, hBorderColor, RoundedCornerShape(20.dp))
                    .clickable { onNavigateToSurah(chapter.id - 1, null) }
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "Surah $prevName",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = hInk,
                        fontFamily = fontFamilyUi,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(hSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = NurIcons.ArrowRight,
                            contentDescription = null,
                            tint = hInkMid,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

// ── Compose-native tajweed renderer (no WebView) ────────────────────────
/** Colors tajweed segments over `text` and exposes word / tajweed-rule taps via annotations. */
@Composable
private fun SurahTajweedText(
    text: String,
    wordRanges: List<Pair<IntRange, Int>>,
    segments: List<TajweedSegment>,
    fontFamily: FontFamily,
    fontSize: TextUnit,
    lineHeight: TextUnit,
    isInteractive: Boolean = true,
    onWordClick: (Int) -> Unit,
    onTajweedClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val annotated = remember(text, segments, isDarkThemeGlobal) {
        buildAnnotatedString {
            var cursor = 0
            for (seg in segments.sortedBy { it.start }) {
                if (seg.start > cursor) append(text.substring(cursor, seg.start))
                val segStart = length
                append(text.substring(seg.start, seg.end))
                val isEndRule = seg.ruleClass == "end"
                addStyle(
                    style = SpanStyle(
                        color = Color(android.graphics.Color.parseColor(if (isEndRule) "#C6A87C" else seg.colorHex))
                    ),
                    start = segStart,
                    end = length
                )
                if (!isEndRule && seg.ruleClass != null) {
                    addStringAnnotation("TAJWEED_RULE", seg.ruleClass, segStart, length)
                }
                cursor = seg.end
            }
            if (cursor < text.length) append(text.substring(cursor))
            for ((range, wordIndex) in wordRanges) {
                addStringAnnotation("WORD_INDEX", wordIndex.toString(), range.first, range.last)
            }
        }
    }
    ClickableText(
        text = annotated,
        modifier = modifier,
        style = TextStyle(
            fontSize = fontSize,
            color = hInk,
            fontFamily = fontFamily,
            textAlign = TextAlign.Right,
            lineHeight = lineHeight
        ),
        onClick = { offset ->
            if (!isInteractive) return@ClickableText
            val rule = annotated.getStringAnnotations("TAJWEED_RULE", offset, offset).firstOrNull()
            if (rule != null) {
                onTajweedClick(rule.item)
                return@ClickableText
            }
            val word = annotated.getStringAnnotations("WORD_INDEX", offset, offset).firstOrNull()
            if (word != null) {
                val idx = word.item.toIntOrNull()
                if (idx != null) onWordClick(idx)
            }
        }
    )
}

// ── Continuous Reading View (per-page flow, web reading mode) ───────────
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ContinuousReadingPageItem(
    page: Int,
    pageVerses: List<VerseEntity>,
    wordsMap: Map<Int, List<WordEntity>>,
    tajweedMap: Map<String, String>?,
    isTajweedEnabled: Boolean,
    mushafId: String = "madani-standard",
    onWordClick: (WordEntity) -> Unit,
    onTajweedClick: (TajweedRule) -> Unit = {},
    arabicFontScale: Float = 1.0f,
    fontFamilyArabic: FontFamily = fontScheherazade,
    selectedArabicFontName: String = "Scheherazade New"
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        WebPageDivider(pageNumber = page)
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                // Build all page words as a single AnnotatedString
                val allPageWords = remember(pageVerses, wordsMap) {
                    pageVerses.flatMap { verse ->
                        wordsMap[verse.id] ?: emptyList()
                    }
                }

                val fullPageHtml = remember(pageVerses, wordsMap, tajweedMap, selectedArabicFontName) {
                    buildString {
                        pageVerses.forEachIndexed { index, verse ->
                            if (index > 0) append(" ")
                            val vHtml = tajweedMap?.get(verse.verseKey)
                            val vWords = wordsMap[verse.id] ?: emptyList()
                            append(buildCleanVerseTajweedHtml(vHtml, vWords, verse.verseNumber, selectedArabicFontName))
                        }
                    }
                }
                
                if (isTajweedEnabled && fullPageHtml.isNotBlank()) {
                    val pagePlainText = remember(allPageWords, mushafId, selectedArabicFontName) {
                        val sb = StringBuilder()
                        val ranges = mutableListOf<Pair<IntRange, Int>>()
                        val displayWords = mushafPlainWordTexts(allPageWords, mushafId, selectedArabicFontName)
                        allPageWords.forEachIndexed { wordIndex, word ->
                            if (wordIndex > 0) sb.append(" ")
                            val rawText = displayWords.getOrElse(wordIndex) { "" }
                            val start = sb.length
                            sb.append(rawText)
                            ranges.add(Pair(start..sb.length, wordIndex))
                        }
                        sb.toString() to ranges
                    }
                    val pageSegments = remember(fullPageHtml, pagePlainText.first, isDarkThemeGlobal) {
                        val textColorHex = if (isDarkThemeGlobal) "#EFECE4" else "#2B3F3C"
                        TajweedProcessor.getWordTajweedSegments(pagePlainText.first, fullPageHtml, textColorHex)
                    }

                    SurahTajweedText(
                        text = pagePlainText.first,
                        wordRanges = pagePlainText.second,
                        segments = pageSegments,
                        fontFamily = fontFamilyArabic,
                        fontSize = (26 * arabicFontScale).sp,
                        lineHeight = (52 * arabicFontScale).sp,
                        onWordClick = { idx ->
                            if (idx >= 0 && idx < allPageWords.size) {
                                onWordClick(allPageWords[idx])
                            }
                        },
                        onTajweedClick = { ruleClass ->
                            val rule = TajweedRules.RULES[ruleClass]
                            if (rule != null) {
                                onTajweedClick(rule)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    val pageAnnotated = remember(allPageWords, fontFamilyArabic, arabicFontScale, isDarkThemeGlobal, mushafId, selectedArabicFontName) {
                        buildAnnotatedString {
                            val displayWords = mushafPlainWordTexts(allPageWords, mushafId, selectedArabicFontName)
                            allPageWords.forEachIndexed { wordIndex, word ->
                                if (wordIndex > 0) append(" ")
                                val wordStart = length
                                val isEndMarker = word.charTypeName == "end"
                                val rawText = displayWords.getOrElse(wordIndex) { "" }
                                val plainText = rawText
                                val displayText = plainText
                                append(displayText)
                                if (isEndMarker) {
                                    addStyle(
                                        SpanStyle(color = hGold),
                                        wordStart, length
                                    )
                                }
                                addStringAnnotation("WORD_INDEX", wordIndex.toString(), wordStart, length)
                            }
                        }
                    }

                    ClickableText(
                        text = pageAnnotated,
                        modifier = Modifier.fillMaxWidth(),
                        style = TextStyle(
                            fontSize = (26 * arabicFontScale).sp,
                            color = hInk,
                            fontFamily = fontFamilyArabic,
                            textAlign = TextAlign.Right,
                            lineHeight = (52 * arabicFontScale).sp
                        ),
                        onClick = { offset ->
                            val wordAnn = pageAnnotated.getStringAnnotations("WORD_INDEX", offset, offset).firstOrNull()
                            if (wordAnn != null) {
                                val idx = wordAnn.item.toIntOrNull()
                                if (idx != null && idx < allPageWords.size) {
                                    onWordClick(allPageWords[idx])
                                }
                            }
                        }
                    )
                }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordTranslationTooltipDrawer(
    word: WordEntity,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = hCream,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(40.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(hBorderColor)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = word.textUthmani ?: "",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = fontFamilyArabic,
                color = hInk
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (!word.transliteration.isNullOrEmpty()) {
                Text(
                    text = word.transliteration,
                    fontSize = 14.sp,
                    fontStyle = FontStyle.Italic,
                    color = hInkMuted,
                    fontFamily = fontFamilyMono
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(1.dp)
                    .background(hBorderColor)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = word.translation ?: "Translation not available",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = hTeal,
                textAlign = TextAlign.Center,
                fontFamily = fontFamilyUi
            )
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ── Tafsir Loading Overlay ──────────────────────────────────────────────
@Composable
fun TafsirLoadingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(enabled = false) { },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = hCream,
            modifier = Modifier.padding(32.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    color = hGold,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Loading Tafsir...",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = hInkMuted,
                    fontFamily = fontFamilyUi
                )
            }
        }
    }
}

// ── Tafsir Error Overlay ────────────────────────────────────────────────
@Composable
fun TafsirErrorOverlay(
    message: String,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = hCream,
            modifier = Modifier.padding(32.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = NurIcons.Info,
                    contentDescription = null,
                    tint = hGold,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = message,
                    fontSize = 15.sp,
                    color = hInkMid,
                    fontFamily = fontFamilyUi,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = hGold),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Dismiss", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ── Tafsir Bottom Sheet (web-matched) ───────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TafsirBottomSheet(
    verseNumber: String,
    text: String,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = hWhite,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(40.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(hBorderColor)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tafsir (Ayah $verseNumber)",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = hInk,
                    fontFamily = fontFamilyUi
                )
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(hSurface)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = NurIcons.X,
                        contentDescription = "Close Tafsir",
                        tint = hInkMid,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            val cleanTafsir = remember(text) {
                HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_LEGACY).toString().trim()
            }
            Text(
                text = cleanTafsir,
                fontSize = 16.sp,
                color = hInkMid,
                fontFamily = fontFamilyBody,
                lineHeight = 29.sp,
                textAlign = TextAlign.Start
            )
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

enum class SettingsSubView {
    ROOT,
    TRANSLATION,
    ARABIC_FONT
}

@Composable
fun SettingsSelectionRow(
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontFamily = fontFamilyUi, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = hInk)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, fontFamily = fontFamilyBody, fontSize = 12.sp, color = hInkMuted)
        }
        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = hInkMuted,
            modifier = Modifier.size(16.dp)
        )
    }
}

data class TajweedRule(
    val name: String,
    val nameAr: String,
    val colorHex: String,
    val description: String
)

object TajweedRules {
    val RULES = mapOf(
        "ham_wasl" to TajweedRule(
            "Hamzat al-Wasl",
            "همزة الوصل",
            "#AAAAAA",
            "A connecting hamza that is pronounced at the beginning of speech but silent when preceded by another word. It serves to connect words smoothly in recitation."
        ),
        "laam_shamsiyah" to TajweedRule(
            "Laam Shamsiyyah",
            "لام شمسية",
            "#AAAAAA",
            "The \"Lam\" of the definite article (Al-) is silent and assimilates into the following letter, which is one of the 14 \"sun letters\" (huruf shamsiyyah)."
        ),
        "madda_normal" to TajweedRule(
            "Madda (Normal)",
            "مد طبيعي",
            "#537FFF",
            "A natural elongation of 2 counts (harakaat). It occurs with the three letters of madd: Alif, Waw, and Ya when they follow their corresponding vowels."
        ),
        "madda_permissible" to TajweedRule(
            "Madda (Permissible)",
            "مد جائز",
            "#4050FF",
            "An elongation of 2, 4, or 5 counts that occurs when a hamza comes after a madd letter. The reader may choose the length, hence \"permissible\" (Jaa'iz)."
        ),
        "madda_obligatory" to TajweedRule(
            "Madda (Obligatory)",
            "مد لازم",
            "#000FB5",
            "A mandatory elongation of 6 counts that occurs when a madd letter is followed by a shaddah or sukoon within the same word. It must always be stretched to 6 counts."
        ),
        "madda_necessary" to TajweedRule(
            "Madda (Necessary)",
            "مد واجب",
            "#2142c7",
            "A necessary elongation of 4-5 counts that occurs when a madd letter is followed by a hamza in the same word."
        ),
        "qalpiala" to TajweedRule(
            "Qalqalah",
            "قلقلة",
            "#DD0008",
            "An echoing or bouncing sound produced when pronouncing one of the five Qalqalah letters (ق ط ب ج د) with a sukoon. The sound bounces off the articulation point."
        ),
        "qalaqah" to TajweedRule(
            "Qalqalah",
            "قلقلة",
            "#DD0008",
            "An echoing or bouncing sound produced when pronouncing one of the five Qalqalah letters (ق ط ب ج د) with a sukoon. The sound bounces off the articulation point."
        ),
        "ikhfa_shafawi" to TajweedRule(
            "Ikhfa Shafawi",
            "إخفاء شفوي",
            "#D500B7",
            "Oral hiding — when a Meem Saakinah (مْ) is followed by the letter Ba (ب). The meem is pronounced with a slight nasalization while hiding its sound."
        ),
        "ikhfa" to TajweedRule(
            "Ikhfa",
            "إخفاء",
            "#26BFFD",
            "Hiding — when a Noon Saakinah or Tanween is followed by one of 15 specific letters. The noon sound is hidden with a nasal tone for 2 counts."
        ),
        "ikhafa" to TajweedRule(
            "Ikhfa",
            "إخفاء",
            "#26BFFD",
            "Hiding — when a Noon Saakinah or Tanween is followed by one of 15 specific letters. The noon sound is hidden with a nasal tone for 2 counts."
        ),
        "idghaam_shafawi" to TajweedRule(
            "Idghaam Shafawi",
            "إدغام شفوي",
            "#169777",
            "Lip merging — when a Meem Saakinah (مْ) is followed by another Meem (م). The two meems merge into one with a ghunnah (nasalization) of 2 counts."
        ),
        "idgham_shafawi" to TajweedRule(
            "Idghaam Shafawi",
            "إدغام شفوي",
            "#169777",
            "Lip merging — when a Meem Saakinah (مْ) is followed by another Meem (م). The two meems merge into one with a ghunnah (nasalization) of 2 counts."
        ),
        "idghaam_ghunnah" to TajweedRule(
            "Idghaam with Ghunnah",
            "إدغام بغنة",
            "#169200",
            "Merging with nasalization — when a Noon Saakinah or Tanween is followed by one of 4 letters (ي ن م و). The noon merges into the following letter with a ghunnah of 2 counts."
        ),
        "idgham_ghunnah" to TajweedRule(
            "Idghaam with Ghunnah",
            "إدغام بغنة",
            "#169200",
            "Merging with nasalization — when a Noon Saakinah or Tanween is followed by one of 4 letters (ي ن م و). The noon merges into the following letter with a ghunnah of 2 counts."
        ),
        "idghaam_no_ghunnah" to TajweedRule(
            "Idghaam without Ghunnah",
            "إدغام بلا غنة",
            "#169200",
            "Merging without nasalization — when a Noon Saakinah or Tanween is followed by Lam (ل) or Ra (ر). The noon merges completely without any nasal sound."
        ),
        "idgham_wo_ghunnah" to TajweedRule(
            "Idghaam without Ghunnah",
            "إدغام بلا غنة",
            "#169200",
            "Merging without nasalization — when a Noon Saakinah or Tanween is followed by Lam (ل) or Ra (ر). The noon merges completely without any nasal sound."
        ),
        "idghaam_mutajanisayn" to TajweedRule(
            "Idghaam Mutajanisayn",
            "إدغام متجانسين",
            "#A1A1A1",
            "Merging of homorganic letters — when two letters share the same articulation point but differ in characteristics. The first letter merges into the second."
        ),
        "idgham_mutajanisayn" to TajweedRule(
            "Idghaam Mutajanisayn",
            "إدغام متجانسين",
            "#A1A1A1",
            "Merging of homorganic letters — when two letters share the same articulation point but differ in characteristics. The first letter merges into the second."
        ),
        "idghaam_mutaqaribayn" to TajweedRule(
            "Idghaam Mutaqaribayn",
            "إدغام متقاربين",
            "#A1A1A1",
            "Merging of close letters — when two letters have close articulation points and similar characteristics. The first letter assimilates into the second."
        ),
        "idgham_mutaqaribayn" to TajweedRule(
            "Idghaam Mutaqaribayn",
            "إدغام متقاربين",
            "#A1A1A1",
            "Merging of close letters — when two letters have close articulation points and similar characteristics. The first letter assimilates into the second."
        ),
        "iqlab" to TajweedRule(
            "Iqlab",
            "إقلاب",
            "#26BFFD",
            "Conversion — when a Noon Saakinah or Tanween is followed by the letter Ba (ب). The noon sound converts into a Meem (م) with a ghunnah of 2 counts."
        ),
        "ghunnah" to TajweedRule(
            "Ghunnah",
            "غنة",
            "#FF7E1E",
            "A nasalization sound of 2 counts that comes from the nasal passage. It naturally accompanies the letters Noon (ن) and Meem (م), especially with shaddah."
        ),
        "silent" to TajweedRule(
            "Silent",
            "حرف ساكن",
            "#AAAAAA",
            "A letter that is written but not pronounced during recitation. It appears in the script but is skipped when reading aloud."
        ),
        "slnt" to TajweedRule(
            "Silent",
            "حرف ساكن",
            "#AAAAAA",
            "A letter that is written but not pronounced during recitation. It appears in the script but is skipped when reading aloud."
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TajweedRuleBottomSheet(
    rule: TajweedRule,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = hCream,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(40.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(hBorderColor)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(android.graphics.Color.parseColor(rule.colorHex)).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(android.graphics.Color.parseColor(rule.colorHex)))
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = rule.name,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = hInk,
                fontFamily = fontFamilyUi
            )
            Text(
                text = rule.nameAr,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = hGold,
                fontFamily = fontFamilyArabic,
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = rule.description,
                fontSize = 14.sp,
                color = hInkMid,
                textAlign = TextAlign.Center,
                fontFamily = fontFamilyUi,
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SurahTourDialog(
    onDismiss: () -> Unit
) {
    var currentStep by remember { mutableStateOf(0) }
    val steps = listOf(
        Triple(
            "Read & Listen",
            "Read the ayahs and tap the play icon on any ayah to start listening from there.",
            NurIcons.Play
        ),
        Triple(
            "Tafsir & Translations",
            "Tap the info icon on any ayah to read its Tafsir. Change translation languages in the settings drawer.",
            NurIcons.Info
        ),
        Triple(
            "Offline Access",
            "Use the Download button at the top of the page to save this Surah's audio for offline listening.",
            NurIcons.Download
        )
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    if (currentStep < steps.lastIndex) {
                        currentStep++
                    } else {
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = hGold)
            ) {
                Text(
                    text = if (currentStep < steps.lastIndex) "Next" else "Got it",
                    fontFamily = fontFamilyUi,
                    fontWeight = FontWeight.Bold,
                    color = hWhite
                )
            }
        },
        dismissButton = {
            if (currentStep > 0) {
                TextButton(onClick = { currentStep-- }) {
                    Text("Back", fontFamily = fontFamilyUi, color = hInkMid)
                }
            }
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = steps[currentStep].third,
                    contentDescription = null,
                    tint = hGold,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = steps[currentStep].first,
                    fontFamily = fontFamilyUi,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = hInk
                )
            }
        },
        text = {
            Text(
                text = steps[currentStep].second,
                fontFamily = fontFamilyBody,
                fontSize = 14.sp,
                color = hInkMid,
                lineHeight = 20.sp
            )
        },
        containerColor = hCream,
        shape = RoundedCornerShape(24.dp)
    )
}

// ── Sauka Group Completion Banner ───────────────────────────────────────
@Composable
private fun SaukaCompletionBanner(
    assignmentId: String,
    backToSauka: String,
    isCompleting: Boolean,
    onComplete: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        shape = RoundedCornerShape(20.dp),
        color = hTeal,
        border = BorderStroke(1.5.dp, hTealSoft)
    ) {
        Box(
            modifier = Modifier
                .background(Brush.verticalGradient(listOf(hTeal, hTealMid)))
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = NurIcons.CheckCircle2,
                    contentDescription = null,
                    tint = hGold,
                    modifier = Modifier.size(44.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Alhamdulillah!",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = fontFamilyUi
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "You've reached the end of your assigned reading.",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onComplete,
                        enabled = !isCompleting,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                    ) {
                        if (isCompleting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = hTeal, strokeWidth = 2.dp)
                        } else {
                            Icon(imageVector = NurIcons.Check, contentDescription = null, tint = hTeal, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Mark Complete", color = hTeal, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            val shareMessage = "Alhamdulillah! I just completed my assigned Khatmah reading on Quran Nur: https://quran-nur.appwrite.network"
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, shareMessage)
                                setType("text/plain")
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Progress"))
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.6f))
                    ) {
                        Icon(imageVector = NurIcons.Share2, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Share", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurahNavigationModal(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    allChapters: List<ChapterEntity>,
    currentChapterId: Int,
    onNavigateToSurah: (Int) -> Unit,
    onJumpToAyah: (Int, Int) -> Unit,
    onJumpToPage: (Int) -> Unit,
    fontFamilyUi: FontFamily,
    fontFamilyArabic: FontFamily
) {
    if (!isOpen) return

    var activeTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedSurahId by remember(currentChapterId) { mutableIntStateOf(currentChapterId) }
    var ayahInput by remember { mutableStateOf("") }
    var pageInput by remember { mutableStateOf("") }

    val selectedChapter = remember(selectedSurahId, allChapters) {
        allChapters.firstOrNull { it.id == selectedSurahId } ?: allChapters.firstOrNull()
    }
    val maxAyahs = selectedChapter?.versesCount ?: 286

    val filteredChapters = remember(searchQuery, allChapters) {
        if (searchQuery.isBlank()) {
            allChapters
        } else {
            val q = searchQuery.trim().lowercase()
            allChapters.filter { c ->
                c.nameSimple.lowercase().contains(q) ||
                c.nameArabic.contains(q) ||
                c.id.toString() == q
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = hWhite,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Navigation",
                        fontFamily = fontFamilyUi,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = hInk
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = hInkMuted
                        )
                    }
                }

                // Tabs: Surah, Ayah, Page
                TabRow(
                    selectedTabIndex = activeTab,
                    containerColor = Color.Transparent,
                    contentColor = hGold,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                            color = hGold
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(NurIcons.BookOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Surah", fontFamily = fontFamilyUi, fontWeight = FontWeight.SemiBold)
                            }
                        },
                        selectedContentColor = hGold,
                        unselectedContentColor = hInkMuted
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(NurIcons.Hash, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Ayah", fontFamily = fontFamilyUi, fontWeight = FontWeight.SemiBold)
                            }
                        },
                        selectedContentColor = hGold,
                        unselectedContentColor = hInkMuted
                    )
                    Tab(
                        selected = activeTab == 2,
                        onClick = { activeTab = 2 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(NurIcons.Layers3, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Page", fontFamily = fontFamilyUi, fontWeight = FontWeight.SemiBold)
                            }
                        },
                        selectedContentColor = hGold,
                        unselectedContentColor = hInkMuted
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                when (activeTab) {
                    0 -> { // Surah Tab
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = {
                                    Text(
                                        text = "Search Surah by name or number...",
                                        fontFamily = fontFamilyUi,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = hInkMuted) },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = hInkMuted)
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = hGold,
                                    unfocusedBorderColor = hBoneDark,
                                    focusedContainerColor = hBone,
                                    unfocusedContainerColor = hBone
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 320.dp)
                            ) {
                                items(filteredChapters) { chapter ->
                                    val isCurrent = chapter.id == currentChapterId
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isCurrent) hGold.copy(alpha = 0.12f) else Color.Transparent)
                                            .clickable {
                                                onDismiss()
                                                onNavigateToSurah(chapter.id)
                                            }
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isCurrent) hGold else hBoneDark)
                                            ) {
                                                Text(
                                                    text = chapter.id.toString(),
                                                    fontFamily = fontFamilyUi,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = if (isCurrent) Color.White else hInk
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = chapter.nameSimple,
                                                    fontFamily = fontFamilyUi,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 15.sp,
                                                    color = if (isCurrent) hGold else hInk
                                                )
                                                Text(
                                                    text = "${chapter.versesCount} verses",
                                                    fontFamily = fontFamilyUi,
                                                    fontSize = 12.sp,
                                                    color = hInkMuted
                                                )
                                            }
                                        }
                                        Text(
                                            text = chapter.nameArabic,
                                            fontFamily = fontFamilyArabic,
                                            fontSize = 20.sp,
                                            color = hGold
                                        )
                                    }
                                }

                                if (filteredChapters.isEmpty()) {
                                    item {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(32.dp)
                                        ) {
                                            Text(
                                                text = "No Surah found.",
                                                fontFamily = fontFamilyUi,
                                                color = hInkMuted,
                                                fontSize = 14.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    1 -> { // Ayah Tab
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp)
                        ) {
                            Text("Select Surah", fontFamily = fontFamilyUi, fontSize = 13.sp, color = hInkMuted)
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            var expandedDropdown by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = expandedDropdown,
                                onExpandedChange = { expandedDropdown = !expandedDropdown }
                            ) {
                                OutlinedTextField(
                                    value = "${selectedChapter?.id}. ${selectedChapter?.nameSimple ?: ""}",
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = hGold,
                                        unfocusedBorderColor = hBoneDark
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor()
                                )
                                ExposedDropdownMenu(
                                    expanded = expandedDropdown,
                                    onDismissRequest = { expandedDropdown = false },
                                    modifier = Modifier.heightIn(max = 240.dp)
                                ) {
                                    allChapters.forEach { chap ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text("${chap.id}. ${chap.nameSimple}", fontFamily = fontFamilyUi)
                                                    Text(chap.nameArabic, fontFamily = fontFamilyArabic, color = hGold)
                                                }
                                            },
                                            onClick = {
                                                selectedSurahId = chap.id
                                                expandedDropdown = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text("Ayah Number", fontFamily = fontFamilyUi, fontSize = 13.sp, color = hInkMuted)
                            Spacer(modifier = Modifier.height(4.dp))

                            OutlinedTextField(
                                value = ayahInput,
                                onValueChange = { ayahInput = it.filter { c -> c.isDigit() } },
                                placeholder = { Text("e.g. 255", fontFamily = fontFamilyUi) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = hGold,
                                    unfocusedBorderColor = hBoneDark
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Verses 1 - $maxAyahs",
                                fontFamily = fontFamilyUi,
                                fontSize = 12.sp,
                                color = hInkMuted
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = {
                                    val ayahNum = ayahInput.toIntOrNull()?.coerceIn(1, maxAyahs) ?: 1
                                    onDismiss()
                                    onJumpToAyah(selectedSurahId, ayahNum)
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = hGold),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Text("Go to Ayah", fontFamily = fontFamilyUi, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                    2 -> { // Page Tab
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp)
                        ) {
                            Text("Page Number", fontFamily = fontFamilyUi, fontSize = 13.sp, color = hInkMuted)
                            Spacer(modifier = Modifier.height(4.dp))

                            OutlinedTextField(
                                value = pageInput,
                                onValueChange = { pageInput = it.filter { c -> c.isDigit() } },
                                placeholder = { Text("e.g. 293", fontFamily = fontFamilyUi) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = hGold,
                                    unfocusedBorderColor = hBoneDark
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Pages 1 - 604",
                                fontFamily = fontFamilyUi,
                                fontSize = 12.sp,
                                color = hInkMuted
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = {
                                    val pageNum = pageInput.toIntOrNull()?.coerceIn(1, 604) ?: 1
                                    onDismiss()
                                    onJumpToPage(pageNum)
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = hGold),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Text("Go to Page", fontFamily = fontFamilyUi, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

