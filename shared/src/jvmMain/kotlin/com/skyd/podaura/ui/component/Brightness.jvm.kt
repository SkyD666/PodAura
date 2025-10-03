package com.skyd.podaura.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberBrightnessController(): BrightnessController {
    return remember {
        object : BrightnessController {
            override var percent: Float
                get() = TODO("Not yet implemented")
                set(value) = TODO("Not yet implemented")
        }
    }
}