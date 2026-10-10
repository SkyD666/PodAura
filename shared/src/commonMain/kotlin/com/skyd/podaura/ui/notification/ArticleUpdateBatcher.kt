package com.skyd.podaura.ui.notification

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.seconds

/** A refresh's delivery outcome travels with its child coroutines, including database saves. */
internal class ArticleUpdateDelivery : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<ArticleUpdateDelivery>

    // Read and written only by the batcher's event loop; flush never resets another caller's result.
    internal var succeeded = true
}

/** Serializes article delivery and lifecycle flushes, including native notification submission. */
internal class ArticleUpdateBatcher(
    scope: CoroutineScope,
    private val deliver: suspend (List<String>) -> Unit,
) {
    private sealed interface Event {
        data class Articles(val ids: List<String>, val delivery: ArticleUpdateDelivery?) : Event
        data class Flush(
            val delivery: ArticleUpdateDelivery?,
            val completed: CompletableDeferred<Boolean>
        ) : Event

        data class Immediate(val enabled: Boolean) : Event
    }

    private val events = Channel<Event>(Channel.UNLIMITED)

    init {
        scope.launch {
            val pending = linkedSetOf<String>()
            var immediate = false
            val pendingDeliveries = mutableSetOf<ArticleUpdateDelivery>()
            suspend fun sendPending() {
                if (pending.isEmpty()) return
                val ids = pending.toList()
                pending.clear()
                val deliveries = pendingDeliveries.toList()
                pendingDeliveries.clear()
                try {
                    deliver(ids)
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    deliveries.forEach { it.succeeded = false }
                    Logger.e("Article update delivery failed", error, "ArticleUpdatedManager")
                }
            }
            while (isActive) {
                val event = if (pending.isEmpty()) events.receive() else {
                    withTimeoutOrNull(20.seconds) { events.receive() }
                }
                when (event) {
                    is Event.Articles -> {
                        pending += event.ids
                        event.delivery?.let { pendingDeliveries += it }
                        if (immediate) sendPending()
                    }

                    is Event.Immediate -> {
                        immediate = event.enabled
                        if (immediate) sendPending()
                    }

                    is Event.Flush -> {
                        sendPending()
                        event.completed.complete(event.delivery?.succeeded ?: true)
                    }

                    null -> sendPending()
                }
            }
        }
    }

    suspend fun send(ids: List<String>) {
        if (ids.isNotEmpty()) {
            events.trySend(
                Event.Articles(
                    ids.toList(),
                    currentCoroutineContext()[ArticleUpdateDelivery]
                )
            )
                .getOrThrow()
        }
    }

    fun setImmediateDelivery(enabled: Boolean) {
        events.trySend(Event.Immediate(enabled)).getOrThrow()
    }

    /** Waits until earlier events have been processed; this is not a delivery success result. */
    suspend fun flush() {
        awaitDelivery(null)
    }

    /** Waits for queued work and returns only this refresh's outcome, without consuming it. */
    suspend fun flush(delivery: ArticleUpdateDelivery): Boolean = awaitDelivery(delivery)

    private suspend fun awaitDelivery(delivery: ArticleUpdateDelivery?): Boolean {
        val completed = CompletableDeferred<Boolean>()
        events.send(Event.Flush(delivery, completed))
        return completed.await()
    }
}
