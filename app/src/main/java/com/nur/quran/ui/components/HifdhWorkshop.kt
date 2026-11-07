package com.nur.quran.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.nur.quran.R
import com.nur.quran.data.db.entities.ChapterEntity
import com.nur.quran.data.db.entities.VerseEntity
import com.nur.quran.data.hifdh.*
import com.nur.quran.ui.screens.*
import com.nur.quran.ui.theme.ReaderSystemBars
import com.nur.quran.ui.viewmodels.SurahViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Offline workshop. Ordinary reading preferences never change this plan's Hafs identity. */
@Composable
fun HifdhWorkshop(viewModel: SurahViewModel, chapters: List<ChapterEntity>, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("hifdh_workshop", 0) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { prefs.edit().putString("pinned_edition", "madani-standard").apply() }
    val events by viewModel.hifdhEvents.collectAsState()
    val memorized by viewModel.memorizedAyahs.collectAsState()
    val history by viewModel.hifdhHistory.collectAsState()
    val inventory by produceState<Map<String, Int>>(emptyMap()) { value = viewModel.memorizationInventory() }
    val evidence = remember(events) { events.groupBy { it.verseKey }.mapValues { evidenceFor(it.value) } }
    var minutes by rememberSaveable { mutableIntStateOf(prefs.getInt("minutes", 20).coerceIn(5, 120)) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var selection by rememberSaveable { mutableIntStateOf(prefs.getInt("chapter", 1).coerceIn(1, 114)) }
    var from by rememberSaveable { mutableStateOf("1") }
    var to by rememberSaveable { mutableStateOf("3") }
    var chapterPicker by remember { mutableStateOf(false) }
    var lesson by remember { mutableStateOf<List<String>?>(null) }
    var assessmentKeys by remember { mutableStateOf<List<String>?>(null) }
    var task by remember { mutableStateOf(RecallTask.AYAH) }
    val keys = remember(inventory, selection) { inventory.keys.filter { it.startsWith("$selection:") } }
    val daily = remember(keys, memorized, history, events, minutes) {
        buildDailyHifdhPlan(keys, memorized, history, events, System.currentTimeMillis(), minutes, inventory)
    }
    fun startTest(items: List<String>, mode: RecallTask = RecallTask.AYAH) {
        viewModel.stopPlaying(); task = mode; assessmentKeys = items
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        ReaderSystemBars()
        Surface(Modifier.fillMaxSize(), color = hCream) {
            Column(Modifier.fillMaxSize().navigationBarsPadding()) {
                Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) { Icon(NurIcons.ArrowLeft, stringResource(R.string.ui_back_b52b36), tint = hInk) }
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.ui_hifdh_tracker_377987), fontFamily = fontFamilyUi, color = hInk, fontSize = 20.sp)
                        Text(stringResource(R.string.hifdh_scope) + " · " + stringResource(R.string.settings_madani_standard), color = hInkMuted, fontSize = 11.sp)
                    }
                }
                ScrollableTabRow(selectedTabIndex = tab, containerColor = hCream, edgePadding = 8.dp) {
                    listOf(R.string.ui_today_7a4686, R.string.hifdh_practice, R.string.ui_review_mistakes_961ede, R.string.ui_history_f6dc58).forEachIndexed { i, label ->
                        Tab(selected = tab == i, onClick = { tab = i }, text = { Text(stringResource(label), color = if (tab == i) hGold else hInkMid) })
                    }
                }
                when (tab) {
                    0 -> Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(stringResource(R.string.hifdh_start_today), fontFamily = fontFamilyUi, fontSize = 26.sp, color = hInk)
                        Text("$minutes " + stringResource(R.string.ui_minutes_092f99), color = hInk)
                        Slider(minutes.toFloat(), { minutes = it.toInt(); prefs.edit().putInt("minutes", minutes).apply() }, valueRange = 5f..120f)
                        TextButton(onClick = { chapterPicker = true }) { Text(chapters.firstOrNull { it.id == selection }?.nameSimple ?: "$selection", color = hInk) }
                        if (daily.recovery) Text(stringResource(R.string.hifdh_repair), color = hGold)
                        WorkshopSection(stringResource(R.string.ui_manzil_old_cbdb98), daily.olderKeys) { startTest(daily.olderKeys) }
                        WorkshopSection(stringResource(R.string.ui_sabqi_recent_5663bb), daily.recentKeys) { startTest(daily.recentKeys) }
                        WorkshopSection(stringResource(R.string.ui_sabaq_new_e037d7), daily.newKeys) { lesson = daily.newKeys }
                        if (daily.reviewKeys.isNotEmpty()) Button(onClick = { startTest(daily.reviewKeys) }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.hifdh_start_today))
                        }
                        Text(stringResource(R.string.ui_review_due_b4ee46) + ": ${daily.remainingReviewCount}", color = hInkMuted)
                        Text(stringResource(R.string.hifdh_correction), color = hInkMuted, fontSize = 12.sp)
                    }
                    1 -> Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        TextButton(onClick = { chapterPicker = true }) { Text(chapters.firstOrNull { it.id == selection }?.nameSimple ?: "$selection", color = hInk) }
                        Text(stringResource(R.string.ui_ayah_playback_range_abedf8), color = hInkMuted, fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(from, { from = it.filter(Char::isDigit).take(3) }, modifier = Modifier.weight(1f), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                            OutlinedTextField(to, { to = it.filter(Char::isDigit).take(3) }, modifier = Modifier.weight(1f), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        }
                        val start = from.toIntOrNull() ?: 0
                        val end = to.toIntOrNull() ?: 0
                        val valid = start in 1..(chapters.firstOrNull { it.id == selection }?.versesCount ?: 0) && end >= start && end <= start + 19 && end <= (chapters.firstOrNull { it.id == selection }?.versesCount ?: 0)
                        val range = if (valid) (start..end).map { "$selection:$it" } else emptyList()
                        Button(enabled = valid, onClick = { lesson = range }) { Text(stringResource(R.string.ui_start_memorizing_756aad)) }
                        Button(enabled = valid, onClick = { startTest(range) }) { Text(stringResource(R.string.hifdh_independent)) }
                        OutlinedButton(enabled = valid && start > 1, onClick = { startTest(range, RecallTask.TRANSITION) }) { Text(stringResource(R.string.ui_weak_transition_detected_c43370)) }
                        OutlinedButton(enabled = valid, onClick = { startTest(range, RecallTask.PASSAGE) }) { Text(stringResource(R.string.hifdh_passage)) }
                        Text(stringResource(R.string.hifdh_scope), color = hInkMuted)
                    }
                    2 -> HifdhMistakeNotebook(viewModel, events, onRepair = { startTest(listOf(it)) })
                    else -> LazyColumn(Modifier.weight(1f).padding(horizontal = 20.dp)) {
                        item {
                            val total = memorized.size
                            val retained = evidence.count { it.value.mastery == HifdhMastery.RETAINED }
                            val independent = events.count { it.independentPass }
                            val failed = events.count { it.outcome == "review" && it.rating == 1 && it.task != "CORRECTION" }
                            Text(stringResource(R.string.hifdh_retained) + ": $retained / $total", color = hInk, modifier = Modifier.padding(vertical = 16.dp))
                            Text(stringResource(R.string.hifdh_independent) + ": $independent · " + stringResource(R.string.hifdh_repair) + ": $failed", color = hInkMid)
                            Text(stringResource(R.string.hifdh_imported) + ": " + memorized.count { evidence[it]?.learnedAt == null }, color = hInkMuted, modifier = Modifier.padding(vertical = 12.dp))
                        }
                        item { HifdhRecordingLibrary(viewModel) }
                        items(events.take(250), key = { it.id }) { event ->
                            val stamp = remember(event.timestamp) { Instant.ofEpochMilli(event.timestamp).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("MMM d, HH:mm")) }
                            ListItem(headlineContent = { Text(event.verseKey + " · " + event.task, color = hInk) },
                                supportingContent = { Text("$stamp · ${event.assessor} · ${event.rating} · ${event.durationSeconds}s\n${event.note}", color = hInkMid) },
                                colors = ListItemDefaults.colors(containerColor = hCream))
                        }
                    }
                }
            }
        }
    }
    if (chapterPicker) AlertDialog(onDismissRequest = { chapterPicker = false }, containerColor = hCream,
        title = { Text(stringResource(R.string.ui_select_surah_97d36a), color = hInk) },
        text = { LazyColumn(Modifier.heightIn(max = 480.dp)) { items(chapters, key = { it.id }) { chapter ->
            Text("${chapter.id}. ${chapter.nameSimple}", color = hInk, modifier = Modifier.fillMaxWidth().clickable {
                selection = chapter.id; from = "1"; to = minOf(3, chapter.versesCount).toString()
                prefs.edit().putInt("chapter", selection).apply(); chapterPicker = false
            }.padding(vertical = 12.dp))
        } } }, confirmButton = { TextButton(onClick = { chapterPicker = false }) { Text(stringResource(R.string.ui_done_e9b450)) } })
    lesson?.let { HifdhLesson(viewModel, it, { lesson = null }, { startTest(it); lesson = null }) }
    assessmentKeys?.let { test ->
        if (task == RecallTask.PASSAGE) HifdhPassageAssessment(viewModel, test, { assessmentKeys = null })
        else HifdhTestModal(test, if (task == RecallTask.TRANSITION) "Rabt" else "Hifdh", chapters,
            onDismiss = { assessmentKeys = null }, onLogReview = viewModel::logHifdhReview,
            loadVerses = viewModel::getVerseTexts,
            weakTransitions = if (task == RecallTask.TRANSITION) test.toSet() else emptySet(),
            onAssessment = { key, result -> viewModel.logHifdhAssessment(key, result) })
    }
}

@Composable
private fun WorkshopSection(title: String, keys: List<String>, onStart: () -> Unit) {
    Surface(color = hSurface, shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp), border = BorderStroke(1.dp, hBoneDark)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text(title, color = hInk, fontWeight = FontWeight.SemiBold); Text(keys.joinToString(" · "), color = hInkMuted, maxLines = 2) }
            IconButton(enabled = keys.isNotEmpty(), onClick = onStart) { Icon(NurIcons.ArrowRight, stringResource(R.string.hifdh_practice), tint = hGold) }
        }
    }
}

@Composable
private fun HifdhLesson(viewModel: SurahViewModel, keys: List<String>, onDismiss: () -> Unit, onTest: () -> Unit) {
    val verses by produceState<List<VerseEntity>>(emptyList(), keys) { value = viewModel.getVerseTexts(keys).let { byKey -> keys.mapNotNull { byKey[it] } } }
    var visible by remember { mutableStateOf(true) }
    var step by remember { mutableIntStateOf(0) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = hCream) {
            Column(Modifier.fillMaxSize().navigationBarsPadding().padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.stopPlaying(); onDismiss() }) { Icon(NurIcons.ArrowLeft, stringResource(R.string.ui_back_b52b36), tint = hInk) }
                    Text(keys.joinToString(" – "), color = hInk)
                    Spacer(Modifier.weight(1f))
                    IconButton(enabled = verses.isNotEmpty(), onClick = { viewModel.playPauseHifdhChunk(verses, verses.first().chapterId, 3, 3, 1) }) { Icon(NurIcons.Volume2, stringResource(R.string.ui_play_preview_47bb44), tint = hGold) }
                }
                Text(stringResource(if (step == 0) R.string.ui_start_reading_ec8c2d else R.string.hifdh_independent), color = hGold)
                LazyColumn(Modifier.weight(1f)) { items(verses, key = { it.verseKey }) { verse ->
                    Text(verse.verseKey, color = hGold, modifier = Modifier.padding(top = 20.dp))
                    if (visible) {
                        WorkshopArabic(verse)
                        Text(verse.translation.orEmpty(), color = hInkMid, modifier = Modifier.padding(vertical = 12.dp))
                    } else TextButton(onClick = { visible = true }) { Text(stringResource(R.string.ui_tap_to_reveal_505d60)) }
                } }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    OutlinedButton(onClick = { viewModel.stopPlaying(); visible = !visible; step = 1 }) { Text(stringResource(R.string.ui_toggle_reveal_22f7ea)) }
                    Button(enabled = verses.size == keys.size, onClick = { viewModel.stopPlaying(); onTest() }) { Text(stringResource(R.string.ui_test_my_hifdh_7302f5)) }
                }
            }
        }
    }
    DisposableEffect(Unit) { onDispose { viewModel.stopPlaying() } }
}

@Composable
internal fun WorkshopArabic(verse: VerseEntity) {
    TajweedAndroidText(text = verse.textQpcHafs?.takeIf { it.isNotBlank() } ?: verse.textUthmani.orEmpty(),
        wordRanges = emptyList(), segments = emptyList(), selectedArabicFontName = "KFGQPC Hafs",
        fontSizeSp = 28f, lineHeightRatio = 1.8f, textColor = hInk, isInteractive = false,
        onWordClick = {}, onTajweedClick = {}, modifier = Modifier.fillMaxWidth())
}
