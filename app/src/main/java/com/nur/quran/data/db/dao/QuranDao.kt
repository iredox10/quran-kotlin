package com.nur.quran.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.nur.quran.data.db.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface QuranDao {

    // Chapters
    @Query("SELECT * FROM chapters ORDER BY id ASC")
    fun getAllChapters(): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM chapters WHERE id = :chapterId LIMIT 1")
    suspend fun getChapterById(chapterId: Int): ChapterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapters(chapters: List<ChapterEntity>)

    // Verses
    @Query("SELECT * FROM verses WHERE chapterId = :chapterId ORDER BY verseNumber ASC")
    fun getVersesByChapter(chapterId: Int): Flow<List<VerseEntity>>

    @Query("SELECT * FROM verses WHERE pageNumber = :pageNumber ORDER BY chapterId ASC, verseNumber ASC")
    fun getVersesByPage(pageNumber: Int): Flow<List<VerseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVerses(verses: List<VerseEntity>)

    // Words
    @Query("SELECT * FROM words WHERE verseId = :verseId ORDER BY position ASC")
    suspend fun getWordsForVerse(verseId: Int): List<WordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWords(words: List<WordEntity>)

    // Bookmarks
    @Query("SELECT * FROM bookmarks ORDER BY timestamp DESC")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE verseKey = :verseKey LIMIT 1")
    suspend fun getBookmarkByVerse(verseKey: String): BookmarkEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE verseKey = :verseKey")
    suspend fun deleteBookmark(verseKey: String)

    // Collections
    @Query("SELECT * FROM collections ORDER BY createdAt DESC")
    fun getAllCollections(): Flow<List<CollectionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollection(collection: CollectionEntity)

    @Query("DELETE FROM collections WHERE id = :collectionId")
    suspend fun deleteCollection(collectionId: Long)

    // Collection Items
    @Query("SELECT * FROM collection_items WHERE collectionId = :collectionId ORDER BY addedAt DESC")
    fun getItemsForCollection(collectionId: Long): Flow<List<CollectionItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollectionItem(item: CollectionItemEntity)

    @Query("DELETE FROM collection_items WHERE collectionId = :collectionId AND verseKey = :verseKey")
    suspend fun deleteCollectionItem(collectionId: Long, verseKey: String)

    // Key-Value API Response Cache (Dexie Equivalent)
    @Query("SELECT * FROM api_responses WHERE `key` = :key LIMIT 1")
    suspend fun getCacheEntry(key: String): ApiResponseCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCacheEntry(entry: ApiResponseCacheEntity)

    @Query("DELETE FROM api_responses WHERE `key` LIKE :prefix || '%'")
    suspend fun clearCacheByPrefix(prefix: String)
}
