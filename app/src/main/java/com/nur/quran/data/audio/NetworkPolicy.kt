package com.nur.quran.data.audio

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

/**
 * WiFi-only download policy for auto-downloads (auto-cache, packs).
 *
 * Streaming is unaffected — this gates AUTO downloads only.
 * All functions never throw.
 */
object NetworkPolicy {
    private const val PREFS_NAME = "net_policy"
    private const val KEY_WIFI_ONLY = "wifi_only"

    /** True when downloads should wait for unmetered WiFi. Default TRUE. */
    fun isWifiOnly(context: Context): Boolean {
        return try {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_WIFI_ONLY, true)
        } catch (_: Exception) {
            true
        }
    }

    fun setWifiOnly(context: Context, v: Boolean) {
        try {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_WIFI_ONLY, v)
                .apply()
        } catch (_: Exception) {
            // Never throws; preference write is best-effort.
        }
    }

    /** True when the active network is unmetered (API-safe). False on error. */
    fun isUnmetered(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = cm.activeNetwork ?: return false
                val caps = cm.getNetworkCapabilities(network) ?: return false
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
            } else {
                @Suppress("DEPRECATION")
                !cm.isActiveNetworkMetered
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Whether an AUTO download may proceed now.
     * Explicit user-tapped downloads bypass this; this gates AUTO only.
     * On any error returns false (safe with mobile data).
     */
    fun canAutoDownload(context: Context): Boolean {
        return try {
            if (!isWifiOnly(context)) return true
            isUnmetered(context)
        } catch (_: Exception) {
            false
        }
    }
}
