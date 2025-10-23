package com.skyd.downloader.util

import com.skyd.fundation.ext.deleteRecursively
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.path
import io.github.vinceglb.filekit.toKotlinxIoPath
import org.kotlincrypto.hash.md.MD5
import kotlin.experimental.and

internal object FileUtil {

    internal val PlatformFile.tempFile
        get() = PlatformFile("$path.temp")

    fun getFileNameFromUrl(url: String): String {
        val cleanUrl = url.substringBefore('?').substringBefore('#')
        return cleanUrl.substringAfterLast('/').takeIf { it.isNotEmpty() } ?: "download"
    }

    fun getUniqueId(url: String, dirPath: String, fileName: String): Int {
        val hash: ByteArray = try {
            val string = (PlatformFile(url) / dirPath / fileName).path
            val digest = MD5()
            digest.update(string.toByteArray(charset("UTF-8")))
            ByteArray(digest.digestLength()).apply {
                digest.digestInto(dest = this, destOffset = 0)
            }
        } catch (_: Exception) {
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
