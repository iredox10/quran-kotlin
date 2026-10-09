package com.nur.quran.ui.screens

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The reader bottom dock (Prev / N-M / Next) reveals only once the verses
 * list is scrolled to its final layout item; scrolling up hides it again.
 * The dock itself is not a LazyColumn item, so totalItemsCount already
 * covers all content (status pill + optional headers + verses/page item).
 */
class ReaderDockVisibilityTest {

    @Test
    fun `hidden while last visible is not the final item`() {
        assertFalse(isReaderDockAtBottom(0, 8))
        assertFalse(isReaderDockAtBottom(6, 8))
    }

    @Test
    fun `revealed when last visible is the final item`() {
        assertTrue(isReaderDockAtBottom(7, 8))
    }

    @Test
    fun `revealed for single-item content already at bottom`() {
        assertTrue(isReaderDockAtBottom(0, 1))
    }

    @Test
    fun `hidden when nothing laid out yet`() {
        assertFalse(isReaderDockAtBottom(null, 5))
    }

    @Test
    fun `visible when no lazy content exists so nav is never stranded`() {
        assertTrue(isReaderDockAtBottom(null, 0))
    }
}
