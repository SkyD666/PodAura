package com.skyd.htmlrender.base.css.model

data class CSSStyleBlock(val start: Int, val end: Int, val declarations: List<CSSDeclaration>)