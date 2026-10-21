package com.skyd.podaura.ui.player.pip

import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import kotlinx.cinterop.value
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import platform.AVFoundation.AVAssetTrack
import platform.AVFoundation.AVAssetWriter
import platform.AVFoundation.AVAssetWriterInput
import platform.AVFoundation.AVAssetWriterInputPixelBufferAdaptor
import platform.AVFoundation.AVAssetWriterStatusCompleted
import platform.AVFoundation.AVFileTypeQuickTimeMovie
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.AVMutableComposition
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVURLAsset
import platform.AVFoundation.AVVideoCodecKey
import platform.AVFoundation.AVVideoCodecTypeH264
import platform.AVFoundation.AVVideoHeightKey
import platform.AVFoundation.AVVideoWidthKey
import platform.AVFoundation.addMutableTrackWithMediaType
import platform.AVFoundation.tracksWithMediaType
import platform.AppKit.NSImage
import platform.CoreGraphics.CGBitmapContextCreate
import platform.CoreGraphics.CGColorSpaceCreateDeviceRGB
import platform.CoreGraphics.CGColorSpaceRelease
import platform.CoreGraphics.CGContextDrawImage
import platform.CoreGraphics.CGContextFillRect
import platform.CoreGraphics.CGContextRelease
import platform.CoreGraphics.CGContextSetRGBFillColor
import platform.CoreGraphics.CGImageAlphaInfo
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.kCGBitmapByteOrder32Little
import platform.CoreMedia.CMTimeMake
import platform.CoreMedia.CMTimeRangeMake
import platform.CoreMedia.kCMPersistentTrackID_Invalid
import platform.CoreMedia.kCMTimeZero
import platform.CoreVideo.CVPixelBufferCreate
import platform.CoreVideo.CVPixelBufferGetBaseAddress
import platform.CoreVideo.CVPixelBufferGetBytesPerRow
import platform.CoreVideo.CVPixelBufferLockBaseAddress
import platform.CoreVideo.CVPixelBufferRefVar
import platform.CoreVideo.CVPixelBufferRelease
import platform.CoreVideo.CVPixelBufferUnlockBaseAddress
import platform.CoreVideo.kCVPixelFormatType_32BGRA
import platform.CoreVideo.kCVReturnSuccess
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import kotlin.coroutines.resume
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Duration.Companion.milliseconds

/** A one-frame video track lets AVKit show audio artwork at its original aspect ratio. */
internal suspend fun writePipArtwork(image: NSImage?, url: NSURL) {
    val size = image?.size?.useContents { width to height } ?: (640.0 to 640.0)
    val scale = min(1.0, 640.0 / max(size.first, size.second).coerceAtLeast(1.0))
    val width = max(2, (size.first * scale).toInt() / 2 * 2)
    val height = max(2, (size.second * scale).toInt() / 2 * 2)
    val writer = AVAssetWriter(uRL = url, fileType = AVFileTypeQuickTimeMovie, error = null)
    val input = AVAssetWriterInput(
        mediaType = AVMediaTypeVideo,
        outputSettings = mapOf(
            AVVideoCodecKey to AVVideoCodecTypeH264,
            AVVideoWidthKey to width,
            AVVideoHeightKey to height,
        ),
    )
    val adaptor = AVAssetWriterInputPixelBufferAdaptor(input, sourcePixelBufferAttributes = null)
    writer.addInput(input)
    check(writer.startWriting()) { writer.error?.localizedDescription ?: "Artwork writer failed" }
    writer.startSessionAtSourceTime(kCMTimeZero.readValue())
    try {
        memScoped {
            val buffer = alloc<CVPixelBufferRefVar>()
            check(
                CVPixelBufferCreate(
                    null, width.toULong(), height.toULong(),
                    kCVPixelFormatType_32BGRA, null, buffer.ptr
                ) == kCVReturnSuccess
            )
            val pixel = checkNotNull(buffer.value)
            try {
                CVPixelBufferLockBaseAddress(pixel, 0u)
                val color = CGColorSpaceCreateDeviceRGB()
                val canvas = checkNotNull(
                    CGBitmapContextCreate(
                        CVPixelBufferGetBaseAddress(pixel), width.toULong(), height.toULong(), 8u,
                        CVPixelBufferGetBytesPerRow(pixel), color,
                        kCGBitmapByteOrder32Little or CGImageAlphaInfo.kCGImageAlphaPremultipliedFirst.value,
                    )
                )
                CGContextSetRGBFillColor(canvas, 0.0, 0.0, 0.0, 1.0)
                val rect = CGRectMake(0.0, 0.0, width.toDouble(), height.toDouble())
                CGContextFillRect(canvas, rect)
                image?.CGImageForProposedRect(null, null, null)?.let {
                    CGContextDrawImage(canvas, rect, it)
                }
                CGContextRelease(canvas)
                CGColorSpaceRelease(color)
                CVPixelBufferUnlockBaseAddress(pixel, 0u)
                withTimeout(5_000.milliseconds) {
                    while (!input.readyForMoreMediaData) delay(10.milliseconds)
                }
                check(
                    adaptor.appendPixelBuffer(
                        pixelBuffer = pixel, withPresentationTime = kCMTimeZero.readValue()
                    )
                )
            } finally {
                CVPixelBufferRelease(pixel)
            }
        }
        writer.endSessionAtSourceTime(CMTimeMake(1, 1))
        input.markAsFinished()
        suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { writer.cancelWriting() }
            writer.finishWritingWithCompletionHandler { continuation.resume(Unit) }
        }
        check(writer.status == AVAssetWriterStatusCompleted) {
            writer.error?.localizedDescription ?: "Artwork video could not be written"
        }
    } catch (failure: Throwable) {
        writer.cancelWriting()
        NSFileManager.defaultManager.removeItemAtURL(url, null)
        throw failure
    }
}

internal fun pipArtworkItem(url: NSURL, durationSeconds: Long): AVPlayerItem {
    val asset = AVURLAsset(uRL = url, options = null)
    val source =
        asset.tracksWithMediaType(AVMediaTypeVideo).filterIsInstance<AVAssetTrack>().first()
    val composition = AVMutableComposition()
    val track = checkNotNull(
        composition.addMutableTrackWithMediaType(
            AVMediaTypeVideo, preferredTrackID = kCMPersistentTrackID_Invalid,
        )
    )
    val range = CMTimeRangeMake(kCMTimeZero.readValue(), CMTimeMake(1, 1))
    check(track.insertTimeRange(range, source, atTime = kCMTimeZero.readValue(), error = null))
    // Live audio has no seek bar; a long still track keeps its cover available.
    track.scaleTimeRange(
        range,
        toDuration = CMTimeMake(durationSeconds.takeIf { it > 0 } ?: 86_400, 1))
    return AVPlayerItem(asset = composition)
}
