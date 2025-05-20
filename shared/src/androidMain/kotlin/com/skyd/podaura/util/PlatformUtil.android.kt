package com.skyd.podaura.util

import kotlin.system.exitProcess

actual val platform: Platform
    get() = Platform.Android

actual fun exitApp() {
    exitProcess(0)
}