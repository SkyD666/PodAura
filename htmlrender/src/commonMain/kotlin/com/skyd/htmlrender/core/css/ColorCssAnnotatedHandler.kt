package com.skyd.htmlrender.core.css

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import co.touchlab.kermit.Logger
import com.skyd.htmlrender.core.styler.SpanStyleStyler
import com.skyd.htmlrender.base.model.TextStyler

open class ColorCssAnnotatedHandler : CSSAnnotatedHandler() {

    override fun addStyle(list: MutableList<TextStyler>, value: String) {
        parseColor(value)?.also { color ->
            list.add(SpanStyleStyler { SpanStyle(color = color) })
        }
    }

    private val parser by lazy { CSSColorParser() }

    internal open fun parseColor(cssColor: String): Color? =
        parser.parseColor(cssColor).also { color ->
            if (color == null) {
                Logger.w(MODULE) {
                    "unsupported parse color: $cssColor"
                }
            }
        }

    companion object {
        const val MODULE = "ColorCssAnnotatedHandler"
    }
}