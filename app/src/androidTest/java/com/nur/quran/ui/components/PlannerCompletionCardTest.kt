package com.nur.quran.ui.components

import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.JsonParser
import com.nur.quran.R
import com.nur.quran.data.db.entities.VerseEntity
import com.nur.quran.ui.screens.DayCelebrationCard
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlannerCompletionCardTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private fun find(predicate: (AccessibilityNodeInfo) -> Boolean, node: AccessibilityNodeInfo? = instrumentation.uiAutomation.rootInActiveWindow): AccessibilityNodeInfo? {
        node ?: return null
        if (predicate(node)) return node
        for (i in 0 until node.childCount) find(predicate, node.getChild(i))?.let { return it }
        return null
    }
    @Test fun longestAyahScrollsWhileCloseAndReturnRemainReachable() {
        val context = instrumentation.targetContext
        val row = context.assets.open("data/quran_full.json").bufferedReader().use { reader ->
            JsonParser.parseReader(reader).asJsonArray.first { it.asJsonObject["verse_key"].asString == "2:282" }.asJsonObject
        }
        val verse = VerseEntity(1, 2, 282, "2:282", row["text_uthmani"].asString, null, null, 48, 3, row["translation"].asString)
        var returns = 0
        val returnLabel = context.getString(R.string.planner_return_planner)
        val closeLabel = context.getString(R.string.ui_close_bbfa77)
        fun assertControlsVisible() {
            assertNotNull("Return action must stay visible", find({ it.text?.toString() == returnLabel && it.isVisibleToUser }))
            assertNotNull("Close must stay visible", find({ it.contentDescription?.toString() == closeLabel && it.isVisibleToUser }))
        }
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            scenario.onActivity { activity -> activity.setContent {
                MaterialTheme {
                    DayCelebrationCard(1, verse, "", 120, {}, { returns++ }, Modifier.height(420.dp))
                }
            } }
            repeat(50) { if (find({ it.isScrollable }) == null) Thread.sleep(100) }
            assertControlsVisible()
            val reflectionLabel = context.getString(R.string.planner_reflection_optional)
            repeat(40) {
                if (find({ it.text?.toString() == reflectionLabel && it.isVisibleToUser }) == null) {
                    (find({ it.isScrollable }) ?: error("Long completion content must scroll"))
                        .performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                    Thread.sleep(75)
                }
            }
            assertNotNull("Reflection must be reachable below the long verse", find({ it.text?.toString() == reflectionLabel && it.isVisibleToUser }))
            assertControlsVisible()
            var button = find({ it.text?.toString() == returnLabel }) ?: error("Missing return action")
            while (!button.isClickable) button = button.parent ?: error("Return action is not clickable")
            assertTrue(button.performAction(AccessibilityNodeInfo.ACTION_CLICK))
            instrumentation.waitForIdleSync()
            assertEquals(1, returns)
        }
    }
}
