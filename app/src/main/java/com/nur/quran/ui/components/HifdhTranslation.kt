package com.nur.quran.ui.components

import androidx.core.text.HtmlCompat

/** Footnote markers are not translation words; decode the remaining text for plain reading. */
internal fun hifdhTranslationText(source: String?): String = HtmlCompat.fromHtml(
    source.orEmpty().replace(Regex("<sup\\b[^>]*>.*?</sup>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), ""),
    HtmlCompat.FROM_HTML_MODE_LEGACY
).toString().trim()
