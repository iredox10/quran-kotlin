package com.nur.quran

import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.gson.Gson
import com.nur.quran.data.db.QuranDatabase
import com.nur.quran.data.db.dao.QuranDao
import com.nur.quran.data.db.entities.*
import com.nur.quran.data.planner.*
import com.nur.quran.data.repository.QuranRepository
import com.nur.quran.ui.viewmodels.PlannerViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlannerOutsideReadingPersistenceTest {
    @Test fun aSavedOutsideLogSurvivesColdLoadAndRetriesDoNotAddTimeAgain() = runBlocking {
        lateinit var repository: QuranRepository
        lateinit var dao: QuranDao
        lateinit var oldPlans: List<ReadingPlan>
        lateinit var oldArchives: List<ReadingPlan>
        var oldId: String? = null
        var timestamp: Long? = null
        var ready = false
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = context.getSharedPreferences("ReaderResume", 0)
        val oldResume = prefs.all.toMap()
        var previousRecent: RecentlyReadEntity? = null
        val fixture = PlannerEngine.buildReadingPlanner("page", 2, PlannerEngine.formatPlannerDate(), 1, 4, chapters = emptyList())
        suspend fun await(test: suspend () -> Boolean) = withTimeout(20_000) { while (!test()) delay(40) }
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                lateinit var vm: PlannerViewModel
                scenario.onActivity { vm = ViewModelProvider(it)[PlannerViewModel::class.java] }
                repository = vm.javaClass.getDeclaredField("repository").apply { isAccessible = true }.get(vm) as QuranRepository
                dao = repository.javaClass.getDeclaredField("quranDao").apply { isAccessible = true }.get(repository) as QuranDao
                oldPlans = repository.getAllPlans(); oldArchives = repository.getArchivedPlans(); oldId = repository.getActivePlannerId()
                previousRecent = dao.getRecentlyReadForChapter(2); ready = true
                scenario.onActivity { vm.setActivePlan(fixture) }
                await { repository.getActivePlannerId() == fixture.id }
                val result = withContext(Dispatchers.Main) { vm.recordOutsideReading(fixture.id, 1, 2,
                    PlannerEngine.formatPlannerDate(), "manual", mapOf(1 to 90L, 2 to 150L), "outside-fixture") }
                timestamp = result.entry.createdAt
                assertEquals(3, result.resumePage); assertEquals(2, result.resumeDay)
                assertEquals(240L, repository.getReadingSessionsFlow().first().single { it.timestamp == timestamp }.duration)
                assertEquals("outside-fixture", repository.getAllPlans().single { it.id == fixture.id }.outsideReadings.keys.single())
                assertEquals(fixture.id, prefs.getString("plannerId", null))
                assertEquals(2, prefs.getInt("plannerDay", 0))
            }
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                lateinit var vm: PlannerViewModel
                scenario.onActivity { vm = ViewModelProvider(it)[PlannerViewModel::class.java] }
                await { vm.activePlan.value?.id == fixture.id }
                val restored = vm.activePlan.value!!
                assertEquals(240L, PlannerHistory.summarize(restored, emptyMap()).totalSeconds)
                assertEquals(3, restored.lastReadPage)
                assertEquals(3, PlannerEngine.getAssignmentReadingResumePage(restored, restored.assignments[1]))
                withContext(Dispatchers.Main) { vm.recordOutsideReading(fixture.id, 1, 2,
                    PlannerEngine.formatPlannerDate(), "manual", mapOf(1 to 90L, 2 to 150L), "outside-fixture") }
                assertEquals(1, repository.getReadingSessionsFlow().first().count { it.timestamp == timestamp })
                assertEquals(1, vm.activePlan.value!!.outsideReadings.size)
                // Finish within the app, then restart: old run keeps the log; new run does not.
                scenario.onActivity { vm.markPageRead(2, 3); vm.markPageRead(2, 4) }
                await { vm.activePlan.value?.let(PlannerHistory::isComplete) == true }
                scenario.onActivity { vm.restartActivePlan() }
                await { vm.activePlan.value?.id != fixture.id }
                assertTrue(vm.activePlan.value!!.outsideReadings.isEmpty())
                assertEquals(240L, PlannerHistory.summarize(vm.archivedPlans.value.single { it.id == fixture.id }, emptyMap()).totalSeconds)
                await { repository.getActivePlannerId() == vm.activePlannerId.value }
            }
        } finally {
            if (ready) {
                repository.savePlannerSelection(oldPlans, oldId); repository.saveArchivedPlans(oldArchives)
                withContext(Dispatchers.IO) {
                    val db = dao.javaClass.getDeclaredField("__db").apply { isAccessible = true }.get(dao) as RoomDatabase
                    timestamp?.let { db.openHelper.writableDatabase.execSQL("DELETE FROM reading_sessions WHERE timestamp = ?", arrayOf(it)) }
                    if (previousRecent != null) dao.upsertRecentlyRead(previousRecent!!) else dao.deleteRecentlyReadByChapter(2)
                }
                prefs.edit().clear().apply {
                    oldResume.forEach { (key, value) -> when (value) {
                        is String -> putString(key, value); is Int -> putInt(key, value); is Long -> putLong(key, value)
                        is Boolean -> putBoolean(key, value)
                    } }
                }.commit()
            }
        }
    }

    @Test fun pagesAndLogRollBackWhenTheAnalyticsWriteFails() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, QuranDatabase::class.java).build()
        try {
            val dao = db.quranDao()
            val baseline = listOf(ApiResponseCacheEntity("planner_all_plans", "before"))
            dao.insertCacheEntries(baseline)
            withContext(Dispatchers.IO) { db.openHelper.writableDatabase.execSQL(
                "CREATE TRIGGER fail_outside_session BEFORE INSERT ON reading_sessions BEGIN SELECT RAISE(ABORT, 'fixture rollback'); END") }
            var failed = false
            try { dao.saveOutsidePlannerReading(listOf(ApiResponseCacheEntity("planner_all_plans", "after")),
                ReadingSessionEntity(date = "2026-11-01", duration = 120)) } catch (_: Exception) { failed = true }
            assertTrue(failed)
            assertEquals("before", dao.getCacheEntry("planner_all_plans")!!.dataJson)
            assertTrue(dao.getReadingSessions().first().isEmpty())
        } finally { db.close() }
    }
}
