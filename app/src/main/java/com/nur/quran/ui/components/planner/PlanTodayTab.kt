package com.nur.quran.ui.components.planner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.data.planner.PlannerAssignment
import com.nur.quran.data.planner.PrayerSlot
import com.nur.quran.data.planner.ReadingPlan
import com.nur.quran.ui.components.NurIcons
import com.nur.quran.ui.screens.*

/**
 * Today tab content matching web Planner.jsx lines 939-1064.
 */
@Composable
fun PlanTodayTab(
    planner: ReadingPlan,
    todayAssignment: PlannerAssignment?,
    todayPct: Int,
    todayDone: Int,
    todayTotal: Int,
    unitsLabel: String,
    todaySessionSeconds: Long,
    prayerSlots: List<PrayerSlot>,
    hasStartedReading: Boolean,
    onStartReading: () -> Unit,
    onMarkPrayerDone: (PrayerSlot) -> Unit,
    onUndoPrayer: (PrayerSlot) -> Unit,
    onShareProgress: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Hero Ring Progress (240dp)
        val ringSubtitle = "$todayDone OF $todayTotal ${unitsLabel.uppercase()} TODAY"
        RingProgress(
            percent = todayPct,
            subtitle = ringSubtitle,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 2. Daily Ritual Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Accent vertical bar
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(22.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(hGold)
                )
                Text(
                    text = "Daily Ritual",
                    fontFamily = fontFamilyUi,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = hInk
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (todaySessionSeconds > 0) {
                    Surface(
                        shape = RoundedCornerShape(100),
                        color = hTeal.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, hTeal.copy(alpha = 0.25f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = NurIcons.Clock,
                                contentDescription = null,
                                tint = hTeal,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = PlannerUtils.formatSessionTime(todaySessionSeconds),
                                fontFamily = fontFamilyMono,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = hTeal
                            )
                        }
                    }
                }

                // Share icon button
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, hBoneDark, CircleShape)
                        .background(hWhite)
                        .clickable { onShareProgress() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = NurIcons.Share2,
                        contentDescription = "Share",
                        tint = hInkMid,
                        modifier = Modifier.size(15.dp)
                    )
                }

                // Settings icon button
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, hBoneDark, CircleShape)
                        .background(hWhite)
                        .clickable { onOpenSettings() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = NurIcons.Settings,
                        contentDescription = "Settings",
                        tint = hInkMid,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3. 5 Prayer Slots
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            prayerSlots.forEach { slot ->
                val isCurrent = slot.status == "current"
                val isCompleted = slot.status == "completed"
                val isUpcoming = slot.status == "upcoming"
                val isLocked = slot.status == "locked"

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            isCurrent -> hCream
                            isCompleted -> Color(0x0A10B981)
                            else -> hWhite
                        }
                    ),
                    border = BorderStroke(
                        width = if (isCurrent) 1.5.dp else 1.dp,
                        color = when {
                            isCurrent -> hGold
                            isCompleted -> Color(0x4010B981)
                            else -> hBoneDark
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            // Avatar Icon (42dp circle)
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isCompleted -> Color(0xFF10B981)
                                            isCurrent -> hGold
                                            else -> hSurface
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                when {
                                    isCompleted -> Icon(NurIcons.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                    isCurrent -> Icon(NurIcons.BookOpen, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    isUpcoming -> Icon(NurIcons.Clock, contentDescription = null, tint = hInkMuted, modifier = Modifier.size(17.dp))
                                    isLocked -> Icon(NurIcons.Lock, contentDescription = null, tint = hInkMuted.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                                    else -> Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(hInkMuted))
                                }
                            }

                            // Slot Name & Subtitle
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = slot.name,
                                        fontFamily = fontFamilyUi,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCompleted) hInk.copy(alpha = 0.5f) else hInk,
                                        textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                    )
                                    if (!slot.time.isNullOrBlank()) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = hCream,
                                            border = BorderStroke(1.dp, hBoneDark)
                                        ) {
                                            Text(
                                                text = slot.time,
                                                fontFamily = fontFamilyMono,
                                                fontSize = 9.5.sp,
                                                color = hInkMid,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                val statusText = when {
                                    isCompleted -> "${slot.doneInSlot}/${slot.count} $unitsLabel ✓"
                                    isCurrent -> "${slot.doneInSlot} of ${slot.count} $unitsLabel read"
                                    isUpcoming -> "${slot.count} $unitsLabel · pending"
                                    isLocked -> "${slot.count} $unitsLabel · locked"
                                    else -> "No reading required"
                                }

                                Text(
                                    text = statusText,
                                    fontFamily = fontFamilyMono,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isCurrent) hGold else hInkMuted,
                                    letterSpacing = 0.4.sp
                                )
                            }
                        }

                        // Right action
                        when {
                            isCompleted -> {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(0x1F10B981))
                                        .border(1.dp, Color(0x4D10B981), CircleShape)
                                        .clickable { onUndoPrayer(slot) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(NurIcons.Check, contentDescription = "Completed (tap to undo)", tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                                }
                            }
                            isCurrent -> {
                                Button(
                                    onClick = onStartReading,
                                    shape = RoundedCornerShape(100),
                                    colors = ButtonDefaults.buttonColors(containerColor = hGold),
                                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text(
                                        text = if (hasStartedReading) "Resume" else "Start",
                                        fontFamily = fontFamilyUi,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                            isUpcoming -> {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .border(1.5.dp, hBoneDark, CircleShape)
                                        .background(hWhite)
                                        .clickable { onMarkPrayerDone(slot) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .border(1.dp, hInkMuted, CircleShape)
                                    )
                                }
                            }
                            isLocked -> {
                                Box(
                                    modifier = Modifier.size(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(NurIcons.Lock, contentDescription = "Locked", tint = hInkMuted.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
