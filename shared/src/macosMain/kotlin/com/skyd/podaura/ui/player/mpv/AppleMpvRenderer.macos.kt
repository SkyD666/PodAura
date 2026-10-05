package com.skyd.podaura.ui.player.mpv

import cnames.structs.mpv_handle
import com.skyd.podaura.libmpv.MPV_FORMAT_INT64
import com.skyd.podaura.libmpv.mpv_set_property
import com.skyd.podaura.ui.PlatformSurfaceHolder
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.LongVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.objcPtr
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value

internal actual val appleMpvGpuContext = "moltenvk"
internal actual val appleMpvAudioOutput = "coreaudio"
internal actual fun PlatformSurfaceHolder.isMpvSurfaceActive() = true

// AppKit sets drawableSize before notifying mpv to reconfigure and redraw.
internal actual fun MPV.resizeSurface(width: Int, height: Int) {
    setPropertyString("macos-surface-size", "${width}x${height}")
}

/** MPVKit's moltenvk context takes a CAMetalLayer pointer, not an NSView. */
internal actual class AppleMpvRenderer actual constructor(private val handle: CPointer<mpv_handle>) {
    private var holder: PlatformSurfaceHolder? = null

    actual fun attach(holder: PlatformSurfaceHolder) {
        this.holder = holder // Retain the layer until mpv has stopped its video output.
        memScoped {
            val wid = alloc<LongVar> { value = holder.layer.objcPtr().toLong() }
            check(mpv_set_property(handle, "wid", MPV_FORMAT_INT64, wid.ptr) >= 0) {
                "Unable to attach mpv Metal layer"
            }
        }
    }

    // The swapchain uses CAMetalLayer.drawableSize; AppKit owns its geometry.
    actual fun videoSize(width: Int, height: Int, rotation: Int) = Unit
    actual fun setActive(active: Boolean) = Unit
    actual val failed: Boolean get() = false
    actual fun close() {
        holder = null
    }
}
