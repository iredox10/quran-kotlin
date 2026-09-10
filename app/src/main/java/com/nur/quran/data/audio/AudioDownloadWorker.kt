package com.nur.quran.data.audio

import android.app.Notification
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException

/**
 * Background per-chapter audio downloader (no Hilt — plain [CoroutineWorker]
 * with manually constructed [AudioDownloadManager]).
 *
 * One work per chapter: input is the chapter's verse keys (`"S:A"`) as a
 * StringArray (worst case ~286 keys ≈ 2KB, well under the 10KB InputData
 * limit). Network constraints are supplied by the enqueuer ([enqueue] /
 * [enqueueMushaf]); none are hardcoded here.
 *
 * Progress is reported two ways: the foreground notification via
 * [DownloadNotif] and WorkManager progress ([KEY_DONE]/[KEY_TOTAL]).
 */
class AudioDownloadWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val reciterId = inputData.getInt(KEY_RECITER_ID, Reciters.DEFAULT_ID)
        val chapterId = inputData.getInt(KEY_CHAPTER_ID, -1)
        if (chapterId == -1) return Result.failure()
        val verseKeys = inputData.getStringArray(KEY_VERSE_KEYS)?.toList().orEmpty()
        if (verseKeys.isEmpty()) return Result.success()

        val title = "Surah $chapterId • ${Reciters.nameOf(reciterId)}"
        val notifId = notifId(reciterId, chapterId)
        val manager = AudioDownloadManager(applicationContext)

        return try {
            val initial: Notification =
                DownloadNotif.showProgress(applicationContext, notifId, title, 0, verseKeys.size)
            setForeground(toForegroundInfo(notifId, initial))

            val downloaded = manager.downloadMissingKeys(
                reciterId,
                verseKeys,
                chapterId
            ) { done, total ->
                val notification: Notification =
                    DownloadNotif.showProgress(applicationContext, notifId, title, done, total)
                // onProgress is not suspend: use the async variants.
                setForegroundAsync(toForegroundInfo(notifId, notification))
                setProgressAsync(workDataOf(KEY_DONE to done, KEY_TOTAL to total))
            }

            setProgress(workDataOf(KEY_DONE to downloaded, KEY_TOTAL to verseKeys.size))
            val allPresent = verseKeys.all { manager.isVerseDownloaded(reciterId, it) }
            if (allPresent) {
                DownloadNotif.showComplete(applicationContext, notifId, title)
                Result.success(workDataOf(KEY_DONE to verseKeys.size, KEY_TOTAL to verseKeys.size))
            } else {
                DownloadNotif.showFailed(applicationContext, notifId, title)
                Result.failure(workDataOf(KEY_DONE to downloaded, KEY_TOTAL to verseKeys.size))
            }
        } catch (e: CancellationException) {
            DownloadNotif.cancel(applicationContext, notifId)
            throw e
        } catch (e: IOException) {
            DownloadNotif.showFailed(applicationContext, notifId, title)
            Result.retry()
        } catch (e: Exception) {
            DownloadNotif.showFailed(applicationContext, notifId, title)
            Result.failure()
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val reciterId = inputData.getInt(KEY_RECITER_ID, Reciters.DEFAULT_ID)
        val chapterId = inputData.getInt(KEY_CHAPTER_ID, -1)
        val total = inputData.getStringArray(KEY_VERSE_KEYS)?.size ?: 0
        val title = "Surah $chapterId • ${Reciters.nameOf(reciterId)}"
        val notifId = notifId(reciterId, chapterId)
        val notification: Notification =
            DownloadNotif.showProgress(applicationContext, notifId, title, 0, total)
        return toForegroundInfo(notifId, notification)
    }

    private fun toForegroundInfo(notifId: Int, notification: Notification): ForegroundInfo {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                notifId,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(notifId, notification)
        }
    }

    companion object {
        const val KEY_RECITER_ID = "reciterId"
        const val KEY_CHAPTER_ID = "chapterId"
        const val KEY_VERSE_KEYS = "verseKeys"
        const val KEY_DONE = "done"
        const val KEY_TOTAL = "total"

        /** Unique-work name for one chapter. */
        fun workName(reciterId: Int, chapterId: Int): String = "audio-$reciterId-$chapterId"

        /** Unique-work name for a full-mushaf chain. */
        fun mushafWorkName(reciterId: Int): String = "audio-$reciterId-mushaf"

        /** Stable foreground notification id for a chapter. */
        fun notifId(reciterId: Int, chapterId: Int): Int = reciterId * 1000 + chapterId

        /**
         * Enqueues a single-chapter download. Constraints come from [wifiOnly]
         * (unmetered) or plain connected; backoff is exponential, 10s; an
         * existing work for the same chapter is kept.
         */
        fun enqueue(
            context: Context,
            reciterId: Int,
            chapterId: Int,
            verseKeys: List<String>,
            wifiOnly: Boolean
        ) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(
                    if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED
                )
                .build()
            val request = OneTimeWorkRequestBuilder<AudioDownloadWorker>()
                .setInputData(
                    workDataOf(
                        KEY_RECITER_ID to reciterId,
                        KEY_CHAPTER_ID to chapterId,
                        KEY_VERSE_KEYS to verseKeys.toTypedArray()
                    )
                )
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
                .addTag(workName(reciterId, chapterId))
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                workName(reciterId, chapterId),
                ExistingWorkPolicy.KEEP,
                request
            )
        }

        /** Cancels the chapter work (foreground notification cleanup is in the worker). */
        fun cancel(context: Context, reciterId: Int, chapterId: Int) {
            WorkManager.getInstance(context).cancelUniqueWork(workName(reciterId, chapterId))
        }

        /**
         * Enqueues a full-mushaf download as a sequential continuation chain
         * (chapter order), one [AudioDownloadWorker] per entry of
         * [allVerseKeys]. KEEPs the existing chain if already enqueued.
         */
        fun enqueueMushaf(
            context: Context,
            reciterId: Int,
            allVerseKeys: Map<Int, List<String>>
        ) {
            val sorted = allVerseKeys.toSortedMap()
            if (sorted.isEmpty()) return
            val wm = WorkManager.getInstance(context)
            val requests = sorted.map { (chapterId, keys) ->
                OneTimeWorkRequestBuilder<AudioDownloadWorker>()
                    .setInputData(
                        workDataOf(
                            KEY_RECITER_ID to reciterId,
                            KEY_CHAPTER_ID to chapterId,
                            KEY_VERSE_KEYS to keys.toTypedArray()
                        )
                    )
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build()
                    )
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
                    .addTag(workName(reciterId, chapterId))
                    .build()
            }
            var continuation =
                wm.beginUniqueWork(mushafWorkName(reciterId), ExistingWorkPolicy.KEEP, requests.first())
            requests.drop(1).forEach { continuation = continuation.then(it) }
            continuation.enqueue()
        }
    }
}
