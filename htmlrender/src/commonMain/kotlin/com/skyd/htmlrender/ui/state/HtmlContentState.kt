package com.skyd.htmlrender.ui.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.AnnotatedString
import com.skyd.htmlrender.core.HtmlAnnotator
import com.skyd.htmlrender.ui.RawHtmlData
import com.skyd.htmlrender.ui.cache.HtmlAnnotatorCache
import com.skyd.htmlrender.ui.cache.rememberLifecycle
import com.skyd.htmlrender.ui.splitByAnnotation


@Composable
fun rememberHtmlContentState(
    splitTags: List<String>,
    annotator: HtmlAnnotator = rememberHtmlAnnotator(),
    cache: HtmlAnnotatorCache<List<AnnotatedString>> = rememberLifecycle(),
    buildHtml: suspend HtmlAnnotator.(rawHtmlData: RawHtmlData) -> List<AnnotatedString> = {
        from(it).splitByAnnotation(splitTags)
    },
): HtmlContentState = remember(annotator, buildHtml) {
    HtmlContentState(annotator, cache, splitTags, buildHtml)
}


@Stable
class HtmlContentState(
    annotator: HtmlAnnotator,
    cache: HtmlAnnotatorCache<List<AnnotatedString>>,
    val splitTags: List<String>,
    buildHtml: suspend HtmlAnnotator.(rawHtmlData: RawHtmlData) -> List<AnnotatedString>
) : BasicHtmlRenderState<List<AnnotatedString>>(annotator, cache, buildHtml)
