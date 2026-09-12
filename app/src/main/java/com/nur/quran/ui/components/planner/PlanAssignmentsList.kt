package com.nur.quran.ui.components.planner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.data.planner.PlannerAssignment
import com.nur.quran.ui.components.NurIcons
import com.nur.quran.ui.screens.*

/**
 * Schedule list of all plan assignments matching web Planner.jsx lines 1120-1280.
 */
@Composable
fun PlanAssignmentsList(
    assignments: List<PlannerAssignment>,
    completedDays: Set<Int>,
    todayDayNumber: Int,
    onReadDay: (Int) -> Unit,
    onToggleDayComplete: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        assignments.forEach { item ->
            val isDone = completedDays.contains(item.dayNumber)
            val isToday = item.dayNumber == todayDayNumber

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isToday) hCream else hSurface
                ),
                border = BorderStroke(
                    width = if (isToday) 1.5.dp else 1.dp,
                    color = if (isToday) hGold else hBoneDark.copy(alpha = 0.6f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onReadDay(item.dayNumber) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = when {
                                isDone -> Color(0x2610B981)
                                isToday -> hGoldSoft
                                else -> hBoneDark.copy(alpha = 0.3f)
                            }
                        ) {
                            Text(
                                text = "DAY ${item.dayNumber}",
                                fontFamily = fontFamilyMono,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    isDone -> Color(0xFF10B981)
                                    isToday -> hGold
                                    else -> hInkMuted
                                },
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }

                        Column {
                            Text(
                                text = item.title,
                                fontFamily = fontFamilyUi,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = hInk
                            )
                            if (item.subtitle.isNotBlank()) {
                                Text(
                                    text = item.subtitle,
                                    fontFamily = fontFamilyBody,
                                    fontSize = 11.sp,
                                    color = hInkMuted
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = { onToggleDayComplete(item.dayNumber) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isDone) NurIcons.CheckCircle2 else NurIcons.Circle,
                            contentDescription = if (isDone) "Done" else "Mark Done",
                            tint = if (isDone) Color(0xFF10B981) else hInkMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
