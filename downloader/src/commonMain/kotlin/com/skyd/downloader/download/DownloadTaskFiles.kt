package com.skyd.downloader.download

import com.skyd.downloader.db.DownloadEntity
import com.skyd.downloader.util.FileUtil
import io.github.vinceglb.filekit.PlatformFile

internal open class DownloadTaskFiles(protected val entity: DownloadEntity) {
    open suspend fun partial(fileName: String): PlatformFile =
        FileUtil.resolvePartFile(path = entity.path, fileName = fileName)

    open suspend fun discardPartial(fileName: String) {
        FileUtil.deletePartIfExists(path = entity.path, fileName = fileName)
    }

    open suspend fun publish(fileName: String, totalBytes: Long) {
        FileUtil.commitPart(path = entity.path, fileName = fileName)
    }

    open suspend fun claimDestination(
        fileName: String,
        resuming: Boolean,
        claim: suspend (String) -> Boolean,
    ): String? = if (claim(fileName)) fileName else null

    open suspend fun recover(claim: suspend (TransferResponseMetadata) -> String): Long? = null

    open suspend fun finish() = Unit

    open suspend fun discardCancelled() {
        discardPartial(entity.fileName)
    }

    suspend fun delete(deleteFinal: Boolean) {
        if (deleteFinal && ownsDestination()) {
            FileUtil.deleteFinalIfExists(path = entity.path, fileName = entity.fileName)
        }
        discardPartial(entity.fileName)
    }

    protected open suspend fun ownsDestination(): Boolean = true
}
