package com.nur.quran.ui.components.analytics

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.ui.components.NurIcons
import com.nur.quran.ui.screens.fontFamilyMono
import com.nur.quran.ui.screens.fontFamilyUi
import com.nur.quran.ui.screens.hBoneDark
import com.nur.quran.ui.screens.hCream
import com.nur.quran.ui.screens.hGold
import com.nur.quran.ui.screens.hGoldLight
import com.nur.quran.ui.screens.hInk
import com.nur.quran.ui.screens.hInkMuted

/**
 * Bottom Quick Navigation Cards matching web Progress.jsx lines 538-557.
 *
 * Displays 3 navigation cards (Bookmarks, Collections, Recent Surahs)
 * with responsive layout (stacked on small screens, 3 columns when screen width >= 600dp).
 *
 * Each card features:
 * - Left rounded box (38dp x 38dp) with icon (BookmarkFilled, BookOpen, Brain) in hGold.
 * - Text content: Large count (20sp bold hInk) + mono uppercase label ("BOOKMARKS", "COLLECTIONS", "RECENT SURAHS").
 * - Right chevron arrow tinted hInkMuted.
 * - Clickable invoking the respective callback.
 */
@Composable
fun AnalyticsQuickCards(
    bookmarksCount: Int,
    collectionsCount: Int,
    recentSurahsCount: Int,
    onOpenLibraryClick: () -> Unit,
    onOpenCollectionsClick: () -> Unit,
    onOpenQuranClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val isWide = maxWidth >= 600.dp

        if (isWide) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickCardItem(
                    icon = NurIcons.BookmarkFilled,
                    count = bookmarksCount,
                    label = "BOOKMARKS",
                    onClick = onOpenLibraryClick,
                    modifier = Modifier.weight(1f)
                )
                QuickCardItem(
                    icon = NurIcons.BookOpen,
                    count = collectionsCount,
                    label = "COLLECTIONS",
                    onClick = onOpenCollectionsClick,
                    modifier = Modifier.weight(1f)
                )
                QuickCardItem(
                    icon = NurIcons.Brain,
                    count = recentSurahsCount,
                    label = "RECENT SURAHS",
                    onClick = onOpenQuranClick,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickCardItem(
                    icon = NurIcons.BookmarkFilled,
                    count = bookmarksCount,
                    label = "BOOKMARKS",
                    onClick = onOpenLibraryClick,
                    modifier = Modifier.fillMaxWidth()
                )
                QuickCardItem(
                    icon = NurIcons.BookOpen,
                    count = collectionsCount,
                    label = "COLLECTIONS",
                    onClick = onOpenCollectionsClick,
                    modifier = Modifier.fillMaxWidth()
                )
                QuickCardItem(
                    icon = NurIcons.Brain,
                    count = recentSurahsCount,
                    label = "RECENT SURAHS",
                    onClick = onOpenQuranClick,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickCardItem(
    icon: ImageVector,
    count: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = hCream),
        border = BorderStroke(1.5.dp, hBoneDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(hGoldLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = hGold,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "$count",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = fontFamilyUi,
                        color = hInk,
                        lineHeight = 22.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        fontFamily = fontFamilyMono,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = hInkMuted
                    )
                }
            }
            Icon(
                imageVector = NurIcons.ChevronRight,
                contentDescription = null,
                tint = hInkMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
