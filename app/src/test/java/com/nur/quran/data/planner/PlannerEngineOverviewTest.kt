package com.nur.quran.data.planner

import org.junit.Assert.*
import org.junit.Test

class PlannerEngineOverviewTest {

    private fun pageItem(page: Int) = PlannerItem(
        id = page,
        title = "Page $page",
        subtitle = "Mushaf page view",
        route = "/page/$page",
        rangeValue = page.toString(),
        pageStart = page,
        pageEnd = page
    )

    private fun assignment(day: Int, date: String, pages: List<Int>) = PlannerAssignment(
        dayNumber = day,
        date = date,
        unitType = "page",
        title = "Day $day",
        subtitle = "",
        startUnit = 1,
        endUnit = 604,
        primaryRoute = "/page/1",
        pageStart = 1,
        pageEnd = 604,
        items = pages.map(::pageItem)
    )

    private fun plan(assignments: List<PlannerAssignment>, progress: Map<Int, Int> = emptyMap()) =
        ReadingPlan(
            id = "test",
            unitType = "page",
            title = "Test Plan",
            durationDays = assignments.size,
            startDate = "2026-01-01",
            startUnit = 1,
            endUnit = 604,
            assignmentProgress = progress,
            assignments = assignments
        )

    @Test
    fun `overview counts empty assignment as one page like web`() {
        val p = plan(
            listOf(
                assignment(1, "2026-01-01", listOf(1, 2)),
                assignment(2, "2026-01-02", emptyList())
            ),
            progress = mapOf(1 to 2)
        )
        val overview = PlannerEngine.getPlannerOverview(p, today = "2026-01-02")
        assertNotNull(overview)
        // Web: totalPages = 2 + (0 || 1) = 3, readPages = 2
        assertEquals(2f / 3f, overview!!.completionRatio, 0.001f)
    }

    @Test
    fun `overview ratio unaffected without empty assignments`() {
        val p = plan(
            listOf(
                assignment(1, "2026-01-01", listOf(1, 2)),
                assignment(2, "2026-01-02", listOf(3, 4))
            ),
            progress = mapOf(1 to 2)
        )
        val overview = PlannerEngine.getPlannerOverview(p, today = "2026-01-02")
        assertNotNull(overview)
        assertEquals(0.5f, overview!!.completionRatio, 0.001f)
    }
}
