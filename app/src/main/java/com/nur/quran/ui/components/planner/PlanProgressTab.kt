package com.nur.quran.ui.components.planner

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.data.planner.PlannerAssignment
import com.nur.quran.data.planner.PlannerEngine
import com.nur.quran.data.planner.ReadingPlan
import com.nur.quran.ui.components.NurIcons
import com.nur.quran.ui.screens.*

/**
 * Progress tab content matching web Planner.jsx lines 1065-1232.
 */
@Composable
fun PlanProgressTab(
    planner: ReadingPlan,
    completedDays: Int,
    overdueDays: Int,
    onOpenRebalance: () -> Unit,
    onOpenAdjustPace: () -> Unit,
    onExtendPlan: () -> Unit,
    onSpreadPlan: () -> Unit,
    onReadAssignment: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val today = remember { PlannerEngine.formatPlannerDate() }
    var selectedAssignment by remember { mutableStateOf<PlannerAssignment?>(null) }
    val analytics = remember(planner) { PlannerEngine.getPlannerAnalytics(planner) }
    val weeklySummary = remember(planner) { PlannerEngine.getWeeklySummary(planner) }
    // Web parity: difficulty indicators — red dot for heavy days (>1.3× avg), green for light (<0.7× avg)
    val difficulty = remember(planner) { PlannerEngine.getDifficultyIndicators(planner.assignments) }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // 1. Header: Timeline + Rebalance / Adjust buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(20.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(hInkMuted)
                )
                Text(
                    text = "Timeline",
                    fontFamily = fontFamilyUi,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = hInk
                )
                Text(
                    text = "($completedDays/${planner.durationDays} done)",
                    fontFamily = fontFamilyMono,
                    fontSize = 11.sp,
                    color = hInkMuted
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Rebalance button
                Surface(
                    shape = RoundedCornerShape(100),
                    color = hWhite,
                    border = BorderStroke(1.5.dp, hBoneDark),
                    modifier = Modifier.clickable { onOpenRebalance() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(NurIcons.RotateCcw, contentDescription = null, tint = hInkMid, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Rebalance", fontFamily = fontFamilyBody, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = hInk)
                    }
                }

                // Adjust button
                Surface(
                    shape = RoundedCornerShape(100),
                    color = hWhite,
                    border = BorderStroke(1.5.dp, hBoneDark),
                    modifier = Modifier.clickable { onOpenAdjustPace() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(NurIcons.Sliders, contentDescription = null, tint = hInkMid, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Adjust", fontFamily = fontFamilyBody, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = hInk)
                    }
                }
            }
        }

        // 2. Grid of Circular Day Badges (matching web lines 1080-1118)
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = hCream),
            border = BorderStroke(1.5.dp, hBoneDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                val days = planner.assignments
                val chunkedDays = days.chunked(7)

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    chunkedDays.forEach { rowDays ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            rowDays.forEach { a ->
                                val status = PlannerEngine.getAssignmentStatus(planner, a)
                                val isSelected = selectedAssignment?.dayNumber == a.dayNumber
                                val isDone = status == "completed"
                                val isToday = status == "today"

                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                isDone -> hGold
                                                isToday -> hWhite
                                                status == "overdue" -> Color(0x1ADC2626)
                                                else -> hWhite
                                            }
                                        )
                                        .border(
                                            width = if (isToday || isSelected) 2.dp else 1.dp,
                                            color = when {
                                                isSelected -> hInk
                                                isToday -> hGold
                                                isDone -> hGold
                                                status == "overdue" -> Color(0x66DC2626)
                                                else -> hBoneDark
                                            },
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            selectedAssignment = if (isSelected) null else a
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${a.dayNumber}",
                                        fontFamily = fontFamilyMono,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            isDone -> Color.White
                                            isToday -> hGold
                                            status == "overdue" -> Color(0xFFDC2626)
                                            else -> hInkMuted
                                        }
                                    )
                                    // Difficulty dot (web parity: red for heavy >1.3×, green for light <0.7×)
                                    val dayDifficulty = difficulty[a.dayNumber]
                                    if (dayDifficulty != null && dayDifficulty.level != "moderate") {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .offset(x = (-2).dp, y = 2.dp)
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (dayDifficulty.level == "heavy") Color(0xFFDC2626)
                                                    else Color(0xFF10B981)
                                                )
                                        )
                                    }
                                }
                            }
                            repeat(7 - rowDays.size) {
                                Spacer(modifier = Modifier.size(38.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Legend Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    LegendItem(color = hGold, label = "Done")
                    LegendItem(color = hGoldSoft, label = "Today")
                    LegendItem(color = Color(0xFFDC2626), label = "Missed")
                    LegendItem(color = hInkMuted, label = "Upcoming")
                }
                // Difficulty legend (web parity)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendItem(color = Color(0xFFDC2626), label = "Heavy", dotSize = 7)
                    Spacer(modifier = Modifier.width(16.dp))
                    LegendItem(color = Color(0xFF10B981), label = "Light", dotSize = 7)
                }

                // Selected Day Details Card
                if (selectedAssignment != null) {
                    val sel = selectedAssignment!!
                    val status = PlannerEngine.getAssignmentStatus(planner, sel)
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = hWhite,
                        border = BorderStroke(1.dp, hBoneDark),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Day ${sel.dayNumber}: ${sel.title}",
                                fontFamily = fontFamilyUi,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = hInk
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = when (status) {
                                    "completed" -> "✓ Completed"
                                    "today" -> "Today's Reading"
                                    "overdue" -> "Missed day"
                                    else -> "Scheduled for ${sel.date}"
                                },
                                fontFamily = fontFamilyMono,
                                fontSize = 11.sp,
                                color = hInkMuted
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { onReadAssignment(sel.dayNumber) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = hTeal),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Read Day ${sel.dayNumber} →",
                                    fontFamily = fontFamilyUi,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Performance Insights Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = hCream),
            border = BorderStroke(1.5.dp, hBoneDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Performance Insights",
                    fontFamily = fontFamilyUi,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = hInk
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    InsightBox(
                        label = "ON-TIME RATE",
                        value = "${analytics?.onTimeRate ?: 0}%",
                        valueColor = Color(0xFF10B981),
                        modifier = Modifier.weight(1f)
                    )
                    InsightBox(
                        label = "CATCH-UPS",
                        value = "${analytics?.catchUpDays ?: 0}",
                        valueColor = hGold,
                        modifier = Modifier.weight(1f)
                    )
                    InsightBox(
                        label = "PACE",
                        value = "${Math.round(analytics?.avgPagesPerDay ?: 0f)} u/d",
                        valueColor = hInk,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Weekly Progress Card (web: Planner.jsx lines 1178-1202)
        if (weeklySummary.isNotEmpty()) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = hCream),
                border = BorderStroke(1.5.dp, hBoneDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Weekly Progress",
                        fontFamily = fontFamilyUi,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = hInk
                    )
                    weeklySummary.takeLast(4).forEach { week ->
                        val pct = Math.round(week.completionRatio * 100).coerceIn(0, 100)
                        val isComplete = pct == 100
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = hWhite,
                            border = BorderStroke(1.dp, hBoneDark),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Week ${week.weekNumber}",
                                    fontFamily = fontFamilyUi,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = hInk
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "${week.completedCount} / ${week.totalCount}",
                                        fontFamily = fontFamilyMono,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isComplete) Color(0xFF10B981) else hInkMuted
                                    )
                                    Box(
                                        modifier = Modifier
                                            .width(100.dp)
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(hBoneDark)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(pct / 100f)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(if (isComplete) Color(0xFF10B981) else hTeal)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Overdue Warning Card
        if (overdueDays > 0) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0x0ADC2626)),
                border = BorderStroke(1.5.dp, Color(0x33DC2626)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Action Required",
                        fontFamily = fontFamilyUi,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFDC2626)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "You have fallen $overdueDays day${if (overdueDays > 1) "s" else ""} behind schedule.",
                        fontFamily = fontFamilyBody,
                        fontSize = 13.sp,
                        color = hInkMid
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = onExtendPlan,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = hWhite),
                        border = BorderStroke(1.dp, Color(0x4DDC2626)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Shift Deadlines (Extend Plan)", fontFamily = fontFamilyUi, fontWeight = FontWeight.SemiBold, color = Color(0xFFDC2626))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onSpreadPlan,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x1FDC2626)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Redistribute Missed Pages", fontFamily = fontFamilyUi, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String, dotSize: Int = 8) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(modifier = Modifier.size(dotSize.dp).clip(CircleShape).background(color))
        Text(text = label, fontFamily = fontFamilyMono, fontSize = 10.sp, color = hInkMuted)
    }
}

@Composable
private fun InsightBox(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = hWhite,
        border = BorderStroke(1.dp, hBoneDark),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                fontFamily = fontFamilyMono,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.5.sp,
                color = hInkMuted
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontFamily = fontFamilyUi,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
        }
    }
}
