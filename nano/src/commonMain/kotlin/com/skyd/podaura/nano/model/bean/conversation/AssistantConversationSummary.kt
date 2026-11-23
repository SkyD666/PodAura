package com.skyd.podaura.nano.model.bean.conversation

import kotlin.uuid.Uuid

data class AssistantConversationSummary(
    val id: Uuid,
    val title: String,
    val createdAt: Long,
    val activityAt: Long,
    val status: AssistantTurnStatus?,
    val retained: Boolean,
)

internal fun AssistantSessionState.summary() = AssistantConversationSummary(
    id = id,
    title = title,
    createdAt = createdAt,
    activityAt = activityAt,
    status = lastStatus,
    retained = retained,
)
