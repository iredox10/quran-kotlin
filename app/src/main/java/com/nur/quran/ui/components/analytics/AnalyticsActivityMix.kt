package com.nur.quran.ui.components.analytics

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.ui.components.NurIcons
import com.nur.quran.ui.screens.fontFamilyMono
import com.nur.quran.ui.screens.fontFamilyUi
import com.nur.quran.ui.screens.hBoneDark
import com.nur.quran.ui.screens.hBorderColor
import com.nur.quran.ui.screens.hCream
import com.nur.quran.ui.screens.hGold
import com.nur.quran.ui.screens.hInk
import com.nur.quran.ui.screens.hInkMuted
import com.nur.quran.ui.screens.hSurface
import com.nur.quran.ui.screens.hWhite

/**
 * Data representation for an individual arc segment in the Activity Mix donut chart.
 */
data class ActivityMixSegment(
    val name: String,
    val mins: Int,
    val color: Color,
    val startAngle: Float,
    val sweepAngle: Float
)

/**
 * Calculates start and sweep angles for each category in [activityMix].
 * Proportional sweep: (mins / totalMins) * 360f.
 * Maintains an angle gap (paddingAngle ~4-6 degrees) between segments matching
 * Recharts paddingAngle={6} cornerRadius={8}.
 */
fun calculateActivityMixSegments(
    activityMix: List<Triple<String, Int, Color>>,
    paddingAngle: Float = 6f
): List<ActivityMixSegment> {
    val valid = activityMix.filter { it.second > 0 }
    if (valid.isEmpty()) return emptyList()

    val totalMins = valid.sumOf { it.second }
    if (totalMins <= 0) return emptyList()

    if (valid.size == 1) {
        val single = valid[0]
        return listOf(
            ActivityMixSegment(
                name = single.first,
                mins = single.second,
                color = single.third,
                startAngle = -90f,
                sweepAngle = 360f
            )
        )
    }

    var currentAngle = -90f
    val result = ArrayList<ActivityMixSegment>(valid.size)

    for (item in valid) {
        val rawSweep = (item.second.toFloat() / totalMins.toFloat()) * 360f
        val gap = if (rawSweep > paddingAngle * 1.5f) paddingAngle else (rawSweep * 0.4f)
        val sweepAngle = maxOf(0.5f, rawSweep - gap)
        val startAngle = currentAngle + (gap / 2f)

        result.add(
            ActivityMixSegment(
                name = item.first,
                mins = item.second,
                color = item.third,
                startAngle = startAngle,
                sweepAngle = sweepAngle
            )
        )
        currentAngle += rawSweep
    }

    return result
}

/**
 * Activity Mix Multi-Segment Donut Chart component matching web Progress.jsx lines 398-446.
 *
 * @param activityMix Category breakdown: Name, Minutes, Color (e.g. Reading, Memorizing, Focus, Listening)
 * @param allTimeTotalMins Total minutes across all time displayed in the donut center hole
 * @param modifier Card layout modifier
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AnalyticsActivityMix(
    activityMix: List<Triple<String, Int, Color>>,
    allTimeTotalMins: Int,
    modifier: Modifier = Modifier
) {
    val validSegments = remember(activityMix) {
        activityMix.filter { it.second > 0 }
    }
    val totalMixMins = remember(validSegments) {
        validSegments.sumOf { it.second }
    }
    val isEmpty = allTimeTotalMins <= 0 || validSegments.isEmpty() || totalMixMins <= 0

    val segments = remember(validSegments) {
        calculateActivityMixSegments(validSegments, paddingAngle = 6f)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = hCream),
        border = BorderStroke(1.5.dp, hBoneDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Header matching web Progress.jsx lines 399-404
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = NurIcons.Layers3,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = hGold
                    )
                    Text(
                        text = "Activity Mix",
                        fontFamily = fontFamilyUi,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = hInk
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(hSurface)
                        .border(BorderStroke(0.5.dp, hBorderColor.copy(alpha = 0.5f)), CircleShape)
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "ALL TIME",
                        fontFamily = fontFamilyMono,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = hInkMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isEmpty) {
                // Empty state matching web Progress.jsx line 426
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No activity data yet.",
                        fontFamily = fontFamilyUi,
                        fontSize = 14.sp,
                        color = hInkMuted
                    )
                }
            } else {
                // Donut Chart Container matching web Progress.jsx lines 405-434
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(
                        modifier = Modifier.size(170.dp)
                    ) {
                        val strokeWidthPx = 24.dp.toPx()
                        val diameter = size.minDimension - strokeWidthPx
                        val arcRadius = diameter / 2f
                        val arcSize = Size(diameter, diameter)
                        val topLeft = Offset(
                            (size.width - diameter) / 2f,
                            (size.height - diameter) / 2f
                        )

                        // Subtle track ring background
                        drawCircle(
                            color = hBorderColor.copy(alpha = 0.25f),
                            radius = arcRadius,
                            center = center,
                            style = Stroke(width = strokeWidthPx)
                        )

                        if (segments.size == 1) {
                            // Single segment: full 360 circle
                            drawCircle(
                                color = segments[0].color,
                                radius = arcRadius,
                                center = center,
                                style = Stroke(width = strokeWidthPx)
                            )
                        } else {
                            // Multiple segments: draw with StrokeCap.Round
                            // capAngleDeg accounts for StrokeCap.Round extending beyond arc endpoints
                            val capAngleDeg = if (arcRadius > 0f) {
                                Math.toDegrees((strokeWidthPx / 2.0) / arcRadius.toDouble()).toFloat()
                            } else 0f

                            val totalCapOffset = 2f * capAngleDeg
                            for (seg in segments) {
                                val adjustedSweep = if (seg.sweepAngle > totalCapOffset + 2f) {
                                    seg.sweepAngle - totalCapOffset
                                } else {
                                    (seg.sweepAngle * 0.6f).coerceAtLeast(1f)
                                }
                                val adjustedStart = seg.startAngle + (seg.sweepAngle - adjustedSweep) / 2f

                                drawArc(
                                    color = seg.color,
                                    startAngle = adjustedStart,
                                    sweepAngle = adjustedSweep,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                                )
                            }
                        }
                    }

                    // Center Hole matching web Progress.jsx lines 428-433
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "$allTimeTotalMins",
                            fontFamily = fontFamilyUi,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = hInk
                        )
                        Text(
                            text = "Mins Total",
                            fontFamily = fontFamilyMono,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.5.sp,
                            color = hInkMuted
                        )
                    }
                }

                // Legend below matching web Progress.jsx lines 435-445
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    validSegments.forEach { (name, mins, color) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                            Text(
                                text = name,
                                fontFamily = fontFamilyUi,
                                fontSize = 13.sp,
                                color = hInkMuted
                            )
                            Text(
                                text = "${mins}m",
                                fontFamily = fontFamilyUi,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = hInk
                            )
                        }
                    }
                }
            }
        }
    }
}
