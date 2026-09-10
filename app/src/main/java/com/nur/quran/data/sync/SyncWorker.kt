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
 * construction: the SyncService dependency graph (Appwrite client, snapshot
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
        setProgress(workDataOf(KEY_PROGRESS to PROGRESS_RUNNING))
        return try {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                SyncWorkerEntryPoint::class.java
            )
            val message: String = entryPoint.syncService().sync()
            setProgress(workDataOf(KEY_PROGRESS to PROGRESS_DONE))
            Result.success(workDataOf(KEY_RESULT to message))
        } catch (e: IOException) {
            setProgress(workDataOf(KEY_PROGRESS to PROGRESS_RETRY))
            Result.retry()
        } catch (e: Exception) {
            if (runAttemptCount >= MAX_ATTEMPTS) {
                Result.failure(workDataOf(KEY_RESULT to (e.message ?: "Sync failed")))
            } else {
                Result.retry()
            }
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
    }
}
