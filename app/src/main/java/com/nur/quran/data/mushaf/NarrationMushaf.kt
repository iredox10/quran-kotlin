package com.nur.quran.data.mushaf

import android.content.Context
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import java.util.concurrent.ConcurrentHashMap

/** Native narration records; their verse keys must never be interpreted as Hafs keys. */
data class NarrationAyah(
    val id: Int,
    @SerializedName("sura_no") val surah: Int,
    @SerializedName("sura_name_en") val surahName: String,
    @SerializedName("aya_no") val number: Int,
    @SerializedName("aya_text") val text: String,
    val page: String,
) {
    val pages: List<Int> get() {
        val values = Regex("\\d+").findAll(page).map { it.value.toInt() }.toList()
        return if (values.size == 2) (values.first()..values.last()).toList() else values
    }
}

enum class NarrationMushaf(val id: String, val title: String, val fontName: String) {
    Warsh("warsh", "Warsh ‘an Nafi‘", "warsh-quran"),
    Qaloon("qaloon", "Qaloon ‘an Nafi‘", "qaloon-quran");

    fun ayahs(context: Context): List<NarrationAyah> = cache.getOrPut(id) {
        val type = object : TypeToken<List<NarrationAyah>>() {}.type
        context.assets.open("data/mushafs/$id.json").bufferedReader().use {
            val rows: List<NarrationAyah> = Gson().fromJson(it, type)
            check(rows.size == 6214 && rows.map { row -> row.surah }.toSet().size == 114)
            rows
        }
    }
    companion object { private val cache = ConcurrentHashMap<String, List<NarrationAyah>>() }
}
