package com.nur.quran.ui.components.audio

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.nur.quran.R
import com.nur.quran.ui.components.NurIcons
import com.nur.quran.ui.screens.fontFamilyMono
import com.nur.quran.ui.screens.fontFamilyUi
import com.nur.quran.ui.screens.hGold

private val playerBarBg = Color(0xFF1E4A38)
private val playerTitleColor = Color(0xFFF5EFE0)
private val playerSubtitleColor = Color(0xFFB9C8BE)
private val playerTrackColor = Color(0xFF3A6B58)
private val playerOnGoldColor = Color(0xFF1E4A38)

/**
 * Compact bottom player bar in the style of a music mini-player: rounded deep-green
 * bar with mushaf artwork, surah title + ayah subtitle, prev/play/next cluster and
 * a thin gold progress track (shown only when [progress] is provided).
 *
 * Props only — no ViewModel, no ExoPlayer reference. Hosts pass the currently-playing
 * [verseKey] plus transport lambdas and decide visibility. Progress comes from the
 * host ViewModel's polled `(positionMs, durationMs)` as a 0..1 fraction.
 */
@Composable
fun MiniPlayer(
    verseKey: String,
    isPlaying: Boolean,
    visible: Boolean = true,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    progress: Float? = null,
    onSettings: (() -> Unit)? = null,
    onStop: (() -> Unit)? = null,
    onVerseClick: (() -> Unit)? = null,
) {
    // Title = surah name, subtitle = ayah number (clickable to jump to reciting ayah).
    val (titleText, subtitleText) = androidx.compose.runtime.remember(verseKey) {
        if (verseKey.isBlank()) {
            "—" to ""
        } else {
            val parts = verseKey.split(":")
            val chId = parts.getOrNull(0)?.toIntOrNull()
            val ayahNo = parts.getOrNull(1)
            val surah = if (chId != null) {
                runCatching {
                    com.nur.quran.data.mushaf.ChapterMetadata.nameById(chId)
                }.getOrNull()
            } else {
                null
            }
            if (surah != null && ayahNo != null) surah to "Ayah $ayahNo"
            else verseKey to ""
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = 24.dp)
            .zIndex(100f),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = playerBarBg,
                shadowElevation = 14.dp
            ) {
                Column {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Artwork thumb.
                        Image(
                            painter = painterResource(id = R.drawable.ic_logo),
                            contentDescription = "Quran Nur",
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )

                        // Title + subtitle.
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .then(
                                    if (onVerseClick != null) Modifier.clickable { onVerseClick() }
                                    else Modifier
                                )
                        ) {
                            Text(
                                text = titleText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = fontFamilyUi,
                                color = playerTitleColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (subtitleText.isNotEmpty()) {
                                Text(
                                    text = subtitleText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = fontFamilyMono,
                                    color = playerSubtitleColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Transport cluster: prev / play / next.
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .clickable { onPrevious() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = NurIcons.ChevronLeft,
                                    contentDescription = "Previous ayah",
                                    tint = playerTitleColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(hGold)
                                    .clickable { onPlayPause() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) NurIcons.PauseFilled else NurIcons.PlayFilled,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = playerOnGoldColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .clickable { onNext() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = NurIcons.ChevronRight,
                                    contentDescription = "Next ayah",
                                    tint = playerTitleColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Audio settings (gear) — only when the host passes onSettings.
                        if (onSettings != null) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .clickable { onSettings() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = NurIcons.Settings,
                                    contentDescription = "Audio settings",
                                    tint = playerSubtitleColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Stop (■) — dedicated stop: resets to range start + hides.
                        // Distinct from Close (X) which only hides the bar and keeps
                        // playing in the background. Only when the host passes onStop.
                        if (onStop != null) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .clickable { onStop() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = NurIcons.Square,
                                    contentDescription = "Stop playback",
                                    tint = playerSubtitleColor,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        // Close.
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .clickable { onClose() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = NurIcons.X,
                                contentDescription = "Close player",
                                tint = playerSubtitleColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Progress track — only while the host reports position.
                    if (progress != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .background(playerTrackColor)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                                    .height(3.dp)
                                    .background(hGold)
                            )
                        }
                    }
                }
            }
        }
    }
}
