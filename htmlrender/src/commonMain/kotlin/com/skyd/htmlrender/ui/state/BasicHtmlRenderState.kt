@file:Suppress("MemberVisibilityCanBePrivate")

package com.skyd.htmlrender.ui.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import com.skyd.htmlrender.base.handler.TagHandler
import com.skyd.htmlrender.core.HtmlAnnotator
import com.skyd.htmlrender.core.css.CSSAnnotatedHandler
import com.skyd.htmlrender.ui.RawHtmlData
import com.skyd.htmlrender.ui.cache.HtmlAnnotatorCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun rememberHtmlAnnotator(
    preTagHandlers: Map<String, TagHandler>? = HtmlAnnotator.defaultPreTagHandlers,
    preCSSHandlers: Map<String, CSSAnnotatedHandler>? = HtmlAnnotator.defaultPreCSSHandlers,
): HtmlAnnotator = remember(preTagHandlers, preCSSHandlers) {
    HtmlAnnotator(preTagHandlers, preCSSHandlers)
}

@Stable
abstract class BasicHtmlRenderState<R>(
    private val annotator: HtmlAnnotator,
    private val cache: HtmlAnnotatorCache<R>,
    private val buildHtml: suspend HtmlAnnotator.(rawHtmlData: RawHtmlData) -> R
) : RememberObserver {

    private var rememberedCount = 0
    private var coroutineScope: CoroutineScope? = null

    var rawHtmlData: RawHtmlData? by mutableStateOf(null)
    var resultHtml: R? by mutableStateOf(null)
        private set

    var isRendering: Boolean by mutableStateOf(false)
        private set

    private val srcFlow = snapshotFlow { rawHtmlData }

    override fun onRemembered() {
        rememberedCount++
        if (rememberedCount != 1) return

        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate).apply {
            coroutineScope = this
            launch {
                srcFlow.collectLatest { htmlData ->
                    if (htmlData == null) {
                        resultHtml = null
                        return@collectLatest
                    }

                    isRendering = true

                    cache.get(htmlData)?.also { cacheResult ->
                        resultHtml = cacheResult
                        isRendering = false
                        return@collectLatest
                    }

                    annotator.buildHtml(htmlData).also { result ->
                        resultHtml = result
                        isRendering = false
                        cache.put(htmlData, result)
                    }
                }
            }
        }
    }

    override fun onAbandoned() = onForgotten()

    override fun onForgotten() {
        if (rememberedCount <= 0) return
        rememberedCount--
        if (rememberedCount != 0) return

        coroutineScope?.cancel()
        coroutineScope = null
    }
}