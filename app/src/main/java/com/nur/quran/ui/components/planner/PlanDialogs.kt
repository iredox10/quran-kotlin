package com.nur.quran.ui.components.planner

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.data.planner.ReadingPlan
import com.nur.quran.ui.screens.*

/**
 * Dialogs for Plan management: Adjust Pace, Smart Rebalance, and Delete Confirmation.
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
                fontSize = 18.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Extend or shorten your total journey days. The app will recalibrate your daily reading pace automatically.",
                    fontSize = 13.sp,
                    fontFamily = fontFamilyBody,
                    color = hInk
                )
                OutlinedTextField(
                    value = newDaysText,
                    onValueChange = { newDaysText = it },
                    label = { Text("Total Duration (Days)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val days = newDaysText.toIntOrNull() ?: currentDurationDays
                    onConfirmNewDuration(days.coerceAtLeast(1))
                },
                colors = ButtonDefaults.buttonColors(containerColor = hGold)
            ) {
                Text("Apply New Pace", fontWeight = FontWeight.Bold, color = hWhite)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = hInkMuted)
            }
        }
    )
}

@Composable
fun RebalancePlanDialog(
    missedCount: Int,
    onConfirmRebalance: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Smart Rebalance",
                fontFamily = fontFamilyUi,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Text(
                text = if (missedCount > 0) {
                    "You have $missedCount past unread days. Rebalancing will distribute those remaining units evenly over your future days so you finish on time."
                } else {
                    "Your plan is currently on track! Rebalancing will redistribute all unread assignments smoothly across your remaining days."
                },
                fontSize = 13.sp,
                fontFamily = fontFamilyBody,
                color = hInk
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirmRebalance,
                colors = ButtonDefaults.buttonColors(containerColor = hTeal)
            ) {
                Text("Rebalance Now", fontWeight = FontWeight.Bold, color = hWhite)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = hInkMuted)
            }
        }
    )
}

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
                text = "Delete Plan",
                fontFamily = fontFamilyUi,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = hRed
            )
        },
        text = {
            Text(
                text = "Are you sure you want to delete '$planTitle'? This action cannot be undone.",
                fontSize = 13.sp,
                fontFamily = fontFamilyBody,
                color = hInk
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirmDelete,
                colors = ButtonDefaults.buttonColors(containerColor = hRed)
            ) {
                Text("Delete", fontWeight = FontWeight.Bold, color = hWhite)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = hInkMuted)
            }
        }
    )
}
