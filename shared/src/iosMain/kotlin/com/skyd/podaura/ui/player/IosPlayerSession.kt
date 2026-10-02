package com.skyd.podaura.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.skyd.fundation.di.get
import com.skyd.podaura.ui.player.coordinator.PlayerCoordinator
import com.skyd.podaura.ui.screen.AppEntrance
import com.skyd.podaura.ui.screen.SettingsProvider
import com.skyd.podaura.ui.theme.PodAuraTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

internal val LocalIosPlayerSession = staticCompositionLocalOf<IosPlayerSession> {
    error("iOS player session is not installed")
}

internal class IosPlayerSession : PlayerSession {
    private val viewModel = get<PlayerViewModel>()
    val articleContext = get<PlayerArticleContextViewModel>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var mediaSession: IosMediaSession? = null
    override var coordinator by mutableStateOf<PlayerCoordinator?>(null)
        private set
    override var isFullPlayerVisible by mutableStateOf(false)
        private set
    private val entry = PlatformPlayerEntry(::openAccepted, { isFullPlayerVisible = false })

    init {
        scope.launch { viewModel.mediaInfos.collect { coordinator?.onCommand(it.toLoadCommand()) } }
    }

    fun open(request: PlayerOpenRequest) = entry.open(request)

    private fun openAccepted(request: PlayerOpenRequest) {
        if (coordinator == null) {
            val created = PlayerCoordinator()
            coordinator = created
            mediaSession = IosMediaSession(created)
            created.lifecycle.addObserver(object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    if (coordinator === created) destroySession()
                }
            })
        }
        isFullPlayerVisible = true
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
    }

    override fun openFullPlayer() = open(PlayerOpenRequest.Resume)
    fun closeFullPlayer() {
        isFullPlayerVisible = false
    }

    override fun destroySession() {
        isFullPlayerVisible = false
        val old = coordinator
        coordinator = null
        mediaSession?.close()
        mediaSession = null
        viewModel.clearPendingPlayback()
        old?.onCommand(PlayerCommand.Destroy)
    }

    fun close() {
        destroySession(); scope.cancel()
    }
}

@Composable
internal fun IosPlayerApp() {
    val session = remember { IosPlayerSession() }
    DisposableEffect(session) { onDispose { session.close() } }
    CompositionLocalProvider(
        LocalPlayerSession provides session,
        LocalIosPlayerSession provides session
    ) {
        Box(Modifier.fillMaxSize()) {
            // Keep navigation alive while the full player is above it.
            AppEntrance()
            if (session.isFullPlayerVisible) {
                SettingsProvider {
                    PodAuraTheme(com.skyd.podaura.model.preference.appearance.DarkModePreference.current) {
                        Box(
                            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                        ) {
                            PlayerViewRoute(
                                session.coordinator, session.articleContext,
                                onBack = session::closeFullPlayer, onSaveScreenshot = {})
                        }
                    }
                }
            }
        }
    }
}
