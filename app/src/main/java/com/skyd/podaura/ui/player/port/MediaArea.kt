package com.skyd.podaura.ui.player.port

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.EventListener
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import com.skyd.podaura.ui.component.PodAuraImage
import com.skyd.podaura.ui.component.rememberPodAuraImageLoader
import com.skyd.podaura.ui.player.component.state.PlayState
import com.skyd.podaura.util.coil.localmedia.LocalMediaFetcher

@Composable
internal fun MediaArea(
    playState: PlayState,
    modifier: Modifier = Modifier,
    playerContent: @Composable () -> Unit,
) {
    val isVideo = playState.isVideo
    Box(
        modifier = modifier
            .padding(6.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .animateContentSize(),
    ) {
        if (isVideo) {
            Box(modifier = Modifier.aspectRatio(1f)) { playerContent() }
        } else {
            Thumbnail(playState.thumbnail ?: playState.mediaThumbnail ?: playState.thumbnailAny)
        }
    }
}

@Composable
private fun Thumbnail(thumbnail: Any?) {
    var imageLoadFailed by rememberSaveable(thumbnail) { mutableStateOf(thumbnail == null) }
    val modifier = Modifier
        .aspectRatio(1f)
        .fillMaxSize()
    if (imageLoadFailed) {
        Card(modifier = modifier) {

        }
    } else {
        PodAuraImage(
            modifier = modifier,
            model = thumbnail,
            imageLoader = rememberPodAuraImageLoader(
                listener = object : EventListener() {
                    override fun onError(request: ImageRequest, result: ErrorResult) {
                        imageLoadFailed = true
                    }
                },
                components = { add(LocalMediaFetcher.Factory()) },
            ),
            contentScale = ContentScale.Crop,
        )
    }
}