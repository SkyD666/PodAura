package com.skyd.podaura.podnya.core

/** Never use a data class for credentials: generated toString/copy would expose the key. */
class AssistantConnection(
    val baseUrl: String,
    val modelId: String,
    val apiKey: String,
    val protocol: AssistantProtocol = AssistantProtocol.OPENAI_COMPATIBLE,
) {
    override fun toString(): String = "AssistantConnection([redacted])"
}

