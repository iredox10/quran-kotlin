package com.nur.quran.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nur.quran.R
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TajweedGlyphShapeTest {
    @Test fun colorBoundariesPreserveLamAlifAndWrappedArabicGlyphs() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val examples = listOf("لَا", "لَأُحِبُّ", "قَالَ لَا أُحِبُّ ٱلۡأٓفِلِينَ", "فَلَمَّا جَنَّ عَلَيْهِ ٱلَّيْلُ رَأَىٰ كَوْكَبًا قَالَ هَٰذَا رَبِّي قَالَ لَا أُحِبُّ ٱلْآفِلِينَ")
        for (font in listOf(R.font.kfgqpc_hafs, R.font.uthman_taha_naskh, R.font.amiri_regular)) {
            for (text in examples) {
                lateinit var plain: Bitmap
                lateinit var colored: Bitmap
                lateinit var white: Bitmap
                instrumentation.runOnMainSync {
                    fun setup(view: TextView) = view.apply {
                        typeface = ResourcesCompat.getFont(context, font)
                        textSize = 38f
                        includeFontPadding = false
                        textDirection = View.TEXT_DIRECTION_RTL
                        layoutDirection = View.LAYOUT_DIRECTION_RTL
                        gravity = Gravity.RIGHT
                        breakStrategy = android.text.Layout.BREAK_STRATEGY_SIMPLE
                        setTextColor(Color.WHITE)
                        this.text = text
                    }
                    fun bitmap(view: TextView): Bitmap {
                        view.measure(View.MeasureSpec.makeMeasureSpec(900, View.MeasureSpec.EXACTLY),
                            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
                        view.layout(0,0,view.measuredWidth,view.measuredHeight)
                        return Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888).also { view.draw(Canvas(it)) }
                    }
                    plain = bitmap(setup(TextView(context)))
                    val coloredView = setup(ShapedTajweedTextView(context)) as ShapedTajweedTextView
                    // Boundaries at every codepoint stress ligatures and combining marks.
                    coloredView.colorSegments = text.indices.map { i -> TajweedSegment(i,i+1,if(i%2==0) "#00BBFF" else "#FFFFFF") }
                    colored = bitmap(coloredView)
                    coloredView.colorSegments = coloredView.colorSegments.map { it.copy(colorHex = "#FFFFFF") }
                    white = bitmap(coloredView)
                }
                assertEquals(plain.height, colored.height)
                fun pixels(bitmap: Bitmap) = IntArray(bitmap.width * bitmap.height).also {
                    bitmap.getPixels(it,0,bitmap.width,0,0,bitmap.width,bitmap.height)
                }
                val p = pixels(plain)
                val c = pixels(colored)
                var ink = 0
                var coloredInk = 0
                var differentShapePixels = 0
                for (i in p.indices) {
                    if ((Color.alpha(p[i]) > 0) != (Color.alpha(c[i]) > 0)) differentShapePixels++
                    if (Color.alpha(c[i])>0) {
                        ink++
                        if (Color.red(c[i])<100 && Color.blue(c[i])>100) coloredInk++
                    }
                }
                assertTrue("No glyphs drawn", ink>0)
                assertTrue("Tajweed colors missing",coloredInk>0)
                // Skia adjusts edge coverage for color luminance; compare silhouettes.
                assertTrue("Glyph silhouette changed for font=$font text=$text: $differentShapePixels / $ink",
                    differentShapePixels <= maxOf(3, ink / 200))
                assertArrayEquals("Span boundaries changed glyphs for font=$font text=$text",p,pixels(white))
                plain.recycle()
                colored.recycle()
                white.recycle()
            }
        }
    }
}
