package com.nur.quran.data.hifdh

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Persistence for hifdh (memorization) state, mirroring the web app's
 * `hifdhHistory`, `transitionLinks` and `hifdhGoals` stores.
 * History and goals are kept as JSON strings in the shared `hifdh_settings` prefs.
 */
class HifdhStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("hifdh_settings", Context.MODE_PRIVATE)

    private val gson = Gson()

    private val historyType = object : TypeToken<Map<String, HifdhHistoryEntry>>() {}.type
    private val goalsType = object : TypeToken<List<HifdhGoal>>() {}.type

    fun loadHifdhHistory(): Map<String, HifdhHistoryEntry> {
        val raw = prefs.getString(KEY_HISTORY, null) ?: return emptyMap()
        return runCatching {
            gson.fromJson<Map<String, HifdhHistoryEntry>>(raw, historyType)
        }.getOrNull() ?: emptyMap()
    }

    fun saveHifdhHistory(history: Map<String, HifdhHistoryEntry>) {
        prefs.edit().putString(KEY_HISTORY, gson.toJson(history)).apply()
    }

    fun loadTransitionLinks(): Set<String> =
        prefs.getStringSet(KEY_TRANSITION_LINKS, null)?.toSet() ?: emptySet()

    fun saveTransitionLinks(links: Set<String>) {
        prefs.edit().putStringSet(KEY_TRANSITION_LINKS, links).apply()
    }

    fun loadHifdhGoals(): List<HifdhGoal> {
        val raw = prefs.getString(KEY_GOALS, null) ?: return emptyList()
        return runCatching {
            gson.fromJson<List<HifdhGoal>>(raw, goalsType)
        }.getOrNull() ?: emptyList()
    }

    fun saveHifdhGoals(goals: List<HifdhGoal>) {
        prefs.edit().putString(KEY_GOALS, gson.toJson(goals)).apply()
    }

    private companion object {
        const val KEY_HISTORY = "hifdh_history"
        const val KEY_TRANSITION_LINKS = "transition_links"
        const val KEY_GOALS = "hifdh_goals"
    }
}
