package com.nur.quran.ui.components.profile

/**
 * Profile page formatting helpers matching web app Profile.jsx lines 26-40 and 128-135.
 */
object ProfileUtils {

    /**
     * Formats reading seconds into clean human-readable duration matching web formatMinutes:
     * - < 60 mins: "${m}m"
     * - >= 60 mins with remainder: "${h}h ${r}m"
     * - >= 60 mins without remainder: "${h}h"
     */
    fun formatMinutes(seconds: Long): String {
        val m = Math.round(seconds / 60.0).toInt()
        if (m < 60) return "${m}m"
        val h = m / 60
        val r = m % 60
        return if (r > 0) "${h}h ${r}m" else "${h}h"
    }

    /**
     * Formats last sync timestamp to human readable relative time matching web timeAgo:
     * - null / 0: "Never"
     * - < 1 min: "Just now"
     * - < 60 min: "${m}m ago"
     * - < 24 hrs: "${h}h ago"
     * - >= 24 hrs: "${d}d ago"
     */
    fun timeAgo(timestamp: Long?, now: Long = System.currentTimeMillis()): String {
        if (timestamp == null || timestamp <= 0) return "Never"
        val diffMs = now - timestamp
        if (diffMs < 0) return "Just now"
        val m = (diffMs / 60000L).toInt()
        if (m < 1) return "Just now"
        if (m < 60) return "${m}m ago"
        val h = m / 60
        if (h < 24) return "${h}h ago"
        val d = h / 24
        return "${d}d ago"
    }

    /**
     * Generates up to 2 uppercase initials for avatar badge matching web getInitials.
     */
    fun getInitials(nameOrEmail: String?): String {
        if (nameOrEmail.isNullOrBlank()) return "?"
        val clean = nameOrEmail.substringBefore("@").trim()
        val parts = clean.split(" ", "_", ".").filter { it.isNotBlank() }
        return if (parts.size >= 2) {
            "${parts[0].first()}${parts[1].first()}".uppercase()
        } else {
            clean.take(2).uppercase()
        }
    }

    /**
     * Time-of-day greeting matching web greeting.
     */
    fun getGreeting(hour: Int = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)): String = when {
        hour < 12 -> "Good Morning"
        hour < 17 -> "Good Afternoon"
        else -> "Good Evening"
    }
}
