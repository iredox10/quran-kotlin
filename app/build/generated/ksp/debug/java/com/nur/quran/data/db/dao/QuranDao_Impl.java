package com.nur.quran.data.db.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomDatabaseKt;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.room.util.StringUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.nur.quran.data.db.entities.ApiResponseCacheEntity;
import com.nur.quran.data.db.entities.BookmarkEntity;
import com.nur.quran.data.db.entities.ChapterEntity;
import com.nur.quran.data.db.entities.CollectionEntity;
import com.nur.quran.data.db.entities.CollectionItemEntity;
import com.nur.quran.data.db.entities.ReadingSessionEntity;
import com.nur.quran.data.db.entities.RecentlyReadEntity;
import com.nur.quran.data.db.entities.VerseEntity;
import com.nur.quran.data.db.entities.WordEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class QuranDao_Impl implements QuranDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<ChapterEntity> __insertionAdapterOfChapterEntity;

  private final EntityInsertionAdapter<VerseEntity> __insertionAdapterOfVerseEntity;

  private final EntityInsertionAdapter<WordEntity> __insertionAdapterOfWordEntity;

  private final EntityInsertionAdapter<BookmarkEntity> __insertionAdapterOfBookmarkEntity;

  private final EntityInsertionAdapter<CollectionEntity> __insertionAdapterOfCollectionEntity;

  private final EntityInsertionAdapter<CollectionItemEntity> __insertionAdapterOfCollectionItemEntity;

  private final EntityInsertionAdapter<ApiResponseCacheEntity> __insertionAdapterOfApiResponseCacheEntity;

  private final EntityInsertionAdapter<ReadingSessionEntity> __insertionAdapterOfReadingSessionEntity;

  private final EntityInsertionAdapter<RecentlyReadEntity> __insertionAdapterOfRecentlyReadEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteBookmark;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAllBookmarks;

  private final SharedSQLiteStatement __preparedStmtOfDeleteCollection;

  private final SharedSQLiteStatement __preparedStmtOfDeleteCollectionItem;

  private final SharedSQLiteStatement __preparedStmtOfClearCacheByPrefix;

  private final SharedSQLiteStatement __preparedStmtOfPruneReadingSessions;

  private final SharedSQLiteStatement __preparedStmtOfPruneRecentlyRead;

  public QuranDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfChapterEntity = new EntityInsertionAdapter<ChapterEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `chapters` (`id`,`nameSimple`,`nameArabic`,`nameComplex`,`translatedName`,`revelationPlace`,`revelationOrder`,`versesCount`,`pagesStart`,`pagesEnd`) VALUES (?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ChapterEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getNameSimple());
        statement.bindString(3, entity.getNameArabic());
        statement.bindString(4, entity.getNameComplex());
        statement.bindString(5, entity.getTranslatedName());
        statement.bindString(6, entity.getRevelationPlace());
        statement.bindLong(7, entity.getRevelationOrder());
        statement.bindLong(8, entity.getVersesCount());
        statement.bindLong(9, entity.getPagesStart());
        statement.bindLong(10, entity.getPagesEnd());
      }
    };
    this.__insertionAdapterOfVerseEntity = new EntityInsertionAdapter<VerseEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `verses` (`id`,`chapterId`,`verseNumber`,`verseKey`,`textUthmani`,`textIndopak`,`textQpcHafs`,`pageNumber`,`juzNumber`,`translation`,`audioUrl`) VALUES (?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final VerseEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getChapterId());
        statement.bindLong(3, entity.getVerseNumber());
        statement.bindString(4, entity.getVerseKey());
        if (entity.getTextUthmani() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getTextUthmani());
        }
        if (entity.getTextIndopak() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getTextIndopak());
        }
        if (entity.getTextQpcHafs() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getTextQpcHafs());
        }
        statement.bindLong(8, entity.getPageNumber());
        statement.bindLong(9, entity.getJuzNumber());
        if (entity.getTranslation() == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, entity.getTranslation());
        }
        if (entity.getAudioUrl() == null) {
          statement.bindNull(11);
        } else {
          statement.bindString(11, entity.getAudioUrl());
        }
      }
    };
    this.__insertionAdapterOfWordEntity = new EntityInsertionAdapter<WordEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `words` (`id`,`verseId`,`position`,`textUthmani`,`textIndopak`,`textQpcHafs`,`textUthmaniTajweed`,`translation`,`transliteration`,`charTypeName`) VALUES (?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final WordEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getVerseId());
        statement.bindLong(3, entity.getPosition());
        if (entity.getTextUthmani() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getTextUthmani());
        }
        if (entity.getTextIndopak() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getTextIndopak());
        }
        if (entity.getTextQpcHafs() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getTextQpcHafs());
        }
        if (entity.getTextUthmaniTajweed() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getTextUthmaniTajweed());
        }
        if (entity.getTranslation() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getTranslation());
        }
        if (entity.getTransliteration() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getTransliteration());
        }
        statement.bindString(10, entity.getCharTypeName());
      }
    };
    this.__insertionAdapterOfBookmarkEntity = new EntityInsertionAdapter<BookmarkEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `bookmarks` (`id`,`verseKey`,`chapterId`,`surahName`,`timestamp`) VALUES (?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final BookmarkEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getVerseKey());
        statement.bindLong(3, entity.getChapterId());
        statement.bindString(4, entity.getSurahName());
        statement.bindLong(5, entity.getTimestamp());
      }
    };
    this.__insertionAdapterOfCollectionEntity = new EntityInsertionAdapter<CollectionEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `collections` (`id`,`name`,`createdAt`) VALUES (?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CollectionEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindLong(3, entity.getCreatedAt());
      }
    };
    this.__insertionAdapterOfCollectionItemEntity = new EntityInsertionAdapter<CollectionItemEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `collection_items` (`collectionId`,`verseKey`,`chapterId`,`surahName`,`addedAt`) VALUES (?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CollectionItemEntity entity) {
        statement.bindLong(1, entity.getCollectionId());
        statement.bindString(2, entity.getVerseKey());
        statement.bindLong(3, entity.getChapterId());
        statement.bindString(4, entity.getSurahName());
        statement.bindLong(5, entity.getAddedAt());
      }
    };
    this.__insertionAdapterOfApiResponseCacheEntity = new EntityInsertionAdapter<ApiResponseCacheEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `api_responses` (`key`,`dataJson`,`updatedAt`) VALUES (?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ApiResponseCacheEntity entity) {
        statement.bindString(1, entity.getKey());
        statement.bindString(2, entity.getDataJson());
        statement.bindLong(3, entity.getUpdatedAt());
      }
    };
    this.__insertionAdapterOfReadingSessionEntity = new EntityInsertionAdapter<ReadingSessionEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `reading_sessions` (`id`,`date`,`duration`,`type`,`chapterId`,`timestamp`) VALUES (nullif(?, 0),?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ReadingSessionEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getDate());
        statement.bindLong(3, entity.getDuration());
        statement.bindString(4, entity.getType());
        if (entity.getChapterId() == null) {
          statement.bindNull(5);
        } else {
          statement.bindLong(5, entity.getChapterId());
        }
        statement.bindLong(6, entity.getTimestamp());
      }
    };
    this.__insertionAdapterOfRecentlyReadEntity = new EntityInsertionAdapter<RecentlyReadEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `recently_read` (`chapterId`,`chapterName`,`verseKey`,`timestamp`) VALUES (?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final RecentlyReadEntity entity) {
        statement.bindLong(1, entity.getChapterId());
        statement.bindString(2, entity.getChapterName());
        if (entity.getVerseKey() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getVerseKey());
        }
        statement.bindLong(4, entity.getTimestamp());
      }
    };
    this.__preparedStmtOfDeleteBookmark = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM bookmarks WHERE verseKey = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteAllBookmarks = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM bookmarks";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteCollection = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM collections WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteCollectionItem = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM collection_items WHERE collectionId = ? AND verseKey = ?";
        return _query;
      }
    };
    this.__preparedStmtOfClearCacheByPrefix = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM api_responses WHERE `key` LIKE ? || '%'";
        return _query;
      }
    };
    this.__preparedStmtOfPruneReadingSessions = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM reading_sessions WHERE id NOT IN (SELECT id FROM reading_sessions ORDER BY timestamp DESC LIMIT 500)";
        return _query;
      }
    };
    this.__preparedStmtOfPruneRecentlyRead = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM recently_read WHERE chapterId NOT IN (SELECT chapterId FROM recently_read ORDER BY timestamp DESC LIMIT 5)";
        return _query;
      }
    };
  }

  @Override
  public Object insertChapters(final List<ChapterEntity> chapters,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfChapterEntity.insert(chapters);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertVerses(final List<VerseEntity> verses,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfVerseEntity.insert(verses);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertWords(final List<WordEntity> words,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfWordEntity.insert(words);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertBookmark(final BookmarkEntity bookmark,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfBookmarkEntity.insert(bookmark);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertCollection(final CollectionEntity collection,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfCollectionEntity.insert(collection);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertCollectionItem(final CollectionItemEntity item,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfCollectionItemEntity.insert(item);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertCacheEntry(final ApiResponseCacheEntity entry,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfApiResponseCacheEntity.insert(entry);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertReadingSession(final ReadingSessionEntity session,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfReadingSessionEntity.insert(session);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object upsertRecentlyRead(final RecentlyReadEntity item,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfRecentlyReadEntity.insert(item);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertVersesAndWords(final List<VerseEntity> verses, final List<WordEntity> words,
      final Continuation<? super Unit> $completion) {
    return RoomDatabaseKt.withTransaction(__db, (__cont) -> QuranDao.DefaultImpls.insertVersesAndWords(QuranDao_Impl.this, verses, words, __cont), $completion);
  }

  @Override
  public Object deleteBookmark(final String verseKey,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteBookmark.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, verseKey);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteBookmark.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAllBookmarks(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAllBookmarks.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteAllBookmarks.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteCollection(final long collectionId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteCollection.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, collectionId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteCollection.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteCollectionItem(final long collectionId, final String verseKey,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteCollectionItem.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, collectionId);
        _argIndex = 2;
        _stmt.bindString(_argIndex, verseKey);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteCollectionItem.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object clearCacheByPrefix(final String prefix,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearCacheByPrefix.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, prefix);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfClearCacheByPrefix.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object pruneReadingSessions(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfPruneReadingSessions.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfPruneReadingSessions.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object pruneRecentlyRead(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfPruneRecentlyRead.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfPruneRecentlyRead.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<ChapterEntity>> getAllChapters() {
    final String _sql = "SELECT * FROM chapters ORDER BY id ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"chapters"}, new Callable<List<ChapterEntity>>() {
      @Override
      @NonNull
      public List<ChapterEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfNameSimple = CursorUtil.getColumnIndexOrThrow(_cursor, "nameSimple");
          final int _cursorIndexOfNameArabic = CursorUtil.getColumnIndexOrThrow(_cursor, "nameArabic");
          final int _cursorIndexOfNameComplex = CursorUtil.getColumnIndexOrThrow(_cursor, "nameComplex");
          final int _cursorIndexOfTranslatedName = CursorUtil.getColumnIndexOrThrow(_cursor, "translatedName");
          final int _cursorIndexOfRevelationPlace = CursorUtil.getColumnIndexOrThrow(_cursor, "revelationPlace");
          final int _cursorIndexOfRevelationOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "revelationOrder");
          final int _cursorIndexOfVersesCount = CursorUtil.getColumnIndexOrThrow(_cursor, "versesCount");
          final int _cursorIndexOfPagesStart = CursorUtil.getColumnIndexOrThrow(_cursor, "pagesStart");
          final int _cursorIndexOfPagesEnd = CursorUtil.getColumnIndexOrThrow(_cursor, "pagesEnd");
          final List<ChapterEntity> _result = new ArrayList<ChapterEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ChapterEntity _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpNameSimple;
            _tmpNameSimple = _cursor.getString(_cursorIndexOfNameSimple);
            final String _tmpNameArabic;
            _tmpNameArabic = _cursor.getString(_cursorIndexOfNameArabic);
            final String _tmpNameComplex;
            _tmpNameComplex = _cursor.getString(_cursorIndexOfNameComplex);
            final String _tmpTranslatedName;
            _tmpTranslatedName = _cursor.getString(_cursorIndexOfTranslatedName);
            final String _tmpRevelationPlace;
            _tmpRevelationPlace = _cursor.getString(_cursorIndexOfRevelationPlace);
            final int _tmpRevelationOrder;
            _tmpRevelationOrder = _cursor.getInt(_cursorIndexOfRevelationOrder);
            final int _tmpVersesCount;
            _tmpVersesCount = _cursor.getInt(_cursorIndexOfVersesCount);
            final int _tmpPagesStart;
            _tmpPagesStart = _cursor.getInt(_cursorIndexOfPagesStart);
            final int _tmpPagesEnd;
            _tmpPagesEnd = _cursor.getInt(_cursorIndexOfPagesEnd);
            _item = new ChapterEntity(_tmpId,_tmpNameSimple,_tmpNameArabic,_tmpNameComplex,_tmpTranslatedName,_tmpRevelationPlace,_tmpRevelationOrder,_tmpVersesCount,_tmpPagesStart,_tmpPagesEnd);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getAllChaptersDirect(final Continuation<? super List<ChapterEntity>> $completion) {
    final String _sql = "SELECT * FROM chapters ORDER BY id ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<ChapterEntity>>() {
      @Override
      @NonNull
      public List<ChapterEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfNameSimple = CursorUtil.getColumnIndexOrThrow(_cursor, "nameSimple");
          final int _cursorIndexOfNameArabic = CursorUtil.getColumnIndexOrThrow(_cursor, "nameArabic");
          final int _cursorIndexOfNameComplex = CursorUtil.getColumnIndexOrThrow(_cursor, "nameComplex");
          final int _cursorIndexOfTranslatedName = CursorUtil.getColumnIndexOrThrow(_cursor, "translatedName");
          final int _cursorIndexOfRevelationPlace = CursorUtil.getColumnIndexOrThrow(_cursor, "revelationPlace");
          final int _cursorIndexOfRevelationOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "revelationOrder");
          final int _cursorIndexOfVersesCount = CursorUtil.getColumnIndexOrThrow(_cursor, "versesCount");
          final int _cursorIndexOfPagesStart = CursorUtil.getColumnIndexOrThrow(_cursor, "pagesStart");
          final int _cursorIndexOfPagesEnd = CursorUtil.getColumnIndexOrThrow(_cursor, "pagesEnd");
          final List<ChapterEntity> _result = new ArrayList<ChapterEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ChapterEntity _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpNameSimple;
            _tmpNameSimple = _cursor.getString(_cursorIndexOfNameSimple);
            final String _tmpNameArabic;
            _tmpNameArabic = _cursor.getString(_cursorIndexOfNameArabic);
            final String _tmpNameComplex;
            _tmpNameComplex = _cursor.getString(_cursorIndexOfNameComplex);
            final String _tmpTranslatedName;
            _tmpTranslatedName = _cursor.getString(_cursorIndexOfTranslatedName);
            final String _tmpRevelationPlace;
            _tmpRevelationPlace = _cursor.getString(_cursorIndexOfRevelationPlace);
            final int _tmpRevelationOrder;
            _tmpRevelationOrder = _cursor.getInt(_cursorIndexOfRevelationOrder);
            final int _tmpVersesCount;
            _tmpVersesCount = _cursor.getInt(_cursorIndexOfVersesCount);
            final int _tmpPagesStart;
            _tmpPagesStart = _cursor.getInt(_cursorIndexOfPagesStart);
            final int _tmpPagesEnd;
            _tmpPagesEnd = _cursor.getInt(_cursorIndexOfPagesEnd);
            _item = new ChapterEntity(_tmpId,_tmpNameSimple,_tmpNameArabic,_tmpNameComplex,_tmpTranslatedName,_tmpRevelationPlace,_tmpRevelationOrder,_tmpVersesCount,_tmpPagesStart,_tmpPagesEnd);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getChapterById(final int chapterId,
      final Continuation<? super ChapterEntity> $completion) {
    final String _sql = "SELECT * FROM chapters WHERE id = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, chapterId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<ChapterEntity>() {
      @Override
      @Nullable
      public ChapterEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfNameSimple = CursorUtil.getColumnIndexOrThrow(_cursor, "nameSimple");
          final int _cursorIndexOfNameArabic = CursorUtil.getColumnIndexOrThrow(_cursor, "nameArabic");
          final int _cursorIndexOfNameComplex = CursorUtil.getColumnIndexOrThrow(_cursor, "nameComplex");
          final int _cursorIndexOfTranslatedName = CursorUtil.getColumnIndexOrThrow(_cursor, "translatedName");
          final int _cursorIndexOfRevelationPlace = CursorUtil.getColumnIndexOrThrow(_cursor, "revelationPlace");
          final int _cursorIndexOfRevelationOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "revelationOrder");
          final int _cursorIndexOfVersesCount = CursorUtil.getColumnIndexOrThrow(_cursor, "versesCount");
          final int _cursorIndexOfPagesStart = CursorUtil.getColumnIndexOrThrow(_cursor, "pagesStart");
          final int _cursorIndexOfPagesEnd = CursorUtil.getColumnIndexOrThrow(_cursor, "pagesEnd");
          final ChapterEntity _result;
          if (_cursor.moveToFirst()) {
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpNameSimple;
            _tmpNameSimple = _cursor.getString(_cursorIndexOfNameSimple);
            final String _tmpNameArabic;
            _tmpNameArabic = _cursor.getString(_cursorIndexOfNameArabic);
            final String _tmpNameComplex;
            _tmpNameComplex = _cursor.getString(_cursorIndexOfNameComplex);
            final String _tmpTranslatedName;
            _tmpTranslatedName = _cursor.getString(_cursorIndexOfTranslatedName);
            final String _tmpRevelationPlace;
            _tmpRevelationPlace = _cursor.getString(_cursorIndexOfRevelationPlace);
            final int _tmpRevelationOrder;
            _tmpRevelationOrder = _cursor.getInt(_cursorIndexOfRevelationOrder);
            final int _tmpVersesCount;
            _tmpVersesCount = _cursor.getInt(_cursorIndexOfVersesCount);
            final int _tmpPagesStart;
            _tmpPagesStart = _cursor.getInt(_cursorIndexOfPagesStart);
            final int _tmpPagesEnd;
            _tmpPagesEnd = _cursor.getInt(_cursorIndexOfPagesEnd);
            _result = new ChapterEntity(_tmpId,_tmpNameSimple,_tmpNameArabic,_tmpNameComplex,_tmpTranslatedName,_tmpRevelationPlace,_tmpRevelationOrder,_tmpVersesCount,_tmpPagesStart,_tmpPagesEnd);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<VerseEntity>> getVersesByChapter(final int chapterId) {
    final String _sql = "SELECT * FROM verses WHERE chapterId = ? ORDER BY verseNumber ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, chapterId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"verses"}, new Callable<List<VerseEntity>>() {
      @Override
      @NonNull
      public List<VerseEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfChapterId = CursorUtil.getColumnIndexOrThrow(_cursor, "chapterId");
          final int _cursorIndexOfVerseNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "verseNumber");
          final int _cursorIndexOfVerseKey = CursorUtil.getColumnIndexOrThrow(_cursor, "verseKey");
          final int _cursorIndexOfTextUthmani = CursorUtil.getColumnIndexOrThrow(_cursor, "textUthmani");
          final int _cursorIndexOfTextIndopak = CursorUtil.getColumnIndexOrThrow(_cursor, "textIndopak");
          final int _cursorIndexOfTextQpcHafs = CursorUtil.getColumnIndexOrThrow(_cursor, "textQpcHafs");
          final int _cursorIndexOfPageNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "pageNumber");
          final int _cursorIndexOfJuzNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "juzNumber");
          final int _cursorIndexOfTranslation = CursorUtil.getColumnIndexOrThrow(_cursor, "translation");
          final int _cursorIndexOfAudioUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "audioUrl");
          final List<VerseEntity> _result = new ArrayList<VerseEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final VerseEntity _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final int _tmpChapterId;
            _tmpChapterId = _cursor.getInt(_cursorIndexOfChapterId);
            final int _tmpVerseNumber;
            _tmpVerseNumber = _cursor.getInt(_cursorIndexOfVerseNumber);
            final String _tmpVerseKey;
            _tmpVerseKey = _cursor.getString(_cursorIndexOfVerseKey);
            final String _tmpTextUthmani;
            if (_cursor.isNull(_cursorIndexOfTextUthmani)) {
              _tmpTextUthmani = null;
            } else {
              _tmpTextUthmani = _cursor.getString(_cursorIndexOfTextUthmani);
            }
            final String _tmpTextIndopak;
            if (_cursor.isNull(_cursorIndexOfTextIndopak)) {
              _tmpTextIndopak = null;
            } else {
              _tmpTextIndopak = _cursor.getString(_cursorIndexOfTextIndopak);
            }
            final String _tmpTextQpcHafs;
            if (_cursor.isNull(_cursorIndexOfTextQpcHafs)) {
              _tmpTextQpcHafs = null;
            } else {
              _tmpTextQpcHafs = _cursor.getString(_cursorIndexOfTextQpcHafs);
            }
            final int _tmpPageNumber;
            _tmpPageNumber = _cursor.getInt(_cursorIndexOfPageNumber);
            final int _tmpJuzNumber;
            _tmpJuzNumber = _cursor.getInt(_cursorIndexOfJuzNumber);
            final String _tmpTranslation;
            if (_cursor.isNull(_cursorIndexOfTranslation)) {
              _tmpTranslation = null;
            } else {
              _tmpTranslation = _cursor.getString(_cursorIndexOfTranslation);
            }
            final String _tmpAudioUrl;
            if (_cursor.isNull(_cursorIndexOfAudioUrl)) {
              _tmpAudioUrl = null;
            } else {
              _tmpAudioUrl = _cursor.getString(_cursorIndexOfAudioUrl);
            }
            _item = new VerseEntity(_tmpId,_tmpChapterId,_tmpVerseNumber,_tmpVerseKey,_tmpTextUthmani,_tmpTextIndopak,_tmpTextQpcHafs,_tmpPageNumber,_tmpJuzNumber,_tmpTranslation,_tmpAudioUrl);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getVersesByChapterDirect(final int chapterId,
      final Continuation<? super List<VerseEntity>> $completion) {
    final String _sql = "SELECT * FROM verses WHERE chapterId = ? ORDER BY verseNumber ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, chapterId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<VerseEntity>>() {
      @Override
      @NonNull
      public List<VerseEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfChapterId = CursorUtil.getColumnIndexOrThrow(_cursor, "chapterId");
          final int _cursorIndexOfVerseNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "verseNumber");
          final int _cursorIndexOfVerseKey = CursorUtil.getColumnIndexOrThrow(_cursor, "verseKey");
          final int _cursorIndexOfTextUthmani = CursorUtil.getColumnIndexOrThrow(_cursor, "textUthmani");
          final int _cursorIndexOfTextIndopak = CursorUtil.getColumnIndexOrThrow(_cursor, "textIndopak");
          final int _cursorIndexOfTextQpcHafs = CursorUtil.getColumnIndexOrThrow(_cursor, "textQpcHafs");
          final int _cursorIndexOfPageNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "pageNumber");
          final int _cursorIndexOfJuzNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "juzNumber");
          final int _cursorIndexOfTranslation = CursorUtil.getColumnIndexOrThrow(_cursor, "translation");
          final int _cursorIndexOfAudioUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "audioUrl");
          final List<VerseEntity> _result = new ArrayList<VerseEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final VerseEntity _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final int _tmpChapterId;
            _tmpChapterId = _cursor.getInt(_cursorIndexOfChapterId);
            final int _tmpVerseNumber;
            _tmpVerseNumber = _cursor.getInt(_cursorIndexOfVerseNumber);
            final String _tmpVerseKey;
            _tmpVerseKey = _cursor.getString(_cursorIndexOfVerseKey);
            final String _tmpTextUthmani;
            if (_cursor.isNull(_cursorIndexOfTextUthmani)) {
              _tmpTextUthmani = null;
            } else {
              _tmpTextUthmani = _cursor.getString(_cursorIndexOfTextUthmani);
            }
            final String _tmpTextIndopak;
            if (_cursor.isNull(_cursorIndexOfTextIndopak)) {
              _tmpTextIndopak = null;
            } else {
              _tmpTextIndopak = _cursor.getString(_cursorIndexOfTextIndopak);
            }
            final String _tmpTextQpcHafs;
            if (_cursor.isNull(_cursorIndexOfTextQpcHafs)) {
              _tmpTextQpcHafs = null;
            } else {
              _tmpTextQpcHafs = _cursor.getString(_cursorIndexOfTextQpcHafs);
            }
            final int _tmpPageNumber;
            _tmpPageNumber = _cursor.getInt(_cursorIndexOfPageNumber);
            final int _tmpJuzNumber;
            _tmpJuzNumber = _cursor.getInt(_cursorIndexOfJuzNumber);
            final String _tmpTranslation;
            if (_cursor.isNull(_cursorIndexOfTranslation)) {
              _tmpTranslation = null;
            } else {
              _tmpTranslation = _cursor.getString(_cursorIndexOfTranslation);
            }
            final String _tmpAudioUrl;
            if (_cursor.isNull(_cursorIndexOfAudioUrl)) {
              _tmpAudioUrl = null;
            } else {
              _tmpAudioUrl = _cursor.getString(_cursorIndexOfAudioUrl);
            }
            _item = new VerseEntity(_tmpId,_tmpChapterId,_tmpVerseNumber,_tmpVerseKey,_tmpTextUthmani,_tmpTextIndopak,_tmpTextQpcHafs,_tmpPageNumber,_tmpJuzNumber,_tmpTranslation,_tmpAudioUrl);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<VerseEntity>> getVersesByPage(final int pageNumber) {
    final String _sql = "SELECT * FROM verses WHERE pageNumber = ? ORDER BY chapterId ASC, verseNumber ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, pageNumber);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"verses"}, new Callable<List<VerseEntity>>() {
      @Override
      @NonNull
      public List<VerseEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfChapterId = CursorUtil.getColumnIndexOrThrow(_cursor, "chapterId");
          final int _cursorIndexOfVerseNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "verseNumber");
          final int _cursorIndexOfVerseKey = CursorUtil.getColumnIndexOrThrow(_cursor, "verseKey");
          final int _cursorIndexOfTextUthmani = CursorUtil.getColumnIndexOrThrow(_cursor, "textUthmani");
          final int _cursorIndexOfTextIndopak = CursorUtil.getColumnIndexOrThrow(_cursor, "textIndopak");
          final int _cursorIndexOfTextQpcHafs = CursorUtil.getColumnIndexOrThrow(_cursor, "textQpcHafs");
          final int _cursorIndexOfPageNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "pageNumber");
          final int _cursorIndexOfJuzNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "juzNumber");
          final int _cursorIndexOfTranslation = CursorUtil.getColumnIndexOrThrow(_cursor, "translation");
          final int _cursorIndexOfAudioUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "audioUrl");
          final List<VerseEntity> _result = new ArrayList<VerseEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final VerseEntity _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final int _tmpChapterId;
            _tmpChapterId = _cursor.getInt(_cursorIndexOfChapterId);
            final int _tmpVerseNumber;
            _tmpVerseNumber = _cursor.getInt(_cursorIndexOfVerseNumber);
            final String _tmpVerseKey;
            _tmpVerseKey = _cursor.getString(_cursorIndexOfVerseKey);
            final String _tmpTextUthmani;
            if (_cursor.isNull(_cursorIndexOfTextUthmani)) {
              _tmpTextUthmani = null;
            } else {
              _tmpTextUthmani = _cursor.getString(_cursorIndexOfTextUthmani);
            }
            final String _tmpTextIndopak;
            if (_cursor.isNull(_cursorIndexOfTextIndopak)) {
              _tmpTextIndopak = null;
            } else {
              _tmpTextIndopak = _cursor.getString(_cursorIndexOfTextIndopak);
            }
            final String _tmpTextQpcHafs;
            if (_cursor.isNull(_cursorIndexOfTextQpcHafs)) {
              _tmpTextQpcHafs = null;
            } else {
              _tmpTextQpcHafs = _cursor.getString(_cursorIndexOfTextQpcHafs);
            }
            final int _tmpPageNumber;
            _tmpPageNumber = _cursor.getInt(_cursorIndexOfPageNumber);
            final int _tmpJuzNumber;
            _tmpJuzNumber = _cursor.getInt(_cursorIndexOfJuzNumber);
            final String _tmpTranslation;
            if (_cursor.isNull(_cursorIndexOfTranslation)) {
              _tmpTranslation = null;
            } else {
              _tmpTranslation = _cursor.getString(_cursorIndexOfTranslation);
            }
            final String _tmpAudioUrl;
            if (_cursor.isNull(_cursorIndexOfAudioUrl)) {
              _tmpAudioUrl = null;
            } else {
              _tmpAudioUrl = _cursor.getString(_cursorIndexOfAudioUrl);
            }
            _item = new VerseEntity(_tmpId,_tmpChapterId,_tmpVerseNumber,_tmpVerseKey,_tmpTextUthmani,_tmpTextIndopak,_tmpTextQpcHafs,_tmpPageNumber,_tmpJuzNumber,_tmpTranslation,_tmpAudioUrl);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getWordsForVerse(final int verseId,
      final Continuation<? super List<WordEntity>> $completion) {
    final String _sql = "SELECT * FROM words WHERE verseId = ? ORDER BY position ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, verseId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<WordEntity>>() {
      @Override
      @NonNull
      public List<WordEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfVerseId = CursorUtil.getColumnIndexOrThrow(_cursor, "verseId");
          final int _cursorIndexOfPosition = CursorUtil.getColumnIndexOrThrow(_cursor, "position");
          final int _cursorIndexOfTextUthmani = CursorUtil.getColumnIndexOrThrow(_cursor, "textUthmani");
          final int _cursorIndexOfTextIndopak = CursorUtil.getColumnIndexOrThrow(_cursor, "textIndopak");
          final int _cursorIndexOfTextQpcHafs = CursorUtil.getColumnIndexOrThrow(_cursor, "textQpcHafs");
          final int _cursorIndexOfTextUthmaniTajweed = CursorUtil.getColumnIndexOrThrow(_cursor, "textUthmaniTajweed");
          final int _cursorIndexOfTranslation = CursorUtil.getColumnIndexOrThrow(_cursor, "translation");
          final int _cursorIndexOfTransliteration = CursorUtil.getColumnIndexOrThrow(_cursor, "transliteration");
          final int _cursorIndexOfCharTypeName = CursorUtil.getColumnIndexOrThrow(_cursor, "charTypeName");
          final List<WordEntity> _result = new ArrayList<WordEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final WordEntity _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final int _tmpVerseId;
            _tmpVerseId = _cursor.getInt(_cursorIndexOfVerseId);
            final int _tmpPosition;
            _tmpPosition = _cursor.getInt(_cursorIndexOfPosition);
            final String _tmpTextUthmani;
            if (_cursor.isNull(_cursorIndexOfTextUthmani)) {
              _tmpTextUthmani = null;
            } else {
              _tmpTextUthmani = _cursor.getString(_cursorIndexOfTextUthmani);
            }
            final String _tmpTextIndopak;
            if (_cursor.isNull(_cursorIndexOfTextIndopak)) {
              _tmpTextIndopak = null;
            } else {
              _tmpTextIndopak = _cursor.getString(_cursorIndexOfTextIndopak);
            }
            final String _tmpTextQpcHafs;
            if (_cursor.isNull(_cursorIndexOfTextQpcHafs)) {
              _tmpTextQpcHafs = null;
            } else {
              _tmpTextQpcHafs = _cursor.getString(_cursorIndexOfTextQpcHafs);
            }
            final String _tmpTextUthmaniTajweed;
            if (_cursor.isNull(_cursorIndexOfTextUthmaniTajweed)) {
              _tmpTextUthmaniTajweed = null;
            } else {
              _tmpTextUthmaniTajweed = _cursor.getString(_cursorIndexOfTextUthmaniTajweed);
            }
            final String _tmpTranslation;
            if (_cursor.isNull(_cursorIndexOfTranslation)) {
              _tmpTranslation = null;
            } else {
              _tmpTranslation = _cursor.getString(_cursorIndexOfTranslation);
            }
            final String _tmpTransliteration;
            if (_cursor.isNull(_cursorIndexOfTransliteration)) {
              _tmpTransliteration = null;
            } else {
              _tmpTransliteration = _cursor.getString(_cursorIndexOfTransliteration);
            }
            final String _tmpCharTypeName;
            _tmpCharTypeName = _cursor.getString(_cursorIndexOfCharTypeName);
            _item = new WordEntity(_tmpId,_tmpVerseId,_tmpPosition,_tmpTextUthmani,_tmpTextIndopak,_tmpTextQpcHafs,_tmpTextUthmaniTajweed,_tmpTranslation,_tmpTransliteration,_tmpCharTypeName);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getWordsForVerses(final List<Integer> verseIds,
      final Continuation<? super List<WordEntity>> $completion) {
    final StringBuilder _stringBuilder = StringUtil.newStringBuilder();
    _stringBuilder.append("SELECT * FROM words WHERE verseId IN (");
    final int _inputSize = verseIds.size();
    StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
    _stringBuilder.append(") ORDER BY verseId ASC, position ASC");
    final String _sql = _stringBuilder.toString();
    final int _argCount = 0 + _inputSize;
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, _argCount);
    int _argIndex = 1;
    for (int _item : verseIds) {
      _statement.bindLong(_argIndex, _item);
      _argIndex++;
    }
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<WordEntity>>() {
      @Override
      @NonNull
      public List<WordEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfVerseId = CursorUtil.getColumnIndexOrThrow(_cursor, "verseId");
          final int _cursorIndexOfPosition = CursorUtil.getColumnIndexOrThrow(_cursor, "position");
          final int _cursorIndexOfTextUthmani = CursorUtil.getColumnIndexOrThrow(_cursor, "textUthmani");
          final int _cursorIndexOfTextIndopak = CursorUtil.getColumnIndexOrThrow(_cursor, "textIndopak");
          final int _cursorIndexOfTextQpcHafs = CursorUtil.getColumnIndexOrThrow(_cursor, "textQpcHafs");
          final int _cursorIndexOfTextUthmaniTajweed = CursorUtil.getColumnIndexOrThrow(_cursor, "textUthmaniTajweed");
          final int _cursorIndexOfTranslation = CursorUtil.getColumnIndexOrThrow(_cursor, "translation");
          final int _cursorIndexOfTransliteration = CursorUtil.getColumnIndexOrThrow(_cursor, "transliteration");
          final int _cursorIndexOfCharTypeName = CursorUtil.getColumnIndexOrThrow(_cursor, "charTypeName");
          final List<WordEntity> _result = new ArrayList<WordEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final WordEntity _item_1;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final int _tmpVerseId;
            _tmpVerseId = _cursor.getInt(_cursorIndexOfVerseId);
            final int _tmpPosition;
            _tmpPosition = _cursor.getInt(_cursorIndexOfPosition);
            final String _tmpTextUthmani;
            if (_cursor.isNull(_cursorIndexOfTextUthmani)) {
              _tmpTextUthmani = null;
            } else {
              _tmpTextUthmani = _cursor.getString(_cursorIndexOfTextUthmani);
            }
            final String _tmpTextIndopak;
            if (_cursor.isNull(_cursorIndexOfTextIndopak)) {
              _tmpTextIndopak = null;
            } else {
              _tmpTextIndopak = _cursor.getString(_cursorIndexOfTextIndopak);
            }
            final String _tmpTextQpcHafs;
            if (_cursor.isNull(_cursorIndexOfTextQpcHafs)) {
              _tmpTextQpcHafs = null;
            } else {
              _tmpTextQpcHafs = _cursor.getString(_cursorIndexOfTextQpcHafs);
            }
            final String _tmpTextUthmaniTajweed;
            if (_cursor.isNull(_cursorIndexOfTextUthmaniTajweed)) {
              _tmpTextUthmaniTajweed = null;
            } else {
              _tmpTextUthmaniTajweed = _cursor.getString(_cursorIndexOfTextUthmaniTajweed);
            }
            final String _tmpTranslation;
            if (_cursor.isNull(_cursorIndexOfTranslation)) {
              _tmpTranslation = null;
            } else {
              _tmpTranslation = _cursor.getString(_cursorIndexOfTranslation);
            }
            final String _tmpTransliteration;
            if (_cursor.isNull(_cursorIndexOfTransliteration)) {
              _tmpTransliteration = null;
            } else {
              _tmpTransliteration = _cursor.getString(_cursorIndexOfTransliteration);
            }
            final String _tmpCharTypeName;
            _tmpCharTypeName = _cursor.getString(_cursorIndexOfCharTypeName);
            _item_1 = new WordEntity(_tmpId,_tmpVerseId,_tmpPosition,_tmpTextUthmani,_tmpTextIndopak,_tmpTextQpcHafs,_tmpTextUthmaniTajweed,_tmpTranslation,_tmpTransliteration,_tmpCharTypeName);
            _result.add(_item_1);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<BookmarkEntity>> getAllBookmarks() {
    final String _sql = "SELECT * FROM bookmarks ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"bookmarks"}, new Callable<List<BookmarkEntity>>() {
      @Override
      @NonNull
      public List<BookmarkEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfVerseKey = CursorUtil.getColumnIndexOrThrow(_cursor, "verseKey");
          final int _cursorIndexOfChapterId = CursorUtil.getColumnIndexOrThrow(_cursor, "chapterId");
          final int _cursorIndexOfSurahName = CursorUtil.getColumnIndexOrThrow(_cursor, "surahName");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final List<BookmarkEntity> _result = new ArrayList<BookmarkEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final BookmarkEntity _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpVerseKey;
            _tmpVerseKey = _cursor.getString(_cursorIndexOfVerseKey);
            final int _tmpChapterId;
            _tmpChapterId = _cursor.getInt(_cursorIndexOfChapterId);
            final String _tmpSurahName;
            _tmpSurahName = _cursor.getString(_cursorIndexOfSurahName);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            _item = new BookmarkEntity(_tmpId,_tmpVerseKey,_tmpChapterId,_tmpSurahName,_tmpTimestamp);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<String>> getBookmarkedVerseKeys() {
    final String _sql = "SELECT verseKey FROM bookmarks";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"bookmarks"}, new Callable<List<String>>() {
      @Override
      @NonNull
      public List<String> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final List<String> _result = new ArrayList<String>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final String _item;
            _item = _cursor.getString(0);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getBookmarkByVerse(final String verseKey,
      final Continuation<? super BookmarkEntity> $completion) {
    final String _sql = "SELECT * FROM bookmarks WHERE verseKey = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, verseKey);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<BookmarkEntity>() {
      @Override
      @Nullable
      public BookmarkEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfVerseKey = CursorUtil.getColumnIndexOrThrow(_cursor, "verseKey");
          final int _cursorIndexOfChapterId = CursorUtil.getColumnIndexOrThrow(_cursor, "chapterId");
          final int _cursorIndexOfSurahName = CursorUtil.getColumnIndexOrThrow(_cursor, "surahName");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final BookmarkEntity _result;
          if (_cursor.moveToFirst()) {
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpVerseKey;
            _tmpVerseKey = _cursor.getString(_cursorIndexOfVerseKey);
            final int _tmpChapterId;
            _tmpChapterId = _cursor.getInt(_cursorIndexOfChapterId);
            final String _tmpSurahName;
            _tmpSurahName = _cursor.getString(_cursorIndexOfSurahName);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            _result = new BookmarkEntity(_tmpId,_tmpVerseKey,_tmpChapterId,_tmpSurahName,_tmpTimestamp);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<CollectionEntity>> getAllCollections() {
    final String _sql = "SELECT * FROM collections ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"collections"}, new Callable<List<CollectionEntity>>() {
      @Override
      @NonNull
      public List<CollectionEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<CollectionEntity> _result = new ArrayList<CollectionEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CollectionEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new CollectionEntity(_tmpId,_tmpName,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<CollectionItemEntity>> getItemsForCollection(final long collectionId) {
    final String _sql = "SELECT * FROM collection_items WHERE collectionId = ? ORDER BY addedAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, collectionId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"collection_items"}, new Callable<List<CollectionItemEntity>>() {
      @Override
      @NonNull
      public List<CollectionItemEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfCollectionId = CursorUtil.getColumnIndexOrThrow(_cursor, "collectionId");
          final int _cursorIndexOfVerseKey = CursorUtil.getColumnIndexOrThrow(_cursor, "verseKey");
          final int _cursorIndexOfChapterId = CursorUtil.getColumnIndexOrThrow(_cursor, "chapterId");
          final int _cursorIndexOfSurahName = CursorUtil.getColumnIndexOrThrow(_cursor, "surahName");
          final int _cursorIndexOfAddedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "addedAt");
          final List<CollectionItemEntity> _result = new ArrayList<CollectionItemEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CollectionItemEntity _item;
            final long _tmpCollectionId;
            _tmpCollectionId = _cursor.getLong(_cursorIndexOfCollectionId);
            final String _tmpVerseKey;
            _tmpVerseKey = _cursor.getString(_cursorIndexOfVerseKey);
            final int _tmpChapterId;
            _tmpChapterId = _cursor.getInt(_cursorIndexOfChapterId);
            final String _tmpSurahName;
            _tmpSurahName = _cursor.getString(_cursorIndexOfSurahName);
            final long _tmpAddedAt;
            _tmpAddedAt = _cursor.getLong(_cursorIndexOfAddedAt);
            _item = new CollectionItemEntity(_tmpCollectionId,_tmpVerseKey,_tmpChapterId,_tmpSurahName,_tmpAddedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<CollectionItemEntity>> getAllCollectionItems() {
    final String _sql = "SELECT * FROM collection_items";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"collection_items"}, new Callable<List<CollectionItemEntity>>() {
      @Override
      @NonNull
      public List<CollectionItemEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfCollectionId = CursorUtil.getColumnIndexOrThrow(_cursor, "collectionId");
          final int _cursorIndexOfVerseKey = CursorUtil.getColumnIndexOrThrow(_cursor, "verseKey");
          final int _cursorIndexOfChapterId = CursorUtil.getColumnIndexOrThrow(_cursor, "chapterId");
          final int _cursorIndexOfSurahName = CursorUtil.getColumnIndexOrThrow(_cursor, "surahName");
          final int _cursorIndexOfAddedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "addedAt");
          final List<CollectionItemEntity> _result = new ArrayList<CollectionItemEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CollectionItemEntity _item;
            final long _tmpCollectionId;
            _tmpCollectionId = _cursor.getLong(_cursorIndexOfCollectionId);
            final String _tmpVerseKey;
            _tmpVerseKey = _cursor.getString(_cursorIndexOfVerseKey);
            final int _tmpChapterId;
            _tmpChapterId = _cursor.getInt(_cursorIndexOfChapterId);
            final String _tmpSurahName;
            _tmpSurahName = _cursor.getString(_cursorIndexOfSurahName);
            final long _tmpAddedAt;
            _tmpAddedAt = _cursor.getLong(_cursorIndexOfAddedAt);
            _item = new CollectionItemEntity(_tmpCollectionId,_tmpVerseKey,_tmpChapterId,_tmpSurahName,_tmpAddedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getCacheEntry(final String key,
      final Continuation<? super ApiResponseCacheEntity> $completion) {
    final String _sql = "SELECT * FROM api_responses WHERE `key` = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, key);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<ApiResponseCacheEntity>() {
      @Override
      @Nullable
      public ApiResponseCacheEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfKey = CursorUtil.getColumnIndexOrThrow(_cursor, "key");
          final int _cursorIndexOfDataJson = CursorUtil.getColumnIndexOrThrow(_cursor, "dataJson");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final ApiResponseCacheEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpKey;
            _tmpKey = _cursor.getString(_cursorIndexOfKey);
            final String _tmpDataJson;
            _tmpDataJson = _cursor.getString(_cursorIndexOfDataJson);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _result = new ApiResponseCacheEntity(_tmpKey,_tmpDataJson,_tmpUpdatedAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<ReadingSessionEntity>> getAllReadingSessions() {
    final String _sql = "SELECT * FROM reading_sessions ORDER BY timestamp ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"reading_sessions"}, new Callable<List<ReadingSessionEntity>>() {
      @Override
      @NonNull
      public List<ReadingSessionEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfDuration = CursorUtil.getColumnIndexOrThrow(_cursor, "duration");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfChapterId = CursorUtil.getColumnIndexOrThrow(_cursor, "chapterId");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final List<ReadingSessionEntity> _result = new ArrayList<ReadingSessionEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ReadingSessionEntity _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpDate;
            _tmpDate = _cursor.getString(_cursorIndexOfDate);
            final long _tmpDuration;
            _tmpDuration = _cursor.getLong(_cursorIndexOfDuration);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final Integer _tmpChapterId;
            if (_cursor.isNull(_cursorIndexOfChapterId)) {
              _tmpChapterId = null;
            } else {
              _tmpChapterId = _cursor.getInt(_cursorIndexOfChapterId);
            }
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            _item = new ReadingSessionEntity(_tmpId,_tmpDate,_tmpDuration,_tmpType,_tmpChapterId,_tmpTimestamp);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<RecentlyReadEntity>> getRecentlyRead(final int limit) {
    final String _sql = "SELECT * FROM recently_read ORDER BY timestamp DESC LIMIT ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, limit);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"recently_read"}, new Callable<List<RecentlyReadEntity>>() {
      @Override
      @NonNull
      public List<RecentlyReadEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfChapterId = CursorUtil.getColumnIndexOrThrow(_cursor, "chapterId");
          final int _cursorIndexOfChapterName = CursorUtil.getColumnIndexOrThrow(_cursor, "chapterName");
          final int _cursorIndexOfVerseKey = CursorUtil.getColumnIndexOrThrow(_cursor, "verseKey");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final List<RecentlyReadEntity> _result = new ArrayList<RecentlyReadEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final RecentlyReadEntity _item;
            final int _tmpChapterId;
            _tmpChapterId = _cursor.getInt(_cursorIndexOfChapterId);
            final String _tmpChapterName;
            _tmpChapterName = _cursor.getString(_cursorIndexOfChapterName);
            final String _tmpVerseKey;
            if (_cursor.isNull(_cursorIndexOfVerseKey)) {
              _tmpVerseKey = null;
            } else {
              _tmpVerseKey = _cursor.getString(_cursorIndexOfVerseKey);
            }
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            _item = new RecentlyReadEntity(_tmpChapterId,_tmpChapterName,_tmpVerseKey,_tmpTimestamp);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getRecentlyReadForChapter(final int chapterId,
      final Continuation<? super RecentlyReadEntity> $completion) {
    final String _sql = "SELECT * FROM recently_read WHERE chapterId = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, chapterId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<RecentlyReadEntity>() {
      @Override
      @Nullable
      public RecentlyReadEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfChapterId = CursorUtil.getColumnIndexOrThrow(_cursor, "chapterId");
          final int _cursorIndexOfChapterName = CursorUtil.getColumnIndexOrThrow(_cursor, "chapterName");
          final int _cursorIndexOfVerseKey = CursorUtil.getColumnIndexOrThrow(_cursor, "verseKey");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final RecentlyReadEntity _result;
          if (_cursor.moveToFirst()) {
            final int _tmpChapterId;
            _tmpChapterId = _cursor.getInt(_cursorIndexOfChapterId);
            final String _tmpChapterName;
            _tmpChapterName = _cursor.getString(_cursorIndexOfChapterName);
            final String _tmpVerseKey;
            if (_cursor.isNull(_cursorIndexOfVerseKey)) {
              _tmpVerseKey = null;
            } else {
              _tmpVerseKey = _cursor.getString(_cursorIndexOfVerseKey);
            }
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            _result = new RecentlyReadEntity(_tmpChapterId,_tmpChapterName,_tmpVerseKey,_tmpTimestamp);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<BookmarkEntity> getLatestBookmark() {
    final String _sql = "SELECT * FROM bookmarks ORDER BY timestamp DESC LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"bookmarks"}, new Callable<BookmarkEntity>() {
      @Override
      @Nullable
      public BookmarkEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfVerseKey = CursorUtil.getColumnIndexOrThrow(_cursor, "verseKey");
          final int _cursorIndexOfChapterId = CursorUtil.getColumnIndexOrThrow(_cursor, "chapterId");
          final int _cursorIndexOfSurahName = CursorUtil.getColumnIndexOrThrow(_cursor, "surahName");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final BookmarkEntity _result;
          if (_cursor.moveToFirst()) {
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpVerseKey;
            _tmpVerseKey = _cursor.getString(_cursorIndexOfVerseKey);
            final int _tmpChapterId;
            _tmpChapterId = _cursor.getInt(_cursorIndexOfChapterId);
            final String _tmpSurahName;
            _tmpSurahName = _cursor.getString(_cursorIndexOfSurahName);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            _result = new BookmarkEntity(_tmpId,_tmpVerseKey,_tmpChapterId,_tmpSurahName,_tmpTimestamp);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
