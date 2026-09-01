package com.skyd.downloader.download

import com.skyd.downloader.db.DownloadEntity
import com.skyd.downloader.util.FileUtil
import com.skyd.fundation.ext.currentTimeMillis
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.sink
import io.github.vinceglb.filekit.size
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.request
import io.ktor.http.ContentDisposition
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.contentLength
import io.ktor.http.decodeURLPart
import io.ktor.http.isSuccess
import io.ktor.http.takeFrom
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.core.remaining
import io.ktor.utils.io.exhausted
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.CancellationException
import kotlinx.io.IOException
import kotlin.math.max

internal data class TransferResponseMetadata(
    val suggestedFileName: String?,
    val eTag: String,
    val lastModified: String,
    val finalUrl: String,
    val totalBytes: Long,
)

internal data class TransferProgress(
    val downloadedBytes: Long,
    val totalBytes: Long,
    val speedInBytePerMs: Float,
)

internal data class TransferResult(
    val totalBytes: Long,
    val finalUrl: String,
)

internal class DownloadTransferEngine(
    private val httpClient: HttpClient = HttpClient {
        followRedirects = false
    },
) {
    suspend fun transfer(
        entity: DownloadEntity,
        onResponse: suspend (TransferResponseMetadata) -> String,
        onProgress: suspend (TransferProgress) -> Unit,
    ): TransferResult {
        var fileName = entity.fileName
        var partFile = FileUtil.resolvePartFile(entity.path, fileName)
        var rangeStart = partFile.takeIf { it.exists() }?.size()?.coerceAtLeast(0) ?: 0
        val validator = entity.strongValidator()
        if (rangeStart > 0 && validator == null) {
            FileUtil.deletePartIfExists(entity.path, fileName)
            rangeStart = 0
        }

        var requestUrl = entity.url
        var redirects = 0
        var restarted = false

        while (true) {
            val rangeOffset = rangeStart
            val step = try {
                httpClient.prepareGet(requestUrl) {
                    header(HttpHeaders.AcceptEncoding, "identity")
                    if (rangeOffset > 0) {
                        header(HttpHeaders.Range, "bytes=$rangeOffset-")
                        validator?.let { header(HttpHeaders.IfRange, it) }
                    }
                }.execute { response ->
                    if (response.status.value in REDIRECT_STATUS_CODES) {
                        val location = response.headers[HttpHeaders.Location]
                            ?: throw DownloadFailure(
                                DownloadFailureCode.Redirect,
                                "Redirect response has no location",
                                retryable = false,
                            )
                        return@execute TransferStep.Redirect(
                            URLBuilder(response.request.url).takeFrom(location).buildString()
                        )
                    }

                    if (response.status == HttpStatusCode.RequestedRangeNotSatisfiable) {
                        val total =
                            parseUnsatisfiedTotal(response.headers[HttpHeaders.ContentRange])
                        return@execute if (rangeOffset > 0 && total == rangeOffset) {
                            onResponse(
                                TransferResponseMetadata(
                                    suggestedFileName = null,
                                    eTag = response.headers[HttpHeaders.ETag] ?: entity.eTag,
                                    lastModified = response.headers[HttpHeaders.LastModified]
                                        ?: entity.lastModified,
                                    finalUrl = response.request.url.toString(),
                                    totalBytes = rangeOffset,
                                )
                            )
                            TransferStep.Complete(rangeOffset, response.request.url.toString())
                        } else {
                            TransferStep.Restart
                        }
                    }

                    if (!response.status.isSuccess()) {
                        throw response.toFailure()
                    }

                    val contentRange = parseContentRange(
                        response.headers[HttpHeaders.ContentRange]
                    )
                    if (response.status == HttpStatusCode.PartialContent) {
                        if (contentRange == null || contentRange.start != rangeOffset) {
                            return@execute TransferStep.Restart
                        }
                    } else if (rangeOffset > 0 && response.status != HttpStatusCode.OK) {
                        return@execute TransferStep.Restart
                    }

                    val append = rangeOffset > 0 && response.status == HttpStatusCode.PartialContent
                    val actualStart = if (append) rangeOffset else 0
                    val responseLength = response.contentLength()
                    val rangeLength = contentRange?.let { it.end - it.start + 1 }
                    if (rangeLength != null && responseLength != null &&
                        rangeLength != responseLength
                    ) {
                        return@execute TransferStep.Restart
                    }
                    val totalBytes = contentRange?.total
                        ?: responseLength?.let { actualStart + it }
                        ?: 0
                    val responseETag = response.headers[HttpHeaders.ETag].orEmpty()
                    val responseLastModified = response.headers[HttpHeaders.LastModified].orEmpty()

                    if (append && entity.eTag.isNotBlank() && responseETag.isNotBlank() &&
                        entity.eTag != responseETag
                    ) {
                        return@execute TransferStep.Restart
                    }
                    if (append && entity.eTag.isBlank() && entity.lastModified.isNotBlank() &&
                        responseLastModified.isNotBlank() && entity.lastModified != responseLastModified
                    ) {
                        return@execute TransferStep.Restart
                    }

                    val responseFileName = response.suggestedFileName()
                    val resolvedFileName = onResponse(
                        TransferResponseMetadata(
                            suggestedFileName = responseFileName,
                            eTag = responseETag,
                            lastModified = responseLastModified,
                            finalUrl = response.request.url.toString(),
                            totalBytes = totalBytes,
                        )
                    )
                    if (resolvedFileName != fileName) {
                        check(actualStart == 0L) { "A resumed task cannot change its file name" }
                        fileName = resolvedFileName
                        partFile = FileUtil.resolvePartFile(entity.path, fileName)
                    }

                    val downloaded = streamResponse(
                        channel = response.body(),
                        append = append,
                        rangeStart = actualStart,
                        totalBytes = totalBytes,
                        partFile = partFile,
                        onProgress = onProgress,
                    )
                    val expectedDownloaded = totalBytes.takeIf { it > 0 }
                        ?: rangeLength?.let { actualStart + it }
                    if (expectedDownloaded != null && downloaded != expectedDownloaded) {
                        throw DownloadFailure(
                            DownloadFailureCode.Integrity,
                            "Downloaded length does not match the response length",
                            retryable = true,
                        )
                    }
                    TransferStep.Complete(downloaded, response.request.url.toString())
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: DownloadFailure) {
                throw error
            } catch (error: Throwable) {
                throw DownloadFailure(
                    DownloadFailureCode.Network,
                    "Network transfer failed",
                    retryable = true,
                    cause = error,
                )
            }

            when (step) {
                is TransferStep.Redirect -> {
                    redirects++
                    if (redirects > MAX_REDIRECTS) {
                        throw DownloadFailure(
                            DownloadFailureCode.Redirect,
                            "Too many redirects",
                            retryable = false,
                        )
                    }
                    validateRedirect(requestUrl, step.url)
                    requestUrl = step.url
                }

                TransferStep.Restart -> {
                    if (restarted) {
                        throw DownloadFailure(
                            DownloadFailureCode.InvalidResponse,
                            "Server returned an invalid range response",
                            retryable = false,
                        )
                    }
                    restarted = true
                    FileUtil.deletePartIfExists(entity.path, fileName)
                    rangeStart = 0
                    requestUrl = entity.url
                    redirects = 0
                }

                is TransferStep.Complete -> {
                    try {
                        FileUtil.commitPart(entity.path, fileName)
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: Throwable) {
                        throw DownloadFailure(
                            DownloadFailureCode.Storage,
                            "Unable to save the downloaded file",
                            retryable = false,
                            cause = error,
                        )
                    }
                    return TransferResult(step.totalBytes, step.finalUrl)
                }
            }
        }
    }

    private suspend fun streamResponse(
        channel: ByteReadChannel,
        append: Boolean,
        rangeStart: Long,
        totalBytes: Long,
        partFile: io.github.vinceglb.filekit.PlatformFile,
        onProgress: suspend (TransferProgress) -> Unit,
    ): Long {
        var received = rangeStart
        var lastReportedBytes = rangeStart
        var lastReportedAt = kotlin.time.Clock.currentTimeMillis()
        onProgress(TransferProgress(received, totalBytes, 0f))
        try {
            partFile.sink(append = append).use { sink ->
                while (!channel.exhausted()) {
                    val chunk = channel.readRemaining(CHUNK_SIZE)
                    val chunkSize = chunk.remaining
                    chunk.transferTo(sink)
                    received += chunkSize
                    val now = kotlin.time.Clock.currentTimeMillis()
                    if (now - lastReportedAt >= PROGRESS_INTERVAL_MILLIS) {
                        val elapsed = max(1, now - lastReportedAt)
                        onProgress(
                            TransferProgress(
                                downloadedBytes = received,
                                totalBytes = totalBytes,
                                speedInBytePerMs = (received - lastReportedBytes).toFloat() / elapsed,
                            )
                        )
                        lastReportedBytes = received
                        lastReportedAt = now
                    }
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: IOException) {
            throw DownloadFailure(
                DownloadFailureCode.Network,
                "Transfer interrupted",
                retryable = true,
                cause = error,
            )
        }
        onProgress(TransferProgress(received, totalBytes, 0f))
        return received
    }

    private fun DownloadEntity.strongValidator(): String? = when {
        eTag.isNotBlank() && !eTag.startsWith("W/", ignoreCase = true) -> eTag
        lastModified.isNotBlank() -> lastModified
        else -> null
    }

    private fun io.ktor.client.statement.HttpResponse.suggestedFileName(): String? {
        val raw = headers[HttpHeaders.ContentDisposition] ?: return null
        val disposition = runCatching { ContentDisposition.parse(raw) }.getOrNull() ?: return null
        val encoded = disposition.parameter(ContentDisposition.Parameters.FileNameAsterisk)
            ?.substringAfter("''", missingDelimiterValue = "")
            ?.takeIf { it.isNotBlank() }
            ?.let { runCatching { it.decodeURLPart() }.getOrNull() }
        return encoded ?: disposition.parameter(ContentDisposition.Parameters.FileName)
    }

    private fun io.ktor.client.statement.HttpResponse.toFailure(): DownloadFailure {
        val retryAfter = headers[HttpHeaders.RetryAfter]?.toLongOrNull()?.times(1_000)
        return when {
            status == HttpStatusCode.TooManyRequests -> DownloadFailure(
                DownloadFailureCode.TooManyRequests,
                "Server rate limit reached (${status.value})",
                retryable = true,
                retryAfterMillis = retryAfter,
            )

            status == HttpStatusCode.RequestTimeout -> DownloadFailure(
                DownloadFailureCode.HttpServer,
                "Server request timed out (${status.value})",
                retryable = true,
            )

            status.value in 500..599 -> DownloadFailure(
                DownloadFailureCode.HttpServer,
                "Server error (${status.value})",
                retryable = true,
            )

            else -> DownloadFailure(
                DownloadFailureCode.HttpClient,
                "Download request failed (${status.value})",
                retryable = false,
            )
        }
    }

    private fun validateRedirect(from: String, to: String) {
        val fromUrl = runCatching { io.ktor.http.Url(from) }.getOrNull()
        val toUrl = runCatching { io.ktor.http.Url(to) }.getOrNull()
            ?: throw DownloadFailure(
                DownloadFailureCode.Redirect,
                "Redirect target is invalid",
                retryable = false,
            )
        if (fromUrl?.protocol == URLProtocol.HTTPS && toUrl.protocol != URLProtocol.HTTPS) {
            throw DownloadFailure(
                DownloadFailureCode.InsecureRedirect,
                "HTTPS download cannot redirect to an insecure URL",
                retryable = false,
            )
        }
        if (toUrl.protocol != URLProtocol.HTTP && toUrl.protocol != URLProtocol.HTTPS) {
            throw DownloadFailure(
                DownloadFailureCode.Redirect,
                "Redirect target uses an unsupported protocol",
                retryable = false,
            )
        }
    }

    private fun parseContentRange(value: String?): ParsedContentRange? {
        val match = CONTENT_RANGE.matchEntire(value.orEmpty()) ?: return null
        val start = match.groupValues[1].toLongOrNull() ?: return null
        val end = match.groupValues[2].toLongOrNull() ?: return null
        if (end < start) return null
        val total = match.groupValues[3].takeUnless { it == "*" }?.toLongOrNull() ?: 0
        if (total > 0 && end >= total) return null
        return ParsedContentRange(start, end, total)
    }

    private fun parseUnsatisfiedTotal(value: String?): Long? =
        UNSATISFIED_CONTENT_RANGE.matchEntire(value.orEmpty())
            ?.groupValues?.get(1)?.toLongOrNull()

    private sealed interface TransferStep {
        data class Redirect(val url: String) : TransferStep
        data class Complete(val totalBytes: Long, val finalUrl: String) : TransferStep
        data object Restart : TransferStep
    }

    private data class ParsedContentRange(val start: Long, val end: Long, val total: Long)

    companion object {
        private const val MAX_REDIRECTS = 10
        private const val CHUNK_SIZE = 64L * 1024
        private const val PROGRESS_INTERVAL_MILLIS = 500L
        private val REDIRECT_STATUS_CODES = setOf(301, 302, 303, 307, 308)
        private val CONTENT_RANGE =
            Regex("^bytes (\\d+)-(\\d+)/(\\d+|\\*)$", RegexOption.IGNORE_CASE)
        private val UNSATISFIED_CONTENT_RANGE = Regex("^bytes \\*/(\\d+)$", RegexOption.IGNORE_CASE)
    }
}
