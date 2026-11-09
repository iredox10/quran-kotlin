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
import com.nur.quran.ui.screens.*

@Composable
internal fun PlannerFormIntro(icon: ImageVector, title: String, description: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
        Surface(shape = RoundedCornerShape(16.dp), color = hGold.copy(alpha = .13f)) {
            Icon(icon, null, Modifier.padding(14.dp).size(26.dp), tint = plannerAccentInk)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, color = hInk, fontFamily = fontFamilyUi, fontSize = 25.sp, lineHeight = 31.sp)
            Text(description, color = hInkMid, fontSize = 14.sp, lineHeight = 22.sp)
        }
    }
}

@Composable
internal fun PlannerFormSection(number: String, icon: ImageVector, title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(18.dp), color = hWhite, border = BorderStroke(1.dp, hBoneDark)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(icon, null, Modifier.size(20.dp), tint = plannerAccentInk)
                Text(title, color = hInk, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text(number, color = plannerAccentInk, fontSize = 12.sp, fontFamily = fontFamilyMono)
            }
            content()
        }
    }
}
