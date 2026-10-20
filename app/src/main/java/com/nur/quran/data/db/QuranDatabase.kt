package com.nur.quran.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 9,
    exportSchema = false
)
abstract class QuranDatabase : RoomDatabase() {
    abstract fun quranDao(): QuranDao

    companion object {
        /**
         * v8→v9: reading-mode-aware Continue/Bookmark deep links. Adds:
         * - `recently_read.pageNumber` (page the user was behind when last read)
         * - `bookmarks.sourceMode` / `sourcePage` (the reader UI the verse was
         *   bookmarked from). ALTERs keep existing user data intact.
         */
        val MIGRATION_8_9: Migration = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE recently_read ADD COLUMN pageNumber INTEGER")
                db.execSQL("ALTER TABLE bookmarks ADD COLUMN sourceMode TEXT")
                db.execSQL("ALTER TABLE bookmarks ADD COLUMN sourcePage INTEGER")
            }
        }
    }
}
