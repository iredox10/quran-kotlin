package com.nur.quran.utils

import com.nur.quran.ui.components.TajweedSegment
import java.util.regex.Pattern

object TajweedProcessor {

    val TAJWEED_COLORS = mapOf(
        "ham_wasl" to "#AAAAAA",
        "laam_shamsiyah" to "#AAAAAA",
        "madda_normal" to "#537FFF",
        "madda_permissible" to "#4050FF",
        "madda_obligatory" to "#000FB5",
        "madda_necessary" to "#2142c7",
        "qalaqah" to "#DD0008",
        "qalpiala" to "#DD0008",
        "ikhafa_shafawi" to "#D500B7",
        "ikhfa_shafawi" to "#D500B7",
        "ikhafa" to "#26BFFD",
        "ikhfa" to "#26BFFD",
        "idgham_shafawi" to "#169777",
        "idghaam_shafawi" to "#169777",
        "idgham_ghunnah" to "#169200",
        "idghaam_ghunnah" to "#169200",
        "idgham_wo_ghunnah" to "#169200",
        "idghaam_no_ghunnah" to "#169200",
        "idgham_mutajanisayn" to "#A1A1A1",
        "idghaam_mutajanisayn" to "#A1A1A1",
        "idgham_mutaqaribayn" to "#A1A1A1",
        "idghaam_mutaqaribayn" to "#A1A1A1",
        "iqlab" to "#26BFFD",
        "ghunnah" to "#FF7E1E",
        "slnt" to "#AAAAAA",
        "silent" to "#AAAAAA"
    )

    fun extractTajweedRuleAndColor(wordTajweedHtml: String?): Pair<String?, String?> {
        if (wordTajweedHtml.isNullOrBlank()) return Pair(null, null)
        val pattern = Pattern.compile("<tajweed\\s+class=['\"]?([^'\"\\s>]+)['\"]?>")
        val matcher = pattern.matcher(wordTajweedHtml)
        while (matcher.find()) {
            val ruleClass = matcher.group(1)
            if (!ruleClass.isNullOrBlank() && ruleClass != "end") {
                val colorHex = TAJWEED_COLORS[ruleClass]
                if (colorHex != null) {
                    return Pair(ruleClass, colorHex)
                }
            }
        }
        return Pair(null, null)
    }

    fun sanitizeTajweedHtml(html: String?): String {
        if (html.isNullOrEmpty()) return ""
        var cleaned = html
            .replace("\u0672", "\u0670")
            .replace("\u25cc", "")
            .replace("<[^>]+>\\s*[مۘۙۚۛۜ]\\s*</[^>]+>".toRegex(), "")
            .replace("<(span|tajweed|rule)\\s+class=['\"]?[^'\">]*['\"]?>\\s*[مۘۙۚۛۜ]\\s*</(span|tajweed|rule)>".toRegex(), "")
            .replace("<rule ", "<tajweed ")
            .replace("</rule>", "</tajweed>")
            .replace("<(span|tajweed|rule)\\s+class=['\"]?end['\"]?>.*?</(span|tajweed|rule)>".toRegex(), "")
        return cleaned
    }

    fun parseTajweedHtml(html: String?): List<RawSegment> {
        if (html.isNullOrEmpty()) return emptyList()
        val sanitized = sanitizeTajweedHtml(html)
        val rawSegments = mutableListOf<RawSegment>()

        val pattern = Pattern.compile("<tajweed\\s+class=['\"]?([^'\"\\s>]+)['\"]?>(.*?)</tajweed>")
        val matcher = pattern.matcher(sanitized)

        var lastIndex = 0
        val matches = mutableListOf<TagMatch>()

        while (matcher.find()) {
            matches.add(TagMatch(matcher.start(), matcher.end(), matcher.group(2) ?: "", matcher.group(1) ?: ""))
        }

        for (tag in matches) {
            if (tag.start > lastIndex) {
                val rawText = sanitized.substring(lastIndex, tag.start)
                val cleanText = rawText.replace("<[^>]+>".toRegex(), "")
                if (cleanText.isNotEmpty()) {
                    rawSegments.add(RawSegment(cleanText, null))
                }
            }
            val tagText = tag.text.trim()
            if (tagText != "م" && tagText != "ۘ" && tagText != "ۙ" && tagText != "ۚ" && tagText != "ۛ" && tagText != "ۜ") {
                rawSegments.add(RawSegment(tag.text, tag.className))
            }
            lastIndex = tag.end
        }

        if (lastIndex < sanitized.length) {
            val rawText = sanitized.substring(lastIndex)
            val cleanText = rawText.replace("<[^>]+>".toRegex(), "")
            if (cleanText.isNotEmpty()) {
                rawSegments.add(RawSegment(cleanText, null))
            }
        }

        return rawSegments
    }

    fun stripDiacritics(text: String): String {
        return text.replace("[\u064B-\u065F\u0670\u06D6-\u06DC\u06DF-\u06E8\u06EA-\u06ED]".toRegex(), "")
    }

    fun getWordTajweedSegments(
        plainText: String,
        tajweedHtml: String,
        defaultColor: String
    ): List<TajweedSegment> {
        if (plainText.isEmpty() || tajweedHtml.isEmpty()) return emptyList()

        val segs = parseTajweedHtml(tajweedHtml)
        if (segs.isEmpty()) return emptyList()

        val plainBase = stripDiacritics(plainText)
        val tajweedBase = stripDiacritics(segs.joinToString("") { it.text })

        if (plainBase.isEmpty() || tajweedBase.isEmpty()) return emptyList()

        val plainCharToBaseIdx = IntArray(plainText.length)
        var baseIdx = 0
        for (i in plainText.indices) {
            val ch = plainText[i].toString()
            val stripped = stripDiacritics(ch)
            if (stripped.isNotEmpty()) {
                plainCharToBaseIdx[i] = baseIdx
                baseIdx++
            } else {
                plainCharToBaseIdx[i] = maxOf(0, baseIdx - 1)
            }
        }

        val tajweedBaseToSeg = mutableListOf<TajweedBaseMapping>()
        for (si in segs.indices) {
            val seg = segs[si]
            val segBase = stripDiacritics(seg.text)
            val color = if (seg.className != null) (TAJWEED_COLORS[seg.className] ?: defaultColor) else defaultColor
            for (j in segBase.indices) {
                tajweedBaseToSeg.add(TajweedBaseMapping(si, color))
            }
        }

        val charColors = Array(plainText.length) { defaultColor }
        for (i in plainText.indices) {
            val pBase = plainCharToBaseIdx[i]
            if (pBase < tajweedBaseToSeg.size) {
                charColors[i] = tajweedBaseToSeg[pBase].color
            } else {
                charColors[i] = defaultColor
            }
        }

        val charRules = Array<String?>(plainText.length) { null }
        for (i in plainText.indices) {
            val pBase = plainCharToBaseIdx[i]
            if (pBase < tajweedBaseToSeg.size) {
                val segIdx = tajweedBaseToSeg[pBase].segIdx
                charRules[i] = segs[segIdx].className
            }
        }

        val result = mutableListOf<TajweedSegment>()
        var segStart = 0
        for (i in 1..charColors.size) {
            if (i == charColors.size || charColors[i] != charColors[segStart] || charRules[i] != charRules[segStart]) {
                result.add(
                    TajweedSegment(
                        start = segStart,
                        end = i,
                        colorHex = charColors[segStart],
                        ruleClass = charRules[segStart]
                    )
                )
                segStart = i
            }
        }

        return result
    }

    fun splitTajweedHtmlIntoWords(tajweedHtml: String): List<String> {
        val tokens = mutableListOf<String>()
        val matcher = Pattern.compile("<[^>]+>|[^<\\s]+|\\s+").matcher(tajweedHtml)
        while (matcher.find()) {
            tokens.add(matcher.group())
        }

        val words = mutableListOf<StringBuilder>()
        words.add(StringBuilder())
        val activeTags = mutableListOf<String>()

        for (token in tokens) {
            if (token.startsWith("<") && token.endsWith(">")) {
                if (token.startsWith("</")) {
                    if (activeTags.isNotEmpty()) activeTags.removeAt(activeTags.lastIndex)
                    words.last().append(token)
                } else {
                    activeTags.add(token)
                    words.last().append(token)
                }
            } else if (token.trim().isEmpty()) {
                for (tag in activeTags.asReversed()) {
                    val tagName = tag.substring(1).split(" ", ">")[0]
                    words.last().append("</$tagName>")
                }
                words.add(StringBuilder())
                for (tag in activeTags) {
                    words.last().append(tag)
                }
            } else {
                words.last().append(token)
            }
        }

        for (tag in activeTags.asReversed()) {
            val tagName = tag.substring(1).split(" ", ">")[0]
            if (!words.last().toString().endsWith("</$tagName>")) {
                words.last().append("</$tagName>")
            }
        }

        return words.map { it.toString().trim() }.filter { it.isNotEmpty() }
    }

    data class RawSegment(val text: String, val className: String?)
    private data class TagMatch(val start: Int, val end: Int, val text: String, val className: String)
    private data class TajweedBaseMapping(val segIdx: Int, val color: String)
}
