package com.skyd.podaura.ui.player.mpv

import cnames.structs.mpv_handle
import com.skyd.podaura.libmpv.podaura_output_attach
import com.skyd.podaura.libmpv.podaura_output_create
import com.skyd.podaura.libmpv.podaura_output_destroy
import com.skyd.podaura.libmpv.podaura_output_failed
import com.skyd.podaura.libmpv.podaura_output_resize
import com.skyd.podaura.libmpv.podaura_output_set_active
import com.skyd.podaura.libmpv.podaura_output_set_layer
import com.skyd.podaura.ui.PlatformSurfaceHolder
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.interpretCPointer
import kotlinx.cinterop.objcPtr
import kotlin.math.max
import kotlin.math.min

/** gpu-next renders into bounded IOSurfaces shared with AVFoundation. */
internal class SampleBufferRenderer(private val handle: CPointer<mpv_handle>) : AutoCloseable {
    private var output: COpaquePointer? = null
    private var holder: PlatformSurfaceHolder? = null
    private var sourceWidth = 0
    private var sourceHeight = 0

    fun attach(holder: PlatformSurfaceHolder) {
        this.holder = holder
        if (output == null) {
            output = checkNotNull(
                podaura_output_create(
                    interpretCPointer<ByteVar>(holder.layer.objcPtr()), holder.width, holder.height
                )
            )
            check(podaura_output_attach(handle, output) >= 0) { "Unable to attach Metal output" }
        } else {
            podaura_output_set_layer(output, interpretCPointer<ByteVar>(holder.layer.objcPtr()))
        }
        resize()
        podaura_output_set_active(output, holder.isActive)
    }

    fun videoSize(width: Int, height: Int, rotation: Int) {
        sourceWidth = if (rotation % 180 == 90) height else width
        sourceHeight = if (rotation % 180 == 90) width else height
        resize()
    }

    fun resize() {
        val target = holder ?: return
        // Desktop zoom/pan must render into the entire viewport, including letterboxed areas.
        if (appleMpvUsesViewportSize) {
            output?.let {
                podaura_output_resize(
                    it,
                    target.width.coerceAtLeast(2),
                    target.height.coerceAtLeast(2)
                )
            }
            return
        }
        // The buffer keeps the media aspect ratio; AVFoundation fits it into the
        // inline view and the system derives the floating window's aspect from it.
        val scale = if (sourceWidth > 0 && sourceHeight > 0) min(
            1.0,
            max(target.width, target.height).toDouble() / max(sourceWidth, sourceHeight)
        ) else 1.0
        output?.let {
            podaura_output_resize(
                it,
                if (sourceWidth > 0) max(
                    2,
                    (sourceWidth * scale).toInt()
                ) else target.width.coerceAtLeast(2),
                if (sourceHeight > 0) max(
                    2,
                    (sourceHeight * scale).toInt()
                ) else target.height.coerceAtLeast(2)
            )
        }
    }

    fun setActive(active: Boolean) {
        output?.let { podaura_output_set_active(it, active) }
    }

    val failed: Boolean get() = output?.let { podaura_output_failed(it) } == true

    override fun close() {
        output?.let { podaura_output_set_active(it, false); podaura_output_destroy(it) }
        output = null
        holder = null
    }
}
