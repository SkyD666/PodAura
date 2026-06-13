package com.skyd.fundation.util

import kotlin.system.exitProcess

actual val platform: Platform
    get() = Platform.macOS_Native

actual fun exitApp() {
    exitProcess(0)
}
