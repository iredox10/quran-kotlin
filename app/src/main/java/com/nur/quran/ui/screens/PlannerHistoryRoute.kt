package com.nur.quran.ui.screens

import androidx.compose.runtime.*
import com.nur.quran.data.planner.PlannerHistory
import com.nur.quran.ui.components.planner.PlannerHistoryScreen
import com.nur.quran.ui.viewmodels.PlannerViewModel
import com.nur.quran.ui.viewmodels.SurahViewModel

@Composable
fun PlannerHistoryRoute(planner:PlannerViewModel,reader:SurahViewModel,selectedPlanId:String?=null,
    onBack:()->Unit,onOpenPlan:(String)->Unit,onOpenVerse:(String)->Unit) {
    val live by planner.allPlans.collectAsState()
    val archived by planner.archivedPlans.collectAsState()
    val timers by planner.sessionTimersByPlan.collectAsState()
    val notes by planner.plannerReflectionsByPlan.collectAsState()
    val highlights by planner.plannerBookmarksByPlan.collectAsState()
    val plans=remember(live,archived){PlannerHistory.plans(live,archived)}
    PlannerHistoryScreen(plans,timers,notes,highlights,onBack,onOpenPlan,selectedPlanId,reader::getHistoryVerseTexts,onOpenVerse)
}
