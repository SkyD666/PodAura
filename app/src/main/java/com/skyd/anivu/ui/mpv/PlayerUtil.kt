package com.skyd.anivu.ui.mpv

import android.content.Context
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import com.skyd.anivu.appContext
import com.skyd.anivu.ext.getImage
import com.skyd.anivu.ui.component.imageLoaderBuilder
import com.skyd.anivu.ui.mpv.service.PlayerState
import com.skyd.anivu.util.image.decodeSampledBitmap
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

internal fun Uri.resolveUri(context: Context): String? {
    var filepath = when (scheme) {
        "file" -> path
        "content" -> openContentFd(context)
        "http", "https", "rtmp", "rtmps", "rtp", "rtsp",
        "mms", "mmst", "mmsh", "tcp", "udp", "lavf", "fd" -> this.toString()

        else -> null
    }

    if (filepath == null) {
        Log.e("resolveUri", "unknown scheme: $scheme")
        if (scheme == null && path?.startsWith("/") == true) {
            filepath = path
        }
    }
    return filepath
}

private fun Uri.openContentFd(context: Context): String? {
    val resolver = context.contentResolver
    val fd = try {
        resolver.openFileDescriptor(this, "r")!!.detachFd()
    } catch (e: Exception) {
        Log.e("openContentFd", "Failed to open content fd: $e")
        return null
    }
    // See if we skip the indirection and read the real file directly
    val path = findRealPath(fd)
    if (path != null) {
        Log.v("openContentFd", "Found real file path: $path")
        ParcelFileDescriptor.adoptFd(fd).close() // we don't need that anymore
        return path
    }
    // Else, pass the fd to mpv
    return "fd://${fd}"
}

fun findRealPath(fd: Int): String? {
    var ins: InputStream? = null
    try {
        val path = File("/proc/self/fd/${fd}").canonicalPath
        if (!path.startsWith("/proc") && File(path).canRead()) {
            // Double check that we can read it
            ins = FileInputStream(path)
            ins.read()
            return path
        }
    } catch (_: Exception) {
    } finally {
        ins?.close()
    }
    return null
}

fun isFdFileExists(fdPath: String): Boolean {
    var pfd: ParcelFileDescriptor? = null
    var inputStream: InputStream? = null
    return try {
        val fd = fdPath.substringAfterLast("fd://").toInt()
        pfd = ParcelFileDescriptor.fromFd(fd)
        inputStream = FileInputStream(pfd.fileDescriptor)
        inputStream.read() != -1
    } catch (e: IOException) {
        e.printStackTrace()
        false
    } finally {
        inputStream?.close()
        pfd?.close()
    }
}

suspend fun createThumbnailFile(
    thumbnailPath: String?,
): File? {
    thumbnailPath ?: return null
    return appContext.imageLoaderBuilder().build()
        .getImage(appContext, thumbnailPath)
}

suspend fun createThumbnail(
    thumbnailPath: String?,
    reqWidth: Int = 512,
    reqHeight: Int = 512,
): Bitmap? =
    createThumbnailFile(thumbnailPath)?.let { decodeSampledBitmap(it, reqWidth, reqHeight) }

fun copyAssetsForMpv(context: Context, configDir: String) {
    val assetManager = context.assets
    arrayOf(
        "subfont.ttf", "cacert.pem"
    ).forEach { filename ->
        try {
            assetManager.open(filename, AssetManager.ACCESS_STREAMING).use { ins ->
                val outFile = File("$configDir/$filename")
                // Note that .available() officially returns an *estimated* number of bytes available
                // this is only true for generic streams, asset streams return the full file size
                if (outFile.length() == ins.available().toLong()) {
                    Log.v(
                        "copyAssetsForMpv",
                        "Skipping copy of asset file (exists same size): $filename"
                    )
                    return@forEach
                }
                FileOutputStream(outFile).use { out -> ins.copyTo(out) }
                Log.w("copyAssetsForMpv", "Copied asset file: $filename")
            }
        } catch (e: IOException) {
            Log.e("copyAssetsForMpv", "Failed to copy asset file: $filename", e)
        }
    }
}

fun PlayerState.playbackState(): Int = when {
    idling || position < 0 || duration <= 0 || playlist.isEmpty() -> {
        PlaybackStateCompat.STATE_NONE
    }

    pausedForCache -> PlaybackStateCompat.STATE_BUFFERING
    paused -> PlaybackStateCompat.STATE_PAUSED
    else -> PlaybackStateCompat.STATE_PLAYING
}

val PlayerState.isPlaying
    get() = playbackState() != PlaybackStateCompat.STATE_PLAYING
