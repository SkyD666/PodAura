package com.skyd.podaura.model.repository.translation

import com.skyd.podaura.model.bean.translation.TranslationProfile
import com.skyd.podaura.model.bean.translation.TranslationProviderConfig
import com.skyd.podaura.model.bean.translation.TranslationProviderType
import io.ktor.http.URLProtocol
import io.ktor.http.Url

object TranslationProfileValidator {
    private val headerName = Regex("^[!#$%&'*+.^_`|~0-9A-Za-z-]+$")
    private val forbiddenHeaders = setOf(
        "authorization",
        "connection",
        "content-length",
        "cookie",
        "host",
        "proxy-authorization",
        "set-cookie",
        "transfer-encoding",
    )

    fun isValid(profile: TranslationProfile, allowLocalHttp: Boolean = false): Boolean {
        if (profile.id.isBlank() || profile.name.isBlank()) return false
        if (profile.requestTimeoutMillis !in 5_000L..120_000L) return false
        if (profile.targetLanguage.isBlank()) return false
        if (profile.isDefault && !profile.enabled) return false
        val matchingConfig = when (profile.providerType) {
            TranslationProviderType.DeepL -> profile.config is TranslationProviderConfig.DeepL
            TranslationProviderType.Google -> profile.config is TranslationProviderConfig.Google
            TranslationProviderType.Azure -> profile.config is TranslationProviderConfig.Azure
            TranslationProviderType.CustomPodAura -> profile.config is TranslationProviderConfig.CustomPodAura
            TranslationProviderType.ChatCompletionsCompatible ->
                profile.config is TranslationProviderConfig.ChatCompletions
        }
        if (!matchingConfig) return false
        if (profile.providerType in setOf(
                TranslationProviderType.DeepL,
                TranslationProviderType.Google,
            ) && profile.endpoint != null
        ) return false
        if (profile.customHeaders.any { header ->
                header.sensitive ||
                        !headerName.matches(header.name) ||
                        header.name.lowercase() in forbiddenHeaders ||
                        header.value.contains('\r') ||
                        header.value.contains('\n')
            }
        ) return false
        val endpoint = profile.endpoint ?: return true
        val url = runCatching { Url(endpoint) }.getOrNull() ?: return false
        if (url.user != null || url.password != null) return false
        if (url.protocol == URLProtocol.HTTPS) return true
        return allowLocalHttp && url.protocol == URLProtocol.HTTP &&
                url.host.lowercase() in setOf("127.0.0.1", "::1", "localhost")
    }
}
