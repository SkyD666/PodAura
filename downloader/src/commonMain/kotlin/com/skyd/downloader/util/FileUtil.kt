package com.skyd.downloader.util

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.atomicMove
import io.github.vinceglb.filekit.delete
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.parent
import io.github.vinceglb.filekit.path
import io.ktor.http.decodeURLPart

internal object FileUtil {
    private const val MAX_FILE_NAME_LENGTH = 240

    fun partFile(parent: PlatformFile, fileName: String): PlatformFile = parent / "$fileName.part"

    private fun legacyTempFile(parent: PlatformFile, fileName: String): PlatformFile =
        parent / "$fileName.temp"

    fun finalFile(path: String, fileName: String): PlatformFile = PlatformFile(path) / fileName

    fun getFileNameFromUrl(url: String): String {
        val cleanUrl = url.substringBefore('?').substringBefore('#')
        val encodedName = cleanUrl.substringAfterLast('/').takeIf { it.isNotEmpty() }
            ?: return "download"
        val decodedName = runCatching { encodedName.decodeURLPart() }.getOrDefault(encodedName)
        return sanitizeFileName(decodedName)
    }

    fun sanitizeFileName(value: String): String {
        val sanitized = buildString(value.length) {
            value.trim().forEach { char ->
                append(
                    when {
                        char.code < 32 -> '_'
                        char in INVALID_FILE_NAME_CHARS -> '_'
                        else -> char
                    }
                )
            }
        }.trim(' ', '.')
            .take(MAX_FILE_NAME_LENGTH)
        val validName = sanitized.takeUnless { it.isBlank() || it == "." || it == ".." }
            ?: return "download"
        return if (validName.substringBefore('.').uppercase() in WINDOWS_RESERVED_NAMES) {
            "_$validName"
        } else {
            validName
        }
    }

    suspend fun resolvePartFile(path: String, fileName: String): PlatformFile {
        val parent = PlatformFile(path)
        val part = partFile(parent, fileName)
        val legacy = legacyTempFile(parent, fileName)
        if (!part.exists() && legacy.exists()) {
            legacy.atomicMove(part)
        }
        return part
    }

    suspend fun commitPart(path: String, fileName: String) {
        val parent = PlatformFile(path)
        val part = partFile(parent, fileName)
        val target = parent / fileName
        val backup = parent / "$fileName.download-backup"

        if (backup.exists()) backup.delete(mustExist = false)
        if (target.exists()) target.atomicMove(backup)
        try {
            part.atomicMove(target)
            backup.delete(mustExist = false)
        } catch (error: Throwable) {
            if (!target.exists() && backup.exists()) backup.atomicMove(target)
            throw error
        }
    }

    suspend fun deletePartIfExists(path: String, fileName: String) {
        val parent = PlatformFile(path)
        partFile(parent, fileName).delete(mustExist = false)
        legacyTempFile(parent, fileName).delete(mustExist = false)
    }

    suspend fun deleteFinalIfExists(path: String, fileName: String) {
        finalFile(path, fileName).delete(mustExist = false)
    }

    suspend fun deleteDownloadFiles(path: String, fileName: String) {
        deleteFinalIfExists(path, fileName)
        deletePartIfExists(path, fileName)
    }

    fun finalFileExists(path: String, fileName: String): Boolean =
        finalFile(path, fileName).exists()

    fun validateTarget(path: String, fileName: String) {
        require(path.isNotBlank()) { "Download path is empty" }
        require(fileName == sanitizeFileName(fileName)) { "Unsafe download file name" }
        val parent = PlatformFile(path)
        val target = finalFile(path, fileName)
        require(target.parent()?.path == parent.path) { "Download target escapes its directory" }
        require(target.name == fileName) { "Invalid download target" }
    }

    private val INVALID_FILE_NAME_CHARS = setOf('/', '\\', ':', '*', '?', '"', '<', '>', '|')
    private val WINDOWS_RESERVED_NAMES = buildSet {
        addAll(listOf("CON", "PRN", "AUX", "NUL"))
        (1..9).forEach { index ->
            add("COM$index")
            add("LPT$index")
        }
    }
}
