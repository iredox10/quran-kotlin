package com.nur.quran.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import androidx.core.content.res.ResourcesCompat
import com.nur.quran.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Export coordinates are pixels, independent of screen density and accessibility font scale. */
enum class ShareCardTheme(val label: String, val background: Int, val ink: Int, val gold: Int) {
    Ivory("Ivory Gold", 0xFFFAF6ED.toInt(), 0xFF123D32.toInt(), 0xFFAA8032.toInt()),
    Emerald("Emerald", 0xFF103B30.toInt(), 0xFFF9F3E6.toInt(), 0xFFD2B16D.toInt()),
    Obsidian("Obsidian", 0xFF1B1D20.toInt(), 0xFFF9F3E6.toInt(), 0xFFC9AD75.toInt())
}

enum class ShareCardFormat(val label: String, val height: Int) {
    Square("1:1", 1080), Portrait("9:16", 1920)
}

data class VerseCardPage(val arabic: String, val translation: String)

private const val CARD_WIDTH = 1080
private const val TEXT_WIDTH = 864
private const val ARABIC_SIZE = 96f
private const val TRANSLATION_SIZE = 44f

private fun arabicPaint(context: Context, color: Int) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
    typeface = ResourcesCompat.getFont(context, R.font.kfgqpc_hafs) ?: Typeface.DEFAULT
    textSize = ARABIC_SIZE
    this.color = color
}

private fun translationPaint(color: Int) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
    typeface = Typeface.create("serif", Typeface.ITALIC)
    textSize = TRANSLATION_SIZE
    this.color = color
}

private fun textLayout(text: String, paint: TextPaint, arabic: Boolean): StaticLayout =
    StaticLayout.Builder.obtain(text, 0, text.length, paint, TEXT_WIDTH)
        .setAlignment(Layout.Alignment.ALIGN_CENTER)
        .setTextDirection(if (arabic) TextDirectionHeuristics.RTL else TextDirectionHeuristics.FIRSTSTRONG_LTR)
        .setIncludePad(true)
        .setLineSpacing(if (arabic) 26f else 12f, 1f)
        .build()

/** A single growing canvas keeps the complete ayah and translation together at readable sizes. */
@Suppress("UNUSED_PARAMETER")
fun paginateVerseCard(context: Context, arabic: String, translation: String?, format: ShareCardFormat = ShareCardFormat.Square): List<VerseCardPage> =
    listOf(VerseCardPage(arabic, translation.orEmpty()))

fun verseCardContentHeight(context: Context, page: VerseCardPage): Int {
    val arabicHeight = if (page.arabic.isEmpty()) 0 else textLayout(page.arabic, arabicPaint(context, android.graphics.Color.BLACK), true).height
    val translationHeight = if (page.translation.isEmpty()) 0 else textLayout(page.translation, translationPaint(android.graphics.Color.BLACK), false).height
    return arabicHeight + translationHeight + if (arabicHeight > 0 && translationHeight > 0) 52 else 0
}

private fun hasBismillahHeader(page: VerseCardPage): Boolean {
    val letters = page.arabic.replace('ٱ', 'ا').filter { it in '\u0621'..'\u064A' && it != 'ـ' }
    return letters != "بسماللهالرحمنالرحيم"
}

fun verseCardHeight(context: Context, page: VerseCardPage, format: ShareCardFormat = ShareCardFormat.Square): Int =
    maxOf(format.height, verseCardContentHeight(context, page) + 320 + if (hasBismillahHeader(page)) 80 else 0)

/** Preview and export use the same pixel layout; only the optional output resolution differs. */
@Suppress("UNUSED_PARAMETER")
fun renderVerseCardBitmap(
    context: Context,
    page: VerseCardPage,
    reference: String,
    theme: ShareCardTheme = ShareCardTheme.Ivory,
    format: ShareCardFormat = ShareCardFormat.Square,
    pageNumber: Int = 1,
    pageCount: Int = 1,
    outputWidth: Int = CARD_WIDTH
): Bitmap {
    val height = verseCardHeight(context, page, format)
    val scale = outputWidth.toFloat() / CARD_WIDTH
    val bitmap = Bitmap.createBitmap(outputWidth, kotlin.math.ceil(height * scale).toInt(), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.scale(scale, scale)
    canvas.drawColor(theme.background)
    val accent = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = theme.gold; strokeWidth = 1.5f; style = Paint.Style.STROKE; alpha = 100 }
    canvas.drawRoundRect(RectF(28f, 28f, 1052f, height - 28f), 28f, 28f, accent)
    val headerHeight = if (hasBismillahHeader(page)) 80 else 0
    if (headerHeight > 0) {
        val bismillahStyle = arabicPaint(context, theme.gold).apply { textSize = 40f }
        val bismillah = textLayout("بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ", bismillahStyle, true)
        canvas.save()
        canvas.translate(108f, 68f)
        bismillah.draw(canvas)
        canvas.restore()
    }
    val arabic = page.arabic.takeIf { it.isNotEmpty() }?.let { textLayout(it, arabicPaint(context, theme.ink), true) }
    val translation = page.translation.takeIf { it.isNotEmpty() }?.let { textLayout(it, translationPaint(theme.ink), false) }
    val gap = if (arabic != null && translation != null) 52 else 0
    val contentHeight = (arabic?.height ?: 0) + (translation?.height ?: 0) + gap
    var top = 96f + headerHeight + (height - 320 - headerHeight - contentHeight) / 2f
    listOfNotNull(arabic, translation).forEachIndexed { index, layout ->
        if (index > 0) top += gap
        canvas.save()
        canvas.translate((CARD_WIDTH - TEXT_WIDTH) / 2f, top)
        layout.draw(canvas)
        canvas.restore()
        top += layout.height
    }
    val label = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = theme.gold; textSize = 29f; typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }
    val referenceLayout = StaticLayout.Builder.obtain(reference, 0, reference.length, label, TEXT_WIDTH)
        .setAlignment(Layout.Alignment.ALIGN_CENTER).setIncludePad(true)
        .setMaxLines(2).setEllipsize(android.text.TextUtils.TruncateAt.END).build()
    canvas.save()
    canvas.translate(108f, height - 164f)
    referenceLayout.draw(canvas)
    canvas.restore()
    label.color = theme.ink
    label.alpha = 160
    label.textSize = 28f
    label.textAlign = Paint.Align.CENTER
    canvas.drawText("Quran Nur", 540f, height - 80f, label)
    return bitmap
}

suspend fun renderVerseCardsToFiles(
    context: Context,
    verseKey: String,
    arabic: String,
    translation: String?,
    reference: String,
    theme: ShareCardTheme = ShareCardTheme.Ivory,
    format: ShareCardFormat = ShareCardFormat.Square
): List<File> = withContext(Dispatchers.IO) {
    val pages = paginateVerseCard(context, arabic, translation, format)
    // Separate exports keep permission-granted files stable while another theme is prepared.
    val directory = File(context.cacheDir, "shared/${java.util.UUID.randomUUID()}").apply { mkdirs() }
    pages.mapIndexed { index, page ->
        val bitmap = renderVerseCardBitmap(context, page, reference, theme, format, index + 1, pages.size)
        try {
            File(directory, "Quran-${verseKey.replace(Regex("[^A-Za-z0-9]+"), "-")}-${index + 1}.png").also { file ->
                file.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            }
        } finally { bitmap.recycle() }
    }
}
