package com.nur.quran.data.mushaf

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class IndopakSurah(val surah: Int, val name: String, val page: Int)
object IndopakNavigation {
    @Volatile private var cached: List<IndopakSurah>? = null
    @Synchronized fun surahs(context: Context): List<IndopakSurah> = cached ?: context.assets
        .open("data/mushafs/indopak_navigation.json").bufferedReader().use {
            Gson().fromJson<List<IndopakSurah>>(it, object : TypeToken<List<IndopakSurah>>() {}.type)
        }.also { check(it.size == 114); cached = it }
}
