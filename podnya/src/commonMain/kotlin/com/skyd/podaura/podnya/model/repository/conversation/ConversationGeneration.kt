package com.skyd.podaura.podnya.model.repository.conversation

import com.skyd.podaura.podnya.core.AssistantContent
import com.skyd.podaura.podnya.core.AssistantContext
import com.skyd.podaura.podnya.core.AssistantError
import com.skyd.podaura.podnya.core.AssistantException
import com.skyd.podaura.podnya.core.AssistantHistoryTurn
import com.skyd.podaura.podnya.core.AssistantLimits
import com.skyd.podaura.podnya.core.AssistantPhase
import com.skyd.podaura.podnya.core.AssistantRunEvent
import com.skyd.podaura.podnya.core.AssistantTurnRequest
import com.skyd.podaura.podnya.model.bean.conversation.AssistantSessionTurn
import com.skyd.podaura.podnya.model.bean.conversation.AssistantTurnStatus
import kotlinx.coroutines.Job
import kotlinx.serialization.json.Json

/** One run's identity and result. The repository owns its job and all synchronization. */
internal class ConversationGeneration {
    var job: Job? = null
    var turn: AssistantSessionTurn? = null
    var valid = true

    fun update(event: AssistantRunEvent): AssistantSessionTurn? {
        val turn = turn?.takeIf { it.status == AssistantTurnStatus.RUNNING } ?: return null
        val next = when (event) {
            is AssistantRunEvent.Progress -> turn.copy(progress = event.progress)
            is AssistantRunEvent.ContentReady -> {
                checkContent(event.content)
                turn.copy(content = event.content)
            }

            is AssistantRunEvent.ReplyDelta -> {
                val remaining = (AssistantLimits.REPLY_CHARS - turn.reply.length).coerceAtLeast(0)
                val overflow = event.text.length > remaining
                if (overflow) valid = false
                turn.copy(
                    reply = turn.reply + event.text.take(remaining),
                    status = if (overflow) AssistantTurnStatus.FAILED else turn.status,
                    error = if (overflow) AssistantError.STREAM_INTERRUPTED else turn.error,
                    progress = if (overflow) null else AssistantPhase.STREAMING,
                )
            }

            AssistantRunEvent.Completed -> turn.copy(
                status = AssistantTurnStatus.COMPLETED,
                progress = null
            )

            is AssistantRunEvent.Failed -> {
                event.content?.let(::checkContent)
                turn.copy(
                    status = AssistantTurnStatus.FAILED,
                    progress = null,
                    error = event.error,
                    content = turn.content ?: event.content
                )
            }
        }
        this.turn = next
        return next
    }

    fun request(
        turns: List<AssistantSessionTurn>,
        question: String,
        context: AssistantContext
    ): AssistantTurnRequest {
        var history =
            turns.filter { it.id != turn?.id && it.status != AssistantTurnStatus.RUNNING }
                .mapNotNull { turn ->
                    val snapshot = turn.content?.historySnapshot()
                    when {
                        snapshot != null -> AssistantHistoryTurn(
                            question = turn.question,
                            reply = snapshot.summary.take(AssistantLimits.HISTORY_REPLY_CHARS),
                            content = snapshot
                        )

                        turn.status == AssistantTurnStatus.COMPLETED -> AssistantHistoryTurn(
                            question = turn.question,
                            reply = turn.reply.take(AssistantLimits.HISTORY_REPLY_CHARS)
                        )

                        else -> null
                    }
                }.takeLast(AssistantLimits.HISTORY_TURNS)
        val questionBytes = Json.encodeToString(question).encodeToByteArray().size
        while (history.isNotEmpty() && Json.encodeToString(history)
                .encodeToByteArray().size + questionBytes > AssistantLimits.PROMPT_BYTES
        ) {
            history = history.drop(1)
        }
        return AssistantTurnRequest(question = question, context = context, history = history)
    }

    private fun checkContent(content: AssistantContent) {
        if (content.retainedBytes !in 0..AssistantLimits.CONTENT_BYTES) {
            throw AssistantException(AssistantError.BUDGET_EXHAUSTED)
        }
    }
}
