package com.nur.quran.ui.components.planner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.ui.screens.*

@Composable
fun PlanStatsHeaderCards(overallPct: Int, completionDate: String, streakDays: Int, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(10.dp), color = hCream, border = BorderStroke(1.dp, hBoneDark), modifier = modifier.fillMaxWidth()) {
        Row(Modifier.padding(vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            PlannerStat("Overall", "$overallPct%", Modifier.weight(1f))
            Divider(Modifier.height(34.dp).width(1.dp), color = hBoneDark)
            PlannerStat("Finish by", completionDate, Modifier.weight(1f))
            Divider(Modifier.height(34.dp).width(1.dp), color = hBoneDark)
            PlannerStat("Streak", "$streakDays day${if (streakDays == 1) "" else "s"}", Modifier.weight(1f))
        }
    }
}

@Composable
private fun PlannerStat(label: String, value: String, modifier: Modifier) {
    Column(modifier.padding(horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, fontFamily = fontFamilyBody, fontSize = 11.sp, color = hInkMid)
        Text(value, fontFamily = fontFamilyUi, fontSize = 23.sp, color = hInk)
    }
}
