package com.skyd.podaura.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberBrightnessController(): BrightnessController {
    return remember {
        object : BrightnessController {
            override var percent: Float
                get() = platform.UIKit.UIScreen.mainScreen.brightness.toFloat()
                set(value) {
                    platform.UIKit.UIScreen.mainScreen.brightness =
                        value.coerceIn(0f, 1f).toDouble()
                }
        }
    }
}
