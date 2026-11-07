package com.nur.quran

import android.content.Context
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nur.quran.data.mushaf.IndopakEdition
import com.nur.quran.ui.components.IndopakMushafReader
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IndopakMushafUiTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private fun find(label: String, node: AccessibilityNodeInfo? = instrumentation.uiAutomation.rootInActiveWindow): AccessibilityNodeInfo? {
        node ?: return null
        if (node.isVisibleToUser && (node.text?.toString() == label || node.contentDescription?.toString() == label)) return node
        for (i in 0 until node.childCount) find(label, node.getChild(i))?.let { return it }
        return null
    }
    private fun waitFor(label: String): AccessibilityNodeInfo {
        repeat(150) { find(label)?.let { return it }; Thread.sleep(100) }
        error("Missing $label")
    }
    private fun click(label: String) {
        repeat(8) {
            if (find(label) == null) {
                fun scroll(node: AccessibilityNodeInfo?): Boolean {
                    node ?: return false
                    if (node.isScrollable && node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)) return true
                    for (i in 0 until node.childCount) if (scroll(node.getChild(i))) return true
                    return false
                }
                scroll(instrumentation.uiAutomation.rootInActiveWindow)
                Thread.sleep(150)
            }
        }
        var node = waitFor(label)
        while (!node.isClickable) node = node.parent ?: error("Not clickable: $label")
        assertTrue(node.performAction(AccessibilityNodeInfo.ACTION_CLICK))
        instrumentation.waitForIdleSync()
    }
    @Test fun printedEditionOpensAndNavigatesFromLocalStorage(): Unit = runBlocking {
        val context = instrumentation.targetContext
        // Network check is explicit and optional. Supply a partial original PDF before running.
        if (InstrumentationRegistry.getArguments().getString("indopakDownload") == "true") {
            IndopakEdition.download(context) { assertTrue(it in 0f..1f) }
        }
        assumeTrue("Install the original edition to run the printed-page test", IndopakEdition.installed(context))
        val prefs = context.getSharedPreferences("NarrationReader", Context.MODE_PRIVATE)
        val saved = if (prefs.contains("indopak")) prefs.getInt("indopak", 2) else null
        prefs.edit().putInt("indopak", 2).commit()
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                scenario.onActivity { it.setContent { MaterialTheme { IndopakMushafReader {} } } }
                waitFor("IndoPak · 2")
                assertNull(find(context.getString(R.string.ui_next_0fed81)))
                assertNull(find(context.getString(R.string.ui_previous_f67a76)))
                swipeMushafPage(right = true)
                waitFor("IndoPak · 3")
                repeat(100) { if (prefs.getInt("indopak", 0) != 3) Thread.sleep(50) }
                assertEquals(3, prefs.getInt("indopak", 0))
                click(context.getString(R.string.ui_settings_c7f73b))
                waitFor(context.getString(R.string.settings_general))
                click(context.getString(R.string.settings_reading))
                click("A+")
                click(context.getString(R.string.settings_close))
                swipeMushafPage(right = true) // A zoomed page pans instead of turning.
                assertEquals(3, prefs.getInt("indopak", 0))
                click(context.getString(R.string.ui_settings_c7f73b))
                click(context.getString(R.string.settings_reading))
                click("A−")
                click(context.getString(R.string.settings_close))
                swipeMushafPage(right = false)
                waitFor("IndoPak · 2")
                assertEquals(2, prefs.getInt("indopak", 0))
                swipeMushafPage(right = false) // First page is bounded.
                assertEquals(2, prefs.getInt("indopak", 0))
                swipeMushafPage(right = true)
                waitFor("IndoPak · 3")
                instrumentation.waitForIdleSync()
                val shot = instrumentation.uiAutomation.takeScreenshot()
                java.io.File(context.cacheDir, "indopak-printed-reader.png").outputStream().use {
                    shot.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
                }
            }
        } finally {
            if (saved == null) prefs.edit().remove("indopak").commit()
            else prefs.edit().putInt("indopak", saved).commit()
        }
    }
}
