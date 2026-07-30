package com.skyd.podaura.ui.screen.image

import androidx.compose.runtime.Composable

internal interface ImagePreviewOpener {
    fun open(image: String)
}

@Composable
internal expect fun rememberImagePreviewOpener(): ImagePreviewOpener
