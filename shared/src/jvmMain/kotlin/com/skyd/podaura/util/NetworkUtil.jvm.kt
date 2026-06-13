package com.skyd.podaura.util

import com.skyd.fundation.util.Platform
import com.skyd.fundation.util.platform

actual fun isFreeNetworkAvailable(): Boolean {
    return when (platform) {
        Platform.Android,
        Platform.iOS,
        Platform.macOS_Native -> error("Not supported platform")

        Platform.Linux,
        Platform.macOS_Jvm -> true

        Platform.Windows -> TODO()
    }
}
