package com.nur.quran

import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nur.quran.data.db.entities.VerseEntity
import com.nur.quran.data.hifdh.*
import com.nur.quran.ui.components.HifdhTestModal
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CopyOnWriteArrayList

@RunWith(AndroidJUnit4::class)
class HifdhPracticeDeviceTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private fun find(text: String): AccessibilityNodeInfo? {
        fun visit(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
            if (node == null) return null
            if (node.text?.toString() == text) return node
            for (i in 0 until node.childCount) visit(node.getChild(i))?.let { return it }
            return null
        }
        return visit(instrumentation.uiAutomation.rootInActiveWindow)
    }
    private fun click(text: String) {
        val until = System.currentTimeMillis() + 10_000
        var node: AccessibilityNodeInfo? = null
        while (System.currentTimeMillis() < until && node == null) { node = find(text); if (node == null) Thread.sleep(100) }
        while (node != null && !node.isClickable) node = node.parent
        assertTrue("Control missing: $text", node?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true)
        Thread.sleep(400)
    }
    @Test fun promptedRecallIsFailureAndRetryIsOnlyCorrection() {
        val attempts = CopyOnWriteArrayList<RecallAssessment>()
        val verse = VerseEntity(1, 1, 1, "1:1", "بِسْمِ اللَّهِ", null, null, 1, 1)
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            scenario.onActivity { activity -> activity.setContent { MaterialTheme {
                HifdhTestModal(listOf("1:1"), "Hifdh", emptyList(), {}, { _, _ -> error("Legacy callback should not be called") },
                    loadVerses = { mapOf("1:1" to verse) }, onAssessment = { _, result -> attempts.add(result) })
            } } }
            click("Tap to Reveal")
            click("Needed Prompt · Again")
            assertEquals(1, attempts.size)
            assertEquals(1, attempts[0].schedulerRating)
            assertEquals(RecallCue.ANSWER_PROMPT, attempts[0].cue)
            assertFalse(attempts[0].independent)
            click("Test Another Ayah")
            click("Tap to Reveal")
            click("Perfect")
            assertEquals(2, attempts.size)
            assertEquals(RecallTask.CORRECTION, attempts[1].task)
            assertFalse(attempts[1].independent)
        }
    }
    @Test fun translationFootnotesAndHtmlDoNotLeakIntoReadingText() {
        assertEquals("Allah & mercy", com.nur.quran.ui.components.hifdhTranslationText("Allah<sup foot_note=1>2</sup> &amp; mercy"))
        assertEquals("", com.nur.quran.ui.components.hifdhTranslationText(null))
    }
    @Test fun similarPassagesUseBundledOriginalText() {
        val context = instrumentation.targetContext
        val matches = HifdhSimilarity(context).find("55:13")
        assertTrue(matches.isNotEmpty())
        assertTrue(matches.none { it.verseKey == "55:13" })
        assertTrue(matches.all { it.sharedWords.isNotEmpty() && it.text.any { ch -> ch in '\u064B'..'\u065F' } })
        assertTrue(matches.any { it.verseKey.startsWith("55:") })
    }
}
