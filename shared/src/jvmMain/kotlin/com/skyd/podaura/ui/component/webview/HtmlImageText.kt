package com.skyd.podaura.ui.component.webview

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.onClick
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
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
) = BasicHtmlImageText(
    html = html,
    styleConfig = StyleConfig(
        textStyle = textStyle,
        linkStyles = TextLinkStyles(
            style = SpanStyle(
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline,
            ),
        ),
        uriHandler = LocalUriHandler.current,
    ),
    imageContent = {
        PodAuraImage(
            model = it,
            contentDescription = "photo",
            modifier = Modifier
                .widthIn(max = 1000.dp)
                .wrapContentHeight()
                .onClick(onClick = { onImageClick?.invoke(it, "") }),
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