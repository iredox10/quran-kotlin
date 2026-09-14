package com.nur.quran.ui.components.planner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.nur.quran.data.planner.ReadingPlan
import com.nur.quran.ui.components.NurIcons
import com.nur.quran.ui.screens.*

/**
 * Adjust Pace dialog matching web Planner.jsx lines 1320-1358.
 */
@Composable
fun AdjustPaceDialog(
    currentDurationDays: Int,
    onConfirmNewDuration: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var newDaysText by remember { mutableStateOf(currentDurationDays.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Adjust Pace",
                fontFamily = fontFamilyUi,
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp,
                color = hInk
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Change how many days you want to complete your remaining plan in. This will re-calculate your daily assignments.",
                    fontSize = 13.5.sp,
                    fontFamily = fontFamilyBody,
                    color = hInkMid,
                    lineHeight = 19.sp
                )
                Text(
                    text = "NEW TOTAL DAYS",
                    fontFamily = fontFamilyMono,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = hGold
                )
                OutlinedTextField(
                    value = newDaysText,
                    onValueChange = { newDaysText = it },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        Text("DAYS", fontFamily = fontFamilyMono, fontSize = 11.sp, color = hInkMuted, modifier = Modifier.padding(end = 12.dp))
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val days = newDaysText.toIntOrNull() ?: currentDurationDays
                    onConfirmNewDuration(days.coerceAtLeast(1))
                },
                colors = ButtonDefaults.buttonColors(containerColor = hGold),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Apply", fontFamily = fontFamilyUi, fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", fontFamily = fontFamilyUi, color = hInkMid)
            }
        },
        containerColor = hCream
    )
}

/**
 * Planner Settings dialog matching web Planner.jsx lines 1362-1430.
 */
@Composable
fun PlannerSettingsDialog(
    activePrayers: List<String>,
    onTogglePrayer: (String) -> Unit,
    readingPreference: String,
    onSelectPreference: (String) -> Unit,
    useIntentionPrompt: Boolean = true,
    onToggleIntentionPrompt: (Boolean) -> Unit = {},
    onDismiss: () -> Unit
) {
    val allPrayers = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = hCream,
            border = BorderStroke(1.dp, hBoneDark),
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(22.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Planner Settings",
                        fontFamily = fontFamilyUi,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = hInk
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(NurIcons.X, contentDescription = "Close", tint = hInkMid, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Daily Prayers Section
                Text(
                    text = "DAILY PRAYERS",
                    fontFamily = fontFamilyMono,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = hTeal
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Select which prayers you want to distribute reading across.",
                    fontFamily = fontFamilyBody,
                    fontSize = 12.5.sp,
                    color = hInkMid
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    allPrayers.forEach { pName ->
                        val isActive = activePrayers.contains(pName)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isActive) hTeal else hWhite,
                            border = BorderStroke(1.5.dp, if (isActive) hTeal else hBoneDark),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onTogglePrayer(pName) }
                        ) {
                            Text(
                                text = pName,
                                fontFamily = fontFamilyBody,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isActive) Color.White else hInkMid,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Reading Preference
                Text(
                    text = "READING PREFERENCE",
                    fontFamily = fontFamilyMono,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = hTeal
                )
                Spacer(modifier = Modifier.height(8.dp))

                listOf("after" to "After Prayer", "before" to "Before Prayer", "around" to "Around Prayer").forEach { (key, label) ->
                    val isSelected = readingPreference == key
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) Color(0x1F2E4F4A) else hWhite,
                        border = BorderStroke(1.dp, if (isSelected) hTeal else hBoneDark),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                            .clickable { onSelectPreference(key) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(label, fontFamily = fontFamilyBody, fontSize = 13.5.sp, fontWeight = FontWeight.Medium, color = hInk)
                            if (isSelected) {
                                Icon(NurIcons.Check, contentDescription = null, tint = hTeal, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Intention Prompt Toggle Section (web: Planner.jsx:1418-1430)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = hWhite,
                    border = BorderStroke(1.dp, hBoneDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = "RENEWAL OF INTENTION",
                                fontFamily = fontFamilyMono,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp,
                                color = hTeal
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Remind to pause and renew intention before reading.",
                                fontFamily = fontFamilyBody,
                                fontSize = 12.sp,
                                color = hInkMid
                            )
                        }
                        Switch(
                            checked = useIntentionPrompt,
                            onCheckedChange = onToggleIntentionPrompt,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = hTeal,
                                uncheckedThumbColor = hWhite,
                                uncheckedTrackColor = hBoneDark
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = hTeal),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Done", fontFamily = fontFamilyUi, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

/**
 * Smart Rebalancing modal dialog matching web Planner.jsx lines 1450-1510.
 */
@Composable
fun RebalancePlanDialog(
    onExtend: () -> Unit,
    onSpread: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Smart Rebalance",
                fontFamily = fontFamilyUi,
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp,
                color = hInk
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "If you have missed some assignments, choose how you would like to recalibrate your remaining days.",
                    fontSize = 13.5.sp,
                    fontFamily = fontFamilyBody,
                    color = hInkMid,
                    lineHeight = 19.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = { onExtend(); onDismiss() },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = hWhite),
                    border = BorderStroke(1.5.dp, hBoneDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Shift Deadlines (Extend Plan)", fontFamily = fontFamilyUi, fontWeight = FontWeight.SemiBold, color = hInk)
                }
                Button(
                    onClick = { onSpread(); onDismiss() },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = hGold),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Redistribute Missed Pages", fontFamily = fontFamilyUi, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", fontFamily = fontFamilyUi, color = hInkMid)
            }
        },
        containerColor = hCream
    )
}

/**
 * Past Plans Archive modal matching web Planner.jsx lines 514-558.
 */
@Composable
fun ArchivesDialog(
    archivedPlans: List<ReadingPlan>,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = hCream,
            border = BorderStroke(1.dp, hBoneDark),
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.7f)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Past Plans Archive",
                        fontFamily = fontFamilyUi,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = hInk
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(NurIcons.X, contentDescription = "Close", tint = hInkMid, modifier = Modifier.size(18.dp))
                    }
                }

                Divider(color = hBoneDark, thickness = 1.dp)

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (archivedPlans.isEmpty()) {
                        Text(
                            text = "No archived plans yet.",
                            fontFamily = fontFamilyBody,
                            fontSize = 13.5.sp,
                            color = hInkMuted,
                            modifier = Modifier.padding(vertical = 24.dp)
                        )
                    } else {
                        archivedPlans.forEach { plan ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = hWhite,
                                border = BorderStroke(1.dp, hBoneDark),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = plan.title,
                                        fontFamily = fontFamilyUi,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = hInk
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${plan.durationDays} days · ${plan.completedDays.size} days finished",
                                        fontFamily = fontFamilyMono,
                                        fontSize = 11.sp,
                                        color = hInkMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Delete Confirmation Dialog.
 */
@Composable
fun DeletePlanConfirmDialog(
    planTitle: String,
    onConfirmDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Delete Plan?",
                fontFamily = fontFamilyUi,
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp,
                color = hInk
            )
        },
        text = {
            Text(
                text = "Are you sure you want to delete \"$planTitle\"? All reading progress for this plan will be removed.",
                fontSize = 13.5.sp,
                fontFamily = fontFamilyBody,
                color = hInkMid
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirmDelete,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Delete", fontFamily = fontFamilyUi, fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", fontFamily = fontFamilyUi, color = hInkMid)
            }
        },
        containerColor = hCream
    )
}
