package com.skyd.podaura.ui.component.webview

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun PodAuraWebView(
    modifier: Modifier = Modifier,
    content: String,
    baseUrl: String? = null,
    refererDomain: String? = null,
    styleMode: HtmlStyleMode = HtmlStyleMode.ReaderTheme,
    horizontalPadding: Float = 0f,
    onImageClick: ((imageUrl: String, alt: String) -> Unit)? = null,
    onTimestampClick: ((positionSeconds: Long) -> Unit)? = null,
)
