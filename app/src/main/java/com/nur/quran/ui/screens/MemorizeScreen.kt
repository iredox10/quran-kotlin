package com.nur.quran.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.data.JUZ_STARTS
import com.nur.quran.data.db.entities.ChapterEntity
import com.nur.quran.ui.components.HifdhBreakdownModal
import com.nur.quran.ui.components.HifdhGoalModal
import com.nur.quran.ui.components.HifdhTestModal
import com.nur.quran.ui.components.NurIcons
import com.nur.quran.ui.components.SettingsDrawer
import com.nur.quran.ui.viewmodels.HomeUiState
import com.nur.quran.ui.viewmodels.HomeViewModel
import com.nur.quran.ui.viewmodels.SurahViewModel

data class HifdhGoalItem(
    val id: String,
    val chapterId: Int,
    val targetDays: Int
)

@Composable
fun MemorizeScreen(
    homeViewModel: HomeViewModel,
    surahViewModel: SurahViewModel,
    onSurahClick: (Int) -> Unit
) {
    val homeState by homeViewModel.uiState.collectAsState()
    val memorizedAyahs by surahViewModel.memorizedAyahs.collectAsState()
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }
    var showMemorizedOnly by remember { mutableStateOf(false) }
    var viewMode by remember { mutableStateOf("surah") } // "surah" | "juz"
    var activeTestQueue by remember { mutableStateOf<Pair<String, List<String>>?>(null) }
    var showSettingsDrawer by remember { mutableStateOf(false) }
    var showGoalModal by remember { mutableStateOf(false) }
    var activeBreakdownModal by remember { mutableStateOf<Pair<String, List<String>>?>(null) }

    val hifdhGoals = remember { mutableStateListOf<HifdhGoalItem>() }
    val chapters = (homeState as? HomeUiState.Success)?.chapters ?: emptyList()

    val (sabaqList, sabqiList, manzilList) = remember(memorizedAyahs) {
        val ayahs = memorizedAyahs.toList()
        val sabaq = ayahs.takeLast(10)
        val sabqi = ayahs.dropLast(10).takeLast(30)
        val manzil = ayahs.dropLast(40)
        Triple(sabaq, sabqi, manzil)
    }

    val memorizedSurahIds = remember(memorizedAyahs, chapters) {
        chapters.filter { chapter ->
            val count = memorizedAyahs.count { it.startsWith("${chapter.id}:") }
            count >= chapter.versesCount
        }.map { "${it.id}:${it.nameSimple}" }
    }

    Column(modifier = Modifier.fillMaxSize().background(hWhite)) {
        // Web Top Navbar
        WebTopNavbar(
            title = "Quran Nur",
            subTitle = "Hifdh",
            onThemeToggle = {
                isDarkThemeGlobal = !isDarkThemeGlobal
                context.getSharedPreferences("Settings", Context.MODE_PRIVATE)
                    .edit().putBoolean("is_dark_theme", isDarkThemeGlobal).apply()
            },
            onSettingsClick = { showSettingsDrawer = true }
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp)
        ) {
            // ── Header Title & Subtitle (Web MemorizeIndex.jsx) ──
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "MEMORIZATION HUB",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = hInkMuted,
                        fontFamily = fontFamilyMono,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Hifdh Tracker",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = hInk,
                        fontFamily = fontFamilyUi
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val recentlyRead by homeViewModel.recentlyRead.collectAsState()
                    val lastReadItem = recentlyRead.firstOrNull()
                    val lastSurahId = lastReadItem?.chapterId ?: 67
                    val lastSurahName = lastReadItem?.chapterName ?: "Al-Mulk"

                    // Continue Last Session Banner Card
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 460.dp)
                            .clickable { onSurahClick(lastSurahId) }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Brush.horizontalGradient(listOf(hTeal, hTealMid)))
                                .padding(18.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.White.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(imageVector = NurIcons.BookOpen, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text("Continue: $lastSurahName", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White, fontFamily = fontFamilyUi)
                                        Text("Last practiced recently", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f), fontFamily = fontFamilyMono)
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.2f))
                                        .clickable { onSurahClick(lastSurahId) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(imageVector = NurIcons.PlayFilled, contentDescription = "Play", tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            // ── Top 3 Metric Cards Row (Surahs / Ayahs / Progress %) ──
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricStatCard(
                        countText = "${memorizedSurahIds.size}",
                        totalText = "/114",
                        label = "SURAHS",
                        modifier = Modifier.weight(1f),
                        onClick = { activeBreakdownModal = "Memorized Surahs" to memorizedSurahIds }
                    )
                    MetricStatCard(
                        countText = "${memorizedAyahs.size}",
                        totalText = "/6236",
                        label = "AYAHS",
                        modifier = Modifier.weight(1f),
                        onClick = { activeBreakdownModal = "Memorized Ayahs" to memorizedAyahs.toList() }
                    )
                    MetricStatCard(
                        countText = "${if (memorizedAyahs.isNotEmpty()) Math.round((memorizedAyahs.size.toFloat() / 6236f) * 100) else 0}",
                        totalText = "%",
                        label = "PROGRESS",
                        modifier = Modifier.weight(1f),
                        onClick = {}
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // ── Two Large Quick Action Cards (Test My Hifdh + Set Goal) ──
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Test My Hifdh Card (Teal Gradient Background)
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = hTeal),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(130.dp)
                            .clickable { activeTestQueue = "All Memorized" to memorizedAyahs.toList() }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = NurIcons.Brain, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Test My Hifdh", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = fontFamilyUi)
                            Text("Review mistakes", fontSize = 10.sp, color = Color.White.copy(alpha = 0.75f), fontFamily = fontFamilyMono)
                        }
                    }

                    // Set Goal Card (White Card with Gold Hover Accent)
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = hCream),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, hBoneDark),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(130.dp)
                            .clickable { showGoalModal = true }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(hBone),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = NurIcons.Sparkles, contentDescription = null, tint = hInkMid, modifier = Modifier.size(24.dp))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Set Goal", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = hInk, fontFamily = fontFamilyUi)
                            Text("Plan memorization", fontSize = 10.sp, color = hInkMuted, fontFamily = fontFamilyMono)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // ── 3 Revision Queue Cards (Sabaq, Sabqi, Manzil) matching Web colors ──
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    WebRevisionQueueCard(
                        title = "Sabaq (New)",
                        totalCount = sabaqList.size,
                        dueCount = sabaqList.size,
                        bgColor = Color(0xFFF0FDF4),
                        borderColor = Color(0xFFBBF7D0),
                        textColor = Color(0xFF166534),
                        onClick = { activeTestQueue = "Sabaq" to sabaqList }
                    )
                    WebRevisionQueueCard(
                        title = "Sabqi (Recent)",
                        totalCount = sabqiList.size,
                        dueCount = sabqiList.size,
                        bgColor = Color(0xFFFEFCE8),
                        borderColor = Color(0xFFFEF08A),
                        textColor = Color(0xFF854D0E),
                        onClick = { activeTestQueue = "Sabqi" to sabqiList }
                    )
                    WebRevisionQueueCard(
                        title = "Manzil (Old)",
                        totalCount = manzilList.size,
                        dueCount = manzilList.size,
                        bgColor = Color(0xFFF0F9FF),
                        borderColor = Color(0xFFBAE6FD),
                        textColor = Color(0xFF075985),
                        onClick = { activeTestQueue = "Manzil" to manzilList }
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))
            }

            // ── Active Goals Section ──
            if (hifdhGoals.isNotEmpty()) {
                item {
                    Text(
                        text = "Active Goals",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = hInk,
                        fontFamily = fontFamilyUi,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        hifdhGoals.forEach { goal ->
                            val ch = chapters.find { it.id == goal.chapterId }
                            val memCount = memorizedAyahs.count { it.startsWith("${goal.chapterId}:") }
                            val totalVerses = ch?.versesCount ?: 1
                            val pct = Math.round((memCount.toFloat() / totalVerses) * 100)

                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = hCream),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, hGold.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(18.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(text = "Memorize ${ch?.nameSimple ?: "Surah"}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = hInk, fontFamily = fontFamilyUi)
                                        Text(text = "${goal.targetDays} days left • $memCount / $totalVerses Ayahs", fontSize = 12.sp, color = hInkMuted)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(hGoldSoft),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = "$pct%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = hGold, fontFamily = fontFamilyMono)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // ── View Mode Tabs & Search Filter ──
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(hBone)
                            .padding(3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (viewMode == "surah") hWhite else Color.Transparent)
                                .clickable { viewMode = "surah" }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text("Surahs", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (viewMode == "surah") hInk else hInkMuted)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (viewMode == "juz") hWhite else Color.Transparent)
                                .clickable { viewMode = "juz" }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text("Ajzaa (Juz)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (viewMode == "juz") hInk else hInkMuted)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search...", fontSize = 14.sp, color = hInkMuted) },
                        leadingIcon = { Icon(imageVector = NurIcons.Search, contentDescription = null, tint = hInkMuted, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = hTeal,
                            unfocusedBorderColor = hBoneDark,
                            focusedContainerColor = hCream,
                            unfocusedContainerColor = hCream
                        ),
                        singleLine = true
                    )

                    Surface(
                        onClick = { showMemorizedOnly = !showMemorizedOnly },
                        shape = RoundedCornerShape(16.dp),
                        color = if (showMemorizedOnly) hGreen.copy(alpha = 0.15f) else hCream,
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, if (showMemorizedOnly) hGreen else hBoneDark),
                        modifier = Modifier.height(52.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = NurIcons.Award, contentDescription = null, tint = if (showMemorizedOnly) hGreen else hInkMid, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (showMemorizedOnly) "Memorized Only" else "Filter",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (showMemorizedOnly) hGreen else hInkMid
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
            }

            // ── Surah or Juz Items Display ──
            if (viewMode == "surah") {
                val filteredChapters = chapters.filter { chapter ->
                    val matchesSearch = chapter.nameSimple.contains(searchQuery, ignoreCase = true) || chapter.id.toString() == searchQuery.trim()
                    if (showMemorizedOnly) {
                        val memCount = memorizedAyahs.count { it.startsWith("${chapter.id}:") }
                        matchesSearch && memCount > 0
                    } else matchesSearch
                }

                items(filteredChapters, key = { it.id }) { chapter ->
                    val memCount = memorizedAyahs.count { it.startsWith("${chapter.id}:") }
                    val isMemorized = memCount >= chapter.versesCount
                    val memPct = Math.round((memCount.toFloat() / chapter.versesCount) * 100)

                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = hCream),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, if (isMemorized) hGreen else hBoneDark),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onSurahClick(chapter.id) }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isMemorized) hGreen.copy(alpha = 0.15f) else hBone),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = chapter.id.toString(),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isMemorized) hGreen else hInkMid,
                                            fontFamily = fontFamilyMono
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(text = chapter.nameSimple, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = hInk, fontFamily = fontFamilyUi)
                                            if (isMemorized) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Icon(imageVector = NurIcons.CheckCircle2, contentDescription = null, tint = hGreen, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                        Text(text = "${chapter.versesCount} Ayahs", fontSize = 12.sp, color = hInkMuted)
                                    }
                                }

                                IconButton(
                                    onClick = { onSurahClick(chapter.id) },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(hBone)
                                ) {
                                    Icon(imageVector = NurIcons.Play, contentDescription = "Play", tint = hInkMuted, modifier = Modifier.size(16.dp))
                                }
                            }

                            if (isMemorized || memCount > 0) {
                                Spacer(modifier = Modifier.height(10.dp))
                                LinearProgressIndicator(
                                    progress = (if (isMemorized) 100 else memPct).toFloat() / 100f,
                                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                    color = if (isMemorized) hGreen else hTeal,
                                    trackColor = hBone
                                )
                            }
                        }
                    }
                }
            } else {
                items((1..30).toList(), key = { it }) { juzNum ->
                    val juzInfo = JUZ_STARTS.getOrNull(juzNum - 1)
                    val startSurahId = juzInfo?.verseKey?.split(":")?.getOrNull(0)?.toIntOrNull() ?: 1
                    val startAyahNum = juzInfo?.verseKey?.split(":")?.getOrNull(1)?.toIntOrNull() ?: 1

                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = hCream),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, hBoneDark),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onSurahClick(startSurahId) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(hGoldSoft),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = juzNum.toString(),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = hGold,
                                        fontFamily = fontFamilyMono
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(text = "Juz $juzNum", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = hInk, fontFamily = fontFamilyUi)
                                    Text(text = "Starts at Surah $startSurahId:$startAyahNum", fontSize = 12.sp, color = hInkMuted)
                                }
                            }

                            Icon(imageVector = NurIcons.ArrowRight, contentDescription = null, tint = hInkMuted, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(96.dp)) }
        }
    }

    // Modal Overlays
    if (showGoalModal) {
        HifdhGoalModal(
            chapters = chapters,
            onDismiss = { showGoalModal = false },
            onAddGoal = { chapterId, days ->
                hifdhGoals.add(HifdhGoalItem(id = System.currentTimeMillis().toString(), chapterId = chapterId, targetDays = days))
            }
        )
    }

    activeBreakdownModal?.let { (modalTitle, modalItems) ->
        HifdhBreakdownModal(
            title = modalTitle,
            items = modalItems,
            chapters = chapters,
            onDismiss = { activeBreakdownModal = null }
        )
    }

    activeTestQueue?.let { (queueName, queueItems) ->
        HifdhTestModal(
            testQueue = queueItems,
            queueType = queueName,
            chapters = chapters,
            onDismiss = { activeTestQueue = null },
            onLogReview = { _, _ -> }
        )
    }

    if (showSettingsDrawer) {
        SettingsDrawer(
            viewModel = surahViewModel,
            onDismiss = { showSettingsDrawer = false }
        )
    }
}

@Composable
private fun MetricStatCard(
    countText: String,
    totalText: String,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = hCream),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, hBoneDark),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(text = countText, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = hInk, fontFamily = fontFamilyUi)
                Text(text = totalText, fontSize = 12.sp, color = hInkMuted, modifier = Modifier.padding(bottom = 2.dp))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = hInkMuted, fontFamily = fontFamilyMono, letterSpacing = 1.sp)
        }
    }
}

@Composable
private fun WebRevisionQueueCard(
    title: String,
    totalCount: Int,
    dueCount: Int,
    bgColor: Color,
    borderColor: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, borderColor),
        modifier = Modifier
            .width(150.dp)
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textColor, fontFamily = fontFamilyUi)
                Text(text = "$dueCount", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor, fontFamily = fontFamilyMono)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "$totalCount TOTAL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textColor.copy(alpha = 0.7f), fontFamily = fontFamilyMono)
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth().height(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = "Review Due", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor)
                }
            }
        }
    }
}
