package com.nur.quran

import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.Gson
import com.nur.quran.data.db.dao.QuranDao
import com.nur.quran.data.db.entities.ApiResponseCacheEntity
import com.nur.quran.data.planner.*
import com.nur.quran.data.reminders.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalTime

@RunWith(AndroidJUnit4::class)
class PlannerReminderDeliveryTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun restore(prefs: SharedPreferences, values: Map<String, *>) {
        prefs.edit().clear().apply {
            values.forEach { (key, value) -> when (value) {
                is String -> putString(key, value); is Boolean -> putBoolean(key, value)
                is Int -> putInt(key, value); is Long -> putLong(key, value)
                is Float -> putFloat(key, value); is Set<*> -> putStringSet(key, value.filterIsInstance<String>().toSet())
            } }
        }.commit()
    }
    private fun fixture(test: suspend (PlannerReminderCoordinator, QuranDao, ReadingPlan) -> Unit) = runBlocking {
        val reminders = context.plannerReminders()
        val dao = reminders.javaClass.getDeclaredField("dao").apply { isAccessible = true }.get(reminders) as QuranDao
        val rows = dao.getPlannerCacheSnapshot().filter { it.key in setOf("planner_all_plans", "planner_active_id") }
        val prefs = listOf("PlannerReminders", "PlannerReminderDelivery", "PlannerSettings").map { context.getSharedPreferences(it, Context.MODE_PRIVATE) }
        val saved = prefs.map { it.all.toMap() }
        val plan = PlannerEngine.buildReadingPlanner("page", 1, PlannerEngine.formatPlannerDate(), 1, 10, chapters = emptyList()).copy(lastReadPage = 1)
        try {
            prefs[1].edit().clear().commit()
            prefs[2].edit().putStringSet("active_prayers", setOf("Fajr")).commit()
            dao.insertCacheEntry(ApiResponseCacheEntity("planner_all_plans", Gson().toJson(listOf(plan))))
            dao.insertCacheEntry(ApiResponseCacheEntity("planner_active_id", plan.id))
            reminders.save(ReminderSettings(enabled = true, confirmed = true,
                times = mapOf("Fajr" to LocalTime.now().withSecond(0).withNano(0).toString())))
            assertTrue(reminders.notificationsAllowed())
            test(reminders, dao, plan)
        } finally {
            reminders.setReading("fixture", plan.id, null, false)
            reminders.cancelVisible()
            for (key in listOf("planner_all_plans", "planner_active_id")) {
                val row = rows.firstOrNull { it.key == key }
                if (row == null) dao.clearCacheByPrefix(key) else dao.insertCacheEntry(row)
            }
            prefs.zip(saved).forEach { (p, values) -> restore(p, values) }
            reminders.rebuild(force = true)
        }
    }
    private fun notification() = context.getSystemService(NotificationManager::class.java).activeNotifications.firstOrNull { it.id == PlannerReminderCoordinator.NOTIFICATION_ID }

    @Test fun dueDeliveryIsDeduplicatedAndOutsideCompletionCancelsIt() = fixture { reminders, dao, plan ->
        // First delivery must arrive through the real AlarmManager + receiver path.
        withTimeout(20_000) { while (notification() == null) delay(100) }
        val first = notification() ?: error("Due reminder was not posted")
        assertEquals(3, first.notification.actions.size)
        val receipt = context.getSharedPreferences("PlannerReminderDelivery", 0).getString("receipts", null)
        reminders.rebuild(force = true, deliver = true)
        assertEquals(receipt, context.getSharedPreferences("PlannerReminderDelivery", 0).getString("receipts", null))
        assertEquals(first.postTime, notification()!!.postTime)
        val key = reminders.next.value!!.portion.key
        assertEquals(1, reminders.target(key)!!.pages.first())
        val completed = PlannerOutsideReadings.record(plan, 1, 10, PlannerEngine.formatPlannerDate(), "manual", (1..10).associateWith { 60L }).plan
        dao.insertCacheEntry(ApiResponseCacheEntity("planner_all_plans", Gson().toJson(listOf(completed))))
        reminders.rebuild(force = true, deliver = true)
        assertNull(notification()); assertNull(reminders.target(key)); assertNull(reminders.next.value)
    }
    @Test fun foregroundReadingSuppressesAndSnoozePreservesProgress() = fixture { reminders, dao, plan ->
        reminders.setReading("fixture", plan.id, null, true)
        reminders.rebuild(force = true, deliver = true)
        assertNull(notification())
        reminders.setReading("fixture", plan.id, null, false)
        reminders.rebuild(force = true, deliver = true)
        assertNotNull(notification())
        val key = reminders.next.value!!.portion.key
        val before = dao.getCacheEntry("planner_all_plans")!!.dataJson
        reminders.snooze(key)
        assertNull(notification())
        assertTrue(reminders.next.value!!.at > System.currentTimeMillis() + 8 * 60_000)
        reminders.rebuild(force = true, deliver = true)
        assertNull(notification())
        assertEquals(before, dao.getCacheEntry("planner_all_plans")!!.dataJson)
        assertEquals((1..10).toList(), reminders.target(key)!!.pages)
    }
    @Test fun changedActivePlanInvalidatesAnOldNotificationLink() = fixture { reminders, dao, plan ->
        val key = reminders.next.value!!.portion.key
        val other = plan.copy(id = "replacement-run")
        dao.insertCacheEntry(ApiResponseCacheEntity("planner_all_plans", Gson().toJson(listOf(plan, other))))
        dao.insertCacheEntry(ApiResponseCacheEntity("planner_active_id", other.id))
        reminders.rebuild(force = true)
        assertNull(reminders.target(key))
        assertEquals(other.id, reminders.next.value!!.portion.planId)
    }
}
