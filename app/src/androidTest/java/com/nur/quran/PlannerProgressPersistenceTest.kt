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
class PlannerProgressPersistenceTest {
    @Test fun queuedResumeWritesPreserveReadMarkersAndPersistAcrossReload() = runBlocking {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            lateinit var vm: PlannerViewModel
            scenario.onActivity { vm = ViewModelProvider(it)[PlannerViewModel::class.java] }
            val repository = vm.javaClass.getDeclaredField("repository").apply { isAccessible = true }.get(vm) as QuranRepository
            val mutex = vm.javaClass.getDeclaredField("plannerMutationMutex").apply { isAccessible = true }.get(vm) as Mutex
            val oldPlans = repository.getAllPlans()
            val oldId = repository.getActivePlannerId()
            val plan = PlannerEngine.buildReadingPlanner("page",2,PlannerEngine.formatPlannerDate(),1,4,chapters=emptyList())
            suspend fun savedSelection(): ReadingPlan? {
                val (plans, activeId) = repository.parsePlannerSelection(repository.getPlannerCacheSnapshot())
                return plans.find { it.id == activeId }
            }
            suspend fun awaitSaved(predicate: (ReadingPlan) -> Boolean): ReadingPlan = withTimeout(15000) {
                while (true) {
                    val saved = savedSelection()
                    if (saved?.id == plan.id && predicate(saved)) return@withTimeout saved
                    delay(50)
                }
                error("unreachable")
            }
            try {
                scenario.onActivity { vm.setActivePlan(plan) }
                awaitSaved { true }
                mutex.lock()
                try {
                    scenario.onActivity {
                        vm.markPageRead(1,1)
                        vm.setLastReadPosition(2,"2:1")
                        vm.markPageRead(1,2)
                        vm.setLastReadPosition(3,"2:6")
                        vm.setLastReadPosition(3)
                    }
                } finally { mutex.unlock() }
                val saved = awaitSaved {
                    it.assignmentReadPages[1]?.toSet() == setOf(1,2) && it.lastReadPage == 3 && it.lastReadVerseKey == "2:6"
                }
                assertTrue(PlannerEngine.getAssignmentProgress(saved,saved.assignments[0]).isComplete)
                assertEquals(2,PlannerEngine.getResumeAssignment(saved)?.dayNumber)
                assertEquals(3,savedSelection()!!.lastReadPage)
                scenario.onActivity {
                    vm.toggleAssignmentCompleted(1)
                    vm.stopPlannerTimerForPlan(plan.id,2,45)
                }
                val undone = awaitSaved { !PlannerEngine.getAssignmentProgress(it,it.assignments[0]).isComplete }
                assertEquals(0,PlannerEngine.getAssignmentProgress(undone,undone.assignments[0]).readPagesCount)
                assertEquals(3,undone.lastReadPage)
                val readingDates = vm.sessionTimersByPlan.value[plan.id].orEmpty().values.flatMap { it.secondsByDate.keys }.toSet()
                assertEquals(1,PlannerEngine.getPlannerSuccessMetrics(undone,readingDates=readingDates)!!.consistencyStreak)
            } finally {
                mutex.withLock { repository.savePlannerSelection(oldPlans,oldId) }
                repository.savePlannerSessionTimers(plan.id,emptyMap())
            }
        }
    }
}
