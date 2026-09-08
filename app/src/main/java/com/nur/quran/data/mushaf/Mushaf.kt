package com.nur.quran.data.mushaf

/**
 * Port of quran-app/src/config/mushaf.js.
 *
 * Decision for v1 (no QCF binary fonts): use the web's "qcf-page" approach —
 * Unicode text grouped by API `line_number` for the requested `apiMushafId`,
 * rendered with the bundled Madani fonts. No new font binaries required.
 */
enum class MushafRenderMode { UNICODE, QCF_PAGE }

data class Mushaf(
    val id: String,
    val name: String,
    val description: String,
    val apiMushafId: Int,
    /** VerseEntity/WordEntity field to prefer, e.g. "text_qpc_hafs" or "text_indopak". */
    val verseField: String,
    val scriptField: String,
    val renderMode: MushafRenderMode,
    val pageCount: Int = 604,
    val defaultFontId: String,
    val supportedFontIds: List<String>,
    val supportsTajweedToggle: Boolean,
    val forcesTajweed: Boolean,
    val hasTajweedSource: Boolean
) {
    companion object {
        val MADANI_STANDARD = Mushaf(
            id = "madani-standard",
            name = "Madani Standard",
            description = "QPC Hafs script, continuous reading.",
            apiMushafId = 5,
            verseField = "text_qpc_hafs",
            scriptField = "text_qpc_hafs",
            renderMode = MushafRenderMode.UNICODE,
            defaultFontId = "kfgqpc-hafs",
            supportedFontIds = listOf(
                "kfgqpc-hafs", "uthman-taha-naskh", "amiri-quran",
                "noto-naskh-arabic", "scheherazade-new"
            ),
            supportsTajweedToggle = true,
            forcesTajweed = false,
            hasTajweedSource = true
        )
        val MADANI_TAJWEED = Mushaf(
            id = "madani-tajweed",
            name = "Madani Tajweed",
            description = "Page-accurate 15-line Madani layout with Tajweed.",
            apiMushafId = 19,
            verseField = "text_qpc_hafs",
            scriptField = "text_qpc_hafs",
            renderMode = MushafRenderMode.QCF_PAGE,
            defaultFontId = "kfgqpc-hafs",
            supportedFontIds = listOf("kfgqpc-hafs", "uthman-taha-naskh", "amiri-quran"),
            supportsTajweedToggle = true,
            forcesTajweed = true,
            hasTajweedSource = true
        )
        val INDOPAK = Mushaf(
            id = "indopak",
            name = "IndoPak",
            description = "South Asian Naskh script, 16-line layout.",
            apiMushafId = 3,
            verseField = "text_indopak",
            scriptField = "text_indopak",
            renderMode = MushafRenderMode.UNICODE,
            defaultFontId = "scheherazade-new",
            supportedFontIds = listOf("scheherazade-new", "amiri-quran"),
            supportsTajweedToggle = false,
            forcesTajweed = false,
            hasTajweedSource = false
        )

        val ALL = listOf(MADANI_STANDARD, MADANI_TAJWEED, INDOPAK)
        val DEFAULT = MADANI_STANDARD

        /** Legacy preset keys stored in prefs ("uthmani"/"tajweed"/"indopak"). */
        fun fromPresetKey(key: String?): Mushaf = when (key?.trim()?.lowercase()) {
            "tajweed", "madani-tajweed" -> MADANI_TAJWEED
            "indopak", "indopak-naskh" -> INDOPAK
            else -> MADANI_STANDARD
        }

        fun fromId(id: String?): Mushaf =
            ALL.firstOrNull { it.id == id } ?: fromPresetKey(id)

        /** Web: getCompatibleArabicFontId — keep requested if compatible, else default. */
        fun compatibleFontId(mushaf: Mushaf, requestedFontId: String?): String {
            if (!requestedFontId.isNullOrBlank() && mushaf.supportedFontIds.contains(requestedFontId)) {
                return requestedFontId
            }
            // Legacy display names ("KFGQPC Hafs") -> ids ("kfgqpc-hafs").
            val normalized = requestedFontId?.trim()?.lowercase()
                ?.replace(" ", "-")
            if (normalized != null && mushaf.supportedFontIds.contains(normalized)) {
                return normalized
            }
            return mushaf.defaultFontId
        }

        /** Web: isTajweedEnabledForMushaf — Indopak never, Tajweed preset always. */
        fun isTajweedEffective(mushaf: Mushaf, userToggle: Boolean): Boolean {
            if (!mushaf.hasTajweedSource) return false
            return mushaf.forcesTajweed || userToggle
        }

        /** Verse/word fields per mushaf — mirrors buildFieldsForMushaf in quranApi.js. */
        fun verseFields(mushaf: Mushaf): String = when (mushaf.verseField) {
            "text_indopak" -> "text_indopak,text_uthmani,page_number"
            "text_qpc_hafs" -> "text_qpc_hafs,text_uthmani,page_number"
            else -> "text_uthmani,page_number"
        }

        fun wordFields(mushaf: Mushaf): String = when (mushaf.scriptField) {
            "text_indopak" -> "text_indopak,text_uthmani,page_number,line_number,translation,text_uthmani_tajweed"
            else -> "text_qpc_hafs,text_uthmani,page_number,line_number,translation,text_uthmani_tajweed"
        }
    }
}

/**
 * Web: getWordArabicText / getVerseArabicText — prefer the mushaf script field,
 * fall back through the other scripts. Keeps Indopak actually reachable.
 */
fun wordTextForMushaf(mushaf: Mushaf, uthmani: String?, indopak: String?, qpcHafs: String?): String {
    val preferred = when (mushaf.scriptField) {
        "text_indopak" -> indopak
        "text_qpc_hafs" -> qpcHafs
        else -> uthmani
    }
    if (!preferred.isNullOrBlank()) return preferred
    if (!qpcHafs.isNullOrBlank()) return qpcHafs
    if (!uthmani.isNullOrBlank()) return uthmani
    if (!indopak.isNullOrBlank()) return indopak
    return ""
}

fun verseTextForMushaf(mushaf: Mushaf, uthmani: String?, indopak: String?, qpcHafs: String?): String =
    wordTextForMushaf(mushaf, uthmani, indopak, qpcHafs)

/** Legacy font display name ("Scheherazade New") -> stable id ("scheherazade-new"). */
fun fontNameToId(name: String?): String = when (name?.trim()?.lowercase()) {
    "kfgqpc hafs", "kfgqpc-hafs" -> "kfgqpc-hafs"
    "uthman taha naskh", "uthman-taha-naskh" -> "uthman-taha-naskh"
    "amiri quran", "amiri-quran", "amiri" -> "amiri-quran"
    "noto naskh arabic", "noto-naskh-arabic", "noto" -> "noto-naskh-arabic"
    "scheherazade new", "scheherazade-new", "scheherazade" -> "scheherazade-new"
    else -> name?.trim()?.lowercase()?.replace(" ", "-") ?: "scheherazade-new"
}

/** Stable id -> display name used by the UI. */
fun fontIdToName(id: String?): String = when (id) {
    "kfgqpc-hafs" -> "KFGQPC Hafs"
    "uthman-taha-naskh" -> "Uthman Taha Naskh"
    "amiri-quran" -> "Amiri Quran"
    "noto-naskh-arabic" -> "Noto Naskh Arabic"
    "scheherazade-new" -> "Scheherazade New"
    else -> id ?: "Scheherazade New"
}
