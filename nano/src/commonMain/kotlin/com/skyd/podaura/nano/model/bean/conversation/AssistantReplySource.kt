package com.skyd.podaura.nano.model.bean.conversation

import kotlinx.serialization.Serializable

/** Actual non-secret origin of this reply; immutable even after settings change. */
@Serializable
data class AssistantReplySource(
    val configId: String,
    val revision: Long,
    val baseUrl: String,
    val modelId: String,
    val protocol: String? = null,
    val reasoningEffort: String? = null,
    val profileName: String? = null,
)
