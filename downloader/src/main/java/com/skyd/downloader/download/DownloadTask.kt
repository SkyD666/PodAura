package com.skyd.downloader.download

import com.skyd.downloader.net.DownloadService
import com.skyd.downloader.util.FileUtil
import com.skyd.downloader.util.FileUtil.tempFile
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

internal class DownloadTask(
    private var url: String,
    private var path: String,
    private var fileName: String,
    private val downloadService: DownloadService,
) {

    companion object {
        private const val VALUE_200 = 200
        private const val VALUE_299 = 299
        private const val TIME_TO_TRIGGER_PROGRESS = 500

        private const val RANGE_HEADER = "Range"
        private const val HTTP_RANGE_NOT_SATISFY = 416
        internal const val ETAG_HEADER = "ETag"
    }

    suspend fun download(
        headers: MutableMap<String, String> = mutableMapOf(),
        onStart: suspend (Long) -> Unit,
        onProgress: suspend (Long, Long, Float) -> Unit
    ): Long {
        var rangeStart = 0L
        val file = File(path, fileName)
        val tempFile = file.tempFile

        if (tempFile.exists()) {
            rangeStart = tempFile.length()
        }

        if (rangeStart != 0L) {
            headers[RANGE_HEADER] = "bytes=$rangeStart-"
        }

        var response = downloadService.getUrl(url, headers)
        if (response.code() == HTTP_RANGE_NOT_SATISFY) {
            FileUtil.deleteDownloadFileIfExists(path, fileName)
            headers.remove(RANGE_HEADER)
            rangeStart = 0
            response = downloadService.getUrl(url, headers)
        }

        val responseBody = response.body()

        if (response.code() !in VALUE_200..VALUE_299 ||
            responseBody == null
        ) {
            throw IOException(
                "Something went wrong, response code: ${response.code()}, responseBody null: ${responseBody == null}"
            )
        }

        var totalBytes = responseBody.contentLength()
        if (totalBytes < 0) throw IOException("Content Length is wrong: $totalBytes")
        totalBytes += rangeStart

        var progressBytes = 0L

        responseBody.byteStream().use { inputStream ->
            FileOutputStream(tempFile, true).use { outputStream ->
                if (rangeStart != 0L) {
                    progressBytes = rangeStart
                }

                onStart.invoke(totalBytes)

                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var bytes = inputStream.read(buffer)
                var tempBytes = 0L
                var progressInvokeTime = System.currentTimeMillis()
                var speed: Float

                while (bytes >= 0) {
                    outputStream.write(buffer, 0, bytes)
                    progressBytes += bytes
                    tempBytes += bytes
                    bytes = inputStream.read(buffer)
                    val finalTime = System.currentTimeMillis()
                    if (finalTime - progressInvokeTime >= TIME_TO_TRIGGER_PROGRESS) {
                        speed = tempBytes.toFloat() / ((finalTime - progressInvokeTime).toFloat())
                        tempBytes = 0L
                        progressInvokeTime = System.currentTimeMillis()
                        if (progressBytes > totalBytes) progressBytes = totalBytes
                        onProgress.invoke(
                            progressBytes,
                            totalBytes,
                            speed
                        )
                    }
                }
                onProgress.invoke(totalBytes, totalBytes, 0F)
            }
        }

        require(tempFile.renameTo(file)) { "Temp file rename failed" }

        return totalBytes
    }
}
