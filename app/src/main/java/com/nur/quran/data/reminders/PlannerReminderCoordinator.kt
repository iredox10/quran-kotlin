package com.nur.quran.data.reminders

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nur.quran.MainActivity
import com.nur.quran.R
import com.nur.quran.data.db.dao.QuranDao
import com.nur.quran.data.planner.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** A single local scheduler; no network, repository bootstrap or foreground service. */
@Singleton
class PlannerReminderCoordinator @Inject constructor(
    @ApplicationContext private val context: Context, private val dao: QuranDao, private val gson: Gson
) {
    private val prefs = context.getSharedPreferences("PlannerReminders", Context.MODE_PRIVATE)
    private val delivery = context.getSharedPreferences("PlannerReminderDelivery", Context.MODE_PRIVATE)
    private val plannerPrefs = context.getSharedPreferences("PlannerSettings", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val busy = java.util.concurrent.ConcurrentHashMap<String, Pair<String, Int?>>()
    private var started = false
    private val _settings = MutableStateFlow(readSettings())
    val settings = _settings.asStateFlow()
    private val _next = MutableStateFlow<ReminderEvent?>(null)
    val next = _next.asStateFlow()
    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        _settings.value = readSettings(); refresh()
    }

    fun start() {
        if (started) return
        started = true
        createChannels()
        prefs.registerOnSharedPreferenceChangeListener(listener)
        plannerPrefs.registerOnSharedPreferenceChangeListener(listener)
        scope.launch {
            dao.observePlannerCache().map { rows -> rows.filter { it.key in setOf("planner_all_plans", "planner_active_id", "planner_archived_plans") || it.key.startsWith("planner_sessions_") }.associate { it.key to it.dataJson } }
                .distinctUntilChanged().collect { delay(100); runCatching { rebuild() } }
        }
        refresh(force = true)
    }

    private fun readSettings(): ReminderSettings = runCatching {
        gson.fromJson(prefs.getString("settings", null), ReminderSettings::class.java)
    }.getOrNull() ?: ReminderSettings()

    suspend fun save(settings: ReminderSettings) = withContext(Dispatchers.IO) {
        require(!settings.enabled || settings.valid(activePrayers(), ZoneId.systemDefault()))
        check(prefs.edit().putString("settings", gson.toJson(settings)).commit())
        _settings.value = settings
        rebuild(force = true)
    }

    fun activePrayers(): List<String> {
        val selected = plannerPrefs.getStringSet("active_prayers", PRAYER_NAMES.toSet()).orEmpty()
        return PRAYER_NAMES.filter { it in selected }
    }

    fun notificationsAllowed(): Boolean = NotificationManagerCompat.from(context).areNotificationsEnabled() &&
        (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)

    fun exactAllowed(): Boolean = Build.VERSION.SDK_INT < 31 || context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    fun setReading(owner: String, planId: String, day: Int?, active: Boolean) {
        if (active) busy[owner] = planId to day else busy.remove(owner)
        refresh()
    }
    private fun suppressed(p: ReminderPortion) = busy.values.any { it.first == p.planId && (it.second == null || it.second == p.day) }
    fun refresh(force: Boolean = false) { scope.launch { runCatching { rebuild(force = force) } } }

    private suspend fun portions(): List<ReminderPortion> {
        val rows = dao.getPlannerCacheSnapshot().associate { it.key to it.dataJson }
        val all = rows["planner_all_plans"]?.let { PlannerEngine.parseReadingPlans(it, gson) }.orEmpty()
        val id = rows["planner_active_id"]?.takeIf { candidate -> all.any { it.id == candidate } } ?: all.firstOrNull()?.id
        val plan = all.firstOrNull { it.id == id }
        val archived = rows["planner_archived_plans"]?.let { PlannerEngine.parseReadingPlans(it, gson) }.orEmpty()
        val timers = rows.filterKeys { it.startsWith("planner_sessions_") }.flatMap { (_, json) ->
            runCatching { PlannerWire.sessionTimersFromJson(json).entries.map { it.key to it.value } }.getOrDefault(emptyList())
        }.toMap()
        return PlannerReminderEngine.portions(plan, readSettings(), activePrayers(),
            plannerPrefs.getString("reading_preference", "after") ?: "after", System.currentTimeMillis(), ZoneId.systemDefault(),
            PlannerOutsideReadings.pace((all + archived).distinctBy { it.id }, timers))
    }

    private fun receipts(): Map<String, ReminderReceipt> = runCatching {
        gson.fromJson<Map<String, ReminderReceipt>>(delivery.getString("receipts", "{}"), object : TypeToken<Map<String, ReminderReceipt>>() {}.type)
    }.getOrNull().orEmpty()

    private fun saveReceipts(receipts: Map<String, ReminderReceipt>) {
        check(delivery.edit().putString("receipts", gson.toJson(receipts.entries.sortedByDescending { it.value.sentAt }.take(200).associate { it.toPair() })).commit())
    }

    suspend fun snooze(key: String) = mutex.withLock {
        val portion = portions().firstOrNull { it.key == key } ?: return@withLock
        val receipts = receipts()
        saveReceipts(receipts + (portion.key to (receipts[portion.key] ?: ReminderReceipt()).copy(snoozeUntil = System.currentTimeMillis() + 600_000)))
        cancelVisible(key)
        schedule(portions(), force = true)
    }

    suspend fun target(key: String): ReminderPortion? = withContext(Dispatchers.IO) {
        portions().firstOrNull { it.key == key }
    }

    fun cancelVisible(key: String? = null) {
        if (key == null || delivery.getString("visible", null) == key) {
            context.getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
            delivery.edit().remove("visible").apply()
        }
    }

    suspend fun rebuild(force: Boolean = false, deliver: Boolean = false) = mutex.withLock {
        val settings = readSettings()
        val current = portions()
        val visible = delivery.getString("visible", null)
        if (visible != null && current.none { it.key == visible && !suppressed(it) }) cancelVisible()
        if (deliver && settings.enabled && notificationsAllowed()) {
            val now = System.currentTimeMillis()
            val receipts = receipts()
            val event = PlannerReminderEngine.next(current, settings, receipts, now, delivery.getLong("last_sent", 0), ::suppressed)
            if (event != null && event.at <= now) {
                // Claim before posting: concurrent alarm/backup delivery cannot ring twice.
                saveReceipts(receipts + (event.portion.key to ReminderReceipt(event.phase, now)))
                delivery.edit().putLong("last_sent", now).putString("visible", event.portion.key).commit()
                post(event, settings)
            }
        }
        schedule(current, force)
    }

    private fun schedule(current: List<ReminderPortion>, force: Boolean) {
        val settings = readSettings()
        val alarm = context.getSystemService(AlarmManager::class.java)
        if (!settings.enabled || !notificationsAllowed() || !settings.valid(activePrayers(), ZoneId.systemDefault())) {
            alarm.cancel(alarmIntent()); _next.value = null; cancelVisible()
            delivery.edit().remove("token").remove("fire_at").apply()
            WorkManager.getInstance(context).cancelUniqueWork("planner-reminder-backup")
            WorkManager.getInstance(context).cancelUniqueWork("planner-reminder-repair")
            return
        }
        val now = System.currentTimeMillis()
        val next = PlannerReminderEngine.next(current, settings, receipts(), now, delivery.getLong("last_sent", 0), ::suppressed)
        _next.value = next
        val midnight = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() + 1000
        val at = minOf(next?.at ?: midnight, midnight).coerceAtLeast(now + 1000)
        val token = if (next != null && next.at < midnight) next.token else "midnight:$midnight"
        if (force || token != delivery.getString("token", null) || delivery.getLong("fire_at", 0) < now - 60_000) {
            try {
                if (exactAllowed()) alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, alarmIntent())
                else alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, alarmIntent())
            } catch (_: SecurityException) { alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, alarmIntent()) }
            delivery.edit().putString("token", token).putLong("fire_at", at).apply()
            WorkManager.getInstance(context).enqueueUniqueWork("planner-reminder-backup", ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<PlannerReminderWorker>().setInitialDelay((at - now + 120_000).coerceAtLeast(0), TimeUnit.MILLISECONDS).build())
        }
        WorkManager.getInstance(context).enqueueUniquePeriodicWork("planner-reminder-repair", ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<PlannerReminderWorker>(12, TimeUnit.HOURS).build())
    }

    private fun alarmIntent() = PendingIntent.getBroadcast(context, 6100,
        Intent(context, PlannerReminderReceiver::class.java).setAction(ACTION_ALARM), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    private fun activityIntent(portion: ReminderPortion, outside: Boolean) = PendingIntent.getActivity(context, if (outside) 6102 else 6101,
        Intent(context, MainActivity::class.java).setAction(ACTION_OPEN).setData(Uri.parse("quran-reminder://${if (outside) "outside" else "read"}/${Uri.encode(portion.key)}"))
            .putExtra("reminder_key", portion.key).putExtra("outside", outside), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    private fun post(event: ReminderEvent, settings: ReminderSettings) {
        val strings = com.nur.quran.i18n.AppLanguages.wrap(context)
        val p = event.portion
        val quiet = settings.intensity == "gentle" || event.phase == 0
        val channel = if (quiet) QUIET else if (settings.intensity == "strong") STRONG else FOCUSED
        val prayer = strings.getString(when (p.prayer) {
            "Fajr" -> R.string.planner_fajr; "Dhuhr" -> R.string.planner_dhuhr; "Asr" -> R.string.planner_asr
            "Maghrib" -> R.string.planner_maghrib; else -> R.string.planner_isha
        })
        val title = strings.getString(if (event.phase == 0) R.string.reminder_prepare else R.string.reminder_due, prayer)
        val text = strings.getString(R.string.reminder_remaining, p.pages.size) +
            (p.seconds?.let { " · " + strings.getString(R.string.reminder_estimate, ((it + 59) / 60).toInt()) } ?: "")
        val snooze = PendingIntent.getBroadcast(context, 6103, Intent(context, PlannerReminderReceiver::class.java)
            .setAction(ACTION_SNOOZE).putExtra("reminder_key", p.key), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, channel).setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title).setContentText(text).setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(activityIntent(p, false)).setAutoCancel(true)
            .setPriority(if (quiet) NotificationCompat.PRIORITY_LOW else NotificationCompat.PRIORITY_HIGH)
            .setSound(if (quiet) null else RingtoneManager.getDefaultUri(if (settings.intensity == "strong") RingtoneManager.TYPE_ALARM else RingtoneManager.TYPE_NOTIFICATION))
            .setVibrate(if (quiet) longArrayOf(0) else longArrayOf(0, 250, 150, 250))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .addAction(0, strings.getString(R.string.reminder_read), activityIntent(p, false))
            .addAction(0, strings.getString(R.string.reminder_snooze), snooze)
            .addAction(0, strings.getString(R.string.planner_outside_log), activityIntent(p, true)).build()
        if (notificationsAllowed()) context.getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification)
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT < 26) return
        val strings = com.nur.quran.i18n.AppLanguages.wrap(context)
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(QUIET, strings.getString(R.string.reminder_gentle), NotificationManager.IMPORTANCE_LOW).apply { setSound(null, null) })
        listOf(FOCUSED to R.string.reminder_focused, STRONG to R.string.reminder_strong).forEach { (id, name) ->
            manager.createNotificationChannel(NotificationChannel(id, strings.getString(name), NotificationManager.IMPORTANCE_HIGH).apply {
                enableVibration(true)
                setSound(RingtoneManager.getDefaultUri(if (id == STRONG) RingtoneManager.TYPE_ALARM else RingtoneManager.TYPE_NOTIFICATION),
                    AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT).build())
                description = strings.getString(R.string.reminder_channel_description)
            })
        }
    }

    companion object {
        const val ACTION_ALARM = "com.nur.quran.REMINDER_ALARM"
        const val ACTION_SNOOZE = "com.nur.quran.REMINDER_SNOOZE"
        const val ACTION_OPEN = "com.nur.quran.REMINDER_OPEN"
        const val NOTIFICATION_ID = 6100
        const val QUIET = "planner_reminder_quiet"
        const val FOCUSED = "planner_reminder_focused"
        const val STRONG = "planner_reminder_strong"
    }
}
