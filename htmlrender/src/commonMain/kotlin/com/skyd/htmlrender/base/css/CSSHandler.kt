package com.skyd.htmlrender.base.css

import com.skyd.htmlrender.base.model.TextStyler


interface CSSHandler {
    fun addStyle(list: MutableList<TextStyler>, value: String)
}