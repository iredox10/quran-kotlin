package com.nur.quran.data.audio

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import com.nur.quran.data.db.dao.QuranDao
import com.nur.quran.data.db.entities.LinkedTimingEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Imports gapless-pack `timings.db` files (copied by the user next to the
 * 001..114.mp3 files) into the Room `linked_timings` table.
 *
 * Source schema: SQLite table `timings(sura INT, ayah INT, timing INT ms)`;
 * ayah 0 = basmalah start where present, 999 = end marker.
 * Player seeks via [LinkedTimingEntity.startMs].
 */
@Singleton
class TimingImporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val quranDao: QuranDao
) {
    /**
     * Copies the `timings.db` at [timingDbUri] to cache, reads every
     * `(sura, ayah, timing)` row and replaces/inserts them for [reciterId].
     *
     * @return number of rows inserted, or 0 on any failure (never throws).
     */
    suspend fun importTimingDb(reciterId: Int, timingDbUri: Uri): Int =
        withContext(Dispatchers.IO) {
            var db: SQLiteDatabase? = null
            val tmp = File(context.cacheDir, "timing-$reciterId.db")
            try {
                context.contentResolver.openFileDescriptor(timingDbUri, "r")?.use { pfd ->
                    java.io.FileInputStream(pfd.fileDescriptor).use { input ->
                        FileOutputStream(tmp).use { output ->
                            val buffer = ByteArray(8192)
                            var read: Int
                            while (input.read(buffer).also { read = it } != -1) {
                                output.write(buffer, 0, read)
                            }
                            output.fd.sync()
                        }
                    }
                } ?: return@withContext 0
                if (!tmp.exists() || tmp.length() == 0L) return@withContext 0

                db = SQLiteDatabase.openDatabase(
                    tmp.absolutePath,
                    null,
                    SQLiteDatabase.OPEN_READONLY
                )
                val rows = mutableListOf<LinkedTimingEntity>()
                val nonNullDb = db ?: return@withContext 0
                val cursor = try {
                    nonNullDb.query(
                        "timings",
                        arrayOf("sura", "ayah", "timing"),
                        null, null, null, null,
                        "sura, ayah"
                    )
                } catch (e: Exception) {
                    // Fallback: singular table name "timing".
                    nonNullDb.query(
                        "timing",
                        arrayOf("sura", "ayah", "timing"),
                        null, null, null, null,
                        "sura, ayah"
                    )
                }
                try {
                    val suraIdx = cursor.getColumnIndex("sura")
                    val ayahIdx = cursor.getColumnIndex("ayah")
                    val timingIdx = cursor.getColumnIndex("timing")
                    if (suraIdx < 0 || ayahIdx < 0 || timingIdx < 0) return@withContext 0
                    while (cursor.moveToNext()) {
                        val sura = cursor.getInt(suraIdx)
                        val ayah = cursor.getInt(ayahIdx)
                        val timing = cursor.getLong(timingIdx)
                        if (sura !in 1..114) continue
                        if (!(ayah in 0..287 || ayah == 999)) continue
                        rows.add(LinkedTimingEntity(reciterId, sura, ayah, timing))
                    }
                } finally {
                    cursor.close()
                }

                rows.chunked(500).forEach { chunk ->
                    quranDao.insertTimings(chunk)
                }
                rows.size
            } catch (e: Exception) {
                0
            } finally {
                try {
                    db?.close()
                } catch (e: Exception) {
                    // Ignore close failures.
                }
                tmp.delete()
            }
        }

    /** All imported timings for a reciter + sura, ordered by ayah. */
    suspend fun getTimings(reciterId: Int, sura: Int): List<LinkedTimingEntity> =
        withContext(Dispatchers.IO) {
            try {
                quranDao.getTimings(reciterId, sura)
            } catch (e: Exception) {
                emptyList()
            }
        }

    /** True when more than one timing row exists for the reciter + sura. */
    suspend fun hasTimings(reciterId: Int, sura: Int): Boolean =
        withContext(Dispatchers.IO) {
            try {
                quranDao.timingCount(reciterId, sura) > 1
            } catch (e: Exception) {
                false
            }
        }

    /** Start position in ms for a single ayah, or null when absent. */
    suspend fun getStartMs(reciterId: Int, sura: Int, ayah: Int): Long? =
        getTimings(reciterId, sura).find { it.ayah == ayah }?.startMs
}
