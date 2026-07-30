package com.skyd.podaura.ui.screen.image

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.skyd.podaura.ui.window.LocalDesktopAppState

@Composable
internal actual fun rememberImagePreviewOpener(): ImagePreviewOpener {
    val appState = LocalDesktopAppState.current
    return remember(appState) {
        object : ImagePreviewOpener {
            override fun open(image: String) {
                appState.openImagePreview(image)
            }
        }
    }
}
