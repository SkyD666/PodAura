package com.skyd.podaura.ui.screen.download

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.EventListener
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import com.skyd.compone.component.ComponeIconButton
import com.skyd.compone.component.blockString
import com.skyd.compone.ext.thenIf
import com.skyd.downloader.Status
import com.skyd.podaura.ext.fileSize
import com.skyd.podaura.model.download.ArticleDownloadInfoBean
import com.skyd.podaura.model.download.DownloadInfoBean
import com.skyd.podaura.ui.component.PodAuraImage
import com.skyd.podaura.ui.component.rememberPodAuraImageLoader
import com.skyd.podaura.ui.screen.feed.FeedIcon
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import podaura.downloader.generated.resources.download_cancelled
import podaura.downloader.generated.resources.download_paused
import podaura.downloader.generated.resources.download_retry
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.delete
import podaura.shared.generated.resources.download
import podaura.shared.generated.resources.download_completed
import podaura.shared.generated.resources.download_download_payload_rate
import podaura.shared.generated.resources.download_error_paused
import podaura.shared.generated.resources.download_initializing
import podaura.shared.generated.resources.download_pause
import podaura.shared.generated.resources.downloading
import podaura.shared.generated.resources.play

@Composable
fun DownloadItem(
    data: DownloadInfoBean,
    onPause: (DownloadInfoBean) -> Unit,
    onResume: (DownloadInfoBean) -> Unit,
    onRetry: (DownloadInfoBean) -> Unit,
    onDelete: (DownloadInfoBean) -> Unit,
    onPlay: ((DownloadInfoBean) -> Unit)? = null,
) {
    var description by remember { mutableStateOf(blockString(Res.string.download_initializing)) }
    var pauseButtonIcon by remember { mutableStateOf(Icons.Outlined.Pause) }
    var pauseButtonContentDescription by rememberSaveable { mutableStateOf("") }
    var pauseButtonEnabled by rememberSaveable { mutableStateOf(true) }
    var cancelButtonEnabled by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(data.status) {
        when (data.status) {
            Status.Downloading -> {
                pauseButtonEnabled = true
                pauseButtonIcon = Icons.Outlined.Pause
                pauseButtonContentDescription = getString(Res.string.download_pause)
                description = getString(Res.string.downloading)
            }

            Status.Failed -> {
                pauseButtonEnabled = true
                pauseButtonIcon = Icons.Outlined.Refresh
                pauseButtonContentDescription =
                    getString(podaura.downloader.generated.resources.Res.string.download_retry)
                description = getString(Res.string.download_error_paused)
            }

            Status.Cancelled -> {
                pauseButtonEnabled = true
                pauseButtonIcon = Icons.Outlined.Refresh
                pauseButtonContentDescription =
                    getString(podaura.downloader.generated.resources.Res.string.download_retry)
                description =
                    getString(podaura.downloader.generated.resources.Res.string.download_cancelled)
            }

            Status.Paused -> {
                pauseButtonEnabled = true
                pauseButtonIcon = Icons.Outlined.PlayArrow
                pauseButtonContentDescription = getString(Res.string.download)
                description =
                    getString(podaura.downloader.generated.resources.Res.string.download_paused)
            }

            Status.Init,
            Status.Started,
            Status.Queued -> {
                pauseButtonEnabled = false
                pauseButtonIcon = Icons.Outlined.PlayArrow
                pauseButtonContentDescription = getString(Res.string.download)
                description = getString(Res.string.download_initializing)
            }

            Status.Success -> {
                pauseButtonEnabled = false
                description = getString(Res.string.download_completed)
            }

        }
    }

    val canPlay = data.status == Status.Success && data.isPlayableMedia && onPlay != null
    val playLabel = stringResource(Res.string.play)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .thenIf(canPlay) {
                clickable(onClickLabel = playLabel) { onPlay?.invoke(data) }
            }
            .padding(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        data.articleDownloadInfo?.let { articleDownloadInfo ->
            ArticleDownloadArtwork(info = articleDownloadInfo)
            Spacer(modifier = Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = data.displayTitle,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = if (data.articleDownloadInfo == null) 4 else 2,
                overflow = TextOverflow.Ellipsis,
            )
            data.secondaryFileName?.let { fileName ->
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = fileName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        modifier = Modifier.padding(end = 12.dp),
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row {
                        Text(
                            modifier = Modifier.alignByBaseline(),
                            text = if (data.totalBytes == 0L) {
                                data.downloadedBytes.fileSize()
                            } else {
                                "${data.downloadedBytes * 100 / data.totalBytes}%"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .alignByBaseline(),
                            text = stringResource(
                                Res.string.download_download_payload_rate,
                                (data.speedInBytePerMs * 1000).toLong().fileSize() + "/s"
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (data.status != Status.Success) {
                    ComponeIconButton(
                        enabled = pauseButtonEnabled,
                        onClick = {
                            when (data.status) {
                                Status.Downloading -> {
                                    onPause(data)
                                    pauseButtonEnabled = false
                                }

                                Status.Paused -> {
                                    onResume(data)
                                    pauseButtonEnabled = false
                                }

                                Status.Failed -> {
                                    onRetry(data)
                                    pauseButtonEnabled = false
                                }

                                Status.Cancelled -> {
                                    onRetry(data)
                                    pauseButtonEnabled = false
                                }

                                Status.Started,
                                Status.Init,
                                Status.Queued,
                                Status.Success -> Unit
                            }
                        },
                        imageVector = pauseButtonIcon,
                        contentDescription = pauseButtonContentDescription,
                    )
                }
                ComponeIconButton(
                    enabled = cancelButtonEnabled,
                    onClick = {
                        onDelete(data)
                        pauseButtonEnabled = false
                        cancelButtonEnabled = false
                    },
                    imageVector = Icons.Outlined.Close,
                    contentDescription = stringResource(Res.string.delete)
                )
            }
            ProgressIndicator(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .fillMaxWidth(),
                data = data,
            )
        }
    }
}

@Composable
private fun ArticleDownloadArtwork(info: ArticleDownloadInfoBean) {
    val imageCandidates = remember(info.episodeImage, info.articleImage) {
        info.imageCandidates
    }
    var imageIndex by remember(imageCandidates) { mutableIntStateOf(0) }
    val image = imageCandidates.getOrNull(imageIndex)
    val shape = RoundedCornerShape(6.dp)

    if (image == null) {
        FeedIcon(
            data = info.feed,
            size = 50.dp,
            shape = shape,
        )
    } else {
        key(image) {
            PodAuraImage(
                modifier = Modifier
                    .size(50.dp)
                    .clip(shape),
                model = image,
                imageLoader = rememberPodAuraImageLoader(listener = object : EventListener() {
                    override fun onError(request: ImageRequest, result: ErrorResult) {
                        imageIndex++
                    }
                }),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Composable
private fun ProgressIndicator(
    modifier: Modifier = Modifier,
    data: DownloadInfoBean
) {
    when (data.status) {
        Status.Init,
        Status.Downloading,
        Status.Paused,
        Status.Failed,
        Status.Cancelled -> {
            if (data.status == Status.Downloading && data.totalBytes == 0L) {
                LinearProgressIndicator(modifier = modifier)
                return
            }
            val animatedProgress by animateFloatAsState(
                targetValue = if (data.totalBytes == 0L) 0f else data.downloadedBytes.toFloat() / data.totalBytes,
                animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
                label = "progressIndicatorAnimatedProgress"
            )
            LinearProgressIndicator(
                modifier = modifier,
                progress = { animatedProgress },
            )
        }

        Status.Started,
        Status.Queued -> LinearProgressIndicator(modifier = modifier)

        Status.Success -> LinearProgressIndicator(
            modifier = modifier,
            progress = { 1f },
        )
    }
}
