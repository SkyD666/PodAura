package com.skyd.podaura.model.repository.feed.convert

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PodcastDurationTest {
    @Test
    fun parsesSecondsAndColonSeparatedDurationsWithoutClockLimits() {
        mapOf(
            "0" to 0L,
            "2593" to 2_593_000L,
            "3907" to 3_907_000L,
            "01:05" to 65_000L,
            "43:13" to 2_593_000L,
            "65:07" to 3_907_000L,
            "01:05:07" to 3_907_000L,
            "001:005:007" to 3_907_000L,
            "25:00:00" to 90_000_000L,
            " 43:13.5 " to 2_593_500L,
            "01:05:07.123456" to 3_907_123L,
            "2593.001" to 2_593_001L,
            "0.999999999999" to 999L,
            "00:00.000999999999" to 0L,
            // Duration's finite millisecond range ends one below Long.MAX_VALUE / 2.
            "4611686018427387.902" to Long.MAX_VALUE / 2 - 1,
        ).forEach { (input, expected) -> assertEquals(expected, parsePodcastDurationMillis(input), input) }
    }

    @Test
    fun rejectsInvalidAndOverflowingDurations() {
        listOf(
            "", " ", "-1", "+1", "NaN", "Infinity", "1e3", "1:", ":01", "1:60", "1:60:00",
            "1:2:3:4", "1.2:03", "1.", "1..2", "1.abc", "1:-2", "1:+2", "1:2e1", "1:2S",
            "4611686018427387.903", "9223372036854775.807", "9223372036854775.808",
            "9223372036854776", "9223372036854775808", "2562047788016:00:00",
        ).forEach { assertNull(parsePodcastDurationMillis(it), it) }
    }
}
