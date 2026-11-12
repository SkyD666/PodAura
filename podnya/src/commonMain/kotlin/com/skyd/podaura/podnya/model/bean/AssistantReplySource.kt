package com.skyd.podaura.podnya.model.bean

/** Actual non-secret origin of this reply; immutable even after settings change. */
data class AssistantReplySource(
    val configId: String,
    val revision: Long,
    val baseUrl: String,
    val modelId: String
)
