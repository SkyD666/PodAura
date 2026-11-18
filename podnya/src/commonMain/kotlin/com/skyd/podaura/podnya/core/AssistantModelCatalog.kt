package com.skyd.podaura.podnya.core

import io.ktor.http.Url

internal object AssistantModelCatalog {
    // Picker order, with each provider's default first.
    val openAiResponses = listOf("gpt-6.1-sol", "gpt-6-luna", "gpt-6-astra", "gpt-6.1-terra")
    val deepSeek = listOf("deepseek-flash", "deepseek-v4-pro")
    val claude = listOf("claude-sonnet-5-5", "claude-haiku-5-5", "claude-opus-5-5")
    val gemini = listOf("gemini-4-hydrogen", "gemini-3.8-flash")
    val grok = listOf("grok-4.7")

    class ModelPolicy(
        val protocol: AssistantProtocol,
        val models: List<String> = emptyList(),
        val matchesEndpoint: (String) -> Boolean = { true },
        val supportsReasoningEffort: Boolean = false,
        val timeouts: AssistantTimeouts = AssistantTimeouts.STANDARD,
    )

    // First match wins. Put model-specific declarations before protocol-wide defaults.
    private val policies = listOf(
        ModelPolicy(
            protocol = AssistantProtocol.OPENAI_COMPATIBLE,
            models = openAiResponses,
            matchesEndpoint = { url ->
                runCatching { Url(url).host }.getOrNull()
                    .equals("api.openai.com", ignoreCase = true)
            },
            supportsReasoningEffort = true,
        ),
        ModelPolicy(
            protocol = AssistantProtocol.GEMINI_GENERATE_CONTENT,
            timeouts = AssistantTimeouts.EXTENDED,
        ),
        ModelPolicy(
            protocol = AssistantProtocol.OPENAI_COMPATIBLE,
            matchesEndpoint = {
                it.startsWith(
                    "https://generativelanguage.googleapis.com/",
                    ignoreCase = true
                )
            },
            timeouts = AssistantTimeouts.EXTENDED,
        ),
    )

    fun policy(baseUrl: String, modelId: String, protocol: AssistantProtocol): ModelPolicy? {
        val model = modelId.trim()
        return policies.firstOrNull { policy ->
            policy.protocol == protocol &&
                    (policy.models.isEmpty() || policy.models.any {
                        model == it || model.startsWith("$it-")
                    }) && policy.matchesEndpoint(baseUrl)
        }
    }
}
