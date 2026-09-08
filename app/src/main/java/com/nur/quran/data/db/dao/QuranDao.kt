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

    @Query("SELECT * FROM chapters ORDER BY id ASC")
    suspend fun getAllChaptersDirect(): List<ChapterEntity>

    @Query("SELECT * FROM chapters WHERE id = :chapterId LIMIT 1")
    suspend fun getChapterById(chapterId: Int): ChapterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapters(chapters: List<ChapterEntity>)

    // Verses
    @Query("SELECT * FROM verses WHERE chapterId = :chapterId ORDER BY verseNumber ASC")
    fun getVersesByChapter(chapterId: Int): Flow<List<VerseEntity>>

    @Query("SELECT * FROM verses WHERE chapterId = :chapterId ORDER BY verseNumber ASC")
    suspend fun getVersesByChapterDirect(chapterId: Int): List<VerseEntity>

    @Query("SELECT * FROM verses WHERE pageNumber = :pageNumber ORDER BY chapterId ASC, verseNumber ASC")
    fun getVersesByPage(pageNumber: Int): Flow<List<VerseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVerses(verses: List<VerseEntity>)

    @Transaction
    suspend fun insertVersesAndWords(verses: List<VerseEntity>, words: List<WordEntity>) {
        insertVerses(verses)
        insertWords(words)
    }

    @Query("DELETE FROM words WHERE verseId IN (:verseIds)")
    suspend fun deleteWordsForVerses(verseIds: List<Int>)

    /**
     * Network refresh setter: deletes stale words (e.g. offline synthetic rows
     * with different PKs) before inserting, so a verse never renders
     * duplicated words / double ayah end markers.
     */
    @Transaction
    suspend fun replaceVersesAndWords(verses: List<VerseEntity>, words: List<WordEntity>) {
        if (verses.isNotEmpty()) deleteWordsForVerses(verses.map { it.id })
        insertVerses(verses)
        insertWords(words)
    }

    // Words
    @Query("SELECT * FROM words WHERE verseId = :verseId ORDER BY position ASC")
    suspend fun getWordsForVerse(verseId: Int): List<WordEntity>

    @Query("SELECT * FROM words WHERE verseId IN (:verseIds) ORDER BY verseId ASC, position ASC")
    suspend fun getWordsForVerses(verseIds: List<Int>): List<WordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWords(words: List<WordEntity>)

    // Bookmarks
    @Query("SELECT * FROM bookmarks ORDER BY timestamp DESC")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>

    @Query("SELECT verseKey FROM bookmarks")
    fun getBookmarkedVerseKeys(): Flow<List<String>>

    @Query("SELECT * FROM bookmarks WHERE verseKey = :verseKey LIMIT 1")
    suspend fun getBookmarkByVerse(verseKey: String): BookmarkEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE verseKey = :verseKey")
    suspend fun deleteBookmark(verseKey: String)

    @Query("DELETE FROM bookmarks")
    suspend fun deleteAllBookmarks()

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

    @Query("SELECT * FROM collection_items")
    fun getAllCollectionItems(): Flow<List<CollectionItemEntity>>

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

    // Verses by key (hifdh test modal lookups)
    @Query("SELECT * FROM verses WHERE verseKey IN (:keys)")
    suspend fun getVersesByKey(keys: List<String>): List<VerseEntity>

    // Reading Sessions
    @Query("SELECT * FROM reading_sessions ORDER BY timestamp ASC")
    fun getAllReadingSessions(): Flow<List<ReadingSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReadingSession(session: ReadingSessionEntity)

    // Keep only the newest 500 sessions (matches the web store's slice(-500))
    @Query("DELETE FROM reading_sessions WHERE id NOT IN (SELECT id FROM reading_sessions ORDER BY timestamp DESC LIMIT 500)")
    suspend fun pruneReadingSessions()

    // Recently Read
    @Query("SELECT * FROM recently_read ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentlyRead(limit: Int = 5): Flow<List<RecentlyReadEntity>>

    @Query("SELECT * FROM recently_read WHERE chapterId = :chapterId LIMIT 1")
    suspend fun getRecentlyReadForChapter(chapterId: Int): RecentlyReadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecentlyRead(item: RecentlyReadEntity)

    // Keep only the newest 5 entries (matches the web store's slice(0, 5))
    @Query("DELETE FROM recently_read WHERE chapterId NOT IN (SELECT chapterId FROM recently_read ORDER BY timestamp DESC LIMIT 5)")
    suspend fun pruneRecentlyRead()

    // Latest bookmark (web app shows a single active bookmark card on Home)
    @Query("SELECT * FROM bookmarks ORDER BY timestamp DESC LIMIT 1")
    fun getLatestBookmark(): Flow<BookmarkEntity?>
}
