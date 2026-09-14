package com.nur.quran.ui.components.audio

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.data.audio.PlaybackSettings
import com.nur.quran.ui.components.NurIcons
import com.nur.quran.ui.screens.fontFamilyBody
import com.nur.quran.ui.screens.fontFamilyMono
import com.nur.quran.ui.screens.fontFamilyUi
import com.nur.quran.ui.screens.hBoneDark
import com.nur.quran.ui.screens.hBorderColor
import com.nur.quran.ui.screens.hGold
import com.nur.quran.ui.screens.hInk
import com.nur.quran.ui.screens.hInkMid
import com.nur.quran.ui.screens.hInkMuted
import com.nur.quran.ui.screens.hWhite

val AyahRepeatCardOptions = listOf(1, 2, 3, 5, 10, PlaybackSettings.REPEAT_INFINITE)
val RangeRepeatCardOptions = listOf(1, 2, 3, 5, PlaybackSettings.REPEAT_INFINITE)
val SpeedCardOptions = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)
val DelayCardOptions = listOf(0f to "Off", 1f to "1s", 2f to "2s", 3f to "3s", 5f to "5s")

/**
 * Repetition & Audio Playback settings card for the Audio Setup Modal.
 *
 * Provides granular controls for:
 * 1. Individual ayah repetition (ideal for memorization / Hifz).
 * 2. Entire selected range repetition loop.
 * 3. Expandable advanced settings (playback speed, pause delay between verses,
 *    offline save toggle, and reader auto-scroll toggle).
 */
@Composable
fun RepetitionCard(
    ayahRepeat: Int,
    onAyahRepeatChange: (Int) -> Unit,
    rangeRepeat: Int,
    onRangeRepeatChange: (Int) -> Unit,
    speed: Float,
    onSpeedChange: (Float) -> Unit,
    delaySec: Float,
    onDelaySecChange: (Float) -> Unit,
    streamOnly: Boolean,
    onStreamOnlyChange: (Boolean) -> Unit,
    scrollWhilePlaying: Boolean,
    onScrollWhilePlayingChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var isAdvancedExpanded by rememberSaveable { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (isAdvancedExpanded) 180f else 0f,
        label = "advancedChevronRotation"
    )

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = hWhite),
        border = BorderStroke(1.dp, hBorderColor),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Sub-section 1: AYAH REPETITION ──
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = NurIcons.Repeat,
                            contentDescription = null,
                            tint = hGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "AYAH REPETITION",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = hInkMid,
                            letterSpacing = 1.sp,
                            fontFamily = fontFamilyUi
                        )
                    }
                    Text(
                        text = "Memorization (Hifz)",
                        fontSize = 11.sp,
                        color = hInkMuted,
                        fontFamily = fontFamilyBody
                    )
                }

                Text(
                    text = "Number of times each individual verse will play before advancing",
                    fontSize = 12.sp,
                    color = hInkMuted,
                    fontFamily = fontFamilyBody,
                    lineHeight = 16.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AyahRepeatCardOptions.forEach { count ->
                        val selected = count == ayahRepeat
                        RepetitionOptionChip(
                            text = formatRepeatLabel(count),
                            selected = selected,
                            onClick = { onAyahRepeatChange(count) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // ── Divider ──
            Divider(color = hBoneDark, thickness = 1.dp)

            // ── Sub-section 2: REPEAT ENTIRE RANGE ──
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "REPEAT ENTIRE RANGE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = hInkMid,
                        letterSpacing = 1.sp,
                        fontFamily = fontFamilyUi
                    )
                    Text(
                        text = "Active (${if (rangeRepeat == PlaybackSettings.REPEAT_INFINITE) "∞" else "${rangeRepeat}×"})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = hGold,
                        fontFamily = fontFamilyMono
                    )
                }

                Text(
                    text = "Number of loops for the selected surah or ayah block",
                    fontSize = 12.sp,
                    color = hInkMuted,
                    fontFamily = fontFamilyBody,
                    lineHeight = 16.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    RangeRepeatCardOptions.forEach { count ->
                        val selected = count == rangeRepeat
                        RepetitionOptionChip(
                            text = formatRepeatLabel(count),
                            selected = selected,
                            onClick = { onRangeRepeatChange(count) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // ── Divider ──
            Divider(color = hBoneDark, thickness = 1.dp)

            // ── Advanced Audio Settings (Expandable) ──
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { isAdvancedExpanded = !isAdvancedExpanded }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = NurIcons.Sliders,
                            contentDescription = null,
                            tint = hInkMid,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "ADVANCED AUDIO SETTINGS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = hInkMid,
                            letterSpacing = 1.sp,
                            fontFamily = fontFamilyUi
                        )
                    }
                    Icon(
                        imageVector = NurIcons.ChevronDown,
                        contentDescription = if (isAdvancedExpanded) "Collapse advanced settings" else "Expand advanced settings",
                        tint = hInkMuted,
                        modifier = Modifier
                            .size(16.dp)
                            .rotate(chevronRotation)
                    )
                }

                AnimatedVisibility(
                    visible = isAdvancedExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Playback Speed
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "PLAYBACK SPEED",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = hInkMuted,
                                    letterSpacing = 1.sp,
                                    fontFamily = fontFamilyUi
                                )
                                Text(
                                    text = "${formatSpeed(speed)}×",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = hGold,
                                    fontFamily = fontFamilyMono
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                SpeedCardOptions.forEach { opt ->
                                    val selected = kotlin.math.abs(speed - opt) < 0.05f
                                    RepetitionOptionChip(
                                        text = "${formatSpeed(opt)}×",
                                        selected = selected,
                                        onClick = { onSpeedChange(opt) },
                                        height = 36.dp,
                                        fontSize = 11.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        // Pause Delay Between Verses
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "PAUSE DELAY",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = hInkMuted,
                                    letterSpacing = 1.sp,
                                    fontFamily = fontFamilyUi
                                )
                                Text(
                                    text = if (delaySec <= 0f) "Off" else "${delaySec.toInt()}s",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = hGold,
                                    fontFamily = fontFamilyMono
                                )
                            }
                            Text(
                                text = "Silence added between ayahs for reflection",
                                fontSize = 11.sp,
                                color = hInkMuted,
                                fontFamily = fontFamilyBody
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                DelayCardOptions.forEach { (sec, label) ->
                                    val selected = kotlin.math.abs(delaySec - sec) < 0.05f
                                    RepetitionOptionChip(
                                        text = label,
                                        selected = selected,
                                        onClick = { onDelaySecChange(sec) },
                                        height = 36.dp,
                                        fontSize = 12.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        // Toggles
                        Divider(color = hBoneDark, thickness = 0.5.dp)

                        RepetitionSwitchRow(
                            title = "Save for offline",
                            subtitle = "Keep audio for offline listening",
                            checked = !streamOnly,
                            onCheckedChange = { onStreamOnlyChange(!it) }
                        )

                        Divider(color = hBoneDark, thickness = 0.5.dp)

                        RepetitionSwitchRow(
                            title = "Auto-scroll while playing",
                            subtitle = "Highlight and scroll to verse while playing",
                            checked = scrollWhilePlaying,
                            onCheckedChange = onScrollWhilePlayingChange
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RepetitionOptionChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 40.dp,
    fontSize: TextUnit = 13.sp,
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .height(height)
            .then(
                if (selected) {
                    Modifier.shadow(
                        elevation = 2.dp,
                        shape = shape,
                        spotColor = hGold.copy(alpha = 0.35f)
                    )
                } else {
                    Modifier
                }
            )
            .clip(shape)
            .background(if (selected) hGold else hWhite)
            .then(
                if (!selected) {
                    Modifier.border(width = 1.dp, color = hBorderColor, shape = shape)
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = fontSize,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) Color.White else hInk,
            fontFamily = fontFamilyUi,
            maxLines = 1
        )
    }
}

@Composable
private fun RepetitionSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = hInk,
                fontFamily = fontFamilyUi
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = hInkMuted,
                fontFamily = fontFamilyBody
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = hGold,
                uncheckedThumbColor = hInkMuted,
                uncheckedTrackColor = hBoneDark
            )
        )
    }
}

private fun formatRepeatLabel(count: Int): String {
    return if (count == PlaybackSettings.REPEAT_INFINITE) "∞" else "${count}×"
}

private fun formatSpeed(speed: Float): String {
    return if (speed == speed.toInt().toFloat()) speed.toInt().toString() else speed.toString()
}
