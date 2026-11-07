package com.nur.quran

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.core.view.WindowCompat
import com.nur.quran.ui.screens.isDarkThemeGlobal
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReaderSystemBarThemeTest {
    @Suppress("DEPRECATION")
    private fun awaitColor(scenario: ActivityScenario<MainActivity>, expected: Int) {
        repeat(100) {
            var matches = false
            scenario.onActivity { matches = it.window.statusBarColor == expected }
            if (matches) return
            Thread.sleep(20)
        }
        fail("System bar did not follow the reader theme")
    }
    @Suppress("DEPRECATION")
    @Test fun themeChangesReplaceTheGreenHeaderAndKeepSystemIconsLegible() {
        val before = isDarkThemeGlobal
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            try {
                scenario.onActivity { isDarkThemeGlobal = true }
                InstrumentationRegistry.getInstrumentation().waitForIdleSync()
                awaitColor(scenario, 0xFF101415.toInt())
                scenario.onActivity { activity ->
                    assertEquals(0xFF101415.toInt(), activity.window.statusBarColor)
                    assertEquals(0xFF101415.toInt(), activity.window.navigationBarColor)
                    assertFalse(WindowCompat.getInsetsController(activity.window, activity.window.decorView).isAppearanceLightStatusBars)
                }
                Thread.sleep(1800) // Let the launch cover finish before taking the visual reference.
                val dark = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
                java.io.File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "neutral-dark-header.png").outputStream().use {
                    dark.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
                }
                scenario.onActivity { isDarkThemeGlobal = false }
                InstrumentationRegistry.getInstrumentation().waitForIdleSync()
                awaitColor(scenario, 0xFFFAF7F0.toInt())
                scenario.onActivity { activity ->
                    assertEquals(0xFFFAF7F0.toInt(), activity.window.statusBarColor)
                    assertTrue(WindowCompat.getInsetsController(activity.window, activity.window.decorView).isAppearanceLightStatusBars)
                }
            } finally { scenario.onActivity { isDarkThemeGlobal = before } }
        }
    }
}
