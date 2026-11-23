package com.skyd.podaura.nano.core

import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.ktor.http.encodeURLPathPart

/** Stable wire identifiers, independent of the settings provider catalogue. */
enum class AssistantProtocol(val id: String) {
    OPENAI_COMPATIBLE("openai-compatible"),
    ANTHROPIC_MESSAGES("anthropic-messages"),
    GEMINI_GENERATE_CONTENT("gemini-generate-content");

    companion object {
        fun fromId(id: String): AssistantProtocol? = entries.firstOrNull { it.id == id }
    }
}

/** Explicit official model requirements only; gateways retain Chat Completions semantics. */
fun assistantEndpointPath(
    baseUrl: String,
    modelId: String,
    protocol: AssistantProtocol = AssistantProtocol.OPENAI_COMPATIBLE,
): String {
    when (protocol) {
        AssistantProtocol.ANTHROPIC_MESSAGES -> return "messages"
        AssistantProtocol.GEMINI_GENERATE_CONTENT -> return "models/${
            modelId.trim().encodeURLPathPart()
        }:generateContent"

        AssistantProtocol.OPENAI_COMPATIBLE -> Unit
    }
    return if (supportsAssistantReasoningEffort(
            baseUrl = baseUrl,
            modelId = modelId,
            protocol = protocol
        )
    ) {
        "responses"
    } else {
        "chat/completions"
    }
}

/** Shared by routing, settings and the immutable connection; never infer support from a brand. */
fun supportsAssistantReasoningEffort(
    baseUrl: String,
    modelId: String,
    protocol: AssistantProtocol = AssistantProtocol.OPENAI_COMPATIBLE,
): Boolean = AssistantModelCatalog.policy(baseUrl = baseUrl, modelId = modelId, protocol = protocol)
    ?.supportsReasoningEffort == true

/** API prefixes belong to the configured address; never invent a version prefix. */
fun normalizeAssistantBaseUrl(
    input: String,
    protocol: AssistantProtocol = AssistantProtocol.OPENAI_COMPATIBLE,
): String {
    val raw = input.trim()
    val url = try {
        Url(raw)
    } catch (_: Exception) {
        throw AssistantException(AssistantError.INVALID_ADDRESS)
    }
    if (raw.length > 2048 || raw.any(Char::isISOControl) ||
        !raw.startsWith("https://", ignoreCase = true) || url.protocol != URLProtocol.HTTPS ||
        url.host.isBlank() || url.user != null || url.password != null ||
        raw.contains('?') || raw.contains('#') || raw.contains('\\')
    ) throw AssistantException(AssistantError.INVALID_ADDRESS)
    var normalized = url.toString().trimEnd('/')
    normalized = when (protocol) {
        AssistantProtocol.OPENAI_COMPATIBLE -> normalized.removeSuffix("/chat/completions")
        AssistantProtocol.ANTHROPIC_MESSAGES -> normalized.removeSuffix("/messages")
        AssistantProtocol.GEMINI_GENERATE_CONTENT -> normalized.replace(
            Regex("/models/[^/]+:(?:streamGenerateContent|generateContent)$"),
            "",
        )
    }
    return "$normalized/"
}
