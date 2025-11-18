package com.skyd.htmlrender.base.handler

import com.fleeksoft.ksoup.nodes.Node
import com.skyd.htmlrender.base.css.model.CSSDeclaration
import com.skyd.htmlrender.base.model.TextStyler
import com.skyd.htmlrender.core.StyleConfig

abstract class AbsLinkHandler : TagHandler() {

    override fun addTagStylers(
        list: MutableList<TextStyler>,
        node: Node,
        cssDeclarations: List<CSSDeclaration>?,
        styleConfig: StyleConfig
    ) {
        list.add(
            getUrlStyler(
                node.attr("href").ifBlank { node.attr("src") },
                cssDeclarations
            )
        )
    }


    abstract fun getUrlStyler(url: String, cssDeclarations: List<CSSDeclaration>?): TextStyler
}