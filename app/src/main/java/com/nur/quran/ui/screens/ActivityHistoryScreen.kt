package com.nur.quran.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.analytics.ActivityHistoryFilters
import com.nur.quran.analytics.AnalyticsStats
import com.nur.quran.data.db.entities.ChapterEntity
import com.nur.quran.data.db.entities.ReadingSessionEntity
import com.nur.quran.ui.components.NurIcons
import com.nur.quran.ui.components.analytics.TimelineSessionRow
import com.nur.quran.ui.viewmodels.HomeViewModel
import java.util.Date
import java.util.Locale

/**
 * Activity History — web /progress/activity parity (ActivityHistory.jsx):
 * the full session log with history tabs, search, day-grouped rows capped
 * at 100, and summary chips.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityHistoryScreen(
    homeViewModel: HomeViewModel,
    chapters: List<ChapterEntity>,
    onBack: () -> Unit,
) {
    val sessions by homeViewModel.readingSessions.collectAsState()

    var range by remember { mutableStateOf(ActivityHistoryFilters.HistoryRange.WEEK) } // web default
    var query by remember { mutableStateOf("") }

    // Chapter resolver: name_simple (+ name_arabic when present), web parity.
    val resolver: (Int?) -> Pair<String?, String?>? = { chapterId ->
        chapters.find { it.id == chapterId }?.let { ch ->
            ch.nameSimple to ch.nameArabic
        }
    }

    val filtered = remember(sessions, range, query, chapters) {
        ActivityHistoryFilters.filterSessions(sessions, range, query, resolver, now = Date())
    }
    val groups = remember(filtered) {
        ActivityHistoryFilters.groupSessionsByDay(filtered, now = Date())
    }
    val stats = remember(filtered) {
        AnalyticsStats.summarize(filtered.map {
            AnalyticsStats.Session(it.date, it.duration, it.type, it.chapterId, it.timestamp)
        })
    }
    val capped = filtered.size > HISTORY_MAX_ROWS
    val visibleGroups = remember(groups, capped) {
        if (!capped) groups
        else {
            var remaining = HISTORY_MAX_ROWS
            val out = mutableListOf<ActivityHistoryFilters.DayGroup>()
            for (group in groups) {
                if (remaining <= 0) break
                val take = minOf(group.sessions.size, remaining)
                remaining -= take
                val rows = if (take == group.sessions.size) group.sessions else group.sessions.take(take)
                out += group.copy(sessions = rows)
            }
            out
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Activity History",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = hInk,
                        fontFamily = fontFamilyUi
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = NurIcons.ArrowLeft, contentDescription = "Back", tint = hInk)
                    }
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
            // Description header (web "Session Log" eyebrow + h1)
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "SESSION LOG",
                        fontFamily = fontFamilyMono,
                        fontSize = 10.sp,
                        letterSpacing = 1.5.sp,
                        color = hInkMuted
                    )
                    Text(
                        text = "Every reading, memorizing, listening and focus session you log.",
                        fontSize = 13.sp,
                        color = hInkMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // History period tabs (web HISTORY_TABS)
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActivityHistoryFilters.HISTORY_TABS.forEach { tab ->
                        val selected = tab == range
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (selected) hTeal else hCream,
                            border = BorderStroke(1.5.dp, if (selected) hTeal else hBoneDark),
                            modifier = Modifier.clickable { range = tab }
                        ) {
                            Text(
                                text = tab.label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (selected) Color.White else hInkMid,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
            }

            // Search + summary chips card
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = hCream,
                    border = BorderStroke(1.5.dp, hBoneDark)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        FilterField(query = query, onQueryChange = { query = it })
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            HistoryStatCell(
                                modifier = Modifier.weight(1f),
                                value = AnalyticsStats.formatDuration(stats.seconds),
                                label = "TOTAL TIME"
                            )
                            HistoryStatCell(
                                modifier = Modifier.weight(1f),
                                value = "${stats.count}",
                                label = "SESSIONS"
                            )
                            HistoryStatCell(
                                modifier = Modifier.weight(1f),
                                value = "${stats.activeDays}",
                                label = "ACTIVE DAYS"
                            )
                        }
                    }
                }
            }

            // Body: groups or empty states (web renderBody)
            if (sessions.isEmpty()) {
                item {
                    HistoryEmptyState(
                        title = "No activity yet",
                        body = "Reading, memorizing, focus and listening sessions you log will show up here."
                    )
                }
            } else if (filtered.isEmpty() && query.trim().isNotEmpty()) {
                item {
                    HistoryEmptyState(
                        title = "No results for \u201c${query.trim()}\u201d",
                        body = "Try a different surah name or activity type."
                    )
                }
            } else if (filtered.isEmpty()) {
                item {
                    HistoryEmptyState(
                        title = "No activity in this period",
                        body = "Pick another range above or check All Time."
                    )
                }
            } else {
                visibleGroups.forEach { group ->
                    item(key = group.key) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                            ) {
                                Text(
                                    text = group.label.uppercase(Locale.US),
                                    fontFamily = fontFamilyMono,
                                    fontSize = 10.sp,
                                    letterSpacing = 1.5.sp,
                                    color = hInkMuted
                                )
                                Text(
                                    text = "${AnalyticsStats.formatDuration(group.totalSeconds)} · ${group.sessions.size} " +
                                        if (group.sessions.size == 1) "session" else "sessions",
                                    fontFamily = fontFamilyMono,
                                    fontSize = 10.sp,
                                    color = hInkMuted
                                )
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                group.sessions.forEach { session ->
                                    TimelineSessionRow(session = session, chapters = chapters)
                                }
                            }
                        }
                    }
                }
                if (capped) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = hWhite,
                            border = BorderStroke(1.5.dp, hBoneDark)
                        ) {
                            Text(
                                text = "SHOWING FIRST ${HISTORY_MAX_ROWS} OF ${filtered.size} SESSIONS — REFINE YOUR SEARCH TO NARROW IT DOWN",
                                fontFamily = fontFamilyMono,
                                fontSize = 9.5.sp,
                                letterSpacing = 0.8.sp,
                                color = hInkMuted,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(hWhite)
                                    .padding(12.dp)
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(48.dp)) }
        }
    }
}

private const val HISTORY_MAX_ROWS = 100 // web MAX_ROWS
