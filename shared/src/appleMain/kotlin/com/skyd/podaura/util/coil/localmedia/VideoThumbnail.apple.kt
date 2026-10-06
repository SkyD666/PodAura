package com.skyd.podaura.util.coil.localmedia

import cnames.structs.mpv_handle
import com.skyd.podaura.libmpv.MPV_EVENT_END_FILE
import com.skyd.podaura.libmpv.MPV_EVENT_PLAYBACK_RESTART
import com.skyd.podaura.libmpv.MPV_EVENT_SHUTDOWN
import com.skyd.podaura.libmpv.mpv_command
import com.skyd.podaura.libmpv.mpv_command_ret
import com.skyd.podaura.libmpv.mpv_create
import com.skyd.podaura.libmpv.mpv_free_node_contents
import com.skyd.podaura.libmpv.mpv_initialize
import com.skyd.podaura.libmpv.mpv_node
import com.skyd.podaura.libmpv.mpv_set_option_string
import com.skyd.podaura.libmpv.mpv_terminate_destroy
import com.skyd.podaura.libmpv.mpv_wait_event
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.cstr
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.set
import kotlinx.cinterop.toKString
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.impl.use
import platform.Foundation.NSLock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

// Decode one video at a time, so a grid cannot create many full-size decoder buffers at once.
private val videoThumbnailLock = NSLock()

internal fun getLocalVideoThumbnailData(filePath: String): ByteArray? {
    videoThumbnailLock.lock()
    try {
        return extractVideoFrame(filePath, "10%") ?: extractVideoFrame(filePath, "0")
    } finally {
        videoThumbnailLock.unlock()
    }
}

private fun extractVideoFrame(filePath: String, start: String): ByteArray? = memScoped {
    // This handle is independent of playback and never opens an audio device or a GPU surface.
    val player = mpv_create() ?: return null
    try {
        val options = mapOf(
            "config" to "no",
            "terminal" to "no",
            "audio" to "no",
            "sid" to "no",
            "vo" to "null",
            "pause" to "yes",
            "start" to start,
            "vf" to "lavfi=[scale=320:320:force_original_aspect_ratio=decrease:force_divisible_by=2]",
            "vd-lavc-threads" to "1",
            "demuxer-max-bytes" to "1MiB",
            "demuxer-max-back-bytes" to "0",
        )
        for ((name, value) in options) {
            if (mpv_set_option_string(player, name, value) < 0) return null
        }
        if (mpv_initialize(player) < 0) return null
        val args = allocArray<CPointerVar<ByteVar>>(3)
        args[0] = "loadfile".cstr.ptr
        args[1] = filePath.cstr.ptr
        args[2] = null
        if (mpv_command(player, args) < 0) return null
        val begun = TimeSource.Monotonic.markNow()
        while (begun.elapsedNow() < 5.seconds) {
            val event = mpv_wait_event(player, 0.1)?.pointed ?: continue
            when (event.event_id) {
                MPV_EVENT_PLAYBACK_RESTART -> return frameAsJpeg(player)
                MPV_EVENT_END_FILE, MPV_EVENT_SHUTDOWN -> return null
            }
        }
        return null
    } finally {
        mpv_terminate_destroy(player)
    }
}

private fun frameAsJpeg(player: CPointer<mpv_handle>): ByteArray? = memScoped {
    val args = allocArray<CPointerVar<ByteVar>>(4)
    args[0] = "screenshot-raw".cstr.ptr
    args[1] = "video".cstr.ptr
    args[2] = "rgba".cstr.ptr
    args[3] = null
    val result = alloc<mpv_node>()
    if (mpv_command_ret(player, args, result.ptr) < 0) return null
    try {
        val fields = result.u.list?.pointed ?: return null
        val keys = fields.keys ?: return null
        val values = fields.values ?: return null
        val entries = (0 until fields.num).associate { keys[it]?.toKString() to values[it] }
        val width = entries["w"]?.u?.int64?.toInt() ?: return null
        val height = entries["h"]?.u?.int64?.toInt() ?: return null
        val stride = entries["stride"]?.u?.int64?.toInt() ?: return null
        val data = entries["data"]?.u?.ba?.pointed ?: return null
        if (width !in 1..320 || height !in 1..320 || stride < width * 4 || data.size > 1_048_576uL) return null
        val pixels = data.data?.reinterpret<ByteVar>()?.readBytes(data.size.toInt()) ?: return null
        Bitmap().use { bitmap ->
            val info = ImageInfo(width, height, ColorType.RGBA_8888, ColorAlphaType.OPAQUE)
            if (!bitmap.installPixels(info, pixels, stride)) return null
            // MPVKit omits still-image encoders; use the Skia encoder already used by Coil.
            Image.makeFromBitmap(bitmap).use { image ->
                return image.encodeToData(EncodedImageFormat.JPEG, 80)?.use { it.bytes }
            }
        }
    } finally {
        mpv_free_node_contents(result.ptr)
    }
}
