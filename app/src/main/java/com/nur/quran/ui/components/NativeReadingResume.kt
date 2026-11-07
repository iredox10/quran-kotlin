package com.nur.quran.ui.components

import android.content.SharedPreferences
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.nur.quran.data.db.entities.RecentlyReadEntity
import com.nur.quran.data.mushaf.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun rememberNativeReadingResume(editionId: String): RecentlyReadEntity? {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("NarrationReader", 0) }
    var page by remember(editionId) { mutableIntStateOf(prefs.getInt(editionId, if (editionId == "indopak") 2 else 1)) }
    DisposableEffect(prefs, editionId) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == editionId) page = prefs.getInt(editionId, if (editionId == "indopak") 2 else 1)
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    val item by produceState<RecentlyReadEntity?>(null, editionId, page) {
        value = withContext(Dispatchers.IO) {
            val timestamp = prefs.getLong("${editionId}_timestamp", System.currentTimeMillis())
            when (editionId) {
                "warsh", "qaloon" -> NarrationMushaf.entries.first { it.id == editionId }.ayahs(context)
                    .firstOrNull { page in it.pages }?.let { RecentlyReadEntity(it.surah, it.surahName, timestamp = timestamp, pageNumber = page) }
                "indopak" -> IndopakNavigation.surahs(context).lastOrNull { it.page <= page }
                    ?.let { RecentlyReadEntity(it.surah, it.name, timestamp = timestamp, pageNumber = page) }
                else -> null
            }
        }
    }
    return item
}
