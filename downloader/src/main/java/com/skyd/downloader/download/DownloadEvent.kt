package com.skyd.downloader.download

import com.skyd.downloader.db.DownloadEntity
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow

internal object DownloadEvent {
    private val event = Channel<Event>(Channel.UNLIMITED)
    val eventFlow = event.consumeAsFlow()

    fun sendEvent(e: Event) = event.trySend(e)
}

sealed interface Event {
    data class Success(val entity: DownloadEntity) : Event
    data class Remove(val entity: DownloadEntity) : Event
}