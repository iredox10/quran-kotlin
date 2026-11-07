package com.nur.quran.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.view.Window
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import com.nur.quran.ui.screens.isDarkThemeGlobal

/** The system bar is part of the reader surface, rather than a separate green brand stripe. */
@Suppress("DEPRECATION")
internal fun Window.applyReaderSystemBars(dark: Boolean) {
    val background = if (dark) 0xFF101415.toInt() else 0xFFFAF7F0.toInt()
    statusBarColor = background
    navigationBarColor = background
    if (Build.VERSION.SDK_INT >= 28) navigationBarDividerColor = background
    if (Build.VERSION.SDK_INT >= 29) {
        isStatusBarContrastEnforced = false
        isNavigationBarContrastEnforced = false
    }
    WindowCompat.getInsetsController(this, decorView).apply {
        isAppearanceLightStatusBars = !dark
        isAppearanceLightNavigationBars = !dark
    }
}

@Composable
internal fun ReaderSystemBars(dark: Boolean = isDarkThemeGlobal) {
    val view = LocalView.current
    val window = (view.parent as? DialogWindowProvider)?.window ?: view.context.activity()?.window
    SideEffect { window?.applyReaderSystemBars(dark) }
}

private tailrec fun Context.activity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> if (baseContext !== this) baseContext.activity() else null
    else -> null
}
