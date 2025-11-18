package com.skyd.htmlrender.ui.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.AnnotatedString
import com.skyd.htmlrender.core.HtmlAnnotator
import com.skyd.htmlrender.ui.RawHtmlData
import com.skyd.htmlrender.ui.cache.HtmlAnnotatorCache
import com.skyd.htmlrender.ui.cache.rememberLifecycle


@Composable
fun rememberHtmlTextState(
    annotator: HtmlAnnotator = rememberHtmlAnnotator(),
    cache: HtmlAnnotatorCache<AnnotatedString> = rememberLifecycle(),
    buildHtml: suspend HtmlAnnotator.(rawHtmlData: RawHtmlData) -> AnnotatedString = { from(it) },
): HtmlTextState = remember(annotator, buildHtml) {
    HtmlTextState(annotator, cache, buildHtml)
}


@Stable
class HtmlTextState(
    annotator: HtmlAnnotator,
    cache: HtmlAnnotatorCache<AnnotatedString>,
    buildHtml: suspend HtmlAnnotator.(rawHtmlData: RawHtmlData) -> AnnotatedString
) : BasicHtmlRenderState<AnnotatedString>(annotator, cache, buildHtml)