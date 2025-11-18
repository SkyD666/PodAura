package com.skyd.htmlrender.base.handler

import com.fleeksoft.ksoup.nodes.Node
import com.skyd.htmlrender.base.css.model.CSSDeclaration
import com.skyd.htmlrender.base.model.TextStyler
import com.skyd.htmlrender.core.StyleConfig

abstract class ListItemHandler : TagHandler() {

    private fun getMyIndex(node: Node): Int? {
        val parent = node.parent() ?: return null
        var i = 1
        for (child in parent.childNodes()) {
            if (child === node) {
                return i
            }
            if ("li" == child.nodeName()) {
                i++
            }
        }
        return null
    }

    override fun addTagStylers(
        list: MutableList<TextStyler>,
        node: Node,
        cssDeclarations: List<CSSDeclaration>?,
        styleConfig: StyleConfig
    ) {
        val parent = node.parent() ?: return
        when (parent.nodeName()) {
            "ul" -> {
                addUnorderedItem(list, node, cssDeclarations, parent)
            }

            "ol" -> {
                val index = getMyIndex(node) ?: return
                addOrderedItem(list, node, cssDeclarations, parent, index)
            }
        }
    }

    abstract fun addUnorderedItem(
        list: MutableList<TextStyler>,
        node: Node,
        cssDeclarations: List<CSSDeclaration>?,
        parent: Node
    )

    abstract fun addOrderedItem(
        list: MutableList<TextStyler>,
        node: Node,
        cssDeclarations: List<CSSDeclaration>?,
        parent: Node,
        index: Int
    )
}