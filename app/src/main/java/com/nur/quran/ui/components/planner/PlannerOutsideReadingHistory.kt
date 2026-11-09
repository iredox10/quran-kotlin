package com.nur.quran.ui.components.planner

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.R
import com.nur.quran.data.planner.*
import com.nur.quran.ui.components.NurIcons
import com.nur.quran.ui.screens.*

internal fun outsidePageRanges(pages: Collection<Int>): String {
    val sorted = pages.sorted()
    if (sorted.isEmpty()) return ""
    val groups = mutableListOf<String>()
    var first = sorted.first(); var last = first
    fun add() { groups.add(if (first == last) "$first" else "$first–$last") }
    sorted.drop(1).forEach { page -> if (page == last + 1) last = page else { add(); first = page; last = page } }
    add()
    return groups.joinToString(", ")
}

@Composable
internal fun OutsideReadingHistoryEntry(log: PlannerOutsideReading) {
    var expanded by remember(log.id) { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HistoryInline(NurIcons.BookOpen, stringResource(R.string.planner_outside_pages_label, outsidePageRanges(log.pageSeconds.keys)))
        Text(plannerDateLabel(log.date), fontSize = 12.sp, color = hInkMid)
        Text(stringResource(if (log.timing == PlannerOutsideReadings.ESTIMATED) R.string.planner_outside_estimated else R.string.planner_outside_entered),
            fontSize = 14.sp, color = plannerAccentInk)
        Text(plannerDuration(log.totalSeconds), fontFamily = fontFamilyUi, fontSize = 24.sp, color = hInk)
        val sameTime = log.pageSeconds.values.distinct().singleOrNull()
        if (sameTime != null) Text(stringResource(R.string.planner_outside_per_page, plannerDuration(sameTime)), fontSize = 14.sp, color = hInkMid)
        else {
            TextButton(onClick = { expanded = !expanded }) {
                Text(stringResource(if (expanded) R.string.planner_outside_hide_page_times else R.string.planner_outside_show_page_times), color = plannerAccentInk)
                Icon(if (expanded) NurIcons.ChevronUp else NurIcons.ChevronDown, null, Modifier.size(16.dp))
            }
            if (expanded) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                log.pageSeconds.toSortedMap().forEach { (page, seconds) ->
                    Text(stringResource(R.string.planner_outside_page, page) + " · " + plannerDuration(seconds), fontSize = 14.sp, color = hInk)
                }
            }
        }
        Divider(color = hBoneDark, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable internal fun OutsideTimeSources(summary: PlannerHistorySummary) {
    val rows = summary.readingDays
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HistoryInline(NurIcons.Clock, stringResource(R.string.planner_outside_in_app) + " · " + plannerDuration(rows.sumOf { it.inAppSeconds }))
        HistoryInline(NurIcons.BookOpen, stringResource(R.string.planner_outside_entered) + " · " + plannerDuration(rows.sumOf { it.outsideManualSeconds }))
        HistoryInline(NurIcons.Clock, stringResource(R.string.planner_outside_estimated) + " · " + plannerDuration(rows.sumOf { it.outsideEstimatedSeconds }))
    }
}
