package com.skyd.podaura.podnya.runtime

import com.skyd.podaura.podnya.core.AssistantError
import com.skyd.podaura.podnya.core.AssistantException
import com.skyd.podaura.podnya.core.AssistantLimits
import com.skyd.podaura.podnya.core.AssistantTimeouts
import com.skyd.podaura.podnya.core.AssistantPhase
import com.skyd.podaura.podnya.core.AssistantRunEvent
import com.skyd.podaura.podnya.core.AssistantTurnRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds

/**
 * Shared request checks, timeout and final events for any capability.
 * The block sends progress, content and reply text; this function sends Completed or Failed.
 */
internal fun assistantRunEvents(
    request: AssistantTurnRequest,
    timeouts: AssistantTimeouts = AssistantTimeouts.STANDARD,
    execute: suspend (event: suspend (AssistantRunEvent) -> Unit) -> Unit,
): Flow<AssistantRunEvent> = channelFlow {
    var published = false
    try {
        send(AssistantRunEvent.Progress(AssistantPhase.PREPARING))
        val finished = withTimeoutOrNull(timeouts.turnMillis.milliseconds) {
            if (request.question.isBlank() || request.question.length > AssistantLimits.INPUT_CHARS) {
                throw AssistantException(AssistantError.INVALID_OUTPUT)
            }
            execute { event ->
                if (event is AssistantRunEvent.ContentReady ||
                    event is AssistantRunEvent.ReplyDelta && event.text.isNotEmpty()
                ) published = true
                send(event)
            }
            true
        }
        if (finished != true) throw AssistantException(
            if (published) AssistantError.STREAM_INTERRUPTED else AssistantError.TIMEOUT,
        )
        send(AssistantRunEvent.Completed)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        currentCoroutineContext().ensureActive()
        send(
            AssistantRunEvent.Failed(
                if (published) AssistantError.STREAM_INTERRUPTED else failure.assistantError(),
            )
        )
    }
}
