package com.nur.quran.ui.components

import android.content.Context
import android.content.ContextWrapper
import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nur.quran.R
import com.nur.quran.ui.screens.fontFamilyArabic
import com.nur.quran.ui.screens.fontFamilyBody
import com.nur.quran.ui.screens.fontFamilyMono
import com.nur.quran.ui.screens.hGold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

// Fixed (theme-independent) dark palette so the exported PNG looks identical
// in light and dark mode. Gold/mono accents mirror ShareVerseDialog preview.
private val ShareCardBg = Color(0xFF16211F)
private val ShareCardInnerBg = Color(0xFF20302C)
private val ShareCardCream = Color(0xFFF7F2E7)
private val ShareCardMuted = Color(0xFF9DB3AD)

private const val SHARE_FOOTER_URL = "quran-nur.appwrite.network"

/**
 * Pure presentational verse card (no hooks: no remember/effects/context reads).
 * Dark branded card mirroring the ShareVerseDialog preview: logo + "Quran Nur",
 * Bismillah header, Arabic (RTL), translation, mono gold reference, footer URL.
 *
 * The 1000.dp max width gives a 1080px-equivalent export layout when rendered
 * via [renderVerseCardToFile] (measured EXACTLY 1080px wide, height wrapped).
 */
@Composable
fun VerseShareCard(
    verseKey: String,
    arabic: String,
    translation: String?,
    reference: String,
    modifier: Modifier = Modifier
) {
    val cleanTranslation = translation?.replace(Regex("<[^>]*>"), "")?.trim().orEmpty()
    val refLine = if (reference.isBlank()) verseKey else reference

    Column(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 1000.dp)
            .background(ShareCardBg)
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Brand row: logo + app name.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_logo),
                contentDescription = null,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
            )
            Text(
                text = "Quran Nur",
                fontFamily = fontFamilyMono,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = hGold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Bismillah header.
        Text(
            text = "﷽",
            fontSize = 32.sp,
            color = hGold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Inner bordered card with the verse content.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 2.dp,
                    color = hGold.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(24.dp)
                )
                .background(ShareCardInnerBg, RoundedCornerShape(24.dp))
                .padding(horizontal = 40.dp, vertical = 44.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Text(
                    text = arabic,
                    fontFamily = fontFamilyArabic,
                    fontSize = 40.sp,
                    lineHeight = 68.sp,
                    color = ShareCardCream,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (cleanTranslation.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "\"$cleanTranslation\"",
                    fontFamily = fontFamilyBody,
                    fontSize = 20.sp,
                    lineHeight = 32.sp,
                    fontStyle = FontStyle.Italic,
                    color = Color.White.copy(alpha = 0.78f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "— $refLine",
                fontFamily = fontFamilyMono,
                fontSize = 16.sp,
                letterSpacing = 1.sp,
                color = hGold,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = SHARE_FOOTER_URL,
            fontFamily = fontFamilyMono,
            fontSize = 14.sp,
            letterSpacing = 1.sp,
            color = ShareCardMuted,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Renders [VerseShareCard] off-screen to a PNG under
 * `<cacheDir>/shared/verse-<verseKey>.png` (':' sanitized to '-').
 *
 * Measure/layout run on Dispatchers.Main AFTER awaiting composed frames:
 * [ComposeView.setContent] schedules composition asynchronously, so measuring
 * immediately would capture a blank (often 1px) frame — the #1 cause of
 * broken share images. We await 3 frames, then validate the measured height.
 * PNG compression runs on Dispatchers.IO.
 * Never throws: returns null on any failure (logged under "ShareDebug").
 *
 * Serve the returned file via FileProvider authority
 * `"${context.packageName}.fileprovider"` (see AndroidManifest + xml/file_paths).
 */
private const val ShareDebugTag = "ShareDebug"

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

suspend fun renderVerseCardToFile(
    context: Context,
    verseKey: String,
    arabic: String,
    translation: String?,
    reference: String
): File? = withContext(Dispatchers.IO) {
    runCatching {
        // Off-screen ComposeViews have no window and therefore no recomposer
        // ("Cannot locate windowRecomposer") — composition never runs and the
        // capture is blank. Attach invisibly to the Activity decor so the
        // window recomposer drives composition, then detach after draw.
        val activity = context.findActivity()
            ?: throw IllegalStateException("share render needs an Activity context")
        val bitmap = withContext(AndroidUiDispatcher.Main) {
            val composeView = ComposeView(activity)
            composeView.visibility = View.INVISIBLE
            val decor = activity.window.decorView as? ViewGroup
                ?: throw IllegalStateException("no decor view")
            decor.addView(
                composeView,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
            try {
                composeView.setContent {
                    VerseShareCard(
                        verseKey = verseKey,
                        arabic = arabic,
                        translation = translation,
                        reference = reference
                    )
                }
                // Let composition settle before measuring (see KDoc). Frame
                // callbacks need AndroidUiDispatcher — plain Dispatchers.Main
                // carries no MonotonicFrameClock.
                repeat(3) { withFrameNanos { } }
                val widthSpec = View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY)
                val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                composeView.measure(widthSpec, heightSpec)
                val height = composeView.measuredHeight
                Log.d(ShareDebugTag, "render $verseKey measured=${1080}x$height")
                require(height > 100) { "composed height too small: $height" }
                val capped = height.coerceAtMost(1920)
                composeView.layout(0, 0, 1080, capped)
                // One more frame so layout changes settle before draw.
                withFrameNanos { }
                val bmp = Bitmap.createBitmap(1080, capped, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bmp)
                composeView.draw(canvas)
                bmp
            } finally {
                runCatching { decor.removeView(composeView) }
            }
        }
        val safeKey = verseKey.replace(":", "-")
        val dir = File(context.cacheDir, "shared")
        dir.mkdirs()
        val outFile = File(dir, "verse-$safeKey.png")
        FileOutputStream(outFile).use { fos ->
            val ok = bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            fos.flush()
            require(ok) { "PNG compress failed" }
        }
        Log.d(ShareDebugTag, "render $verseKey saved ${outFile.length()} bytes")
        outFile
    }.onFailure {
        Log.e(ShareDebugTag, "render $verseKey failed", it)
    }.getOrNull()
}
