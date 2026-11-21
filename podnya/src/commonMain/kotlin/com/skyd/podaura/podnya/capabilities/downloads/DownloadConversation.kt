package com.skyd.podaura.podnya.capabilities.downloads

import com.skyd.podaura.api.model.bean.download.AssistantDownloadReceipt
import com.skyd.podaura.api.model.bean.download.AssistantDownloadTarget
import com.skyd.podaura.api.model.repository.IAssistantDownloadRepository
import com.skyd.podaura.podnya.capabilities.SubscriptionHistory
import com.skyd.podaura.podnya.capabilities.downloads.model.DownloadContent
import com.skyd.podaura.podnya.capabilities.downloads.model.DownloadFeedReference
import com.skyd.podaura.podnya.capabilities.downloads.model.DownloadHistoryCodec
import com.skyd.podaura.podnya.capabilities.downloads.model.DownloadIssue
import com.skyd.podaura.podnya.capabilities.downloads.model.DownloadIssueKind
import com.skyd.podaura.podnya.capabilities.downloads.model.DownloadOutcome
import com.skyd.podaura.podnya.capabilities.downloads.model.DownloadRequestEvidence
import com.skyd.podaura.podnya.capabilities.downloads.model.DownloadSubmission
import com.skyd.podaura.podnya.capabilities.downloads.model.DownloadSubmissionState
import com.skyd.podaura.podnya.capabilities.downloads.model.PendingDownloadSelection
import com.skyd.podaura.podnya.capabilities.downloads.selection.ValidatedDownloadPlan
import com.skyd.podaura.podnya.core.AssistantAction
import com.skyd.podaura.podnya.core.AssistantContent
import com.skyd.podaura.podnya.core.AssistantError
import com.skyd.podaura.podnya.core.AssistantException
import com.skyd.podaura.podnya.core.AssistantHandoff
import com.skyd.podaura.podnya.core.AssistantLimits
import com.skyd.podaura.podnya.model.bean.AssistantReplyContent
import com.skyd.podaura.podnya.model.bean.conversation.AssistantSessionTurn
import com.skyd.podaura.podnya.model.bean.conversation.AssistantTurnStatus
import com.skyd.podaura.podnya.model.repository.conversation.ConversationAccess
import com.skyd.podaura.podnya.model.repository.conversation.ConversationAdmission
import com.skyd.podaura.podnya.model.repository.conversation.ConversationCapability
import com.skyd.podaura.podnya.model.repository.conversation.ConversationHistory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.isActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data object OpenDownloads : AssistantAction

internal data class SelectDownloadOption(
    val turnId: Long,
    val choiceId: String,
    val optionId: String
) : AssistantAction

internal data class RefreshDownloadStatus(val turnId: Long) : AssistantAction

/** Owns download choices, batch submissions and the receipt projection into conversation history. */
internal class DownloadConversation(
    private val downloads: IAssistantDownloadRepository,
) : ConversationCapability {
    private val jobs = mutableMapOf<Pair<String, String>, Job>()

    override suspend fun evidence(
        conversation: ConversationHistory,
        history: List<AssistantSessionTurn>,
        currentTurnId: Long,
    ): DownloadRequestEvidence {
        val previous = history.lastOrNull { it.id != currentTurnId }
        val pending = pendingSelections(conversation, currentTurnId)
        val recent = conversation.recentTurns(limit = HISTORY_PAGE_SIZE, throughTurnId = currentTurnId - 1).asReversed()
        return DownloadRequestEvidence(
            pendingSelections = pending,
            latestMessageId = previous?.downloadOriginId(),
            currentTurnId = currentTurnId,
            userMessage = { id -> if (id in 1 until currentTurnId) conversation.turn(id)?.question else null },
            localFeeds = recent.flatMap { feedEvidence(it.content) }.distinctBy { it.feedUrl },
        )
    }

    private fun feedEvidence(content: AssistantContent?): List<DownloadFeedReference> =
        when (content) {
            is SubscriptionHistory -> content.subscriptions()
            is AssistantReplyContent -> content.parts.flatMap(::feedEvidence)
            else -> emptyList()
        }

    private fun AssistantSessionTurn.downloadOriginId(): Long? =
        (content as? DownloadContent)?.let { it.originTurnId ?: id }

    private suspend fun pendingSelections(
        conversation: ConversationHistory,
        currentTurnId: Long,
    ): List<PendingDownloadSelection> {
        val pending = mutableListOf<PendingDownloadSelection>()
        var before = currentTurnId
        do {
            val page = conversation.recentCapabilityTurns(
                capabilityId = DownloadHistoryCodec.capabilityId,
                beforeTurnId = before,
                limit = HISTORY_PAGE_SIZE,
            )
            for (message in page.asReversed()) {
                val content = message.content as? DownloadContent ?: continue
                val choices = content.availableChoices
                val scopes = content.availableScopeClarifications
                if (choices.isNotEmpty() || scopes.isNotEmpty()) {
                    pending += PendingDownloadSelection(
                        originTurnId = message.id,
                        choices = choices,
                        scopeClarifications = scopes,
                    )
                }
            }
            before = page.firstOrNull()?.id ?: break
        } while (page.size == HISTORY_PAGE_SIZE)
        return pending.asReversed()
    }

    override suspend fun accept(
        handoff: AssistantHandoff,
        admission: ConversationAdmission
    ): Boolean {
        if (handoff !is ValidatedDownloadPlan) return false
        acceptPlan(plan = handoff, admission = admission)
        return true
    }

    private suspend fun acceptPlan(plan: ValidatedDownloadPlan, admission: ConversationAdmission) {
        val conversation = admission.conversation
        val messageId = admission.turn.id
        val originTurn = plan.originTurnId?.let { conversation.turn(it) }
        val originContent = originTurn?.content as? DownloadContent
        val stale = plan.originTurnId != null && originContent?.availablePendingIds
            ?.containsAll(plan.resolvedChoiceIds) != true
        val targets = if (stale) emptyList() else plan.targets
        val attempt = if (targets.isEmpty()) null else DownloadSubmission(
            id = "turn:$messageId",
            messageId = messageId,
            requestedCount = targets.distinctBy { it.key }.size,
            choiceIds = plan.resolvedChoiceIds,
        )
        val visibleIssues = if (stale || originContent != null) {
            plan.issues.map { it.copy(scopeClarification = null) }
        } else plan.issues
        val content = DownloadContent(
            originTurnId = plan.originTurnId.takeUnless { stale },
            choices = if (stale || originContent != null) emptyList() else plan.choices,
            issues = if (stale && visibleIssues.none { it.kind == DownloadIssueKind.STALE_CHOICE }) {
                visibleIssues + DownloadIssue(title = "", kind = DownloadIssueKind.STALE_CHOICE)
            } else visibleIssues,
            submissions = listOfNotNull(attempt),
            choicesAddedToOrigin = !stale && originContent != null && plan.choices.isNotEmpty(),
        )
        val updatedOrigin =
            if (!stale && originContent != null && plan.resolvedChoiceIds.isNotEmpty()) {
                val updated = originContent.copy(
                    choices = originContent.choices + plan.choices,
                    issues = originContent.issues + plan.issues.filter { it.scopeClarification != null },
                    submissions = originContent.submissions + listOfNotNull(attempt),
                    resolvedLocally = if (attempt == null) {
                        originContent.resolvedLocally + plan.resolvedChoiceIds
                    } else originContent.resolvedLocally,
                )
                checkContent(updated)
                originTurn.copy(content = updated)
            } else null
        checkContent(content)
        withContext(NonCancellable) {
            if (attempt != null && !admission.checkpoint(DownloadContent(preparing = true))) {
                return@withContext
            }
            if (!admission.commit(content = content, updatedTurns = listOfNotNull(updatedOrigin))) {
                return@withContext
            }
            if (attempt != null) {
                launchSubmission(
                    conversation = conversation,
                    originId = content.originTurnId ?: messageId,
                    attempt = attempt,
                    targets = targets,
                )
            }
        }
    }

    override fun handles(action: AssistantAction) =
        action is SelectDownloadOption || action is RefreshDownloadStatus

    override suspend fun perform(conversation: ConversationAccess, action: AssistantAction) {
        when (action) {
            is SelectDownloadOption -> selectOption(conversation, action)
            is RefreshDownloadStatus -> conversation.turn(action.turnId)
                ?.let { conversation.refresh(it) }
        }
    }

    private suspend fun selectOption(
        conversation: ConversationAccess,
        action: SelectDownloadOption
    ) {
        if (!conversation.scope.isActive) return
        val turn = conversation.turn(action.turnId) ?: return
        val content = turn.content as? DownloadContent ?: return
        if (content.originTurnId != null) return
        val attemptId = "option:${action.choiceId}:${action.optionId}"
        if (content.submissions.any { it.id == attemptId }) {
            conversation.refresh(turn)
            return
        }
        val choice = content.availableChoices.firstOrNull { it.id == action.choiceId } ?: return
        val option = choice.options.firstOrNull { it.id == action.optionId } ?: return
        if (option.targets.isEmpty()) return
        val attempt = DownloadSubmission(
            id = attemptId,
            messageId = turn.id,
            requestedCount = option.targets.distinctBy { it.key }.size,
            choiceIds = setOf(choice.id),
        )
        val nextContent = content.copy(submissions = content.submissions + attempt)
        checkContent(nextContent)
        val changes = listOf(turn.copy(content = nextContent))
        withContext(NonCancellable) {
            if (conversation.updateTurns(changes)) {
                launchSubmission(
                    conversation = conversation,
                    originId = turn.id,
                    attempt = attempt,
                    targets = option.targets
                )
            } else {
                conversation.reject(AssistantError.CONVERSATION_STORAGE)
            }
        }
    }

    private fun launchSubmission(
        conversation: ConversationAccess,
        originId: Long,
        attempt: DownloadSubmission,
        targets: List<AssistantDownloadTarget>,
    ) {
        val batch = "${conversation.id}:$originId"
        val key = batch to attempt.id
        val previous = jobs.filterKeys { it.first == batch }.values.toList()
        val job = conversation.scope.launch(start = CoroutineStart.LAZY) {
            try {
                previous.joinAll()
                val result = try {
                    attempt.completed(
                        downloads.submit(
                            batchId = batch,
                            submissionId = attempt.id,
                            targets = targets
                        )
                    )
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    readReceipt(batch, attempt, missingState = DownloadSubmissionState.FAILED)
                }
                conversation.write {
                    val changed =
                        listOf(originId, attempt.messageId).distinct().mapNotNull { turnId ->
                            val turn = conversation.turn(turnId = turnId, allowCached = true)
                                ?: return@mapNotNull null
                            val content = turn.content as? DownloadContent ?: return@mapNotNull null
                            val submissions =
                                content.submissions.map { if (it.id == attempt.id) result else it }
                            turn.copy(content = content.copy(submissions = submissions))
                        }
                    if (changed.isNotEmpty()) {
                        conversation.updateTurns(turns = changed, showOnSaveFailure = true)
                    }
                }
            } finally {
                withContext(NonCancellable) { conversation.write { jobs.remove(key) } }
            }
        }
        jobs[key] = job
        job.start()
    }

    override suspend fun restore(
        conversation: ConversationAccess,
        turn: AssistantSessionTurn
    ): AssistantSessionTurn {
        val content = turn.content as? DownloadContent ?: return turn
        if (content.preparing) return turn.copy(
            content = null,
            status = AssistantTurnStatus.STOPPED,
            progress = null,
            error = null,
            rawError = null,
        )
        val batch = "${conversation.id}:${content.originTurnId ?: turn.id}"
        val submissions = content.submissions.map { attempt ->
            if (attempt.receipt != null || (batch to attempt.id) in jobs) {
                attempt
            } else {
                val missing = if (attempt.state == DownloadSubmissionState.FAILED) {
                    DownloadSubmissionState.FAILED
                } else DownloadSubmissionState.INTERRUPTED
                readReceipt(batch = batch, attempt = attempt, missingState = missing)
            }
        }
        if (submissions == content.submissions) return turn
        return turn.copy(content = content.copy(submissions = submissions))
    }

    override suspend fun retry(
        conversation: ConversationAccess,
        turn: AssistantSessionTurn
    ): Boolean {
        val content = turn.content as? DownloadContent ?: return false
        return !content.preparing && content.submissions.isNotEmpty()
    }

    private suspend fun readReceipt(
        batch: String,
        attempt: DownloadSubmission,
        missingState: DownloadSubmissionState,
    ): DownloadSubmission = try {
        val receipt = downloads.receipt(batchId = batch, submissionId = attempt.id)
        if (receipt == null) attempt.copy(state = missingState) else attempt.completed(receipt)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        attempt.copy(state = DownloadSubmissionState.INTERRUPTED)
    }

    private fun DownloadSubmission.completed(receipt: AssistantDownloadReceipt) = copy(
        receipt = DownloadOutcome(receipt),
        state = DownloadSubmissionState.FINISHED,
    )

    private fun checkContent(content: DownloadContent) {
        // Leave space for receipts written after the submission marker.
        if (content.retainedBytes > AssistantLimits.CONTENT_BYTES - 32 * 1024) {
            throw AssistantException(AssistantError.BUDGET_EXHAUSTED)
        }
    }
}

private const val HISTORY_PAGE_SIZE = 40
