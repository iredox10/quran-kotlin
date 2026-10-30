package com.nur.quran

import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nur.quran.data.planner.*
import com.nur.quran.ui.components.planner.PlannerHistoryDialog
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Uses platform accessibility because the emulator API is newer than Espresso's input driver. */
@RunWith(AndroidJUnit4::class)
class PlannerHistoryUiTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private fun find(label: String): AccessibilityNodeInfo? {
        fun visit(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
            if (node == null) return null
            if (node.text?.toString() == label || node.contentDescription?.toString() == label) return node
            for (index in 0 until node.childCount) visit(node.getChild(index))?.let { return it }
            return null
        }
        return visit(instrumentation.uiAutomation.rootInActiveWindow)
    }
    private fun waitFor(label: String): AccessibilityNodeInfo {
        val until = System.currentTimeMillis() + 10000
        while (System.currentTimeMillis() < until) {
            find(label)?.let { return it }
            Thread.sleep(100)
        }
        error("History text not found: $label")
    }
    private fun click(label: String) {
        var node: AccessibilityNodeInfo? = waitFor(label)
        while (node != null && !node.isClickable) node = node.parent
        assertTrue("History control must accept clicks: $label", node?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true)
    }
    private fun scrollTo(label: String) {
        repeat(12) {
            if (find(label) != null) return
            fun scroll(node: AccessibilityNodeInfo?): Boolean {
                if (node == null) return false
                if (node.isScrollable) return node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                for (i in 0 until node.childCount) if (scroll(node.getChild(i))) return true
                return false
            }
            scroll(instrumentation.uiAutomation.rootInActiveWindow)
            Thread.sleep(250)
        }
        waitFor(label)
    }
    @Test fun completedHistoryShowsDailyTimeAccumulatedTimeAndReflections() {
        val built = PlannerEngine.buildReadingPlanner("page", 2, "2026-10-01", 1, 4, chapters = emptyList())
        val completed = built.copy(title = "A little every day", assignmentProgress = mapOf(1 to 2, 2 to 2), assignmentCompletedAt = mapOf(1 to "2026-10-01", 2 to "2026-10-03"))
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            scenario.onActivity { activity -> activity.setContent {
                MaterialTheme { PlannerHistoryDialog(
                    listOf(completed), mapOf(completed.id to mapOf(
                        1 to PlannerSessionTimer(1800, secondsByDate = mapOf("2026-10-01" to 1800)),
                        2 to PlannerSessionTimer(3600, secondsByDate = mapOf("2026-10-03" to 3600))
                    )), mapOf(completed.id to mapOf(1 to "Slow down and reflect.")), emptyMap(), {}
                ) }
            } }
            click("A little every day")
            waitFor("Total recorded reading time")
            waitFor("1h 30m")
            val shot = instrumentation.uiAutomation.takeScreenshot()
            java.io.File(instrumentation.targetContext.cacheDir, "planner-history-native.png").outputStream().use {
                shot.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
            }
            scrollTo("1h 30m accumulated")
            scrollTo("Slow down and reflect.")
            click("Back to planner history")
            waitFor("Your reading journey")
        }
    }
    @Test fun emptyHistoryExplainsHowPlansAppear() {
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            scenario.onActivity { activity -> activity.setContent { MaterialTheme {
                PlannerHistoryDialog(emptyList(), emptyMap(), emptyMap(), emptyMap(), {})
            } } }
            waitFor("No completed plans yet")
            click("Completed")
            waitFor("No completed plans yet")
        }
    }
}
