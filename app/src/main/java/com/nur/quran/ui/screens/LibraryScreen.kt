package com.nur.quran.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
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
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = hInk,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = "My Library",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = hInk,
                    fontFamily = fontFamilyUi
                )
                Text(
                    text = "${bookmarks.size} bookmarks • ${collectionsWithItems.size} collections",
                    fontSize = 12.sp,
                    color = hInkMuted,
                    fontFamily = fontFamilyBody
                )
            }
        }

        // Scrollable Content
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 48.dp)
        ) {
            item {
                BookmarksSection(
                    bookmarks = bookmarks,
                    onNavigateToVerse = onNavigateToVerse,
                    onDeleteBookmark = { verseKey -> viewModel.deleteBookmark(verseKey) }
                )
            }

            item {
                CollectionsSection(
                    collections = collectionsWithItems,
                    onCreateCollection = { name -> viewModel.addCollection(name) },
                    onDeleteCollection = { colId -> viewModel.deleteCollection(colId) },
                    onRemoveItem = { colId, verseKey -> viewModel.removeFromCollection(colId, verseKey) },
                    onNavigateToVerse = onNavigateToVerse,
                    onLaunchHifdh = onLaunchHifdh
                )
            }
        }
    }
}
