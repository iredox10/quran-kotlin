package com.nur.quran.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.R
import com.nur.quran.data.db.entities.VerseEntity
import com.nur.quran.data.hifdh.*
import com.nur.quran.ui.screens.*
import com.nur.quran.ui.viewmodels.SurahViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun HifdhMistakeNotebook(viewModel: SurahViewModel, events: List<HifdhReviewEvent>, onRepair: (List<String>) -> Unit) {
    val resolved = events.filter { it.outcome == "resolved" }.mapNotNull { it.relatedEventId }.toSet()
    val mistakes = events.filter { it.outcome == "mistake" || it.outcome == "confusion" || (it.outcome == "review" && it.rating == 1 && it.task != "CORRECTION") }
        .filter { failed -> failed.id !in resolved && events.none { it.verseKey == failed.verseKey && it.timestamp > failed.timestamp && it.independentPass } }
    var newNote by remember { mutableStateOf(false) }
    var compare by remember { mutableStateOf<String?>(null) }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        item {
            Button(onClick = { newNote = true }, modifier = Modifier.padding(vertical = 16.dp)) { Text(stringResource(R.string.hifdh_notes)) }
            if (mistakes.isEmpty()) Text(stringResource(R.string.ui_no_items_logged_yet_401266), color = hInkMuted)
        }
        items(mistakes, key = { it.id }) { event ->
            Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                Text(listOfNotNull(event.verseKey, event.relatedVerse).joinToString(" ↔ "), color = hGold)
                Text(stringResource(when (event.errorType) {
                    "prompt" -> R.string.ui_needed_prompt_353bdc
                    "hesitation" -> R.string.ui_hesitated_3dbf06
                    "transition" -> R.string.ui_weak_transition_detected_c43370
                    "pronunciation" -> R.string.settings_tajweed
                    else -> R.string.hifdh_repair
                }), color = hInkMuted)
                Text(event.note, color = hInk)
                if (event.wordPosition != null) Text(stringResource(R.string.ui_words_1_s_2_s_b2e62d, event.wordPosition, ""), color = hInkMuted)
                Row {
                    TextButton(onClick = { onRepair(listOfNotNull(event.verseKey, event.relatedVerse)) }) { Text(stringResource(R.string.hifdh_practice)) }
                    TextButton(onClick = { compare = event.verseKey }) { Text(stringResource(R.string.hifdh_similar)) }
                    TextButton(onClick = {
                        viewModel.logHifdhAssessment(event.verseKey, RecallAssessment(3), outcome = "resolved", relatedEventId = event.id)
                    }) { Text(stringResource(R.string.ui_done_e9b450)) }
                }
                Divider(color = hBoneDark)
            }
        }
    }
    if (newNote) HifdhMistakeForm(viewModel, { newNote = false })
    compare?.let { HifdhSimilarPassages(viewModel, it) { compare = null } }
}

@Composable
private fun HifdhMistakeForm(viewModel: SurahViewModel, onDismiss: () -> Unit) {
    var key by remember { mutableStateOf("") }
    var position by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("word") }
    var saved by remember { mutableStateOf(false) }
    val validKey = key.split(":").let { it.size == 2 && (it[0].toIntOrNull() ?: 0) in 1..114 && (it[1].toIntOrNull() ?: 0) in 1..286 }
    val verse by produceState<VerseEntity?>(null, key) { value = if (validKey) viewModel.getVerseTexts(listOf(key))[key] else null }
    val words = verse?.textUthmani.orEmpty().split(Regex("\\s+")).filter { it.isNotBlank() }
    AlertDialog(onDismissRequest = onDismiss, containerColor = hCream,
        title = { Text(stringResource(R.string.hifdh_notes), color = hInk) },
        text = { Column(Modifier.heightIn(max = 500.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(key, { key = it.filter { ch -> ch.isDigit() || ch == ':' }.take(7) }, label = { Text(stringResource(R.string.ui_go_to_ayah_ee520e)) }, placeholder = { Text("2:255") })
            verse?.let { WorkshopArabic(it) }
            OutlinedTextField(position, { position = it.filter(Char::isDigit).take(3) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), label = { Text(stringResource(R.string.ui_words_1_s_2_s_b2e62d, "", words.size)) })
            listOf("word" to R.string.ui_word_translations_1f21be, "prompt" to R.string.ui_needed_prompt_353bdc,
                "hesitation" to R.string.ui_hesitated_3dbf06, "transition" to R.string.ui_weak_transition_detected_c43370,
                "pronunciation" to R.string.settings_tajweed).forEach { (id, label) ->
                Row(Modifier.clickable { type = id }) { RadioButton(type == id, { type = id }); Text(stringResource(label), color = hInk, modifier = Modifier.padding(top = 12.dp)) }
            }
            OutlinedTextField(note, { note = it.take(2000) }, label = { Text(stringResource(R.string.hifdh_notes)) })
        } }, confirmButton = { TextButton(enabled = !saved && verse?.verseKey == key && (position.isBlank() || position.toIntOrNull() in 1..words.size), onClick = {
            saved = true
            viewModel.logHifdhAssessment(key, RecallAssessment(1), note = note, errorType = type, wordPosition = position.toIntOrNull(), outcome = "mistake")
            onDismiss()
        }) { Text(stringResource(R.string.ui_done_e9b450)) } }, dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ui_cancel_77dfd2)) } })
}

@Composable
private fun HifdhSimilarPassages(viewModel: SurahViewModel, key: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val verses by produceState<List<SimilarHifdhPassage>?>(null, key) { value = withContext(Dispatchers.IO) { HifdhSimilarity(context).find(key) } }
    val source by produceState<VerseEntity?>(null, key) { value = viewModel.getVerseTexts(listOf(key))[key] }
    var confirmed by remember { mutableStateOf(setOf<String>()) }
    AlertDialog(onDismissRequest = onDismiss, containerColor = hCream,
        title = { Text(stringResource(R.string.hifdh_similar), color = hInk) },
        text = { Column(Modifier.heightIn(max = 560.dp).verticalScroll(rememberScrollState())) {
            source?.let { Text(it.verseKey, color = hGold); WorkshopArabic(it) }
            if (verses == null) CircularProgressIndicator(color = hGold)
            else if (verses!!.isEmpty()) Text(stringResource(R.string.ui_no_items_logged_yet_401266), color = hInkMuted)
            verses.orEmpty().forEach { candidate ->
                Divider(color = hBoneDark)
                Text(candidate.verseKey, color = hGold, modifier = Modifier.padding(top = 12.dp))
                // Highlight matching words as a comparison tool, independently of Tajweed colors.
                val segments = remember(candidate) { Regex("\\S+").findAll(candidate.text).mapNotNull { word ->
                    if (HifdhSimilarity.tokenize(word.value).firstOrNull() in candidate.sharedWords)
                        TajweedSegment(word.range.first, word.range.last + 1, "#B8924A", "comparison") else null
                }.toList() }
                TajweedAndroidText(candidate.text, emptyList(), segments, "Amiri Quran", 25f, 1.8f,
                    textColor = hInk, isInteractive = false, onWordClick = {}, onTajweedClick = {}, modifier = Modifier.fillMaxWidth())
                TextButton(enabled = candidate.verseKey !in confirmed, onClick = {
                    confirmed = confirmed + candidate.verseKey
                    viewModel.logHifdhAssessment(key, RecallAssessment(1), relatedVerse = candidate.verseKey, outcome = "confusion")
                }) { Text(stringResource(R.string.hifdh_notes)) }
            }
        } }, confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ui_close_bbfa77)) } })
}
