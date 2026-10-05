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
        if (DAY_FMT.format(cursor.time) !in uniqueDates) {
            // Today missing but yesterday present (validated above): start at yesterday,
            // mirroring HomeViewModel.computeStreak's i==0 tolerance.
            cursor.add(Calendar.DATE, -1)
        }
        while (count <= 366) {
            val ds = DAY_FMT.format(cursor.time)
            if (ds in uniqueDates) count++ else break
            cursor.add(Calendar.DATE, -1)
        }
        return count
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
        ((weeklyMins.toFloat() / goalMins.toFloat()) * 100f).coerceAtMost(100f).toInt()

    fun formatMinutes(seconds: Long): String {
        val mins = Math.round(seconds / 60.0f).toInt()
        if (mins < 60) return "${mins}m"
        val hrs = mins / 60
        val rem = mins % 60
        return if (rem > 0) "${hrs}h ${rem}m" else "${hrs}h"
    }

    /** Best weekday by duration seconds — mirrors web smartInsight text. */
    fun smartInsight(sessions: List<Session>): String {
        if (sessions.isEmpty()) return "Start reading to unlock insights!"
        val dayCounts = mutableMapOf("Sun" to 0L, "Mon" to 0L, "Tue" to 0L, "Wed" to 0L, "Thu" to 0L, "Fri" to 0L, "Sat" to 0L)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val label = SimpleDateFormat("EEE", Locale.US)
        sessions.forEach { s ->
            runCatching { label.format(sdf.parse(s.date)!!) }.getOrNull()?.let { d ->
                dayCounts[d] = (dayCounts[d] ?: 0L) + s.durationSec
            }
        }
        val best = dayCounts.maxByOrNull { it.value }?.key ?: return "Start reading to unlock insights!"
        if ((dayCounts[best] ?: 0L) == 0L) return "Start reading to unlock insights!"
        val full = mapOf("Sun" to "Sundays", "Mon" to "Mondays", "Tue" to "Tuesdays", "Wed" to "Wednesdays", "Thu" to "Thursdays", "Fri" to "Fridays", "Sat" to "Saturdays")
        return "You usually read best on ${full[best]}. Keep up the great momentum!"
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

    /** Web parity: Today/Week/Month calendar ranges (week starts Sunday). */
    enum class FlowRange(val id: String, val label: String) {
        TODAY("today", "Today"), WEEK("week", "Week"), MONTH("month", "Month");
    }

    private fun dayKeyOf(cal: Calendar): String = DAY_FMT.format(cal.time)

    /** Calendar keys: today=[today], week Sunday-start 7 days, month=calendar month. */
    fun rangeKeys(range: FlowRange, today: Date = Date()): List<String> {
        val cal = Calendar.getInstance().apply { time = today }
        return when (range) {
            FlowRange.TODAY -> listOf(dayKeyOf(cal))
            FlowRange.WEEK -> {
                val diff = (cal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY + 7) % 7
                cal.add(Calendar.DATE, -diff)
                val start = cal.timeInMillis
                (0 until 7).map { i ->
                    cal.timeInMillis = start
                    cal.add(Calendar.DATE, i)
                    dayKeyOf(cal)
                }
            }
            FlowRange.MONTH -> {
                val month = cal.get(Calendar.MONTH)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                buildList {
                    while (cal.get(Calendar.MONTH) == month) {
                        add(dayKeyOf(cal))
                        cal.add(Calendar.DATE, 1)
                    }
                }
            }
        }
    }

    /** Sessions whose date falls in the calendar range. */
    fun filterByRange(sessions: List<Session>, range: FlowRange, today: Date = Date()): List<Session> {
        val keys = rangeKeys(range, today).toSet()
        return sessions.filter { it.date in keys }
    }

    data class DayBucket(val key: String, val label: String, val dayNum: String, val minutes: Int)
    data class HourBucket(val hour: Int, val label: String, val minutes: Int)

    /** One point per date key, oldest first — web bucketByDay parity. */
    fun bucketByDay(sessions: List<Session>, keys: List<String>): List<DayBucket> {
        val secs = sessions.groupBy { it.date }.mapValues { e -> e.value.sumOf { it.durationSec } }
        return keys.map { k ->
            val mins = Math.round((secs[k] ?: 0L) / 60.0f).toInt()
            val date = runCatching { DAY_FMT.parse(k) }.getOrNull() ?: Date()
            DayBucket(k, LABEL_FMT.format(date), k.substring(8).trimStart('0').ifEmpty { "0" }, mins)
        }
    }

    /** 24 local-hour buckets for one day — web bucketByHour parity. */
    fun bucketByHour(sessions: List<Session>, dayKey: String): List<HourBucket> {
        val secs = LongArray(24)
        sessions.filter { it.date == dayKey }.forEach { s ->
            val ts = if (s.timestamp > 0L) s.timestamp else runCatching { DAY_FMT.parse(s.date)?.time ?: 0L }.getOrDefault(0L)
            secs[Calendar.getInstance().apply { timeInMillis = ts }.get(Calendar.HOUR_OF_DAY)] += s.durationSec
        }
        return (0 until 24).map { h ->
            HourBucket(h, if (h == 0) "12a" else if (h < 12) "${h}a" else if (h == 12) "12p" else "${h - 12}p", Math.round(secs[h] / 60.0f).toInt())
        }
    }

    data class FlowSummary(val seconds: Long, val count: Int, val activeDays: Int, val avgPerDayMin: Int)

    /** Totals for a session list — web summarize() parity. */
    fun summarize(sessions: List<Session>): FlowSummary {
        val secs = sessions.sumOf { it.durationSec }
        val active = sessions.filter { it.durationSec > 0 }.map { it.date }.toSet().size
        return FlowSummary(secs, sessions.size, active, if (active > 0) Math.round(secs / active / 60.0f).toInt() else 0)
    }

    /** % change vs previous window; null = no baseline, 0 = no data. */
    fun deltaPercent(currentSec: Long, previousSec: Long): Int? {
        if (previousSec <= 0L) return if (currentSec > 0L) null else 0
        return Math.round((currentSec - previousSec) / previousSec.toFloat() * 100f)
    }

    /** Equal-length window right before `range` — for delta comparisons. */
    fun previousRangeKeys(range: FlowRange, today: Date = Date()): List<String> {
        val keys = rangeKeys(range, today)
        if (keys.isEmpty()) return emptyList()
        val start = Calendar.getInstance().apply {
            time = DAY_FMT.parse(keys.first()) ?: Date(); add(Calendar.DATE, -keys.size) }.timeInMillis
        return keys.indices.map { i ->
            Calendar.getInstance().apply { timeInMillis = start; add(Calendar.DATE, i) }.let { dayKeyOf(it) } }
    }

    private val TITLE_FMT = SimpleDateFormat("MMM d", Locale.US)

    /** Human range title, e.g. "Oct 5" / "Oct 1 – 7" — web rangeTitle parity. */
    fun rangeTitle(range: FlowRange, today: Date = Date()): String {
        val keys = rangeKeys(range, today)
        if (keys.size <= 1) return keys.firstOrNull()?.let { TITLE_FMT.format(DAY_FMT.parse(it) ?: Date()) } ?: ""
        val sameMonth = keys.last().substring(0, 7) == keys.first().substring(0, 7)
        val last = if (sameMonth) keys.last().substring(8).trimStart('0').ifEmpty { "0" } else TITLE_FMT.format(DAY_FMT.parse(keys.last()) ?: Date())
        return "${TITLE_FMT.format(DAY_FMT.parse(keys.first()) ?: Date())} – $last"
    }
}
