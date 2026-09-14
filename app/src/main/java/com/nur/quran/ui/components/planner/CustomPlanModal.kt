package com.nur.quran.ui.components.planner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.window.Dialog
import com.nur.quran.data.db.entities.ChapterEntity
import com.nur.quran.data.planner.PLANNER_UNITS
import com.nur.quran.data.planner.PlannerEngine
import com.nur.quran.data.planner.ReadingPlan
import com.nur.quran.ui.components.NurIcons
import com.nur.quran.ui.screens.*

/**
 * "Create Custom Plan" modal dialog matching web Planner.jsx lines 395-506.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomPlanModal(
    chapters: List<ChapterEntity>,
    onDismiss: () -> Unit,
    onCreatePlan: (ReadingPlan) -> Unit
) {
    var unitType by remember { mutableStateOf("page") }
    var customTitle by remember { mutableStateOf("") }
    var startUnitText by remember { mutableStateOf("1") }
    var endUnitText by remember { mutableStateOf("604") }
    var pagesPerDay by remember { mutableStateOf(10) }
    var startDateText by remember { mutableStateOf(PlannerEngine.formatPlannerDate()) }
    var excludeDays by remember { mutableStateOf(setOf<Int>()) }

    val daysList = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    val maxUnit = PLANNER_UNITS[unitType]?.max ?: 604
    val sUnit = (startUnitText.toIntOrNull() ?: 1).coerceIn(1, maxUnit)
    val eUnit = (endUnitText.toIntOrNull() ?: maxUnit).coerceIn(sUnit, maxUnit)
    val totalSelectedUnits = (eUnit - sUnit + 1).coerceAtLeast(1)
    val computedDuration = Math.ceil(totalSelectedUnits.toDouble() / pagesPerDay.coerceAtLeast(1)).toInt().coerceAtLeast(1)

    val stats = remember(computedDuration, excludeDays, startDateText, totalSelectedUnits) {
        PlannerUtils.getPaceStats(
            durationDays = computedDuration,
            excludeDays = excludeDays.toList(),
            startDateStr = startDateText,
            totalUnits = totalSelectedUnits
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = hCream,
            border = BorderStroke(1.dp, hBoneDark),
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Create Custom Plan",
                        fontFamily = fontFamilyUi,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = hInk
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(NurIcons.X, contentDescription = "Close", tint = hInkMid, modifier = Modifier.size(18.dp))
                    }
                }

                Divider(color = hBoneDark, thickness = 1.dp)

                // Scrollable Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Plan name
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "PLAN NAME (OPTIONAL)",
                            fontFamily = fontFamilyMono,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.sp,
                            color = hInkMuted
                        )
                        OutlinedTextField(
                            value = customTitle,
                            onValueChange = { customTitle = it },
                            placeholder = { Text("e.g. Morning Routine, Juz Amma...", fontSize = 13.sp, color = hInkMuted) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Reading Unit Pills
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "READING UNIT",
                            fontFamily = fontFamilyMono,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.sp,
                            color = hInkMuted
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("page" to "Page", "juz" to "Juz", "hizb" to "Hizb", "surah" to "Surah").forEach { (key, label) ->
                                val isSelected = unitType == key
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isSelected) hTeal else hCream,
                                    border = BorderStroke(1.5.dp, if (isSelected) hTeal else hBoneDark),
                                    modifier = Modifier.clickable {
                                        unitType = key
                                        startUnitText = "1"
                                        endUnitText = (PLANNER_UNITS[key]?.max ?: 604).toString()
                                    }
                                ) {
                                    Text(
                                        text = label,
                                        fontFamily = fontFamilyBody,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isSelected) Color.White else hInkMid,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Range From / To
                    val unitLabel = PLANNER_UNITS[unitType]?.label ?: "Page"
                    val unitPlural = PLANNER_UNITS[unitType]?.plural ?: "Pages"
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "$unitLabel RANGE".uppercase(),
                            fontFamily = fontFamilyMono,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.sp,
                            color = hInkMuted
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("From", fontFamily = fontFamilyMono, fontSize = 9.sp, color = hInkMuted)
                                OutlinedTextField(
                                    value = startUnitText,
                                    onValueChange = { startUnitText = it },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                            Text("→", fontSize = 18.sp, color = hInkMuted, modifier = Modifier.padding(top = 16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("To", fontFamily = fontFamilyMono, fontSize = 9.sp, color = hInkMuted)
                                OutlinedTextField(
                                    value = endUnitText,
                                    onValueChange = { endUnitText = it },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }
                        Text(
                            text = "$totalSelectedUnits $unitPlural selected",
                            fontFamily = fontFamilyMono,
                            fontSize = 10.5.sp,
                            color = hInkMuted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                        )
                    }

                    // Units per day slider + number
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "$unitPlural PER DAY".uppercase(),
                            fontFamily = fontFamilyMono,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.sp,
                            color = hInkMuted
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.5.dp, hBoneDark),
                                color = hCream,
                                modifier = Modifier.width(60.dp)
                            ) {
                                Text(
                                    text = "$pagesPerDay",
                                    fontFamily = fontFamilyBody,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 10.dp)
                                )
                            }
                            Slider(
                                value = pagesPerDay.toFloat(),
                                onValueChange = { pagesPerDay = it.toInt().coerceAtLeast(1) },
                                valueRange = 1f..50f,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(thumbColor = hTeal, activeTrackColor = hTeal)
                            )
                        }
                    }

                    // Days Off (Exclude Days)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "DAYS OFF (OPTIONAL)",
                            fontFamily = fontFamilyMono,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.sp,
                            color = hInkMuted
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            daysList.forEachIndexed { idx, dName ->
                                val isExcluded = excludeDays.contains(idx)
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isExcluded) hTeal else hCream,
                                    border = BorderStroke(1.5.dp, if (isExcluded) hTeal else hBoneDark),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            excludeDays = if (isExcluded) excludeDays - idx else excludeDays + idx
                                        }
                                ) {
                                    Text(
                                        text = dName,
                                        fontFamily = fontFamilyBody,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Center,
                                        color = if (isExcluded) Color.White else hInkMid,
                                        modifier = Modifier.padding(vertical = 6.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "Reading assignments will skip these days.",
                            fontFamily = fontFamilyBody,
                            fontSize = 11.sp,
                            color = hInkMuted
                        )
                    }
                }

                Divider(color = hBoneDark, thickness = 1.dp)

                // Bottom CTA
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$pagesPerDay ${PLANNER_UNITS[unitType]?.plural ?: "units"}/day",
                            fontFamily = fontFamilyMono,
                            fontSize = 11.sp,
                            color = hInkMid
                        )
                        Text("·", color = hBoneDark, fontWeight = FontWeight.Bold)
                        Text(
                            text = "$computedDuration days",
                            fontFamily = fontFamilyMono,
                            fontSize = 11.sp,
                            color = hInkMid
                        )
                        Text("·", color = hBoneDark, fontWeight = FontWeight.Bold)
                        Text(
                            text = "Done by ${stats.endLabel}",
                            fontFamily = fontFamilyMono,
                            fontSize = 11.sp,
                            color = hInkMid
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            val built = PlannerEngine.buildReadingPlanner(
                                unitType = unitType,
                                durationDays = computedDuration,
                                startDate = startDateText,
                                startUnit = sUnit,
                                endUnit = eUnit,
                                customTitle = customTitle.ifBlank { "Custom Plan" },
                                excludeDays = excludeDays.toList(),
                                chapters = chapters
                            )
                            onCreatePlan(built)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = hTeal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(
                            text = "CREATE PLAN",
                            fontFamily = fontFamilyMono,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.4.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
