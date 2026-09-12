package com.nur.quran.ui.components.planner

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.data.planner.PLANNER_UNITS
import com.nur.quran.data.planner.PlannerEngine
import com.nur.quran.data.planner.ReadingPlan
import com.nur.quran.ui.components.NurIcons
import com.nur.quran.ui.screens.*

/**
 * Custom Plan creation form matching web Planner.jsx custom plan builder.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomPlanForm(
    chapters: List<com.nur.quran.data.db.entities.ChapterEntity>,
    onBeginCustomPlan: (ReadingPlan) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var unitType by remember { mutableStateOf("page") }
    var startUnitText by remember { mutableStateOf("1") }
    var endUnitText by remember { mutableStateOf("604") }
    var durationDaysText by remember { mutableStateOf("30") }
    var customTitleText by remember { mutableStateOf("") }
    var startDateText by remember { mutableStateOf(PlannerEngine.formatPlannerDate()) }
    var excludeDaysList by remember { mutableStateOf(listOf<Int>()) }

    val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    val maxUnits = PLANNER_UNITS[unitType]?.max ?: 604
    val sUnit = (startUnitText.toIntOrNull() ?: 1).coerceIn(1, maxUnits)
    val eUnit = (endUnitText.toIntOrNull() ?: maxUnits).coerceIn(sUnit, maxUnits)
    val totalSelectedUnits = (eUnit - sUnit + 1).coerceAtLeast(1)
    val durationDays = (durationDaysText.toIntOrNull() ?: 30).coerceAtLeast(1)

    val stats = remember(durationDays, excludeDaysList, startDateText, totalSelectedUnits) {
        PlannerUtils.getPaceStats(
            durationDays = durationDays,
            excludeDays = excludeDaysList,
            startDateStr = startDateText,
            totalUnits = totalSelectedUnits
        )
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = hCream),
        border = BorderStroke(1.5.dp, hBoneDark),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "CUSTOM PLAN BUILDER",
                fontFamily = fontFamilyMono,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = hTeal
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Plan Title
            OutlinedTextField(
                value = customTitleText,
                onValueChange = { customTitleText = it },
                label = { Text("Plan Title (optional)") },
                placeholder = { Text("e.g. My 30-Day Khatm") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Unit Selector Pills
            Text(
                text = "UNIT TYPE",
                fontFamily = fontFamilyMono,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = hInkMuted
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PLANNER_UNITS.keys.forEach { u ->
                    val selected = unitType == u
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) hGold else hSurface,
                        border = BorderStroke(1.dp, if (selected) hGold else hBoneDark),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                unitType = u
                                startUnitText = "1"
                                endUnitText = (PLANNER_UNITS[u]?.max ?: 604).toString()
                            }
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = PLANNER_UNITS[u]?.label ?: u,
                                fontFamily = fontFamilyUi,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) hWhite else hInk
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Range: Start and End Unit
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = startUnitText,
                    onValueChange = { startUnitText = it },
                    label = { Text("Start $unitType") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                )
                OutlinedTextField(
                    value = endUnitText,
                    onValueChange = { endUnitText = it },
                    label = { Text("End $unitType") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Duration in days
            OutlinedTextField(
                value = durationDaysText,
                onValueChange = { durationDaysText = it },
                label = { Text("Duration (Days)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Exclude Days of Week
            Text(
                text = "REST DAYS (EXCLUDE)",
                fontFamily = fontFamilyMono,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = hInkMuted
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                dayNames.forEachIndexed { index, name ->
                    val isExcluded = excludeDaysList.contains(index)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isExcluded) hRed.copy(alpha = 0.15f) else hSurface,
                        border = BorderStroke(1.dp, if (isExcluded) hRed else hBoneDark),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                excludeDaysList = if (isExcluded) {
                                    excludeDaysList - index
                                } else {
                                    excludeDaysList + index
                                }
                            }
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = name,
                                fontSize = 10.sp,
                                fontFamily = fontFamilyMono,
                                fontWeight = FontWeight.Bold,
                                color = if (isExcluded) hRed else hInkMuted
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Live Pace Calculation Bar
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = hSurface,
                border = BorderStroke(1.dp, hBoneDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${stats.dailyPages}",
                            fontFamily = fontFamilyUi,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = hGold
                        )
                        Text(
                            text = "UNITS / DAY",
                            fontFamily = fontFamilyMono,
                            fontSize = 8.sp,
                            color = hInkMuted
                        )
                    }

                    Divider(
                        modifier = Modifier
                            .height(28.dp)
                            .width(1.dp),
                        color = hBoneDark
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "~${stats.perPrayer}",
                            fontFamily = fontFamilyUi,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = hTeal
                        )
                        Text(
                            text = "PER PRAYER",
                            fontFamily = fontFamilyMono,
                            fontSize = 8.sp,
                            color = hInkMuted
                        )
                    }

                    Divider(
                        modifier = Modifier
                            .height(28.dp)
                            .width(1.dp),
                        color = hBoneDark
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stats.endLabel,
                            fontFamily = fontFamilyUi,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = hInk
                        )
                        Text(
                            text = "COMPLETION",
                            fontFamily = fontFamilyMono,
                            fontSize = 8.sp,
                            color = hInkMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Cancel", color = hInkMuted)
                }

                Button(
                    onClick = {
                        val plan = PlannerEngine.buildReadingPlanner(
                            unitType = unitType,
                            durationDays = durationDays,
                            startDate = startDateText,
                            startUnit = sUnit,
                            endUnit = eUnit,
                            customTitle = customTitleText.ifBlank { "Custom $durationDays-Day Journey" },
                            excludeDays = excludeDaysList,
                            chapters = chapters
                        )
                        onBeginCustomPlan(plan)
                    },
                    modifier = Modifier.weight(1.5f),
                    colors = ButtonDefaults.buttonColors(containerColor = hGold),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Begin Journey", fontWeight = FontWeight.Bold, color = hWhite)
                }
            }
        }
    }
}
