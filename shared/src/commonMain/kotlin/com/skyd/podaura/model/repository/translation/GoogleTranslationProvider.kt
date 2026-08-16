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
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.cancel
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.delay
import kotlinx.io.readByteArray
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.coroutines.cancellation.CancellationException

class GoogleTranslationProvider(
    private val client: HttpClient,
    private val credentialStore: CredentialStore,
    private val log: Logger = Logger.withTag("GoogleTranslationProvider"),
    private val retryDelay: suspend (Long) -> Unit = { delay(it) },
) : TranslationProvider {
    override val type: TranslationProviderType = TranslationProviderType.Google

    override suspend fun getCapabilities(profile: TranslationProfile) = CAPABILITIES

    override suspend fun verify(
        profile: TranslationProfile,
        credential: String?,
    ): TranslationVerificationResult {
        return when (val response = translateHtml(
            profile = profile,
            request = HtmlTranslationRequest(
                html = "<p>PodAura</p><p>Hello</p>",
                targetLanguage = profile.targetLanguage,
            ),
            credential = credential,
        )) {
            is TranslationProviderResult.Failure ->
                TranslationVerificationResult.Failure(response.error)

            is TranslationProviderResult.Success -> {
                val body = runCatching {
                    Ksoup.parseBodyFragment(response.value.html).body()
                }.getOrNull()
                if (body?.select("p")?.size == 2 && body.text().isNotBlank()) {
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
    ): TranslationProviderResult = translateHtml(profile, request, credential = null)

    private suspend fun translateHtml(
        profile: TranslationProfile,
        request: HtmlTranslationRequest,
        credential: String?,
    ): TranslationProviderResult {
        if (!TranslationProfileValidator.isValid(profile) ||
            profile.providerType != TranslationProviderType.Google ||
            profile.config !is TranslationProviderConfig.Google
        ) {
            return failure(TranslationError.InvalidConfiguration)
        }
        val requestBody = request.toGoogleRequest()
        val actualBytes = REQUEST_JSON.encodeToString(requestBody)
            .encodeToByteArray()
            .size
            .toLong()
        if (actualBytes > MAX_TEXT_REQUEST_BYTES) {
            return failure(
                TranslationError.ContentTooLarge(actualBytes, MAX_TEXT_REQUEST_BYTES)
            )
        }
        val resolvedCredential = credential?.takeIf { it.isNotBlank() } ?: run {
            val credentialId = profile.credentialId
                ?: return failure(TranslationError.MissingCredential)
            try {
                credentialStore.get(credentialId)
            } catch (_: CredentialStorageException) {
                return failure(TranslationError.SecureStorageUnavailable)
            }?.takeIf { it.isNotBlank() }
                ?: return failure(TranslationError.MissingCredential)
        }

        var attempt = 0
        while (true) {
            val result = execute(resolvedCredential, profile, request, requestBody)
            val retryDelayMillis = when (result) {
                is GoogleCallResult.Retryable ->
                    result.retryAfterMillis ?: DEFAULT_RETRY_DELAY_MILLIS

                is GoogleCallResult.Finished -> return result.result
            }
            if (attempt++ >= MAX_RETRIES) return failure(result.error)
            retryDelay(retryDelayMillis.coerceIn(0, MAX_RETRY_DELAY_MILLIS))
        }
    }

    private suspend fun execute(
        credential: String,
        profile: TranslationProfile,
        request: HtmlTranslationRequest,
        requestBody: GoogleTranslateRequest,
    ): GoogleCallResult {
        return try {
            val response = client.post(ENDPOINT) {
                parameter("key", credential)
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                timeout {
                    requestTimeoutMillis = profile.requestTimeoutMillis
                    socketTimeoutMillis = profile.requestTimeoutMillis
                    connectTimeoutMillis = minOf(profile.requestTimeoutMillis, 20_000L)
                }
                setBody(
                    requestBody
                )
            }

            if (response.status == HttpStatusCode.OK) {
                return success(response, request)
            }
            val error = response.readError()
            when {
                error.isAuthenticationError() || response.status.value == 401 ->
                    finished(TranslationError.Authentication)

                response.status.value == 429 || error.isRateLimitError() -> retryable(
                    TranslationError.RateLimited(response.retryAfterMillis()),
                    response.retryAfterMillis(),
                )

                error.isQuotaError() -> finished(TranslationError.QuotaExceeded)
                response.status.value == 400 -> finished(TranslationError.UnsupportedLanguage)
                response.status.value == 403 -> finished(TranslationError.Authentication)
                response.status.value == 413 -> finished(
                    TranslationError.ContentTooLarge(
                        REQUEST_JSON.encodeToString(requestBody)
                            .encodeToByteArray()
                            .size
                            .toLong(),
                        MAX_TEXT_REQUEST_BYTES,
                    )
                )

                response.status.value in 500..599 -> retryable(
                    TranslationError.ServiceUnavailable,
                    response.retryAfterMillis(),
                )

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

    private suspend fun success(
        response: HttpResponse,
        request: HtmlTranslationRequest,
    ): GoogleCallResult {
        val body = response.readLimitedBody()
            ?: return finished(TranslationError.InvalidHtml)
        val decoded = runCatching {
            RESPONSE_JSON.decodeFromString<GoogleTranslateResponse>(body.decodeToString())
        }.getOrNull()
        if (decoded == null) {
            log.w {
                "Invalid translation response: reason=JsonDecodeFailed, " +
                        "status=${response.status.value}, responseBytes=${body.size}"
            }
        }
        val translation = decoded?.data?.translations?.singleOrNull()
        if (decoded != null && translation == null) {
            log.w {
                "Invalid translation response: reason=TranslationCount, " +
                        "status=${response.status.value}, responseBytes=${body.size}, " +
                        "translationCount=${decoded.data.translations.size}"
            }
        }
        translation ?: return finished(TranslationError.InvalidHtml)
        if (translation.translatedText.encodeToByteArray().size > MAX_RESPONSE_BYTES) {
            log.w {
                "Invalid translation response: reason=TranslatedHtmlTooLarge, " +
                        "status=${response.status.value}, " +
                        "translatedHtmlBytes=${translation.translatedText.encodeToByteArray().size}"
            }
            return finished(TranslationError.InvalidHtml)
        }
        return GoogleCallResult.Finished(
            TranslationProviderResult.Success(
                HtmlTranslationResult(
                    html = translation.translatedText,
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

    private suspend fun HttpResponse.readError(): GoogleError? {
        val body = readLimitedBody() ?: return null
        return runCatching {
            RESPONSE_JSON.decodeFromString<GoogleErrorResponse>(body.decodeToString()).error
        }.getOrNull()
    }

    private suspend fun HttpResponse.readLimitedBody(): ByteArray? {
        val channel = bodyAsChannel()
        val body = channel.readRemaining(MAX_RESPONSE_BYTES.toLong() + 1).readByteArray()
        if (body.size <= MAX_RESPONSE_BYTES) return body
        channel.cancel()
        log.w {
            "Invalid translation response: reason=ResponseTooLarge, " +
                    "status=${status.value}, responseBytes=${body.size}"
        }
        return null
    }

    private fun GoogleError?.isAuthenticationError(): Boolean {
        if (this == null) return false
        if (message?.contains("API key not valid", ignoreCase = true) == true) return true
        return normalizedReasons.any {
            it in setOf("apikeyinvalid", "keyinvalid", "accessnotconfigured")
        }
    }

    private fun GoogleError?.isRateLimitError(): Boolean {
        if (this == null) return false
        return normalizedReasons.any { it.contains("ratelimit") }
    }

    private fun GoogleError?.isQuotaError(): Boolean {
        if (this == null) return false
        if (status.equals("RESOURCE_EXHAUSTED", ignoreCase = true)) return true
        return normalizedReasons.any {
            it.contains("quota") || it.contains("dailylimit")
        }
    }

    private val GoogleError.normalizedReasons: List<String>
        get() = reasons.map { reason -> reason.filter(Char::isLetterOrDigit).lowercase() }

    private val GoogleError.reasons: List<String>
        get() = errors.mapNotNull(GoogleErrorItem::reason) +
                details.mapNotNull(GoogleErrorDetail::reason)

    private fun HttpResponse.retryAfterMillis(): Long? =
        headers[HttpHeaders.RetryAfter]?.trim()?.toLongOrNull()?.times(1_000L)

    private fun String.toGoogleLanguageCode(): String {
        val parts = split('-')
        return parts.mapIndexed { index, part ->
            if (index == 0) part.lowercase() else part.uppercase()
        }.joinToString("-")
    }

    private fun HtmlTranslationRequest.toGoogleRequest() = GoogleTranslateRequest(
        q = html,
        target = targetLanguage.toGoogleLanguageCode(),
        source = sourceLanguage
            ?.takeIf { it.isNotBlank() }
            ?.toGoogleLanguageCode(),
        format = "html",
    )

    private fun failure(error: TranslationError) = TranslationProviderResult.Failure(error)

    private fun finished(error: TranslationError) =
        GoogleCallResult.Finished(failure(error))

    private fun retryable(error: TranslationError, retryAfterMillis: Long?) =
        GoogleCallResult.Retryable(error, retryAfterMillis)

    private sealed interface GoogleCallResult {
        data class Finished(val result: TranslationProviderResult) : GoogleCallResult
        data class Retryable(
            val error: TranslationError,
            val retryAfterMillis: Long?,
        ) : GoogleCallResult
    }

    @Serializable
    private data class GoogleTranslateRequest(
        val q: String,
        val target: String,
        val source: String? = null,
        val format: String,
    )

    @Serializable
    private data class GoogleTranslateResponse(val data: GoogleTranslateResponseData)

    @Serializable
    private data class GoogleTranslateResponseData(
        val translations: List<GoogleTranslation>,
    )

    @Serializable
    private data class GoogleTranslation(
        val translatedText: String,
        val detectedSourceLanguage: String? = null,
    )

    @Serializable
    private data class GoogleErrorResponse(val error: GoogleError)

    @Serializable
    private data class GoogleError(
        val message: String? = null,
        val status: String? = null,
        val errors: List<GoogleErrorItem> = emptyList(),
        val details: List<GoogleErrorDetail> = emptyList(),
    )

    @Serializable
    private data class GoogleErrorItem(val reason: String? = null)

    @Serializable
    private data class GoogleErrorDetail(val reason: String? = null)

    companion object {
        const val ENDPOINT = "https://translation.googleapis.com/language/translate/v2"
        const val MAX_TEXT_REQUEST_BYTES = 100_000L
        const val MAX_RESPONSE_BYTES = 2 * 1024 * 1024
        private const val MAX_RETRIES = 1
        private const val DEFAULT_RETRY_DELAY_MILLIS = 1_000L
        private const val MAX_RETRY_DELAY_MILLIS = 30_000L
        private val REQUEST_JSON = Json { explicitNulls = false }
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
