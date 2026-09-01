package com.skyd.downloader.download

enum class DownloadFailureCode {
    Network,
    HttpClient,
    HttpServer,
    TooManyRequests,
    Redirect,
    InsecureRedirect,
    InvalidResponse,
    Integrity,
    Storage,
    DestinationConflict,
    Unknown,
}

internal class DownloadFailure(
    val code: DownloadFailureCode,
    override val message: String,
    val retryable: Boolean,
    val retryAfterMillis: Long? = null,
    cause: Throwable? = null,
) : Exception(message, cause)

internal sealed interface DownloadExecutionResult {
    data class Success(val totalBytes: Long) : DownloadExecutionResult
    data class Retry(val delayMillis: Long) : DownloadExecutionResult
    data object Failed : DownloadExecutionResult
    data object Ignored : DownloadExecutionResult
}
