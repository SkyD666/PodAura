package com.skyd.downloader.util

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.delete
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.path
import org.kotlincrypto.hash.md.MD5
import kotlin.experimental.and

internal object FileUtil {

    internal fun tempFile(parent: PlatformFile, fileName: String): PlatformFile =
        parent / "$fileName.temp"

    fun getFileNameFromUrl(url: String): String {
        val cleanUrl = url.substringBefore('?').substringBefore('#')
        return cleanUrl.substringAfterLast('/').takeIf { it.isNotEmpty() } ?: "download"
    }

    fun getUniqueId(url: String, dirPath: String, fileName: String): Int {
        val hash: ByteArray = try {
            val string = (PlatformFile(url) / dirPath / fileName).path
            val digest = MD5()
            digest.update(string.encodeToByteArray())
            ByteArray(digest.digestLength()).apply {
                digest.digestInto(dest = this, destOffset = 0)
            }
        } catch (_: Exception) {
            return getUniqueIdFallback(url, dirPath, fileName)
        }
        val hex = StringBuilder(hash.size * 2)
        for (b in hash) {
            if (b and 0xFF.toByte() < 0x10) hex.append("0")
            hex.append((b and 0xFF.toByte()).toString(16))
        }
        return hex.toString().hashCode()
    }

    private fun getUniqueIdFallback(url: String, dirPath: String, fileName: String): Int {
        return (url.hashCode() * 31 + dirPath.hashCode()) * 31 + fileName.hashCode()
    }

    suspend fun deleteDownloadFileIfExists(path: String, name: String) {
        val parent = PlatformFile(path)
        (parent / name).delete(mustExist = false)
        tempFile(parent, name).delete(mustExist = false)
    }
}
