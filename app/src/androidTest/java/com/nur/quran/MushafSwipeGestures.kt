package com.nur.quran

import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import androidx.test.platform.app.InstrumentationRegistry

internal fun swipeMushafPage(right: Boolean) {
    val ui = InstrumentationRegistry.getInstrumentation().uiAutomation
    val shot = ui.takeScreenshot()
    val width = shot.width.toFloat()
    val y = shot.height * .5f
    shot.recycle()
    val start = if (right) width * .2f else width * .8f
    val end = width - start
    val down = SystemClock.uptimeMillis()
    fun send(action: Int, x: Float) {
        MotionEvent.obtain(down, SystemClock.uptimeMillis(), action, x, y, 0).apply {
            source = InputDevice.SOURCE_TOUCHSCREEN
            ui.injectInputEvent(this, true)
            recycle()
        }
    }
    send(MotionEvent.ACTION_DOWN, start)
    for (step in 1..16) {
        SystemClock.sleep(16)
        send(MotionEvent.ACTION_MOVE, start + (end - start) * step / 16)
    }
    send(MotionEvent.ACTION_UP, end)
    InstrumentationRegistry.getInstrumentation().waitForIdleSync()
}

internal fun tapMushafPage() {
    val ui = InstrumentationRegistry.getInstrumentation().uiAutomation
    val shot = ui.takeScreenshot()
    val x = shot.width * .5f
    val y = shot.height * .5f
    shot.recycle()
    val down = SystemClock.uptimeMillis()
    for (action in listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP)) {
        MotionEvent.obtain(down, SystemClock.uptimeMillis(), action, x, y, 0).apply {
            source = InputDevice.SOURCE_TOUCHSCREEN
            ui.injectInputEvent(this, true)
            recycle()
        }
        SystemClock.sleep(30)
    }
    InstrumentationRegistry.getInstrumentation().waitForIdleSync()
}
