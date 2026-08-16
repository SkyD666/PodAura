package com.skyd.podaura.model.repository.translation

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.StaticConfig
import com.skyd.podaura.model.bean.translation.HtmlTranslationRequest
import com.skyd.podaura.model.bean.translation.TranslationError
import com.skyd.podaura.model.bean.translation.TranslationProfile
import com.skyd.podaura.model.bean.translation.TranslationProviderConfig
import com.skyd.podaura.model.bean.translation.TranslationProviderResult
import com.skyd.podaura.model.bean.translation.TranslationProviderType
import com.skyd.podaura.model.bean.translation.TranslationVerificationResult
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import io.ktor.http.parseQueryString
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DeepLTranslationProviderTest {
    private val json = Json { ignoreUnknownKeys = true }
    private val credentials = MemoryCredentialStore(mutableMapOf("credential" to "test-api-key"))
    private val profile = TranslationProfile(
        id = "profile",
        name = "DeepL Free",
        providerType = TranslationProviderType.DeepL,
        credentialId = "credential",
        targetLanguage = "ZH",
        config = TranslationProviderConfig.DeepL(useFreeEndpoint = true),
    )

    @Test
    fun verificationUsesTheDraftCredentialLanguageAndEndpoint() = runTest {
        var requestCount = 0
        val client = client(MockEngine { request ->
            requestCount++
            assertEquals(DeepLTranslationProvider.PRO_ENDPOINT, request.url.toString())
            assertEquals("DeepL-Auth-Key draft-api-key", request.headers[HttpHeaders.Authorization])
            val body = (request.body as OutgoingContent.ByteArrayContent).bytes().decodeToString()
            assertEquals("DE", parseQueryString(body)["target_lang"])
            respondJson("<p>PodAura</p><p>Hallo</p>")
        })
        val draft = profile.copy(
            targetLanguage = "DE",
            config = TranslationProviderConfig.DeepL(useFreeEndpoint = false),
        )

        val result = DeepLTranslationProvider(client, credentials).verify(
            profile = draft,
            credential = "draft-api-key",
        )

        assertIs<TranslationVerificationResult.Success>(result)
        assertEquals(1, requestCount)
        client.close()
    }

    @Test
    fun sendsExactlyOneCompleteHtmlDocumentWithProtectedAuthAndParameters() = runTest {
        var requestCount = 0
        val source = TranslationDocumentBuilder().build(
            "Title",
            "<p>Hello <strong>world</strong>.</p><img src='cover.jpg'>",
        ).html
        val client = client(MockEngine { request ->
            requestCount++
            assertEquals(DeepLTranslationProvider.FREE_ENDPOINT, request.url.toString())
            assertEquals("DeepL-Auth-Key test-api-key", request.headers[HttpHeaders.Authorization])
            val body = (request.body as OutgoingContent.ByteArrayContent).bytes().decodeToString()
            val parameters = parseQueryString(body)
            assertEquals(listOf(source), parameters.getAll("text"))
            assertEquals("html", parameters["tag_handling"])
            assertEquals("v2", parameters["tag_handling_version"])
            assertEquals("true", parameters["preserve_formatting"])
            assertEquals("ZH", parameters["target_lang"])
            respondJson(source)
        })

        val result = DeepLTranslationProvider(client, credentials).translateHtml(
            profile,
            HtmlTranslationRequest(source, targetLanguage = "zh"),
        )

        assertIs<TranslationProviderResult.Success>(result)
        assertEquals(1, requestCount)
        client.close()
    }

    @Test
    fun retries429OnceAndHonorsRetryAfter() = runTest {
        var requestCount = 0
        val delays = mutableListOf<Long>()
        val client = client(MockEngine {
            requestCount++
            if (requestCount == 1) {
                respond(
                    content = "rate limited",
                    status = HttpStatusCode.TooManyRequests,
                    headers = headersOf(HttpHeaders.RetryAfter, "2"),
                )
            } else {
                respondJson("<p>Bonjour</p>")
            }
        })

        val result = DeepLTranslationProvider(client, credentials) { delays += it }
            .translateHtml(profile, HtmlTranslationRequest("<p>Hello</p>", targetLanguage = "FR"))

        assertIs<TranslationProviderResult.Success>(result)
        assertEquals(2, requestCount)
        assertEquals(listOf(2_000L), delays)
        client.close()
    }

    @Test
    fun retries503OnceAndMapsTimeoutWithoutRetrying() = runTest {
        var serviceRequests = 0
        val delays = mutableListOf<Long>()
        val serviceClient = client(MockEngine {
            serviceRequests++
            if (serviceRequests == 1) {
                respond("unavailable", HttpStatusCode.ServiceUnavailable)
            } else {
                respondJson("<p>Bonjour</p>")
            }
        })
        val serviceResult = DeepLTranslationProvider(serviceClient, credentials) { delays += it }
            .translateHtml(profile, HtmlTranslationRequest("<p>Hello</p>", targetLanguage = "FR"))

        assertIs<TranslationProviderResult.Success>(serviceResult)
        assertEquals(2, serviceRequests)
        assertEquals(listOf(1_000L), delays)
        serviceClient.close()

        var timeoutRequests = 0
        val timeoutClient = client(MockEngine { request ->
            timeoutRequests++
            throw HttpRequestTimeoutException(request)
        })
        val timeoutResult = DeepLTranslationProvider(timeoutClient, credentials).translateHtml(
            profile,
            HtmlTranslationRequest("<p>Hello</p>", targetLanguage = "FR"),
        )

        assertEquals(
            TranslationError.Timeout,
            assertIs<TranslationProviderResult.Failure>(timeoutResult).error,
        )
        assertEquals(1, timeoutRequests)
        timeoutClient.close()
    }

    @Test
    fun rejectsUrlEncodedBodyThatExceedsLimitBeforeNetwork() = runTest {
        var requestCount = 0
        val client = client(MockEngine {
            requestCount++
            error("An oversized encoded body must not be requested")
        })
        val html = "<".repeat(50_000)

        val result = DeepLTranslationProvider(client, credentials).translateHtml(
            profile,
            HtmlTranslationRequest(html, targetLanguage = "FR"),
        )

        assertIs<TranslationError.ContentTooLarge>(
            assertIs<TranslationProviderResult.Failure>(result).error
        )
        assertEquals(0, requestCount)
        client.close()
    }

    @Test
    fun mapsAuthenticationQuotaAndInvalidResponsesWithoutSensitiveMessages() = runTest {
        suspend fun status(status: HttpStatusCode): TranslationProviderResult {
            val client = client(MockEngine { respond("secret upstream body", status) })
            return DeepLTranslationProvider(client, credentials).translateHtml(
                profile,
                HtmlTranslationRequest("<p>Hello</p>", targetLanguage = "FR"),
            ).also { client.close() }
        }

        val authenticationFailure = status(HttpStatusCode.Unauthorized)
        assertEquals(
            TranslationError.Authentication,
            assertIs<TranslationProviderResult.Failure>(authenticationFailure).error,
        )
        assertFalse(authenticationFailure.toString().contains("test-api-key"))
        assertFalse(authenticationFailure.toString().contains("secret upstream body"))
        assertEquals(
            TranslationError.QuotaExceeded,
            assertIs<TranslationProviderResult.Failure>(status(HttpStatusCode(456, "Quota"))).error,
        )
        val logMessages = mutableListOf<String>()
        val logger = Logger(
            config = StaticConfig(
                logWriterList = listOf(object : LogWriter() {
                    override fun log(
                        severity: Severity,
                        message: String,
                        tag: String,
                        throwable: Throwable?,
                    ) {
                        logMessages += message
                    }
                })
            ),
            tag = "DeepLTranslationProviderTest",
        )
        val invalidClient = client(MockEngine {
            respond(
                content = "secret upstream body",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        })
        val invalid = DeepLTranslationProvider(
            client = invalidClient,
            credentialStore = credentials,
            log = logger,
        ).translateHtml(
            profile,
            HtmlTranslationRequest("<p>Hello</p>", targetLanguage = "FR"),
        )
        assertEquals(
            TranslationError.InvalidHtml,
            assertIs<TranslationProviderResult.Failure>(invalid).error,
        )
        assertTrue(logMessages.any { it.contains("reason=JsonDecodeFailed") })
        assertFalse(logMessages.any { it.contains("test-api-key") })
        assertFalse(logMessages.any { it.contains("secret upstream body") })
        invalidClient.close()
    }

    @Test
    fun rejectsOversizedDocumentWithoutNetworkOrDomChunking() = runTest {
        var requestCount = 0
        val client = client(MockEngine {
            requestCount++
            error("Oversized HTML must not be requested")
        })
        val html = "x".repeat(DeepLTranslationProvider.MAX_TEXT_REQUEST_BYTES.toInt() + 1)

        val result = DeepLTranslationProvider(client, credentials).translateHtml(
            profile,
            HtmlTranslationRequest(html, targetLanguage = "FR"),
        )

        assertIs<TranslationError.ContentTooLarge>(
            assertIs<TranslationProviderResult.Failure>(result).error
        )
        assertEquals(0, requestCount)
        client.close()
    }

    @Test
    fun cancellationImmediatelyCancelsTheSingleRequest() = runTest {
        val started = CompletableDeferred<Unit>()
        val client = client(MockEngine {
            started.complete(Unit)
            awaitCancellation()
        })
        val provider = DeepLTranslationProvider(client, credentials)
        val job = launch {
            provider.translateHtml(
                profile,
                HtmlTranslationRequest("<p>Hello</p>", targetLanguage = "FR"),
            )
        }
        started.await()

        job.cancel()
        job.join()

        assertTrue(job.isCancelled)
        client.close()
    }

    @Test
    fun rejectsResponseLargerThanTheHardLimit() = runTest {
        val oversized = "x".repeat(DeepLTranslationProvider.MAX_RESPONSE_BYTES)
        val client = client(MockEngine { respondJson(oversized) })

        val result = DeepLTranslationProvider(client, credentials).translateHtml(
            profile,
            HtmlTranslationRequest("<p>Hello</p>", targetLanguage = "FR"),
        )

        assertEquals(
            TranslationError.InvalidHtml,
            assertIs<TranslationProviderResult.Failure>(result).error,
        )
        client.close()
    }

    private fun client(engine: MockEngine) = HttpClient(engine) {
        install(ContentNegotiation) { json(json) }
    }

    private fun io.ktor.client.engine.mock.MockRequestHandleScope.respondJson(
        first: String,
        second: String? = null,
    ) = respond(
        content = buildString {
            append("{\"translations\":[{\"detected_source_language\":\"EN\",\"text\":")
            append(json.encodeToString(first))
            append('}')
            second?.let {
                append(",{\"detected_source_language\":\"EN\",\"text\":")
                append(json.encodeToString(it))
                append('}')
            }
            append("]}")
        },
        status = HttpStatusCode.OK,
        headers = headersOf(HttpHeaders.ContentType, "application/json"),
    )

    private class MemoryCredentialStore(
        private val values: MutableMap<String, String> = mutableMapOf(),
    ) : CredentialStore {
        override suspend fun put(id: String, secret: String) {
            values[id] = secret
        }

        override suspend fun get(id: String): String? = values[id]

        override suspend fun delete(id: String) {
            values.remove(id)
        }
    }
}
