package com.skyd.htmlrender.base

import co.touchlab.kermit.Logger
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.nodes.Element
import com.fleeksoft.ksoup.nodes.Node
import com.fleeksoft.ksoup.nodes.TextNode
import com.skyd.htmlrender.base.css.CSSHandler
import com.skyd.htmlrender.base.css.model.CSSDeclaration
import com.skyd.htmlrender.base.css.model.CSSDeclarationWithPriority
import com.skyd.htmlrender.base.css.model.CSSRuleSet
import com.skyd.htmlrender.base.css.model.StyleOrigin
import com.skyd.htmlrender.base.css.parseCssDeclarations
import com.skyd.htmlrender.base.css.parseCssRuleBlock
import com.skyd.htmlrender.base.handler.AbsImageHandler
import com.skyd.htmlrender.base.handler.TagHandler
import com.skyd.htmlrender.base.model.HtmlNode
import com.skyd.htmlrender.base.model.StringNode
import com.skyd.htmlrender.base.model.StyleNode
import com.skyd.htmlrender.base.model.TextStyler
import com.skyd.htmlrender.core.StyleConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

private const val TAG = "HtmlAnnotationBuilder"

suspend fun toHtmlNode(
    doc: Document,
    tagHandles: Map<String, TagHandler>,
    cssHandles: Map<String, CSSHandler>,
    getExternalCSS: (suspend (link: String) -> String)?,
    styleConfig: StyleConfig,
): HtmlNode = withContext(Dispatchers.Default) {
    val body = doc.body()
    ensureActive()
    val externalCssJob = async {
        getExternalCSS?.let { get ->
            buildExternalCSSBlock(get, doc)
        }
    }
    val internalCSS = buildInternalCSSBlock(doc)
    val externalCss = externalCssJob.await()
    val cssMap = if (internalCSS != null || externalCss != null) {
        val map = mutableMapOf<Element, MutableList<CSSRuleSet>>()
        fun List<CSSRuleSet>.putToCssMap() {
            forEach { rule ->
                runCatching {
                    body.select(rule.selector).forEach { e ->
                        map.getOrPut(e) { mutableListOf() }.add(rule)
                    }
                }.onFailure {
                    Logger.e(
                        throwable = it,
                        tag = TAG
                    ) { "unsupported css selector: ${rule.selector}" }
                }
            }
        }
        internalCSS?.putToCssMap()
        externalCss?.putToCssMap()
        ensureActive()
        map
    } else {
        null
    }

    val textFlow = TextFlowState()

    fun handleNode(node: Node): HtmlNode? {
        ensureActive()
        if (node is TextNode) {
            return textFlow.normalize(node.text())?.let(::StringNode)
        }

        val name = node.nodeName()
        val handler = tagHandles[name]
        val isTextBoundary = node is Element && (!node.tag().isInline() || node.nameIs("br"))
        if (isTextBoundary) {
            textFlow.reset()
        }
        if (handler == null && name != "body" && name != "#comment") {
            Logger.w(tag = TAG) { "unsupported node:${node.nodeName()}" }
        }
        val cssDeclarations = buildFinalCSS(cssMap, node)

        val stylers = ArrayList<TextStyler>().apply {
            handler?.run {
                addTagStylers(this@apply, node, cssDeclarations, styleConfig)
            }
            cssDeclarations?.let { declarations ->
                declarations.forEach { css ->
                    cssHandles[css.property]?.run {
                        addStyle(this@apply, css.value)
                    }
                }
            }
        }.ifEmpty { null }

        val children = ArrayList<HtmlNode>().apply {
            handler?.handleChildrenNode.let { handle ->
                if (handle != null) {
                    handle(node, cssDeclarations)
                } else {
                    for (childNode in node.childNodes()) {
                        val childHandler = tagHandles[childNode.nodeName()]
                        if (childNode is Element && childHandler is AbsImageHandler) {
                            textFlow.consumeWhitespace()?.let { add(StringNode(it)) }
                            handleNode(childNode)?.also(::add)
                            textFlow.markContent()
                        } else {
                            handleNode(childNode)?.also(::add)
                        }
                    }
                }
            }
        }

        return StyleNode(stylers, children).also {
            if (isTextBoundary) {
                textFlow.reset()
            }
        }
    }

    requireNotNull(handleNode(body))
}

private class TextFlowState {
    private var hasContent = false
    private var whitespacePending = false

    fun normalize(text: String): String? {
        val content = text.trim(' ')
        if (content.isEmpty()) {
            if (hasContent && text.isNotEmpty()) {
                whitespacePending = true
            }
            return null
        }

        val needsLeadingSpace = hasContent && (whitespacePending || text.startsWith(' '))
        hasContent = true
        whitespacePending = text.endsWith(' ')
        return if (needsLeadingSpace) " $content" else content
    }

    fun consumeWhitespace(): String? {
        if (!hasContent || !whitespacePending) return null
        whitespacePending = false
        return " "
    }

    fun markContent() {
        hasContent = true
    }

    fun reset() {
        hasContent = false
        whitespacePending = false
    }
}

private suspend fun buildExternalCSSBlock(
    getExternalCSS: (suspend (link: String) -> String),
    doc: Document
): List<CSSRuleSet> = withContext(Dispatchers.Default) {
    doc.select("link[rel=stylesheet]").map { e ->
        async(Dispatchers.IO) {
            getExternalCSS(e.attr("href"))
        }
    }.awaitAll().joinToString("\n").let { css ->
        parseCssRuleBlock(StyleOrigin.EXTERNAL, css)
    }
}

private fun buildInternalCSSBlock(doc: Document): List<CSSRuleSet>? =
    parseCssRuleBlock(StyleOrigin.INTERNAL, doc.select("style").joinToString("\n") {
        it.html()
    }).ifEmpty { null }

private fun buildFinalCSS(
    cssMap: Map<Element, MutableList<CSSRuleSet>>?,
    node: Node
): List<CSSDeclaration>? {
    val noInlineCSS = cssMap?.get(node)
    val inlineCSS = node.attr("style").ifBlank { null }?.let(::parseCssDeclarations)

    if (inlineCSS == null && noInlineCSS == null) {
        return null
    } else if (inlineCSS != null && noInlineCSS == null) {
        return inlineCSS
    }

    val finalCssMap = mutableMapOf<String, CSSDeclarationWithPriority>()

    fun CSSDeclarationWithPriority.compareAndPutMap() {
        val mapValue = finalCssMap[property]
        if (mapValue == null || this >= mapValue) {
            finalCssMap[property] = this
        }
    }

    inlineCSS?.forEach { declaration ->
        CSSDeclarationWithPriority(declaration, StyleOrigin.INLINE).compareAndPutMap()
    }
    noInlineCSS?.mapIndexed { index, ruleSet ->
        ruleSet.declarations.forEach { declaration ->
            CSSDeclarationWithPriority(
                declaration,
                ruleSet.origin,
                ruleSet.selector,
                index
            ).compareAndPutMap()
        }
    }
    return finalCssMap.values.toList()
}
