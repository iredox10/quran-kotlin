package com.nur.quran.data.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object DownloadNotif {
    const val CHANNEL_ID = "audio_downloads"

    private const val CHANNEL_NAME = "Audio downloads"
    private const val CHANNEL_DESCRIPTION = "Shows audio download progress"

    fun ensureChannel(context: Context) {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                        ?: return@runCatching
                if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                    val channel = NotificationChannel(
                        CHANNEL_ID,
                        CHANNEL_NAME,
                        NotificationManager.IMPORTANCE_LOW
                    ).apply {
                        description = CHANNEL_DESCRIPTION
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
                .setContentText("$done/$total ayahs")
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
                .setContentText("Download complete")
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
                .setContentText("Download failed")
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
