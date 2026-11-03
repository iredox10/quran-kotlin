package com.nur.quran.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.R
import com.nur.quran.ui.screens.fontFamilyUi
import com.nur.quran.ui.screens.fontFamilyMono
import com.nur.quran.ui.screens.hTeal
import com.nur.quran.ui.screens.hInk
import com.nur.quran.ui.screens.hInkMuted

/** A normal list footer: it never changes the reader viewport as the user scrolls. */
@Composable
fun ReaderPageNavigation(
    page: Int,
    firstPage: Int = 1,
    lastPage: Int = 604,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    modifier: Modifier = Modifier,
    onPageClick: (() -> Unit)? = null,
    onFinish: (() -> Unit)? = null,
    isCompleted: Boolean = false
) {
    // Keep mushaf page order: next on the left, previous on the right.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            modifier = modifier.fillMaxWidth().padding(vertical = 24.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val atEnd = page >= lastPage
            val nextEnabled = if (atEnd) onFinish != null && !isCompleted else true
            Text(
                text = if (atEnd && onFinish != null) stringResource(R.string.planner_finish_day)
                    else stringResource(R.string.ui_next_0fed81),
                fontFamily = fontFamilyUi,
                fontWeight = FontWeight.Bold,
                color = if (nextEnabled) hTeal else hInkMuted,
                modifier = Modifier.clip(RoundedCornerShape(8.dp))
                    .clickable(enabled = nextEnabled) { if (atEnd) onFinish?.invoke() else onNext() }
                    .heightIn(min = 48.dp).wrapContentHeight().padding(horizontal = 8.dp)
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clip(RoundedCornerShape(8.dp))
                    .clickable(enabled = onPageClick != null) { onPageClick?.invoke() }
                    .heightIn(min = 48.dp).wrapContentHeight().padding(horizontal = 8.dp)
            ) {
                Text(stringResource(R.string.ui_page_618c75), fontFamily = fontFamilyMono,
                    fontSize = 10.sp, color = hInkMuted, letterSpacing = 1.sp)
                Text(stringResource(R.string.ui_1_s_2_s_5935d7, page, lastPage),
                    fontFamily = fontFamilyUi, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = hInk)
            }
            Text(
                text = stringResource(R.string.ui_previous_f67a76),
                fontFamily = fontFamilyUi,
                fontWeight = FontWeight.Bold,
                color = if (page > firstPage) hTeal else hInkMuted,
                modifier = Modifier.clip(RoundedCornerShape(8.dp))
                    .clickable(enabled = page > firstPage, onClick = onPrevious)
                    .heightIn(min = 48.dp).wrapContentHeight().padding(horizontal = 8.dp)
            )
        }
    }
}
