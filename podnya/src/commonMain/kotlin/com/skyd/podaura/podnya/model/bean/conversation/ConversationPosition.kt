package com.skyd.podaura.podnya.model.bean.conversation

import kotlinx.serialization.Serializable

/** Stable LazyColumn item key, plus a turn fallback. No Compose objects cross this boundary. */
@Serializable
data class ConversationPosition(
    val turnId: Long? = null,
    val itemKey: String? = null,
    val offset: Int = 0,
    val followLatest: Boolean = true,
)

data class ConversationWorkspace(
    val selectedId: kotlin.uuid.Uuid? = null,
    val lastModelId: String? = null,
)

data class ConversationWindow(
    val turns: List<AssistantSessionTurn> = emptyList(),
    val hasBefore: Boolean = false,
    val hasAfter: Boolean = false,
)
