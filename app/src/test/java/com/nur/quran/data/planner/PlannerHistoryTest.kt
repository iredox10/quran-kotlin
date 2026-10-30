package com.nur.quran.data.planner

import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class PlannerHistoryTest {
    private fun plan() = PlannerEngine.buildReadingPlanner("page", 2, "2026-10-01", 1, 4, chapters = emptyList())
    private fun completed() = plan().copy(assignmentProgress = mapOf(1 to 2, 2 to 2), assignmentCompletedAt = mapOf(1 to "2026-10-01", 2 to "2026-10-03"))

    @Test fun `completed live plans and archived incomplete plans appear once with archived version winning`() {
        val complete = completed()
        val archived = complete.copy(archivedAt = "2026-10-04T00:00:00Z")
        val ended = plan().copy(id = "ended", archivedAt = "2026-10-02T00:00:00Z")
        val entries = PlannerHistory.plans(listOf(complete, ended), listOf(archived, ended))
        assertEquals(2, entries.size)
        assertEquals(archived, entries.find { it.id == complete.id })
        assertFalse(PlannerHistory.isComplete(ended))
    }

    @Test fun `recorded time totals includes old and orphan day entries without inventing dates`() {
        val summary = PlannerHistory.summarize(completed(), mapOf(
            1 to PlannerSessionTimer(150, secondsByDate = mapOf("2026-10-01" to 90)),
            2 to PlannerSessionTimer(60, secondsByDate = mapOf("2026-10-01" to 30, "2026-10-03" to 30)),
            9 to PlannerSessionTimer(20)
        ))
        assertEquals(230L, summary.totalSeconds)
        assertEquals(230L, summary.readingDays.last().cumulativeSeconds)
        assertEquals(2, summary.timedDays)
        assertEquals(75L, summary.averageSeconds)
        assertEquals(2, summary.readingDays.count { it.date == null })
        assertEquals(4, summary.pagesRead)
        assertEquals("2026-10-03", summary.completedOn)
    }

    @Test fun `calendar reading log survives schedule adjustment`() {
        val timers = mapOf(1 to PlannerSessionTimer(120, secondsByDate = mapOf("2026-10-01" to 120)))
        val original = plan().copy(assignmentReadPages = mapOf(1 to listOf(1)))
        val adjusted = PlannerEngine.rebalancePlanner(original, "custom_pace", 3, "2026-10-05")
        assertEquals(PlannerHistory.summarize(original, timers).readingDays, PlannerHistory.summarize(adjusted, timers).readingDays)
    }

    @Test fun `new time splits across local midnight and preserves existing totals exactly`() {
        val end = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 31, 0, 0, 30); set(Calendar.MILLISECOND, 0) }.timeInMillis
        val updated = PlannerHistory.addReadingTime(PlannerSessionTimer(10), 60, end)
        assertEquals(70L, updated.totalSeconds)
        assertEquals(mapOf("2026-10-30" to 30L, "2026-10-31" to 30L), updated.secondsByDate)
        assertEquals(70L, PlannerHistory.summarize(plan(), mapOf(1 to updated)).totalSeconds)
    }

    @Test fun `history timer wire roundtrip and legacy formats both remain readable`() {
        val timer = PlannerSessionTimer(60, secondsByDate = mapOf("2026-10-30" to 60))
        val encoded = PlannerWire.sessionTimersToJson(mapOf("p" to mapOf(1 to timer)))
        assertEquals(timer, PlannerWire.sessionTimersFromJson(encoded)["p"]!![1])
        assertEquals(PlannerSessionTimer(20), PlannerWire.sessionTimersFromJson("""{"p":{"1":20}}""")["p"]!![1])
    }

    @Test fun `no timer is no recorded time and completion derives from real progress`() {
        val summary = PlannerHistory.summarize(completed(), emptyMap())
        assertTrue(summary.completed)
        assertEquals(0L, summary.totalSeconds)
        assertTrue(summary.readingDays.isEmpty())
        assertFalse(PlannerHistory.isComplete(plan().copy(completedDays = listOf(1, 2))))
    }
}
