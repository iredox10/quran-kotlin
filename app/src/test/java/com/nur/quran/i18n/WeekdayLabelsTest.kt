package com.nur.quran.i18n

import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

class WeekdayLabelsTest {
    @Test fun `stable English identifiers are displayed using selected locale weekday names`() {
        assertEquals("sam.", weekdayLabel("Sat", Locale.FRENCH))
        assertEquals("dim.", weekdayLabel("Sun", Locale.FRENCH))
        assertEquals("Sat", weekdayLabel("Sat", Locale.US))
        assertNotEquals("Sat", weekdayLabel("Sat", Locale.forLanguageTag("ar")))
    }

    @Test fun `unknown labels are not silently reinterpreted as dates or identifiers`() {
        assertEquals("custom label", weekdayLabel("custom label", Locale.FRENCH))
    }
}
