package com.skyd.downloader.util

import android.os.Environment
import android.webkit.URLUtil
import com.skyd.fundation.ext.deleteRecursively
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.path
import io.github.vinceglb.filekit.toKotlinxIoPath
import java.io.File
import java.security.MessageDigest
import kotlin.experimental.and

internal object FileUtil {

    internal val PlatformFile.tempFile
        get() = PlatformFile("$path.temp")

    fun getFileNameFromUrl(url: String): String {
        return URLUtil.guessFileName(url, null, null)
    }

    fun getDefaultDownloadPath(): String {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path
    }

    fun getUniqueId(url: String, dirPath: String, fileName: String): Int {
        val string = url + File.separator + dirPath + File.separator + fileName
        val hash: ByteArray = try {
            MessageDigest.getInstance("MD5").digest(string.toByteArray(charset("UTF-8")))
        } catch (e: Exception) {
            return getUniqueIdFallback(url, dirPath, fileName)
        }
        val hex = StringBuilder(hash.size * 2)
        for (b in hash) {
            if (b and 0xFF.toByte() < 0x10) hex.append("0")
            hex.append(Integer.toHexString((b and 0xFF.toByte()).toInt()))
        }
        return hex.toString().hashCode()
    }

    private fun getUniqueIdFallback(url: String, dirPath: String, fileName: String): Int {
        return (url.hashCode() * 31 + dirPath.hashCode()) * 31 + fileName.hashCode()
    }

    fun deleteDownloadFileIfExists(path: String, name: String) {
        val file = PlatformFile(path) / name
        file.toKotlinxIoPath().deleteRecursively(mustExist = false)
        file.tempFile.toKotlinxIoPath().deleteRecursively(mustExist = false)
    }
}
