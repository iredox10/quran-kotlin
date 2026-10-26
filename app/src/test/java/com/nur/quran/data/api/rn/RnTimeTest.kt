package com.nur.quran.data.api.rn

import org.junit.Assert.assertEquals
import org.junit.Test

/** The sync watermark must come from the server's stamps only — never from garbage. */
class RnTimeTest {

    @Test
    fun `epoch ms numbers pass through`() {
        assertEquals(1769000000000L, RnTime.parse(1769000000000L))
        assertEquals(900L, RnTime.parse(900))
        assertEquals(0L, RnTime.parse(null))
        assertEquals(0L, RnTime.parse(mapOf<String, Any?>()))
    }

    @Test
    fun `all-digit strings are epoch ms`() {
        assertEquals(1769000000000L, RnTime.parse("1769000000000"))
    }

    @Test
    fun `iso8601 zulu parses`() {
        assertEquals(1700000000000L, RnTime.parse("2023-11-14T22:13:20.000Z"))
        assertEquals(1700000000000L, RnTime.parse("2023-11-14T22:13:20Z"))
    }

    @Test
    fun `appwrite offset style parses (fix_login-restore-pull legacy)`() {
        assertEquals(1700000000000L, RnTime.parse("2023-11-15T00:13:20+02:00"))
        assertEquals(1700000000000L, RnTime.parse("2023-11-15T00:13:20.000+02:00"))
    }

    @Test
    fun `garbage parses to zero`() {
        assertEquals(0L, RnTime.parse("not-a-date"))
        assertEquals(0L, RnTime.parse(""))
        assertEquals(0L, RnTime.parse("   "))
    }
}
