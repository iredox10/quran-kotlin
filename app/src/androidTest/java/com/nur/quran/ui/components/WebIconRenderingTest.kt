package com.nur.quran.ui.components

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.Gson
import com.google.gson.JsonParser
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WebIconRenderingTest {
    @Test fun renderEveryWebIconForSvgComparison() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val fixture = instrumentation.context.assets.open("lucide-web-icons.json").bufferedReader().use {
            JsonParser.parseReader(it).asJsonObject
        }
        val names = fixture.getAsJsonArray("icons").map { it.asJsonObject.get("name").asString }
        val bounds = ConcurrentHashMap<String, List<Int>>()
        val ready = CountDownLatch(1)
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            scenario.onActivity { activity -> activity.setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f), LocalLayoutDirection provides LayoutDirection.Ltr) {
                    MaterialTheme { Surface(color = Color.White, modifier = Modifier.fillMaxSize()) {
                        Column(Modifier.padding(16.dp)) {
                            names.chunked(8).forEach { row -> Row {
                                row.forEach { name -> Column(Modifier.size(112.dp, 104.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    val vector = NurIcons::class.java.getMethod("get$name").invoke(NurIcons) as ImageVector
                                    Icon(vector, name, tint = Color.Black, modifier = Modifier.size(72.dp).onGloballyPositioned {
                                        val r = it.boundsInWindow()
                                        bounds[name] = listOf(r.left.roundToInt(), r.top.roundToInt(), r.right.roundToInt(), r.bottom.roundToInt())
                                        if (bounds.size == names.size) ready.countDown()
                                    })
                                    Text(name, color = Color.Black, fontSize = 10.sp, maxLines = 1)
                                } }
                            } }
                        }
                    } }
                }
            } }
            assertTrue("Every SVG must parse and lay out", ready.await(15, TimeUnit.SECONDS))
            instrumentation.waitForIdleSync()
            val shot = instrumentation.uiAutomation.takeScreenshot()
            File(context.cacheDir, "lucide-native-grid.png").outputStream().use {
                shot.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            File(context.cacheDir, "lucide-native-bounds.json").writeText(Gson().toJson(bounds))
            assertEquals(names.toSet(), bounds.keys)
            assertTrue(bounds.values.all { it[2] - it[0] == 72 && it[3] - it[1] == 72 })
        }
    }
}
