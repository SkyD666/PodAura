package com.skyd.htmlrender.core.css

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import co.touchlab.kermit.Logger
import com.skyd.htmlrender.core.styler.SpanStyleStyler
import com.skyd.htmlrender.base.model.TextStyler

open class BackgroundColorCssAnnotatedHandler : CSSAnnotatedHandler() {

    override fun addStyle(list: MutableList<TextStyler>, value: String) {
        parseColor(value)?.also { color ->
            list.add(SpanStyleStyler { SpanStyle(background = color) })
        }
    }

    private val parser by lazy { CSSColorParser() }

    internal open fun parseColor(cssColor: String): Color? = if (cssColor == "transparent") {
        Color.Transparent
    } else {
        parser.parseColor(cssColor).also { color ->
            if (color == null) {
                Logger.w(MODULE) {
                    "unsupported parse background color: $cssColor"
                }
            }
        }
    }


    companion object {
        const val MODULE = "BackgroundColorCssAnnotatedHandler"
    }
}