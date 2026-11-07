package com.nur.quran

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nur.quran.data.hifdh.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HifdhJournalTest {
    @Test fun journalIsIdempotentAccountScopedAndSyncRetainsBothDevicesEvents() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val owner = "test-${java.util.UUID.randomUUID()}"
        val local = HifdhJournal(context, owner)
        val other = HifdhJournal(context, "$owner-other")
        val event = HifdhReviewEvent(verseKey = "1:1", rating = 3)
        local.append(event); local.append(event)
        assertEquals(1, local.events().size)
        assertTrue(other.events().isEmpty())
        val remote = HifdhReviewEvent(verseKey = "1:2", rating = 1)
        other.append(remote)
        local.mergeEnvelope(other.syncEnvelope()); other.mergeEnvelope(local.syncEnvelope())
        assertEquals(setOf(event.id, remote.id), local.events().map { it.id }.toSet())
        assertEquals(setOf(event.id, remote.id), other.events().map { it.id }.toSet())
        local.mergeEnvelope(mapOf("wrong-id" to remote))
        assertEquals(2, local.events().size)
    }
}
