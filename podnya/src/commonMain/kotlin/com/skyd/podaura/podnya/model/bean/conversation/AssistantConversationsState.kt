package com.skyd.podaura.podnya.model.bean.conversation

import kotlin.uuid.Uuid

/** Only a bounded summary page, selected message window, and live task identities. */
data class AssistantConversationsState(
    val summaries: List<AssistantConversationSummary> = emptyList(),
    val selected: AssistantSessionState = AssistantSessionState(),
    val runningIds: Set<Uuid> = emptySet(),
    val storage: ConversationStorageState = ConversationStorageState.LOADING,
    val summaryOffset: Int = 0,
    val hasMoreSummaries: Boolean = false,
    val windowRevision: Long = 0,
)
