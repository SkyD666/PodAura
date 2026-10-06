package com.skyd.podaura.ui.player.media

import com.skyd.podaura.ui.player.PlayerCommand
import com.skyd.podaura.ui.player.service.PlayerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

internal class DesktopMediaSessionManager(
    playerState: StateFlow<PlayerState>,
    commandSink: (PlayerCommand) -> Unit,
    val adapter: DesktopMediaSessionAdapter,
    artworkLoader: DesktopArtworkLoader,
    private val scope: CoroutineScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main.immediate
    ),
) : AutoCloseable {
    private val controller = DesktopMediaSessionController(
        adapter = adapter,
        artworkLoader = artworkLoader,
        commandSink = commandSink,
        scope = scope,
    )

    // StateFlow supplies the initial state; subscribe before returning to the player session.
    private val stateJob = scope.launch(start = CoroutineStart.UNDISPATCHED) {
        playerState.collect(controller::update)
    }

    override fun close() {
        stateJob.cancel()
        controller.close()
        scope.cancel()
    }
}
