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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.R
import com.nur.quran.data.HIZB_STARTS
import com.nur.quran.data.JUZ_STARTS
import com.nur.quran.data.MUSHAF_PAGE_COUNT
import com.nur.quran.data.db.entities.BookmarkEntity
import com.nur.quran.data.db.entities.ChapterEntity
import com.nur.quran.data.db.entities.RecentlyReadEntity
import com.nur.quran.ui.components.NurIcons
import com.nur.quran.ui.viewmodels.HomeStats
import com.nur.quran.ui.viewmodels.HomeUiState
import com.nur.quran.ui.viewmodels.HomeViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// ── Google Fonts Setup ──────────────────────────────────────────────────
private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private val cormorantGaramondFont = GoogleFont("Cormorant Garamond")
private val piazzollaFont = GoogleFont("Piazzolla")
private val geistMonoFont = GoogleFont("Geist Mono")
private val scheherazadeNewFont = GoogleFont("Scheherazade New")

val fontFamilyUi = FontFamily(
    Font(googleFont = cormorantGaramondFont, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = cormorantGaramondFont, fontProvider = provider, weight = FontWeight.Bold),
    Font(googleFont = cormorantGaramondFont, fontProvider = provider, weight = FontWeight.SemiBold)
)

val fontFamilyBody = FontFamily(
    Font(googleFont = piazzollaFont, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = piazzollaFont, fontProvider = provider, weight = FontWeight.Bold)
)

val fontFamilyMono = FontFamily(
    Font(googleFont = geistMonoFont, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = geistMonoFont, fontProvider = provider, weight = FontWeight.Bold),
    Font(googleFont = geistMonoFont, fontProvider = provider, weight = FontWeight.Medium)
)

val fontFamilyArabic = FontFamily(
    Font(googleFont = scheherazadeNewFont, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = scheherazadeNewFont, fontProvider = provider, weight = FontWeight.Bold)
)

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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onChapterClick: (Int) -> Unit,
    onPageClick: (Int) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val browseMode by viewModel.browseMode.collectAsState()
    val recentlyRead by viewModel.recentlyRead.collectAsState()
    val latestBookmark by viewModel.latestBookmark.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val context = LocalContext.current

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

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn() + slideInVertically(initialOffsetY = { 20 })
            ) {
                LazyColumn(
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
                                    onClick = { onChapterClick(lastRead.chapterId) }
                                )
                            }
                        }
                    }

                    // ─── Stats Row + Share Progress ───
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
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
                                    shareText(
                                        context,
                                        "My Quran reading progress on Quran Nur: " +
                                            "${stats.streak} day streak · ${stats.todayMinutes} min today · " +
                                            "${stats.totalHours} hrs total. $APP_URL"
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
                            onCopy = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(
                                    ClipData.newPlainText(
                                        "Verse",
                                        "${dailyVerse.arabic}\n\n${dailyVerse.translation}\n— ${dailyVerse.reference}"
                                    )
                                )
                                copied = true
                            },
                            onShare = {
                                shareText(
                                    context,
                                    "${dailyVerse.arabic}\n\n${dailyVerse.translation}\n— ${dailyVerse.reference}"
                                )
                            }
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
                                onClick = { onChapterClick(bookmark.chapterId) }
                            )
                            Spacer(modifier = Modifier.height(28.dp))
                        }
                    }

                    // ─── Recently Read ───
                    if (recentlyRead.isNotEmpty()) {
                        item {
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
                                        onClick = { onChapterClick(item.chapterId) }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }

                    // ─── Browse the Quran ───
                    item {
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
                                    item.chapterId != null -> onChapterClick(item.chapterId)
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
    onCopy: () -> Unit,
    onShare: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
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
                    // Copy button
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
