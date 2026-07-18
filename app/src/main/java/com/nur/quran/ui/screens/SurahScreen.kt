package com.nur.quran.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.HtmlCompat
import com.nur.quran.data.db.entities.ChapterEntity
import com.nur.quran.data.db.entities.VerseEntity
import com.nur.quran.data.db.entities.WordEntity
import com.nur.quran.data.getHizbByPage
import com.nur.quran.data.getJuzByPage
import com.nur.quran.ui.components.ColoredArabicText
import com.nur.quran.ui.components.NurIcons
import com.nur.quran.ui.viewmodels.SurahUiState
import com.nur.quran.ui.viewmodels.SurahViewModel
import com.nur.quran.ui.viewmodels.TafsirUiState
import com.nur.quran.utils.TajweedProcessor
import kotlinx.coroutines.delay

// Colors matching the web app CSS variables
private val hCream     = Color(0xFFFAF7F0)
private val hBone      = Color(0xFFEDE8DA)
private val hBoneDark  = Color(0xFFDDD7C7)
private val hInk       = Color(0xFF2B3F3C)
private val hInkMid    = Color(0xFF4D5F5C)
private val hInkMuted  = Color(0xFF8E9B97)
private val hGold      = Color(0xFFB8924A)
private val hGoldSoft  = Color(0x2EB8924A) // rgba(184,146,74,0.18)
private val hGoldLight = Color(0x26C6A87C) // accent-light ~15% opacity
private val hTeal      = Color(0xFF2E4F4A)
private val hTealSoft  = Color(0x142E4F4A) // ~8% opacity
private val hWhite     = Color(0xFFFAFAF5)
private val hSurface   = Color(0xFFEFECE4)
private val hBorderColor = Color(0xFFDDD7C7)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SurahScreen(
    viewModel: SurahViewModel,
    chapterId: Int,
    onBackClick: () -> Unit,
    onNavigateToSurah: (Int) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isTajweedEnabled by viewModel.isTajweedEnabled.collectAsState()
    val isTranslationEnabled by viewModel.isTranslationEnabled.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val playingVerseKey by viewModel.playingVerseKey.collectAsState()
    val tafsirState by viewModel.tafsirState.collectAsState()
    val bookmarkedVerses by viewModel.bookmarkedVerses.collectAsState()
    val allChapters by viewModel.allChapters.collectAsState()
    val downloadedChapters by viewModel.downloadedChapters.collectAsState()
    val isDownloading by viewModel.isDownloading.collectAsState()
    val collections by viewModel.collections.collectAsState()
    val collectionItems by viewModel.collectionItems.collectAsState()

    var isReadingMode by remember { mutableStateOf(false) }
    var selectedWordForTooltip by remember { mutableStateOf<WordEntity?>(null) }
    var collectionVerse by remember { mutableStateOf<VerseEntity?>(null) }

    val listState = rememberLazyListState()

    LaunchedEffect(chapterId) {
        viewModel.loadChapterDetails(chapterId)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val state = uiState
                    Text(
                        text = if (state is SurahUiState.Success) state.chapter.nameSimple else "Quran Nur",
                        fontFamily = fontFamilyUi,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = hInk
                    )
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
                    // Web Layout IconBtns: reading mode, tajweed, translation
                    TopBarIconBtn(
                        icon = NurIcons.BookOpen,
                        active = isReadingMode,
                        label = "Reading mode"
                    ) { isReadingMode = !isReadingMode }
                    TopBarIconBtn(
                        icon = NurIcons.Highlighter,
                        active = isTajweedEnabled,
                        label = "Tajweed"
                    ) { viewModel.toggleTajweed() }
                    TopBarIconBtn(
                        icon = NurIcons.Type,
                        active = isTranslationEnabled,
                        label = "Translation"
                    ) { viewModel.toggleTranslation() }
                    Spacer(modifier = Modifier.width(8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = hWhite)
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
                                    onNavigateToSurah(chapterId + 1)
                                swipeTotal < -swipeThreshold && chapterId > 1 ->
                                    onNavigateToSurah(chapterId - 1)
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

                    // Restore scroll position once verses are ready
                    LaunchedEffect(chapterId, verses.isNotEmpty()) {
                        if (verses.isNotEmpty()) {
                            viewModel.scrollPositions[chapterId]?.let { (index, offset) ->
                                listState.scrollToItem(index, offset)
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

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 120.dp)
                    ) {
                        item {
                            SurahHeader(
                                chapter = chapter,
                                versesStartPage = verses.firstOrNull()?.pageNumber ?: 0,
                                isPlaying = isPlaying,
                                isDownloaded = chapter.id in downloadedChapters,
                                isDownloading = isDownloading,
                                onPlayClick = { viewModel.playPauseChapter(verses, chapter.id) },
                                onDownloadClick = { viewModel.downloadChapterAudio(chapter.id, verses) }
                            )
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
                                    isTranslationEnabled = isTranslationEnabled,
                                    isTajweedEnabled = isTajweedEnabled,
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
                                    onShareClick = { viewModel.shareVerse(verse.verseKey, chapter.nameSimple) },
                                    onAddToCollection = { collectionVerse = verse },
                                    onWordClick = { word -> selectedWordForTooltip = word },
                                    onLoadFootnote = { id -> viewModel.getFootnoteText(id) }
                                )
                            }
                        } else {
                            item {
                                ContinuousReadingView(
                                    verses = verses,
                                    wordsMap = wordsMap,
                                    isTajweedEnabled = isTajweedEnabled,
                                    onWordClick = { word -> selectedWordForTooltip = word }
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
    onPlayClick: () -> Unit,
    onDownloadClick: () -> Unit
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
                letterSpacing = (-0.5).sp
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
    isTranslationEnabled: Boolean,
    isTajweedEnabled: Boolean,
    isCurrentlyPlaying: Boolean,
    isBookmarked: Boolean,
    isTafsirOpen: Boolean,
    onPlayClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    onTafsirClick: () -> Unit,
    onShareClick: () -> Unit,
    onAddToCollection: () -> Unit,
    onWordClick: (WordEntity) -> Unit,
    onLoadFootnote: suspend (String) -> String
) {
    var activeFootnoteId by remember { mutableStateOf<String?>(null) }
    var footnoteText by remember { mutableStateOf("") }
    var isFootnoteLoading by remember { mutableStateOf(false) }

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
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            words.forEach { word ->
                val isEndMarker = word.charTypeName == "end"
                Box(
                    modifier = Modifier
                        .padding(horizontal = 2.dp, vertical = 4.dp)
                        .clickable(enabled = !isEndMarker) { onWordClick(word) }
                ) {
                    if (isTajweedEnabled && word.textUthmaniTajweed != null) {
                        val parsedSegments = remember(word.textUthmaniTajweed, word.textUthmani) {
                            TajweedProcessor.getWordTajweedSegments(
                                plainText = word.textUthmani ?: "",
                                tajweedHtml = word.textUthmaniTajweed ?: "",
                                defaultColor = "#2B3F3C"
                            )
                        }
                        ColoredArabicText(
                            text = word.textUthmani ?: "",
                            segments = parsedSegments,
                            fontFamily = fontFamilyArabic,
                            textStyle = TextStyle(
                                fontSize = 26.sp,
                                color = hInk,
                                textAlign = TextAlign.Right,
                                lineHeight = 52.sp
                            ),
                            onSegmentClick = { _, _ -> onWordClick(word) }
                        )
                    } else {
                        Text(
                            text = word.textUthmani ?: "",
                            fontSize = 26.sp,
                            color = if (isEndMarker) hGold else hInk,
                            fontFamily = fontFamilyArabic,
                            textAlign = TextAlign.Right,
                            fontWeight = if (isEndMarker) FontWeight.Bold else FontWeight.Normal,
                            lineHeight = 52.sp
                        )
                    }
                }
            }
        }

        // Translation with clickable footnotes
        if (isTranslationEnabled && !verse.translation.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(20.dp))
            TranslationText(
                html = verse.translation,
                onFootnoteClick = { footnoteId ->
                    activeFootnoteId = if (activeFootnoteId == footnoteId) null else footnoteId
                }
            )
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
    onFootnoteClick: (String) -> Unit
) {
    val annotated = remember(html) { buildTranslationAnnotatedString(html) }
    ClickableText(
        text = annotated,
        style = TextStyle(
            fontSize = 15.sp,
            color = hInkMid,
            fontFamily = fontFamilyBody,
            lineHeight = 24.sp,
            textAlign = TextAlign.Start
        ),
        onClick = { offset ->
            annotated.getStringAnnotations("footnote", offset, offset)
                .firstOrNull()?.let { onFootnoteClick(it.item) }
        }
    )
}

private fun buildTranslationAnnotatedString(html: String): AnnotatedString {
    val footnoteIds = mutableListOf<String>()
    // Replace each footnote sup with sentinel-wrapped digits, in order
    val withSentinels = footnoteSupRegex.replace(html) { match ->
        footnoteIds.add(match.groupValues[1])
        "${match.groupValues[2]}"
    }
    // Strip all remaining HTML
    val plain = HtmlCompat.fromHtml(withSentinels, HtmlCompat.FROM_HTML_MODE_LEGACY).toString()

    return buildAnnotatedString {
        var i = 0
        var footnoteIndex = 0
        var plainStart = 0
        while (i < plain.length) {
            if (plain[i] == '') {
                if (plainStart < i) append(plain.substring(plainStart, i))
                val end = plain.indexOf('', i + 1)
                if (end == -1) break
                val digits = plain.substring(i + 1, end)
                val id = footnoteIds.getOrNull(footnoteIndex) ?: continue
                footnoteIndex++
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
                i = end + 1
                plainStart = i
            } else {
                i++
            }
        }
        if (plainStart < plain.length) append(plain.substring(plainStart))
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
    onNavigateToSurah: (Int) -> Unit
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
                    .clickable { onNavigateToSurah(chapter.id + 1) }
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
                    .clickable { onNavigateToSurah(chapter.id - 1) }
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

// ── Continuous Reading View (per-page flow, web reading mode) ───────────
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ContinuousReadingView(
    verses: List<VerseEntity>,
    wordsMap: Map<Int, List<WordEntity>>,
    isTajweedEnabled: Boolean,
    onWordClick: (WordEntity) -> Unit
) {
    val versesByPage = verses.groupBy { it.pageNumber }.toSortedMap()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        versesByPage.forEach { (page, pageVerses) ->
            WebPageDivider(pageNumber = page)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                pageVerses.forEach { verse ->
                    val words = wordsMap[verse.id] ?: emptyList()
                    words.forEach { word ->
                        val isEndMarker = word.charTypeName == "end"
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 2.dp, vertical = 6.dp)
                                .clickable(enabled = !isEndMarker) { onWordClick(word) }
                        ) {
                            if (isTajweedEnabled && word.textUthmaniTajweed != null) {
                                val parsedSegments = remember(word.textUthmaniTajweed, word.textUthmani) {
                                    TajweedProcessor.getWordTajweedSegments(
                                        plainText = word.textUthmani ?: "",
                                        tajweedHtml = word.textUthmaniTajweed ?: "",
                                        defaultColor = "#2B3F3C"
                                    )
                                }
                                ColoredArabicText(
                                    text = word.textUthmani ?: "",
                                    segments = parsedSegments,
                                    fontFamily = fontFamilyArabic,
                                    textStyle = TextStyle(
                                        fontSize = 26.sp,
                                        color = hInk,
                                        textAlign = TextAlign.Right,
                                        lineHeight = 52.sp
                                    ),
                                    onSegmentClick = { _, _ -> onWordClick(word) }
                                )
                            } else {
                                Text(
                                    text = word.textUthmani ?: "",
                                    fontSize = 26.sp,
                                    color = if (isEndMarker) hGold else hInk,
                                    fontFamily = fontFamilyArabic,
                                    textAlign = TextAlign.Right,
                                    fontWeight = if (isEndMarker) FontWeight.Bold else FontWeight.Normal,
                                    lineHeight = 52.sp
                                )
                            }
                        }
                    }
                }
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
