package com.nur.quran.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.nur.quran.ui.screens.fontFamilyMono
import com.nur.quran.ui.screens.hBoneDark
import com.nur.quran.ui.screens.hGold
import com.nur.quran.ui.screens.hGoldSoft
import com.nur.quran.ui.screens.hInk
import com.nur.quran.ui.screens.isDarkThemeGlobal

@Composable
fun AutoScrollerBar(
    isAutoScrollActive: Boolean,
    isAutoScrollPaused: Boolean,
    autoScrollSpeed: Int,
    onPauseToggle: () -> Unit,
    onSpeedChange: (Int) -> Unit,
    onJumpUp: () -> Unit,
    onJumpDown: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
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
            visible = isAutoScrollActive,
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
                    modifier = Modifier
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Quick Jump Up / Down buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(btnSecondaryBg)
                                .clickable { onJumpUp() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = NurIcons.ArrowLeft,
                                contentDescription = "Jump Up",
                                tint = hInk,
                                modifier = Modifier
                                    .size(14.dp)
                                    .rotate(90f)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(btnSecondaryBg)
                                .clickable { onJumpDown() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = NurIcons.ArrowRight,
                                contentDescription = "Jump Down",
                                tint = hInk,
                                modifier = Modifier
                                    .size(14.dp)
                                    .rotate(90f)
                            )
                        }
                    }

                    Spacer(
                        modifier = Modifier
                            .width(1.dp)
                            .height(20.dp)
                            .background(hBoneDark)
                    )

                    // Speed controls: Minus | 3x | Plus
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .border(1.dp, hBoneDark, CircleShape)
                            .clickable { onSpeedChange(maxOf(1, autoScrollSpeed - 1)) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = NurIcons.Minus,
                            contentDescription = "Decrease Speed",
                            tint = hInk,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Text(
                        text = "${autoScrollSpeed}x",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = fontFamilyMono,
                        color = hInk,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .border(1.dp, hBoneDark, CircleShape)
                            .clickable { onSpeedChange(minOf(7, autoScrollSpeed + 1)) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = NurIcons.Plus,
                            contentDescription = "Increase Speed",
                            tint = hInk,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    // Pause / Play toggle
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (isAutoScrollPaused) hGoldSoft else Color.Transparent)
                            .clickable { onPauseToggle() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isAutoScrollPaused) NurIcons.PlayFilled else NurIcons.PauseFilled,
                            contentDescription = if (isAutoScrollPaused) "Play" else "Pause",
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

                    // Stop / Close button
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
                            contentDescription = "Close Auto-scroll",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
