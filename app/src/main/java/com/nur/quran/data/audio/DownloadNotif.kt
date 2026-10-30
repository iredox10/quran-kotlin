package com.nur.quran.data.audio

import com.nur.quran.R

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object DownloadNotif {
    const val CHANNEL_ID = "audio_downloads"


    fun ensureChannel(context: Context) {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                        ?: return@runCatching
                run {
                    val channel = NotificationChannel(
                        CHANNEL_ID,
                        com.nur.quran.i18n.AppLanguages.wrap(context).getString(R.string.ui_audio_downloads_716e48),
                        NotificationManager.IMPORTANCE_LOW
                    ).apply {
                        description = com.nur.quran.i18n.AppLanguages.wrap(context).getString(R.string.ui_shows_audio_download_progress_3b6c55)
                    }
                    manager.createNotificationChannel(channel)
                }
            }
        }
    }

    fun showProgress(
        context: Context,
        notifId: Int,
        title: String,
        done: Int,
        total: Int
    ): Notification {
        runCatching { ensureChannel(context) }
        return runCatching {
            newBuilder(context)
                .setContentTitle(title)
                .setContentText(com.nur.quran.i18n.AppLanguages.wrap(context).getString(R.string.ui_1_s_2_s_ayahs_16b09d, done, total))
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setProgress(total, done, false)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .build()
        }.getOrElse {
            runCatching {
                newBuilder(context)
                    .setContentTitle(title)
                    .setSmallIcon(android.R.drawable.stat_sys_download)
                    .setOnlyAlertOnce(true)
                    .build()
            }.getOrElse { Notification() }
        }
    }

    fun showComplete(context: Context, notifId: Int, title: String) {
        runCatching {
            ensureChannel(context)
            val notification = newBuilder(context)
                .setContentTitle(title)
                .setContentText(com.nur.quran.i18n.AppLanguages.wrap(context).getString(R.string.ui_download_complete_95efb3))
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .build()
            manager(context)?.notify(notifId, notification)
        }
    }

    fun showFailed(context: Context, notifId: Int, title: String) {
        runCatching {
            ensureChannel(context)
            val notification = newBuilder(context)
                .setContentTitle(title)
                .setContentText(com.nur.quran.i18n.AppLanguages.wrap(context).getString(R.string.ui_download_failed_01125e))
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .build()
            manager(context)?.notify(notifId, notification)
        }
    }

    fun cancel(context: Context, notifId: Int) {
        runCatching {
            manager(context)?.cancel(notifId)
        }
    }

    private fun newBuilder(context: Context): Notification.Builder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(context)
        }
    }

    private fun manager(context: Context): NotificationManager? {
        return runCatching {
            context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        }.getOrNull()
    }
}
