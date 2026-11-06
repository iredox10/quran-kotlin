package com.nur.quran.utils

/** Opaque reader surfaces and hue-preserving tajweed colors for dark mode. */
object DarkReaderPalette {
    const val BACKGROUND = "#101415"
    const val SURFACE = "#191F20"
    val tajweed = mapOf(
        "#AAAAAA" to "#BAC4C8", "#A1A1A1" to "#BAC4C8",
        "#537FFF" to "#82A4FF", "#4050FF" to "#A5A0FF",
        "#000FB5" to "#B1B8FF", "#2142C7" to "#92ACFF",
        "#DD0008" to "#FF7F89", "#D500B7" to "#F68DE5",
        "#26BFFD" to "#63D9FF", "#169777" to "#5DD8BA",
        "#169200" to "#73D461", "#FF7E1E" to "#FFB36B",
        "#B8924A" to "#D6B783"
    )
    fun color(hex: String, dark: Boolean): String = if (dark) tajweed[hex.uppercase()] ?: hex else hex
}
