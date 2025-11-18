package com.skyd.htmlrender.core.handler

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import com.skyd.htmlrender.core.styler.SpanStyleStyler
import com.skyd.htmlrender.base.handler.AbsPreHandler
import com.skyd.htmlrender.base.model.TextStyler

class PreAnnotatedHandler : AbsPreHandler() {
    override fun getMonospaceStyler(): TextStyler = SpanStyleStyler {
        SpanStyle(fontFamily = FontFamily.Monospace)
    }
}