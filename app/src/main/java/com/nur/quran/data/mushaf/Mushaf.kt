package com.nur.quran.data.mushaf

/**
 * Offline-readable Hafs presentations. QCF glyph editions and alternate physical
 * page counts require their own fonts and page packs; a Unicode font is not a
 * substitute for those assets. See docs/mushaf-support.md for the source audit.
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
    val hasTajweedSource: Boolean,
    val isSelectable: Boolean = true,
    val unavailableReason: String? = null
) {
    companion object {
        val MADANI_STANDARD = Mushaf(
            id = "madani-standard",
            name = "Madani Standard",
            description = "Madani QPC Hafs script with flowing, adjustable text. 604-page navigation.",
            apiMushafId = 5,
            verseField = "text_qpc_hafs",
            scriptField = "text_qpc_hafs",
            renderMode = MushafRenderMode.UNICODE,
            defaultFontId = "kfgqpc-hafs",
            supportedFontIds = listOf(
                "kfgqpc-hafs", "amiri-quran", "noto-naskh-arabic", "scheherazade-new"
            ),
            supportsTajweedToggle = true,
            forcesTajweed = false,
            hasTajweedSource = true
        )
        val MADANI_TAJWEED = Mushaf(
            id = "madani-tajweed",
            name = "Madani Tajweed",
            description = "Madani Hafs text with Tajweed colors. Adjustable text, not a fixed printed-page facsimile.",
            apiMushafId = 5,
            verseField = "text_qpc_hafs",
            scriptField = "text_qpc_hafs",
            renderMode = MushafRenderMode.UNICODE,
            defaultFontId = "kfgqpc-hafs",
            supportedFontIds = listOf("kfgqpc-hafs", "amiri-quran", "noto-naskh-arabic", "scheherazade-new"),
            supportsTajweedToggle = true,
            forcesTajweed = true,
            hasTajweedSource = true
        )
        val UTHMANI_HAFS = Mushaf(
            id = "uthmani-hafs",
            name = "Uthmani Hafs",
            description = "Traditional Uthmani orthography in Amiri Quran. 604-page navigation.",
            apiMushafId = 4,
            verseField = "text_uthmani",
            scriptField = "text_uthmani",
            renderMode = MushafRenderMode.UNICODE,
            defaultFontId = "amiri-quran",
            supportedFontIds = listOf("amiri-quran", "kfgqpc-hafs", "noto-naskh-arabic", "scheherazade-new"),
            supportsTajweedToggle = true,
            forcesTajweed = false,
            hasTajweedSource = true
        )
        val INDOPAK = Mushaf(
            id = "indopak",
            name = "IndoPak",
            description = "South Asian IndoPak script. A complete compatible offline edition is not installed.",
            apiMushafId = 3,
            verseField = "text_indopak",
            scriptField = "text_indopak",
            renderMode = MushafRenderMode.UNICODE,
            defaultFontId = "scheherazade-new",
            supportedFontIds = listOf("scheherazade-new", "amiri-quran"),
            supportsTajweedToggle = false,
            forcesTajweed = false,
            hasTajweedSource = false,
            isSelectable = false,
            unavailableReason = "A matching IndoPak font and offline text edition are required before this mushaf can be selected."
        )

        val ALL = listOf(MADANI_STANDARD, MADANI_TAJWEED, UTHMANI_HAFS, INDOPAK)
        val SELECTABLE = ALL.filter { it.isSelectable }
        val DEFAULT = MADANI_STANDARD

        /** Legacy preset keys stored in prefs ("uthmani"/"tajweed"/"indopak"). */
        fun fromPresetKey(key: String?): Mushaf = when (key?.trim()?.lowercase()) {
            "tajweed", "madani-tajweed" -> MADANI_TAJWEED
            "indopak", "indopak-naskh" -> INDOPAK
            "uthmani-hafs" -> UTHMANI_HAFS
            else -> MADANI_STANDARD
        }

        fun fromId(id: String?): Mushaf =
            ALL.firstOrNull { it.id == id } ?: fromPresetKey(id)

        /** Previously stored unsupported editions migrate to a complete, bundled Hafs edition. */
        fun forReading(id: String?): Mushaf = fromId(id).takeIf { it.isSelectable } ?: DEFAULT

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

        fun compatibleFontName(mushafId: String?, requestedName: String?): String {
            val requested = requestedName?.takeIf { it.isNotBlank() && fromId(mushafId).isSelectable }?.let(::fontNameToId)
            return fontIdToName(compatibleFontId(forReading(mushafId), requested))
        }

        /** Web: isTajweedEnabledForMushaf — Indopak never, Tajweed preset always. */
        fun isTajweedEffective(mushaf: Mushaf, userToggle: Boolean): Boolean {
            if (!mushaf.hasTajweedSource) return false
            return mushaf.forcesTajweed || userToggle
        }

        /** Verse/word fields per mushaf — mirrors buildFieldsForMushaf in quranApi.js. */
        // Division/sajdah fields are always requested (Quran.com API v4 field
        // reference: juz/hizb/rub/ruku/manzil/sajdah_number/sajdah_type) so a
        // future schema migration can persist them; UI derives markers
        // in-memory from VerseDividers until then.
        private const val DIVISION_FIELDS =
            "hizb_number,rub_el_hizb_number,ruku_number,manzil_number,sajdah_number,sajdah_type"

        fun verseFields(mushaf: Mushaf): String = when (mushaf.verseField) {
            "text_indopak" -> "text_indopak,text_uthmani,page_number,$DIVISION_FIELDS"
            "text_qpc_hafs" -> "text_qpc_hafs,text_uthmani,page_number,$DIVISION_FIELDS"
            else -> "text_uthmani,page_number,$DIVISION_FIELDS"
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
    // A restored legacy preference must never publish unsupported private-use
    // glyphs as squares, or relabel different text as a supported IndoPak edition.
    val readable = Mushaf.forReading(mushaf.id)
    val preferred = when (readable.scriptField) {
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
