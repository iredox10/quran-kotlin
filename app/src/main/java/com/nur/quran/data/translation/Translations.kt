package com.nur.quran.data.translation

/**
 * Catalog of Quran.com (api.quran.com v4) translation editions.
 *
 * IDs are `resource_id`s used in `translations=<id>` query params.
 * Verified against `GET /api/v4/resources/translations` (126 entries)
 * plus the versioned content-API docs at api-docs.quran.com.
 *
 * Corrections found during verification (kept: accurate label, dropped: unverified claim):
 * - Russian Kuliev is 45, not 199 (199 = Spanish, Noor International Center).
 * - Spanish 83 is Isa Garcia, not Cortes (Cortes is not on the v4 list).
 * - Tamil Omar Sharif is 229, not 180 (180 is not on the v4 list).
 * - Portuguese El-Hayek is 43, not 90 (90 is not on the v4 list).
 * - Bengali 163 is Sheikh Mujibur Rahman, not Muhiuddin Khan (not on the v4 list).
 * - Indonesian 33 name field reads "Indonesian Islamic Affairs Ministry".
 * - German 27 author reads "Frank Bubenheim and Nadeem" (Nadeem Elyas).
 * - 131 (Khattab) is absent from the live v4 list and returns no verses,
 *   but is kept: it is trusted web-parity (quran-app TRANSLATIONS) and docs use it.
 */
data class TranslationEdition(val id: Int, val name: String, val language: String)

val TRANSLATION_EDITIONS: List<TranslationEdition> = listOf(
    // English first (default 20 first), then Hausa, Urdu,
    // then the rest alphabetically by language.
    TranslationEdition(20, "Saheeh International", "English"),
    TranslationEdition(85, "M.A.S. Abdel Haleem", "English"),
    TranslationEdition(131, "Dr. Mustafa Khattab", "English"),
    TranslationEdition(22, "A. Yusuf Ali", "English"),
    TranslationEdition(84, "Mufti Taqi Usmani", "English"),
    TranslationEdition(32, "Abubakar Mahmoud Gumi", "Hausa"),
    TranslationEdition(234, "Fatah Muhammad Jalandhari", "Urdu"),
    TranslationEdition(163, "Sheikh Mujibur Rahman", "Bengali"),
    TranslationEdition(56, "Ma Jian", "Chinese"),
    TranslationEdition(31, "Muhammad Hamidullah", "French"),
    TranslationEdition(27, "Bubenheim & Elyas", "German"),
    TranslationEdition(33, "Ministry of Religious Affairs", "Indonesian"),
    TranslationEdition(43, "Samir El-Hayek", "Portuguese"),
    TranslationEdition(45, "Elmir Kuliev", "Russian"),
    TranslationEdition(83, "Isa Garcia", "Spanish"),
    TranslationEdition(229, "Omar Sharif", "Tamil"),
    TranslationEdition(77, "Diyanet", "Turkish"),
)

fun translationNameOf(id: Int): String =
    TRANSLATION_EDITIONS.find { it.id == id }?.name ?: "Translation $id"

fun translationsByLanguage(): Map<String, List<TranslationEdition>> =
    TRANSLATION_EDITIONS.groupBy { it.language }
