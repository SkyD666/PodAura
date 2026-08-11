package com.skyd.htmlrender.core.css

import com.skyd.htmlrender.base.model.TextStyler
import kotlin.test.Test
import kotlin.test.assertEquals

class LetterSpacingCssAnnotatedHandlerTest {
    @Test
    fun rejectsNonFiniteSpacing() {
        val styles = mutableListOf<TextStyler>()
        val handler = LetterSpacingCssAnnotatedHandler()

        handler.addStyle(styles, "NaNem")
        handler.addStyle(styles, "1e100em")

        assertEquals(0, styles.size)
    }
}
