package com.nur.quran.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.nur.quran.data.db.dao.QuranDao
import com.nur.quran.data.db.entities.*

@Database(
    entities = [
        ChapterEntity::class,
        VerseEntity::class,
        WordEntity::class,
        BookmarkEntity::class,
        CollectionEntity::class,
        CollectionItemEntity::class,
        ApiResponseCacheEntity::class,
        ReadingSessionEntity::class,
        RecentlyReadEntity::class,
        LinkedTimingEntity::class,
        TranslationTextEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class QuranDatabase : RoomDatabase() {
    abstract fun quranDao(): QuranDao
}
