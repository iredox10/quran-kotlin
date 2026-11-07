package com.nur.quran.ui.components

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import com.nur.quran.data.mushaf.IndopakNavigation
import com.nur.quran.data.mushaf.IndopakSurah
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import kotlinx.coroutines.launch
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
fun IndopakMushafReader(initialSurah: Int? = null, initialPage: Int? = null,
    onSelectHafs: (String) -> Unit = {}, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("NarrationReader", 0) }
    var page by rememberSaveable(initialSurah, initialPage) { mutableIntStateOf((initialPage ?: prefs.getInt("indopak", IndopakEdition.FIRST_PAGE)).coerceIn(IndopakEdition.FIRST_PAGE, IndopakEdition.LAST_PAGE)) }
    var renderedPage by remember { mutableIntStateOf(page) }
    var pageCount by remember { mutableIntStateOf(0) }
    var image by remember { mutableStateOf<Bitmap?>(null) }
    var error by remember { mutableStateOf(false) }
    var controls by remember { mutableStateOf(true) }
    var settings by remember { mutableStateOf(false) }
    var surahPicker by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val surahs by produceState<List<IndopakSurah>>(emptyList()) {
        value = withContext(Dispatchers.IO) { IndopakNavigation.surahs(context) }
    }
    var anchorApplied by rememberSaveable(initialSurah) { mutableStateOf(initialSurah == null) }
    LaunchedEffect(surahs, initialSurah) {
        if (!anchorApplied && surahs.isNotEmpty()) {
            val current = surahs.lastOrNull { it.page <= page }?.surah
            if (current != initialSurah) surahs.firstOrNull { it.surah == initialSurah }?.let { page = it.page }
            anchorApplied = true
        }
    }
    var zoom by remember { mutableFloatStateOf(prefs.getFloat("indopak_zoom", 1f).coerceIn(1f, 3f)) }
    var jump by remember { mutableStateOf(false) }
    val pages = remember { android.util.LruCache<Int, Bitmap>(3) }
    val vertical = rememberScrollState()
    val horizontal = rememberScrollState()
    val auto = rememberNativeAutoScroll(page, { vertical.canScrollForward }, { vertical.scrollBy(it) },
        { if (page < IndopakEdition.LAST_PAGE) { page++; true } else false })
    LaunchedEffect(zoom) { prefs.edit().putFloat("indopak_zoom", zoom).apply() }
    val nightFilter = if (isDarkThemeGlobal) ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
        -.8745f, 0f, 0f, 0f, 239f, 0f, -.847f, 0f, 0f, 236f,
        0f, 0f, -.812f, 0f, 228f, 0f, 0f, 0f, 1f, 0f
    ))) else null
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
            prefs.edit().putInt("indopak", page).putLong("indopak_timestamp", System.currentTimeMillis()).apply()
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
    if (settings) NativeReaderSettings(zoom, 1f, 3f, { zoom = it },
        { settings = false; jump = true }, { settings = false }, onSelectHafs)
    if (surahPicker) AlertDialog(onDismissRequest = { surahPicker = false }, containerColor = hCream,
        title = { Text(stringResource(R.string.ui_select_surah_97d36a), color = hInk) },
        text = { LazyColumn(Modifier.heightIn(max = 480.dp)) { items(surahs, key = { it.surah }) { surah ->
            Text("${surah.surah}. ${surah.name}", color = hInk,
                modifier = Modifier.fillMaxWidth().clickable { page = surah.page; surahPicker = false }.padding(vertical = 14.dp))
        } } }, confirmButton = { TextButton(onClick = { surahPicker = false }) { Text(stringResource(R.string.ui_close_bbfa77)) } })
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
        Surface(Modifier.fillMaxSize(), color = hCream) {
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
                            Modifier.fillMaxSize().then(tap), contentScale = ContentScale.Fit, colorFilter = nightFilter)
                    } else {
                        Box(Modifier.fillMaxSize().verticalScroll(vertical).horizontalScroll(horizontal)) {
                            Image(bitmap.asImageBitmap(), "IndoPak · $renderedPage",
                                Modifier.requiredSize(viewportWidth * zoom, viewportHeight * zoom).then(tap), contentScale = ContentScale.Fit, colorFilter = nightFilter)
                        }
                    }
                } else CircularProgressIndicator(color = hGold, modifier = Modifier.align(Alignment.Center))
                if (controls) NativeReaderHeader(surahs.lastOrNull { it.page <= renderedPage }?.name ?: "IndoPak", auto,
                    onDismiss, { auto.paused = auto.active; surahPicker = true }, { controls = false }, { auto.paused = auto.active; settings = true })
                NativeAutoScrollerBar(auto, { scope.launch { vertical.animateScrollTo(0) } },
                    { scope.launch { if (vertical.canScrollForward) vertical.animateScrollTo(vertical.maxValue) else if (page < IndopakEdition.LAST_PAGE) page++ } })
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
