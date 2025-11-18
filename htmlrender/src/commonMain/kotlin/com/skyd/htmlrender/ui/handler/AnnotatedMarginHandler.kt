package com.skyd.htmlrender.ui.handler

import com.fleeksoft.ksoup.nodes.Node
import com.skyd.htmlrender.ui.styler.MarginStyler
import com.skyd.htmlrender.base.css.model.CSSDeclaration
import com.skyd.htmlrender.base.handler.TagHandler
import com.skyd.htmlrender.base.model.TextStyler
import com.skyd.htmlrender.core.StyleConfig

open class AnnotatedMarginHandler(val addMargin: () -> List<MarginStyler>) : TagHandler() {
    override fun addTagStylers(
        list: MutableList<TextStyler>,
        node: Node,
        cssDeclarations: List<CSSDeclaration>?,
        styleConfig: StyleConfig
    ) {
        list.addAll(addMargin())
    }
}