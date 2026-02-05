package com.skyd.fundation.util

import kotlin.system.exitProcess

actual val platform: Platform
    get() = Platform.MacOS

actual fun exitApp() {
    exitProcess(0)
}
