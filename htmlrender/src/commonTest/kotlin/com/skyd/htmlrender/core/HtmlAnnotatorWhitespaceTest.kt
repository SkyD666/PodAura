package com.skyd.htmlrender.core

import com.skyd.htmlrender.ui.RawHtmlData
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class HtmlAnnotatorWhitespaceTest {

    @Test
    fun preservesSpacesAroundInlineFormatting() = runBlocking {
        val text = render("<span>Before <strong>bold</strong> after</span>")

        assertEquals("Before bold after", text)
    }

    @Test
    fun preservesWhitespaceTextNodesBetweenInlineElements() = runBlocking {
        val text = render("<span>Before</span> <strong>bold</strong> <em>after</em>")

        assertEquals("Before bold after", text)
    }

    @Test
    fun collapsesWhitespaceAcrossInlineFormatting() = runBlocking {
        val text = render("<span>Before <strong> bold </strong> after</span>")

        assertEquals("Before bold after", text)
    }

    @Test
    fun ignoresFormattingWhitespaceBetweenBlockElements() = runBlocking {
        val compact = render("<div><p>First</p><p>Second</p></div>")
        val formatted = render(
            """
            <div>
                <p>First</p>
                <p>Second</p>
            </div>
            """.trimIndent(),
        )

        assertEquals(compact, formatted)
    }

    private suspend fun render(html: String): String =
        HtmlAnnotator().from(RawHtmlData(html)).text
}
