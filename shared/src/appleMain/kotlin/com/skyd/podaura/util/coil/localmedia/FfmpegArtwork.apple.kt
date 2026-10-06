package com.skyd.podaura.util.coil.localmedia

import cnames.structs.AVDictionary
import com.skyd.podaura.ffmpeg.AVFormatContext
import com.skyd.podaura.ffmpeg.AVPacket
import com.skyd.podaura.ffmpeg.AV_DISPOSITION_ATTACHED_PIC
import com.skyd.podaura.ffmpeg.av_dict_free
import com.skyd.podaura.ffmpeg.av_dict_get
import com.skyd.podaura.ffmpeg.av_dict_set
import com.skyd.podaura.ffmpeg.avformat_close_input
import com.skyd.podaura.ffmpeg.avformat_open_input
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value

internal const val MAX_FFMPEG_ARTWORK_BYTES = 16 * 1024 * 1024

// Reuse the player's demuxer for formats AVFoundation cannot read (e.g. WMA and ADTS AAC).
// Read the original image packet, so this does not require FFmpeg image decoders.
internal fun getFfmpegArtworkData(filePath: String): ByteArray? = memScoped {
    val context = alloc<CPointerVar<AVFormatContext>>().apply { value = null }
    val options = alloc<CPointerVar<AVDictionary>>().apply { value = null }
    try {
        av_dict_set(options.ptr, "protocol_whitelist", "file", 0)
        av_dict_set(options.ptr, "probesize", "1048576", 0)
        if (avformat_open_input(context.ptr, filePath, null, options.ptr) < 0) return null
        val format = context.value?.pointed ?: return null
        var selected: CPointer<AVPacket>? = null
        for (index in 0 until format.nb_streams.toInt()) {
            val stream = format.streams?.get(index)?.pointed ?: continue
            if (stream.disposition and AV_DISPOSITION_ATTACHED_PIC == 0) continue
            val packet = stream.attached_pic
            // probesize does not bound attached pictures. This caps only the Kotlin copy;
            // FFmpeg may already have allocated a larger native packet while reading the header.
            if (packet.size !in 1..MAX_FFMPEG_ARTWORK_BYTES || packet.data == null) continue
            val type = av_dict_get(stream.metadata, "comment", null, 0)?.pointed?.value?.toKString()
            if (selected == null) selected = packet.ptr
            if (type == "Cover (front)") {
                selected = packet.ptr
                break
            }
        }
        val packet = selected?.pointed ?: return null
        packet.data?.readBytes(packet.size)
    } finally {
        avformat_close_input(context.ptr)
        av_dict_free(options.ptr)
    }
}
