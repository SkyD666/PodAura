package com.skyd.podaura.ui.player.port

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
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
    Box(
        modifier = modifier
            .padding(horizontal = 2.dp, vertical = 6.dp)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        val modifier = Modifier
            .aspectRatio(1f)
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .heightIn(min = 20.dp)
        val isVideo = playState.isVideo
        if (isVideo) {
            Box(modifier = modifier) { playerContent() }
        } else {
            Thumbnail(
                modifier = modifier,
                thumbnail = playState.thumbnail
                    ?: playState.mediaThumbnail
                    ?: playState.thumbnailAny,
            )
        }
    }
}

@Composable
private fun Thumbnail(modifier: Modifier, thumbnail: Any?) {
    var imageLoadFailed by rememberSaveable(thumbnail) { mutableStateOf(thumbnail == null) }
    if (imageLoadFailed) {
        Card(modifier = modifier) {

        }
    } else {
        val contentScale = ContentScale.Crop
        if (thumbnail is ImageBitmap) {
            Image(
                bitmap = thumbnail,
                contentDescription = null,
                modifier = modifier,
                contentScale = contentScale,
            )
        } else {
            PodAuraImage(
                model = thumbnail,
                modifier = modifier,
                imageLoader = rememberPodAuraImageLoader(
                    listener = object : EventListener() {
                        override fun onError(request: ImageRequest, result: ErrorResult) {
                            imageLoadFailed = true
                        }
                    },
                    components = { add(LocalMediaFetcher.Factory()) },
                ),
                contentScale = contentScale,
            )
        }
    }
}