package com.nur.quran.data.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test


/**
 * Web truth getSyncableState vs the payload `SnapshotBuilder` emits. Only the fields both
 * sides genuinely share are asserted here — a pull must land the web value in the local
 * store, and the next push must put the very same value back on the wire.
 */
class WebApplyEmitTest {

    private val web = webState()

    @Test
    fun `applied web settings come back out of the snapshot unchanged`() {
        val rig = syncRig()
        rig.applyRemote(web)
        val snapshot = rig.snapshot()

        assertEquals("dark", snapshot["theme"])
        assertEquals(131.0, numberOf(snapshot["translationId"]))
        // reciterId is apply-only: numeric web values land in prefs, but are
        // never re-emitted — web uses string reciter keys and an int would
        // clobber them under last-write-wins merge.
        assertNull(snapshot["reciterId"])
        assertEquals(3, rig.prefs.values["reciter_id"])
        assertEquals(169.0, numberOf(snapshot["tafsirId"]))
        assertTrue(snapshot["tajweedEnabled"] == true)
        assertEquals("Amiri Quran", snapshot["arabicFont"])
        assertEquals("madani-standard", snapshot["mushafPreset"])
        assertEquals("madani-standard", snapshot["mushafId"])
    }

    @Test
    fun `every value that survives equals the web value exactly`() {
        val rig = syncRig()
        rig.applyRemote(web)
        val snapshot = rig.snapshot()
        listOf("theme", "translationId", "tafsirId", "tajweedEnabled", "arabicFont")
            .forEach { field -> assertSameValue(web[field], snapshot[field]) }
        listOf("mushafId", "mushafPreset")
            .forEach { field -> assertSameValue(web["mushafId"], snapshot[field]) }
    }

    @Test
    fun `reading mode round trips and keeps translationEnabled as its inverse`() {
        val rig = syncRig()
        rig.applyRemote(web)
        val snapshot = rig.snapshot()
        assertTrue(snapshot["readingMode"] == true)
        assertFalse(snapshot["translationEnabled"] as Boolean)
        assertEquals(web["readingMode"], snapshot["readingMode"])
    }
}
