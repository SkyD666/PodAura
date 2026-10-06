package com.skyd.podaura.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.rememberNavigationEventDispatcherOwner
import co.touchlab.kermit.Logger
import com.skyd.fundation.di.get
import com.skyd.podaura.ext.flowOf
import com.skyd.podaura.ext.getOrDefault
import com.skyd.podaura.model.preference.dataStore
import com.skyd.podaura.model.preference.player.BackgroundPlayPreference
import com.skyd.podaura.ui.component.ComposeWindow
import com.skyd.podaura.ui.component.Window
import com.skyd.podaura.ui.player.coordinator.PlayerCoordinator
import com.skyd.podaura.ui.player.media.MacosMediaSession
import com.skyd.podaura.ui.screen.SettingsProvider
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import platform.AppKit.NSWindow
import platform.Foundation.NSMakePoint

internal val LocalMacosPlayerSession = staticCompositionLocalOf<MacosPlayerSession> {
    error("macOS player session is not installed")
}

/** Application-owned session: closing the video window need not end audio playback. */
internal class MacosPlayerSession : PlayerSession {
    var mainWindow: NSWindow? = null
    private var playerWindow: ComposeWindow? = null
    private var mediaSession: MacosMediaSession? = null
    private val viewModel = get<PlayerViewModel>()
    private val articleContext = get<PlayerArticleContextViewModel>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val cleanupJob = SupervisorJob(scope.coroutineContext[Job])
    private val cleanupScope = CoroutineScope(scope.coroutineContext + cleanupJob)
    private var shuttingDown = false
    override var coordinator by mutableStateOf<PlayerCoordinator?>(null)
        private set
    override var isFullPlayerVisible by mutableStateOf(false)
        private set
    private val entry = PlatformPlayerEntry(::openAccepted) {
        destroySession()
        mainWindow?.makeKeyAndOrderFront(null)
    }

    init {
        scope.launch {
            viewModel.mediaInfos.collect { coordinator?.onCommand(it.toLoadCommand()) }
        }
        scope.launch {
            dataStore.flowOf(BackgroundPlayPreference).collect { enabled ->
                if (!enabled && !isFullPlayerVisible) destroySession()
            }
        }
    }

    fun open(request: PlayerOpenRequest) {
        if (!shuttingDown) entry.open(request)
    }

    override fun openFullPlayer() = open(PlayerOpenRequest.Resume)

    private fun openAccepted(request: PlayerOpenRequest) {
        if (request == PlayerOpenRequest.Resume && coordinator == null) return
        val player = coordinator ?: PlayerCoordinator().also { created ->
            coordinator = created
            mediaSession = runCatching { MacosMediaSession(created) }
                .onFailure { throwable ->
                    Logger.e(throwable = throwable, tag = "MacosPlayerSession") {
                        "Could not initialize macOS system media controls"
                    }
                }.getOrNull()
            created.lifecycle.addObserver(object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    if (coordinator === created) destroySession()
                }
            })
        }
        when (request) {
            is PlayerOpenRequest.Media -> viewModel.handlePlayDataMode(
                request.mode,
                request.requestId
            )

            is PlayerOpenRequest.Files -> viewModel.handlePlatformFiles(
                request.files,
                request.requestId
            )

            PlayerOpenRequest.Resume -> Unit
        }
        isFullPlayerVisible = true
        if (playerWindow == null) {
            playerWindow = Window(
                title = "Player",
                size = DpSize(400.dp, 780.dp),
                transparent = true,
                onKeyEvent = player::onKey,
                onClose = {
                    playerWindow = null
                    isFullPlayerVisible = false
                    if (!dataStore.getOrDefault(BackgroundPlayPreference)) destroySession()
                },
            ) {
                PlayerWindowContent(
                    session = this@MacosPlayerSession,
                    player = player,
                    articleContext = articleContext,
                    window = window,
                    onBack = { playerWindow?.close() },
                )
            }.also { host ->
                mainWindow?.frame?.useContents {
                    host.window.setFrameOrigin(
                        NSMakePoint(
                            origin.x + (size.width - 400) / 2,
                            origin.y + (size.height - 780) / 2
                        )
                    )
                }
            }
        }
        playerWindow?.activate()
    }

    override fun destroySession() {
        val old = coordinator
        mediaSession?.close()
        mediaSession = null
        old?.let {
            cleanupScope.launch { it.awaitDestroyed() }
        }
        coordinator = null
        viewModel.clearPendingPlayback()
        val window = playerWindow
        playerWindow = null
        isFullPlayerVisible = false
        window?.close()
        old?.onCommand(PlayerCommand.Destroy)
    }

    fun shutdown(onComplete: () -> Unit) {
        shuttingDown = true
        destroySession()
        cleanupJob.complete()
        scope.launch {
            yield() // Reply only after AppKit has received NSTerminateLater.
            cleanupJob.join()
            onComplete()
            scope.cancel()
        }
    }
}

@Composable
private fun PlayerWindowContent(
    session: MacosPlayerSession,
    player: PlayerCoordinator,
    articleContext: PlayerArticleContextViewModel,
    window: NSWindow,
    onBack: () -> Unit,
) {
    val state by player.playerState.collectAsState()
    SideEffect {
        window.title = state.currentMedia?.title.orEmpty()
            .ifBlank { state.mediaTitle.orEmpty() }
            .ifBlank { "Player" }
    }
    val navigation = rememberNavigationEventDispatcherOwner()
    CompositionLocalProvider(
        LocalPlayerSession provides session,
        LocalMacosPlayerSession provides session,
        LocalNavigationEventDispatcherOwner provides navigation,
    ) {
        SettingsProvider {
            PlayerViewRoute(player, articleContext, onBack = onBack, onSaveScreenshot = {})
        }
    }
}
