package com.skyd.htmlrender.core.css

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.BaselineShift
import com.skyd.htmlrender.base.model.TextStyler
import com.skyd.htmlrender.core.styler.SpanStyleStyler

class VerticalAlignCssAnnotatedHandler : CSSAnnotatedHandler() {
    override fun addStyle(list: MutableList<TextStyler>, value: String) {
        val shift = when (value.lowercase()) {
            "super" -> BaselineShift.Superscript
            "sub" -> BaselineShift.Subscript
            else -> return
        }
        list += SpanStyleStyler { SpanStyle(baselineShift = shift) }
    }
}
