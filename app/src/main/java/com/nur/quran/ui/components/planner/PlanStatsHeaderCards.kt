package com.nur.quran.ui.components.planner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.ui.components.NurIcons
import com.nur.quran.ui.screens.*

/**
 * 3-Card stats header row matching web Planner.jsx lines 901-915.
 */
@Composable
fun PlanStatsHeaderCards(
    overallPct: Int,
    completionDate: String,
    streakDays: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatCard(
            label = "OVERALL",
            value = "$overallPct%",
            icon = NurIcons.TrendingUp,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "FINISH BY",
            value = completionDate,
            icon = NurIcons.CalendarDays,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "STREAK",
            value = "$streakDays Day${if (streakDays != 1) "s" else ""}",
            icon = NurIcons.Flame,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = hCream),
        border = BorderStroke(1.5.dp, hBoneDark),
        modifier = modifier
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = hInkMuted.copy(alpha = 0.25f),
                modifier = Modifier
                    .size(20.dp)
                    .align(Alignment.TopEnd)
            )

            Column {
                Text(
                    text = label,
                    fontFamily = fontFamilyMono,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.8.sp,
                    color = hInkMuted
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = value,
                    fontFamily = fontFamilyUi,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = hInk
                )
            }
        }
    }
}
