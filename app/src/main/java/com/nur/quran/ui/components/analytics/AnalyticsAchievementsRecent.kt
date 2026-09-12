package com.nur.quran.ui.components.analytics

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.analytics.AnalyticsStats
import com.nur.quran.data.db.entities.ChapterEntity
import com.nur.quran.data.db.entities.ReadingSessionEntity
import com.nur.quran.ui.components.NurIcons
import com.nur.quran.ui.screens.BadgeItem
import com.nur.quran.ui.screens.fontFamilyMono
import com.nur.quran.ui.screens.fontFamilyUi
import com.nur.quran.ui.screens.hBoneDark
import com.nur.quran.ui.screens.hBorderColor
import com.nur.quran.ui.screens.hCream
import com.nur.quran.ui.screens.hGold
import com.nur.quran.ui.screens.hGoldSoft
import com.nur.quran.ui.screens.hInk
import com.nur.quran.ui.screens.hInkMuted
import com.nur.quran.ui.screens.hSurface
import com.nur.quran.ui.screens.hWhite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Achievements Section matching web Progress.jsx lines 450-473.
 *
 * Displays unlocked achievement badges in rounded cards with emoji icons,
 * or an empty-state message if no badges have been earned yet.
 */
@Composable
fun AchievementsSection(
    achievements: List<BadgeItem>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = hCream),
        border = BorderStroke(1.5.dp, hBoneDark)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Title Header with Award icon in hGold
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = NurIcons.Award,
                    contentDescription = null,
                    tint = hGold,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Achievements",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = fontFamilyUi,
                    color = hInk
                )
            }

            if (achievements.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    achievements.forEach { badge ->
                        AchievementBadgeRow(badge = badge)
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Read consistently to unlock badges!",
                        fontSize = 13.sp,
                        color = hInkMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Individual achievement badge card:
 * Rounded card (16dp) with hWhite background, 1.5dp hBoneDark border.
 * Left box: 48dp x 48dp rounded box with emoji icon (size 24sp).
 * Title: 14sp bold hInk. Description: 12sp hInkMuted.
 */
@Composable
fun AchievementBadgeRow(
    badge: BadgeItem,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(hWhite)
            .border(BorderStroke(1.5.dp, hBoneDark), RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(hWhite)
                .border(BorderStroke(1.dp, hBoneDark.copy(alpha = 0.5f)), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = badge.icon,
                fontSize = 24.sp,
                textAlign = TextAlign.Center
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = badge.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = fontFamilyUi,
                color = hInk
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = badge.desc,
                fontSize = 12.sp,
                color = hInkMuted,
                lineHeight = 16.sp
            )
        }
    }
}

/**
 * Timeline-Style Recent Activity component matching web Progress.jsx lines 475-536.
 *
 * Replicates the vertical timeline connector line (width 2dp, color hBoneDark) down the left side,
 * overlaid with circular 38dp icon badges and right content cards displaying resolved session
 * details and formatted durations.
 */
@Composable
fun RecentActivityTimeline(
    sessions: List<ReadingSessionEntity>,
    chapters: List<ChapterEntity>,
    modifier: Modifier = Modifier
) {
    val recentSessions = remember(sessions) {
        sessions.sortedWith(
            compareByDescending<ReadingSessionEntity> { it.timestamp }
                .thenByDescending { it.date }
                .thenByDescending { it.id }
        ).take(5)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = hCream),
        border = BorderStroke(1.5.dp, hBoneDark)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header with BookOpen / History icon in hGold
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = NurIcons.BookOpen,
                    contentDescription = null,
                    tint = hGold,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Recent Activity",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = fontFamilyUi,
                    color = hInk
                )
            }

            if (recentSessions.isNotEmpty()) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    // Vertical timeline connector line (width 2dp, color hBoneDark) running down the left side
                    if (recentSessions.size > 1) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .padding(top = 19.dp, bottom = 19.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(start = 18.dp)
                                    .width(2.dp)
                                    .fillMaxHeight()
                                    .background(hBoneDark)
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        recentSessions.forEach { session ->
                            TimelineSessionRow(
                                session = session,
                                chapters = chapters
                            )
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No recent activity found.",
                        fontSize = 13.sp,
                        color = hInkMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Visual styling configuration for each session type in the timeline.
 */
private data class SessionVisualConfig(
    val icon: ImageVector,
    val bg: Color,
    val tint: Color,
    val baseTitle: String
)

private fun resolveSessionVisual(type: String): SessionVisualConfig {
    return when (type.lowercase().trim()) {
        "memorizing", "memorization" -> SessionVisualConfig(
            icon = NurIcons.Brain,
            bg = Color(0x1A3B82F6),
            tint = Color(0xFF3B82F6),
            baseTitle = "Memorization"
        )
        "pomodoro", "focus" -> SessionVisualConfig(
            icon = NurIcons.Award,
            bg = Color(0x1A8B5CF6),
            tint = Color(0xFF8B5CF6),
            baseTitle = "Focus Session"
        )
        "listening" -> SessionVisualConfig(
            icon = NurIcons.Volume2,
            bg = Color(0x1AF59E0B),
            tint = Color(0xFFF59E0B),
            baseTitle = "Listening Session"
        )
        else -> SessionVisualConfig(
            icon = NurIcons.BookOpen,
            bg = Color(0x1A10B981),
            tint = Color(0xFF10B981),
            baseTitle = "Reading Session"
        )
    }
}

/**
 * Subtitle: formatted date & time ("Sep 12 • 10:30 AM" or date if timestamp 0).
 */
private fun formatSessionSubtitle(timestamp: Long, dateString: String): String {
    return if (timestamp > 0L) {
        val date = Date(timestamp)
        val datePart = SimpleDateFormat("MMM d", Locale.US).format(date)
        val timePart = SimpleDateFormat("h:mm a", Locale.US).format(date)
        "$datePart • $timePart"
    } else {
        try {
            val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateString)
            if (parsed != null) {
                SimpleDateFormat("MMM d", Locale.US).format(parsed)
            } else {
                dateString
            }
        } catch (_: Exception) {
            dateString
        }
    }
}

@Composable
private fun TimelineSessionRow(
    session: ReadingSessionEntity,
    chapters: List<ChapterEntity>,
    modifier: Modifier = Modifier
) {
    val visual = remember(session.type) {
        resolveSessionVisual(session.type)
    }

    val resolvedTitle = remember(session.type, session.chapterId, chapters) {
        val chapterName = session.chapterId?.let { chId ->
            chapters.find { it.id == chId }?.nameSimple ?: "Surah $chId"
        }
        if (chapterName != null) {
            "${visual.baseTitle} - $chapterName"
        } else {
            visual.baseTitle
        }
    }

    val subtitle = remember(session.timestamp, session.date) {
        formatSessionSubtitle(session.timestamp, session.date)
    }

    val durationText = remember(session.duration) {
        AnalyticsStats.formatMinutes(session.duration)
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Circular icon badge (size 38dp) overlaid directly on top of the vertical line
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(hCream)
                .background(visual.bg)
                .border(BorderStroke(1.5.dp, hCream), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = visual.icon,
                contentDescription = null,
                tint = visual.tint,
                modifier = Modifier.size(16.dp)
            )
        }

        // Right content card: rounded (16dp), hWhite background, 1.5dp border
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(hWhite)
                .border(BorderStroke(1.5.dp, hBoneDark), RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = resolvedTitle,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = fontFamilyUi,
                    color = hInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    fontFamily = fontFamilyMono,
                    color = hInkMuted
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = durationText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = fontFamilyUi,
                color = hInkMuted
            )
        }
    }
}
