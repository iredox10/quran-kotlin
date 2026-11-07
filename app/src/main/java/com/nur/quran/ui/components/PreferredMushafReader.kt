package com.nur.quran.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import com.nur.quran.data.mushaf.NarrationMushaf
import com.nur.quran.data.mushaf.rememberReaderMushaf

/** Every ordinary Quran entry resolves the saved edition before composing a Hafs loader. */
@Composable
fun PreferredMushafReader(
    onDismiss: () -> Unit,
    onSelectHafs: (String) -> Unit,
    initialSurah: Int? = null,
    initialPage: Int? = null,
    hafsVerseKey: String? = null,
    hafsContent: @Composable () -> Unit
) {
    val edition = rememberReaderMushaf()
    // Bookmarks/audio links explicitly identify a Hafs verse. Keep that owner and its exact anchor.
    if (hafsVerseKey != null) { hafsContent(); return }
    key(edition) { when (edition) {
        "warsh", "qaloon" -> NarrationMushafReader(NarrationMushaf.entries.first { it.id == edition },
            onDismiss = onDismiss, initialSurah = initialSurah, initialPage = initialPage, onSelectHafs = onSelectHafs)
        "indopak" -> IndopakMushafReader(onDismiss = onDismiss, initialSurah = initialSurah,
            initialPage = initialPage, onSelectHafs = onSelectHafs)
        else -> hafsContent()
    } }
}
