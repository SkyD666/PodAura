package com.skyd.podaura.podnya.settings

import com.skyd.podaura.podnya.core.AssistantModelIds
import com.skyd.podaura.podnya.core.AssistantProtocol
import com.skyd.podaura.podnya.core.normalizeAssistantBaseUrl

/** Form defaults only. Saved connections never consult this catalogue at execution time. */
data class AssistantProviderPreset(
    val id: String,
    val displayName: String,
    val baseUrl: String,
    val protocol: AssistantProtocol,
    val models: List<String>,
    val defaultModelId: String = models.first(),
)

object AssistantProviderPresets {
    const val CUSTOM = "custom"

    // Official documentation checked 2026-11-15; source links and validation limits are in the design.
    val entries = listOf(
        AssistantProviderPreset(
            id = "openai",
            displayName = "OpenAI",
            baseUrl = "https://api.openai.com/v1/",
            protocol = AssistantProtocol.OPENAI_COMPATIBLE,
            models = AssistantModelIds.openAiResponses,
        ),
        AssistantProviderPreset(
            id = "deepseek",
            displayName = "DeepSeek",
            baseUrl = "https://api.deepseek.com/",
            protocol = AssistantProtocol.OPENAI_COMPATIBLE,
            models = AssistantModelIds.deepSeek,
        ),
        AssistantProviderPreset(
            id = "claude",
            displayName = "Claude",
            baseUrl = "https://api.anthropic.com/v1/",
            protocol = AssistantProtocol.ANTHROPIC_MESSAGES,
            models = AssistantModelIds.claude,
        ),
        AssistantProviderPreset(
            id = "gemini",
            displayName = "Gemini",
            baseUrl = "https://generativelanguage.googleapis.com/v1beta/",
            protocol = AssistantProtocol.GEMINI_GENERATE_CONTENT,
            models = AssistantModelIds.gemini,
        ),
        AssistantProviderPreset(
            id = "grok",
            displayName = "Grok",
            baseUrl = "https://api.x.ai/v1/",
            protocol = AssistantProtocol.OPENAI_COMPATIBLE,
            models = AssistantModelIds.grok,
        ),
    )

    fun find(id: String?) = entries.firstOrNull { it.id == id }

    /** Exact normalized endpoints only; custom and unknown explicit IDs are never re-identified. */
    fun selection(settings: AssistantSettings): String? {
        settings.providerId?.let { return it }
        if (settings.baseUrl.isBlank()) return null
        val address = runCatching {
            normalizeAssistantBaseUrl(input = settings.baseUrl, protocol = settings.protocol)
        }.getOrNull() ?: return CUSTOM
        return entries.firstOrNull { it.baseUrl == address }?.id ?: when (address) {
            "https://api.deepseek.com/v1/" -> "deepseek"
            "https://generativelanguage.googleapis.com/v1beta/openai/" -> "gemini"
            else -> CUSTOM
        }
    }
}
