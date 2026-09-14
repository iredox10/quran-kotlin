package com.nur.quran.ui.components.planner

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.ui.components.NurIcons
import com.nur.quran.ui.screens.*

data class PresetPaceItem(
    val id: String,
    val title: String,
    val durationText: String,
    val durationDays: Int,
    val icon: ImageVector,
    val badge: String? = null
)

val PRESET_PACES = listOf(
    PresetPaceItem(
        id = "ramadan",
        title = "Ramadan Pace",
        durationText = "30 DAYS",
        durationDays = 30,
        icon = NurIcons.Moon
    ),
    PresetPaceItem(
        id = "steady",
        title = "Steady Journey",
        durationText = "60 DAYS",
        durationDays = 60,
        icon = NurIcons.Sun,
        badge = "MOST BALANCED"
    ),
    PresetPaceItem(
        id = "devotional",
        title = "Devotional Path",
        durationText = "1 YEAR",
        durationDays = 365,
        icon = NurIcons.BookOpen
    )
)

/**
 * Preset pace selection card matching web PACES cards in Planner.jsx lines 242-299.
 */
@Composable
fun PresetPaceCard(
    pace: PresetPaceItem,
    stats: PaceStats,
    selected: Boolean,
    onSelect: () -> Unit,
    onBegin: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.width(260.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = hCream),
            border = BorderStroke(
                width = 1.5.dp,
                color = if (selected) hGold else hBoneDark
            ),
            modifier = Modifier
                .padding(top = 10.dp)
                .fillMaxWidth()
                .clickable { onSelect() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(6.dp))

                // Arc Gauge Ring
                PaceRing(
                    durationDays = pace.durationDays,
                    dailyPages = stats.dailyPages,
                    selected = selected
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Title
                Text(
                    text = pace.title,
                    fontFamily = fontFamilyUi,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = hInk
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Daily pages label
                Text(
                    text = "${stats.dailyPages} pages per day",
                    fontFamily = fontFamilyMono,
                    fontSize = 11.sp,
                    color = hInkMid,
                    letterSpacing = 0.3.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Prayer breakdown
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = pace.icon,
                        contentDescription = null,
                        tint = hInkMid,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "${stats.perPrayer} pages per prayer",
                        fontFamily = fontFamilyBody,
                        fontSize = 13.sp,
                        fontStyle = FontStyle.Italic,
                        color = hInkMid
                    )
                }

                // Selected expander: end date & Begin Journey button
                AnimatedVisibility(
                    visible = selected,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Divider(color = hBoneDark, thickness = 1.dp)

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${stats.dailyPages} pages/day",
                                fontFamily = fontFamilyMono,
                                fontSize = 11.sp,
                                color = hInkMid
                            )
                            Text(
                                text = "·",
                                color = hBoneDark,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Done by ${stats.endLabel}",
                                fontFamily = fontFamilyMono,
                                fontSize = 11.sp,
                                color = hInkMid
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = onBegin,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = hTeal),
                            contentPadding = PaddingValues(vertical = 14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "BEGIN MY JOURNEY",
                                fontFamily = fontFamilyMono,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.4.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Top Floating Badge (matching web: bg-[var(--plr-teal)] rounded-[20px])
        if (pace.badge != null) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = hTeal,
                shadowElevation = 2.dp,
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Text(
                    text = pace.badge,
                    fontFamily = fontFamilyMono,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 1.2.sp,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                )
            }
        }
    }
}

/**
 * Horizontal scrollable row of pace cards matching web Planner.jsx.
 */
@Composable
fun PresetPacesRow(
    selectedPaceId: String,
    onSelectPace: (String) -> Unit,
    onBeginPlan: (PresetPaceItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        PRESET_PACES.forEach { pace ->
            val stats = PlannerUtils.getPaceStats(pace.durationDays)
            PresetPaceCard(
                pace = pace,
                stats = stats,
                selected = selectedPaceId == pace.id,
                onSelect = { onSelectPace(pace.id) },
                onBegin = { onBeginPlan(pace) }
            )
        }
    }
}
