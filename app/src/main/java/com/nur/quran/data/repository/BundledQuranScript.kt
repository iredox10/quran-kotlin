package com.nur.quran.data.repository

import com.nur.quran.data.api.ApiVerse
import com.nur.quran.data.db.entities.VerseEntity
import com.nur.quran.data.db.entities.WordEntity

/** Use the bundled API word scripts, rather than labelling legacy Uthmani as QPC Hafs. */
internal fun withBundledQuranScript(
    verse: VerseEntity,
    bundled: ApiVerse,
): Pair<VerseEntity, List<WordEntity>>? {
    val source = bundled.words?.takeIf { it.isNotEmpty() } ?: return null
    if (source.any { it.text_qpc_hafs.isNullOrBlank() }) return null
    val words = source.map { word ->
        WordEntity(
            id = word.id, verseId = verse.id, position = word.position,
            textUthmani = word.text_uthmani, textIndopak = word.text_indopak,
            textQpcHafs = word.text_qpc_hafs, textUthmaniTajweed = word.text_uthmani_tajweed,
            translation = word.translation?.text, transliteration = word.transliteration?.text,
            charTypeName = word.char_type_name, lineNumber = word.line_number ?: 0,
        )
    }
    return verse.copy(textQpcHafs = source.joinToString(" ") { it.text_qpc_hafs!! }) to words
}
