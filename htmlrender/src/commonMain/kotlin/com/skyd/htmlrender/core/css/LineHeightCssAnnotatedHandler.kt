package com.skyd.htmlrender.core.css

import androidx.compose.ui.text.ParagraphStyle
import com.skyd.htmlrender.base.model.TextStyler
import com.skyd.htmlrender.core.styler.ParagraphStyleStyler
import com.skyd.htmlrender.core.util.TextUnitParser

class LineHeightCssAnnotatedHandler : CSSAnnotatedHandler() {
    override fun addStyle(list: MutableList<TextStyler>, value: String) {
        val lineHeight = runCatching { TextUnitParser.parse(value, unitlessAsEm = true) }.getOrNull()
            ?: return
        if (!lineHeight.value.isFinite() || lineHeight.value < 0f) return
        list += ParagraphStyleStyler { ParagraphStyle(lineHeight = lineHeight) }
    }
}
