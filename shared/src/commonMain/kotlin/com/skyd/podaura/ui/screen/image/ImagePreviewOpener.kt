package com.skyd.podaura.ui.screen.image

import androidx.compose.runtime.Composable

internal interface ImagePreviewOpener {
    fun open(image: String, title: String? = null)
}

@Composable
internal expect fun rememberImagePreviewOpener(): ImagePreviewOpener
