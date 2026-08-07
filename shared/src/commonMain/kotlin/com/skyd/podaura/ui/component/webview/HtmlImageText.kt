package com.skyd.podaura.ui.component.webview

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.skyd.htmlrender.core.StyleConfig
import com.skyd.htmlrender.ui.widgets.BasicHtmlImageText
import com.skyd.podaura.ui.component.PodAuraImage

@Composable
fun HtmlImageText(
    html: String,
    textStyle: TextStyle = LocalTextStyle.current,
    modifier: Modifier = Modifier,
    onImageClick: ((imageUrl: String, alt: String) -> Unit)?,
    onTimestampClick: ((positionSeconds: Long) -> Unit)?,
) {
    val timestampUriHandler = rememberTimestampUriHandler(
        delegate = LocalUriHandler.current,
        onTimestampClick = onTimestampClick,
    )
    BasicHtmlImageText(
        html = html,
        styleConfig = StyleConfig(
            textStyle = textStyle,
            linkStyles = TextLinkStyles(
                style = SpanStyle(
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline,
                ),
            ),
            uriHandler = timestampUriHandler,
        ),
        imageContent = {
            PodAuraImage(
                model = it,
                contentDescription = "photo",
                modifier = Modifier
                    .widthIn(max = 1000.dp)
                    .wrapContentHeight()
                    .clickable { onImageClick?.invoke(it, "") },
                contentScale = ContentScale.FillWidth,
            )
        },
        linkContent = { text ->
            BasicText(
                text = text,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        modifier = modifier,
        renderDefault = { text ->
            BasicText(
                text = text,
                modifier = Modifier.fillMaxWidth(),
                style = textStyle,
            )
        }
    )
}

@Composable
private fun rememberTimestampUriHandler(
    delegate: UriHandler,
    onTimestampClick: ((positionSeconds: Long) -> Unit)?,
): UriHandler {
    val currentOnTimestampClick by rememberUpdatedState(onTimestampClick)
    return remember(delegate) {
        object : UriHandler {
            override fun openUri(uri: String) {
                val seconds = timestampSecondsFromUri(uri)
                if (seconds != null) currentOnTimestampClick?.invoke(seconds)
                else delegate.openUri(uri)
            }
        }
    }
}
