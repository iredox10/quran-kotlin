package com.nur.quran.ui.components.planner

import androidx.compose.ui.res.stringResource
import com.nur.quran.R
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.nur.quran.data.planner.*
import com.nur.quran.ui.components.NurIcons
import com.nur.quran.ui.screens.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerHistoryDialog(
    plans: List<ReadingPlan>,
    timersByPlan: Map<String, Map<Int, PlannerSessionTimer>>,
    reflectionsByPlan: Map<String, Map<Int, String>>,
    bookmarksByPlan: Map<String, List<PlannerBookmark>>,
    onDismiss: () -> Unit
) {
    var selectedId by remember { mutableStateOf<String?>(null) }
    var completedOnly by remember { mutableStateOf(false) }
    val selected = plans.find { it.id == selectedId }
    val summaries = remember(plans, timersByPlan) { plans.associate { it.id to PlannerHistory.summarize(it, timersByPlan[it.id].orEmpty()) } }
    Dialog(onDismissRequest = { if (selectedId != null) selectedId = null else onDismiss() }) {
        Surface(shape = RoundedCornerShape(12.dp), color = hCream, border = BorderStroke(1.dp, hGold.copy(alpha = .4f)),
            modifier = Modifier.fillMaxWidth().fillMaxHeight(.9f)) {
            Column {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (selected != null) IconButton(onClick = { selectedId = null }) {
                        Icon(NurIcons.ArrowLeft, stringResource(R.string.planner_back_history), tint = hInk)
                    }
                    Text(if (selected == null) stringResource(R.string.planner_planner_history) else stringResource(R.string.planner_plan_details), modifier = Modifier.weight(1f).padding(start = 8.dp),
                        fontFamily = fontFamilyUi, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = hInk)
                    IconButton(onClick = onDismiss) { Icon(NurIcons.X, stringResource(R.string.planner_close_history), tint = hInkMid) }
                }
                Divider(color = hBoneDark)
                LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (selected == null) {
                        item {
                            Text(stringResource(R.string.planner_reading_journey), fontFamily = fontFamilyUi, fontSize = 23.sp, color = hInk)
                            Text(stringResource(R.string.planner_history_totals , summaries.values.count { it.completed }, plannerDuration(summaries.values.sumOf { it.totalSeconds })),
                                fontFamily = fontFamilyBody, fontSize = 13.sp, color = hInkMid, modifier = Modifier.padding(top = 6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                HistoryFilter(stringResource(R.string.planner_all_past_plans), !completedOnly) { completedOnly = false }
                                HistoryFilter(stringResource(R.string.planner_completed), completedOnly) { completedOnly = true }
                            }
                        }
                        val visible = plans.filter { !completedOnly || summaries[it.id]?.completed == true }
                        if (visible.isEmpty()) item {
                            Text(stringResource(R.string.planner_no_completed), fontFamily = fontFamilyUi, fontSize = 18.sp, color = hInk)
                            Text(stringResource(R.string.planner_history_empty),
                                fontFamily = fontFamilyBody, fontSize = 14.sp, color = hInkMid)
                        }
                        items(visible, key = { it.id }) { plan ->
                            val summary = summaries.getValue(plan.id)
                            Surface(shape = RoundedCornerShape(8.dp), color = hWhite, border = BorderStroke(1.dp, hBoneDark),
                                modifier = Modifier.fillMaxWidth().clickable { selectedId = plan.id }) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(if (summary.completed) stringResource(R.string.planner_completed_upper) else stringResource(R.string.planner_ended_early), fontFamily = fontFamilyMono, fontSize = 10.sp, color = plannerAccentInk)
                                    Text(plan.title, fontFamily = fontFamilyUi, fontSize = 18.sp, color = hInk)
                                    Text(stringResource(R.string.planner_history_progress , summary.completedDays, summary.totalDays, summary.pagesRead, summary.totalPages),
                                        fontFamily = fontFamilyBody, fontSize = 13.sp, color = hInkMid)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(NurIcons.Clock, null, modifier = Modifier.size(16.dp), tint = hGold)
                                        Text(if (summary.totalSeconds > 0) plannerDuration(summary.totalSeconds) + stringResource(R.string.planner_time_recorded) else stringResource(R.string.planner_no_time),
                                            modifier = Modifier.weight(1f).padding(start = 8.dp), fontFamily = fontFamilyBody, fontSize = 13.sp, color = hInk)
                                        Icon(NurIcons.ChevronRight, stringResource(R.string.planner_view_history), tint = hInkMid, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    } else {
                        val plan = selected
                        val summary = summaries.getValue(plan.id)
                        item {
                            Text(if (summary.completed) stringResource(R.string.planner_completed_plan) else stringResource(R.string.planner_ended_plan), fontFamily = fontFamilyMono, fontSize = 10.sp, color = plannerAccentInk)
                            Text(plan.title, fontFamily = fontFamilyUi, fontSize = 25.sp, color = hInk, modifier = Modifier.padding(top = 8.dp))
                            Text(stringResource(R.string.planner_started_date , plannerDateLabel(plan.startDate)), fontFamily = fontFamilyBody, fontSize = 13.sp, color = hInkMid)
                            summary.completedOn?.let { Text(stringResource(R.string.planner_completed_date , plannerDateLabel(it)), color = hInkMid, fontSize = 13.sp) }
                            plan.archivedAt?.take(10)?.let { Text(stringResource(R.string.planner_archived_date , plannerDateLabel(it)), color = hInkMid, fontSize = 13.sp) }
                            Text(plannerDuration(summary.totalSeconds), fontFamily = fontFamilyUi, fontSize = 36.sp, color = hInk, modifier = Modifier.padding(top = 18.dp))
                            Text(stringResource(R.string.planner_total_time), fontFamily = fontFamilyBody, fontSize = 13.sp, color = hInkMid)
                            Text(stringResource(R.string.planner_history_progress_full , summary.completedDays, summary.totalDays, summary.pagesRead, summary.totalPages),
                                modifier = Modifier.padding(top = 12.dp), fontSize = 14.sp, color = hInk)
                            if (summary.timedDays > 0) Text(stringResource(R.string.planner_history_average , summary.timedDays, plannerDuration(summary.averageSeconds)), fontSize = 13.sp, color = hInkMid)
                            val highlights = bookmarksByPlan[plan.id].orEmpty().size
                            if (highlights > 0) Text(stringResource(R.string.planner_saved_highlights , highlights), fontSize = 13.sp, color = hInkMid)
                        }
                        item { HistoryHeading(stringResource(R.string.planner_reading_time)) }
                        if (summary.readingDays.isEmpty()) item {
                            Text(stringResource(R.string.planner_no_plan_time), fontSize = 14.sp, color = hInkMid)
                        }
                        items(summary.readingDays, key = { it.date ?: "legacy-${it.dayNumber}" }) { day ->
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(day.date?.let { plannerDateLabel(it) } ?: stringResource(R.string.planner_earlier_reading, day.dayNumber ?: 0),
                                        modifier = Modifier.weight(1f), fontFamily = fontFamilyBody, fontSize = 13.sp, color = hInk)
                                    Text(plannerDuration(day.seconds), fontFamily = fontFamilyMono, fontSize = 13.sp, color = plannerAccentInk)
                                }
                                Text(stringResource(R.string.planner_accumulated , plannerDuration(day.cumulativeSeconds)) + if (day.date == null) stringResource(R.string.planner_date_not_recorded) else "",
                                    fontSize = 12.sp, color = hInkMid)
                                Divider(color = hBoneDark, modifier = Modifier.padding(top = 8.dp))
                            }
                        }
                        item { HistoryHeading(stringResource(R.string.planner_daily_reading_reflections)) }
                        items(plan.assignments, key = { it.dayNumber }) { assignment ->
                            val progress = PlannerEngine.getAssignmentProgress(plan, assignment)
                            val reflection = reflectionsByPlan[plan.id]?.get(assignment.dayNumber) ?: plan.assignmentReflections[assignment.dayNumber]
                            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text(stringResource(R.string.planner_history_day_title , assignment.dayNumber, plannerAssignmentTitle(assignment)), fontFamily = fontFamilyUi, fontSize = 16.sp, color = hInk)
                                Text(stringResource(R.string.planner_day_read_status, progress.readPagesCount, progress.totalPagesCount, if (progress.isComplete) stringResource(R.string.planner_completed) else if (progress.readPagesCount > 0) stringResource(R.string.planner_partially_read) else stringResource(R.string.planner_not_read)), fontSize = 13.sp, color = plannerAccentInk)
                                Text(stringResource(R.string.planner_scheduled_date , plannerDateLabel(assignment.date)), fontSize = 12.sp, color = hInkMid)
                                progress.completedAt?.let { Text(stringResource(R.string.planner_finished_date , plannerDateLabel(it)), fontSize = 12.sp, color = hInkMid) }
                                if (!reflection.isNullOrBlank()) Text(reflection, fontFamily = fontFamilyBody, fontSize = 14.sp, color = hInk, modifier = Modifier.padding(top = 4.dp))
                                Divider(color = hBoneDark, modifier = Modifier.padding(top = 8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryHeading(text: String) {
    Text(text, fontFamily = fontFamilyUi, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = hInk)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryFilter(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick,
        label = { Text(label, fontFamily = fontFamilyBody) },
        shape = RoundedCornerShape(8.dp),
        border = FilterChipDefaults.filterChipBorder(borderColor = hBoneDark, selectedBorderColor = hGold, borderWidth = 1.dp, selectedBorderWidth = 1.dp),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = hWhite, labelColor = hInkMid,
            selectedContainerColor = hGold.copy(alpha = .12f), selectedLabelColor = plannerAccentInk
        ))
}
