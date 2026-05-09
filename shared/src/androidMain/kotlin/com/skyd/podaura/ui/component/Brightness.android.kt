package com.skyd.podaura.ui.component

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.skyd.podaura.ext.getScreenBrightness

@Composable
actual fun rememberBrightnessController(): BrightnessController {
    val activity = LocalActivity.current

    return remember(activity) {
        object : BrightnessController {
            override var percent: Float
                get() {
                    return activity?.window?.attributes?.apply {
                        if (screenBrightness <= 0.00f) {
                            val brightness = activity.getScreenBrightness()
                            if (brightness != null) {
                                screenBrightness = brightness / 255.0f
                                activity.window.attributes = this
                            }
                        }
                    }?.screenBrightness ?: 0f
                }
                set(value) {
                    val layoutParams = activity?.window?.attributes
                    if (layoutParams != null) {
                        layoutParams.screenBrightness = value.coerceIn(0.01f .. 1f)
                        activity.window.setAttributes(layoutParams)
                    }
                }
        }
    }
}
