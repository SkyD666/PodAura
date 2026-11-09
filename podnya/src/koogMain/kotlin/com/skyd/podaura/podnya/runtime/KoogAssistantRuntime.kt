package com.skyd.podaura.podnya.runtime

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.dsl.builder.node
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.prompt.message.MessagePart
import ai.koog.prompt.structure.StructuredRequest
import ai.koog.prompt.structure.StructuredRequestConfig
import com.skyd.podaura.podnya.core.AssistantConnectionResult
import com.skyd.podaura.podnya.core.AssistantError
import com.skyd.podaura.podnya.core.AssistantException
import com.skyd.podaura.podnya.core.AssistantLimits
import com.skyd.podaura.podnya.core.AssistantPhase
import com.skyd.podaura.podnya.core.AssistantRunEvent
import com.skyd.podaura.podnya.core.AssistantRuntime
import com.skyd.podaura.podnya.core.AssistantTurnRequest
import com.skyd.podaura.podnya.settings.AssistantConnection
import com.skyd.podaura.podnya.settings.AssistantSettingsRepository
import io.ktor.client.HttpClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow

internal class KoogAssistantRuntime(
    private val capabilities: (AssistantTurnRequest) -> List<KoogCapability>,
    private val connection: suspend () -> AssistantConnection,
    private val httpClient: () -> HttpClient,
) : AssistantRuntime {
    constructor(
        settings: AssistantSettingsRepository,
        capabilities: (AssistantTurnRequest) -> List<KoogCapability>
    ) : this(capabilities, settings::connection, ::createPodnyaHttpClient)

    init {
        initializePodnyaLogging()
    }

    override val available: Boolean = true

    override fun run(request: AssistantTurnRequest): Flow<AssistantRunEvent> =
        assistantRunEvents(request) { event -> executeTurn(request, event) }

    override suspend fun verify(): AssistantConnectionResult =
        PodnyaConnectionProbe(connection, httpClient).verify()

    private suspend fun executeTurn(
        request: AssistantTurnRequest,
        event: suspend (AssistantRunEvent) -> Unit,
    ) {
        val active = capabilities(request)
        if (active.isEmpty()) throw AssistantException(AssistantError.INVALID_OUTPUT)
        require(active.map { it.id }.distinct().size == active.size)
        val toolOwners =
            active.flatMap { capability -> capability.tools.map { it.name to capability } }.toMap()
        require(toolOwners.size == active.sumOf { it.tools.size })
        val prompts = AssistantPrompts(request, active)
        val configuration = connection()
        PodnyaTransport(configuration, httpClient()).use { transport ->
            val executor = BudgetedExecutor(transport.executor)
            var attempts = 0
            var completedCalls = 0
            val graph = strategy<String, ValidatedAssistantReply>("podnya") {
                val useTools by node<String, Unit> {
                    while (attempts < AssistantLimits.TOOL_CALLS) {
                        event(
                            AssistantRunEvent.Progress(AssistantPhase.PROCESSING)
                        )
                        val response = llm.writeSession {
                            if (promptBytes(prompt, tools) > AssistantLimits.PROMPT_BYTES - 8_192) {
                                rewritePrompt { prompts.snapshot() }
                            }
                            requestLLM()
                        }
                        if (response.finishReason == "length" || response.finishReason == "content_filter") {
                            throw AssistantException(AssistantError.INVALID_OUTPUT)
                        }
                        val calls = response.parts.filterIsInstance<MessagePart.Tool.Call>()
                        if (calls.isEmpty()) break
                        val results = calls.map { call ->
                            // Share one budget across all tools, before decoding any arguments.
                            attempts++
                            val error = when {
                                attempts > AssistantLimits.TOOL_CALLS -> "TOOL_BUDGET_EXHAUSTED"
                                call.tool !in toolOwners -> "UNKNOWN_TOOL"
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
                    }
                    if (attempts >= AssistantLimits.TOOL_CALLS && completedCalls == 0) {
                        throw AssistantException(AssistantError.BUDGET_EXHAUSTED)
                    }
                }
                val buildReply by node<Unit, ValidatedAssistantReply> {
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
                toolRegistry = ToolRegistry { active.flatMap { it.tools }.forEach { tool(it) } },
            )
            val validated = try {
                agent.run(request.question)
            } finally {
                agent.close()
            }
            event(AssistantRunEvent.ContentReady(validated.content()))
            event(AssistantRunEvent.Progress(AssistantPhase.STREAMING))
            collectReply(
                executor.executeStreaming(
                    prompt = prompts.reply(validated), model = transport.model, tools = emptyList(),
                )
            ) { event(AssistantRunEvent.ReplyDelta(it)) }
        }
    }
}
