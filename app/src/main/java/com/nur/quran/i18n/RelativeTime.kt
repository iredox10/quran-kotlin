package com.nur.quran.i18n

import android.icu.text.RelativeDateTimeFormatter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.nur.quran.R
import java.util.Locale

/** UI-only relative timestamps. ICU supplies grammatical word order and plural forms. */
@Composable
fun relativeTime(timestamp: Long?, now: Long = System.currentTimeMillis()): String {
    if (timestamp == null || timestamp <= 0L) return stringResource(R.string.ui_time_never)
    val locale = LocalConfiguration.current.locales[0]
    val formatter = remember(locale) { RelativeDateTimeFormatter.getInstance(locale) }
    return formatRelativeTime(timestamp, now, formatter)
}

internal fun relativeTimeLabel(timestamp: Long, now: Long, locale: Locale): String =
    formatRelativeTime(timestamp, now, RelativeDateTimeFormatter.getInstance(locale))

private fun formatRelativeTime(timestamp: Long, now: Long, formatter: RelativeDateTimeFormatter): String {
    val minutes = ((now - timestamp).coerceAtLeast(0L)) / 60_000
    if (minutes == 0L) return formatter.format(
        RelativeDateTimeFormatter.Direction.PLAIN,
        RelativeDateTimeFormatter.AbsoluteUnit.NOW
    )
    val (amount, unit) = when {
        minutes < 60 -> minutes to RelativeDateTimeFormatter.RelativeUnit.MINUTES
        minutes < 1440 -> (minutes / 60) to RelativeDateTimeFormatter.RelativeUnit.HOURS
        else -> (minutes / 1440) to RelativeDateTimeFormatter.RelativeUnit.DAYS
    }
    return formatter.format(amount.toDouble(), RelativeDateTimeFormatter.Direction.LAST, unit)
}
