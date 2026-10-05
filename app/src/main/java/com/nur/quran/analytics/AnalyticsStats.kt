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

    // ── 20-badge web-parity engine (additive; legacy achievements() kept intact) ──
    const val TOTAL_SURAHS = 114
    const val COLLAPSED_BADGE_COUNT = 4
    data class AchievementDef(val id: String, val icon: String, val title: String, val desc: String, val metric: String, val target: Int)
    data class AchievementStats(val totalMinutes: Int, val sessionCount: Int, val activeDays: Int, val activeDaysLast7: Int, val currentStreak: Int, val memorizingMinutes: Int, val listeningMinutes: Int, val focusSessions: Int, val uniqueSurahs: Int, val todaySessions: Int)
    data class AchievementBadge(val id: String, val icon: String, val title: String, val desc: String, val unlocked: Boolean, val current: Int, val target: Int, val progress: Float)
    val ACHIEVEMENT_CATALOG = listOf(
        AchievementDef("streak-3", "🔥", "Kindled", "Active 3 days in a row.", "currentStreak", 3),
        AchievementDef("streak-7", "🔥", "Steady Flame", "A full week without a gap.", "currentStreak", 7),
        AchievementDef("streak-14", "🔥", "Two Weeks of Light", "14 days unbroken.", "currentStreak", 14),
        AchievementDef("streak-30", "🔥", "A Month Steadfast", "30 consecutive days in the Quran.", "currentStreak", 30),
        AchievementDef("streak-100", "🔥", "Hundred Days of Nur", "100 days without slipping.", "currentStreak", 100),
        AchievementDef("minutes-100", "⏱️", "First 100 Minutes", "100 minutes of quiet effort.", "totalMinutes", 100),
        AchievementDef("minutes-500", "⏱️", "500 Minutes", "A real habit taking root.", "totalMinutes", 500),
        AchievementDef("minutes-1000", "⏱️", "1,000 Minutes", "Over 16 hours with the Book.", "totalMinutes", 1000),
        AchievementDef("minutes-5000", "⏱️", "5,000 Minutes", "A deep well of time invested.", "totalMinutes", 5000),
        AchievementDef("surahs-5", "🗺️", "Curious Traveler", "Opened 5 different surahs.", "uniqueSurahs", 5),
        AchievementDef("surahs-30", "🗺️", "Many Paths", "Explored 30 different surahs.", "uniqueSurahs", 30),
        AchievementDef("khatm", "👑", "Khatm", "All 114 surahs — a complete journey.", "uniqueSurahs", TOTAL_SURAHS),
        AchievementDef("memorize-500", "📖", "Carried in the Heart", "500 minutes of memorization.", "memorizingMinutes", 500),
        AchievementDef("listening-100", "🎧", "Listening Ears", "100 minutes of recitation listened to.", "listeningMinutes", 100),
        AchievementDef("focus-10", "🎯", "Deep Focus", "10 focus sessions completed.", "focusSessions", 10),
        AchievementDef("focus-50", "🎯", "Unshaken Attention", "50 focus sessions completed.", "focusSessions", 50),
        AchievementDef("active-7", "📅", "Seven Days Present", "Active on 7 different days.", "activeDays", 7),
        AchievementDef("active-30", "📅", "Regular Rhythm", "Active on 30 different days.", "activeDays", 30),
        AchievementDef("active-100", "📅", "Centurion of Days", "Active on 100 different days.", "activeDays", 100),
        AchievementDef("perfect-week", "🌟", "Perfect Week", "Active every day for the last 7 days.", "activeDaysLast7", 7)
    )
    fun computeAchievementStats(sessions: List<Session>, recentlyReadIds: Collection<Int> = emptyList(), now: Date = Date()): AchievementStats {
        val todayKey = DAY_FMT.format(now)
        val activeDates = sessions.mapNotNull { it.date.takeIf { d -> d.isNotBlank() } }.toSet()
        val surahIds = (sessions.mapNotNull { it.chapterId }.filter { it > 0 } + recentlyReadIds.filter { it > 0 }).toSet()
        var memSec = 0L; var lisSec = 0L; var focusCount = 0
        sessions.forEach { s ->
            val sec = s.durationSec.coerceAtLeast(0L)
            when (s.type.lowercase().trim()) { "memorizing" -> memSec += sec; "listening" -> lisSec += sec; "pomodoro", "focus" -> focusCount++ }
        }
        // Web counts pomodoro sessions only; Kotlin also records "focus" — count both.
        val cal = Calendar.getInstance().apply { time = now }
        var last7 = 0
        repeat(7) { if (DAY_FMT.format(cal.time) in activeDates) last7++; cal.add(Calendar.DATE, -1) }
        fun mins(sec: Long) = Math.round(sec / 60.0f).toInt()
        return AchievementStats(mins(sessions.sumOf { it.durationSec.coerceAtLeast(0L) }), sessions.size, activeDates.size, last7, streak(activeDates, now), mins(memSec), mins(lisSec), focusCount, surahIds.size, sessions.count { it.date == todayKey })
    }
    private fun metricValue(stats: AchievementStats, metric: String): Int = when (metric) {
        "currentStreak" -> stats.currentStreak; "totalMinutes" -> stats.totalMinutes; "uniqueSurahs" -> stats.uniqueSurahs
        "memorizingMinutes" -> stats.memorizingMinutes; "listeningMinutes" -> stats.listeningMinutes; "focusSessions" -> stats.focusSessions
        "activeDays" -> stats.activeDays; "activeDaysLast7" -> stats.activeDaysLast7; else -> 0
    }
    fun evaluateAchievements(stats: AchievementStats): List<AchievementBadge> = ACHIEVEMENT_CATALOG.map { def ->
        val cur = metricValue(stats, def.metric).coerceAtLeast(0)
        AchievementBadge(def.id, def.icon, def.title, def.desc, cur >= def.target, cur, def.target, if (def.target > 0) (cur.toFloat() / def.target).coerceIn(0f, 1f) else 0f)
    }
    fun orderBadges(badges: List<AchievementBadge>): List<AchievementBadge> =
        badges.filter { it.unlocked }.reversed() + badges.filter { !it.unlocked }.sortedWith(compareByDescending<AchievementBadge> { it.progress }.thenBy { it.target })

    fun heatmapLevel(active: Boolean, durationSec: Long): Int {
        if (!active) return 0
        val mins = Math.round(durationSec / 60.0f).toInt()
        return when {
            mins < 10 -> 1
            mins < 30 -> 2
            else -> 3
        }
    }
}
