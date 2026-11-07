package com.nur.quran

import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nur.quran.data.hifdh.HifdhRecordings
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HifdhRecordingsDeviceTest {
    @Test fun explicitlyRecordedAudioCanBeReplayedDeletedAndNeverCrossesAccounts() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val owner = "record-test-${java.util.UUID.randomUUID()}"
        val store = HifdhRecordings(context, owner)
        val other = HifdhRecordings(context, "$owner-other")
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            try {
                scenario.onActivity { store.start { store.stop() } }
                Thread.sleep(1600)
                var id: String? = null
                scenario.onActivity { id = store.stop() }
                assertNotNull(id)
                val file = store.file(id!!)!!
                assertTrue(file.length() > 0)
                assertNull(other.file(id!!))
                assertNull(store.file("../../shared.m4a"))
                scenario.onActivity {
                    android.media.MediaPlayer().apply { setDataSource(file.absolutePath); prepare(); start(); stop(); release() }
                }
                store.delete(id!!)
                assertNull(store.file(id!!))
            } finally { store.stop(); store.files().forEach { store.delete(it.name) } }
        }
    }
}
