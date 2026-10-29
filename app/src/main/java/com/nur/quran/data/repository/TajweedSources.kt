package com.nur.quran.data.repository

import com.nur.quran.data.api.ApiTajweedVerse
import com.nur.quran.utils.TajweedProcessor

/** Full-verse markup is available before first paint, including on a cold offline open. */
internal fun completeTajweedRows(
    chapterId: Int,
    bundled: List<ApiTajweedVerse>,
    cached: List<ApiTajweedVerse>,
): List<ApiTajweedVerse> {
    val rows = linkedMapOf<String, ApiTajweedVerse>()
    for (row in bundled + cached) {
        if (row.verse_key?.substringBefore(":")?.toIntOrNull() == chapterId && !row.text_uthmani_tajweed.isNullOrBlank()) {
            rows[row.verse_key] = row
        }
    }
    return rows.values.toList()
}

internal fun tajweedMapForKeys(keys: Set<String>, rows: List<ApiTajweedVerse>): Map<String, String> =
    rows.filter { it.verse_key in keys && it.text_uthmani_tajweed.isNotBlank() }
        .associate { it.verse_key to TajweedProcessor.sanitizeTajweedHtml(it.text_uthmani_tajweed) }
