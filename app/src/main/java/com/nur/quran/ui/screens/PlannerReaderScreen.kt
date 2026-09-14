package com.nur.quran.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.data.db.entities.VerseEntity
import com.nur.quran.data.db.entities.WordEntity
import com.nur.quran.data.planner.PlannerEngine
import com.nur.quran.ui.components.AutoScrollerBar
import androidx.compose.foundation.gestures.scrollBy
import com.nur.quran.ui.components.NurIcons
import com.nur.quran.ui.components.SettingsDrawer
import com.nur.quran.ui.components.MushafPageView
import com.nur.quran.ui.components.audio.AudioSetupSheet
import com.nur.quran.ui.viewmodels.PackViewModel
import androidx.hilt.navigation.compose.hiltViewModel
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
    // Web parity (PlannerReader.jsx showAudioSetup modal): in-reader audio setup
    // with day-scoped playlist wiring — the sheet plays the current page's
    // verses (the day's loaded content) via playRange on confirm.
    var showReaderAudioSetup by remember { mutableStateOf(false) }
    val currentReciterId by surahViewModel.currentReciterId.collectAsState()
    val playbackSettings by surahViewModel.playbackSettings.collectAsState()
    val readerStreamOnly by surahViewModel.streamOnly.collectAsState()
    val readerScrollWhilePlaying by surahViewModel.scrollWhilePlaying.collectAsState()

    // Mushaf-aware rendering (web parity: script/tajweed follow the mushaf)
    val isTajweedEffective by surahViewModel.isTajweedEffective.collectAsState()
    val currentMushaf by surahViewModel.currentMushaf.collectAsState()
    val isTranslationEnabled by surahViewModel.isTranslationEnabled.collectAsState()
    val arabicFontScale by surahViewModel.arabicFontScale.collectAsState()
    val translationFontScale by surahViewModel.translationFontScale.collectAsState()
    val planBookmarks by plannerViewModel.plannerBookmarks.collectAsState()
    val sessionTotals by plannerViewModel.sessionTotals.collectAsState()
    val context = LocalContext.current
    val appPrefs = context.getSharedPreferences("PlannerSettings", Context.MODE_PRIVATE)
    val showIntentionPrompt = appPrefs.getBoolean("show_intention_prompt", true)

    // ── Focus mode (web parity: PlannerReader.jsx isFocusMode immersive
    // hide-chrome): explicit user toggle that hides the header + bottom bar.
    // Separate from the existing scroll-driven headerVisible auto-hide.
    var isFocusMode by remember { mutableStateOf(false) }
    var isReadingMode by remember { mutableStateOf(false) }
    var showPageJumpDialog by remember { mutableStateOf(false) }
    var showSettingsDrawer by remember { mutableStateOf(false) }
    var selectedWordForTooltip by remember { mutableStateOf<WordEntity?>(null) }
    var selectedTajweedRule by remember { mutableStateOf<TajweedRule?>(null) }
    var collectionVerse by remember { mutableStateOf<VerseEntity?>(null) }
    val collections by surahViewModel.collections.collectAsState()
    val collectionItems by surahViewModel.collectionItems.collectAsState()

    // Sync global dark theme state
    val hifdhPrefs = LocalContext.current.getSharedPreferences("hifdh_settings", Context.MODE_PRIVATE)
    LaunchedEffect(Unit) {
        isDarkThemeGlobal = hifdhPrefs.getBoolean("is_dark_theme", false)
    }

    val advanceToNextPage: () -> Unit = {
        if (currentPage < pageEnd) {
            val currentItem = assignment.items.find { currentPage in it.pageStart..it.pageEnd }
            if (currentItem != null) {
                val completedRanges = activePlan?.assignmentCompletedItems?.get(dayNumber) ?: emptyList()
                if (!completedRanges.contains(currentItem.rangeValue)) {
                    if (activePlan?.unitType == "page" || currentPage >= currentItem.pageEnd) {
                        plannerViewModel.markPlannerItemComplete(dayNumber, currentItem.rangeValue)
                    }
                }
            }
            currentPage++
        }
    }

    // Seamless Audio Autoplay across pages in assignment (web parity: PlannerReader.jsx:450-480)
    var wasAudioPlaying by remember { mutableStateOf(false) }
    var shouldAutoPlayNextPage by remember { mutableStateOf(false) }

    LaunchedEffect(isPlaying, playingVerseKey) {
        if (wasAudioPlaying && !isPlaying && playingVerseKey == null) {
            if (currentPage < pageEnd) {
                shouldAutoPlayNextPage = true
                advanceToNextPage()
            }
        }
        wasAudioPlaying = isPlaying
    }

    // ── Sticky Header HUD & Auto-Scroll ──────────────────────────────
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val isScrolled by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 40
        }
    }
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
    // Web parity (PlannerReader.jsx pagehide/visibilitychange): auto-flush the
    // timer when the app backgrounds so a killed process loses ~0s. Compose has
    // no pagehide; ON_PAUSE/ON_STOP is the equivalent lifecycle hook.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, dayNumber) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                val delta = timerTick - savedTick
                if (delta > 0) {
                    plannerViewModel.stopPlannerTimer(dayNumber, delta.toLong())
                    savedTick = timerTick
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
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
    val progressPct = remember(activePlan, assignment, readPages, totalPages) {
        if (activePlan?.unitType == "surah" && totalPages > 0) {
            (readPages * 100 / totalPages).coerceIn(0, 100)
        } else {
            val completed = activePlan?.assignmentCompletedItems?.get(dayNumber)?.size ?: 0
            val totalItems = assignment.items.size
            if (totalItems > 0) (completed * 100 / totalItems).coerceIn(0, 100)
            else if (totalPages > 0) (readPages * 100 / totalPages).coerceIn(0, 100) else 0
        }
    }
    // Web parity (PlannerReader.jsx:578): estimated reading time label.
    val estimatedMins = ceil(totalPages * 2.5).toInt()

    // ── Takeaway verse for celebration ──────────────────────────────
    var takeawayVerse by remember { mutableStateOf<VerseEntity?>(null) }

    // ── Swipe gesture handler ────────────────────────────────────────
    var accumulatedDrag by remember { mutableFloatStateOf(0f) }

    val currentChapter = remember(chapters, currentPage, activePlan, assignment) {
        if (chapters.isEmpty()) null
        else {
            val matching = chapters.filter { currentPage in it.pagesStart..it.pagesEnd }
            when {
                matching.isEmpty() -> null
                matching.size == 1 -> matching.first()
                activePlan?.unitType == "surah" -> {
                    val assignedIds = assignment.items.mapNotNull { it.rangeValue.toIntOrNull() }
                    matching.find { it.id in assignedIds } ?: matching.find { it.pagesStart == currentPage } ?: matching.last()
                }
                else -> {
                    matching.find { it.pagesStart == currentPage } ?: matching.last()
                }
            }
        }
    }
    var isIntentionDismissed by remember { mutableStateOf(false) }

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
                            advanceToNextPage()
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
        // ── Header Bar (stays sticky on scroll; fully hidden in focus mode) ──
        AnimatedVisibility(
            visible = !isFocusMode,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(hCream)
            ) {
                // Top Minimal Nav Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                            Icon(imageVector = NurIcons.ArrowLeft, contentDescription = "Back", tint = hInk)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showPageJumpDialog = true }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Day $dayNumber Reader",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = hInk,
                                fontFamily = fontFamilyUi
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = NurIcons.ChevronDown,
                                contentDescription = "Select page",
                                tint = hInkMuted,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        // 1. Auto-scroll: ChevronsDown (web Layout.jsx:254-256)
                        IconButton(
                            onClick = {
                                isAutoScrollActive = !isAutoScrollActive
                                if (!isAutoScrollActive) isAutoScrollPaused = false
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = NurIcons.ChevronsDown,
                                contentDescription = if (isAutoScrollActive) "Stop Auto-scroll" else "Auto-scroll",
                                tint = if (isAutoScrollActive) hGold else hInkMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // 2. Reading mode: BookOpen (web Layout.jsx:257-259)
                        IconButton(
                            onClick = { isReadingMode = !isReadingMode },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = NurIcons.BookOpen,
                                contentDescription = if (isReadingMode) "Translation Mode" else "Reading Mode",
                                tint = if (isReadingMode) hGold else hInkMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // 3. Audio toggle: Volume2 (web Layout.jsx:265-278)
                        Box(
                            modifier = Modifier.size(36.dp),
                            contentAlignment = Alignment.Center
                        ) {
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
                                    imageVector = NurIcons.Volume2,
                                    contentDescription = "Audio Play/Pause",
                                    tint = if (isPlaying) hTeal else hInkMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            if (isPlaying) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .align(Alignment.TopEnd)
                                        .offset(x = (-4).dp, y = 4.dp)
                                        .clip(CircleShape)
                                        .background(hGold)
                                )
                            }
                        }

                        // 4. Toggle Theme: Moon / Sun (web Layout.jsx:280-292)
                        IconButton(
                            onClick = {
                                val nextTheme = !isDarkThemeGlobal
                                isDarkThemeGlobal = nextTheme
                                context.getSharedPreferences("hifdh_settings", Context.MODE_PRIVATE)
                                    .edit()
                                    .putBoolean("is_dark_theme", nextTheme)
                                    .apply()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isDarkThemeGlobal) NurIcons.Sun else NurIcons.Moon,
                                contentDescription = "Toggle Theme",
                                tint = if (isDarkThemeGlobal) hGold else hInkMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // 5. Settings: Settings gear (web Layout.jsx:294-296)
                        IconButton(
                            onClick = { showSettingsDrawer = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = NurIcons.Settings,
                                contentDescription = "Settings",
                                tint = if (showSettingsDrawer) hGold else hInkMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Sticky Glass Planner Card (web parity: PlannerReader.jsx lines 616-671)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = hWhite,
                    border = androidx.compose.foundation.BorderStroke(1.dp, hBoneDark.copy(alpha = 0.8f)),
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = if (isScrolled) 2.dp else 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(if (isScrolled) 10.dp else 14.dp),
                        verticalArrangement = Arrangement.spacedBy(if (isScrolled) 6.dp else 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left: Page number or Assignment Title + Subtitle
                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                if (activePlan?.unitType == "page") {
                                    Text(
                                        text = "Page $currentPage / $pageEnd",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = hInk,
                                        fontFamily = fontFamilyUi,
                                        modifier = Modifier.clickable { showPageJumpDialog = true }
                                    )
                                } else {
                                    Text(
                                        text = assignment.title,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = hInk,
                                        fontFamily = fontFamilyUi,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val subPrefix = remember(assignment.subtitle) {
                                        val parts = assignment.subtitle.split(" · ")
                                        if (parts.isNotEmpty() && parts[0].isNotBlank()) "${parts[0]} • " else ""
                                    }
                                    Text(
                                        text = "${subPrefix}Page $currentPage / $pageEnd",
                                        fontSize = 11.sp,
                                        color = hInkMuted,
                                        fontFamily = fontFamilyUi,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.clickable { showPageJumpDialog = true }
                                    )
                                }
                            }

                            // Right: Surah badge (if Juz/Page or different) + Timer pill + % Achieved
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (currentChapter != null && activePlan?.unitType != "surah") {
                                    Surface(
                                        shape = RoundedCornerShape(100),
                                        color = hTeal.copy(alpha = 0.08f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, hTeal.copy(alpha = 0.2f))
                                    ) {
                                        Text(
                                            text = "${currentChapter.id}. Surah ${currentChapter.nameSimple}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = hTeal,
                                            fontFamily = fontFamilyUi,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(100),
                                    color = hTeal.copy(alpha = 0.08f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, hTeal.copy(alpha = 0.2f)),
                                    modifier = Modifier.clickable { isTimerRunning = !isTimerRunning }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isTimerRunning) NurIcons.PauseFilled else NurIcons.PlayFilled,
                                            contentDescription = if (isTimerRunning) "Pause session timer" else "Resume session timer",
                                            tint = hTeal,
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = formatSessionTime(displayedSeconds),
                                            fontSize = 11.sp,
                                            fontFamily = fontFamilyMono,
                                            fontWeight = FontWeight.Bold,
                                            color = hTeal
                                        )
                                    }
                                }

                                Text(
                                    text = "$progressPct%",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = hTeal,
                                    fontFamily = fontFamilyUi
                                )
                            }
                        }

                        // Progress bar line: 3dp teal fill matching web
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(100))
                                .background(Color.Black.copy(alpha = 0.06f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(fraction = (progressPct / 100f).coerceIn(0f, 1f))
                                    .clip(RoundedCornerShape(100))
                                    .background(hTeal)
                            )
                        }
                    }
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

                    // Filter verses on boundary pages to only assigned items
                    val plan = activePlan
                    val assignedVerses = remember(verses, plan, assignment) {
                        if (verses.isEmpty() || plan == null) verses
                        else if (plan.unitType == "surah") {
                            val assignedSurahIds = assignment.items.mapNotNull { it.rangeValue.toIntOrNull() }
                            verses.filter { it.chapterId in assignedSurahIds }
                        } else if (plan.unitType == "juz") {
                            val assignedJuzIds = assignment.items.mapNotNull { it.rangeValue.toIntOrNull() }
                            verses.filter { it.juzNumber in assignedJuzIds }
                        } else {
                            verses
                        }
                    }
                    val assignedVerseKeys = remember(assignedVerses) {
                        assignedVerses.map { it.verseKey }.toSet()
                    }

                    // Tajweed overlays for every chapter on this page
                    LaunchedEffect(assignedVerses) {
                        assignedVerses.map { it.chapterId }.distinct().forEach { chapterId ->
                            surahViewModel.ensureTajweedForChapter(chapterId)
                        }
                        if (assignedVerses.isNotEmpty() && takeawayVerse == null) {
                            takeawayVerse = assignedVerses.random()
                        }
                    }

                    // Web parity (PlannerReader.jsx:215-268): non-verse header items
                    // (intention banner + swipe hint) shift LazyColumn indices.
                    val verseHeaderOffset =
                        (if (showIntentionPrompt && !isIntentionDismissed) 1 else 0) + (if (currentPage == pageStart) 1 else 0)

                    // Web parity: scroll to top of page on page change
                    LaunchedEffect(currentPage) {
                        lazyListState.scrollToItem(0)
                    }

                    // Auto-scroll to the last-read verse on initial load.
                    LaunchedEffect(assignedVerses, currentPage, lastReadVerseKey) {
                        if (hasAutoScrolledToLastRead || assignedVerses.isEmpty()) return@LaunchedEffect
                        val targetKey = lastReadVerseKey ?: run {
                            hasAutoScrolledToLastRead = true
                            return@LaunchedEffect
                        }
                        val verseIndex = assignedVerses.indexOfFirst { it.verseKey == targetKey }
                        if (verseIndex >= 0) {
                            lazyListState.scrollToItem(verseIndex + verseHeaderOffset)
                        }
                        // Mark done even when not found (web parity) to prevent looping.
                        hasAutoScrolledToLastRead = true
                    }

                    // Track the top-visible verse (debounced) as the last-read position.
                    LaunchedEffect(lazyListState.firstVisibleItemIndex, assignedVerses.size, currentPage) {
                        if (assignedVerses.isEmpty() || !hasAutoScrolledToLastRead) return@LaunchedEffect
                        delay(1000)
                        val verseIndex = lazyListState.firstVisibleItemIndex - verseHeaderOffset
                        val visibleVerse = assignedVerses.getOrNull(verseIndex) ?: return@LaunchedEffect
                        if (activePlan?.lastReadVerseKey != visibleVerse.verseKey) {
                            plannerViewModel.setLastReadPosition(currentPage, visibleVerse.verseKey)
                        }
                    }

                    if (currentMushaf.renderMode == com.nur.quran.data.mushaf.MushafRenderMode.QCF_PAGE && !isReadingMode) {
                        LazyColumn(
                            state = lazyListState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
                        ) {
                            item {
                                MushafPageView(
                                    page = currentPage,
                                    verses = assignedVerses,
                                    wordsMap = wordsMap,
                                    tajweedMap = tajweedMap,
                                    isTajweedEnabled = isTajweedEffective,
                                    mushaf = currentMushaf,
                                    activeAudioVerseKey = playingVerseKey,
                                    assignedVerseKeys = assignedVerseKeys,
                                    onWordClick = { word -> selectedWordForTooltip = word },
                                    onTajweedClick = { selectedTajweedRule = it },
                                    arabicFontScale = arabicFontScale,
                                    fontFamilyArabic = fontFamilyArabic,
                                    selectedArabicFontName = selectedArabicFontName
                                )
                            }
                        }
                    } else if (isReadingMode) {
                        LazyColumn(
                            state = lazyListState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp)
                        ) {
                            item {
                                ContinuousReadingPageItem(
                                    page = currentPage,
                                    pageVerses = assignedVerses,
                                    wordsMap = wordsMap,
                                    tajweedMap = tajweedMap,
                                    isTajweedEnabled = isTajweedEffective,
                                    mushafId = currentMushaf.id,
                                    onWordClick = { word -> selectedWordForTooltip = word },
                                    onTajweedClick = { selectedTajweedRule = it },
                                    arabicFontScale = arabicFontScale,
                                    fontFamilyArabic = fontFamilyArabic,
                                    selectedArabicFontName = selectedArabicFontName
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            state = lazyListState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(28.dp)
                        ) {
                            // Renewal of Intention Banner (web: intentionPromptEnabled)
                            if (showIntentionPrompt && !isIntentionDismissed) {
                                item {
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = hTeal.copy(alpha = 0.06f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, hTeal.copy(alpha = 0.18f)),
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(imageVector = NurIcons.Sparkles, contentDescription = null, tint = hTeal, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = "Pause to renew your intention for Allah's sake before reading.",
                                                fontSize = 12.sp,
                                                color = hInk,
                                                fontFamily = fontFamilyUi,
                                                modifier = Modifier.weight(1f)
                                            )
                                            IconButton(
                                                onClick = { isIntentionDismissed = true },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = NurIcons.X,
                                                    contentDescription = "Dismiss",
                                                    tint = hInkMuted,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
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

                            itemsIndexed(assignedVerses, key = { _, verse -> verse.id }) { index, verse ->
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    if (verse.verseNumber == 1) {
                                        val chapter = chapters.find { it.id == verse.chapterId }
                                        if (chapter != null) {
                                            PlannerMinimalHeader(
                                                overline = "Surah ${chapter.id}",
                                                title = chapter.nameSimple,
                                                pillPrimary = chapter.translatedName.ifBlank { null },
                                                pillSecondary = "${chapter.versesCount} Ayahs"
                                            )
                                        }
                                        if (verse.chapterId != 1 && verse.chapterId != 9) {
                                            Text(
                                                text = "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ",
                                                fontSize = 28.sp,
                                                fontFamily = fontFamilyArabic,
                                                color = hGold,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 10.dp, bottom = 20.dp)
                                            )
                                        }
                                    }

                                    if (index > 0 && verse.verseNumber != 1) {
                                        VerseDivider()
                                    }

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
                                        onPlayClick = { surahViewModel.playVerse(assignedVerses, verse.chapterId, verse) },
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
                                        onAddToCollection = { collectionVerse = verse },
                                        onWordClick = { word -> selectedWordForTooltip = word },
                                        onTajweedClick = { selectedTajweedRule = it },
                                        onLoadFootnote = { id -> surahViewModel.getFootnoteText(id) },
                                        arabicFontScale = arabicFontScale,
                                        translationFontScale = translationFontScale,
                                        fontFamilyArabic = fontFamilyArabic,
                                        selectedArabicFontName = selectedArabicFontName,
                                        showShareAction = false
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

        // Bottom Dock Bar: Navigation between pages (web parity: PlannerReader.jsx lines 832-869)
        if (!isFocusMode) {
            Surface(
                color = hWhite,
                tonalElevation = 4.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, hBoneDark.copy(alpha = 0.7f)),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // Per-item Done (web: Mark {currentItem.title} Done)
                    val completedRangeValues = activePlan?.assignmentCompletedItems?.get(dayNumber) ?: emptyList()
                    val currentItem = assignment.items.find { currentPage in it.pageStart..it.pageEnd }
                        ?: assignment.items.firstOrNull()
                    val dayProgressComplete = activePlan?.let { PlannerEngine.getAssignmentProgress(it, assignment).isComplete } == true
                    val isCurrentItemComplete = currentItem == null || completedRangeValues.contains(currentItem.rangeValue)
                    if (currentItem != null && !isCurrentItemComplete && !dayProgressComplete &&
                        (assignment.unitType != "surah" || currentPage >= assignment.pageEnd)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(100),
                            color = hTeal,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                                .clickable {
                                    plannerViewModel.markPlannerItemComplete(dayNumber, currentItem.rangeValue)
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = NurIcons.CheckCircle2, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Mark ${currentItem.title} Done", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Prev Page Button
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (currentPage > pageStart) hBone.copy(alpha = 0.6f) else hBone.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, hBoneDark.copy(alpha = 0.5f)),
                            modifier = Modifier.clickable(enabled = currentPage > pageStart) {
                                currentPage--
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = NurIcons.ChevronLeft,
                                    contentDescription = "Prev",
                                    tint = if (currentPage > pageStart) hInk else hInkMuted.copy(alpha = 0.4f),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text("PREV", fontSize = 9.sp, color = hInkMuted, fontFamily = fontFamilyMono, letterSpacing = 0.5.sp)
                                    Text("Page ${if (currentPage > pageStart) currentPage - 1 else pageStart}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (currentPage > pageStart) hInk else hInkMuted.copy(alpha = 0.4f))
                                }
                            }
                        }

                        // Center: page progress indicator
                        Text(
                            text = "$currentPage / $pageEnd",
                            fontSize = 13.sp,
                            fontFamily = fontFamilyMono,
                            fontWeight = FontWeight.Bold,
                            color = hInkMuted,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { showPageJumpDialog = true }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )

                        // Next / Complete Assignment Button
                        if (currentPage < pageEnd) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = hTeal,
                                modifier = Modifier.clickable { advanceToNextPage() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("NEXT", fontSize = 9.sp, color = Color.White.copy(alpha = 0.8f), fontFamily = fontFamilyMono, letterSpacing = 0.5.sp)
                                        Text("Page ${currentPage + 1}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = NurIcons.ChevronRight,
                                        contentDescription = "Next",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = hGreen,
                                modifier = Modifier.clickable {
                                    plannerViewModel.markAssignmentCompleted(dayNumber)
                                    showCelebrationDialog = true
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(imageVector = NurIcons.CheckCircle2, contentDescription = "Finish", tint = Color.White, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Finish Day", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
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

    // ── Celebration Dialog with Takeaway Verse (web parity: PlannerReader.jsx lines 677-718) ──
    if (showCelebrationDialog) {
        Box(modifier = Modifier.fillMaxSize()) {
            ConfettiOverlay()
            Dialog(
                onDismissRequest = {
                    showCelebrationDialog = false
                    onBack()
                }
            ) {
                DayCelebrationCard(
                    dayNumber = dayNumber,
                    takeawayVerse = takeawayVerse,
                    reflectionNote = reflectionNote,
                    displayedSeconds = displayedSeconds,
                    onReflectionChange = {
                        reflectionNote = it
                        plannerViewModel.saveReflection(dayNumber, it)
                    },
                    onSaveAndReturn = {
                        showCelebrationDialog = false
                        if (reflectionNote.isNotBlank()) {
                            plannerViewModel.saveReflection(dayNumber, reflectionNote)
                        }
                        onBack()
                    }
                )
            }
        }
    }

    // ── In-reader AudioSetupSheet (web parity: AudioSetupModal, day-scoped) ──
    // Plays the currently loaded page verses as the day's playlist slice.
    val readerVerses = (uiState as? SurahUiState.Success)?.verses ?: emptyList()
    if (showReaderAudioSetup && readerVerses.isNotEmpty()) {
        val firstVerse = readerVerses.first()
        AudioSetupSheet(
            chapterId = firstVerse.chapterId,
            versesCount = readerVerses.size,
            initialReciterId = currentReciterId,
            initialStartAyah = 1,
            initialEndAyah = readerVerses.size,
            initialAyahRepeat = playbackSettings.ayahRepeat,
            initialRangeRepeat = playbackSettings.rangeRepeat,
            initialDelayMs = playbackSettings.delayMs,
            initialSpeed = playbackSettings.speed,
            initialStreamOnly = readerStreamOnly,
            initialScrollWhilePlaying = readerScrollWhilePlaying,
            onScrollWhilePlayingChange = surahViewModel::setScrollWhilePlaying,
            onDismiss = { showReaderAudioSetup = false },
            onPlayRange = { reciterId, startKey, endKey, ayahRepeat, rangeRepeat, delayMs, speed, streamOnly ->
                if (playingVerseKey != null) {
                    surahViewModel.applyInPlaySettings(
                        reciterId, ayahRepeat, rangeRepeat, delayMs, speed, streamOnly,
                        startKey, endKey
                    )
                } else {
                    surahViewModel.setReciterId(reciterId)
                    surahViewModel.setAyahRepeat(ayahRepeat)
                    surahViewModel.setRangeRepeat(rangeRepeat)
                    surahViewModel.setDelayMs(delayMs)
                    surahViewModel.setSpeed(speed)
                    surahViewModel.setStreamOnly(streamOnly)
                    surahViewModel.playRange(readerVerses, firstVerse.chapterId, startKey, endKey)
                }
                showReaderAudioSetup = false
            },
            onPlayAll = { reciterId, ayahRepeat, rangeRepeat, delayMs, speed, streamOnly ->
                if (playingVerseKey != null) {
                    surahViewModel.applyInPlaySettings(
                        reciterId, ayahRepeat, rangeRepeat, delayMs, speed, streamOnly
                    )
                } else {
                    surahViewModel.setReciterId(reciterId)
                    surahViewModel.setAyahRepeat(ayahRepeat)
                    surahViewModel.setRangeRepeat(rangeRepeat)
                    surahViewModel.setDelayMs(delayMs)
                    surahViewModel.setSpeed(speed)
                    surahViewModel.setStreamOnly(streamOnly)
                    surahViewModel.playRange(
                        readerVerses, firstVerse.chapterId,
                        readerVerses.first().verseKey, readerVerses.last().verseKey
                    )
                }
                showReaderAudioSetup = false
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

    // Page Jump Dialog
    if (showPageJumpDialog) {
        PageJumpDialog(
            currentPage = currentPage,
            pageStart = pageStart,
            pageEnd = pageEnd,
            onPageSelected = { selectedPage ->
                currentPage = selectedPage
            },
            onDismiss = { showPageJumpDialog = false }
        )
    }

    // Word Translation Tooltip Drawer
    selectedWordForTooltip?.let { word ->
        WordTranslationTooltipDrawer(
            word = word,
            onDismiss = { selectedWordForTooltip = null },
            onDownloadWordPack = { surahViewModel.downloadChapterWords(currentChapter?.id ?: 1) }
        )
    }

    // Tajweed Rule Tooltip Bottom Sheet
    selectedTajweedRule?.let { rule ->
        TajweedRuleBottomSheet(
            rule = rule,
            onDismiss = { selectedTajweedRule = null }
        )
    }

    // Add To Collection Modal
    collectionVerse?.let { verse ->
        CollectionModal(
            verse = verse,
            chapterId = verse.chapterId,
            surahName = chapters.find { it.id == verse.chapterId }?.nameSimple ?: "Surah ${verse.chapterId}",
            collections = collections,
            collectionItems = collectionItems,
            onAddToCollection = { collectionId ->
                surahViewModel.addToCollection(
                    collectionId,
                    verse.verseKey,
                    verse.chapterId,
                    chapters.find { it.id == verse.chapterId }?.nameSimple ?: "Surah ${verse.chapterId}"
                )
                collectionVerse = null
            },
            onCreateCollection = { name ->
                surahViewModel.addCollection(name) { newId ->
                    surahViewModel.addToCollection(
                        newId,
                        verse.verseKey,
                        verse.chapterId,
                        chapters.find { it.id == verse.chapterId }?.nameSimple ?: "Surah ${verse.chapterId}"
                    )
                }
                collectionVerse = null
            },
            onDismiss = { collectionVerse = null }
        )
    }

    // Reader Settings Drawer (web parity: SettingsDrawer - fonts, translations, mushaf)
    if (showSettingsDrawer) {
        val packVm: PackViewModel = hiltViewModel()
        val tafsirPackRows by packVm.tafsirPacks.collectAsState()
        val wordCached by packVm.wordCachedCount.collectAsState()
        val wordDownloading by packVm.wordIsDownloading.collectAsState()
        val wordProgress by packVm.wordProgressByTafsir.collectAsState()
        val syncState by packVm.syncUiState.collectAsState()
        SettingsDrawer(
            viewModel = surahViewModel,
            onDismiss = { showSettingsDrawer = false },
            tafsirPacks = tafsirPackRows,
            onDownloadTafsir = packVm::downloadTafsirPack,
            onCancelTafsir = packVm::cancelTafsirPack,
            onDeleteTafsir = packVm::deleteTafsirPack,
            translationPacks = packVm.translationPacks.collectAsState().value,
            onDownloadTranslation = packVm::downloadTranslationPack,
            onCancelTranslation = packVm::cancelTranslationPack,
            onDeleteTranslation = packVm::deleteTranslationPack,
            wordCachedCount = wordCached,
            wordIsDownloading = wordDownloading,
            onDownloadAllWords = packVm::downloadAllMissingWordPacks,
            onCancelAllWords = packVm::cancelAllWordPacks,
            completedEvents = packVm.completedEvents,
            syncState = syncState,
            onBackup = packVm::backupNow,
            onRestore = packVm::restoreNow,
            wordProgressByTafsir = wordProgress
        )
    }
}

// ── Helper: Format session time (web parity: MM:SS) ─────────────────────
private fun formatSessionTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "%02d:%02d".format(mins, secs)
}

/**
 * Quick jump dialog allowing user to jump directly to any page in the assignment.
 */
@Composable
fun PageJumpDialog(
    currentPage: Int,
    pageStart: Int,
    pageEnd: Int,
    onPageSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var targetPage by remember { mutableIntStateOf(currentPage) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = hCream,
            border = BorderStroke(1.5.dp, hBoneDark),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Jump to Page",
                    fontFamily = fontFamilyUi,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = hInk
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Assignment range: $pageStart – $pageEnd",
                    fontFamily = fontFamilyMono,
                    fontSize = 12.sp,
                    color = hInkMuted
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = { if (targetPage > pageStart) targetPage-- },
                        enabled = targetPage > pageStart
                    ) {
                        Icon(NurIcons.ChevronLeft, contentDescription = "Decrease", tint = if (targetPage > pageStart) hInk else hInkMuted.copy(alpha = 0.4f))
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = hWhite,
                        border = BorderStroke(1.dp, hBoneDark),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = "$targetPage",
                            fontFamily = fontFamilyMono,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = hTeal,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                        )
                    }

                    IconButton(
                        onClick = { if (targetPage < pageEnd) targetPage++ },
                        enabled = targetPage < pageEnd
                    ) {
                        Icon(NurIcons.ChevronRight, contentDescription = "Increase", tint = if (targetPage < pageEnd) hInk else hInkMuted.copy(alpha = 0.4f))
                    }
                }

                if (pageEnd > pageStart) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Slider(
                        value = targetPage.toFloat(),
                        onValueChange = { targetPage = it.toInt() },
                        valueRange = pageStart.toFloat()..pageEnd.toFloat(),
                        steps = (pageEnd - pageStart - 1).coerceAtLeast(0),
                        colors = SliderDefaults.colors(
                            thumbColor = hTeal,
                            activeTrackColor = hTeal,
                            inactiveTrackColor = hBoneDark
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = hWhite),
                        border = BorderStroke(1.dp, hBoneDark),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", fontFamily = fontFamilyUi, color = hInkMuted)
                    }
                    Button(
                        onClick = {
                            onPageSelected(targetPage)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = hTeal),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Jump", fontFamily = fontFamilyUi, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
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

// ── Web Parity: PlannerMinimalHeader ────────────────────────────────────
@Composable
fun PlannerMinimalHeader(
    overline: String?,
    title: String,
    pillPrimary: String?,
    pillSecondary: String?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!overline.isNullOrBlank()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(32.dp)
                        .height(1.dp)
                        .background(hBoneDark)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = overline.uppercase(),
                    fontSize = 11.sp,
                    fontFamily = fontFamilyMono,
                    fontWeight = FontWeight.SemiBold,
                    color = hInkMuted,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .width(32.dp)
                        .height(1.dp)
                        .background(hBoneDark)
                )
            }
        }

        Text(
            text = title,
            fontSize = 32.sp,
            fontFamily = fontFamilyUi,
            fontWeight = FontWeight.Bold,
            color = hInk,
            textAlign = TextAlign.Center
        )

        if (!pillPrimary.isNullOrBlank() || !pillSecondary.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = hWhite,
                border = BorderStroke(1.dp, hBoneDark),
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!pillPrimary.isNullOrBlank()) {
                        Text(
                            text = pillPrimary,
                            fontSize = 13.sp,
                            fontFamily = fontFamilyUi,
                            fontWeight = FontWeight.Medium,
                            color = hInk
                        )
                    }
                    if (!pillPrimary.isNullOrBlank() && !pillSecondary.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .background(hTeal, CircleShape)
                        )
                    }
                    if (!pillSecondary.isNullOrBlank()) {
                        Text(
                            text = pillSecondary,
                            fontSize = 12.sp,
                            fontFamily = fontFamilyUi,
                            color = hInkMuted
                        )
                    }
                }
            }
        }
    }
}

// ── Web Parity: DayCelebrationCard ───────────────────────────────────────
@Composable
fun DayCelebrationCard(
    dayNumber: Int,
    takeawayVerse: VerseEntity?,
    reflectionNote: String,
    displayedSeconds: Int,
    onReflectionChange: (String) -> Unit,
    onSaveAndReturn: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = hTeal,
        shadowElevation = 8.dp
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
            // Background watermark icon
            Icon(
                imageVector = NurIcons.CheckCircle2,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.12f),
                modifier = Modifier
                    .size(130.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 24.dp, y = (-20).dp)
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Alhamdulillah! 🎉",
                    fontSize = 24.sp,
                    fontFamily = fontFamilyUi,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "You have completed Day $dayNumber.",
                    fontSize = 15.sp,
                    fontFamily = fontFamilyUi,
                    color = Color.White.copy(alpha = 0.9f)
                )

                if (displayedSeconds > 0) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Session Time: ${formatSessionTime(displayedSeconds)}",
                        fontSize = 12.sp,
                        fontFamily = fontFamilyMono,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                }

                if (takeawayVerse != null) {
                    Spacer(modifier = Modifier.height(18.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = Color.Black.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "TAKEAWAY OF THE DAY",
                                fontSize = 10.sp,
                                fontFamily = fontFamilyUi,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            val arabicText = takeawayVerse.textUthmani ?: takeawayVerse.textQpcHafs ?: ""
                            if (arabicText.isNotBlank()) {
                                Text(
                                    text = arabicText,
                                    fontSize = 20.sp,
                                    fontFamily = fontScheherazade,
                                    color = Color.White,
                                    textAlign = TextAlign.Right,
                                    lineHeight = 36.sp,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            val cleanTranslation = takeawayVerse.translation
                                ?.replace(Regex("<sup[^>]*>.*?</sup>"), "")
                                ?.replace(Regex("<[^>]+>"), "")
                                ?.trim()

                            if (!cleanTranslation.isNullOrBlank()) {
                                Text(
                                    text = "\"$cleanTranslation\"",
                                    fontSize = 13.sp,
                                    fontFamily = fontFamilyBody,
                                    color = Color.White.copy(alpha = 0.9f),
                                    lineHeight = 20.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                            }

                            Text(
                                text = "— Surah ${takeawayVerse.verseKey.replace(":", ", Ayah ")}",
                                fontSize = 11.sp,
                                fontFamily = fontFamilyUi,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.65f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Daily Reflection (Optional)",
                        fontSize = 13.sp,
                        fontFamily = fontFamilyUi,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.95f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = reflectionNote,
                        onValueChange = onReflectionChange,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White.copy(alpha = 0.12f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.08f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color.White,
                            focusedBorderColor = Color.White.copy(alpha = 0.5f),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                        ),
                        placeholder = {
                            Text(
                                text = "Write a brief reflection for today's reading...",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.45f)
                            )
                        },
                        minLines = 3,
                        maxLines = 5
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onSaveAndReturn,
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = hTeal
                    ),
                    modifier = Modifier.height(44.dp)
                ) {
                    Text(
                        text = if (reflectionNote.isNotBlank()) "Save & Return" else "Return to Planner",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = fontFamilyUi
                    )
                }
            }
        }
    }
}

