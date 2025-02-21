package com.skyd.anivu.ui.mpv

import android.content.Context
import android.content.res.AssetManager
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import com.skyd.anivu.config.Const
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

fun copyAssetsForMpv(context: Context) {
    val assetManager = context.assets
    arrayOf(
        "subfont.ttf", "cacert.pem"
    ).forEach { filename ->
        try {
            assetManager.open(filename, AssetManager.ACCESS_STREAMING).use { ins ->
                val outFile = File("${Const.MPV_CONFIG_DIR.path}/$filename")
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