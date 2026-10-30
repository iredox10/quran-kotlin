package com.nur.quran.i18n

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import java.text.DateFormatSymbols
import java.util.Locale

/** ViewModel weekday identifiers stay stable; only their displayed CLDR labels change. */
@Composable
fun localizedWeekday(stableEnglishLabel: String): String =
    weekdayLabel(stableEnglishLabel, LocalConfiguration.current.locales[0])

internal fun weekdayLabel(stableEnglishLabel: String, locale: Locale): String {
    val index = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").indexOf(stableEnglishLabel)
    return if (index < 0) stableEnglishLabel else DateFormatSymbols.getInstance(locale).shortWeekdays[index + 1]
}
