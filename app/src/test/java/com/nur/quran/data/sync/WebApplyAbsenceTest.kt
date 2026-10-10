package com.nur.quran.data.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The remaining apply/emit checks: canonicalization of the mushaf preset, a payload that
 * only carries keys no device stores, and the scale keys that must stay absent.
 */
class WebApplyAbsenceTest {

    private val web = webState()

    @Test
    fun `mushaf preset is canonicalized before it is re-emitted`() {
        val rig = syncRig()
        rig.applyRemote(web + mapOf("mushafId" to "tajweed"))
        assertEquals("madani-tajweed", rig.snapshot()["mushafPreset"])
    }

    @Test
    fun `a web payload with no settings leaves prefs untouched`() {
        val seed = mapOf("is_dark_theme" to true, "translation_id" to 131)
        val rig = syncRig(seed)
        rig.applyRemote(mapOf("theme" to null, "bookmarks" to listOf(mapOf("verseKey" to "1:1"))))
        val snapshot = rig.snapshot()
        assertTrue(snapshot["theme"] == "dark")
        assertEquals(131.0, numberOf(snapshot["translationId"]))
        assertNull(snapshot["reciterId"])
    }

    @Test
    fun `scale keys absent from a payload never get re-emitted`() {
        val rig = syncRig()
        rig.applyRemote(web)
        val snapshot = rig.snapshot()
        assertNull(snapshot["arabicScale"])
        assertNull(snapshot["translationScale"])
        assertNull(snapshot["wordTapBehavior"])
        assertEquals(1.0, numberOf(snapshot["version"]))
    }
}
/** JSON hands back Double for every number; the snapshot side emits Int/Long. */
private fun assertSameValue(web: Any?, snapshot: Any?) {
    if (web is Number) assertEquals((web as Number).toDouble(), numberOf(snapshot))
    else assertEquals(web, snapshot)
}
