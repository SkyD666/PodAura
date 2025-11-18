package com.skyd.htmlrender.core.styler

import androidx.compose.ui.text.AnnotatedString


data class ParagraphStartStyler(val extraLine: Boolean) : IBeforeChildrenAnnotatedStyler {
    override fun beforeChildren(builder: AnnotatedString.Builder) {
        if (builder.length != 0) {
            builder.appendLine()
            if (extraLine) {
                builder.appendLine()
            }
        }
    }
}

data class ParagraphEndStyler(val extraLine: Boolean) : IAfterChildrenAnnotatedStyler {
    override fun afterChildren(builder: AnnotatedString.Builder) {
        builder.appendLine()
        if (extraLine) {
            builder.appendLine()
        }
    }
}
