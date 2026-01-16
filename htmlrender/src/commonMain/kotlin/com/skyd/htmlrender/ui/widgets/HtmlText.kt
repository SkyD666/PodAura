package com.skyd.htmlrender.ui.widgets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextOverflow
import com.skyd.htmlrender.core.StyleConfig
import com.skyd.htmlrender.ui.RawHtmlData
import com.skyd.htmlrender.ui.state.HtmlTextState
import com.skyd.htmlrender.ui.state.rememberHtmlTextState


@Composable
fun HtmlText(
    html: String?,
    modifier: Modifier = Modifier,
    styleConfig: StyleConfig = StyleConfig.Default,
    state: HtmlTextState = rememberHtmlTextState(),
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    inlineContent: Map<String, InlineTextContent> = emptyMap(),
    onTextLayout: (TextLayoutResult) -> Unit = {},
): Unit = with(state) {
    Box(modifier) {
        rawHtmlData = html?.let { RawHtmlData(it, styleConfig) }
        resultHtml?.also { annotated ->
            BasicText(
                text = annotated,
                modifier = Modifier.fillMaxWidth(),
                style = styleConfig.textStyle,
                onTextLayout = onTextLayout,
                overflow = overflow,
                softWrap = softWrap,
                maxLines = maxLines,
                minLines = minLines,
                inlineContent = inlineContent
            )
        }
    }
}