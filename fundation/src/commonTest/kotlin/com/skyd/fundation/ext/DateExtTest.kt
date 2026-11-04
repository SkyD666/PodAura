package com.skyd.fundation.ext

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Instant

class DateExtTest {
    @Test
    fun parsesFeedDatesWithOffsetsAndRfc1123Fallback() {
        val instant = assertNotNull(Instant.tryParse("2026-11-02T16:00:42+00:00"))
        assertEquals(Instant.parse("2026-11-02T16:00:42Z"), instant)
        assertEquals(
            LocalDateTime(2026, 11, 3, 0, 0, 42),
            instant.toLocalDateTime(TimeZone.of("Asia/Shanghai")),
        )
        assertEquals(instant, Instant.tryParse("2026-11-03T00:00:42+08:00"))
        assertEquals(instant, Instant.tryParse("Mon, 02 Nov 2026 16:00:42 GMT"))
        assertEquals(
            Instant.parse("2026-10-26T16:18:03Z"),
            Instant.tryParse("2026-10-26T16:18:03+00:00"),
        )
        assertEquals(
            Instant.parse("2026-11-02T16:00:42.123Z"),
            Instant.tryParse("2026-11-02T16:00:42.123+00:00"),
        )
    }

    @Test
    fun invalidDatesReturnNull() {
        listOf("", "invalid", "2026-02-30T16:00:42+00:00", "2026-11-02T16:00:42+25:00")
            .forEach { assertNull(Instant.tryParse(it), it) }
    }
}
