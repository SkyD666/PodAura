package com.skyd.podaura.ui.player

import android.content.Context
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import com.skyd.podaura.di.get
import com.skyd.podaura.ext.getImage
import com.skyd.podaura.ui.component.imageLoaderBuilder
import com.skyd.podaura.ui.player.service.PlayerState
import com.skyd.podaura.util.image.decodeSampledBitmap
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

/*internal*/ fun Uri.resolveUri(context: Context): String? {
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
    } catch (e: Exception) {
        Log.e("findRealPath", "Failed to findRealPath: $e")
    } finally {
        ins?.close()
    }
    return null
}

suspend fun createThumbnailFile(
    thumbnailPath: String?,
): File? {
    thumbnailPath ?: return null
    val context = get<Context>()
    return context.imageLoaderBuilder().build()
        .getImage(context, thumbnailPath)?.toString()?.let { File(it) }
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

    loading -> PlaybackStateCompat.STATE_BUFFERING
    paused -> PlaybackStateCompat.STATE_PAUSED
    else -> PlaybackStateCompat.STATE_PLAYING
}

val PlayerState.isPlaying
    get() = playbackState() != PlaybackStateCompat.STATE_PLAYING
