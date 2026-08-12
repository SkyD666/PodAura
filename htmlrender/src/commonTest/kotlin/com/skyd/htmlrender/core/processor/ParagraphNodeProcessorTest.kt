package com.skyd.htmlrender.core.processor

import com.skyd.htmlrender.core.HtmlAnnotator
import com.skyd.htmlrender.ui.RawHtmlData
import com.skyd.htmlrender.ui.handler.AnnotatedMarginHandler
import com.skyd.htmlrender.ui.handler.ListItemAnnotatedHandler
import com.skyd.htmlrender.ui.styler.MarginStyler
import com.skyd.htmlrender.ui.styler.UnorderedListStyler
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ParagraphNodeProcessorTest {

    @Test
    fun listItemParagraphStartsOnTheMarkerLine() = runBlocking {
        val result = listAnnotator().from(
            RawHtmlData(
                """
                <ul>
                  <li><p>First item.</p></li>
                  <li><p>Second item.</p></li>
                </ul>
                """.trimIndent(),
            ),
        )

        val items = result.getStringAnnotations(
            tag = UnorderedListStyler.TAG_NAME,
            start = 0,
            end = result.length,
        ).map { range -> result.subSequence(range.start, range.end).text }

        assertEquals(2, items.size)
        assertEquals("First item.", items[0])
        assertEquals("Second item.", items[1])
        assertFalse(items.any { it.startsWith('\n') })
    }

    @Test
    fun keepsParagraphBreaksInsideAListItem() = runBlocking {
        val result = listAnnotator().from(
            RawHtmlData("<ul><li><p>First paragraph.</p><p>Second paragraph.</p></li></ul>"),
        )
        val item = result.getStringAnnotations(
            tag = UnorderedListStyler.TAG_NAME,
            start = 0,
            end = result.length,
        ).single()

        assertEquals(
            "First paragraph.\n\nSecond paragraph.",
            result.subSequence(item.start, item.end).text,
        )
    }

    private fun listAnnotator() = HtmlAnnotator(
        preTagHandlers = mapOf(
            "li" to ListItemAnnotatedHandler(),
            "ul" to AnnotatedMarginHandler {
                listOf(MarginStyler.Left("4em"))
            },
        ),
    )
}
