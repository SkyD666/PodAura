package com.skyd.podaura.model.bean.translation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class TranslationProviderType {
    @SerialName("deepl")
    DeepL,

    @SerialName("google")
    Google,

    @SerialName("azure")
    Azure,

    @SerialName("custom_podaura")
    CustomPodAura,

    @SerialName("chat_completions")
    ChatCompletionsCompatible,
}

@Serializable
data class TranslationHeader(
    val name: String,
    val value: String,
    val sensitive: Boolean = false,
)

@Serializable
sealed interface TranslationProviderConfig {
    val schemaVersion: Int

    @Serializable
    @SerialName("deepl")
    data class DeepL(
        override val schemaVersion: Int = 1,
        val useFreeEndpoint: Boolean = true,
    ) : TranslationProviderConfig

    @Serializable
    @SerialName("google")
    data class Google(
        override val schemaVersion: Int = 1,
    ) : TranslationProviderConfig

    @Serializable
    @SerialName("azure")
    data class Azure(
        override val schemaVersion: Int = 1,
        val region: String? = null,
    ) : TranslationProviderConfig

    @Serializable
    @SerialName("custom_podaura")
    data class CustomPodAura(
        override val schemaVersion: Int = 1,
    ) : TranslationProviderConfig

    @Serializable
    @SerialName("chat_completions")
    data class ChatCompletions(
        override val schemaVersion: Int = 1,
        val model: String,
        val temperature: Float = 0f,
    ) : TranslationProviderConfig
}

@Serializable
data class TranslationProfile(
    val id: String,
    val name: String,
    val providerType: TranslationProviderType,
    val endpoint: String? = null,
    val credentialId: String? = null,
    val customHeaders: List<TranslationHeader> = emptyList(),
    val requestTimeoutMillis: Long = 60_000,
    val enabled: Boolean = true,
    val isDefault: Boolean = false,
    val targetLanguage: String = "EN",
    val config: TranslationProviderConfig,
)

data class TranslationCapabilities(
    val supportsHtml: Boolean,
    val supportsDocumentTranslation: Boolean,
    val supportsLanguageDetection: Boolean,
    val maxTextRequestBytes: Long?,
    val maxDocumentBytes: Long?,
    val supportedLanguages: Set<String>?,
)

data class HtmlTranslationRequest(
    val html: String,
    val sourceLanguage: String? = null,
    val targetLanguage: String,
)

data class HtmlTranslationResult(
    val html: String,
    val detectedSourceLanguage: String?,
    val usage: TranslationUsage?,
)

data class TranslationUsage(
    val inputCharacters: Long?,
    val inputTokens: Long?,
    val outputTokens: Long?,
)

sealed interface TranslationVerificationResult {
    data class Success(val capabilities: TranslationCapabilities) : TranslationVerificationResult
    data class Failure(val error: TranslationError) : TranslationVerificationResult
}

sealed interface TranslationError {
    data object Authentication : TranslationError
    data object UnsupportedLanguage : TranslationError
    data object QuotaExceeded : TranslationError
    data class RateLimited(val retryAfterMillis: Long?) : TranslationError
    data class ContentTooLarge(val actualBytes: Long, val limitBytes: Long?) : TranslationError
    data object NetworkUnavailable : TranslationError
    data object Timeout : TranslationError
    data object ContentRejected : TranslationError
    data object InvalidHtml : TranslationError
    data object HtmlNotSupported : TranslationError
    data object MissingCredential : TranslationError
    data object SecureStorageUnavailable : TranslationError
    data object InvalidConfiguration : TranslationError
    data object ServiceUnavailable : TranslationError
}

sealed interface TranslationProviderResult {
    data class Success(val value: HtmlTranslationResult) : TranslationProviderResult
    data class Failure(val error: TranslationError) : TranslationProviderResult
}

enum class TranslationContentSource {
    Feed,
    FullText,
}

data class ArticleTranslation(
    val title: String,
    val html: String,
    val detectedSourceLanguage: String?,
    val fromCache: Boolean,
)

sealed interface ArticleTranslationResult {
    data class Success(val translation: ArticleTranslation) : ArticleTranslationResult
    data class Failure(val error: TranslationError) : ArticleTranslationResult
}
