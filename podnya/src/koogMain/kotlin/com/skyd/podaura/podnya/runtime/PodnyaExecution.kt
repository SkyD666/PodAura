package com.skyd.podaura.podnya.runtime

import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.http.client.KoogHttpClientException
import ai.koog.prompt.Prompt
import ai.koog.prompt.dsl.ModerationResult
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.Message
import ai.koog.prompt.streaming.StreamFrame
import com.skyd.podaura.podnya.core.AssistantError
import com.skyd.podaura.podnya.core.AssistantException
import com.skyd.podaura.podnya.core.AssistantFailure
import com.skyd.podaura.podnya.core.AssistantLimits
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
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal val podnyaJson = Json { encodeDefaults = true }

internal fun promptBytes(prompt: Prompt, tools: List<ToolDescriptor> = emptyList()): Int =
    podnyaJson.encodeToString(prompt).encodeToByteArray().size +
            // ToolDescriptor is not @Serializable in Koog 1.3.0. The wire body has a second exact guard.
            podnyaJson.encodeToString(tools.map { it.toString() }).encodeToByteArray().size

/** Count the actual executor boundary, including manual structured output and streaming. */
internal class BudgetedExecutor(private val delegate: PromptExecutor) : PromptExecutor() {
    var calls = 0
        private set

    private fun admit(prompt: Prompt, tools: List<ToolDescriptor>) {
        if (calls >= AssistantLimits.MODEL_CALLS ||
            promptBytes(prompt, tools) > AssistantLimits.PROMPT_BYTES
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
            withTimeoutOrNull(45_000.milliseconds) {
                delegate.execute(
                    prompt = prompt,
                    model = model,
                    tools = tools
                ).also { response ->
                    // Structured parsing must not accept valid-looking JSON from a truncated response.
                    if (response.finishReason in
                        setOf("length", "content_filter", "max_tokens", "MAX_TOKENS")
                    ) {
                        throw AssistantException(AssistantError.INVALID_OUTPUT)
                    }
                }
            }
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
            delegate.executeStreaming(prompt = prompt, model = model, tools = tools)
                .collect { emit(it) }
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
                                    text.append(part)
                                    delta(part)
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

/** Emit public completion only after the protocol's HTTP guard has validated the whole stream. */
internal fun Flow<StreamFrame>.normalizeNativeFrames(success: String): Flow<StreamFrame> = flow {
    val text = StringBuilder()
    var end: StreamFrame.End? = null
    collect { frame ->
        if (end != null) throw AssistantException(AssistantError.STREAM_INTERRUPTED)
        when (frame) {
            is StreamFrame.TextDelta -> {
                text.append(frame.text)
                if (text.length > AssistantLimits.REPLY_CHARS) throw AssistantException(
                    AssistantError.STREAM_INTERRUPTED
                )
                emit(frame)
            }

            is StreamFrame.TextComplete -> Unit
            is StreamFrame.End -> {
                if (frame.finishReason != success) throw AssistantException(AssistantError.STREAM_INTERRUPTED)
                end = frame
            }

            is StreamFrame.ToolCallDelta, is StreamFrame.ToolCallComplete ->
                throw AssistantException(AssistantError.STREAM_INTERRUPTED)

            else -> Unit
        }
    }
    val terminal = end ?: throw AssistantException(AssistantError.STREAM_INTERRUPTED)
    emit(StreamFrame.TextComplete(text.toString()))
    emit(StreamFrame.End("stop", terminal.metaInfo))
}
