package com.skyd.anivu.ui.component

import android.os.Build
import coil3.ComponentRegistry
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import coil3.video.VideoFrameDecoder

actual fun ComponentRegistry.Builder.platformComponents() {
    if (Build.VERSION.SDK_INT >= 28) {
        add(AnimatedImageDecoder.Factory())
    } else {
        add(GifDecoder.Factory())
    }
    add(VideoFrameDecoder.Factory())
}