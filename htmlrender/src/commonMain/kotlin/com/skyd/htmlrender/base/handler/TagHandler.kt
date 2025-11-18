package com.skyd.htmlrender.base.handler

import com.fleeksoft.ksoup.nodes.Node
import com.skyd.htmlrender.base.css.model.CSSDeclaration
import com.skyd.htmlrender.base.model.HtmlNode
import com.skyd.htmlrender.base.model.TextStyler
import com.skyd.htmlrender.core.StyleConfig

open class TagHandler {

    open fun addTagStylers(
        list: MutableList<TextStyler>,
        node: Node,
        cssDeclarations: List<CSSDeclaration>?,
        styleConfig: StyleConfig,
    ) {
    }


    open val handleChildrenNode: ((node: Node, cssDeclarations: List<CSSDeclaration>?) -> List<HtmlNode>)? =
        null

}