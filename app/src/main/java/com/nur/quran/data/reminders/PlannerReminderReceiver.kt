package com.nur.quran.data.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.*

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ReminderEntryPoint { fun reminders(): PlannerReminderCoordinator }

internal fun Context.plannerReminders(): PlannerReminderCoordinator =
    EntryPointAccessors.fromApplication(applicationContext, ReminderEntryPoint::class.java).reminders()

class PlannerReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                withTimeout(8_000) {
                    val reminders = context.plannerReminders()
                    if (intent.action == PlannerReminderCoordinator.ACTION_SNOOZE)
                        intent.getStringExtra("reminder_key")?.let { reminders.snooze(it) }
                    else reminders.rebuild(force = true, deliver = true)
                }
            } finally { pending.finish() }
        }
    }
}

class PlannerReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = try {
        applicationContext.plannerReminders().rebuild(force = true, deliver = true)
        Result.success()
    } catch (_: Exception) { Result.retry() }
}
