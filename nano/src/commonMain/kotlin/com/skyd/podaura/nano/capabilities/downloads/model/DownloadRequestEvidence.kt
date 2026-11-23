package com.skyd.podaura.nano.capabilities.downloads.model

import com.skyd.podaura.nano.core.AssistantRequestEvidence
import com.skyd.podaura.nano.core.AssistantTurnRequest

data class DownloadRequestEvidence(
    val pendingSelections: List<PendingDownloadSelection> = emptyList(),
    val currentTurnId: Long? = null,
    val userMessage: (suspend (Long) -> String?)? = null,
    /** Real subscription URLs stay local. The model receives titles and temporary IDs. */
    val localFeeds: List<DownloadFeedReference> = emptyList(),
    /** Original batch of the immediately preceding download message, if there is one. */
    val latestMessageId: Long? = null,
) : AssistantRequestEvidence

data class PendingDownloadSelection(
    val originTurnId: Long,
    val choices: List<DownloadChoice>,
    val scopeClarifications: List<DownloadScopeClarification> = emptyList(),
)

private val emptyDownloadEvidence = DownloadRequestEvidence()

val AssistantTurnRequest.downloadEvidence: DownloadRequestEvidence
    get() = evidence.filterIsInstance<DownloadRequestEvidence>().singleOrNull()
        ?: emptyDownloadEvidence
