package com.nur.quran.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
fun NarrationMushafReader(edition: NarrationMushaf, initialSurah: Int? = null, initialPage: Int? = null,
    onSelectHafs: (String) -> Unit = {}, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("NarrationReader", 0) }
    val rows by produceState<List<NarrationAyah>?>(null, edition) {
        value = withContext(Dispatchers.IO) { edition.ayahs(context.applicationContext) }
    }
    var page by rememberSaveable(edition.id, initialSurah, initialPage) { mutableIntStateOf((initialPage ?: prefs.getInt(edition.id, 1)).coerceIn(1, 604)) }
    val scope = rememberCoroutineScope()
    var controls by remember { mutableStateOf(true) }
    var settings by remember { mutableStateOf(false) }
    var showSurahs by remember { mutableStateOf(false) }
    var jump by remember { mutableStateOf(false) }
    var fontSize by remember { mutableFloatStateOf(prefs.getFloat("text_size", 30f).coerceIn(24f, 42f)) }
    val list = rememberLazyListState()
    val auto = rememberNativeAutoScroll(page, { list.canScrollForward }, { list.scrollBy(it) },
        { if (page < 604) { page++; true } else false })
    var anchorApplied by rememberSaveable(edition.id, initialSurah) { mutableStateOf(initialSurah == null) }
    LaunchedEffect(rows, initialSurah) {
        if (!anchorApplied && rows != null) {
            if (rows.orEmpty().none { it.surah == initialSurah && page in it.pages }) {
                rows?.firstOrNull { it.surah == initialSurah }?.let { page = it.pages.first() }
            }
            anchorApplied = true
        }
    }
    val pageIndex = remember(rows) { rows.orEmpty().flatMap { row -> row.pages.map { it to row } }.groupBy({ it.first }, { it.second }) }
    val pageRows = pageIndex[page].orEmpty()
    val basmala = remember(rows) { rows?.firstOrNull { it.surah == 27 && it.number == 30 }?.text
        ?.substringAfter("بِسْمِ", "")?.substringBefore('\u00a0')?.trim()?.let { "بِسْمِ $it" } }
    LaunchedEffect(page) {
        prefs.edit().putInt(edition.id, page).putLong("${edition.id}_timestamp", System.currentTimeMillis()).apply()
        list.scrollToItem(0)
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = hCream) {
            Column(Modifier.fillMaxSize().navigationBarsPadding().imePadding()) {
                if (controls) NativeReaderHeader(pageRows.firstOrNull()?.surahName ?: edition.title, auto,
                    onDismiss, { auto.paused = auto.active; showSurahs = true }, { controls = false }, { auto.paused = auto.active; settings = true })
                if (rows == null) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = hGold) }
                } else {
                    Box(Modifier.weight(1f)) {
                    LazyColumn(state = list, modifier = Modifier.fillMaxSize().pointerInput(controls) {
                        if (!controls) detectTapGestures(onTap = { controls = true })
                    }.mushafPageSwipe(
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
                    NativeAutoScrollerBar(auto, { scope.launch { list.animateScrollToItem(0) } },
                        { scope.launch { if (list.canScrollForward) list.animateScrollToItem((list.layoutInfo.totalItemsCount - 1).coerceAtLeast(0)) else if (page < 604) page++ } })
                    }
                }
                if (controls) ReaderPageNavigation(page, 1, 604, { if (page < 604) page++ }, { if (page > 1) page-- }, onPageClick = { auto.paused = auto.active; jump = true })
            }
        }
    }
    if (settings) NativeReaderSettings(fontSize, 24f, 42f,
        { fontSize = it; prefs.edit().putFloat("text_size", it).apply() },
        { settings = false; jump = true }, { settings = false }, onSelectHafs)
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
