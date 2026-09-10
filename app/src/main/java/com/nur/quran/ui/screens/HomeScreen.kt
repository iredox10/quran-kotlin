package com.nur.quran.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.nur.quran.data.db.entities.BookmarkEntity
import com.nur.quran.data.db.entities.ChapterEntity
import com.nur.quran.data.db.entities.RecentlyReadEntity
import com.nur.quran.ui.components.Coachmark
import com.nur.quran.ui.components.NurIcons
import com.nur.quran.ui.components.PageTourModal
import com.nur.quran.ui.components.ShareCardDialog
import com.nur.quran.ui.components.ShareCardType
import com.nur.quran.ui.components.TourStep
import com.nur.quran.ui.viewmodels.HomeStats
import com.nur.quran.ui.viewmodels.HomeUiState
import com.nur.quran.ui.viewmodels.HomeViewModel
import com.nur.quran.ui.viewmodels.OnboardingState
import com.nur.quran.ui.viewmodels.OnboardingTours
import com.nur.quran.ui.viewmodels.PackViewModel
import com.nur.quran.ui.viewmodels.SaukaGoal
import com.nur.quran.ui.viewmodels.SurahViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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

// ── Curated Verses of the Day (identical to the web app) ────────────────
private val DAILY_VERSES = listOf(
    DailyVerse("بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ", "\"In the name of Allah, the Most Gracious, the Most Merciful.\"", "Al-Fatiha 1:1"),
    DailyVerse("ٱهْدِنَا ٱلصِّرَٰطَ ٱلْمُسْتَقِيمَ", "\"Guide us to the straight path.\"", "Al-Fatiha 1:6"),
    DailyVerse("إِنَّ مَعَ ٱلْعُسْرِ يُسْرًا", "\"Indeed, with hardship comes ease.\"", "Ash-Sharh 94:6"),
    DailyVerse("وَلَسَوْفَ يُعْطِيكَ رَبُّكَ فَتَرْضَىٰ", "\"And your Lord is going to give you, and you will be satisfied.\"", "Ad-Duha 93:5"),
    DailyVerse("فَٱذْكُرُونِىٓ أَذْكُرْكُمْ", "\"So remember Me; I will remember you.\"", "Al-Baqarah 2:152"),
    DailyVerse("وَمَن يَتَوَكَّلْ عَلَى ٱللَّهِ فَهُوَ حَسْبُهُۥ", "\"Whoever puts their trust in Allah, He is sufficient for them.\"", "At-Talaq 65:3"),
    DailyVerse("رَبِّ ٱشْرَحْ لِى صَدْرِى", "\"My Lord, expand for me my chest.\"", "Ta-Ha 20:25"),
    DailyVerse("وَقُل رَّبِّ زِدْنِى عِلْمًا", "\"And say: My Lord, increase me in knowledge.\"", "Ta-Ha 20:114"),
    DailyVerse("إِنَّ ٱللَّهَ مَعَ ٱلصَّـٰبِرِينَ", "\"Indeed, Allah is with the patient.\"", "Al-Baqarah 2:153"),
    DailyVerse("وَنَحْنُ أَقْرَبُ إِلَيْهِ مِنْ حَبْلِ ٱلْوَرِيدِ", "\"And We are closer to him than his jugular vein.\"", "Qaf 50:16"),
    DailyVerse("فَإِنَّ ذِكْرَىٰ تَنفَعُ ٱلْمُؤْمِنِينَ", "\"And remind, for indeed, the reminder benefits the believers.\"", "Adh-Dhariyat 51:55"),
    DailyVerse("لَا يُكَلِّفُ ٱللَّهُ نَفْسًا إِلَّا وُسْعَهَا", "\"Allah does not burden a soul beyond that it can bear.\"", "Al-Baqarah 2:286")
)

data class DailyVerse(val arabic: String, val translation: String, val reference: String)

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

private const val APP_URL = "https://quran-nur.appwrite.network"
private const val SHARE_DESCRIPTION =
    "Elevate your spiritual journey with Quran Nur. Enjoy beautiful recitations, offline access, personalized reading planners, and a seamless ad-free experience. Start your daily habit today!"

// ── Guided tours (identical steps to the web app's Home.jsx) ────────────
private val HOME_TOUR_STEPS = listOf(
    TourStep(
        title = "Welcome to Quran Nur",
        description = "This is your personal companion for reading, memorizing, and studying the Quran.",
        icon = NurIcons.Sparkles
    ),
    TourStep(
        title = "Daily Progress",
        description = "Track your reading streak, daily minutes, and total hours right from the dashboard.",
        icon = NurIcons.Flame,
        target = "stats"
    ),
    TourStep(
        title = "Verse of the Day",
        description = "Start your day with a selected Ayah. You can copy or share it easily.",
        icon = NurIcons.BookOpen,
        target = "verse"
    ),
    TourStep(
        title = "Resume Reading",
        description = "Pick up exactly where you left off last time.",
        icon = NurIcons.ArrowRight,
        target = "resume"
    ),
    TourStep(
        title = "Browse the Quran",
        description = "Navigate quickly to any Surah, Juz, or Page.",
        icon = NurIcons.Search,
        target = "browse"
    ),
    TourStep(
        title = "Set a Reading Goal 📅",
        description = "Head to the Planner tab to create a personalized Khatm plan with daily assignments.",
        icon = NurIcons.CalendarDays,
        link = "planner"
    ),
    TourStep(
        title = "Start Memorizing 🧠",
        description = "Use the Memorize tab to practice Hifdh with spaced repetition and word-by-word reveal.",
        icon = NurIcons.Brain,
        link = "memorize"
    )
)

private val HOME_ADVANCED_TOUR_STEPS = listOf(
    TourStep(
        title = "Swipe Between Surahs",
        description = "While reading, try swiping left or right to quickly jump between Surahs.",
        icon = NurIcons.ArrowRight
    ),
    TourStep(
        title = "Create a Habit",
        description = "Consistency is key. Use the Planner to build a daily reading habit.",
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
    onNavigateToSauka: (() -> Unit)? = null,
    onNavigateToBookmarks: (() -> Unit)? = null,
    onNavigateToRoute: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val browseMode by viewModel.browseMode.collectAsState()
    val recentlyRead by viewModel.recentlyRead.collectAsState()
    val latestBookmark by viewModel.latestBookmark.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val activeGoals by viewModel.activeGoals.collectAsState()
    val onboardingState by viewModel.onboardingState.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val completedTours by viewModel.completedTours.collectAsState()
    val homeVisits by viewModel.homeVisits.collectAsState()
    val dismissedCoachmarks by viewModel.dismissedCoachmarks.collectAsState()
    val context = LocalContext.current

    // Page-visit analytics (feeds the tour visit thresholds, like the web app)
    LaunchedEffect(Unit) { viewModel.recordHomeVisit() }

    // Tour target tracking: each targeted section records its window rect here.
    val targetRects = remember { mutableStateMapOf<String, Rect>() }
    val lazyListState: LazyListState = rememberLazyListState()

    // Item indices of the tour-targeted sections (mirrors the LazyColumn order below).
    val sectionIndices = remember(onboardingState.isDismissed, isOnline, activeGoals, latestBookmark, recentlyRead) {
        buildSectionIndexMap(
            isOnline = isOnline,
            onboardingDismissed = onboardingState.isDismissed,
            hasActiveGoals = activeGoals.isNotEmpty(),
            hasBookmark = latestBookmark != null,
            hasRecentlyRead = recentlyRead.isNotEmpty()
        )
    }

    var shareDialogState by remember { mutableStateOf<ShareCardType?>(null) }
    var showSettingsDrawer by remember { mutableStateOf(false) }
    val greeting = remember { getGreeting() }
    val dailyVerse = remember { getDailyVerse() }
    val todayDate = remember {
        SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.ENGLISH).format(Date()).uppercase()
    }
    var copied by remember { mutableStateOf(false) }

    // Reset the copied state after 2s (same as the web app)
    LaunchedEffect(copied) {
        if (copied) {
            kotlinx.coroutines.delay(2000)
            copied = false
        }
    }

    // Entrance animation
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    shareDialogState?.let { shareType ->
        ShareCardDialog(
            type = shareType,
            onDismiss = { shareDialogState = null }
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(hWhite)) {
        // Top Header matching Web App Layout.jsx header
        WebTopNavbar(
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
                            Text(text = "Loading...", color = hInkMuted, fontSize = 14.sp)
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
                            text = "Error fetching data. Please try again later.",
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
                        filterBrowseItems(buildBrowseItems(browseMode, chapters), searchQuery)
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
                    // ─── Offline Banner ───
                    if (!isOnline) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp, bottom = 16.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.5.dp, hGold, RoundedCornerShape(12.dp))
                                    .background(hGoldSoft)
                                    .padding(horizontal = 16.dp, vertical = 13.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(hGold)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Offline Mode — Using Cached Data",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = hGold
                                )
                            }
                        }
                    }

                    // ─── Greeting Hero + Continue Reading ───
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 24.dp, bottom = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.res.painterResource(id = com.nur.quran.R.drawable.ic_logo),
                                contentDescription = "Quran Nur Logo",
                                modifier = Modifier
                                    .size(64.dp)
                                    .padding(bottom = 8.dp)
                            )
                            Text(
                                text = greeting.first,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = hInk,
                                fontFamily = fontFamilyUi
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = greeting.second,
                                fontSize = 13.sp,
                                color = hInkMuted
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = todayDate,
                                fontSize = 10.sp,
                                color = hInkMuted,
                                fontFamily = fontFamilyMono,
                                letterSpacing = 1.sp,
                                fontWeight = FontWeight.Medium
                            )

                            if (lastRead != null) {
                                Spacer(modifier = Modifier.height(20.dp))
                                ContinueReadingCard(
                                    item = lastRead,
                                    onClick = { onChapterClick(lastRead.chapterId, lastRead.verseKey) }
                                )
                            }
                        }
                    }

                    // ─── My Sauka Readings (Active Goals) ───
                    if (activeGoals.isNotEmpty()) {
                        item {
                            MySaukaReadingsSection(
                                goals = activeGoals,
                                onGoalClick = { onNavigateToSauka?.invoke() }
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                        }
                    }


                    // ─── Quick Action Mobile Shortcuts ───
                    item {
                        QuickActionGrid(
                            onNavigateToSauka = { onNavigateToSauka?.invoke() },
                            onNavigateToBookmarks = { onNavigateToBookmarks?.invoke() }
                        )
                        Spacer(modifier = Modifier.height(20.dp))
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

                    // ─── Stats Row + Share Progress ───
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { targetRects["stats"] = it.boundsInWindow() },
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatCard(
                                modifier = Modifier.weight(1f),
                                icon = NurIcons.Flame,
                                iconTint = if (stats.streak > 0) hRed else hInkMuted,
                                value = stats.streak.toString(),
                                unit = " days",
                                label = "STREAK"
                            )
                            StatCard(
                                modifier = Modifier.weight(1f),
                                icon = NurIcons.Clock,
                                iconTint = hTeal,
                                value = stats.todayMinutes.toString(),
                                unit = " min",
                                label = "TODAY"
                            )
                            StatCard(
                                modifier = Modifier.weight(1f),
                                icon = NurIcons.BarChart3,
                                iconTint = hGold,
                                value = stats.totalHours,
                                unit = " hrs",
                                label = "TOTAL"
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp, bottom = 28.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                onClick = {
                                    shareDialogState = ShareCardType.Progress(
                                        streak = stats.streak,
                                        todayMinutes = stats.todayMinutes,
                                        totalHours = stats.totalHours
                                    )
                                },
                                shape = RoundedCornerShape(20.dp),
                                color = hTeal
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = NurIcons.Share2,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Share Progress",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    // ─── Verse of the Day ───
                    item {
                        VerseOfDayCard(
                            verse = dailyVerse,
                            copied = copied,
                            coachmarkDismissed = "home-copy-verse" in dismissedCoachmarks,
                            onCoachmarkDismiss = { viewModel.dismissCoachmark("home-copy-verse") },
                            onCopy = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(
                                    ClipData.newPlainText(
                                        "Verse",
                                        "${dailyVerse.arabic}\n\n${dailyVerse.translation}\n— ${dailyVerse.reference}"
                                    )
                                )
                                copied = true
                                viewModel.dismissCoachmark("home-copy-verse")
                            },
                            onShare = {
                                shareDialogState = ShareCardType.Verse(
                                    arabic = dailyVerse.arabic,
                                    translation = dailyVerse.translation,
                                    reference = dailyVerse.reference
                                )
                            },
                            modifier = Modifier.onGloballyPositioned { targetRects["verse"] = it.boundsInWindow() }
                        )
                        Spacer(modifier = Modifier.height(28.dp))
                    }

                    // ─── Weekly Heatmap ───
                    item {
                        WeeklyHeatmapCard(stats = stats)
                        Spacer(modifier = Modifier.height(28.dp))
                    }

                    // ─── Invite Friends ───
                    item {
                        InviteFriendsCard(
                            onInvite = { shareText(context, "$SHARE_DESCRIPTION $APP_URL") }
                        )
                        Spacer(modifier = Modifier.height(28.dp))
                    }

                    // ─── Bookmark ───
                    latestBookmark?.let { bookmark ->
                        item {
                            BookmarkCard(
                                bookmark = bookmark,
                                onClick = { onChapterClick(bookmark.chapterId, bookmark.verseKey) }
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
                                    Text(
                                        text = "Recently Read",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = hInk,
                                        fontFamily = fontFamilyUi
                                    )
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
                                        text = "Browse the Quran",
                                        fontSize = 21.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = hInk,
                                        fontFamily = fontFamilyUi
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Select a Surah, Page, Juz, or Hizb to begin.",
                                    fontSize = 13.sp,
                                    color = hInkMuted
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                // Mode pills
                                val modes = listOf(
                                    Triple("surah", "Surah", NurIcons.BookOpen),
                                    Triple("page", "Page", NurIcons.Rows3),
                                    Triple("juz", "Juz", NurIcons.LibraryBig),
                                    Triple("hizb", "Hizb", NurIcons.Layers3)
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    modes.forEach { (mode, label, icon) ->
                                        val isSelected = browseMode == mode
                                        Surface(
                                            onClick = { viewModel.setBrowseMode(mode) },
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
                                    "Search $browseMode...",
                                    color = hInkMuted,
                                    fontSize = 15.sp
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    NurIcons.Search,
                                    contentDescription = "Search",
                                    tint = hInkMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = hTeal,
                                unfocusedBorderColor = hBoneDark,
                                focusedContainerColor = hCream,
                                unfocusedContainerColor = hCream,
                                cursorColor = hTeal
                            ),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Browse items
                    items(browseItems, key = { it.key }) { item ->
                        BrowseItemCard(
                            item = item,
                            onClick = {
                                when {
                                    item.chapterId != null -> onChapterClick(item.chapterId, null)
                                    item.pageNumber != null -> onPageClick(item.pageNumber)
                                }
                            }
                        )
                    }
                    if (browseItems.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No results matching your search.",
                                    fontSize = 14.sp,
                                    fontStyle = FontStyle.Italic,
                                    color = hInkMuted
                                )
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
        steps = HOME_TOUR_STEPS,
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
        steps = HOME_ADVANCED_TOUR_STEPS,
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
fun WebTopNavbar(
    title: String = "Quran Nur",
    subTitle: String? = null,
    onThemeToggle: () -> Unit,
    onSettingsClick: () -> Unit
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
                // Left Brand Logo & Title
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = com.nur.quran.R.drawable.ic_logo),
                        contentDescription = "Quran Nur Logo",
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
                            contentDescription = "Toggle Theme",
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
                            contentDescription = "Settings",
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
        modifier = Modifier.widthIn(max = 420.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .background(Brush.verticalGradient(listOf(hTeal, hTealMid)))
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
                    text = "Continue: ${item.chapterName}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    fontFamily = fontFamilyUi,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                val verseLabel = item.verseKey?.split(":")?.getOrNull(1)?.let { "Verse $it" }
                    ?: "From the beginning"
                Text(
                    text = "$verseLabel · ${timeAgo(item.timestamp)}",
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

// ── Stat Card (bordered, cream bg, matching web) ────────────────────────
@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconTint: Color,
    value: String,
    unit: String,
    label: String
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = hCream,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, hBoneDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 13.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = value,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = hInk,
                    fontFamily = fontFamilyUi,
                    lineHeight = 29.sp
                )
                Text(
                    text = unit,
                    fontSize = 11.sp,
                    color = hInkMuted,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 9.sp,
                color = hInkMuted,
                fontFamily = fontFamilyMono,
                letterSpacing = 1.sp
            )
        }
    }
}

// ── Verse of the Day Card ───────────────────────────────────────────────
@Composable
private fun VerseOfDayCard(
    verse: DailyVerse,
    copied: Boolean,
    coachmarkDismissed: Boolean,
    onCoachmarkDismiss: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = hCream,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, hBoneDark)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Bismillah watermark
            Text(
                text = "﷽",
                fontSize = 96.sp,
                color = hGold.copy(alpha = 0.03f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 10.dp, y = (-16).dp)
            )

            Column(modifier = Modifier.padding(24.dp)) {
                // Label (left-aligned, matching web)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = NurIcons.Sparkles,
                        contentDescription = null,
                        tint = hGold,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "VERSE OF THE DAY",
                        fontSize = 10.sp,
                        color = hGold,
                        fontFamily = fontFamilyMono,
                        letterSpacing = 1.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Arabic text
                Text(
                    text = verse.arabic,
                    fontFamily = fontFamilyArabic,
                    fontSize = 26.sp,
                    color = hInk,
                    textAlign = TextAlign.Center,
                    lineHeight = 56.sp,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Translation
                Text(
                    text = verse.translation,
                    fontFamily = fontFamilyBody,
                    fontSize = 14.sp,
                    color = hInkMid,
                    fontStyle = FontStyle.Italic,
                    textAlign = TextAlign.Center,
                    lineHeight = 23.sp,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Reference
                Text(
                    text = "— ${verse.reference}",
                    fontSize = 11.sp,
                    color = hInkMuted,
                    fontFamily = fontFamilyMono,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Copy button (wrapped in a Coachmark hint, like the web app)
                    Coachmark(
                        id = "home-copy-verse",
                        label = "Share the Ayah",
                        isDismissed = coachmarkDismissed,
                        onDismiss = onCoachmarkDismiss
                    ) {
                        Surface(
                            onClick = onCopy,
                            shape = RoundedCornerShape(20.dp),
                            color = if (copied) hGreen.copy(alpha = 0.1f) else Color.Transparent,
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                if (copied) hGreen else hBoneDark
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (copied) NurIcons.Check else NurIcons.Copy,
                                    contentDescription = null,
                                    tint = if (copied) hGreen else hInkMid,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (copied) "Copied" else "Copy",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (copied) hGreen else hInkMid
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Share button
                    Surface(
                        onClick = onShare,
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Transparent,
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, hBoneDark)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = NurIcons.Share2,
                                contentDescription = null,
                                tint = hInkMid,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Share",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = hInkMid
                            )
                        }
                    }
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
                    text = "This Week",
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
                            text = day.label,
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
                            text = "${day.mins}m",
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

// ── Invite Friends Card (teal gradient) ─────────────────────────────────
@Composable
private fun InviteFriendsCard(onInvite: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .background(Brush.horizontalGradient(listOf(hTeal, hTealMid)))
                .padding(24.dp)
        ) {
            // Decorative blurred circles (matching web)
            Box(
                modifier = Modifier
                    .size(128.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 40.dp, y = (-40).dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.05f))
            )
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .align(Alignment.BottomStart)
                    .offset(x = (-32.dp), y = 32.dp)
                    .clip(CircleShape)
                    .background(hGold.copy(alpha = 0.10f))
            )

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = NurIcons.Sparkles,
                        contentDescription = null,
                        tint = hGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Invite Friends",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = fontFamilyUi
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Inspire others to build a daily Quran habit. Share the app and grow together!",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    fontFamily = fontFamilyBody,
                    lineHeight = 21.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    onClick = onInvite,
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = NurIcons.Share2,
                            contentDescription = null,
                            tint = hTeal,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Send Invite",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = hTeal
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
                    text = "Verse $verseNum · Resume reading",
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
                    text = "Verse $verseNum",
                    fontSize = 11.sp,
                    color = hInkMuted
                )
            }
            Text(
                text = timeAgo(item.timestamp),
                fontSize = 11.sp,
                color = hInkMuted
            )
        }
    }
}

// ── Browse Item Card (unified Surah/Page/Juz/Hizb, matching web) ────────
@Composable
private fun BrowseItemCard(item: BrowseItem, onClick: () -> Unit) {
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
                            fontSize = 18.sp,
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
private fun buildBrowseItems(mode: String, chapters: List<ChapterEntity>): List<BrowseItem> {
    return when (mode) {
        "page" -> (1..MUSHAF_PAGE_COUNT).map { n ->
            BrowseItem(
                key = "page-$n",
                title = "Page $n",
                subtitle = "Mushaf page view",
                meta = "Page ${n.toString().padStart(3, '0')}",
                arabic = null,
                prefix = null,
                pageNumber = n
            )
        }
        "juz" -> JUZ_STARTS.map { juz ->
            BrowseItem(
                key = "juz-${juz.id}",
                title = "Juz ${juz.id}",
                subtitle = "Starts at ${juz.verseKey}",
                meta = "Page ${juz.pageNumber}",
                arabic = "الجزء ${juz.id}",
                prefix = juz.id,
                pageNumber = juz.pageNumber
            )
        }
        "hizb" -> HIZB_STARTS.map { hizb ->
            BrowseItem(
                key = "hizb-${hizb.id}",
                title = "Hizb ${hizb.id}",
                subtitle = "Starts at ${hizb.verseKey}",
                meta = "Page ${hizb.pageNumber}",
                arabic = "حزب ${hizb.id}",
                prefix = hizb.id,
                pageNumber = hizb.pageNumber
            )
        }
        else -> chapters.map { chapter ->
            BrowseItem(
                key = "surah-${chapter.id}",
                title = chapter.nameSimple,
                subtitle = chapter.translatedName,
                meta = "${chapter.versesCount} Ayahs",
                arabic = chapter.nameArabic,
                prefix = chapter.id,
                chapterId = chapter.id
            )
        }
    }
}

/** Mirrors the web app's search filter (title/subtitle/meta/arabic). */
private fun filterBrowseItems(items: List<BrowseItem>, query: String): List<BrowseItem> {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return items
    return items.filter { item ->
        listOfNotNull(item.title, item.subtitle, item.meta, item.arabic)
            .any { it.lowercase().contains(q) }
    }
}

private fun getGreeting(): Pair<String, String> {
    val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        h in 5..11 -> Pair("Assalamu Alaikum", "May your morning be blessed")
        h in 12..16 -> Pair("Assalamu Alaikum", "Wishing you a productive afternoon")
        h in 17..20 -> Pair("Assalamu Alaikum", "May your evening be peaceful")
        else -> Pair("Assalamu Alaikum", "May your night be filled with barakah")
    }
}

private fun getDailyVerse(): DailyVerse {
    // Same day-of-year rotation as the web app (Jan 1 → index 1)
    val day = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
    return DAILY_VERSES[day % DAILY_VERSES.size]
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

private fun shareText(context: Context, text: String) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, text)
        type = "text/plain"
    }
    context.startActivity(Intent.createChooser(sendIntent, null))
}


// ── Quick Action Mobile Shortcuts (2-Column Grid) ───────────────────────
@Composable
private fun QuickActionGrid(
    onNavigateToSauka: () -> Unit,
    onNavigateToBookmarks: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Sauka Groups Card
        Surface(
            onClick = onNavigateToSauka,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(18.dp),
            color = hWhite,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, hBoneDark)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(hTealSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = NurIcons.Users,
                        contentDescription = null,
                        tint = hTeal,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Sauka Groups",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = hInk,
                    fontFamily = fontFamilyUi
                )
            }
        }

        // Bookmarks Card
        Surface(
            onClick = onNavigateToBookmarks,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(18.dp),
            color = hWhite,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, hBoneDark)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(hInk.copy(alpha = 0.05f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = NurIcons.Bookmark,
                        contentDescription = null,
                        tint = hInk,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Bookmarks",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = hInk,
                    fontFamily = fontFamilyUi
                )
            }
        }
    }
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
                        contentDescription = "Dismiss",
                        tint = hInkMuted,
                        modifier = Modifier.size(14.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Welcome to Quran Nur",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = hInk,
                    fontFamily = fontFamilyUi
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${state.completedSteps} of ${state.totalSteps} steps completed",
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

// ── My Sauka Readings (Active Goals) Widget ─────────────────────────────
@Composable
private fun MySaukaReadingsSection(
    goals: List<SaukaGoal>,
    onGoalClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = NurIcons.Users,
                contentDescription = null,
                tint = hGold,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "My Sauka Readings",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = hInk,
                fontFamily = fontFamilyUi
            )
        }
        goals.forEach { goal ->
            Surface(
                onClick = onGoalClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                shape = RoundedCornerShape(20.dp),
                color = hGold.copy(alpha = 0.05f),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, hGold)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = goal.groupTitle.uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = hGold,
                            fontFamily = fontFamilyMono,
                            letterSpacing = 0.8.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${goal.divisionType} ${goal.partNumber}".replaceFirstChar { it.uppercase() },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = hInk,
                            fontFamily = fontFamilyUi,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = hGold
                    ) {
                        Row(
                            modifier = Modifier
                                .height(40.dp)
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Read",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Icon(
                                imageVector = NurIcons.ArrowRight,
                                contentDescription = null,
                                tint = Color.White,
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
    isOnline: Boolean,
    onboardingDismissed: Boolean,
    hasActiveGoals: Boolean,
    hasBookmark: Boolean,
    hasRecentlyRead: Boolean
): Map<String, Int> {
    var idx = 0
    if (!isOnline) idx++ // offline banner
    idx++ // greeting hero
    if (hasActiveGoals) idx++ // my sauks
    idx++ // quick actions
    if (!onboardingDismissed) idx++ // onboarding card
    val stats = idx; idx++
    val verse = idx; idx++
    idx++ // weekly heatmap
    idx++ // invite friends
    if (hasBookmark) idx++ // bookmark card
    var resume = -1
    if (hasRecentlyRead) {
        resume = idx
        idx++
    }
    val browse = idx
    return mapOf(
        "stats" to stats,
        "verse" to verse,
        "resume" to resume,
        "browse" to browse
    )
}

