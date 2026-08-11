package com.skyd.podaura.ui.component.webview

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skyd.podaura.model.preference.appearance.read.ReadTextSizePreference


@Composable
actual fun PodAuraWebView(
    modifier: Modifier,
    content: String,
    baseUrl: String?,
    refererDomain: String?,
    styleMode: HtmlStyleMode,
    horizontalPadding: Float,
    onImageClick: ((imageUrl: String, alt: String) -> Unit)?,
    onTimestampClick: ((positionSeconds: Long) -> Unit)?,
) {
    val palette = currentHtmlRenderPalette()
    val renderedContent = remember(content, palette, styleMode) {
        if (styleMode == HtmlStyleMode.HarmonizedSource) {
            content.resolveThemeColorTokens(palette)
        } else {
            content
        }
    }
    SelectionContainer(modifier) {
        val fontSize = ReadTextSizePreference.current.sp
        HtmlImageText(
            html = renderedContent,
            baseUri = baseUrl.orEmpty(),
            textStyle = LocalTextStyle.current.let { textStyle ->
                textStyle.copy(
                    color = textStyle.color.takeOrElse { LocalContentColor.current },
                    fontSize = fontSize,
                    lineHeight = fontSize * 1.3f,
                )
            },
            modifier = Modifier.padding(horizontal = horizontalPadding.dp),
            onImageClick = onImageClick,
            onTimestampClick = onTimestampClick,
        )
    }
}
