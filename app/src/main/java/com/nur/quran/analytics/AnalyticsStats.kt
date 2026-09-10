package com.nur.quran.analytics

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Pure-Kotlin port of the web app's Progress.jsx analytics math
 * (dailyActivity, streak, activityByType, smartInsight, achievements).
 * Free of Compose/Room so it is unit-testable on the JVM.
 */
object AnalyticsStats {

    private val DAY_FMT = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val LABEL_FMT = SimpleDateFormat("EEE", Locale.US)

    data class Session(
        val date: String,
        val durationSec: Long,
        val type: String = "reading",
        val chapterId: Int? = null,
        val timestamp: Long = 0L
    )

    data class Badge(val icon: String, val title: String, val desc: String)

    fun dayMinutes(sessions: List<Session>, dateStr: String): Int =
        Math.round(sessions.filter { it.date == dateStr }.sumOf { it.durationSec } / 60.0f).toInt()

    fun last7Days(sessions: List<Session>, today: Date = Date()): List<Pair<String, Int>> {
        val out = mutableListOf<Pair<String, Int>>()
        for (i in 6 downTo 0) {
            val cal = Calendar.getInstance().apply { time = today; add(Calendar.DATE, -i) }
            val dStr = DAY_FMT.format(cal.time)
            out.add(LABEL_FMT.format(cal.time) to dayMinutes(sessions, dStr))
        }
        return out
    }

    /** Consecutive-day streak ending today (or yesterday if today is empty). */
    fun streak(uniqueDates: Set<String>, today: Date = Date()): Int {
        if (uniqueDates.isEmpty()) return 0
        val sorted = uniqueDates.sortedDescending()
        val cal = Calendar.getInstance().apply { time = today }
        val todayStr = DAY_FMT.format(cal.time)
        if (sorted.first() != todayStr) {
            cal.add(Calendar.DATE, -1)
            if (sorted.first() != DAY_FMT.format(cal.time)) return 0
        }
        var count = 0
        val cursor = Calendar.getInstance().apply { time = today }
        while (count <= 366) {
            val ds = DAY_FMT.format(cursor.time)
            if (ds in uniqueDates) count++ else break
            cursor.add(Calendar.DATE, -1)
        }

    fun todayMinutes(sessions: List<Session>, todayStr: String = DAY_FMT.format(Date())): Int =
        dayMinutes(sessions, todayStr)

    fun allTimeMinutes(sessions: List<Session>): Int =
        Math.round(sessions.sumOf { it.durationSec } / 60.0f).toInt()

    fun minutesByType(sessions: List<Session>): Map<String, Int> = mapOf(
        "reading" to Math.round(sessions.filter { it.type == "reading" || it.type.isEmpty() }.sumOf { it.durationSec } / 60.0f).toInt(),
        "memorizing" to Math.round(sessions.filter { it.type == "memorizing" }.sumOf { it.durationSec } / 60.0f).toInt(),
        "focus" to Math.round(sessions.filter { it.type == "pomodoro" || it.type == "focus" }.sumOf { it.durationSec } / 60.0f).toInt(),
        "listening" to Math.round(sessions.filter { it.type == "listening" }.sumOf { it.durationSec } / 60.0f).toInt()
    )

    fun weeklyTotalMinutes(sessions: List<Session>, last7: List<String>): Int =
        Math.round(last7.sumOf { d -> sessions.filter { it.date == d }.sumOf { it.durationSec } } / 60.0f).toInt()

    fun weeklyGoalPercent(weeklyMins: Int, goalMins: Int = 180): Int =

    /** Best weekday by session count — mirrors web smartInsight text. */
    fun smartInsight(sessions: List<Session>): String {
        if (sessions.isEmpty()) return "Start reading to unlock insights!"
        val dayCounts = mutableMapOf("Sun" to 0L, "Mon" to 0L, "Tue" to 0L, "Wed" to 0L, "Thu" to 0L, "Fri" to 0L, "Sat" to 0L)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val label = SimpleDateFormat("EEE", Locale.US)
        sessions.forEach { s ->
            runCatching { label.format(sdf.parse(s.date)!!) }.getOrNull()?.let { d ->
                dayCounts[d] = (dayCounts[d] ?: 0L) + 1
            }
        }
        val best = dayCounts.maxByOrNull { it.value }?.key ?: return "Start reading to unlock insights!"
        if ((dayCounts[best] ?: 0L) == 0L) return "Start reading to unlock insights!"
        val full = mapOf("Sun" to "Sundays", "Mon" to "Mondays", "Tue" to "Tuesdays", "Wed" to "Wednesdays", "Thu" to "Thursdays", "Fri" to "Fridays", "Sat" to "Saturdays")
        return "You're most consistent on ${full[best]}. Keep it up!"
    }

    /** Web parity: union of session chapterIds + recentlyRead, newest tier first. */
    fun achievements(streakDays: Int, allTimeMins: Int, sessionChapterIds: Collection<Int?>, recentlyReadIds: Collection<Int>): List<Badge> {
        val badges = mutableListOf<Badge>()
        if (streakDays >= 3) badges.add(Badge("🔥", "3-Day Streak", "Consistency is key."))
        if (streakDays >= 7) badges.add(Badge("🔥", "7-Day Streak", "A whole week!"))
        if (streakDays >= 30) badges.add(Badge("🔥", "30-Day Streak", "Unstoppable!"))
        if (allTimeMins >= 100) badges.add(Badge("⏱️", "100 Minutes", "First big milestone."))
        if (allTimeMins >= 500) badges.add(Badge("⏱️", "500 Minutes", "Dedicated reader."))
        val surahCount = (recentlyReadIds.toSet() + sessionChapterIds.filterNotNull().toSet()).size
        if (surahCount >= 5) badges.add(Badge("🗺️", "Explorer", "Read 5 Surahs."))
        if (surahCount >= 30) badges.add(Badge("🗺️", "Traveler", "Read 30 Surahs."))
        if (surahCount >= 114) badges.add(Badge("👑", "Khatm", "Read all 114 Surahs!"))
        return badges.takeLast(3).reversed()
    }

    fun heatmapLevel(active: Boolean, durationSec: Long): Int {
        if (!active) return 0
        val mins = Math.round(durationSec / 60.0f).toInt()
        return when {
            mins < 10 -> 1
            mins < 30 -> 2
            else -> 3
        }
    }

        ((weeklyMins.toFloat() / goalMins.toFloat()) * 100f).coerceAtMost(100f).toInt()

        return count
    }
}
