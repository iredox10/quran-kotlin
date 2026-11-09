package com.nur.quran.data.planner

import org.junit.Assert.*
import org.junit.Test

class PlannerPrayerResumeTest {
    private fun plan(page: Int = 6) = PlannerEngine.buildReadingPlanner("page",1,"2026-11-09",1,11,chapters=emptyList())
        .copy(assignmentReadPages=mapOf(1 to listOf(1,2,4,5)),lastReadPage=page)
    private fun slots(plan: ReadingPlan) = PlannerEngine.buildPrayerSlots(plan,plan.assignments[0],null)

    @Test fun resumeFollowsAsrWhenFajrHasAGapAndDhuhrIsComplete() {
        val plan = plan()
        val slots = slots(plan)
        assertEquals("upcoming",slots[0].status)
        assertEquals(2,slots[0].doneInSlot)
        assertEquals("completed",slots[1].status)
        assertEquals("current",slots[2].status)
        assertEquals("locked",slots[3].status)
        assertEquals(6,PlannerEngine.getAssignmentReadingResumePage(plan,plan.assignments[0]))
    }
    @Test fun aSavedCompletedPageAdvancesToTheNextReadingSlotRatherThanAnEarlierGap() {
        val plan = plan(5)
        assertEquals(6,PlannerEngine.getAssignmentReadingResumePage(plan,plan.assignments[0]))
        assertEquals("Asr",slots(plan).single { it.status=="current" }.name)
    }
    @Test fun markingAndUndoingFajrDoesNotEraseDhuhrOrMoveTheResumePoint() {
        val initial = plan()
        val fajr = slots(initial)[0]
        val marked = PlannerReadingProgress.setPrayerSlotCompleted(initial,1,fajr.slotStart,fajr.slotEnd,true)
        assertEquals("completed",slots(marked)[1].status)
        assertEquals(1,PlannerEngine.getPlannerSuccessMetrics(marked)!!.consistencyStreak)
        val undone = PlannerReadingProgress.setPrayerSlotCompleted(marked,1,fajr.slotStart,fajr.slotEnd,false)
        assertEquals("completed",slots(undone)[1].status)
        assertEquals(0,slots(undone)[0].doneInSlot)
        assertEquals(6,undone.lastReadPage)
        assertEquals("Asr",slots(undone).single { it.status=="current" }.name)
    }
    @Test fun partialReadingCountsForStreakAndLegacyPlansDefaultToNoInventedDates() {
        val original = PlannerEngine.buildReadingPlanner("page",1,"2026-11-09",1,11,chapters=emptyList())
        val first = PlannerReadingProgress.markPageRead(original,1,1,"2026-11-08")
        val second = PlannerReadingProgress.markPageRead(first,1,2,"2026-11-09")
        assertFalse(PlannerEngine.getAssignmentProgress(second,second.assignments[0]).isComplete)
        assertEquals(2,PlannerEngine.getPlannerSuccessMetrics(second,"2026-11-09")!!.consistencyStreak)
        val gson = com.google.gson.Gson()
        val raw = gson.toJsonTree(second).asJsonObject
        raw.remove("readingActivityDates")
        assertTrue(PlannerEngine.parseReadingPlan(raw.toString(),gson)!!.readingActivityDates.isEmpty())
        assertEquals(second.readingActivityDates,PlannerEngine.parseReadingPlan(gson.toJson(second),gson)!!.readingActivityDates)
        assertTrue(PlannerEngine.restartPlanner(second).readingActivityDates.isEmpty())
    }

}
