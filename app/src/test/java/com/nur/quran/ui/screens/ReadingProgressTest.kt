package com.nur.quran.ui.screens

import com.nur.quran.data.db.entities.VerseEntity
import org.junit.Assert.*
import org.junit.Test

class ReadingProgressTest {
    @Test fun `both verse rows and continuous page rows update the reading position`() {
        val verses=listOf(VerseEntity(1,10,1,"10:1",null,null,null,208,11),VerseEntity(2,10,2,"10:2",null,null,null,209,11))
        assertEquals("10:2",readingVerseForItem(verses,2))
        assertEquals("10:2",readingVerseForItem(verses,"page_209"))
        assertNull(readingVerseForItem(verses,"header"))
        assertNull(readingVerseForItem(verses,"page_999"))
    }
}
