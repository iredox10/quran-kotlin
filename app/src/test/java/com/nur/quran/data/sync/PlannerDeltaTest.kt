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

    @Test fun staleProgressPacketCannotRestoreOldScheduleMetadata() {
        val current=mapOf("id" to "p", "assignmentsUpdatedAt" to 20L,"durationDays" to 1,"startDate" to "new", "assignmentProgress" to mapOf("1" to 1), "assignments" to listOf(mapOf("dayNumber" to 1,"title" to "new")))
        val stale=mapOf("id" to "p", "assignmentsUpdatedAt" to 10L,"durationDays" to 30,"startDate" to "old", "assignments" to listOf(mapOf("dayNumber" to 1,"title" to "old")),"assignmentProgress" to mapOf("1" to 2))
        val merged=mergePlannerRow(current,stale)
        assertEquals(1,merged["durationDays"])
        assertEquals("new",merged["startDate"])
        assertEquals(current["assignments"],merged["assignments"])
        assertEquals(mapOf("1" to 1),merged["assignmentProgress"])
        assertEquals(20L,merged["assignmentsUpdatedAt"])
    }

    @Test fun nullMalformedOrEmptyReplacementCannotClearAnExistingSchedule() {
        val current=mapOf("id" to "p", "assignmentsUpdatedAt" to 20L,"durationDays" to 1,"assignments" to listOf(mapOf("dayNumber" to 1)))
        for (bad in listOf(null,emptyList<Any>(),"broken",listOf(mapOf("dayNumber" to null)))) {
            val merged=mergePlannerRow(current,mapOf("id" to "p","assignmentsUpdatedAt" to 30L,"durationDays" to 0,"assignments" to bad))
            assertEquals(current["assignments"],merged["assignments"])
            assertEquals(1,merged["durationDays"])
            assertEquals(20L,merged["assignmentsUpdatedAt"])
        }
    }

    @Test fun completeSnapshotCanRepairAPartialRowAtTheSameRevision() {
        val partial=mapOf("id" to "p","assignmentsUpdatedAt" to 20L,"assignmentProgress" to mapOf("1" to 2))
        val full=mapOf("id" to "p","assignmentsUpdatedAt" to 20L,"title" to "Plan","durationDays" to 1,"assignments" to listOf(mapOf("dayNumber" to 1)))
        val merged=mergePlannerRow(partial,full)
        assertEquals(full["assignments"],merged["assignments"])
        assertEquals("Plan",merged["title"])
        assertEquals(mapOf("1" to 2),merged["assignmentProgress"])
    }

    @Test fun sparseRevisedRowsCannotEraseACompleteSchedule() {
        val item=mapOf("title" to "Page 1","subtitle" to "","route" to "/page/1","rangeValue" to "1","pageStart" to 1,"pageEnd" to 1)
        val day=mapOf("dayNumber" to 1,"date" to "2026-10-29","unitType" to "page","title" to "Day 1","subtitle" to "", "primaryRoute" to "/page/1","startUnit" to 1,"endUnit" to 1,"pageStart" to 1,"pageEnd" to 1,"items" to listOf(item))
        val current=mapOf("id" to "p","durationDays" to 1,"assignmentsUpdatedAt" to 20L,"assignments" to listOf(day))
        for (replacement in listOf(mapOf("dayNumber" to 1), day + ("items" to listOf(mapOf("id" to 1))),day + ("items" to emptyList<Any>()),day + ("date" to null))) {
            assertEquals(current,mergePlannerRow(current,mapOf("id" to "p","assignmentsUpdatedAt" to 30L,"assignments" to listOf(replacement),"durationDays" to 30)))
        }
        val next=mergePlannerRow(current,mapOf("id" to "p","assignmentsUpdatedAt" to 30L,"assignments" to listOf(day + mapOf("date" to "2026-10-30", "items" to listOf(item + ("rangeValue" to 1))))))
        assertEquals("2026-10-30",((next["assignments"] as List<*>).single() as Map<*,*>)["date"])
        val broken=current + ("assignments" to listOf(mapOf("dayNumber" to 1)))
        assertEquals(current,mergePlannerRow(broken,current))
    }

    @Test fun revisedScheduleReplacesDayIndexedProgressAndDeltaCarriesEmptyMaps() {
        val days=(1..3).map { mapOf("dayNumber" to it) }
        val old=mapOf("id" to "p","assignmentsUpdatedAt" to 10L,"assignments" to days,"completedDays" to listOf(3),
            "assignmentProgress" to mapOf("3" to 1),"assignmentReadPages" to mapOf("3" to listOf(7)),
            "assignmentCompletedItems" to mapOf("3" to listOf("7")),"assignmentCompletedAt" to mapOf("3" to "2026-10-28"))
        val revised=old + mapOf("assignmentsUpdatedAt" to 20L,"completedDays" to listOf(1),
            "assignmentProgress" to mapOf("1" to 1),"assignmentReadPages" to mapOf("1" to listOf(7)),
            "assignmentCompletedItems" to emptyMap<String,Any>(),"assignmentCompletedAt" to emptyMap<String,Any>())
        val delta=syncDelta(mapOf("planners" to listOf(old)),mapOf("planners" to listOf(revised)))
        val row=(delta["planners"] as List<*>).single() as Map<*,*>
        assertTrue(row.containsKey("assignmentCompletedAt"))
        assertEquals(emptyMap<String,Any>(),row["assignmentCompletedAt"])
        val merged=mergePlannerRow(old,row)
        assertEquals(revised,merged)
        assertEquals(revised,mergePlannerRow(merged,old))
    }

    @Test fun progressDeltaKeepsItsScheduleRevisionAndMergesWithConcurrentCurrentProgress() {
        val old=mapOf("id" to "p","assignmentsUpdatedAt" to 20L,"assignments" to listOf(mapOf("dayNumber" to 1)),"assignmentProgress" to emptyMap<String,Int>())
        val next=old + ("assignmentProgress" to mapOf("1" to 1))
        val delta=syncDelta(mapOf("planners" to listOf(old)),mapOf("planners" to listOf(next)))
        val row=(delta["planners"] as List<*>).single() as Map<*,*>
        assertEquals(20L,row["assignmentsUpdatedAt"])
        assertFalse(row.containsKey("assignments"))
        assertEquals(next,mergePlannerRow(old,row))
    }

    @Test fun savedProgressPacketRecoversItsRevisionFromItsSavedSnapshot() {
        val changes=mapOf("planners" to listOf(mapOf("id" to "p","assignmentProgress" to mapOf("1" to 1))))
        val snapshot=mapOf("planners" to listOf(mapOf("id" to "p","assignmentsUpdatedAt" to 20L)))
        val recovered=compactSyncPayload(changes,snapshot)
        val row=(recovered["planners"] as List<*>).single() as Map<*,*>
        assertEquals(20L,row["assignmentsUpdatedAt"])
    }

}
