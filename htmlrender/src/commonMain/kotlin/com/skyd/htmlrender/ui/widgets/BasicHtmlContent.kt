package com.skyd.htmlrender.ui.widgets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import com.skyd.htmlrender.core.StyleConfig
import com.skyd.htmlrender.ui.RawHtmlData
import com.skyd.htmlrender.ui.state.HtmlContentState


@Composable
fun BasicHtmlContent(
    html: String?,
    state: HtmlContentState,
    styleConfig: StyleConfig = StyleConfig.Default,
    renderTag: @Composable ColumnScope.(annotation: AnnotatedString.Range<String>, AnnotatedString) -> Unit,
    modifier: Modifier = Modifier,
    renderDefault: @Composable ColumnScope.(AnnotatedString) -> Unit = { text ->
        BasicText(text, Modifier.fillMaxWidth(), styleConfig.textStyle)
    }
): Unit = with(state) {
    rawHtmlData = html?.let { RawHtmlData(it, styleConfig) }
    resultHtml?.let { result ->
        BasicHtmlContentUI(
            resultHtml = result,
            tags = state.splitTags,
            renderTag = renderTag,
            modifier = modifier,
            defaultStyle = styleConfig.textStyle,
            renderDefault = renderDefault
        )
    }
}

@Composable
fun BasicHtmlContentUI(
    resultHtml: List<AnnotatedString>,
    tags: List<String>,
    renderTag: @Composable ColumnScope.(annotation: AnnotatedString.Range<String>, AnnotatedString) -> Unit,
    modifier: Modifier = Modifier,
    defaultStyle: TextStyle = TextStyle.Default,
    renderDefault: @Composable ColumnScope.(AnnotatedString) -> Unit = { text ->
        BasicText(text, Modifier.fillMaxWidth(), defaultStyle)
    }
) {
    Column(modifier) {
        resultHtml.forEach { string ->
            var annotation: AnnotatedString.Range<String>? = null
            for (tag in tags) {
                annotation =
                    string.getStringAnnotations(tag, 0, string.length).firstOrNull()
                if (annotation != null) {
                    break
                }
            }
            if (annotation == null) {
                renderDefault(string)
            } else {
                renderTag(annotation, string)
            }
        }
    }
}