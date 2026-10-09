package com.nur.quran.data.hifdh

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject

/**
 * Persistence for hifdh (memorization) state, mirroring the web app's
 * `hifdhHistory`, `transitionLinks` and `hifdhGoals` stores.
 * History and goals are kept as JSON strings in the shared `hifdh_settings` prefs.
 *
 * Both are written in the web's own shape (snake_case ts-fsrs cards,
 * `"YYYY-MM-DD"` targetDates, numeric goal ids) and read back per entry, so a
 * synced payload loads as-is and a single bad entry cannot wipe the store.
 * See the web-compatibility block in FsrsScheduler.kt.
 */
class HifdhStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("hifdh_settings", Context.MODE_PRIVATE)

    private val gson = Gson()

    fun loadHifdhHistory(): Map<String, HifdhHistoryEntry> {
        val raw = prefs.getString(KEY_HISTORY, null) ?: return emptyMap()
        return parseWebHistoryMap(parseJsonElement(raw))
    }

    fun saveHifdhHistory(history: Map<String, HifdhHistoryEntry>) {
        val json = JsonObject()
        history.forEach { (key, entry) -> json.add(key, entry.toWebJson()) }
        prefs.edit().putString(KEY_HISTORY, json.toString()).apply()
    }

    fun loadTransitionLinks(): Set<String> =
        prefs.getStringSet(KEY_TRANSITION_LINKS, null)?.toSet() ?: emptySet()

    fun saveTransitionLinks(links: Set<String>) {
        prefs.edit().putStringSet(KEY_TRANSITION_LINKS, links).apply()
    }

    fun loadHifdhGoals(): List<HifdhGoal> {
        val raw = prefs.getString(KEY_GOALS, null) ?: return emptyList()
        return parseWebGoalList(parseJsonElement(raw))
    }

    fun saveHifdhGoals(goals: List<HifdhGoal>) {
        val json = JsonArray()
        goals.forEach { json.add(it.toWebJson()) }
        prefs.edit().putString(KEY_GOALS, json.toString()).apply()
    }

    /** Web `memorizedSurahs` (chapter ids) — persisted as a string set like `memorized_ayahs`. */
    fun loadMemorizedSurahs(): Set<Int> =
        prefs.getStringSet(KEY_MEMORIZED_SURAHS, null)
            ?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet()

    fun saveMemorizedSurahs(surahs: Set<Int>) {
        prefs.edit().putStringSet(KEY_MEMORIZED_SURAHS, surahs.map { it.toString() }.toSet())
            .apply()
    }

    /** Unparseable JSON degrades to null so callers fall back to empty state. */
    private fun parseJsonElement(raw: String): JsonElement? =
        runCatching { gson.fromJson(raw, JsonElement::class.java) }.getOrNull()

    private companion object {
        const val KEY_HISTORY = "hifdh_history"
        const val KEY_TRANSITION_LINKS = "transition_links"
        const val KEY_GOALS = "hifdh_goals"
        const val KEY_MEMORIZED_SURAHS = "memorized_surahs"
    }
}
