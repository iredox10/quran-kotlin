package com.nur.quran.ui.components

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.painterResource
import com.nur.quran.data.mushaf.NarrationMushaf
import com.nur.quran.data.mushaf.ReaderMushafPreference
import com.nur.quran.data.mushaf.rememberReaderMushaf
import com.nur.quran.data.mushaf.IndopakEdition
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import android.view.Gravity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import com.nur.quran.R
import com.nur.quran.data.db.entities.WordEntity
import com.nur.quran.data.mushaf.Mushaf
import com.nur.quran.data.mushaf.fontIdToName
import com.nur.quran.ui.screens.*
import com.nur.quran.utils.TajweedProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

internal data class MushafPreviewVerse(val number: Int, val words: List<WordEntity>)

/** Actual bundled words, using the same script selection, ornaments and shaping as the reader. */
internal fun loadMushafPreview(context: Context): List<MushafPreviewVerse> {
    val root = context.assets.open("data/words/1.json").bufferedReader().use { JSONObject(it.readText()) }
    val verses = root.getJSONArray("verses")
    return (0 until verses.length()).mapNotNull { index ->
        val verse = verses.getJSONObject(index)
        val number = verse.getInt("verse_number")
        if (number !in 2..3) return@mapNotNull null
        val data = verse.getJSONArray("words")
        val words = (0 until data.length()).map { position ->
            val word = data.getJSONObject(position)
            fun field(name: String) = word.optString(name).takeIf { it.isNotBlank() && it != "null" }
            WordEntity(
                id = word.getInt("id"), verseId = verse.getInt("id"), position = word.getInt("position"),
                textUthmani = field("text_uthmani"), textIndopak = field("text_indopak"), textQpcHafs = field("text_qpc_hafs"),
                textUthmaniTajweed = field("text_uthmani_tajweed"), translation = null, transliteration = null,
                charTypeName = word.optString("char_type_name", "word"), lineNumber = word.optInt("line_number")
            )
        }
        MushafPreviewVerse(number, words)
    }
}

@Composable
fun MushafPicker(selectedMushafId: String, onSelect: (String) -> Unit, openOnSelect: Boolean = true) {
    val context = LocalContext.current
    val preferredMushaf = rememberReaderMushaf()
    val previews by produceState<List<MushafPreviewVerse>?>(null, context.applicationContext) {
        value = withContext(Dispatchers.IO) { loadMushafPreview(context.applicationContext) }
    }
    val scope = rememberCoroutineScope()
    var indopakInstalled by remember { mutableStateOf(IndopakEdition.installed(context)) }
    var indopakProgress by remember { mutableStateOf<Float?>(null) }
    var indopakError by remember { mutableStateOf(false) }
    var openIndopak by remember { mutableStateOf(false) }
    var openNarration by remember { mutableStateOf<NarrationMushaf?>(null) }
    val narrationSamples by produceState<Map<NarrationMushaf, List<com.nur.quran.data.mushaf.NarrationAyah>>>(emptyMap()) {
        value = withContext(Dispatchers.IO) {
            NarrationMushaf.entries.associateWith { it.ayahs(context.applicationContext).filter { row -> row.surah == 1 && row.number in 2..3 } }
        }
    }
    if (openIndopak || openNarration != null) key(preferredMushaf) {
        if (preferredMushaf == "indopak") IndopakMushafReader(onDismiss = { openIndopak = false; openNarration = null }, onSelectHafs = onSelect)
        else NarrationMushaf.entries.firstOrNull { it.id == preferredMushaf }?.let { edition ->
            NarrationMushafReader(edition, onDismiss = { openNarration = null; openIndopak = false }, onSelectHafs = onSelect)
        }
    }
    Column(Modifier.fillMaxWidth().selectableGroup(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(stringResource(R.string.mushaf_picker_intro), fontFamily = fontFamilyBody, fontSize = 13.sp, color = hInkMid, lineHeight = 20.sp)
        Mushaf.ALL.forEach { mushaf ->
            val cardSelectable = mushaf.isSelectable || (mushaf.id == "indopak" && indopakInstalled)
            val selected = preferredMushaf == mushaf.id && cardSelectable
            Surface(
                shape = RoundedCornerShape(12.dp), color = if (selected) hGold.copy(alpha = .07f) else hCream,
                border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) hGold else hBoneDark),
                modifier = Modifier.fillMaxWidth().selectable(selected = selected, enabled = cardSelectable, role = Role.RadioButton, onClick = {
                    ReaderMushafPreference.select(context, mushaf.id)
                    if (mushaf.id == "indopak" && openOnSelect) openIndopak = true else onSelect(mushaf.id)
                })
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(mushafDisplayName(mushaf), fontFamily = fontFamilyUi, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = hInk)
                            Text(mushafDescription(mushaf), fontFamily = fontFamilyBody, fontSize = 12.sp, color = hInkMid, lineHeight = 18.sp)
                        }
                        if (cardSelectable) RadioButton(selected = selected, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = hGold, unselectedColor = hInkMuted))
                        else Icon(NurIcons.BookOpen, contentDescription = null, tint = hGold)
                    }
                    if (mushaf.isSelectable) {
                        Surface(shape = RoundedCornerShape(8.dp), color = hWhite, border = BorderStroke(.5.dp, hBoneDark)) {
                            Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 12.dp)) {
                                val sample = previews
                                if (sample == null) {
                                    Text(stringResource(R.string.mushaf_preview_preparing), fontFamily = fontFamilyBody, fontSize = 13.sp, color = hInkMid)
                                } else {
                                    sample.forEach { verse -> MushafPreviewText(mushaf, verse) }
                                }
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(R.string.mushaf_preview_reference), fontFamily = fontFamilyBody, fontSize = 11.sp, color = hInkMid)
                            if (selected) Text(stringResource(R.string.mushaf_selected), fontFamily = fontFamilyBody, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = hInk)
                        }
                    } else {
                        Image(painterResource(R.drawable.indopak_preview),
                            contentDescription = "IndoPak Quran preview — Al-Fatihah, Quraan Al Majeed 13 lines",
                            modifier = Modifier.fillMaxWidth())
                        Text("Quraan Al Majeed · 13 lines · Al-Fatihah", fontFamily = fontFamilyBody, fontSize = 11.sp, color = hInkMid)
                        Text(stringResource(R.string.indopak_download_description), fontFamily = fontFamilyBody, fontSize = 12.sp, lineHeight = 18.sp, color = hInkMid)
                        Button(enabled = indopakProgress == null, onClick = {
                            if (indopakInstalled) { ReaderMushafPreference.select(context, "indopak"); if (openOnSelect) openIndopak = true else onSelect("indopak") }
                            else scope.launch {
                                indopakProgress = 0f
                                indopakError = false
                                try {
                                    IndopakEdition.download(context.applicationContext) { amount ->
                                        scope.launch { indopakProgress = amount }
                                    }
                                    indopakInstalled = true
                                    ReaderMushafPreference.select(context, "indopak")
                                    if (openOnSelect) openIndopak = true else onSelect("indopak")
                                } catch (cancel: CancellationException) { throw cancel }
                                catch (_: Exception) { indopakError = true }
                                finally { indopakProgress = null }
                            }
                        }) {
                            Text(when {
                                indopakInstalled -> stringResource(R.string.indopak_open)
                                indopakProgress != null -> stringResource(R.string.indopak_downloading, (indopakProgress!! * 100).toInt())
                                else -> stringResource(R.string.indopak_download)
                            })
                        }
                        if (indopakError) Text(stringResource(R.string.indopak_download_error), color = hInkMid)
                    }
                }
            }
        }
        NarrationMushaf.entries.forEach { edition ->
            Surface(shape = RoundedCornerShape(12.dp), color = hCream, border = BorderStroke(if (preferredMushaf == edition.id) 1.5.dp else 1.dp, if (preferredMushaf == edition.id) hGold else hBoneDark),
                modifier = Modifier.fillMaxWidth().clickable { ReaderMushafPreference.select(context, edition.id); if (openOnSelect) openNarration = edition else onSelect(edition.id) }) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(edition.title, fontFamily = fontFamilyUi, fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp, color = hInk, modifier = Modifier.weight(1f))
                        Icon(if (preferredMushaf == edition.id) NurIcons.Check else NurIcons.BookOpen, contentDescription = null, tint = hGold)
                    }
                    Surface(shape = RoundedCornerShape(8.dp), color = hWhite, border = BorderStroke(.5.dp, hBoneDark)) {
                        Column(Modifier.fillMaxWidth().padding(12.dp)) {
                            val sample = narrationSamples[edition]
                            if (sample == null) Text(stringResource(R.string.mushaf_preview_preparing), color = hInkMid)
                            else sample.forEach { NarrationAyahText(edition, it, preview = true) }
                        }
                    }
                    Text("King Fahd Quran Complex · 604 pages · 6,214 ayahs", fontFamily = fontFamilyBody, fontSize = 11.sp, color = hInkMid)
                }
            }
        }
        Text(stringResource(R.string.mushaf_flow_note), fontFamily = fontFamilyBody, fontSize = 12.sp, lineHeight = 18.sp, color = hInkMid)
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
internal fun mushafDisplayName(mushaf: Mushaf): String = when (mushaf.id) {
    "madani-standard" -> stringResource(R.string.settings_madani_standard)
    "madani-tajweed" -> stringResource(R.string.settings_madani_tajweed)
    "uthmani-hafs" -> stringResource(R.string.mushaf_name_uthmani)
    else -> stringResource(R.string.settings_indopak)
}

@Composable
private fun mushafDescription(mushaf: Mushaf): String = when (mushaf.id) {
    "madani-standard" -> stringResource(R.string.mushaf_description_qpc)
    "madani-tajweed" -> stringResource(R.string.mushaf_description_tajweed)
    "uthmani-hafs" -> stringResource(R.string.mushaf_description_uthmani)
    else -> stringResource(R.string.mushaf_description_indopak)
}

@Composable
private fun MushafPreviewText(mushaf: Mushaf, verse: MushafPreviewVerse) {
    val fontName = fontIdToName(mushaf.defaultFontId)
    val display = remember(verse, mushaf.id, fontName) { buildVerseDisplayText(verse.words, mushaf.id, fontName) }
    val dark = isDarkThemeGlobal
    val segments = remember(verse, mushaf.id, fontName, dark) {
        if (mushaf.forcesTajweed) {
            val html = buildCleanVerseTajweedHtml(null, verse.words, verse.number, fontName)
            TajweedProcessor.getWordTajweedSegments(display.first, html, if (dark) "#EFECE4" else "#2B3F3C")
        } else emptyList()
    }
    TajweedAndroidText(
        text = display.first, wordRanges = emptyList(), segments = segments,
        selectedArabicFontName = fontName, fontSizeSp = 25f, lineHeightRatio = 2f,
        textColor = hInk, isInteractive = false, onWordClick = {}, onTajweedClick = {},
        modifier = Modifier.fillMaxWidth(), textAlign = Gravity.CENTER
    )
}
