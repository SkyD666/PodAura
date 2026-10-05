package com.skyd.podaura.ui.player.mpv

import com.skyd.podaura.ui.PlatformSurfaceHolder

internal actual typealias AppleMpvRenderer = SampleBufferRenderer

internal actual val appleMpvGpuContext = "podaura"
internal actual val appleMpvAudioOutput = "audiounit"
internal actual val appleMpvUsesViewportSize = false
internal actual fun PlatformSurfaceHolder.isMpvSurfaceActive() = isActive

// IOSurface targets follow the decoded video size, independently of the host view.
internal actual fun MPV.resizeSurface(width: Int, height: Int) = Unit
