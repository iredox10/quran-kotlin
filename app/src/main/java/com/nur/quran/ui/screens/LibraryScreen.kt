package com.nur.quran.ui.screens

import com.nur.quran.ui.components.NurIcons
import androidx.compose.ui.res.stringResource
import com.nur.quran.R

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nur.quran.ui.components.library.*
import com.nur.quran.ui.components.library.BookmarksSection
import com.nur.quran.ui.components.library.CollectionsSection
import com.nur.quran.ui.viewmodels.LibraryViewModel

/**
 * Library screen matching web app Library.jsx.
 *
 * Houses Bookmarks and Collections sections with:
 * - Top header displaying "My Library" and summary counts.
 * - Bookmarks grid with direct reader jump and delete actions.
 * - Collections section with inline creation bar, verse preview cards, and "Launch Hifdh".
 */
@Composable
fun LibraryScreen(
    onBackClick: () -> Unit,
    onNavigateToVerse: (chapterId: Int, verseKey: String) -> Unit,
    onLaunchHifdh: (chapterId: Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val bookmarks by viewModel.bookmarks.collectAsState()
    val collectionsWithItems by viewModel.collectionsWithItems.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(hCream)
    ) {
        // Top Navigation Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = NurIcons.ArrowLeft,
                    contentDescription = stringResource(R.string.ui_back_b52b36),
                    tint = hInk,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = stringResource(R.string.ui_my_library_7996cc),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = hInk,
                    fontFamily = fontFamilyUi
                )
                Text(
                    text = stringResource(R.string.ui_1_s_bookmarks_2_s_collections_938b04, bookmarks.size, collectionsWithItems.size),
                    fontSize = 12.sp,
                    color = hInkMuted,
                    fontFamily = fontFamilyBody
                )
            }
        }

        // The scrolling parent owns individual rows. A whole library inside one
        // item eagerly composes every card, even when it is far off screen.
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val bookmarkColumns = if (maxWidth >= 632.dp) 2 else 1
            val collectionColumns = if (maxWidth >= 712.dp) 2 else 1
            val bookmarkRows = remember(bookmarks, bookmarkColumns) { bookmarks.chunked(bookmarkColumns) }
            val collectionRows = remember(collectionsWithItems, collectionColumns) { collectionsWithItems.chunked(collectionColumns) }
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 48.dp)
            ) {
                item(key = "bookmarks-header", contentType = "header") {
                    BookmarksSection(bookmarks, onNavigateToVerse, viewModel::deleteBookmark, showEntries = false)
                }
                if (bookmarks.isEmpty()) item(key = "bookmarks-empty") { BookmarksEmptyState() }
                items(bookmarkRows, key = { "bookmark:${it.first().verseKey}" }, contentType = { "bookmark-row" }) { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { bookmark ->
                            BookmarkCard(bookmark,
                                onNavigate = { onNavigateToVerse(bookmark.chapterId, bookmark.verseKey) },
                                onDelete = { viewModel.deleteBookmark(bookmark.verseKey) }, modifier = Modifier.weight(1f))
                        }
                        if (row.size < bookmarkColumns) Spacer(Modifier.weight(1f))
                    }
                }
                item(key = "collections-header", contentType = "header") {
                    CollectionsSection(collectionsWithItems, viewModel::addCollection, viewModel::deleteCollection,
                        viewModel::removeFromCollection, onNavigateToVerse, onLaunchHifdh,
                        modifier = Modifier.padding(top = 12.dp), showEntries = false)
                }
                if (collectionsWithItems.isEmpty()) item(key = "collections-empty") { CollectionsEmptyState() }
                items(collectionRows, key = { "collection:${it.first().collection.id}" }, contentType = { "collection-row" }) { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        row.forEach { entry ->
                            CollectionCard(entry,
                                onDeleteCollection = { viewModel.deleteCollection(entry.collection.id) },
                                onRemoveItem = { viewModel.removeFromCollection(entry.collection.id, it) },
                                onNavigateToVerse = onNavigateToVerse,
                                onLaunchHifdh = { entry.items.firstOrNull()?.let { onLaunchHifdh(it.chapterId) } },
                                modifier = Modifier.weight(1f))
                        }
                        if (row.size < collectionColumns) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
