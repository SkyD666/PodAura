package com.skyd.podaura.podnya.model.db

import com.skyd.podaura.podnya.core.AssistantAnswerKind
import com.skyd.podaura.podnya.core.AssistantContent
import com.skyd.podaura.podnya.core.AssistantContext
import com.skyd.podaura.podnya.core.AssistantError
import com.skyd.podaura.podnya.core.AssistantHistoryCodec
import com.skyd.podaura.podnya.core.CapabilityRecord
import com.skyd.podaura.podnya.core.UnreadableContent
import com.skyd.podaura.podnya.core.UnreadableContext
import com.skyd.podaura.podnya.model.bean.AssistantReplyContent
import com.skyd.podaura.podnya.model.bean.conversation.AssistantReplySource
import com.skyd.podaura.podnya.model.bean.conversation.AssistantSessionState
import com.skyd.podaura.podnya.model.bean.conversation.AssistantSessionTurn
import com.skyd.podaura.podnya.model.bean.conversation.AssistantTurnStatus
import com.skyd.podaura.podnya.model.bean.conversation.ConversationPosition
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.uuid.Uuid

class ConversationRecordCodec(private val capabilities: List<AssistantHistoryCodec>) {
    private val json = Json { ignoreUnknownKeys = true }

    // CapabilityRecord starts with its serialized ID; the prefix works with Android's platform SQLite.
    internal fun contentPrefix(capabilityId: String): String =
        "{\"capabilityId\":${json.encodeToString(capabilityId)},"

    @Serializable
    private data class Aggregate(
        val kind: AssistantAnswerKind,
        val text: String,
        val parts: List<String>,
        val reference: String? = null,
    )

    fun content(value: AssistantContent?): String? = when (value) {
        null -> null
        is UnreadableContent -> value.raw
        is AssistantReplyContent -> json.encodeToString(
            CapabilityRecord(
                capabilityId = "assistant",
                version = 1,
                payload = json.encodeToString(
                    Aggregate(
                        kind = value.kind,
                        text = value.text,
                        parts = value.parts.mapNotNull { content(it) },
                        reference = content(value.reference),
                    )
                ),
                fallback = value.text,
            )
        )

        else -> json.encodeToString(capabilities.firstNotNullOfOrNull { it.encodeContent(value) }
            ?: error("Unregistered conversation content"))
    }

    fun content(raw: String?): AssistantContent? {
        if (raw == null) return null
        val record = runCatching { json.decodeFromString<CapabilityRecord>(raw) }.getOrNull()
            ?: return UnreadableContent(raw = raw, fallback = "")
        return runCatching {
            if (record.capabilityId == "assistant" && record.version == 1) {
                val aggregate = json.decodeFromString<Aggregate>(record.payload)
                AssistantReplyContent(
                    parts = aggregate.parts.mapNotNull { content(it) },
                    kind = aggregate.kind,
                    text = aggregate.text,
                    reference = content(aggregate.reference),
                )
            } else {
                capabilities.first { it.capabilityId == record.capabilityId }.decodeContent(record)
            }
        }.getOrElse { UnreadableContent(raw = raw, fallback = record.fallback) }
    }

    fun context(value: AssistantContext): String = when (value) {
        AssistantContext.Empty -> ""
        is UnreadableContext -> value.raw
        else -> json.encodeToString(capabilities.firstNotNullOfOrNull { it.encodeContext(value) }
            ?: error("Unregistered conversation context"))
    }

    fun context(raw: String): AssistantContext {
        if (raw.isEmpty()) return AssistantContext.Empty
        return runCatching {
            val record = json.decodeFromString<CapabilityRecord>(raw)
            capabilities.first { it.capabilityId == record.capabilityId }.decodeContext(record)
        }.getOrElse { UnreadableContext(raw) }
    }

    fun encode(value: AssistantSessionState) = ConversationRecord(
        conversationId = value.id.toString(),
        title = value.title,
        manuallyNamed = value.manuallyNamed,
        createdAt = value.createdAt,
        activityAt = value.activityAt,
        draft = value.draft,
        editorId = value.editorId,
        draftRevision = value.draftRevision,
        selectionStart = value.selectionStart,
        selectionEnd = value.selectionEnd,
        context = context(value.context),
        modelConfigId = value.modelConfigId,
        destinations = value.rawDestinations ?: json.encodeToString(value.destinations),
        position = value.rawPosition ?: json.encodeToString(value.position),
        lastTurnId = value.lastTurnId,
        lastStatus = value.lastStatus?.name,
        retained = value.retained,
    )

    fun decode(value: ConversationRecord): AssistantSessionState {
        val destinations =
            runCatching { json.decodeFromString<Set<String>>(value.destinations) }.getOrNull()
        val position =
            runCatching { json.decodeFromString<ConversationPosition>(value.position) }.getOrNull()
        return AssistantSessionState(
            id = Uuid.parse(value.conversationId),
            title = value.title,
            manuallyNamed = value.manuallyNamed,
            createdAt = value.createdAt,
            activityAt = value.activityAt,
            draft = value.draft,
            editorId = value.editorId,
            draftRevision = value.draftRevision,
            selectionStart = value.selectionStart.coerceIn(0, value.draft.length),
            selectionEnd = value.selectionEnd.coerceIn(0, value.draft.length),
            context = context(value.context),
            modelConfigId = value.modelConfigId,
            destinations = destinations.orEmpty(),
            rawDestinations = value.destinations.takeIf { destinations == null },
            position = position ?: ConversationPosition(),
            rawPosition = value.position.takeIf { position == null },
            lastTurnId = value.lastTurnId,
            lastStatus = value.lastStatus?.let { status(it) },
        )
    }

    fun encode(id: Uuid, value: AssistantSessionTurn) = ConversationTurnRecord(
        conversationId = id.toString(),
        turnId = value.id,
        question = value.question,
        createdAt = value.createdAt,
        source = value.rawSource ?: value.source?.let { json.encodeToString(it) },
        reply = value.reply,
        content = content(value.content),
        context = context(value.context),
        status = value.status.name,
        error = value.rawError ?: value.error?.let { failure ->
            if (failure is AssistantError) "core:${failure.name}"
            else capabilities.firstNotNullOfOrNull { it.encodeFailure(failure) }
                ?: error("Unregistered conversation failure")
        },
    )

    fun decode(value: ConversationTurnRecord): AssistantSessionTurn {
        val source =
            value.source?.let { runCatching { json.decodeFromString<AssistantReplySource>(it) }.getOrNull() }
        val error = value.error?.let { raw ->
            runCatching { AssistantError.valueOf(raw.removePrefix("core:")) }.getOrNull()
                ?: capabilities.firstNotNullOfOrNull { it.decodeFailure(raw) }
        }
        return AssistantSessionTurn(
            id = value.turnId,
            question = value.question,
            createdAt = value.createdAt,
            source = source,
            rawSource = value.source?.takeIf { raw ->
                source == null || json.parseToJsonElement(raw).jsonObject.keys.any {
                    it !in setOf(
                        "configId",
                        "revision",
                        "baseUrl",
                        "modelId",
                        "protocol",
                        "reasoningEffort",
                        "profileName",
                    )
                }
            },
            reply = value.reply,
            content = content(value.content),
            context = context(value.context),
            status = status(value.status),
            error = error ?: value.error?.let { AssistantError.INVALID_OUTPUT },
            rawError = value.error.takeIf { error == null },
            progress = null,
        )
    }

    private fun status(value: String) = runCatching { AssistantTurnStatus.valueOf(value) }
        .getOrDefault(AssistantTurnStatus.INTERRUPTED)
}
