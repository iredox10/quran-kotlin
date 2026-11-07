package com.nur.quran.data.mushaf

import android.content.Context
import com.google.gson.Gson
import java.util.concurrent.ConcurrentHashMap

data class NativeBrowseChapter(val id: Int, val page: Int, val count: Int)
data class NativeBrowseDivision(val id: Int, val page: Int, val surah: Int)
data class NativeBrowseIndex(
    val firstPage: Int, val lastPage: Int, val chapters: List<NativeBrowseChapter>,
    val juz: List<NativeBrowseDivision>, val hizb: List<NativeBrowseDivision>
) {
    companion object {
        private val cache = ConcurrentHashMap<String, NativeBrowseIndex>()
        fun load(context: Context, edition: String): NativeBrowseIndex? {
            if (!ReaderMushafPreference.isNative(edition)) return null
            return cache.getOrPut(edition) {
                context.assets.open("data/mushafs/${edition}_browse.json").bufferedReader().use {
                    Gson().fromJson(it, NativeBrowseIndex::class.java)
                }.also { check(it.chapters.size == 114 && it.juz.size == 30 && it.hizb.size == 60) }
            }
        }
    }
}
