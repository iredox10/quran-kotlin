package com.nur.quran.data.mushaf

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

/** Reading edition is separate from Hafs planner/verse keys. Never coerce a native edition into Hafs. */
object ReaderMushafPreference {
    const val KEY = "preferred_reader_mushaf"
    fun preferences(context: Context): SharedPreferences = context.getSharedPreferences("hifdh_settings", Context.MODE_PRIVATE)
    fun isNative(id: String) = id in setOf("warsh", "qaloon", "indopak")
    fun selected(context: Context): String {
        val prefs = preferences(context)
        val saved = prefs.getString(KEY, null) ?: prefs.getString("mushaf_preset", Mushaf.DEFAULT.id)
        return when {
            saved == "indopak" && IndopakEdition.installed(context) -> saved
            saved == "warsh" || saved == "qaloon" -> saved!!
            else -> Mushaf.forReading(saved).id
        }
    }
    fun select(context: Context, id: String) {
        require(isNative(id) || Mushaf.ALL.any { it.id == id && it.isSelectable })
        require(id != "indopak" || IndopakEdition.installed(context))
        preferences(context).edit().putString(KEY, id).apply()
    }
}

@Composable
fun rememberReaderMushaf(): String {
    val context = LocalContext.current.applicationContext
    var selected by remember(context) { mutableStateOf(ReaderMushafPreference.selected(context)) }
    DisposableEffect(context) {
        val prefs = ReaderMushafPreference.preferences(context)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == ReaderMushafPreference.KEY || key == "mushaf_preset") selected = ReaderMushafPreference.selected(context)
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return selected
}
