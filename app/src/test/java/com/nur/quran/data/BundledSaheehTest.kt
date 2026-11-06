package com.nur.quran.data

import com.google.gson.JsonParser
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class BundledSaheehTest {
    @Test fun bundledEditionHasEveryCanonicalAyahAndSaheehOpening() {
        val root = sequenceOf(File("src/main/assets/data"), File("app/src/main/assets/data")).first { it.isDirectory }
        val translations = File(root, "translation_saheeh.json").reader().use { JsonParser.parseReader(it).asJsonObject }
        val chapters = File(root, "chapters.json").reader().use { JsonParser.parseReader(it).asJsonArray }
        val expected = chapters.flatMap { row ->
            val chapter = row.asJsonObject
            (1..chapter["verses_count"].asInt).map { "${chapter["id"].asInt}:$it" }
        }.toSet()
        assertEquals(6236, translations.size())
        assertEquals(expected, translations.keySet())
        assertTrue(translations.entrySet().all { it.value.asString.isNotBlank() })
        assertTrue(translations["1:1"].asString.contains("Entirely Merciful"))
    }
}
