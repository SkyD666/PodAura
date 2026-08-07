package com.skyd.podaura.ui.component.webview

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TimestampLinkTest {
    @Test
    fun linkifiesTwoAndThreePartTimestamps() {
        val result = linkifyTimestamps("<p>1:23 01:23 12:34 1:02:03</p>")

        assertTrue(result.contains("${TIMESTAMP_LINK_PREFIX}83"))
        assertTrue(result.contains("${TIMESTAMP_LINK_PREFIX}754"))
        assertTrue(result.contains("${TIMESTAMP_LINK_PREFIX}3723"))
    }

    @Test
    fun supportsFullWidthAndMixedColons() {
        val result = linkifyTimestamps("<p>1：23 01：23 1：02:03</p>")

        assertEquals(2, result.countOccurrences("${TIMESTAMP_LINK_PREFIX}83"))
        assertTrue(result.contains("${TIMESTAMP_LINK_PREFIX}3723"))
        assertTrue(result.contains("1：02:03"))
    }

    @Test
    fun rejectsInvalidAndPartialTimestamps() {
        val result = linkifyTimestamps("<p>1:2 1:60 1:02:60 12:34:56:00</p>")

        assertFalse(result.contains(TIMESTAMP_LINK_PREFIX))
    }

    @Test
    fun ignoresLinksCodeAndNonTextContent() {
        val result = linkifyTimestamps(
            """
            <a href="https://example.com/1:23">1:23</a>
            <code>2:34</code><pre>3:45</pre><script>4:56</script><style>.x-5:06{}</style>
            <p data-time="6:07">7:08</p>
            """.trimIndent()
        )

        assertEquals(1, result.countOccurrences(TIMESTAMP_LINK_PREFIX))
        assertTrue(result.contains("data-time=\"6:07\""))
        assertTrue(result.contains("${TIMESTAMP_LINK_PREFIX}428"))
    }

    @Test
    fun preservesEscapedTextAroundTimestamp() {
        val result = linkifyTimestamps("<p>a &lt; b &amp; c 1:23</p>")

        assertTrue(result.contains("a &lt; b &amp; c"))
        assertTrue(result.contains("${TIMESTAMP_LINK_PREFIX}83"))
    }

    @Test
    fun ignoresTimestampsInsidePlainTextUrls() {
        val result = linkifyTimestamps(
            "<p>https://example.com/1:23 www.example.com/2：34 normal 3:45</p>"
        )

        assertTrue(result.contains("https://example.com/1:23"))
        assertTrue(result.contains("www.example.com/2：34"))
        assertEquals(1, result.countOccurrences(TIMESTAMP_LINK_PREFIX))
        assertTrue(result.contains("${TIMESTAMP_LINK_PREFIX}225"))
    }

    @Test
    fun parsesOnlyInternalTimestampUris() {
        assertEquals(83, timestampSecondsFromUri("${TIMESTAMP_LINK_PREFIX}83"))
        assertEquals(null, timestampSecondsFromUri("https://example.com/83"))
        assertEquals(null, timestampSecondsFromUri("${TIMESTAMP_LINK_PREFIX}-1"))
    }

    private fun String.countOccurrences(value: String): Int =
        Regex(Regex.escape(value)).findAll(this).count()
}
