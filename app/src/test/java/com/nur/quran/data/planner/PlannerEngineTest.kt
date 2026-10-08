package com.nur.quran.data.planner

import com.nur.quran.data.db.entities.ChapterEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Web parity with quran-app/src/utils/planner.test.js (groupContiguousPages,
 * rebalancePlanner) plus build/progress/overview/status coverage.
 */
class PlannerEngineTest {

    private fun item(id: Int, page: Int) = PlannerItem(
        id = id,
        title = "Page $page",
        subtitle = "1 page",
        route = "",
        rangeValue = page.toString(),
        pageStart = page,
        pageEnd = page
    )

    private fun assignment(
        day: Int,
        date: String,
        pages: IntRange,
        unitType: String = "page"
    ) = PlannerAssignment(
        dayNumber = day,
        date = date,
        unitType = unitType,
        title = "Pages ${pages.first}-${pages.last}",
        subtitle = "${pages.count()} pages",
        startUnit = pages.first,
        endUnit = pages.last,
        primaryRoute = "",
        pageStart = pages.first,
        pageEnd = pages.last,
        items = pages.map { item(it, it) }
    )

    private fun plan(
        assignments: List<PlannerAssignment>,
        readPages: Map<Int, List<Int>> = emptyMap(),
        completedAt: Map<Int, String> = emptyMap(),
        completedDays: List<Int> = emptyList(),
        startDate: String = "2026-01-01"
    ) = ReadingPlan(
        id = "test-plan",
        unitType = "page",
        title = "Test",
        durationDays = assignments.size,
        startDate = startDate,
        startUnit = 1,
        endUnit = 604,
        assignments = assignments,
        assignmentReadPages = readPages,
        assignmentCompletedAt = completedAt,
        completedDays = completedDays
    )

    // ── groupContiguousPages (mirrors planner.test.js) ──────────────

    @Test
    fun `groups single contiguous sequence`() {
        assertEquals(
            listOf(ContiguousPageGroup(1, 3)),
            PlannerEngine.groupContiguousPages(listOf(1, 2, 3))
        )
    }

    @Test
    fun `groups multiple sequences with gaps`() {
        assertEquals(
            listOf(ContiguousPageGroup(1, 2), ContiguousPageGroup(5, 6), ContiguousPageGroup(9, 9)),
            PlannerEngine.groupContiguousPages(listOf(1, 2, 5, 6, 9))
        )
    }

    @Test
    fun `handles out of order and duplicates`() {
        assertEquals(
            listOf(ContiguousPageGroup(1, 2), ContiguousPageGroup(5, 6)),
            PlannerEngine.groupContiguousPages(listOf(5, 1, 2, 5, 6, 2))
        )
    }

    @Test
    fun `empty input yields empty groups`() {
        assertTrue(PlannerEngine.groupContiguousPages(emptyList()).isEmpty())
    }

    // ── buildReadingPlanner ─────────────────────────────────────────

    @Test
    fun `build splits pages fairly across days`() {
        val p = PlannerEngine.buildReadingPlanner(
            unitType = "page", durationDays = 2, startDate = "2026-01-01",
            startUnit = 1, endUnit = 10, chapters = emptyList()
        )
        assertEquals(2, p.assignments.size)
        assertEquals(1, p.assignments[0].dayNumber)
        assertEquals("2026-01-01", p.assignments[0].date)
        assertEquals(5, p.assignments[0].items.size)
        assertEquals(5, p.assignments[1].items.size)
    }

    @Test
    fun `build skips excluded weekdays`() {
        // 2026-01-01 is a Thursday; exclude Thursdays (4) -> first reading day shifts.
        val p = PlannerEngine.buildReadingPlanner(
            unitType = "page", durationDays = 1, startDate = "2026-01-01",
            startUnit = 1, endUnit = 1, excludeDays = listOf(4), chapters = emptyList()
        )
        assertEquals("2026-01-02", p.assignments[0].date)
    }

    @Test
    fun `build rejects empty scope and bad unit`() {
        try {
            PlannerEngine.buildReadingPlanner(
                unitType = "surah", durationDays = 1, startDate = "2026-01-01",
                startUnit = 1, endUnit = 2, chapters = emptyList()
            )
            fail("expected empty scope error")
        } catch (_: IllegalArgumentException) {}
        try {
            PlannerEngine.buildReadingPlanner(
                unitType = "nope", durationDays = 1, startDate = "2026-01-01",
                startUnit = 1, endUnit = 2, chapters = emptyList()
            )
            fail("expected unsupported unit error")
        } catch (_: IllegalArgumentException) {}
    }

    @Test
    fun `build surah unit uses chapter bounds`() {
        val chapters = listOf(
            ChapterEntity(1, "Al-Fatihah", "الفاتحة", "Al-Fatihah", "", "makkah", 5, 7, 1, 1),
            ChapterEntity(2, "Al-Baqarah", "البقرة", "Al-Baqarah", "", "madinah", 87, 286, 2, 49)
        )
        val p = PlannerEngine.buildReadingPlanner(
            unitType = "surah", durationDays = 2, startDate = "2026-01-01",
            startUnit = 1, endUnit = 2, chapters = chapters
        )
        assertEquals(2, p.assignments.size)
        assertEquals(1, p.assignments[0].pageStart)
        assertEquals(49, p.assignments[1].pageEnd)
    }

    // ── progress / status / overview ────────────────────────────────

    @Test
    fun `progress counts items pages and next item`() {
        val p = plan(listOf(assignment(1, "2026-01-01", 1..4)))
        val prog = PlannerEngine.getAssignmentProgress(p, p.assignments[0])
        assertEquals(0, prog.completedCount)
        assertEquals(4, prog.totalPagesCount)
        assertEquals("1", prog.nextItem?.rangeValue)
        assertFalse(prog.isComplete)
    }

    @Test
    fun `explicit read pages complete items`() {
        val p = plan(
            listOf(assignment(1, "2026-01-01", 1..2)),
            readPages = mapOf(1 to listOf(1, 2))
        )
        val prog = PlannerEngine.getAssignmentProgress(p, p.assignments[0])
        assertTrue(prog.isComplete)
        assertEquals(2, prog.readPagesCount)
    }

    @Test
    fun `status covers all five states`() {
        val assignments = listOf(
            assignment(1, "2026-01-01", 1..2),
            assignment(2, "2026-01-02", 3..4),
            assignment(3, "2026-06-01", 5..6)
        )
        val p = plan(
            assignments,
            readPages = mapOf(1 to listOf(1, 2), 2 to listOf(3)),
            completedDays = listOf(1),
            startDate = "2026-01-01"
        )
        assertEquals("completed", PlannerEngine.getAssignmentStatus(p, assignments[0], "2026-06-10"))
        assertEquals("partial", PlannerEngine.getAssignmentStatus(p, assignments[1], "2026-06-10"))
        // date == today with no progress -> today
        assertEquals("today", PlannerEngine.getAssignmentStatus(p, assignments[2], "2026-06-01"))
        // past date, no progress -> overdue
        assertEquals("overdue", PlannerEngine.getAssignmentStatus(p, assignments[2], "2026-06-10"))
        // future date -> upcoming
        assertEquals("upcoming", PlannerEngine.getAssignmentStatus(p, assignments[2], "2026-01-01"))
    }

    @Test
    fun `overview counts overdue and current day`() {
        val p = plan(
            listOf(assignment(1, "2026-01-01", 1..2), assignment(2, "2026-01-02", 3..4)),
            startDate = "2026-01-01"
        )
        val o = PlannerEngine.getPlannerOverview(p, "2026-01-05")!!
        assertEquals(0, o.completedCount)
        assertEquals(2, o.overdueDays)
        assertEquals(2, o.currentDayNumber)
        assertEquals(2, o.remainingCount)
        assertEquals(1, o.firstIncomplete.dayNumber)
    }

    // ── rebalance (mirrors planner.test.js guard + spread) ──────────

    @Test
    fun `rebalance preserves read portions`() {
        val p = plan(
            listOf(assignment(1, "2026-01-01", 1..10)),
            readPages = mapOf(1 to listOf(1, 2, 3)),
            startDate = "2026-01-01"
        )
        val out = PlannerEngine.rebalancePlanner(p, "extend")
        val preserved = out.assignments.first()
        assertEquals("page", preserved.unitType)
        assertEquals(1, preserved.pageStart)
        assertEquals(3, preserved.pageEnd)
        // Unread pool 4..10 re-chunked after preserved days.
        assertTrue(out.assignments.size > 1)
        assertEquals(1, out.assignments.first().dayNumber)
        assertTrue(out.completedDays.contains(1))
    }

    @Test
    fun `custom pace shorter than completed days refuses`() {
        val p = plan(
            listOf(assignment(1, "2026-01-01", 1..4), assignment(2, "2026-01-02", 5..8)),
            readPages = mapOf(1 to listOf(1, 2, 3, 4)),
            completedDays = listOf(1),
            startDate = "2026-01-01"
        )
        val out = PlannerEngine.rebalancePlanner(p, "custom_pace", 1)
        assertEquals(p.assignments.size, out.assignments.size)
    }

    @Test
    fun `unknown strategy returns plan unchanged`() {
        val p = plan(listOf(assignment(1, "2026-01-01", 1..2)), startDate = "2026-01-01")
        assertEquals(p, PlannerEngine.rebalancePlanner(p, "nope"))
    }

    @Test
    fun `spread with more remaining days than unread pages does not crash`() {
        // Regression: subList(from > size) threw IllegalArgumentException
        // (app crash on Spread); JS slice() clamps to [] instead.
        val today = PlannerEngine.formatPlannerDate()
        val farFuture = PlannerEngine.addDays(today, 29)
        val done = assignment(1, today, 1..10).copy(date = today)
        val todo = assignment(2, farFuture, 11..33)
        val p = plan(
            listOf(done, todo),
            readPages = mapOf(1 to (1..10).toList()),
            completedDays = listOf(1),
            startDate = today
        )
        val out = PlannerEngine.rebalancePlanner(p, "spread")
        // 23 unread pages spread 1/day over the remaining window.
        assertTrue(out.assignments.size > 1)
        val totalUnread = out.assignments.drop(1).sumOf { it.pageEnd - it.pageStart + 1 }
        assertEquals(23, totalUnread)
    }

    // ── status web parity (planner.js:625-643) ───────────────────────

    @Test
    fun `status complete via progress even without completedDays entry`() {
        val p = plan(
            listOf(assignment(1, "2026-01-01", 1..2)),
            readPages = mapOf(1 to listOf(1, 2)),
            startDate = "2026-01-01"
        )
        assertEquals("completed", PlannerEngine.getAssignmentStatus(p, p.assignments[0], "2026-06-10"))
    }

    @Test
    fun `status today with progress is still today`() {
        val p = plan(
            listOf(assignment(1, "2026-06-01", 1..2)),
            readPages = mapOf(1 to listOf(1)),
            startDate = "2026-06-01"
        )
        // Web has no partial branch for today (planner.js:638-640).
        assertEquals("today", PlannerEngine.getAssignmentStatus(p, p.assignments[0], "2026-06-01"))
    }

    @Test
    fun `status overdue with item-derived progress is partial`() {
        // Completed via item tap: no explicit read pages, but progress
        // readPagesCount > 0 (planner.js:631-636).
        val base = plan(listOf(assignment(1, "2026-01-01", 1..2)), startDate = "2026-01-01")
        val p = base.copy(assignmentCompletedItems = mapOf(1 to listOf("1")))
        assertEquals("partial", PlannerEngine.getAssignmentStatus(p, p.assignments[0], "2026-06-10"))
    }

    // ── zero page-bound parity (planner.js:339-340,670-671,758-759) ──

    @Test
    fun `zero item bounds fall back to 1 not assignment start`() {
        val zeroItem = item(1, 0).copy(pageStart = 0, pageEnd = 0)
        val a = assignment(1, "2026-01-01", 1..1).copy(
            pageStart = 50, pageEnd = 50, items = listOf(zeroItem)
        )
        val p = plan(listOf(a), startDate = "2026-01-01")
        val (read, unread) = PlannerEngine.splitAssignmentReadPages(p, a)
        assertTrue(read.isEmpty())
        assertEquals(listOf(1), unread)
        val prog = PlannerEngine.getAssignmentProgress(p, a)
        assertEquals(1, prog.totalPagesCount)
        assertFalse(prog.isComplete)
    }

    // ── misc ────────────────────────────────────────────────────────

    @Test
    fun `difficulty thresholds mark heavy and light days`() {
        val a = listOf(
            assignment(1, "2026-01-01", 1..20),
            assignment(2, "2026-01-02", 21..34),
            assignment(3, "2026-01-03", 41..42)
        )
        val ind = PlannerEngine.getDifficultyIndicators(a)
        assertEquals("heavy", ind[1]?.level)
        assertEquals("moderate", ind[2]?.level)
        assertEquals("light", ind[3]?.level)
    }

    @Test
    fun `resume page finds first unread`() {
        val p = plan(
            listOf(assignment(1, "2026-01-01", 5..8)),
            readPages = mapOf(1 to listOf(5, 6))
        )
        assertEquals(7, PlannerEngine.getAssignmentResumePageNumber(p, p.assignments[0]))
        val fresh = plan(listOf(assignment(1, "2026-01-01", 5..8)))
        assertEquals(5, PlannerEngine.getAssignmentResumePageNumber(fresh, fresh.assignments[0]))
    }

    @Test
    fun `revision plan halves duration with ceil`() {
        val chapters = emptyList<ChapterEntity>()
        val p = PlannerEngine.buildReadingPlanner(
            unitType = "page", durationDays = 5, startDate = "2026-01-01",
            startUnit = 1, endUnit = 10, customTitle = "T", chapters = chapters
        )
        val rev = PlannerEngine.buildRevisionPlanner(p, chapters)
        assertEquals(3, rev.durationDays)
        assertTrue(rev.title.contains("(Revision)"))
    }

    @Test
    fun `success metrics count on-time and streak`() {
        val p = plan(
            listOf(assignment(1, "2026-01-01", 1..2), assignment(2, "2026-01-02", 3..4)),
            completedAt = mapOf(1 to "2026-01-01", 2 to "2026-06-01"),
            completedDays = listOf(1, 2),
            startDate = "2026-01-01"
        )
        // Seed completed items so progress counts them complete.
        val seeded = p.copy(
            assignmentCompletedItems = mapOf(1 to listOf("1", "2"), 2 to listOf("3", "4")),
            assignmentProgress = mapOf(1 to 2, 2 to 2)
        )
        val m = PlannerEngine.getPlannerSuccessMetrics(seeded, "2026-06-10")!!
        assertEquals(2, m.completedCount)
        assertEquals(1, m.onTimeCount)
        assertEquals(1, m.lateCount)
        assertEquals(50, m.successRate)
        assertEquals(1, m.consistencyStreak)
    }

    @Test
    fun `legacy cached plan missing newer keys parses with defaults and copy is safe`() {
        // Real-world shape: cached before assignmentReflections existed. Gson
        // leaves absent keys null (Unsafe alloc skips Kotlin defaults), which
        // crashed Planner open via rebalance copy() NPE on assignmentReflections.
        val gson = com.google.gson.Gson()
        val legacyJson = """
            {"id":"plan-1","unitType":"page","title":"routine","durationDays":2,
             "startDate":"2026-08-10","startUnit":1,"endUnit":2,
             "assignments":[
               {"dayNumber":1,"date":"2026-08-10","unitType":"page","title":"Day 1",
                "subtitle":"","startUnit":1,"endUnit":1,"primaryRoute":"",
                "pageStart":1,"pageEnd":1,
                "items":[{"id":1,"title":"Page 1","subtitle":"","route":"",
                          "rangeValue":"1","pageStart":1,"pageEnd":1}]},
               {"dayNumber":2,"date":"2026-08-11","unitType":"page","title":"Day 2",
                "subtitle":"","startUnit":2,"endUnit":2,"primaryRoute":"",
                "pageStart":2,"pageEnd":2,
                "items":[{"id":2,"title":"Page 2","subtitle":"","route":"",
                          "rangeValue":"2","pageStart":2,"pageEnd":2}]}]}
        """.trimIndent()
        val plan = PlannerEngine.parseReadingPlan(legacyJson, gson)
        assertNotNull(plan)
        assertEquals(emptyMap<Int, String>(), plan!!.assignmentReflections)
        assertEquals(emptyMap<Int, Int>(), plan.assignmentProgress)
        assertEquals(emptyList<Int>(), plan.completedDays)
        assertEquals(emptyList<Int>(), plan.excludeDays)
        // The exact crashing call: copy() without overriding the field.
        val copied = plan.copy(durationDays = plan.assignments.size)
        assertEquals(2, copied.durationDays)
        // Malformed input degrades gracefully.
        assertNull(PlannerEngine.parseReadingPlan("not json", gson))
        assertEquals(emptyList<ReadingPlan>(), PlannerEngine.parseReadingPlans("not json", gson))
    }
}
