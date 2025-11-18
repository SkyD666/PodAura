@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package com.skyd.htmlrender.core

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.sp
import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import com.skyd.htmlrender.base.handler.TagHandler
import com.skyd.htmlrender.base.model.HtmlNode
import com.skyd.htmlrender.base.model.NodeProcessor
import com.skyd.htmlrender.base.model.StringNode
import com.skyd.htmlrender.base.model.StyleNode
import com.skyd.htmlrender.base.toHtmlNode
import com.skyd.htmlrender.core.css.BackgroundColorCssAnnotatedHandler
import com.skyd.htmlrender.core.css.CSSAnnotatedHandler
import com.skyd.htmlrender.core.css.ColorCssAnnotatedHandler
import com.skyd.htmlrender.core.css.FontSizeCssAnnotatedHandler
import com.skyd.htmlrender.core.css.FontStyleCssAnnotatedHandler
import com.skyd.htmlrender.core.css.FontWeightCssAnnotatedHandler
import com.skyd.htmlrender.core.css.TextAlignCssAnnotatedHandler
import com.skyd.htmlrender.core.css.TextDecorationCssAnnotatedHandler
import com.skyd.htmlrender.core.css.TextIndentCssAnnotatedHandler
import com.skyd.htmlrender.core.handler.AppendLinesHandler
import com.skyd.htmlrender.core.handler.ImageAnnotatedHandler
import com.skyd.htmlrender.core.handler.LinkAnnotatedHandler
import com.skyd.htmlrender.core.handler.ParagraphHandler
import com.skyd.htmlrender.core.handler.ParagraphStyleHandler
import com.skyd.htmlrender.core.handler.PreAnnotatedHandler
import com.skyd.htmlrender.core.handler.SpanStyleHandler
import com.skyd.htmlrender.core.processor.ParagraphNodeProcessor
import com.skyd.htmlrender.core.styler.IAfterChildrenAnnotatedStyler
import com.skyd.htmlrender.core.styler.IAtChildrenAfterAnnotatedStyler
import com.skyd.htmlrender.core.styler.IAtChildrenBeforeAnnotatedStyler
import com.skyd.htmlrender.core.styler.IBeforeChildrenAnnotatedStyler
import com.skyd.htmlrender.core.styler.IParagraphStyleStyler
import com.skyd.htmlrender.core.styler.ISpanStyleStyler
import com.skyd.htmlrender.core.styler.IStringAnnotationStyler
import com.skyd.htmlrender.core.styler.IUrlAnnotationStyler
import com.skyd.htmlrender.core.util.ParagraphInterval
import com.skyd.htmlrender.core.util.buildNotOverlapList
import com.skyd.htmlrender.ui.RawHtmlData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

class HtmlAnnotator(
    preTagHandlers: Map<String, TagHandler>? = defaultPreTagHandlers,
    preCSSHandlers: Map<String, CSSAnnotatedHandler>? = defaultPreCSSHandlers,
    val nodeProcessors: List<NodeProcessor> = preNodeProcessors
) {

    private val handlers = HashMap<String, TagHandler>()

    private val cssHandlers = HashMap<String, CSSAnnotatedHandler>()

    init {
        registerBuiltInHandlers(preTagHandlers)
        registerBuiltInCssHandlers(preCSSHandlers)
    }

    fun registerHandler(tagName: String, handler: TagHandler) {
        handlers[tagName] = handler
    }

    fun unregisterHandler(tagName: String) {
        handlers.remove(tagName)
    }

    fun registerCssHandler(property: String, handler: CSSAnnotatedHandler) {
        cssHandlers[property] = handler
    }

    fun unregisterCssHandler(property: String) {
        cssHandlers.remove(property)
    }

    suspend fun from(
        rawHtmlData: RawHtmlData,
        baseUri: String = "",
        getExternalCSS: (suspend (link: String) -> String)? = null
    ): AnnotatedString = from(
        doc = Ksoup.parse(rawHtmlData.srcHtml, baseUri),
        styleConfig = rawHtmlData.styleConfig,
        getExternalCSS = getExternalCSS,
    )

    suspend fun from(
        doc: Document,
        styleConfig: StyleConfig,
        getExternalCSS: (suspend (link: String) -> String)? = null
    ): AnnotatedString = withContext(Dispatchers.Default) {
        val root = toHtmlNode(doc, handlers, cssHandlers, getExternalCSS, styleConfig)
        AnnotatedString.Builder().apply {
            val paragraphIntervalList = ArrayList<ParagraphInterval>()

            nodeProcessors.forEach { processor ->
                processor.processNode(root)
            }

            handleNode(root, paragraphIntervalList, styleConfig)

            paragraphIntervalList.buildNotOverlapList(length).forEach { e ->
                addStyle(e.style, e.start, e.end)
            }
        }.toAnnotatedString()
    }

    private suspend fun AnnotatedString.Builder.handleNode(
        node: HtmlNode,
        paragraphIntervalList: ArrayList<ParagraphInterval>,
        styleConfig: StyleConfig,
    ) {
        currentCoroutineContext().ensureActive()
        when (node) {
            is StringNode -> {
                append(node.string)
            }

            is StyleNode -> {
                handleStyleNode(node, paragraphIntervalList, styleConfig)
            }
        }
    }

    @OptIn(ExperimentalTextApi::class)
    private suspend fun AnnotatedString.Builder.handleStyleNode(
        node: StyleNode,
        paragraphIntervalList: ArrayList<ParagraphInterval>,
        styleConfig: StyleConfig,
    ) {
        suspend fun appendChildren() {
            node.children?.forEach { child ->
                handleNode(child, paragraphIntervalList, styleConfig)
            }
        }

        val stylers = node.stylers?.asSequence()
        if (stylers == null) {
            appendChildren()
            return
        }

        val paragraphStylerList =
            stylers.filterIsInstance<IParagraphStyleStyler>().toList()
        val spanStylerList = stylers.filterIsInstance<ISpanStyleStyler>().toList()
        val stringStylerList =
            stylers.filterIsInstance<IStringAnnotationStyler>().toList()
        val urlStylerList = stylers.filterIsInstance<IUrlAnnotationStyler>().toList()

        stylers.filterIsInstance<IBeforeChildrenAnnotatedStyler>().forEach {
            it.beforeChildren(this)
        }

        spanStylerList.forEach {
            pushStyle(it.getSpanStyler())
        }
        stringStylerList.forEach {
            pushStringAnnotation(it.getTag(), it.getAnnotation())
        }
        urlStylerList.forEach {
            pushLink(
                it.getUrlAnnotation(
                    linkStyles = styleConfig.linkStyles,
                    uriHandler = styleConfig.uriHandler,
                )
            )
        }

        val popNum = spanStylerList.size + stringStylerList.size + urlStylerList.size

        val startIndex = length

        stylers.filterIsInstance<IAtChildrenBeforeAnnotatedStyler>().forEach {
            it.atChildrenBefore(this)
        }

        appendChildren()

        stylers.filterIsInstance<IAtChildrenAfterAnnotatedStyler>().forEach {
            it.atChildrenAfter(this)
        }

        repeat(popNum) {
            pop()
        }

        val endIndex = length
        paragraphStylerList.map { styler ->
            ParagraphInterval(startIndex, endIndex, styler.getParagraphStyle())
        }.forEach { e ->
            paragraphIntervalList.add(e)
            e.priority = paragraphStylerList.size
        }

        stylers.filterIsInstance<IAfterChildrenAnnotatedStyler>().forEach {
            it.afterChildren(this)
        }
    }

    private fun registerBuiltInHandlers(pre: Map<String, TagHandler>?) {
        pre?.also { map ->
            handlers.putAll(map)
        }

        fun registerHandlerIfAbsent(tag: String, getHandler: () -> TagHandler) {
            if (pre?.containsKey(tag) != true) {
                registerHandler(tag, getHandler())
            }
        }

        val italicHandler by lazy {
            SpanStyleHandler(false) { styleConfig ->
                styleConfig.textStyle.toSpanStyle().copy(fontStyle = FontStyle.Italic)
            }
        }

        registerHandlerIfAbsent("i") { italicHandler }
        registerHandlerIfAbsent("em") { italicHandler }
        registerHandlerIfAbsent("cite") { italicHandler }
        registerHandlerIfAbsent("dfn") { italicHandler }

        val boldHandler by lazy {
            SpanStyleHandler(false) { styleConfig ->
                styleConfig.textStyle.toSpanStyle().copy(fontWeight = FontWeight.Bold)
            }
        }

        registerHandlerIfAbsent("b") { boldHandler }
        registerHandlerIfAbsent("strong") { boldHandler }

        val marginHandler by lazy {
            ParagraphStyleHandler { styleConfig ->
                styleConfig.textStyle.toParagraphStyle().copy(textIndent = TextIndent(4.sp, 4.sp))
            }
        }
        registerHandlerIfAbsent("blockquote") { marginHandler }

        registerHandlerIfAbsent("br") { AppendLinesHandler(1) }

        registerHandlerIfAbsent("p") { ParagraphHandler(true) }
        registerHandlerIfAbsent("div") { ParagraphHandler(false) }

        registerHandlerIfAbsent("h1") {
            SpanStyleHandler { styleConfig ->
                styleConfig.textStyle.toSpanStyle().run {
                    copy(fontWeight = FontWeight.Bold, fontSize = fontSize * 2)
                }
            }
        }
        registerHandlerIfAbsent("h2") {
            SpanStyleHandler { styleConfig ->
                styleConfig.textStyle.toSpanStyle().run {
                    copy(fontWeight = FontWeight.Bold, fontSize = fontSize * 1.5)
                }
            }
        }
        registerHandlerIfAbsent("h3") {
            SpanStyleHandler { styleConfig ->
                styleConfig.textStyle.toSpanStyle().run {
                    copy(fontWeight = FontWeight.Bold, fontSize = fontSize * 1.17)
                }
            }
        }
        registerHandlerIfAbsent("h4") {
            SpanStyleHandler { styleConfig ->
                styleConfig.textStyle.toSpanStyle().copy(fontWeight = FontWeight.Bold)
            }
        }
        registerHandlerIfAbsent("h5") {
            SpanStyleHandler { styleConfig ->
                styleConfig.textStyle.toSpanStyle().run {
                    copy(fontWeight = FontWeight.Bold, fontSize = fontSize * 0.83)
                }
            }
        }
        registerHandlerIfAbsent("h6") {
            SpanStyleHandler { styleConfig ->
                styleConfig.textStyle.toSpanStyle().run {
                    copy(fontWeight = FontWeight.Bold, fontSize = fontSize * 0.67)
                }
            }
        }

        registerHandlerIfAbsent("tt") {
            SpanStyleHandler { styleConfig ->
                styleConfig.textStyle.toSpanStyle().run { copy(fontFamily = FontFamily.Monospace) }
            }
        }

        registerHandlerIfAbsent("pre") { PreAnnotatedHandler() }

        registerHandlerIfAbsent("big") {
            SpanStyleHandler(false) { styleConfig ->
                styleConfig.textStyle.toSpanStyle().run {
                    copy(fontWeight = FontWeight.Bold, fontSize = fontSize * 1.25)
                }
            }
        }

        registerHandlerIfAbsent("small") {
            SpanStyleHandler(false) { styleConfig ->
                styleConfig.textStyle.toSpanStyle().run {
                    copy(fontWeight = FontWeight.Bold, fontSize = fontSize * 0.8)
                }
            }
        }

        registerHandlerIfAbsent("sub") {
            SpanStyleHandler(false) { styleConfig ->
                styleConfig.textStyle.toSpanStyle().run {
                    copy(baselineShift = BaselineShift.Subscript, fontSize = fontSize * 0.7)
                }
            }
        }

        registerHandlerIfAbsent("sup") {
            SpanStyleHandler(false) { styleConfig ->
                styleConfig.textStyle.toSpanStyle().run {
                    copy(baselineShift = BaselineShift.Superscript, fontSize = fontSize * 0.7)
                }
            }
        }

        registerHandlerIfAbsent("center") {
            ParagraphStyleHandler { styleConfig ->
                styleConfig.textStyle.toParagraphStyle().copy(textAlign = TextAlign.Center)
            }
        }

        registerHandlerIfAbsent("a") { LinkAnnotatedHandler() }
        registerHandlerIfAbsent("img") { ImageAnnotatedHandler() }

        registerHandlerIfAbsent("span") { TagHandler() }
    }

    private fun registerBuiltInCssHandlers(pre: Map<String, CSSAnnotatedHandler>?) {
        pre?.also { map ->
            cssHandlers.putAll(map)
        }

        fun registerHandlerIfAbsent(tag: String, getHandler: () -> CSSAnnotatedHandler) {
            if (pre?.containsKey(tag) != true) {
                registerCssHandler(tag, getHandler())
            }
        }

        registerHandlerIfAbsent("text-align") { TextAlignCssAnnotatedHandler() }
        registerHandlerIfAbsent("font-size") { FontSizeCssAnnotatedHandler() }
        registerHandlerIfAbsent("font-weight") { FontWeightCssAnnotatedHandler() }
        registerHandlerIfAbsent("font-style") { FontStyleCssAnnotatedHandler() }
        registerHandlerIfAbsent("color") { ColorCssAnnotatedHandler() }
        registerHandlerIfAbsent("background-color") { BackgroundColorCssAnnotatedHandler() }
        registerHandlerIfAbsent("text-indent") { TextIndentCssAnnotatedHandler() }
        registerHandlerIfAbsent("text-decoration") { TextDecorationCssAnnotatedHandler() }

    }

    companion object {
        private const val TAG = "HtmlAnnotator"

        var defaultPreTagHandlers: Map<String, TagHandler>? = null
        var defaultPreCSSHandlers: Map<String, CSSAnnotatedHandler>? = null
        var preNodeProcessors: List<NodeProcessor> = listOf(ParagraphNodeProcessor)
    }
}