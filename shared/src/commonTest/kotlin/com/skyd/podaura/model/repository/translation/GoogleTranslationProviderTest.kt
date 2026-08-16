package com.skyd.podaura.model.repository.translation

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
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class GoogleTranslationProviderTest {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }
    private val credentials = MemoryCredentialStore(
        mutableMapOf("credential" to "test-api-key")
    )
    private val profile = TranslationProfile(
        id = "profile",
        name = "Google",
        providerType = TranslationProviderType.Google,
        credentialId = "credential",
        targetLanguage = "ZH-TW",
        config = TranslationProviderConfig.Google(),
    )

    @Test
    fun sendsCompleteHtmlWithApiKeyAndGoogleLanguageCodes() = runTest {
        val source = TranslationDocumentBuilder().build(
            title = "Title",
            contentHtml = "<p>Hello &amp; welcome.</p>",
        ).html
        var requestedUrl: String? = null
        var requestedApiKey: String? = null
        var requestedBody: String? = null
        val client = client(MockEngine { request ->
            requestedUrl = request.url.toString().substringBefore('?')
            requestedApiKey = request.url.parameters["key"]
            requestedBody =
                (request.body as OutgoingContent.ByteArrayContent).bytes().decodeToString()
            respondJson(source, detectedSourceLanguage = null)
        })

        val result = GoogleTranslationProvider(client, credentials).translateHtml(
            profile = profile,
            request = HtmlTranslationRequest(
                html = source,
                sourceLanguage = "EN",
                targetLanguage = "ZH-TW",
            ),
        )

        assertEquals(source, assertIs<TranslationProviderResult.Success>(result).value.html)
        assertEquals(GoogleTranslationProvider.ENDPOINT, requestedUrl)
        assertEquals("test-api-key", requestedApiKey)
        val bodyJson = json.parseToJsonElement(checkNotNull(requestedBody)).jsonObject
        assertEquals(source, bodyJson.getValue("q").jsonPrimitive.content)
        assertEquals("zh-TW", bodyJson.getValue("target").jsonPrimitive.content)
        assertEquals("en", bodyJson.getValue("source").jsonPrimitive.content)
        assertEquals("html", bodyJson.getValue("format").jsonPrimitive.content)
        client.close()
    }

    @Test
    fun verificationUsesDraftCredentialAndProfileLanguage() = runTest {
        val client = client(MockEngine { request ->
            assertEquals("draft-key", request.url.parameters["key"])
            val body = (request.body as OutgoingContent.ByteArrayContent).bytes().decodeToString()
            assertEquals(
                "de",
                json.parseToJsonElement(body).jsonObject.getValue("target").jsonPrimitive.content,
            )
            respondJson("<p>PodAura</p><p>Hallo</p>")
        })

        val result = GoogleTranslationProvider(client, credentials).verify(
            profile = profile.copy(targetLanguage = "DE"),
            credential = "draft-key",
        )

        assertIs<TranslationVerificationResult.Success>(result)
        client.close()
    }

    @Test
    fun mapsInvalidKeyQuotaAndRateLimitErrors() = runTest {
        suspend fun result(
            status: HttpStatusCode,
            body: String,
        ): TranslationProviderResult {
            val client = client(MockEngine {
                respond(
                    content = body,
                    status = status,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            })
            return GoogleTranslationProvider(client, credentials).translateHtml(
                profile,
                HtmlTranslationRequest("<p>Hello</p>", targetLanguage = "FR"),
            ).also { client.close() }
        }

        val authentication = result(
            HttpStatusCode.BadRequest,
            """{"error":{"status":"INVALID_ARGUMENT","details":[{"reason":"API_KEY_INVALID"}]}}""",
        )
        assertEquals(
            TranslationError.Authentication,
            assertIs<TranslationProviderResult.Failure>(authentication).error,
        )

        val quota = result(
            HttpStatusCode.Forbidden,
            """{"error":{"status":"RESOURCE_EXHAUSTED"}}""",
        )
        assertEquals(
            TranslationError.QuotaExceeded,
            assertIs<TranslationProviderResult.Failure>(quota).error,
        )

        var requests = 0
        val delays = mutableListOf<Long>()
        val retryClient = client(MockEngine {
            requests++
            if (requests == 1) {
                respond(
                    content = "{}",
                    status = HttpStatusCode.TooManyRequests,
                    headers = headersOf(HttpHeaders.RetryAfter, "2"),
                )
            } else {
                respondJson("<p>Bonjour</p>")
            }
        })
        val retried = GoogleTranslationProvider(retryClient, credentials) { delays += it }
            .translateHtml(
                profile,
                HtmlTranslationRequest("<p>Hello</p>", targetLanguage = "FR"),
            )

        assertIs<TranslationProviderResult.Success>(retried)
        assertEquals(2, requests)
        assertEquals(listOf(2_000L), delays)
        retryClient.close()
    }

    @Test
    fun rejectsOversizedHtmlBeforeNetwork() = runTest {
        var requests = 0
        val client = client(MockEngine {
            requests++
            error("Oversized HTML must not be requested")
        })

        val result = GoogleTranslationProvider(client, credentials).translateHtml(
            profile,
            HtmlTranslationRequest(
                html = "x".repeat(GoogleTranslationProvider.MAX_TEXT_REQUEST_BYTES.toInt() + 1),
                targetLanguage = "FR",
            ),
        )

        assertIs<TranslationError.ContentTooLarge>(
            assertIs<TranslationProviderResult.Failure>(result).error
        )
        assertEquals(0, requests)
        client.close()
    }

    private fun client(engine: MockEngine) = HttpClient(engine) {
        install(ContentNegotiation) { json(json) }
    }

    private fun io.ktor.client.engine.mock.MockRequestHandleScope.respondJson(
        html: String,
        detectedSourceLanguage: String? = "en",
    ) = respond(
        content = buildString {
            append("{\"data\":{\"translations\":[{\"translatedText\":")
            append(json.encodeToString(html))
            detectedSourceLanguage?.let {
                append(",\"detectedSourceLanguage\":")
                append(json.encodeToString(it))
            }
            append("}]}}")
        },
        status = HttpStatusCode.OK,
        headers = headersOf(HttpHeaders.ContentType, "application/json"),
    )

    private class MemoryCredentialStore(
        private val values: MutableMap<String, String>,
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
