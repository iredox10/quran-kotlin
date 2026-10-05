package com.nur.quran.analytics

/**
 * Pure-Kotlin port of the web app's utils/activityRecord.js: validation +
 * payload helpers for the manual "Log activity" form, before handing the
 * payload to `logReadingSession` (Room reading_sessions store).
 */
object ActivityRecord {

    /** Hard ceiling for a single manual entry (minutes). */
    const val MAX_LOG_MINUTES = 600

    /** Quick-pick chips offered next to the minutes input. */
    val MINUTES_CHIPS = listOf(5, 10, 15, 30)

    /** Activity types the session store understands. */
    val LOG_TYPES = listOf("reading", "memorizing", "listening", "pomodoro")

    /** Coerce an activity type to one the store understands (falls back to reading). */
    fun normalizeLogType(type: String?): String =
        if (LOG_TYPES.contains(type)) type!! else "reading"

    /** Coerce a chapter/surah id to 1..114, or null when unusable. */
    fun normalizeChapterId(value: String?): Int? {
        val raw = value?.trim().orEmpty()
        if (raw.isEmpty()) return null
        val num = raw.toIntOrNull() ?: return null
        return if (num in 1..114) num else null
    }

    data class MinutesResult(val valid: Boolean, val minutes: Int?, val capped: Boolean, val error: String?)

    /** Validate a minutes entry; accepts decimals by rounding, caps at MAX_LOG_MINUTES. */
    fun validateMinutes(value: String?): MinutesResult {
        val raw = value?.trim().orEmpty()
        if (raw.isEmpty()) return MinutesResult(false, null, false, "Enter how many minutes you logged.")
        val num = raw.toDoubleOrNull()
            ?: return MinutesResult(false, null, false, "Minutes must be a number.")
        val mins = Math.round(num).toInt()
        if (mins < 1) return MinutesResult(false, null, false, "Minutes must be at least 1.")
        val capped = mins > MAX_LOG_MINUTES
        return MinutesResult(true, minOf(mins, MAX_LOG_MINUTES), capped, null)
    }

    data class SessionLog(
        val ok: Boolean,
        val durationSec: Int = 0,
        val type: String = "reading",
        val chapterId: Int? = null,
        val capped: Boolean = false,
        val error: String? = null
    )

    /** Build the arguments for `logReadingSession` from raw form values. */
    fun buildSessionLog(type: String?, minutes: String?, chapterId: String?): SessionLog {
        val mins = validateMinutes(minutes)
        if (!mins.valid) return SessionLog(false, error = mins.error)
        return SessionLog(true, mins.minutes!! * 60, normalizeLogType(type), normalizeChapterId(chapterId), mins.capped)
    }
}
