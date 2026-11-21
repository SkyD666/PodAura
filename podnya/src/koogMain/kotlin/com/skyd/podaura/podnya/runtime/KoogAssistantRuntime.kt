package com.skyd.podaura.podnya.runtime

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.dsl.builder.node
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.prompt.message.MessagePart
import ai.koog.prompt.structure.StructuredRequest
import ai.koog.prompt.structure.StructuredRequestConfig
import com.skyd.podaura.podnya.core.AssistantConnection
import com.skyd.podaura.podnya.core.AssistantConnectionResult
import com.skyd.podaura.podnya.core.AssistantError
import com.skyd.podaura.podnya.core.AssistantException
import com.skyd.podaura.podnya.core.AssistantLimits
import com.skyd.podaura.podnya.core.AssistantPhase
import com.skyd.podaura.podnya.core.AssistantRunEvent
import com.skyd.podaura.podnya.core.AssistantRuntime
import com.skyd.podaura.podnya.core.AssistantTurnRequest
import io.ktor.client.HttpClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow

internal class KoogAssistantRuntime(
    private val capabilities: (AssistantTurnRequest) -> List<KoogCapability>,
    private val httpClient: () -> HttpClient = ::createPodnyaHttpClient,
) : AssistantRuntime {
    init {
        initializePodnyaLogging()
    }

    override val available: Boolean = true

    override fun run(
        request: AssistantTurnRequest,
        connection: AssistantConnection
    ): Flow<AssistantRunEvent> {
        return assistantRunEvents(request = request, timeouts = connection.timeouts) { event ->
            executeTurn(
                request = request,
                configuration = connection,
                event = event
            )
        }
    }

    override suspend fun verify(connection: suspend () -> AssistantConnection): AssistantConnectionResult =
        PodnyaConnectionProbe(connection = connection, httpClient = httpClient).verify()

    private suspend fun executeTurn(
        request: AssistantTurnRequest,
        configuration: AssistantConnection,
        event: suspend (AssistantRunEvent) -> Unit,
    ) {
        val active = capabilities(request)
        if (active.isEmpty()) throw AssistantException(AssistantError.INVALID_OUTPUT)
        require(active.map { it.id }.distinct().size == active.size)
        active.forEach { it.prepare() }
        val lookups = active.mapNotNull { it.lookupTool }
        lookups.forEach { it.prepare() }
        val registeredTools = active.flatMap { owner -> owner.tools.map { it to owner } }
        val toolOwners = registeredTools.associate { (tool, owner) -> tool.name to owner }
        require(toolOwners.size == registeredTools.size)
        val prompts = AssistantPrompts(request, active)
        PodnyaTransport(
            connection = configuration,
            baseClient = httpClient(),
        ).use { transport ->
            val executor =
                BudgetedExecutor(delegate = transport.executor, timeouts = configuration.timeouts)
            var attempts = 0
            var completedCalls = 0
            val graph = strategy<String, ValidatedAssistantReply>("podnya") {
                val useTools by node<String, ValidatedAssistantReply?> {
                    while (attempts < AssistantLimits.TOOL_CALLS) {
                        event(
                            AssistantRunEvent.Progress(AssistantPhase.PROCESSING)
                        )
                        val response = llm.writeSession {
                            if (promptBytes(prompt, tools) > AssistantLimits.PROMPT_BYTES - 8_192) {
                                rewritePrompt { prompts.snapshot(lookupAvailable = completedCalls == 0) }
                            }
                            requestLLM()
                        }
                        if (response.finishReason == "length" || response.finishReason == "content_filter") {
                            throw AssistantException(AssistantError.INVALID_OUTPUT)
                        }
                        val calls = response.parts.filterIsInstance<MessagePart.Tool.Call>()
                        if (calls.isEmpty()) {
                            val finished = response.finishReason == "stop" ||
                                    response.finishReason.equals("completed", ignoreCase = true)
                            if (attempts == 0 && finished) {
                                return@node parseDirectExplanation(response.textContent())
                            }
                            break
                        }
                        val lookup = calls.singleOrNull()
                            ?.takeIf { completedCalls == 0 }
                            ?.let { call -> toolOwners[call.tool]?.lookupTool?.takeIf { it.name == call.tool } }
                        val results = calls.map { call ->
                            // Share one budget across all tools, before decoding any arguments.
                            attempts++
                            val error = when {
                                attempts > AssistantLimits.TOOL_CALLS -> "TOOL_BUDGET_EXHAUSTED"
                                call.tool !in toolOwners -> "UNKNOWN_TOOL"
                                lookup == null && toolOwners[call.tool]?.lookupTool?.name == call.tool ->
                                    "STANDALONE_LOOKUP_REQUIRED"

                                else -> null
                            }
                            val result = if (error != null) MessagePart.Tool.Result(
                                call.id, call.tool, "{\"error\":\"$error\"}", isError = true
                            ) else toolOwners.getValue(call.tool).admit(call)
                                ?: environment.executeTool(call).toMessagePart()
                            if (!result.isError) completedCalls++
                            result
                        }
                        // Every call, even an excess or malformed call, receives a result before leaving this phase.
                        llm.writeSession {
                            appendPrompt { user { results.forEach { toolResult(it) } } }
                        }
                        active.firstNotNullOfOrNull { it.failure }
                            ?.let { throw AssistantException(it) }
                        lookup?.reply?.let { return@node it }
                    }
                    if (attempts >= AssistantLimits.TOOL_CALLS && completedCalls == 0) {
                        throw AssistantException(AssistantError.BUDGET_EXHAUSTED)
                    }
                    null
                }
                val buildReply by node<ValidatedAssistantReply?, ValidatedAssistantReply> { directReply ->
                    if (directReply != null) return@node directReply
                    event(
                        AssistantRunEvent.Progress(
                            AssistantPhase.PROCESSING
                        )
                    )
                    var validated: ValidatedAssistantReply? = null
                    for (attempt in 0..1) {
                        currentCoroutineContext().ensureActive()
                        val answer = llm.writeSession {
                            tools = emptyList()
                            rewritePrompt { prompts.snapshot(final = true, repair = attempt > 0) }
                            requestLLMStructured(
                                config = StructuredRequestConfig(
                                    default = StructuredRequest.Manual(outputStructure(active)),
                                ),
                                fixingParser = null,
                            )
                        }
                        try {
                            val parsed = answer.getOrElse { failure ->
                                if (failure is CancellationException) throw failure
                                if (failure is AssistantException) throw failure
                                throw AssistantException(AssistantError.INVALID_OUTPUT)
                            }.data
                            validated = validateOutput(parsed, active)
                            break
                        } catch (failure: AssistantException) {
                            if (attempt == 1 || failure.error != AssistantError.INVALID_OUTPUT) throw failure
                        }
                    }
                    validated ?: throw AssistantException(AssistantError.INVALID_OUTPUT)
                }
                edge(nodeStart forwardTo useTools)
                edge(useTools forwardTo buildReply)
                edge(buildReply forwardTo nodeFinish)
            }
            val agent = AIAgent(
                promptExecutor = executor,
                agentConfig = AIAgentConfig(
                    prompt = prompts.snapshot(), model = transport.model, maxAgentIterations = 24,
                ),
                strategy = graph,
                toolRegistry = ToolRegistry {
                    registeredTools.forEach { (registered, _) ->
                        tool(registered)
                    }
                },
            )
            val validated = try {
                agent.run(request.question)
            } finally {
                agent.close()
            }
            validated.results.values.singleOrNull()?.handoff?.let { handoff ->
                event(AssistantRunEvent.Handoff(handoff))
                return@use
            }
            if (validated.results.isEmpty()) {
                publishDirectReply(reply = validated, event = event)
                return@use
            }
            event(AssistantRunEvent.ContentReady(validated.content()))
            event(AssistantRunEvent.Progress(AssistantPhase.STREAMING))
            collectReply(
                frames = executor.executeStreaming(
                    prompt = prompts.reply(validated), model = transport.model, tools = emptyList(),
                ),
                timeouts = configuration.timeouts,
            ) { event(AssistantRunEvent.ReplyDelta(it)) }
        }
    }

    private suspend fun publishDirectReply(
        reply: ValidatedAssistantReply,
        event: suspend (AssistantRunEvent) -> Unit,
    ) {
        event(AssistantRunEvent.ContentReady(reply.content()))
        event(AssistantRunEvent.ReplyDelta(reply.text))
    }
}
