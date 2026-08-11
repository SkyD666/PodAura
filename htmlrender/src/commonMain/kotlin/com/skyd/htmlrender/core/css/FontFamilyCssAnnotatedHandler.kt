package com.skyd.htmlrender.core.css

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import com.skyd.htmlrender.base.model.TextStyler
import com.skyd.htmlrender.core.styler.SpanStyleStyler

class FontFamilyCssAnnotatedHandler : CSSAnnotatedHandler() {
    override fun addStyle(list: MutableList<TextStyler>, value: String) {
        val normalized = value.lowercase()
        val family = when {
            "monospace" in normalized -> FontFamily.Monospace
            "serif" in normalized && "sans-serif" !in normalized -> FontFamily.Serif
            "sans-serif" in normalized -> FontFamily.SansSerif
            else -> null
        } ?: return
        list += SpanStyleStyler { SpanStyle(fontFamily = family) }
    }
}
