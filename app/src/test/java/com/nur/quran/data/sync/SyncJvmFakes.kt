package com.nur.quran.data.sync

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences

/**
 * In-memory `SharedPreferences` for the hifzh_settings / Settings files the sync engine
 * reads and writes. The unit-test `android.jar` is the mockable one, so every real
 * framework call would be a no-op; a real map keeps `contains` honest, which matters
 * because a snapshot must only carry keys a device actually stored.
 */
internal class FakePrefs : SharedPreferences {
    val values: MutableMap<String, Any?> = mutableMapOf()

    override fun getAll(): MutableMap<String, *> = values

    override fun getString(key: String, defValue: String?): String? = values[key] as? String ?: defValue

    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(key: String, defValue: MutableSet<String>?): MutableSet<String>? =
        values[key] as? MutableSet<String> ?: defValue

    override fun getInt(key: String, defValue: Int): Int = values[key] as? Int ?: defValue

    override fun getLong(key: String, defValue: Long): Long = values[key] as? Long ?: defValue

    override fun getFloat(key: String, defValue: Float): Float = values[key] as? Float ?: defValue

    override fun getBoolean(key: String, defValue: Boolean): Boolean = values[key] as? Boolean ?: defValue

    override fun contains(key: String): Boolean = values.containsKey(key)

    override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
        override fun putString(key: String, value: String?) = apply { values[key] = value }
        override fun putStringSet(key: String, value: MutableSet<String>?) = apply { values[key] = value }
        override fun putInt(key: String, value: Int) = apply { values[key] = value }
        override fun putLong(key: String, value: Long) = apply { values[key] = value }
        override fun putFloat(key: String, value: Float) = apply { values[key] = value }
        override fun putBoolean(key: String, value: Boolean) = apply { values[key] = value }
        override fun remove(key: String) = apply { values.remove(key) }
        override fun clear() = apply { values.clear() }
        override fun commit(): Boolean = true
        override fun apply() = Unit
    }

    override fun registerOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?,
    ) = Unit

    override fun unregisterOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?,
    ) = Unit
}

/**
 * Android's `Context` has ~100 abstract members and no usable test double on the unit-test
 * classpath, so the harness wraps `ContextWrapper` and answers only `getSharedPreferences`.
 */
internal class FakePrefsContext(private val prefs: SharedPreferences) : ContextWrapper(null) {
    override fun getSharedPreferences(name: String, mode: Int): SharedPreferences = prefs
}
