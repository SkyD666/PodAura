package com.skyd.htmlrender.core.styler

import androidx.compose.ui.text.AnnotatedString


object NewLineStyler : IAtChildrenBeforeAnnotatedStyler {

    override fun atChildrenBefore(builder: AnnotatedString.Builder) {
        builder.appendLine()
    }
}

