package com.nur.quran.ui.components.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.data.repository.QuranRepository
import com.nur.quran.ui.components.NurIcons
import com.nur.quran.ui.screens.*

/**
 * Collections section matching web app Library.jsx lines 71-161.
 *
 * Renders:
 * - Header with Folder icon, "Collections" title, and inline "+ Create Collection" bar.
 * - Empty state with dashed border when no collections exist.
 * - Responsive grid of collection cards showing verse items, remove actions, and "Launch Hifdh".
 */
@Composable
fun CollectionsSection(
    collections: List<QuranRepository.CollectionWithItems>,
    onCreateCollection: (name: String) -> Unit,
    onDeleteCollection: (collectionId: Long) -> Unit,
    onRemoveItem: (collectionId: Long, verseKey: String) -> Unit,
    onNavigateToVerse: (chapterId: Int, verseKey: String) -> Unit,
    onLaunchHifdh: (chapterId: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var newCollectionName by remember { mutableStateOf("") }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section Header + Inline Creation Bar
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = NurIcons.Folder,
                    contentDescription = null,
                    tint = hGold,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Collections",
                    fontFamily = fontFamilyUi,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = hInk
                )
                if (collections.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(hGoldLight)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${collections.size}",
                            fontFamily = fontFamilyMono,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = hGold
                        )
                    }
                }
            }

            // Create Collection Input Bar
            CreateCollectionBar(
                collectionName = newCollectionName,
                onNameChange = { newCollectionName = it },
                onCreate = {
                    if (newCollectionName.isNotBlank()) {
                        onCreateCollection(newCollectionName.trim())
                        newCollectionName = ""
                    }
                }
            )
        }

        if (collections.isEmpty()) {
            CollectionsEmptyState()
        } else {
            // Adaptive Grid / List of Collection Cards
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val isWide = maxWidth >= 680.dp
                if (isWide) {
                    // Two-column layout for tablets / large screens
                    val chunked = collections.chunked(2)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        for (row in chunked) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                for (colWithItems in row) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        CollectionCard(
                                            collectionWithItems = colWithItems,
                                            onDeleteCollection = { onDeleteCollection(colWithItems.collection.id) },
                                            onRemoveItem = { verseKey -> onRemoveItem(colWithItems.collection.id, verseKey) },
                                            onNavigateToVerse = onNavigateToVerse,
                                            onLaunchHifdh = {
                                                colWithItems.items.firstOrNull()?.let { firstItem ->
                                                    onLaunchHifdh(firstItem.chapterId)
                                                }
                                            }
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
                    // Single column on phones
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        collections.forEach { colWithItems ->
                            CollectionCard(
                                collectionWithItems = colWithItems,
                                onDeleteCollection = { onDeleteCollection(colWithItems.collection.id) },
                                onRemoveItem = { verseKey -> onRemoveItem(colWithItems.collection.id, verseKey) },
                                onNavigateToVerse = onNavigateToVerse,
                                onLaunchHifdh = {
                                    colWithItems.items.firstOrNull()?.let { firstItem ->
                                        onLaunchHifdh(firstItem.chapterId)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CreateCollectionBar(
    collectionName: String,
    onNameChange: (String) -> Unit,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = hBorderColor

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(hSurface)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            if (collectionName.isEmpty()) {
                Text(
                    text = "New Collection Name...",
                    fontFamily = fontFamilyBody,
                    fontSize = 14.sp,
                    color = hInkMuted
                )
            }
            BasicTextField(
                value = collectionName,
                onValueChange = onNameChange,
                singleLine = true,
                textStyle = TextStyle(
                    fontFamily = fontFamilyBody,
                    fontSize = 14.sp,
                    color = hInk
                ),
                cursorBrush = SolidColor(hGold),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(hGold)
                .clickable(onClick = onCreate)
                .padding(horizontal = 14.dp, vertical = 9.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = NurIcons.Plus,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Create",
                    fontFamily = fontFamilyBody,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun CollectionCard(
    collectionWithItems: QuranRepository.CollectionWithItems,
    onDeleteCollection: () -> Unit,
    onRemoveItem: (verseKey: String) -> Unit,
    onNavigateToVerse: (chapterId: Int, verseKey: String) -> Unit,
    onLaunchHifdh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = hBorderColor
    val collection = collectionWithItems.collection
    val items = collectionWithItems.items

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(hSurface)
            .drawBehind {
                drawRoundRect(
                    color = borderColor,
                    style = Stroke(width = 1.dp.toPx())
                )
            }
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Card Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = collection.name,
                    fontFamily = fontFamilyUi,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = hInk
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = NurIcons.LibraryBig,
                        contentDescription = null,
                        tint = hInkMuted,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "${items.size} verses",
                        fontFamily = fontFamilyMono,
                        fontSize = 12.sp,
                        color = hInkMuted
                    )
                }
            }

            // Delete Collection Button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0x1ADC2626))
                    .clickable(onClick = onDeleteCollection),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = NurIcons.Trash2,
                    contentDescription = "Delete collection",
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Preview Items List
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (items.isEmpty()) {
                Text(
                    text = "No verses in this collection.",
                    fontFamily = fontFamilyBody,
                    fontSize = 13.sp,
                    color = hInkMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                )
            } else {
                val previewItems = items.take(4)
                previewItems.forEach { item ->
                    val ayahNum = item.verseKey.substringAfter(':', item.verseKey)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(hCream)
                            .border(1.dp, borderColor.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${item.surahName} $ayahNum",
                            fontFamily = fontFamilyBody,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = hInk
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { onNavigateToVerse(item.chapterId, item.verseKey) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = NurIcons.ArrowRight,
                                    contentDescription = "Read verse",
                                    tint = hGold,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { onRemoveItem(item.verseKey) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = NurIcons.X,
                                    contentDescription = "Remove verse",
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                if (items.size > 4) {
                    Text(
                        text = "+ ${items.size - 4} more verses",
                        fontFamily = fontFamilyMono,
                        fontSize = 11.sp,
                        color = hInkMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    )
                }
            }
        }

        // Launch Hifdh Button
        if (items.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(hGold)
                    .clickable(onClick = onLaunchHifdh),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = NurIcons.BookOpen,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Launch Hifdh",
                        fontFamily = fontFamilyBody,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun CollectionsEmptyState(
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
                    imageVector = NurIcons.Folder,
                    contentDescription = null,
                    tint = hGold.copy(alpha = 0.5f),
                    modifier = Modifier.size(26.dp)
                )
            }
            Text(
                text = "No collections yet. Group verses together for better hifdh focus.",
                fontFamily = fontFamilyBody,
                fontSize = 14.sp,
                color = hInkMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}
