package com.nur.quran.ui.components

import android.content.Context
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
fun MushafPicker(selectedMushafId: String, onSelect: (String) -> Unit) {
    val context = LocalContext.current
    val previews by produceState<List<MushafPreviewVerse>?>(null, context.applicationContext) {
        value = withContext(Dispatchers.IO) { loadMushafPreview(context.applicationContext) }
    }
    Column(Modifier.fillMaxWidth().selectableGroup(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(stringResource(R.string.mushaf_picker_intro), fontFamily = fontFamilyBody, fontSize = 13.sp, color = hInkMid, lineHeight = 20.sp)
        Mushaf.ALL.forEach { mushaf ->
            val selected = selectedMushafId == mushaf.id && mushaf.isSelectable
            Surface(
                shape = RoundedCornerShape(12.dp), color = if (selected) hGold.copy(alpha = .07f) else hCream,
                border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) hGold else hBoneDark),
                modifier = Modifier.fillMaxWidth().selectable(selected = selected, enabled = mushaf.isSelectable, role = Role.RadioButton, onClick = { onSelect(mushaf.id) })
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(mushafDisplayName(mushaf), fontFamily = fontFamilyUi, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = hInk)
                            Text(mushafDescription(mushaf), fontFamily = fontFamilyBody, fontSize = 12.sp, color = hInkMid, lineHeight = 18.sp)
                        }
                        RadioButton(selected = selected, enabled = mushaf.isSelectable, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = hGold, unselectedColor = hInkMuted, disabledUnselectedColor = hInkMuted.copy(alpha = .35f)))
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
                        Text(stringResource(R.string.mushaf_indopak_unavailable), fontFamily = fontFamilyBody, fontSize = 12.sp, lineHeight = 18.sp, color = hInkMid)
                    }
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
