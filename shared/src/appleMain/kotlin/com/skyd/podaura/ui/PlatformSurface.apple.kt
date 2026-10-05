package com.skyd.podaura.ui

import platform.AVFoundation.AVSampleBufferDisplayLayer

actual interface PlatformSurfaceHolder {
    val layer: AVSampleBufferDisplayLayer
    val isActive: Boolean get() = true
    val width: Int
    val height: Int
    var onResize: ((Int, Int) -> Unit)?
}
