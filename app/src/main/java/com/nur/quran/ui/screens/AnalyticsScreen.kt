package com.nur.quran.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.data.db.entities.ReadingSessionEntity
import com.nur.quran.ui.components.NurIcons
import com.nur.quran.ui.viewmodels.HomeViewModel
import com.nur.quran.ui.viewmodels.SurahViewModel
import java.text.SimpleDateFormat
import java.util.*

data class BadgeItem(
    val icon: String,
    val title: String,
    val desc: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    homeViewModel: HomeViewModel,
    surahViewModel: SurahViewModel,
    onOpenQuranClick: () -> Unit
) {
    val context = LocalContext.current
    val sessions by homeViewModel.readingSessions.collectAsState()
    val recentlyRead by homeViewModel.recentlyRead.collectAsState()
    val bookmarks by homeViewModel.bookmarks.collectAsState()
    val collections by homeViewModel.collections.collectAsState()
    val chapters by surahViewModel.allChapters.collectAsState()

    var chartMode by remember { mutableStateOf("flow") } // "flow" or "heatmap"
    var selectedDayTooltip by remember { mutableStateOf<String?>(null) }

    val todayStr = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when {
        hour < 12 -> "Good Morning"
        hour < 17 -> "Good Afternoon"
        else -> "Good Evening"
    }

    // 7 Days Labels and Totals
    val last7DaysData = remember(sessions) {
        val days = mutableListOf<Pair<String, Int>>()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val labelSdf = SimpleDateFormat("EEE", Locale.US)
        for (i in 6 downTo 0) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DATE, -i)
            val dStr = sdf.format(cal.time)
            val dayLabel = labelSdf.format(cal.time)
            val dayMins = (sessions.filter { it.date == dStr }.sumOf { it.duration } / 60.0).toInt()
            days.add(Pair(dayLabel, dayMins))
        }
        days
    }

    val streak = remember(sessions) {
        if (sessions.isEmpty()) 0
        else {
            val uniqueDatesSet = sessions.map { it.date }.toSet()
            val sortedDates = uniqueDatesSet.sortedDescending()
            val cal = Calendar.getInstance()
            val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val today = fmt.format(cal.time)
            if (sortedDates.firstOrNull() != today) {
                cal.add(Calendar.DATE, -1)
                if (sortedDates.firstOrNull() != fmt.format(cal.time)) 0
                else computeStreak(uniqueDatesSet, fmt)
            } else {
                computeStreak(uniqueDatesSet, fmt)
            }
        }
    }

    val todayTotalMins = remember(sessions, todayStr) {
        (sessions.filter { it.date == todayStr }.sumOf { it.duration } / 60.0).toInt()
    }

    val allTimeTotalMins = remember(sessions) {
        (sessions.sumOf { it.duration } / 60.0).toInt()
    }

    val weeklyGoalMins = 180
    val weeklyTotalMins = remember(last7DaysData) {
        last7DaysData.sumOf { it.second }
    }
    val weeklyGoalPercent = remember(weeklyTotalMins) {
        ((weeklyTotalMins.toFloat() / weeklyGoalMins.toFloat()) * 100f).coerceAtMost(100f).toInt()
    }

    // Activity breakdown mix (Reading, Memorizing, Focus, Listening)
    val activityMix = remember(sessions) {
        val readingMins = (sessions.filter { it.type == "reading" || it.type.isEmpty() }.sumOf { it.duration } / 60.0).toInt()
        val memorizingMins = (sessions.filter { it.type == "memorizing" }.sumOf { it.duration } / 60.0).toInt()
        val focusMins = (sessions.filter { it.type == "pomodoro" || it.type == "focus" }.sumOf { it.duration } / 60.0).toInt()
        val listeningMins = (sessions.filter { it.type == "listening" }.sumOf { it.duration } / 60.0).toInt()
        listOf(
            Triple("Reading", readingMins, Color(0xFF10B981)),
            Triple("Memorizing", memorizingMins, Color(0xFF3B82F6)),
            Triple("Focus", focusMins, Color(0xFF8B5CF6)),
            Triple("Listening", listeningMins, Color(0xFFF59E0B))
        ).filter { it.second > 0 }
    }

    // Smart Insight
    val smartInsight = remember(sessions) {
        if (sessions.isEmpty()) "Start reading to unlock insights!"
        else {
            val dayCounts = mutableMapOf("Sun" to 0L, "Mon" to 0L, "Tue" to 0L, "Wed" to 0L, "Thu" to 0L, "Fri" to 0L, "Sat" to 0L)
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val labelSdf = SimpleDateFormat("EEE", Locale.US)
            sessions.forEach { s ->
                try {
                    val dateObj = sdf.parse(s.date)
                    if (dateObj != null) {
                        val day = labelSdf.format(dateObj)
                        dayCounts[day] = (dayCounts[day] ?: 0L) + s.duration
                    }
                } catch (_: Exception) {}
            }
            val bestDay = dayCounts.maxByOrNull { it.value }?.key
            if (bestDay != null && (dayCounts[bestDay] ?: 0) > 0) {
                val fullDays = mapOf("Sun" to "Sundays", "Mon" to "Mondays", "Tue" to "Tuesdays", "Wed" to "Wednesdays", "Thu" to "Thursdays", "Fri" to "Fridays", "Sat" to "Saturdays")
                "You usually read best on ${fullDays[bestDay] ?: bestDay}. Keep up the great momentum!"
            } else {
                "Start reading to unlock insights!"
            }
        }
    }

    // Dynamic Achievements / Badges (web parity: union sessions+recentlyRead, newest first)
    val achievements = remember(streak, allTimeTotalMins, sessions, recentlyRead) {
        val badges = mutableListOf<BadgeItem>()
        if (streak >= 3) badges.add(BadgeItem("🔥", "3-Day Streak", "Consistency is key."))
        if (streak >= 7) badges.add(BadgeItem("🔥", "7-Day Streak", "A whole week!"))
        if (streak >= 30) badges.add(BadgeItem("🔥", "30-Day Streak", "Unstoppable!"))
        if (allTimeTotalMins >= 100) badges.add(BadgeItem("⏱️", "100 Minutes", "First big milestone."))
        if (allTimeTotalMins >= 500) badges.add(BadgeItem("⏱️", "500 Minutes", "Dedicated reader."))
        // Web parity (Progress.jsx uniqueSurahsRead): sessions' chapterIds + recentlyRead
        val surahIds = recentlyRead.map { it.chapterId }.toMutableSet()
        sessions.mapNotNullTo(surahIds) { it.chapterId }
        val surahCount = surahIds.size
        if (surahCount >= 5) badges.add(BadgeItem("🗺️", "Explorer", "Read 5 Surahs."))
        if (surahCount >= 30) badges.add(BadgeItem("🗺️", "Traveler", "Read 30 Surahs."))
        if (surahCount >= 114) badges.add(BadgeItem("👑", "Khatm", "Read all 114 Surahs!"))
        badges.takeLast(3).reversed()
    }

    // Heatmap data (35 days = 5 weeks)
    val heatmap35Days = remember(sessions) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        (34 downTo 0).map { i ->
            val cal = Calendar.getInstance()
            cal.add(Calendar.DATE, -i)
            val dStr = sdf.format(cal.time)
            val dayMins = (sessions.filter { it.date == dStr }.sumOf { it.duration } / 60.0).toInt()
            Pair(dStr, dayMins)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Analytics",
                        fontFamily = fontFamilyUi,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = hInk
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = hWhite)
            )
        },
        containerColor = hWhite
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Dashboard Header
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = com.nur.quran.R.drawable.ic_logo),
                        contentDescription = "Quran Nur Logo",
                        modifier = Modifier
                            .size(56.dp)
                            .padding(bottom = 6.dp)
                    )
                    Text(
                        text = "ANALYTICS DASHBOARD",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = fontFamilyMono,
                        letterSpacing = 1.2.sp,
                        color = hInkMuted
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Your Progress",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = fontFamilyUi,
                        color = hInk
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$greeting. Here's the story of your consistency.",
                        fontSize = 13.sp,
                        color = hInkMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Smart Insight Banner
            if (sessions.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = hGoldSoft),
                        border = BorderStroke(1.dp, hGold.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = NurIcons.Brain,
                                contentDescription = null,
                                tint = hGold,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = smartInsight,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = hInk,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            // Top 3 Metrics Grid Cards
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Card 1: Streak
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = hSurface),
                        border = BorderStroke(1.dp, hBoneDark)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "CONSISTENCY",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = fontFamilyMono,
                                    letterSpacing = 1.sp,
                                    color = hInkMuted
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = "$streak",
                                        fontSize = 42.sp,
                                        fontWeight = FontWeight.Black,
                                        color = hInk,
                                        lineHeight = 42.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Days",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = hInkMuted,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )
                                }
                                Text(text = "Current active streak", fontSize = 12.sp, color = hInkMuted)
                            }
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x1AEF4444)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "🔥", fontSize = 24.sp)
                            }
                        }
                    }

                    // Card 2: Today's Focus
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = hSurface),
                        border = BorderStroke(1.dp, hBoneDark)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "TODAY'S FOCUS",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = fontFamilyMono,
                                    letterSpacing = 1.sp,
                                    color = hInkMuted
                                )
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(hGoldLight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(NurIcons.Clock, contentDescription = null, tint = hGold, modifier = Modifier.size(18.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "$todayTotalMins",
                                    fontSize = 42.sp,
                                    fontWeight = FontWeight.Black,
                                    color = hGold,
                                    lineHeight = 42.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Mins",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = hGold.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                LinearProgressIndicator(
                                    progress = (todayTotalMins.toFloat() / 30f).coerceAtMost(1f),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(8.dp)
                                        .clip(CircleShape),
                                    color = hGold,
                                    trackColor = hBorderColor
                                )
                                Text(
                                    text = "Goal: 30m",
                                    fontSize = 11.sp,
                                    fontFamily = fontFamilyMono,
                                    color = hInkMuted
                                )
                            }
                        }
                    }

                    // Card 3: Weekly Goal
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = hSurface),
                        border = BorderStroke(1.dp, hBoneDark)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    progress = (weeklyGoalPercent.toFloat() / 100f),
                                    modifier = Modifier.size(68.dp),
                                    color = hTeal,
                                    strokeWidth = 7.dp,
                                    trackColor = hBorderColor
                                )
                                Text(
                                    text = "$weeklyGoalPercent%",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = hInk
                                )
                            }
                            Column {
                                Text(
                                    text = "WEEKLY GOAL",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = fontFamilyMono,
                                    letterSpacing = 1.sp,
                                    color = hInkMuted
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = "$weeklyTotalMins",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = hInk
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "/ ${weeklyGoalMins}m",
                                        fontSize = 14.sp,
                                        color = hInkMuted
                                    )
                                }
                                Text(text = "Total this week", fontSize = 12.sp, color = hInkMuted)
                            }
                        }
                    }
                }
            }

            // Empty State Card
            if (sessions.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = hSurface),
                        border = BorderStroke(1.dp, hBoneDark)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = NurIcons.BookOpen,
                                contentDescription = null,
                                tint = hGold,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Start Your Journey",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = hInk,
                                fontFamily = fontFamilyUi
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Your reading and memorization activity will beautifully visualize here as you use the app.",
                                fontSize = 13.sp,
                                color = hInkMuted,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = onOpenQuranClick,
                                shape = RoundedCornerShape(24.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = hTeal)
                            ) {
                                Text(text = "Open Quran", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Activity Flow & Heatmap Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = hSurface),
                    border = BorderStroke(1.dp, hBoneDark)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = NurIcons.BookOpen,
                                    contentDescription = null,
                                    tint = hGold,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Activity Flow",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = hInk,
                                    fontFamily = fontFamilyUi
                                )
                            }
                            // Tab Selector (7 Days / Heatmap)
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(hBorderColor.copy(alpha = 0.5f))
                                    .padding(3.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (chartMode == "flow") hWhite else Color.Transparent)
                                        .clickable { chartMode = "flow" }
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = "7 DAYS",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = fontFamilyMono,
                                        color = if (chartMode == "flow") hInk else hInkMuted
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (chartMode == "heatmap") hWhite else Color.Transparent)
                                        .clickable { chartMode = "heatmap" }
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = "HEATMAP",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = fontFamilyMono,
                                        color = if (chartMode == "heatmap") hInk else hInkMuted
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        if (chartMode == "flow") {
                            // 7-Day Line/Bar Chart with Curve Path & Heights
                            val maxMins = maxOf(last7DaysData.maxOfOrNull { it.second } ?: 1, 30)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                last7DaysData.forEach { (dayLabel, mins) ->
                                    val heightFactor = (mins.toFloat() / maxMins.toFloat()).coerceIn(0.08f, 1f)
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        if (mins > 0) {
                                            Text(
                                                text = "${mins}m",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = fontFamilyMono,
                                                color = hGold,
                                                modifier = Modifier.padding(bottom = 4.dp)
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(0.5f)
                                                .fillMaxHeight(heightFactor)
                                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                                .background(
                                                    if (mins > 0) Brush.verticalGradient(
                                                        listOf(hGold, hGoldSoft)
                                                    ) else Brush.verticalGradient(listOf(hBorderColor, hBorderColor))
                                                )
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = dayLabel,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = hInkMuted
                                        )
                                    }
                                }
                            }
                        } else {
                            // 35-Day Consistency Heatmap Grid (5 cols x 7 rows)
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                selectedDayTooltip?.let { tooltip ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = hGoldSoft,
                                        border = BorderStroke(1.dp, hGold.copy(alpha = 0.4f)),
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    ) {
                                        Text(
                                            text = tooltip,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = fontFamilyMono,
                                            color = hInk,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(vertical = 12.dp)
                                ) {
                                    val weeks = heatmap35Days.chunked(7)
                                    weeks.forEach { weekDays ->
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            weekDays.forEach { (dStr, mins) ->
                                                val tileBg = when {
                                                    mins <= 0 -> hBorderColor.copy(alpha = 0.5f)
                                                    mins < 10 -> hGoldSoft
                                                    mins < 30 -> hGold.copy(alpha = 0.7f)
                                                    else -> Color(0xFF10B981)
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(tileBg)
                                                        .clickable {
                                                            selectedDayTooltip = if (mins > 0) "$dStr • ${mins} mins read" else "$dStr • No activity"
                                                            Toast.makeText(context, selectedDayTooltip, Toast.LENGTH_SHORT).show()
                                                        }
                                                )
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(text = "Less", fontSize = 10.sp, fontFamily = fontFamilyMono, color = hInkMuted)
                                    Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(hBorderColor.copy(alpha = 0.5f)))
                                    Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(hGoldSoft))
                                    Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(hGold.copy(alpha = 0.7f)))
                                    Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFF10B981)))
                                    Text(text = "More", fontSize = 10.sp, fontFamily = fontFamilyMono, color = hInkMuted)
                                }
                            }
                        }
                    }
                }
            }

            // Activity Mix Breakdown Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = hSurface),
                    border = BorderStroke(1.dp, hBoneDark)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Activity Mix",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = hInk,
                                fontFamily = fontFamilyUi
                            )
                            Text(
                                text = "ALL TIME",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = fontFamilyMono,
                                color = hInkMuted
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (activityMix.isNotEmpty()) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.size(140.dp)
                                ) {
                                    CircularProgressIndicator(
                                        progress = 1f,
                                        modifier = Modifier.size(120.dp),
                                        color = activityMix.first().third,
                                        strokeWidth = 14.dp,
                                        trackColor = hBorderColor
                                    )
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "$allTimeTotalMins",
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = hInk
                                        )
                                        Text(
                                            text = "Mins Total",
                                            fontSize = 10.sp,
                                            fontFamily = fontFamilyMono,
                                            color = hInkMuted
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    activityMix.forEach { (name, mins, color) ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(color)
                                            )
                                            Text(text = name, fontSize = 12.sp, color = hInkMuted)
                                            Text(text = "${mins}m", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = hInk)
                                        }
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = "No activity data logged yet.",
                                fontSize = 13.sp,
                                color = hInkMuted,
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        }
                    }
                }
            }

            // Achievements / Badges Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = hSurface),
                    border = BorderStroke(1.dp, hBoneDark)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Achievements",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = hInk,
                            fontFamily = fontFamilyUi
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        if (achievements.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                achievements.forEach { badge ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(hWhite)
                                            .border(1.dp, hBoneDark, RoundedCornerShape(16.dp))
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        Text(text = badge.icon, fontSize = 28.sp)
                                        Column {
                                            Text(
                                                text = badge.title,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = hInk
                                            )
                                            Text(
                                                text = badge.desc,
                                                fontSize = 12.sp,
                                                color = hInkMuted
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = "Read consistently to unlock achievements & badges!",
                                fontSize = 13.sp,
                                color = hInkMuted,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            // Recent Activity Section (Resolved with Chapter Names!)
            if (sessions.isNotEmpty()) {
                item {
                    Text(
                        text = "Recent Activity",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = hInk,
                        fontFamily = fontFamilyUi,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )
                }

                items(sessions.take(5)) { session ->
                    val surahName = remember(session.chapterId, chapters) {
                        if (session.chapterId != null) {
                            chapters.find { c -> c.id == session.chapterId }?.nameSimple ?: "Surah ${session.chapterId}"
                        } else null
                    }
                    val sessionTitle = when (session.type) {
                        "memorizing" -> if (surahName != null) "Memorization - $surahName" else "Memorization Session"
                        "pomodoro", "focus" -> if (surahName != null) "Focus - $surahName" else "Focus Session"
                        "listening" -> if (surahName != null) "Listening - $surahName" else "Listening Session"
                        else -> if (surahName != null) "Reading - $surahName" else "Reading Session"
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = hSurface),
                        border = BorderStroke(1.dp, hBoneDark)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when (session.type) {
                                                "memorizing" -> Color(0x1A3B82F6)
                                                "pomodoro", "focus" -> Color(0x1A8B5CF6)
                                                "listening" -> Color(0x1AF59E0B)
                                                else -> Color(0x1A10B981)
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when (session.type) {
                                            "memorizing" -> NurIcons.Brain
                                            "pomodoro", "focus" -> NurIcons.Award
                                            "listening" -> NurIcons.Volume2
                                            else -> NurIcons.BookOpen
                                        },
                                        contentDescription = null,
                                        tint = when (session.type) {
                                            "memorizing" -> Color(0xFF3B82F6)
                                            "pomodoro", "focus" -> Color(0xFF8B5CF6)
                                            "listening" -> Color(0xFFF59E0B)
                                            else -> Color(0xFF10B981)
                                        },
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = sessionTitle,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = hInk
                                    )
                                    Text(
                                        text = session.date,
                                        fontSize = 12.sp,
                                        fontFamily = fontFamilyMono,
                                        color = hInkMuted
                                    )
                                }
                            }
                            Text(
                                text = "${Math.round(session.duration / 60.0)} mins",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = hInk
                            )
                        }
                    }
                }
            }

            // Bottom Quick Stats Row Cards (3 Cards matching web Progress.jsx lines 538-557!)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Bookmarks Card
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = hSurface),
                        border = BorderStroke(1.dp, hBoneDark)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(hGoldLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(NurIcons.BookmarkFilled, contentDescription = null, tint = hGold, modifier = Modifier.size(14.dp))
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "${bookmarks.size}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = hInk
                            )
                            Text(
                                text = "BOOKMARKS",
                                fontSize = 9.sp,
                                fontFamily = fontFamilyMono,
                                fontWeight = FontWeight.Bold,
                                color = hInkMuted
                            )
                        }
                    }

                    // Collections Card
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = hSurface),
                        border = BorderStroke(1.dp, hBoneDark)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(hGoldLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(NurIcons.BookOpen, contentDescription = null, tint = hGold, modifier = Modifier.size(14.dp))
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "${collections.size}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = hInk
                            )
                            Text(
                                text = "COLLECTIONS",
                                fontSize = 9.sp,
                                fontFamily = fontFamilyMono,
                                fontWeight = FontWeight.Bold,
                                color = hInkMuted
                            )
                        }
                    }

                    // Recent Surahs Card
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = hSurface),
                        border = BorderStroke(1.dp, hBoneDark)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(hGoldLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(NurIcons.Brain, contentDescription = null, tint = hGold, modifier = Modifier.size(14.dp))
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "${recentlyRead.size}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = hInk
                            )
                            Text(
                                text = "RECENT SURAHS",
                                fontSize = 9.sp,
                                fontFamily = fontFamilyMono,
                                fontWeight = FontWeight.Bold,
                                color = hInkMuted
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun computeStreak(uniqueDates: Set<String>, fmt: SimpleDateFormat): Int {
    if (uniqueDates.isEmpty()) return 0
    var count = 0
    for (i in 0 until 365) {
        val check = Calendar.getInstance()
        check.add(Calendar.DATE, -i)
        val ds = fmt.format(check.time)
        if (ds in uniqueDates) count++
        else if (i > 0) break
    }
    return count
}
