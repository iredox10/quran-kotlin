package com.nur.quran.ui.components.analytics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.nur.quran.ui.screens.fontFamilyMono
import com.nur.quran.ui.screens.hBoneDark
import com.nur.quran.ui.screens.hBorderColor
import com.nur.quran.ui.screens.hCream
import com.nur.quran.ui.screens.hGold
import com.nur.quran.ui.screens.hGoldLight
import com.nur.quran.ui.screens.hGoldSoft
import com.nur.quran.ui.screens.hInk
import com.nur.quran.ui.screens.hInkMuted
import com.nur.quran.ui.screens.hSurface
import com.nur.quran.ui.screens.hWhite

val HeatmapEmerald = Color(0xFF10B981)

/**
 * Returns heatmap level (0 to 3) for the given duration in minutes.
 * - Level 0: 0m / inactive
 * - Level 1: 1m - 9m
 * - Level 2: 10m - 29m
 * - Level 3: >= 30m
 */
fun getHeatmapLevel(mins: Int): Int = when {
    mins <= 0 -> 0
    mins < 10 -> 1
    mins < 30 -> 2
    else -> 3
}

/**
 * Formats the popup banner text according to parity requirements:
 * "${date} — ${if (mins > 0) "${mins}m" else "No activity"}"
 */
fun formatHeatmapBannerText(date: String, mins: Int): String =
    "$date — ${if (mins > 0) "${mins}m" else "No activity"}"

/**
 * Consistency Heatmap Grid component matching web Progress.jsx lines 359-392.
 *
 * @param heatmapData 35 days (5 columns of 7 days each, representing 5 weeks).
 *                    Each item is a Pair of (date string, minutes read).
 * @param modifier Modifier for the layout container.
 */
@Composable
fun AnalyticsHeatmap(
    heatmapData: List<Pair<String, Int>>,
    modifier: Modifier = Modifier
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val selectedDay = selectedIndex?.let { heatmapData.getOrNull(it) }

    // 5 columns of 7 days each (representing Monday through Sunday across 5 weeks)
    val weeks = remember(heatmapData) {
        if (heatmapData.size >= 35) heatmapData.take(35).chunked(7)
        else heatmapData.chunked(7)
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Floating popup banner above the grid
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 40.dp),
            contentAlignment = Alignment.Center
        ) {
            this@Column.AnimatedVisibility(
                visible = selectedDay != null,
                enter = fadeIn(animationSpec = tween(200)) + scaleIn(initialScale = 0.9f, animationSpec = tween(200)),
                exit = fadeOut(animationSpec = tween(150)) + scaleOut(targetScale = 0.9f, animationSpec = tween(150))
            ) {
                selectedDay?.let { (date, mins) ->
                    val dotColor = when (getHeatmapLevel(mins)) {
                        0 -> hBorderColor.copy(alpha = 0.5f)
                        1 -> hGoldSoft
                        2 -> hGold.copy(alpha = 0.8f)
                        else -> HeatmapEmerald
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = hCream,
                        border = BorderStroke(1.dp, hBoneDark),
                        shadowElevation = 3.dp,
                        modifier = Modifier
                            .clickable { selectedIndex = null }
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(dotColor)
                                    .then(
                                        if (mins in 1..9) Modifier.border(0.75.dp, hGold.copy(alpha = 0.3f), CircleShape)
                                        else Modifier
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = formatHeatmapBannerText(date, mins),
                                fontFamily = fontFamilyMono,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = hInk
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Heatmap Grid: 5 columns x 7 days
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            weeks.forEachIndexed { colIndex, weekDays ->
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    weekDays.forEachIndexed { rowIndex, (date, mins) ->
                        val flatIndex = colIndex * 7 + rowIndex
                        val isSelected = selectedIndex == flatIndex

                        val animatedScale by animateFloatAsState(
                            targetValue = if (isSelected) 1.2f else 1.0f,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "heatmap_tile_scale"
                        )

                        val level = getHeatmapLevel(mins)

                        val tileColor = when (level) {
                            0 -> hBorderColor.copy(alpha = 0.5f)
                            1 -> hGoldSoft
                            2 -> hGold.copy(alpha = 0.8f)
                            else -> HeatmapEmerald
                        }

                        val tileShape = RoundedCornerShape(5.dp)

                        val borderModifier = when {
                            isSelected -> Modifier.border(2.dp, hInk, tileShape)
                            level == 1 -> Modifier.border(1.dp, hGold.copy(alpha = 0.25f), tileShape)
                            else -> Modifier
                        }

                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .zIndex(if (isSelected) 1f else 0f)
                                .graphicsLayer {
                                    scaleX = animatedScale
                                    scaleY = animatedScale
                                }
                                .clip(tileShape)
                                .background(tileColor)
                                .then(borderModifier)
                                .clickable {
                                    selectedIndex = if (selectedIndex == flatIndex) null else flatIndex
                                }
                                .semantics {
                                    contentDescription = formatHeatmapBannerText(date, mins)
                                }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Legend at bottom:
        // "Less" • [Level 0 square] [Level 1 square] [Level 2 square] [Level 3 square] • "More"
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "LESS",
                fontSize = 10.sp,
                fontFamily = fontFamilyMono,
                fontWeight = FontWeight.Medium,
                letterSpacing = 2.sp,
                color = hInkMuted,
                modifier = Modifier.semantics { contentDescription = "Less" }
            )

            Spacer(modifier = Modifier.width(2.dp))

            // Level 0 square (0m / inactive)
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(hBorderColor.copy(alpha = 0.5f))
                    .semantics { contentDescription = "Level 0: No activity" }
            )

            // Level 1 square (1m - 9m)
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(hGoldSoft)
                    .border(1.dp, hGold.copy(alpha = 0.25f), RoundedCornerShape(3.dp))
                    .semantics { contentDescription = "Level 1: 1m - 9m" }
            )

            // Level 2 square (10m - 29m)
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(hGold.copy(alpha = 0.8f))
                    .semantics { contentDescription = "Level 2: 10m - 29m" }
            )

            // Level 3 square (>= 30m)
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(HeatmapEmerald)
                    .semantics { contentDescription = "Level 3: 30m or more" }
            )

            Spacer(modifier = Modifier.width(2.dp))

            Text(
                text = "MORE",
                fontSize = 10.sp,
                fontFamily = fontFamilyMono,
                fontWeight = FontWeight.Medium,
                letterSpacing = 2.sp,
                color = hInkMuted,
                modifier = Modifier.semantics { contentDescription = "More" }
            )
        }
    }
}
