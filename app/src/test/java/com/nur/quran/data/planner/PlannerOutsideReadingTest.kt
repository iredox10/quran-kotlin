package com.nur.quran.data.planner

import com.google.gson.Gson
import com.nur.quran.data.sync.mergePlannerRow
import org.junit.Assert.*
import org.junit.Test

class PlannerOutsideReadingTest {
    // Pin the actual reader position; an unstarted overdue plan opens its current scheduled day.
    private fun plan() = PlannerEngine.buildReadingPlanner("page", 3, "2026-11-01", 1, 9, chapters = emptyList()).copy(lastReadPage = 1)
    private fun record(plan: ReadingPlan, first: Int = 1, last: Int = 2, id: String = "outside-1", date: String = "2026-11-03") =
        PlannerOutsideReadings.record(plan, first, last, date, PlannerOutsideReadings.MANUAL,
            (first..last).associateWith { 120L }, id = id, now = 1234L, today = "2026-11-09")

    @Test fun `outside pages advance existing progress and resume at the next page`() {
        val result = record(plan())
        assertEquals(listOf(1, 2), result.plan.assignmentReadPages[1])
        assertEquals(2, PlannerEngine.getAssignmentProgress(result.plan, result.plan.assignments[0]).readPagesCount)
        assertEquals(3, result.resumePage)
        assertEquals(1, result.resumeDay)
        assertEquals(3, PlannerEngine.getAssignmentReadingResumePage(result.plan, result.plan.assignments[0]))
        assertEquals(listOf("2026-11-03"), result.plan.readingActivityDates)
        assertNull(result.plan.lastReadVerseKey)
    }

    @Test fun `range crossing multiple days resumes the next day and no page is timed twice`() {
        val result = record(plan(), last = 6)
        assertEquals(listOf(1, 2), result.plan.completedDays)
        assertEquals(3, result.resumeDay)
        assertEquals(7, result.resumePage)
        assertEquals(mapOf(1 to 360L, 2 to 360L), PlannerOutsideReadings.secondsByDay(result.plan))
        assertEquals(720L, result.entry.totalSeconds)
    }

    @Test fun `overlap skips already read pages and their time`() {
        val initial = PlannerReadingProgress.markPageRead(plan(), 1, 1)
        val selection = PlannerOutsideReadings.select(initial, 1, 3)
        assertEquals(1, selection.skippedPages)
        assertEquals(listOf(2, 3), selection.pages)
        val result = record(initial, first = 2, last = 3)
        assertEquals(240L, result.entry.totalSeconds)
        assertEquals(setOf(2, 3), result.entry.pageSeconds.keys)
        assertEquals(4, result.resumePage)
        assertEquals(listOf(1), result.plan.completedDays)
    }

    @Test fun `all read pages and future dates reject without changing the plan`() {
        val initial = plan()
        val complete = record(initial, last = 9).plan
        assertNull(record(initial, last = 9).resumePage)
        for (action in listOf<() -> Unit>(
            { record(complete) }, { record(initial, date = "2026-11-10") },
            { record(initial, first = 4, last = 2) }, { record(initial, first = 9, last = 10) },
            { record(initial, date = "not-a-date") }
        )) assertThrows(IllegalArgumentException::class.java) { action() }
        assertTrue(initial.assignmentReadPages.isEmpty())
        assertTrue(initial.outsideReadings.isEmpty())
    }

    @Test fun `individual manual times remain exact and invalid durations reject`() {
        val initial = plan()
        val result = PlannerOutsideReadings.record(initial, 1, 2, "2026-11-03", "manual",
            mapOf(1 to 90L, 2 to 150L), today = "2026-11-09")
        assertEquals(mapOf(1 to 90L, 2 to 150L), result.entry.pageSeconds)
        assertEquals(240L, result.entry.totalSeconds)
        for (times in listOf(emptyMap(), mapOf(1 to 0L, 2 to 5L), mapOf(1 to -2L, 2 to 5L), mapOf(1 to 86_400L, 2 to 1L))) {
            assertThrows(IllegalArgumentException::class.java) {
                PlannerOutsideReadings.record(initial, 1, 2, "2026-11-03", "manual", times, today = "2026-11-09")
            }
        }
    }

    @Test fun `pace is weighted measured time only and estimates cannot train themselves`() {
        val original = PlannerReadingProgress.markPageRead(plan(), 1, 1)
        val timers = mapOf(original.id to mapOf(1 to PlannerSessionTimer(180)))
        val pace = PlannerOutsideReadings.pace(listOf(original), timers)!!
        assertEquals(180L, pace.secondsPerPage)
        val logged = PlannerOutsideReadings.record(original, 2, 3, "2026-11-03", "estimated", pace = pace, today = "2026-11-09")
        assertEquals(360L, logged.entry.totalSeconds)
        assertEquals(1, logged.entry.samplePages)
        assertEquals(pace, PlannerOutsideReadings.pace(listOf(logged.plan, logged.plan), timers))
        assertNull(PlannerOutsideReadings.pace(listOf(record(plan()).plan), emptyMap()))
        assertThrows(IllegalArgumentException::class.java) {
            PlannerOutsideReadings.record(plan(), 1, 2, "2026-11-03", "estimated", today = "2026-11-09")
        }
    }

    @Test fun `history combines time once with source breakdown on the actual reading date`() {
        val entered = record(plan(), date = "2026-11-02")
        val estimated = PlannerOutsideReadings.record(entered.plan, 3, 3, "2026-11-02", "estimated",
            pace = PlannerPagePace(100, 2), today = "2026-11-09")
        val summary = PlannerHistory.summarize(estimated.plan, mapOf(1 to PlannerSessionTimer(60, secondsByDate = mapOf("2026-11-02" to 60))))
        assertEquals(400L, summary.totalSeconds)
        val day = summary.readingDays.single()
        assertEquals("2026-11-02", day.date)
        assertEquals(60L, day.inAppSeconds)
        assertEquals(240L, day.outsideManualSeconds)
        assertEquals(100L, day.outsideEstimatedSeconds)
        assertEquals(400L, PlannerHistory.chartPoints(summary).single().cumulativeSeconds)
    }

    @Test fun `old plans restore and completed run logs remain outside a restart`() {
        val gson = Gson()
        val done = record(plan(), last = 9).plan
        val restored = PlannerEngine.parseReadingPlan(gson.toJson(done), gson)!!
        assertEquals(done, restored)
        val json = gson.toJsonTree(plan()).asJsonObject.apply { remove("outsideReadings") }.toString()
        assertTrue(PlannerEngine.parseReadingPlan(json, gson)!!.outsideReadings.isEmpty())
        val next = PlannerEngine.restartPlanner(restored, "2026-11-09")
        assertTrue(next.outsideReadings.isEmpty())
        assertEquals(1, restored.outsideReadings.size)
    }

    @Test fun `sync merges independent log IDs without doubling a replay`() {
        val gson = Gson()
        fun row(plan: ReadingPlan): Map<*, *> = gson.fromJson(gson.toJson(plan), Map::class.java)
        val original = plan()
        val first = record(original, first = 1, last = 2)
        val second = record(original.copy(lastReadPage = 4), first = 4, last = 5, id = "other-device")
        val merged = mergePlannerRow(row(first.plan), row(second.plan))
        val replayed = mergePlannerRow(merged, row(first.plan))
        val restored = PlannerEngine.parseReadingPlan(gson.toJson(replayed), gson)!!
        assertEquals(setOf("outside-1", "other-device"), restored.outsideReadings.keys)
        assertEquals(480L, PlannerOutsideReadings.entries(restored).sumOf { it.totalSeconds })
        assertEquals(480L, PlannerHistory.summarize(restored, emptyMap()).totalSeconds)
    }

    @Test fun `legacy item completion and a partial outside page remain complete`() {
        val initial = plan()
        val legacy = initial.copy(assignmentProgress = mapOf(1 to 1))
        val result = record(legacy, first = 2, last = 2)
        assertEquals(2, PlannerEngine.getAssignmentProgress(result.plan, result.plan.assignments[0]).completedCount)
        assertEquals(setOf(1, 2), result.plan.assignmentReadPages[1]!!.toSet())
    }
    @Test fun `shared boundary pages advance both assignments but contribute time once`() {
        val initial = plan()
        val a = initial.assignments[0].copy(pageStart = 1, pageEnd = 2,
            items = listOf(initial.assignments[0].items[0].copy(pageStart = 1, pageEnd = 2)))
        val b = initial.assignments[1].copy(pageStart = 2, pageEnd = 3,
            items = listOf(initial.assignments[1].items[0].copy(pageStart = 2, pageEnd = 3)))
        val shared = initial.copy(assignments = listOf(a, b), durationDays = 2)
        val result = record(PlannerReadingProgress.markPageRead(shared, 1, 1), first = 2, last = 2)
        assertEquals(listOf(1, 2), result.plan.assignmentReadPages[1])
        assertEquals(listOf(2), result.plan.assignmentReadPages[2])
        assertEquals(120L, result.entry.totalSeconds)
        assertEquals(mapOf(1 to 120L), PlannerOutsideReadings.secondsByDay(result.plan))
        assertEquals(3, result.resumePage)
    }

    @Test fun `corrupt synced logs cannot discard the schedule or valid history`() {
        val gson = Gson()
        val valid = record(plan()).plan
        val json = gson.toJsonTree(valid).asJsonObject
        json.getAsJsonObject("outsideReadings").add("bad", com.google.gson.JsonParser.parseString(
            """{"date":"2026-11-03","timing":"manual","pageSeconds":{"1":-4},"dayByPage":{},"createdAt":1}"""))
        val restored = PlannerEngine.parseReadingPlan(json.toString(), gson)!!
        assertEquals(valid.assignments, restored.assignments)
        assertEquals(setOf("outside-1"), restored.outsideReadings.keys)
        assertEquals(240L, PlannerHistory.summarize(restored, emptyMap()).totalSeconds)
    }

    @Test fun `an older client updating title cannot erase outside log entries`() {
        val gson = Gson()
        val result = record(plan())
        val current = gson.fromJson(gson.toJson(result.plan), Map::class.java)
        val merged = mergePlannerRow(current, mapOf("id" to result.plan.id, "title" to "Changed elsewhere"))
        val restored = PlannerEngine.parseReadingPlan(gson.toJson(merged), gson)!!
        assertEquals("Changed elsewhere", restored.title)
        assertEquals(result.entry, restored.outsideReadings["outside-1"])
    }

    @Test fun `page count and last page start at the current active planner position`() {
        val active = plan().copy(lastReadPage = 4)
        assertEquals(4, PlannerOutsideReadings.currentPage(active))
        assertEquals(5, PlannerOutsideReadings.lastPage(active, 4, 2, true))
        assertEquals(5, PlannerOutsideReadings.lastPage(active, 4, 5, false))
        for (action in listOf<() -> Unit>(
            { PlannerOutsideReadings.lastPage(active, 4, 7, true) },
            { PlannerOutsideReadings.lastPage(active, 4, 3, false) },
            { PlannerOutsideReadings.lastPage(active, 4, 0, true) },
            { PlannerOutsideReadings.lastPage(active, 3, 5, false) },
            { record(active, 1, 2) }
        )) assertThrows(IllegalArgumentException::class.java) { action() }
    }

}
