package com.skyd.htmlrender.core.styler

import androidx.compose.ui.text.ParagraphStyle

class ParagraphStyleStyler(val getSpan: () -> ParagraphStyle) : IParagraphStyleStyler {
    override fun getParagraphStyle(): ParagraphStyle = getSpan()
}

