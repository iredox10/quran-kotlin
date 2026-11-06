package com.nur.quran.ui.components

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nur.quran.ui.components.audio.MiniPlayer
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlannerMiniPlayerLayoutTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private fun find(predicate: (AccessibilityNodeInfo) -> Boolean, node: AccessibilityNodeInfo? = instrumentation.uiAutomation.rootInActiveWindow): AccessibilityNodeInfo? {
        node ?: return null
        if (predicate(node)) return node
        for (i in 0 until node.childCount) find(predicate, node.getChild(i))?.let { return it }
        return null
    }
    @Test fun inlinePlayerLeavesQuranVisibleDuringPlaybackAndPause() {
        var playing by mutableStateOf(true)
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            scenario.onActivity { activity -> activity.setContent {
                MaterialTheme {
                    Column(Modifier.fillMaxSize()) {
                        LazyColumn(Modifier.weight(1f)) {
                            items(30) { Text("Verse $it", Modifier.height(64.dp)) }
                        }
                        MiniPlayer(verseKey = "2:1", isPlaying = playing, overlay = false,
                            onPlayPause = { playing = !playing }, onPrevious = {}, onNext = {}, onClose = {})
                    }
                }
            } }
            repeat(50) {
                if (find({ it.isScrollable }) == null) Thread.sleep(100)
            }
            val viewport = Rect()
            (find({ it.isScrollable }) ?: error("Quran reader disappeared")).getBoundsInScreen(viewport)
            assertTrue("Player must leave most of the screen for Quran", viewport.height() > instrumentation.targetContext.resources.displayMetrics.heightPixels / 2)
            assertNotNull(find({ it.text?.toString() == "Verse 0" && it.isVisibleToUser }))
            var pause = find({ it.contentDescription?.toString() == "Pause" }) ?: error("Missing pause control")
            while (!pause.isClickable) pause = pause.parent ?: error("Pause is not clickable")
            assertTrue(pause.performAction(AccessibilityNodeInfo.ACTION_CLICK))
            instrumentation.waitForIdleSync()
            assertFalse(playing)
            assertNotNull(find({ it.text?.toString() == "Verse 0" && it.isVisibleToUser }))
        }
    }
}
