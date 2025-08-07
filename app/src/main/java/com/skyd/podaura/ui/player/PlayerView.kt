package com.skyd.podaura.ui.player

import android.content.pm.ActivityInfo
import android.provider.Settings
import android.view.OrientationEventListener
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.rememberAsyncImagePainter
import com.skyd.compone.ext.ratio
import com.skyd.compone.ext.size
import com.skyd.compone.ext.thenIfNotNull
import com.skyd.podaura.ext.activity
import com.skyd.podaura.ext.screenIsLand
import com.skyd.podaura.model.preference.player.BackgroundPlayPreference
import com.skyd.podaura.ui.component.OnLifecycleEvent
import com.skyd.podaura.ui.player.component.PlayerAndroidView
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
import com.skyd.podaura.ui.player.land.LandscapePlayerView
import com.skyd.podaura.ui.player.pip.PipBroadcastReceiver
import com.skyd.podaura.ui.player.pip.PipListenerPreAPI12
import com.skyd.podaura.ui.player.pip.pipParams
import com.skyd.podaura.ui.player.pip.rememberIsInPipMode
import com.skyd.podaura.ui.player.port.PortraitPlayerView
import com.skyd.podaura.ui.player.service.PlayerService
import com.skyd.podaura.ui.screen.settings.playerconfig.ForwardSecondsDialog
import com.skyd.podaura.ui.screen.settings.playerconfig.ReplaySecondsDialog
import java.io.File


@Composable
fun PlayerViewRoute(
    service: PlayerService?,
    onBack: () -> Unit,
    onSaveScreenshot: (File) -> Unit,
) {
    if (service != null) {
        PlayerView(service, onBack, onSaveScreenshot)
    }
}

@Composable
fun PlayerView(
    service: PlayerService,
    onBack: () -> Unit,
    onSaveScreenshot: (File) -> Unit,
) {
    val context = LocalContext.current

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

    val playerObserver = PlayerService.Observer { command ->
        when (command) {
            is PlayerEvent.Shutdown -> context.activity.finish()
            PlayerEvent.Seek -> playState = playState.copy(isSeeking = false)
            else -> Unit
        }
    }

    val inPipMode = rememberIsInPipMode()
    val autoPip = BackgroundPlayPreference.current
    val shouldEnterPipMode = autoPip && playerState.mediaStarted && playState.isPlaying
    PipListenerPreAPI12(shouldEnterPipMode = shouldEnterPipMode)

    LifecycleStartEffect(Unit) {
        service.addObserver(playerObserver)
        onStopOrDispose {
            service.removeObserver(playerObserver)
        }
    }

    if (inPipMode) {
        PipContent(
            playState = playState,
            autoEnterPipMode = shouldEnterPipMode,
            onCommand = { service.onCommand(it) },
        )
    } else {
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

    PipBroadcastReceiver(playStateCallback = playStateCallback)

    var needPlayWhenResume by rememberSaveable { mutableStateOf(false) }

    val backgroundPlay = BackgroundPlayPreference.current
    OnLifecycleEvent { _, event ->
        when (event) {
            Lifecycle.Event.ON_RESUME -> {
                if (needPlayWhenResume) {
                    service.onCommand(PlayerCommand.Paused(false))
                }
            }

            Lifecycle.Event.ON_PAUSE -> {
                (playState.isPlaying && !backgroundPlay && !autoPip).let { condition ->
                    needPlayWhenResume = condition
                    if (condition) {
                        service.onCommand(PlayerCommand.Paused(true))
                    }
                }
            }

            Lifecycle.Event.ON_STOP -> {
                if (inPipMode) {    // Close button in PIP window is clicked
                    context.activity.finish()
                }
            }

            Lifecycle.Event.ON_DESTROY -> {
                if (!backgroundPlay) {
                    service.onCommand(PlayerCommand.Destroy)
                    service.removeObserver(playerObserver)
                }
            }

            else -> Unit
        }
    }
}

@Composable
private fun PipContent(
    playState: PlayState,
    autoEnterPipMode: Boolean,
    onCommand: (PlayerCommand) -> Unit,
) {
    if (playState.isVideo) {
        PlayerAndroidView(
            onCommand = onCommand,
            modifier = Modifier
                .pipParams(
                    context = LocalContext.current,
                    autoEnterPipMode = autoEnterPipMode,
                    isVideo = true,
                    playState = playState,
                )
                .fillMaxSize()
        )
    } else {
        var useThumbnailAny by rememberSaveable { mutableStateOf(true) }
        val thumbnailAny = playState.thumbnailAny
        val mediaThumbnail = playState.mediaThumbnail
        val contentScale = ContentScale.Fit
        val modifier = Modifier
            .fillMaxSize()
            .pipParams(
                context = LocalContext.current,
                autoEnterPipMode = autoEnterPipMode,
                isVideo = false,
                playState = playState,
            )
            .background(Color.Black)
        if (useThumbnailAny && thumbnailAny != null) {
            val painter = rememberAsyncImagePainter(
                model = thumbnailAny,
                onError = { useThumbnailAny = false },
            )
            Image(
                painter = painter,
                contentDescription = null,
                modifier = Modifier
                    .thenIfNotNull(painter.intrinsicSize.ratio) { aspectRatio(it) }
                    .then(modifier),
                contentScale = contentScale,
            )
        } else if (mediaThumbnail != null) {
            Image(
                bitmap = mediaThumbnail,
                contentDescription = null,
                modifier = Modifier
                    .thenIfNotNull(mediaThumbnail.size().ratio) { aspectRatio(it) }
                    .then(modifier),
                contentScale = contentScale,
            )
        }
    }
}

@Composable
private fun Content(
    playState: PlayState,
    playStateCallback: PlayStateCallback,
    dialogState: DialogState,
    dialogCallback: DialogCallback,
    onBack: () -> Unit,
    onSaveScreenshot: (File) -> Unit,
    onDialogVisibilityChanged: OnDialogVisibilityChanged,
    onCommand: (PlayerCommand) -> Unit,
) {
    ScreenOrientationHandler()
    val player = @Composable {
        PlayerAndroidView(
            onCommand = onCommand,
            modifier = Modifier.fillMaxSize()
        )
    }

    val configuration = LocalConfiguration.current
    if (configuration.screenIsLand) {
        LandscapePlayerView(
            playState = playState,
            playStateCallback = playStateCallback,
            dialogState = dialogState,
            onBack = onBack,
            onDialogVisibilityChanged = onDialogVisibilityChanged,
            onSaveScreenshot = onSaveScreenshot,
            onCommand = onCommand,
            playerContent = player,
        )
    } else {
        PortraitPlayerView(
            playState = playState,
            playStateCallback = playStateCallback,
            onDialogVisibilityChanged = onDialogVisibilityChanged,
            onBack = onBack,
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
private fun ScreenOrientationHandler() {
    val activity = LocalContext.current.activity
    LifecycleStartEffect(Unit) {
        val listener = object : OrientationEventListener(activity) {
            private var orientation = -1

            override fun onOrientationChanged(newOrientation: Int) {
                if (activity.isFinishing) return

                val lastOrientation = orientation
                if (newOrientation == ORIENTATION_UNKNOWN) {
                    orientation = ORIENTATION_UNKNOWN
                    return
                }
                when {
                    newOrientation > 350 || newOrientation < 10 -> orientation = 0
                    newOrientation in 80..100 -> orientation = 90
                    newOrientation in 170..190 -> orientation = 180
                    newOrientation in 260..280 -> orientation = 270
                }
                try {
                    val accelerometerRotationEnabled = Settings.System.getInt(
                        activity.contentResolver,
                        Settings.System.ACCELEROMETER_ROTATION
                    ) != 0
                    if (!accelerometerRotationEnabled) {
                        return
                    }
                } catch (e: Settings.SettingNotFoundException) {
                    e.printStackTrace()
                    return
                }

                if (lastOrientation != orientation) {
                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                }
            }
        }
        listener.enable()

        onStopOrDispose {
            listener.disable()
        }
    }
}