package com.nur.quran.data.planner

import org.junit.Assert.*
import org.junit.Test

class PlannerRebalanceNumbersTest {
    private val today get() = PlannerEngine.formatPlannerDate()
    private fun plan(pages: Int, days: Int = 7) = PlannerEngine.buildReadingPlanner(
        unitType = "page", durationDays = days, startDate = PlannerEngine.addDays(today, -20),
        startUnit = 1, endUnit = pages, chapters = emptyList()
    )
    private fun pages(plan: ReadingPlan) = plan.assignments.flatMap { a -> a.items.flatMap { (it.pageStart..it.pageEnd).toList() } }

    @Test fun `custom pace creates requested day count rather than ceil chunk count`() {
        for ((total, days) in listOf(604 to 30, 23 to 15, 196 to 30)) {
            val adjusted = PlannerEngine.rebalancePlanner(plan(total), "custom_pace", days)
            assertEquals(days, adjusted.durationDays)
            assertEquals(days, adjusted.assignments.size)
            assertEquals((1..total).toList(), pages(adjusted))
            val sizes = adjusted.assignments.map { it.items.size }
            assertTrue(sizes.maxOrNull()!! - sizes.minOrNull()!! <= 1)
            val overview = PlannerEngine.getPlannerOverview(adjusted, today)!!
            assertEquals(1, overview.currentDayNumber)
            assertFalse(overview.isFinishedWindow)
            assertEquals(PlannerEngine.addDays(today, days - 1), PlannerEngine.getPlanEndDate(adjusted))
        }
    }

    @Test fun `partial reading remains accurate through repeated adjustments and selecting today`() {
        val original = plan(23, 1).let { p ->
            p.copy(assignments = p.assignments.map { it.copy(date = today) },
                assignmentReadPages = mapOf(1 to listOf(1, 2, 3)))
        }
        assertEquals(2, PlannerEngine.getMinimumDurationDays(original))
        val adjusted = PlannerEngine.rebalancePlanner(original, "custom_pace", 15)
        assertEquals(15, adjusted.durationDays)
        assertEquals(listOf(1), adjusted.completedDays)
        assertEquals(3f / 23, PlannerEngine.getPlannerOverview(adjusted)!!.completionRatio, .0001f)
        val current = PlannerEngine.getCurrentAssignment(adjusted)!!
        assertEquals(2, current.dayNumber)
        assertEquals(2, PlannerEngine.getAssignmentProgress(adjusted, current).totalPagesCount)
        assertEquals(0, PlannerEngine.getAssignmentProgress(adjusted, current).readPagesCount)
        for (strategy in listOf("extend", "spread", "custom_pace")) {
            val next = PlannerEngine.rebalancePlanner(adjusted, strategy, 8)
            assertEquals((1..23).toList(), pages(next))
            assertEquals(3f / 23, PlannerEngine.getPlannerOverview(next)!!.completionRatio, .0001f)
        }
    }

    @Test fun `spread fills remaining reading dates without dropping the end date to rounding`() {
        val p = plan(23, 1).let { original -> original.copy(
            assignments = original.assignments.map { it.copy(date = PlannerEngine.addDays(today, 14)) }
        ) }
        val spread = PlannerEngine.rebalancePlanner(p, "spread")
        assertEquals(15, spread.durationDays)
        assertEquals(PlannerEngine.addDays(today, 14), PlannerEngine.getPlanEndDate(spread))
        assertEquals((1..23).toList(), pages(spread))
    }

    @Test fun `excluded days and original historical start do not corrupt dashboard day or finish`() {
        val p = plan(23).copy(excludeDays = listOf(0, 6))
        val adjusted = PlannerEngine.rebalancePlanner(p, "custom_pace", 15)
        assertEquals(15, adjusted.durationDays)
        val first = adjusted.assignments.first()
        assertEquals(1, PlannerEngine.getPlannerOverview(adjusted, first.date)!!.currentDayNumber)
        assertEquals(adjusted.assignments.last().date, PlannerEngine.getPlanEndDate(adjusted))
        assertFalse(PlannerEngine.getPlannerOverview(adjusted, today)!!.isFinishedWindow)
    }

    @Test fun `more days than unread pages are capped without empty assignments`() {
        val adjusted = PlannerEngine.rebalancePlanner(plan(3, 1), "custom_pace", 100)
        assertEquals(3, adjusted.durationDays)
        assertTrue(adjusted.assignments.all { it.items.size == 1 })
    }
    @Test fun `old day read pages outside revised assignment cannot inflate progress`() {
        val p = plan(2, 1).copy(assignmentReadPages = mapOf(1 to listOf(1, 2, 300)))
        val progress = PlannerEngine.getAssignmentProgress(p, p.assignments.first())
        assertEquals(2, progress.readPagesCount)
        assertEquals(1f, PlannerEngine.getPlannerOverview(p)!!.completionRatio, .0001f)
    }

}
