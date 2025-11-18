package com.skyd.htmlrender.base.model

interface NodeProcessor {
    suspend fun processNode(node: HtmlNode)
}