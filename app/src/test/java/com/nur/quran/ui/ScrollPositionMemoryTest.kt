package com.nur.quran.ui

import com.nur.quran.ui.screens.surahScrollPositions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for scroll position memory during Surah navigation.
 *
 * Verifies:
 * 1. The scroll positions map stores and retrieves (firstVisibleItemIndex, firstVisibleItemScrollOffset) by chapterId.
 * 2. Retrieving and removing returns the correct pair and leaves the map clean (one-time restore pattern).
 * 3. Module-level surahScrollPositions and ScrollPositionMemory stay synchronized.
 */
class ScrollPositionMemoryTest {

    @Before
    fun setUp() {
        ScrollPositionMemory.clear()
    }

    /**
     * Requirement 1:
     * Test that scroll positions map stores and retrieves
     * `(firstVisibleItemIndex, firstVisibleItemScrollOffset)` by `chapterId`.
     */
    @Test
    fun scrollPositionsMap_storesAndRetrievesByChapterId() {
        val chapterId = 1 // Surah Al-Fatihah
        val firstVisibleItemIndex = 5
        val firstVisibleItemScrollOffset = 120

        // Store into scroll positions map
        ScrollPositionMemory.scrollPositions[chapterId] = Pair(firstVisibleItemIndex, firstVisibleItemScrollOffset)

        // Retrieve from scroll positions map
        val retrieved = ScrollPositionMemory.scrollPositions[chapterId]

        assertNotNull("Retrieved scroll position should not be null", retrieved)
        assertEquals("firstVisibleItemIndex should match stored index", firstVisibleItemIndex, retrieved?.first)
        assertEquals("firstVisibleItemScrollOffset should match stored offset", firstVisibleItemScrollOffset, retrieved?.second)
    }

    /**
     * Requirement 1 (multiple chapters):
     * Verify independent chapter storage so navigation across multiple surahs preserves
     * separate scroll coordinates without cross-talk or overwrite.
     */
    @Test
    fun scrollPositionsMap_storesMultipleChaptersIndependently() {
        val fatihahId = 1
        val baqarahId = 2
        val yasinId = 36
        val nasId = 114

        ScrollPositionMemory.scrollPositions[fatihahId] = Pair(2, 40)
        ScrollPositionMemory.scrollPositions[baqarahId] = Pair(142, 350)
        ScrollPositionMemory.scrollPositions[yasinId] = Pair(45, 0)
        ScrollPositionMemory.scrollPositions[nasId] = Pair(0, 15)

        assertEquals(4, ScrollPositionMemory.scrollPositions.size)

        // Verify each surah retains its distinct coordinates
        assertEquals(Pair(2, 40), ScrollPositionMemory.scrollPositions[fatihahId])
        assertEquals(Pair(142, 350), ScrollPositionMemory.scrollPositions[baqarahId])
        assertEquals(Pair(45, 0), ScrollPositionMemory.scrollPositions[yasinId])
        assertEquals(Pair(0, 15), ScrollPositionMemory.scrollPositions[nasId])
    }

    /**
     * Requirement 2:
     * Test that retrieving and removing returns correct pair and leaves map clean.
     * This models the one-time restore pattern upon navigating back into a Surah.
     */
    @Test
    fun scrollPositionsMap_retrievingAndRemoving_returnsCorrectPairAndLeavesMapClean() {
        val chapterId = 55 // Surah Ar-Rahman
        val firstVisibleItemIndex = 39
        val firstVisibleItemScrollOffset = 85

        ScrollPositionMemory.scrollPositions[chapterId] = Pair(firstVisibleItemIndex, firstVisibleItemScrollOffset)
        assertEquals(1, ScrollPositionMemory.scrollPositions.size)

        // Simulate one-time restore: retrieve and remove
        val restored = ScrollPositionMemory.scrollPositions.remove(chapterId)

        assertNotNull("Restored position must not be null", restored)
        assertEquals("Restored index must match", firstVisibleItemIndex, restored?.first)
        assertEquals("Restored offset must match", firstVisibleItemScrollOffset, restored?.second)

        // Map must now be clean
        assertNull("Subsequent retrieval for chapterId must return null", ScrollPositionMemory.scrollPositions[chapterId])
        assertFalse("Map must not contain chapterId key", ScrollPositionMemory.scrollPositions.containsKey(chapterId))
        assertTrue("Map should be empty after removing single stored entry", ScrollPositionMemory.scrollPositions.isEmpty())
    }

    /**
     * Requirement 2 (partial consumption):
     * When multiple chapters have stored positions, restoring and removing one chapter
     * leaves the other chapters intact and only cleans the restored chapter.
     */
    @Test
    fun scrollPositionsMap_retrievingAndRemovingOneChapter_leavesOtherChaptersIntact() {
        ScrollPositionMemory.scrollPositions[18] = Pair(10, 50)  // Al-Kahf
        ScrollPositionMemory.scrollPositions[67] = Pair(25, 100) // Al-Mulk

        // Retrieve and remove Al-Kahf (18)
        val kahfPos = ScrollPositionMemory.scrollPositions.remove(18)
        assertEquals(Pair(10, 50), kahfPos)

        // Chapter 18 must be cleaned, chapter 67 must remain untouched
        assertNull(ScrollPositionMemory.scrollPositions[18])
        assertFalse(ScrollPositionMemory.scrollPositions.containsKey(18))
        assertEquals(Pair(25, 100), ScrollPositionMemory.scrollPositions[67])
        assertEquals(1, ScrollPositionMemory.scrollPositions.size)

        // Now retrieve and remove Al-Mulk (67)
        val mulkPos = ScrollPositionMemory.scrollPositions.remove(67)
        assertEquals(Pair(25, 100), mulkPos)
        assertTrue("Map must be completely clean", ScrollPositionMemory.scrollPositions.isEmpty())
    }

    /**
     * Test overwriting scroll position when user scrolls further before leaving.
     */
    @Test
    fun scrollPositionsMap_overwritingUpdatesToLatestCoordinates() {
        val chapterId = 12 // Surah Yusuf
        ScrollPositionMemory.scrollPositions[chapterId] = Pair(10, 20)
        assertEquals(Pair(10, 20), ScrollPositionMemory.scrollPositions[chapterId])

        // User scrolled further down
        ScrollPositionMemory.scrollPositions[chapterId] = Pair(40, 300)
        assertEquals(Pair(40, 300), ScrollPositionMemory.scrollPositions[chapterId])
        assertEquals(1, ScrollPositionMemory.scrollPositions.size)
    }

    /**
     * Test that non-existent chapter queries return null and do not affect the map.
     */
    @Test
    fun scrollPositionsMap_nonExistentChapter_returnsNullWithoutSideEffects() {
        assertNull(ScrollPositionMemory.scrollPositions[999])
        val removed = ScrollPositionMemory.scrollPositions.remove(999)
        assertNull(removed)
        assertTrue(ScrollPositionMemory.scrollPositions.isEmpty())
    }

    /**
     * Test ScrollPositionMemory helper methods (savePosition, getPosition, retrieveAndRemove, clear).
     */
    @Test
    fun scrollPositionMemory_helperMethodsOperateCorrectly() {
        ScrollPositionMemory.savePosition(chapterId = 2, firstVisibleItemIndex = 255, firstVisibleItemScrollOffset = 80)

        // getPosition without removing
        val peek = ScrollPositionMemory.getPosition(chapterId = 2)
        assertEquals(Pair(255, 80), peek)
        assertEquals(1, ScrollPositionMemory.scrollPositions.size)

        // retrieveAndRemove
        val removed = ScrollPositionMemory.retrieveAndRemove(chapterId = 2)
        assertEquals(Pair(255, 80), removed)
        assertNull(ScrollPositionMemory.getPosition(chapterId = 2))
        assertTrue(ScrollPositionMemory.scrollPositions.isEmpty())

        // save again and clear
        ScrollPositionMemory.savePosition(chapterId = 3, firstVisibleItemIndex = 1, firstVisibleItemScrollOffset = 0)
        assertFalse(ScrollPositionMemory.scrollPositions.isEmpty())
        ScrollPositionMemory.clear()
        assertTrue(ScrollPositionMemory.scrollPositions.isEmpty())
    }

    /**
     * Verify that SurahScreen's surahScrollPositions references the exact same map,
     * ensuring navigation lifecycle in SurahScreen saves and restores seamlessly.
     */
    @Test
    fun surahScrollPositions_isSynchronizedWithScrollPositionMemory() {
        surahScrollPositions[1] = Pair(7, 50)

        // Should be visible through ScrollPositionMemory
        assertEquals(Pair(7, 50), ScrollPositionMemory.scrollPositions[1])
        assertEquals(Pair(7, 50), ScrollPositionMemory.getPosition(1))

        // Removing via surahScrollPositions cleans ScrollPositionMemory
        val removed = surahScrollPositions.remove(1)
        assertEquals(Pair(7, 50), removed)
        assertNull(ScrollPositionMemory.getPosition(1))
        assertTrue(ScrollPositionMemory.scrollPositions.isEmpty())
    }
}
