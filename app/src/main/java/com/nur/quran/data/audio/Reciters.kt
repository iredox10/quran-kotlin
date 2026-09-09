package com.nur.quran.data.audio

/**
 * Canonical list of reciters (GreenTech-style).
 *
 * IDs are compatible with the existing `reciter_id` pref
 * (see SurahViewModel, default 7 = Mishari Rashid al-Afasy)
 * and double as the on-disk per-reciter folder name:
 * `filesDir/audio/<reciterId>/SSSAAA.mp3`.
 */
data class Reciter(
    val id: Int,
    val name: String,
    /** Recitation style: Murattal, Mujawwad or Muallim. */
    val style: String,
    /**
     * Server path for per-ayah mp3s (web: RECITER_PATHS in audioUrl.js).
     * `everyayah/` prefix resolves to the quranicaudio mirror, anything else
     * to verses.quran.com. Null = no deterministic URL (stored verse audioUrl
     * is the only source).
     */
    val audioPath: String? = null
)

object Reciters {
    const val DEFAULT_ID = 7

    const val STYLE_MURATTAL = "Murattal"
    const val STYLE_MUJAWWAD = "Mujawwad"
    const val STYLE_MUALLIM = "Muallim"

    val ALL: List<Reciter> = listOf(
        Reciter(7, "Mishari Rashid al-Afasy", STYLE_MURATTAL, "Alafasy/mp3"),
        Reciter(1, "AbdulBaset AbdulSamad", STYLE_MURATTAL, "AbdulBaset/Murattal/mp3"),
        Reciter(2, "Abdur-Rahman as-Sudais", STYLE_MURATTAL, "Sudais/mp3"),
        Reciter(3, "Abu Bakr al-Shatri", STYLE_MURATTAL, "Shatri/mp3"),
        Reciter(4, "Hani ar-Rifai", STYLE_MURATTAL, "Rifai/mp3"),
        Reciter(5, "Mahmoud Khalil Al-Husary", STYLE_MURATTAL, "everyayah/Husary_64kbps"),
        Reciter(6, "Mohamed Siddiq Al-Minshawi", STYLE_MURATTAL, "Minshawi/Murattal/mp3"),
        Reciter(9, "Mishari Rashid al-Afasy (Mujawwad)", STYLE_MUJAWWAD, "everyayah/Alafasy_128kbps"),
        Reciter(10, "Mahmoud Khalil Al-Husary (Muallim)", STYLE_MUALLIM, "everyayah/Husary_Muallim_128kbps"),
        Reciter(11, "Maher Al-Muaiqly (Gapless)", STYLE_MURATTAL),
        Reciter(12, "Idris Abkar", STYLE_MURATTAL),
        Reciter(13, "Mahmoud Khalil Al-Husary (Gapless)", STYLE_MURATTAL, "everyayah/Husary_64kbps"),
        Reciter(14, "Maher Al-Muaiqly (KFGQPC)", STYLE_MURATTAL),
        Reciter(15, "Mohamed Siddiq al-Minshawi (Mujawwad)", STYLE_MUJAWWAD, "Minshawi/Mujawwad/mp3"),
        Reciter(16, "Saud ash-Shuraym", STYLE_MURATTAL, "Shuraym/mp3"),
        Reciter(17, "Mohamed al-Tablawi", STYLE_MURATTAL, "everyayah/Mohammad_al_Tablaway_128kbps"),
        Reciter(18, "Maher Al Muaiqly", STYLE_MURATTAL, "everyayah/MaherAlMuaiqly128kbps"),
        Reciter(19, "Maher Al Muaiqly (Haramain)", STYLE_MURATTAL, "everyayah/Maher_AlMuaiqly_64kbps"),
        Reciter(20, "Yasser Ad-Dussary", STYLE_MURATTAL, "everyayah/Yasser_Ad-Dussary_128kbps")
    )

    /** Web parity: buildReciterUrl — deterministic per-ayah mp3 URL without API. */
    fun buildAudioUrl(reciterId: Int, verseKey: String): String? {
        val path = byId(reciterId)?.audioPath ?: return null
        val parts = verseKey.split(":")
        val surah = parts.getOrNull(0)?.toIntOrNull() ?: return null
        val ayah = parts.getOrNull(1)?.toIntOrNull() ?: return null
        val file = "%03d%03d.mp3".format(surah, ayah)
        return if (path.startsWith("everyayah/")) {
            "https://mirrors.quranicaudio.com/$path/$file"
        } else {
            "https://verses.quran.com/$path/$file"
        }
    }

    fun byId(id: Int): Reciter? = ALL.find { it.id == id }

    fun nameOf(id: Int): String = byId(id)?.name ?: "Reciter $id"

    fun normalizeFolder(name: String): String = name.trim().lowercase()

    fun groupedByStyle(): Map<String, List<Reciter>> = ALL.groupBy { it.style }

    fun findByFolder(folderName: String): Reciter? {
        val normalized = normalizeFolder(folderName)
        val exactId = when (normalized) {
            "muaiqly_non_haramain_gapless" -> 11
            "muaigly_non_haramain_gapless" -> 11
            "idris-abkar" -> 12
            "idris_abkar" -> 12
            "husary" -> 13
            "muaiqly_kfgqpc" -> 14
            else -> null
        }
        if (exactId != null) return byId(exactId)
        val fuzzyId = when {
            normalized.contains("muaiqly") || normalized.contains("muaigly") -> 11
            normalized.contains("abkar") -> 12
            normalized.contains("husary") -> 13
            else -> null
        }
        return fuzzyId?.let { byId(it) }
    }
}
