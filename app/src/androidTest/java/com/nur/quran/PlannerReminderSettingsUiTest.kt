package com.nur.quran

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.AnnotatedString
import android.view.View
import android.view.ViewGroup
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nur.quran.data.reminders.*
import com.nur.quran.ui.screens.PlannerReminderSettingsScreen
import com.nur.quran.ui.screens.isDarkThemeGlobal
import com.nur.quran.ui.theme.applyReaderSystemBars
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(AndroidJUnit4::class)
class PlannerReminderSettingsUiTest {
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
            java.io.File(activity.cacheDir, "reminder-semantics-failure.txt").writeText(nodes(activity, false).joinToString("\n") { it.config.toString() })
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

    private fun screenshot(name: String) {
        instrumentation.waitForIdleSync()
        instrumentation.uiAutomation.takeScreenshot().let { bitmap ->
            java.io.File(instrumentation.targetContext.cacheDir, name).outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
    @Test fun setupRequiresConfirmationAndSavesTheChosenReminderStrength() = runBlocking {
        val context = instrumentation.targetContext
        val reminders = context.plannerReminders()
        val prefs = context.getSharedPreferences("PlannerReminders", 0)
        val old = prefs.getString("settings", null)
        val plannerPrefs = context.getSharedPreferences("PlannerSettings", 0)
        val oldPrayers = plannerPrefs.getStringSet("active_prayers", null)?.toSet()
        val dark = isDarkThemeGlobal
        val saved = AtomicBoolean(false)
        try {
            plannerPrefs.edit().putStringSet("active_prayers", setOf("Fajr")).commit()
            reminders.save(ReminderSettings(times = mapOf("Fajr" to "05:30")))
            instrumentation.runOnMainSync { isDarkThemeGlobal = true }
            show({ PlannerReminderSettingsScreen(reminders) { saved.set(true) } }) {
                act({ it.config.contains(SemanticsProperties.ToggleableState) && it.config.contains(SemanticsActions.OnClick) }) {
                    assertTrue(it.config[SemanticsActions.OnClick].action!!.invoke())
                }
                scrollItem(6)
                waitFor({ matches(it, "Save reminders") && it.config.contains(SemanticsProperties.Disabled) })
                scrollItem(3)
                click("I have checked these local times")
                scrollItem(4)
                click("Strong")
                screenshot("reminder-settings-dark.png")
                scrollItem(6)
                click("Save reminders")
                val until = android.os.SystemClock.uptimeMillis() + 10_000
                while (!saved.get() && android.os.SystemClock.uptimeMillis() < until) Thread.sleep(30)
                assertTrue(saved.get())
                assertTrue(reminders.settings.value.enabled)
                assertEquals("strong", reminders.settings.value.intensity)
                assertTrue(reminders.settings.value.confirmed)
            }
        } finally {
            instrumentation.runOnMainSync { isDarkThemeGlobal = dark }
            prefs.edit().apply { if (old == null) remove("settings") else putString("settings", old) }.commit()
            plannerPrefs.edit().apply { if (oldPrayers == null) remove("active_prayers") else putStringSet("active_prayers", oldPrayers) }.commit()
            reminders.rebuild(force = true)
        }
    }
}
