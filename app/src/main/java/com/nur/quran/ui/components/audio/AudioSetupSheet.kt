package com.nur.quran.ui.components.audio

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.data.audio.PlaybackSettings.Companion.REPEAT_INFINITE
import com.nur.quran.data.audio.Reciters
import com.nur.quran.data.mushaf.ChapterMetadata
import com.nur.quran.ui.components.NurIcons
import com.nur.quran.ui.screens.fontFamilyUi
import com.nur.quran.ui.screens.hBone
import com.nur.quran.ui.screens.hBorderColor
import com.nur.quran.ui.screens.hCream
import com.nur.quran.ui.screens.hGold
import com.nur.quran.ui.screens.hInk
import com.nur.quran.ui.screens.hInkMid

val AyahRepeatOptions = listOf(1, 2, 3, 5, 10, REPEAT_INFINITE)
val RangeRepeatOptions = listOf(1, 2, 3, 5, REPEAT_INFINITE)
val SpeedOptions = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)

fun repeatLabel(value: Int): String = if (value == REPEAT_INFINITE) "∞" else "×$value"

/**
 * Redesigned Audio & Recitation Setup Modal Sheet.
 *
 * Visual hierarchy:
 * 1. Sheet Handle Bar: subtle taupe rounded pill.
 * 2. Header:
 *    - Title: "Audio & Recitation" (bold 18.sp, hInk).
 *    - Subtitle: "SURAH ${chapterName.uppercase()}" (bold 11.sp, letterSpacing 1.2.sp, hGold).
 *    - Circular Close Button with NurIcons.X on the top right.
 * 3. Card 1: [ChosenReciterCard] (Avatar with Arabic calligraphy initials, verified checkmark,
 *    "Hafs an Asim" badge, Reciter name, style description, chevron, and 1:1 preview play button).
 * 4. Card 2: [PlaybackRangeCard] (Ayah playback range with 3-segment switcher: Full Surah, Page range,
 *    Custom Range; From/To readouts; dual-thumb golden ring range slider; and start/total ayah labels).
 * 5. Card 3: [RepetitionCard] (Ayah repetition chips for memorization, range loop repetition chips,
 *    and collapsible advanced options for speed, pause delay, offline download, and auto-scroll).
 * 6. Sticky Bottom CTA: [AudioPlayButtonCta] (rich golden amber button: "Play Full Surah • 48 min").
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AudioSetupSheet(
    chapterId: Int,
    versesCount: Int,
    chapterName: String = "",
    pagesStart: Int = 0,
    pagesEnd: Int = 0,
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
    val safeCount = versesCount.coerceAtLeast(1)

    // Chapter metadata resolution
    val chapterDisplayName = remember(chapterId, chapterName) {
        if (chapterName.isNotBlank()) chapterName else ChapterMetadata.nameById(chapterId)
    }
    val (defaultStartPage, defaultEndPage) = remember(chapterId) {
        ChapterMetadata.pageRangeById(chapterId)
    }
    val effectiveStartPage = if (pagesStart > 0) pagesStart else defaultStartPage
    val effectiveEndPage = if (pagesEnd > 0) pagesEnd else defaultEndPage

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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = hCream,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 6.dp)
                    .width(42.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(hBorderColor)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            // ── Top Header matching mockup ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Audio & Recitation",
                        fontFamily = fontFamilyUi,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = hInk
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "SURAH ${chapterDisplayName.uppercase()}",
                        fontFamily = fontFamilyUi,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.2.sp,
                        color = hGold
                    )
                }

                // Close Button on top-right
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(hBone)
                        .clickable(onClick = onDismiss)
                        .align(Alignment.CenterEnd),
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

            Spacer(modifier = Modifier.height(6.dp))

            // ── Scrollable Body with Cards ──
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Card 1: Chosen Reciter
                ChosenReciterCard(
                    selectedReciterId = reciterId,
                    onReciterSelected = { reciterId = it },
                    riwayah = "Hafs an Asim"
                )

                // Card 2: Ayah Playback Range
                PlaybackRangeCard(
                    chapterName = chapterDisplayName,
                    totalAyahs = safeCount,
                    startPage = effectiveStartPage,
                    endPage = effectiveEndPage,
                    startAyah = startAyah,
                    endAyah = endAyah,
                    onRangeChange = { start, end ->
                        startAyah = start
                        endAyah = end
                    }
                )

                // Card 3: Repetition (Ayah loop + Range loop + Advanced settings)
                RepetitionCard(
                    ayahRepeat = ayahRepeat,
                    onAyahRepeatChange = { ayahRepeat = it },
                    rangeRepeat = rangeRepeat,
                    onRangeRepeatChange = { rangeRepeat = it },
                    speed = speed,
                    onSpeedChange = { speed = it },
                    delaySec = delaySec,
                    onDelaySecChange = { delaySec = it },
                    streamOnly = streamOnly,
                    onStreamOnlyChange = { streamOnly = it },
                    scrollWhilePlaying = scrollWhilePlaying,
                    onScrollWhilePlayingChange = {
                        scrollWhilePlaying = it
                        onScrollWhilePlayingChange(it)
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))
            }

            // ── Sticky Bottom CTA Button ──
            val isFullSurah = (startAyah == 1 && endAyah == safeCount)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                AudioPlayButtonCta(
                    isFullSurah = isFullSurah,
                    startAyah = startAyah,
                    endAyah = endAyah,
                    speed = speed,
                    onClick = {
                        if (isFullSurah) {
                            onPlayAll(
                                reciterId,
                                ayahRepeat,
                                rangeRepeat,
                                (delaySec * 1000).toLong(),
                                speed,
                                streamOnly
                            )
                        } else {
                            onPlayRange(
                                reciterId,
                                "$chapterId:$startAyah",
                                "$chapterId:$endAyah",
                                ayahRepeat,
                                rangeRepeat,
                                (delaySec * 1000).toLong(),
                                speed,
                                streamOnly
                            )
                        }
                        onDismiss()
                    }
                )
            }
        }
    }
}
