package com.skyd.podaura.ui.player

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import com.skyd.podaura.ui.player.land.FullscreenPlayerView
import com.skyd.podaura.ui.player.port.PortraitPlayerView
import com.skyd.podaura.ui.player.service.PlayerState
import com.skyd.podaura.ui.screen.settings.playerconfig.ForwardSecondsDialog
import com.skyd.podaura.ui.screen.settings.playerconfig.ReplaySecondsDialog
import io.github.vinceglb.filekit.PlatformFile

@Composable
fun PlayerViewRoute(
    service: PlayerCoordinator?,
    onBack: () -> Unit,
    onSaveScreenshot: (PlatformFile) -> Unit,
) {
    if (service != null) {
        PlayerView(service, onBack, onSaveScreenshot)
    }
}

@Composable
fun PlayerView(
    service: PlayerCoordinator,
    onBack: () -> Unit,
    onSaveScreenshot: (PlatformFile) -> Unit,
) {
    val playerState by service.playerState.collectAsStateWithLifecycle()
    var playState by remember { mutableStateOf(PlayState.initial) }

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
            onPlayStateChanged = { service.onCommand(PlayerCommand.Paused(playState.isPlaying)) },
            onPlayOrPause = { service.onCommand(PlayerCommand.PlayOrPause) },
            onSeekTo = {
                playState = playState.copy(isSeeking = true)
                service.onCommand(PlayerCommand.SeekTo(it))
            },
            onSpeedChanged = { service.onCommand(PlayerCommand.SetSpeed(it)) },
            onPreviousMedia = { service.onCommand(PlayerCommand.PreviousMedia) },
            onNextMedia = { service.onCommand(PlayerCommand.NextMedia) },
            onCycleLoop = { service.onCommand(PlayerCommand.CycleLoop) },
            onShuffle = { service.onCommand(PlayerCommand.Shuffle(it)) },
            onPlayFileInPlaylist = { service.onCommand(PlayerCommand.PlayFileInPlaylist(it)) },
            onRemoveFromPlaylist = { service.onCommand(PlayerCommand.RemoveMediaFromPlaylist(it)) }
        )
    }

    val dialogCallback = remember {
        DialogCallback(
            speedDialogCallback = SpeedDialogCallback(
                onSpeedChanged = { service.onCommand(PlayerCommand.SetSpeed(it)) },
            ),
            audioTrackDialogCallback = AudioTrackDialogCallback(
                onAudioTrackChanged = { service.onCommand(PlayerCommand.SetAudioTrack(it.trackId)) },
                onAddAudioTrack = { service.onCommand(PlayerCommand.AddAudio(it)) },
                onAudioDelayChanged = { service.onCommand(PlayerCommand.AudioDelay(it)) },
            ),
            subtitleTrackDialogCallback = SubtitleTrackDialogCallback(
                onSubtitleTrackChanged = { service.onCommand(PlayerCommand.SetSubtitleTrack(it.trackId)) },
                onAddSubtitle = { service.onCommand(PlayerCommand.AddSubtitle(it)) },
                onSubtitleDelayChanged = { service.onCommand(PlayerCommand.SubtitleDelay(it)) },
            ),
        )
    }

    LaunchedEffect(playerState) {
        playState = playState.copy(
            isPlaying = !playerState.paused && playerState.mediaStarted,
            state = playerState,
        )
    }

    val playerObserver = PlayerCoordinator.Observer { command ->
        when (command) {
            is PlayerEvent.Shutdown -> onBack()
            PlayerEvent.Seek -> playState = playState.copy(isSeeking = false)
            else -> Unit
        }
    }

    LifecycleStartEffect(Unit) {
        service.addObserver(playerObserver)
        onStopOrDispose {
            service.removeObserver(playerObserver)
        }
    }

    val commonContent = @Composable {
        Content(
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
            playStateCallback = playStateCallback,
            dialogState = dialogState,
            dialogCallback = dialogCallback,
            onBack = onBack,
            onSaveScreenshot = onSaveScreenshot,
            onCommand = { service.onCommand(it) },
        )
    }

    PlatformContent(
        modifier = Modifier.fillMaxSize(),
        onBack = onBack,
        service = service,
        playerObserver = playerObserver,
        playerState = playerState,
        playState = playState,
        playStateCallback = playStateCallback,
        commonContent = commonContent,
    )
}

@Composable
private fun Content(
    playState: PlayState,
    playStateCallback: PlayStateCallback,
    dialogState: DialogState,
    dialogCallback: DialogCallback,
    onBack: () -> Unit,
    onSaveScreenshot: (PlatformFile) -> Unit,
    onDialogVisibilityChanged: OnDialogVisibilityChanged,
    onCommand: (PlayerCommand) -> Unit,
) {
    val player = @Composable {
        PlatformPlayerView(
            onCommand = onCommand,
            modifier = Modifier.fillMaxSize()
        )
    }
    var fullscreen by rememberSaveable { mutableStateOf(false) }
    val orientationController = rememberOrientationController()

    if (fullscreen) {
        FullscreenPlayerView(
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
            playState = playState,
            playStateCallback = playStateCallback,
            onDialogVisibilityChanged = onDialogVisibilityChanged,
            onBack = onBack,
            onEnterFullscreen = {
                orientationController.landscape()
                fullscreen = true
            },
            playerContent = player,
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
    modifier: Modifier,
    onCommand: (PlayerCommand) -> Unit,
)

@Composable
expect fun PlatformContent(
    modifier: Modifier,
    onBack: () -> Unit,
    service: PlayerCoordinator,
    playerObserver: PlayerCoordinator.Observer,
    playerState: PlayerState,
    playState: PlayState,
    playStateCallback: PlayStateCallback,
    commonContent: @Composable () -> Unit,
)

