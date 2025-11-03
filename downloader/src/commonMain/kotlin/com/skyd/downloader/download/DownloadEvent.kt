package com.skyd.downloader.download

import com.skyd.downloader.db.DownloadEntity
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.shareIn

internal object DownloadEvent {
    private val event = Channel<Event>(Channel.UNLIMITED)

    @OptIn(DelicateCoroutinesApi::class)
    val eventFlow = event.consumeAsFlow()
        .shareIn(scope = GlobalScope, started = SharingStarted.Eagerly)

    fun sendEvent(e: Event) = event.trySend(e)
}

sealed class Event(open val entity: DownloadEntity) {
    data class Start(override val entity: DownloadEntity) : Event(entity)
    data class Success(override val entity: DownloadEntity) : Event(entity)
    data class Remove(override val entity: DownloadEntity) : Event(entity)
    data class Progress(override val entity: DownloadEntity) : Event(entity)
    data class Paused(override val entity: DownloadEntity) : Event(entity)
    data class Failed(override val entity: DownloadEntity) : Event(entity)
}