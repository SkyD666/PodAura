package com.skyd.anivu.ui.screen.download

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.skyd.anivu.ext.fileSize
import com.skyd.anivu.ext.getString
import com.skyd.anivu.model.bean.download.DownloadInfoBean
import com.skyd.anivu.ui.component.PodAuraIconButton
import com.skyd.downloader.Status
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.delete
import podaura.shared.generated.resources.download
import podaura.shared.generated.resources.download_completed
import podaura.shared.generated.resources.download_download_payload_rate
import podaura.shared.generated.resources.download_error_paused
import podaura.shared.generated.resources.download_initializing
import podaura.shared.generated.resources.download_pause
import podaura.shared.generated.resources.download_paused
import podaura.shared.generated.resources.download_retry
import podaura.shared.generated.resources.downloading

@Composable
fun DownloadItem(
    data: DownloadInfoBean,
    onPause: (DownloadInfoBean) -> Unit,
    onResume: (DownloadInfoBean) -> Unit,
    onRetry: (DownloadInfoBean) -> Unit,
    onDelete: (DownloadInfoBean) -> Unit,
) {
    val context = LocalContext.current
    var description by remember { mutableStateOf(context.getString(Res.string.download_initializing)) }
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
                pauseButtonContentDescription = getString(Res.string.download_retry)
                description = getString(Res.string.download_error_paused)
            }

            Status.Paused -> {
                pauseButtonEnabled = true
                pauseButtonIcon = Icons.Outlined.PlayArrow
                pauseButtonContentDescription = getString(Res.string.download)
                description = getString(Res.string.download_paused)
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
                pauseButtonIcon = Icons.Outlined.PlayArrow
                pauseButtonContentDescription = getString(Res.string.delete)
                description = getString(Res.string.download_completed)
            }

        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = data.fileName,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
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
                        text = "${if (data.totalBytes == 0L) 0 else (data.downloadedBytes * 100 / data.totalBytes)}%",
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
                            (data.speedInBytePerMs * 1000).toLong().fileSize(context) + "/s"
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            PodAuraIconButton(
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

                        Status.Started,
                        Status.Init,
                        Status.Queued,
                        Status.Success -> Unit
                    }
                },
                imageVector = pauseButtonIcon,
                contentDescription = pauseButtonContentDescription,
            )
            PodAuraIconButton(
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

@Composable
private fun ProgressIndicator(
    modifier: Modifier = Modifier,
    data: DownloadInfoBean
) {
    when (data.status) {
        Status.Init,
        Status.Downloading,
        Status.Paused,
        Status.Failed -> {
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