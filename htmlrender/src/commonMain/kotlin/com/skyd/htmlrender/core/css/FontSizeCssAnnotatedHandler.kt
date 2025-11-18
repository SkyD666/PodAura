package com.skyd.htmlrender.core.css

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.TextUnit
import co.touchlab.kermit.Logger
import com.skyd.htmlrender.core.styler.SpanStyleStyler
import com.skyd.htmlrender.base.model.TextStyler
import com.skyd.htmlrender.core.util.TextUnitParser

open class FontSizeCssAnnotatedHandler : CSSAnnotatedHandler() {
    override fun addStyle(list: MutableList<TextStyler>, value: String) {
        parse(value)?.also { size ->
            list.add(SpanStyleStyler { SpanStyle(fontSize = size) })
        }
    }

    internal open fun parse(value: String): TextUnit? = runCatching {
        TextUnitParser.parse(value).also {
            if (it == null) {
                logFail(value)
            }
        }
    }.onFailure {
        logFail(value, it)
    }.getOrNull()

    private fun logFail(value: String, throwable: Throwable? = null) {
        Logger.w(MODULE, throwable) {
            "parse FontSize fail: $value"
        }
    }

    companion object {
        const val MODULE = "FontSizeCssAnnotatedHandler"
    }
}

