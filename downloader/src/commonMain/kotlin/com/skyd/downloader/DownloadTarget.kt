package com.skyd.downloader

import com.skyd.downloader.db.DownloadDao
import com.skyd.downloader.db.DownloadEntity
import com.skyd.downloader.download.DownloadConstraints
import com.skyd.downloader.util.FileUtil
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
internal data class DownloadTarget(
    val url: String,
    val path: String,
    val fileName: String,
    val requestedFileName: String,
) {
    suspend fun findSource(dao: DownloadDao) = dao.findBySourceAndTarget(
        url = url,
        path = path,
        requestedFileName = requestedFileName,
    )

    fun queuedEntity(
        now: Long,
        attemptId: String,
        metadata: String?,
        constraints: DownloadConstraints,
    ) = DownloadEntity(
        id = Uuid.random().toString(),
        url = url,
        path = path,
        fileName = fileName,
        requestedFileName = requestedFileName,
        fileNameResolved = requestedFileName.isNotEmpty(),
        timeQueued = now,
        status = Status.Queued.name,
        attemptId = attemptId,
        createTime = now,
        updatedTime = now,
        metadata = metadata,
        requireUnmetered = constraints.requireUnmetered,
        requiresCharging = constraints.requiresCharging,
        requiresBatteryNotLow = constraints.requiresBatteryNotLow,
    )

    companion object {
        fun resolve(url: String, path: String, fileName: String?): DownloadTarget {
            val parsedUrl = runCatching { Url(url) }.getOrElse {
                throw IllegalArgumentException("Invalid download URL")
            }
            require(parsedUrl.protocol == URLProtocol.HTTP || parsedUrl.protocol == URLProtocol.HTTPS) {
                "Only HTTP and HTTPS downloads are supported"
            }
            val explicitFileName = fileName?.let(FileUtil::sanitizeFileName)
            val resolvedFileName = explicitFileName ?: FileUtil.getFileNameFromUrl(url)
            FileUtil.validateTarget(path, resolvedFileName)
            return DownloadTarget(
                url = url,
                path = path,
                fileName = resolvedFileName,
                requestedFileName = explicitFileName.orEmpty(),
            )
        }
    }
}
