package com.skyd.podaura.podnya.runtime

import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.http.client.KoogHttpClient
import ai.koog.http.client.KoogHttpClientException
import ai.koog.http.client.ktor.KtorKoogHttpClient
import ai.koog.prompt.Prompt
import ai.koog.prompt.dsl.ModerationResult
import ai.koog.prompt.executor.clients.ConnectionTimeoutConfig
import ai.koog.prompt.executor.clients.openai.OpenAIClientSettings
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.clients.openai.base.models.OpenAIContentPart
import ai.koog.prompt.executor.clients.openai.base.models.OpenAIMessage
import ai.koog.prompt.executor.clients.openai.models.OpenAIChatCompletionResponse
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.MessagePart
import ai.koog.prompt.streaming.StreamFrame
import com.skyd.podaura.podnya.core.AssistantError
import com.skyd.podaura.podnya.core.AssistantException
import com.skyd.podaura.podnya.core.AssistantFailure
import com.skyd.podaura.podnya.core.AssistantLimits
import com.skyd.podaura.podnya.settings.AssistantConnection
import com.skyd.podaura.podnya.settings.assistantEndpointPath
import io.ktor.client.HttpClient
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.timeout
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.serializer
import kotlin.reflect.KClass
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import ai.koog.prompt.executor.clients.openai.base.models.Content as OpenAIContent

internal val podnyaJson = Json { encodeDefaults = true }

/** Dedicated engines have platform-level connection retries and redirects disabled. */
internal expect fun createPodnyaHttpClient(): HttpClient

internal fun podnyaModel(modelId: String, responses: Boolean = false) = LLModel(
    provider = LLMProvider.OpenAI, id = modelId,
    capabilities = listOf(LLMCapability.Completion, LLMCapability.Tools) + if (responses) {
        listOf(LLMCapability.OpenAIEndpoint.Responses, LLMCapability.Thinking)
    } else listOf(LLMCapability.OpenAIEndpoint.Completions),
)

/** Owns both the factory's configured client copy and the original engine-owning client. */
internal class PodnyaTransport(
    connection: AssistantConnection,
    private val baseClient: HttpClient
) : AutoCloseable {
    private val responses =
        assistantEndpointPath(connection.baseUrl, connection.modelId) == "responses"
    val model = podnyaModel(connection.modelId, responses)
    val executor: PromptExecutor

    init {
        initializePodnyaLogging()
        executor = try {
            MultiLLMPromptExecutor(object : OpenAILLMClient(
                apiKey = connection.apiKey,
                settings = OpenAIClientSettings(
                    baseUrl = connection.baseUrl,
                    chatCompletionsPath = "chat/completions",
                    responsesAPIPath = "responses",
                    timeoutConfig = ConnectionTimeoutConfig(
                        requestTimeoutMillis = 45_000, connectTimeoutMillis = 15_000,
                        socketTimeoutMillis = 15_000,
                    ),
                ),
                httpClientFactory = PodnyaHttpFactory(baseClient, responses),
            ) {
                override fun executeStreaming(
                    prompt: Prompt,
                    model: LLModel,
                    tools: List<ToolDescriptor>
                ): Flow<StreamFrame> {
                    val frames = super.executeStreaming(prompt, model, tools)
                    return if (responses) frames.normalizeResponsesFrames() else frames
                }

                // Koog 1.3 eagerly parses function arguments while converting provider responses.
                // Retain the raw string so malformed calls reach our budget gate before decoding.
                override fun processProviderChatResponse(response: OpenAIChatCompletionResponse): List<Message.Assistant> {
                    if (response.choices.isEmpty()) throw AssistantException(AssistantError.INVALID_OUTPUT)
                    return response.choices.map { choice ->
                        val message = choice.message as? OpenAIMessage.Assistant
                            ?: throw AssistantException(AssistantError.INVALID_OUTPUT)
                        val parts = buildList {
                            when (val content = message.content) {
                                is OpenAIContent.Text -> add(MessagePart.Text(content.value))
                                is OpenAIContent.Parts -> content.value.forEach {
                                    if (it is OpenAIContentPart.Text) add(MessagePart.Text(it.text))
                                }

                                null -> Unit
                            }
                            message.reasoningContent?.takeIf { it.isNotBlank() }?.let {
                                add(MessagePart.Reasoning(content = listOf(it)))
                            }
                            message.toolCalls.orEmpty().forEach {
                                add(
                                    MessagePart.Tool.Call(
                                        id = it.id,
                                        tool = it.function.name,
                                        args = it.function.arguments
                                    )
                                )
                            }
                        }
                        Message.Assistant(
                            parts,
                            createMetaInfo(response.usage),
                            choice.finishReason
                        )
                    }
                }
            })
        } catch (failure: Throwable) {
            baseClient.close()
            throw failure
        }
    }

    override fun close() {
        try {
            executor.close()
        } finally {
            baseClient.close()
        }
    }
}

/** Add application budgets and protocol validation around Koog's dedicated HTTP client. */
private class PodnyaHttpFactory(baseClient: HttpClient, private val responses: Boolean) :
    KoogHttpClient.Factory {
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
    ): KoogHttpClient = PodnyaHttpClient(
        factory.create(
            clientName, baseUrl, headers, queryParameters,
            requestTimeoutMillis, connectTimeoutMillis, socketTimeoutMillis, json
        ), json,
        if (responses) PodnyaResponses() else null
    )
}

private class PodnyaHttpClient(
    private val delegate: KoogHttpClient,
    private val json: Json,
    private val responses: PodnyaResponses?,
) : KoogHttpClient by delegate {
    private fun prepareBody(body: Any): String {
        // The OpenAI client serializes every request to a String before reaching this boundary.
        if (body !is String || body.encodeToByteArray().size > AssistantLimits.PROMPT_BYTES) {
            throw AssistantException(AssistantError.BUDGET_EXHAUSTED)
        }
        val prepared = responses?.request(body) ?: body
        if (prepared.encodeToByteArray().size > AssistantLimits.PROMPT_BYTES) {
            throw AssistantException(AssistantError.BUDGET_EXHAUSTED)
        }
        return prepared
    }

    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun <T : Any, R : Any> post(
        path: String, requestBody: T, requestBodyType: KClass<T>, responseType: KClass<R>,
        parameters: Map<String, String>, headers: Map<String, String>,
    ): R {
        val body = prepareBody(requestBody)
        if (responses == null) return delegate.post(
            path,
            body,
            String::class,
            responseType,
            parameters,
            headers
        )
        // Koog's Responses converter ignores failed/incomplete statuses. Validate before it can
        // turn an unfinished function_call into a runnable tool, then use the SDK's own serializer.
        val raw = delegate.post(path, body, String::class, String::class, parameters, headers)
        responses.response(raw)
        @Suppress("UNCHECKED_CAST")
        return json.decodeFromString(
            json.serializersModule.serializer(
                responseType,
                emptyList(),
                false
            ), raw
        ) as R
    }

    override fun <T : Any, R : Any, O : Any> sse(
        path: String, requestBody: T, requestBodyType: KClass<T>, dataFilter: (String?) -> Boolean,
        decodeStreamingResponse: (String) -> R, processStreamingChunk: (R) -> O?,
        parameters: Map<String, String>, headers: Map<String, String>,
    ): Flow<O> = flow {
        val body = prepareBody(requestBody)
        val responsesGuard = responses?.let { ResponsesStreamGuard() }
        var done = false
        delegate.sse(path, body, String::class, { raw ->
            if (responsesGuard != null) responsesGuard.accept(raw)
            else if (!raw.isNullOrBlank()) {
                if (done) throw AssistantException(AssistantError.STREAM_INTERRUPTED)
                if (raw.trim() == "[DONE]") done = true
            }
            dataFilter(raw)
        }, decodeStreamingResponse, processStreamingChunk, parameters, headers).collect { emit(it) }
        if (responsesGuard != null) responsesGuard.finish()
        else if (!done) throw AssistantException(AssistantError.STREAM_INTERRUPTED)
    }
}

internal fun promptBytes(prompt: Prompt, tools: List<ToolDescriptor> = emptyList()): Int =
    podnyaJson.encodeToString(prompt).encodeToByteArray().size +
            // ToolDescriptor is not @Serializable in Koog 1.3.0. The wire body has a second exact guard.
            podnyaJson.encodeToString(tools.map { it.toString() }).encodeToByteArray().size

/** Count the actual executor boundary, including manual structured output and streaming. */
internal class BudgetedExecutor(private val delegate: PromptExecutor) : PromptExecutor() {
    var calls = 0
        private set

    private fun admit(prompt: Prompt, tools: List<ToolDescriptor>) {
        if (calls >= AssistantLimits.MODEL_CALLS || promptBytes(
                prompt,
                tools
            ) > AssistantLimits.PROMPT_BYTES
        ) {
            throw AssistantException(AssistantError.BUDGET_EXHAUSTED)
        }
        calls++
    }

    override suspend fun execute(
        prompt: Prompt,
        model: LLModel,
        tools: List<ToolDescriptor>
    ): Message.Assistant {
        admit(prompt, tools)
        return try {
            withTimeoutOrNull(45_000.milliseconds) { delegate.execute(prompt, model, tools) }
                ?: throw AssistantException(AssistantError.TIMEOUT)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            throw AssistantException(failure.assistantError())
        }
    }

    override fun executeStreaming(
        prompt: Prompt,
        model: LLModel,
        tools: List<ToolDescriptor>
    ): Flow<StreamFrame> = flow {
        admit(prompt, tools)
        try {
            delegate.executeStreaming(prompt, model, tools).collect { emit(it) }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            throw AssistantException(failure.assistantError())
        }
    }

    override suspend fun moderate(prompt: Prompt, model: LLModel): ModerationResult =
        throw AssistantException(AssistantError.UNAVAILABLE)

    // The enclosing transport owns the executor, including while its agent is being closed.
    override fun close() = Unit
}

internal fun Throwable.assistantError(): AssistantFailure {
    var current: Throwable? = this
    repeat(8) {
        when (val error = current) {
            is AssistantException -> return error.error
            is KoogHttpClientException -> when (error.statusCode) {
                401, 403 -> return AssistantError.AUTHENTICATION
                404 -> return AssistantError.ENDPOINT_OR_MODEL
                429 -> return requestLimitError(error.errorBody)
                in 300..399 -> return AssistantError.INVALID_ADDRESS
                400, 405, 422 -> return AssistantError.ENDPOINT_OR_MODEL
            }

            is HttpRequestTimeoutException, is ConnectTimeoutException, is SocketTimeoutException ->
                return AssistantError.TIMEOUT

            is SerializationException -> return AssistantError.INVALID_OUTPUT
        }
        current = current?.cause
    }
    return AssistantError.NETWORK
}

private fun requestLimitError(body: String?): AssistantFailure {
    // Only recognize bounded, structured codes. Never expose or match free-form server messages.
    val error = body?.takeIf { it.length <= 16_384 }?.let {
        runCatching {
            (podnyaJson.parseToJsonElement(it) as? JsonObject)?.get("error") as? JsonObject
        }.getOrNull()
    }

    fun field(name: String) = (error?.get(name) as? JsonPrimitive)?.takeIf { it.isString }?.content
    return when (field("code")) {
        "insufficient_quota", "credit_balance_exhausted", "organization_spend_limit_exceeded",
        "project_spend_limit_exceeded", "organization_usage_limit_exceeded" -> AssistantError.QUOTA_EXCEEDED

        "rate_limit_exceeded", "rate_limit_error", "slow_down" -> AssistantError.RATE_LIMIT
        else -> when (field("type")) {
            "insufficient_quota" -> AssistantError.QUOTA_EXCEEDED
            "rate_limit_error", "rate_limit_exceeded" -> AssistantError.RATE_LIMIT
            else -> AssistantError.REQUEST_LIMITED
        }
    }
}

/** No TextComplete replay, reasoning, tool payloads, or synthesized typing events. */
@OptIn(FlowPreview::class)
internal suspend fun collectReply(frames: Flow<StreamFrame>, delta: suspend (String) -> Unit) {
    val finished = withTimeoutOrNull(45_000.milliseconds) {
        coroutineScope {
            val firstText = CompletableDeferred<Unit>()
            val watchdog = launch {
                if (withTimeoutOrNull(15_000.milliseconds) { firstText.await() } == null) {
                    throw AssistantException(AssistantError.STREAM_INTERRUPTED)
                }
            }
            val text = StringBuilder()
            var complete = false
            var ended = false
            try {
                frames.timeout(15.seconds).collect { frame ->
                    if (ended) throw AssistantException(AssistantError.STREAM_INTERRUPTED)
                    when (frame) {
                        is StreamFrame.TextDelta -> {
                            if (complete) throw AssistantException(AssistantError.STREAM_INTERRUPTED)
                            if (frame.text.isNotEmpty()) {
                                firstText.complete(Unit)
                                val remaining = AssistantLimits.REPLY_CHARS - text.length
                                val part = frame.text.take(remaining)
                                if (part.isNotEmpty()) {
                                    text.append(part); delta(part)
                                }
                                if (part.length < frame.text.length) {
                                    throw AssistantException(AssistantError.STREAM_INTERRUPTED)
                                }
                            }
                        }

                        is StreamFrame.TextComplete -> {
                            if (complete || frame.text != text.toString()) {
                                throw AssistantException(AssistantError.STREAM_INTERRUPTED)
                            }
                            complete = true
                        }

                        is StreamFrame.End -> {
                            if (frame.finishReason != "stop" || !complete || text.isBlank()) {
                                throw AssistantException(AssistantError.STREAM_INTERRUPTED)
                            }
                            ended = true
                        }

                        is StreamFrame.ToolCallDelta, is StreamFrame.ToolCallComplete ->
                            throw AssistantException(AssistantError.STREAM_INTERRUPTED)

                        else -> Unit
                    }
                }
                if (!ended) throw AssistantException(AssistantError.STREAM_INTERRUPTED)
            } catch (_: TimeoutCancellationException) {
                currentCoroutineContext().ensureActive()
                throw AssistantException(AssistantError.STREAM_INTERRUPTED)
            } finally {
                watchdog.cancel()
            }
        }
        true
    }
    if (finished != true) throw AssistantException(AssistantError.STREAM_INTERRUPTED)
}
