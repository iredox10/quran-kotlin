package com.nur.quran

import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nur.quran.data.mushaf.ReaderMushafPreference
import com.nur.quran.ui.components.PreferredMushafReader
import com.nur.quran.ui.screens.isDarkThemeGlobal
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PreferredMushafUiTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private fun find(label: String, node: AccessibilityNodeInfo? = instrumentation.uiAutomation.rootInActiveWindow): AccessibilityNodeInfo? {
        node ?: return null
        if (node.isVisibleToUser && (node.text?.toString() == label || node.contentDescription?.toString() == label)) return node
        for (i in 0 until node.childCount) find(label, node.getChild(i))?.let { return it }
        return null
    }
    private fun waitFor(label: String): AccessibilityNodeInfo {
        repeat(200) { find(label)?.let { return it }; Thread.sleep(100) }
        error("Missing $label")
    }
    private fun click(label: String) {
        var node = waitFor(label)
        while (!node.isClickable) node = node.parent ?: error("Not clickable: $label")
        assertTrue(node.performAction(AccessibilityNodeInfo.ACTION_CLICK))
        instrumentation.waitForIdleSync()
    }
    private fun open(): ActivityScenario<ComponentActivity> = ActivityScenario.launch(ComponentActivity::class.java).also { scenario ->
        val context = instrumentation.targetContext
        scenario.onActivity { activity -> activity.setContent { MaterialTheme {
            PreferredMushafReader(onDismiss = {}, onSelectHafs = { ReaderMushafPreference.select(context, it) }) {
                Text("Hafs test reader")
            }
        } } }
    }
    @Test fun defaultSurvivesReopeningAndHeaderControlsWork() {
        val context = instrumentation.targetContext
        val selection = ReaderMushafPreference.preferences(context)
        val selectedBefore = selection.getString(ReaderMushafPreference.KEY, null)
        val reader = context.getSharedPreferences("NarrationReader", 0)
        val warshBefore = reader.getInt("warsh", 1)
        val qaloonBefore = reader.getInt("qaloon", 1)
        val speedBefore = reader.getInt("scroll_speed", 3)
        val settings = context.getSharedPreferences("Settings", 0)
        val darkBefore = settings.getBoolean("is_dark_theme", false)
        val globalBefore = isDarkThemeGlobal
        reader.edit().putInt("warsh", 1).putInt("qaloon", 1).putInt("scroll_speed", 3).commit()
        ReaderMushafPreference.select(context, "warsh")
        try {
            open().use { scenario ->
                waitFor("1 / 604")
                assertEquals("warsh", ReaderMushafPreference.selected(context))
                scenario.onActivity { isDarkThemeGlobal = darkBefore }
                click(context.getString(R.string.ui_toggle_theme_9b0eaf))
                assertEquals(!darkBefore, settings.getBoolean("is_dark_theme", darkBefore))
                click(context.getString(R.string.ui_toggle_theme_9b0eaf))
                click(context.getString(R.string.ui_auto_scroll_a03e7f))
                repeat(4) { click(context.getString(R.string.ui_increase_speed_55fa91)) }
                waitFor("2 / 604")
                click("Pause")
                assertEquals(2, reader.getInt("warsh", 0))
                click(context.getString(R.string.ui_close_auto_scroll_b6f06a))
            }
            open().use { scenario ->
                waitFor("2 / 604")
                assertEquals("warsh", ReaderMushafPreference.selected(context))
                scenario.onActivity { ReaderMushafPreference.select(context, "qaloon") }
                waitFor("1 / 604")
                assertEquals("qaloon", ReaderMushafPreference.selected(context))
                click(context.getString(R.string.ui_settings_c7f73b))
                click(context.getString(R.string.settings_choose_mushaf))
                click(context.getString(R.string.settings_madani_standard))
                waitFor("Hafs test reader")
                assertEquals("madani-standard", ReaderMushafPreference.selected(context))
            }
        } finally {
            val edit = selection.edit()
            if (selectedBefore == null) edit.remove(ReaderMushafPreference.KEY) else edit.putString(ReaderMushafPreference.KEY, selectedBefore)
            edit.commit()
            reader.edit().putInt("warsh", warshBefore).putInt("qaloon", qaloonBefore).putInt("scroll_speed", speedBefore).commit()
            settings.edit().putBoolean("is_dark_theme", darkBefore).commit()
            isDarkThemeGlobal = globalBefore
        }
    }
    @Test fun normalSurahEntryUsesTheSavedNativeEdition() {
        val context = instrumentation.targetContext
        val selection = ReaderMushafPreference.preferences(context)
        val before = selection.getString(ReaderMushafPreference.KEY, null)
        val reader = context.getSharedPreferences("NarrationReader", 0)
        val pageBefore = reader.getInt("warsh", 1)
        reader.edit().putInt("warsh", 1).commit()
        ReaderMushafPreference.select(context, "warsh")
        try {
            val intent = android.content.Intent(context, MainActivity::class.java).putExtra("chapterId", 2)
            ActivityScenario.launch<MainActivity>(intent).use {
                waitFor("2 / 604")
                assertEquals("warsh", ReaderMushafPreference.selected(context))
                assertEquals(2, reader.getInt("warsh", 0))
            }
        } finally {
            val edit = selection.edit()
            if (before == null) edit.remove(ReaderMushafPreference.KEY) else edit.putString(ReaderMushafPreference.KEY, before)
            edit.commit()
            reader.edit().putInt("warsh", pageBefore).commit()
        }
    }

    @Test fun explicitHafsVerseKeepsItsOwnerWithoutResettingTheDefault() {
        val context = instrumentation.targetContext
        val prefs = ReaderMushafPreference.preferences(context)
        val before = prefs.getString(ReaderMushafPreference.KEY, null)
        ReaderMushafPreference.select(context, "warsh")
        try {
            ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
                scenario.onActivity { activity -> activity.setContent { MaterialTheme {
                    PreferredMushafReader(onDismiss = {}, onSelectHafs = {}, hafsVerseKey = "2:255") {
                        Text("Exact Hafs verse 2:255")
                    }
                } } }
                waitFor("Exact Hafs verse 2:255")
                assertEquals("warsh", ReaderMushafPreference.selected(context))
            }
        } finally {
            val edit = prefs.edit()
            if (before == null) edit.remove(ReaderMushafPreference.KEY) else edit.putString(ReaderMushafPreference.KEY, before)
            edit.commit()
        }
    }

    @Test fun pageAnchorDoesNotOverrideTheReadingPositionAfterRecreation() {
        val context = instrumentation.targetContext
        val prefs = ReaderMushafPreference.preferences(context)
        val before = prefs.getString(ReaderMushafPreference.KEY, null)
        val reader = context.getSharedPreferences("NarrationReader", 0)
        val pageBefore = reader.getInt("warsh", 1)
        ReaderMushafPreference.select(context, "warsh")
        fun render(activity: ComponentActivity) { activity.setContent { MaterialTheme {
            PreferredMushafReader(onDismiss = {}, onSelectHafs = {}, initialPage = 2) { Text("Hafs test reader") }
        } } }
        try {
            ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
                scenario.onActivity(::render)
                waitFor("2 / 604")
                swipeMushafPage(right = true)
                waitFor("3 / 604")
                scenario.recreate()
                scenario.onActivity(::render)
                waitFor("3 / 604")
                assertEquals(3, reader.getInt("warsh", 0))
            }
        } finally {
            val edit = prefs.edit()
            if (before == null) edit.remove(ReaderMushafPreference.KEY) else edit.putString(ReaderMushafPreference.KEY, before)
            edit.commit()
            reader.edit().putInt("warsh", pageBefore).commit()
        }
    }

}
