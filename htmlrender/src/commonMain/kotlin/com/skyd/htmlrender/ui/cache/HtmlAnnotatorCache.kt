package com.skyd.htmlrender.ui.cache

import com.skyd.htmlrender.ui.RawHtmlData

interface HtmlAnnotatorCache<R> {
    fun put(src: RawHtmlData, result: R)
    fun get(src: RawHtmlData): R?
}