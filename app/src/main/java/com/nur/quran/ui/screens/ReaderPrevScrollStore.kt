package com.nur.quran.ui.screens

/**
 * Per-page scroll positions for PlannerReader backward navigation.
 *
 * Stores `(firstVisibleItemIndex, firstVisibleItemScrollOffset)` keyed by
 * page number so Prev restores where the user was. Pure Kotlin (no Compose
 * or Android deps) for JVM unit testing.
 */
object ReaderPrevScrollStore {

    private val positions = mutableMapOf<Int, Pair<Int, Int>>()

    /** Saves the scroll position for [page], overwriting any earlier entry. */
    fun save(page: Int, firstVisibleItemIndex: Int, firstVisibleItemScrollOffset: Int) {
        positions[page] = Pair(firstVisibleItemIndex, firstVisibleItemScrollOffset)
    }

    /** Peeks at the saved position for [page] (null when never visited). */
    fun get(page: Int): Pair<Int, Int>? = positions[page]

    /** One-time restore: returns and removes the saved position for [page]. */
    fun take(page: Int): Pair<Int, Int>? = positions.remove(page)

    /** Clears all saved positions. */
    fun clear() {
        positions.clear()
    }
}
