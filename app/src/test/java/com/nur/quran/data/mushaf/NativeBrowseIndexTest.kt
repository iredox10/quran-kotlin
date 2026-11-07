package com.nur.quran.data.mushaf

import com.google.gson.Gson
import com.google.gson.JsonParser
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class NativeBrowseIndexTest {
    @Test fun eachEditionUsesItsOwnCompletePageAndDivisionIndex() {
        val root = sequenceOf(File("src/main/assets/data/mushafs"), File("app/src/main/assets/data/mushafs")).first { it.isDirectory }
        for (edition in listOf("warsh", "qaloon", "indopak")) {
            val json = File(root, "${edition}_browse.json").readText()
            val index = Gson().fromJson(json, NativeBrowseIndex::class.java)
            assertEquals((1..114).toList(), index.chapters.map { it.id })
            assertEquals((1..30).toList(), index.juz.map { it.id })
            assertEquals((1..60).toList(), index.hizb.map { it.id })
            for (divisions in listOf(index.juz, index.hizb)) {
                assertEquals(divisions.map { it.page }.sorted(), divisions.map { it.page })
                assertTrue(divisions.all { it.page in index.firstPage..index.lastPage && it.surah in 1..114 })
            }
            if (edition == "indopak") {
                assertEquals(2, index.firstPage)
                assertEquals(848, index.lastPage)
                assertEquals(16, index.hizb[1].page) // Original PDF's Juz 01 half bookmark, not Hafs page 11.
            } else {
                assertEquals(6214, index.chapters.sumOf { it.count })
                val source = JsonParser.parseString(File(root, "$edition.json").readText()).asJsonArray
                    .associateBy { it.asJsonObject.get("id").asInt }
                val boundaries = JsonParser.parseString(json).asJsonObject.getAsJsonArray("hizb")
                for (boundary in boundaries) {
                    val item = boundary.asJsonObject
                    val row = source.getValue(item.get("nativeAyahId").asInt).asJsonObject
                    assertEquals(row.get("sura_no").asInt, item.get("surah").asInt)
                    assertEquals(Regex("\\d+").find(row.get("page").asString)!!.value.toInt(), item.get("page").asInt)
                    assertTrue(item.get("anchor").asString.isNotBlank())
                }
            }
        }
    }
}
