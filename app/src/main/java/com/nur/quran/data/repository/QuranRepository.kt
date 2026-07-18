package com.nur.quran.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nur.quran.data.api.QuranApi
import com.nur.quran.data.db.dao.QuranDao
import com.nur.quran.data.db.entities.ApiResponseCacheEntity
import com.nur.quran.data.db.entities.BookmarkEntity
import com.nur.quran.data.db.entities.ChapterEntity
import com.nur.quran.data.db.entities.CollectionEntity
import com.nur.quran.data.db.entities.CollectionItemEntity
import com.nur.quran.data.db.entities.ReadingSessionEntity
import com.nur.quran.data.db.entities.RecentlyReadEntity
import com.nur.quran.data.db.entities.VerseEntity
import com.nur.quran.data.db.entities.WordEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuranRepository @Inject constructor(
    private val quranDao: QuranDao,
    private val quranApi: QuranApi,
    private val gson: Gson
) {

    // Helper to cache API responses in key-value table
    private suspend inline fun <reified T> fetchWithOfflineCache(
        cacheKey: String,
        crossinline apiCall: suspend () -> T
    ): T = withContext(Dispatchers.IO) {
        try {
            val networkResult = apiCall()
            val jsonString = gson.toJson(networkResult)
            quranDao.insertCacheEntry(ApiResponseCacheEntity(cacheKey, jsonString))
            networkResult
        } catch (e: Exception) {
            val cachedEntry = quranDao.getCacheEntry(cacheKey)
            if (cachedEntry != null) {
                val type = object : TypeToken<T>() {}.type
                gson.fromJson<T>(cachedEntry.dataJson, type)
            } else {
                throw e
            }
        }
    }

    // Chapters
    fun getChaptersFlow(): Flow<List<ChapterEntity>> = quranDao.getAllChapters()

    suspend fun refreshChapters() = withContext(Dispatchers.IO) {
        val response = quranApi.getChapters()
        val entities = response.chapters.map { apiChapter ->
            ChapterEntity(
                id = apiChapter.id,
                nameSimple = apiChapter.name_simple,
                nameArabic = apiChapter.name_arabic,
                nameComplex = apiChapter.name_complex,
                translatedName = apiChapter.translated_name?.name ?: "",
                revelationPlace = apiChapter.revelation_place,
                revelationOrder = apiChapter.revelation_order,
                versesCount = apiChapter.verses_count,
                pagesStart = apiChapter.pages.firstOrNull() ?: 1,
                pagesEnd = apiChapter.pages.lastOrNull() ?: 1
            )
        }
        quranDao.insertChapters(entities)
    }

    // Verses by Chapter
    fun getVersesByChapterFlow(chapterId: Int): Flow<List<VerseEntity>> = quranDao.getVersesByChapter(chapterId)

    suspend fun refreshVersesByChapter(chapterId: Int, translationId: Int) = withContext(Dispatchers.IO) {
        val response = quranApi.getVersesByChapter(
            chapterId = chapterId,
            translations = translationId.toString(),
            fields = "text_uthmani,text_indopak,text_qpc_hafs",
            wordFields = "text_uthmani,text_indopak,text_qpc_hafs,text_uthmani_tajweed,translation,transliteration",
            perPage = 300
        )
        
        val verseEntities = mutableListOf<VerseEntity>()
        val wordEntities = mutableListOf<WordEntity>()

        for (apiVerse in response.verses) {
            val verseEntity = VerseEntity(
                id = apiVerse.id,
                chapterId = chapterId,
                verseNumber = apiVerse.verse_number,
                verseKey = apiVerse.verse_key,
                textUthmani = apiVerse.words?.joinToString(" ") { it.text_uthmani ?: "" } ?: "",
                textIndopak = apiVerse.words?.joinToString(" ") { it.text_indopak ?: "" } ?: "",
                textQpcHafs = apiVerse.words?.joinToString(" ") { it.text_qpc_hafs ?: "" } ?: "",
                pageNumber = apiVerse.page_number,
                juzNumber = apiVerse.juz_number,
                translation = apiVerse.translations?.firstOrNull()?.text,
                audioUrl = apiVerse.audio?.url
            )
            verseEntities.add(verseEntity)

            apiVerse.words?.forEach { apiWord ->
                wordEntities.add(
                    WordEntity(
                        id = apiWord.id,
                        verseId = apiVerse.id,
                        position = apiWord.position,
                        textUthmani = apiWord.text_uthmani,
                        textIndopak = apiWord.text_indopak,
                        textQpcHafs = apiWord.text_qpc_hafs,
                        textUthmaniTajweed = apiWord.text_uthmani_tajweed,
                        translation = apiWord.translation?.text,
                        transliteration = apiWord.transliteration?.text,
                        charTypeName = apiWord.char_type_name
                    )
                )
            }
        }

        quranDao.insertVersesAndWords(verseEntities, wordEntities)
    }

    // Words
    suspend fun getWordsForVerse(verseId: Int): List<WordEntity> = quranDao.getWordsForVerse(verseId)

    // Tajweed HTML
    suspend fun getTajweedHtmlForChapter(chapterId: Int) = fetchWithOfflineCache("tajweed_chapter_$chapterId") {
        quranApi.getUthmaniTajweed(chapterNumber = chapterId)
    }

    // Tafsir
    suspend fun getTafsirForChapter(tafsirId: Int, chapterId: Int) = fetchWithOfflineCache("tafsir_${tafsirId}_chapter_$chapterId") {
        quranApi.getTafsirByChapter(tafsirId, chapterId)
    }

    // Footnote
    suspend fun getFootnote(footnoteId: String) = fetchWithOfflineCache("footnote_$footnoteId") {
        quranApi.getFootnote(footnoteId)
    }

    // Bookmarks
    fun getBookmarkedVerseKeysFlow(): Flow<List<String>> = quranDao.getBookmarkedVerseKeys()

    suspend fun insertBookmark(bookmark: BookmarkEntity) {
        quranDao.insertBookmark(bookmark)
    }

    suspend fun deleteBookmark(verseKey: String) {
        quranDao.deleteBookmark(verseKey)
    }

    // Latest bookmark (for the Home bookmark card)
    fun getLatestBookmarkFlow(): Flow<BookmarkEntity?> = quranDao.getLatestBookmark()

    // Reading Sessions
    fun getReadingSessionsFlow(): Flow<List<ReadingSessionEntity>> = quranDao.getAllReadingSessions()

    suspend fun logReadingSession(durationSeconds: Long, type: String = "reading", chapterId: Int? = null) {
        withContext(Dispatchers.IO) {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            quranDao.insertReadingSession(
                ReadingSessionEntity(
                    date = today,
                    duration = durationSeconds,
                    type = type,
                    chapterId = chapterId
                )
            )
            quranDao.pruneReadingSessions()
        }
    }

    // Recently Read
    fun getRecentlyReadFlow(): Flow<List<RecentlyReadEntity>> = quranDao.getRecentlyRead()

    suspend fun addRecentlyRead(chapterId: Int, chapterName: String, verseKey: String? = null) {
        withContext(Dispatchers.IO) {
            quranDao.upsertRecentlyRead(
                RecentlyReadEntity(
                    chapterId = chapterId,
                    chapterName = chapterName,
                    verseKey = verseKey
                )
            )
            quranDao.pruneRecentlyRead()
        }
    }

    // Collections
    fun getCollectionsFlow(): Flow<List<CollectionEntity>> = quranDao.getAllCollections()

    fun getAllCollectionItemsFlow(): Flow<List<CollectionItemEntity>> = quranDao.getAllCollectionItems()

    suspend fun addCollection(name: String): Long = withContext(Dispatchers.IO) {
        val id = System.currentTimeMillis()
        quranDao.insertCollection(CollectionEntity(id = id, name = name))
        id
    }

    suspend fun addToCollection(collectionId: Long, verseKey: String, chapterId: Int, surahName: String) {
        withContext(Dispatchers.IO) {
            quranDao.insertCollectionItem(
                CollectionItemEntity(
                    collectionId = collectionId,
                    verseKey = verseKey,
                    chapterId = chapterId,
                    surahName = surahName
                )
            )
        }
    }
}
