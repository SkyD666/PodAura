package com.skyd.podaura.model.repository.fullcontent

internal data class RenderedPageSnapshot(
    val html: String,
    val finalUrl: String,
)

internal interface RenderedPageProvider {
    suspend fun render(url: String): RenderedPageSnapshot
}

internal class RenderedPageException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

