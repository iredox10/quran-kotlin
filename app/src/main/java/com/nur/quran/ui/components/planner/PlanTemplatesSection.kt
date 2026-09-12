package com.nur.quran.ui.components.planner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.data.planner.PLAN_TEMPLATES
import com.nur.quran.data.planner.PlanTemplate
import com.nur.quran.data.planner.PlannerEngine
import com.nur.quran.data.planner.ReadingPlan
import com.nur.quran.ui.components.NurIcons
import com.nur.quran.ui.screens.*

/**
 * Curated Plan Templates section matching web Planner.jsx lines 650-780.
 */
@Composable
fun PlanTemplatesSection(
    chapters: List<com.nur.quran.data.db.entities.ChapterEntity>,
    onSelectTemplate: (ReadingPlan) -> Unit,
    modifier: Modifier = Modifier
) {
    var previewTemplate by remember { mutableStateOf<PlanTemplate?>(null) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "CURATED JOURNEYS",
            fontFamily = fontFamilyMono,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            color = hInkMuted,
            modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PLAN_TEMPLATES.forEach { tmpl ->
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = hCream),
                    border = BorderStroke(1.dp, hBoneDark),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { previewTemplate = tmpl }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = tmpl.title,
                                    fontFamily = fontFamilyUi,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = hInk
                                )
                                tmpl.tags.forEach { tag ->
                                    Surface(
                                        shape = RoundedCornerShape(100),
                                        color = hGoldSoft
                                    ) {
                                        Text(
                                            text = tag,
                                            fontSize = 8.sp,
                                            fontFamily = fontFamilyMono,
                                            fontWeight = FontWeight.Bold,
                                            color = hGold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = tmpl.description,
                                fontFamily = fontFamilyBody,
                                fontSize = 12.sp,
                                color = hInkMuted,
                                maxLines = 2
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "${tmpl.durationDays} Days · ${tmpl.startUnit}-${tmpl.endUnit} ${tmpl.unitType}s",
                                fontFamily = fontFamilyMono,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = hTeal
                            )
                        }

                        Icon(
                            imageVector = NurIcons.ChevronRight,
                            contentDescription = null,
                            tint = hInkMuted.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }

    // Confirmation Preview Dialog
    if (previewTemplate != null) {
        val tmpl = previewTemplate!!
        val stats = PlannerUtils.getPaceStats(
            durationDays = tmpl.durationDays,
            totalUnits = (tmpl.endUnit - tmpl.startUnit + 1).coerceAtLeast(1)
        )

        AlertDialog(
            onDismissRequest = { previewTemplate = null },
            title = {
                Text(
                    text = tmpl.title,
                    fontFamily = fontFamilyUi,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = tmpl.description,
                        fontSize = 13.sp,
                        fontFamily = fontFamilyBody,
                        color = hInk
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = hSurface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "PACE PREVIEW",
                                fontFamily = fontFamilyMono,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = hInkMuted
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "• Duration: ${tmpl.durationDays} days",
                                fontSize = 12.sp,
                                fontFamily = fontFamilyBody,
                                color = hInk
                            )
                            Text(
                                text = "• Target: ~${stats.dailyPages} ${tmpl.unitType}s / day (~${stats.perPrayer} / prayer)",
                                fontSize = 12.sp,
                                fontFamily = fontFamilyBody,
                                color = hInk
                            )
                            Text(
                                text = "• Est. Completion: ${stats.endLabel}",
                                fontSize = 12.sp,
                                fontFamily = fontFamilyBody,
                                color = hInk
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val plan = PlannerEngine.buildReadingPlanner(
                            unitType = tmpl.unitType,
                            durationDays = tmpl.durationDays,
                            startDate = PlannerEngine.formatPlannerDate(),
                            startUnit = tmpl.startUnit,
                            endUnit = tmpl.endUnit,
                            customTitle = tmpl.title,
                            chapters = chapters
                        )
                        previewTemplate = null
                        onSelectTemplate(plan)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = hGold)
                ) {
                    Text("Start This Plan", fontWeight = FontWeight.Bold, color = hWhite)
                }
            },
            dismissButton = {
                TextButton(onClick = { previewTemplate = null }) {
                    Text("Cancel", color = hInkMuted)
                }
            }
        )
    }
}
