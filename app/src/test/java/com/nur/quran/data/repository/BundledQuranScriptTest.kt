package com.nur.quran.data.repository

import com.google.gson.Gson
import com.nur.quran.data.api.VersesResponse
import com.nur.quran.data.db.entities.VerseEntity
import com.nur.quran.ui.screens.mushafPlainWordTexts
import java.io.File
import org.junit.Assert.*
import org.junit.Test

class BundledQuranScriptTest {
    private val source = com.nur.quran.data.api.rn.RnContent.parseWordsPack(Gson().fromJson(File("src/main/assets/data/words/10.json").readText(), com.google.gson.JsonObject::class.java))
        .verses.first { it.verse_key == "10:2" }
    private val legacy = "ءَامَنُوٓا\u06df"
    private fun cached() = VerseEntity(source.id, 10, 2, "10:2", legacy, "Indopak text", legacy, 208, 11, "Keep this translation")

    @Test
    fun `offline Hafs uses the same bundled QPC word spelling as the web`() {
        val (verse, words) = withBundledQuranScript(cached(), source)!!
        val believed = words.first { it.position == 14 }
        assertEquals("ءَامَنُوٓاْ", believed.textQpcHafs)
        assertEquals(legacy, believed.textUthmani)
        val display = mushafPlainWordTexts(words, "madani-standard", "KFGQPC Hafs")
        assertEquals("ءَامَنُوٓاْ", display[13])
        assertFalse(display[13].contains('\u06df'))
        assertEquals(source.words!!.joinToString(" ") { it.text_qpc_hafs!! }, verse.textQpcHafs)
    }

    @Test
    fun `repair preserves original scripts translation and verse identity`() {
        val before = cached()
        val (after, words) = withBundledQuranScript(before, source)!!
        assertEquals(before, after.copy(textQpcHafs = before.textQpcHafs))
        assertEquals(words.size, words.map { it.position }.toSet().size)
        assertEquals(1, words.count { it.charTypeName == "end" })
        assertEquals(words.map { it.id }, withBundledQuranScript(after, source)!!.second.map { it.id })
    }

    @Test
    fun `missing or incomplete bundle never fabricates a QPC spelling`() {
        assertNull(withBundledQuranScript(cached(), source.copy(words = emptyList())))
        val incomplete = source.words!!.mapIndexed { i, word -> if (i == 0) word.copy(text_qpc_hafs = null) else word }
        assertNull(withBundledQuranScript(cached(), source.copy(words = incomplete)))
    }
}
