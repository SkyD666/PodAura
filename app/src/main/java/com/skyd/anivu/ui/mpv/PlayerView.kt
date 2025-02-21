package com.skyd.anivu.ui.mpv

import android.content.pm.ActivityInfo
import android.provider.Settings
import android.view.OrientationEventListener
import androidx.compose.foundation.background
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
import com.skyd.anivu.ext.activity
import com.skyd.anivu.ext.screenIsLand
import com.skyd.anivu.ui.component.OnLifecycleEvent
import com.skyd.anivu.ui.component.PodAuraImage
import com.skyd.anivu.ui.local.LocalBackgroundPlay
import com.skyd.anivu.ui.local.LocalPlayerAutoPip
import com.skyd.anivu.ui.mpv.component.PlayerAndroidView
import com.skyd.anivu.ui.mpv.component.dialog.AudioTrackDialog
import com.skyd.anivu.ui.mpv.component.dialog.SpeedDialog
import com.skyd.anivu.ui.mpv.component.dialog.SubtitleTrackDialog
import com.skyd.anivu.ui.mpv.component.state.PlayState
import com.skyd.anivu.ui.mpv.component.state.PlayStateCallback
import com.skyd.anivu.ui.mpv.component.state.dialog.DialogCallback
import com.skyd.anivu.ui.mpv.component.state.dialog.DialogState
import com.skyd.anivu.ui.mpv.component.state.dialog.OnDialogVisibilityChanged
import com.skyd.anivu.ui.mpv.component.state.dialog.SpeedDialogCallback
import com.skyd.anivu.ui.mpv.component.state.dialog.SpeedDialogState
import com.skyd.anivu.ui.mpv.component.state.dialog.track.AudioTrackDialogCallback
import com.skyd.anivu.ui.mpv.component.state.dialog.track.AudioTrackDialogState
import com.skyd.anivu.ui.mpv.component.state.dialog.track.SubtitleTrackDialogCallback
import com.skyd.anivu.ui.mpv.component.state.dialog.track.SubtitleTrackDialogState
import com.skyd.anivu.ui.mpv.land.LandscapePlayerView
import com.skyd.anivu.ui.mpv.pip.PipBroadcastReceiver
import com.skyd.anivu.ui.mpv.pip.PipListenerPreAPI12
import com.skyd.anivu.ui.mpv.pip.pipParams
import com.skyd.anivu.ui.mpv.pip.rememberIsInPipMode
import com.skyd.anivu.ui.mpv.port.PortraitPlayerView
import com.skyd.anivu.ui.mpv.service.PlayerService
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

    val dialogState by remember {
        mutableStateOf(
            DialogState(
                speedDialogState = { speedDialogState },
                audioTrackDialogState = { audioTrackDialogState },
                subtitleTrackDialogState = { subtitleTrackDialogState },
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
            ),
            subtitleTrackDialogCallback = SubtitleTrackDialogCallback(
                onSubtitleTrackChanged = { service.onCommand(PlayerCommand.SetSubtitleTrack(it.trackId)) },
                onAddSubtitle = { service.onCommand(PlayerCommand.AddSubtitle(it)) },
            )
        )
    }

    LaunchedEffect(playerState) {
        playState = playState.copy(
            isPlaying = !playerState.paused && playerState.mediaLoaded,
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
    val autoPip = LocalPlayerAutoPip.current
    val shouldEnterPipMode = autoPip && playerState.mediaLoaded && playState.isPlaying
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

    val backgroundPlay = LocalBackgroundPlay.current
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
        PodAuraImage(
            model = playState.thumbnail ?: playState.mediaThumbnail,
            modifier = Modifier
                .pipParams(
                    context = LocalContext.current,
                    autoEnterPipMode = autoEnterPipMode,
                    isVideo = false,
                    playState = playState,
                )
                .background(Color.Black),
            contentScale = ContentScale.Fit,
        )
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
    )
    SubtitleTrackDialog(
        onDismissRequest = { onDialogVisibilityChanged.onSubtitleTrackDialog(false) },
        playState = { playState },
        subtitleTrackDialogState = dialogState.subtitleTrackDialogState,
        subtitleTrackDialogCallback = dialogCallback.subtitleTrackDialogCallback,
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