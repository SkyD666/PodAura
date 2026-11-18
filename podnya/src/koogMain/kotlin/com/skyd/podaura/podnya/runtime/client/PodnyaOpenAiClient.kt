package com.skyd.podaura.podnya.runtime.client

import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.prompt.Prompt
import ai.koog.prompt.executor.clients.ConnectionTimeoutConfig
import ai.koog.prompt.executor.clients.openai.OpenAIClientSettings
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.clients.openai.base.models.OpenAIContentPart
import ai.koog.prompt.executor.clients.openai.base.models.OpenAIMessage
import ai.koog.prompt.executor.clients.openai.models.OpenAIChatCompletionResponse
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.MessagePart
import ai.koog.prompt.streaming.StreamFrame
import com.skyd.podaura.podnya.core.AssistantConnection
import com.skyd.podaura.podnya.core.AssistantError
import com.skyd.podaura.podnya.core.AssistantException
import com.skyd.podaura.podnya.runtime.PodnyaHttpFactory
import com.skyd.podaura.podnya.runtime.normalizeResponsesFrames
import kotlinx.coroutines.flow.Flow
import ai.koog.prompt.executor.clients.openai.base.models.Content as OpenAIContent

internal fun podnyaModel(modelId: String, responses: Boolean = false) = LLModel(
    provider = LLMProvider.OpenAI,
    id = modelId,
    capabilities = listOf(LLMCapability.Completion, LLMCapability.Tools) + if (responses) {
        listOf(LLMCapability.OpenAIEndpoint.Responses, LLMCapability.Thinking)
    } else {
        listOf(LLMCapability.OpenAIEndpoint.Completions)
    },
)

internal fun podnyaOpenAiClient(
    connection: AssistantConnection,
    httpFactory: PodnyaHttpFactory,
    responses: Boolean,
    timeoutConfig: ConnectionTimeoutConfig,
): OpenAILLMClient = object : OpenAILLMClient(
    apiKey = connection.apiKey,
    settings = OpenAIClientSettings(
        baseUrl = connection.baseUrl,
        chatCompletionsPath = "chat/completions",
        responsesAPIPath = "responses",
        timeoutConfig = timeoutConfig,
    ),
    httpClientFactory = httpFactory,
) {
    override fun executeStreaming(
        prompt: Prompt,
        model: LLModel,
        tools: List<ToolDescriptor>
    ): Flow<StreamFrame> {
        val frames = super.executeStreaming(prompt = prompt, model = model, tools = tools)
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
                parts = parts,
                metaInfo = createMetaInfo(response.usage),
                finishReason = choice.finishReason
            )
        }
    }
}
