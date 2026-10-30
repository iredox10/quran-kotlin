package com.nur.quran.i18n

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

/** Localization catalogs must be complete and remain safe for String.format after translation. */
class LocalizationContractTest {
    private val resources = File("src/main/res")
    private val placeholders = Regex("""%(\d+)\$([a-zA-Z])""")

    @Test fun `format audit recognizes Android positional arguments`() {
        val sample = "%1\$s · %2\$d"
        assertEquals(listOf("1" to "s", "2" to "d"), placeholders.findAll(sample).map {
            it.groupValues[1] to it.groupValues[2]
        }.toList())
    }

    private fun parseStrings(directory: File): Map<String, String> {
        val strings = linkedMapOf<String, String>()
        directory.listFiles { file -> file.extension == "xml" }.orEmpty().sortedBy { it.name }.forEach { file ->
            val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
            val rows = document.getElementsByTagName("string")
            for (index in 0 until rows.length) {
                val row = rows.item(index) as Element
                val key = row.getAttribute("name")
                assertFalse("Duplicate string '$key' in ${directory.name}", strings.containsKey(key))
                strings[key] = row.textContent
            }
        }
        return strings
    }

    private fun configuredLanguages(): List<String> {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File(resources, "xml/locales_config.xml"))
        val rows = document.getElementsByTagName("locale")
        return (0 until rows.length).map { (rows.item(it) as Element).getAttribute("android:name") }
    }

    private fun localeDirectories(code: String): List<File> {
        val locale = Locale.forLanguageTag(code)
        return listOf(
            "values-${locale.language}", "values-${mapOf("id" to "in", "he" to "iw", "yi" to "ji")[locale.language] ?: locale.language}", "values-$code", "values-${locale.language}-r${locale.country}",
            "values-b+${code.replace('-', '+')}"
        ).distinct().map { File(resources, it) }.filter { it.isDirectory }
    }

    @Test fun `every configured interface language includes all interface messages with matching arguments`() {
        val base = parseStrings(File(resources, "values"))
        assertTrue("Interface catalog should cover all major screens", base.size >= 800)
        val codes = configuredLanguages()
        assertTrue(codes.containsAll(listOf("en", "ar")))
        assertEquals("Locale selector must not advertise duplicate languages", codes.size, codes.distinct().size)
        codes.filter { it != "en" }.forEach { code ->
            val localized = localeDirectories(code).fold(emptyMap<String, String>()) { result, dir -> result + parseStrings(dir) }
            assertTrue("Missing planner strings for $code: ${base.keys - localized.keys}", localized.keys.containsAll(base.keys))
            base.forEach { (key, english) ->
                val translated = localized.getValue(key)
                assertTrue("Blank $code translation for $key", translated.isNotBlank())
                assertFalse("Generator token leaked into $code/$key", translated.contains("ZXQPLACE"))
                assertFalse("Batch label leaked into $code/$key", Regex("""[\[【]\d{4}[\]】]""").containsMatchIn(translated))
                val expected = placeholders.findAll(english).map { it.groupValues[1] to it.groupValues[2] }.toList().sortedBy { it.first }
                val actual = placeholders.findAll(translated).map { it.groupValues[1] to it.groupValues[2] }.toList().sortedBy { it.first }
                assertEquals("Translated format parameters changed: $code/$key", expected, actual)
            }
        }
    }

    @Test fun `language selector codes exactly match Android per-app language configuration`() {
        assertEquals(configuredLanguages().toSet(), AppLanguages.supported.map { it.code }.filter(String::isNotBlank).toSet())
    }
}
