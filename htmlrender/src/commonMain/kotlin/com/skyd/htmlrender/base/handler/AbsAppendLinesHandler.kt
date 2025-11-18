package com.skyd.htmlrender.base.handler

import com.fleeksoft.ksoup.nodes.Node
import com.skyd.htmlrender.base.css.model.CSSDeclaration
import com.skyd.htmlrender.base.model.TextStyler
import com.skyd.htmlrender.core.StyleConfig

abstract class AbsAppendLinesHandler(private val amount: Int) : TagHandler() {
    override fun addTagStylers(
        list: MutableList<TextStyler>,
        node: Node,
        cssDeclarations: List<CSSDeclaration>?,
        styleConfig: StyleConfig,
    ) {
        val styler = getNewLineStyler()
        repeat(amount) {
            list.add(styler)
        }
    }

    abstract fun getNewLineStyler(): TextStyler
}