package com.skyd.htmlrender.base.handler

import com.fleeksoft.ksoup.nodes.Node
import com.skyd.htmlrender.base.css.model.CSSDeclaration
import com.skyd.htmlrender.base.model.HtmlNode
import com.skyd.htmlrender.base.model.StringNode
import com.skyd.htmlrender.base.model.StyleNode
import com.skyd.htmlrender.base.model.TextStyler

abstract class AbsPreHandler : TagHandler() {

    override val handleChildrenNode: ((node: Node, cssDeclarations: List<CSSDeclaration>?) -> List<HtmlNode>)? =
        { node, _ ->
            val nodeLength = node.nodeName().length
            val contentHtml = buildString {
                node.outerHtml().let { html ->
                    html.substring(nodeLength + 2, html.length - nodeLength - 3)
                }.also(::append)
                repeat(2) {
                    append('\n')
                }
            }

            listOf(
                StyleNode(
                    mutableListOf(getMonospaceStyler()),
                    mutableListOf(StringNode(contentHtml))
                )
            )
        }


    abstract fun getMonospaceStyler(): TextStyler
}