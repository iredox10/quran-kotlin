package com.nur.quran.data.sync

import com.nur.quran.data.db.entities.BookmarkEntity
import com.nur.quran.data.db.entities.CollectionEntity
import com.nur.quran.data.db.entities.CollectionItemEntity
import com.nur.quran.data.db.entities.ReadingSessionEntity
import com.nur.quran.data.db.entities.RecentlyReadEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * JVM unit tests for the pure Android → web state mappings in SyncSnapshot.
 * Prefs/DAO reads stay in SnapshotBuilder (Android Context) and are not
 * covered here.
 */
class SyncSnapshotMappingsTest {

    @Test
    fun `current appearance setting wins over an older restored legacy copy`() = kotlinx.coroutines.runBlocking {
        val settings = FakePrefs().apply { values["is_dark_theme"] = true }
        val legacy = FakePrefs().apply { values["is_dark_theme"] = false }
        val context = object : android.content.ContextWrapper(null) {
            override fun getSharedPreferences(name: String, mode: Int): android.content.SharedPreferences =
                if (name == "Settings") settings else legacy
        }
        val builder = SnapshotBuilder(com.google.gson.Gson(), context, RecordingQuranDao().dao)
        assertEquals("dark", builder.build().theme)
        settings.values["is_dark_theme"] = false
        legacy.values["is_dark_theme"] = true
        assertEquals("light", builder.build().theme)
        settings.values.clear()
        assertEquals("dark", builder.build().theme)
    }


    @Test
    fun `default scale maps to web default font index`() {
        assertEquals(2, fontScaleToWebFontSizeIndex(1f))
    }

    @Test
    fun `font scale maps across the whole web index range`() {
        assertEquals(1, fontScaleToWebFontSizeIndex(0.5f))
        assertEquals(3, fontScaleToWebFontSizeIndex(1.25f))
        assertEquals(4, fontScaleToWebFontSizeIndex(1.5f))
        assertEquals(6, fontScaleToWebFontSizeIndex(2f))
        assertEquals(8, fontScaleToWebFontSizeIndex(2.5f))
    }

    @Test
    fun `font scale clamps past the web index range`() {
        assertEquals(1, fontScaleToWebFontSizeIndex(0f))
        assertEquals(8, fontScaleToWebFontSizeIndex(3f))
    }

    @Test
    fun `word tooltip behavior passes through known values`() {
        assertEquals("tajweed", normalizeWordTooltipBehavior("tajweed"))
        assertEquals("translation", normalizeWordTooltipBehavior(" translation "))
        assertEquals("none", normalizeWordTooltipBehavior("None"))
    }

    @Test
    fun `word tooltip behavior drops unknown or blank values`() {
        assertNull(normalizeWordTooltipBehavior(null))
        assertNull(normalizeWordTooltipBehavior(""))
        assertNull(normalizeWordTooltipBehavior("highlight"))
    }

    @Test
    fun `arabic font id is derived from the android font display name`() {
        assertEquals("kfgqpc-hafs", arabicFontIdFromName("KFGQPC Hafs"))
        assertEquals("scheherazade-new", arabicFontIdFromName("Scheherazade New"))
        assertEquals("amiri-quran", arabicFontIdFromName("Amiri Quran"))
        assertEquals("noto-naskh-arabic", arabicFontIdFromName("Noto Naskh Arabic"))
    }

    @Test
    fun `arabic font id is dropped for names web does not know`() {
        assertNull(arabicFontIdFromName(null))
        assertNull(arabicFontIdFromName(""))
        assertNull(arabicFontIdFromName("Comic Sans Arabic"))
    }

    @Test
    fun `read preference passes through web values`() {
        assertEquals("before", normalizeReadPreference("before"))
        assertEquals("split", normalizeReadPreference(" SPLIT "))
    }

    @Test
    fun `read preference drops values web does not accept`() {
        assertNull(normalizeReadPreference(null))
        assertNull(normalizeReadPreference(""))
        assertNull(normalizeReadPreference("whenever"))
    }

    @Test
    fun `active prayers keep canonical order and drop unknown names`() {
        val parsed = normalizeActivePrayers(
            linkedSetOf("Isha", "Fajr", "Duha", "Asr", "Maghrib")
        )
        assertEquals(listOf("Fajr", "Asr", "Maghrib", "Isha"), parsed)
    }

    @Test
    fun `active prayers are omitted when nothing valid is stored`() {
        assertNull(normalizeActivePrayers(null))
        assertNull(normalizeActivePrayers(emptySet()))
        assertNull(normalizeActivePrayers(linkedSetOf("Duha")))
    }

    @Test
    fun `memorized surah ids are deduped and sorted`() {
        assertEquals(listOf(1, 2, 18), memorizedSurahIds(linkedSetOf("18", "1", "2", "1", "")))
    }

    @Test
    fun `memorized surah ids drop non-numeric entries`() {
        assertEquals(emptyList<Int>(), memorizedSurahIds(linkedSetOf("nope", "")))
        assertEquals(emptyList<Int>(), memorizedSurahIds(null))
    }

    @Test
    fun `memorized ayah keys are sorted and deduped`() {
        assertEquals(
            listOf("1:1", "2:5"),
            memorizedAyahKeys(linkedSetOf("2:5", "1:1", "2:5", " 1:1 "))
        )
        assertEquals(emptyList<String>(), memorizedAyahKeys(null))
    }

    @Test
    fun `tour flags become sorted ids`() {
        val prefs = mapOf<String, Any?>(
            "tour_home" to true,
            "tour_advanced-tour" to true,
            "tour_memorization-tour" to false,
            "coachmark_home" to true,
            "visit_home" to 3
        )
        assertEquals(listOf("advanced-tour", "home"), flagIds(prefs, "tour_"))
        assertEquals(listOf("home"), flagIds(prefs, "coachmark_"))
    }

    @Test
    fun `page visit counts keep positive entries only`() {
        val prefs = mapOf<String, Any?>(
            "visit_home" to 3,
            "visit_planner" to 0,
            "visit_" to 4,
            "onboarding_dismissed" to true
        )
        assertEquals(mapOf("home" to 3), pageVisitCounts(prefs))
    }

    @Test
    fun `bookmark is shaped like a web bookmark entry`() {
        assertEquals(
            mapOf(
                "verseKey" to "2:255",
                "surahName" to "Al-Baqarah",
                "chapterId" to 2,
                "createdAt" to 1700L
            ),
            webBookmark(
                BookmarkEntity(
                    verseKey = "2:255",
                    chapterId = 2,
                    surahName = "Al-Baqarah",
                    timestamp = 1700L
                )
            )
        )
    }

    @Test
    fun `reading session keeps the timestamp union key and seconds`() {
        assertEquals(
            mapOf<String, Any?>(
                "date" to "2026-01-01",
                "duration" to 120L,
                "type" to "reading",
                "chapterId" to 2,
                "timestamp" to 1700L
            ),
            webReadingSession(
                ReadingSessionEntity(
                    date = "2026-01-01",
                    duration = 120L,
                    type = "reading",
                    chapterId = 2,
                    timestamp = 1700L
                )
            )
        )
    }

    @Test
    fun `recently read keeps chapter id and name`() {
        assertEquals(
            mapOf<String, Any?>(
                "chapterId" to 18,
                "chapterName" to "Al-Kahf",
                "verseKey" to "18:10",
                "timestamp" to 1700L
            ),
            webRecentlyRead(
                RecentlyReadEntity(
                    chapterId = 18,
                    chapterName = "Al-Kahf",
                    verseKey = "18:10",
                    timestamp = 1700L
                )
            )
        )
    }

    @Test
    fun `collections nest their items like the web collection shape`() {
        val collections = listOf(
            CollectionEntity(id = 4L, name = "Favourites", createdAt = 90L),
            CollectionEntity(id = 5L, name = "Empty")
        )
        val items = listOf(
            CollectionItemEntity(
                collectionId = 4L,
                verseKey = "1:1",
                chapterId = 1,
                surahName = "Al-Fatihah",
                addedAt = 91L
            ),
            CollectionItemEntity(collectionId = 99L, verseKey = "2:1", chapterId = 2, surahName = "Al-Baqarah")
        )

        val flat = webCollections(collections, items)

        assertEquals(2, flat.size)
        assertEquals(4L, flat[0]["id"])
        assertEquals("Favourites", flat[0]["name"])
        assertEquals(
            listOf(
                mapOf<String, Any?>(
                    "verseKey" to "1:1",
                    "surahName" to "Al-Fatihah",
                    "chapterId" to 1,
                    "addedAt" to 91L
                )
            ),
            flat[0]["items"]
        )
        assertEquals(emptyList<Map<String, Any?>>(), flat[1]["items"])
    }

    @Test
    fun `planner reflections are lifted out of the plans into the web keyed shape`() {
        val plans = listOf(
            mapOf<String, Any?>(
                "id" to "plan-1",
                "assignmentReflections" to mapOf<String, Any?>("1" to "day one", "2" to "   ")
            ),
            mapOf<String, Any?>("id" to "plan-2"),
            mapOf<String, Any?>("assignmentReflections" to mapOf<String, Any?>("1" to "orphan"))
        )

        assertEquals(
            mapOf("plan-1" to mapOf("1" to mapOf("text" to "day one"))),
            plannerReflectionsFromPlans(plans)
        )
    }

    @Test
    fun `planner session timers are wrapped in the web totalSeconds entry`() {
        // Gson reads JSON numbers as Double, so both shapes must survive.
        assertEquals(
            mapOf(
                "1" to mapOf("totalSeconds" to 120L),
                "2" to mapOf("totalSeconds" to 45L)
            ),
            plannerSessionTimers(
                mapOf<String, Any?>("1" to 120.0, "2" to 45L, "3" to "nope", "4" to null)
            )
        )
        assertEquals(emptyMap<String, Map<String, Long>>(), plannerSessionTimers(emptyMap()))
    }
}
