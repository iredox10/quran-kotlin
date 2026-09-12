package com.nur.quran.ui.components.analytics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.ui.screens.fontFamilyMono
import com.nur.quran.ui.screens.fontFamilyUi
import com.nur.quran.ui.screens.hBoneDark
import com.nur.quran.ui.screens.hBorderColor
import com.nur.quran.ui.screens.hCream
import com.nur.quran.ui.screens.hGold
import com.nur.quran.ui.screens.hGoldLight
import com.nur.quran.ui.screens.hGoldSoft
import com.nur.quran.ui.screens.hInk
import com.nur.quran.ui.screens.hInkMuted
import com.nur.quran.ui.screens.hWhite
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Interactive smooth curved Activity Flow Line Chart component matching
 * Recharts <Line type="monotone" /> from web Progress.jsx lines 341-356.
 *
 * Visuals & Behavior:
 * - Smooth cubic bezier curve (monotone Hermite spline converted to cubic Bezier)
 * - Gradient stroke: Brush.verticalGradient(listOf(hGold, hGoldLight))
 * - Filled circular dots at each day's coordinate: inner circle hCream, outer stroke hGold (2dp stroke, 4dp radius)
 * - Touch scrubbing gesture detection:
 *     - Detects nearest day index on touch/drag
 *     - Draws vertical dashed cursor line (like strokeDasharray: '4 4')
 *     - Enlarges active dot to radius 6dp with hGold fill and hCream outer ring
 *     - Displays floating CustomTooltip card above the touched point showing day label, date, and "${mins} min" in bold hGold
 * - Bottom X-axis: displays 7 day labels ("Mon", "Tue", etc.) in hInkMuted 11sp
 * - Left Y-axis & reference lines: subtle dotted grid lines with labels ("0m", "15m", "30m", etc.)
 * - Fully robust: handles 0 minutes, empty lists, single items without crashes.
 *
 * @param dailyActivity List of (dayLabel, minutes) pairs (e.g. [("Mon", 15), ("Tue", 30), ...])
 * @param modifier Modifier for the chart container.
 */
@OptIn(ExperimentalTextApi::class)
@Composable
fun AnalyticsFlowChart(
    dailyActivity: List<Pair<String, Int>>,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var tooltipSize by remember { mutableStateOf(IntSize.Zero) }

    // Compute nice Y-axis ticks and maximum value
    val maxMins = remember(dailyActivity) {
        dailyActivity.maxOfOrNull { it.second.coerceAtLeast(0) } ?: 0
    }
    val yTicks = remember(maxMins) { computeYTicks(maxMins) }
    val yMax = remember(yTicks) { yTicks.last().coerceAtLeast(1) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 220.dp)
    ) {
        val totalWidthPx = constraints.maxWidth.toFloat()
        val totalHeightPx = if (constraints.hasBoundedHeight && constraints.maxHeight > 0) {
            constraints.maxHeight.toFloat()
        } else {
            with(density) { 220.dp.toPx() }
        }

        // Layout margins for axes and labels
        val plotLeft = with(density) { 36.dp.toPx() }
        val plotRight = (totalWidthPx - with(density) { 16.dp.toPx() }).coerceAtLeast(plotLeft)
        val plotTop = with(density) { 28.dp.toPx() }
        val plotBottom = (totalHeightPx - with(density) { 30.dp.toPx() }).coerceAtLeast(plotTop)

        val plotWidth = (plotRight - plotLeft).coerceAtLeast(0f)
        val plotHeight = (plotBottom - plotTop).coerceAtLeast(0f)

        // Calculate coordinate points for each day
        val n = dailyActivity.size
        val points = remember(dailyActivity, plotWidth, plotHeight, plotLeft, plotBottom, yMax) {
            if (n == 0) {
                emptyList()
            } else if (n == 1) {
                val x = (plotLeft + plotRight) / 2f
                val mins = dailyActivity[0].second.coerceAtLeast(0)
                val y = plotBottom - (mins.toFloat() / yMax.toFloat()) * plotHeight
                listOf(Offset(x, y))
            } else {
                dailyActivity.mapIndexed { index, (_, mins) ->
                    val x = plotLeft + (index.toFloat() / (n - 1).toFloat()) * plotWidth
                    val clampedMins = mins.coerceAtLeast(0)
                    val y = plotBottom - (clampedMins.toFloat() / yMax.toFloat()) * plotHeight
                    Offset(x, y)
                }
            }
        }

        // Touch scrubbing gesture detector
        val gestureModifier = if (points.isNotEmpty()) {
            Modifier.pointerInput(points) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val initialIdx = findNearestIndex(down.position.x, points)
                    val previouslySelected = selectedIndex
                    selectedIndex = initialIdx

                    var isDrag = false
                    var currentPointer = down
                    val startX = down.position.x
                    val startY = down.position.y

                    while (currentPointer.pressed) {
                        val event = awaitPointerEvent()
                        val nextPointer = event.changes.firstOrNull { it.id == currentPointer.id } ?: break
                        val dx = kotlin.math.abs(nextPointer.position.x - startX)
                        val dy = kotlin.math.abs(nextPointer.position.y - startY)

                        if (!isDrag) {
                            if (dx > viewConfiguration.touchSlop && dx > dy) {
                                isDrag = true
                                nextPointer.consume()
                                selectedIndex = findNearestIndex(nextPointer.position.x, points)
                            } else if (dy > viewConfiguration.touchSlop) {
                                // Vertical scrolling for parent list/screen
                                break
                            }
                        } else {
                            nextPointer.consume()
                            selectedIndex = findNearestIndex(nextPointer.position.x, points)
                        }
                        currentPointer = nextPointer
                    }

                    // Tapping an already selected dot toggles the tooltip off
                    if (!isDrag && previouslySelected == initialIdx) {
                        selectedIndex = null
                    }
                }
            }
        } else {
            Modifier
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(with(density) { totalHeightPx.toDp() })
                .then(gestureModifier)
        ) {
            if (size.width <= 0f || size.height <= 0f) return@Canvas

            // 1. Subtle dotted horizontal reference grid lines & Y-axis labels
            yTicks.forEach { tick ->
                val tickFraction = tick.toFloat() / yMax.toFloat()
                val tickY = plotBottom - tickFraction * plotHeight

                // Dotted reference line
                drawLine(
                    color = hBorderColor.copy(alpha = 0.55f),
                    start = Offset(plotLeft, tickY),
                    end = Offset(plotRight, tickY),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx()), 0f)
                )

                // Left Y-axis label ("0m", "15m", "30m", etc.)
                val tickLayout = textMeasurer.measure(
                    text = "${tick}m",
                    style = TextStyle(
                        fontFamily = fontFamilyMono,
                        fontSize = 10.sp,
                        color = hInkMuted,
                        textAlign = TextAlign.End
                    )
                )
                drawText(
                    textLayoutResult = tickLayout,
                    topLeft = Offset(
                        x = plotLeft - tickLayout.size.width - 6.dp.toPx(),
                        y = tickY - tickLayout.size.height / 2f
                    )
                )
            }

            // 2. Bottom X-axis day labels ("Mon", "Tue", etc.) in hInkMuted 11sp
            points.forEachIndexed { i, pt ->
                if (i in dailyActivity.indices) {
                    val (dayLabel, _) = dailyActivity[i]
                    val isSelected = (i == selectedIndex)
                    val labelLayout = textMeasurer.measure(
                        text = dayLabel,
                        style = TextStyle(
                            fontFamily = fontFamilyUi,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) hGold else hInkMuted,
                            textAlign = TextAlign.Center
                        )
                    )
                    drawText(
                        textLayoutResult = labelLayout,
                        topLeft = Offset(
                            x = pt.x - labelLayout.size.width / 2f,
                            y = plotBottom + 8.dp.toPx()
                        )
                    )
                }
            }

            // 3. Smooth Cubic Bezier Monotone Spline Curve
            if (points.isNotEmpty()) {
                val linePath = buildMonotoneCubicPath(points)

                // Soft subtle gradient area fill below the curve
                if (points.size >= 2) {
                    val fillPath = Path().apply {
                        addPath(linePath)
                        lineTo(points.last().x, plotBottom)
                        lineTo(points.first().x, plotBottom)
                        close()
                    }
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                hGold.copy(alpha = 0.22f),
                                hGoldLight.copy(alpha = 0.05f),
                                Color.Transparent
                            ),
                            startY = plotTop,
                            endY = plotBottom
                        )
                    )
                }

                // Gradient stroke: Brush.verticalGradient(listOf(hGold, hGoldLight))
                drawPath(
                    path = linePath,
                    brush = Brush.verticalGradient(
                        colors = listOf(hGold, hGoldLight),
                        startY = plotTop,
                        endY = plotBottom
                    ),
                    style = Stroke(
                        width = 3.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                // 4. Vertical dashed cursor line when scrubbing
                if (selectedIndex != null && selectedIndex!! in points.indices) {
                    val activePt = points[selectedIndex!!]
                    drawLine(
                        color = hGoldLight.copy(alpha = 0.8f),
                        start = Offset(activePt.x, plotTop),
                        end = Offset(activePt.x, plotBottom),
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()), 0f)
                    )
                }

                // 5. Filled circular dots at each day's coordinate
                points.forEachIndexed { i, pt ->
                    if (i == selectedIndex) {
                        // Enlarged active dot to radius 6dp with hGold fill and hCream outer ring
                        // Subtle halo
                        drawCircle(
                            color = hGoldSoft,
                            radius = 11.dp.toPx(),
                            center = pt
                        )
                        // Outer ring hCream
                        drawCircle(
                            color = hCream,
                            radius = 8.dp.toPx(),
                            center = pt
                        )
                        // Active dot fill hGold (radius 6dp)
                        drawCircle(
                            color = hGold,
                            radius = 6.dp.toPx(),
                            center = pt
                        )
                    } else {
                        // Normal dot: inner circle hCream, outer stroke hGold (strokeWidth 2dp, radius 4dp)
                        drawCircle(
                            color = hCream,
                            radius = 4.dp.toPx(),
                            center = pt
                        )
                        drawCircle(
                            color = hGold,
                            radius = 4.dp.toPx(),
                            center = pt,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }
            }
        }

        // 6. Floating CustomTooltip card above the touched point
        // Mirrors CustomTooltip in Progress.jsx lines 38-56
        AnimatedVisibility(
            visible = selectedIndex != null && selectedIndex!! in points.indices && selectedIndex!! in dailyActivity.indices,
            enter = fadeIn(animationSpec = tween(150)),
            exit = fadeOut(animationSpec = tween(150))
        ) {
            val idx = selectedIndex ?: return@AnimatedVisibility
            if (idx !in points.indices || idx !in dailyActivity.indices) return@AnimatedVisibility

            val activePt = points[idx]
            val (dayLabel, mins) = dailyActivity[idx]

            // Calculate date string for this day index
            val dateText = remember(idx, dailyActivity.size, dayLabel) {
                formatDateForIndex(idx, dailyActivity.size, dayLabel)
            }

            // Calculate clamped tooltip coordinates
            val halfW = tooltipSize.width / 2
            val tooltipXPx = (activePt.x.toInt() - halfW).coerceIn(
                with(density) { 8.dp.roundToPx() },
                (totalWidthPx.toInt() - tooltipSize.width - with(density) { 8.dp.roundToPx() }).coerceAtLeast(0)
            )

            val gapPx = with(density) { 12.dp.roundToPx() }
            val aboveY = (activePt.y - tooltipSize.height - gapPx).toInt()
            val tooltipYPx = if (aboveY >= with(density) { 4.dp.roundToPx() }) {
                aboveY
            } else {
                // If dot is too close to the top, position below the dot
                (activePt.y + gapPx).toInt().coerceAtMost(
                    (totalHeightPx.toInt() - tooltipSize.height - with(density) { 4.dp.roundToPx() }).coerceAtLeast(0)
                )
            }

            CustomTooltipCard(
                dayLabel = dayLabel,
                dateText = dateText,
                minutes = mins,
                modifier = Modifier
                    .offset { IntOffset(tooltipXPx, tooltipYPx) }
                    .onSizeChanged { tooltipSize = it }
                    .clickable { selectedIndex = null }
            )
        }
    }
}

/**
 * Floating CustomTooltip card matching Progress.jsx lines 38-56.
 *
 * <div className="rounded-[12px] border-[1.5px] border-[var(--h-bone-dark)] bg-[var(--h-cream)] px-3 py-2 z-50">
 *     <p className="font-mono text-[0.65rem] uppercase tracking-wider text-[var(--text-secondary)] mb-1">
 *         {labelFormatter ? labelFormatter(label) : label}
 *     </p>
 *     <p className="font-ui text-[1rem] font-bold" style={{ color: 'var(--accent-primary)' }}>
 *         {value} min
 *     </p>
 * </div>
 */
@Composable
fun CustomTooltipCard(
    dayLabel: String,
    dateText: String,
    minutes: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = hCream,
        border = BorderStroke(1.5.dp, hBoneDark),
        shadowElevation = 5.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = if (dateText.isNotEmpty()) "$dayLabel · $dateText".uppercase(Locale.getDefault()) else dayLabel.uppercase(Locale.getDefault()),
                fontFamily = fontFamilyMono,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp,
                color = hInkMuted
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "$minutes min",
                fontFamily = fontFamilyUi,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = hGold
            )
        }
    }
}

/**
 * Fritsch-Carlson Monotone Cubic Spline interpolation converted to Cubic Bezier curve.
 * Matches Recharts <Line type="monotone" /> (d3-shape curveMonotoneX).
 *
 * Guarantees:
 * 1. Monotonicity: no overshoots or artificial oscillations.
 * 2. Flat tangents at local extrema and zero baselines.
 * 3. Exact C1 continuous smoothness.
 */
internal fun buildMonotoneCubicPath(points: List<Offset>): Path {
    val path = Path()
    if (points.isEmpty()) return path
    path.moveTo(points[0].x, points[0].y)
    if (points.size == 1) return path
    if (points.size == 2) {
        path.lineTo(points[1].x, points[1].y)
        return path
    }

    val n = points.size
    val dx = FloatArray(n - 1)
    val dy = FloatArray(n - 1)
    val slope = FloatArray(n - 1)

    for (i in 0 until n - 1) {
        dx[i] = points[i + 1].x - points[i].x
        dy[i] = points[i + 1].y - points[i].y
        slope[i] = if (dx[i] != 0f) dy[i] / dx[i] else 0f
    }

    // Tangents m
    val m = FloatArray(n)
    m[0] = slope[0]
    for (i in 1 until n - 1) {
        val s0 = slope[i - 1]
        val s1 = slope[i]
        if (s0 * s1 <= 0f) {
            m[i] = 0f
        } else {
            // Harmonic mean (Fritsch-Carlson)
            m[i] = (2f * s0 * s1) / (s0 + s1)
        }
    }
    m[n - 1] = slope[n - 2]

    // Fritsch-Carlson monotonic limiter
    for (i in 0 until n - 1) {
        if (slope[i] == 0f) {
            m[i] = 0f
            m[i + 1] = 0f
        } else {
            val alpha = m[i] / slope[i]
            val beta = m[i + 1] / slope[i]
            val dist = alpha * alpha + beta * beta
            if (dist > 9f) {
                val tau = 3f / kotlin.math.sqrt(dist)
                m[i] = tau * alpha * slope[i]
                m[i + 1] = tau * beta * slope[i]
            }
        }
    }

    // Convert Hermite spline to Cubic Bezier segments
    for (i in 0 until n - 1) {
        val p0 = points[i]
        val p1 = points[i + 1]
        val h = dx[i]
        val cp1X = p0.x + h / 3f
        val cp1Y = p0.y + m[i] * (h / 3f)
        val cp2X = p1.x - h / 3f
        val cp2Y = p1.y - m[i + 1] * (h / 3f)
        path.cubicTo(cp1X, cp1Y, cp2X, cp2Y, p1.x, p1.y)
    }

    return path
}

/**
 * Finds the index of the point whose X coordinate is closest to the given touch X.
 */
internal fun findNearestIndex(touchX: Float, points: List<Offset>): Int {
    if (points.isEmpty()) return -1
    var bestIndex = 0
    var minDistance = Float.MAX_VALUE
    for (i in points.indices) {
        val dist = kotlin.math.abs(points[i].x - touchX)
        if (dist < minDistance) {
            minDistance = dist
            bestIndex = i
        }
    }
    return bestIndex
}

/**
 * Generates clean, human-readable Y-axis ticks ("0m", "15m", "30m", etc.)
 * based on the maximum minute value.
 */
internal fun computeYTicks(maxVal: Int): List<Int> {
    if (maxVal <= 25) return listOf(0, 15, 30)
    if (maxVal <= 50) return listOf(0, 15, 30, 45, 60)
    if (maxVal <= 90) return listOf(0, 30, 60, 90)
    if (maxVal <= 120) return listOf(0, 30, 60, 90, 120)
    val step = if (maxVal <= 240) 60 else 120
    val ceiling = ((maxVal + step - 1) / step) * step
    return (0..ceiling step step).toList()
}

/**
 * Calculates a date string ("Sep 12") for a day index in the last N days.
 */
internal fun formatDateForIndex(index: Int, totalDays: Int, dayLabel: String): String {
    if (dayLabel.any { it.isDigit() }) return ""
    val daysAgo = (totalDays - 1) - index
    val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -daysAgo) }
    return SimpleDateFormat("MMM d", Locale.getDefault()).format(cal.time)
}

@Preview(showBackground = true)
@Composable
private fun AnalyticsFlowChartPreview() {
    val sampleData = listOf(
        "Mon" to 15,
        "Tue" to 25,
        "Wed" to 0,
        "Thu" to 40,
        "Fri" to 30,
        "Sat" to 55,
        "Sun" to 20
    )
    Box(modifier = Modifier.padding(16.dp)) {
        AnalyticsFlowChart(dailyActivity = sampleData)
    }
}

@Preview(showBackground = true)
@Composable
private fun AnalyticsFlowChartZeroMinutesPreview() {
    val zeroData = listOf(
        "Mon" to 0,
        "Tue" to 0,
        "Wed" to 0,
        "Thu" to 0,
        "Fri" to 0,
        "Sat" to 0,
        "Sun" to 0
    )
    Box(modifier = Modifier.padding(16.dp)) {
        AnalyticsFlowChart(dailyActivity = zeroData)
    }
}
