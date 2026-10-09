package com.nur.quran.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/** JVM unit tests for [ReaderPrevScrollStore]: save/restore/default, keyed by page. */
class ReaderPrevScrollStoreTest {

    @Before
    fun setUp() {
        ReaderPrevScrollStore.clear()
    }

    @Test
    fun saveAndGet_roundTripsIndexAndOffset() {
        ReaderPrevScrollStore.save(page = 5, firstVisibleItemIndex = 7, firstVisibleItemScrollOffset = 120)
        assertEquals(Pair(7, 120), ReaderPrevScrollStore.get(5))
    }

    @Test
    fun get_unknownPage_returnsNull() {
        assertNull(ReaderPrevScrollStore.get(999))
    }

    @Test
    fun take_returnsPositionAndRemovesIt() {
        ReaderPrevScrollStore.save(page = 5, firstVisibleItemIndex = 3, firstVisibleItemScrollOffset = 40)
        assertEquals(Pair(3, 40), ReaderPrevScrollStore.take(5))
        assertNull("one-time restore must leave the entry clean", ReaderPrevScrollStore.get(5))
    }

    @Test
    fun pages_areKeyedIndependently() {
        ReaderPrevScrollStore.save(page = 4, firstVisibleItemIndex = 2, firstVisibleItemScrollOffset = 10)
        ReaderPrevScrollStore.save(page = 6, firstVisibleItemIndex = 9, firstVisibleItemScrollOffset = 300)
        assertEquals(Pair(9, 300), ReaderPrevScrollStore.take(6))
        assertEquals("other pages stay intact", Pair(2, 10), ReaderPrevScrollStore.get(4))
    }

    @Test
    fun save_overwritesWithLatestPosition() {
        ReaderPrevScrollStore.save(page = 5, firstVisibleItemIndex = 1, firstVisibleItemScrollOffset = 0)
        ReaderPrevScrollStore.save(page = 5, firstVisibleItemIndex = 8, firstVisibleItemScrollOffset = 55)
        assertEquals(Pair(8, 55), ReaderPrevScrollStore.get(5))
    }
}
