package com.skyd.podaura.podnya.model.bean

import kotlin.uuid.Uuid

data class AssistantConversationSummary(
    val id: Uuid,
    val title: String,
    val createdAt: Long,
    val activityAt: Long,
    val status: AssistantTurnStatus?,
    val retained: Boolean,
    val retainedBytes: Long,
)

internal fun AssistantSessionState.summary() = AssistantConversationSummary(
    id = id,
    title = title,
    createdAt = createdAt,
    activityAt = activityAt,
    status = turns.lastOrNull()?.status,
    retained = retained,
    retainedBytes = retainedBytes,
)
