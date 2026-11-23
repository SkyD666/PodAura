package com.skyd.podaura.nano.runtime

import ai.koog.http.client.KoogHttpClient
import ai.koog.http.client.ktor.KtorKoogHttpClient
import com.skyd.podaura.nano.core.AssistantError
import com.skyd.podaura.nano.core.AssistantException
import com.skyd.podaura.nano.core.AssistantLimits
import com.skyd.podaura.nano.core.AssistantProtocol
import com.skyd.podaura.nano.core.AssistantReasoningEffort
import com.skyd.podaura.nano.runtime.client.AnthropicStreamGuard
import com.skyd.podaura.nano.runtime.client.GeminiStreamGuard
import com.skyd.podaura.nano.runtime.client.NanoAnthropicExchange
import com.skyd.podaura.nano.runtime.client.NanoGeminiExchange
import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.reflect.KClass

/** Dedicated engines have platform-level connection retries and redirects disabled. */
internal expect fun createNanoHttpClient(): HttpClient

/** Add application budgets and protocol validation around Koog's dedicated HTTP client. */
internal class NanoHttpFactory(
    baseClient: HttpClient,
    private val reasoningEffort: AssistantReasoningEffort? = null,
    private val protocol: AssistantProtocol = AssistantProtocol.OPENAI_COMPATIBLE,
    private val gemini: NanoGeminiExchange? = null,
) :
    KoogHttpClient.Factory, AutoCloseable {
    private var owned: KoogHttpClient? = null
    private val factory = KtorKoogHttpClient.Factory(baseClient)
    override fun create(
        clientName: String,
        baseUrl: String,
        headers: Map<String, String>,
        queryParameters: Map<String, String>,
        requestTimeoutMillis: Long,
        connectTimeoutMillis: Long,
        socketTimeoutMillis: Long,
        json: Json,
    ): KoogHttpClient {
        check(owned == null)
        return NanoHttpClient(
            delegate = factory.create(
                clientName = clientName,
                baseUrl = baseUrl,
                headers = if (protocol == AssistantProtocol.GEMINI_GENERATE_CONTENT) {
                    headers + mapOf("x-goog-api-key" to queryParameters.getValue("key"))
                } else headers,
                queryParameters = if (protocol == AssistantProtocol.GEMINI_GENERATE_CONTENT) {
                    queryParameters - "key"
                } else queryParameters,
                requestTimeoutMillis = requestTimeoutMillis,
                connectTimeoutMillis = connectTimeoutMillis,
                socketTimeoutMillis = socketTimeoutMillis,
                json = json,
            ),
            json = json,
            responses = reasoningEffort?.let(::NanoResponses),
            anthropic = if (protocol == AssistantProtocol.ANTHROPIC_MESSAGES) NanoAnthropicExchange() else null,
            gemini = gemini,
        ).also { owned = it }
    }

    override fun close() {
        owned?.close()
        owned = null
    }
}

private class NanoHttpClient(
    private val delegate: KoogHttpClient,
    private val json: Json,
    private val responses: NanoResponses?,
    private val anthropic: NanoAnthropicExchange?,
    private val gemini: NanoGeminiExchange?,
) : KoogHttpClient by delegate {
    private var closed = false

    override fun close() {
        if (!closed) {
            closed = true
            delegate.close()
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun <T : Any> prepareBody(body: T, bodyType: KClass<T>): String {
        val serialized = if (body is String) {
            body
        } else {
            json.encodeToString(
                serializer = json.serializersModule.serializer(
                    kClass = bodyType,
                    typeArgumentsSerializers = emptyList(),
                    isNullable = false,
                ),
                value = body,
            )
        }
        if (serialized.encodeToByteArray().size > AssistantLimits.PROMPT_BYTES) {
            throw AssistantException(AssistantError.BUDGET_EXHAUSTED)
        }
        val prepared =
            responses?.request(serialized) ?: anthropic?.request(serialized) ?: gemini?.request(
                serialized
            ) ?: serialized
        if (prepared.encodeToByteArray().size > AssistantLimits.PROMPT_BYTES) {
            throw AssistantException(AssistantError.BUDGET_EXHAUSTED)
        }
        return prepared
    }

    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun <T : Any, R : Any> post(
        path: String,
        requestBody: T,
        requestBodyType: KClass<T>,
        responseType: KClass<R>,
        parameters: Map<String, String>,
        headers: Map<String, String>,
    ): R {
        val body = prepareBody(body = requestBody, bodyType = requestBodyType)
        if (responses == null && anthropic == null && gemini == null) {
            return delegate.post(
                path = path,
                requestBody = body,
                requestBodyType = String::class,
                responseType = responseType,
                parameters = parameters,
                headers = headers,
            )
        }
        // Validate terminal status before the SDK can turn a failed response into a tool call.
        val raw = delegate.post(
            path = path,
            requestBody = body,
            requestBodyType = String::class,
            responseType = String::class,
            parameters = parameters,
            headers = headers,
        )
        responses?.response(raw)
        val validated = anthropic?.response(raw) ?: gemini?.response(raw) ?: raw
        @Suppress("UNCHECKED_CAST")
        return json.decodeFromString(
            json.serializersModule.serializer(
                kClass = responseType,
                typeArgumentsSerializers = emptyList(),
                isNullable = false
            ), validated
        ) as R
    }

    override fun <T : Any, R : Any, O : Any> sse(
        path: String,
        requestBody: T,
        requestBodyType: KClass<T>,
        dataFilter: (String?) -> Boolean,
        decodeStreamingResponse: (String) -> R,
        processStreamingChunk: (R) -> O?,
        parameters: Map<String, String>,
        headers: Map<String, String>,
    ): Flow<O> = flow {
        val body = prepareBody(body = requestBody, bodyType = requestBodyType)
        val responsesGuard = responses?.let { ResponsesStreamGuard() }
        val anthropicGuard = anthropic?.let { AnthropicStreamGuard() }
        val geminiGuard = gemini?.let { GeminiStreamGuard() }
        var done = false
        delegate.sse(
            path = path,
            requestBody = body,
            requestBodyType = String::class,
            dataFilter = { raw ->
                if (responsesGuard != null) {
                    responsesGuard.accept(raw)
                } else if (anthropicGuard != null) {
                    if (!anthropicGuard.accept(raw)) return@sse false
                } else if (geminiGuard != null) {
                    geminiGuard.accept(raw)
                } else if (!raw.isNullOrBlank()) {
                    if (done) throw AssistantException(AssistantError.STREAM_INTERRUPTED)
                    if (raw.trim() == "[DONE]") done = true
                }
                dataFilter(raw)
            },
            decodeStreamingResponse = decodeStreamingResponse,
            processStreamingChunk = processStreamingChunk,
            parameters = parameters,
            headers = headers,
        ).collect { emit(it) }
        if (responsesGuard != null) responsesGuard.finish()
        else if (anthropicGuard != null) anthropicGuard.finish()
        else if (geminiGuard != null) geminiGuard.finish()
        else if (!done) throw AssistantException(AssistantError.STREAM_INTERRUPTED)
    }
}
