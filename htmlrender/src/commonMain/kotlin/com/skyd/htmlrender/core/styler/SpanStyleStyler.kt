package com.skyd.htmlrender.core.styler

import androidx.compose.ui.text.SpanStyle

class SpanStyleStyler(val getSpan: () -> SpanStyle) : ISpanStyleStyler {
    override fun getSpanStyler(): SpanStyle = getSpan()
}