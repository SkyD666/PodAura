package com.skyd.podaura.util

actual val isDebug: Boolean
    get() = Platform.isDebugBinary
