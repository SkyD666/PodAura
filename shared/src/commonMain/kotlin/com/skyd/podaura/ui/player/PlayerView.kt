package com.skyd.podaura.ui.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.skyd.fundation.util.Platform
import com.skyd.fundation.util.platform
import com.skyd.podaura.ui.component.rememberOrientationController
import com.skyd.podaura.ui.component.tickVibrate
import com.skyd.podaura.ui.player.component.dialog.SpeedDialog
import com.skyd.podaura.ui.player.component.dialog.audio.AudioTrackDialog
import com.skyd.podaura.ui.player.component.dialog.subtitle.SubtitleTrackDialog
import com.skyd.podaura.ui.player.component.state.PlayState
import com.skyd.podaura.ui.player.component.state.PlayStateCallback
import com.skyd.podaura.ui.player.component.state.dialog.DialogCallback
import com.skyd.podaura.ui.player.component.state.dialog.DialogState
import com.skyd.podaura.ui.player.component.state.dialog.OnDialogVisibilityChanged
import com.skyd.podaura.ui.player.component.state.dialog.SpeedDialogCallback
import com.skyd.podaura.ui.player.component.state.dialog.SpeedDialogState
import com.skyd.podaura.ui.player.component.state.dialog.seconds.ForwardSecondsDialogState
import com.skyd.podaura.ui.player.component.state.dialog.seconds.ReplaySecondsDialogState
import com.skyd.podaura.ui.player.component.state.dialog.track.AudioTrackDialogCallback
import com.skyd.podaura.ui.player.component.state.dialog.track.AudioTrackDialogState
import com.skyd.podaura.ui.player.component.state.dialog.track.SubtitleTrackDialogCallback
import com.skyd.podaura.ui.player.component.state.dialog.track.SubtitleTrackDialogState
import com.skyd.podaura.ui.player.coordinator.PlayerCoordinator
import com.skyd.podaura.ui.player.coordinator.PlayerEngineState
import com.skyd.podaura.ui.player.land.FullscreenPlayerView
import com.skyd.podaura.ui.player.land.controller.preview.LongPressSpeedPreview
import com.skyd.podaura.ui.player.port.PlayerPresentationState
import com.skyd.podaura.ui.player.port.PortraitPlayerView
import com.skyd.podaura.ui.player.service.PlayerState
import com.skyd.podaura.ui.screen.settings.playerconfig.ForwardSecondsDialog
import com.skyd.podaura.ui.screen.settings.playerconfig.ReplaySecondsDialog
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.jetbrains.compose.resources.stringResource
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.playback_failed
import podaura.shared.generated.resources.retry
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun PlayerViewRoute(
    coordinator: PlayerCoordinator?,
    articleContextViewModel: PlayerArticleContextViewModel,
    onBack: () -> Unit,
    onSaveScreenshot: (PlatformFile) -> Unit,
) {
    if (coordinator == null) PlayerEngineScreen(
        engineState = PlayerEngineState.Initializing,
        onBack = onBack,
    )
    else PlayerView(coordinator, articleContextViewModel, onBack, onSaveScreenshot)
}

@Composable
fun PlayerView(
    coordinator: PlayerCoordinator,
    articleContextViewModel: PlayerArticleContextViewModel,
    onBack: () -> Unit,
    onSaveScreenshot: (PlatformFile) -> Unit,
) {
    val providedPressAndHoldSpeedController = LocalPressAndHoldSpeedController.current
    val pressAndHoldSpeedController = remember(
        coordinator,
        providedPressAndHoldSpeedController,
    ) {
        providedPressAndHoldSpeedController ?: PressAndHoldSpeedController(
            currentSpeed = { coordinator.playerState.value.speed },
            onSpeedChanged = { coordinator.onCommand(PlayerCommand.SetSpeed(it)) },
        )
    }
    DisposableEffect(pressAndHoldSpeedController) {
        onDispose { pressAndHoldSpeedController.cancelAll() }
    }

    val engineState by coordinator.engineState.collectAsStateWithLifecycle()
    PlatformPlayerLifecycleEffect(coordinator = coordinator)
    val presentationState = engineState.toPresentationState()
    val stablePlayerState = remember(coordinator.playerState) {
        coordinator.playerState.withoutHotPlayerValues()
    }
    val playerState by stablePlayerState.collectAsStateWithLifecycle(
        initialValue = coordinator.playerState.value.copy(
            position = 0L,
            buffer = 0,
            zoom = 1f,
            offsetX = 0f,
            offsetY = 0f,
            rotate = 0f,
        )
    )
    val articleContextState by articleContextViewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var isSeeking by remember { mutableStateOf(false) }
    val playState = remember(playerState, isSeeking) {
        PlayState(
            isPlaying = !playerState.paused && playerState.mediaStarted,
            isSeeking = isSeeking,
            state = playerState,
        )
    }

    val currentArticle = playerState.currentMedia?.article?.articleWithEnclosure?.article
        ?.takeIf { presentationState is PlayerPresentationState.Ready }
    val currentArticleId = currentArticle?.articleId
    LaunchedEffect(currentArticleId) {
        articleContextViewModel.bindArticle(
            articleId = currentArticleId,
            initialFavorite = currentArticle?.isFavorite,
        )
    }
    DisposableEffect(articleContextViewModel) {
        onDispose { articleContextViewModel.bindArticle(null, null) }
    }
    LaunchedEffect(articleContextViewModel, snackbarHostState) {
        articleContextViewModel.events.collect { event ->
            when (event) {
                is PlayerArticleContextEvent.FavoriteFailed ->
                    snackbarHostState.showSnackbar(event.message)
            }
        }
    }
    val playbackFailedMessage = stringResource(Res.string.playback_failed)
    val retryLabel = stringResource(Res.string.retry)
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(coordinator, snackbarHostState, playbackFailedMessage, retryLabel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            coordinator.playerState.map { it.pendingPlaybackFailures.firstOrNull() }
                .distinctUntilChanged()
                .collectLatest { failure ->
                    if (failure == null) return@collectLatest
                    if (failure.retryEnd != null) delay(250.milliseconds)
                    val result = snackbarHostState.showSnackbar(
                        message = listOfNotNull(playbackFailedMessage, failure.details).joinToString("\n"),
                        actionLabel = retryLabel.takeIf { failure.retryEnd != null },
                        withDismissAction = true,
                        duration = SnackbarDuration.Long,
                    )
                    coordinator.onPlaybackFailureHandled(
                        id = failure.id,
                        retry = result == SnackbarResult.ActionPerformed,
                    )
                }
        }
    }

    var subtitleTrackDialogState by remember { mutableStateOf(SubtitleTrackDialogState.initial) }
    var audioTrackDialogState by remember { mutableStateOf(AudioTrackDialogState.initial) }
    var speedDialogState by remember { mutableStateOf(SpeedDialogState.initial) }
    var forwardSecondsDialogState by remember { mutableStateOf(ForwardSecondsDialogState.initial) }
    var replaySecondsDialogState by remember { mutableStateOf(ReplaySecondsDialogState.initial) }

    val dialogState by remember {
        mutableStateOf(
            DialogState(
                speedDialogState = { speedDialogState },
                audioTrackDialogState = { audioTrackDialogState },
                subtitleTrackDialogState = { subtitleTrackDialogState },
                forwardSecondsDialogState = { forwardSecondsDialogState },
                replaySecondsDialogState = { replaySecondsDialogState },
            )
        )
    }

    val playStateCallback = remember(coordinator, pressAndHoldSpeedController) {
        PlayStateCallback(
            onPlayStateChanged = {
                val state = coordinator.playerState.value
                coordinator.onCommand(PlayerCommand.Paused(!state.paused && state.mediaStarted))
            },
            onPlayOrPause = { coordinator.onCommand(PlayerCommand.PlayOrPause) },
            onSeekTo = {
                isSeeking = true
                coordinator.onCommand(PlayerCommand.SeekTo(it))
            },
            onSeekBy = {
                isSeeking = true
                coordinator.onCommand(
                    PlayerCommand.SeekTo(coordinator.playerState.value.position + it)
                )
            },
            onSpeedChanged = pressAndHoldSpeedController::setRegularSpeed,
            onPreviousMedia = { coordinator.onCommand(PlayerCommand.PreviousMedia) },
            onNextMedia = { coordinator.onCommand(PlayerCommand.NextMedia) },
            onCycleLoop = { coordinator.onCommand(PlayerCommand.CycleLoop) },
            onShuffle = { coordinator.onCommand(PlayerCommand.Shuffle(it)) },
            onPlayFileInPlaylist = { coordinator.onCommand(PlayerCommand.PlayFileInPlaylist(it)) },
            onRemoveFromPlaylist = { coordinator.onCommand(PlayerCommand.RemoveMediaFromPlaylist(it)) }
        )
    }

    val dialogCallback = remember(coordinator, pressAndHoldSpeedController) {
        DialogCallback(
            speedDialogCallback = SpeedDialogCallback(
                onSpeedChanged = pressAndHoldSpeedController::setRegularSpeed,
            ),
            audioTrackDialogCallback = AudioTrackDialogCallback(
                onAudioTrackChanged = { coordinator.onCommand(PlayerCommand.SetAudioTrack(it.trackId)) },
                onAddAudioTrack = { coordinator.onCommand(PlayerCommand.AddAudio(it)) },
                onAudioDelayChanged = { coordinator.onCommand(PlayerCommand.AudioDelay(it)) },
            ),
            subtitleTrackDialogCallback = SubtitleTrackDialogCallback(
                onSubtitleTrackChanged = { coordinator.onCommand(PlayerCommand.SetSubtitleTrack(it.trackId)) },
                onAddSubtitle = { coordinator.onCommand(PlayerCommand.AddSubtitle(it)) },
                onSubtitleDelayChanged = { coordinator.onCommand(PlayerCommand.SubtitleDelay(it)) },
            ),
        )
    }

    // Keep the observer identity stable for lifecycle registration and removal.
    val currentOnBack by rememberUpdatedState(onBack)
    val playerObserver = remember {
        PlayerCoordinator.Observer { command ->
            when (command) {
                is PlayerEvent.Shutdown -> currentOnBack()
                PlayerEvent.Seek -> isSeeking = false
                else -> Unit
            }
        }
    }

    LifecycleStartEffect(Unit) {
        coordinator.addObserver(playerObserver)
        onStopOrDispose {
            coordinator.removeObserver(playerObserver)
        }
    }

    val commonContent = @Composable {
        val ready = presentationState is PlayerPresentationState.Ready
        Content(
            coordinator = coordinator,
            playerStateFlow = coordinator.playerState,
            onDialogVisibilityChanged = remember {
                OnDialogVisibilityChanged(
                    onSpeedDialog = { speedDialogState = speedDialogState.copy(show = it) },
                    onAudioTrackDialog = {
                        audioTrackDialogState = audioTrackDialogState.copy(show = it)
                    },
                    onSubtitleTrackDialog = {
                        subtitleTrackDialogState = subtitleTrackDialogState.copy(show = it)
                    },
                    onSubtitleSettingDialog = {
                        subtitleTrackDialogState = subtitleTrackDialogState.copy(showSetting = it)
                    },
                    onAudioSettingDialog = {
                        audioTrackDialogState = audioTrackDialogState.copy(showSetting = it)
                    },
                    onReplaySecondDialog = {
                        replaySecondsDialogState = replaySecondsDialogState.copy(show = it)
                    },
                    onForwardSecondDialog = {
                        forwardSecondsDialogState = forwardSecondsDialogState.copy(show = it)
                    },
                )
            },
            playState = if (ready) playState else enginePlayState,
            articleContextState = articleContextState.takeIf {
                ready && it.articleId == currentArticleId
            } ?: PlayerArticleContextState(),
            playStateCallback = playStateCallback,
            dialogState = dialogState,
            dialogCallback = dialogCallback,
            onBack = onBack,
            onSaveScreenshot = onSaveScreenshot,
            onCommand = { coordinator.onCommand(it) },
            onSetArticleFavorite = articleContextViewModel::setFavorite,
            snackbarHostState = snackbarHostState,
            modifier = Modifier.fillMaxSize().then(
                if (ready) Modifier else Modifier.playerEngineSemantics(engineState)
            ),
            presentationState = presentationState,
            onRetry = { coordinator.onCommand(PlayerCommand.RetryInitialize) },
        )
    }

    CompositionLocalProvider(
        LocalPressAndHoldSpeedController provides pressAndHoldSpeedController,
    ) {
        PlatformContent(
            modifier = Modifier.fillMaxSize(),
            onBack = onBack,
            coordinator = coordinator,
            playerState = playerState,
            playState = playState,
            playStateCallback = playStateCallback,
            commonContent = commonContent,
        )
    }
}

@Composable
private fun PlayerEngineScreen(
    engineState: PlayerEngineState,
    onBack: () -> Unit,
    onRetry: () -> Unit = {},
) {
    PortraitPlayerView(
        playState = enginePlayState,
        articleContextState = PlayerArticleContextState(),
        playStateCallback = emptyPlayStateCallback,
        onDialogVisibilityChanged = emptyDialogVisibilityChanged,
        onBack = onBack,
        onEnterFullscreen = {},
        playerContent = {},
        onSetArticleFavorite = {},
        snackbarHostState = remember { SnackbarHostState() },
        modifier = Modifier.fillMaxSize().playerEngineSemantics(engineState),
        presentationState = engineState.toPresentationState(),
        onRetry = onRetry,
    )
}

private val enginePlayState = PlayState.initial

private val emptyPlayStateCallback = PlayStateCallback(
    onPlayStateChanged = {},
    onPlayOrPause = {},
    onSeekTo = {},
    onSeekBy = {},
    onSpeedChanged = {},
    onPreviousMedia = {},
    onNextMedia = {},
    onCycleLoop = {},
    onShuffle = {},
    onPlayFileInPlaylist = {},
    onRemoveFromPlaylist = {},
)

private val emptyDialogVisibilityChanged = OnDialogVisibilityChanged(
    onSpeedDialog = {},
    onSubtitleTrackDialog = {},
    onAudioTrackDialog = {},
    onSubtitleSettingDialog = {},
    onAudioSettingDialog = {},
    onReplaySecondDialog = {},
    onForwardSecondDialog = {},
)

private fun PlayerEngineState.toPresentationState(): PlayerPresentationState = when (this) {
    PlayerEngineState.Ready -> PlayerPresentationState.Ready
    is PlayerEngineState.Failed -> PlayerPresentationState.Failed(message)
    else -> PlayerPresentationState.Loading
}

private fun Modifier.playerEngineSemantics(engineState: PlayerEngineState): Modifier = semantics {
    contentDescription = when (engineState) {
        PlayerEngineState.Initializing -> PLAYER_ENGINE_INITIALIZING_SEMANTICS
        PlayerEngineState.AwaitingMedia -> PLAYER_ENGINE_AWAITING_MEDIA_SEMANTICS
        PlayerEngineState.LoadingMedia -> PLAYER_ENGINE_LOADING_MEDIA_SEMANTICS
        is PlayerEngineState.Failed -> PLAYER_ENGINE_FAILED_SEMANTICS
        PlayerEngineState.Ready,
        PlayerEngineState.Destroyed -> PLAYER_ENGINE_LOADING_MEDIA_SEMANTICS
    }
}

private const val PLAYER_ENGINE_INITIALIZING_SEMANTICS = "podaura_player_engine_initializing"
private const val PLAYER_ENGINE_AWAITING_MEDIA_SEMANTICS = "podaura_player_engine_awaiting_media"
private const val PLAYER_ENGINE_LOADING_MEDIA_SEMANTICS = "podaura_player_engine_loading_media"
private const val PLAYER_ENGINE_FAILED_SEMANTICS = "podaura_player_engine_failed"

@Composable
private fun Content(
    coordinator: PlayerCoordinator,
    playerStateFlow: StateFlow<PlayerState>,
    playState: PlayState,
    articleContextState: PlayerArticleContextState,
    playStateCallback: PlayStateCallback,
    dialogState: DialogState,
    dialogCallback: DialogCallback,
    onBack: () -> Unit,
    onSaveScreenshot: (PlatformFile) -> Unit,
    onDialogVisibilityChanged: OnDialogVisibilityChanged,
    onCommand: (PlayerCommand) -> Unit,
    onSetArticleFavorite: (Boolean) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    presentationState: PlayerPresentationState = PlayerPresentationState.Ready,
    onRetry: () -> Unit = {},
) {
    val pressAndHoldSpeedController =
        requireNotNull(LocalPressAndHoldSpeedController.current)
    val playerProgress = remember(playerStateFlow) { playerStateFlow.playerProgressValues() }
    val player = @Composable {
        if (presentationState is PlayerPresentationState.Ready) {
            PlatformPlayerView(
                coordinator = coordinator,
                onCommand = onCommand,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
    var fullscreen by rememberSaveable { mutableStateOf(false) }
    val orientationController = rememberOrientationController()

    Box(modifier = Modifier.fillMaxSize()) {
        if (fullscreen && presentationState is PlayerPresentationState.Ready) {
            FullscreenPlayerView(
                playerStateFlow = playerStateFlow,
                playState = playState,
                playStateCallback = playStateCallback,
                dialogState = dialogState,
                onDialogVisibilityChanged = onDialogVisibilityChanged,
                onSaveScreenshot = onSaveScreenshot,
                onCommand = onCommand,
                snackbarHostState = snackbarHostState,
                playerContent = player,
                onExitFullscreen = {
                    orientationController.unspecified()
                    fullscreen = false
                }
            )
        } else {
            PortraitPlayerView(
                playState = playState,
                articleContextState = articleContextState,
                playStateCallback = playStateCallback,
                onDialogVisibilityChanged = onDialogVisibilityChanged,
                onBack = onBack,
                onEnterFullscreen = {
                    orientationController.landscape()
                    fullscreen = true
                },
                playerContent = player,
                onSetArticleFavorite = onSetArticleFavorite,
                snackbarHostState = snackbarHostState,
                modifier = modifier,
                presentationState = presentationState,
                onRetry = onRetry,
                playerProgress = playerProgress,
                initialProgress = playerStateFlow.value.toPlayerProgress(),
            )
        }

        if (pressAndHoldSpeedController.isActive) {
            LaunchedEffect(Unit) {
                if (platform == Platform.Android) tickVibrate()
            }
            LongPressSpeedPreview(
                speed = { PressAndHoldSpeedController.HOLD_SPEED },
            )
        }
    }

    if (presentationState is PlayerPresentationState.Ready) {
        SpeedDialog(
            onDismissRequest = { onDialogVisibilityChanged.onSpeedDialog(false) },
            playState = { playState },
            speedDialogState = dialogState.speedDialogState,
            speedDialogCallback = dialogCallback.speedDialogCallback,
        )
        AudioTrackDialog(
            onDismissRequest = { onDialogVisibilityChanged.onAudioTrackDialog(false) },
            playState = { playState },
            audioTrackDialogState = dialogState.audioTrackDialogState,
            audioTrackDialogCallback = dialogCallback.audioTrackDialogCallback,
            onDialogVisibilityChanged = onDialogVisibilityChanged,
        )
        SubtitleTrackDialog(
            onDismissRequest = { onDialogVisibilityChanged.onSubtitleTrackDialog(false) },
            playState = { playState },
            subtitleTrackDialogState = dialogState.subtitleTrackDialogState,
            subtitleTrackDialogCallback = dialogCallback.subtitleTrackDialogCallback,
            onDialogVisibilityChanged = onDialogVisibilityChanged,
        )
        ReplaySecondsDialog(
            visible = { dialogState.replaySecondsDialogState().show },
            onDismissRequest = { onDialogVisibilityChanged.onReplaySecondDialog(false) },
        )
        ForwardSecondsDialog(
            visible = { dialogState.forwardSecondsDialogState().show },
            onDismissRequest = { onDialogVisibilityChanged.onForwardSecondDialog(false) },
        )
    }
}

@Composable
expect fun PlatformPlayerView(
    coordinator: PlayerCoordinator,
    modifier: Modifier,
    onCommand: (PlayerCommand) -> Unit,
)

@Composable
expect fun PlatformPlayerLifecycleEffect(
    coordinator: PlayerCoordinator,
)

@Composable
expect fun PlatformContent(
    modifier: Modifier,
    onBack: () -> Unit,
    coordinator: PlayerCoordinator,
    playerState: PlayerState,
    playState: PlayState,
    playStateCallback: PlayStateCallback,
    commonContent: @Composable () -> Unit,
)
