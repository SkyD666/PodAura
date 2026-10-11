package com.skyd.podaura.util.coil.localmedia

import com.skyd.podaura.util.media.runtime.mediaLibrary
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Structure
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import java.util.concurrent.TimeUnit

// Decode one thumbnail at a time to bound decoder memory when loading a media grid.
private val videoThumbnailLock = Mutex()
private val mpv by lazy {
    Native.load(
        mediaLibrary("mpv", 2).path,
        ThumbnailMpv::class.java,
        mapOf(Library.OPTION_STRING_ENCODING to Charsets.UTF_8.name()),
    )
}

internal suspend fun getLocalVideoThumbnailData(
    filePath: String,
    loadLibrary: () -> ThumbnailMpv = { mpv },
): ByteArray? =
    videoThumbnailLock.withLock {
        currentCoroutineContext().ensureActive()
        val api = loadLibrary()
        extractVideoFrame(filePath, "10%", api) ?: extractVideoFrame(filePath, "0", api)
    }

private suspend fun extractVideoFrame(filePath: String, start: String, mpv: ThumbnailMpv): ByteArray? {
    val context = currentCoroutineContext()
    context.ensureActive()
    val player = mpv.mpv_create() ?: return null
    try {
        val options = mapOf(
            "config" to "no",
            "terminal" to "no",
            "audio" to "no",
            "sid" to "no",
            "vo" to "null",
            "pause" to "yes",
            "start" to start,
            "autoload-files" to "no",
            "vf" to "lavfi=[scale=320:320:force_original_aspect_ratio=decrease:force_divisible_by=2]",
            "vd-lavc-threads" to "1",
            "demuxer-max-bytes" to "1MiB",
            "demuxer-max-back-bytes" to "0",
        )
        for ((name, value) in options) {
            if (mpv.mpv_set_option_string(player, name, value) < 0) return null
        }
        context.ensureActive()
        if (mpv.mpv_initialize(player) < 0) return null
        context.ensureActive()
        if (mpv.mpv_command(player, arrayOf("loadfile", filePath, null)) < 0) return null
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
        while (System.nanoTime() < deadline) {
            context.ensureActive()
            val event = mpv.mpv_wait_event(player, 0.1)?.getInt(0)
            context.ensureActive()
            when (event) {
                21 -> return frameAsJpeg(player, mpv) // MPV_EVENT_PLAYBACK_RESTART
                7, 1 -> return null // MPV_EVENT_END_FILE, MPV_EVENT_SHUTDOWN
            }
        }
        return null
    } finally {
        mpv.mpv_terminate_destroy(player)
    }
}

private fun frameAsJpeg(player: Pointer, mpv: ThumbnailMpv): ByteArray? {
    val result = ThumbnailNode()
    if (mpv.mpv_command_ret(
            player,
            arrayOf("screenshot-raw", "video", "rgba", null),
            result
        ) < 0
    ) return null
    try {
        if (result.format != 8) return null // MPV_FORMAT_NODE_MAP
        val fields = ThumbnailNodeList(result.value ?: return null).apply { read() }
        val entries = (0 until fields.num).associate { index ->
            fields.keys?.getPointer(index.toLong() * Native.POINTER_SIZE)?.getString(0, Charsets.UTF_8.name()) to
                    ThumbnailNode(fields.values!!.share(index.toLong() * result.size())).apply { read() }
        }

        fun integer(name: String): Int? =
            entries[name]?.takeIf { it.format == 4 } // MPV_FORMAT_INT64
                ?.pointer?.getLong(0)?.toInt()

        val width = integer("w") ?: return null
        val height = integer("h") ?: return null
        val stride = integer("stride") ?: return null
        val dataNode =
            entries["data"]?.takeIf { it.format == 9 } ?: return null // MPV_FORMAT_BYTE_ARRAY
        val data = ThumbnailByteArray(dataNode.value ?: return null).apply { read() }
        if (width !in 1..320 || height !in 1..320 || stride < width * 4 || data.size !in 1..1_048_576) return null
        val pixels = data.data?.getByteArray(0, data.size.toInt()) ?: return null
        Bitmap().use { bitmap ->
            val info = ImageInfo(width, height, ColorType.RGBA_8888, ColorAlphaType.OPAQUE)
            if (!bitmap.installPixels(info, pixels, stride)) return null
            // The bundled FFmpeg omits image encoders. Use Coil's existing Skia encoder.
            Image.makeFromBitmap(bitmap).use { image ->
                return image.encodeToData(EncodedImageFormat.JPEG, 80)?.use { it.bytes }
            }
        }
    } finally {
        mpv.mpv_free_node_contents(result)
    }
}

internal interface ThumbnailMpv : Library {
    fun mpv_create(): Pointer?
    fun mpv_set_option_string(player: Pointer, name: String, value: String): Int
    fun mpv_initialize(player: Pointer): Int
    fun mpv_command(player: Pointer, args: Array<String?>): Int
    fun mpv_command_ret(player: Pointer, args: Array<String?>, result: ThumbnailNode): Int
    fun mpv_wait_event(player: Pointer, timeout: Double): Pointer?
    fun mpv_free_node_contents(node: ThumbnailNode)
    fun mpv_terminate_destroy(player: Pointer)
}

@Structure.FieldOrder("value", "format")
internal class ThumbnailNode(pointer: Pointer? = null) : Structure(pointer) {
    // mpv_node's union is pointer/int64/double sized on all supported desktop targets (64-bit).
    @JvmField
    var value: Pointer? = null

    @JvmField
    var format = 0
}

@Structure.FieldOrder("num", "values", "keys")
internal class ThumbnailNodeList(pointer: Pointer) : Structure(pointer) {
    @JvmField
    var num = 0

    @JvmField
    var values: Pointer? = null

    @JvmField
    var keys: Pointer? = null
}

@Structure.FieldOrder("data", "size")
internal class ThumbnailByteArray(pointer: Pointer) : Structure(pointer) {
    @JvmField
    var data: Pointer? = null

    @JvmField
    var size = 0L
}
