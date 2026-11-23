package com.skyd.podaura.nano.core

import kotlinx.serialization.Serializable

/** Disk format, deliberately independent of the model's history snapshot. */
@Serializable
data class CapabilityRecord(
    val capabilityId: String,
    val version: Int,
    val payload: String,
    val fallback: String,
)

interface AssistantHistoryCodec {
    val capabilityId: String
    fun encodeContent(content: AssistantContent): CapabilityRecord?
    fun decodeContent(record: CapabilityRecord): AssistantContent
    fun encodeFailure(error: AssistantFailure): String? = null
    fun decodeFailure(value: String): AssistantFailure? = null
    fun encodeContext(context: AssistantContext): CapabilityRecord? = null
    fun decodeContext(record: CapabilityRecord): AssistantContext = error("Unsupported context")
}

/** Keep the exact bytes on edits; unknown records never grant navigation or search authority. */
data class UnreadableContent(val raw: String, val fallback: String) : AssistantContent {
    override val retainedBytes: Int get() = raw.encodeToByteArray().size
    override fun historySnapshot(): AssistantHistorySnapshot? = null
}

data class UnreadableContext(val raw: String) : AssistantContext {
    override val retainedBytes: Int get() = raw.encodeToByteArray().size
    override fun snapshot() = this
}
