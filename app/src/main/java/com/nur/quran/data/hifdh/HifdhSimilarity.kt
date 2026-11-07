package com.nur.quran.data.hifdh

import android.content.Context
import com.google.gson.JsonParser
import java.util.concurrent.ConcurrentHashMap

data class SimilarHifdhPassage(val verseKey: String, val text: String, val sharedWords: Set<String>)

/** Candidate discovery only: exact source verses remain untouched; user/listener confirms a confusion pair. */
class HifdhSimilarity(context: Context) {
    private val verses: Map<String, String> = context.assets.open("data/quran_full.json").bufferedReader().use {
        JsonParser.parseReader(it).asJsonArray.associate { row ->
            row.asJsonObject.get("verse_key").asString to row.asJsonObject.get("text_uthmani").asString
        }
    }
    private val tokens = verses.mapValues { tokenize(it.value) }
    private val inverted = buildMap<String, MutableSet<String>> {
        tokens.forEach { (key, words) -> words.distinct().forEach { word -> getOrPut(word) { mutableSetOf() }.add(key) } }
    }
    fun find(key: String): List<SimilarHifdhPassage> {
        val query = tokens[key].orEmpty()
        if (query.size < 3) return emptyList()
        val queryPairs = query.zipWithNext().toSet()
        val candidates = query.flatMap { inverted[it].orEmpty() }.toSet() - key
        return candidates.mapNotNull { candidate ->
            val words = tokens[candidate].orEmpty()
            val sharedPairs = queryPairs intersect words.zipWithNext().toSet()
            if (sharedPairs.size < 2) null else {
                val shared = sharedPairs.flatMap { listOf(it.first, it.second) }.toSet()
                val score = 2f * sharedPairs.size / (queryPairs.size + words.zipWithNext().toSet().size).coerceAtLeast(1)
                score to SimilarHifdhPassage(candidate, verses.getValue(candidate), shared)
            }
        }.sortedWith(compareByDescending<Pair<Float, SimilarHifdhPassage>> { it.first }.thenBy { it.second.verseKey }).take(5).map { it.second }
    }
    companion object {
        internal fun tokenize(text: String): List<String> = text.split(Regex("\\s+")).map {
            it.replace(Regex("[\\u0610-\\u061A\\u064B-\\u065F\\u0670\\u06D6-\\u06ED\\u0640]"), "")
                .replace('ٱ', 'ا').replace('أ', 'ا').replace('إ', 'ا').replace('آ', 'ا')
        }.filter { it.any { ch -> ch in '\u0621'..'\u064A' } }
    }
}
