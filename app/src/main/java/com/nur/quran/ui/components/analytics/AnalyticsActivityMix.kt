package com.nur.quran.ui.components.analytics

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.nur.quran.analytics.AnalyticsStats
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
 * Activity Mix donut with Today/Week/Month/All tabs (web ActivityMix.jsx parity,
 * default Week). Donut per type (rounded minutes, share %), center shows total +
 * session count, legend rows show Nm + %.
 *
 * @param sessions raw sessions; filtering + aggregation happen here via AnalyticsStats
 */
@Composable
fun AnalyticsActivityMix(
    sessions: List<AnalyticsStats.Session>,
    initialRange: String = AnalyticsStats.MixRange.WEEK,
    modifier: Modifier = Modifier
) {
    var range by remember { mutableStateOf(initialRange) }
    val rangeSessions = remember(sessions, range) {
        AnalyticsStats.filterByMixRange(sessions, range)
    }
    val totalSeconds = remember(rangeSessions) { rangeSessions.sumOf { it.durationSec } }
    val sessionCount = rangeSessions.size
    // Web parity: donut value = rounded minutes per type; share = seconds ratio.
    val secondsByType = remember(rangeSessions) {
        mapOf(
            "Reading" to rangeSessions.filter { it.type == "reading" || it.type.isEmpty() }.sumOf { it.durationSec },
            "Memorizing" to rangeSessions.filter { it.type == "memorizing" }.sumOf { it.durationSec },
            "Listening" to rangeSessions.filter { it.type == "listening" }.sumOf { it.durationSec },
            "Focus" to rangeSessions.filter { it.type == "pomodoro" || it.type == "focus" }.sumOf { it.durationSec }
        )
    }
    val activityMix = remember(rangeSessions) {
        val byType = AnalyticsStats.minutesByType(rangeSessions)
        listOf(
            Triple("Reading", byType["reading"] ?: 0, Color(0xFF10B981)),
            Triple("Memorizing", byType["memorizing"] ?: 0, Color(0xFF3B82F6)),
            Triple("Listening", byType["listening"] ?: 0, Color(0xFFF59E0B)),
            Triple("Focus", byType["focus"] ?: 0, Color(0xFF8B5CF6))
        ).filter { it.second > 0 }
    }
    val validSegments = remember(activityMix) { activityMix.filter { it.second > 0 } }
    val totalMixMins = remember(validSegments) { validSegments.sumOf { it.second } }
    val isEmpty = totalSeconds <= 0 || validSegments.isEmpty() || totalMixMins <= 0

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
            // Header matching web Progress.jsx lines 399-404; tabs on their own
            // row so TODAY/WEEK/MONTH/ALL TIME never squeeze or wrap on phones.
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
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

                // Range tabs (web MIX_TABS parity, default Week)
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(hSurface)
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    AnalyticsStats.MixRange.TABS.forEach { tab ->
                        val selected = tab == range
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (selected) hWhite else Color.Transparent)
                                .clickable { range = tab }
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = AnalyticsStats.MixRange.label(tab).uppercase(),
                                fontFamily = fontFamilyMono,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = if (selected) hInk else hInkMuted
                            )
                        }
                    }
                }
            }

            Text(
                text = AnalyticsStats.mixRangeTitle(range),
                fontFamily = fontFamilyMono,
                fontSize = 10.sp,
                letterSpacing = 1.5.sp,
                color = hInkMuted
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (isEmpty) {
                // Empty state (web: "No activity in this period yet.")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No activity in this period yet.",
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

                    // Center: total + session count (web ActivityMix.jsx parity)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = AnalyticsStats.formatMinutes(totalSeconds),
                            fontFamily = fontFamilyUi,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = hInk
                        )
                        Text(
                            text = "$sessionCount Session${if (sessionCount == 1) "" else "s"}",
                            fontFamily = fontFamilyMono,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.5.sp,
                            color = hInkMuted
                        )
                    }
                }

                // Legend rows: dot + name + Nm + share % (web ActivityMix.jsx parity)
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    validSegments.forEach { (name, mins, color) ->
                        // Web parity: share = seconds / totals.seconds (not rounded mins).
                        val share = if (totalSeconds > 0) Math.round((secondsByType[name] ?: 0L) * 100f / totalSeconds).toInt() else 0
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                                color = hInkMuted,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${mins}m",
                                fontFamily = fontFamilyUi,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = hInk
                            )
                            Text(
                                text = "$share%",
                                fontFamily = fontFamilyMono,
                                fontSize = 10.sp,
                                color = hInkMuted,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
