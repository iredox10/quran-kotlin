package com.nur.quran.ui.components

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.delay

/** Due work refreshes on resume, including after device clock/time-zone changes. */
@Composable
internal fun rememberHifdhClock(): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) now = System.currentTimeMillis() }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(lifecycle) {
        while (true) {
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) now = System.currentTimeMillis()
            delay(15_000)
        }
    }
    return now
}
