package com.skyd.podaura.podnya.core

/** Never use a data class for credentials: generated toString/copy would expose the key. */
class AssistantConnection(
    val baseUrl: String,
    val modelId: String,
    val apiKey: String,
    val protocol: AssistantProtocol = AssistantProtocol.OPENAI_COMPATIBLE,
    reasoningEffort: AssistantReasoningEffort = AssistantReasoningEffort.LOW,
) {
    private val policy = AssistantModelCatalog.policy(baseUrl = baseUrl, modelId = modelId, protocol = protocol)
    val reasoningEffort: AssistantReasoningEffort? = reasoningEffort.takeIf { policy?.supportsReasoningEffort == true }
    internal val timeouts = this.reasoningEffort?.timeoutOverride ?: policy?.timeouts ?: AssistantTimeouts.STANDARD

    override fun toString(): String = "AssistantConnection([redacted])"
}

