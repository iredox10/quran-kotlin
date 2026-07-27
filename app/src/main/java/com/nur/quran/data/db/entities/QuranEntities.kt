package com.nur.quran.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(tableName = "chapters")
data class ChapterEntity(
    @PrimaryKey val id: Int,
    val nameSimple: String,
    val nameArabic: String,
    val nameComplex: String,
    val translatedName: String = "",
    val revelationPlace: String,
    val revelationOrder: Int,
    val versesCount: Int,
    val pagesStart: Int,
    val pagesEnd: Int
)

@Entity(
    tableName = "verses",
    foreignKeys = [
        ForeignKey(
            entity = ChapterEntity::class,
            parentColumns = ["id"],
            childColumns = ["chapterId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["chapterId"]), Index(value = ["verseKey"], unique = true), Index(value = ["pageNumber"])]
)
data class VerseEntity(
    @PrimaryKey val id: Int,
    val chapterId: Int,
    val verseNumber: Int,
    val verseKey: String,
    val textUthmani: String?,
    val textIndopak: String?,
    val textQpcHafs: String?,
    val pageNumber: Int,
    val juzNumber: Int,
    val translation: String? = null,
    val audioUrl: String? = null
)

@Entity(
    tableName = "words",
    foreignKeys = [
        ForeignKey(
            entity = VerseEntity::class,
            parentColumns = ["id"],
            childColumns = ["verseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["verseId"])]
)
data class WordEntity(
    @PrimaryKey val id: Int,
    val verseId: Int,
    val position: Int,
    val textUthmani: String?,
    val textIndopak: String?,
    val textQpcHafs: String?,
    val textUthmaniTajweed: String?,
    val translation: String?,
    val transliteration: String?,
    val charTypeName: String
)

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey val id: Int = 1,
    val verseKey: String,
    val chapterId: Int,
    val surahName: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "collections")
data class CollectionEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "collection_items",
    primaryKeys = ["collectionId", "verseKey"],
    foreignKeys = [
        ForeignKey(
            entity = CollectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["collectionId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class CollectionItemEntity(
    val collectionId: Long,
    val verseKey: String,
    val chapterId: Int,
    val surahName: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "api_responses")
data class ApiResponseCacheEntity(
    @PrimaryKey val key: String,
    val dataJson: String,
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * A single reading/memorizing/listening session, equivalent to the web app's
 * `readingSessions` store entries: { date (YYYY-MM-DD), duration (seconds), type, chapterId, timestamp }.
 */
@Entity(tableName = "reading_sessions")
data class ReadingSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,
    val duration: Long,
    val type: String = "reading",
    val chapterId: Int? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Recently read chapters, equivalent to the web app's `recentlyRead` store
 * entries: { chapterId, chapterName, verseKey, timestamp }. Kept most-recent-first.
 */
@Entity(tableName = "recently_read")
data class RecentlyReadEntity(
    @PrimaryKey val chapterId: Int,
    val chapterName: String,
    val verseKey: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
