package com.nur.quran.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.analytics.AnalyticsStats
import com.nur.quran.ui.components.NurIcons
import com.nur.quran.ui.components.analytics.*
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
    onOpenQuranClick: () -> Unit,
    onOpenLibraryClick: () -> Unit = onOpenQuranClick,
    onOpenCollectionsClick: () -> Unit = onOpenQuranClick
) {
    val sessions by homeViewModel.readingSessions.collectAsState()
    val recentlyRead by homeViewModel.recentlyRead.collectAsState()
    val bookmarks by homeViewModel.bookmarks.collectAsState()
    val collections by homeViewModel.collections.collectAsState()
    val chapters by surahViewModel.allChapters.collectAsState()

    var chartMode by remember { mutableStateOf("flow") } // "flow" or "heatmap"
    var flowRange by remember { mutableStateOf(AnalyticsStats.FlowRange.TODAY) }

    val todayStr = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when {
        hour < 12 -> "Good Morning"
        hour < 17 -> "Good Afternoon"
        else -> "Good Evening"
    }

    // Single-source stats input for AnalyticsStats delegation (web parity).
    val statsSessions = remember(sessions) {
        sessions.map { AnalyticsStats.Session(it.date, it.duration, it.type, it.chapterId, it.timestamp) }
    }

    // Flow card range filtering (web ActivityFlow parity; charts consume it next).
    val flowRangeSessions = remember(statsSessions, flowRange) {
        AnalyticsStats.filterByRange(statsSessions, flowRange)
    }

    val flowSummary = remember(flowRangeSessions) { AnalyticsStats.summarize(flowRangeSessions) }
    val flowPrevSec = remember(statsSessions, flowRange) { statsSessions.filter { it.date in AnalyticsStats.previousRangeKeys(flowRange).toSet() }.sumOf { it.durationSec } }

    // Last-7 yyyy-MM-dd keys for the web-parity weekly total (round AFTER summing raw secs).
    val last7Keys = remember {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        (6 downTo 0).map { i ->
            val cal = Calendar.getInstance()
            cal.add(Calendar.DATE, -i)
            sdf.format(cal.time)
        }
    }

    val streak = remember(statsSessions) {
        AnalyticsStats.streak(statsSessions.map { it.date }.toSet())
    }

    val todayTotalMins = remember(statsSessions, todayStr) {
        AnalyticsStats.todayMinutes(statsSessions, todayStr)
    }

    val allTimeTotalMins = remember(statsSessions) {
        AnalyticsStats.allTimeMinutes(statsSessions)
    }

    val weeklyGoalMins = 180
    val weeklyTotalMins = remember(statsSessions, last7Keys) {
        AnalyticsStats.weeklyTotalMinutes(statsSessions, last7Keys)
    }
    val weeklyGoalPercent = remember(weeklyTotalMins) {
        AnalyticsStats.weeklyGoalPercent(weeklyTotalMins, weeklyGoalMins)
    }

    // Activity Mix range filtering lives in AnalyticsActivityMix (default Week).

    // Smart Insight (single source: duration-weighted per weekday, web parity)
    val smartInsight = remember(statsSessions) {
        AnalyticsStats.smartInsight(statsSessions)
    }

    // Web-parity 20-badge engine: stats snapshot -> evaluated badges (board orders them).
    val achievementStats = remember(statsSessions, recentlyRead) {
        AnalyticsStats.computeAchievementStats(statsSessions, recentlyRead.map { it.chapterId })
    }
    val achievementBadges = remember(achievementStats) {
        AnalyticsStats.evaluateAchievements(achievementStats)
    }

    // Dynamic Achievements / Badges (single source: union sessions+recentlyRead, newest first)
    val achievements = remember(streak, allTimeTotalMins, sessions, recentlyRead) {
        AnalyticsStats.achievements(
            streakDays = streak,
            allTimeMins = allTimeTotalMins,
            sessionChapterIds = sessions.map { it.chapterId },
            recentlyReadIds = recentlyRead.map { it.chapterId }
        ).map { BadgeItem(it.icon, it.title, it.desc) }
    }

    // Heatmap data (35 days = 5 weeks, web parity: Math.round like heatmapData)
    val heatmap35Days = remember(sessions) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        (34 downTo 0).map { i ->
            val cal = Calendar.getInstance()
            cal.add(Calendar.DATE, -i)
            val dStr = sdf.format(cal.time)
            val dayMins = Math.round(sessions.filter { it.date == dStr }.sumOf { it.duration } / 60.0f).toInt()
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
            // 1. Dashboard Header
            item {
                AnalyticsDashboardHeader(greeting = greeting)
            }

            // 2. Smart Insight Banner (Web lines 237-241)
            if (sessions.isNotEmpty() && smartInsight.isNotBlank()) {
                item {
                    SmartInsightBanner(insight = smartInsight)
                }
            }

            // 3. Top 3 Metric Cards (Consistency, Today Focus, Weekly Goal)
            item {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    if (maxWidth >= 650.dp) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                ConsistencyCard(streak = streak)
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                TodayFocusCard(todayMins = todayTotalMins)
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                WeeklyGoalCard(
                                    weeklyMins = weeklyTotalMins,
                                    weeklyGoalMins = weeklyGoalMins,
                                    percent = weeklyGoalPercent
                                )
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ConsistencyCard(streak = streak)
                            TodayFocusCard(todayMins = todayTotalMins)
                            WeeklyGoalCard(
                                weeklyMins = weeklyTotalMins,
                                weeklyGoalMins = weeklyGoalMins,
                                percent = weeklyGoalPercent
                            )
                        }
                    }
                }
            }

            // 4. Empty State Card (Web lines 304-314)
            if (sessions.isEmpty()) {
                item {
                    AnalyticsEmptyState(onOpenQuranClick = onOpenQuranClick)
                }
            }

            // 5. Activity Flow Card & Activity Mix Card (Web lines 317-447)
            item {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    if (maxWidth >= 768.dp) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Activity Flow Card (50% width on tablet/desktop)
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = hCream),
                                border = BorderStroke(1.5.dp, hBoneDark)
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    ActivityFlowPeriodTabs(
                                        flowRange = flowRange,
                                        onFlowRangeChange = { flowRange = it }
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    ActivityFlowHeader(
                                        chartMode = chartMode,
                                        onChartModeChange = { chartMode = it }
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                        Text(AnalyticsStats.rangeTitle(flowRange), fontFamily = fontFamilyMono, fontSize = 10.sp, letterSpacing = 1.sp, color = hInkMuted)
                                        FlowDeltaBadge(AnalyticsStats.deltaPercent(flowSummary.seconds, flowPrevSec), flowSummary.seconds > 0L || flowPrevSec > 0L)
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    FlowStatCells(flowSummary, AnalyticsStats.rangeKeys(flowRange).size)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    if (chartMode == "flow") {
                                        AnalyticsFlowChartForRange(range = flowRange, sessions = flowRangeSessions)
                                    } else {
                                        AnalyticsHeatmap(heatmapData = heatmap35Days)
                                    }
                                }
                            }

                            // Activity Mix Card (50% width on tablet/desktop)
                            AnalyticsActivityMix(
                                sessions = statsSessions,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Activity Flow Card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = hCream),
                                border = BorderStroke(1.5.dp, hBoneDark)
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    ActivityFlowPeriodTabs(
                                        flowRange = flowRange,
                                        onFlowRangeChange = { flowRange = it }
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    ActivityFlowHeader(
                                        chartMode = chartMode,
                                        onChartModeChange = { chartMode = it }
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                        Text(AnalyticsStats.rangeTitle(flowRange), fontFamily = fontFamilyMono, fontSize = 10.sp, letterSpacing = 1.sp, color = hInkMuted)
                                        FlowDeltaBadge(AnalyticsStats.deltaPercent(flowSummary.seconds, flowPrevSec), flowSummary.seconds > 0L || flowPrevSec > 0L)
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    FlowStatCells(flowSummary, AnalyticsStats.rangeKeys(flowRange).size)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    if (chartMode == "flow") {
                                        AnalyticsFlowChartForRange(range = flowRange, sessions = flowRangeSessions)
                                    } else {
                                        AnalyticsHeatmap(heatmapData = heatmap35Days)
                                    }
                                }
                            }

                            // Activity Mix Card
                            AnalyticsActivityMix(
                                sessions = statsSessions
                            )
                        }
                    }
                }
            }

            // 6. Log activity (web ActivityFlow "Log activity" form parity)
            item {
                AnalyticsLogActivityCard(
                    chapters = chapters,
                    onLog = { durationSec, type, chapterId ->
                        homeViewModel.logReadingSession(durationSec, type, chapterId)
                    }
                )
            }

            // 6. Achievements & Recent Activity Row (Web lines 450-536)
            item {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    if (maxWidth >= 768.dp) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                FreshUnlockCelebration(badges = achievementBadges, todaySessions = achievementStats.todaySessions)
                                AchievementsBoard(badges = achievementBadges, hasSessions = sessions.isNotEmpty())
                            }
                            RecentActivityTimeline(
                                sessions = sessions,
                                chapters = chapters,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            FreshUnlockCelebration(badges = achievementBadges, todaySessions = achievementStats.todaySessions)
                            AchievementsBoard(badges = achievementBadges, hasSessions = sessions.isNotEmpty())
                            // No activity route in Screen.kt, so See-all stays hidden (onSeeAllClick = null).
                            RecentActivityTimeline(sessions = sessions, chapters = chapters)
                        }
                    }
                }
            }

            // 7. Bottom Quick Navigation Cards (Web lines 538-557)
            item {
                AnalyticsQuickCards(
                    bookmarksCount = bookmarks.size,
                    collectionsCount = collections.size,
                    recentSurahsCount = recentlyRead.size,
                    onOpenLibraryClick = onOpenLibraryClick,
                    onOpenCollectionsClick = onOpenCollectionsClick,
                    onOpenQuranClick = onOpenQuranClick
                )
            }

            // Bottom nav padding spacer
            item {
                Spacer(modifier = Modifier.height(64.dp))
            }
        }
    }
}

@Composable
private fun ActivityFlowHeader(
    chartMode: String,
    onChartModeChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = NurIcons.TrendingUp,
                contentDescription = null,
                tint = hGold,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "Activity Flow",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = hInk,
                fontFamily = fontFamilyUi
            )
        }

        // Tab Selector (7 Days / Heatmap) matching web Progress.jsx lines 322-335
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(hSurface)
                .padding(3.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (chartMode == "flow") hWhite else Color.Transparent)
                    .clickable { onChartModeChange("flow") }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "CHART",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = fontFamilyMono,
                    letterSpacing = 1.sp,
                    color = if (chartMode == "flow") hInk else hInkMuted
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (chartMode == "heatmap") hWhite else Color.Transparent)
                    .clickable { onChartModeChange("heatmap") }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "HEATMAP",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = fontFamilyMono,
                    letterSpacing = 1.sp,
                    color = if (chartMode == "heatmap") hInk else hInkMuted
                )
            }
        }
    }
}

/** % delta pill vs previous window + Total/Sessions/Active/Avg cells — web parity. */
@Composable
private fun FlowDeltaBadge(delta: Int?, hasData: Boolean) {
    val text = if (!hasData) "No data" else if (delta == null) "No baseline" else if (delta == 0) "0%" else if (delta > 0) "+$delta%" else "$delta%"
    val color = if (text.startsWith("+")) Color(0xFF10B981) else if (text.startsWith("-")) Color(0xFFE75344) else hInkMuted
    Surface(color = if (color == hInkMuted) hSurface else color.copy(alpha = 0.12f), shape = RoundedCornerShape(20.dp)) {
        Text(text.uppercase(Locale.getDefault()), color = color, fontFamily = fontFamilyMono, fontSize = 10.sp, letterSpacing = 1.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
    }
}
@Composable
private fun FlowStatCells(summary: AnalyticsStats.FlowSummary, daysInRange: Int) {
    val cells = listOf("Total time" to AnalyticsStats.formatMinutes(summary.seconds), "Sessions" to "${summary.count}",
        "Active days" to "${summary.activeDays}/$daysInRange", "Avg / day" to "${summary.avgPerDayMin}m")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        cells.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (label, value) ->
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(hSurface)
                        .padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text(label.uppercase(Locale.getDefault()), fontFamily = fontFamilyMono, fontSize = 9.sp, letterSpacing = 1.sp, color = hInkMuted)
                        Text(value, fontFamily = fontFamilyUi, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = hInk)
                    }
                }
            }
        }
    }
}
/** Today/Week/Month pill selector — web PeriodTabs parity for the Flow card. */
@Composable
private fun ActivityFlowPeriodTabs(
    flowRange: AnalyticsStats.FlowRange,
    onFlowRangeChange: (AnalyticsStats.FlowRange) -> Unit
) {
    Row(Modifier.clip(RoundedCornerShape(20.dp)).background(hSurface).padding(3.dp)) {
        AnalyticsStats.FlowRange.entries.forEach { range ->
            val active = range == flowRange
            Box(Modifier.clip(RoundedCornerShape(16.dp))
                .background(if (active) hWhite else Color.Transparent)
                .clickable { onFlowRangeChange(range) }
                .padding(horizontal = 10.dp, vertical = 5.dp)) {
                Text(range.label.uppercase(Locale.getDefault()), fontSize = 10.sp,
                    fontWeight = FontWeight.Bold, fontFamily = fontFamilyMono,
                    letterSpacing = 1.sp, color = if (active) hInk else hInkMuted)
            }
        }
    }
}
