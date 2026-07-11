package com.skyd.podaura.ui.player

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.Lifecycle
import coil3.asImage
import coil3.compose.AsyncImage
import coil3.compose.rememberAsyncImagePainter
import com.skyd.compone.ext.ratio
import com.skyd.compone.ext.thenIfNotNull
import com.skyd.podaura.model.preference.player.BackgroundPlayPreference
import com.skyd.podaura.ui.component.OnLifecycleEvent
import com.skyd.podaura.ui.player.component.PlayerAndroidView
import com.skyd.podaura.ui.player.component.state.PlayState
import com.skyd.podaura.ui.player.component.state.PlayStateCallback
import com.skyd.podaura.ui.player.coordinator.PlayerCoordinator
import com.skyd.podaura.ui.player.pip.PipBroadcastReceiver
import com.skyd.podaura.ui.player.pip.PipListenerPreAPI12
import com.skyd.podaura.ui.player.pip.pipParams
import com.skyd.podaura.ui.player.pip.rememberIsInPipMode
import com.skyd.podaura.ui.player.service.PlayerState

@Composable
actual fun PlatformPlayerView(
    modifier: Modifier,
    onCommand: (PlayerCommand) -> Unit
) {
    PlayerAndroidView(
        onCommand = onCommand,
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
actual fun PlatformContent(
    modifier: Modifier,
    onBack: () -> Unit,
    service: PlayerCoordinator,
    playerObserver: PlayerCoordinator.Observer,
    playerState: PlayerState,
    playState: PlayState,
    playStateCallback: PlayStateCallback,
    commonContent: @Composable () -> Unit,
) {
    val inPipMode = rememberIsInPipMode()
    val autoPip = BackgroundPlayPreference.current
    val shouldEnterPipMode = autoPip && playerState.mediaStarted && playState.isPlaying
    PipListenerPreAPI12(shouldEnterPipMode = shouldEnterPipMode)

    if (inPipMode) {
        PipContent(
            playState = playState,
            autoEnterPipMode = shouldEnterPipMode,
            onCommand = { service.onCommand(it) },
        )
    } else {
        commonContent()
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
                    onBack
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
                    autoEnterPipMode = autoEnterPipMode,
                    isVideo = true,
                    playState = playState,
                )
                .fillMaxSize()
        )
    } else {
        var useThumbnailAny by rememberSaveable { mutableStateOf(true) }
        val thumbnailAny = playState.thumbnailAny
        val mediaThumbnail =
            remember(playState.mediaThumbnail) { playState.mediaThumbnail?.asImage() }
        val contentScale = ContentScale.Fit
        val modifier = Modifier
            .fillMaxSize()
            .pipParams(
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
            AsyncImage(
                model = mediaThumbnail,
                contentDescription = null,
                modifier = Modifier
                    .thenIfNotNull(
                        Size(
                            width = mediaThumbnail.width.toFloat(),
                            height = mediaThumbnail.height.toFloat(),
                        ).ratio
                    ) { aspectRatio(it) }
                    .then(modifier),
                contentScale = contentScale,
            )
        }
    }
}