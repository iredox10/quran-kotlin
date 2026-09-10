package com.nur.quran

import android.graphics.BitmapFactory
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nur.quran.ui.components.renderVerseCardToFile
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Proves the off-screen share-card renderer produces a real PNG.
 * Regression test for blank/1px captures caused by measuring before
 * composition settles (ComposeView.setContent is asynchronous).
 */
@RunWith(AndroidJUnit4::class)
class ShareImageRenderTest {

    @Test
    fun renderVerseCard_producesNonBlankPng(): Unit = runBlocking {
        // Renderer needs an Activity window (off-screen ComposeViews have no
        // recomposer) — same as the real dialog path in MainActivity.
        val scenario = ActivityScenario.launch(ComponentActivity::class.java)
        var file: java.io.File? = null
        scenario.onActivity { activity ->
            file = runBlocking {
                renderVerseCardToFile(
                    context = activity,
                    verseKey = "2:255",
                    arabic = "اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ",
                    translation = "Allah! There is no god but He, the Living, the Self-subsisting, Eternal.",
                    reference = "Al-Baqarah 2:255"
                )
            }
        }
        val out = file
        assertNotNull("render returned null", out)
        requireNotNull(out)
        assertTrue("file missing", out.exists())
        assertTrue("file suspiciously small: ${out.length()}", out.length() > 10_000)
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(out.absolutePath, opts)
        assertTrue("width wrong: ${opts.outWidth}", opts.outWidth == 1080)
        assertTrue("height too small (blank capture?): ${opts.outHeight}", opts.outHeight > 100)
    }
}
