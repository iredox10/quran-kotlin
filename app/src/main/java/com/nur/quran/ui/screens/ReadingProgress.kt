package com.nur.quran.ui.screens

import com.nur.quran.data.db.entities.VerseEntity

/** Translation mode uses verse rows; continuous reading mode uses printed-page rows. */
internal fun readingVerseForItem(verses: List<VerseEntity>, key: Any): String? = when (key) {
    is Int -> verses.firstOrNull { it.id == key }?.verseKey
    is String -> key.takeIf { it.startsWith("page_") }?.removePrefix("page_")?.toIntOrNull()
        ?.let { page -> verses.firstOrNull { it.pageNumber == page }?.verseKey }
    else -> null
}
