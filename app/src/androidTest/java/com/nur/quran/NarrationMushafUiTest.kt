package com.nur.quran

import android.content.Context
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nur.quran.data.mushaf.NarrationMushaf
import com.nur.quran.ui.components.NarrationMushafReader
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NarrationMushafUiTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private fun find(label: String, node: AccessibilityNodeInfo? = instrumentation.uiAutomation.rootInActiveWindow): AccessibilityNodeInfo? {
        node ?: return null
        if (node.isVisibleToUser && (node.text?.toString()?.contains(label) == true || node.contentDescription?.toString() == label)) return node
        for (i in 0 until node.childCount) find(label, node.getChild(i))?.let { return it }
        return null
    }
    private fun waitFor(label: String): AccessibilityNodeInfo {
        repeat(100) { find(label)?.let { return it }; Thread.sleep(100) }
        error("Missing $label")
    }
    private fun click(label: String) {
        var node = waitFor(label)
        while (!node.isClickable) node = node.parent ?: error("$label has no action")
        assertTrue(node.performAction(AccessibilityNodeInfo.ACTION_CLICK))
        instrumentation.waitForIdleSync()
    }
    @Test fun bothOfflineReadersNavigateAndRememberTheirOwnPosition() {
        val context = instrumentation.targetContext
        val prefs = context.getSharedPreferences("NarrationReader", Context.MODE_PRIVATE)
        for (edition in NarrationMushaf.entries) {
            val saved = if (prefs.contains(edition.id)) prefs.getInt(edition.id, 1) else null
            prefs.edit().putInt(edition.id, 1).commit()
            try {
                ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
                    scenario.onActivity { it.setContent { MaterialTheme { NarrationMushafReader(edition) {} } } }
                    waitFor(edition.title)
                    waitFor("1 / 604")
                    val first = edition.ayahs(context).first()
                    waitFor(first.text)
                    swipeMushafPage(right = true)
                    waitFor("2 / 604")
                    swipeMushafPage(right = false)
                    waitFor("1 / 604")
                    click(context.getString(R.string.ui_next_0fed81))
                    waitFor("2 / 604")
                    assertEquals(2, prefs.getInt(edition.id, 0))
                    click(context.getString(R.string.ui_previous_f67a76))
                    waitFor("1 / 604")
                    val shot = instrumentation.uiAutomation.takeScreenshot()
                    java.io.File(context.cacheDir, "${edition.id}-native-reader.png").outputStream().use {
                        shot.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
                    }
                }
            } finally {
                if (saved == null) prefs.edit().remove(edition.id).commit()
                else prefs.edit().putInt(edition.id, saved).commit()
            }
        }
    }
}
