package com.nur.quran.ui.components.planner

import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.ceil

data class PaceStats(
    val dailyPages: Int,
    val perPrayer: Int,
    val endLabel: String
)

object PlannerUtils {
    const val TOTAL_QURAN_PAGES = 604

    /**
     * Web parity matching getPaceStats in Planner.jsx:
     * Calculates daily pages, pages per prayer (5 prayers), and projected end date
     * skipping excluded days of week (Sunday=0 .. Saturday=6, or Calendar.SUNDAY..SATURDAY).
     */
    fun getPaceStats(
        durationDays: Int,
        excludeDays: List<Int> = emptyList(),
        startDateStr: String? = null,
        totalUnits: Int = TOTAL_QURAN_PAGES
    ): PaceStats {
        val safeDuration = if (durationDays <= 0) 1 else durationDays
        val dailyPages = ceil(totalUnits.toDouble() / safeDuration.toDouble()).toInt()
        val perPrayer = ceil(dailyPages / 5.0).toInt()

        val cal = Calendar.getInstance()
        if (!startDateStr.isNullOrBlank()) {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            try {
                val parsed = sdf.parse(startDateStr)
                if (parsed != null) cal.time = parsed
            } catch (_: Exception) {}
        }

        var daysFound = 0
        while (daysFound < safeDuration) {
            // Calendar.DAY_OF_WEEK: Sunday is 1, Monday is 2, ..., Saturday is 7
            // Web JS getDay(): Sunday is 0, Monday is 1, ..., Saturday is 6
            val jsDay = (cal.get(Calendar.DAY_OF_WEEK) - 1 + 7) % 7
            if (!excludeDays.contains(jsDay)) {
                daysFound++
            }
            if (daysFound < safeDuration) {
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val outFormat = SimpleDateFormat("MMM d, yyyy", Locale.US)
        val endLabel = outFormat.format(cal.time)

        return PaceStats(
            dailyPages = dailyPages,
            perPrayer = perPrayer,
            endLabel = endLabel
        )
    }

    /**
     * Formats reading seconds into clean human-readable session duration:
     * e.g. "12m", "1h 05m", "45s"
     */
    fun formatSessionTime(seconds: Long): String {
        if (seconds <= 0) return "0m"
        val m = seconds / 60
        val s = seconds % 60
        if (m == 0L) return "${s}s"
        val h = m / 60
        val remM = m % 60
        return if (h > 0) "${h}h ${remM}m" else "${m}m"
    }

    /**
     * Formats days remaining label:
     * e.g. "30 days left", "1 day left", "Completed"
     */
    fun formatDaysRemaining(totalDays: Int, completedDays: Int): String {
        val remaining = (totalDays - completedDays).coerceAtLeast(0)
        return when {
            remaining == 0 -> "Completed"
            remaining == 1 -> "1 day left"
            else -> "$remaining days left"
        }
    }

    /**
     * Calculates completion ratio from 0.0f to 1.0f.
     */
    fun calculateRingProgress(completed: Int, total: Int): Float {
        if (total <= 0) return 0f
        return (completed.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    }
}
