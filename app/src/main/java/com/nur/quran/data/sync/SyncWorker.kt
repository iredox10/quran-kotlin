package com.nur.quran.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Periodic auto-sync worker (no hilt-work — plain [CoroutineWorker]).
 *
 * Resolves [SyncService] via a Hilt [EntryPoint] instead of manual
 * construction: the SyncService dependency graph (Heroku client, snapshot
 * builder, Room DAO, …) is owned by the DI graph and the build fixer aligns
 * it, so the worker must not duplicate that construction. EntryPoint access
 * needs only the application context and compiles against the assumed API
 * `data.sync.SyncService @Singleton with suspend fun sync(): String`.
 */
class SyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SyncWorkerEntryPoint {
        fun syncService(): SyncService
    }

    override suspend fun doWork(): Result {
        android.util.Log.d("NurSync", "SyncWorker doWork start (attempt=$runAttemptCount)")
        setProgress(workDataOf(KEY_PROGRESS to PROGRESS_RUNNING))
        return try {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                SyncWorkerEntryPoint::class.java
            )
            val message: String = entryPoint.syncService().sync()
            android.util.Log.d("NurSync", "SyncWorker result: $message")
            when {
                // Signed out (SyncService.currentUserId() is null): no-op, no retry.
                message == RESULT_SIGNED_OUT ->
                    Result.success(workDataOf(KEY_RESULT to RESULT_SIGNED_OUT))

                // sync() never throws — transport failures come back as "error:<msg>"
                // (currentUserId() also returns null on transport failure), so map
                // them onto the retry ladder instead of reporting success.
                message.startsWith(RESULT_ERROR_PREFIX) -> retryOrFail(message)

                else -> {
                    setProgress(workDataOf(KEY_PROGRESS to PROGRESS_DONE))
                    Result.success(workDataOf(KEY_RESULT to message))
                }
            }
        } catch (e: IOException) {
            setProgress(workDataOf(KEY_PROGRESS to PROGRESS_RETRY))
            Result.retry()
        } catch (e: Exception) {
            retryOrFail(e.message ?: "Sync failed")
        }
    }

    /**
     * Retries with the request backoff until [MAX_ATTEMPTS] (web ladder is
     * 0/5/20s = 3 attempts), then fails so the next enqueue can start clean.
     */
    private suspend fun retryOrFail(reason: String): Result {
        return if (runAttemptCount >= MAX_ATTEMPTS) {
            Result.failure(workDataOf(KEY_RESULT to reason))
        } else {
            setProgress(workDataOf(KEY_PROGRESS to PROGRESS_RETRY))
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME_PERIODIC = "sync-periodic"
        const val WORK_NAME_ONCE = "sync-once"
        const val KEY_RESULT = "result"
        const val KEY_PROGRESS = "progress"

        const val PROGRESS_RUNNING = "running"
        const val PROGRESS_DONE = "done"
        const val PROGRESS_RETRY = "retry"

        /** Contract of SyncService.sync() (signed out — no user, nothing to do). */
        private const val RESULT_SIGNED_OUT = "signed-out"

        /** Contract of SyncService.sync() (error:<message> — transport/merge failure). */
        private const val RESULT_ERROR_PREFIX = "error:"

        private const val MAX_ATTEMPTS = 3

        private val connectedConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        /**
         * Enqueues 6-hourly auto-sync (CONNECTED). KEEPs existing periodic
         * work so repeated calls are idempotent.
         */
        fun enqueuePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<SyncWorker>(6, TimeUnit.HOURS)
                .setConstraints(connectedConstraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .addTag(WORK_NAME_PERIODIC)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME_PERIODIC,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        /** Enqueues a one-shot sync (CONNECTED). REPLACEs any pending once-off. */
        fun enqueueOnce(context: Context) {
            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(connectedConstraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .addTag(WORK_NAME_ONCE)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME_ONCE,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        /**
         * Auto-sync entry point for app start (mirrors web cold-start pull +
         * debounced push): schedules the 6-hourly periodic sync and kicks one
         * immediate pass, both gated on CONNECTED. Idempotent.
         */
        fun enqueueAutoSync(context: Context) {
            enqueuePeriodic(context)
            enqueueOnce(context)
        }
    }
}
