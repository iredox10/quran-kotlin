package com.nur.quran.ui.components.planner

import android.app.DatePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.R
import com.nur.quran.data.planner.*
import com.nur.quran.ui.components.NurIcons
import com.nur.quran.ui.screens.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.util.UUID

/** No network, live progress preview, and a single durable save before navigating. */
@Composable
fun PlannerOutsideReadingScreen(plan: ReadingPlan, pace: PlannerPagePace?, onBack: () -> Unit,
    onSave: suspend (Int, Int, String, String, Map<Int, Long>, String) -> Unit) {
    val initialPage = rememberSaveable(plan.id) { PlannerOutsideReadings.currentPage(plan) ?: 1 }
    val first = initialPage.toString()
    var rangeMode by rememberSaveable(plan.id) { mutableStateOf("count") }
    var count by rememberSaveable(plan.id) { mutableStateOf("1") }
    var lastInput by rememberSaveable(plan.id) { mutableStateOf(initialPage.toString()) }
    val rangeResult = remember(plan, initialPage, rangeMode, count, lastInput) { runCatching {
        PlannerOutsideReadings.lastPage(plan, initialPage,
            (if (rangeMode == "count") count else lastInput).toIntOrNull() ?: 0, rangeMode == "count")
    } }
    val last = rangeResult.getOrNull()?.toString().orEmpty()
    val bounds = remember(plan.assignments) {
        val pages = plan.assignments.flatMap { day -> day.items.flatMap { (it.pageStart..it.pageEnd).toList() } }
        (pages.minOrNull() ?: initialPage) to (pages.maxOrNull() ?: initialPage)
    }
    var date by rememberSaveable(plan.id) { mutableStateOf(PlannerEngine.formatPlannerDate()) }
    var timing by rememberSaveable(plan.id) { mutableStateOf(if (pace == null) PlannerOutsideReadings.MANUAL else PlannerOutsideReadings.ESTIMATED) }
    var timingChosen by rememberSaveable(plan.id) { mutableStateOf(false) }
    LaunchedEffect(pace) { if (!timingChosen && pace != null) timing = PlannerOutsideReadings.ESTIMATED }
    var minutes by rememberSaveable(plan.id) { mutableStateOf("") }
    var seconds by rememberSaveable(plan.id) { mutableStateOf("0") }
    var individual by rememberSaveable(plan.id) { mutableStateOf(false) }
    var minuteOverrides by rememberSaveable(plan.id) { mutableStateOf(mapOf<Int, String>()) }
    var secondOverrides by rememberSaveable(plan.id) { mutableStateOf(mapOf<Int, String>()) }
    val requestId = rememberSaveable(plan.id) { UUID.randomUUID().toString() }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    BackHandler(enabled = saving) {}
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val selection = remember(plan, initialPage, rangeResult) { rangeResult.mapCatching { end -> PlannerOutsideReadings.select(plan, initialPage, end) } }
    val pages = selection.getOrNull()?.pages.orEmpty()
    fun duration(min: String, sec: String): Long? {
        val m = min.ifBlank { "0" }.toLongOrNull() ?: return null
        val s = sec.ifBlank { "0" }.toLongOrNull() ?: return null
        return (m * 60 + s).takeIf { m in 0..1440 && s in 0..59 && it in 1..86_400 }
    }
    val times = if (timing == PlannerOutsideReadings.ESTIMATED) pages.associateWith { pace?.secondsPerPage ?: 0 }
        else pages.associateWith { page -> duration(if (individual) minuteOverrides[page] ?: minutes else minutes,
            if (individual) secondOverrides[page] ?: seconds else seconds) ?: 0 }
    val validTimes = times.isNotEmpty() && times.values.all { it > 0 } && times.values.sum() <= 86_400
    val preview by key(plan, first, last, date, timing, times, pace) {
        produceState<OutsideReadingResult?>(null) {
            if (validTimes) {
                delay(120)
                value = withContext(Dispatchers.Default) { runCatching {
                    PlannerOutsideReadings.record(plan, first.toInt(), last.toInt(), date, timing, times, pace)
                }.getOrNull() }
            }
        }
    }
    val rangeLabel = stringResource(if (rangeMode == "count") R.string.planner_outside_count else R.string.planner_outside_to)
    val colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = hGold, unfocusedBorderColor = hBoneDark,
        focusedTextColor = hInk, unfocusedTextColor = hInk, focusedLabelColor = plannerAccentInk, unfocusedLabelColor = hInkMid)
    Surface(Modifier.fillMaxSize(), color = hCream) {
        Column(Modifier.fillMaxSize().navigationBarsPadding().imePadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, enabled = !saving) { Icon(NurIcons.ArrowLeft, stringResource(R.string.ui_back_b52b36), tint = hInk) }
                Text(stringResource(R.string.planner_outside_log), fontFamily = fontFamilyUi, fontSize = 20.sp, color = hInk, modifier = Modifier.weight(1f))
            }
            Divider(color = hBoneDark)
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                item {
                    Text(plan.title, fontFamily = fontFamilyUi, fontSize = 28.sp, color = hInk)
                    Text(stringResource(R.string.planner_outside_intro), color = hInkMid, fontSize = 16.sp, lineHeight = 24.sp, modifier = Modifier.padding(top = 10.dp))
                }
                item {
                    HistoryHeading(NurIcons.BookOpen, stringResource(R.string.planner_outside_current, initialPage))
                    Text(stringResource(R.string.planner_outside_bounds, bounds.first, bounds.second), color = hInkMid,
                        fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp))
                    TimingChoice(stringResource(R.string.planner_outside_count), rangeMode == "count", !saving) {
                        if (rangeMode != "count") count = ((lastInput.toIntOrNull() ?: initialPage) - initialPage + 1).coerceAtLeast(1).toString()
                        rangeMode = "count"; error = null
                    }
                    TimingChoice(stringResource(R.string.planner_outside_last_choice), rangeMode == "last", !saving) {
                        if (rangeMode != "last") lastInput = rangeResult.getOrNull()?.toString() ?: initialPage.toString()
                        rangeMode = "last"; error = null
                    }
                    OutlinedTextField(if (rangeMode == "count") count else lastInput, {
                        if (rangeMode == "count") count = it else lastInput = it
                        error = null
                    }, label = { Text(rangeLabel) },
                        singleLine = true, enabled = !saving, isError = rangeResult.isFailure,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = colors,
                        modifier = Modifier.fillMaxWidth().semantics { contentDescription = rangeLabel })
                    Text(stringResource(R.string.planner_outside_start_hint, initialPage, bounds.second), color = hInkMid,
                        fontSize = 14.sp, lineHeight = 21.sp, modifier = Modifier.padding(top = 8.dp))
                    Text(stringResource(R.string.planner_outside_numbering), fontSize = 12.sp, lineHeight = 18.sp, color = hInkMid, modifier = Modifier.padding(top = 8.dp))
                }
                item {
                    OutlinedButton(onClick = {
                        val day = LocalDate.parse(date)
                        DatePickerDialog(context, { _, y, m, d -> date = LocalDate.of(y, m + 1, d).toString(); error = null },
                            day.year, day.monthValue - 1, day.dayOfMonth).apply { datePicker.maxDate = System.currentTimeMillis() }.show()
                    }, enabled = !saving, modifier = Modifier.fillMaxWidth()) {
                        Icon(NurIcons.CalendarDays, null, Modifier.size(18.dp), tint = plannerAccentInk)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.planner_outside_date) + ": " + plannerDateLabel(date), color = hInk)
                    }
                }
                item {
                    HistoryHeading(NurIcons.Clock, stringResource(R.string.planner_outside_timing))
                    TimingChoice(stringResource(R.string.planner_outside_estimate), timing == PlannerOutsideReadings.ESTIMATED,
                        !saving && pace != null) { timingChosen = true; timing = PlannerOutsideReadings.ESTIMATED; error = null }
                    Text(if (pace == null) stringResource(R.string.planner_outside_no_pace)
                        else stringResource(R.string.planner_outside_pace, plannerDuration(pace.secondsPerPage), pace.samplePages),
                        fontSize = 14.sp, lineHeight = 21.sp, color = hInkMid, modifier = Modifier.padding(start = 12.dp, bottom = 8.dp))
                    TimingChoice(stringResource(R.string.planner_outside_manual), timing == PlannerOutsideReadings.MANUAL, !saving) {
                        timingChosen = true; timing = PlannerOutsideReadings.MANUAL; error = null
                    }
                    if (timing == PlannerOutsideReadings.MANUAL) {
                        Text(stringResource(R.string.planner_outside_per_page, stringResource(R.string.planner_outside_timing)),
                            fontSize = 14.sp, color = hInkMid, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(minutes, { minutes = it; timingChosen = true; error = null }, label = { Text(stringResource(R.string.ui_minutes_092f99)) },
                                singleLine = true, enabled = !saving, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = colors, modifier = Modifier.weight(1f))
                            OutlinedTextField(seconds, { seconds = it; timingChosen = true; error = null }, label = { Text(stringResource(R.string.planner_outside_seconds)) },
                                singleLine = true, enabled = !saving, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = colors, modifier = Modifier.weight(1f))
                        }
                        Row(Modifier.fillMaxWidth().clickable(enabled = !saving) { individual = !individual }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(individual, { individual = it }, enabled = !saving, colors = CheckboxDefaults.colors(checkedColor = hGold))
                            Text(stringResource(R.string.planner_outside_each), color = hInk, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        }
                    }
                }
                if (individual && timing == PlannerOutsideReadings.MANUAL) items(pages, key = { it }) { page ->
                    val minuteLabel = stringResource(R.string.planner_outside_page, page) + " " + stringResource(R.string.planner_outside_minutes)
                    val secondLabel = stringResource(R.string.planner_outside_page, page) + " " + stringResource(R.string.planner_outside_seconds)
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.planner_outside_page, page), fontSize = 16.sp, color = hInk)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(minuteOverrides[page] ?: minutes, { minuteOverrides = minuteOverrides + (page to it); timingChosen = true; error = null },
                                label = { Text(stringResource(R.string.ui_minutes_092f99)) }, singleLine = true, enabled = !saving,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = colors, modifier = Modifier.weight(1f).semantics { contentDescription = minuteLabel })
                            OutlinedTextField(secondOverrides[page] ?: seconds, { secondOverrides = secondOverrides + (page to it); timingChosen = true; error = null },
                                label = { Text(stringResource(R.string.planner_outside_seconds)) }, singleLine = true, enabled = !saving,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = colors, modifier = Modifier.weight(1f).semantics { contentDescription = secondLabel })
                        }
                    }
                }
                item {
                    selection.getOrNull()?.let { selected ->
                        if (selected.skippedPages > 0) Text(stringResource(R.string.planner_outside_skipped, selected.skippedPages), color = hInkMid, fontSize = 14.sp, lineHeight = 21.sp)
                    }
                    Text(stringResource(R.string.planner_outside_preview, pages.size, plannerDuration(times.values.sum())), fontFamily = fontFamilyUi, fontSize = 24.sp, color = hInk)
                    preview?.let { result ->
                        Text(if (result.resumePage != null) stringResource(R.string.planner_outside_resume, result.resumePage)
                            else stringResource(R.string.planner_outside_complete), color = hInkMid, fontSize = 14.sp, lineHeight = 21.sp, modifier = Modifier.padding(top = 8.dp))
                    }
                    if (timing == PlannerOutsideReadings.ESTIMATED) Text(stringResource(R.string.planner_outside_estimate_note), fontSize = 12.sp, color = hInkMid, modifier = Modifier.padding(top = 8.dp))
                    val issue = error ?: selection.exceptionOrNull()?.message ?: if (pages.isEmpty()) "already_read" else if (!validTimes && (minutes.isNotBlank() || seconds !in listOf("", "0") || minuteOverrides.isNotEmpty() || secondOverrides.isNotEmpty())) "time" else null
                    if (issue != null) Text(stringResource(outsideReadingError(issue)), color = MaterialTheme.colorScheme.error,
                        fontSize = 14.sp, lineHeight = 21.sp, modifier = Modifier.padding(top = 10.dp))
                }
            }
            Divider(color = hBoneDark)
            Button(onClick = {
                saving = true; error = null
                scope.launch {
                    try { onSave(first.toInt(), last.toInt(), date, timing, times, requestId) }
                    catch (cancel: CancellationException) { throw cancel }
                    catch (failure: Exception) { error = failure.message ?: "save" }
                    finally { saving = false }
                }
            }, enabled = preview != null && !saving, colors = ButtonDefaults.buttonColors(containerColor = hTeal),
                modifier = Modifier.fillMaxWidth().padding(16.dp).heightIn(min = 52.dp)) {
                Icon(if (saving) NurIcons.Clock else NurIcons.Check, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(if (saving) R.string.planner_outside_saving else if (preview?.resumePage != null) R.string.planner_outside_save_continue else R.string.planner_outside_save))
            }
        }
    }
}

@Composable private fun TimingChoice(label: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick).heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected, onClick, enabled = enabled, colors = RadioButtonDefaults.colors(selectedColor = hGold))
        Text(label, color = if (enabled) hInk else hInkMid, fontSize = 16.sp)
    }
}
internal fun outsideReadingError(key: String): Int = when (key) {
    "range" -> R.string.planner_outside_active_range_error
    "outside_plan" -> R.string.planner_outside_error_plan
    "already_read", "duplicate" -> R.string.planner_outside_error_read
    "date" -> R.string.planner_outside_error_date
    "time" -> R.string.planner_outside_error_time
    "no_pace" -> R.string.planner_outside_no_pace
    "reading_changed" -> R.string.planner_outside_position_changed
    "archived", "plan_changed" -> R.string.planner_outside_error_changed
    else -> R.string.planner_outside_error_save
}
