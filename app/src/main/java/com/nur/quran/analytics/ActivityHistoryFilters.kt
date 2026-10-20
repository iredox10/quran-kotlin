package com.nur.quran.analytics

import com.nur.quran.data.db.entities.ReadingSessionEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Kotlin port of the web app's src/utils/historyFilter.js (Activity History
 * page, /progress/activity) — pure search + grouping helpers; `now` is
 * injected so tests can pin a reference date.
 */
object ActivityHistoryFilters {

    enum class HistoryRange(val id: String, val label: String) {
        TODAY("today", "Today"),
        YESTERDAY("yesterday", "Yesterday"),
        WEEK("week", "This Week"),
        MONTH("month", "This Month"),
        ALL("all", "All Time");
    }

    /** The Activity History tab ids (web HISTORY_TABS). */
    val HISTORY_TABS = HistoryRange.entries

    private val DAY_FMT = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun dateKey(date: Date): String = DAY_FMT.format(date)

    private fun addDays(key: String, days: Int): String {
        val cal = Calendar.getInstance()
        runCatching { cal.time = DAY_FMT.parse(key) ?: cal.time }
        cal.add(Calendar.DATE, days)
        return DAY_FMT.format(cal.time)
    }

    /** web rangeKeys(): null keys == all time. */
    fun rangeKeys(range: HistoryRange, now: Date = Date()): List<String>? {
        val today = dateKey(now)
        return when (range) {
            HistoryRange.TODAY -> listOf(today)
            HistoryRange.YESTERDAY -> listOf(addDays(today, -1))
            HistoryRange.WEEK -> {
                val cal = Calendar.getInstance().apply { time = now }
                val diff = (cal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY + 7) % 7
                val start = Calendar.getInstance().apply { time = now; add(Calendar.DATE, -diff) }
                (0 until 7).map { i ->
                    val c = Calendar.getInstance().apply { time = start.time; add(Calendar.DATE, i) }
                    DAY_FMT.format(c.time)
                }
            }
            HistoryRange.MONTH -> {
                val cal = Calendar.getInstance().apply { time = now }
                val month = cal.get(Calendar.MONTH)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                buildList {
                    while (cal.get(Calendar.MONTH) == month) {
                        add(DAY_FMT.format(cal.time))
                        cal.add(Calendar.DATE, 1)
                    }
                }
            }
            HistoryRange.ALL -> null
        }
    }

    fun normalizeQuery(query: String?): String = query?.trim()?.lowercase(Locale.US) ?: ""

    /** Case-insensitive match on type id/label, surah names and "surah {id}". */
    fun matchesQuery(
        session: ReadingSessionEntity,
        query: String,
        chapterNameResolver: (Int?) -> Pair<String?, String?>? = { null },
    ): Boolean {
        val q = normalizeQuery(query)
        if (q.isEmpty()) return true
       	val typed = listOfNotNull(session.type, TYPE_LABELS[session.type])
        typed.forEach { if (it.trim().lowercase(Locale.US).contains(q)) return true }
        session.chapterId?.let { chapterId ->
            chapterNameResolver(chapterId)?.let { (simple, arabic) ->
                listOfNotNull(simple, arabic).forEach { name ->
                    if (name.trim().lowercase(Locale.US).contains(q)) return true
                }
            }
            if ("surah $chapterId".lowercase(Locale.US).contains(q)) return true
        }
        return false
    }

    val TYPE_LABELS: Map<String, String> = mapOf(
        "reading" to "Reading",
        "memorizing" to "Memorizing",
        "listening" to "Listening",
        "pomodoro" to "Focus",
    )

    /** web filterSessions: range filter + query filter combined. */
    fun filterSessions(
        sessions: List<ReadingSessionEntity>,
        range: HistoryRange,
        query: String = "",
        chapterNameResolver: (Int?) -> Pair<String?, String?>? = { null },
        now: Date = Date(),
    ): List<ReadingSessionEntity> {
        val keys = rangeKeys(range, now)
        val inRange = if (keys == null) sessions else {
            val set = keys.toSet()
            sessions.filter { it.date in set }
        }
        if (normalizeQuery(query).isEmpty()) return inRange
        return inRange.filter { matchesQuery(it, query, chapterNameResolver) }
    }

    /** Explicit timestamp, else the session's date at UTC midnight (web sessionTime). */
    fun sessionTime(session: ReadingSessionEntity): Long =
        if (session.timestamp > 0L) session.timestamp else runCatching {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(session.date)?.time ?: 0L
        }.getOrDefault(0L)

    /** "Today" / "Yesterday" / "Sat, Oct 3" heading for a date key (web dayLabel). */
    fun dayLabel(key: String, now: Date = Date()): String {
        val today = dateKey(now)
        if (key == today) return "Today"
        if (key == addDays(today, -1)) return "Yesterday"
        return runCatching {
            SimpleDateFormat("EEE, MMM d", Locale.US).format(DAY_FMT.parse(key) ?: now)
        }.getOrDefault(key)
    }

    /** One day bucket of sessions: { key, label, sessions, totalSeconds }. */
    data class DayGroup(
        val key: String,
        val label: String,
        val sessions: List<ReadingSessionEntity>,
        val totalSeconds: Long,
    )

    /** Group by session.date, newest day first, sessions newest-first inside each day. */
    fun groupSessionsByDay(sessions: List<ReadingSessionEntity>, now: Date = Date()): List<DayGroup> {
        val byDay = LinkedHashMap<String, MutableList<ReadingSessionEntity>>()
        sessions.forEach { s ->
            val key = s.date.ifBlank { return@forEach }
            byDay.getOrPut(key) { mutableListOf() }.add(s)
        }
        return byDay.entries
            .map { (key, list) ->
                val sorted = list.sortedByDescending { sessionTime(it) }
                DayGroup(
                    key = key,
                    label = dayLabel(key, now),
                    sessions = sorted,
                    totalSeconds = sorted.sumOf { it.duration },
                )
            }
            .sortedWith(compareByDescending<DayGroup> { it.key })
    }
}
