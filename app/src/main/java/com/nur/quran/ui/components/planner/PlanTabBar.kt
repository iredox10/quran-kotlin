package com.nur.quran.ui.components.planner

import androidx.compose.ui.res.stringResource
import com.nur.quran.R
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.ui.screens.*

enum class PlannerTab(val title: String) { TODAY("Today"), PROGRESS("Progress"), JOURNAL("Journal") }

@Composable
fun PlanTabBar(selectedTab: PlannerTab, onTabSelected: (PlannerTab) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().selectableGroup()) {
        PlannerTab.values().forEach { tab ->
            val selected = selectedTab == tab
            Column(Modifier.weight(1f).selectable(selected = selected, role = Role.Tab, onClick = { onTabSelected(tab) }), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.heightIn(min = 48.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(stringResource(when (tab) { PlannerTab.TODAY -> R.string.planner_today; PlannerTab.PROGRESS -> R.string.planner_progress; PlannerTab.JOURNAL -> R.string.planner_journal }), fontFamily = fontFamilyUi, fontSize = 17.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selected) plannerAccentInk else hInkMid)
                }
                Box(Modifier.fillMaxWidth().height(if (selected) 2.dp else 1.dp).background(if (selected) hGold else hBoneDark))
            }
        }
    }
}
