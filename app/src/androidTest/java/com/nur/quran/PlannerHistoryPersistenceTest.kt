package com.nur.quran

import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nur.quran.data.planner.*
import com.nur.quran.data.repository.QuranRepository
import com.nur.quran.ui.viewmodels.PlannerViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlannerHistoryPersistenceTest {
    @Test fun completedJournalAndHighlightsSurviveRestartAndAColdViewModel() = runBlocking {
        lateinit var repository:QuranRepository
        lateinit var oldPlans:List<ReadingPlan>
        lateinit var oldArchives:List<ReadingPlan>
        var oldId:String?=null
        var baselineReady=false
        val plan=PlannerEngine.buildReadingPlanner("page",1,PlannerEngine.formatPlannerDate(),1,2,chapters=emptyList())
        val completed=PlannerReadingProgress.setItemCount(plan,1,2)
        suspend fun await(test:suspend()->Boolean) = withTimeout(15000) { while(!test()) delay(50) }
        var newId:String?=null
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                lateinit var vm:PlannerViewModel
                scenario.onActivity { vm=ViewModelProvider(it)[PlannerViewModel::class.java] }
                repository=vm.javaClass.getDeclaredField("repository").apply{isAccessible=true}.get(vm) as QuranRepository
                oldPlans=repository.getAllPlans();oldArchives=repository.getArchivedPlans();oldId=repository.getActivePlannerId();baselineReady=true
                scenario.onActivity { vm.setActivePlan(completed) }
                await { repository.getAllPlans().any{it.id==plan.id} }
                val mutex=vm.javaClass.getDeclaredField("plannerMutationMutex").apply{isAccessible=true}.get(vm) as Mutex
                mutex.lock()
                try { scenario.onActivity {
                    vm.saveReflectionForPlan(plan.id,1,"Be patient and keep returning.")
                    vm.addPlannerBookmark(plan.id,"1:1","Al-Fatihah","Begin with intention.")
                    vm.restartActivePlan()
                } } finally { mutex.unlock() }
                await { vm.archivedPlans.value.any{it.id==plan.id} && vm.activePlan.value?.id!=plan.id }
                await { repository.getPlannerReflections(plan.id)[1]?.text=="Be patient and keep returning." && repository.getPlannerBookmarks(plan.id).singleOrNull()?.note=="Begin with intention." }
                await { repository.getActivePlannerId()==vm.activePlannerId.value && vm.activePlannerId.value!=plan.id }
                newId=vm.activePlannerId.value
            }
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                lateinit var vm:PlannerViewModel
                scenario.onActivity { vm=ViewModelProvider(it)[PlannerViewModel::class.java] }
                await { vm.plannerReflectionsByPlan.value[plan.id]?.get(1)=="Be patient and keep returning." && vm.plannerBookmarksByPlan.value[plan.id]?.singleOrNull()?.note=="Begin with intention." }
                assertEquals(newId,vm.activePlannerId.value)
                val archived=vm.archivedPlans.value.single{it.id==plan.id}
                assertEquals("Be patient and keep returning.",PlannerHistory.reflections(archived,vm.plannerReflectionsByPlan.value[plan.id].orEmpty())[1])
                scenario.onActivity { vm.saveReflectionForPlan(plan.id,1,"A reflection added to the completed run.") }
                await { repository.getArchivedPlans().find{it.id==plan.id}?.assignmentReflections?.get(1)=="A reflection added to the completed run." }
                assertEquals("Begin with intention.",repository.getPlannerBookmarks(plan.id).single().note)
                val mutex=vm.javaClass.getDeclaredField("plannerMutationMutex").apply{isAccessible=true}.get(vm) as Mutex
                mutex.withLock {
                    repository.savePlannerSelection(oldPlans,oldId)
                    repository.saveArchivedPlans(oldArchives)
                }
            }
        } finally {
            if(baselineReady) {
                repository.savePlannerSelection(oldPlans,oldId);repository.saveArchivedPlans(oldArchives)
                repository.savePlannerBookmarks(plan.id,emptyList());repository.savePlannerReflections(plan.id,emptyMap())
            }
        }
    }
}
