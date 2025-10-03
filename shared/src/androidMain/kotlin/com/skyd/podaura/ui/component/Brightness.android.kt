package com.skyd.podaura.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.skyd.podaura.ext.activity
import com.skyd.podaura.ext.getScreenBrightness

@Composable
actual fun rememberBrightnessController(): BrightnessController {
    val context = LocalContext.current
    return remember(context) {
        val activity = context.activity
        object : BrightnessController {
            override var percent: Float
                get() {
                    return activity.window.attributes.apply {
                        if (screenBrightness <= 0.00f) {
                            val brightness = activity.getScreenBrightness()
                            if (brightness != null) {
                                screenBrightness = brightness / 255.0f
                                activity.window.setAttributes(this)
                            }
                        }
                    }.screenBrightness
                }
                set(value) {
                    val layoutParams = activity.window.attributes
                    layoutParams.screenBrightness = value.coerceIn(0.01f..1f)
                    activity.window.setAttributes(layoutParams)
                }
        }
    }
}