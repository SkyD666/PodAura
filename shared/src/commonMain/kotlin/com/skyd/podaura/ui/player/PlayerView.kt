package com.skyd.podaura.ui.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skyd.podaura.ui.component.rememberOrientationController
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
import com.skyd.podaura.ui.player.coordinator.isReady
import com.skyd.podaura.ui.player.land.FullscreenPlayerView
import com.skyd.podaura.ui.player.port.PortraitPlayerView
import com.skyd.podaura.ui.player.service.PlayerState
import com.skyd.podaura.ui.screen.settings.playerconfig.ForwardSecondsDialog
import com.skyd.podaura.ui.screen.settings.playerconfig.ReplaySecondsDialog
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.resources.stringResource
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.close
import podaura.shared.generated.resources.retry

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
    val engineState by coordinator.engineState.collectAsStateWithLifecycle()
    if (!engineState.isReady) {
        PlayerEngineScreen(
            engineState = engineState,
            onBack = {
                coordinator.onCommand(PlayerCommand.Destroy)
                onBack()
            },
            onRetry = { coordinator.onCommand(PlayerCommand.RetryInitialize) },
        )
        return
    }
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

    val playStateCallback = remember {
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
            onSpeedChanged = { coordinator.onCommand(PlayerCommand.SetSpeed(it)) },
            onPreviousMedia = { coordinator.onCommand(PlayerCommand.PreviousMedia) },
            onNextMedia = { coordinator.onCommand(PlayerCommand.NextMedia) },
            onCycleLoop = { coordinator.onCommand(PlayerCommand.CycleLoop) },
            onShuffle = { coordinator.onCommand(PlayerCommand.Shuffle(it)) },
            onPlayFileInPlaylist = { coordinator.onCommand(PlayerCommand.PlayFileInPlaylist(it)) },
            onRemoveFromPlaylist = { coordinator.onCommand(PlayerCommand.RemoveMediaFromPlaylist(it)) }
        )
    }

    val dialogCallback = remember {
        DialogCallback(
            speedDialogCallback = SpeedDialogCallback(
                onSpeedChanged = { coordinator.onCommand(PlayerCommand.SetSpeed(it)) },
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

    // Must be remembered: a fresh lambda on every recomposition means the instance handed to
    // PlatformContent is not the one that was registered, so removeObserver() silently no-ops.
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
            playState = playState,
            articleContextState = articleContextState.takeIf {
                it.articleId == currentArticleId
            } ?: PlayerArticleContextState(),
            playStateCallback = playStateCallback,
            dialogState = dialogState,
            dialogCallback = dialogCallback,
            onBack = onBack,
            onSaveScreenshot = onSaveScreenshot,
            onCommand = { coordinator.onCommand(it) },
            onSetArticleFavorite = articleContextViewModel::setFavorite,
            snackbarHostState = snackbarHostState,
        )
    }

    PlatformContent(
        modifier = Modifier.fillMaxSize(),
        onBack = onBack,
        coordinator = coordinator,
        playerObserver = playerObserver,
        playerState = playerState,
        playState = playState,
        playStateCallback = playStateCallback,
        commonContent = commonContent,
    )
}

@Composable
private fun PlayerEngineScreen(
    engineState: PlayerEngineState,
    onBack: () -> Unit,
    onRetry: () -> Unit = {},
) {
    Box(
        modifier = Modifier.fillMaxSize().semantics {
            contentDescription = when (engineState) {
                PlayerEngineState.Initializing -> PLAYER_ENGINE_INITIALIZING_SEMANTICS
                PlayerEngineState.AwaitingMedia -> PLAYER_ENGINE_AWAITING_MEDIA_SEMANTICS
                PlayerEngineState.LoadingMedia -> PLAYER_ENGINE_LOADING_MEDIA_SEMANTICS
                is PlayerEngineState.Failed -> PLAYER_ENGINE_FAILED_SEMANTICS
                PlayerEngineState.Ready,
                PlayerEngineState.Destroyed -> PLAYER_ENGINE_LOADING_MEDIA_SEMANTICS
            }
        }
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = stringResource(Res.string.close),
            )
        }
        Column(
            modifier = Modifier.align(Alignment.Center).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (engineState is PlayerEngineState.Failed) {
                Text(
                    text = engineState.message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = onRetry) {
                    Text(stringResource(Res.string.retry))
                }
            } else {
                CircularProgressIndicator()
            }
        }
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
) {
    val player = @Composable {
        PlatformPlayerView(
            coordinator = coordinator,
            onCommand = onCommand,
            modifier = Modifier.fillMaxSize()
        )
    }
    var fullscreen by rememberSaveable { mutableStateOf(false) }
    val orientationController = rememberOrientationController()

    if (fullscreen) {
        FullscreenPlayerView(
            playerStateFlow = playerStateFlow,
            playState = playState,
            playStateCallback = playStateCallback,
            dialogState = dialogState,
            onDialogVisibilityChanged = onDialogVisibilityChanged,
            onSaveScreenshot = onSaveScreenshot,
            onCommand = onCommand,
            playerContent = player,
            onExitFullscreen = {
                orientationController.unspecified()
                fullscreen = false
            }
        )
    } else {
        PortraitPlayerView(
            playerStateFlow = playerStateFlow,
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
        )
    }

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

@Composable
expect fun PlatformPlayerView(
    coordinator: PlayerCoordinator,
    modifier: Modifier,
    onCommand: (PlayerCommand) -> Unit,
)

@Composable
expect fun PlatformContent(
    modifier: Modifier,
    onBack: () -> Unit,
    coordinator: PlayerCoordinator,
    playerObserver: PlayerCoordinator.Observer,
    playerState: PlayerState,
    playState: PlayState,
    playStateCallback: PlayStateCallback,
    commonContent: @Composable () -> Unit,
)
