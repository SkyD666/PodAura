package com.skyd.podaura.ui.screen.read

import coil3.ComponentRegistry
import coil3.video.VideoFrameDecoder

actual val components: ComponentRegistry.Builder.() -> Unit
    get() = { add(VideoFrameDecoder.Factory()) }