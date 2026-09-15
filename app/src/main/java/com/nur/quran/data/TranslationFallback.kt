package com.nur.quran.data

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nur.quran.data.api.ApiVerse
import com.nur.quran.data.db.entities.VerseEntity
import java.io.File
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * Provides translation fallback mechanisms:
 * 1. Maps unsupported translation edition IDs (e.g., 131 Khattab) to fallback ID 20 (Saheeh International).
 * 2. Provides offline translation fallback text (from bundled JSON assets) when an API verse has null/blank translation.
 */
object TranslationFallback {

    /** Default fallback translation ID: 20 (Saheeh International). */
    const val FALLBACK_TRANSLATION_ID = 20

    /**
     * Maps translation ID to a supported edition.
     * Translation 131 (Khattab) is absent from the live v4 API and returns no verses,
     * so it maps to the default fallback translation ID (20).
     */
    fun resolveTranslationId(translationId: Int): Int {
        return if (translationId == 131) FALLBACK_TRANSLATION_ID else translationId
    }

    /**
     * In-memory cache of offline translations mapped by verse key (e.g., "1:1" -> "In the name of God...").
     */
    private val offlineTranslations = ConcurrentHashMap<String, String>()

    @Volatile
    private var isLoaded = false

    init {
        // Seed first surah (Al-Fatihah) as guaranteed immediate fallback
        offlineTranslations["1:1"] = "In the name of God, the Lord of Mercy, the Giver of Mercy!"
        offlineTranslations["1:2"] = "Praise belongs to God, Lord of the Worlds,"
        offlineTranslations["1:3"] = "the Lord of Mercy, the Giver of Mercy,"
        offlineTranslations["1:4"] = "Master of the Day of Judgement."
        offlineTranslations["1:5"] = "It is You we worship; it is You we ask for help."
        offlineTranslations["1:6"] = "Guide us to the straight path:"
        offlineTranslations["1:7"] = "the path of those You have blessed, those who incur no anger and who have not gone astray."
    }

    /**
     * Seeds or overrides offline translations in the cache.
     */
    fun loadTranslations(translations: Map<String, String>) {
        offlineTranslations.putAll(translations)
        isLoaded = true
    }

    /**
     * Loads translations from a JSON input stream.
     */
    fun loadFromJson(inputStream: InputStream, gson: Gson = Gson()): Boolean {
        return try {
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val type = object : TypeToken<List<OfflineVerseDto>>() {}.type
            val list: List<OfflineVerseDto> = gson.fromJson(jsonString, type)
            list.forEach { item ->
                if (!item.translation.isNullOrBlank()) {
                    offlineTranslations[item.verse_key] = item.translation
                }
            }
            isLoaded = true
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Retrieves the offline fallback translation text for a given verse key (e.g. "1:1").
     */
    fun getOfflineTranslation(verseKey: String): String? {
        offlineTranslations[verseKey]?.let { return it }
        if (!isLoaded) {
            tryAutoLoad()
        }
        return offlineTranslations[verseKey]
    }

    /**
     * Resolves the translation for an [ApiVerse]. If the API translation is present and non-blank,
     * returns it; otherwise, falls back to the offline translation for the verse key.
     */
    fun resolveTranslation(apiVerse: ApiVerse): String? {
        val apiText = apiVerse.translations?.firstOrNull()?.text
        if (!apiText.isNullOrBlank()) {
            return apiText
        }
        return getOfflineTranslation(apiVerse.verse_key)
    }

    /**
     * Resolves the translation for a verse key and nullable API translation text.
     */
    fun resolveTranslation(verseKey: String, apiTranslationText: String?): String? {
        if (!apiTranslationText.isNullOrBlank()) {
            return apiTranslationText
        }
        return getOfflineTranslation(verseKey)
    }

    /**
     * Backfills blank or null translations on [VerseEntity] list from offline cache.
     */
    fun backfillBlankTranslations(verses: List<VerseEntity>): List<VerseEntity> {
        if (verses.isEmpty() || verses.none { it.translation.isNullOrBlank() }) return verses
        return verses.map { verse ->
            if (!verse.translation.isNullOrBlank()) {
                verse
            } else {
                val fallback = getOfflineTranslation(verse.verseKey)
                if (!fallback.isNullOrBlank()) verse.copy(translation = fallback) else verse
            }
        }
    }

    private fun tryAutoLoad() {
        val candidates = listOf(
            File("app/src/main/assets/data/quran_full.json"),
            File("src/main/assets/data/quran_full.json"),
            File("../app/src/main/assets/data/quran_full.json")
        )
        for (file in candidates) {
            if (file.exists() && file.canRead()) {
                try {
                    file.inputStream().use { loadFromJson(it) }
                    if (isLoaded) return
                } catch (_: Exception) {}
            }
        }
    }

    private data class OfflineVerseDto(
        val verse_key: String,
        val translation: String?
    )
}
