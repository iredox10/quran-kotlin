package com.nur.quran.data.db

import java.time.Instant
import java.time.ZoneOffset

/**
 * Web parity for reading-session logging (Memorization.jsx unmount guard +
 * useAppStore.logReadingSession): sessions shorter than 10s are dropped, and
 * the `date` key is the UTC calendar day (`toISOString().split('T')[0]`),
 * not the device-local day.
 */
const val MIN_READING_SESSION_SECONDS = 10L

/** UTC `YYYY-MM-DD` date key for a session, matching web `toISOString()` semantics. */
fun utcDateKey(epochMillis: Long = System.currentTimeMillis()): String =
    Instant.ofEpochMilli(epochMillis).atOffset(ZoneOffset.UTC).toLocalDate().toString()

/** Web parity: only sessions with duration >= 10s are persisted. */
fun isLoggableReadingSession(durationSeconds: Long): Boolean =
    durationSeconds >= MIN_READING_SESSION_SECONDS
