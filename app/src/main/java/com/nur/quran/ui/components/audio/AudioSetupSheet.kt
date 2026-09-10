package com.nur.quran.ui.components.audio

import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.data.audio.PlaybackSettings.Companion.REPEAT_INFINITE
import com.nur.quran.data.audio.Reciter
import com.nur.quran.data.audio.Reciters
import com.nur.quran.ui.components.NurIcons
import com.nur.quran.ui.screens.fontFamilyBody
import com.nur.quran.ui.screens.fontFamilyMono
import com.nur.quran.ui.screens.fontFamilyUi
import com.nur.quran.ui.screens.hBoneDark
import com.nur.quran.ui.screens.hBorderColor
import com.nur.quran.ui.screens.hCream
import com.nur.quran.ui.screens.hGold
import com.nur.quran.ui.screens.hInk
import com.nur.quran.ui.screens.hInkMid
import com.nur.quran.ui.screens.hInkMuted
import com.nur.quran.ui.screens.hSurface
import com.nur.quran.ui.screens.hWhite

/**
 * Reciter list comes from [Reciters] (`data/audio/Reciters.kt`), the canonical
 * source also used by `SurahViewModel` (`reciter_id` pref, default 7).
 * If that file is ever removed, inline the 7 core ids here instead.
 */

val AyahRepeatOptions = listOf(1, 2, 3, 5, 10, REPEAT_INFINITE)
val RangeRepeatOptions = listOf(1, 2, 3, 5, REPEAT_INFINITE)
val SpeedOptions = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)

fun repeatLabel(value: Int): String = if (value == REPEAT_INFINITE) "∞" else "×$value"

/**
 * Bottom-sheet replacement for the AlertDialog audio setup in SurahScreen.
 *
 * All state is hoisted by the caller via `initial*` params; this sheet owns
 * only transient UI state (dropdown expanded, slider positions) and reports
 * the confirmed selection through [onPlayRange] / [onPlayAll].
 *
 * Verse keys are derived as `"$chapterId:$ayahNumber"`. The confirmed values
 * map 1:1 onto `PlaybackSettings(ayahRepeat, rangeRepeat, delayMs, speed,
 * rangeStart = startKey, rangeEnd = endKey)` plus a reciter id.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AudioSetupSheet(
    chapterId: Int,
    versesCount: Int,
    initialReciterId: Int = 7,
    initialStartAyah: Int = 1,
    initialEndAyah: Int = versesCount.coerceAtLeast(1),
    initialAyahRepeat: Int = 1,
    initialRangeRepeat: Int = 1,
    initialDelayMs: Long = 0L,
    initialSpeed: Float = 1f,
    initialStreamOnly: Boolean = false,
    initialScrollWhilePlaying: Boolean = true,
    onScrollWhilePlayingChange: (Boolean) -> Unit = {},
    onDismiss: () -> Unit,
    onPlayRange: (reciterId: Int, startKey: String, endKey: String, ayahRepeat: Int, rangeRepeat: Int, delayMs: Long, speed: Float, streamOnly: Boolean) -> Unit,
    onPlayAll: (reciterId: Int, ayahRepeat: Int, rangeRepeat: Int, delayMs: Long, speed: Float, streamOnly: Boolean) -> Unit,
) {
    // Alias matching the requested `onConfirm(reciterId, startKey, endKey,
    // ayahRepeat, rangeRepeat, delayMs, speed)` wiring name.
    val onConfirm = onPlayRange

    val safeCount = versesCount.coerceAtLeast(1)
    var reciterId by remember(initialReciterId) { mutableIntStateOf(initialReciterId) }
    var startAyah by remember(initialStartAyah) {
        mutableIntStateOf(initialStartAyah.coerceIn(1, safeCount))
    }
    var endAyah by remember(initialEndAyah) {
        mutableIntStateOf(initialEndAyah.coerceIn(1, safeCount))
    }
    var ayahRepeat by remember { mutableIntStateOf(initialAyahRepeat) }
    var rangeRepeat by remember { mutableIntStateOf(initialRangeRepeat) }
    var delaySec by remember { mutableFloatStateOf(initialDelayMs.coerceIn(0L, 10_000L) / 1000f) }
    var speed by remember { mutableFloatStateOf(initialSpeed) }
    var streamOnly by remember { mutableStateOf(initialStreamOnly) }
    var scrollWhilePlaying by remember(initialScrollWhilePlaying) {
        mutableStateOf(initialScrollWhilePlaying)
    }
    var reciterExpanded by remember { mutableStateOf(false) }

    val selectedReciter = Reciters.byId(reciterId) ?: Reciters.byId(Reciters.DEFAULT_ID)!!

    // ── Reciter preview player (sample verse 1:1 stream, never throws) ──
    var previewPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var previewPlayingId by remember { mutableIntStateOf(-1) }
    var previewLoadingId by remember { mutableIntStateOf(-1) }

    fun stopPreview() {
        runCatching {
            previewPlayer?.setOnPreparedListener(null)
            previewPlayer?.setOnCompletionListener(null)
            previewPlayer?.setOnErrorListener(null)
            runCatching { previewPlayer?.stop() }
            previewPlayer?.release()
        }
        previewPlayer = null
        previewPlayingId = -1
        previewLoadingId = -1
    }

    fun startPreview(targetId: Int) {
        // Toggle off when tapping the reciter that is already previewing/loading.
        if (previewPlayingId == targetId || previewLoadingId == targetId) {
            stopPreview()
            return
        }
        val url = runCatching { Reciters.buildAudioUrl(targetId, "1:1") }.getOrNull() ?: return
        stopPreview()
        previewLoadingId = targetId
        runCatching {
            val player = MediaPlayer()
            previewPlayer = player
            player.setDataSource(url)
            player.setOnPreparedListener {
                if (previewLoadingId == targetId) {
                    previewLoadingId = -1
                    previewPlayingId = targetId
                    runCatching { it.start() }
                } else {
                    runCatching { it.release() }
                }
            }
            player.setOnCompletionListener { stopPreview() }
            player.setOnErrorListener { _, _, _ ->
                stopPreview()
                true
            }
            player.prepareAsync()
        }.onFailure { stopPreview() }
    }

    fun previewUrlOrNull(id: Int): String? =
        runCatching { Reciters.buildAudioUrl(id, "1:1") }.getOrNull()

    val selectedPreviewUrl = remember(selectedReciter.id) { previewUrlOrNull(selectedReciter.id) }

    // Release the preview player when the sheet dismisses.
    DisposableEffect(Unit) {
        onDispose { stopPreview() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = hWhite,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(40.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(hBorderColor)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Audio Settings",
                    fontFamily = fontFamilyUi,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = hInk
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(hSurface)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = NurIcons.X,
                        contentDescription = "Close",
                        tint = hInkMid,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // ── Reciter dropdown (grouped by style) + preview ──
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SheetSectionLabel("RECITER")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(hCream)
                                .clickable { reciterExpanded = true }
                                .padding(horizontal = 14.dp, vertical = 13.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedReciter.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = hInk,
                                fontFamily = fontFamilyUi,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = NurIcons.ChevronDown,
                                contentDescription = "Choose reciter",
                                tint = hInkMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = reciterExpanded,
                            onDismissRequest = { reciterExpanded = false },
                            modifier = Modifier.heightIn(max = 320.dp)
                        ) {
                            val grouped =
                                runCatching { Reciters.groupedByStyle() }.getOrNull()
                            if (grouped.isNullOrEmpty()) {
                                Reciters.ALL.forEach { option ->
                                    ReciterMenuRow(
                                        option = option,
                                        selected = option.id == reciterId,
                                        previewTrailing = if (previewUrlOrNull(option.id) != null) {
                                            {
                                                ReciterPreviewButton(
                                                    playing = previewPlayingId == option.id,
                                                    loading = previewLoadingId == option.id,
                                                    onClick = { startPreview(option.id) }
                                                )
                                            }
                                        } else null,
                                        onClick = {
                                            reciterId = option.id
                                            reciterExpanded = false
                                        }
                                    )
                                }
                            } else {
                                val styleOrder = listOf(
                                    Reciters.STYLE_MURATTAL,
                                    Reciters.STYLE_MUJAWWAD,
                                    Reciters.STYLE_MUALLIM
                                )
                                val orderedStyles =
                                    styleOrder.filter { grouped.containsKey(it) } +
                                        (grouped.keys - styleOrder.toSet()).sorted()
                                orderedStyles.forEach { style ->
                                    Text(
                                        text = style.uppercase(),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = hInkMuted,
                                        letterSpacing = 1.sp,
                                        fontFamily = fontFamilyMono,
                                        modifier = Modifier.padding(
                                            horizontal = 12.dp,
                                            vertical = 6.dp
                                        )
                                    )
                                    grouped[style].orEmpty().forEach { option ->
                                        ReciterMenuRow(
                                            option = option,
                                            selected = option.id == reciterId,
                                            previewTrailing = if (previewUrlOrNull(option.id) != null) {
                                                {
                                                    ReciterPreviewButton(
                                                        playing = previewPlayingId == option.id,
                                                        loading = previewLoadingId == option.id,
                                                        onClick = { startPreview(option.id) }
                                                    )
                                                }
                                            } else null,
                                            onClick = {
                                                reciterId = option.id
                                                reciterExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    // Preview button for the currently selected reciter; hidden
                    // when buildAudioUrl has no streamable sample (null).
                    if (selectedPreviewUrl != null) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(hCream)
                                .clickable { startPreview(selectedReciter.id) },
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                previewLoadingId == selectedReciter.id -> {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = hGold
                                    )
                                }
                                previewPlayingId == selectedReciter.id -> {
                                    Icon(
                                        imageVector = NurIcons.PauseFilled,
                                        contentDescription = "Stop preview",
                                        tint = hGold,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                else -> {
                                    Icon(
                                        imageVector = NurIcons.Play,
                                        contentDescription = "Preview reciter voice",
                                        tint = hInkMid,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Ayah range: compact dual sliders in one row ──
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SheetSectionLabel("AYAH RANGE")
                    Text(
                        text = if (startAyah == 1 && endAyah == safeCount) "Full surah"
                        else "$chapterId:$startAyah – $chapterId:$endAyah",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = hGold,
                        fontFamily = fontFamilyMono
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("From", fontSize = 11.sp, color = hInkMuted, fontFamily = fontFamilyBody)
                    Slider(
                        value = startAyah.toFloat(),
                        onValueChange = {
                            startAyah = it.toInt().coerceIn(1, safeCount)
                            if (endAyah < startAyah) endAyah = startAyah
                        },
                        valueRange = 1f..safeCount.toFloat(),
                        steps = (safeCount - 2).coerceAtLeast(0),
                        colors = SliderDefaults.colors(thumbColor = hGold, activeTrackColor = hGold),
                        modifier = Modifier.weight(1f)
                    )
                    Text("To", fontSize = 11.sp, color = hInkMuted, fontFamily = fontFamilyBody)
                    Slider(
                        value = endAyah.toFloat(),
                        onValueChange = { endAyah = it.toInt().coerceIn(startAyah, safeCount) },
                        valueRange = startAyah.toFloat()..safeCount.toFloat(),
                        steps = ((safeCount - startAyah) - 1).coerceAtLeast(0),
                        colors = SliderDefaults.colors(thumbColor = hGold, activeTrackColor = hGold),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ── Repeat card: ayah × selection + delay chips ──
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SheetSectionLabel("REPEAT")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AyahRepeatOptions.forEach { option ->
                        RepeatChip(
                            label = "Ayah ${repeatLabel(option)}",
                            selected = ayahRepeat == option,
                            onClick = { ayahRepeat = option }
                        )
                    }
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RangeRepeatOptions.forEach { option ->
                        RepeatChip(
                            label = "All ${repeatLabel(option)}",
                            selected = rangeRepeat == option,
                            onClick = { rangeRepeat = option }
                        )
                    }
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Pause", fontSize = 12.sp, color = hInkMuted, fontFamily = fontFamilyBody,
                        modifier = Modifier.align(Alignment.CenterVertically))
                    listOf(0, 1, 2, 3, 5, 10).forEach { secs ->
                        RepeatChip(
                            label = if (secs == 0) "Off" else "${secs}s",
                            selected = delaySec.toInt() == secs,
                            onClick = { delaySec = secs.toFloat() }
                        )
                    }
                }
            }

            // ── Speed chips ──
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SheetSectionLabel("SPEED  •  ${formatSpeed(speed)}×")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SpeedOptions.forEach { option ->
                        RepeatChip(
                            label = "${formatSpeed(option)}×",
                            selected = speed == option,
                            onClick = { speed = option }
                        )
                    }
                }
            }

            // ── Toggles: offline save + follow (standard Switch rows) ──
            Column {
                SettingSwitchRow(
                    title = "Save for offline",
                    subtitle = "Keep audio for offline listening",
                    checked = !streamOnly,
                    onCheckedChange = { streamOnly = !it }
                )
                Divider(color = hBoneDark)
                SettingSwitchRow(
                    title = "Follow ayahs",
                    subtitle = "Highlight and scroll while playing",
                    checked = scrollWhilePlaying,
                    onCheckedChange = {
                        scrollWhilePlaying = it
                        onScrollWhilePlayingChange(it)
                    }
                )
            }

            // ── Single primary action (standard one-CTA sheet) ──
            val isFullRange = startAyah == 1 && endAyah == safeCount
            Button(
                onClick = {
                    stopPreview()
                    if (isFullRange) {
                        onPlayAll(reciterId, ayahRepeat, rangeRepeat, (delaySec.toInt() * 1000).toLong(), speed, streamOnly)
                    } else {
                        onConfirm(
                            reciterId,
                            "$chapterId:$startAyah",
                            "$chapterId:$endAyah",
                            ayahRepeat,
                            rangeRepeat,
                            (delaySec.toInt() * 1000).toLong(),
                            speed,
                            streamOnly
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = hGold),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = NurIcons.PlayFilled,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = if (isFullRange) "  Play full surah"
                    else "  Play ayahs $startAyah–$endAyah",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontFamily = fontFamilyUi,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun SheetSectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = hInkMuted,
        letterSpacing = 1.sp,
        fontFamily = fontFamilyMono
    )
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = hInk,
                fontFamily = fontFamilyUi
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = hInkMuted,
                fontFamily = fontFamilyBody
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = hGold
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RepeatChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                fontFamily = fontFamilyMono
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = hGold,
            containerColor = hCream
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaybackModeChip(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Column {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    fontFamily = fontFamilyUi
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    color = if (selected) Color.White.copy(alpha = 0.9f) else hInkMuted,
                    fontFamily = fontFamilyBody
                )
            }
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = hGold,
            containerColor = hCream
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReciterMenuRow(
    option: Reciter,
    selected: Boolean,
    previewTrailing: (@Composable () -> Unit)?,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = {
            Column {
                Text(
                    text = option.name,
                    fontSize = 14.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    color = if (selected) hGold else hInk
                )
                Text(
                    text = option.style,
                    fontSize = 11.sp,
                    color = hInkMuted,
                    fontFamily = fontFamilyMono
                )
            }
        },
        trailingIcon = if (selected) {
            {
                Icon(
                    imageVector = NurIcons.Check,
                    contentDescription = null,
                    tint = hGold,
                    modifier = Modifier.size(16.dp)
                )
            }
        } else previewTrailing,
        onClick = onClick
    )
}

@Composable
private fun ReciterPreviewButton(
    playing: Boolean,
    loading: Boolean,
    onClick: () -> Unit,
) {
    when {
        loading -> {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = hGold
            )
        }
        playing -> {
            IconButton(onClick = onClick, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = NurIcons.PauseFilled,
                    contentDescription = "Stop preview",
                    tint = hGold,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        else -> {
            IconButton(onClick = onClick, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = NurIcons.Play,
                    contentDescription = "Preview reciter voice",
                    tint = hInkMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

private fun formatSpeed(speed: Float): String {
    return if (speed == speed.toInt().toFloat()) speed.toInt().toString() else speed.toString()
}
