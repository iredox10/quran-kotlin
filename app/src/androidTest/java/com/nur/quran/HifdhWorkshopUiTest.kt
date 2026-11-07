package com.nur.quran

import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nur.quran.data.db.entities.ChapterEntity
import com.nur.quran.ui.components.HifdhWorkshop
import com.nur.quran.ui.viewmodels.SurahViewModel
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HifdhWorkshopUiTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private fun find(label: String): AccessibilityNodeInfo? {
        fun visit(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
            if (node == null) return null
            if (node.text?.toString()?.startsWith(label, ignoreCase = true) == true || node.contentDescription?.toString() == label) return node
            for (i in 0 until node.childCount) visit(node.getChild(i))?.let { return it }
            return null
        }
        return visit(instrumentation.uiAutomation.rootInActiveWindow)
    }
    private fun waitFor(label: String): AccessibilityNodeInfo {
        repeat(150) { find(label)?.let { return it }; Thread.sleep(100) }
        error("Workshop content missing: $label")
    }
    private fun click(label: String) {
        var node: AccessibilityNodeInfo? = waitFor(label)
        while (node != null && !node.isClickable) node = node.parent
        assertTrue(node?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true)
        Thread.sleep(400)
    }
    @Test fun workshopOpensBudgetPracticeAndHistoryWithoutChangingReadingEdition() {
        val context = instrumentation.targetContext
        val prefs = context.getSharedPreferences("hifdh_workshop", 0)
        val original = prefs.all
        prefs.edit().putInt("chapter",1).putInt("minutes",20).commit()
        var originalMushaf: String? = null
        lateinit var model: SurahViewModel
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                scenario.onActivity { activity ->
                    model = ViewModelProvider(activity)[SurahViewModel::class.java]
                    originalMushaf = model.currentMushaf.value.id
                    activity.setContent { MaterialTheme {
                        HifdhWorkshop(model, listOf(ChapterEntity(1,"Al-Fatihah","الفاتحة","Al-Fatihah",revelationPlace="makkah",revelationOrder=5,versesCount=7,pagesStart=1,pagesEnd=1)), {})
                    } }
                }
                waitFor("Start today")
                waitFor("20 Minutes")
                click("Practice")
                waitFor("Independent recall")
                click("Start Memorizing 🧠")
                waitFor("1:1")
                waitFor("Test My Hifdh")
                Thread.sleep(800)
                instrumentation.uiAutomation.takeScreenshot().let { bitmap ->
                    java.io.File(context.cacheDir, "hifdh-workshop-native.png").outputStream().use {
                        bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)
                    }
                }
                click("Back")
                var tabs: AccessibilityNodeInfo? = waitFor("Practice")
                while (tabs != null && !tabs.isScrollable) tabs = tabs.parent
                tabs?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                Thread.sleep(400)
                click("History")
                waitFor("Self-declared; earlier tests unknown:")
                assertEquals("madani-standard", prefs.getString("pinned_edition",null))
                assertEquals(originalMushaf, model.currentMushaf.value.id)
            }
        } finally {
            prefs.edit().clear().apply()
            val editor = prefs.edit()
            original.forEach { (key,value) -> when(value) { is Int -> editor.putInt(key,value); is String -> editor.putString(key,value) } }
            editor.commit()
        }
    }
}
