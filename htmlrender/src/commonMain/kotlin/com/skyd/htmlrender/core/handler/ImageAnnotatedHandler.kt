package com.skyd.htmlrender.core.handler

import com.skyd.htmlrender.base.css.model.CSSDeclaration
import com.skyd.htmlrender.base.handler.AbsImageHandler
import com.skyd.htmlrender.base.handler.ImageStyler
import com.skyd.htmlrender.core.styler.ImageAnnotatedStyler

class ImageAnnotatedHandler : AbsImageHandler() {
    override fun getImageStyler(
        imageUrl: String,
        cssDeclarations: List<CSSDeclaration>?
    ): ImageStyler = ImageAnnotatedStyler(imageUrl)
}