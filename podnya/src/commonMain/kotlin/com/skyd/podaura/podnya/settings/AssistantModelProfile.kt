package com.skyd.podaura.podnya.settings

import com.skyd.podaura.podnya.core.AssistantProtocol
import com.skyd.podaura.podnya.core.AssistantReasoningEffort
import com.skyd.podaura.podnya.core.assistantEndpointPath

enum class AssistantConfigurationReadError { NEWER_FORMAT, UNKNOWN_PROTOCOL, CORRUPT, STORAGE }

data class AssistantSettings(
    val configId: String? = null,
    val revision: Long = 0,
    val baseUrl: String = "",
    val modelId: String = "",
    val hasApiKey: Boolean = false,
    val name: String = "",
    val providerId: String? = null,
    val protocol: AssistantProtocol = AssistantProtocol.OPENAI_COMPATIBLE,
    val reasoningEffort: AssistantReasoningEffort = AssistantReasoningEffort.LOW,
    val readError: AssistantConfigurationReadError? = null,
) {
    val configured: Boolean get() = readError == null && baseUrl.isNotBlank() && modelId.isNotBlank() && hasApiKey
    val endpoint: String
        get() = if (baseUrl.isEmpty()) ""
        else baseUrl + assistantEndpointPath(
            baseUrl = baseUrl,
            modelId = modelId,
            protocol = protocol
        )
}

data class AssistantProfiles(
    val profiles: List<AssistantSettings> = emptyList(),
    val activeProfileId: String? = null,
    val readError: AssistantConfigurationReadError? = null,
) {
    val active: AssistantSettings
        get() = profiles.find { it.configId == activeProfileId }
            ?: AssistantSettings(readError = readError)

    /** Equal names are valid even for separate accounts at the same endpoint. */
    fun label(profile: AssistantSettings): String {
        val matches = profiles.filter { it.name == profile.name }
        if (matches.size < 2) return profile.name
        val index = matches.indexOfFirst { it.configId == profile.configId }
        return "${profile.name} (${index + 1}) · ${profile.baseUrl}"
    }
}
