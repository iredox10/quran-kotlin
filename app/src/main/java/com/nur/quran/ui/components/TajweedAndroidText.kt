package com.nur.quran.ui.components

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.res.ResourcesCompat
import com.nur.quran.R
import kotlin.math.abs

/**
 * Renders tajweed-colored Arabic text in a native TextView.
 *
 * Unlike Compose's span-based text, TextView lays out the whole word as a
 * single shaping run first and paints per-span colors afterwards, so
 * per-letter ForegroundColorSpan boundaries do not break Arabic cursive
 * joins / diacritic placement. Tap targets are attached as ClickableSpans.
 */
@Composable
fun TajweedAndroidText(
    text: String,
    wordRanges: List<Pair<IntRange, Int>>,
    segments: List<TajweedSegment>,
    selectedArabicFontName: String,
    fontSizeSp: Float,
    lineHeightRatio: Float,
    textColor: Color,
    isInteractive: Boolean,
    onWordClick: (Int) -> Unit,
    onTajweedClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    textAlign: Int = Gravity.RIGHT
) {
    val context = LocalContext.current
    val typeface = remember(selectedArabicFontName) {
        arabicTypeface(context, selectedArabicFontName)
    }
    val spannable = remember(text, wordRanges, segments, isInteractive, textColor) {
        buildTajweedSpannable(
            text = text,
            wordRanges = wordRanges,
            segments = segments,
            textColor = textColor,
            isInteractive = isInteractive,
            onWordClick = onWordClick,
            onTajweedClick = onTajweedClick
        )
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            TextView(ctx).apply {
                textSize = fontSizeSp
                includeFontPadding = false
                setLineSpacing(0f, lineHeightRatio)
                layoutDirection = View.LAYOUT_DIRECTION_RTL
                textDirection = View.TEXT_DIRECTION_RTL
                gravity = textAlign or Gravity.CENTER_VERTICAL
                this.typeface = typeface
                highlightColor = android.graphics.Color.TRANSPARENT
                if (isInteractive) {
                    setOnTouchListener(TajweedTouchListener())
                }
            }
        },
        update = { tv ->
            tv.textSize = fontSizeSp
            tv.setLineSpacing(0f, lineHeightRatio)
            tv.typeface = typeface
            tv.text = spannable
        }
    )
}

private fun arabicTypeface(context: Context, fontName: String): Typeface {
    val resId = when (fontName.trim().lowercase()) {
        "kfgqpc-hafs", "kfgqpc hafs" -> R.font.kfgqpc_hafs
        "uthman-taha-naskh", "uthman taha naskh" -> R.font.uthman_taha_naskh
        "amiri-quran", "amiri quran" -> R.font.amiri_regular
        "noto-naskh-arabic", "noto naskh arabic" -> R.font.noto_regular
        "scheherazade-new", "scheherazade new" -> R.font.scheherazade_regular
        else -> 0
    }
    return if (resId != 0) {
        ResourcesCompat.getFont(context, resId) ?: Typeface.DEFAULT
    } else {
        Typeface.DEFAULT
    }
}

private fun buildTajweedSpannable(
    text: String,
    wordRanges: List<Pair<IntRange, Int>>,
    segments: List<TajweedSegment>,
    textColor: Color,
    isInteractive: Boolean,
    onWordClick: (Int) -> Unit,
    onTajweedClick: (String) -> Unit
): SpannableString {
    val sp = SpannableString(text)
    val baseArgb = textColor.toArgb()

    val sorted = segments.sortedBy { it.start }
    for (seg in sorted) {
        val start = seg.start.coerceIn(0, text.length)
        val end = seg.end.coerceIn(start, text.length)
        if (end <= start) continue
        val isEndRule = seg.ruleClass == "end"
        val colorArgb = try {
            android.graphics.Color.parseColor(if (isEndRule) "#B8924A" else seg.colorHex)
        } catch (t: Throwable) {
            baseArgb
        }
        sp.setSpan(ForegroundColorSpan(colorArgb), start, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        val rule = seg.ruleClass
        if (isInteractive && !rule.isNullOrBlank() && !isEndRule) {
            sp.setSpan(
                TajweedClickableSpan(colorArgb, underline = false) { onTajweedClick(rule) },
                start, end,
                android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    if (isInteractive) {
        for ((range, wordIndex) in wordRanges) {
            val start = range.first.coerceIn(0, text.length)
            val end = range.last.coerceIn(start, text.length)
            if (end <= start) continue
            // Word taps lose to tajweed-rule taps on overlapping positions
            val overlapsRule = sorted.any { seg ->
                (seg.ruleClass != null && seg.ruleClass != "end") &&
                    seg.start < end && seg.end > start
            }
            if (overlapsRule) continue
            sp.setSpan(
                TajweedClickableSpan(baseArgb, underline = false) { onWordClick(wordIndex) },
                start, end,
                android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    return sp
}

private class TajweedClickableSpan(
    private val spanColor: Int,
    private val underline: Boolean,
    private val onClick: () -> Unit
) : ClickableSpan() {
    override fun onClick(widget: View) {
        onClick()
    }

    override fun updateDrawState(ds: TextPaint) {
        super.updateDrawState(ds)
        ds.color = spanColor
        ds.isUnderlineText = underline
    }
}

/**
 * Touch listener that differentiates between a single tap (to trigger ClickableSpans)
 * and vertical scroll gestures, ensuring LazyColumn scrolling is never hijacked.
 */
private class TajweedTouchListener : View.OnTouchListener {
    private var startX = 0f
    private var startY = 0f
    private var isTap = false

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        val tv = v as? TextView ?: return false
        val spanned = tv.text as? Spanned ?: return false

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.x
                startY = event.y
                isTap = true
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = abs(event.x - startX)
                val dy = abs(event.y - startY)
                val slop = ViewConfiguration.get(tv.context).scaledTouchSlop
                if (dx > slop || dy > slop) {
                    isTap = false
                }
            }
            MotionEvent.ACTION_UP -> {
                if (isTap) {
                    val x = event.x.toInt() - tv.totalPaddingLeft + tv.scrollX
                    val y = event.y.toInt() - tv.totalPaddingTop + tv.scrollY
                    val layout = tv.layout
                    if (layout != null) {
                        val line = layout.getLineForVertical(y)
                        val offset = layout.getOffsetForHorizontal(line, x.toFloat())
                        val spans = spanned.getSpans(offset, offset, ClickableSpan::class.java)
                        if (spans.isNotEmpty()) {
                            spans[0].onClick(tv)
                            return true
                        }
                    }
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                isTap = false
            }
        }
        return false
    }
}
