package com.nur.quran.ui.screens

import androidx.compose.ui.res.stringResource

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nur.quran.R
import com.nur.quran.data.HIZB_STARTS
import com.nur.quran.data.JUZ_STARTS
import com.nur.quran.data.MUSHAF_PAGE_COUNT
import com.nur.quran.data.getJuzByPage
import com.nur.quran.data.db.entities.BookmarkEntity
import com.nur.quran.data.db.entities.ChapterEntity
import com.nur.quran.data.db.entities.RecentlyReadEntity
import com.nur.quran.ui.components.Coachmark
import com.nur.quran.ui.components.NurIcons
import com.nur.quran.ui.components.PageTourModal
import com.nur.quran.ui.components.TourStep
import com.nur.quran.ui.viewmodels.HomeStats
import com.nur.quran.ui.viewmodels.HomeUiState
import com.nur.quran.ui.viewmodels.HomeViewModel
import com.nur.quran.ui.viewmodels.OnboardingState
import com.nur.quran.ui.viewmodels.OnboardingTours
import com.nur.quran.ui.viewmodels.PackViewModel
import com.nur.quran.ui.viewmodels.SurahViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

// ── Local & System Fonts Setup ──────────────────────────────────────────────

val fontFamilyUi = FontFamily(
    Font(R.font.cormorant_garamond_regular, FontWeight.Normal),
    Font(R.font.cormorant_garamond_bold, FontWeight.Bold)
)
val fontFamilyBody = FontFamily(
    Font(R.font.piazzolla_regular, FontWeight.Normal),
    Font(R.font.piazzolla_bold, FontWeight.Bold)
)
val fontFamilyMono = FontFamily.Monospace
val fontFamilyArabic = fontScheherazade

// ── Browse item model (mirrors the web getBrowseItems output) ───────────
private data class BrowseItem(
    val key: String,
    val title: String,
    val subtitle: String,
    val meta: String,
    val arabic: String?,
    val prefix: Int?,          // null → Hash icon (page mode)
    val chapterId: Int? = null,
    val pageNumber: Int? = null
)

// ── Guided tours (identical steps to the web app's Home.jsx) ────────────
@Composable
private fun homeTourSteps() = listOf(
    TourStep(
        title = stringResource(R.string.ui_welcome_to_quran_nur_3e4675),
        description = stringResource(R.string.ui_this_is_your_personal_companion_for_reading_memo_01f627),
        icon = NurIcons.Sparkles
    ),
    TourStep(
        title = stringResource(R.string.ui_resume_reading_ba8c6b),
        description = stringResource(R.string.ui_pick_up_exactly_where_you_left_off_last_time_a2318e),
        icon = NurIcons.ArrowRight,
        target = "resume"
    ),
    TourStep(
        title = stringResource(R.string.ui_browse_the_quran_d892dc),
        description = stringResource(R.string.ui_navigate_quickly_to_any_surah_juz_or_page_92dee7),
        icon = NurIcons.Search,
        target = "browse"
    ),
    TourStep(
        title = stringResource(R.string.ui_set_a_reading_goal_83f485),
        description = stringResource(R.string.ui_head_to_the_planner_tab_to_create_a_personalized_6dc411),
        icon = NurIcons.CalendarDays,
        link = "planner"
    ),
    TourStep(
        title = stringResource(R.string.ui_start_memorizing_756aad),
        description = stringResource(R.string.ui_use_the_memorize_tab_to_practice_hifdh_with_spac_cbb3b8),
        icon = NurIcons.Brain,
        link = "memorize"
    )
)

@Composable
private fun homeAdvancedTourSteps() = listOf(
    TourStep(
        title = stringResource(R.string.ui_swipe_between_surahs_82bf82),
        description = stringResource(R.string.ui_while_reading_try_swiping_left_or_right_to_quick_bc1534),
        icon = NurIcons.ArrowRight
    ),
    TourStep(
        title = stringResource(R.string.ui_create_a_habit_56a3d9),
        description = stringResource(R.string.ui_consistency_is_key_use_the_planner_to_build_a_da_781e2c),
        icon = NurIcons.CalendarDays,
        link = "planner"
    )
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    surahViewModel: SurahViewModel? = null,
    onChapterClick: (Int, String?) -> Unit,
    onPageClick: (Int) -> Unit,
    onNavigateToRoute: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val browseMode by viewModel.browseMode.collectAsState()
    val recentlyRead by viewModel.recentlyRead.collectAsState()
    val latestBookmark by viewModel.latestBookmark.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val onboardingState by viewModel.onboardingState.collectAsState()
    val completedTours by viewModel.completedTours.collectAsState()
    val homeVisits by viewModel.homeVisits.collectAsState()
    val context = LocalContext.current

    // Page-visit analytics (feeds the tour visit thresholds, like the web app)
    LaunchedEffect(Unit) { viewModel.recordHomeVisit() }

    // Tour target tracking: each targeted section records its window rect here.
    val targetRects = remember { mutableStateMapOf<String, Rect>() }
    val lazyListState: LazyListState = rememberLazyListState()

    // Item indices of the tour-targeted sections (mirrors the LazyColumn order below).
    val sectionIndices = remember(onboardingState.isDismissed, latestBookmark, recentlyRead) {
        buildSectionIndexMap(
            onboardingDismissed = onboardingState.isDismissed,
            hasBookmark = latestBookmark != null,
            hasRecentlyRead = recentlyRead.isNotEmpty()
        )
    }

    var showSettingsDrawer by remember { mutableStateOf(false) }

    val defaultArabicFontName = remember {
        context.getSharedPreferences("HifdhPrefs", Context.MODE_PRIVATE)
            .getString("arabic_font", "KFGQPC Hafs") ?: "KFGQPC Hafs"
    }
    val activeArabicFontName = surahViewModel?.selectedArabicFontName?.collectAsState(initial = defaultArabicFontName)?.value ?: defaultArabicFontName
    val fontFamilyArabic = remember(activeArabicFontName) {
        getArabicFontFamily(activeArabicFontName)
    }

    // Entrance animation
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Column(modifier = Modifier.fillMaxSize().background(hWhite)) {
        // Top Header matching Web App Layout.jsx header
        TopNavbar(
            title = "Quran Nur",
            onThemeToggle = {
                isDarkThemeGlobal = !isDarkThemeGlobal
                context.getSharedPreferences("Settings", Context.MODE_PRIVATE)
                    .edit().putBoolean("is_dark_theme", isDarkThemeGlobal).apply()
            },
            onSettingsClick = { showSettingsDrawer = true }
        )

        Box(modifier = Modifier.weight(1f)) {
            when (val state = uiState) {
                is HomeUiState.Loading -> {
                    // Web replaces the whole page with a centered spinner while loading
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(hWhite)
                            .padding(top = 80.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = hTeal,
                                trackColor = hBoneDark,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(text = stringResource(R.string.ui_loading_b04ba4), color = hInkMuted, fontSize = 14.sp)
                        }
                    }
                }
                is HomeUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(hWhite)
                            .padding(32.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Text(
                            text = stringResource(R.string.ui_error_fetching_data_please_try_again_later_69594e),
                            color = hInkMuted,
                            fontSize = 14.sp,
                            fontStyle = FontStyle.Italic,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                is HomeUiState.Success -> {
                    val chapters = state.chapters
                    val lastRead = recentlyRead.firstOrNull()
                    val browseItems = remember(browseMode, chapters, searchQuery) {
                        filterBrowseItems(buildBrowseItems(browseMode, chapters, context), searchQuery)
                    }

            androidx.compose.animation.AnimatedVisibility(
                visible = visible,
                enter = fadeIn() + slideInVertically(initialOffsetY = { 20 })
            ) {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(hWhite),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    // ─── Continue Reading (web Home.jsx parity) ───
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 24.dp, bottom = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (lastRead != null) {
                                ContinueReadingCard(
                                    item = lastRead,
                                    onClick = {
                                        // Reopen the exact reader the user was in:
                                        // page reads (page/juz/hizb) → page screen,
                                        // surah reads → surah screen at the anchor.
                                        val lastPage = lastRead.pageNumber
                                        if (lastPage != null && lastRead.verseKey == null) {
                                            onPageClick(lastPage)
                                        } else {
                                            onChapterClick(lastRead.chapterId, lastRead.verseKey)
                                        }
                                    }
                                )
                            } else {
                                ZeroStateContinueCard(
                                    onClick = { onChapterClick(1, null) }
                                )
                            }
                        }
                    }

                    // ─── Onboarding Progress Checklist ───
                    if (!onboardingState.isDismissed) {
                        item {
                            OnboardingProgressCard(
                                state = onboardingState,
                                onDismiss = { viewModel.dismissOnboarding() },
                                onRowClick = { tourId ->
                                    viewModel.completeTour(tourId)
                                    val route = when (tourId) {
                                        OnboardingTours.HOME -> "home"
                                        OnboardingTours.SURAH -> "surah"
                                        OnboardingTours.MEMORIZATION -> "memorize"
                                        OnboardingTours.PLANNER -> "planner"
                                        OnboardingTours.LIBRARY -> "library"
                                        else -> null
                                    }
                                    if (route != null) onNavigateToRoute(route)
                                }
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                        }
                    }

                    // ─── Weekly Heatmap ───
                    item {
                        WeeklyHeatmapCard(stats = stats)
                        Spacer(modifier = Modifier.height(28.dp))
                    }

                    // ─── Bookmark ───
                    latestBookmark?.let { bookmark ->
                        item {
                            BookmarkCard(
                                bookmark = bookmark,
                                onClick = {
                                    // Reopen in the reader context the bookmark
                                    // was taken from (page mode → page screen).
                                    val pg = bookmark.sourcePage
                                    if (bookmark.sourceMode == "page" && pg != null) {
                                        onPageClick(pg)
                                    } else {
                                        onChapterClick(bookmark.chapterId, bookmark.verseKey)
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.height(28.dp))
                        }
                    }

                    // ─── Recently Read ───
                    if (recentlyRead.isNotEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onGloballyPositioned { targetRects["resume"] = it.boundsInWindow() }
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = NurIcons.BookOpen,
                                            contentDescription = null,
                                            tint = hInk,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = stringResource(R.string.ui_recently_read_6f71b4),
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = hInk,
                                            fontFamily = fontFamilyUi
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState())
                                            .padding(bottom = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        recentlyRead.take(6).forEach { item ->
                                            RecentlyReadCard(
                                                item = item,
                                                onClick = { onChapterClick(item.chapterId, item.verseKey) }
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }

                    // ─── Browse the Quran ───
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { targetRects["browse"] = it.boundsInWindow() }
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = NurIcons.BookOpen,
                                        contentDescription = null,
                                        tint = hInk,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.ui_browse_the_quran_d892dc),
                                        fontSize = 21.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = hInk,
                                        fontFamily = fontFamilyUi
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = stringResource(R.string.ui_select_a_surah_page_juz_or_hizb_to_begin_badc18),
                                    fontSize = 13.sp,
                                    color = hInkMuted
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                // Mode pills
                                val modes = listOf(
                                    Triple("surah", stringResource(R.string.ui_surah_a9f7c5), NurIcons.BookOpen),
                                    Triple("page", stringResource(R.string.ui_page_fb0627), NurIcons.Rows3),
                                    Triple("juz", stringResource(R.string.ui_juz_b60a3b), NurIcons.LibraryBig),
                                    Triple("hizb", stringResource(R.string.ui_hizb_922ec9), NurIcons.Layers3)
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    modes.forEach { (mode, label, icon) ->
                                        val isSelected = browseMode == mode
                                        Surface(
                                            onClick = {
                                                viewModel.setBrowseMode(mode)
                                                viewModel.setSearchQuery("")
                                            },
                                            shape = RoundedCornerShape(20.dp),
                                            color = if (isSelected) hTealSoft else Color.Transparent,
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.5.dp,
                                                if (isSelected) hTeal else hBoneDark
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = icon,
                                                    contentDescription = null,
                                                    tint = if (isSelected) hTeal else hInkMuted,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = label,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (isSelected) hTeal else hInkMuted
                                                )
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    }

                    // Search
                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = {
                                Text(
                                    stringResource(R.string.ui_search_1_s_b63680, when (browseMode) { "page" -> stringResource(R.string.ui_page_fb0627); "juz" -> stringResource(R.string.ui_juz_b60a3b); "hizb" -> stringResource(R.string.ui_hizb_922ec9); else -> stringResource(R.string.ui_surah_a9f7c5) }),
                                    color = hInkMuted,
                                    fontSize = 15.sp,
                                    fontFamily = fontFamilyBody
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    NurIcons.Search,
                                    contentDescription = stringResource(R.string.ui_search_bce064),
                                    tint = hInkMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { viewModel.setSearchQuery("") },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            NurIcons.X,
                                            contentDescription = stringResource(R.string.ui_clear_search_87e328),
                                            tint = hInkMuted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = hTeal,
                                unfocusedBorderColor = hBoneDark,
                                focusedContainerColor = hCream,
                                unfocusedContainerColor = hCream,
                                cursorColor = hTeal,
                                focusedTextColor = hInk,
                                unfocusedTextColor = hInk
                            ),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Browse items
                    items(browseItems, key = { it.key }) { item ->
                        BrowseItemCard(
                            item = item,
                            fontFamilyArabic = fontFamilyArabic,
                            onClick = {
                                when {
                                    item.pageNumber != null && browseMode != "surah" -> onPageClick(item.pageNumber)
                                    item.chapterId != null -> onChapterClick(item.chapterId, null)
                                    item.pageNumber != null -> onPageClick(item.pageNumber)
                                }
                            }
                        )
                    }
                    if (browseItems.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 36.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = NurIcons.Search,
                                    contentDescription = null,
                                    tint = hInkMuted.copy(alpha = 0.4f),
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = if (searchQuery.isNotEmpty()) {
                                        "No results found for \"$searchQuery\" in $browseMode"
                                    } else {
                                        "No results matching your search."
                                    },
                                    fontSize = 14.sp,
                                    fontFamily = fontFamilyUi,
                                    color = hInk,
                                    textAlign = TextAlign.Center
                                )
                                if (searchQuery.isNotEmpty()) {
                                    Surface(
                                        onClick = { viewModel.setSearchQuery("") },
                                        shape = RoundedCornerShape(12.dp),
                                        color = hCream,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, hBoneDark),
                                        modifier = Modifier.padding(top = 4.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.ui_clear_search_87e328),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = hInk,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Bottom spacing for floating nav bar
                    item { Spacer(modifier = Modifier.height(96.dp)) }
                }
            }
        }
    }
    }
}

    if (showSettingsDrawer && surahViewModel != null) {
        val packVm: PackViewModel = hiltViewModel()
        val tafsirPackRows by packVm.tafsirPacks.collectAsState()
        val wordCached by packVm.wordCachedCount.collectAsState()
        val wordDownloading by packVm.wordIsDownloading.collectAsState()
        val wordProgress by packVm.wordProgressByTafsir.collectAsState()
                val syncState by packVm.syncUiState.collectAsState()
        com.nur.quran.ui.components.SettingsDrawer(
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

    // ─── Guided Tours (first-visit + advanced, mirroring web Home.jsx) ───
    val toursEnabled = uiState is HomeUiState.Success
    PageTourModal(
        tourId = "home-tour",
        pageId = "home",
        visitThreshold = 1,
        enabled = toursEnabled,
        steps = homeTourSteps(),
        completedTours = completedTours,
        pageVisits = homeVisits,
        isDark = isDarkThemeGlobal,
        sectionIndices = sectionIndices,
        lazyListState = lazyListState,
        targetRects = targetRects,
        onCompleteTour = viewModel::completeTour,
        onNavigateToRoute = onNavigateToRoute
    )
    PageTourModal(
        tourId = "home-tour-advanced",
        pageId = "home",
        visitThreshold = 3,
        enabled = toursEnabled,
        steps = homeAdvancedTourSteps(),
        completedTours = completedTours,
        pageVisits = homeVisits,
        isDark = isDarkThemeGlobal,
        sectionIndices = sectionIndices,
        lazyListState = lazyListState,
        targetRects = targetRects,
        onCompleteTour = viewModel::completeTour,
        onNavigateToRoute = onNavigateToRoute
    )
}

// ── Web Top Navbar (matching Web App Layout.jsx header) ──────────────────
@Composable
fun TopNavbar(
    title: String = "Quran Nur",
    subTitle: String? = null,
    onThemeToggle: () -> Unit,
    onSettingsClick: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        color = hCream.copy(alpha = 0.95f),
        shadowElevation = 0.dp
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Brand Logo & Title (optional back chevron first)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onBack != null) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onBack() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = NurIcons.ArrowLeft,
                                contentDescription = stringResource(R.string.ui_back_b52b36),
                                tint = hInk,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = com.nur.quran.R.drawable.ic_logo),
                        contentDescription = stringResource(R.string.ui_quran_nur_logo_5e0764),
                        modifier = Modifier.size(28.dp).clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = title,
                        fontFamily = fontFamilyUi,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = hInk
                    )
                    if (!subTitle.isNullOrEmpty()) {
                        Text(
                            text = " / ",
                            fontSize = 12.sp,
                            color = hInkMuted
                        )
                        Text(
                            text = subTitle,
                            fontFamily = fontFamilyMono,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = hInkMid
                        )
                    }
                }

                // Right Action Buttons (Theme Toggle + Settings Gear)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Theme Toggle Button (Sun/Moon)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onThemeToggle() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isDarkThemeGlobal) NurIcons.Sun else NurIcons.Moon,
                            contentDescription = stringResource(R.string.ui_toggle_theme_b74c4e),
                            tint = hInkMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Settings Button (Gear)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSettingsClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = NurIcons.Settings,
                            contentDescription = stringResource(R.string.ui_settings_c7f73b),
                            tint = hInkMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            Divider(color = hBoneDark.copy(alpha = 0.5f), thickness = 1.dp)
        }
    }
}

// ── Continue Reading Card (teal gradient) ───────────────────────────────
@Composable
private fun ContinueReadingCard(item: RecentlyReadEntity, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 440.dp)
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .background(Brush.linearGradient(listOf(hTeal, hTealMid)))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = NurIcons.BookOpen,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.ui_continue_1_s_a3e532, item.chapterName),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    fontFamily = fontFamilyUi,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                val verseLabel = item.verseKey?.split(":")?.getOrNull(1)?.let { "Verse $it" }
                    ?: stringResource(R.string.ui_from_the_beginning_8c8e8d)
                Text(
                    text = stringResource(R.string.ui_1_s_2_s_c1aacd, verseLabel, com.nur.quran.i18n.relativeTime(item.timestamp)),
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.75f),
                    fontFamily = fontFamilyMono,
                    letterSpacing = 0.3.sp
                )
            }
            Icon(
                imageVector = NurIcons.ArrowRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun ZeroStateContinueCard(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 440.dp)
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(18.dp),
        color = hTealSoft,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, hTeal.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(hTeal.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = NurIcons.BookOpen,
                    contentDescription = null,
                    tint = hTeal,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.ui_begin_your_journey_02d598),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = hInk,
                fontFamily = fontFamilyUi
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.ui_start_reading_with_surah_al_fatihah_the_opening_db108d),
                fontSize = 12.sp,
                color = hInkMuted
            )
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = hTeal,
                modifier = Modifier.height(34.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.ui_start_reading_ec8c2d),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = fontFamilyUi
                    )
                    Icon(
                        imageVector = NurIcons.ArrowRight,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

// ── Weekly Heatmap ("This Week") ────────────────────────────────────────
@Composable
private fun WeeklyHeatmapCard(stats: HomeStats) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = hCream,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, hBoneDark)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = NurIcons.BarChart3,
                    contentDescription = null,
                    tint = hInk,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.ui_this_week_4e68dc),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = hInk,
                    fontFamily = fontFamilyUi
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                stats.week.forEach { day ->
                    val pct = Math.round(day.mins * 100f / stats.weekMax)
                    // Level thresholds identical to the web app
                    val level = when {
                        pct > 80 -> 3 // max
                        pct > 50 -> 2 // high
                        pct > 20 -> 1 // med
                        else -> 0     // low
                    }
                    val fillColor = when (level) {
                        3 -> hGreen
                        2 -> hTeal
                        1 -> hTealSoft
                        else -> hBoneDark
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = com.nur.quran.i18n.localizedWeekday(day.label),
                            fontSize = 9.sp,
                            fontFamily = fontFamilyMono,
                            letterSpacing = 0.5.sp,
                            fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
                            color = if (day.isToday) hTeal else hInkMuted
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(32.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(hBone)
                        ) {
                            if (day.mins > 0) {
                                val fraction = maxOf(15, pct) / 100f
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(fraction)
                                        .align(Alignment.BottomCenter)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(fillColor)
                                        .then(
                                            if (level == 1) Modifier.border(
                                                1.dp,
                                                Color(0x262E4F4A),
                                                RoundedCornerShape(6.dp)
                                            ) else Modifier
                                        )
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.ui_1_sm_830877, day.mins),
                            fontSize = 9.sp,
                            fontFamily = fontFamilyMono,
                            fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
                            color = if (day.isToday) hTeal else hInkMuted
                        )
                    }
                }
            }
        }
    }
}

// ── Bookmark Card (gold) ────────────────────────────────────────────────
@Composable
private fun BookmarkCard(bookmark: BookmarkEntity, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = hGoldSoft,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, hGold)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(hGold.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = NurIcons.Bookmark,
                    contentDescription = null,
                    tint = hGold,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = bookmark.surahName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = hGold,
                    fontFamily = fontFamilyUi
                )
                Spacer(modifier = Modifier.height(2.dp))
                val verseNum = bookmark.verseKey.split(":").getOrNull(1) ?: ""
                Text(
                    text = stringResource(R.string.ui_verse_1_s_resume_reading_8168fe, verseNum),
                    fontSize = 11.sp,
                    color = hInkMuted
                )
            }
            Icon(
                imageVector = NurIcons.ArrowRight,
                contentDescription = null,
                tint = hGold,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// ── Recently Read Card ──────────────────────────────────────────────────
@Composable
private fun RecentlyReadCard(item: RecentlyReadEntity, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.widthIn(min = 155.dp, max = 180.dp),
        shape = RoundedCornerShape(14.dp),
        color = hCream,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, hBoneDark)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(hTealSoft),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.chapterId.toString(),
                    fontWeight = FontWeight.Bold,
                    color = hTeal,
                    fontSize = 11.sp,
                    fontFamily = fontFamilyMono
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = item.chapterName,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = hInk,
                fontFamily = fontFamilyUi,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            item.verseKey?.split(":")?.getOrNull(1)?.let { verseNum ->
                Text(
                    text = stringResource(R.string.ui_verse_1_s_7e45d7, verseNum),
                    fontSize = 11.sp,
                    color = hInkMuted
                )
            }
            Text(
                text = com.nur.quran.i18n.relativeTime(item.timestamp),
                fontSize = 11.sp,
                color = hInkMuted
            )
        }
    }
}

// ── Browse Item Card (unified Surah/Page/Juz/Hizb, matching web) ────────
@Composable
private fun BrowseItemCard(
    item: BrowseItem,
    fontFamilyArabic: FontFamily = fontScheherazade,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        shape = RoundedCornerShape(14.dp),
        color = hCream,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, hBoneDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Number badge (or Hash icon for pages)
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(hTealSoft),
                contentAlignment = Alignment.Center
            ) {
                if (item.prefix != null) {
                    Text(
                        text = item.prefix.toString(),
                        fontWeight = FontWeight.Bold,
                        color = hTeal,
                        fontSize = 13.sp,
                        fontFamily = fontFamilyMono
                    )
                } else {
                    Icon(
                        imageVector = NurIcons.Hash,
                        contentDescription = null,
                        tint = hTeal,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.title,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = hInk,
                        fontFamily = fontFamilyUi,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (item.arabic != null) {
                        Text(
                            text = item.arabic,
                            fontFamily = fontFamilyArabic,
                            fontSize = 20.sp,
                            color = hGold,
                            maxLines = 1
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.subtitle,
                        fontSize = 11.sp,
                        fontFamily = fontFamilyBody,
                        color = hInkMuted,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    // Meta chip
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(hBone)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = item.meta,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = hInkMuted
                        )
                    }
                }
            }
        }
    }
}

// ── Helpers ─────────────────────────────────────────────────────────────

/** Mirrors the web app's getBrowseItems(). */
private fun buildBrowseItems(mode: String, chapters: List<ChapterEntity>, context: Context): List<BrowseItem> {
    val chapMap = chapters.associateBy { it.id }
    return when (mode) {
        "page" -> (1..MUSHAF_PAGE_COUNT).map { n ->
            val juz = getJuzByPage(n)
            BrowseItem(
                key = "page-$n",
                title = context.getString(R.string.ui_page_1_s_6654d7, n),
                subtitle = context.getString(R.string.ui_juz_1_s_mushaf_view_98d63e, juz.id),
                meta = context.getString(R.string.ui_page_1_s_6654d7, n.toString().padStart(3, '0')),
                arabic = "صفحة $n",
                prefix = n,
                pageNumber = n
            )
        }
        "juz" -> JUZ_STARTS.map { juz ->
            val chapId = juz.verseKey.substringBefore(":").toIntOrNull() ?: 1
            val surahName = chapMap[chapId]?.nameSimple ?: context.getString(R.string.ui_surah_1_s_ffa393, chapId)
            BrowseItem(
                key = "juz-${juz.id}",
                title = context.getString(R.string.ui_juz_1_s_146e8b, juz.id),
                subtitle = "$surahName (${juz.verseKey})",
                meta = context.getString(R.string.ui_page_1_s_6654d7, juz.pageNumber),
                arabic = "الجزء ${juz.id}",
                prefix = juz.id,
                pageNumber = juz.pageNumber,
                chapterId = chapId
            )
        }
        "hizb" -> HIZB_STARTS.map { hizb ->
            val chapId = hizb.verseKey.substringBefore(":").toIntOrNull() ?: 1
            val surahName = chapMap[chapId]?.nameSimple ?: context.getString(R.string.ui_surah_1_s_ffa393, chapId)
            BrowseItem(
                key = "hizb-${hizb.id}",
                title = context.getString(R.string.ui_hizb_1_s_ab29bf, hizb.id),
                subtitle = "$surahName (${hizb.verseKey})",
                meta = context.getString(R.string.ui_page_1_s_6654d7, hizb.pageNumber),
                arabic = "حزب ${hizb.id}",
                prefix = hizb.id,
                pageNumber = hizb.pageNumber,
                chapterId = chapId
            )
        }
        else -> chapters.map { chapter ->
            val place = if (chapter.revelationPlace.equals("makkah", ignoreCase = true)) context.getString(R.string.ui_meccan_c6f6fb) else context.getString(R.string.ui_medinan_609839)
            BrowseItem(
                key = "surah-${chapter.id}",
                title = chapter.nameSimple,
                subtitle = chapter.translatedName.ifEmpty { context.getString(R.string.ui_surah_1_s_ffa393, chapter.id) },
                meta = context.getString(R.string.ui_1_s_2_s_ayahs_165547, place, chapter.versesCount),
                arabic = chapter.nameArabic,
                prefix = chapter.id,
                chapterId = chapter.id
            )
        }
    }
}

/** Mirrors the web app's search filter (title/subtitle/meta/arabic/number). */
private fun filterBrowseItems(items: List<BrowseItem>, query: String): List<BrowseItem> {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return items
    return items.filter { item ->
        listOfNotNull(
            item.title,
            item.subtitle,
            item.meta,
            item.arabic,
            item.prefix?.toString(),
            item.chapterId?.toString(),
            item.pageNumber?.toString()
        ).any { it.lowercase().contains(q) }
    }
}

/** Mirrors the web app's timeAgo(). */
private fun timeAgo(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val mins = diff / 60000
    if (mins < 60) return "${mins}m ago"
    val hrs = mins / 60
    if (hrs < 24) return "${hrs}h ago"
    return "${hrs / 24}d ago"
}

// ── Onboarding Progress Checklist (5 tours, matching web OnboardingProgress.jsx) ─
@Composable
private fun OnboardingProgressCard(
    state: OnboardingState,
    onDismiss: () -> Unit,
    onRowClick: (String) -> Unit
) {
    var autoDismissed by remember { mutableStateOf(false) }
    val isFullyCompleted = state.completedSteps >= state.totalSteps

    // Web auto-dismisses the card 5s after the checklist is fully completed.
    LaunchedEffect(isFullyCompleted) {
        if (isFullyCompleted) {
            delay(5000)
            autoDismissed = true
        }
    }

    if (autoDismissed || (isFullyCompleted && state.completedTours.isNotEmpty())) return

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = hCream,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, hBoneDark)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Elegant background watermark logo
            androidx.compose.foundation.Image(
                painter = painterResource(id = R.drawable.ic_logo),
                contentDescription = null,
                alpha = 0.03f,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(160.dp)
                    .offset(x = 24.dp, y = (-24).dp)
            )

            Column(modifier = Modifier.padding(24.dp)) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.End)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(hWhite)
                        .border(1.dp, hBone, CircleShape)
                ) {
                    Icon(
                        imageVector = NurIcons.X,
                        contentDescription = stringResource(R.string.ui_dismiss_70afe9),
                        tint = hInkMuted,
                        modifier = Modifier.size(14.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.ui_welcome_to_quran_nur_3e4675),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = hInk,
                    fontFamily = fontFamilyUi
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.ui_1_s_of_2_s_steps_completed_8c2687, state.completedSteps, state.totalSteps),
                    fontSize = 13.sp,
                    color = hInkMid,
                    fontFamily = fontFamilyBody
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Progress bar (teal → gold gradient, animated width like the web)
                val progressFraction = state.completedSteps.toFloat() / state.totalSteps
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(100.dp))
                        .background(hBoneDark)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressFraction)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(100.dp))
                            .background(Brush.horizontalGradient(listOf(hTeal, Color(0xFFB8924A))))
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Tour rows (deep-link into the app, check off when done)
                OnboardingTours.ROWS.forEach { row ->
                    val isCompleted = row.id in state.completedTours
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onRowClick(row.id) }
                            .padding(vertical = 10.dp, horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(hWhite)
                                .shadow(2.dp, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = row.emoji, fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = row.label,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isCompleted) hInkMuted.copy(alpha = 0.6f) else hInk,
                            modifier = Modifier.weight(1f)
                        )
                        if (isCompleted) {
                            Icon(
                                imageVector = NurIcons.CheckCircle2,
                                contentDescription = null,
                                tint = Color(0xFFB8924A).copy(alpha = 0.8f),
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Icon(
                                imageVector = NurIcons.ChevronRight,
                                contentDescription = null,
                                tint = hInkMuted.copy(alpha = 0.4f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Tour target indices (mirrors the LazyColumn item order) ─────────────
private fun buildSectionIndexMap(
    onboardingDismissed: Boolean,
    hasBookmark: Boolean,
    hasRecentlyRead: Boolean
): Map<String, Int> {
    var idx = 0
    idx++ // hero (continue reading)
    if (!onboardingDismissed) idx++ // onboarding card
    val stats = idx; idx++ // weekly heatmap
    if (hasBookmark) idx++ // bookmark card
    var resume = -1
    if (hasRecentlyRead) {
        resume = idx
        idx++
    }
    val browse = idx
    return mapOf(
        "stats" to stats,
        "resume" to resume,
        "browse" to browse
    )
}

