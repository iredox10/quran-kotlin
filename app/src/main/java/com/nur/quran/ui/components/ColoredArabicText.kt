package com.nur.quran.ui.components

import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily

data class TajweedSegment(
    val start: Int,
    val end: Int,
    val colorHex: String,
    val ruleClass: String? = null
)

@Composable
fun ColoredArabicText(
    text: String,
    segments: List<TajweedSegment>,
    fontFamily: FontFamily,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
    onSegmentClick: (ruleClass: String?, offset: Int) -> Unit
) {
    val defaultColor = textStyle.color.takeIf { it != Color.Unspecified } ?: LocalContentColor.current

    val annotatedString = buildAnnotatedString {
        if (segments.isEmpty()) {
            append(text)
        } else {
            val sortedSegments = segments.sortedBy { it.start }
            var cursor = 0

            for (seg in sortedSegments) {
                if (seg.start > cursor) {
                    append(text.substring(cursor, seg.start))
                }
                
                val startIdx = length
                append(text.substring(seg.start, seg.end))
                val endIdx = length

                // Apply style
                addStyle(
                    style = SpanStyle(color = Color(android.graphics.Color.parseColor(seg.colorHex))),
                    start = startIdx,
                    end = endIdx
                )

                // Attach metadata annotation for clickable tooltips
                seg.ruleClass?.let { rule ->
                    addStringAnnotation(
                        tag = "TAJWEED_RULE",
                        annotation = rule,
                        start = startIdx,
                        end = endIdx
                    )
                }

                cursor = seg.end
            }

            if (cursor < text.length) {
                append(text.substring(cursor))
            }
        }
    }

    ClickableText(
        text = annotatedString,
        modifier = modifier,
        style = textStyle.copy(fontFamily = fontFamily),
        onClick = { offset ->
            val annotation = annotatedString.getStringAnnotations(
                tag = "TAJWEED_RULE",
                start = offset,
                end = offset
            ).firstOrNull()
            
            onSegmentClick(annotation?.item, offset)
        }
    )
}
