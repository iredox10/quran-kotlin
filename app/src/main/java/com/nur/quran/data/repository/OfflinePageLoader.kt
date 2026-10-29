package com.nur.quran.data.repository

import com.nur.quran.data.db.entities.VerseEntity
import com.nur.quran.data.db.entities.WordEntity

data class LocalPageContent(val verses: List<VerseEntity>, val words: List<WordEntity>)

/** Complete a partially cached page without replacing its downloaded translation or layout. */
internal fun mergeLocalPage(cached: LocalPageContent, bundled: LocalPageContent): LocalPageContent {
    if (bundled.verses.isEmpty()) return cached
    val saved = cached.verses.associateBy { it.verseKey }
    val verses = (bundled.verses.map { local ->
        saved[local.verseKey]?.let { old -> old.copy(
            textUthmani = old.textUthmani?.takeIf { it.isNotBlank() } ?: local.textUthmani,
            textIndopak = old.textIndopak?.takeIf { it.isNotBlank() } ?: local.textIndopak,
            textQpcHafs = old.textQpcHafs?.takeIf { it.isNotBlank() } ?: local.textQpcHafs,
            translation = old.translation?.takeIf { it.isNotBlank() } ?: local.translation,
        ) } ?: local
    } + cached.verses.filter { old -> bundled.verses.none { it.verseKey == old.verseKey } }).sortedBy { it.id }
    val cachedWords = cached.words.groupBy { it.verseId }
    val localWords = bundled.words.groupBy { it.verseId }
    val words = verses.flatMap { verse ->
        val old = cachedWords[verse.id].orEmpty()
        val source = localWords[verse.id].orEmpty()
        val complete = old.size >= source.size && old.map { it.position }.toSet().size == old.size &&
            old.any { it.charTypeName == "word" }
        if (complete || source.isEmpty()) {
            val byPosition = source.associateBy { it.position }
            old.map { word -> word.copy(
                translation = word.translation?.takeIf { it.isNotBlank() } ?: byPosition[word.position]?.translation,
                transliteration = word.transliteration?.takeIf { it.isNotBlank() } ?: byPosition[word.position]?.transliteration,
            ) }
        } else source
    }.sortedWith(compareBy<WordEntity> { it.verseId }.thenBy { it.position })
    return LocalPageContent(verses, words)
}

/** A missing Room page must not cause an HTTP wait when its content ships with the app. */
internal suspend fun loadPageOfflineFirst(
    cached: LocalPageContent,
    bundled: () -> LocalPageContent,
    persist: suspend (LocalPageContent) -> Unit,
    network: suspend () -> LocalPageContent,
): LocalPageContent {
    val local = mergeLocalPage(cached, bundled())
    if (local.verses.isNotEmpty() && local.verses.all { !it.textUthmani.isNullOrBlank() }) {
        if (local != cached) persist(local)
        return local
    }
    return network().also { if (it.verses.isNotEmpty()) persist(it) }
}
