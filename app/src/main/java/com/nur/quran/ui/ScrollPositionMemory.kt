package com.nur.quran.ui

/**
 * In-memory cache for preserving scroll positions across Surah navigation.
 *
 * Stores `(firstVisibleItemIndex, firstVisibleItemScrollOffset)` keyed by `chapterId`
 * so that when a user leaves a Surah and returns, their exact scroll position
 * is restored and then removed (one-time restore pattern).
 */
object ScrollPositionMemory {

    /**
     * Map storing `(firstVisibleItemIndex, firstVisibleItemScrollOffset)` by `chapterId`.
     */
    val scrollPositions: MutableMap<Int, Pair<Int, Int>> = mutableMapOf()

    /**
     * Saves the scroll position for the given [chapterId].
     */
    fun savePosition(chapterId: Int, firstVisibleItemIndex: Int, firstVisibleItemScrollOffset: Int) {
        scrollPositions[chapterId] = Pair(firstVisibleItemIndex, firstVisibleItemScrollOffset)
    }

    /**
     * Retrieves the saved scroll position for [chapterId] without removing it.
     */
    fun getPosition(chapterId: Int): Pair<Int, Int>? {
        return scrollPositions[chapterId]
    }

    /**
     * Retrieves and removes the saved scroll position for [chapterId],
     * implementing a one-time restore that leaves the map clean.
     */
    fun retrieveAndRemove(chapterId: Int): Pair<Int, Int>? {
        return scrollPositions.remove(chapterId)
    }

    /**
     * Clears all saved scroll positions.
     */
    fun clear() {
        scrollPositions.clear()
    }
}
