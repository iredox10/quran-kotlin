package com.nur.quran.ui.components

import com.nur.quran.R
import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Accessibility actions also work on new Android releases unsupported by old Espresso injection. */
@RunWith(AndroidJUnit4::class)
class ReaderPageNavigationTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val nextLabel get() = instrumentation.targetContext.getString(R.string.ui_next_0fed81)
    private val previousLabel get() = instrumentation.targetContext.getString(R.string.ui_previous_f67a76)
    private val finishLabel get() = instrumentation.targetContext.getString(R.string.planner_finish_day)
    private fun find(predicate: (AccessibilityNodeInfo) -> Boolean, node: AccessibilityNodeInfo? = instrumentation.uiAutomation.rootInActiveWindow): AccessibilityNodeInfo? {
        node ?: return null
        if (predicate(node)) return node
        for (index in 0 until node.childCount) find(predicate, node.getChild(index))?.let { return it }
        return null
    }
    private fun waitFor(label: String, enabled: Boolean? = null): AccessibilityNodeInfo {
        repeat(80) {
            find({ it.isVisibleToUser && it.text?.toString() == label && (enabled == null || it.isEnabled == enabled) })?.let { return it }
            Thread.sleep(100)
        }
        error("Missing $label (enabled=$enabled)")
    }
    private fun click(label: String) {
        assertTrue(waitFor(label, true).performAction(AccessibilityNodeInfo.ACTION_CLICK))
        instrumentation.waitForIdleSync()
    }
    private fun viewport(): Rect = Rect().also { bounds ->
        val list = find({ it.isScrollable }) ?: error("Missing reader list")
        list.getBoundsInScreen(bounds)
    }

    @Test fun scrollingToFooterKeepsViewportStableAndDoesNotAdvancePage() {
        var page by mutableIntStateOf(5)
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            scenario.onActivity { activity -> activity.setContent {
                MaterialTheme {
                    LazyColumn(state = rememberLazyListState(), modifier = Modifier.fillMaxWidth().height(300.dp)) {
                        items(20) { Text("Verse $it", modifier = Modifier.height(64.dp)) }
                        item {
                            ReaderPageNavigation(page = page, firstPage = 5, lastPage = 7,
                                onNext = { page++ }, onPrevious = { page-- })
                        }
                    }
                }
            } }
            waitFor("Verse 0")
            val before = viewport()
            repeat(12) {
                if (find({ it.isVisibleToUser && it.text?.toString() == nextLabel }) == null) {
                    assertTrue(find({ it.isScrollable })!!.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD))
                    Thread.sleep(200)
                }
            }
            waitFor(nextLabel)
            assertEquals(5, page)
            assertEquals(before, viewport())
            waitFor(previousLabel, false)
            click(nextLabel)
            waitFor("6 / 7")
            assertEquals(6, page)
            click(previousLabel)
            waitFor("5 / 7")
            assertEquals(5, page)
        }
    }

    @Test fun finalAssignmentPageFinishesOnceAndPreviousRemainsAvailable() {
        var page by mutableIntStateOf(3)
        var finishes by mutableIntStateOf(0)
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            scenario.onActivity { activity -> activity.setContent {
                MaterialTheme {
                    ReaderPageNavigation(page = page, firstPage = 2, lastPage = 3,
                        onNext = { page++ }, onPrevious = { page-- },
                        onFinish = { finishes++ }, isCompleted = finishes > 0)
                }
            } }
            click(finishLabel)
            waitFor(finishLabel, false)
            assertEquals(1, finishes)
            click(previousLabel)
            waitFor(previousLabel, false)
            waitFor(nextLabel, true)
            assertEquals(2, page)
        }
    }
}
