package com.skyd.podaura.model.repository.fullcontent

data class FullContent(
    val html: String,
    val sourceUrl: String,
)

class FullContentException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
