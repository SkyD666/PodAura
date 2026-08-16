package com.skyd.podaura.model.repository.translation

import co.touchlab.kermit.Logger
import com.fleeksoft.ksoup.Ksoup
import com.skyd.podaura.model.bean.translation.HtmlTranslationRequest
import com.skyd.podaura.model.bean.translation.HtmlTranslationResult
import com.skyd.podaura.model.bean.translation.TranslationCapabilities
import com.skyd.podaura.model.bean.translation.TranslationError
import com.skyd.podaura.model.bean.translation.TranslationProfile
import com.skyd.podaura.model.bean.translation.TranslationProviderConfig
import com.skyd.podaura.model.bean.translation.TranslationProviderResult
import com.skyd.podaura.model.bean.translation.TranslationProviderType
import com.skyd.podaura.model.bean.translation.TranslationUsage
import com.skyd.podaura.model.bean.translation.TranslationVerificationResult
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.timeout
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.parameters
import io.ktor.utils.io.cancel
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.delay
import kotlinx.io.readByteArray
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.coroutines.cancellation.CancellationException

class DeepLTranslationProvider(
    private val client: HttpClient,
    private val credentialStore: CredentialStore,
    private val log: Logger = Logger.withTag("DeepLTranslationProvider"),
    private val retryDelay: suspend (Long) -> Unit = { delay(it) },
) : TranslationProvider {
    override val type: TranslationProviderType = TranslationProviderType.DeepL

    override suspend fun getCapabilities(profile: TranslationProfile) = CAPABILITIES

    override suspend fun verify(
        profile: TranslationProfile,
        credential: String?,
    ): TranslationVerificationResult {
        val response = translateHtml(
            profile = profile,
            request = HtmlTranslationRequest(
                html = "<p translate=\"no\">PodAura</p><p>Hello</p>",
                targetLanguage = profile.targetLanguage,
            ),
            credential = credential,
        )
        return when (response) {
            is TranslationProviderResult.Failure -> TranslationVerificationResult.Failure(response.error)
            is TranslationProviderResult.Success -> {
                val body =
                    runCatching { Ksoup.parseBodyFragment(response.value.html).body() }.getOrNull()
                if (body?.select("p")?.size == 2 &&
                    body.select("p").first()?.text() == "PodAura" &&
                    body.text().isNotBlank()
                ) {
                    TranslationVerificationResult.Success(CAPABILITIES)
                } else {
                    TranslationVerificationResult.Failure(TranslationError.InvalidHtml)
                }
            }
        }
    }

    override suspend fun translateHtml(
        profile: TranslationProfile,
        request: HtmlTranslationRequest,
    ): TranslationProviderResult = translateHtml(
        profile = profile,
        request = request,
        credential = null,
    )

    private suspend fun translateHtml(
        profile: TranslationProfile,
        request: HtmlTranslationRequest,
        credential: String?,
    ): TranslationProviderResult {
        if (!TranslationProfileValidator.isValid(profile) ||
            profile.providerType != TranslationProviderType.DeepL
        ) {
            return TranslationProviderResult.Failure(TranslationError.InvalidConfiguration)
        }
        val actualBytes = request.toFormDataContent().contentLength
        val maximumBytes = CAPABILITIES.maxTextRequestBytes
        if (maximumBytes != null && actualBytes > maximumBytes) {
            return TranslationProviderResult.Failure(
                TranslationError.ContentTooLarge(actualBytes, maximumBytes)
            )
        }
        val resolvedCredential = credential?.takeIf { it.isNotBlank() } ?: run {
            val credentialId = profile.credentialId
                ?: return TranslationProviderResult.Failure(TranslationError.MissingCredential)
            try {
                credentialStore.get(credentialId)
            } catch (_: CredentialStorageException) {
                return TranslationProviderResult.Failure(
                    TranslationError.SecureStorageUnavailable
                )
            }?.takeIf { it.isNotBlank() }
                ?: return TranslationProviderResult.Failure(TranslationError.MissingCredential)
        }

        val config = profile.config as? TranslationProviderConfig.DeepL
            ?: return TranslationProviderResult.Failure(TranslationError.InvalidConfiguration)
        val endpoint = if (config.useFreeEndpoint) FREE_ENDPOINT else PRO_ENDPOINT

        var attempt = 0
        while (true) {
            val result = execute(endpoint, resolvedCredential, profile, request)
            val retryDelayMillis = when (result) {
                is DeepLCallResult.Retryable -> result.retryAfterMillis
                    ?: DEFAULT_RETRY_DELAY_MILLIS

                is DeepLCallResult.Finished -> return result.result
            }
            if (attempt++ >= MAX_RETRIES) {
                return TranslationProviderResult.Failure(result.error)
            }
            retryDelay(retryDelayMillis.coerceIn(0, MAX_RETRY_DELAY_MILLIS))
        }
    }

    private suspend fun execute(
        endpoint: String,
        credential: String,
        profile: TranslationProfile,
        request: HtmlTranslationRequest,
    ): DeepLCallResult {
        return try {
            val response = client.post(endpoint) {
                header(HttpHeaders.Authorization, "DeepL-Auth-Key $credential")
                timeout {
                    requestTimeoutMillis = profile.requestTimeoutMillis
                    socketTimeoutMillis = profile.requestTimeoutMillis
                    connectTimeoutMillis = minOf(profile.requestTimeoutMillis, 20_000L)
                }
                setBody(
                    request.toFormDataContent()
                )
            }

            when {
                response.status == HttpStatusCode.OK -> {
                    val channel = response.bodyAsChannel()
                    val body =
                        channel.readRemaining(MAX_RESPONSE_BYTES.toLong() + 1).readByteArray()
                    if (body.size > MAX_RESPONSE_BYTES) {
                        channel.cancel()
                        log.w {
                            "Invalid translation response: reason=ResponseTooLarge, " +
                                    "status=${response.status.value}, responseBytes=${body.size}"
                        }
                        return DeepLCallResult.Finished(
                            TranslationProviderResult.Failure(TranslationError.InvalidHtml)
                        )
                    }
                    val decoded = runCatching {
                        RESPONSE_JSON.decodeFromString<DeepLResponse>(body.decodeToString())
                    }.getOrNull()
                    if (decoded == null) {
                        log.w {
                            "Invalid translation response: reason=JsonDecodeFailed, " +
                                    "status=${response.status.value}, responseBytes=${body.size}"
                        }
                    }
                    val result = decoded?.translations?.singleOrNull()
                    if (decoded != null && result == null) {
                        log.w {
                            "Invalid translation response: reason=TranslationCount, " +
                                    "status=${response.status.value}, responseBytes=${body.size}, " +
                                    "translationCount=${decoded.translations.size}"
                        }
                    }
                    val translation = result
                        ?: return DeepLCallResult.Finished(
                            TranslationProviderResult.Failure(TranslationError.InvalidHtml)
                        )
                    if (translation.text.encodeToByteArray().size > MAX_RESPONSE_BYTES) {
                        log.w {
                            "Invalid translation response: reason=TranslatedHtmlTooLarge, " +
                                    "status=${response.status.value}, " +
                                    "translatedHtmlBytes=${translation.text.encodeToByteArray().size}"
                        }
                        DeepLCallResult.Finished(
                            TranslationProviderResult.Failure(TranslationError.InvalidHtml)
                        )
                    } else {
                        DeepLCallResult.Finished(
                            TranslationProviderResult.Success(
                                HtmlTranslationResult(
                                    html = translation.text,
                                    detectedSourceLanguage = translation.detectedSourceLanguage,
                                    usage = TranslationUsage(
                                        inputCharacters = request.html.length.toLong(),
                                        inputTokens = null,
                                        outputTokens = null,
                                    ),
                                )
                            )
                        )
                    }
                }

                response.status.value == 401 || response.status.value == 403 -> finished(
                    TranslationError.Authentication
                )

                response.status.value == 400 -> finished(TranslationError.UnsupportedLanguage)
                response.status.value == 413 -> finished(
                    TranslationError.ContentTooLarge(
                        request.toFormDataContent().contentLength,
                        CAPABILITIES.maxTextRequestBytes,
                    )
                )

                response.status.value == 429 -> retryable(
                    TranslationError.RateLimited(response.retryAfterMillis()),
                    response.retryAfterMillis(),
                )

                response.status.value == 456 -> finished(TranslationError.QuotaExceeded)
                response.status.value == 503 -> retryable(
                    TranslationError.ServiceUnavailable,
                    response.retryAfterMillis()
                )

                response.status.value == 422 -> finished(TranslationError.ContentRejected)
                else -> finished(TranslationError.ServiceUnavailable)
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: HttpRequestTimeoutException) {
            finished(TranslationError.Timeout)
        } catch (_: Throwable) {
            retryable(TranslationError.NetworkUnavailable, null)
        }
    }

    private fun io.ktor.client.statement.HttpResponse.retryAfterMillis(): Long? =
        headers[HttpHeaders.RetryAfter]?.trim()?.toLongOrNull()?.times(1_000L)

    private fun HtmlTranslationRequest.toFormDataContent() = FormDataContent(
        parameters {
            append("text", html)
            append("target_lang", targetLanguage.uppercase())
            sourceLanguage?.takeIf { it.isNotBlank() }?.let {
                append("source_lang", it.uppercase())
            }
            append("tag_handling", "html")
            append("tag_handling_version", "v2")
            append("preserve_formatting", "true")
        }
    )

    private fun finished(error: TranslationError) =
        DeepLCallResult.Finished(TranslationProviderResult.Failure(error))

    private fun retryable(error: TranslationError, retryAfterMillis: Long?) =
        DeepLCallResult.Retryable(error, retryAfterMillis)

    private sealed interface DeepLCallResult {
        data class Finished(val result: TranslationProviderResult) : DeepLCallResult
        data class Retryable(
            val error: TranslationError,
            val retryAfterMillis: Long?,
        ) : DeepLCallResult
    }

    @Serializable
    private data class DeepLResponse(val translations: List<DeepLTranslation>)

    @Serializable
    private data class DeepLTranslation(
        @SerialName("detected_source_language")
        val detectedSourceLanguage: String? = null,
        val text: String,
    )

    companion object {
        const val FREE_ENDPOINT = "https://api-free.deepl.com/v2/translate"
        const val PRO_ENDPOINT = "https://api.deepl.com/v2/translate"
        const val MAX_TEXT_REQUEST_BYTES = 128L * 1024L
        const val MAX_RESPONSE_BYTES = 2 * 1024 * 1024
        private const val MAX_RETRIES = 1
        private const val DEFAULT_RETRY_DELAY_MILLIS = 1_000L
        private const val MAX_RETRY_DELAY_MILLIS = 30_000L
        private val RESPONSE_JSON = Json { ignoreUnknownKeys = true }

        val CAPABILITIES = TranslationCapabilities(
            supportsHtml = true,
            supportsDocumentTranslation = false,
            supportsLanguageDetection = true,
            maxTextRequestBytes = MAX_TEXT_REQUEST_BYTES,
            maxDocumentBytes = null,
            supportedLanguages = null,
        )
    }
}
