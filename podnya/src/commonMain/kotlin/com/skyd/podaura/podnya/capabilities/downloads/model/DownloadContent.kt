package com.skyd.podaura.podnya.capabilities.downloads.model

import com.skyd.podaura.api.model.bean.download.AssistantDownloadReceipt
import com.skyd.podaura.podnya.core.AssistantContent
import com.skyd.podaura.podnya.core.AssistantHistorySnapshot
import kotlinx.serialization.Serializable

@Serializable
internal data class DownloadContent(
    val preparing: Boolean = false,
    val originTurnId: Long? = null,
    val choices: List<DownloadChoice> = emptyList(),
    val issues: List<DownloadIssue> = emptyList(),
    val submissions: List<DownloadSubmission> = emptyList(),
    val resolvedLocally: Set<String> = emptySet(),
    val choicesAddedToOrigin: Boolean = false,
) : AssistantContent {
    private val resolvedIds: Set<String>
        get() = submissions
            .filter { it.receipt?.limitExceeded != true }
            .flatMapTo(resolvedLocally.toMutableSet()) { it.choiceIds }

    val availableChoices: List<DownloadChoice>
        get() {
            val taken = resolvedIds
            return choices.filter { it.id !in taken }
        }

    val visibleIssues: List<DownloadIssue>
        get() {
            val taken = resolvedIds
            return issues.filter { it.scopeClarification?.id !in taken }
        }

    val availableScopeClarifications: List<DownloadScopeClarification>
        get() = visibleIssues.mapNotNull { it.scopeClarification }

    val availablePendingIds: Set<String>
        get() = (availableChoices.map { it.id } + availableScopeClarifications.map { it.id }).toSet()

    override val retainedBytes: Int get() = DownloadHistoryCodec.retainedBytes(this)
    override val keepsPreviousLocalSnapshot: Boolean get() = true

    override fun historySnapshot() = AssistantHistorySnapshot(
        capabilityId = DownloadHistoryCodec.capabilityId,
        summary = "Download request. Pending choices or clarifications: ${availablePendingIds.size}. " +
                "Added: ${submissions.sumOf { it.receipt?.added ?: 0 }}. " +
                "Already queued: ${submissions.sumOf { it.receipt?.existing ?: 0 }}. " +
                "Failed: ${submissions.sumOf { it.receipt?.failed ?: 0 }}.",
        payload = "",
    )
}

@Serializable
internal data class DownloadSubmission(
    val id: String,
    val messageId: Long,
    val requestedCount: Int,
    val choiceIds: Set<String> = emptySet(),
    val receipt: DownloadOutcome? = null,
    val state: DownloadSubmissionState = DownloadSubmissionState.SUBMITTING,
)

@Serializable
internal enum class DownloadSubmissionState { SUBMITTING, FINISHED, INTERRUPTED, FAILED }

@Serializable
internal data class DownloadOutcome(
    val added: Int,
    val existing: Int,
    val failed: Int,
    val matchedCount: Int,
    val remainingCount: Int,
    val limitExceeded: Boolean,
) {
    constructor(receipt: AssistantDownloadReceipt) : this(
        added = receipt.queued.size,
        existing = receipt.existing.size,
        failed = receipt.failed.size,
        matchedCount = receipt.matchedCount,
        remainingCount = receipt.remainingCount,
        limitExceeded = receipt.limitExceeded,
    )
}
