package com.nur.quran.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.data.db.entities.VerseEntity
import com.nur.quran.data.planner.PlannerEngine
import com.nur.quran.ui.components.AutoScrollerBar
import androidx.compose.foundation.gestures.scrollBy
import com.nur.quran.ui.components.NurIcons
import com.nur.quran.ui.viewmodels.PlannerViewModel
import com.nur.quran.ui.viewmodels.SurahUiState
import com.nur.quran.ui.viewmodels.SurahViewModel
import com.nur.quran.ui.viewmodels.TafsirUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.ceil
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.random.Random
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat

@Composable
fun PlannerReaderScreen(
    dayNumber: Int,
    plannerViewModel: PlannerViewModel,
    surahViewModel: SurahViewModel,
    onBack: () -> Unit
) {
    val activePlan by plannerViewModel.activePlan.collectAsState()
    val assignment = remember(activePlan, dayNumber) {
        activePlan?.assignments?.find { it.dayNumber == dayNumber }
    }

    if (assignment == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Assignment not found", color = hInkMuted)
        }
        return
    }

    val pageStart = assignment.pageStart
    val pageEnd = assignment.pageEnd

    // ── Auto-resume: start at first unread page ──────────────────────
    val resumePage = remember(activePlan, assignment) {
        activePlan?.let { PlannerEngine.getAssignmentResumePageNumber(it, assignment) } ?: pageStart
    }
    var currentPage by remember { mutableStateOf(resumePage) }

    val existingReflection = activePlan?.assignmentReflections?.get(dayNumber) ?: ""
    var reflectionNote by remember(existingReflection) { mutableStateOf(existingReflection) }
    var showCelebrationDialog by remember { mutableStateOf(false) }

    val selectedArabicFontName by surahViewModel.selectedArabicFontName.collectAsState(initial = "KFGQPC Hafs")
    val fontFamilyArabic = remember(selectedArabicFontName) {
        getArabicFontFamily(selectedArabicFontName)
    }

    val uiState by surahViewModel.uiState.collectAsState()
    val tafsirState by surahViewModel.tafsirState.collectAsState()
    val chapters by plannerViewModel.chapters.collectAsState()
    val playingVerseKey by surahViewModel.playingVerseKey.collectAsState()
    val isPlaying by surahViewModel.isPlaying.collectAsState()
    var verseToShare by remember { mutableStateOf<VerseEntity?>(null) }

    // Mushaf-aware rendering (web parity: script/tajweed follow the mushaf)
    val isTajweedEffective by surahViewModel.isTajweedEffective.collectAsState()
    val currentMushaf by surahViewModel.currentMushaf.collectAsState()
    val isTranslationEnabled by surahViewModel.isTranslationEnabled.collectAsState()
    val arabicFontScale by surahViewModel.arabicFontScale.collectAsState()
    val translationFontScale by surahViewModel.translationFontScale.collectAsState()
    val planBookmarks by plannerViewModel.plannerBookmarks.collectAsState()
    val sessionTotals by plannerViewModel.sessionTotals.collectAsState()
    val appPrefs = LocalContext.current.getSharedPreferences("PlannerSettings", Context.MODE_PRIVATE)
    val showIntentionPrompt = appPrefs.getBoolean("show_intention_prompt", true)

    // ── Focus mode (web parity: PlannerReader.jsx isFocusMode immersive
    // hide-chrome): explicit user toggle that hides the header + bottom bar.
    // Separate from the existing scroll-driven headerVisible auto-hide.
    var isFocusMode by remember { mutableStateOf(false) }
    // ── Focus mode: auto-hide header during scroll ───────────────────
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var headerVisible by remember { mutableStateOf(true) }
    var isAutoScrollActive by remember { mutableStateOf(false) }
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
                            if (lazyListState.canScrollForward) {
                                coroutineScope.launch {
                                    lazyListState.scrollBy(wholePixels.toFloat())
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

    LaunchedEffect(lazyListState.isScrollInProgress) {
        if (lazyListState.isScrollInProgress) {
            headerVisible = false
        } else {
            delay(800)
            headerVisible = true
        }
    }

    // ── Bridge: planner time → reading_sessions ─────────────────────
    // No page→chapter resolver exists in this file; best-known chapter is the
    // first loaded verse (exact current-page content), falling back to the
    // ChapterEntity page range (SurahScreen.kt:1641 pattern), else null.
    // rememberUpdatedState so DisposableEffect.onDispose reads the latest value.
    val bridgeChapterId by rememberUpdatedState(
        (uiState as? SurahUiState.Success)?.verses?.firstOrNull()?.chapterId
            ?: chapters.find { currentPage in it.pagesStart..it.pagesEnd }?.id
    )

    // ── Session timer (web parity: persisted per plan/day) ──────────
    // Display ticks locally; deltas are flushed to the store every 30s + on exit.
    // Web PlannerReader.jsx:280-345 — tick 1s while running, pause/resume toggle,
    // MM:SS display of persisted base + session.
    // Each flush is also bridged into reading_sessions via
    // logPlannerDayToSessions (checkpoint: totals reset to 0 after logging).
    // Flush-then-bridge ordering matters: stopPlannerTimer updates
    // sessionTotals synchronously, so the bridge reads the just-flushed total.
    var isTimerRunning by remember(dayNumber) { mutableStateOf(true) }
    var timerTick by remember(dayNumber) { mutableIntStateOf(0) }
    var savedTick by remember(dayNumber) { mutableIntStateOf(0) }
    var lastBridgedTick by remember(dayNumber) { mutableIntStateOf(0) }
    LaunchedEffect(dayNumber, isTimerRunning) {
        while (isTimerRunning) {
            delay(1000)
            timerTick++
            if (timerTick - savedTick >= 30) {
                plannerViewModel.stopPlannerTimer(dayNumber, (timerTick - savedTick).toLong())
                savedTick = timerTick
                if (timerTick != lastBridgedTick) {
                    plannerViewModel.logPlannerDayToSessions(dayNumber, bridgeChapterId)
                    lastBridgedTick = timerTick
                }
            }
        }
    }
    DisposableEffect(dayNumber) {
        onDispose {
            val delta = timerTick - savedTick
            if (delta > 0) plannerViewModel.stopPlannerTimer(dayNumber, delta.toLong())
            if (timerTick != lastBridgedTick) {
                plannerViewModel.logPlannerDayToSessions(dayNumber, bridgeChapterId)
            }
        }
    }
    // NB: totals already include everything flushed this session (savedTick),
    // so only the unflushed remainder is added — otherwise time counts double.
    val displayedSeconds = ((sessionTotals[dayNumber] ?: 0L).toInt() + (timerTick - savedTick).coerceAtLeast(0))

    // ── Last-read verse position (web parity: IntersectionObserver :215-268) ──
    // Tracks the top-visible verse (debounced) and auto-scrolls to it on load.
    val lastReadVerseKey = activePlan?.lastReadVerseKey
    var hasAutoScrolledToLastRead by remember(dayNumber, currentPage) { mutableStateOf(false) }

    // Fetch verses for page
    LaunchedEffect(currentPage) {
        surahViewModel.loadPageVerses(currentPage)
    }

    // Save scroll position & mark page read
    DisposableEffect(currentPage) {
        plannerViewModel.markPageRead(dayNumber, currentPage)
        onDispose {}
    }

    val isDayCompleted = activePlan?.completedDays?.contains(dayNumber) == true

    // ── Completion progress tracking ────────────────────────────────
    val totalPages = (pageEnd - pageStart + 1)
    val readPages = activePlan?.assignmentReadPages?.get(dayNumber)?.size ?: 0
    val progressPct = if (totalPages > 0) (readPages * 100 / totalPages) else 0
    // Web parity (PlannerReader.jsx:578): estimated reading time label.
    val estimatedMins = ceil(totalPages * 2.5).toInt()

    // ── Takeaway verse for celebration ──────────────────────────────
    var takeawayVerse by remember { mutableStateOf<VerseEntity?>(null) }

    // ── Swipe gesture handler ────────────────────────────────────────
    var accumulatedDrag by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(hCream)
            .pointerInput(currentPage, pageStart, pageEnd) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (accumulatedDrag > 100f && currentPage > pageStart) {
                            currentPage--
                        } else if (accumulatedDrag < -100f && currentPage < pageEnd) {
                            currentPage++
                        }
                        accumulatedDrag = 0f
                    },
                    onDragCancel = { accumulatedDrag = 0f },
                    onHorizontalDrag = { _, dragAmount ->
                        accumulatedDrag += dragAmount
                    }
                )
            }
    ) {
        // ── Header Bar (auto-hides on scroll; fully hidden in focus mode) ──
        AnimatedVisibility(
            visible = headerVisible && !isFocusMode,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
        ) {
            Surface(
                color = hCream,
                tonalElevation = 2.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, hBoneDark)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                                Icon(imageVector = NurIcons.ArrowLeft, contentDescription = "Back", tint = hInk)
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Column {
                                Text("DAY $dayNumber ASSIGNMENT", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = hGold, fontFamily = fontFamilyMono, letterSpacing = 1.sp)
                                Text(assignment.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = hInk, fontFamily = fontFamilyUi)
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Focus mode toggle (web parity: isFocusMode immersive hide-chrome)
                            IconButton(
                                onClick = { isFocusMode = !isFocusMode },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (isFocusMode) NurIcons.EyeOff else NurIcons.Eye,
                                    contentDescription = if (isFocusMode) "Exit focus mode" else "Enter focus mode",
                                    tint = if (isFocusMode) hGold else hInk
                                )
                            }

                            // Auto-scroll toggle button
                            IconButton(
                                onClick = {
                                    isAutoScrollActive = !isAutoScrollActive
                                    if (!isAutoScrollActive) isAutoScrollPaused = false
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = NurIcons.Rows3,
                                    contentDescription = "Auto-scroll",
                                    tint = if (isAutoScrollActive) hGold else hInk
                                )
                            }

                            // Audio Play/Pause Button
                            IconButton(
                                onClick = {
                                    val state = uiState
                                    val verses = (state as? SurahUiState.Success)?.verses ?: emptyList()
                                    if (verses.isNotEmpty()) {
                                        surahViewModel.playPauseChapter(verses, verses.first().chapterId)
                                    }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) NurIcons.PauseFilled else NurIcons.PlayFilled,
                                    contentDescription = if (isPlaying) "Pause Audio" else "Play Audio",
                                    tint = hTeal
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Surface(
                                    shape = RoundedCornerShape(100),
                                    color = if (isDayCompleted) hGreen.copy(alpha = 0.15f) else hGoldSoft
                                ) {
                                    Text(
                                        text = "Page $currentPage of $pageEnd",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDayCompleted) hGreen else hGold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        fontFamily = fontFamilyMono
                                    )
                                }
                                // Web parity: estimated time label + session timer with pause/resume toggle (MM:SS)
                                Text(
                                    text = "≈$estimatedMins min",
                                    fontSize = 9.sp,
                                    color = hInkMuted,
                                    fontFamily = fontFamilyMono,
                                    modifier = Modifier.padding(top = 2.dp, end = 4.dp)
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .padding(top = 2.dp, end = 4.dp)
                                        .clip(RoundedCornerShape(100))
                                        .clickable { isTimerRunning = !isTimerRunning }
                                ) {
                                    Icon(
                                        imageVector = if (isTimerRunning) NurIcons.PauseFilled else NurIcons.PlayFilled,
                                        contentDescription = if (isTimerRunning) "Pause session timer" else "Resume session timer",
                                        tint = hTeal,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = formatSessionTime(displayedSeconds),
                                        fontSize = 9.sp,
                                        color = hInkMuted,
                                        fontFamily = fontFamilyMono
                                    )
                                }
                            }
                        }
                    }

                    // Progress bar under header
                    LinearProgressIndicator(
                        progress = progressPct / 100f,
                        modifier = Modifier.fillMaxWidth().height(2.dp),
                        color = hGold,
                        trackColor = hBone
                    )
                }
            }
        }

        // Verses View Canvas
        Box(modifier = Modifier.weight(1f)) {
            when (val state = uiState) {
                is SurahUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = hGold)
                    }
                }
                is SurahUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(state.message, color = hInkMuted)
                    }
                }
                is SurahUiState.Success -> {
                    val verses = state.verses
                    val wordsMap = state.wordsMap
                    val tajweedMap = state.tajweedMap

                    // Tajweed overlays for every chapter on this page
                    LaunchedEffect(verses) {
                        verses.map { it.chapterId }.distinct().forEach { chapterId ->
                            surahViewModel.ensureTajweedForChapter(chapterId)
                        }
                        if (verses.isNotEmpty() && takeawayVerse == null) {
                            takeawayVerse = verses.random()
                        }
                    }

                    // Web parity (PlannerReader.jsx:215-268): non-verse header items
                    // (intention banner + swipe hint) shift LazyColumn indices.
                    val verseHeaderOffset =
                        (if (showIntentionPrompt) 1 else 0) + (if (currentPage == pageStart) 1 else 0)

                    // Auto-scroll to the last-read verse on initial load.
                    LaunchedEffect(verses, currentPage, lastReadVerseKey) {
                        if (hasAutoScrolledToLastRead || verses.isEmpty()) return@LaunchedEffect
                        val targetKey = lastReadVerseKey ?: run {
                            hasAutoScrolledToLastRead = true
                            return@LaunchedEffect
                        }
                        val verseIndex = verses.indexOfFirst { it.verseKey == targetKey }
                        if (verseIndex >= 0) {
                            lazyListState.scrollToItem(verseIndex + verseHeaderOffset)
                        }
                        // Mark done even when not found (web parity) to prevent looping.
                        hasAutoScrolledToLastRead = true
                    }

                    // Track the top-visible verse (debounced) as the last-read position.
                    LaunchedEffect(lazyListState.firstVisibleItemIndex, verses.size, currentPage) {
                        if (verses.isEmpty() || !hasAutoScrolledToLastRead) return@LaunchedEffect
                        delay(1000)
                        val verseIndex = lazyListState.firstVisibleItemIndex - verseHeaderOffset
                        val visibleVerse = verses.getOrNull(verseIndex) ?: return@LaunchedEffect
                        if (activePlan?.lastReadVerseKey != visibleVerse.verseKey) {
                            plannerViewModel.setLastReadPosition(currentPage, visibleVerse.verseKey)
                        }
                    }

                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(28.dp)
                    ) {
                        // Renewal of Intention Banner (web: intentionPromptEnabled)
                        if (showIntentionPrompt) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = hGoldSoft,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, hGold.copy(alpha = 0.3f)),
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(imageVector = NurIcons.Sparkles, contentDescription = null, tint = hGold, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("Pause to renew your intention for Allah's sake before reading.", fontSize = 12.sp, color = hInk, fontFamily = fontFamilyUi)
                                    }
                                }
                            }
                        }

                        // Swipe hint (shown on first page)
                        if (currentPage == pageStart) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(imageVector = NurIcons.ArrowLeft, contentDescription = null, tint = hInkMuted.copy(alpha = 0.5f), modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Swipe to turn pages", fontSize = 10.sp, color = hInkMuted.copy(alpha = 0.5f), fontFamily = fontFamilyMono)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(imageVector = NurIcons.ArrowRight, contentDescription = null, tint = hInkMuted.copy(alpha = 0.5f), modifier = Modifier.size(12.dp))
                                }
                            }
                        }

                        itemsIndexed(verses, key = { _, verse -> verse.id }) { index, verse ->
                            val prevVerse = if (index > 0) verses[index - 1] else null
                            val showPageDivider = verse.pageNumber != 0 && (prevVerse == null || prevVerse.pageNumber != verse.pageNumber)

                            Column(modifier = Modifier.fillMaxWidth()) {
                                if (showPageDivider) {
                                    PageDivider(pageNumber = verse.pageNumber)
                                }

                                if (verse.verseNumber == 1) {
                                    val chapter = chapters.find { it.id == verse.chapterId }
                                    if (chapter != null) {
                                        SurahHeader(
                                            chapter = chapter,
                                            versesStartPage = verse.pageNumber,
                                            isPlaying = isPlaying,
                                            isDownloaded = true,
                                            isDownloading = false,
                                            onPlayClick = { surahViewModel.playPauseChapter(verses, chapter.id) },
                                            onDownloadClick = {}
                                        )
                                    }
                                    if (verse.chapterId != 1 && verse.chapterId != 9) {
                                        Box(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp, horizontal = 16.dp),
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

                                VerseDivider()

                                val verseWords = wordsMap[verse.id] ?: emptyList()
                                val verseChapter = chapters.find { it.id == verse.chapterId }
                                VerseItem(
                                    verse = verse,
                                    words = verseWords,
                                    isTranslationEnabled = isTranslationEnabled,
                                    isTajweedEnabled = isTajweedEffective,
                                    mushafId = currentMushaf.id,
                                    tajweedMap = tajweedMap,
                                    isCurrentlyPlaying = playingVerseKey == verse.verseKey && isPlaying,
                                    isBookmarked = planBookmarks.any { it.verseKey == verse.verseKey },
                                    isTafsirOpen = (tafsirState as? TafsirUiState.Visible)?.verseKey == verse.verseKey,
                                    onPlayClick = { surahViewModel.playVerse(verses, verse.chapterId, verse) },
                                    onBookmarkClick = {
                                        plannerViewModel.togglePlannerBookmark(
                                            verse.verseKey,
                                            verseChapter?.nameSimple ?: "Surah ${verse.chapterId}"
                                        )
                                    },
                                    onTafsirClick = {
                                        if ((tafsirState as? TafsirUiState.Visible)?.verseKey == verse.verseKey) {
                                            surahViewModel.dismissTafsir()
                                        } else {
                                            surahViewModel.loadTafsir(verse.verseKey, verse.chapterId)
                                        }
                                    },
                                    onShareClick = { verseToShare = verse },
                                    onAddToCollection = {},
                                    onWordClick = {},
                                    onTajweedClick = {},
                                    onLoadFootnote = { id -> surahViewModel.getFootnoteText(id) },
                                    arabicFontScale = arabicFontScale,
                                    translationFontScale = translationFontScale,
                                    fontFamilyArabic = fontFamilyArabic,
                                    selectedArabicFontName = selectedArabicFontName
                                )
                            }
                        }

                        // Day Reflection Note Section
                        item {
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = hCream),
                                border = androidx.compose.foundation.BorderStroke(1.dp, hBoneDark),
                                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 20.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("DAY $dayNumber REFLECTION & NOTES", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = hGold, fontFamily = fontFamilyMono, letterSpacing = 1.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = reflectionNote,
                                        onValueChange = {
                                            reflectionNote = it
                                            plannerViewModel.saveReflection(dayNumber, it)
                                        },
                                        placeholder = { Text("Write your thoughts or reflections for today's reading...", fontSize = 13.sp, color = hInkMuted) },
                                        modifier = Modifier.fillMaxWidth(),
                                        minLines = 2
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Tafsir Bottom Sheet
            when (val tState = tafsirState) {
                is TafsirUiState.Loading -> TafsirLoadingOverlay()
                is TafsirUiState.Visible -> TafsirBottomSheet(
                    verseNumber = tState.verseKey,
                    text = tState.text,
                    onDismiss = { surahViewModel.dismissTafsir() }
                )
                is TafsirUiState.Error -> TafsirErrorOverlay(
                    message = tState.message,
                    onDismiss = { surahViewModel.dismissTafsir() }
                )
                TafsirUiState.Hidden -> {}
            }
        }

        // Per-item Done (web: Mark {currentItem.title} Done)
        val completedRangeValues = activePlan?.assignmentCompletedItems?.get(dayNumber) ?: emptyList()
        val currentItem = assignment.items.find { currentPage in it.pageStart..it.pageEnd }
            ?: assignment.items.firstOrNull()
        val dayProgressComplete = activePlan?.let { PlannerEngine.getAssignmentProgress(it, assignment).isComplete } == true
        val isCurrentItemComplete = currentItem == null || completedRangeValues.contains(currentItem.rangeValue)
        if (currentItem != null && !isCurrentItemComplete && !dayProgressComplete &&
            (assignment.unitType != "surah" || currentPage >= assignment.pageEnd)
        ) {
            Button(
                onClick = { plannerViewModel.markPlannerItemComplete(dayNumber, currentItem.rangeValue) },
                colors = ButtonDefaults.buttonColors(containerColor = hTeal),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Icon(imageVector = NurIcons.CheckCircle2, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Mark ${currentItem.title} Done", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        // Bottom Dock Bar: Navigation between pages (hidden in focus mode)
        if (!isFocusMode) {
        Surface(
            color = hWhite,
            tonalElevation = 6.dp,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, hBoneDark),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Prev Page Button
                Button(
                    onClick = { if (currentPage > pageStart) currentPage-- },
                    enabled = currentPage > pageStart,
                    colors = ButtonDefaults.buttonColors(containerColor = hCream),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(imageVector = NurIcons.ArrowLeft, contentDescription = "Prev", tint = if (currentPage > pageStart) hInk else hInkMuted, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Prev", fontSize = 12.sp, color = if (currentPage > pageStart) hInk else hInkMuted, fontWeight = FontWeight.Bold)
                }

                // Complete Assignment / Next Page Button
                if (currentPage < pageEnd) {
                    Button(
                        onClick = { currentPage++ },
                        colors = ButtonDefaults.buttonColors(containerColor = hGold),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Text("Next Page", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(imageVector = NurIcons.ArrowRight, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                } else {
                    Button(
                        onClick = {
                            plannerViewModel.toggleAssignmentCompleted(dayNumber)
                            showCelebrationDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = hGreen),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Icon(imageVector = NurIcons.CheckCircle2, contentDescription = "Done", tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Finish Day Assignment", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        }
    }

    // Floating exit-focus chip while chrome is hidden (web: toggle back out).
    if (isFocusMode) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            Surface(
                onClick = { isFocusMode = false },
                shape = RoundedCornerShape(100),
                color = hInk.copy(alpha = 0.7f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(imageVector = NurIcons.Eye, contentDescription = "Exit focus mode", tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Exit Focus", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }

    // Share Verse Dialog
    if (verseToShare != null) {
        val shareChapter = chapters.find { it.id == verseToShare!!.chapterId }
        com.nur.quran.ui.components.ShareVerseDialog(
            verse = verseToShare!!,
            chapterName = shareChapter?.nameSimple ?: "Surah ${verseToShare!!.chapterId}",
            onDismiss = { verseToShare = null }
        )
    }

    // ── Celebration Dialog with Takeaway Verse ────────────────────────
    if (showCelebrationDialog) {
        Box(modifier = Modifier.fillMaxSize()) {
            ConfettiOverlay()
        }
        AlertDialog(
            onDismissRequest = {
                showCelebrationDialog = false
                onBack()
            },
            containerColor = hCream,
            shape = RoundedCornerShape(24.dp),
            title = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🎉", fontSize = 42.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Alhamdulillah!",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = hGreen,
                        fontFamily = fontFamilyUi,
                        textAlign = TextAlign.Center
                    )
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "You have completed Day $dayNumber! May Allah bless your journey with the Quran.",
                        fontSize = 14.sp,
                        color = hInk,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Session time summary
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = hBone,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("⏱ ", fontSize = 14.sp)
                            Text(
                                "Session: ${formatSessionTime(displayedSeconds)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = hInkMid,
                                fontFamily = fontFamilyMono
                            )
                        }
                    }

                    // Takeaway of the Day verse card
                    takeawayVerse?.let { verse ->
                        Spacer(modifier = Modifier.height(16.dp))
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = hGoldSoft,
                            border = androidx.compose.foundation.BorderStroke(1.dp, hGold.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "TAKEAWAY OF THE DAY",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = hGold,
                                    fontFamily = fontFamilyMono,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                val arabicText = verse.textUthmani ?: verse.textQpcHafs ?: ""
                                if (arabicText.isNotBlank()) {
                                    Text(
                                        text = arabicText.replace("\u25cc", ""),
                                        fontSize = 20.sp,
                                        fontFamily = fontFamilyArabic,
                                        color = hInk,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 36.sp,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                if (!verse.translation.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = verse.translation,
                                        fontSize = 13.sp,
                                        color = hInkMid,
                                        fontFamily = fontFamilyBody,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "— ${verse.verseKey}",
                                    fontSize = 11.sp,
                                    color = hInkMuted,
                                    fontFamily = fontFamilyMono
                                )
                            }
                        }
                    }

                    // Reflection quick-save
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = reflectionNote,
                        onValueChange = {
                            reflectionNote = it
                            plannerViewModel.saveReflection(dayNumber, it)
                        },
                        placeholder = { Text("Daily reflection...", fontSize = 13.sp, color = hInkMuted) },
                        label = { Text("Reflection", fontSize = 11.sp, color = hGold) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCelebrationDialog = false
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = hGreen),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (reflectionNote.isNotBlank()) "Save & Return" else "Return to Planner", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Auto-scroll controls: screen-level overlay (was unreachable inside the
    // celebration dialog). Toggle via the header Rows3 button.
    if (isAutoScrollActive || isAutoScrollPaused) {
        AutoScrollerBar(
            isAutoScrollActive = isAutoScrollActive,
            isAutoScrollPaused = isAutoScrollPaused,
            autoScrollSpeed = autoScrollSpeed,
            onPauseToggle = { isAutoScrollPaused = !isAutoScrollPaused },
            onSpeedChange = { autoScrollSpeed = it },
            onJumpUp = {
                coroutineScope.launch {
                    lazyListState.scrollBy(-300f)
                }
            },
            onJumpDown = {
                coroutineScope.launch {
                    lazyListState.scrollBy(300f)
                }
            },
            onClose = {
                isAutoScrollActive = false
                isAutoScrollPaused = false
            }
        )
    }
}

// ── Helper: Format session time (web parity: MM:SS) ─────────────────────
private fun formatSessionTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "%02d:%02d".format(mins, secs)
}

@Composable
fun ConfettiOverlay() {
    val colors = listOf(
        Color(0xFFC8A951), // hGold
        Color(0xFF2D958E), // hTeal
        Color(0xFF22C55E), // hGreen
        Color.White,
        Color(0xFFEF4444)  // Red
    )

    class Particle {
        var x = -1f
        var y = -1f
        var size = Random.nextFloat() * 20f + 10f
        var color = colors.random()
        var speed = Random.nextFloat() * 10f + 5f
        var drift = Random.nextFloat() * 4f - 2f
        var rotation = Random.nextFloat() * 360f
        var rotationSpeed = Random.nextFloat() * 10f - 5f
        var initialized = false
    }

    val particles = remember { List(50) { Particle() } }
    
    val infiniteTransition = rememberInfiniteTransition(label = "confetti")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "tick"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val tick = time // force recompose
        particles.forEach { p ->
            if (!p.initialized) {
                p.x = Random.nextFloat() * size.width
                p.y = -Random.nextFloat() * size.height * 0.5f - 50f
                p.initialized = true
            }
            p.y += p.speed
            p.x += p.drift
            p.rotation += p.rotationSpeed
            
            if (p.y > size.height + 50f) {
                p.y = -50f
                p.x = Random.nextFloat() * size.width
            }
            
            withTransform({
                translate(left = p.x, top = p.y)
                rotate(degrees = p.rotation)
            }) {
                drawRect(
                    color = p.color,
                    topLeft = Offset(-p.size / 2, -p.size / 2),
                    size = Size(p.size, p.size)
                )
            }
        }
    }
}
