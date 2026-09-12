package com.nur.quran.ui.components.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.data.db.entities.BookmarkEntity
import com.nur.quran.ui.components.NurIcons
import com.nur.quran.ui.screens.*

/**
 * Bookmarks section matching web app Library.jsx lines 30-68.
 *
 * Renders:
 * - Header with Bookmark icon, "Bookmarks" title, and count chip.
 * - Empty state with dashed border and prompt when no bookmarks exist.
 * - Grid of bookmark cards with Surah name, Ayah key, navigation button, and delete button.
 */
@Composable
fun BookmarksSection(
    bookmarks: List<BookmarkEntity>,
    onNavigateToVerse: (chapterId: Int, verseKey: String) -> Unit,
    onDeleteBookmark: (verseKey: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = NurIcons.Bookmark,
                contentDescription = null,
                tint = hGold,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = "Bookmarks",
                fontFamily = fontFamilyUi,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = hInk
            )
            if (bookmarks.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(hGoldLight)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${bookmarks.size}",
                        fontFamily = fontFamilyMono,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = hGold
                    )
                }
            }
        }

        if (bookmarks.isEmpty()) {
            BookmarksEmptyState()
        } else {
            // Adaptive Grid / Column of Cards
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val isWide = maxWidth >= 600.dp
                if (isWide) {
                    // Two-column layout for tablets and landscape
                    val chunked = bookmarks.chunked(2)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        for (row in chunked) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                for (bookmark in row) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        BookmarkCard(
                                            bookmark = bookmark,
                                            onNavigate = { onNavigateToVerse(bookmark.chapterId, bookmark.verseKey) },
                                            onDelete = { onDeleteBookmark(bookmark.verseKey) }
                                        )
                                    }
                                }
                                if (row.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                } else {
                    // Single column on mobile
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        bookmarks.forEach { bookmark ->
                            BookmarkCard(
                                bookmark = bookmark,
                                onNavigate = { onNavigateToVerse(bookmark.chapterId, bookmark.verseKey) },
                                onDelete = { onDeleteBookmark(bookmark.verseKey) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BookmarkCard(
    bookmark: BookmarkEntity,
    onNavigate: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = hBorderColor
    val ayahNumber = bookmark.verseKey.substringAfter(':', bookmark.verseKey)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(hSurface)
            .drawBehind {
                drawRoundRect(
                    color = borderColor,
                    style = Stroke(width = 1.dp.toPx())
                )
            }
            .padding(18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = bookmark.surahName,
                fontFamily = fontFamilyUi,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = hInk
            )
            Text(
                text = "AYAH $ayahNumber",
                fontFamily = fontFamilyMono,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp,
                color = hInkMuted
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Navigate to Verse Button
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(hGoldLight)
                    .clickable(onClick = onNavigate),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = NurIcons.ArrowRight,
                    contentDescription = "Read verse",
                    tint = hGold,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Delete Bookmark Button
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0x1ADC2626))
                    .clickable(onClick = onDelete),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = NurIcons.Trash2,
                    contentDescription = "Delete bookmark",
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun BookmarksEmptyState(
    modifier: Modifier = Modifier
) {
    val borderColor = hBorderColor

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(hSurface)
            .drawBehind {
                val stroke = Stroke(
                    width = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                )
                drawRoundRect(
                    color = borderColor,
                    style = stroke
                )
            }
            .padding(vertical = 36.dp, horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(hGoldLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = NurIcons.Bookmark,
                    contentDescription = null,
                    tint = hGold.copy(alpha = 0.5f),
                    modifier = Modifier.size(26.dp)
                )
            }
            Text(
                text = "No bookmarks yet. Save your favorite ayahs to see them here.",
                fontFamily = fontFamilyBody,
                fontSize = 14.sp,
                color = hInkMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}
