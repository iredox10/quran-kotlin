package com.nur.quran

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsActions
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.*
import android.view.View
import android.view.ViewGroup
import androidx.test.core.app.ActivityScenario
import androidx.compose.ui.text.AnnotatedString
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nur.quran.data.planner.*
import com.nur.quran.ui.components.planner.PlannerOutsideReadingScreen
import com.nur.quran.ui.screens.isDarkThemeGlobal
import com.nur.quran.ui.theme.applyReaderSystemBars
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicReference

/** Drives the app's real Compose semantics without Espresso's unsupported API-37 input driver. */
@RunWith(AndroidJUnit4::class)
class PlannerOutsideReadingUiTest {
    private lateinit var scenario: ActivityScenario<ComponentActivity>
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private fun owner(view: View): SemanticsOwner? {
        if (view.javaClass.name == "androidx.compose.ui.platform.AndroidComposeView") {
            return view.javaClass.getMethod("getSemanticsOwner").invoke(view) as SemanticsOwner
        }
        if (view is ViewGroup) for (i in 0 until view.childCount) owner(view.getChildAt(i))?.let { return it }
        return null
    }
    private fun nodes(activity: ComponentActivity, merged: Boolean = true): List<SemanticsNode> {
        val owner = owner(activity.window.decorView) ?: return emptyList()
        val root = if (merged) owner.rootSemanticsNode else owner.unmergedRootSemanticsNode
        val out = mutableListOf<SemanticsNode>()
        fun walk(node: SemanticsNode) { out.add(node); node.children.forEach(::walk) }
        walk(root); return out
    }
    private fun matches(node: SemanticsNode, label: String): Boolean =
        (node.config.contains(SemanticsProperties.Text) && node.config[SemanticsProperties.Text].any { it.text == label }) ||
        (node.config.contains(SemanticsProperties.ContentDescription) && label in node.config[SemanticsProperties.ContentDescription])

    private fun waitFor(predicate: (SemanticsNode) -> Boolean, merged: Boolean = true) {
        val deadline = android.os.SystemClock.uptimeMillis() + 10_000
        while (android.os.SystemClock.uptimeMillis() < deadline) {
            var found = false
            scenario.onActivity { found = nodes(it, merged).any(predicate) }
            if (found) return
            Thread.sleep(30)
        }
        scenario.onActivity { activity ->
            java.io.File(activity.cacheDir, "outside-semantics-failure.txt").writeText(nodes(activity, false).joinToString("\n") { it.config.toString() })
        }
        error("Expected form semantics were not found")
    }
    private fun act(predicate: (SemanticsNode) -> Boolean, merged: Boolean = true, action: (SemanticsNode) -> Unit) {
        waitFor(predicate, merged)
        scenario.onActivity { activity -> action(nodes(activity, merged).first(predicate)) }
    }
    private fun show(content: @Composable () -> Unit, test: () -> Unit) {
        ActivityScenario.launch(ComponentActivity::class.java).use { current ->
            scenario = current
            current.onActivity { activity -> activity.window.applyReaderSystemBars(isDarkThemeGlobal); activity.setContent { MaterialTheme { content() } } }
            test()
        }
    }
    private fun scrollItem(index: Int) {
        act({ it.config.contains(SemanticsActions.ScrollToIndex) }) { node ->
            assertTrue(node.config[SemanticsActions.ScrollToIndex].action!!.invoke(index))
        }
        Thread.sleep(150)
    }
    private fun set(label: String, value: String) {
        fun labeled(node: SemanticsNode): Boolean = matches(node, label) || node.children.any(::labeled)
        act({ node -> node.config.contains(SemanticsActions.SetText) &&
            (labeled(node) || node.parent?.let { matches(it, label) } == true || node.parent?.parent?.let { matches(it, label) } == true)
        }, merged = false) { node ->
            assertTrue(node.config[SemanticsActions.SetText].action!!.invoke(AnnotatedString(value)))
        }
        Thread.sleep(150)
    }
    private fun click(label: String) = act({ matches(it, label) && it.config.contains(SemanticsActions.OnClick) }) {
        assertFalse(it.config.contains(SemanticsProperties.Disabled))
        assertTrue(it.config[SemanticsActions.OnClick].action!!.invoke())
    }
    private fun save() {
        waitFor({ matches(it, "Save and continue reading") && !it.config.contains(SemanticsProperties.Disabled) })
        click("Save and continue reading")
    }
    private fun disabledSave() = waitFor({ matches(it, "Save reading") && it.config.contains(SemanticsProperties.Disabled) })
    private fun await(result: AtomicReference<OutsideReadingResult?>): OutsideReadingResult {
        val deadline = android.os.SystemClock.uptimeMillis() + 10_000
        while (result.get() == null && android.os.SystemClock.uptimeMillis() < deadline) Thread.sleep(30)
        return result.get() ?: error("Save callback did not complete")
    }
    private fun screenshot(name: String) {
        instrumentation.waitForIdleSync()
        instrumentation.uiAutomation.takeScreenshot().let { bitmap ->
            java.io.File(instrumentation.targetContext.cacheDir, name).outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    @Test fun rangeUsesThePersonalAverageAndContinuesAtTheNextPlannerPage() {
        val plan = PlannerEngine.buildReadingPlanner("page", 3, PlannerEngine.formatPlannerDate(), 1, 9, chapters = emptyList())
        val result = AtomicReference<OutsideReadingResult?>()
        show({
            PlannerOutsideReadingScreen(plan, PlannerPagePace(120, 6), {}) { first, last, date, timing, times, id ->
                result.set(PlannerOutsideReadings.record(plan, first, last, date, timing, times, PlannerPagePace(120, 6), id))
            }
        }) {
        scrollItem(1)
        set("Number of pages", "3")
        scrollItem(3)
        screenshot("outside-reading-light.png")
        save()
        val saved = await(result)
        assertEquals(360L, saved.entry.totalSeconds)
        assertEquals("estimated", saved.entry.timing)
        assertEquals(4, saved.resumePage); assertEquals(2, saved.resumeDay)
        }
    }

    @Test fun noMeasuredPaceUsesManualTimeAndSupportsIndividualPageOverrides() {
        val plan = PlannerEngine.buildReadingPlanner("page", 2, PlannerEngine.formatPlannerDate(), 1, 20, chapters = emptyList())
        val result = AtomicReference<OutsideReadingResult?>()
        val wasDark = isDarkThemeGlobal
        try {
            instrumentation.runOnMainSync { isDarkThemeGlobal = true }
            show({
                PlannerOutsideReadingScreen(plan, null, {}) { first, last, date, timing, times, id ->
                    result.set(PlannerOutsideReadings.record(plan, first, last, date, timing, times, id = id))
                }
            }) {
            scrollItem(1)
            click("Last page read")
            set("Last page", "12")
            scrollItem(3)
            set("Minutes", "2")
            click("Set a different time for each page")
            scrollItem(15)
            set("Page 12 Seconds", "15")
            waitFor({ matches(it, "Save and continue reading") && !it.config.contains(SemanticsProperties.Disabled) })
            screenshot("outside-reading-dark-long.png")
            save()
            val saved = await(result)
            assertEquals("manual", saved.entry.timing)
            assertEquals(135L, saved.entry.pageSeconds[12])
            assertEquals(1455L, saved.entry.totalSeconds)
            assertEquals(13, saved.resumePage)
            }
        } finally { instrumentation.runOnMainSync { isDarkThemeGlobal = wasDark } }
    }

    @Test fun historyShowsEnteredEstimatedAndMeasuredSourcesSeparately() {
        val initial = PlannerEngine.buildReadingPlanner("page", 1, PlannerEngine.formatPlannerDate(), 1, 2, chapters = emptyList())
        val entered = PlannerOutsideReadings.record(initial, 1, 1, PlannerEngine.formatPlannerDate(), "manual", mapOf(1 to 90L)).plan
        val completed = PlannerOutsideReadings.record(entered, 2, 2, PlannerEngine.formatPlannerDate(), "estimated", pace = PlannerPagePace(120, 5)).plan
        show({
            com.nur.quran.ui.components.planner.PlannerHistoryScreen(listOf(completed), emptyMap(), emptyMap(), emptyMap(),
                onBack = {}, onOpenPlan = {}, selectedPlanId = completed.id)
        }) {
        scrollItem(1)
        waitFor({ matches(it, "Outside app · entered · 1m 30s") })
        waitFor({ matches(it, "Outside app · estimated · 2m 0s") })
        screenshot("outside-reading-history-sources.png")
        }
    }

    @Test fun activePlannerLocksTheCurrentPageAndRejectsCountsBeyondItsEnd() {
        val plan = PlannerEngine.buildReadingPlanner("page", 3, PlannerEngine.formatPlannerDate(), 1, 9, chapters = emptyList()).copy(lastReadPage = 4)
        val result = AtomicReference<OutsideReadingResult?>()
        show({
            PlannerOutsideReadingScreen(plan, PlannerPagePace(120, 3), {}) { first, last, date, timing, times, id ->
                result.set(PlannerOutsideReadings.record(plan, first, last, date, timing, times, PlannerPagePace(120, 3), id))
            }
        }) {
        waitFor({ matches(it, "Current page · 4") })
        waitFor({ matches(it, "Active planner · pages 1–9") })
        scrollItem(1)
        set("Number of pages", "7")
        disabledSave()
        assertNull(result.get())
        click("Last page read")
        set("Last page", "3")
        disabledSave()
        assertNull(result.get())
        set("Last page", "5")
        save()
        val saved = await(result)
        assertEquals(setOf(4, 5), saved.entry.pageSeconds.keys)
        assertEquals(6, saved.resumePage)
        }
    }
}
