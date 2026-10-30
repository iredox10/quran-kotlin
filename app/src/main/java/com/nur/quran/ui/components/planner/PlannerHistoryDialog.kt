package com.nur.quran.ui.components.planner

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
                        Icon(NurIcons.ArrowLeft, "Back to planner history", tint = hInk)
                    }
                    Text(if (selected == null) "Planner history" else "Plan details", modifier = Modifier.weight(1f).padding(start = 8.dp),
                        fontFamily = fontFamilyUi, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = hInk)
                    IconButton(onClick = onDismiss) { Icon(NurIcons.X, "Close history", tint = hInkMid) }
                }
                Divider(color = hBoneDark)
                LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (selected == null) {
                        item {
                            Text("Your reading journey", fontFamily = fontFamilyUi, fontSize = 23.sp, color = hInk)
                            Text("${summaries.values.count { it.completed }} completed plans · ${PlannerHistory.duration(summaries.values.sumOf { it.totalSeconds })} recorded",
                                fontFamily = fontFamilyBody, fontSize = 13.sp, color = hInkMid, modifier = Modifier.padding(top = 6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                HistoryFilter("All past plans", !completedOnly) { completedOnly = false }
                                HistoryFilter("Completed", completedOnly) { completedOnly = true }
                            }
                        }
                        val visible = plans.filter { !completedOnly || summaries[it.id]?.completed == true }
                        if (visible.isEmpty()) item {
                            Text("No completed plans yet", fontFamily = fontFamilyUi, fontSize = 18.sp, color = hInk)
                            Text("Finish a plan to see its reading time, progress and reflections here. Archived plans are kept here too.",
                                fontFamily = fontFamilyBody, fontSize = 14.sp, color = hInkMid)
                        }
                        items(visible, key = { it.id }) { plan ->
                            val summary = summaries.getValue(plan.id)
                            Surface(shape = RoundedCornerShape(8.dp), color = hWhite, border = BorderStroke(1.dp, hBoneDark),
                                modifier = Modifier.fillMaxWidth().clickable { selectedId = plan.id }) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(if (summary.completed) "COMPLETED" else "ENDED EARLY", fontFamily = fontFamilyMono, fontSize = 10.sp, color = plannerAccentInk)
                                    Text(plan.title, fontFamily = fontFamilyUi, fontSize = 18.sp, color = hInk)
                                    Text("${summary.completedDays}/${summary.totalDays} days · ${summary.pagesRead}/${summary.totalPages} pages",
                                        fontFamily = fontFamilyBody, fontSize = 13.sp, color = hInkMid)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(NurIcons.Clock, null, modifier = Modifier.size(16.dp), tint = hGold)
                                        Text(if (summary.totalSeconds > 0) PlannerHistory.duration(summary.totalSeconds) + " recorded" else "No reading time recorded",
                                            modifier = Modifier.weight(1f).padding(start = 8.dp), fontFamily = fontFamilyBody, fontSize = 13.sp, color = hInk)
                                        Icon(NurIcons.ChevronRight, "View plan history", tint = hInkMid, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    } else {
                        val plan = selected
                        val summary = summaries.getValue(plan.id)
                        item {
                            Text(if (summary.completed) "COMPLETED PLAN" else "ENDED PLAN", fontFamily = fontFamilyMono, fontSize = 10.sp, color = plannerAccentInk)
                            Text(plan.title, fontFamily = fontFamilyUi, fontSize = 25.sp, color = hInk, modifier = Modifier.padding(top = 8.dp))
                            Text("Started ${PlannerHistory.dateLabel(plan.startDate)}", fontFamily = fontFamilyBody, fontSize = 13.sp, color = hInkMid)
                            summary.completedOn?.let { Text("Completed ${PlannerHistory.dateLabel(it)}", color = hInkMid, fontSize = 13.sp) }
                            plan.archivedAt?.take(10)?.let { Text("Archived ${PlannerHistory.dateLabel(it)}", color = hInkMid, fontSize = 13.sp) }
                            Text(PlannerHistory.duration(summary.totalSeconds), fontFamily = fontFamilyUi, fontSize = 36.sp, color = hInk, modifier = Modifier.padding(top = 18.dp))
                            Text("Total recorded reading time", fontFamily = fontFamilyBody, fontSize = 13.sp, color = hInkMid)
                            Text("${summary.completedDays}/${summary.totalDays} days finished · ${summary.pagesRead}/${summary.totalPages} pages read",
                                modifier = Modifier.padding(top = 12.dp), fontSize = 14.sp, color = hInk)
                            if (summary.timedDays > 0) Text("${summary.timedDays} dated reading days · ${PlannerHistory.duration(summary.averageSeconds)} daily average", fontSize = 13.sp, color = hInkMid)
                            val highlights = bookmarksByPlan[plan.id].orEmpty().size
                            if (highlights > 0) Text("$highlights saved verse highlights", fontSize = 13.sp, color = hInkMid)
                        }
                        item { HistoryHeading("Reading time") }
                        if (summary.readingDays.isEmpty()) item {
                            Text("No reading time was recorded for this plan. Completed reading is still shown below.", fontSize = 14.sp, color = hInkMid)
                        }
                        items(summary.readingDays, key = { it.date ?: "legacy-${it.dayNumber}" }) { day ->
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(day.date?.let { PlannerHistory.dateLabel(it) } ?: "Earlier reading · day ${day.dayNumber}",
                                        modifier = Modifier.weight(1f), fontFamily = fontFamilyBody, fontSize = 13.sp, color = hInk)
                                    Text(PlannerHistory.duration(day.seconds), fontFamily = fontFamilyMono, fontSize = 13.sp, color = plannerAccentInk)
                                }
                                Text("${PlannerHistory.duration(day.cumulativeSeconds)} accumulated" + if (day.date == null) " · date not recorded" else "",
                                    fontSize = 12.sp, color = hInkMid)
                                Divider(color = hBoneDark, modifier = Modifier.padding(top = 8.dp))
                            }
                        }
                        item { HistoryHeading("Daily reading & reflections") }
                        items(plan.assignments, key = { it.dayNumber }) { assignment ->
                            val progress = PlannerEngine.getAssignmentProgress(plan, assignment)
                            val reflection = reflectionsByPlan[plan.id]?.get(assignment.dayNumber) ?: plan.assignmentReflections[assignment.dayNumber]
                            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text("Day ${assignment.dayNumber} · ${assignment.title}", fontFamily = fontFamilyUi, fontSize = 16.sp, color = hInk)
                                Text("${progress.readPagesCount}/${progress.totalPagesCount} pages · ${if (progress.isComplete) "Completed" else "${if (progress.readPagesCount > 0) "Partially read" else "Not read"}"}", fontSize = 13.sp, color = plannerAccentInk)
                                Text("Scheduled ${PlannerHistory.dateLabel(assignment.date)}", fontSize = 12.sp, color = hInkMid)
                                progress.completedAt?.let { Text("Finished ${PlannerHistory.dateLabel(it)}", fontSize = 12.sp, color = hInkMid) }
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
