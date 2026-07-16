package com.nur.quran.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.data.db.entities.ChapterEntity
import com.nur.quran.ui.viewmodels.HomeUiState
import com.nur.quran.ui.viewmodels.HomeViewModel
import java.text.SimpleDateFormat
import java.util.*

// ── Color Palette (matching web CSS variables) ──────────────────────────
private val hCream     = Color(0xFFFAF7F0)
private val hBone      = Color(0xFFEDE8DA)
private val hBoneDark  = Color(0xFFDDD7C7)
private val hInk       = Color(0xFF2B3F3C)
private val hInkMid    = Color(0xFF4D5F5C)
private val hInkMuted  = Color(0xFF8E9B97)
private val hGold      = Color(0xFFB8924A)
private val hGoldSoft  = Color(0x2EB8924A) // ~18% opacity
private val hTeal      = Color(0xFF2E4F4A)
private val hTealMid   = Color(0xFF3D6560)
private val hTealSoft  = Color(0x142E4F4A) // ~8% opacity
private val hWhite     = Color(0xFFFAFAF5)
private val hGreen     = Color(0xFF10B981)
private val hRed       = Color(0xFFEF4444)

// ── Daily Verses (rotated by day of year, matches web) ──────────────────
private val DAILY_VERSES = listOf(
    DailyVerse("بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ", "\"In the name of Allah, the Most Gracious, the Most Merciful.\"", "Al-Fatiha 1:1"),
    DailyVerse("ٱهْدِنَا ٱلصِّرَٰطَ ٱلْمُسْتَقِيمَ", "\"Guide us to the straight path.\"", "Al-Fatiha 1:6"),
    DailyVerse("إِنَّ مَعَ ٱلْعُسْرِ يُسْرًا", "\"Indeed, with hardship comes ease.\"", "Ash-Sharh 94:6"),
    DailyVerse("وَلَسَوْفَ يُعْطِيكَ رَبُّكَ فَتَرْضَىٰ", "\"And your Lord is going to give you, and you will be satisfied.\"", "Ad-Duha 93:5"),
    DailyVerse("فَٱذْكُرُونِىٓ أَذْكُرْكُمْ", "\"So remember Me; I will remember you.\"", "Al-Baqarah 2:152"),
    DailyVerse("وَمَن يَتَوَكَّلْ عَلَى ٱللَّهِ فَهُوَ حَسْبُهُۥ", "\"Whoever puts their trust in Allah, He is sufficient for them.\"", "At-Talaq 65:3"),
    DailyVerse("رَبِّ ٱشْرَحْ لِى صَدْرِى", "\"My Lord, expand for me my breast [with assurance].\"", "Ta-Ha 20:25"),
    DailyVerse("وَقُل رَّبِّ زِدْنِى عِلْمًا", "\"And say, 'My Lord, increase me in knowledge.'\"", "Ta-Ha 20:114"),
    DailyVerse("إِنَّ ٱللَّهَ مَعَ ٱلصَّـٰبِرِينَ", "\"Indeed, Allah is with the patient.\"", "Al-Baqarah 2:153"),
    DailyVerse("وَلَا تَيْـَٔسُوا۟ مِن رَّوْحِ ٱللَّهِ", "\"And do not despair of relief from Allah.\"", "Yusuf 12:87"),
    DailyVerse("حَسْبُنَا ٱللَّهُ وَنِعْمَ ٱلْوَكِيلُ", "\"Sufficient for us is Allah, and [He is] the best Disposer of affairs.\"", "Ali 'Imran 3:173"),
    DailyVerse("وَهُوَ مَعَكُمْ أَيْنَ مَا كُنتُمْ", "\"And He is with you wherever you are.\"", "Al-Hadid 57:4")
)

data class DailyVerse(val arabic: String, val translation: String, val reference: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onChapterClick: (Int) -> Unit,
    onPageClick: (Int) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val browseMode by viewModel.browseMode.collectAsState()
    val context = LocalContext.current

    val greeting = remember { getGreeting() }
    val dailyVerse = remember { getDailyVerse() }
    val todayDate = remember {
        val sdf = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.ENGLISH)
        sdf.format(Date()).uppercase()
    }
    var copied by remember { mutableStateOf(false) }

    // Entrance animation
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(hWhite),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // ═══════════════════════════════════════════════════════════
        // SECTION A: Greeting Hero (centered)
        // ═══════════════════════════════════════════════════════════
        item {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn() + slideInVertically(initialOffsetY = { 30 })
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp, bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = greeting.first,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = hInk,
                        fontFamily = FontFamily.Serif
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = greeting.second,
                        fontSize = 13.sp,
                        color = hInkMuted
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = todayDate,
                        fontSize = 10.sp,
                        color = hInkMuted,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // ═══════════════════════════════════════════════════════════
        // SECTION B: Stats Row (3 bordered cards)
        // ═══════════════════════════════════════════════════════════
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Streak
                StatCard(
                    modifier = Modifier.weight(1f),
                    emoji = "🔥",
                    value = "5",
                    unit = "days",
                    label = "STREAK",
                    valueColor = hRed
                )
                // Today
                StatCard(
                    modifier = Modifier.weight(1f),
                    emoji = "⏱",
                    value = "15",
                    unit = "min",
                    label = "TODAY",
                    valueColor = hTeal
                )
                // Total
                StatCard(
                    modifier = Modifier.weight(1f),
                    emoji = "📊",
                    value = "4.2",
                    unit = "hrs",
                    label = "TOTAL",
                    valueColor = hGold
                )
            }
        }

        // ═══════════════════════════════════════════════════════════
        // SECTION C: Verse of the Day
        // ═══════════════════════════════════════════════════════════
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = hCream),
                border = CardDefaults.outlinedCardBorder().copy(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(listOf(hBoneDark, hBoneDark))
                )
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    // Bismillah watermark
                    Text(
                        text = "﷽",
                        fontSize = 80.sp,
                        color = hGold.copy(alpha = 0.03f),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 10.dp, y = (-10).dp)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Label
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(text = "✨", fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "VERSE OF THE DAY",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = hGold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.5.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Arabic text
                        Text(
                            text = dailyVerse.arabic,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = hInk,
                            textAlign = TextAlign.Center,
                            lineHeight = 48.sp,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Translation
                        Text(
                            text = dailyVerse.translation,
                            fontSize = 14.sp,
                            color = hInkMid,
                            fontStyle = FontStyle.Italic,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Reference
                        Text(
                            text = dailyVerse.reference,
                            fontSize = 11.sp,
                            color = hInkMuted,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Action buttons
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Copy button
                            OutlinedButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Verse", "${dailyVerse.arabic}\n${dailyVerse.translation} — ${dailyVerse.reference}")
                                    clipboard.setPrimaryClip(clip)
                                    copied = true
                                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(20.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.5.dp,
                                    if (copied) hGreen else hBoneDark
                                ),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (copied) hGreen.copy(alpha = 0.1f) else Color.Transparent
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = if (copied) "✓ Copied" else "📋 Copy",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (copied) hGreen else hInkMid
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Share button
                            OutlinedButton(
                                onClick = { /* Share intent */ },
                                shape = RoundedCornerShape(20.dp),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, hBoneDark),
                                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "📤 Share",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = hInkMid
                                )
                            }
                        }
                    }
                }
            }
        }

        // ═══════════════════════════════════════════════════════════
        // SECTION D: Browse the Quran
        // ═══════════════════════════════════════════════════════════
        item {
            Spacer(modifier = Modifier.height(28.dp))
            // Header
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Browse the Quran",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = hInk,
                    fontFamily = FontFamily.Serif
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "114 Surahs · 604 Pages · 30 Juz",
                    fontSize = 13.sp,
                    color = hInkMuted
                )
            }
        }

        // Pill tabs
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val modes = listOf("surah" to "Surah", "page" to "Page", "juz" to "Juz")
                modes.forEach { (mode, label) ->
                    val isSelected = browseMode == mode
                    Surface(
                        onClick = { viewModel.setBrowseMode(mode) },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) hTealSoft else Color.Transparent,
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            if (isSelected) hTeal else hBoneDark
                        ),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
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
        }

        // Search
        item {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = {
                    Text(
                        "Search surahs, pages...",
                        color = hInkMuted,
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
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
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Browse list content
        when (uiState) {
            is HomeUiState.Loading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(50.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = hTeal, strokeWidth = 2.dp)
                    }
                }
            }
            is HomeUiState.Error -> {
                item {
                    Text(
                        text = (uiState as HomeUiState.Error).message,
                        color = hRed,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            is HomeUiState.Success -> {
                val chapters = (uiState as HomeUiState.Success).chapters
                val filtered = chapters.filter {
                    it.nameSimple.contains(searchQuery, ignoreCase = true) ||
                            it.id.toString() == searchQuery
                }

                if (browseMode == "surah") {
                    items(filtered) { chapter ->
                        SurahCard(
                            chapter = chapter,
                            onClick = { onChapterClick(chapter.id) }
                        )
                    }
                } else if (browseMode == "page") {
                    items((1..604).toList().filter { it.toString().contains(searchQuery) }) { pageNum ->
                        PageCard(
                            pageNumber = pageNum,
                            onClick = { onPageClick(pageNum) }
                        )
                    }
                } else {
                    items((1..30).toList().filter { it.toString().contains(searchQuery) }) { juzNum ->
                        JuzCard(
                            juzNumber = juzNum,
                            onClick = { onPageClick(getJuzStartPage(juzNum)) }
                        )
                    }
                }
            }
        }

        // Bottom spacing for floating nav bar
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

// ── Stat Card (bordered, cream bg, matching web) ──────────────────────────
@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    emoji: String,
    value: String,
    unit: String,
    label: String,
    valueColor: Color
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
                .padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = emoji, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = value,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = valueColor,
                    fontFamily = FontFamily.Serif
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = unit,
                    fontSize = 10.sp,
                    color = hInkMuted,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = hInkMuted,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
        }
    }
}

// ── Surah Card (bordered, matching web browse items) ──────────────────
@Composable
fun SurahCard(chapter: ChapterEntity, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        color = hCream,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, hBoneDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Number badge
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(hTealSoft),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = chapter.id.toString(),
                    fontWeight = FontWeight.Bold,
                    color = hTeal,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Name + meta
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = chapter.nameSimple,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = hInk,
                    fontFamily = FontFamily.Serif
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${chapter.versesCount} Ayahs",
                        fontSize = 11.sp,
                        color = hInkMuted,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // Revelation badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(hBone)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = chapter.revelationPlace.replaceFirstChar { it.uppercase() },
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = hInkMuted
                        )
                    }
                }
            }

            // Arabic name
            Text(
                text = chapter.nameArabic,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = hGold
            )
        }
    }
}

// ── Page Card ─────────────────────────────────────────────────────────
@Composable
fun PageCard(pageNumber: Int, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        color = hCream,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, hBoneDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(hTealSoft),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = pageNumber.toString(),
                    fontWeight = FontWeight.Bold,
                    color = hTeal,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Page $pageNumber",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = hInk,
                    fontFamily = FontFamily.Serif
                )
                Text(
                    text = "Mushaf Page",
                    fontSize = 11.sp,
                    color = hInkMuted,
                    fontFamily = FontFamily.Monospace
                )
            }
            Text(
                text = "صفحة $pageNumber",
                fontSize = 16.sp,
                color = hGold,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ── Juz Card ──────────────────────────────────────────────────────────
@Composable
fun JuzCard(juzNumber: Int, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        color = hCream,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, hBoneDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(hGoldSoft),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = juzNumber.toString(),
                    fontWeight = FontWeight.Bold,
                    color = hGold,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Juz $juzNumber",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = hInk,
                    fontFamily = FontFamily.Serif
                )
                Text(
                    text = "Part $juzNumber of 30",
                    fontSize = 11.sp,
                    color = hInkMuted,
                    fontFamily = FontFamily.Monospace
                )
            }
            Text(
                text = "الجزء $juzNumber",
                fontSize = 16.sp,
                color = hGold,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────
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
    val day = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
    return DAILY_VERSES[day % DAILY_VERSES.size]
}

private fun getJuzStartPage(juz: Int): Int {
    val juzPages = listOf(
        1, 22, 42, 62, 82, 102, 121, 142, 162, 182,
        201, 222, 242, 262, 282, 302, 322, 342, 362, 382,
        402, 422, 442, 462, 482, 502, 522, 542, 562, 582
    )
    return juzPages.getOrNull(juz - 1) ?: 1
}
