package com.skyd.podaura.ui.player

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import coil3.asImage
import coil3.compose.AsyncImage
import coil3.compose.rememberAsyncImagePainter
import com.skyd.compone.ext.ratio
import com.skyd.compone.ext.thenIfNotNull
import com.skyd.podaura.model.preference.player.PlayerAutoPipPreference
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
    coordinator: PlayerCoordinator,
    modifier: Modifier,
    onCommand: (PlayerCommand) -> Unit
) {
    PlayerAndroidView(
        onCommand = onCommand,
        modifier = modifier,
    )
}

// The service owns screen/background pauses, including playback without an Activity.
@Composable
actual fun PlatformPlayerLifecycleEffect(coordinator: PlayerCoordinator) = Unit

@Composable
actual fun PlatformContent(
    modifier: Modifier,
    onBack: () -> Unit,
    coordinator: PlayerCoordinator,
    playerState: PlayerState,
    playState: PlayState,
    playStateCallback: PlayStateCallback,
    commonContent: @Composable () -> Unit,
) {
    val inPipMode = rememberIsInPipMode()
    val shouldEnterPipMode = PlayerAutoPipPreference.current &&
            playerState.mediaStarted && playState.isPlaying
    PipListenerPreAPI12(shouldEnterPipMode = shouldEnterPipMode)

    // Android 12+ needs auto-enter parameters before the first background
    // transition. Keep the same builder alive across normal/PiP content.
    Box(
        modifier = modifier.pipParams(
            autoEnterPipMode = shouldEnterPipMode,
            isVideo = playState.isVideo,
            playState = playState,
            includeBounds = false,
        ),
    ) {
        if (inPipMode) {
            PipContent(
                playState = playState,
                autoEnterPipMode = shouldEnterPipMode,
                onCommand = { coordinator.onCommand(it) },
            )
        } else {
            commonContent()
        }
    }

    PipBroadcastReceiver(playStateCallback = playStateCallback)

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
                .fillMaxSize()
                .pipParams(autoEnterPipMode, true, playState)
        )
    } else {
        var useThumbnailAny by rememberSaveable { mutableStateOf(true) }
        val thumbnailAny = playState.thumbnailAny
        val mediaThumbnail =
            remember(playState.mediaThumbnail) { playState.mediaThumbnail?.asImage() }
        val contentScale = ContentScale.Fit
        val modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pipParams(autoEnterPipMode, false, playState)
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
