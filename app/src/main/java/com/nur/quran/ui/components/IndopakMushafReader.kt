package com.nur.quran.ui.components

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nur.quran.R
import com.nur.quran.data.mushaf.IndopakEdition
import com.nur.quran.ui.screens.*
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Printed IndoPak pages keep their original type, marks, and thirteen-line layout. */
@Composable
fun IndopakMushafReader(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("NarrationReader", 0) }
    var page by remember { mutableIntStateOf(prefs.getInt("indopak", IndopakEdition.FIRST_PAGE).coerceIn(IndopakEdition.FIRST_PAGE, IndopakEdition.LAST_PAGE)) }
    var renderedPage by remember { mutableIntStateOf(page) }
    var pageCount by remember { mutableIntStateOf(0) }
    var image by remember { mutableStateOf<Bitmap?>(null) }
    var error by remember { mutableStateOf(false) }
    var controls by remember { mutableStateOf(false) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var jump by remember { mutableStateOf(false) }
    val pages = remember { android.util.LruCache<Int, Bitmap>(3) }
    val vertical = rememberScrollState()
    val horizontal = rememberScrollState()
    LaunchedEffect(page) {
        image = pages.get(page) ?: image
        error = false
        vertical.scrollTo(0)
        horizontal.scrollTo(0)
        try {
            val result = withContext(Dispatchers.IO) {
                pages.get(page)?.let { Triple(it, pageCount, page) }
                    ?: renderIndopakPage(context, page).also { pages.put(it.third, it.first) }
            }
            image = result.first
            renderedPage = result.third
            pageCount = result.second
            page = result.third
            prefs.edit().putInt("indopak", page).apply()
            withContext(Dispatchers.IO) {
                for (nearby in listOf(page + 1, page - 1)) {
                    kotlinx.coroutines.currentCoroutineContext().ensureActive()
                    if (nearby in IndopakEdition.FIRST_PAGE..pageCount && pages.get(nearby) == null) {
                        val prepared = renderIndopakPage(context, nearby)
                        pages.put(nearby, prepared.first)
                    }
                }
            }
        } catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
        catch (failure: Exception) { android.util.Log.e("IndopakReader", "Local page $page could not be rendered", failure); error = true }
    }
    if (jump && pageCount > 0) PageJumpDialog(page, IndopakEdition.FIRST_PAGE, pageCount, { page = it; jump = false }, { jump = false })
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        val view = LocalView.current
        DisposableEffect(view) {
            val window = (view.parent as? DialogWindowProvider)?.window
            val controller = window?.let { WindowCompat.getInsetsController(it, view) }
            controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            fun enterReadingMode() {
                window?.let { WindowCompat.setDecorFitsSystemWindows(it, false) }
                controller?.hide(WindowInsetsCompat.Type.systemBars())
            }
            val focus = android.view.ViewTreeObserver.OnWindowFocusChangeListener { focused -> if (focused) enterReadingMode() }
            view.viewTreeObserver.addOnWindowFocusChangeListener(focus)
            // Dialog.show() runs after composition; apply immersive flags once its window is attached.
            view.post { enterReadingMode() }
            onDispose {
                view.viewTreeObserver.removeOnWindowFocusChangeListener(focus)
                controller?.show(WindowInsetsCompat.Type.systemBars())
            }
        }
        Surface(Modifier.fillMaxSize(), color = androidx.compose.ui.graphics.Color.White) {
            BoxWithConstraints(Modifier.fillMaxSize().mushafPageSwipe(
                enabled = zoom == 1f && !jump,
                onNext = { if (image != null && page < IndopakEdition.LAST_PAGE) page++ },
                onPrevious = { if (image != null && page > IndopakEdition.FIRST_PAGE) page-- }
            )) {
                val viewportWidth = maxWidth
                val viewportHeight = maxHeight
                val bitmap = image
                if (error) {
                    Text(stringResource(R.string.indopak_open_error), color = hInk, modifier = Modifier.align(Alignment.Center))
                } else if (bitmap != null) {
                    val tap = Modifier.pointerInput(Unit) { detectTapGestures(onTap = { controls = !controls }) }
                    if (zoom == 1f) {
                        Image(bitmap.asImageBitmap(), "IndoPak · $renderedPage",
                            Modifier.fillMaxSize().then(tap), contentScale = ContentScale.Fit)
                    } else {
                        Box(Modifier.fillMaxSize().verticalScroll(vertical).horizontalScroll(horizontal)) {
                            Image(bitmap.asImageBitmap(), "IndoPak · $renderedPage",
                                Modifier.requiredSize(viewportWidth * zoom, viewportHeight * zoom).then(tap), contentScale = ContentScale.Fit)
                        }
                    }
                } else CircularProgressIndicator(color = hGold, modifier = Modifier.align(Alignment.Center))
                if (controls) Surface(color = hCream.copy(alpha = .97f), modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth()) {
                    Row(Modifier.safeDrawingPadding(), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onDismiss) { Icon(NurIcons.ArrowLeft, stringResource(R.string.planner_back), tint = hInk) }
                        TextButton(onClick = { jump = true }) { Text("$renderedPage / ${IndopakEdition.LAST_PAGE}", color = hInk) }
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = { zoom = (zoom - .25f).coerceAtLeast(1f) }) { Text("A−", color = hInk) }
                        TextButton(onClick = { zoom = (zoom + .25f).coerceAtMost(3f) }) { Text("A+", color = hInk) }
                    }
                }
            }
        }
    }
}

private fun renderIndopakPage(context: android.content.Context, requested: Int): Triple<Bitmap, Int, Int> {
    return ParcelFileDescriptor.open(IndopakEdition.file(context), ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
        PdfRenderer(descriptor).use { renderer ->
            check(renderer.pageCount == 874)
            val number = requested.coerceIn(IndopakEdition.FIRST_PAGE, IndopakEdition.LAST_PAGE)
            val bitmap = renderer.openPage(number + 1).use { pdfPage ->
                val width = 1600
                Bitmap.createBitmap(width, (width.toFloat() * pdfPage.height / pdfPage.width).toInt(), Bitmap.Config.ARGB_8888).also {
                    it.eraseColor(android.graphics.Color.WHITE)
                    pdfPage.render(it, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                }
            }
            val trimmed = trimPrintedPageMargins(bitmap)
            if (trimmed !== bitmap) bitmap.recycle()
            Triple(trimmed, IndopakEdition.LAST_PAGE, number)
        }
    }
}

/** Remove only blank scanner margins, retaining every printed letter and annotation. */
private fun trimPrintedPageMargins(bitmap: Bitmap): Bitmap {
    val width = bitmap.width
    val pixels = IntArray(width * bitmap.height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, bitmap.height)
    var left = width
    var right = -1
    var top = bitmap.height
    var bottom = -1
    // The original PDF has a navigation-link arrow above its printed page.
    for (y in (bitmap.height * .04f).toInt() until (bitmap.height * .96f).toInt()) {
        for (x in 0 until width) {
            val color = pixels[y * width + x]
            if ((color shr 16 and 255) < 210 && (color shr 8 and 255) < 210 && (color and 255) < 210) {
                left = minOf(left, x); right = maxOf(right, x)
                top = minOf(top, y); bottom = maxOf(bottom, y)
            }
        }
    }
    if (right < left) return bitmap
    left = (left - 16).coerceAtLeast(0); top = (top - 16).coerceAtLeast(0)
    right = (right + 16).coerceAtMost(width - 1); bottom = (bottom + 16).coerceAtMost(bitmap.height - 1)
    return Bitmap.createBitmap(bitmap, left, top, right - left + 1, bottom - top + 1)
}
