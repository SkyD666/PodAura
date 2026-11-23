package com.skyd.podaura.nano.runtime

import ai.koog.agents.core.tools.Tool
import ai.koog.prompt.message.MessagePart
import ai.koog.prompt.structure.json.JsonStructure
import ai.koog.serialization.typeToken
import com.skyd.podaura.nano.core.AssistantAction
import com.skyd.podaura.nano.core.AssistantAnswerKind
import com.skyd.podaura.nano.core.AssistantContent
import com.skyd.podaura.nano.core.AssistantError
import com.skyd.podaura.nano.core.AssistantException
import com.skyd.podaura.nano.core.AssistantHistorySnapshot
import com.skyd.podaura.nano.core.AssistantRunEvent
import com.skyd.podaura.nano.core.AssistantTurnRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** A third capability with scalar output proves the host is not tied to either search domain. */
class CapabilityExtensionTest {
    @Test
    fun addingAnActionProposalNeedsNoSearchFieldsOrRuntimeBranches() = runTest { withContext(Dispatchers.Default) {
        val capability = VolumeProposalCapability()
        val script = HttpScript(
            chatToolCalls("prepare_volume", "v" to "{\"level\":40}"), chatCompletion("Prepared"),
            chatCompletion("""{"kind":"RESULTS","text":"","outputs":{"volume":{"level":40}}}"""), chatSse(),
        )
        val runtime = KoogAssistantRuntime({ listOf(capability) }, script::client)
        val events = runtime.run(AssistantTurnRequest("Set volume to 40"), com.skyd.podaura.nano.runtime.testConnection()).toList()
        assertEquals(AssistantRunEvent.Completed, events.last())
        val content = events.filterIsInstance<AssistantRunEvent.ContentReady>().single().content as VolumeProposal
        assertEquals(40, content.action.level)
        // The model can only prepare a proposal. The host decides how a user confirms and executes it.
        assertEquals(10, capability.currentVolume)
        assertTrue(script.requests[2].toString().contains("level"))
        assertFalse(script.requests.toString().contains("search_articles"))
        assertFalse(script.requests.toString().contains("search_feeds"))
        assertTrue(script.requests.last().toString().contains("AWAITING_CONFIRMATION"))
        val tool = script.requests.first().getValue("tools").jsonArray.single().jsonObject
        assertEquals("prepare_volume", tool.getValue("function").jsonObject.getValue("name").jsonPrimitive.content)
    } }

    @Test
    fun outputRequiresARegisteredCapabilityAndItsCheckedEvidence() = runTest {
        val capability = VolumeProposalCapability()
        val output = buildJsonObject { put("level", 80) }
        assertEquals(AssistantError.INVALID_OUTPUT, assertFailsWith<AssistantException> {
            validateOutput(AssistantOutput(AssistantAnswerKind.RESULTS, "", mapOf("unknown" to output)), listOf(capability))
        }.error)
        assertEquals(AssistantError.INVALID_OUTPUT, assertFailsWith<AssistantException> {
            validateOutput(AssistantOutput(AssistantAnswerKind.RESULTS, "", mapOf("volume" to output)), listOf(capability))
        }.error)
        assertEquals(10, capability.currentVolume)
    }

    private class VolumeProposalCapability : KoogCapability {
        val currentVolume = 10
        private var prepared: Int? = null
        private val tool = object : Tool<VolumeSelection, VolumeSelection>(
            typeToken<VolumeSelection>(), typeToken<VolumeSelection>(), "prepare_volume", "Prepare a volume change for user confirmation."
        ) {
            override suspend fun execute(args: VolumeSelection): VolumeSelection {
                prepared = args.level
                return args
            }
        }
        override val id = "volume"
        override val tools = listOf(tool)
        override val instructions = "Prepare the requested volume; do not change playback."
        override val selectionInstructions = "Return the prepared level. This is a proposal, not an executed operation."
        override val selectionSchema = JsonStructure.create<VolumeSelection>().schema.schema
        override val failure = null
        override fun admit(call: MessagePart.Tool.Call): MessagePart.Tool.Result? {
            val args = runCatching { nanoJson.decodeFromString<VolumeSelection>(call.args) }.getOrNull()
            return if (args != null && args.level in 0..100) null else
                MessagePart.Tool.Result(call.id, call.tool, "INVALID_ARGUMENT", isError = true)
        }
        override fun snapshot(textLimit: Int) = buildJsonObject { prepared?.let { put("preparedLevel", it) } }
        override fun history(snapshot: AssistantHistorySnapshot, textLimit: Int) = null
        override suspend fun validate(kind: AssistantAnswerKind, output: JsonObject): CapabilityResult {
            val result = nanoJson.decodeFromJsonElement<VolumeSelection>(output)
            if (kind != AssistantAnswerKind.RESULTS || result.level != prepared) throw AssistantException(AssistantError.INVALID_OUTPUT)
            return CapabilityResult(VolumeProposal(ConfirmVolume(result.level)), buildJsonObject {
                put("status", "AWAITING_CONFIRMATION"); put("level", result.level)
            }, itemCount = 1)
        }
    }

    private data class ConfirmVolume(val level: Int) : AssistantAction
    private data class VolumeProposal(val action: ConfirmVolume) : AssistantContent {
        override val retainedBytes = 64
        override fun historySnapshot() = AssistantHistorySnapshot("volume", "Awaiting confirmation",
            nanoJson.encodeToString(VolumeSelection(action.level)))
    }
}

@Serializable
private data class VolumeSelection(val level: Int)
