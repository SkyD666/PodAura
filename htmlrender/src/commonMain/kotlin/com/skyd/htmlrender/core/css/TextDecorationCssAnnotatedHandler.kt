package com.skyd.htmlrender.core.css

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextDecoration
import co.touchlab.kermit.Logger
import com.skyd.htmlrender.core.styler.SpanStyleStyler
import com.skyd.htmlrender.base.model.TextStyler

open class TextDecorationCssAnnotatedHandler : CSSAnnotatedHandler() {
    override fun addStyle(list: MutableList<TextStyler>, value: String) {
        parse(value)?.also { decoration ->
            list.add(SpanStyleStyler { SpanStyle(textDecoration = decoration) })
        }
    }

    internal open fun parse(value: String): TextDecoration? = runCatching {
        val list = buildList {
            if (value.contains("underline")) {
                add(TextDecoration.Underline)
            }
            if (value.contains("line-through")) {
                add(TextDecoration.LineThrough)
            }
        }
        if (list.isEmpty()) {
            logFail(value)
            null
        } else {
            TextDecoration.combine(list)
        }
    }.onFailure {
        logFail(value, it)
    }.getOrNull()

    private fun logFail(value: String, throwable: Throwable? = null) {
        Logger.w(MODULE, throwable) {
            "parse Text Decoration fail: $value"
        }
    }

    companion object {
        const val MODULE = "TextDecorationCssAnnotatedHandler"
    }
}