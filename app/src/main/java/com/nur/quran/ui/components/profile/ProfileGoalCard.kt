package com.nur.quran.ui.components.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.ui.components.NurIcons
import com.nur.quran.ui.screens.*

/**
 * Sleek Daily Goal Card matching web app Profile.jsx lines 174-214.
 */
@Composable
fun ProfileGoalCard(
    todayTotalSeconds: Long,
    dailyGoalMins: Int,
    onGoalSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var showGoalPicker by remember { mutableStateOf(false) }

    val todayMins = Math.round(todayTotalSeconds / 60.0).toInt()
    val goalPct = if (dailyGoalMins > 0) {
        ((todayMins.toFloat() / dailyGoalMins.toFloat()) * 100f).coerceIn(0f, 100f)
    } else 0f

    val goalOptions = remember { listOf(10, 15, 20, 30, 45, 60) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = hCream),
        border = BorderStroke(1.5.dp, hBoneDark)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(hGoldLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = NurIcons.Clock,
                            contentDescription = null,
                            tint = hGold,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Daily Goal",
                            fontFamily = fontFamilyUi,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = hInk
                        )
                        Text(
                            text = "${ProfileUtils.formatMinutes(todayTotalSeconds)} / ${dailyGoalMins}m today",
                            fontFamily = fontFamilyBody,
                            fontSize = 12.sp,
                            color = hInkMuted
                        )
                    }
                }

                IconButton(
                    onClick = { showGoalPicker = !showGoalPicker },
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(hSurface)
                        .border(1.dp, hBoneDark, CircleShape)
                ) {
                    Icon(
                        imageVector = NurIcons.Settings,
                        contentDescription = "Configure Goal",
                        tint = hInkMuted,
                        modifier = Modifier
                            .size(14.dp)
                            .rotate(if (showGoalPicker) 90f else 0f)
                    )
                }
            }

            // Two-tone gradient progress bar (Gold to Green)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(hSurface)
            ) {
                if (goalPct > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = goalPct / 100f)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(hGold, Color(0xFF10B981))
                                )
                            )
                    )
                }
            }

            // Expandable Goal Options Row
            AnimatedVisibility(
                visible = showGoalPicker,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Divider(color = hBoneDark.copy(alpha = 0.6f), thickness = 1.dp)

                    Text(
                        text = "SELECT DAILY GOAL (MINUTES)",
                        fontFamily = fontFamilyMono,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = hInkMuted
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        goalOptions.forEach { mins ->
                            val isSelected = dailyGoalMins == mins
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(CircleShape)
                                    .background(if (isSelected) hGold else hSurface)
                                    .border(
                                        1.dp,
                                        if (isSelected) hGold else hBoneDark,
                                        CircleShape
                                    )
                                    .clickable {
                                        onGoalSelected(mins)
                                        showGoalPicker = false
                                    }
                                    .padding(vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${mins}m",
                                    fontFamily = fontFamilyMono,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else hInk
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
