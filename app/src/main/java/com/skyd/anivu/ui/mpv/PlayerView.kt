package com.skyd.anivu.ui.mpv

import android.net.Uri
import android.view.SurfaceView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil3.Bitmap
import com.skyd.anivu.ext.activity
import com.skyd.anivu.ext.collectIn
import com.skyd.anivu.ui.component.OnLifecycleEvent
import com.skyd.anivu.ui.component.rememberSystemUiController
import com.skyd.anivu.ui.local.LocalBackgroundPlay
import com.skyd.anivu.ui.local.LocalPlayerAutoPip
import com.skyd.anivu.ui.mpv.controller.PlayerController
import com.skyd.anivu.ui.mpv.controller.bar.BottomBarCallback
import com.skyd.anivu.ui.mpv.controller.bar.TopBarCallback
import com.skyd.anivu.ui.mpv.controller.state.PlayState
import com.skyd.anivu.ui.mpv.controller.state.PlayStateCallback
import com.skyd.anivu.ui.mpv.controller.state.TransformState
import com.skyd.anivu.ui.mpv.controller.state.TransformStateCallback
import com.skyd.anivu.ui.mpv.controller.state.dialog.DialogCallback
import com.skyd.anivu.ui.mpv.controller.state.dialog.DialogState
import com.skyd.anivu.ui.mpv.controller.state.dialog.OnDismissDialog
import com.skyd.anivu.ui.mpv.controller.state.dialog.SpeedDialogCallback
import com.skyd.anivu.ui.mpv.controller.state.dialog.SpeedDialogState
import com.skyd.anivu.ui.mpv.controller.state.dialog.track.AudioTrackDialogCallback
import com.skyd.anivu.ui.mpv.controller.state.dialog.track.AudioTrackDialogState
import com.skyd.anivu.ui.mpv.controller.state.dialog.track.SubtitleTrackDialogCallback
import com.skyd.anivu.ui.mpv.controller.state.dialog.track.SubtitleTrackDialogState
import com.skyd.anivu.ui.mpv.pip.PipBroadcastReceiver
import com.skyd.anivu.ui.mpv.pip.PipListenerPreAPI12
import com.skyd.anivu.ui.mpv.pip.manualEnterPictureInPictureMode
import com.skyd.anivu.ui.mpv.pip.pipParams
import com.skyd.anivu.ui.mpv.pip.rememberIsInPipMode
import com.skyd.anivu.ui.mpv.service.PlayerService
import java.io.File


@Composable
fun PlayerViewRoute(
    service: PlayerService?,
    uri: Uri,
    title: String? = null,
    thumbnail: Bitmap? = null,
    onBack: () -> Unit,
    onSaveScreenshot: (File) -> Unit,
) {
    if (service != null) {
        PlayerView(service, uri, title, thumbnail, onBack, onSaveScreenshot)
    }
}

@Composable
fun PlayerView(
    service: PlayerService,
    uri: Uri,
    title: String? = null,
    thumbnail: Bitmap? = null,
    onBack: () -> Unit,
    onSaveScreenshot: (File) -> Unit,
) {
    val systemUiController = rememberSystemUiController().apply {
        isSystemBarsVisible = false
        systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var mediaLoaded by rememberSaveable { mutableStateOf(false) }

    var manualPip by rememberSaveable { mutableStateOf(false) }

    var playState by remember { mutableStateOf(PlayState.initial) }
    var transformState by remember { mutableStateOf(TransformState.initial) }

    var subtitleTrackDialogState by remember { mutableStateOf(SubtitleTrackDialogState.initial) }
    var audioTrackDialogState by remember { mutableStateOf(AudioTrackDialogState.initial) }
    var speedDialogState by remember { mutableStateOf(SpeedDialogState.initial) }
    LaunchedEffect(playState.speed) {
        speedDialogState = speedDialogState.copy(currentSpeed = playState.speed)
    }

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
            onPlayStateChanged = {
                service.onCommand(PlayerCommand.Paused(playState.isPlaying, uri))
            },
            onPlayOrPause = { service.onCommand(PlayerCommand.PlayOrPause) },
            onSeekTo = {
                playState = playState.copyIfNecessary(isSeeking = true)
                service.onCommand(PlayerCommand.SeekTo(it))
            },
            onSpeedChanged = { service.onCommand(PlayerCommand.SetSpeed(it)) },
        )
    }
    val transformStateCallback = remember {
        TransformStateCallback(
            onVideoRotate = { service.onCommand(PlayerCommand.Rotate(it.toInt())) },
            onVideoZoom = { service.onCommand(PlayerCommand.Zoom(it)) },
            onVideoOffset = { service.onCommand(PlayerCommand.VideoOffset(it)) },
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
    val currentOnBack by rememberUpdatedState(newValue = onBack)
    val topBarCallback = remember {
        TopBarCallback(
            onBack = currentOnBack,
            onPictureInPicture = {
                manualPip = true
                context.activity.manualEnterPictureInPictureMode()
            },
        )
    }
    val bottomBarCallback = remember {
        BottomBarCallback(
            onSpeedClick = { speedDialogState = speedDialogState.copy(show = true) },
            onAudioTrackClick = {
                audioTrackDialogState = audioTrackDialogState.copyIfNecessary(show = true)
            },
            onSubtitleTrackClick = {
                subtitleTrackDialogState = subtitleTrackDialogState.copyIfNecessary(show = true)
            },
        )
    }

    LaunchedEffect(Unit) {
        service.playerState.collectIn(lifecycleOwner) { state ->
            mediaLoaded = state.mediaLoaded
            audioTrackDialogState = audioTrackDialogState.copyIfNecessary(
                audioTrack = state.audioTracks,
                currentAudioTrack = state.audioTracks.find { it.trackId == state.audioTrackId }
                    ?: audioTrackDialogState.currentAudioTrack
            )
            subtitleTrackDialogState = subtitleTrackDialogState.copyIfNecessary(
                subtitleTrack = state.subtitleTracks,
                currentSubtitleTrack = state.subtitleTracks.find { it.trackId == state.subtitleTrackId }
                    ?: subtitleTrackDialogState.currentSubtitleTrack
            )
            playState = playState.copyIfNecessary(
                isPlaying = !state.paused && state.mediaLoaded,
                bufferDuration = state.buffer,
                duration = state.duration.toInt(),
                currentPosition = state.position.toInt(),
                speed = state.speed,
                mediaTitle = state.mediaTitle.orEmpty(),
                title = state.title.orEmpty(),
            )
            transformState = transformState.copyIfNecessary(
                videoRotate = state.rotate,
                videoOffset = Offset(x = state.offsetX, y = state.offsetY),
                videoZoom = state.zoom,
            )
        }
    }

    val playerObserver = PlayerService.Observer { command ->
        when (command) {
            is PlayerEvent.Shutdown -> context.activity.finish()
            PlayerEvent.Seek -> playState = playState.copyIfNecessary(isSeeking = false)
            else -> Unit
        }
    }

    val autoPip = LocalPlayerAutoPip.current
    val shouldEnterPipMode = (autoPip || manualPip) && mediaLoaded && playState.isPlaying
    PipListenerPreAPI12(shouldEnterPipMode = shouldEnterPipMode)
    AndroidView(
        modifier = Modifier
            .fillMaxSize()
            .pipParams(
                context = LocalContext.current,
                shouldEnterPipMode = shouldEnterPipMode,
                playState = playState,
            ),
        factory = { c ->
            SurfaceView(c, null).apply {
                service.onCommand(PlayerCommand.Attach(holder))
            }
        },
        onRelease = {
            service.onCommand(PlayerCommand.Detach(it.holder.surface))
        }
    )

    LaunchedEffect(uri) {
        service.addObserver(playerObserver)
        service.onCommand(PlayerCommand.SetUri(uri))
    }
    LaunchedEffect(title) {
        if (title != null) {
            service.onCommand(PlayerCommand.SetTitle(title))
        }
    }
    LaunchedEffect(thumbnail) {
        if (thumbnail != null) {
            service.onCommand(PlayerCommand.SetThumbnail(thumbnail))
        }
    }

    val inPipMode = rememberIsInPipMode()

    if (!inPipMode) {
        PlayerController(
            enabled = { mediaLoaded },
            playState = { playState },
            playStateCallback = playStateCallback,
            topBarCallback = topBarCallback,
            bottomBarCallback = bottomBarCallback,
            dialogState = dialogState,
            dialogCallback = dialogCallback,
            onDismissDialog = remember {
                OnDismissDialog(
                    onDismissSpeedDialog = {
                        speedDialogState = speedDialogState.copy(show = false)
                    },
                    onDismissAudioTrackDialog = {
                        audioTrackDialogState = audioTrackDialogState.copyIfNecessary(show = false)
                    },
                    onDismissSubtitleTrackDialog = {
                        subtitleTrackDialogState =
                            subtitleTrackDialogState.copyIfNecessary(show = false)
                    },
                )
            },
            transformState = { transformState },
            transformStateCallback = transformStateCallback,
            onScreenshot = { service.onCommand(PlayerCommand.Screenshot(onSaveScreenshot)) },
        )
    }

    PipBroadcastReceiver(playStateCallback = playStateCallback)

    var needPlayWhenResume by rememberSaveable { mutableStateOf(false) }

    val backgroundPlay = LocalBackgroundPlay.current
    OnLifecycleEvent { _, event ->
        when (event) {
            Lifecycle.Event.ON_RESUME -> {
                if (inPipMode) {    // Expand button in PIP window is clicked
                    if (manualPip) manualPip = false
                }
                if (needPlayWhenResume) {
                    service.onCommand(PlayerCommand.Paused(false, uri))
                }
                systemUiController.isSystemBarsVisible = false
                systemUiController.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }

            Lifecycle.Event.ON_PAUSE -> {
                (playState.isPlaying && !backgroundPlay && !autoPip && !manualPip).let { condition ->
                    needPlayWhenResume = condition
                    if (condition) {
                        service.onCommand(PlayerCommand.Paused(true, uri))
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