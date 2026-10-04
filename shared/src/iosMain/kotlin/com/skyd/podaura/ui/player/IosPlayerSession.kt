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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.rememberNavigationEventDispatcherOwner
import co.touchlab.kermit.Logger
import com.skyd.fundation.di.get
import com.skyd.podaura.IosPlayerChrome
import com.skyd.podaura.ext.flowOf
import com.skyd.podaura.ext.getOrDefault
import com.skyd.podaura.model.preference.dataStore
import com.skyd.podaura.model.preference.player.BackgroundPlayPreference
import com.skyd.podaura.ui.player.coordinator.PlayerCoordinator
import com.skyd.podaura.ui.player.coordinator.PlayerEngineState
import com.skyd.podaura.ui.player.coordinator.isReady
import com.skyd.podaura.ui.player.pip.IosPictureInPicture
import com.skyd.podaura.ui.screen.AppEntrance
import com.skyd.podaura.ui.screen.SettingsProvider
import kotlin.time.TimeSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import platform.UIKit.UIViewAutoresizingFlexibleHeight
import platform.UIKit.UIViewAutoresizingFlexibleWidth
import platform.UIKit.UIViewController
import platform.UIKit.addChildViewController
import platform.UIKit.didMoveToParentViewController

internal val LocalIosPlayerSession = staticCompositionLocalOf<IosPlayerSession> {
    error("iOS player session is not installed")
}

internal class IosPlayerSession : PlayerSession {
    var navigationController: IosPlayerNavigationController? = null
    private var playerController: UIViewController? = null
    private val viewModel = get<PlayerViewModel>()
    val articleContext = get<PlayerArticleContextViewModel>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var mediaSession: IosMediaSession? = null
    var pictureInPicture by mutableStateOf<IosPictureInPicture?>(null)
        private set
    private var startupJob: Job? = null
    private var startupTime: TimeSource.Monotonic.ValueTimeMark? = null
    private var automaticallyEnterPipOnClose = true
    override var coordinator by mutableStateOf<PlayerCoordinator?>(null)
        private set
    override var isFullPlayerVisible by mutableStateOf(false)
        private set
    private val entry = PlatformPlayerEntry(::openAccepted, ::closeFullPlayer)

    init {
        scope.launch {
            dataStore.flowOf(BackgroundPlayPreference).collect { enabled ->
                if (!enabled) destroyIfUnused()
            }
        }
        scope.launch {
            viewModel.mediaInfos.collect {
                startupTime?.let { started ->
                    Logger.d(tag = "PlayerStartup") {
                        "Player/PlaylistPrepared (${it.playlist.size} items): ${started.elapsedNow()}"
                    }
                }
                coordinator?.onCommand(it.toLoadCommand())
            }
        }
    }

    fun open(request: PlayerOpenRequest) = entry.open(request)

    private fun openAccepted(request: PlayerOpenRequest) {
        val started = TimeSource.Monotonic.markNow()
        startupTime = started
        if (coordinator == null) {
            val created = PlayerCoordinator()
            coordinator = created
            val pip = IosPictureInPicture(created, this)
            pictureInPicture = pip
            mediaSession = IosMediaSession(created, pip::updateArtwork)
            created.lifecycle.addObserver(object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    if (coordinator === created) destroySession()
                }
            })
        }
        isFullPlayerVisible = true
        val controller = playerController ?: IosPlayerViewController(this, checkNotNull(coordinator))
            .also { playerController = it }
        navigationController?.showPlayer(controller)
        startupJob?.cancel()
        startupJob = scope.launch {
            coordinator?.engineState?.onEach { state ->
                Logger.d(tag = "PlayerStartup") { "Player/$state: ${started.elapsedNow()}" }
            }
                ?.first { it.isReady || it is PlayerEngineState.Failed || it == PlayerEngineState.Destroyed }
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
    }

    fun onPictureInPictureChanged(active: Boolean) {
        mediaSession?.setPictureInPictureActive(active)
    }

    override fun openFullPlayer() = open(PlayerOpenRequest.Resume)
    fun restoreFullPlayer(completion: (Boolean) -> Unit) {
        if (coordinator == null) {
            completion(false); return
        }
        openFullPlayer()
        val navigation = navigationController
        val controller = playerController
        if (navigation == null || controller == null) completion(false)
        else navigation.whenPlayerShown(controller, completion)
    }

    fun closeFullPlayer() {
        closeFullPlayer(automaticallyEnterPip = true)
    }

    fun closeFullPlayer(automaticallyEnterPip: Boolean) {
        automaticallyEnterPipOnClose = automaticallyEnterPip
        navigationController?.closePlayer()
    }

    fun onFullPlayerWillClose() {
        if (isFullPlayerVisible) pictureInPicture?.prepareForPlayerClose(
            automaticallyEnterPipOnClose
        )
    }

    fun onFullPlayerCloseCancelled() {
        pictureInPicture?.cancelPlayerClose()
        automaticallyEnterPipOnClose = true
    }

    fun onFullPlayerClosed() {
        if (isFullPlayerVisible) pictureInPicture?.onPlayerClosed(navigationController?.view)
        playerController = null
        isFullPlayerVisible = false
        automaticallyEnterPipOnClose = true
        destroyIfUnused()
    }

    // Navigation and AVKit jointly own the session; the list screen cannot decide its lifetime.
    fun destroyIfUnused() {
        if (coordinator != null && !isFullPlayerVisible &&
            pictureInPicture?.keepsSession != true &&
            !dataStore.getOrDefault(BackgroundPlayPreference)
        ) destroySession()
    }

    override fun destroySession() {
        val old = coordinator
        coordinator = null
        pictureInPicture?.close()
        pictureInPicture = null
        startupJob?.cancel()
        startupJob = null
        startupTime = null
        isFullPlayerVisible = false
        navigationController?.closePlayer(animated = false)
        playerController = null
        mediaSession?.close()
        mediaSession = null
        viewModel.clearPendingPlayback()
        old?.onCommand(PlayerCommand.Destroy)
    }

    fun close() {
        destroySession()
        scope.cancel()
        if (IosPlayerChrome.controller === navigationController) IosPlayerChrome.controller = null
        navigationController = null
    }
}

private class IosPlayerViewController(session: IosPlayerSession, coordinator: PlayerCoordinator) :
    UIViewController(nibName = null, bundle = null) {
    private val compose = ComposeUIViewController { IosFullPlayer(session, coordinator) }

    override fun viewDidLoad() {
        super.viewDidLoad()
        addChildViewController(compose)
        compose.view.setFrame(view.bounds)
        compose.view.autoresizingMask =
            UIViewAutoresizingFlexibleWidth or UIViewAutoresizingFlexibleHeight
        view.addSubview(compose.view)
        compose.didMoveToParentViewController(this)
    }

    override fun prefersStatusBarHidden(): Boolean = IosPlayerChrome.fullscreen
}

@Composable
internal fun IosPlayerApp(session: IosPlayerSession) {
    DisposableEffect(session) { onDispose { session.close() } }
    CompositionLocalProvider(
        LocalPlayerSession provides session,
        LocalIosPlayerSession provides session
    ) {
        val appNavigation = rememberNavigationEventDispatcherOwner(
            enabled = !session.isFullPlayerVisible,
        )
        CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides appNavigation) {
            AppEntrance()
        }
    }
}

@Composable
private fun IosFullPlayer(session: IosPlayerSession, coordinator: PlayerCoordinator) {
    CompositionLocalProvider(
        LocalPlayerSession provides session,
        LocalIosPlayerSession provides session,
    ) {
        SettingsProvider {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                PlayerViewRoute(
                    coordinator = coordinator,
                    articleContextViewModel = session.articleContext,
                    onBack = {
                        // An outgoing controller can receive its old engine's delayed shutdown.
                        if (session.coordinator === coordinator) session.closeFullPlayer()
                    },
                    onSaveScreenshot = {},
                )
            }
        }
    }
}
