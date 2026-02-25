package com.skyd.podaura.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberAudioController(): AudioController {
    return remember {
        object : AudioController {
            override val range: ClosedFloatingPointRange<Float>
                get() = TODO("Not yet implemented")
            override var value: Float
                get() = TODO("Not yet implemented")
                set(value) = TODO("Not yet implemented")
        }
    }
}
