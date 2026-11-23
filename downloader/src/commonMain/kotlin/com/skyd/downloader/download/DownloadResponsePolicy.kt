package com.skyd.downloader.download

import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentDisposition
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readBuffer
import kotlinx.io.Buffer
import kotlinx.io.readByteArray

internal enum class DownloadResponsePolicy {
    AllowAnyContent,
    RequireFile;

    suspend fun inspect(
        response: HttpResponse,
        channel: ByteReadChannel,
        disposition: ContentDisposition?,
        appending: Boolean,
    ): Buffer? {
        if (this == AllowAnyContent) return null
        if (response.status.value == 204 || response.status.value == 205) throw webPageResponse()
        if (disposition?.disposition.equals("attachment", ignoreCase = true)) return null

        val mime =
            response.headers[HttpHeaders.ContentType]?.substringBefore(';')?.trim()?.lowercase()
        if (mime == "text/html" || mime == "application/xhtml+xml") throw webPageResponse()
        if (appending) return null

        val prefix = channel.readBuffer(FILE_PREFIX_BYTES)
        val text = prefix.peek().readByteArray().decodeToString()
            .trimStart { it.isWhitespace() || it == '\uFEFF' }
        if (WEB_DOCUMENT_PREFIX.containsMatchIn(text)) throw webPageResponse()
        return prefix
    }

    fun validateRedirect(target: Url) {
        if (this == RequireFile && (target.host.isBlank() || target.user != null || target.password != null)) {
            throw DownloadFailure(
                code = DownloadFailureCode.Redirect,
                message = "Redirect target is not a direct file link",
                retryable = false,
            )
        }
    }

    companion object {
        fun of(requireFile: Boolean): DownloadResponsePolicy =
            if (requireFile) RequireFile else AllowAnyContent

        private fun webPageResponse() = DownloadFailure(
            code = DownloadFailureCode.Webpage,
            message = "This URL does not return a file. Provide a direct file link.",
            retryable = false,
        )

        private const val FILE_PREFIX_BYTES = 512L
        private val WEB_DOCUMENT_PREFIX = Regex(
            """^(?:(?:<!--.*?-->|<\?xml.*?\?>)\s*)*(?:<!doctype\s+html\b|<(?:html|head|body|script|title|meta|iframe|video|audio|div)\b)""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        )
    }
}
