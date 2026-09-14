package com.nur.quran.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.data.db.entities.VerseEntity
import com.nur.quran.data.db.entities.WordEntity
import com.nur.quran.data.mushaf.Mushaf
import com.nur.quran.data.mushaf.wordTextForMushaf
import com.nur.quran.ui.screens.TajweedRule
import com.nur.quran.ui.screens.ContinuousReadingPageItem
import com.nur.quran.ui.screens.fontFamilyMono
import com.nur.quran.ui.screens.fontScheherazade
import com.nur.quran.ui.screens.hBorderColor
import com.nur.quran.ui.screens.hGold
import com.nur.quran.ui.screens.hGoldLight
import com.nur.quran.ui.screens.hInk
import com.nur.quran.ui.screens.hInkMuted
import com.nur.quran.ui.screens.hSurface

/**
 * 15-line page-accurate physical Mushaf page view.
 * Port of web app: quran-app/src/components/MushafPageView.jsx
 *
 * Groups words by `word.lineNumber` (1..15) and renders strictly justified lines with:
 * - Active audio verse highlighting
 * - Unassigned verse dimming (boundary page support)
 * - Clickable word translations & tajweed rules
 */
@Composable
fun MushafPageView(
    page: Int,
    verses: List<VerseEntity>,
    wordsMap: Map<Int, List<WordEntity>>,
    tajweedMap: Map<String, String>?,
    isTajweedEnabled: Boolean,
    mushaf: Mushaf,
    activeAudioVerseKey: String?,
    assignedVerseKeys: Set<String>?,
    onWordClick: (WordEntity) -> Unit,
    onTajweedClick: (TajweedRule) -> Unit = {},
    arabicFontScale: Float = 1.0f,
    fontFamilyArabic: FontFamily = fontScheherazade,
    selectedArabicFontName: String = "KFGQPC Hafs",
    modifier: Modifier = Modifier
) {
    val lines = remember(verses, wordsMap, mushaf) {
        val lineMap = mutableMapOf<Int, MutableList<Pair<WordEntity, String>>>()
        verses.forEach { verse ->
            val words = wordsMap[verse.id] ?: emptyList()
            words.forEach { word ->
                val lineNum = word.lineNumber
                if (lineNum > 0) {
                    val wordList = lineMap.getOrPut(lineNum) { mutableListOf() }
                    wordList.add(word to verse.verseKey)
                }
            }
        }
        lineMap.entries.sortedBy { it.key }
    }

    if (lines.isEmpty()) {
        // Fallback to continuous reading view if line numbers are unavailable
        ContinuousReadingPageItem(
            page = page,
            pageVerses = verses,
            wordsMap = wordsMap,
            tajweedMap = tajweedMap,
            isTajweedEnabled = isTajweedEnabled,
            mushafId = mushaf.id,
            onWordClick = onWordClick,
            onTajweedClick = onTajweedClick,
            arabicFontScale = arabicFontScale,
            fontFamilyArabic = fontFamilyArabic,
            selectedArabicFontName = selectedArabicFontName
        )
        return
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = hSurface,
        border = BorderStroke(1.dp, hBorderColor),
        shadowElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 18.dp)
        ) {
            // Header badge matching web: "{mushaf.name} · line-grouped page scaffolding"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${mushaf.name} · line-grouped page scaffolding",
                    fontSize = 11.sp,
                    fontFamily = fontFamilyMono,
                    color = hInkMuted,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Page $page",
                    fontSize = 11.sp,
                    fontFamily = fontFamilyMono,
                    color = hGold
                )
            }

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    lines.forEach { (lineNumber, wordsWithKey) ->
                        val isSingleItemLine = wordsWithKey.size <= 2
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height((38 * arabicFontScale).dp),
                            horizontalArrangement = if (isSingleItemLine) Arrangement.Center else Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            wordsWithKey.forEach { (word, verseKey) ->
                                val isAudioPlaying = verseKey == activeAudioVerseKey
                                val isAssigned = assignedVerseKeys == null || assignedVerseKeys.contains(verseKey)
                                val isEndMarker = word.charTypeName == "end"
                                val displayText = wordTextForMushaf(
                                    mushaf,
                                    word.textUthmani,
                                    word.textIndopak,
                                    word.textQpcHafs
                                )

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isAudioPlaying) hGoldLight else Color.Transparent)
                                        .alpha(if (isAssigned) 1.0f else 0.25f)
                                        .clickable { onWordClick(word) }
                                        .padding(horizontal = 2.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = displayText,
                                        fontFamily = fontFamilyArabic,
                                        fontSize = (20 * arabicFontScale).sp,
                                        color = when {
                                            isAudioPlaying || isEndMarker -> hGold
                                            else -> hInk
                                        },
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
