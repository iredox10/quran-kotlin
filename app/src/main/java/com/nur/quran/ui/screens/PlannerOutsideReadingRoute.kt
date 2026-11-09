package com.nur.quran.ui.screens

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import com.nur.quran.R
import com.nur.quran.data.planner.*
import com.nur.quran.ui.components.planner.PlannerOutsideReadingScreen
import com.nur.quran.ui.viewmodels.PlannerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun PlannerOutsideReadingRoute(viewModel: PlannerViewModel, onBack: () -> Unit, onSaved: (Int?) -> Unit) {
    val plan by viewModel.activePlan.collectAsState()
    val all by viewModel.allPlans.collectAsState()
    val archived by viewModel.archivedPlans.collectAsState()
    val timers by viewModel.sessionTimersByPlan.collectAsState()
    val pace by produceState<PlannerPagePace?>(null, all, archived, timers) {
        value = withContext(Dispatchers.Default) { PlannerOutsideReadings.pace((all + archived).distinctBy { it.id }, timers) }
    }
    val lifecycle = androidx.compose.ui.platform.LocalLifecycleOwner.current.lifecycle
    DisposableEffect(plan?.id, lifecycle) {
        val id = plan?.id
        val owner = "outside:$id"
        fun update() { if (id != null) viewModel.reminders.setReading(owner, id, null, lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) }
        val observer = androidx.lifecycle.LifecycleEventObserver { _, _ -> update() }
        lifecycle.addObserver(observer); update()
        onDispose { lifecycle.removeObserver(observer); if (id != null) viewModel.reminders.setReading(owner, id, null, false) }
    }
    val current = plan
    if (current != null) key(current.id) {
        PlannerOutsideReadingScreen(current, pace, onBack) { first, last, date, timing, seconds, id ->
            val result = viewModel.recordOutsideReading(current.id, first, last, date, timing, seconds, id)
            onSaved(result.resumeDay)
        }
    } else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(stringResource(R.string.planner_loading_planner)) }
}
