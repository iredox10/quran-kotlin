package com.nur.quran.data.db;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import com.nur.quran.data.db.dao.QuranDao;
import com.nur.quran.data.db.dao.QuranDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class QuranDatabase_Impl extends QuranDatabase {
  private volatile QuranDao _quranDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(7) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `chapters` (`id` INTEGER NOT NULL, `nameSimple` TEXT NOT NULL, `nameArabic` TEXT NOT NULL, `nameComplex` TEXT NOT NULL, `translatedName` TEXT NOT NULL, `revelationPlace` TEXT NOT NULL, `revelationOrder` INTEGER NOT NULL, `versesCount` INTEGER NOT NULL, `pagesStart` INTEGER NOT NULL, `pagesEnd` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `verses` (`id` INTEGER NOT NULL, `chapterId` INTEGER NOT NULL, `verseNumber` INTEGER NOT NULL, `verseKey` TEXT NOT NULL, `textUthmani` TEXT, `textIndopak` TEXT, `textQpcHafs` TEXT, `pageNumber` INTEGER NOT NULL, `juzNumber` INTEGER NOT NULL, `translation` TEXT, `audioUrl` TEXT, PRIMARY KEY(`id`), FOREIGN KEY(`chapterId`) REFERENCES `chapters`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_verses_chapterId` ON `verses` (`chapterId`)");
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_verses_verseKey` ON `verses` (`verseKey`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_verses_pageNumber` ON `verses` (`pageNumber`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `words` (`id` INTEGER NOT NULL, `verseId` INTEGER NOT NULL, `position` INTEGER NOT NULL, `textUthmani` TEXT, `textIndopak` TEXT, `textQpcHafs` TEXT, `textUthmaniTajweed` TEXT, `translation` TEXT, `transliteration` TEXT, `charTypeName` TEXT NOT NULL, `lineNumber` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`verseId`) REFERENCES `verses`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_words_verseId` ON `words` (`verseId`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `bookmarks` (`id` INTEGER NOT NULL, `verseKey` TEXT NOT NULL, `chapterId` INTEGER NOT NULL, `surahName` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `collections` (`id` INTEGER NOT NULL, `name` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `collection_items` (`collectionId` INTEGER NOT NULL, `verseKey` TEXT NOT NULL, `chapterId` INTEGER NOT NULL, `surahName` TEXT NOT NULL, `addedAt` INTEGER NOT NULL, PRIMARY KEY(`collectionId`, `verseKey`), FOREIGN KEY(`collectionId`) REFERENCES `collections`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE TABLE IF NOT EXISTS `api_responses` (`key` TEXT NOT NULL, `dataJson` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`key`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `reading_sessions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `date` TEXT NOT NULL, `duration` INTEGER NOT NULL, `type` TEXT NOT NULL, `chapterId` INTEGER, `timestamp` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `recently_read` (`chapterId` INTEGER NOT NULL, `chapterName` TEXT NOT NULL, `verseKey` TEXT, `timestamp` INTEGER NOT NULL, PRIMARY KEY(`chapterId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `linked_timings` (`reciterId` INTEGER NOT NULL, `sura` INTEGER NOT NULL, `ayah` INTEGER NOT NULL, `startMs` INTEGER NOT NULL, PRIMARY KEY(`reciterId`, `sura`, `ayah`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `translation_texts` (`translationId` INTEGER NOT NULL, `verseKey` TEXT NOT NULL, `text` TEXT NOT NULL, PRIMARY KEY(`translationId`, `verseKey`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '0f743a9248a864da6e0b3076e96bf2ba')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `chapters`");
        db.execSQL("DROP TABLE IF EXISTS `verses`");
        db.execSQL("DROP TABLE IF EXISTS `words`");
        db.execSQL("DROP TABLE IF EXISTS `bookmarks`");
        db.execSQL("DROP TABLE IF EXISTS `collections`");
        db.execSQL("DROP TABLE IF EXISTS `collection_items`");
        db.execSQL("DROP TABLE IF EXISTS `api_responses`");
        db.execSQL("DROP TABLE IF EXISTS `reading_sessions`");
        db.execSQL("DROP TABLE IF EXISTS `recently_read`");
        db.execSQL("DROP TABLE IF EXISTS `linked_timings`");
        db.execSQL("DROP TABLE IF EXISTS `translation_texts`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        db.execSQL("PRAGMA foreign_keys = ON");
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsChapters = new HashMap<String, TableInfo.Column>(10);
        _columnsChapters.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChapters.put("nameSimple", new TableInfo.Column("nameSimple", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChapters.put("nameArabic", new TableInfo.Column("nameArabic", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChapters.put("nameComplex", new TableInfo.Column("nameComplex", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChapters.put("translatedName", new TableInfo.Column("translatedName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChapters.put("revelationPlace", new TableInfo.Column("revelationPlace", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChapters.put("revelationOrder", new TableInfo.Column("revelationOrder", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChapters.put("versesCount", new TableInfo.Column("versesCount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChapters.put("pagesStart", new TableInfo.Column("pagesStart", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChapters.put("pagesEnd", new TableInfo.Column("pagesEnd", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysChapters = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesChapters = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoChapters = new TableInfo("chapters", _columnsChapters, _foreignKeysChapters, _indicesChapters);
        final TableInfo _existingChapters = TableInfo.read(db, "chapters");
        if (!_infoChapters.equals(_existingChapters)) {
          return new RoomOpenHelper.ValidationResult(false, "chapters(com.nur.quran.data.db.entities.ChapterEntity).\n"
                  + " Expected:\n" + _infoChapters + "\n"
                  + " Found:\n" + _existingChapters);
        }
        final HashMap<String, TableInfo.Column> _columnsVerses = new HashMap<String, TableInfo.Column>(11);
        _columnsVerses.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVerses.put("chapterId", new TableInfo.Column("chapterId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVerses.put("verseNumber", new TableInfo.Column("verseNumber", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVerses.put("verseKey", new TableInfo.Column("verseKey", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVerses.put("textUthmani", new TableInfo.Column("textUthmani", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVerses.put("textIndopak", new TableInfo.Column("textIndopak", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVerses.put("textQpcHafs", new TableInfo.Column("textQpcHafs", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVerses.put("pageNumber", new TableInfo.Column("pageNumber", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVerses.put("juzNumber", new TableInfo.Column("juzNumber", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVerses.put("translation", new TableInfo.Column("translation", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVerses.put("audioUrl", new TableInfo.Column("audioUrl", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysVerses = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysVerses.add(new TableInfo.ForeignKey("chapters", "CASCADE", "NO ACTION", Arrays.asList("chapterId"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesVerses = new HashSet<TableInfo.Index>(3);
        _indicesVerses.add(new TableInfo.Index("index_verses_chapterId", false, Arrays.asList("chapterId"), Arrays.asList("ASC")));
        _indicesVerses.add(new TableInfo.Index("index_verses_verseKey", true, Arrays.asList("verseKey"), Arrays.asList("ASC")));
        _indicesVerses.add(new TableInfo.Index("index_verses_pageNumber", false, Arrays.asList("pageNumber"), Arrays.asList("ASC")));
        final TableInfo _infoVerses = new TableInfo("verses", _columnsVerses, _foreignKeysVerses, _indicesVerses);
        final TableInfo _existingVerses = TableInfo.read(db, "verses");
        if (!_infoVerses.equals(_existingVerses)) {
          return new RoomOpenHelper.ValidationResult(false, "verses(com.nur.quran.data.db.entities.VerseEntity).\n"
                  + " Expected:\n" + _infoVerses + "\n"
                  + " Found:\n" + _existingVerses);
        }
        final HashMap<String, TableInfo.Column> _columnsWords = new HashMap<String, TableInfo.Column>(11);
        _columnsWords.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWords.put("verseId", new TableInfo.Column("verseId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWords.put("position", new TableInfo.Column("position", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWords.put("textUthmani", new TableInfo.Column("textUthmani", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWords.put("textIndopak", new TableInfo.Column("textIndopak", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWords.put("textQpcHafs", new TableInfo.Column("textQpcHafs", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWords.put("textUthmaniTajweed", new TableInfo.Column("textUthmaniTajweed", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWords.put("translation", new TableInfo.Column("translation", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWords.put("transliteration", new TableInfo.Column("transliteration", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWords.put("charTypeName", new TableInfo.Column("charTypeName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWords.put("lineNumber", new TableInfo.Column("lineNumber", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysWords = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysWords.add(new TableInfo.ForeignKey("verses", "CASCADE", "NO ACTION", Arrays.asList("verseId"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesWords = new HashSet<TableInfo.Index>(1);
        _indicesWords.add(new TableInfo.Index("index_words_verseId", false, Arrays.asList("verseId"), Arrays.asList("ASC")));
        final TableInfo _infoWords = new TableInfo("words", _columnsWords, _foreignKeysWords, _indicesWords);
        final TableInfo _existingWords = TableInfo.read(db, "words");
        if (!_infoWords.equals(_existingWords)) {
          return new RoomOpenHelper.ValidationResult(false, "words(com.nur.quran.data.db.entities.WordEntity).\n"
                  + " Expected:\n" + _infoWords + "\n"
                  + " Found:\n" + _existingWords);
        }
        final HashMap<String, TableInfo.Column> _columnsBookmarks = new HashMap<String, TableInfo.Column>(5);
        _columnsBookmarks.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBookmarks.put("verseKey", new TableInfo.Column("verseKey", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBookmarks.put("chapterId", new TableInfo.Column("chapterId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBookmarks.put("surahName", new TableInfo.Column("surahName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBookmarks.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysBookmarks = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesBookmarks = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoBookmarks = new TableInfo("bookmarks", _columnsBookmarks, _foreignKeysBookmarks, _indicesBookmarks);
        final TableInfo _existingBookmarks = TableInfo.read(db, "bookmarks");
        if (!_infoBookmarks.equals(_existingBookmarks)) {
          return new RoomOpenHelper.ValidationResult(false, "bookmarks(com.nur.quran.data.db.entities.BookmarkEntity).\n"
                  + " Expected:\n" + _infoBookmarks + "\n"
                  + " Found:\n" + _existingBookmarks);
        }
        final HashMap<String, TableInfo.Column> _columnsCollections = new HashMap<String, TableInfo.Column>(3);
        _columnsCollections.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCollections.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCollections.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysCollections = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesCollections = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoCollections = new TableInfo("collections", _columnsCollections, _foreignKeysCollections, _indicesCollections);
        final TableInfo _existingCollections = TableInfo.read(db, "collections");
        if (!_infoCollections.equals(_existingCollections)) {
          return new RoomOpenHelper.ValidationResult(false, "collections(com.nur.quran.data.db.entities.CollectionEntity).\n"
                  + " Expected:\n" + _infoCollections + "\n"
                  + " Found:\n" + _existingCollections);
        }
        final HashMap<String, TableInfo.Column> _columnsCollectionItems = new HashMap<String, TableInfo.Column>(5);
        _columnsCollectionItems.put("collectionId", new TableInfo.Column("collectionId", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCollectionItems.put("verseKey", new TableInfo.Column("verseKey", "TEXT", true, 2, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCollectionItems.put("chapterId", new TableInfo.Column("chapterId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCollectionItems.put("surahName", new TableInfo.Column("surahName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCollectionItems.put("addedAt", new TableInfo.Column("addedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysCollectionItems = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysCollectionItems.add(new TableInfo.ForeignKey("collections", "CASCADE", "NO ACTION", Arrays.asList("collectionId"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesCollectionItems = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoCollectionItems = new TableInfo("collection_items", _columnsCollectionItems, _foreignKeysCollectionItems, _indicesCollectionItems);
        final TableInfo _existingCollectionItems = TableInfo.read(db, "collection_items");
        if (!_infoCollectionItems.equals(_existingCollectionItems)) {
          return new RoomOpenHelper.ValidationResult(false, "collection_items(com.nur.quran.data.db.entities.CollectionItemEntity).\n"
                  + " Expected:\n" + _infoCollectionItems + "\n"
                  + " Found:\n" + _existingCollectionItems);
        }
        final HashMap<String, TableInfo.Column> _columnsApiResponses = new HashMap<String, TableInfo.Column>(3);
        _columnsApiResponses.put("key", new TableInfo.Column("key", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsApiResponses.put("dataJson", new TableInfo.Column("dataJson", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsApiResponses.put("updatedAt", new TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysApiResponses = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesApiResponses = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoApiResponses = new TableInfo("api_responses", _columnsApiResponses, _foreignKeysApiResponses, _indicesApiResponses);
        final TableInfo _existingApiResponses = TableInfo.read(db, "api_responses");
        if (!_infoApiResponses.equals(_existingApiResponses)) {
          return new RoomOpenHelper.ValidationResult(false, "api_responses(com.nur.quran.data.db.entities.ApiResponseCacheEntity).\n"
                  + " Expected:\n" + _infoApiResponses + "\n"
                  + " Found:\n" + _existingApiResponses);
        }
        final HashMap<String, TableInfo.Column> _columnsReadingSessions = new HashMap<String, TableInfo.Column>(6);
        _columnsReadingSessions.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsReadingSessions.put("date", new TableInfo.Column("date", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsReadingSessions.put("duration", new TableInfo.Column("duration", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsReadingSessions.put("type", new TableInfo.Column("type", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsReadingSessions.put("chapterId", new TableInfo.Column("chapterId", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsReadingSessions.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysReadingSessions = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesReadingSessions = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoReadingSessions = new TableInfo("reading_sessions", _columnsReadingSessions, _foreignKeysReadingSessions, _indicesReadingSessions);
        final TableInfo _existingReadingSessions = TableInfo.read(db, "reading_sessions");
        if (!_infoReadingSessions.equals(_existingReadingSessions)) {
          return new RoomOpenHelper.ValidationResult(false, "reading_sessions(com.nur.quran.data.db.entities.ReadingSessionEntity).\n"
                  + " Expected:\n" + _infoReadingSessions + "\n"
                  + " Found:\n" + _existingReadingSessions);
        }
        final HashMap<String, TableInfo.Column> _columnsRecentlyRead = new HashMap<String, TableInfo.Column>(4);
        _columnsRecentlyRead.put("chapterId", new TableInfo.Column("chapterId", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRecentlyRead.put("chapterName", new TableInfo.Column("chapterName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRecentlyRead.put("verseKey", new TableInfo.Column("verseKey", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRecentlyRead.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysRecentlyRead = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesRecentlyRead = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoRecentlyRead = new TableInfo("recently_read", _columnsRecentlyRead, _foreignKeysRecentlyRead, _indicesRecentlyRead);
        final TableInfo _existingRecentlyRead = TableInfo.read(db, "recently_read");
        if (!_infoRecentlyRead.equals(_existingRecentlyRead)) {
          return new RoomOpenHelper.ValidationResult(false, "recently_read(com.nur.quran.data.db.entities.RecentlyReadEntity).\n"
                  + " Expected:\n" + _infoRecentlyRead + "\n"
                  + " Found:\n" + _existingRecentlyRead);
        }
        final HashMap<String, TableInfo.Column> _columnsLinkedTimings = new HashMap<String, TableInfo.Column>(4);
        _columnsLinkedTimings.put("reciterId", new TableInfo.Column("reciterId", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLinkedTimings.put("sura", new TableInfo.Column("sura", "INTEGER", true, 2, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLinkedTimings.put("ayah", new TableInfo.Column("ayah", "INTEGER", true, 3, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLinkedTimings.put("startMs", new TableInfo.Column("startMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysLinkedTimings = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesLinkedTimings = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoLinkedTimings = new TableInfo("linked_timings", _columnsLinkedTimings, _foreignKeysLinkedTimings, _indicesLinkedTimings);
        final TableInfo _existingLinkedTimings = TableInfo.read(db, "linked_timings");
        if (!_infoLinkedTimings.equals(_existingLinkedTimings)) {
          return new RoomOpenHelper.ValidationResult(false, "linked_timings(com.nur.quran.data.db.entities.LinkedTimingEntity).\n"
                  + " Expected:\n" + _infoLinkedTimings + "\n"
                  + " Found:\n" + _existingLinkedTimings);
        }
        final HashMap<String, TableInfo.Column> _columnsTranslationTexts = new HashMap<String, TableInfo.Column>(3);
        _columnsTranslationTexts.put("translationId", new TableInfo.Column("translationId", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTranslationTexts.put("verseKey", new TableInfo.Column("verseKey", "TEXT", true, 2, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTranslationTexts.put("text", new TableInfo.Column("text", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysTranslationTexts = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesTranslationTexts = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoTranslationTexts = new TableInfo("translation_texts", _columnsTranslationTexts, _foreignKeysTranslationTexts, _indicesTranslationTexts);
        final TableInfo _existingTranslationTexts = TableInfo.read(db, "translation_texts");
        if (!_infoTranslationTexts.equals(_existingTranslationTexts)) {
          return new RoomOpenHelper.ValidationResult(false, "translation_texts(com.nur.quran.data.db.entities.TranslationTextEntity).\n"
                  + " Expected:\n" + _infoTranslationTexts + "\n"
                  + " Found:\n" + _existingTranslationTexts);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "0f743a9248a864da6e0b3076e96bf2ba", "c225cb0494a7e10b351df01e9b57d6b9");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "chapters","verses","words","bookmarks","collections","collection_items","api_responses","reading_sessions","recently_read","linked_timings","translation_texts");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    final boolean _supportsDeferForeignKeys = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP;
    try {
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = FALSE");
      }
      super.beginTransaction();
      if (_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA defer_foreign_keys = TRUE");
      }
      _db.execSQL("DELETE FROM `chapters`");
      _db.execSQL("DELETE FROM `verses`");
      _db.execSQL("DELETE FROM `words`");
      _db.execSQL("DELETE FROM `bookmarks`");
      _db.execSQL("DELETE FROM `collections`");
      _db.execSQL("DELETE FROM `collection_items`");
      _db.execSQL("DELETE FROM `api_responses`");
      _db.execSQL("DELETE FROM `reading_sessions`");
      _db.execSQL("DELETE FROM `recently_read`");
      _db.execSQL("DELETE FROM `linked_timings`");
      _db.execSQL("DELETE FROM `translation_texts`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = TRUE");
      }
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(QuranDao.class, QuranDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public QuranDao quranDao() {
    if (_quranDao != null) {
      return _quranDao;
    } else {
      synchronized(this) {
        if(_quranDao == null) {
          _quranDao = new QuranDao_Impl(this);
        }
        return _quranDao;
      }
    }
  }
}
