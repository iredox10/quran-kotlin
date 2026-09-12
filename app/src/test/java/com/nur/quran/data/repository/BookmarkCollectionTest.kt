package com.nur.quran.data.repository

import com.nur.quran.data.db.entities.BookmarkEntity
import com.nur.quran.data.db.entities.CollectionEntity
import com.nur.quran.data.db.entities.CollectionItemEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BookmarkCollectionTest {

    @Test
    fun `collectionWithItems groups items by collectionId correctly`() {
        val col1 = CollectionEntity(id = 1L, name = "Daily Morning", createdAt = 1000L)
        val col2 = CollectionEntity(id = 2L, name = "Juz Amma Review", createdAt = 2000L)
        val collections = listOf(col1, col2)

        val item1 = CollectionItemEntity(collectionId = 1L, verseKey = "1:1", chapterId = 1, surahName = "Al-Fatihah")
        val item2 = CollectionItemEntity(collectionId = 1L, verseKey = "2:255", chapterId = 2, surahName = "Al-Baqarah")
        val item3 = CollectionItemEntity(collectionId = 2L, verseKey = "112:1", chapterId = 112, surahName = "Al-Ikhlas")
        val items = listOf(item1, item2, item3)

        val mapped = collections.map { col ->
            QuranRepository.CollectionWithItems(
                collection = col,
                items = items.filter { it.collectionId == col.id }
            )
        }

        assertEquals(2, mapped.size)
        assertEquals("Daily Morning", mapped[0].collection.name)
        assertEquals(2, mapped[0].items.size)
        assertEquals("1:1", mapped[0].items[0].verseKey)
        assertEquals("2:255", mapped[0].items[1].verseKey)

        assertEquals("Juz Amma Review", mapped[1].collection.name)
        assertEquals(1, mapped[1].items.size)
        assertEquals("112:1", mapped[1].items[0].verseKey)
    }

    @Test
    fun `empty collections have empty items list`() {
        val col = CollectionEntity(id = 10L, name = "Empty Collection", createdAt = 3000L)
        val withItems = QuranRepository.CollectionWithItems(
            collection = col,
            items = emptyList()
        )

        assertEquals(0, withItems.items.size)
        assertEquals("Empty Collection", withItems.collection.name)
    }

    @Test
    fun `bookmark hash code generates stable and distinct IDs`() {
        val key1 = "1:1"
        val key2 = "2:255"
        val key3 = "1:1"

        val bookmark1 = BookmarkEntity(id = key1.hashCode(), verseKey = key1, chapterId = 1, surahName = "Al-Fatihah")
        val bookmark2 = BookmarkEntity(id = key2.hashCode(), verseKey = key2, chapterId = 2, surahName = "Al-Baqarah")
        val bookmark3 = BookmarkEntity(id = key3.hashCode(), verseKey = key3, chapterId = 1, surahName = "Al-Fatihah")

        assertEquals(bookmark1.id, bookmark3.id)
        assertNotEquals(bookmark1.id, bookmark2.id)
    }
}
