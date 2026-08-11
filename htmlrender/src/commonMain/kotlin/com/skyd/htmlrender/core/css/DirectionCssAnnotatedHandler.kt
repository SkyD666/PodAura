package com.skyd.htmlrender.core.css

import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.style.TextDirection
import com.skyd.htmlrender.base.model.TextStyler
import com.skyd.htmlrender.core.styler.ParagraphStyleStyler

class DirectionCssAnnotatedHandler : CSSAnnotatedHandler() {
    override fun addStyle(list: MutableList<TextStyler>, value: String) {
        val direction = when (value.lowercase()) {
            "ltr" -> TextDirection.Ltr
            "rtl" -> TextDirection.Rtl
            else -> return
        }
        list += ParagraphStyleStyler { ParagraphStyle(textDirection = direction) }
    }
}
