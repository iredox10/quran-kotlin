package com.nur.quran.ui.components.analytics

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import android.content.Context
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

private fun formatBadgeCount(n: Int): String =
    if (n >= 1000) String.format(Locale.US, "%,d", n) else n.toString()

/** Web-parity board: unlocked/total header, locked rows with current/target + bar, collapsed-4 + expand. */
@Composable
fun AchievementsBoard(
    badges: List<AnalyticsStats.AchievementBadge>,
    hasSessions: Boolean = true,
    modifier: Modifier = Modifier
) {
    val ordered = remember(badges) { AnalyticsStats.orderBadges(badges) }
    var expanded by remember { mutableStateOf(false) }
    val visible = if (expanded) ordered else ordered.take(AnalyticsStats.COLLAPSED_BADGE_COUNT)
    Card(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = hCream), border = BorderStroke(1.5.dp, hBoneDark)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = NurIcons.Award, contentDescription = null, tint = hGold, modifier = Modifier.size(18.dp))
                    Text(text = "Achievements", fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = fontFamilyUi, color = hInk)
                }
                Text(text = "${badges.count { it.unlocked }} / ${badges.size}", fontSize = 10.sp, fontFamily = fontFamilyMono, color = hInkMuted, modifier = Modifier.clip(CircleShape).background(hSurface).padding(horizontal = 10.dp, vertical = 4.dp))
            }
            if (!hasSessions) {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                    Text(text = "Read consistently to unlock badges!", fontSize = 13.sp, color = hInkMuted, textAlign = TextAlign.Center)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    visible.forEach { badge ->
                        if (badge.unlocked) AchievementBadgeRow(badge = BadgeItem(badge.icon, badge.title, badge.desc))
                        else LockedBadgeRow(badge = badge)
                    }
                }
                if (ordered.size > AnalyticsStats.COLLAPSED_BADGE_COUNT) {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 12.dp).clip(RoundedCornerShape(20.dp)).background(hWhite).border(BorderStroke(1.5.dp, hBoneDark), RoundedCornerShape(20.dp)).clickable { expanded = !expanded }.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                        Text(text = if (expanded) "SHOW LESS" else "SHOW ALL", fontSize = 10.sp, fontFamily = fontFamilyMono, color = hInkMuted)
                    }
                }
            }
        }
    }
}

@Composable
private fun LockedBadgeRow(badge: AnalyticsStats.AchievementBadge, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth().alpha(0.75f).clip(RoundedCornerShape(16.dp)).background(hWhite).border(BorderStroke(1.5.dp, hBoneDark), RoundedCornerShape(16.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(modifier = Modifier.size(48.dp).alpha(0.55f).clip(RoundedCornerShape(12.dp)).background(hWhite).border(BorderStroke(1.dp, hBoneDark.copy(alpha = 0.5f)), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            Text(text = badge.icon, fontSize = 24.sp, textAlign = TextAlign.Center)
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = badge.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = fontFamilyUi, color = hInk)
                Text(text = "${formatBadgeCount(badge.current)} / ${formatBadgeCount(badge.target)}", fontSize = 10.sp, fontFamily = fontFamilyMono, color = hInkMuted)
            }
            Text(text = badge.desc, fontSize = 12.sp, color = hInkMuted)
            Spacer(modifier = Modifier.height(6.dp))
            Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(hSurface)) {
                Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(badge.progress.coerceIn(0f, 1f)).clip(RoundedCornerShape(3.dp)).background(hGold))
            }
        }
    }
}

private const val ACHV_SEEN_PREFS = "quran-nur-achv-seen"
private const val ACHV_SEEN_KEY = "seen_ids"

/**
 * Web-parity celebration: persists seen badge ids in prefs and shows a simple
 * in-app banner only for fresh unlocks (no confetti lib — card UI pattern).
 * Mirrors web: celebrate only when fresh unlock exists && todaySessions > 1.
 */
@Composable
fun FreshUnlockCelebration(
    badges: List<AnalyticsStats.AchievementBadge>,
    todaySessions: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    // Best-effort like web readSeen/writeSeen: fold every unlock into seen so
    // old badges never re-celebrate; never break the card over storage.
    val freshTitles = remember(badges) {
        runCatching {
            val prefs = context.getSharedPreferences(ACHV_SEEN_PREFS, Context.MODE_PRIVATE)
            val seen = prefs.getStringSet(ACHV_SEEN_KEY, emptySet()).orEmpty()
            val unlocked = badges.filter { it.unlocked }
            val fresh = unlocked.filter { it.id !in seen }.map { it.title }
            prefs.edit().putStringSet(ACHV_SEEN_KEY, (seen + unlocked.map { it.id }).toSet()).apply()
            fresh
        }.getOrDefault(emptyList())
    }
    if (freshTitles.isNotEmpty() && todaySessions > 1) {
        Card(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = hGoldSoft), border = BorderStroke(1.5.dp, hGold)) {
            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "🎉", fontSize = 26.sp, textAlign = TextAlign.Center)
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "New badge unlocked!", fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = fontFamilyUi, color = hInk)
                    Text(text = freshTitles.take(3).joinToString(", "), fontSize = 12.sp, color = hInkMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
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
    modifier: Modifier = Modifier,
    limit: Int = 5,
    onSeeAllClick: (() -> Unit)? = null
) {
    val recentSessions = remember(sessions, limit) {
        sessions.sortedWith(
            compareByDescending<ReadingSessionEntity> { effectiveTime(it) }
                .thenByDescending { it.id }
        ).take(limit.coerceAtLeast(1))
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = hCream),
        border = BorderStroke(1.5.dp, hBoneDark)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header with BookOpen icon in hGold + optional See-all (no activity route in Screen.kt yet)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = NurIcons.BookOpen, contentDescription = null, tint = hGold, modifier = Modifier.size(18.dp))
                    Text(text = "Recent Activity", fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = fontFamilyUi, color = hInk)
                }
                if (onSeeAllClick != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.clip(CircleShape).background(hWhite).border(BorderStroke(1.5.dp, hBoneDark), CircleShape).clickable(onClick = onSeeAllClick).padding(horizontal = 10.dp, vertical = 6.dp)) {
                        Text(text = "SEE ALL", fontSize = 10.sp, fontFamily = fontFamilyMono, color = hInkMuted)
                        Icon(imageVector = NurIcons.ChevronRight, contentDescription = "See all activity", tint = hInkMuted, modifier = Modifier.size(13.dp))
                    }
                }
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
            icon = NurIcons.Target,
            bg = Color(0x1A8B5CF6),
            tint = Color(0xFF8B5CF6),
            baseTitle = "Focus Session"
        )
        "listening" -> SessionVisualConfig(
            icon = NurIcons.Volume2,
            bg = Color(0x1AF59E0B),
            tint = Color(0xFFF59E0B),
            baseTitle = "Listening"
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
 * Web parity (RecentActivity.jsx): sort key is `timestamp || date-millis`,
 * so legacy sessions without a timestamp interleave by day.
 */
private fun effectiveTime(session: ReadingSessionEntity): Long {
    if (session.timestamp > 0L) return session.timestamp
    return runCatching {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(session.date)?.time ?: 0L
    }.getOrDefault(0L)
}

/**
 * Subtitle: "Sep 12 • 10:30 AM", or "Sep 12 • Logged" without a timestamp (web parity).
 */
private fun formatSessionSubtitle(timestamp: Long, dateString: String): String {
    val datePart = if (timestamp > 0L) {
        SimpleDateFormat("MMM d", Locale.US).format(Date(timestamp))
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
    val timePart = if (timestamp > 0L) {
        SimpleDateFormat("h:mm a", Locale.US).format(Date(timestamp))
    } else {
        "Logged"
    }
    return "$datePart • $timePart"
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
