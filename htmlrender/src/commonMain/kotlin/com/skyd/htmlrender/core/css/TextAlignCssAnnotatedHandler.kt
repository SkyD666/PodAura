package com.skyd.htmlrender.core.css

import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.style.TextAlign
import co.touchlab.kermit.Logger
import com.skyd.htmlrender.base.model.TextStyler
import com.skyd.htmlrender.core.styler.ParagraphStyleStyler

open class TextAlignCssAnnotatedHandler : CSSAnnotatedHandler() {
    override fun addStyle(list: MutableList<TextStyler>, value: String) {
        parse(value)?.also { align ->
            list.add(ParagraphStyleStyler { ParagraphStyle(textAlign = align) })
        }
    }

    internal open fun parse(value: String): TextAlign? = when (value) {
        "start" -> TextAlign.Start
        "end" -> TextAlign.End
        "left" -> TextAlign.Left
        "right" -> TextAlign.Right
        "center" -> TextAlign.Center
        "justify", "justify-all" -> TextAlign.Justify
        else -> {
            Logger.w(MODULE) {
                "parse TextAlign fail: $value"
            }
            null
        }
    }


    companion object {
        const val MODULE = "TextAlignCssAnnotatedHandler"
    }
}