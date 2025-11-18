package com.skyd.htmlrender.core.handler

import androidx.compose.ui.text.ParagraphStyle
import com.fleeksoft.ksoup.nodes.Node
import com.skyd.htmlrender.base.css.model.CSSDeclaration
import com.skyd.htmlrender.base.model.TextStyler
import com.skyd.htmlrender.core.StyleConfig
import com.skyd.htmlrender.core.styler.ParagraphStyleStyler


class ParagraphStyleHandler(
    private val isParagraph: Boolean = true,
    addExtraLine: Boolean = true,
    private val newStyle: (StyleConfig) -> ParagraphStyle
) : ParagraphHandler(addExtraLine) {
    override fun addTagStylers(
        list: MutableList<TextStyler>,
        node: Node,
        cssDeclarations: List<CSSDeclaration>?,
        styleConfig: StyleConfig
    ) {
        if (isParagraph) {
            super.addTagStylers(list, node, cssDeclarations, styleConfig)
        }
        list.add(ParagraphStyleStyler { newStyle(styleConfig) })
    }
}