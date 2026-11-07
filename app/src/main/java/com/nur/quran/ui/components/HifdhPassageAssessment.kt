package com.nur.quran.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.nur.quran.R
import com.nur.quran.data.db.entities.VerseEntity
import com.nur.quran.data.hifdh.*
import com.nur.quran.ui.screens.*
import com.nur.quran.ui.viewmodels.SurahViewModel

/** Listener reads the full reference; independent self-assessment reveals it only after recitation. */
@Composable
internal fun HifdhPassageAssessment(viewModel: SurahViewModel, keys: List<String>, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val verses by produceState<List<VerseEntity>>(emptyList(), keys) { value = viewModel.getVerseTexts(keys).let { rows -> keys.mapNotNull { rows[it] } } }
    var assessor by remember { mutableStateOf("self") }
    var started by remember { mutableStateOf(false) }
    var checked by remember { mutableStateOf(false) }
    var prompted by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }
    var markedErrors by remember { mutableStateOf(setOf<String>()) }
    var submitted by remember { mutableStateOf(false) }
    var startedAt by remember { mutableLongStateOf(0) }
    var recordingId by remember { mutableStateOf<String?>(null) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = hCream) {
            Column(Modifier.fillMaxSize().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.hifdh_passage), color = hInk, fontFamily = fontFamilyUi)
                Text(keys.joinToString(" – "), color = hGold)
                if (!started) {
                    listOf("self" to R.string.hifdh_self, "listener" to R.string.hifdh_listener, "teacher" to R.string.hifdh_teacher).forEach { (id, label) ->
                        Row(Modifier.fillMaxWidth().clickable { assessor = id }) {
                            RadioButton(assessor == id, { assessor = id }); Text(stringResource(label), color = hInk, modifier = Modifier.padding(top = 12.dp))
                        }
                    }
                    Button(enabled = verses.size == keys.size, onClick = { viewModel.stopPlaying(); started = true; startedAt = android.os.SystemClock.elapsedRealtime() }) { Text(stringResource(R.string.hifdh_independent)) }
                } else if (!submitted) {
                    Text(stringResource(if (assessor == "self") R.string.hifdh_independent else if (assessor == "teacher") R.string.hifdh_teacher else R.string.hifdh_listener), color = hInkMid)
                    if (assessor != "self" || checked) verses.forEach { verse ->
                        Text(verse.verseKey, color = hGold)
                        WorkshopArabic(verse)
                        Row(Modifier.clickable { markedErrors = if (verse.verseKey in markedErrors) markedErrors - verse.verseKey else markedErrors + verse.verseKey }) {
                            Checkbox(verse.verseKey in markedErrors, { markedErrors = if (it) markedErrors + verse.verseKey else markedErrors - verse.verseKey })
                            Text(stringResource(R.string.hifdh_repair), color = hInk)
                        }
                    }
                    Row { Checkbox(prompted, { prompted = it }); Text(stringResource(R.string.ui_needed_prompt_353bdc), color = hInk) }
                    HifdhRecordingPanel(viewModel, onSaved = { recordingId = it })
                    if (!checked) Button(onClick = { checked = true }) { Text(stringResource(R.string.ui_tap_to_reveal_505d60)) }
                    if (checked) {
                        OutlinedTextField(notes, { notes = it.take(2000) }, label = { Text(stringResource(R.string.hifdh_notes)) })
                        Button(onClick = {
                            if (submitted) return@Button
                            submitted = true
                            val seconds = ((android.os.SystemClock.elapsedRealtime() - startedAt) / 1000).toInt().coerceAtLeast(1)
                            verses.forEach { verse ->
                                val failed = verse.verseKey in markedErrors || prompted
                                viewModel.logHifdhAssessment(verse.verseKey, RecallAssessment(if (failed) 1 else 3,
                                    if (prompted) RecallCue.ANSWER_PROMPT else RecallCue.NONE, RecallTask.PASSAGE, seconds / verses.size),
                                    assessor = assessor, note = notes, recordingId = recordingId)
                            }
                        }) { Text(stringResource(R.string.ui_log_session_a2e239)) }
                    }
                } else {
                    Text(stringResource(R.string.ui_done_e9b450), color = hGold)
                    Text(stringResource(R.string.hifdh_correction), color = hInkMid)
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.ui_close_bbfa77)) }
            }
        }
    }
}

@Composable
internal fun HifdhRecordingPanel(viewModel: SurahViewModel, onSaved: (String?) -> Unit = {}) {
    val context = LocalContext.current
    val store = remember { viewModel.createHifdhRecordings() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var recording by remember { mutableStateOf(false) }
    var id by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf(false) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    fun stop() { id = store.stop(); recording = false; onSaved(id) }
    fun start() {
        try { viewModel.stopPlaying(); id = store.start { stop() }; recording = true; error = false }
        catch (_: Exception) { error = true; recording = false }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> if (granted) start() else error = true }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) { if (recording) stop(); player?.release(); player = null } }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); if (recording) store.stop(); player?.release() }
    }
    Text(stringResource(R.string.hifdh_permission), color = hInkMuted)
    Button(onClick = {
        if (recording) stop()
        else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) start()
        else permission.launch(Manifest.permission.RECORD_AUDIO)
    }) { Text(stringResource(if (recording) R.string.hifdh_stop_recording else R.string.hifdh_record)) }
    if (error) Text(stringResource(R.string.ui_retry_9f5cd8), color = hRed)
    if (!recording && id != null) Row {
        TextButton(onClick = {
            player?.release()
            val file = id?.let(store::file)
            if (file != null) runCatching { MediaPlayer().apply { setDataSource(file.absolutePath); prepare(); start(); player = this } }.onFailure { error = true }
        }) { Text(stringResource(R.string.ui_play_preview_47bb44)) }
        TextButton(onClick = { player?.release(); player = null; id?.let(store::delete); id = null; onSaved(null) }) { Text(stringResource(R.string.ui_delete_f6fdbe)) }
    }
}

@Composable
internal fun HifdhRecordingLibrary(viewModel: SurahViewModel) {
    val store = remember { viewModel.createHifdhRecordings() }
    var files by remember { mutableStateOf(store.files()) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    DisposableEffect(Unit) { onDispose { player?.release() } }
    files.forEach { file ->
        Row(Modifier.fillMaxWidth()) {
            TextButton(onClick = {
                player?.release()
                runCatching { MediaPlayer().apply { setDataSource(file.absolutePath); prepare(); start(); player = this } }
            }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.hifdh_record) + " · " + java.text.DateFormat.getDateTimeInstance().format(java.util.Date(file.lastModified()))) }
            TextButton(onClick = { player?.release(); player = null; store.delete(file.name); files = store.files() }) { Text(stringResource(R.string.ui_delete_f6fdbe)) }
        }
    }
}
