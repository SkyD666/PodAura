package com.skyd.htmlrender.ui.handler

import com.fleeksoft.ksoup.nodes.Node
import com.skyd.htmlrender.base.css.model.CSSDeclaration
import com.skyd.htmlrender.base.handler.ListItemHandler
import com.skyd.htmlrender.base.model.TextStyler
import com.skyd.htmlrender.ui.styler.OrderedListStyler
import com.skyd.htmlrender.ui.styler.UnorderedListStyler

open class ListItemAnnotatedHandler : ListItemHandler() {
    override fun addUnorderedItem(
        list: MutableList<TextStyler>,
        node: Node,
        cssDeclarations: List<CSSDeclaration>?,
        parent: Node
    ) {
        list.add(UnorderedListStyler())
    }

    override fun addOrderedItem(
        list: MutableList<TextStyler>,
        node: Node,
        cssDeclarations: List<CSSDeclaration>?,
        parent: Node,
        index: Int
    ) {
        list.add(OrderedListStyler(index))
    }
}