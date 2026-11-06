package com.nur.quran.utils

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.pow

class DarkReaderPaletteTest {
    private fun luminance(hex: String): Double {
        val value = hex.removePrefix("#").toInt(16)
        val channels = listOf(value shr 16 and 255, value shr 8 and 255, value and 255).map {
            val s = it / 255.0
            if (s <= 0.04045) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
        }
        return channels[0] * .2126 + channels[1] * .7152 + channels[2] * .0722
    }
    @Test fun everyTajweedColorIsReadableOnBothDarkSurfaces() {
        for (background in listOf(DarkReaderPalette.BACKGROUND, DarkReaderPalette.SURFACE)) {
            DarkReaderPalette.tajweed.forEach { (original, adjusted) ->
                val ratio = (luminance(adjusted) + .05) / (luminance(background) + .05)
                assertTrue("$original / $adjusted on $background has contrast $ratio", ratio >= 4.5)
                assertEquals(original, DarkReaderPalette.color(original, false))
                assertEquals(adjusted, DarkReaderPalette.color(original.lowercase(), true))
            }
        }
    }
}
