package com.nur.quran.data.planner

import com.nur.quran.data.sync.mergeStateInto
import org.junit.Assert.*
import org.junit.Test

class PlannerReadingProgressTest {
    private fun plan() = PlannerEngine.buildReadingPlanner("page", 3, "2026-11-01", 1, 6, chapters = emptyList())

    @Test fun pagesStayPartialUntilTheLastPageIsActuallyMarkedRead() {
        val initial = plan()
        val first = PlannerReadingProgress.markPageRead(initial, 1, 1, "2026-11-09")
        assertEquals(1, PlannerEngine.getAssignmentProgress(first, first.assignments[0]).readPagesCount)
        assertFalse(PlannerEngine.getAssignmentProgress(first, first.assignments[0]).isComplete)
        val complete = PlannerReadingProgress.markPageRead(first, 1, 2, "2026-11-09")
        assertTrue(PlannerEngine.getAssignmentProgress(complete, complete.assignments[0]).isComplete)
        assertEquals("2026-11-09", complete.assignmentCompletedAt[1])
        assertEquals(complete, PlannerReadingProgress.markPageRead(complete, 1, 2))
        assertEquals(initial, PlannerReadingProgress.markPageRead(initial, 1, 604))
    }
    @Test fun explicitUndoClearsReadPagesAndSurvivesAMergeWithTheOldSnapshot() {
        val initial = plan()
        val done = PlannerReadingProgress.markPageRead(PlannerReadingProgress.markPageRead(initial,1,1),1,2)
        val undone = PlannerReadingProgress.setItemCount(done, 1, 0)
        assertFalse(PlannerEngine.getAssignmentProgress(undone, undone.assignments[0]).isComplete)
        assertEquals(0, PlannerEngine.getAssignmentProgress(undone, undone.assignments[0]).readPagesCount)
        assertFalse(undone.completedDays.contains(1))
        assertNull(undone.assignmentCompletedAt[1])
        val gson = com.google.gson.Gson()
        val type = object : com.google.gson.reflect.TypeToken<Map<String, Any?>>() {}.type
        fun state(p: ReadingPlan): Map<String, Any?> = mapOf("planners" to listOf(gson.fromJson<Map<String, Any?>>(gson.toJson(p), type)))
        val merged = mergeStateInto(state(done), state(undone)) as Map<*, *>
        val roundTrip = PlannerEngine.parseReadingPlan(gson.toJson((merged["planners"] as List<*>).first()), gson)!!
        assertFalse(PlannerEngine.getAssignmentProgress(roundTrip, roundTrip.assignments[0]).isComplete)
    }
    @Test fun undoDoesNotRecompleteAnUncheckedItemOnASharedBoundaryPage() {
        val initial = plan()
        val assignment = initial.assignments[0].copy(pageStart=1,pageEnd=1,
            items=initial.assignments[0].items.map { it.copy(pageStart=1,pageEnd=1) })
        val done = initial.copy(assignments=listOf(assignment),assignmentReadPages=mapOf(1 to listOf(1)))
        val undone = PlannerReadingProgress.setItemCount(done,1,1)
        val progress = PlannerEngine.getAssignmentProgress(undone,assignment)
        assertEquals(1,progress.completedCount)
        assertFalse(progress.isComplete)
    }
    @Test fun olderCountOnlyProgressAndOtherCompletedDaysSurviveNewReading() {
        val legacy = plan().copy(assignmentProgress=mapOf(1 to 1,2 to 2),completedDays=listOf(2))
        val updated = PlannerReadingProgress.markPageRead(legacy,1,2)
        assertEquals(2,PlannerEngine.getAssignmentProgress(updated,updated.assignments[0]).completedCount)
        assertTrue(updated.completedDays.containsAll(listOf(1,2)))
        val itemUpdated = PlannerReadingProgress.completeItem(legacy,1,"2")
        assertEquals(2,PlannerEngine.getAssignmentProgress(itemUpdated,itemUpdated.assignments[0]).completedCount)
    }
    @Test fun resumeReturnsTheUnfinishedAssignmentActuallyBeingRead() {
        val initial = plan().copy(lastReadPage = 2, lastReadVerseKey = "2:1")
        assertEquals(1, PlannerEngine.getResumeAssignment(initial, "2026-11-03")?.dayNumber)
        val completed = PlannerReadingProgress.setItemCount(initial,1,2)
        assertEquals(2, PlannerEngine.getResumeAssignment(completed, "2026-11-03")?.dayNumber)
        assertEquals(2, PlannerEngine.getResumeAssignment(plan(), "2026-11-02")?.dayNumber)
    }
    @Test fun streakRecoversAfterMissedDaysAndUsesActualReadingDates() {
        assertEquals(3, PlannerEngine.getReadingStreak(setOf("2026-11-01", "2026-11-07", "2026-11-08", "2026-11-09"), "2026-11-09"))
        assertEquals(2, PlannerEngine.getReadingStreak(setOf("2026-11-07", "2026-11-08"), "2026-11-09"))
        assertEquals(0, PlannerEngine.getReadingStreak(setOf("2026-11-07", "2026-11-10"), "2026-11-09"))
        val late = PlannerReadingProgress.setItemCount(plan(),1,2,"2026-11-09")
        assertEquals(1, PlannerEngine.getPlannerSuccessMetrics(late,"2026-11-09")!!.consistencyStreak)
        assertEquals(2, PlannerEngine.getPlannerSuccessMetrics(plan(),"2026-11-09",setOf("2026-11-08","2026-11-09"))!!.consistencyStreak)
    }
}
