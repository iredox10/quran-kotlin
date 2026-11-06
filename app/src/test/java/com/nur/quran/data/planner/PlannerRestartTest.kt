package com.nur.quran.data.planner

import org.junit.Assert.*
import org.junit.Test

class PlannerRestartTest {
    @Test fun restartPreservesScheduleAndOriginalHistoryButClearsProgress() {
        val original = PlannerEngine.buildReadingPlanner("page", 2, "2026-10-01", 1, 4, "Daily Quran", emptyList(), emptyList())
            .copy(assignmentProgress = mapOf(1 to 2, 2 to 2), completedDays = listOf(1, 2),
                lastReadPage = 4, assignmentReflections = mapOf(1 to "Reflection"))
        val fresh = PlannerEngine.restartPlanner(original, "2026-11-05")
        assertNotEquals(original.id, fresh.id)
        assertEquals(original.assignments.map { it.items }, fresh.assignments.map { it.items })
        assertEquals(listOf("2026-11-05", "2026-11-06"), fresh.assignments.map { it.date })
        assertTrue(fresh.assignmentProgress.isEmpty())
        assertTrue(fresh.assignmentReflections.isEmpty())
        assertTrue(fresh.completedDays.isEmpty())
        assertNull(fresh.lastReadPage)
        assertEquals(2, original.assignmentProgress[1])
    }
}
