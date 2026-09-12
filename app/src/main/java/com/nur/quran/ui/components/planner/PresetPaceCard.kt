package com.nur.quran.ui.components.planner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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

data class PresetPaceItem(
    val id: String,
    val title: String,
    val durationText: String,
    val durationDays: Int,
    val icon: ImageVector,
    val badge: String? = null
)

val PRESET_PACES = listOf(
    PresetPaceItem(
        id = "ramadan",
        title = "Ramadan Pace",
        durationText = "30 DAYS",
        durationDays = 30,
        icon = NurIcons.Moon
    ),
    PresetPaceItem(
        id = "steady",
        title = "Steady Journey",
        durationText = "60 DAYS",
        durationDays = 60,
        icon = NurIcons.Sun,
        badge = "MOST BALANCED"
    ),
    PresetPaceItem(
        id = "devotional",
        title = "Devotional Path",
        durationText = "1 YEAR",
        durationDays = 365,
        icon = NurIcons.BookOpen
    )
)

/**
 * Preset pace selection card matching web PACES cards in Planner.jsx.
 */
@Composable
fun PresetPaceCard(
    title: String,
    daysText: String,
    dailyPages: Int,
    selected: Boolean,
    badge: String? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val pagesPerPrayer = "%.1f".format(dailyPages / 5f)
    val ringProgress = dailyPages / 21f // Normalize against Ramadan pace (max ~21 pgs/day)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = if (selected) hCream else hCream.copy(alpha = 0.5f)),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) hGold else hBoneDark),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (badge != null) {
                Surface(
                    shape = RoundedCornerShape(100),
                    color = hGoldSoft
                ) {
                    Text(
                        text = badge,
                        fontSize = 8.sp,
                        fontFamily = fontFamilyMono,
                        fontWeight = FontWeight.Bold,
                        color = hGold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            } else {
                Spacer(modifier = Modifier.height(18.dp))
            }

            PaceRing(
                progress = ringProgress,
                dailyUnits = dailyPages,
                unitLabel = "PAGES",
                isSelected = selected
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = title,
                fontFamily = fontFamilyUi,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = hInk
            )

            Text(
                text = daysText,
                fontFamily = fontFamilyMono,
                fontSize = 10.sp,
                color = hInkMuted
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "~$pagesPerPrayer/prayer",
                fontFamily = fontFamilyBody,
                fontSize = 10.sp,
                color = hInkMid
            )
        }
    }
}

@Composable
fun PresetPacesRow(
    selectedPaceId: String,
    onSelectPace: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        PRESET_PACES.forEach { pace ->
            val stats = PlannerUtils.getPaceStats(pace.durationDays)
            PresetPaceCard(
                title = pace.title,
                daysText = pace.durationText,
                dailyPages = stats.dailyPages,
                selected = selectedPaceId == pace.id,
                badge = pace.badge,
                modifier = Modifier.weight(1f),
                onClick = { onSelectPace(pace.id) }
            )
        }
    }
}
