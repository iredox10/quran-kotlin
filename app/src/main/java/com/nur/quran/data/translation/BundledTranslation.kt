package com.nur.quran.data.translation

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/** Saheeh International ships with the app, including on a first offline launch. */
object BundledTranslation {
    const val ID = 20
    @Volatile private var cached: Map<String, String>? = null
    @Synchronized fun texts(context: Context): Map<String, String> {
        cached?.let { return it }
        val type = object : TypeToken<Map<String, String>>() {}.type
        val rows: Map<String, String> = context.assets.open("data/translation_saheeh.json").bufferedReader().use {
            Gson().fromJson(it, type)
        }
        check(rows.size == 6236 && rows.values.all { it.isNotBlank() }) { "Bundled Saheeh International is incomplete" }
        cached = rows
        return rows
    }
}
