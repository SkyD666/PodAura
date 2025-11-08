package com.skyd.podaura.util

import com.skyd.fundation.util.Platform
import com.skyd.fundation.util.platform

actual fun isFreeNetworkAvailable(): Boolean {
    return when (platform) {
        Platform.Android,
        Platform.IOS -> error("Not supported platform")

        Platform.Linux,
        Platform.MacOS -> true

        Platform.Windows -> TODO()
    }
}