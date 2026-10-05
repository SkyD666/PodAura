package com.skyd.podaura.ui

import platform.QuartzCore.CAMetalLayer

actual interface PlatformSurfaceHolder {
    val layer: CAMetalLayer
    var onResize: ((Int, Int) -> Unit)?
}
