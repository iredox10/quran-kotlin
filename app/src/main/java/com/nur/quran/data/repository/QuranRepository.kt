package com.nur.quran.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nur.quran.data.api.QuranApi
import com.nur.quran.data.api.ApiTafsirVerse
import com.nur.quran.data.api.ApiTajweedVerse
import com.nur.quran.data.api.TafsirResponse
import com.nur.quran.data.api.TajweedResponse
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
import com.nur.quran.utils.TajweedProcessor

@Singleton
class QuranRepository @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
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
        val existing = quranDao.getAllChapters().firstOrNull()
        if (!existing.isNullOrEmpty()) {
            return@withContext
        }
        try {
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
        } catch (e: Exception) {
            try {
                val jsonString = context.assets.open("data/chapters.json").bufferedReader().use { it.readText() }
                val type = object : TypeToken<List<com.nur.quran.data.api.ApiChapter>>() {}.type
                val chapters: List<com.nur.quran.data.api.ApiChapter> = gson.fromJson(jsonString, type)
                val entities = chapters.map { apiChapter ->
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
            } catch (_: Exception) {}
        }
    }

    // Verses by Chapter
    fun getVersesByChapterFlow(chapterId: Int): Flow<List<VerseEntity>> = quranDao.getVersesByChapter(chapterId)

    suspend fun getVersesByChapterDirect(chapterId: Int): List<VerseEntity> = withContext(Dispatchers.IO) {
        quranDao.getVersesByChapterDirect(chapterId)
    }

    fun getVersesByPageFlow(pageNumber: Int): Flow<List<VerseEntity>> = quranDao.getVersesByPage(pageNumber)

    suspend fun getVersesByPage(pageNumber: Int): List<VerseEntity> = withContext(Dispatchers.IO) {
        var list = quranDao.getVersesByPage(pageNumber).firstOrNull()
        if (list.isNullOrEmpty() || list.any { it.textUthmani.isNullOrBlank() }) {
            try {
                val response = quranApi.getVersesByPage(
                    pageNumber = pageNumber,
                    translations = "85",
                    fields = "text_uthmani,text_indopak,text_qpc_hafs",
                    wordFields = "text_uthmani,text_indopak,text_qpc_hafs,text_uthmani_tajweed,translation,transliteration",
                    perPage = 50
                )
                val verseEntities = mutableListOf<VerseEntity>()
                val wordEntities = mutableListOf<WordEntity>()

                for (apiVerse in response.verses) {
                    val wordsUthmani = apiVerse.words?.joinToString(" ") { it.text_uthmani ?: "" }?.trim() ?: ""
                    val wordsIndopak = apiVerse.words?.joinToString(" ") { it.text_indopak ?: "" }?.trim() ?: ""
                    val wordsQpcHafs = apiVerse.words?.joinToString(" ") { it.text_qpc_hafs ?: "" }?.trim() ?: ""

                    val uthmani = if (wordsUthmani.isNotBlank()) wordsUthmani else (apiVerse.text_uthmani ?: "")
                    val indopak = if (wordsIndopak.isNotBlank()) wordsIndopak else (apiVerse.text_indopak ?: "")
                    val qpcHafs = if (wordsQpcHafs.isNotBlank()) wordsQpcHafs else (apiVerse.text_qpc_hafs ?: "")

                    verseEntities.add(
                        VerseEntity(
                            id = apiVerse.id,
                            chapterId = apiVerse.verse_key.split(":")[0].toIntOrNull() ?: 1,
                            verseNumber = apiVerse.verse_number,
                            verseKey = apiVerse.verse_key,
                            textUthmani = if (uthmani.isNotBlank()) uthmani else (apiVerse.words?.joinToString(" ") { it.text_uthmani_tajweed ?: "" } ?: ""),
                            textIndopak = indopak,
                            textQpcHafs = qpcHafs,
                            pageNumber = apiVerse.page_number ?: pageNumber,
                            juzNumber = apiVerse.juz_number ?: 1,
                            translation = apiVerse.translations?.firstOrNull()?.text
                        )
                    )

                    apiVerse.words?.forEachIndexed { index, apiWord ->
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
                if (verseEntities.isNotEmpty()) {
                    quranDao.insertVersesAndWords(verseEntities, wordEntities)
                    list = verseEntities
                }
            } catch (e: Exception) {
                // Offline fallback: load from bundled asset
                val (offlineVerses, offlineWords) = loadOfflineVersesForPage(pageNumber)
                if (offlineVerses.isNotEmpty()) {
                    quranDao.insertVersesAndWords(offlineVerses, offlineWords)
                    list = offlineVerses
                }
            }
        }
        list ?: emptyList()
    }

    data class OfflineVerseItem(
        val id: Int,
        val verse_key: String,
        val text_uthmani: String?,
        val text_indopak: String?,
        val page_number: Int?,
        val juz_number: Int?,
        val translation: String?,
        val text_uthmani_tajweed: String? = null
    )

    data class OfflineTafsirItem(
        val verse_key: String,
        val text: String,
        val resource_id: Int
    )

    /** Cached parsed offline data to avoid re-parsing the large JSON on every call */
    @Volatile
    private var cachedOfflineVerses: List<OfflineVerseItem>? = null

    private fun getAllOfflineVerses(): List<OfflineVerseItem> {
        cachedOfflineVerses?.let { return it }
        return try {
            val jsonString = context.assets.open("data/quran_full.json").bufferedReader().use { it.readText() }
            val type = object : TypeToken<List<OfflineVerseItem>>() {}.type
            val all: List<OfflineVerseItem> = gson.fromJson(jsonString, type)
            cachedOfflineVerses = all
            all
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun loadOfflineVersesFromAssets(chapterId: Int): Pair<List<VerseEntity>, List<WordEntity>> {
        return try {
            val allVerses = getAllOfflineVerses()
            val prefix = "$chapterId:"
            val filtered = allVerses.filter { it.verse_key.startsWith(prefix) }
            val verseEntities = mutableListOf<VerseEntity>()
            val wordEntities = mutableListOf<WordEntity>()

            for (item in filtered) {
                val vNum = item.verse_key.split(":")[1].toIntOrNull() ?: 1
                val uthmani = item.text_uthmani ?: ""
                val indopak = if (!item.text_indopak.isNullOrBlank()) item.text_indopak else uthmani

                verseEntities.add(
                    VerseEntity(
                        id = item.id,
                        chapterId = chapterId,
                        verseNumber = vNum,
                        verseKey = item.verse_key,
                        textUthmani = uthmani,
                        textIndopak = indopak,
                        textQpcHafs = uthmani,
                        pageNumber = item.page_number ?: 1,
                        juzNumber = item.juz_number ?: 1,
                        translation = item.translation
                    )
                )

                // Generate WordEntity objects from tajweed HTML and plain text
                val tajweedHtml = item.text_uthmani_tajweed ?: ""
                val plainWords = uthmani.split("\\s+".toRegex()).filter { it.isNotBlank() }
                val tajweedWords = if (tajweedHtml.isNotBlank())
                    TajweedProcessor.splitTajweedHtmlIntoWords(tajweedHtml)
                else emptyList()

                plainWords.forEachIndexed { wordIdx, wordText ->
                    val wordTajweed = tajweedWords.getOrNull(wordIdx)
                    val isEndMarker = wordText.length <= 3 && wordText.any { it in '\u06D6'..'\u06ED' || it in '٠'..'٩' || it.isDigit() }
                    wordEntities.add(
                        WordEntity(
                            id = item.id * 100 + wordIdx + 1,
                            verseId = item.id,
                            position = wordIdx + 1,
                            textUthmani = wordText,
                            textIndopak = indopak.split("\\s+".toRegex()).filter { it.isNotBlank() }.getOrNull(wordIdx),
                            textQpcHafs = wordText,
                            textUthmaniTajweed = wordTajweed,
                            translation = null,
                            transliteration = null,
                            charTypeName = if (isEndMarker) "end" else "word"
                        )
                    )
                }
            }
            Pair(verseEntities, wordEntities)
        } catch (e: Exception) {
            Pair(emptyList(), emptyList())
        }
    }

    /**
     * Load verses for a specific page from the offline asset.
     */
    private fun loadOfflineVersesForPage(pageNumber: Int): Pair<List<VerseEntity>, List<WordEntity>> {
        return try {
            val allVerses = getAllOfflineVerses()
            val filtered = allVerses.filter { it.page_number == pageNumber }
            val verseEntities = mutableListOf<VerseEntity>()
            val wordEntities = mutableListOf<WordEntity>()

            for (item in filtered) {
                val parts = item.verse_key.split(":")
                val chapId = parts[0].toIntOrNull() ?: 1
                val vNum = parts.getOrNull(1)?.toIntOrNull() ?: 1
                val uthmani = item.text_uthmani ?: ""
                val indopak = if (!item.text_indopak.isNullOrBlank()) item.text_indopak else uthmani

                verseEntities.add(
                    VerseEntity(
                        id = item.id,
                        chapterId = chapId,
                        verseNumber = vNum,
                        verseKey = item.verse_key,
                        textUthmani = uthmani,
                        textIndopak = indopak,
                        textQpcHafs = uthmani,
                        pageNumber = item.page_number ?: pageNumber,
                        juzNumber = item.juz_number ?: 1,
                        translation = item.translation
                    )
                )

                // Generate WordEntity objects
                val tajweedHtml = item.text_uthmani_tajweed ?: ""
                val plainWords = uthmani.split("\\s+".toRegex()).filter { it.isNotBlank() }
                val tajweedWords = if (tajweedHtml.isNotBlank())
                    TajweedProcessor.splitTajweedHtmlIntoWords(tajweedHtml)
                else emptyList()

                plainWords.forEachIndexed { wordIdx, wordText ->
                    val wordTajweed = tajweedWords.getOrNull(wordIdx)
                    val isEndMarker = wordText.length <= 3 && wordText.any { it in '\u06D6'..'\u06ED' || it in '٠'..'٩' || it.isDigit() }
                    wordEntities.add(
                        WordEntity(
                            id = item.id * 100 + wordIdx + 1,
                            verseId = item.id,
                            position = wordIdx + 1,
                            textUthmani = wordText,
                            textIndopak = indopak.split("\\s+".toRegex()).filter { it.isNotBlank() }.getOrNull(wordIdx),
                            textQpcHafs = wordText,
                            textUthmaniTajweed = wordTajweed,
                            translation = null,
                            transliteration = null,
                            charTypeName = if (isEndMarker) "end" else "word"
                        )
                    )
                }
            }
            Pair(verseEntities, wordEntities)
        } catch (e: Exception) {
            Pair(emptyList(), emptyList())
        }
    }

    /** Cached parsed tafsir data to avoid re-reading the large JSON on every call */
    @Volatile
    private var cachedOfflineTafsirs: List<OfflineTafsirItem>? = null

    private fun getAllOfflineTafsirs(): List<OfflineTafsirItem> {
        cachedOfflineTafsirs?.let { return it }
        return try {
            val jsonString = context.assets.open("data/tafsir_ibn_kathir.json").bufferedReader().use { it.readText() }
            val type = object : TypeToken<List<OfflineTafsirItem>>() {}.type
            val parsed: List<OfflineTafsirItem> = gson.fromJson(jsonString, type)
            cachedOfflineTafsirs = parsed
            parsed
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Load tafsir from the bundled offline asset.
     */
    private fun loadOfflineTafsirFromAssets(chapterId: Int): TafsirResponse {
        return try {
            val allTafsirs = getAllOfflineTafsirs()
            val filtered = allTafsirs.filter {
                it.verse_key.split(":").firstOrNull()?.toIntOrNull() == chapterId
            }.map { item ->
                ApiTafsirVerse(verse_key = item.verse_key, text = item.text, resource_id = item.resource_id)
            }
            TafsirResponse(tafsirs = filtered)
        } catch (e: Exception) {
            TafsirResponse(tafsirs = emptyList())
        }
    }

    /**
     * Build a TajweedResponse from offline asset data for a chapter.
     */
    private fun loadOfflineTajweedFromAssets(chapterId: Int): TajweedResponse {
        return try {
            val allVerses = getAllOfflineVerses()
            val tajweedVerses = allVerses
                .filter { it.verse_key.split(":").firstOrNull()?.toIntOrNull() == chapterId && !it.text_uthmani_tajweed.isNullOrBlank() }
                .map { ApiTajweedVerse(id = it.id, verse_key = it.verse_key, text_uthmani_tajweed = it.text_uthmani_tajweed!!) }
            TajweedResponse(verses = tajweedVerses)
        } catch (e: Exception) {
            TajweedResponse(verses = emptyList())
        }
    }

    suspend fun refreshVersesByChapter(chapterId: Int, translationId: Int) = withContext(Dispatchers.IO) {
        val existing = quranDao.getVersesByChapter(chapterId).firstOrNull()
        if (!existing.isNullOrEmpty() && existing.none { it.textUthmani.isNullOrBlank() }
            && existing.any { !it.translation.isNullOrBlank() }) {
            return@withContext
        }
        try {
            val response = quranApi.getVersesByChapter(
                chapterId = chapterId,
                translations = translationId.toString(),
                fields = "text_uthmani,text_indopak,text_qpc_hafs",
                wordFields = "text_uthmani,text_indopak,text_qpc_hafs,text_uthmani_tajweed,translation,transliteration",
                perPage = 300
            )
            
            val tajweedResponse = try {
                quranApi.getUthmaniTajweed(chapterNumber = chapterId)
            } catch (e: Exception) {
                null
            }
            val tajweedMap = tajweedResponse?.verses?.associate { it.verse_key to it.text_uthmani_tajweed } ?: emptyMap()
            
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

                val verseTajweedHtml = tajweedMap[apiVerse.verse_key]
                val wordTajweedList = verseTajweedHtml?.let { TajweedProcessor.splitTajweedHtmlIntoWords(it) } ?: emptyList()

                apiVerse.words?.forEachIndexed { index, apiWord ->
                    val wordTajweed = if (index < wordTajweedList.size) wordTajweedList[index] else null
                    wordEntities.add(
                        WordEntity(
                            id = apiWord.id,
                            verseId = apiVerse.id,
                            position = apiWord.position,
                            textUthmani = apiWord.text_uthmani,
                            textIndopak = apiWord.text_indopak,
                            textQpcHafs = apiWord.text_qpc_hafs,
                            textUthmaniTajweed = apiWord.text_uthmani_tajweed ?: wordTajweed,
                            translation = apiWord.translation?.text,
                            transliteration = apiWord.transliteration?.text,
                            charTypeName = apiWord.char_type_name
                        )
                    )
                }
            }

            quranDao.insertVersesAndWords(verseEntities, wordEntities)
        } catch (e: Exception) {
            // Offline fallback: load from bundled asset with WordEntity generation
            val (offlineVerses, offlineWords) = loadOfflineVersesFromAssets(chapterId)
            if (offlineVerses.isNotEmpty()) {
                quranDao.insertVersesAndWords(offlineVerses, offlineWords)
            }
        }
    }

    // Words
    suspend fun getWordsForVerse(verseId: Int): List<WordEntity> = quranDao.getWordsForVerse(verseId)

    // Tajweed HTML — with offline asset fallback
    suspend fun getTajweedHtmlForChapter(chapterId: Int): TajweedResponse = withContext(Dispatchers.IO) {
        try {
            fetchWithOfflineCache("tajweed_chapter_$chapterId") {
                quranApi.getUthmaniTajweed(chapterNumber = chapterId)
            }
        } catch (e: Exception) {
            // Fallback: build TajweedResponse from bundled asset
            loadOfflineTajweedFromAssets(chapterId)
        }
    }

    // Tafsir — with offline asset fallback
    suspend fun getTafsirForChapter(tafsirId: Int, chapterId: Int): TafsirResponse = withContext(Dispatchers.IO) {
        try {
            fetchWithOfflineCache("tafsir_${tafsirId}_chapter_$chapterId") {
                quranApi.getTafsirByChapter(tafsirId, chapterId)
            }
        } catch (e: Exception) {
            // Fallback: load from bundled Ibn Kathir asset
            loadOfflineTafsirFromAssets(chapterId)
        }
    }

    // Footnote
    suspend fun getFootnote(footnoteId: String) = fetchWithOfflineCache("footnote_$footnoteId") {
        quranApi.getFootnote(footnoteId)
    }

    // Bookmarks
    fun getAllBookmarksFlow(): Flow<List<BookmarkEntity>> = quranDao.getAllBookmarks()

    fun getBookmarkedVerseKeysFlow(): Flow<List<String>> = quranDao.getBookmarkedVerseKeys()

    suspend fun insertBookmark(bookmark: BookmarkEntity) {
        quranDao.deleteAllBookmarks()
        quranDao.insertBookmark(bookmark)
    }

    suspend fun setSingleBookmark(bookmark: BookmarkEntity) {
        quranDao.deleteAllBookmarks()
        quranDao.insertBookmark(bookmark)
    }

    suspend fun clearBookmarks() {
        quranDao.deleteAllBookmarks()
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

    suspend fun getRecentlyReadForChapter(chapterId: Int): RecentlyReadEntity? = withContext(Dispatchers.IO) {
        quranDao.getRecentlyReadForChapter(chapterId)
    }

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

    // ── Planner ────────────────────────────────────────────────────────
    suspend fun getActivePlan(): com.nur.quran.data.planner.ReadingPlan? = withContext(Dispatchers.IO) {
        val entry = quranDao.getCacheEntry("planner_active_plan")
        if (entry != null && entry.dataJson.isNotBlank()) {
            try {
                gson.fromJson(entry.dataJson, com.nur.quran.data.planner.ReadingPlan::class.java)
            } catch (e: Exception) {
                null
            }
        } else null
    }

    suspend fun saveActivePlan(plan: com.nur.quran.data.planner.ReadingPlan?) = withContext(Dispatchers.IO) {
        if (plan != null) {
            val json = gson.toJson(plan)
            quranDao.insertCacheEntry(ApiResponseCacheEntity("planner_active_plan", json))
        } else {
            quranDao.insertCacheEntry(ApiResponseCacheEntity("planner_active_plan", ""))
        }
    }

    suspend fun getArchivedPlans(): List<com.nur.quran.data.planner.ReadingPlan> = withContext(Dispatchers.IO) {
        val entry = quranDao.getCacheEntry("planner_archived_plans")
        if (entry != null && entry.dataJson.isNotBlank()) {
            try {
                val type = object : TypeToken<List<com.nur.quran.data.planner.ReadingPlan>>() {}.type
                gson.fromJson<List<com.nur.quran.data.planner.ReadingPlan>>(entry.dataJson, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        } else emptyList()
    }

    suspend fun saveArchivedPlans(plans: List<com.nur.quran.data.planner.ReadingPlan>) = withContext(Dispatchers.IO) {
        val json = gson.toJson(plans)
        quranDao.insertCacheEntry(ApiResponseCacheEntity("planner_archived_plans", json))
    }

    suspend fun getAllPlans(): List<com.nur.quran.data.planner.ReadingPlan> = withContext(Dispatchers.IO) {
        val entry = quranDao.getCacheEntry("planner_all_plans")
        if (entry != null && entry.dataJson.isNotBlank()) {
            try {
                val type = object : TypeToken<List<com.nur.quran.data.planner.ReadingPlan>>() {}.type
                gson.fromJson<List<com.nur.quran.data.planner.ReadingPlan>>(entry.dataJson, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        } else emptyList()
    }

    suspend fun saveAllPlans(plans: List<com.nur.quran.data.planner.ReadingPlan>) = withContext(Dispatchers.IO) {
        val json = gson.toJson(plans)
        quranDao.insertCacheEntry(ApiResponseCacheEntity("planner_all_plans", json))
    }

    suspend fun getActivePlannerId(): String? = withContext(Dispatchers.IO) {
        val entry = quranDao.getCacheEntry("planner_active_id")
        if (entry != null && entry.dataJson.isNotBlank()) {
            entry.dataJson
        } else null
    }

    suspend fun saveActivePlannerId(id: String?) = withContext(Dispatchers.IO) {
        if (id != null) {
            quranDao.insertCacheEntry(ApiResponseCacheEntity("planner_active_id", id))
        } else {
            quranDao.insertCacheEntry(ApiResponseCacheEntity("planner_active_id", ""))
        }
    }
}
