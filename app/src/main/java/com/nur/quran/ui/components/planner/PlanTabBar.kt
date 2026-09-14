package com.nur.quran.ui.components.planner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.ui.screens.*

enum class PlannerTab(val title: String) {
    TODAY("Today"),
    PROGRESS("Progress"),
    JOURNAL("Journal")
}

/**
 * Tab Navigation Segmented Bar matching web Planner.jsx lines 918-934.
 */
@Composable
fun PlanTabBar(
    selectedTab: PlannerTab,
    onTabSelected: (PlannerTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = hCream,
        border = BorderStroke(1.5.dp, hBoneDark),
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlannerTab.values().forEach { tab ->
                val isActive = selectedTab == tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isActive) hWhite else Color.Transparent)
                        .then(
                            if (isActive) Modifier.border(1.dp, hBoneDark, RoundedCornerShape(14.dp))
                            else Modifier
                        )
                        .clickable { onTabSelected(tab) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab.title,
                        fontFamily = fontFamilyUi,
                        fontSize = 15.sp,
                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (isActive) hGold else hInkMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
