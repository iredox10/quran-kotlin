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
        // Web parity (Planner.jsx): "1h 5m" / "5m 3s" / "12s".
        if (seconds <= 0) return "0s"
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        if (h > 0) return "${h}h ${m}m"
        if (m > 0) return "${m}m ${s}s"
        return "${s}s"
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

    /**
     * Web parity (Planner.jsx handleExportCalendar): builds quran_plan.ics
     * content with one all-day VEVENT per assignment.
     */
    fun buildPlannerIcs(plan: com.nur.quran.data.planner.ReadingPlan): String {
        val unitPlural = com.nur.quran.data.planner.PLANNER_UNITS[plan.unitType]?.plural ?: "Pages"
        val sb = StringBuilder("BEGIN:VCALENDAR\nVERSION:2.0\nPRODID:-//QuranApp//Planner//EN\n")
        for (a in plan.assignments) {
            val dateStr = a.date.replace("-", "")
            sb.append("BEGIN:VEVENT\nDTSTART;VALUE=DATE:$dateStr\nDTEND;VALUE=DATE:$dateStr\n")
            sb.append("SUMMARY:Quran Plan - Day ${a.dayNumber}\n")
            sb.append("DESCRIPTION:Read ${a.items.size} $unitPlural\nEND:VEVENT\n")
        }
        sb.append("END:VCALENDAR")
        return sb.toString()
    }

    /**
     * Shares the plan calendar: cache-dir .ics file via FileProvider,
     * falling back to raw text when file sharing is unavailable.
     */
    fun sharePlannerIcs(context: android.content.Context, plan: com.nur.quran.data.planner.ReadingPlan) {
        val ics = buildPlannerIcs(plan)
        try {
            val dir = java.io.File(context.cacheDir, "shared").apply { mkdirs() }
            val file = java.io.File(dir, "quran_plan.ics")
            file.writeText(ics)
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", file
            )
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/calendar"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(android.content.Intent.createChooser(intent, "Export Plan"))
        } catch (_: Exception) {
            val fallback = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(android.content.Intent.EXTRA_TEXT, ics)
            }
            context.startActivity(android.content.Intent.createChooser(fallback, "Export Plan"))
        }
    }
}
