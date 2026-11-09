package com.nur.quran

import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nur.quran.data.planner.*
import com.nur.quran.ui.components.planner.PlannerHistoryScreen
import androidx.compose.runtime.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Uses platform accessibility because the emulator API is newer than Espresso's input driver. */
@RunWith(AndroidJUnit4::class)
class PlannerHistoryUiTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private fun find(label: String, requireClickable: Boolean = false): AccessibilityNodeInfo? {
        fun visit(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
            if (node == null) return null
            if (node.text?.toString()?.lineSequence()?.any { it == label } == true || node.contentDescription?.toString() == label) {
                var action:AccessibilityNodeInfo?=node
                while(action!=null && !action.isClickable) action=action.parent
                if(!requireClickable || action!=null) return node
            }
            for (index in 0 until node.childCount) visit(node.getChild(index))?.let { return it }
            return null
        }
        return visit(instrumentation.uiAutomation.rootInActiveWindow)
    }
    private fun waitFor(label: String, requireClickable: Boolean = false): AccessibilityNodeInfo {
        val until = System.currentTimeMillis() + 10000
        while (System.currentTimeMillis() < until) {
            find(label,requireClickable)?.let { return it }
            Thread.sleep(100)
        }
        error("History text not found: $label")
    }
    private fun click(label: String) {
        var node: AccessibilityNodeInfo? = waitFor(label,true)
        while (node != null && !node.isClickable) node = node.parent
        assertTrue("History control must accept clicks: $label", node?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true)
    }
    private fun scrollTo(label: String) {
        repeat(12) {
            if (find(label) != null) return
            // Scroll the content vertically; the overview also contains a horizontal chart.
            val metrics=instrumentation.targetContext.resources.displayMetrics
            val x=metrics.widthPixels/2
            val from=(metrics.heightPixels*.85f).toInt()
            val to=(metrics.heightPixels*.55f).toInt()
            val descriptor=instrumentation.uiAutomation.executeShellCommand("input swipe $x $from $x $to 250")
            android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { it.readBytes() }
            Thread.sleep(250)
        }
        waitFor(label)
    }
    private fun screenshot(name:String) {
        instrumentation.waitForIdleSync()
        Thread.sleep(300)
        val bitmap=instrumentation.uiAutomation.takeScreenshot()
        java.io.File(instrumentation.targetContext.cacheDir,name).outputStream().use {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)
        }
    }
    @Test fun completedHistoryShowsDailyTimeAccumulatedTimeAndReflections() {
        val built = PlannerEngine.buildReadingPlanner("page", 2, "2026-10-01", 1, 4, chapters = emptyList())
        val completed = built.copy(title = "A little every day", assignmentProgress = mapOf(1 to 2, 2 to 2), assignmentCompletedAt = mapOf(1 to "2026-10-01", 2 to "2026-10-03"))
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            scenario.onActivity { activity -> activity.setContent {
                MaterialTheme {
                    var selected by remember { mutableStateOf<String?>(null) }
                    PlannerHistoryScreen(
                    listOf(completed), mapOf(completed.id to mapOf(
                        1 to PlannerSessionTimer(1800, secondsByDate = mapOf("2026-10-01" to 1800)),
                        2 to PlannerSessionTimer(3600, secondsByDate = mapOf("2026-10-03" to 3600))
                    )), mapOf(completed.id to mapOf(1 to "Slow down and reflect.")),
                    mapOf(completed.id to listOf(PlannerBookmark("1:1","Al-Fatihah","A saved lesson."))),
                    onBack={selected=null},onOpenPlan={selected=it},selectedPlanId=selected,
                    loadVerses={mapOf("1:1" to com.nur.quran.data.db.entities.VerseEntity(1,1,1,"1:1","بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",null,null,1,1,"In the name of Allah"))}
                ) }
            } }
            waitFor("A little every day")
            screenshot("planner-history-list-native.png")
            click("A little every day")
            waitFor("Total recorded reading time")
            waitFor("1h 30m")
            screenshot("planner-history-native.png")
            click("Accumulated")
            screenshot("planner-history-cumulative-native.png")
            click("Daily")
            scrollTo("1h 30m accumulated")
            click("Reflections")
            waitFor("Slow down and reflect.")
            click("Highlights")
            waitFor("A saved lesson.")
            waitFor("Al-Fatihah · 1:1")
            click("Back to planner history")
            waitFor("Your reading journey")
        }
    }
    @Test fun emptyHistoryExplainsHowPlansAppear() {
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            scenario.onActivity { activity -> activity.setContent { MaterialTheme {
                PlannerHistoryScreen(emptyList(), emptyMap(), emptyMap(), emptyMap(), {}, {})
            } } }
            waitFor("No completed plans yet")
            click("Completed")
            waitFor("No completed plans yet")
        }
    }
    @Test fun longReflectionsAndLongQuranHighlightsRemainReadableInDarkMode() {
        val context=instrumentation.targetContext
        val note="I returned to these verses and wrote what I learned. ".repeat(35)+"A closing thought."
        val built=PlannerEngine.buildReadingPlanner("page",2,"2026-10-01",1,4,chapters=emptyList())
        val plan=built.copy(title="A completed journey",assignmentProgress=mapOf(1 to 2,2 to 2))
        val text=context.assets.open("data/quran_full.json").bufferedReader().use {
            com.google.gson.JsonParser.parseReader(it).asJsonArray.first { row -> row.asJsonObject.get("verse_key").asString=="2:282" }.asJsonObject.get("text_uthmani").asString
        }
        val bundled=context.assets.open("data/words/2.json").bufferedReader().use {
            com.nur.quran.data.api.rn.RnContent.parseWordsPack(com.google.gson.JsonParser.parseReader(it).asJsonObject).verses.first { verse -> verse.verse_key=="2:282" }
        }
        val base=com.nur.quran.data.db.entities.VerseEntity(1,2,282,"2:282",text,null,null,48,3,"A translation that stays readable. ".repeat(20))
        val historyVerse=com.nur.quran.data.repository.withBundledQuranScript(base,bundled)!!.first
        var oldDark=false
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            try {
                scenario.onActivity { activity ->
                    oldDark=com.nur.quran.ui.screens.isDarkThemeGlobal
                    com.nur.quran.ui.screens.isDarkThemeGlobal=true
                    activity.setContent { MaterialTheme {
                        PlannerHistoryScreen(listOf(plan),emptyMap(),mapOf(plan.id to mapOf(1 to note,2 to "A second day’s reflection.")),
                            mapOf(plan.id to listOf(PlannerBookmark("2:282","Al-Baqarah","Keep the whole ayah readable."))),{},{},plan.id,
                            loadVerses={mapOf("2:282" to historyVerse)})
                    } }
                }
                click("Reflections")
                waitFor(note)
                screenshot("planner-history-reflections-dark.png")
                scrollTo("A second day’s reflection.")
                click("Highlights")
                waitFor("Al-Baqarah · 2:282")
                screenshot("planner-history-highlight-dark.png")
                scrollTo("Keep the whole ayah readable.")
                scrollTo("Go to Ayah")
            } finally { scenario.onActivity { com.nur.quran.ui.screens.isDarkThemeGlobal=oldDark } }
        }
    }

}
