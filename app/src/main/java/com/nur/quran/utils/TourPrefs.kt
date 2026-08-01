package com.nur.quran.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Lightweight persistence for onboarding UI state (tours, coachmarks, page visits),
 * mirroring the web app's zustand-persisted keys in localStorage.
 */
class TourPrefs(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("tour_prefs", Context.MODE_PRIVATE)

    fun isTourCompleted(id: String): Boolean = prefs.getBoolean("tour_$id", false)

    fun completeTour(id: String) {
        prefs.edit().putBoolean("tour_$id", true).apply()
    }

    fun isCoachmarkDismissed(id: String): Boolean = prefs.getBoolean("coachmark_$id", false)

    fun dismissCoachmark(id: String) {
        prefs.edit().putBoolean("coachmark_$id", true).apply()
    }

    fun pageVisits(pageId: String): Int = prefs.getInt("visit_$pageId", 0)

    fun incrementPageVisit(pageId: String) {
        prefs.edit().putInt("visit_$pageId", pageVisits(pageId) + 1).apply()
    }

    fun isOnboardingDismissed(): Boolean = prefs.getBoolean("onboarding_dismissed", false)

    fun dismissOnboarding() {
        prefs.edit().putBoolean("onboarding_dismissed", true).apply()
    }
}
