package com.skyd.downloader.download

import com.skyd.downloader.util.FileUtil
import com.skyd.downloader.util.FileUtil.tempFile
import com.skyd.fundation.ext.currentTimeMillis
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.atomicMove
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.sink
import io.github.vinceglb.filekit.size
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpStatusCode.Companion.RequestedRangeNotSatisfiable
import io.ktor.http.contentLength
import io.ktor.http.isSuccess
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.core.remaining
import io.ktor.utils.io.exhausted
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.io.IOException
import kotlinx.io.RawSink
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

internal class DownloadTask(
    private var url: String,
    private var path: String,
    private var fileName: String,
) : KoinComponent {
    private val httpClientConfig: HttpClientConfig<*>.() -> Unit by inject()
    private val httpClient by lazy { HttpClient(httpClientConfig) }

    suspend fun download(
        headers: MutableMap<String, String> = mutableMapOf(),
        onStart: suspend (Long) -> Unit,
        onProgress: suspend (Long, Long, Float) -> Unit
    ): Long {
        var rangeStart = 0L
        val file = PlatformFile(path) / fileName
        val tempFile = file.tempFile

        if (tempFile.exists()) {
            rangeStart = tempFile.size()
        }

        if (rangeStart != 0L) {
            headers[HttpHeaders.Range] = "bytes=$rangeStart-"
        }

        var totalBytes = 0L
        tempFile.sink(append = true).use { sink ->
            var lastReceived = 0L
            var speed: Float
            var lastProgressTime = Clock.System.now().toEpochMilliseconds()
            requestWithAutoRetry(
                url = url,
                headers = headers,
                sink = sink,
                rangeStart = rangeStart,
                onRangeStart = {
                    rangeStart = it
                    lastReceived = rangeStart
                },
                onContentLength = { contentLength ->
                    totalBytes = contentLength
                    onStart(contentLength)
                },
                onProgress = { received: Long ->
                    val currentTime = Clock.currentTimeMillis()
                    speed = (received - lastReceived) / ((currentTime - lastProgressTime).toFloat())
                    lastReceived = received
                    lastProgressTime = currentTime
                    onProgress(
                        received.coerceAtMost(totalBytes),
                        totalBytes,
                        speed
                    )
                },
            )
            onProgress.invoke(totalBytes, totalBytes, 0f)
        }

        tempFile.atomicMove(file)

        return totalBytes
    }

    private suspend fun requestWithAutoRetry(
        url: String,
        headers: MutableMap<String, String> = mutableMapOf(),
        sink: RawSink,
        rangeStart: Long,
        onRangeStart: suspend (Long) -> Unit,
        onContentLength: suspend (Long) -> Unit,
        onProgress: suspend (Long) -> Unit,
    ) {
        val status = request(
            url = url,
            headers = headers,
            sink = sink,
            rangeStart = rangeStart,
            onRangeStart = onRangeStart,
            onContentLength = onContentLength,
            onProgress = onProgress,
        )
        if (!status.isSuccess()) {
            if (status == RequestedRangeNotSatisfiable) {
                FileUtil.deleteDownloadFileIfExists(path, fileName)
                headers.remove(HttpHeaders.Range)
                request(
                    url = url,
                    headers = headers,
                    sink = sink,
                    rangeStart = 0,
                    onRangeStart = onRangeStart,
                    onContentLength = onContentLength,
                    onProgress = onProgress,
                )
            } else {
                throw IOException("Something went wrong, response code: ${status.value}")
            }
        }
    }

    private suspend fun request(
        url: String,
        headers: MutableMap<String, String> = mutableMapOf(),
        sink: RawSink,
        rangeStart: Long,
        onRangeStart: suspend (Long) -> Unit,
        onContentLength: suspend (Long) -> Unit,
        onProgress: suspend (Long) -> Unit,
    ): HttpStatusCode {
        return httpClient.prepareGet(url) {
            headers.forEach { (k, v) -> header(k, v) }
        }.execute { response ->
            if (!response.status.isSuccess()) {
                return@execute response.status
            }
            val channel: ByteReadChannel = response.body()
            var count = 0L
            coroutineScope {
                sink.use {
                    onRangeStart(rangeStart)
                    onContentLength(rangeStart + (response.contentLength() ?: 0L))
                    onProgress(rangeStart)
                    val periodicJob = launch {
                        while (isActive) {
                            delay(0.5.seconds)
                            onProgress(rangeStart + count)
                        }
                    }
                    while (!channel.exhausted()) {
                        val chunk = channel.readRemaining(100)
                        count += chunk.remaining
                        chunk.transferTo(sink)
                    }
                    periodicJob.cancel()
                    onProgress(rangeStart + count)
                }
                return@coroutineScope response.status
            }
        }
    }
}
