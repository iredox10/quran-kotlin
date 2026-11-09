package com.nur.quran

import android.app.Application
import com.nur.quran.data.sync.SyncWorker
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class QuranApplication : Application() {

    @javax.inject.Inject lateinit var reminders: com.nur.quran.data.reminders.PlannerReminderCoordinator

    override fun onCreate() {
        super.onCreate()
        SyncWorker.enqueueAutoSync(this)
        reminders.start()
    }
}
