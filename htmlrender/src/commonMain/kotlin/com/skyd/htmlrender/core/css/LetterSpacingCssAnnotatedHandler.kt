package com.skyd.htmlrender.core.css

import androidx.compose.ui.text.SpanStyle
import com.skyd.htmlrender.base.model.TextStyler
import com.skyd.htmlrender.core.styler.SpanStyleStyler
import com.skyd.htmlrender.core.util.TextUnitParser

class LetterSpacingCssAnnotatedHandler : CSSAnnotatedHandler() {
    override fun addStyle(list: MutableList<TextStyler>, value: String) {
        if (value == "normal") return
        val spacing = runCatching { TextUnitParser.parse(value) }.getOrNull() ?: return
        if (!spacing.value.isFinite()) return
        list += SpanStyleStyler { SpanStyle(letterSpacing = spacing) }
    }
}
