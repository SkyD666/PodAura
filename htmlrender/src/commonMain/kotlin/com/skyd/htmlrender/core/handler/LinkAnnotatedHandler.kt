package com.skyd.htmlrender.core.handler

import com.skyd.htmlrender.base.css.model.CSSDeclaration
import com.skyd.htmlrender.base.handler.AbsLinkHandler
import com.skyd.htmlrender.base.model.TextStyler
import com.skyd.htmlrender.core.styler.LinkAnnotatedStyler

open class LinkAnnotatedHandler : AbsLinkHandler() {
    override fun getUrlStyler(url: String, cssDeclarations: List<CSSDeclaration>?): TextStyler =
        LinkAnnotatedStyler(url)
}