package com.nur.quran.ui.components.planner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.data.planner.PlannerAssignment
import com.nur.quran.ui.components.NurIcons
import com.nur.quran.ui.screens.*

/**
 * Today's Assignment Card matching web Planner.jsx lines 950-1080.
 */
@Composable
fun PlanTodayCard(
    assignment: PlannerAssignment?,
    isCompleted: Boolean,
    onToggleComplete: () -> Unit,
    onReadAssignment: () -> Unit,
    sessionSeconds: Long,
    prayerTimings: Map<String, String>? = null,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = hCream),
        border = BorderStroke(1.5.dp, if (isCompleted) Color(0xFF10B981).copy(alpha = 0.5f) else hBoneDark),
        modifier = modifier.fillMaxWidth()
    ) {
        if (assignment == null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "No assignment scheduled for today",
                    fontFamily = fontFamilyBody,
                    fontSize = 14.sp,
                    color = hInkMuted
                )
            }
        } else {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isCompleted) Color(0x2610B981) else hGoldSoft
                        ) {
                            Text(
                                text = "DAY ${assignment.dayNumber}",
                                fontFamily = fontFamilyMono,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCompleted) Color(0xFF10B981) else hGold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        if (sessionSeconds > 0) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = hSurface
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = NurIcons.Clock,
                                        contentDescription = null,
                                        tint = hTeal,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = PlannerUtils.formatSessionTime(sessionSeconds),
                                        fontFamily = fontFamilyMono,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = hTeal
                                    )
                                }
                            }
                        }
                    }

                    // Checkbox Mark Complete
                    IconButton(
                        onClick = onToggleComplete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isCompleted) NurIcons.CheckCircle2 else NurIcons.Circle,
                            contentDescription = if (isCompleted) "Completed" else "Mark Complete",
                            tint = if (isCompleted) Color(0xFF10B981) else hInkMuted,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Title and Range
                Text(
                    text = assignment.title,
                    fontFamily = fontFamilyUi,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = hInk
                )

                if (assignment.subtitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = assignment.subtitle,
                        fontFamily = fontFamilyBody,
                        fontSize = 13.sp,
                        color = hInkMuted
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Read Button
                Button(
                    onClick = onReadAssignment,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCompleted) hSurface else hTeal
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = NurIcons.BookOpen,
                            contentDescription = null,
                            tint = if (isCompleted) hInk else hWhite,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isCompleted) "Read Again" else "Start Reading",
                            fontFamily = fontFamilyUi,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (isCompleted) hInk else hWhite
                        )
                    }
                }

                // Prayer schedule breakdown if present
                if (!prayerTimings.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = hBoneDark.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(12.dp))
                    PrayerScheduleSection(
                        prayerTimings = prayerTimings,
                        totalUnits = assignment.endUnit - assignment.startUnit + 1,
                        unitType = assignment.unitType
                    )
                }
            }
        }
    }
}

/**
 * 5-prayer daily distribution breakdown matching web Planner.jsx.
 */
@Composable
fun PrayerScheduleSection(
    prayerTimings: Map<String, String>,
    totalUnits: Int,
    unitType: String,
    modifier: Modifier = Modifier
) {
    val prayers = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")
    val perPrayer = (totalUnits / 5).coerceAtLeast(1)

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "PRAYER SCHEDULE DISTRIBUTION",
            fontFamily = fontFamilyMono,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = hInkMuted
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            prayers.forEach { name ->
                val time = prayerTimings[name] ?: "--:--"
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = name,
                        fontFamily = fontFamilyUi,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = hInk
                    )
                    Text(
                        text = time,
                        fontFamily = fontFamilyMono,
                        fontSize = 9.sp,
                        color = hInkMuted
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "~$perPrayer $unitType",
                        fontFamily = fontFamilyBody,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = hTeal
                    )
                }
            }
        }
    }
}
