package com.nur.quran.ui.screens

import android.content.Context
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.data.db.entities.ChapterEntity
import com.nur.quran.data.db.entities.VerseEntity
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.scrollBy
import androidx.hilt.navigation.compose.hiltViewModel
import com.nur.quran.ui.components.AutoScrollerBar
import com.nur.quran.ui.components.NurIcons
import com.nur.quran.ui.components.SettingsDrawer
import com.nur.quran.ui.viewmodels.PackViewModel
import com.nur.quran.ui.viewmodels.SurahUiState
import com.nur.quran.ui.viewmodels.SurahViewModel
import com.nur.quran.utils.TajweedProcessor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HifdhReaderScreen(
    surahViewModel: SurahViewModel,
    chapterId: Int,
    onBackClick: () -> Unit
) {
    val uiState by surahViewModel.uiState.collectAsState()
    val memorizedAyahs by surahViewModel.memorizedAyahs.collectAsState()
    val isPlaying by surahViewModel.isPlaying.collectAsState()
    val playingVerseKey by surahViewModel.playingVerseKey.collectAsState()
    val collections by surahViewModel.collections.collectAsState()
    val bookmarkedVerses by surahViewModel.bookmarkedVerses.collectAsState()
    val arabicFontScale by surahViewModel.arabicFontScale.collectAsState()
    val isTajweedEnabled by surahViewModel.isTajweedEnabled.collectAsState()
    val selectedArabicFontName by surahViewModel.selectedArabicFontName.collectAsState(initial = "KFGQPC Hafs")

    val fontFamilyArabic = remember(selectedArabicFontName) {
        getArabicFontFamily(selectedArabicFontName)
    }

    var currentVerseIndex by remember { mutableStateOf(0) }
    var ayahsPerChunk by remember { mutableStateOf(1) } // 1, 3, 5, -1 (Page)
    var hideMode by remember { mutableStateOf("visible") } // "visible", "blur", "word", "firstletter"
    var showTranslation by remember { mutableStateOf(false) }
    var revealedWords by remember { mutableStateOf(setOf<String>()) }
    var ayahRepeatCount by remember { mutableStateOf(1) }
    var delayBetweenAyahs by remember { mutableStateOf(0) }
    var rangeLoopCount by remember { mutableStateOf(1) } // 1, 2, 3, 5, 10, -1 (infinite)
    var showAudioSettingsPopover by remember { mutableStateOf(false) }
    var showCollectionsPopover by remember { mutableStateOf(false) }
    var showSettingsDrawer by remember { mutableStateOf(false) }
    var newCollectionName by remember { mutableStateOf("") }
    var sessionSeconds by remember { mutableStateOf(0) }
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var isAutoScrollActive by remember { mutableStateOf(false) }
    var isAutoScrollPaused by remember { mutableStateOf(false) }
    var autoScrollSpeed by remember { mutableIntStateOf(3) }
    val speedMap = remember { mapOf(1 to 12f, 2 to 24f, 3 to 45f, 4 to 90f, 5 to 150f, 6 to 270f, 7 to 450f) }

    // ── Auto-hide UI (web: Memorization.jsx showUI on 3s inactivity) ──
    var showUI by remember { mutableStateOf(true) }
    var activityTick by remember { mutableIntStateOf(1) }
    val fadeAlpha by animateFloatAsState(
        targetValue = if (showUI) 1f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "uiFade"
    )
    LaunchedEffect(activityTick) {
        if (activityTick > 0) {
            showUI = true
            delay(3000)
            showUI = false
        }
    }

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

    val context = LocalContext.current

    LaunchedEffect(chapterId) {
        surahViewModel.loadChapterDetails(chapterId)
    }

    DisposableEffect(chapterId) {
        surahViewModel.startReadingSession(chapterId, "memorizing")
        onDispose {
            surahViewModel.endReadingSession("memorizing")
            surahViewModel.stopPlaying()
        }
    }

    // Session Timer
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            sessionSeconds++
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(hWhite)) {
        // ── 1. Top Thin Progress Bar ──
        when (val state = uiState) {
            is SurahUiState.Success -> {
                val total = state.verses.size
                val progressEnd = if (ayahsPerChunk == -1) {
                    currentVerseIndex + currentVersesOf(state.verses, currentVerseIndex, -1).size
                } else {
                    currentVerseIndex + ayahsPerChunk
                }
                val sessionPct = if (total > 0) (progressEnd.toFloat() / total).coerceIn(0f, 1f) else 0f
                LinearProgressIndicator(
                    progress = sessionPct,
                    modifier = Modifier.fillMaxWidth().height(3.dp),
                    color = hTeal,
                    trackColor = hBoneDark
                )
            }
            else -> {}
        }

        // ── 2. Web-Identical Top Navbar Header (Matching Header.jsx) ──
        val chapterName = (uiState as? SurahUiState.Success)?.chapter?.nameSimple ?: "Surah $chapterId"
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            color = hCream,
            shadowElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Back Button + Brand Logo & Title Breadcrumb
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBackClick, modifier = Modifier.size(36.dp)) {
                        Icon(imageVector = NurIcons.ArrowLeft, contentDescription = "Back", tint = hInk)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = com.nur.quran.R.drawable.ic_logo),
                        contentDescription = "Quran Nur Logo",
                        modifier = Modifier.size(28.dp).clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Quran Nur",
                        fontFamily = fontFamilyUi,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = hInk
                    )
                    Text(
                        text = " / ",
                        fontSize = 12.sp,
                        color = hInkMuted
                    )
                    Text(
                        text = "Hifdh Mode: $chapterName",
                        fontFamily = fontFamilyMono,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = hTeal
                    )
                }

                // Right Actions (Award Icon + Theme Toggle + Settings)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    val versesList = (uiState as? SurahUiState.Success)?.verses ?: emptyList()
                    val isSurahFullyMemorized = versesList.isNotEmpty() && versesList.all { memorizedAyahs.contains(it.verseKey) }

                    IconButton(
                        onClick = {
                            versesList.forEach { v -> surahViewModel.toggleMemorizedAyah(v.verseKey) }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = NurIcons.Award,
                            contentDescription = "Mark Surah Memorized",
                            tint = if (isSurahFullyMemorized) hGreen else hInkMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            isAutoScrollActive = !isAutoScrollActive
                            if (!isAutoScrollActive) isAutoScrollPaused = false
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = NurIcons.Rows3,
                            contentDescription = "Auto-scroll",
                            tint = if (isAutoScrollActive) hGold else hInkMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            isDarkThemeGlobal = !isDarkThemeGlobal
                            context.getSharedPreferences("Settings", Context.MODE_PRIVATE)
                                .edit().putBoolean("is_dark_theme", isDarkThemeGlobal).apply()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isDarkThemeGlobal) NurIcons.Sun else NurIcons.Moon,
                            contentDescription = "Theme",
                            tint = hInkMid,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { showSettingsDrawer = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = NurIcons.Settings,
                            contentDescription = "Settings",
                            tint = hInkMid,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }


        when (val state = uiState) {
            is SurahUiState.Loading -> {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = hGold)
                }
            }
            is SurahUiState.Error -> {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(text = state.message, color = hInkMuted)
                }
            }
            is SurahUiState.Success -> {
                val versesList = state.verses
                val chapterObj = state.chapter
                val tajweedMap = state.tajweedMap
                val currentVerses = remember(currentVerseIndex, ayahsPerChunk, versesList) {
                    currentVersesOf(versesList, currentVerseIndex, ayahsPerChunk)
                }
                val step = if (ayahsPerChunk == -1) currentVerses.size else ayahsPerChunk
                val canNext = currentVerseIndex + step < versesList.size

                val goNext: () -> Unit = {
                    if (canNext) {
                        currentVerseIndex += step
                    }
                    revealedWords = emptySet()
                }
                val goPrev: () -> Unit = {
                    if (currentVerseIndex > 0) {
                        if (ayahsPerChunk == -1) {
                            val prevPage = versesList[currentVerseIndex - 1].pageNumber
                            var newIndex = currentVerseIndex - 1
                            while (newIndex > 0 && versesList[newIndex - 1].pageNumber == prevPage) newIndex--
                            currentVerseIndex = newIndex
                        } else {
                            currentVerseIndex = (currentVerseIndex - ayahsPerChunk).coerceAtLeast(0)
                        }
                    }
                    revealedWords = emptySet()
                }
                val latestGoNext by rememberUpdatedState(goNext)
                val latestGoPrev by rememberUpdatedState(goPrev)
                var totalDragX by remember { mutableFloatStateOf(0f) }

                // Web: Memorization.jsx effect on currentVerseIndex — keep chunk playback following
                LaunchedEffect(currentVerses, ayahRepeatCount, delayBetweenAyahs, rangeLoopCount) {
                    if (isPlaying) {
                        surahViewModel.followHifdhChunk(
                            currentVerses, chapterId, ayahRepeatCount, delayBetweenAyahs, rangeLoopCount
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    if (totalDragX < -50.dp.toPx()) latestGoNext()
                                    else if (totalDragX > 50.dp.toPx()) latestGoPrev()
                                    totalDragX = 0f
                                },
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()
                                    totalDragX += dragAmount
                                }
                            )
                        }
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    awaitPointerEvent(PointerEventPass.Main)
                                    activityTick++
                                }
                            }
                        }
                ) {
                    // ── 3. Distraction-Free Verses Canvas (Matching Web Memorization.jsx) ──
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 32.dp),
                        verticalArrangement = if (currentVerses.size == 1) Arrangement.Center else Arrangement.spacedBy(32.dp)
                    ) {
                        itemsIndexed(currentVerses, key = { _, v -> v.verseKey }) { index, verse ->
                            val verseKey = verse.verseKey
                            val isMemorized = memorizedAyahs.contains(verseKey)
                            val isBookmarked = bookmarkedVerses.contains(verseKey)
                            val rawText = verse.textUthmani ?: verse.textQpcHafs ?: verse.textIndopak ?: ""
                            val cleanedText = rawText.replace("\u25cc", "")
                            val verseTajweedHtml = if (isTajweedEnabled) tajweedMap[verseKey] else null
                            val isActiveAudio = playingVerseKey == verseKey
                            val textColor = if (isActiveAudio) hTeal
                            else if (isDarkThemeGlobal) Color(0xFFEFECE4) else Color(0xFF2B3F3C)

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 64.dp)
                                ) {
                                    // Left Action Overlay (Bookmark & CheckCircle)
                                    Column(
                                        modifier = Modifier.align(Alignment.TopStart),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        IconButton(
                                            onClick = { surahViewModel.toggleBookmark(verseKey, chapterId, chapterObj.nameSimple) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = NurIcons.Bookmark,
                                                contentDescription = "Bookmark",
                                                tint = if (isBookmarked) hTeal else hInkMuted.copy(alpha = 0.5f),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = { surahViewModel.toggleMemorizedAyah(verseKey) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isMemorized) NurIcons.CheckCircle2 else NurIcons.Check,
                                                contentDescription = "Mark Memorized",
                                                tint = if (isMemorized) hGreen else hInkMuted.copy(alpha = 0.5f),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    // Centered Arabic Verse Text
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 40.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        // Verse Key Badge
                                        Surface(shape = RoundedCornerShape(100), color = hGoldSoft) {
                                            Text(
                                                text = verseKey,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = hGold,
                                                fontFamily = fontFamilyMono,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(16.dp))

                                        when (hideMode) {
                                            "blur" -> {
                                                var tempRevealed by remember { mutableStateOf(false) }
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable { tempRevealed = !tempRevealed }
                                                ) {
                                                    if (verseTajweedHtml != null) {
                                                        HifdhTajweedText(
                                                            text = cleanedText,
                                                            tajweedHtml = verseTajweedHtml,
                                                            defaultColorHex = if (isDarkThemeGlobal) "#EFECE4" else "#2B3F3C",
                                                            fontFamily = fontFamilyArabic,
                                                            fontSize = (28 * arabicFontScale).sp,
                                                            lineHeight = 48.sp,
                                                            baseColor = textColor,
                                                            textAlign = TextAlign.Center,
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .blur(if (tempRevealed) 0.dp else 12.dp)
                                                        )
                                                    } else {
                                                        Text(
                                                            text = cleanedText,
                                                            fontFamily = fontFamilyArabic,
                                                            fontSize = (28 * arabicFontScale).sp,
                                                            color = textColor,
                                                            textAlign = TextAlign.Center,
                                                            lineHeight = 48.sp,
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .blur(if (tempRevealed) 0.dp else 12.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            "word" -> {
                                                val words = remember(verseKey, cleanedText, verseTajweedHtml) {
                                                    if (verseTajweedHtml != null) TajweedProcessor.splitTajweedHtmlIntoWords(verseTajweedHtml)
                                                    else cleanedText.split(" ")
                                                }
                                                FlowRow(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.Center
                                                ) {
                                                    words.forEachIndexed { wordIdx, rawWord ->
                                                        val cleanWord = rawWord.replace(tajweedTagRegex, "")
                                                        val wordKey = "$verseKey-$wordIdx"
                                                        val isWordRevealed = revealedWords.contains(wordKey)

                                                        Box(
                                                            modifier = Modifier
                                                                .padding(4.dp)
                                                                .clip(RoundedCornerShape(6.dp))
                                                                .background(if (isWordRevealed) Color.Transparent else hBoneDark)
                                                                .clickable {
                                                                    revealedWords = if (isWordRevealed) revealedWords - wordKey else revealedWords + wordKey
                                                                }
                                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                                        ) {
                                                            if (isWordRevealed) {
                                                                if (verseTajweedHtml != null) {
                                                                    HifdhTajweedText(
                                                                        text = cleanWord,
                                                                        tajweedHtml = rawWord,
                                                                        defaultColorHex = if (isDarkThemeGlobal) "#EFECE4" else "#2B3F3C",
                                                                        fontFamily = fontFamilyArabic,
                                                                        fontSize = (26 * arabicFontScale).sp,
                                                                        lineHeight = TextUnit.Unspecified,
                                                                        baseColor = hInk,
                                                                        textAlign = TextAlign.Center
                                                                    )
                                                                } else {
                                                                    Text(
                                                                        text = cleanWord,
                                                                        fontFamily = fontFamilyArabic,
                                                                        fontSize = (26 * arabicFontScale).sp,
                                                                        color = hInk
                                                                    )
                                                                }
                                                            } else {
                                                                Text(
                                                                    text = "▇".repeat(cleanWord.length.coerceAtLeast(3)),
                                                                    fontFamily = fontFamilyArabic,
                                                                    fontSize = (26 * arabicFontScale).sp,
                                                                    color = hInkMuted
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            "firstletter" -> {
                                                val words = remember(verseKey, cleanedText, verseTajweedHtml) {
                                                    if (verseTajweedHtml != null) TajweedProcessor.splitTajweedHtmlIntoWords(verseTajweedHtml)
                                                    else cleanedText.split(" ")
                                                }
                                                FlowRow(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.Center
                                                ) {
                                                    words.forEachIndexed { wordIdx, rawWord ->
                                                        val cleanWord = rawWord.replace(tajweedTagRegex, "")
                                                        val wordKey = "$verseKey-$wordIdx"
                                                        val isWordRevealed = revealedWords.contains(wordKey)
                                                        val firstChar = if (cleanWord.isNotEmpty()) getHifdhFirstLetter(cleanWord) + "⸱".repeat((cleanWord.length / 4).coerceAtLeast(1)) else ""

                                                        Box(
                                                            modifier = Modifier
                                                                .padding(4.dp)
                                                                .clip(RoundedCornerShape(6.dp))
                                                                .background(if (isWordRevealed) Color.Transparent else hGoldSoft)
                                                                .clickable {
                                                                    revealedWords = if (isWordRevealed) revealedWords - wordKey else revealedWords + wordKey
                                                                }
                                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                                        ) {
                                                            if (isWordRevealed) {
                                                                if (verseTajweedHtml != null) {
                                                                    HifdhTajweedText(
                                                                        text = cleanWord,
                                                                        tajweedHtml = rawWord,
                                                                        defaultColorHex = if (isDarkThemeGlobal) "#EFECE4" else "#2B3F3C",
                                                                        fontFamily = fontFamilyArabic,
                                                                        fontSize = (26 * arabicFontScale).sp,
                                                                        lineHeight = TextUnit.Unspecified,
                                                                        baseColor = hInk,
                                                                        textAlign = TextAlign.Center
                                                                    )
                                                                } else {
                                                                    Text(
                                                                        text = cleanWord,
                                                                        fontFamily = fontFamilyArabic,
                                                                        fontSize = (26 * arabicFontScale).sp,
                                                                        color = hInk
                                                                    )
                                                                }
                                                            } else {
                                                                Text(
                                                                    text = firstChar,
                                                                    fontFamily = fontFamilyArabic,
                                                                    fontSize = (26 * arabicFontScale).sp,
                                                                    color = hGold
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            else -> {
                                                if (verseTajweedHtml != null) {
                                                    HifdhTajweedText(
                                                        text = cleanedText,
                                                        tajweedHtml = verseTajweedHtml,
                                                        defaultColorHex = if (isDarkThemeGlobal) "#EFECE4" else "#2B3F3C",
                                                        fontFamily = fontFamilyArabic,
                                                        fontSize = (28 * arabicFontScale).sp,
                                                        lineHeight = 48.sp,
                                                        baseColor = textColor,
                                                        textAlign = TextAlign.Center,
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                } else {
                                                    Text(
                                                        text = cleanedText,
                                                        fontFamily = fontFamilyArabic,
                                                        fontSize = (28 * arabicFontScale).sp,
                                                        color = textColor,
                                                        textAlign = TextAlign.Center,
                                                        lineHeight = 48.sp,
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Translation Card underneath verse (if enabled)
                                if (showTranslation && !verse.translation.isNullOrEmpty()) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Card(
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = hCream),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, hBoneDark),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = verse.translation,
                                            fontSize = 14.sp,
                                            color = hInkMid,
                                            fontFamily = fontFamilyBody,
                                            textAlign = TextAlign.Center,
                                            lineHeight = 22.sp,
                                            modifier = Modifier.padding(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        item { Spacer(modifier = Modifier.height(140.dp)) }
                    }

                    // ── 4. Floating Popovers for Collections & Audio Settings (auto-hidden with UI) ──
                    if (showUI && showCollectionsPopover) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 90.dp)
                                .width(260.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = hWhite,
                            shadowElevation = 12.dp,
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, hBoneDark)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Add to Collection", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = hInk)
                                    IconButton(onClick = { showCollectionsPopover = false }, modifier = Modifier.size(24.dp)) {
                                        Icon(imageVector = NurIcons.X, contentDescription = "Close", tint = hInkMuted, modifier = Modifier.size(14.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                if (collections.isEmpty()) {
                                    Text("No collections yet.", fontSize = 11.sp, color = hInkMuted)
                                } else {
                                    LazyColumn(modifier = Modifier.heightIn(max = 140.dp)) {
                                        items(collections) { col ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        // Web: adds every verse of the visible chunk at once
                                                        currentVerses.forEach { v ->
                                                            surahViewModel.addToCollection(col.id.toLong(), v.verseKey, chapterId, chapterObj.nameSimple)
                                                        }
                                                        showCollectionsPopover = false
                                                    }
                                                    .padding(vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(imageVector = NurIcons.Bookmark, contentDescription = null, tint = hGold, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(col.name, fontSize = 12.sp, color = hInk)
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(
                                        value = newCollectionName,
                                        onValueChange = { newCollectionName = it },
                                        placeholder = { Text("New...", fontSize = 11.sp) },
                                        modifier = Modifier.weight(1f).height(40.dp),
                                        singleLine = true
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    IconButton(
                                        onClick = {
                                            if (newCollectionName.isNotBlank()) {
                                                surahViewModel.addCollection(newCollectionName.trim()) { newId ->
                                                    currentVerses.forEach { v ->
                                                        surahViewModel.addToCollection(newId, v.verseKey, chapterId, chapterObj.nameSimple)
                                                    }
                                                }
                                                newCollectionName = ""
                                                showCollectionsPopover = false
                                            }
                                        },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(hTeal)
                                    ) {
                                        Text("+", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    if (showUI && showAudioSettingsPopover) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 90.dp)
                                .width(240.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = hWhite,
                            shadowElevation = 12.dp,
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, hBoneDark)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Audio Repeat Options", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = hInk)
                                    IconButton(onClick = { showAudioSettingsPopover = false }, modifier = Modifier.size(24.dp)) {
                                        Icon(imageVector = NurIcons.X, contentDescription = "Close", tint = hInkMuted, modifier = Modifier.size(14.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text("Ayah Repeat Count", fontSize = 11.sp, color = hInkMuted)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf(1, 2, 3, 5, 10, -1).forEach { r ->
                                        FilterChip(
                                            selected = ayahRepeatCount == r,
                                            onClick = { ayahRepeatCount = r },
                                            label = { Text(if (r == -1) "∞" else "${r}x", fontSize = 10.sp) }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text("Reciter Delay Gap", fontSize = 11.sp, color = hInkMuted)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf(0, 1, 2, 3, 5).forEach { d ->
                                        FilterChip(
                                            selected = delayBetweenAyahs == d,
                                            onClick = {
                                                delayBetweenAyahs = d
                                            },
                                            label = { Text("${d}s", fontSize = 10.sp) }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text("Range Loop Count", fontSize = 11.sp, color = hInkMuted)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf(1, 2, 3, 5, 10, -1).forEach { r ->
                                        FilterChip(
                                            selected = rangeLoopCount == r,
                                            onClick = { rangeLoopCount = r },
                                            label = { Text(if (r == -1) "∞" else "${r}x", fontSize = 10.sp) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ── 5. Status Timer Line & Dock Bar Stack (auto-hides with UI) ──
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .graphicsLayer { alpha = fadeAlpha },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Web pointer-events-none: while hidden, swallow dock-area taps (they still
                        // reach the parent activity listener, which restores the UI).
                        if (!showUI) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(Unit) {
                                        awaitPointerEventScope {
                                            while (true) {
                                                awaitPointerEvent(PointerEventPass.Main).changes.forEach { it.consume() }
                                            }
                                        }
                                    }
                            )
                        }

                        // Status Line (`Surah Al-Mulk · Page 562 · Ayah 67:1 · 02:45`)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            val surahName = chapterObj.nameSimple
                            val pageNum = currentVerses.firstOrNull()?.pageNumber ?: 1
                            val firstV = currentVerses.firstOrNull()
                            val lastV = currentVerses.lastOrNull()
                            val ayahLabel = if (firstV == null) ""
                            else {
                                val firstNum = firstV.verseKey.substringAfter(':')
                                val lastNum = lastV?.takeIf { it.verseKey != firstV.verseKey }?.verseKey?.substringAfter(':')
                                if (lastNum != null) "$firstNum–$lastNum" else firstNum
                            }
                            val mins = sessionSeconds / 60
                            val secs = sessionSeconds % 60
                            val timeStr = "%d:%02d".format(mins, secs)

                            Text(text = "Surah $surahName", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = hTeal, fontFamily = fontFamilyMono)
                            Text(text = "·", fontSize = 11.sp, color = hInkMuted.copy(alpha = 0.5f))
                            Text(text = "Page $pageNum", fontSize = 11.sp, color = hInkMuted, fontFamily = fontFamilyMono)
                            Text(text = "·", fontSize = 11.sp, color = hInkMuted.copy(alpha = 0.5f))
                            Text(text = "Ayah $ayahLabel", fontSize = 11.sp, color = hInkMuted, fontFamily = fontFamilyMono)
                            Text(text = "·", fontSize = 11.sp, color = hInkMuted.copy(alpha = 0.5f))
                            Text(text = timeStr, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = hGold, fontFamily = fontFamilyMono)
                        }

                        // Floating Dock Bar (`max-w-[480px]`)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth(0.95f)
                                .widthIn(max = 480.dp),
                            shape = RoundedCornerShape(20.dp),
                            color = hWhite,
                            shadowElevation = 8.dp,
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, hBoneDark)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 1. Prev Chunk Button (<ChevronLeft>)
                                IconButton(
                                    onClick = goPrev,
                                    enabled = currentVerseIndex > 0,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(imageVector = NurIcons.ArrowLeft, contentDescription = "Prev", tint = if (currentVerseIndex > 0) hInkMid else hInkMuted.copy(alpha = 0.3f))
                                }

                                // 2. Hide Mode Cycle Button (<EyeOff> / <Eye>)
                                IconButton(
                                    onClick = {
                                        hideMode = when (hideMode) {
                                            "visible" -> "blur"
                                            "blur" -> "word"
                                            "word" -> "firstletter"
                                            else -> "visible"
                                        }
                                        revealedWords = emptySet()
                                    },
                                    enabled = true,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (hideMode != "visible") hGoldSoft else Color.Transparent)
                                ) {
                                    Icon(
                                        imageVector = if (hideMode == "visible") NurIcons.Eye else NurIcons.EyeOff,
                                        contentDescription = "Hide Mode",
                                        tint = if (hideMode != "visible") hGold else hInkMid
                                    )
                                }

                                // 3. Translation Toggle Button (<Languages> / <BookOpen>)
                                IconButton(
                                    onClick = { showTranslation = !showTranslation },
                                    enabled = true,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (showTranslation) hGoldSoft else Color.Transparent)
                                ) {
                                    Icon(imageVector = NurIcons.BookOpen, contentDescription = "Translation", tint = if (showTranslation) hGold else hInkMid)
                                }

                                // Vertical Divider Line
                                Box(modifier = Modifier.height(22.dp).width(1.dp).background(hBoneDark))

                                // 4. Center Audio Play/Pause Circular Button (54.dp) — hifdh chunk loop
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(if (isPlaying) hGold else hTeal)
                                        .clickable {
                                            surahViewModel.playPauseHifdhChunk(
                                                currentVerses, chapterId, ayahRepeatCount, delayBetweenAyahs, rangeLoopCount
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) NurIcons.PauseFilled else NurIcons.PlayFilled,
                                        contentDescription = "Play/Pause",
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                // Vertical Divider Line
                                Box(modifier = Modifier.height(22.dp).width(1.dp).background(hBoneDark))

                                // 5. Ayah Chunking Selector (1, 3, 5 Ayahs / Page)
                                TextButton(
                                    onClick = {
                                        ayahsPerChunk = when (ayahsPerChunk) {
                                            1 -> 3
                                            3 -> 5
                                            5 -> -1
                                            else -> 1
                                        }
                                        revealedWords = emptySet()
                                    },
                                    enabled = true,
                                    modifier = Modifier.height(38.dp)
                                ) {
                                    Text(
                                        if (ayahsPerChunk == -1) "Page" else "${ayahsPerChunk}A",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (ayahsPerChunk == -1) hTeal else hInkMid,
                                        fontFamily = fontFamilyMono
                                    )
                                }

                                // 6. Add to Collection Button (<Bookmark>) — adds whole chunk
                                IconButton(
                                    onClick = { showCollectionsPopover = !showCollectionsPopover },
                                    enabled = true,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(imageVector = NurIcons.Bookmark, contentDescription = "Add to Collection", tint = hInkMid, modifier = Modifier.size(18.dp))
                                }

                                // 7. Audio Settings Gear (<Settings>) — highlighted when non-default
                                val hasNonDefaultAudio = ayahRepeatCount != 1 || delayBetweenAyahs != 0 || rangeLoopCount != 1
                                IconButton(
                                    onClick = { showAudioSettingsPopover = !showAudioSettingsPopover },
                                    enabled = true,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (hasNonDefaultAudio) hGoldSoft else Color.Transparent)
                                ) {
                                    Icon(
                                        imageVector = NurIcons.Settings,
                                        contentDescription = "Audio Settings",
                                        tint = if (hasNonDefaultAudio) hGold else hInkMid,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // 8. Next Chunk Button (<ChevronRight>)
                                IconButton(
                                    onClick = goNext,
                                    enabled = canNext,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(imageVector = NurIcons.ArrowRight, contentDescription = "Next", tint = if (canNext) hInkMid else hInkMuted.copy(alpha = 0.3f))
                                }
                            }
                        }
                    }

                    // Overlay: AutoScrollerBar (inside Box to avoid stealing Column space)
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
                    translationPacks = packVm.translationPacks.collectAsState().value, onDownloadTranslation = packVm::downloadTranslationPack, onCancelTranslation = packVm::cancelTranslationPack, onDeleteTranslation = packVm::deleteTranslationPack,
                            wordCachedCount = wordCached,
                            wordIsDownloading = wordDownloading,
                            onDownloadAllWords = packVm::downloadAllMissingWordPacks,
                    onCancelAllWords = packVm::cancelAllWordPacks,
                    completedEvents = packVm.completedEvents,
                    syncState = syncState, onBackup = packVm::backupNow, onRestore = packVm::restoreNow,
                            wordProgressByTafsir = wordProgress
                        )
                    }
                }
            }
        }
    }
}

private val tajweedTagRegex = Regex("<[^>]+>")

private fun currentVersesOf(
    versesList: List<VerseEntity>,
    currentVerseIndex: Int,
    ayahsPerChunk: Int
): List<VerseEntity> {
    if (versesList.isEmpty()) return emptyList()
    if (ayahsPerChunk == -1) {
        // Page view (web: Memorization.jsx ayahsPerSwipe === -1) — whole mushaf page
        val page = versesList[currentVerseIndex].pageNumber
        var start = currentVerseIndex
        while (start > 0 && versesList[start - 1].pageNumber == page) start--
        var end = start
        while (end < versesList.size && versesList[end].pageNumber == page) end++
        return versesList.subList(start, end)
    }
    return versesList.subList(
        currentVerseIndex,
        (currentVerseIndex + ayahsPerChunk).coerceAtMost(versesList.size)
    )
}

private fun getHifdhFirstLetter(word: String): String {
    for (ch in word) {
        if (ch in '\u0621'..'\u064A' || ch in '\u0671'..'\u06D3') return ch.toString()
    }
    return word.take(1)
}

/** Tajweed-colored Arabic text for the hifdh reader. */
@Composable
private fun HifdhTajweedText(
    text: String,
    tajweedHtml: String,
    defaultColorHex: String,
    fontFamily: FontFamily,
    fontSize: TextUnit,
    baseColor: Color,
    textAlign: TextAlign,
    modifier: Modifier = Modifier,
    lineHeight: TextUnit = TextUnit.Unspecified
) {
    val segments = remember(text, tajweedHtml, defaultColorHex) {
        TajweedProcessor.getWordTajweedSegments(text, tajweedHtml, defaultColorHex)
    }
    val annotated = remember(text, segments) {
        buildAnnotatedString {
            var cursor = 0
            for (seg in segments.sortedBy { it.start }) {
                if (seg.start > cursor) append(text.substring(cursor, seg.start))
                val segStart = length
                append(text.substring(seg.start, seg.end))
                addStyle(
                    style = SpanStyle(color = Color(android.graphics.Color.parseColor(seg.colorHex))),
                    start = segStart,
                    end = length
                )
                cursor = seg.end
            }
            if (cursor < text.length) append(text.substring(cursor))
        }
    }
    Text(
        text = annotated,
        fontFamily = fontFamily,
        fontSize = fontSize,
        lineHeight = lineHeight,
        color = baseColor,
        textAlign = textAlign,
        modifier = modifier
    )
}
