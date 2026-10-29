package com.nur.quran.ui.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Path
import android.graphics.Region
import android.text.Layout
import android.widget.TextView

/** Paints colors over one continuous layout, without reshaping Arabic at color boundaries. */
internal class ShapedTajweedTextView(context: Context) : TextView(context) {
    var colorSegments: List<TajweedSegment> = emptyList()
        set(value) {
            if (field == value) return
            field = value
            cachedLayout = null
            invalidate()
        }

    private var cachedLayout: Layout? = null
    private var coloredPaths: List<Pair<Int, Path>> = emptyList()
    private val allColored = Path()

    private fun preparePaths(layout: Layout) {
        if (cachedLayout === layout) return
        val paths = linkedMapOf<Int, Path>()
        val rangePath = Path()
        allColored.reset()
        for (segment in colorSegments) {
            val start = segment.start.coerceIn(0, text.length)
            val end = segment.end.coerceIn(start, text.length)
            if (start == end) continue
            val color = runCatching {
                Color.parseColor(if (segment.ruleClass == "end") "#B8924A" else segment.colorHex)
            }.getOrDefault(currentTextColor)
            layout.getSelectionPath(start, end, rangePath)
            paths.getOrPut(color) { Path() }.addPath(rangePath)
            allColored.addPath(rangePath)
        }
        // Integer pixel masks partition the glyph image without fractional clip seams.
        val bounds = Region(-1_000_000, -1_000_000, 1_000_000, 1_000_000)
        val covered = Region()
        coloredPaths = paths.entries.reversed().map { (color, path) ->
            val region = Region().apply {
                setPath(path, bounds)
                op(covered, Region.Op.DIFFERENCE)
            }
            covered.op(region, Region.Op.UNION)
            color to region.boundaryPath
        }
        allColored.set(covered.boundaryPath)
        cachedLayout = layout
    }

    override fun onDraw(canvas: Canvas) {
        val shaped = layout ?: return
        if (colorSegments.isEmpty()) {
            super.onDraw(canvas)
            return
        }
        preparePaths(shaped)
        val originalColor = shaped.paint.color
        val origin = canvas.save()
        canvas.translate((totalPaddingLeft - scrollX).toFloat(), (totalPaddingTop - scrollY).toFloat())
        try {
            // Uncolored areas and colored areas are disjoint: antialiased glyph edges draw once.
            val base = canvas.save()
            @Suppress("DEPRECATION")
            canvas.clipPath(allColored, Region.Op.DIFFERENCE)
            shaped.paint.color = currentTextColor
            shaped.draw(canvas)
            canvas.restoreToCount(base)
            for ((color, path) in coloredPaths) {
                val layer = canvas.save()
                canvas.clipPath(path)
                shaped.paint.color = color
                shaped.draw(canvas)
                canvas.restoreToCount(layer)
            }
        } finally {
            shaped.paint.color = originalColor
            canvas.restoreToCount(origin)
        }
    }
}
