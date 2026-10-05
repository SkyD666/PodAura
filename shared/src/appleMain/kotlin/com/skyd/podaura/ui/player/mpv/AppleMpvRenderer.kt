package com.skyd.podaura.ui.player.mpv

import cnames.structs.mpv_handle
import com.skyd.podaura.ui.PlatformSurfaceHolder
import kotlinx.cinterop.CPointer

internal expect val appleMpvGpuContext: String
internal expect val appleMpvAudioOutput: String
internal expect fun PlatformSurfaceHolder.isMpvSurfaceActive(): Boolean

internal expect class AppleMpvRenderer(handle: CPointer<mpv_handle>) {
    fun attach(holder: PlatformSurfaceHolder)
    fun videoSize(width: Int, height: Int, rotation: Int)
    fun setActive(active: Boolean)
    val failed: Boolean
    fun close()
}
