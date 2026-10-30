package com.nur.quran.data.planner

import java.util.Calendar

/** Read-only history derived from the same offline plan and timer records used by sync. */
data class PlannerHistoryDay(val date: String?, val dayNumber: Int?, val seconds: Long, val cumulativeSeconds: Long)
data class PlannerHistorySummary(
    val completed: Boolean,
    val completedDays: Int,
    val totalDays: Int,
    val pagesRead: Int,
    val totalPages: Int,
    val totalSeconds: Long,
    val timedDays: Int,
    val averageSeconds: Long,
    val completedOn: String?,
    val readingDays: List<PlannerHistoryDay>
)

object PlannerHistory {
    fun isComplete(plan: ReadingPlan) = plan.assignments.isNotEmpty() &&
        plan.assignments.all { PlannerEngine.getAssignmentProgress(plan, it).isComplete }

    fun plans(live: List<ReadingPlan>, archived: List<ReadingPlan>): List<ReadingPlan> =
        (live.filter(::isComplete) + archived).associateBy { it.id }.values
            .sortedWith(compareByDescending<ReadingPlan> { completedOn(it) ?: it.archivedAt ?: it.createdAt }.thenBy { it.id })

    private fun completedOn(plan: ReadingPlan): String? = if (isComplete(plan))
        plan.assignmentCompletedAt.values.filter { it.isNotBlank() }.maxOrNull() else null

    fun summarize(plan: ReadingPlan, timers: Map<Int, PlannerSessionTimer>): PlannerHistorySummary {
        val progress = plan.assignments.map { PlannerEngine.getAssignmentProgress(plan, it) }
        val dated = sortedMapOf<String, Long>()
        val legacy = mutableListOf<Pair<Int, Long>>()
        timers.toSortedMap().forEach { (day, timer) ->
            val daily = timer.secondsByDate.filter { it.key.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) && it.value > 0 }
            daily.forEach { (date, seconds) -> dated[date] = (dated[date] ?: 0) + seconds }
            val undated = (timer.totalSeconds.coerceAtLeast(0) - daily.values.sum()).coerceAtLeast(0)
            if (undated > 0) legacy.add(day to undated)
        }
        var cumulative = 0L
        // Historical dates weren't captured by earlier versions; don't invent them from scheduled dates.
        val rows = legacy.map { (day, seconds) ->
            cumulative += seconds
            PlannerHistoryDay(null, day, seconds, cumulative)
        } + dated.map { (date, seconds) ->
            cumulative += seconds
            PlannerHistoryDay(date, null, seconds, cumulative)
        }
        return PlannerHistorySummary(
            completed = isComplete(plan), completedDays = progress.count { it.isComplete },
            totalDays = plan.assignments.size, pagesRead = progress.sumOf { it.readPagesCount },
            totalPages = progress.sumOf { it.totalPagesCount }, totalSeconds = cumulative,
            timedDays = dated.size, averageSeconds = if (dated.isNotEmpty()) dated.values.sum() / dated.size else 0,
            completedOn = completedOn(plan), readingDays = rows
        )
    }

    /** Split an elapsed reading interval at local midnight; scheduled dates can change. */
    fun addReadingTime(timer: PlannerSessionTimer, seconds: Long, endedAt: Long = System.currentTimeMillis()): PlannerSessionTimer {
        val delta = seconds.coerceAtLeast(0)
        if (delta == 0L) return timer.copy(startedAt = null)
        val dates = timer.secondsByDate.toMutableMap()
        var cursor = endedAt - delta * 1000
        while (cursor < endedAt) {
            val midnight = Calendar.getInstance().apply {
                timeInMillis = cursor
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val end = minOf(midnight, endedAt)
            val part = (end - cursor) / 1000
            val date = PlannerEngine.formatPlannerDate(java.util.Date(cursor))
            dates[date] = (dates[date] ?: 0) + part
            cursor = end
        }
        // Millisecond boundary rounding belongs to the last date; total time remains exact.
        val missing = delta - (dates.values.sum() - timer.secondsByDate.values.sum())
        if (missing > 0) {
            val date = PlannerEngine.formatPlannerDate(java.util.Date(endedAt - 1))
            dates[date] = (dates[date] ?: 0) + missing
        }
        return timer.copy(totalSeconds = timer.totalSeconds.coerceAtLeast(0) + delta, startedAt = null, secondsByDate = dates)
    }

    fun dateLabel(date: String): String = runCatching {
        java.text.SimpleDateFormat("EEE, MMM d, yyyy", java.util.Locale.getDefault()).format(PlannerEngine.parsePlannerDate(date))
    }.getOrDefault(date)

    fun duration(seconds: Long): String {
        val s = seconds.coerceAtLeast(0)
        return when {
            s >= 3600 -> "${s / 3600}h ${(s % 3600) / 60}m"
            s >= 60 -> "${s / 60}m ${s % 60}s"
            else -> "${s}s"
        }
    }
}
