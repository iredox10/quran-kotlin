package com.nur.quran.ui.components

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/** Quran page order: a deliberate right swipe advances; vertical scrolling stays independent. */
@Composable
internal fun Modifier.mushafPageSwipe(enabled: Boolean = true, onNext: () -> Unit, onPrevious: () -> Unit): Modifier {
    val next = rememberUpdatedState(onNext)
    val previous = rememberUpdatedState(onPrevious)
    val threshold = with(LocalDensity.current) { 56.dp.toPx() }
    return if (!enabled) this else pointerInput(threshold) {
        var distance = 0f
        detectHorizontalDragGestures(
            onDragStart = { distance = 0f },
            onHorizontalDrag = { change, amount -> change.consume(); distance += amount },
            onDragCancel = { distance = 0f },
            onDragEnd = {
                if (distance > threshold) next.value()
                else if (distance < -threshold) previous.value()
                distance = 0f
            }
        )
    }
}
