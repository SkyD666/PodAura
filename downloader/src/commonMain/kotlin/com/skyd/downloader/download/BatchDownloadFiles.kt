package com.skyd.downloader.download

import co.touchlab.kermit.Logger
import com.skyd.downloader.Status
import com.skyd.downloader.db.DownloadDao
import com.skyd.downloader.db.DownloadEntity
import com.skyd.downloader.util.DownloadPublicationException
import com.skyd.downloader.util.FileUtil
import com.skyd.downloader.util.publishPartWithoutReplacement
import com.skyd.downloader.util.shareFileIdentity
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.delete
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.size
import io.github.vinceglb.filekit.source
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.io.Buffer
import kotlinx.io.IOException
import kotlinx.io.readByteArray
import org.kotlincrypto.hash.md.MD5
import kotlin.uuid.Uuid

internal class BatchDownloadFiles(
    entity: DownloadEntity,
    private val savePublication: suspend (digest: String?, totalBytes: Long) -> Unit,
    private val clearPublication: suspend () -> Unit = {},
) : DownloadTaskFiles(entity) {
    private val staged =
        PlatformFile(PlatformFile(entity.path), ".podaura-download-${entity.id}.part")

    override suspend fun partial(fileName: String): PlatformFile = staged

    override suspend fun discardPartial(fileName: String) {
        staged.delete(mustExist = false)
    }

    override suspend fun publish(fileName: String, totalBytes: Long) {
        savePublication(digest(), totalBytes)
        publishVerifiedPart(fileName)
    }

    override suspend fun claimDestination(
        fileName: String,
        resuming: Boolean,
        claim: suspend (String) -> Boolean,
    ): String? {
        suspend fun claimFreeName(name: String): Boolean =
            !FileUtil.finalFileExists(entity.path, name) && claim(name)

        if (claimFreeName(fileName)) return fileName
        if (entity.requestedFileName.isEmpty() && !resuming) {
            repeat(RESPONSE_NAME_ATTEMPTS) {
                val alternative = suffixedBatchFileName(fileName)
                if (claimFreeName(alternative)) return alternative
            }
        }
        return null
    }

    override suspend fun recover(claim: suspend (TransferResponseMetadata) -> String): Long? {
        val expected = entity.publicationDigest ?: return null
        val totalBytes = entity.downloadedBytes
        if (!matchesPreparedPart(expected)) {
            // Unlink before retrying because the changed staging file may still share a published inode.
            discardPartial(entity.fileName)
            savePublication(null, totalBytes)
            return null
        }
        if (sharesPublishedFileIdentity()) return totalBytes
        val name = claim(
            TransferResponseMetadata(
                suggestedFileName = null,
                eTag = entity.eTag,
                lastModified = entity.lastModified,
                finalUrl = entity.finalUrl,
                totalBytes = totalBytes,
            )
        )
        publishVerifiedPart(name)
        return totalBytes
    }

    override suspend fun finish() {
        try {
            discardPartial(entity.fileName)
            clearPublication()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            log.e(throwable = error) { "Unable to clean completed download staging" }
        }
    }

    override suspend fun discardCancelled() {
        if (!matchesPublishedFile()) discardPartial(entity.fileName)
    }

    override suspend fun ownsDestination(): Boolean =
        entity.status == Status.Success.name || matchesPublishedFile()

    private suspend fun publishVerifiedPart(fileName: String) {
        try {
            withContext(Dispatchers.IO + NonCancellable) {
                val target = FileUtil.finalFile(entity.path, fileName)
                if (!publishPartWithoutReplacement(part = staged, target = target)) {
                    throw IOException("The download destination already exists")
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            throw DownloadFailure(
                code = DownloadFailureCode.Storage,
                message = (error as? DownloadPublicationException)?.message
                    ?: "Unable to save the downloaded file",
                retryable = false,
                cause = error,
            )
        }
    }

    private suspend fun matchesPublishedFile(): Boolean {
        val expected = entity.publicationDigest ?: return false
        return matchesPreparedPart(expected) && sharesPublishedFileIdentity()
    }

    private suspend fun sharesPublishedFileIdentity(): Boolean = withContext(Dispatchers.IO) {
        try {
            shareFileIdentity(
                part = staged,
                target = FileUtil.finalFile(entity.path, entity.fileName)
            )
        } catch (_: Exception) {
            false
        }
    }

    private suspend fun matchesPreparedPart(expected: String): Boolean = try {
        staged.exists() && staged.size() == entity.downloadedBytes && digest() == expected
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        false
    }

    private suspend fun digest(): String = withContext(Dispatchers.IO) {
        val digest = MD5()
        val buffer = Buffer()
        staged.source().use { source ->
            while (source.readAtMostTo(buffer, 64L * 1024) != -1L) {
                digest.update(buffer.readByteArray())
            }
        }
        digest.digest().toHexString()
    }

    private companion object {
        const val RESPONSE_NAME_ATTEMPTS = 8
        val log = Logger.withTag("BatchDownloadFiles")
    }
}

internal fun downloadTaskFiles(entity: DownloadEntity, dao: DownloadDao): DownloadTaskFiles {
    if (entity.batchId == null) return DownloadTaskFiles(entity)
    return BatchDownloadFiles(
        entity = entity,
        savePublication = { digest, totalBytes ->
            if (dao.setPublication(
                    id = entity.id,
                    attemptId = entity.attemptId,
                    digest = digest,
                    totalBytes = totalBytes,
                ) == 0
            ) {
                throw CancellationException("Download attempt is no longer current")
            }
        },
        clearPublication = {
            dao.clearCompletedPublication(id = entity.id, attemptId = entity.attemptId)
        },
    )
}

internal suspend fun cleanCompletedDownloadFiles(dao: DownloadDao) {
    for (entity in dao.findUncleanedPublications()) {
        downloadTaskFiles(entity = entity, dao = dao).finish()
    }
}

internal val DownloadEntity.readyForStartupScheduling: Boolean
    get() = attemptId.isNotBlank() && (status == Status.Queued.name || publicationDigest != null)

internal val DownloadEntity.resumeInterruptedAtStartup: Boolean
    get() = batchId != null && readyForStartupScheduling

internal fun suffixedBatchFileName(fileName: String): String {
    val suffix = "-${Uuid.random().toHexString().take(8)}"
    val extension = fileName.substringAfterLast('.', "")
    val maxLength = FileUtil.MAX_FILE_NAME_LENGTH
    val ending =
        if (extension.isEmpty()) "" else ".${extension.take(maxLength - suffix.length - 2)}"
    val stem = fileName.substringBeforeLast('.').take(maxLength - suffix.length - ending.length)
    return FileUtil.sanitizeFileName("$stem$suffix$ending")
}
