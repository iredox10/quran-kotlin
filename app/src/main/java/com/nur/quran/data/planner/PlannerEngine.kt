package com.nur.quran.data.planner

import com.nur.quran.data.HIZB_STARTS
import com.nur.quran.data.JUZ_STARTS
import com.nur.quran.data.PAGE_GROUPS
import com.nur.quran.data.db.entities.ChapterEntity
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

data class PlannerUnit(val label: String, val plural: String, val max: Int)

val PLANNER_UNITS = mapOf(
    "page" to PlannerUnit("Page", "Pages", 604),
    "juz" to PlannerUnit("Juz", "Ajza", 30),
    "hizb" to PlannerUnit("Hizb", "Ahzab", 60),
    "surah" to PlannerUnit("Surah", "Surahs", 114)
)

data class PlanTemplate(
    val id: String,
    val title: String,
    val durationDays: Int,
    val unitType: String,
    val startUnit: Int,
    val endUnit: Int,
    val description: String
)

val PLAN_TEMPLATES = listOf(
    PlanTemplate("ramadan-last-10", "Ramadan Last 10", 10, "page", 542, 604, "Complete the last 3 Ajza in the last 10 days"),
    PlanTemplate("juz-amma", "Juz Amma Focus", 15, "page", 582, 604, "Take 15 days to master the 30th Juz"),
    PlanTemplate("al-kahf", "Surah Al-Kahf Weekly", 1, "surah", 18, 18, "The recommended Friday reading"),
    PlanTemplate("tafsir-deep-dive", "Tafsir Deep Dive", 114, "surah", 1, 114, "One Surah per week (requires manually setting days off)"),
    PlanTemplate("monthly-juz", "Monthly Juz", 30, "juz", 1, 30, "One Juz per month (set custom duration when creating)"),
    PlanTemplate("quick-revision", "Quick Revision", 10, "juz", 1, 30, "Full Quran in 10 days for intense revision")
)

data class PlannerItem(
    val id: Int,
    val title: String,
    val subtitle: String,
    val route: String,
    val rangeValue: String, // String to handle single numbers or ranges like "1-5"
    val pageStart: Int,
    val pageEnd: Int
)

data class PlannerAssignment(
    val dayNumber: Int,
    var date: String, // YYYY-MM-DD
    val unitType: String,
    val title: String,
    val subtitle: String,
    val startUnit: Int,
    val endUnit: Int,
    var primaryRoute: String,
    val pageStart: Int,
    val pageEnd: Int,
    var items: List<PlannerItem>
)

data class ReadingPlan(
    val id: String,
    val createdAt: String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date()),
    val unitType: String,
    val title: String,
    val durationDays: Int,
    val startDate: String, // YYYY-MM-DD
    val startUnit: Int,
    val endUnit: Int,
    val isCustomRange: Boolean = false,
    val assignmentProgress: Map<Int, Int> = emptyMap(), // dayNumber -> completed item count
    val assignmentReadPages: Map<Int, List<Int>> = emptyMap(), // dayNumber -> read page list
    val assignmentCompletedItems: Map<Int, List<String>> = emptyMap(), // dayNumber -> rangeValue list
    val assignmentCompletedAt: Map<Int, String> = emptyMap(), // dayNumber -> date
    val completedDays: List<Int> = emptyList(),
    val assignments: List<PlannerAssignment>,
    val excludeDays: List<Int> = emptyList(), // Day of week to skip: 0=Sun, 1=Mon, ..., 6=Sat
    val assignmentReflections: Map<Int, String> = emptyMap(), // dayNumber -> reflection text
    val lastReadPage: Int? = null,
    val lastReadVerseKey: String? = null
)

/** Web: plannerBookmarks[planId] entry — a verse highlighted from the reader. */
data class PlannerBookmark(
    val verseKey: String,
    val surahName: String = "",
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

/** Web: groupContiguousPages entry — one contiguous page range { pStart, pEnd }. */
data class ContiguousPageGroup(
    val pStart: Int,
    val pEnd: Int
)

data class AssignmentProgress(
    val completedCount: Int,
    val totalCount: Int,
    val readPagesCount: Int,
    val totalPagesCount: Int,
    val remainingCount: Int,
    val completionRatio: Float,
    val isComplete: Boolean,
    val completedAt: String?,
    val completedRangeValues: List<String>,
    val nextItem: PlannerItem?
)

data class PlannerOverview(
    val completedCount: Int,
    val remainingCount: Int,
    val currentDayNumber: Int,
    val isUpcoming: Boolean,
    val isFinishedWindow: Boolean,
    val completionRatio: Float,
    val firstIncomplete: PlannerAssignment
)

data class PlannerSuccessMetrics(
    val completedCount: Int,
    val onTimeCount: Int,
    val lateCount: Int,
    val dueCount: Int,
    val dueCompletedCount: Int,
    val successRate: Int,
    val consistencyStreak: Int
)

object PlannerEngine {

    fun formatPlannerDate(date: Date = Date()): String {
        return SimpleDateFormat("yyyy-MM-DD", Locale.US).apply {
            // Note: Use upper 'yyyy-MM-dd' correctly
        }.let {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date)
        }
    }

    fun parsePlannerDate(dateStr: String): Date {
        return try {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateStr) ?: Date()
        } catch (e: Exception) {
            Date()
        }
    }

    fun addDays(dateStr: String, days: Int): String {
        val cal = Calendar.getInstance().apply {
            time = parsePlannerDate(dateStr)
            add(Calendar.DAY_OF_YEAR, days)
        }
        return formatPlannerDate(cal.time)
    }

    fun diffDays(startDateStr: String, endDateStr: String): Int {
        val start = parsePlannerDate(startDateStr).time
        val end = parsePlannerDate(endDateStr).time
        return floor((end - start).toDouble() / 86400000.0).toInt()
    }

    /**
     * Web: groupContiguousPages (planner.js:306-324, tested) — dedupe, sort,
     * and fold a page list into contiguous { pStart, pEnd } groups.
     * Pure function, unit-testable.
     */
    fun groupContiguousPages(pages: List<Int>): List<ContiguousPageGroup> {
        if (pages.isEmpty()) return emptyList()
        val sorted = pages.toSet().sorted()
        val groups = mutableListOf<ContiguousPageGroup>()
        var pStart = sorted[0]
        var pEnd = sorted[0]
        for (i in 1 until sorted.size) {
            if (sorted[i] == pEnd + 1) {
                pEnd = sorted[i]
            } else {
                groups.add(ContiguousPageGroup(pStart, pEnd))
                pStart = sorted[i]
                pEnd = sorted[i]
            }
        }
        groups.add(ContiguousPageGroup(pStart, pEnd))
        return groups
    }

    fun getReadingDate(startDateStr: String, index: Int, excludeDays: List<Int> = emptyList()): String {
        val cal = Calendar.getInstance().apply {
            time = parsePlannerDate(startDateStr)
        }

        // Adjust day of week format: Calendar.SUNDAY=1 -> 0, MONDAY=2 -> 1, ..., SATURDAY=7 -> 6
        fun getDayIdx(c: Calendar): Int = c.get(Calendar.DAY_OF_WEEK) - 1

        while (excludeDays.contains(getDayIdx(cal))) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        var daysFound = 0
        while (daysFound < index) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
            if (!excludeDays.contains(getDayIdx(cal))) {
                daysFound++
            }
        }

        return formatPlannerDate(cal.time)
    }

    fun buildUnitSource(unitType: String, chapters: List<ChapterEntity>): List<PlannerItem> {
        return when (unitType) {
            "page" -> PAGE_GROUPS.map { item ->
                PlannerItem(
                    id = item.id,
                    title = "Page ${item.pageNumber}",
                    subtitle = "Mushaf page view",
                    route = "/page/${item.pageNumber}",
                    rangeValue = item.pageNumber.toString(),
                    pageStart = item.pageNumber,
                    pageEnd = item.pageNumber
                )
            }
            "juz" -> JUZ_STARTS.mapIndexed { index, item ->
                val nextItem = JUZ_STARTS.getOrNull(index + 1)
                val pageStart = item.pageNumber
                val pageEnd = if (nextItem != null) nextItem.pageNumber - 1 else 604
                PlannerItem(
                    id = item.id,
                    title = "Juz ${item.id}",
                    subtitle = "Starts at ${item.verseKey}",
                    route = "/page/${item.pageNumber}",
                    rangeValue = item.id.toString(),
                    pageStart = pageStart,
                    pageEnd = pageEnd
                )
            }
            "hizb" -> HIZB_STARTS.mapIndexed { index, item ->
                val nextItem = HIZB_STARTS.getOrNull(index + 1)
                val pageStart = item.pageNumber
                val pageEnd = if (nextItem != null) nextItem.pageNumber - 1 else 604
                PlannerItem(
                    id = item.id,
                    title = "Hizb ${item.id}",
                    subtitle = "Starts at ${item.verseKey}",
                    route = "/page/${item.pageNumber}",
                    rangeValue = item.id.toString(),
                    pageStart = pageStart,
                    pageEnd = pageEnd
                )
            }
            else -> chapters.map { chapter ->
                val pStart = chapter.pagesStart
                val pEnd = chapter.pagesEnd
                PlannerItem(
                    id = chapter.id,
                    title = chapter.nameSimple,
                    subtitle = chapter.translatedName,
                    route = "/surah/${chapter.id}",
                    rangeValue = chapter.id.toString(),
                    pageStart = pStart,
                    pageEnd = pEnd
                )
            }
        }
    }

    private fun splitIntoChunks(items: List<PlannerItem>, chunkCount: Int): List<List<PlannerItem>> {
        val chunks = mutableListOf<List<PlannerItem>>()
        var cursor = 0
        val baseSize = items.size / chunkCount
        val remainder = items.size % chunkCount

        for (index in 0 until chunkCount) {
            val chunkSize = baseSize + (if (index < remainder) 1 else 0)
            if (chunkSize <= 0) break
            chunks.add(items.subList(cursor, cursor + chunkSize))
            cursor += chunkSize
        }
        return chunks
    }

    private fun buildAssignmentTitle(unitType: String, items: List<PlannerItem>): String {
        val first = items.first()
        val last = items.last()

        if (unitType == "surah") {
            if (items.size == 1) return first.title
            return "Surah ${first.id}-${last.id}"
        }

        val unit = PLANNER_UNITS[unitType] ?: PlannerUnit("Page", "Pages", 604)
        if (first.rangeValue == last.rangeValue) {
            return "${unit.label} ${first.rangeValue}"
        }
        return "${unit.plural} ${first.rangeValue}-${last.rangeValue}"
    }

    private fun buildAssignmentSubtitle(unitType: String, items: List<PlannerItem>): String {
        val first = items.first()
        val last = items.last()

        if (unitType == "surah") {
            if (items.size == 1) return "${first.subtitle} · ${first.title}"
            return "${first.title} to ${last.title}"
        }

        if (items.size == 1) return first.subtitle
        return "${first.subtitle} · ${last.subtitle}"
    }

    private fun buildPlanTitle(unitType: String, items: List<PlannerItem>, customTitle: String?): String {
        if (!customTitle.isNullOrBlank()) return customTitle.trim()

        val first = items.first()
        val last = items.last()
        val unit = PLANNER_UNITS[unitType] ?: PlannerUnit("Page", "Pages", 604)

        if (first.rangeValue == last.rangeValue) {
            return "${unit.label} ${first.rangeValue} Plan"
        }
        return "${unit.plural} ${first.rangeValue}-${last.rangeValue} Plan"
    }

    fun buildReadingPlanner(
        unitType: String,
        durationDays: Int,
        startDate: String,
        startUnit: Int,
        endUnit: Int,
        customTitle: String? = null,
        excludeDays: List<Int> = emptyList(),
        chapters: List<ChapterEntity>
    ): ReadingPlan {
        val sourceItems = buildUnitSource(unitType, chapters)
        val unitMeta = PLANNER_UNITS[unitType] ?: throw IllegalArgumentException("Unsupported unit")

        val normStart = max(1, min(startUnit, unitMeta.max))
        val normEnd = max(normStart, min(endUnit, unitMeta.max))
        val scopedItems = sourceItems.filter {
            val v = it.rangeValue.toIntOrNull() ?: 1
            v in normStart..normEnd
        }

        if (scopedItems.isEmpty()) throw IllegalArgumentException("Planner scope is empty")

        val safeDuration = max(1, min(durationDays, scopedItems.size))
        val chunks = splitIntoChunks(scopedItems, safeDuration)
        val planId = "plan-${System.currentTimeMillis()}"

        val assignments = chunks.mapIndexed { index, items ->
            val pStart = items.map { it.pageStart }.minOrNull() ?: 1
            val pEnd = items.map { it.pageEnd }.maxOrNull() ?: 1
            PlannerAssignment(
                dayNumber = index + 1,
                date = getReadingDate(startDate, index, excludeDays),
                unitType = unitType,
                title = buildAssignmentTitle(unitType, items),
                subtitle = buildAssignmentSubtitle(unitType, items),
                startUnit = items.first().rangeValue.toIntOrNull() ?: 1,
                endUnit = items.last().rangeValue.toIntOrNull() ?: 1,
                primaryRoute = items.first().route,
                pageStart = pStart,
                pageEnd = pEnd,
                items = items
            )
        }

        return ReadingPlan(
            id = planId,
            unitType = unitType,
            title = buildPlanTitle(unitType, scopedItems, customTitle),
            durationDays = assignments.size,
            startDate = startDate,
            startUnit = normStart,
            endUnit = normEnd,
            isCustomRange = normStart != 1 || normEnd != unitMeta.max || !customTitle.isNullOrBlank(),
            assignments = assignments,
            excludeDays = excludeDays
        )
    }

    fun getAssignmentProgress(plan: ReadingPlan, assignment: PlannerAssignment): AssignmentProgress {
        val totalCount = assignment.items.size
        val explicitCompleted = plan.assignmentCompletedItems[assignment.dayNumber]
        val rawCompletedCount = plan.assignmentProgress[assignment.dayNumber] ?: 0

        val initialCompletedRangeValues = if (explicitCompleted != null) {
            assignment.items.filter { explicitCompleted.contains(it.rangeValue) }.map { it.rangeValue }
        } else {
            assignment.items.take(max(0, min(rawCompletedCount, totalCount))).map { it.rangeValue }
        }

        val completedRangeValuesSet = initialCompletedRangeValues.toMutableSet()
        val explicitReadPages = plan.assignmentReadPages[assignment.dayNumber] ?: emptyList()

        var totalPagesCount = 0
        val allReadPages = explicitReadPages.toMutableSet()

        assignment.items.forEach { item ->
            val pStart = item.pageStart
            val pEnd = item.pageEnd
            totalPagesCount += (pEnd - pStart + 1)

            var allItemPagesRead = true
            for (p in pStart..pEnd) {
                if (!explicitReadPages.contains(p)) {
                    allItemPagesRead = false
                    break
                }
            }

            if (allItemPagesRead) {
                completedRangeValuesSet.add(item.rangeValue)
            }

            if (completedRangeValuesSet.contains(item.rangeValue)) {
                for (p in pStart..pEnd) {
                    allReadPages.add(p)
                }
            }
        }

        val completedRangeValues = completedRangeValuesSet.toList()
        val completedCount = completedRangeValues.size
        val nextItem = assignment.items.find { !completedRangeValues.contains(it.rangeValue) } ?: assignment.items.lastOrNull()

        return AssignmentProgress(
            completedCount = completedCount,
            totalCount = totalCount,
            readPagesCount = allReadPages.size,
            totalPagesCount = totalPagesCount,
            remainingCount = max(totalCount - completedCount, 0),
            completionRatio = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f,
            isComplete = totalCount > 0 && completedCount >= totalCount,
            completedAt = plan.assignmentCompletedAt[assignment.dayNumber],
            completedRangeValues = completedRangeValues,
            nextItem = nextItem
        )
    }

    fun getPlannerOverview(plan: ReadingPlan?, today: String = formatPlannerDate()): PlannerOverview? {
        if (plan == null) return null

        val elapsedDays = diffDays(plan.startDate, today)
        val currentDayNumber = min(max(elapsedDays + 1, 1), plan.durationDays)
        val completedCount = plan.assignments.count { getAssignmentProgress(plan, it).isComplete }
        val remainingCount = max(plan.durationDays - completedCount, 0)
        val firstIncomplete = plan.assignments.find { !getAssignmentProgress(plan, it).isComplete } ?: plan.assignments.last()

        var totalPages = 0
        var readPages = 0
        plan.assignments.forEach { a ->
            val prog = getAssignmentProgress(plan, a)
            totalPages += prog.totalPagesCount
            readPages += prog.readPagesCount
        }

        return PlannerOverview(
            completedCount = completedCount,
            remainingCount = remainingCount,
            currentDayNumber = currentDayNumber,
            isUpcoming = elapsedDays < 0,
            isFinishedWindow = elapsedDays >= plan.durationDays,
            completionRatio = if (totalPages > 0) readPages.toFloat() / totalPages else 0f,
            firstIncomplete = firstIncomplete
        )
    }

    fun getPlannerSuccessMetrics(plan: ReadingPlan?, today: String = formatPlannerDate()): PlannerSuccessMetrics? {
        if (plan == null) return null

        val completedAssignments = plan.assignments.filter { getAssignmentProgress(plan, it).isComplete }
        val onTimeCount = completedAssignments.count { assignment ->
            val completedAt = plan.assignmentCompletedAt[assignment.dayNumber]
            completedAt != null && completedAt <= assignment.date
        }
        val lateCount = max(completedAssignments.size - onTimeCount, 0)
        val dueAssignments = plan.assignments.filter { it.date <= today }
        val dueCompletedCount = dueAssignments.count { getAssignmentProgress(plan, it).isComplete }
        val successRate = if (dueAssignments.isNotEmpty()) Math.round((onTimeCount.toFloat() / dueAssignments.size) * 100) else 0

        var consistencyStreak = 0
        for (assignment in dueAssignments) {
            val completedAt = plan.assignmentCompletedAt[assignment.dayNumber]
            if (completedAt != null && completedAt <= assignment.date) {
                consistencyStreak++
                continue
            }
            break
        }

        return PlannerSuccessMetrics(
            completedCount = completedAssignments.size,
            onTimeCount = onTimeCount,
            lateCount = lateCount,
            dueCount = dueAssignments.size,
            dueCompletedCount = dueCompletedCount,
            successRate = successRate,
            consistencyStreak = consistencyStreak
        )
    }

    fun rebalancePlanner(
        planner: ReadingPlan,
        strategy: String, // "spread", "extend", "custom_pace"
        customDurationDays: Int? = null
    ): ReadingPlan {
        val today = formatPlannerDate()
        val excludeDays = planner.excludeDays

        val preservedAssignments = mutableListOf<PlannerAssignment>()
        val unreadPagesPool = mutableListOf<Int>()

        planner.assignments.forEach { a ->
            val prog = getAssignmentProgress(planner, a)
            if (prog.isComplete) {
                preservedAssignments.add(a)
            } else {
                val readPages = planner.assignmentReadPages[a.dayNumber] ?: emptyList()
                for (p in a.pageStart..a.pageEnd) {
                    if (!readPages.contains(p)) {
                        unreadPagesPool.add(p)
                    }
                }
            }
        }

        if (unreadPagesPool.isEmpty()) return planner

        val remainingDays = when (strategy) {
            "custom_pace" -> max(1, (customDurationDays ?: 30) - preservedAssignments.size)
            "extend" -> max(1, ceil(unreadPagesPool.size.toDouble() / 10.0).toInt())
            else -> { // "spread"
                val elapsed = diffDays(planner.startDate, today)
                max(1, planner.durationDays - elapsed)
            }
        }

        val pagesPerDay = max(1, ceil(unreadPagesPool.size.toDouble() / remainingDays).toInt())
        val newAssignments = mutableListOf<PlannerAssignment>()

        var cursor = 0
        var nextDayNum = preservedAssignments.size + 1
        var dayIndex = 0

        while (cursor < unreadPagesPool.size) {
            val chunk = unreadPagesPool.subList(cursor, min(cursor + pagesPerDay, unreadPagesPool.size))
            val pStart = chunk.first()
            val pEnd = chunk.last()
            val dateStr = getReadingDate(today, dayIndex, excludeDays)

            newAssignments.add(
                PlannerAssignment(
                    dayNumber = nextDayNum,
                    date = dateStr,
                    unitType = "page",
                    title = "Pages $pStart-$pEnd",
                    subtitle = "${chunk.size} pages",
                    startUnit = pStart,
                    endUnit = pEnd,
                    primaryRoute = "/planner/read/$nextDayNum",
                    pageStart = pStart,
                    pageEnd = pEnd,
                    items = listOf(
                        PlannerItem(
                            id = pStart,
                            title = "Pages $pStart-$pEnd",
                            subtitle = "${chunk.size} pages",
                            route = "/planner/read/$nextDayNum",
                            rangeValue = "$pStart-$pEnd",
                            pageStart = pStart,
                            pageEnd = pEnd
                        )
                    )
                )
            )

            cursor += pagesPerDay
            nextDayNum++
            dayIndex++
        }

        val allAssignments = preservedAssignments + newAssignments
        return planner.copy(
            durationDays = allAssignments.size,
            assignments = allAssignments
        )
    }

    // ── Assignment Status Helper ────────────────────────────────────────
    fun getAssignmentStatus(plan: ReadingPlan, assignment: PlannerAssignment, today: String = formatPlannerDate()): String {
        val isComplete = plan.completedDays.contains(assignment.dayNumber)
        if (isComplete) return "completed"
        if (assignment.date == today) {
            val readPages = plan.assignmentReadPages[assignment.dayNumber]?.size ?: 0
            return if (readPages > 0) "partial" else "today"
        }
        if (assignment.date < today) {
            val readPages = plan.assignmentReadPages[assignment.dayNumber]?.size ?: 0
            return if (readPages > 0) "partial" else "overdue"
        }
        return "upcoming"
    }

    // ── Resume Page Number ──────────────────────────────────────────────
    fun getAssignmentResumePageNumber(plan: ReadingPlan, assignment: PlannerAssignment): Int {
        val readPages = plan.assignmentReadPages[assignment.dayNumber] ?: emptyList()
        if (readPages.isEmpty()) return assignment.pageStart
        for (page in assignment.pageStart..assignment.pageEnd) {
            if (!readPages.contains(page)) return page
        }
        return assignment.pageEnd
    }

    // ── Weekly Summary ──────────────────────────────────────────────────
    fun getWeeklySummary(plan: ReadingPlan): List<WeeklySummary> {
        val weeks = mutableListOf<WeeklySummary>()
        val assignments = plan.assignments
        val chunkSize = 7
        var weekNum = 1

        for (i in assignments.indices step chunkSize) {
            val weekAssignments = assignments.subList(i, min(i + chunkSize, assignments.size))
            val completedCount = weekAssignments.count { plan.completedDays.contains(it.dayNumber) }
            val totalCount = weekAssignments.size
            weeks.add(WeeklySummary(
                weekNumber = weekNum,
                completedCount = completedCount,
                totalCount = totalCount,
                completionRatio = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f
            ))
            weekNum++
        }
        return weeks
    }

    // ── Planner Analytics ───────────────────────────────────────────────
    fun getPlannerAnalytics(plan: ReadingPlan, today: String = formatPlannerDate()): PlannerAnalytics {
        val metrics = getPlannerSuccessMetrics(plan, today)
        val overview = getPlannerOverview(plan, today)

        val totalReadPages = plan.assignmentReadPages.values.sumOf { it.size }
        val elapsed = max(1, diffDays(plan.startDate, today))
        val avgPagesPerDay = if (elapsed > 0) totalReadPages.toFloat() / elapsed else 0f

        val catchUpDays = plan.assignments.count { a ->
            val status = getAssignmentStatus(plan, a, today)
            status == "overdue" || status == "partial"
        }

        return PlannerAnalytics(
            onTimeRate = metrics?.successRate ?: 0,
            catchUpDays = catchUpDays,
            avgPagesPerDay = avgPagesPerDay,
            totalReadPages = totalReadPages,
            elapsedDays = elapsed
        )
    }

    // ── Difficulty Indicators ───────────────────────────────────────────
    fun getDifficultyIndicators(assignments: List<PlannerAssignment>): Map<Int, String> {
        if (assignments.isEmpty()) return emptyMap()
        val avgPages = assignments.sumOf { it.pageEnd - it.pageStart + 1 }.toFloat() / assignments.size
        val result = mutableMapOf<Int, String>()
        assignments.forEach { a ->
            val pages = a.pageEnd - a.pageStart + 1
            result[a.dayNumber] = when {
                pages > avgPages * 1.3f -> "heavy"
                pages < avgPages * 0.7f -> "light"
                else -> "moderate"
            }
        }
        return result
    }

    // ── Build Revision Planner ──────────────────────────────────────────
    fun buildRevisionPlanner(
        completedPlan: ReadingPlan,
        chapters: List<ChapterEntity>
    ): ReadingPlan {
        val halfDuration = max(1, completedPlan.durationDays / 2)
        return buildReadingPlanner(
            unitType = completedPlan.unitType,
            durationDays = halfDuration,
            startDate = formatPlannerDate(),
            startUnit = completedPlan.startUnit,
            endUnit = completedPlan.endUnit,
            customTitle = "${completedPlan.title} (Revision)",
            excludeDays = completedPlan.excludeDays,
            chapters = chapters
        )
    }

    // ── Format Date Label ───────────────────────────────────────────────
    fun formatPlannerDateLabel(dateStr: String): String {
        return try {
            val date = parsePlannerDate(dateStr)
            SimpleDateFormat("EEE, MMM d", Locale.US).format(date)
        } catch (e: Exception) {
            dateStr
        }
    }
}

// ── Additional Data Classes ─────────────────────────────────────────────
data class WeeklySummary(
    val weekNumber: Int,
    val completedCount: Int,
    val totalCount: Int,
    val completionRatio: Float
)

data class PlannerAnalytics(
    val onTimeRate: Int,
    val catchUpDays: Int,
    val avgPagesPerDay: Float,
    val totalReadPages: Int,
    val elapsedDays: Int
)

// ── Prayer Integration ──────────────────────────────────────────────────
val PRAYER_NAMES = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")

data class PrayerTimings(
    val date: String,
    val timings: Map<String, String> // e.g. "Fajr" -> "05:30"
)

data class PrayerSlot(
    val name: String,
    val time: String?,        // Formatted label e.g. "After 5:30 AM"
    val count: Int,           // Total items in this slot
    val doneInSlot: Int,      // Items completed in this slot
    val slotStart: Int,       // Start index into items list
    val slotEnd: Int,         // End index into items list
    val status: String        // "completed", "current", "upcoming", "empty", "locked"
)

fun buildPrayerSlots(
    plan: ReadingPlan,
    todayAssignment: PlannerAssignment?,
    prayerTimings: PrayerTimings?,
    readPreference: String = "after",         // "before", "after", "split"
    activePrayers: List<String> = PRAYER_NAMES
): List<PrayerSlot> {
    val sortedPrayers = activePrayers.sortedBy { PRAYER_NAMES.indexOf(it) }

    if (todayAssignment == null) {
        return sortedPrayers.map { name ->
            PrayerSlot(name = name, time = null, count = 0, doneInSlot = 0, slotStart = 0, slotEnd = 0, status = "upcoming")
        }
    }

    val items = todayAssignment.items
    val total = items.size
    val numSlots = sortedPrayers.size.coerceAtLeast(1)
    val completedRangeValues = plan.assignmentCompletedItems[todayAssignment.dayNumber] ?: emptyList()

    return sortedPrayers.mapIndexed { i, name ->
        val slotStart = ceil((i.toDouble() / numSlots) * total).toInt()
        val slotEnd = ceil(((i + 1).toDouble() / numSlots) * total).toInt()
        val slotItems = items.subList(slotStart.coerceAtMost(total), slotEnd.coerceAtMost(total))
        val count = slotItems.size.coerceAtLeast(0)
        val doneInSlot = slotItems.count { completedRangeValues.contains(it.rangeValue) }

        val isComplete = count > 0 && doneInSlot >= count
        val isCurrent = count > 0 && !isComplete && doneInSlot > 0
        // Web parity: slots unlock sequentially — a slot with pending items is
        // locked until every earlier slot is complete.
        val earlierIncomplete = (0 until i).any { prevIdx ->
            val pStart = ceil((prevIdx.toDouble() / numSlots) * total).toInt()
            val pEnd = ceil(((prevIdx + 1).toDouble() / numSlots) * total).toInt()
            val pItems = items.subList(pStart.coerceAtMost(total), pEnd.coerceAtMost(total))
            pItems.isNotEmpty() && pItems.any { !completedRangeValues.contains(it.rangeValue) }
        }
        // Web order: empty -> completed -> locked -> current -> upcoming.
        val status = when {
            count == 0 -> "empty"
            isComplete -> "completed"
            earlierIncomplete -> "locked"
            isCurrent -> "current"
            else -> "upcoming"
        }

        // Format time label
        var timeLabel: String? = null
        val rawTime = prayerTimings?.timings?.get(name)
        if (rawTime != null) {
            try {
                val parts = rawTime.split(":")
                val h = parts[0].trim().toInt()
                val m = parts[1].trim().take(2) // Strip timezone like " (CEST)"
                val ampm = if (h >= 12) "PM" else "AM"
                val h12 = if (h % 12 == 0) 12 else h % 12
                val timeStr = "$h12:$m $ampm"
                timeLabel = when (readPreference) {
                    "before" -> "Before $timeStr"
                    "after" -> "After $timeStr"
                    else -> "Around $timeStr"
                }
            } catch (_: Exception) { }
        }

        PrayerSlot(
            name = name,
            time = timeLabel,
            count = count,
            doneInSlot = doneInSlot,
            slotStart = slotStart,
            slotEnd = slotEnd,
            status = status
        )
    }
}

