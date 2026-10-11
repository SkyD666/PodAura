package com.skyd.podaura.util.coil.localmedia

import co.touchlab.kermit.Logger
import com.skyd.podaura.util.media.runtime.mediaLibrary
import kotlinx.coroutines.CancellationException
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.ptr.PointerByReference

private val ffmpeg by lazy {
    Native.load(
        mediaLibrary("avformat", 62).path,
        ArtworkAvformat::class.java,
        mapOf(Library.OPTION_STRING_ENCODING to Charsets.UTF_8.name()),
    )
}
private val avutil by lazy {
    Native.load(
        mediaLibrary("avutil", 60).path,
        ArtworkAvutil::class.java,
        mapOf(Library.OPTION_STRING_ENCODING to Charsets.UTF_8.name()),
    )
}

internal fun getFfmpegArtworkData(
    filePath: String,
    loadLibrary: () -> ArtworkAvformat = { ffmpeg },
): ByteArray? = try {
    extractFfmpegArtworkData(filePath, loadLibrary())
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Logger.w(throwable = e, tag = "FfmpegArtwork") { "Failed to extract artwork from $filePath" }
    null
} catch (e: LinkageError) {
    Logger.w(throwable = e, tag = "FfmpegArtwork") { "FFmpeg artwork backend is unavailable" }
    null
}

private fun extractFfmpegArtworkData(filePath: String, api: ArtworkAvformat): ByteArray? {
    // The structure prefixes below describe libavformat 62, bundled by mediamp 0.5.
    // Never read them against a different major ABI after a runtime upgrade.
    val major = api.avformat_version() ushr 16
    check(major == 62) { "Unsupported FFmpeg artwork ABI: $major (expected 62)" }
    val context = PointerByReference()
    val options = PointerByReference()
    try {
        avutil.av_dict_set(options, "protocol_whitelist", "file", 0)
        avutil.av_dict_set(options, "probesize", "1048576", 0)
        if (api.avformat_open_input(context, filePath, null, options) < 0) return null
        val format = ArtworkFormat(context.value ?: return null).apply { read() }
        var selected: ArtworkPacket? = null
        for (index in 0 until format.nb_streams) {
            val pointer =
                format.streams?.getPointer(index.toLong() * Native.POINTER_SIZE) ?: continue
            val stream = ArtworkStream(pointer).apply { read() }
            if (stream.disposition and 0x400 == 0) continue // AV_DISPOSITION_ATTACHED_PIC
            val packet = stream.attached_pic
            // Bound the JVM copy; FFmpeg may already have allocated a larger native packet.
            if (packet.size !in 1..16 * 1024 * 1024 || packet.data == null) continue
            if (selected == null) selected = packet
            val comment = avutil.av_dict_get(stream.metadata, "comment", null, 0)
                ?.getPointer(Native.POINTER_SIZE.toLong())?.getString(0, Charsets.UTF_8.name())
            if (comment == "Cover (front)") {
                selected = packet
                break
            }
        }
        return selected?.let { it.data?.getByteArray(0, it.size) }
    } finally {
        api.avformat_close_input(context)
        avutil.av_dict_free(options)
    }
}

internal interface ArtworkAvformat : Library {
    fun avformat_version(): Int
    fun avformat_open_input(
        context: PointerByReference,
        path: String,
        format: Pointer?,
        options: PointerByReference
    ): Int

    fun avformat_close_input(context: PointerByReference)
}

private interface ArtworkAvutil : Library {
    fun av_dict_set(dictionary: PointerByReference, key: String, value: String, flags: Int): Int
    fun av_dict_get(dictionary: Pointer?, key: String, previous: Pointer?, flags: Int): Pointer?
    fun av_dict_free(dictionary: PointerByReference)
}

// Only the prefixes through the fields we read are needed; FFmpeg owns all allocations.
@Structure.FieldOrder(
    "av_class",
    "iformat",
    "oformat",
    "priv_data",
    "pb",
    "ctx_flags",
    "nb_streams",
    "streams"
)
internal class ArtworkFormat(pointer: Pointer) : Structure(pointer) {
    @JvmField
    var av_class: Pointer? = null

    @JvmField
    var iformat: Pointer? = null

    @JvmField
    var oformat: Pointer? = null

    @JvmField
    var priv_data: Pointer? = null

    @JvmField
    var pb: Pointer? = null

    @JvmField
    var ctx_flags = 0

    @JvmField
    var nb_streams = 0

    @JvmField
    var streams: Pointer? = null
}

@Structure.FieldOrder("buf", "pts", "dts", "data", "size")
internal class ArtworkPacket : Structure() {
    @JvmField
    var buf: Pointer? = null

    @JvmField
    var pts = 0L

    @JvmField
    var dts = 0L

    @JvmField
    var data: Pointer? = null

    @JvmField
    var size = 0
}

@Structure.FieldOrder(
    "av_class",
    "index",
    "id",
    "codecpar",
    "priv_data",
    "time_base",
    "start_time",
    "duration",
    "nb_frames",
    "disposition",
    "discard",
    "sample_aspect_ratio",
    "metadata",
    "avg_frame_rate",
    "attached_pic"
)
internal class ArtworkStream(pointer: Pointer) : Structure(pointer) {
    @JvmField
    var av_class: Pointer? = null

    @JvmField
    var index = 0

    @JvmField
    var id = 0

    @JvmField
    var codecpar: Pointer? = null

    @JvmField
    var priv_data: Pointer? = null

    @JvmField
    var time_base = IntArray(2)

    @JvmField
    var start_time = 0L

    @JvmField
    var duration = 0L

    @JvmField
    var nb_frames = 0L

    @JvmField
    var disposition = 0

    @JvmField
    var discard = 0

    @JvmField
    var sample_aspect_ratio = IntArray(2)

    @JvmField
    var metadata: Pointer? = null

    @JvmField
    var avg_frame_rate = IntArray(2)

    @JvmField
    var attached_pic = ArtworkPacket()
}
