package com.nur.quran.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nur.quran.R
import com.nur.quran.data.mushaf.NarrationAyah
import com.nur.quran.data.mushaf.NarrationMushaf
import com.nur.quran.ui.screens.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Each narration owns its numbering and saved position, independently of Hafs planner/audio keys. */
@Composable
fun NarrationMushafReader(edition: NarrationMushaf, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("NarrationReader", 0) }
    val rows by produceState<List<NarrationAyah>?>(null, edition) {
        value = withContext(Dispatchers.IO) { edition.ayahs(context.applicationContext) }
    }
    var page by remember(edition) { mutableIntStateOf(prefs.getInt(edition.id, 1).coerceIn(1, 604)) }
    var showSurahs by remember { mutableStateOf(false) }
    var jump by remember { mutableStateOf(false) }
    var fontSize by remember { mutableFloatStateOf(prefs.getFloat("text_size", 30f).coerceIn(24f, 42f)) }
    val list = rememberLazyListState()
    val pageIndex = remember(rows) { rows.orEmpty().flatMap { row -> row.pages.map { it to row } }.groupBy({ it.first }, { it.second }) }
    val pageRows = pageIndex[page].orEmpty()
    val basmala = remember(rows) { rows?.firstOrNull { it.surah == 27 && it.number == 30 }?.text
        ?.substringAfter("بِسْمِ", "")?.substringBefore('\u00a0')?.trim()?.let { "بِسْمِ $it" } }
    LaunchedEffect(page) {
        prefs.edit().putInt(edition.id, page).apply()
        list.scrollToItem(0)
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = hCream) {
            Column(Modifier.fillMaxSize().navigationBarsPadding().imePadding()) {
                Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) { Icon(NurIcons.ArrowLeft, stringResource(R.string.planner_back), tint = hInk) }
                    Column(Modifier.weight(1f).clickable { showSurahs = true }.padding(8.dp)) {
                        Text(edition.title, fontFamily = fontFamilyUi, color = hInk)
                        Text(pageRows.firstOrNull()?.surahName.orEmpty(), fontFamily = fontFamilyBody, color = hInkMid)
                    }
                    TextButton(onClick = { fontSize = (fontSize - 2f).coerceAtLeast(24f); prefs.edit().putFloat("text_size", fontSize).apply() }) { Text("A−", color = hInk) }
                    TextButton(onClick = { fontSize = (fontSize + 2f).coerceAtMost(42f); prefs.edit().putFloat("text_size", fontSize).apply() }) { Text("A+", color = hInk) }
                    TextButton(onClick = { showSurahs = true }) { Icon(NurIcons.BookOpen, stringResource(R.string.ui_quran_f5df39), tint = hGold) }
                }
                if (rows == null) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = hGold) }
                } else {
                    LazyColumn(state = list, modifier = Modifier.weight(1f).mushafPageSwipe(
                        onNext = { if (page < 604) page++ }, onPrevious = { if (page > 1) page-- }
                    ), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(pageRows.groupBy { it.surah }.values.toList(), key = { it.first().surah }) { ayahs ->
                            if (ayahs.first().number == 1) {
                                Text(ayahs.first().surahName, fontFamily = fontFamilyUi, color = hGold,
                                    modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                if (ayahs.first().surah != 9) basmala?.let { NarrationPassageText(edition, it, fontSize * .85f) }
                            }
                            NarrationPassageText(edition, ayahs.joinToString(" ") { it.text }, fontSize)
                        }
                    }
                }
                ReaderPageNavigation(page, 1, 604, { if (page < 604) page++ }, { if (page > 1) page-- }, onPageClick = { jump = true })
            }
        }
    }
    if (jump) PageJumpDialog(page, 1, 604, { page = it; jump = false }, { jump = false })
    if (showSurahs) {
        AlertDialog(onDismissRequest = { showSurahs = false }, containerColor = hSurface,
            title = { Text(edition.title, color = hInk) },
            text = {
                LazyColumn(Modifier.heightIn(max = 480.dp)) {
                    items(rows.orEmpty().distinctBy { it.surah }, key = { it.surah }) { ayah ->
                        Text("${ayah.surah}. ${ayah.surahName}", color = hInk,
                            modifier = Modifier.fillMaxWidth().clickable {
                                page = ayah.pages.first(); showSurahs = false
                            }.padding(vertical = 14.dp))
                    }
                }
            }, confirmButton = { TextButton(onClick = { showSurahs = false }) { Text(stringResource(R.string.ui_close_bbfa77)) } })
    }
}

@Composable
internal fun NarrationAyahText(edition: NarrationMushaf, ayah: NarrationAyah, preview: Boolean = false) {
    NarrationPassageText(edition, ayah.text, if (preview) 25f else 30f)
}

@Composable
private fun NarrationPassageText(edition: NarrationMushaf, text: String, size: Float) {
    TajweedAndroidText(text = text, wordRanges = emptyList(), segments = emptyList(),
        selectedArabicFontName = edition.fontName, fontSizeSp = size,
        lineHeightRatio = 1.65f, justified = true, textColor = hInk, isInteractive = false,
        onWordClick = {}, onTajweedClick = {}, modifier = Modifier.fillMaxWidth())
}
