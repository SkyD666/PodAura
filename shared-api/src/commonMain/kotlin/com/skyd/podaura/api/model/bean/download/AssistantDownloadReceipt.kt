package com.skyd.podaura.api.model.bean.download

import kotlinx.serialization.Serializable

/** Queued and existing entries map target keys to IDs in the application's download queue. */
@Serializable
data class AssistantDownloadReceipt(
    val batchId: String,
    val submissionId: String,
    val queued: Map<String, String>,
    val existing: Map<String, String>,
    val failed: Set<String>,
    val matchedCount: Int,
    val remainingCount: Int,
    val limitExceeded: Boolean,
)
