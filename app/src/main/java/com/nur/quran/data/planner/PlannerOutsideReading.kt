package com.nur.quran.data.planner

import java.time.LocalDate
import java.util.UUID
import kotlin.math.roundToLong

/** Immutable audit record. Seconds are separate from measured in-app timers. */
data class PlannerOutsideReading(
    val id: String,
    val date: String,
    val createdAt: Long,
    val timing: String,
    val pageSeconds: Map<Int, Long>,
    /** Each physical page is timed once, even at shared surah boundaries. */
    val dayByPage: Map<Int, Int>,
    val samplePages: Int = 0
) {
    val totalSeconds: Long get() = pageSeconds.values.sum()
}

data class PlannerPagePace(val secondsPerPage: Long, val samplePages: Int)
data class OutsideReadingSelection(val pages: List<Int>, val dayByPage: Map<Int, Int>, val skippedPages: Int)
data class OutsideReadingResult(val plan: ReadingPlan, val entry: PlannerOutsideReading, val resumeDay: Int?, val resumePage: Int?)

object PlannerOutsideReadings {
    const val ESTIMATED = "estimated"
    const val MANUAL = "manual"

    fun entries(plan: ReadingPlan): List<PlannerOutsideReading> = plan.outsideReadings.values
        .filter { it.date.isNotBlank() && it.pageSeconds.isNotEmpty() }
        .sortedWith(compareBy<PlannerOutsideReading> { it.date }.thenBy { it.createdAt })

    fun secondsByDay(plan: ReadingPlan): Map<Int, Long> {
        val totals = mutableMapOf<Int, Long>()
        entries(plan).forEach { log -> log.pageSeconds.forEach { (page, seconds) ->
            log.dayByPage[page]?.let { day -> totals[day] = (totals[day] ?: 0) + seconds }
        } }
        return totals
    }

    /** Weighted personal pace. Neither entered nor estimated outside time trains it. */
    fun pace(plans: List<ReadingPlan>, timers: Map<String, Map<Int, PlannerSessionTimer>>): PlannerPagePace? {
        var totalSeconds = 0L
        var totalPages = 0
        plans.distinctBy { it.id }.forEach { plan ->
            val outsidePages = entries(plan).flatMap { it.pageSeconds.keys }.toSet()
            val seen = mutableSetOf<Int>()
            plan.assignments.forEach { day ->
                val measured = timers[plan.id]?.get(day.dayNumber)?.totalSeconds ?: 0
                val read = PlannerEngine.splitAssignmentReadPages(plan, day).first.toSet() - outsidePages - seen
                if (measured > 0 && read.isNotEmpty()) {
                    seen.addAll(read); totalSeconds += measured; totalPages += read.size
                }
            }
        }
        return if (totalPages == 0) null else PlannerPagePace(
            (totalSeconds.toDouble() / totalPages).roundToLong().coerceAtLeast(1), totalPages)
    }

    fun select(plan: ReadingPlan, first: Int, last: Int): OutsideReadingSelection {
        require(first in 1..604 && last in first..604) { "range" }
        val unreadByDay = plan.assignments.associate { day ->
            day.dayNumber to PlannerEngine.splitAssignmentReadPages(plan, day).second.toSet()
        }
        val assigned = plan.assignments.flatMap { day -> day.items.flatMap { (it.pageStart..it.pageEnd).toList() } }.toSet()
        require((first..last).all { it in assigned }) { "outside_plan" }
        val days = linkedMapOf<Int, Int>()
        for (page in first..last) {
            plan.assignments.firstOrNull { page in unreadByDay[it.dayNumber].orEmpty() }?.let { days[page] = it.dayNumber }
        }
        return OutsideReadingSelection(days.keys.toList(), days, last - first + 1 - days.size)
    }

    fun record(plan: ReadingPlan, first: Int, last: Int, date: String, timing: String,
        manualPageSeconds: Map<Int, Long> = emptyMap(), pace: PlannerPagePace? = null,
        id: String = UUID.randomUUID().toString(), now: Long = System.currentTimeMillis(),
        today: String = PlannerEngine.formatPlannerDate()): OutsideReadingResult {
        require(plan.archivedAt == null) { "archived" }
        require(id !in plan.outsideReadings) { "duplicate" }
        val readingDate = runCatching { LocalDate.parse(date) }.getOrNull()
        require(readingDate != null && !readingDate.isAfter(LocalDate.parse(today))) { "date" }
        require(timing == ESTIMATED || timing == MANUAL) { "timing" }
        val selection = select(plan, first, last)
        require(selection.pages.isNotEmpty()) { "already_read" }
        val seconds = selection.pages.associateWith { page ->
            val value = if (timing == ESTIMATED) pace?.secondsPerPage else manualPageSeconds[page]
            require(value != null && value in 1..86_400) { if (timing == ESTIMATED) "no_pace" else "time" }
            value
        }
        require(seconds.values.sum() <= 86_400) { "time" }
        val entry = PlannerOutsideReading(id, date, now, timing, seconds, selection.dayByPage,
            if (timing == ESTIMATED) pace?.samplePages ?: 0 else 0)
        var updated = plan
        plan.assignments.forEach { day ->
            val pending = PlannerEngine.splitAssignmentReadPages(updated, day).second.toSet()
            if (selection.pages.none { it in pending }) return@forEach
            // Keep legacy item-count progress as explicit pages before adding a
            // partial range; markPageRead must not erase those completed items.
            val previouslyRead = PlannerEngine.splitAssignmentReadPages(updated, day).first.distinct()
            val additions = selection.pages.filter { it in pending }
            updated = updated.copy(assignmentReadPages = updated.assignmentReadPages +
                (day.dayNumber to (previouslyRead + additions.dropLast(1)).distinct()))
            updated = PlannerReadingProgress.markPageRead(updated, day.dayNumber, additions.last(), date)
        }
        updated = updated.copy(outsideReadings = updated.outsideReadings + (id to entry),
            lastReadPage = last, lastReadVerseKey = null)
        // Continue forward from the last logged page, crossing day boundaries.
        val next = updated.assignments.flatMap { day ->
            PlannerEngine.splitAssignmentReadPages(updated, day).second.distinct().map { day.dayNumber to it }
        }.firstOrNull { it.second > last }
            ?: PlannerEngine.getResumeAssignment(updated, today)?.let { day ->
                day.dayNumber to PlannerEngine.getAssignmentReadingResumePage(updated, day)
            }
        if (next != null) updated = updated.copy(lastReadPage = next.second, lastReadVerseKey = null)
        return OutsideReadingResult(updated, entry, next?.first, next?.second)
    }
}
