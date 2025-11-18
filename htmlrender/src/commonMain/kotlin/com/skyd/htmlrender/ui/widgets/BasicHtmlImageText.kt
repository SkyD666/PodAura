package com.skyd.htmlrender.ui.widgets

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.ClickableText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.skyd.htmlrender.core.StyleConfig
import com.skyd.htmlrender.core.styler.ImageAnnotatedStyler
import com.skyd.htmlrender.core.styler.LinkAnnotatedStyler
import com.skyd.htmlrender.ui.handler.AnnotatedMarginHandler
import com.skyd.htmlrender.ui.handler.ListItemAnnotatedHandler
import com.skyd.htmlrender.ui.state.HtmlContentState
import com.skyd.htmlrender.ui.state.rememberHtmlAnnotator
import com.skyd.htmlrender.ui.state.rememberHtmlContentState
import com.skyd.htmlrender.ui.styler.MarginStyler
import com.skyd.htmlrender.ui.styler.OrderedListStyler
import com.skyd.htmlrender.ui.styler.UnorderedListStyler


@OptIn(ExperimentalTextApi::class)
@Composable
fun BasicHtmlImageText(
    html: String?,
    imageContent: @Composable (ColumnScope.(imgUrl: String) -> Unit),
    linkContent: @Composable (ColumnScope.(link: AnnotatedString) -> Unit),
    modifier: Modifier = Modifier,
    styleConfig: StyleConfig = StyleConfig.Default,
    getClickUrlAction: (() -> (url: String) -> Unit)? = null,
    splitTags: List<String> = listOf(
        ImageAnnotatedStyler.TAG_NAME,
        OrderedListStyler.TAG_NAME,
        UnorderedListStyler.TAG_NAME,
        MarginStyler.TOP,
        MarginStyler.LEFT,
        MarginStyler.RIGHT,
        MarginStyler.BOTTOM,
    ),
    state: HtmlContentState = rememberHtmlContentState(
        splitTags = splitTags,
        annotator = rememberHtmlAnnotator(
            preTagHandlers = mapOf(
                "li" to ListItemAnnotatedHandler(),
                "ul" to AnnotatedMarginHandler {
                    listOf(MarginStyler.Left("4em"))
                },
                "ol" to AnnotatedMarginHandler {
                    listOf(MarginStyler.Left("4em"))
                }
            ),
        )
    ),
    renderDefault: @Composable ColumnScope.(AnnotatedString) -> Unit = { text ->
        ClickableText(text, Modifier.fillMaxWidth(), styleConfig.textStyle) { index ->
            text.getUrlAnnotations(index, index).firstOrNull()?.item?.url?.also { url ->
                getClickUrlAction?.invoke()?.invoke(url)
            }
        }
    },
    renderTag: @Composable ColumnScope.(annotation: AnnotatedString.Range<String>, AnnotatedString) -> Unit = { annotation, string ->
        defaultImageTextRenderTag(
            annotation = annotation,
            string = string,
            imageContent = imageContent,
            linkContent = linkContent,
            defaultStyle = styleConfig.textStyle,
            renderDefault = renderDefault,
        )
    }
) = BasicHtmlContent(
    html = html,
    state = state,
    styleConfig = styleConfig,
    renderTag = renderTag,
    modifier = modifier,
    renderDefault = renderDefault
)

@OptIn(ExperimentalTextApi::class)
@Composable
fun ColumnScope.defaultImageTextRenderTag(
    annotation: AnnotatedString.Range<String>,
    string: AnnotatedString,
    imageContent: @Composable (ColumnScope.(imgUrl: String) -> Unit),
    linkContent: @Composable (ColumnScope.(link: AnnotatedString) -> Unit),
    defaultStyle: TextStyle = TextStyle.Default,
    renderDefault: @Composable (ColumnScope.(AnnotatedString) -> Unit)
) {
    when (annotation.tag) {
        ImageAnnotatedStyler.TAG_NAME -> {
            imageContent(annotation.item)
        }

        LinkAnnotatedStyler.TAG_NAME -> {
            linkContent(string)
        }

        OrderedListStyler.TAG_NAME, UnorderedListStyler.TAG_NAME -> {
            MarginWrapper(string, defaultStyle) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    BasicText(
                        text = annotation.item,
                        modifier = Modifier
                            .width(20.dp)
                            .alignByBaseline(),
                        defaultStyle.copy(textAlign = TextAlign.Center)
                    )
                    BasicText(
                        text = string,
                        modifier = Modifier
                            .fillMaxWidth()
                            .alignByBaseline(),
                        style = defaultStyle
                    )
                }
            }
        }

        MarginStyler.TOP, MarginStyler.LEFT, MarginStyler.RIGHT, MarginStyler.BOTTOM -> {
            MarginWrapper(string, defaultStyle) {
                this@defaultImageTextRenderTag.renderDefault(string)
            }
        }
    }
}