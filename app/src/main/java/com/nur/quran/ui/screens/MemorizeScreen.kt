package com.nur.quran.ui.screens

import android.content.Context
import androidx.compose.animation.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nur.quran.data.JUZ_STARTS
import com.nur.quran.data.db.entities.ChapterEntity
import com.nur.quran.data.hifdh.FsrsState
import com.nur.quran.data.hifdh.HifdhHistoryEntry
import com.nur.quran.ui.components.HifdhBreakdownModal
import com.nur.quran.ui.components.HifdhGoalModal
import com.nur.quran.ui.components.HifdhTestModal
import com.nur.quran.ui.components.NurIcons
import com.nur.quran.ui.components.SettingsDrawer
import com.nur.quran.ui.viewmodels.HomeUiState
import com.nur.quran.ui.viewmodels.HomeViewModel
import com.nur.quran.ui.viewmodels.PackViewModel
import com.nur.quran.ui.viewmodels.SurahViewModel

private data class QueueData(
    val sabaqAll: List<String>,
    val sabaqDue: List<String>,
    val sabqiAll: List<String>,
    val sabqiDue: List<String>,
    val manzilAll: List<String>,
    val manzilDue: List<String>
)

private data class JuzProgress(
    val juzNum: Int,
    val totalAyahs: Int,
    val memAyahs: Int,
    val startChapterId: Int,
    val startAyah: Int,
    val pageNumber: Int
)

@Composable
fun MemorizeScreen(
    homeViewModel: HomeViewModel,
    surahViewModel: SurahViewModel,
    onSurahClick: (Int) -> Unit
) {
    val homeState by homeViewModel.uiState.collectAsState()
    val memorizedAyahs by surahViewModel.memorizedAyahs.collectAsState()
    val hifdhHistory by surahViewModel.hifdhHistory.collectAsState()
    val hifdhGoals by surahViewModel.hifdhGoals.collectAsState()
    val readingSessions by homeViewModel.readingSessions.collectAsState()
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }
    var showMemorizedOnly by remember { mutableStateOf(false) }
    var viewMode by remember { mutableStateOf("surah") } // "surah" | "juz"
    var activeTestQueue by remember { mutableStateOf<Pair<String, List<String>>?>(null) }
    var showSettingsDrawer by remember { mutableStateOf(false) }
    var showGoalModal by remember { mutableStateOf(false) }
    var activeBreakdownModal by remember { mutableStateOf<Pair<String, List<Pair<String, String>>>?>(null) }

    val chapters = (homeState as? HomeUiState.Success)?.chapters ?: emptyList()

    // Web MemorizeIndex.jsx queue split: sabaq = new/learning/reps<3,
    // sabqi = in last memorizing chapter, manzil = the rest. Due = no card or due <= now.
    val queueData = remember(memorizedAyahs, hifdhHistory, readingSessions) {
        val lastMemChapterId = readingSessions
            .filter { it.type == "memorizing" && it.chapterId != null }
            .maxByOrNull { it.timestamp }?.chapterId
        buildQueueData(memorizedAyahs, hifdhHistory, lastMemChapterId, System.currentTimeMillis())
    }
    val totalDueToday = queueData.sabaqDue.size + queueData.sabqiDue.size + queueData.manzilDue.size

    val memorizedSurahIds = remember(memorizedAyahs, chapters) {
        chapters.filter { chapter ->
            val count = memorizedAyahs.count { it.startsWith("${chapter.id}:") }
            count >= chapter.versesCount
        }.map { it.id to it.nameSimple }
    }

    val ayahRangeSections = remember(memorizedAyahs, chapters) {
        buildAyahRangeSections(memorizedAyahs, chapters)
    }

    val juzProgressList = remember(chapters, memorizedAyahs) {
        buildJuzRanges(chapters, memorizedAyahs)
    }

    Column(modifier = Modifier.fillMaxSize().background(hWhite)) {
        // Web Top Navbar
        TopNavbar(
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

                    // Web: Continue card = last memorizing session, else last read.
                    val lastMemSession = readingSessions
                        .filter { it.type == "memorizing" && it.chapterId != null }
                        .maxByOrNull { it.timestamp }
                    val continueChapterId = lastMemSession?.chapterId ?: lastReadItem?.chapterId ?: 67
                    val continueChapter = chapters.find { it.id == continueChapterId }
                    val continueName = continueChapter?.nameSimple ?: lastReadItem?.chapterName ?: "Al-Mulk"
                    val continueMemCount = memorizedAyahs.count { it.startsWith("$continueChapterId:") }
                    val continueMemPct = if (continueChapter != null) {
                        Math.round((continueMemCount.toFloat() / continueChapter.versesCount) * 100)
                    } else 0

                    // Continue Last Session Banner Card
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 460.dp)
                            .clickable { onSurahClick(continueChapterId) }
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
                                        Text("Continue: $continueName", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White, fontFamily = fontFamilyUi)
                                        Text(
                                            text = if (lastMemSession != null) "Memorizing • $continueMemPct% memorized" else "Last practiced recently",
                                            fontSize = 11.sp,
                                            color = Color.White.copy(alpha = 0.8f),
                                            fontFamily = fontFamilyMono
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.2f))
                                        .clickable { onSurahClick(continueChapterId) },
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
                        onClick = {
                            activeBreakdownModal = "Memorized Surahs (${memorizedSurahIds.size})" to
                                memorizedSurahIds.map { (id, name) -> "$id. $name" to "Fully memorized ✓" }
                        }
                    )
                    MetricStatCard(
                        countText = "${memorizedAyahs.size}",
                        totalText = "/6236",
                        label = "AYAHS",
                        modifier = Modifier.weight(1f),
                        onClick = { activeBreakdownModal = "Memorized Ayahs (${memorizedAyahs.size})" to ayahRangeSections }
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
                            .clickable {
                                val queue = memorizedAyahs.sortedBy { key ->
                                    val p = key.split(":")
                                    (p.getOrNull(0)?.toIntOrNull() ?: 0) * 10000 + (p.getOrNull(1)?.toIntOrNull() ?: 0)
                                }
                                if (queue.isNotEmpty()) activeTestQueue = "All Memorized" to queue
                            }
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
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("REVISION QUEUES", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = hInkMuted, fontFamily = fontFamilyMono, letterSpacing = 1.5.sp)
                    Text(
                        text = "$totalDueToday ayahs due today",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (totalDueToday > 0) hGreen else hInkMuted,
                        fontFamily = fontFamilyMono
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RevisionQueueCard(
                        title = "Sabaq (New)",
                        totalCount = queueData.sabaqAll.size,
                        dueCount = queueData.sabaqDue.size,
                        bgColor = Color(0xFFF0FDF4),
                        borderColor = Color(0xFFBBF7D0),
                        textColor = Color(0xFF166534),
                        onClick = {
                            val queue = if (queueData.sabaqDue.isNotEmpty()) queueData.sabaqDue else queueData.sabaqAll
                            if (queue.isNotEmpty()) activeTestQueue = "Sabaq" to queue
                        }
                    )
                    RevisionQueueCard(
                        title = "Sabqi (Recent)",
                        totalCount = queueData.sabqiAll.size,
                        dueCount = queueData.sabqiDue.size,
                        bgColor = Color(0xFFFEFCE8),
                        borderColor = Color(0xFFFEF08A),
                        textColor = Color(0xFF854D0E),
                        onClick = {
                            val queue = if (queueData.sabqiDue.isNotEmpty()) queueData.sabqiDue else queueData.sabqiAll
                            if (queue.isNotEmpty()) activeTestQueue = "Sabqi" to queue
                        }
                    )
                    RevisionQueueCard(
                        title = "Manzil (Old)",
                        totalCount = queueData.manzilAll.size,
                        dueCount = queueData.manzilDue.size,
                        bgColor = Color(0xFFF0F9FF),
                        borderColor = Color(0xFFBAE6FD),
                        textColor = Color(0xFF075985),
                        onClick = {
                            val queue = if (queueData.manzilDue.isNotEmpty()) queueData.manzilDue else queueData.manzilAll
                            if (queue.isNotEmpty()) activeTestQueue = "Manzil" to queue
                        }
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
                            val ch = chapters.find { it.id == goal.targetId }
                            val memCount = memorizedAyahs.count { it.startsWith("${goal.targetId}:") }
                            val totalVerses = ch?.versesCount ?: 1
                            val pct = Math.round((memCount.toFloat() / totalVerses) * 100)
                            val daysLeft = goal.daysLeft

                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = hCream),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, hGold.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 18.dp, top = 14.dp, bottom = 14.dp, end = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = "Memorize ${ch?.nameSimple ?: "Surah"}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = hInk, fontFamily = fontFamilyUi)
                                        Text(
                                            text = if (daysLeft > 0) "$daysLeft days left • $memCount / $totalVerses Ayahs"
                                            else if (pct >= 100) "Completed ✓ • $memCount / $totalVerses Ayahs"
                                            else "Due now! • $memCount / $totalVerses Ayahs",
                                            fontSize = 12.sp,
                                            color = if (daysLeft == 0L && pct < 100) Color(0xFFEF4444) else hInkMuted
                                        )
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

                                    IconButton(
                                        onClick = { surahViewModel.deleteHifdhGoal(goal.id) },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(imageVector = NurIcons.X, contentDescription = "Delete goal", tint = hInkMuted, modifier = Modifier.size(15.dp))
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
                    val matchesSearch = chapter.nameSimple.contains(searchQuery, ignoreCase = true) ||
                        chapter.translatedName.contains(searchQuery, ignoreCase = true) ||
                        chapter.id.toString() == searchQuery.trim()
                    if (showMemorizedOnly) {
                        val memCount = memorizedAyahs.count { it.startsWith("${chapter.id}:") }
                        matchesSearch && memCount >= chapter.versesCount
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
                val visibleJuzs = juzProgressList.filter { jp ->
                    if (showMemorizedOnly) jp.memAyahs >= jp.totalAyahs else true
                }

                items(visibleJuzs, key = { it.juzNum }) { jp ->
                    val isMemorized = jp.memAyahs >= jp.totalAyahs
                    val memPct = if (jp.totalAyahs > 0) Math.round((jp.memAyahs.toFloat() / jp.totalAyahs) * 100) else 0

                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = hCream),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, if (isMemorized) hGreen else hBoneDark),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onSurahClick(jp.startChapterId) }
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
                                            .background(if (isMemorized) hGreen.copy(alpha = 0.15f) else hGoldSoft),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = jp.juzNum.toString(),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isMemorized) hGreen else hGold,
                                            fontFamily = fontFamilyMono
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(text = "Juz ${jp.juzNum}", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = hInk, fontFamily = fontFamilyUi)
                                            if (isMemorized) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Icon(imageVector = NurIcons.CheckCircle2, contentDescription = null, tint = hGreen, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                        Text(text = "Starts at Surah ${jp.startChapterId}:${jp.startAyah} • Page ${jp.pageNumber}", fontSize = 12.sp, color = hInkMuted)
                                    }
                                }

                                Icon(imageVector = NurIcons.ArrowRight, contentDescription = null, tint = hInkMuted, modifier = Modifier.size(16.dp))
                            }

                            if (jp.memAyahs > 0 || isMemorized) {
                                Spacer(modifier = Modifier.height(10.dp))
                                LinearProgressIndicator(
                                    progress = memPct.toFloat() / 100f,
                                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                    color = if (isMemorized) hGreen else hTeal,
                                    trackColor = hBone
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "${jp.memAyahs} / ${jp.totalAyahs} ayahs memorized ($memPct%)",
                                    fontSize = 11.sp,
                                    color = hInkMuted,
                                    fontFamily = fontFamilyMono
                                )
                            }
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
            onAddGoal = { chapterId, targetDateMillis ->
                surahViewModel.addHifdhGoal(chapterId, targetDateMillis)
            }
        )
    }

    activeBreakdownModal?.let { (modalTitle, modalSections) ->
        HifdhBreakdownModal(
            title = modalTitle,
            sections = modalSections,
            onDismiss = { activeBreakdownModal = null }
        )
    }

    activeTestQueue?.let { (queueName, queueItems) ->
        HifdhTestModal(
            testQueue = queueItems,
            queueType = queueName,
            chapters = chapters,
            onDismiss = { activeTestQueue = null },
            onLogReview = { verseKey, rating -> surahViewModel.logHifdhReview(verseKey, rating) },
            loadVerses = { keys -> surahViewModel.getVerseTexts(keys) }
        )
    }

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

private fun buildQueueData(
    memorizedAyahs: Set<String>,
    hifdhHistory: Map<String, HifdhHistoryEntry>,
    lastMemChapterId: Int?,
    now: Long
): QueueData {
    val sortedKeys = memorizedAyahs.sortedBy { key ->
        val parts = key.split(":")
        (parts.getOrNull(0)?.toIntOrNull() ?: 0) * 10000 + (parts.getOrNull(1)?.toIntOrNull() ?: 0)
    }
    val sabaqAll = mutableListOf<String>()
    val sabqiAll = mutableListOf<String>()
    val manzilAll = mutableListOf<String>()
    for (key in sortedKeys) {
        val card = hifdhHistory[key]?.card
        val isNewish = card == null ||
            card.state == FsrsState.NEW ||
            card.state == FsrsState.LEARNING ||
            card.reps < 3
        when {
            isNewish -> sabaqAll.add(key)
            lastMemChapterId != null && key.startsWith("$lastMemChapterId:") -> sabqiAll.add(key)
            else -> manzilAll.add(key)
        }
    }
    fun dueKeys(all: List<String>): List<String> = all.filter { key ->
        val card = hifdhHistory[key]?.card
        card == null || card.due <= now
    }
    return QueueData(
        sabaqAll = sabaqAll, sabaqDue = dueKeys(sabaqAll),
        sabqiAll = sabqiAll, sabqiDue = dueKeys(sabqiAll),
        manzilAll = manzilAll, manzilDue = dueKeys(manzilAll)
    )
}

/** Web: juz progress derived from JUZ_STARTS boundaries + chapter verse counts. */
private fun buildJuzRanges(chapters: List<ChapterEntity>, memorizedAyahs: Set<String>): List<JuzProgress> {
    val verseCounts = chapters.associate { it.id to it.versesCount }
    return JUZ_STARTS.mapIndexed { index, start ->
        val sc = start.verseKey.substringBefore(":").toInt()
        val sa = start.verseKey.substringAfter(":").toInt()
        val next = JUZ_STARTS.getOrNull(index + 1)
        val ec: Int
        val ea: Int
        if (next == null) {
            ec = 114
            ea = verseCounts[114] ?: 6
        } else {
            ec = next.verseKey.substringBefore(":").toInt()
            ea = next.verseKey.substringAfter(":").toInt() - 1
        }
        var total = 0
        var mem = 0
        for (c in sc..ec) {
            val vc = verseCounts[c] ?: continue
            val first = if (c == sc) sa else 1
            val last = if (c == ec) ea.coerceAtLeast(0) else vc
            if (last < first) continue
            total += last - first + 1
            mem += memorizedAyahs.count { key ->
                val parts = key.split(":")
                parts.size == 2 &&
                    parts[0].toIntOrNull() == c &&
                    (parts[1].toIntOrNull() ?: 0) in first..last
            }
        }
        JuzProgress(index + 1, total, mem, sc, sa, start.pageNumber)
    }
}

/** Web: memorized ayahs grouped per surah with verse ranges like "1-3, 5". */
private fun buildAyahRangeSections(memorizedAyahs: Set<String>, chapters: List<ChapterEntity>): List<Pair<String, String>> {
    val grouped = memorizedAyahs.groupBy { it.substringBefore(":") }
    return grouped.toSortedMap(compareBy { it.toIntOrNull() ?: 0 }).mapNotNull { (cid, keys) ->
        val chapter = chapters.find { it.id == cid.toIntOrNull() } ?: return@mapNotNull null
        val nums = keys.mapNotNull { it.substringAfter(":").toIntOrNull() }.sorted()
        if (nums.isEmpty()) return@mapNotNull null
        val ranges = mutableListOf<String>()
        var start = nums[0]
        var prev = nums[0]
        for (i in 1 until nums.size) {
            if (nums[i] == prev + 1) {
                prev = nums[i]
            } else {
                ranges.add(if (start == prev) "$start" else "$start-$prev")
                start = nums[i]
                prev = nums[i]
            }
        }
        ranges.add(if (start == prev) "$start" else "$start-$prev")
        "${chapter.id}. ${chapter.nameSimple}" to "Verses: ${ranges.joinToString(", ")} • ${nums.size} ayahs"
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
private fun RevisionQueueCard(
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
