package com.nur.quran

import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nur.quran.data.planner.*
import com.nur.quran.ui.components.planner.PlanTodayTab
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class PlannerPrayerResumeUiTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private fun find(node: AccessibilityNodeInfo?, label: String): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.text?.toString() == label) return node
        for (i in 0 until node.childCount) find(node.getChild(i),label)?.let { return it }
        return null
    }
    @Test fun resumeBelongsToAsrRatherThanTheEarlierFajrGap() {
        val plan = PlannerEngine.buildReadingPlanner("page",1,PlannerEngine.formatPlannerDate(),1,11,chapters=emptyList())
            .copy(assignmentReadPages=mapOf(1 to listOf(1,2,4,5)),lastReadPage=6)
        val assignment = plan.assignments[0]
        val progress = PlannerEngine.getAssignmentProgress(plan,assignment)
        val clicks = AtomicInteger()
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            scenario.onActivity { activity -> activity.setContent { MaterialTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    PlanTodayTab(plan,assignment,36,progress.readPagesCount,11,"Pages",14,
                        PlannerEngine.buildPrayerSlots(plan,assignment,null),
                        { clicks.incrementAndGet() }, {}, {}, {}, {})
                }
            } } }
            var resume: AccessibilityNodeInfo? = null
            fun scroll(node: AccessibilityNodeInfo?): Boolean {
                if (node == null) return false
                if (node.isScrollable && node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)) return true
                for (i in 0 until node.childCount) if (scroll(node.getChild(i))) return true
                return false
            }
            repeat(100) { attempt ->
                if (resume == null) {
                    resume = find(instrumentation.uiAutomation.rootInActiveWindow,"Resume")
                    if (resume == null) {
                        if (attempt % 5 == 4) scroll(instrumentation.uiAutomation.rootInActiveWindow)
                        Thread.sleep(100)
                    }
                }
            }
            assertNotNull("Resume action must exist",resume)
            val asr = find(instrumentation.uiAutomation.rootInActiveWindow,"Asr")
            assertNotNull("Asr card must be visible with Resume",asr)
            val buttonBounds = android.graphics.Rect().also { resume!!.getBoundsInScreen(it) }
            val asrBounds = android.graphics.Rect().also { asr!!.getBoundsInScreen(it) }
            val tolerance = instrumentation.targetContext.resources.displayMetrics.density * 48
            assertTrue("Resume must align with Asr, not Fajr",kotlin.math.abs(buttonBounds.centerY()-asrBounds.centerY()) < tolerance)
            var button = resume
            while (button != null && !button!!.isClickable) button = button!!.parent
            assertTrue(button?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true)
            repeat(20) { if (clicks.get()==0) Thread.sleep(50) }
            assertEquals(1,clicks.get())
            val shot = instrumentation.uiAutomation.takeScreenshot()
            java.io.File(instrumentation.targetContext.cacheDir,"planner-prayer-resume.png").outputStream().use {
                shot.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)
            }
        }
    }
}
