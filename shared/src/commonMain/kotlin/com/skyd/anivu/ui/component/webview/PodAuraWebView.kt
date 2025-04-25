package com.skyd.anivu.ui.component.webview

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun PodAuraWebView(
    modifier: Modifier = Modifier,
    content: String,
    refererDomain: String? = null,
    horizontalPadding: Float = 0f,
    onImageClick: ((imageUrl: String, alt: String) -> Unit)? = null,
)