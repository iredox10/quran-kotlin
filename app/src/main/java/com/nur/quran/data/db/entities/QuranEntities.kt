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
    indices = [Index(value = ["chapterId"]), Index(value = ["verseKey"], unique = true)]
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
    val juzNumber: Int
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
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
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
