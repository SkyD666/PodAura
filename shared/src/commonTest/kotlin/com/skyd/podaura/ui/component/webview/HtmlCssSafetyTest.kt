package com.skyd.podaura.ui.component.webview

import com.skyd.htmlrender.base.model.TextStyler
import com.skyd.htmlrender.core.css.LineHeightCssAnnotatedHandler
import com.skyd.htmlrender.core.styler.IParagraphStyleStyler
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class HtmlCssSafetyTest {

    @Test
    fun rejectsNegativeAndNonFiniteLineHeights() {
        listOf("-1", "-1em", "1e100", "1e100em").forEach { value ->
            val stylers = mutableListOf<TextStyler>()

            LineHeightCssAnnotatedHandler().addStyle(stylers, value)

            assertTrue(stylers.isEmpty(), "Expected '$value' to be rejected")
        }
    }

    @Test
    fun acceptsValidUnitlessLineHeight() {
        val stylers = mutableListOf<TextStyler>()

        LineHeightCssAnnotatedHandler().addStyle(stylers, "1.5")

        val paragraphStyler = assertIs<IParagraphStyleStyler>(stylers.single())
        assertEquals(1.5f, paragraphStyler.getParagraphStyle().lineHeight.value)
    }
}
