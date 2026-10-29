package com.nur.quran.data.sync

import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test

class PlannerDeltaTest {
    @Test fun progressChangeDoesNotUploadUnchangedSchedule() {
        val plan = mapOf("id" to "large", "assignments" to (1..604).map { mapOf("dayNumber" to it,"title" to "x".repeat(1800)) },"assignmentProgress" to emptyMap<String,Int>())
        val before = mapOf("planners" to listOf(plan))
        val after = mapOf("planners" to listOf(plan + ("assignmentProgress" to mapOf("1" to 1))))
        assertTrue(Gson().toJson(before).length>1_100_000)
        assertEquals(after,slimStatePayload(after))
        val delta = syncDelta(before,after)
        assertTrue(Gson().toJson(delta).length<200)
        assertEquals(after["planners"],(mergeStateInto(before,delta) as Map<*,*>)["planners"])
    }

    @Test fun reschedulingOneDayRetainsOtherAssignments() {
        val plan=mapOf("id" to "p", "assignments" to listOf(mapOf("dayNumber" to 1,"date" to "old","updatedAt" to 1),mapOf("dayNumber" to 2,"date" to "kept")))
        val edited=plan + ("assignments" to listOf(mapOf("dayNumber" to 1,"date" to "new","updatedAt" to 2),mapOf("dayNumber" to 2,"date" to "kept")))
        val delta=syncDelta(mapOf("planners" to listOf(plan)),mapOf("planners" to listOf(edited)))
        val rows= ((delta["planners"] as List<*>).first() as Map<*,*>)["assignments"] as List<*>
        assertEquals(1,rows.size)
        assertEquals(listOf(edited),(mergeStateInto(mapOf("planners" to listOf(plan)),delta) as Map<*,*>)["planners"])
    }
    @Test fun revisedPaceReplacesRemovedDaysAndRejectsStaleSchedule() {
        val old=mapOf("id" to "p","assignmentsUpdatedAt" to 10L,"assignments" to listOf(mapOf("dayNumber" to 1),mapOf("dayNumber" to 2)),"assignmentProgress" to mapOf("1" to 1))
        val next=old + mapOf("assignmentsUpdatedAt" to 11L,"assignments" to listOf(mapOf("dayNumber" to 1)))
        val base=mapOf("planners" to listOf(old))
        val edited=mapOf("planners" to listOf(next))
        val delta=syncDelta(base,edited)
        val merged=mergeStateInto(base,delta) as Map<*,*>
        assertEquals(edited["planners"],merged["planners"])
        val stale=mergeStateInto(merged,mapOf("planners" to listOf(old))) as Map<*,*>
        assertEquals(edited["planners"],stale["planners"])
    }

    @Test fun missingOptionalPlanFieldsDoNotCrashOrSendEmptyPlanEdits() {
        val before=mapOf("planners" to listOf(mapOf("id" to "p","title" to "Plan","webOnlyField" to "kept")))
        val after=mapOf("planners" to listOf(mapOf("id" to "p","title" to "Plan")))
        assertTrue(syncDelta(before,after).isEmpty())
    }

}
