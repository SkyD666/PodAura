package com.skyd.podaura.ui.screen.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.EventListener
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import com.skyd.podaura.ext.firstCodePointOrNull
import com.skyd.podaura.model.bean.feed.FeedBean
import com.skyd.podaura.ui.component.PodAuraImage
import com.skyd.podaura.ui.component.rememberPodAuraImageLoader


@Composable
fun FeedIcon(modifier: Modifier = Modifier, data: FeedBean, size: Dp = 22.dp) {
    val defaultIcon: @Composable () -> Unit = {
        Box(
            modifier = modifier
                .size(size)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = (data.nickname?.takeIf { it.isNotEmpty() } ?: data.title)
                    ?.firstCodePointOrNull().orEmpty(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
    var imageLoadError by rememberSaveable(data) { mutableStateOf(false) }

    var icon by remember(data) { mutableStateOf(data.customIcon.orEmpty().ifBlank { data.icon }) }
    if (icon.isNullOrBlank() || imageLoadError) {
        defaultIcon()
    } else {
        PodAuraImage(
            modifier = modifier
                .size(size)
                .clip(CircleShape),
            model = icon,
            imageLoader = rememberPodAuraImageLoader(listener = object : EventListener() {
                override fun onError(request: ImageRequest, result: ErrorResult) {
                    if (icon == data.customIcon) {
                        icon = data.icon
                    } else {
                        imageLoadError = true
                    }
                }
            }),
            contentScale = ContentScale.Crop,
        )
    }
}