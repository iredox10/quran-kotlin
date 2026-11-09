package com.nur.quran.ui.components.planner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.R
import com.nur.quran.ui.components.NurIcons
import com.nur.quran.ui.screens.*

/** Quiet secondary tools: one row, full touch targets, no competing primary buttons. */
@Composable
internal fun PlannerQuickActions(showLog: Boolean, remindersEnabled: Boolean, reminderStatus: String,
    onLog: () -> Unit, onReminders: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (showLog) {
            TextButton(onClick = onLog, modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)) {
                Icon(NurIcons.BookOpen, null, Modifier.size(17.dp), tint = plannerAccentInk)
                Spacer(Modifier.width(7.dp))
                Text(stringResource(R.string.planner_outside_log), color = hInkMid, fontSize = 12.sp, lineHeight = 16.sp)
            }
            Box(Modifier.padding(horizontal = 8.dp).width(1.dp).height(20.dp).background(hBoneDark))
        }
        val label = stringResource(R.string.reminder_title)
        TextButton(onClick = onReminders,
            modifier = Modifier.weight(1f).heightIn(min = 48.dp).semantics { contentDescription = "$label. $reminderStatus" },
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)) {
            Box {
                Icon(NurIcons.Clock, null, Modifier.size(17.dp), tint = plannerAccentInk)
                if (remindersEnabled) Box(Modifier.align(Alignment.TopEnd).size(5.dp).background(hGold, CircleShape))
            }
            Spacer(Modifier.width(7.dp))
            Text(label, color = hInkMid, fontSize = 12.sp, lineHeight = 16.sp)
        }
    }
}
