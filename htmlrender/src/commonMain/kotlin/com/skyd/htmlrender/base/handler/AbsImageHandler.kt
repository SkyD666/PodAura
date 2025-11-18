package com.skyd.htmlrender.base.handler


import com.fleeksoft.ksoup.nodes.Node
import com.skyd.htmlrender.base.css.model.CSSDeclaration
import com.skyd.htmlrender.base.model.TextStyler
import com.skyd.htmlrender.core.StyleConfig

abstract class AbsImageHandler : TagHandler() {
    override fun addTagStylers(
        list: MutableList<TextStyler>,
        node: Node,
        cssDeclarations: List<CSSDeclaration>?,
        styleConfig: StyleConfig
    ) {
        list.add(
            getImageStyler(
                node.attr("src"),
                cssDeclarations
            )
        )
    }

    abstract fun getImageStyler(
        imageUrl: String,
        cssDeclarations: List<CSSDeclaration>?,
    ): ImageStyler
}

abstract class ImageStyler(val imageUrl: String) : TextStyler {
    companion object {
        const val PLACE_HOLDER = "\uD83D\uDDBC\uFE0F"
    }
}