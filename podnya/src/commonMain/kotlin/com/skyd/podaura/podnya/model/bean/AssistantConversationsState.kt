package com.skyd.podaura.podnya.model.bean

import com.skyd.podaura.podnya.core.AssistantLimits
import kotlin.uuid.Uuid

/** A consistent read projection, never an independently writable copy of the store. */
data class AssistantConversationsState(
    val summaries: List<AssistantConversationSummary> = emptyList(),
    val selected: AssistantSessionState = AssistantSessionState(),
    val runningIds: Set<Uuid> = emptySet(),
    val usedBytes: Long = 0,
    val reservedBytes: Long = 0,
) {
    val capacityWarning: Boolean
        get() = summaries.size * 100 >= AssistantLimits.CONVERSATIONS * AssistantLimits.CAPACITY_WARNING_PERCENT ||
                (usedBytes + reservedBytes) * 100 >= AssistantLimits.CONVERSATION_BYTES.toLong() * AssistantLimits.CAPACITY_WARNING_PERCENT
}
