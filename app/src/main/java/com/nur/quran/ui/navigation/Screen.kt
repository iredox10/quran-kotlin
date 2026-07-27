package com.nur.quran.ui.navigation

sealed class Screen(val route: String, val label: String) {
    object Quran : Screen("quran", "Quran")
    object Memorize : Screen("memorize", "Memorize")
    object Planner : Screen("planner", "Planner")
    object Analytics : Screen("analytics", "Analytics")
    object Profile : Screen("profile", "Profile")
    
    // Immersive detail screens (no bottom nav)
    object SurahDetail : Screen("surah/{chapterId}?verseKey={verseKey}", "Surah") {
        fun createRoute(chapterId: Int, verseKey: String? = null) =
            if (!verseKey.isNullOrBlank()) "surah/$chapterId?verseKey=$verseKey" else "surah/$chapterId"
    }
    object MemorizeDetail : Screen("memorize/{chapterId}", "Memorize") {
        fun createRoute(chapterId: Int) = "memorize/$chapterId"
    }
    object PageDetail : Screen("page/{pageNumber}", "Page") {
        fun createRoute(pageNumber: Int) = "page/$pageNumber"
    }
    object PlannerReaderDetail : Screen("planner_reader/{dayNumber}", "Planner Reader") {
        fun createRoute(dayNumber: Int) = "planner_reader/$dayNumber"
    }
}
