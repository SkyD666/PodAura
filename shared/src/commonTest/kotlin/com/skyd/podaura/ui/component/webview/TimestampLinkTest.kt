package com.skyd.podaura.ui.component.webview

import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import com.skyd.htmlrender.core.HtmlAnnotator
import com.skyd.htmlrender.core.StyleConfig
import com.skyd.htmlrender.ui.RawHtmlData
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
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
    fun convertsTimestampsInBlankLinks() {
        val result = linkifyTimestamps(
            "<p><a href=\"\">1:23</a><a>2:34</a><a href=\"\">not a timestamp</a></p>"
        )

        assertTrue(result.contains("href=\"${TIMESTAMP_LINK_PREFIX}83\""))
        assertTrue(result.contains("href=\"${TIMESTAMP_LINK_PREFIX}154\""))
        assertEquals(2, result.countOccurrences(TIMESTAMP_LINK_PREFIX))
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

    @Test
    fun htmlAnnotatorPreservesTimestampUri() = runBlocking {
        val html = linkifyTimestamps("<p>1:23</p>")
        val result = HtmlAnnotator().from(
            RawHtmlData(
                srcHtml = html,
                styleConfig = StyleConfig(
                    textStyle = TextStyle.Default,
                    linkStyles = TextLinkStyles(),
                    uriHandler = object : UriHandler {
                        override fun openUri(uri: String) = Unit
                    },
                ),
            )
        )

        val link = result.getLinkAnnotations(0, result.length).single().item
        assertTrue(link is LinkAnnotation.Clickable)
        assertEquals("${TIMESTAMP_LINK_PREFIX}83", link.tag)
    }

    @Test
    fun htmlAnnotatorResolvesRelativeLinksAndImages() = runBlocking {
        val result = HtmlAnnotator().from(
            RawHtmlData(
                srcHtml = "<p><a href=\"../story\">Story</a><img src=\"images/photo.jpg\"></p>",
                baseUri = "https://example.com/news/today/",
            )
        )

        val link = result.getLinkAnnotations(0, result.length).single().item
        assertEquals("https://example.com/news/story", assertIs<LinkAnnotation.Url>(link).url)
        val image = result.getStringAnnotations("img", 0, result.length).single()
        assertEquals("https://example.com/news/today/images/photo.jpg", image.item)
    }

    private fun String.countOccurrences(value: String): Int =
        Regex(Regex.escape(value)).findAll(this).count()
}
