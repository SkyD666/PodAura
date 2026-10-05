package com.skyd.podaura.ui.player.mpv

import com.skyd.podaura.ui.PlatformSurfaceHolder

internal actual typealias AppleMpvRenderer = SampleBufferRenderer

internal actual val appleMpvGpuContext = "podaura"
internal actual val appleMpvAudioOutput = "coreaudio"
internal actual val appleMpvUsesViewportSize = true
internal actual fun PlatformSurfaceHolder.isMpvSurfaceActive() = isActive

internal actual fun MPV.resizeSurface(width: Int, height: Int) = resizeRenderingSurface()
