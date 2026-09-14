package com.nur.quran.ui.components.planner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.nur.quran.data.planner.PLANNER_UNITS
import com.nur.quran.data.planner.PLAN_TEMPLATES
import com.nur.quran.data.planner.PlanTemplate
import com.nur.quran.data.planner.PlannerEngine
import com.nur.quran.data.planner.ReadingPlan
import com.nur.quran.ui.components.NurIcons
import com.nur.quran.ui.screens.*

/**
 * Curated Plan Templates section matching web Planner.jsx lines 357-393.
 */
@Composable
fun PlanTemplatesSection(
    chapters: List<com.nur.quran.data.db.entities.ChapterEntity>,
    onSelectTemplate: (ReadingPlan) -> Unit,
    modifier: Modifier = Modifier
) {
    var previewTemplate by remember { mutableStateOf<PlanTemplate?>(null) }

    Column(modifier = modifier.fillMaxWidth()) {
        // Section Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Plan Templates",
                fontFamily = fontFamilyUi,
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold,
                color = hInk
            )
            Text(
                text = "CURATED PATHS",
                fontFamily = fontFamilyMono,
                fontSize = 10.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 1.sp,
                color = hInkMuted
            )
        }

        // List of Template Cards
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PLAN_TEMPLATES.forEach { tmpl ->
                val unitCount = (tmpl.endUnit - tmpl.startUnit + 1).coerceAtLeast(1)
                val unitsPerDay = Math.ceil(unitCount.toDouble() / tmpl.durationDays).toInt().coerceAtLeast(1)
                val unitPlural = PLANNER_UNITS[tmpl.unitType]?.plural ?: "units"

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = hCream),
                    border = BorderStroke(1.5.dp, hBoneDark),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { previewTemplate = tmpl }
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = tmpl.title,
                            fontFamily = fontFamilyUi,
                            fontSize = 16.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = hInk
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = tmpl.description,
                            fontFamily = fontFamilyBody,
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            color = hInkMid
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (tmpl.tags.isNotEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0x1A2E4F4A)
                                ) {
                                    Text(
                                        text = tmpl.tags.first(),
                                        fontFamily = fontFamilyMono,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = hTeal,
                                        letterSpacing = 0.5.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.width(1.dp))
                            }

                            Text(
                                text = "$unitsPerDay $unitPlural/day",
                                fontFamily = fontFamilyMono,
                                fontSize = 10.5.sp,
                                color = hInkMuted
                            )
                        }
                    }
                }
            }
        }
    }

    // Preview Template Confirmation Modal (matching web lines 560-600)
    if (previewTemplate != null) {
        val tmpl = previewTemplate!!
        val unitCount = (tmpl.endUnit - tmpl.startUnit + 1).coerceAtLeast(1)
        val perDay = Math.ceil(unitCount.toDouble() / tmpl.durationDays).toInt().coerceAtLeast(1)
        val stats = PlannerUtils.getPaceStats(tmpl.durationDays, totalUnits = unitCount)

        Dialog(onDismissRequest = { previewTemplate = null }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = hCream,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, hBoneDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(22.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Start \"${tmpl.title}\"?",
                            fontFamily = fontFamilyUi,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = hInk
                        )
                        IconButton(
                            onClick = { previewTemplate = null },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(NurIcons.X, contentDescription = "Close", tint = hInkMid, modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = tmpl.description,
                        fontFamily = fontFamilyBody,
                        fontSize = 13.sp,
                        color = hInkMid
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 4 Stat Grid Cards
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatBox(label = "DURATION", value = "${tmpl.durationDays} days", modifier = Modifier.weight(1f))
                            StatBox(label = "PACE", value = "$perDay ${PLANNER_UNITS[tmpl.unitType]?.plural ?: "units"}/day", modifier = Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatBox(label = "STARTS", value = "Today", modifier = Modifier.weight(1f))
                            StatBox(label = "DONE BY", value = stats.endLabel, modifier = Modifier.weight(1f))
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { previewTemplate = null },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.5.dp, hBoneDark),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel", fontFamily = fontFamilyUi, color = hInkMid)
                        }

                        Button(
                            onClick = {
                                val built = PlannerEngine.buildReadingPlanner(
                                    unitType = tmpl.unitType,
                                    durationDays = tmpl.durationDays,
                                    startDate = PlannerEngine.formatPlannerDate(),
                                    startUnit = tmpl.startUnit,
                                    endUnit = tmpl.endUnit,
                                    customTitle = tmpl.title,
                                    excludeDays = emptyList(),
                                    chapters = chapters
                                )
                                previewTemplate = null
                                onSelectTemplate(built)
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = hTeal),
                            modifier = Modifier.weight(1.5f)
                        ) {
                            Text("Start This Plan", fontFamily = fontFamilyUi, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatBox(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = hSurface,
        border = BorderStroke(1.dp, hBoneDark),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                fontFamily = fontFamilyMono,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.8.sp,
                color = hInkMuted
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontFamily = fontFamilyUi,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = hInk
            )
        }
    }
}
