package com.nur.quran.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import kotlin.math.abs
import androidx.hilt.navigation.compose.hiltViewModel
import com.nur.quran.data.getHizbByPage
import com.nur.quran.data.getJuzByPage
import com.nur.quran.ui.components.NurIcons
import com.nur.quran.ui.components.audio.MiniPlayer
import com.nur.quran.ui.viewmodels.PageUiState
import com.nur.quran.ui.viewmodels.PageViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageScreen(
    viewModel: PageViewModel = hiltViewModel(),
    pageNumber: Int,
    onBackClick: () -> Unit,
    onNavigateToPage: (Int) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val isTajweedEffective by viewModel.isTajweedEffective.collectAsState()
    val currentMushaf by viewModel.currentMushaf.collectAsState()
    val isTranslationEnabled by viewModel.isTranslationEnabled.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val playingVerseKey by viewModel.playingVerseKey.collectAsState()
    val playbackProgress by viewModel.playbackProgress.collectAsState()
    val bookmarkedVerses by viewModel.bookmarkedVerses.collectAsState()
    val arabicFontScale by viewModel.arabicFontScale.collectAsState()
    val translationFontScale by viewModel.translationFontScale.collectAsState()
    val selectedArabicFontName by viewModel.selectedArabicFontName.collectAsState()

    val fontFamilyArabic = remember(selectedArabicFontName) {
        getArabicFontFamily(selectedArabicFontName)
    }

    var playerHiddenByUser by rememberSaveable { mutableStateOf(false) }
    var hadPlayback by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(playingVerseKey) {
        val hasPlayback = playingVerseKey != null
        if (hasPlayback && !hadPlayback) playerHiddenByUser = false
        hadPlayback = hasPlayback
    }

    LaunchedEffect(pageNumber) {
        viewModel.loadPage(pageNumber)
    }

    DisposableEffect(pageNumber) {
        val sessionStartMs = System.currentTimeMillis()
        onDispose {
            val durationSec = ((System.currentTimeMillis() - sessionStartMs) / 1000).toInt()
            viewModel.logReadingSession(durationSec, "reading", pageNumber)
        }
    }

    val swipeThreshold = with(LocalDensity.current) { 60.dp.toPx() }

    // Holds the last loaded pane (verses or error) so the flip animation can
    // cross-slide two real panes instead of flickering through the transient
    // Loading state between pages.
    var flipPane by remember { mutableStateOf<PageUiState?>(null) }
    SideEffect {
        when (val s = uiState) {
            is PageUiState.Success -> flipPane = s
            is PageUiState.Error -> flipPane = s
            else -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(hWhite)
            .pointerInput(pageNumber) {
                // Web parity (Page.jsx useSwipeable): swipe right → next page,
                // swipe left → previous page. Observed in the Initial pass with
                // an axis lock so the verse list's vertical scroll cannot
                // consume the gesture and cancel detection mid-swipe.
                awaitEachGesture {
                    awaitFirstDown(pass = PointerEventPass.Initial)
                    val slop = viewConfiguration.touchSlop
                    var totalX = 0f
                    var totalY = 0f
                    var horizontalWins: Boolean? = null
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull() ?: continue
                        val delta = change.positionChange()
                        totalX += delta.x
                        totalY += delta.y
                        if (horizontalWins == null && (abs(totalX) > slop || abs(totalY) > slop)) {
                            horizontalWins = abs(totalX) > abs(totalY)
                        }
                        if (horizontalWins == true) {
                            // Claimed horizontally: the list below must not scroll.
                            change.consume()
                        }
                        if (!change.pressed) break
                    }
                    if (horizontalWins == true) {
                        when {
                            totalX > swipeThreshold && pageNumber < 604 -> onNavigateToPage(pageNumber + 1)
                            totalX < -swipeThreshold && pageNumber > 1 -> onNavigateToPage(pageNumber - 1)
                        }
                    }
                }
            }
    ) {
        // Web page-flip (Page.jsx pageVariants, spring stiffness 300 / damping
        // 30 ≈ 0.87 ratio): the whole pane (header + content) slides and
        // crossfades on page change — next page enters from the LEFT, previous
        // from the RIGHT. The bottom navigator flips with its pane.
        AnimatedContent(
            targetState = flipPane,
            transitionSpec = {
                val from = initialState as? PageUiState.Success
                val to = targetState as? PageUiState.Success
                val toNext = from != null && to != null && to.pageNumber > from.pageNumber
                // First mount (web AnimatePresence initial={false}): no flip.
                if (initialState == null) {
                    fadeIn(snap()) togetherWith fadeOut(snap())
                } else {
                    val slide = spring<IntOffset>(dampingRatio = 0.87f, stiffness = 300f)
                    val fade = spring<Float>(dampingRatio = 0.87f, stiffness = 300f)
                    (slideInHorizontally(slide) { full -> if (toNext) -full else full } +
                        fadeIn(fade)) togetherWith
                        (slideOutHorizontally(slide) { full -> if (toNext) full else -full } +
                            fadeOut(fade))
                }
            },
            label = "pageFlip"
        ) { pane ->
            // Pane-scoped page number: the sliding-out pane keeps showing its
            // own page while it animates (header flips with the content).
            val panePage = (pane as? PageUiState.Success)?.pageNumber ?: pageNumber
            Column(modifier = Modifier.fillMaxSize()) {
            // ─── Custom Header ───
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(hCream)
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = NurIcons.ArrowLeft,
                        contentDescription = "Back",
                        tint = hInk,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "Page $panePage",
                        fontFamily = fontFamilyUi,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = hInk
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val juz = getJuzByPage(panePage)
                        val hizb = getHizbByPage(panePage)
                        Text(
                            text = "Juz ${juz.id} · Hizb ${hizb.id}",
                            fontFamily = fontFamilyUi,
                            fontSize = 12.sp,
                            color = hInkMuted
                        )
                    }
                }
            }

            // ─── Main Content ───
            Box(modifier = Modifier.weight(1f)) {
                when (val state = pane) {
                    null, PageUiState.Loading -> {
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
                                text = "Loading Page $pageNumber...",
                                fontFamily = fontFamilyUi,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = hInkMuted
                            )
                        }
                    }
                    is PageUiState.Error -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = state.message,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(16.dp)
                            )
                            Button(
                                onClick = { viewModel.loadPage(pageNumber) },
                                colors = ButtonDefaults.buttonColors(containerColor = hGold)
                            ) {
                                Text("Retry", color = hWhite)
                            }
                        }
                    }
                    is PageUiState.Success -> {
                        val verses = state.verses
                        val chapterMap = state.chapterMap

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 100.dp)
                        ) {
                            itemsIndexed(verses, key = { _, v -> v.id }) { index, verse ->
                                val chapter = chapterMap[verse.chapterId]
                                val isPlayingVerse = isPlaying && playingVerseKey == verse.verseKey

                                // Surah header divider for new surahs on the page
                                if (verse.verseNumber == 1 && chapter != null) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        // Surah name pill
                                        Surface(
                                            shape = RoundedCornerShape(20.dp),
                                            color = hGoldSoft
                                        ) {
                                            Text(
                                                text = chapter.nameSimple,
                                                fontFamily = fontFamilyUi,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = hGold,
                                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                                            )
                                        }
                                        if (chapter.id != 1 && chapter.id != 9) {
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text(
                                                text = "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ",
                                                fontFamily = fontFamilyArabic,
                                                fontSize = (24 * arabicFontScale).sp,
                                                color = hTeal,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(horizontal = 16.dp)
                                            )
                                        }
                                    }
                                }

                                VerseItem(
                                    verse = verse,
                                    words = state.wordsMap[verse.id] ?: emptyList(),
                                    tajweedMap = state.tajweedMap,
                                    isTranslationEnabled = isTranslationEnabled,
                                    isTajweedEnabled = isTajweedEffective,
                                    mushafId = currentMushaf.id,
                                    isCurrentlyPlaying = isPlayingVerse,
                                    isBookmarked = verse.verseKey in bookmarkedVerses,
                                    isTafsirOpen = false,
                                    onPlayClick = { viewModel.startPlayback(verses, index) },
                                    onBookmarkClick = {
                                        // Bookmark remembers the page reader context,
                                        // so the Home bookmark card reopens this page.
                                        viewModel.toggleBookmark(
                                            verse.verseKey,
                                            verse.chapterId,
                                            chapter?.nameSimple ?: "",
                                            sourceMode = "page",
                                            sourcePage = pageNumber
                                        )
                                    },
                                    onTafsirClick = { /* Tafsir not implemented in page view */ },
                                    onShareClick = { /* Share not implemented in page view */ },
                                    onAddToCollection = { /* Collections not implemented in page view */ },
                                    onWordClick = { /* Word tap not implemented in page view */ },
                                    onLoadFootnote = { viewModel.getFootnote(it) },
                                    arabicFontScale = arabicFontScale,
                                    translationFontScale = translationFontScale,
                                    fontFamilyArabic = fontFamilyArabic,
                                    selectedArabicFontName = selectedArabicFontName
                                )
                            }

                            // ─── Bottom Page Navigator ───
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp, horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Web page navigator (Page.jsx): NEXT is on the LEFT,
                                    // PREVIOUS on the right — RTL mushaf page order.
                                    // Next (left)
                                    Text(
                                        text = "← Next",
                                        fontFamily = fontFamilyUi,
                                        fontWeight = FontWeight.Bold,
                                        color = if (state.pageNumber < 604) hTeal else hInkMuted,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable(enabled = state.pageNumber < 604) {
                                                onNavigateToPage(state.pageNumber + 1)
                                            }
                                            .padding(8.dp)
                                    )

                                    // Center
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "PAGE",
                                            fontFamily = fontFamilyMono,
                                            fontSize = 10.sp,
                                            color = hInkMuted,
                                            letterSpacing = 1.sp
                                        )
                                        Text(
                                            text = "${state.pageNumber} / 604",
                                            fontFamily = fontFamilyUi,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 16.sp,
                                            color = hInk
                                        )
                                    }

                                    // Previous (right)
                                    Text(
                                        text = "Previous →",
                                        fontFamily = fontFamilyUi,
                                        fontWeight = FontWeight.Bold,
                                        color = if (state.pageNumber > 1) hTeal else hInkMuted,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable(enabled = state.pageNumber > 1) {
                                                onNavigateToPage(state.pageNumber - 1)
                                            }
                                            .padding(8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            }
        }

        // ─── MiniPlayer ─── (outside the flip panes: static overlay)
        AnimatedVisibility(
            visible = isPlaying && !playerHiddenByUser && playingVerseKey != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            val playerVerses = (uiState as? PageUiState.Success)?.verses ?: emptyList()
            MiniPlayer(
                verseKey = playingVerseKey ?: "",
                isPlaying = isPlaying,
                onPlayPause = { viewModel.togglePlayPause() },
                onPrevious = {
                    val idx = playerVerses.indexOfFirst { it.verseKey == playingVerseKey }
                    if (idx > 0) viewModel.startPlayback(playerVerses, idx - 1)
                },
                onNext = {
                    val idx = playerVerses.indexOfFirst { it.verseKey == playingVerseKey }
                    if (idx in 0 until playerVerses.size - 1) viewModel.startPlayback(playerVerses, idx + 1)
                },
                onClose = { playerHiddenByUser = true },
                onStop = { viewModel.stopPlaying() },
                progress = playbackProgress?.let { (pos, dur) ->
                    if (dur > 0) (pos.toFloat() / dur).coerceIn(0f, 1f) else null
                }
            )
        }
    }
}
