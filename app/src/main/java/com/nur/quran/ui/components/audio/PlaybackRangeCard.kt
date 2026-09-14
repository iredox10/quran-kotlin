package com.nur.quran.ui.components.audio

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.ui.components.NurIcons
import com.nur.quran.ui.screens.fontFamilyUi
import com.nur.quran.ui.screens.hBoneDark
import com.nur.quran.ui.screens.hBorderColor
import com.nur.quran.ui.screens.hCream
import com.nur.quran.ui.screens.hGold
import com.nur.quran.ui.screens.hGoldSoft
import com.nur.quran.ui.screens.hInk
import com.nur.quran.ui.screens.hInkMid
import com.nur.quran.ui.screens.hInkMuted
import com.nur.quran.ui.screens.hWhite
import kotlin.math.roundToInt

/**
 * Modes for the Ayah Playback Range selector.
 */
enum class PlaybackRangeMode {
    FULL_SURAH,
    PAGE,
    CUSTOM
}

/**
 * Composable card for selecting the ayah playback range in the Audio Settings modal.
 *
 * Features:
 * - Top header with BookOpen icon, "AYAH PLAYBACK RANGE" title, and mode status badge.
 * - 3-Segment selector control (Full Surah, Page range, Custom Range) with elevated active pill.
 * - Current range readout ("From: Ayah X", "To: Ayah Y (End)").
 * - Dual-thumb range slider styled with golden rings and centered dots.
 * - Bottom boundary labels showing surah name (Start) and total ayah count.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackRangeCard(
    chapterName: String,
    totalAyahs: Int,
    startPage: Int,
    endPage: Int,
    startAyah: Int,
    endAyah: Int,
    onRangeChange: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
    pageStartAyah: Int = 1,
    pageEndAyah: Int = totalAyahs
) {
    val safeTotal = totalAyahs.coerceAtLeast(1)
    val safeStart = startAyah.coerceIn(1, safeTotal)
    val safeEnd = endAyah.coerceIn(safeStart, safeTotal)

    val pageLabel = when {
        startPage > 0 && endPage > 0 && startPage == endPage -> "Page $startPage"
        startPage > 0 && endPage > 0 -> "Page $startPage - $endPage"
        startPage > 0 -> "Page $startPage"
        else -> "Page X - Y"
    }

    var userSelectedMode by remember { mutableStateOf<PlaybackRangeMode?>(null) }

    val activeMode = when {
        userSelectedMode == PlaybackRangeMode.CUSTOM -> PlaybackRangeMode.CUSTOM
        safeStart == 1 && safeEnd == safeTotal -> PlaybackRangeMode.FULL_SURAH
        userSelectedMode == PlaybackRangeMode.PAGE -> PlaybackRangeMode.PAGE
        pageStartAyah > 0 && pageEndAyah > 0 && safeStart == pageStartAyah && safeEnd == pageEndAyah -> PlaybackRangeMode.PAGE
        else -> PlaybackRangeMode.CUSTOM
    }

    val badgeText = when (activeMode) {
        PlaybackRangeMode.FULL_SURAH -> "Full Surah"
        PlaybackRangeMode.PAGE -> pageLabel
        PlaybackRangeMode.CUSTOM -> "Custom Range"
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = hWhite,
        border = BorderStroke(1.dp, hBorderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── Top Row: Header & Status Badge ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = NurIcons.BookOpen,
                        contentDescription = null,
                        tint = hGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "AYAH PLAYBACK RANGE",
                        fontFamily = fontFamilyUi,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                        color = hInkMid
                    )
                }

                // Pill Badge
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(hGoldSoft)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = badgeText,
                        fontFamily = fontFamilyUi,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        color = hGold
                    )
                }
            }

            // ── 3-Segment Selector Control ──
            SegmentedRangeSelector(
                activeMode = activeMode,
                pageLabel = pageLabel,
                onSelectMode = { mode ->
                    userSelectedMode = mode
                    when (mode) {
                        PlaybackRangeMode.FULL_SURAH -> {
                            onRangeChange(1, safeTotal)
                        }
                        PlaybackRangeMode.PAGE -> {
                            val pStart = pageStartAyah.coerceIn(1, safeTotal)
                            val pEnd = pageEndAyah.coerceIn(pStart, safeTotal)
                            onRangeChange(pStart, pEnd)
                        }
                        PlaybackRangeMode.CUSTOM -> {
                            // Keep current range, slider handles customization
                        }
                    }
                }
            )

            // ── Range Values Row ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // "From: Ayah $safeStart"
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = hInkMuted, fontWeight = FontWeight.Normal, fontSize = 13.sp)) {
                            append("From: ")
                        }
                        withStyle(SpanStyle(color = hInk, fontWeight = FontWeight.Bold, fontSize = 13.sp)) {
                            append("Ayah $safeStart")
                        }
                    },
                    fontFamily = fontFamilyUi
                )

                // "To: Ayah $safeEnd (End)"
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = hInkMuted, fontWeight = FontWeight.Normal, fontSize = 13.sp)) {
                            append("To: ")
                        }
                        withStyle(SpanStyle(color = hInk, fontWeight = FontWeight.Bold, fontSize = 13.sp)) {
                            append("Ayah $safeEnd")
                        }
                        if (safeEnd >= safeTotal) {
                            withStyle(SpanStyle(color = hInkMuted, fontWeight = FontWeight.Normal, fontSize = 12.sp)) {
                                append(" (End)")
                            }
                        }
                    },
                    fontFamily = fontFamilyUi
                )
            }

            // ── Dual-Thumb Range Slider ──
            val safeSliderMax = if (safeTotal > 1) safeTotal.toFloat() else 1.001f
            RangeSlider(
                value = safeStart.toFloat()..safeEnd.toFloat(),
                onValueChange = { range ->
                    userSelectedMode = PlaybackRangeMode.CUSTOM
                    val newStart = range.start.roundToInt().coerceIn(1, safeTotal)
                    val newEnd = range.endInclusive.roundToInt().coerceIn(newStart, safeTotal)
                    onRangeChange(newStart, newEnd)
                },
                valueRange = 1f..safeSliderMax,
                steps = if (safeTotal > 1) safeTotal - 2 else 0,
                enabled = safeTotal > 1,
                colors = SliderDefaults.colors(
                    activeTrackColor = hGold,
                    inactiveTrackColor = hBoneDark.copy(alpha = 0.5f),
                    thumbColor = hGold
                ),
                startThumb = { GoldenRingThumb() },
                endThumb = { GoldenRingThumb() },
                modifier = Modifier.fillMaxWidth()
            )

            // ── Bottom Labels below Slider ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val startLabel = if (chapterName.isNotBlank()) "$chapterName (Start)" else "Start"
                Text(
                    text = startLabel,
                    fontFamily = fontFamilyUi,
                    fontSize = 11.sp,
                    color = hInkMuted
                )

                val ayahsCountLabel = if (safeTotal == 1) "1 Total Ayah" else "$safeTotal Total Ayahs"
                Text(
                    text = ayahsCountLabel,
                    fontFamily = fontFamilyUi,
                    fontSize = 11.sp,
                    color = hInkMuted
                )
            }
        }
    }
}

/**
 * 3-Segment Selector Control styled with a pill background container and active white pill.
 */
@Composable
private fun SegmentedRangeSelector(
    activeMode: PlaybackRangeMode,
    pageLabel: String,
    onSelectMode: (PlaybackRangeMode) -> Unit
) {
    val segments = listOf(
        PlaybackRangeMode.FULL_SURAH to "Full Surah",
        PlaybackRangeMode.PAGE to pageLabel,
        PlaybackRangeMode.CUSTOM to "Custom Range"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(hCream)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        segments.forEach { (mode, title) ->
            val isActive = activeMode == mode
            val textColor by animateColorAsState(
                targetValue = if (isActive) hInk else hInkMuted,
                label = "SegmentTextColor"
            )

            val pillModifier = if (isActive) {
                Modifier
                    .weight(1f)
                    .shadow(elevation = 1.dp, shape = RoundedCornerShape(9.dp))
                    .clip(RoundedCornerShape(9.dp))
                    .background(hWhite)
                    .border(BorderStroke(0.5.dp, hBorderColor.copy(alpha = 0.4f)), RoundedCornerShape(9.dp))
            } else {
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(Color.Transparent)
            }

            Box(
                modifier = pillModifier
                    .clickable { onSelectMode(mode) }
                    .padding(vertical = 7.dp, horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    fontFamily = fontFamilyUi,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 12.sp,
                    color = textColor,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Golden ring thumb with white inner center and golden center dot.
 */
@Composable
private fun GoldenRingThumb() {
    Box(
        modifier = Modifier
            .size(20.dp)
            .shadow(elevation = 2.dp, shape = CircleShape)
            .background(hWhite, CircleShape)
            .border(2.dp, hGold, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(hGold, CircleShape)
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun PlaybackRangeCardPreview() {
    var start by remember { mutableStateOf(1) }
    var end by remember { mutableStateOf(286) }
    Box(modifier = Modifier.padding(16.dp)) {
        PlaybackRangeCard(
            chapterName = "Al-Baqarah",
            totalAyahs = 286,
            startPage = 2,
            endPage = 49,
            startAyah = start,
            endAyah = end,
            onRangeChange = { s, e ->
                start = s
                end = e
            }
        )
    }
}

