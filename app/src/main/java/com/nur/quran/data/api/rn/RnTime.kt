package com.nur.quran.data.api.rn

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * RN wire timestamp parsing — epoch-ms numbers, all-digit strings, and
 * ISO-8601. Mirrors `SyncService.parseUpdatedAt` (Appwrite `$updatedAt`
 * parity: classic `...Z` form, Appwrite 2.x offset style `...+00:00`, and
 * fractional seconds of any length). 0 when unparseable, so the sync
 * watermark never invents its own "now" from garbage.
 */
object RnTime {
    fun parse(value: Any?): Long {
        return when (value) {
            null -> 0L
            is Number -> value.toLong()
            is String -> value.trim().let { s ->
                s.toLongOrNull() ?: parseIso(s)
            }
            else -> 0L
        }
    }

    private val isoPatterns = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
    )

    private fun parseIso(v: String): Long =
        runCatching { java.time.Instant.parse(v).toEpochMilli() }.getOrNull()
            // SimpleDateFormat fallback below API 26 (no desugaring in this build).
            ?: isoPatterns.firstNotNullOfOrNull { isoFormat(it, v) }
            ?: 0L

    private fun isoFormat(pattern: String, v: String): Long? = runCatching {
        val fmt = SimpleDateFormat(pattern, Locale.US)
        fmt.timeZone = TimeZone.getTimeZone("UTC")
        fmt.parse(v)?.time
    }.getOrNull()
}
