package com.nur.quran.ui.components

import android.text.Editable
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Prevent timer/playback callbacks from rebuilding Quran text while the reader scrolls. */
@RunWith(AndroidJUnit4::class)
class ReaderRenderingUpdatesTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private fun awaitComposition(version: java.util.concurrent.atomic.AtomicInteger, expected: Int) {
        val deadline = android.os.SystemClock.uptimeMillis() + 10_000
        while (version.get() != expected && android.os.SystemClock.uptimeMillis() < deadline) Thread.sleep(10)
        assertEquals("Compose frame did not finish", expected, version.get())
        instrumentation.waitForIdleSync()
    }
    private fun find(view: View): ShapedTajweedTextView? {
        if (view is ShapedTajweedTextView) return view
        if (view is ViewGroup) for (i in 0 until view.childCount) find(view.getChildAt(i))?.let { return it }
        return null
    }
    @Test fun repeatedCallbackAndColorUpdatesKeepTheSameTextLayoutAndFreshTapAction() {
        val tick = mutableIntStateOf(0)
        val rendered = java.util.concurrent.atomic.AtomicInteger(-1)
        var tapped = -1
        val text = List(8) { "ٱلْحَمْدُ لِلَّهِ رَبِّ ٱلْعَٰلَمِينَ" }.joinToString(" ")
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            scenario.onActivity { activity -> activity.setContent {
                val version = tick.intValue
                TajweedAndroidText(text, listOf((0 until text.length) to 1), emptyList(),
                    "KFGQPC Hafs", 28f, 1.8f, textColor = if (version % 2 == 0) Color.Black else Color.DarkGray,
                    isInteractive = true, onWordClick = { tapped = version }, onTajweedClick = {},
                    modifier = Modifier.fillMaxWidth())
                SideEffect { rendered.set(version) }
            } }
            awaitComposition(rendered, 0)
            Thread.sleep(300) // Let the bounded wrap correction finish before observing stability.
            lateinit var view: ShapedTajweedTextView
            lateinit var originalText: CharSequence
            lateinit var originalLayout: android.text.Layout
            var changes = 0
            scenario.onActivity {
                view = find(it.window.decorView)!!
                originalText = view.text
                originalLayout = view.layout
                view.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { changes++ }
                    override fun afterTextChanged(s: Editable?) = Unit
                })
            }
            repeat(30) { index ->
                instrumentation.runOnMainSync { tick.intValue = index + 1 }
                awaitComposition(rendered, index + 1)
            }
            scenario.onActivity {
                assertEquals("Unrelated updates must not assign Quran text again", 0, changes)
                assertSame(originalText, view.text)
                assertSame("Playback/color updates must preserve the shaped layout", originalLayout, view.layout)
                val layout = view.layout
                val x = (layout.getPrimaryHorizontal(2) + layout.getPrimaryHorizontal(3)) / 2 + view.totalPaddingLeft
                val y = (layout.getLineTop(0) + layout.getLineBottom(0)) / 2f + view.totalPaddingTop
                val now = android.os.SystemClock.uptimeMillis()
                listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP).forEach { action ->
                    MotionEvent.obtain(now, now + 20, action, x, y, 0).also { event ->
                        view.dispatchTouchEvent(event); event.recycle()
                    }
                }
                assertEquals("A cached span must invoke the latest callback", 30, tapped)
            }
        }
    }

    @Test fun fontSizeAndTextChangesStillReflowAndInteractiveModeUpdates() {
        val large = mutableStateOf(false)
        val rendered = java.util.concurrent.atomic.AtomicInteger(-1)
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            scenario.onActivity { activity -> activity.setContent {
                val bigger = large.value
                TajweedAndroidText(if (bigger) "ٱلْحَمْدُ لِلَّهِ رَبِّ ٱلْعَٰلَمِينَ" else "ٱلْحَمْدُ لِلَّهِ",
                    emptyList(), emptyList(), "KFGQPC Hafs", if (bigger) 36f else 24f, 1.8f,
                    textColor = Color.Black, isInteractive = !bigger, onWordClick = {}, onTajweedClick = {},
                    modifier = Modifier.fillMaxWidth())
                SideEffect { rendered.set(if (bigger) 1 else 0) }
            } }
            awaitComposition(rendered, 0)
            var originalSize = 0f
            scenario.onActivity { originalSize = find(it.window.decorView)!!.textSize }
            instrumentation.runOnMainSync { large.value = true }
            awaitComposition(rendered, 1)
            scenario.onActivity {
                val view = find(it.window.decorView)!!
                assertTrue(view.textSize > originalSize)
                assertTrue(view.text.contains("رَبِّ"))
                assertFalse(view.appliedInteractive!!)
                assertNotNull(view.layout)
            }
        }
    }
}
