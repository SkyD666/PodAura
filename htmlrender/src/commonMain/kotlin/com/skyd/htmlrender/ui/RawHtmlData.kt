package com.skyd.htmlrender.ui

import com.skyd.htmlrender.core.StyleConfig

data class RawHtmlData(
    val srcHtml: String,
    val styleConfig: StyleConfig = StyleConfig.Companion.Default,
)