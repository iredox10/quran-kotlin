package com.nur.quran.ui.components.audio

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.nur.quran.ui.components.NurIcons
import com.nur.quran.ui.screens.fontFamilyMono
import com.nur.quran.ui.screens.hBoneDark
import com.nur.quran.ui.screens.hGold
import com.nur.quran.ui.screens.hGoldSoft
import com.nur.quran.ui.screens.hInk
import com.nur.quran.ui.screens.isDarkThemeGlobal

/**
 * Bottom pill mini-player, styled after [com.nur.quran.ui.components.AutoScrollerBar].
 *
 * Props only — no ViewModel, no ExoPlayer reference. The host (SurahScreen)
 * passes the currently-playing [verseKey] plus transport lambdas and decides
 * visibility (e.g. `visible = isPlaying || playingVerseKey != null`).
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
) {
    val pillBg = if (isDarkThemeGlobal) Color(0xF22D2D2A) else Color(0xF2EFECE4)
    val pillBorder = if (isDarkThemeGlobal) Color(0xFF4A4A45) else Color(0x66FFFFFF)
    val btnSecondaryBg = if (isDarkThemeGlobal) Color(0xFF3A3A36) else Color(0xFFEDE8DA)

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
                shape = CircleShape,
                color = pillBg,
                border = BorderStroke(1.dp, pillBorder),
                shadowElevation = 14.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Prev / Next
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(btnSecondaryBg)
                                .clickable { onPrevious() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = NurIcons.ChevronLeft,
                                contentDescription = "Previous ayah",
                                tint = hInk,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(btnSecondaryBg)
                                .clickable { onNext() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = NurIcons.ChevronRight,
                                contentDescription = "Next ayah",
                                tint = hInk,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(
                        modifier = Modifier
                            .width(1.dp)
                            .height(20.dp)
                            .background(hBoneDark)
                    )

                    // Current verse key
                    Text(
                        text = verseKey.ifBlank { "—" },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = fontFamilyMono,
                        color = hInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    // Play / Pause toggle
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (isPlaying) Color.Transparent else hGoldSoft)
                            .clickable { onPlayPause() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) NurIcons.PauseFilled else NurIcons.PlayFilled,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = hGold,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(
                        modifier = Modifier
                            .width(1.dp)
                            .height(20.dp)
                            .background(hBoneDark)
                    )

                    // Close
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0x1AEF4444))
                            .clickable { onClose() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = NurIcons.X,
                            contentDescription = "Close player",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
