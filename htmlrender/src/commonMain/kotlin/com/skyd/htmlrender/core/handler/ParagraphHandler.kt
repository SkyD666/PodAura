package com.skyd.htmlrender.core.handler

import com.fleeksoft.ksoup.nodes.Node
import com.skyd.htmlrender.base.css.model.CSSDeclaration
import com.skyd.htmlrender.base.handler.TagHandler
import com.skyd.htmlrender.base.model.TextStyler
import com.skyd.htmlrender.core.StyleConfig
import com.skyd.htmlrender.core.styler.ParagraphEndStyler
import com.skyd.htmlrender.core.styler.ParagraphStartStyler
import com.skyd.htmlrender.core.styler.SpanStyleStyler

open class ParagraphHandler(private val extraLine: Boolean) : TagHandler() {
    override fun addTagStylers(
        list: MutableList<TextStyler>,
        node: Node,
        cssDeclarations: List<CSSDeclaration>?,
        styleConfig: StyleConfig
    ) {
        with(list) {
            add(ParagraphStartStyler(extraLine))
            add(ParagraphEndStyler(extraLine))
            add(SpanStyleStyler { styleConfig.textStyle.toSpanStyle() })
        }
    }
}