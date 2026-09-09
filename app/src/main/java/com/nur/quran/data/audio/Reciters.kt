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
    val style: String
)

object Reciters {
    const val DEFAULT_ID = 7

    const val STYLE_MURATTAL = "Murattal"
    const val STYLE_MUJAWWAD = "Mujawwad"
    const val STYLE_MUALLIM = "Muallim"

    val ALL: List<Reciter> = listOf(
        Reciter(7, "Mishari Rashid al-Afasy", STYLE_MURATTAL),
        Reciter(1, "AbdulBaset AbdulSamad", STYLE_MURATTAL),
        Reciter(2, "Abdur-Rahman as-Sudais", STYLE_MURATTAL),
        Reciter(3, "Abu Bakr al-Shatri", STYLE_MURATTAL),
        Reciter(4, "Hani ar-Rifai", STYLE_MURATTAL),
        Reciter(5, "Mahmoud Khalil Al-Husary", STYLE_MURATTAL),
        Reciter(6, "Mohamed Siddiq Al-Minshawi", STYLE_MURATTAL),
        Reciter(9, "Mishari Rashid al-Afasy (Mujawwad)", STYLE_MUJAWWAD),
        Reciter(10, "Mahmoud Khalil Al-Husary (Muallim)", STYLE_MUALLIM)
    )

    fun byId(id: Int): Reciter? = ALL.find { it.id == id }

    fun nameOf(id: Int): String = byId(id)?.name ?: "Reciter $id"
}
