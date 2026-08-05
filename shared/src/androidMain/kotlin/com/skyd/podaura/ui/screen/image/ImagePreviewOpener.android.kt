package com.skyd.podaura.ui.screen.image

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.skyd.compone.component.navigation.LocalGlobalNavBackStack

@Composable
internal actual fun rememberImagePreviewOpener(): ImagePreviewOpener {
    val navBackStack = LocalGlobalNavBackStack.current
    return remember(navBackStack) {
        object : ImagePreviewOpener {
            override fun open(image: String, title: String?) {
                navBackStack.add(ImagePreviewRoute(image = image, title = title))
            }
        }
    }
}
