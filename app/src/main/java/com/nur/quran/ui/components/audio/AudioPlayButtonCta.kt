package com.nur.quran.ui.components.audio

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.ui.components.NurIcons
import com.nur.quran.ui.screens.fontFamilyUi

/**
 * Rich golden amber colors matching the redesigned Audio Settings Modal CTA button.
 */
private val CtaGoldTop = Color(0xFFC8931C)
private val CtaGoldBottom = Color(0xFFB47D0B)
private val CtaGoldBrush = Brush.verticalGradient(listOf(CtaGoldTop, CtaGoldBottom))
private val CtaDisabledColor = Color(0xFFC5B699)

/**
 * Prominent bottom CTA play button for the redesigned Audio Settings Sheet.
 *
 * Visual specification:
 * - Height: 58.dp, full width, rounded corner (18.dp).
 * - Background: Rich golden amber gradient (`#C8931C` to `#B47D0B`) with soft amber drop shadow.
 * - Circular play badge: 34.dp circle with translucent white overlay (`Color.White.copy(alpha = 0.22f)`),
 *   containing a warm white play triangle ([NurIcons.PlayFilled]).
 * - Button text: "Play Full Surah • 48 min" or "Play Ayahs $startAyah–$endAyah • $duration"
 *   in bold 16.sp white text with [fontFamilyUi].
 *
 * @param isFullSurah Whether the full surah is selected for playback.
 * @param startAyah The starting ayah number of the playback range.
 * @param endAyah The ending ayah number of the playback range.
 * @param durationMinutes The estimated duration in minutes.
 * @param onClick Callback invoked when the CTA button is tapped.
 * @param modifier Optional modifier applied to the outer button container.
 * @param enabled Whether the button is interactable.
 * @param isLoading Whether an audio buffering / loading indicator should be shown.
 * @param durationLabel Optional explicit duration label (e.g. "48 min", "45 sec") overriding [durationMinutes].
 */
@Composable
fun AudioPlayButtonCta(
    isFullSurah: Boolean,
    startAyah: Int,
    endAyah: Int,
    durationMinutes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    durationLabel: String? = null,
) {
    val durationText = durationLabel ?: if (durationMinutes > 0) {
        AudioDurationEstimator.formatMinutes(durationMinutes)
    } else {
        AudioDurationEstimator.estimateDuration(startAyah, endAyah)
    }

    val actionText = when {
        isFullSurah -> "Play Full Surah"
        startAyah == endAyah -> "Play Ayah $startAyah"
        else -> "Play Ayahs $startAyah–$endAyah"
    }

    val fullLabel = "$actionText • $durationText"

    val backgroundModifier = if (enabled) {
        Modifier
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = Color(0x33B47D0B),
                spotColor = Color(0x66B47D0B)
            )
            .background(CtaGoldBrush)
    } else {
        Modifier.background(CtaDisabledColor)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .then(backgroundModifier)
            .clip(RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(color = Color.White.copy(alpha = 0.3f)),
                enabled = enabled && !isLoading,
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 20.dp)
        ) {
            // Circular play icon badge
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Icon(
                        imageVector = NurIcons.PlayFilled,
                        contentDescription = null,
                        tint = Color(0xFFFFF8E8),
                        modifier = Modifier
                            .size(15.dp)
                            .padding(start = 2.dp) // Optical center correction for play triangle
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = fullLabel,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = fontFamilyUi,
                letterSpacing = (-0.2).sp
            )
        }
    }
}

/**
 * Overload of [AudioPlayButtonCta] that automatically calculates estimated duration
 * using [AudioDurationEstimator] based on ayah range and playback speed.
 */
@Composable
fun AudioPlayButtonCta(
    isFullSurah: Boolean,
    startAyah: Int,
    endAyah: Int,
    speed: Float = 1.0f,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
) {
    val durationMinutes = AudioDurationEstimator.estimateMinutes(startAyah, endAyah, speed)
    val durationLabel = AudioDurationEstimator.estimateDuration(startAyah, endAyah, speed)

    AudioPlayButtonCta(
        isFullSurah = isFullSurah,
        startAyah = startAyah,
        endAyah = endAyah,
        durationMinutes = durationMinutes,
        durationLabel = durationLabel,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        isLoading = isLoading,
    )
}

/**
 * Overload of [AudioPlayButtonCta] for full surah playback with explicit duration minutes.
 */
@Composable
fun AudioPlayButtonCta(
    isFullSurah: Boolean,
    durationMinutes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
) {
    AudioPlayButtonCta(
        isFullSurah = isFullSurah,
        startAyah = 1,
        endAyah = 1,
        durationMinutes = durationMinutes,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        isLoading = isLoading,
    )
}

// ── Previews ──

@Preview(showBackground = true, backgroundColor = 0xFFFAF7F0)
@Composable
private fun AudioPlayButtonCtaFullSurahPreview() {
    Box(modifier = Modifier.padding(16.dp)) {
        AudioPlayButtonCta(
            isFullSurah = true,
            startAyah = 1,
            endAyah = 286,
            durationMinutes = 48,
            onClick = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFAF7F0)
@Composable
private fun AudioPlayButtonCtaRangePreview() {
    Box(modifier = Modifier.padding(16.dp)) {
        AudioPlayButtonCta(
            isFullSurah = false,
            startAyah = 1,
            endAyah = 20,
            durationMinutes = 3,
            onClick = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFAF7F0)
@Composable
private fun AudioPlayButtonCtaSingleAyahPreview() {
    Box(modifier = Modifier.padding(16.dp)) {
        AudioPlayButtonCta(
            isFullSurah = false,
            startAyah = 5,
            endAyah = 5,
            durationMinutes = 1,
            durationLabel = "10 sec",
            onClick = {}
        )
    }
}
