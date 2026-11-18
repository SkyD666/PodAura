package com.skyd.podaura.podnya.runtime

import ai.koog.prompt.executor.clients.ConnectionTimeoutConfig
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import com.skyd.podaura.podnya.core.AssistantConnection
import com.skyd.podaura.podnya.core.AssistantProtocol
import com.skyd.podaura.podnya.core.assistantEndpointPath
import com.skyd.podaura.podnya.runtime.client.PodnyaGeminiExchange
import com.skyd.podaura.podnya.runtime.client.podnyaAnthropicClient
import com.skyd.podaura.podnya.runtime.client.podnyaGeminiClient
import com.skyd.podaura.podnya.runtime.client.podnyaModel
import com.skyd.podaura.podnya.runtime.client.podnyaOpenAiClient
import io.ktor.client.HttpClient
import kotlinx.coroutines.cancel

/** Owns both the factory's configured client copy and the original engine-owning client. */
internal class PodnyaTransport(
    connection: AssistantConnection,
    private val baseClient: HttpClient,
) : AutoCloseable {
    private val responses = assistantEndpointPath(
        baseUrl = connection.baseUrl,
        modelId = connection.modelId,
        protocol = connection.protocol
    ) == "responses"
    private val gemini =
        if (connection.protocol == AssistantProtocol.GEMINI_GENERATE_CONTENT) PodnyaGeminiExchange() else null
    private val httpFactory = PodnyaHttpFactory(
        baseClient = baseClient,
        reasoningEffort = connection.reasoningEffort,
        protocol = connection.protocol,
        gemini = gemini,
    )
    val model: LLModel
    val executor: PromptExecutor

    init {
        initializePodnyaLogging()
        executor = try {
            model = when (connection.protocol) {
                AssistantProtocol.OPENAI_COMPATIBLE -> podnyaModel(
                    modelId = connection.modelId,
                    responses = responses,
                )

                else -> LLModel(
                    provider = if (connection.protocol == AssistantProtocol.ANTHROPIC_MESSAGES) {
                        LLMProvider.Anthropic
                    } else {
                        LLMProvider.Google
                    },
                    id = connection.modelId,
                    capabilities = listOf(
                        LLMCapability.Completion,
                        LLMCapability.Tools,
                        LLMCapability.Thinking
                    ),
                )
            }

            val timeoutConfig = ConnectionTimeoutConfig(
                requestTimeoutMillis = connection.timeouts.requestMillis,
                connectTimeoutMillis = connection.timeouts.connectMillis,
                socketTimeoutMillis = connection.timeouts.idleMillis,
            )
            val client = when (connection.protocol) {
                AssistantProtocol.OPENAI_COMPATIBLE -> podnyaOpenAiClient(
                    connection = connection,
                    httpFactory = httpFactory,
                    responses = responses,
                    timeoutConfig = timeoutConfig,
                )

                AssistantProtocol.ANTHROPIC_MESSAGES -> podnyaAnthropicClient(
                    connection = connection,
                    httpFactory = httpFactory,
                    model = model,
                    timeoutConfig = timeoutConfig,
                )

                AssistantProtocol.GEMINI_GENERATE_CONTENT -> podnyaGeminiClient(
                    connection = connection,
                    httpFactory = httpFactory,
                    exchange = requireNotNull(gemini),
                    timeoutConfig = timeoutConfig,
                )
            }
            try {
                MultiLLMPromptExecutor(client)
            } catch (failure: Throwable) {
                client.close()
                throw failure
            }
        } catch (failure: Throwable) {
            closeHttp()
            throw failure
        }
    }

    override fun close() {
        try {
            executor.close()
        } finally {
            closeHttp()
        }
    }

    private fun closeHttp() {
        try {
            httpFactory.close()
        } finally {
            try {
                baseClient.close()
            } finally {
                // A failed Ktor configured-copy constructor can retain an engine reference.
                // This transport exclusively owns the dedicated engine, even on that path.
                baseClient.engine.close()
                baseClient.engine.coroutineContext.cancel()
            }
        }
    }
}
