package com.skyd.podaura.podnya.runtime

import ai.koog.agents.core.tools.Tool
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.model.executeStructured
import ai.koog.prompt.message.MessagePart
import ai.koog.prompt.structure.StructuredRequest
import ai.koog.prompt.structure.StructuredRequestConfig
import ai.koog.prompt.structure.json.JsonStructure
import ai.koog.serialization.typeToken
import com.skyd.podaura.podnya.core.AssistantConnectionResult
import com.skyd.podaura.podnya.core.AssistantError
import com.skyd.podaura.podnya.core.AssistantException
import com.skyd.podaura.podnya.core.AssistantLimits
import com.skyd.podaura.podnya.settings.AssistantConnection
import io.ktor.client.HttpClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.milliseconds

/** Exercises the configured protocol with synthetic data, independently of any capability. */
internal class PodnyaConnectionProbe(
    private val connection: suspend () -> AssistantConnection,
    private val httpClient: () -> HttpClient = ::createPodnyaHttpClient,
) {
    suspend fun verify(): AssistantConnectionResult = try {
        val completed = withTimeoutOrNull(AssistantLimits.TURN_TIMEOUT_MILLIS.milliseconds) {
            PodnyaTransport(connection(), httpClient()).use { transport ->
                val executor = BudgetedExecutor(transport.executor)
                val tool = EchoTool()
                val tools = listOf(tool.descriptor)
                var conversation = prompt("podnya-connection-tool") {
                    system(
                        "This is a connection capability test using synthetic data only. " +
                                "Call podnya_echo with value '$PROBE_VALUE'. After its result, acknowledge it " +
                                "without another tool call. Do not invent a tool result."
                    )
                    user("Run the synthetic echo test.")
                }
                var attempts = 0
                var successful = false
                var pendingResults = false
                while (attempts < PROBE_TOOL_CALLS) {
                    // Never replace a pending tool/result exchange with a summary: that would let
                    // a service pass verification without accepting its own tool results.
                    if (promptBytes(conversation, tools) > AssistantLimits.PROMPT_BYTES - 8_192) {
                        throw AssistantException(AssistantError.BUDGET_EXHAUSTED)
                    }
                    val response = executor.execute(conversation, transport.model, tools)
                    pendingResults = false
                    if (response.finishReason == "length" || response.finishReason == "content_filter") {
                        throw AssistantException(AssistantError.INVALID_OUTPUT)
                    }
                    val calls = response.parts.filterIsInstance<MessagePart.Tool.Call>()
                    if (calls.isEmpty()) break
                    val results = calls.map { call ->
                        // Count raw attempts before JSON decoding, including unknown and excess calls.
                        attempts++
                        val error = when {
                            attempts > PROBE_TOOL_CALLS -> "TOOL_BUDGET_EXHAUSTED"
                            call.tool != tool.name -> "UNKNOWN_TOOL"
                            else -> try {
                                require(call.args.length <= 16_384)
                                val args = podnyaJson.decodeFromString<ProbeValue>(call.args)
                                require(args.value == PROBE_VALUE)
                                tool.execute(args)
                                successful = true
                                null
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (_: Exception) {
                                "INVALID_ARGUMENT"
                            }
                        }
                        MessagePart.Tool.Result(
                            id = call.id, tool = call.tool,
                            output = if (error == null) podnyaJson.encodeToString(
                                ProbeValue(PROBE_VALUE)
                            )
                            else podnyaJson.encodeToString(ProbeError(error)),
                            isError = error != null,
                        )
                    }
                    conversation = prompt(conversation) {
                        message(response)
                        user { results.forEach { toolResult(it) } }
                    }
                    pendingResults = true
                }
                if (pendingResults) {
                    if (promptBytes(conversation, tools) > AssistantLimits.PROMPT_BYTES - 8_192) {
                        throw AssistantException(AssistantError.BUDGET_EXHAUSTED)
                    }
                    val acknowledgement =
                        executor.execute(conversation, transport.model, emptyList())
                    if (acknowledgement.parts.any { it is MessagePart.Tool.Call } ||
                        acknowledgement.finishReason == "length" || acknowledgement.finishReason == "content_filter") {
                        throw AssistantException(AssistantError.INVALID_OUTPUT)
                    }
                }
                if (!successful) throw AssistantException(
                    if (attempts >= PROBE_TOOL_CALLS) AssistantError.BUDGET_EXHAUSTED
                    else AssistantError.TOOLS_UNSUPPORTED,
                )

                // One explicit repair; Koog's automatic fixing parser remains disabled.
                var structured = false
                for (attempt in 0..1) {
                    currentCoroutineContext().ensureActive()
                    val response = executor.executeStructured(
                        prompt = prompt("podnya-connection-structure") {
                            system(
                                "This is a synthetic connection test. Return value '$PROBE_VALUE' " +
                                        "in the required structure. No tools or additional content."
                            )
                            user(
                                if (attempt == 0) "Return the test value."
                                else "The previous output was invalid. Correct the structure and test value."
                            )
                        },
                        model = transport.model,
                        config = StructuredRequestConfig(
                            default = StructuredRequest.Manual(JsonStructure.create<ProbeValue>()),
                        ),
                        fixingParser = null,
                    )
                    val parsed = response.getOrElse { failure ->
                        if (failure is CancellationException) throw failure
                        if (failure is AssistantException && failure.error != AssistantError.INVALID_OUTPUT) throw failure
                        null
                    }
                    if (parsed?.data?.value == PROBE_VALUE) {
                        structured = true
                        break
                    }
                }
                if (!structured) throw AssistantException(AssistantError.INVALID_OUTPUT)
                collectReply(
                    executor.executeStreaming(
                        prompt = prompt("podnya-connection-stream") {
                            system("This is a synthetic connection test. Reply briefly in plain text without tools.")
                            user("Reply OK.")
                        },
                        model = transport.model, tools = emptyList(),
                    )
                ) {}
            }
            true
        }
        if (completed == true) AssistantConnectionResult(success = true)
        else AssistantConnectionResult(success = false, error = AssistantError.TIMEOUT)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        currentCoroutineContext().ensureActive()
        AssistantConnectionResult(success = false, error = failure.assistantError())
    }
}

private const val PROBE_VALUE = "podnya-capability"
private const val PROBE_TOOL_CALLS = 3

@Serializable
private data class ProbeValue(val value: String)

@Serializable
private data class ProbeError(val error: String)

private class EchoTool : Tool<ProbeValue, ProbeValue>(
    argsType = typeToken<ProbeValue>(), resultType = typeToken<ProbeValue>(),
    name = "podnya_echo", description = "Echo the supplied synthetic connection-test value.",
) {
    override suspend fun execute(args: ProbeValue): ProbeValue = args
}
