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
        val attribute = if (node.hasAttr("href")) "href" else "src"
        list.add(
            getUrlStyler(
                node.absUrl(attribute).ifBlank { node.attr(attribute) },
                cssDeclarations
            )
        )
    }


    abstract fun getUrlStyler(url: String, cssDeclarations: List<CSSDeclaration>?): TextStyler
}
