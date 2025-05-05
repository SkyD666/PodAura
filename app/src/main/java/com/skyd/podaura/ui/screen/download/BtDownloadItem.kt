package com.skyd.podaura.ui.screen.download

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import com.skyd.podaura.ext.fileSize
import com.skyd.podaura.ext.toPercentage
import com.skyd.podaura.model.bean.download.bt.BtDownloadInfoBean
import com.skyd.podaura.ui.component.PodAuraIconButton
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.delete
import podaura.shared.generated.resources.download
import podaura.shared.generated.resources.download_click_to_seeding
import podaura.shared.generated.resources.download_completed
import podaura.shared.generated.resources.download_download_payload_rate
import podaura.shared.generated.resources.download_error_paused
import podaura.shared.generated.resources.download_initializing
import podaura.shared.generated.resources.download_pause
import podaura.shared.generated.resources.download_paused
import podaura.shared.generated.resources.download_peer_count
import podaura.shared.generated.resources.download_retry
import podaura.shared.generated.resources.download_seeding
import podaura.shared.generated.resources.download_upload_payload_rate
import podaura.shared.generated.resources.downloading

@Composable
fun BtDownloadItem(
    data: BtDownloadInfoBean,
    onPause: (BtDownloadInfoBean) -> Unit,
    onResume: (BtDownloadInfoBean) -> Unit,
    onCancel: (BtDownloadInfoBean) -> Unit,
) {
    val context = LocalContext.current
    var description by remember { mutableStateOf(data.description) }
    var pauseButtonIcon by remember { mutableStateOf(Icons.Outlined.Pause) }
    var pauseButtonContentDescription by rememberSaveable { mutableStateOf("") }
    var pauseButtonEnabled by rememberSaveable { mutableStateOf(true) }
    var cancelButtonEnabled by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(data.downloadState) {
        when (data.downloadState) {
            BtDownloadInfoBean.DownloadState.Seeding -> {
                pauseButtonEnabled = true
                pauseButtonIcon = Icons.Outlined.Pause
                pauseButtonContentDescription = getString(Res.string.download_pause)
                description = getString(Res.string.download_seeding)
            }

            BtDownloadInfoBean.DownloadState.Downloading -> {
                pauseButtonEnabled = true
                pauseButtonIcon = Icons.Outlined.Pause
                pauseButtonContentDescription = getString(Res.string.download_pause)
                description = getString(Res.string.downloading)
            }

            BtDownloadInfoBean.DownloadState.StorageMovedFailed,
            BtDownloadInfoBean.DownloadState.ErrorPaused -> {
                pauseButtonEnabled = true
                pauseButtonIcon = Icons.Outlined.Refresh
                pauseButtonContentDescription = getString(Res.string.download_retry)
                description = getString(Res.string.download_error_paused)
            }

            BtDownloadInfoBean.DownloadState.SeedingPaused -> {
                pauseButtonEnabled = true
                pauseButtonIcon = Icons.Outlined.CloudUpload
                pauseButtonContentDescription = getString(Res.string.download_click_to_seeding)
                description = getString(Res.string.download_paused)
            }

            BtDownloadInfoBean.DownloadState.Paused -> {
                pauseButtonEnabled = true
                pauseButtonIcon = Icons.Outlined.PlayArrow
                pauseButtonContentDescription = getString(Res.string.download)
                description = getString(Res.string.download_paused)
            }

            BtDownloadInfoBean.DownloadState.Init -> {
                pauseButtonEnabled = false
                pauseButtonIcon = Icons.Outlined.PlayArrow
                pauseButtonContentDescription = getString(Res.string.download)
                description = getString(Res.string.download_initializing)
            }

            BtDownloadInfoBean.DownloadState.Completed -> {
                pauseButtonEnabled = true
                pauseButtonIcon = Icons.Outlined.CloudUpload
                pauseButtonContentDescription = getString(Res.string.download_click_to_seeding)
                description = getString(Res.string.download_completed)
            }
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = data.name,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row {
                    description?.let { desc ->
                        Text(
                            modifier = Modifier.padding(end = 12.dp),
                            text = desc,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = stringResource(
                            Res.string.download_peer_count,
                            data.peerInfoList.count()
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row {
                    Text(
                        modifier = Modifier.alignByBaseline(),
                        text = data.progress.toPercentage(),
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
                            data.downloadPayloadRate.toLong().fileSize(context) + "/s"
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .alignByBaseline(),
                        text = stringResource(
                            Res.string.download_upload_payload_rate,
                            data.uploadPayloadRate.toLong().fileSize(context) + "/s"
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
                    when (data.downloadState) {
                        BtDownloadInfoBean.DownloadState.Seeding,
                        BtDownloadInfoBean.DownloadState.Downloading -> onPause(data)

                        BtDownloadInfoBean.DownloadState.SeedingPaused,
                        BtDownloadInfoBean.DownloadState.Paused -> onResume(data)

                        BtDownloadInfoBean.DownloadState.Completed,
                        BtDownloadInfoBean.DownloadState.StorageMovedFailed,
                        BtDownloadInfoBean.DownloadState.ErrorPaused -> onResume(data)

                        else -> Unit
                    }
                },
                imageVector = pauseButtonIcon,
                contentDescription = pauseButtonContentDescription,
            )
            PodAuraIconButton(
                enabled = cancelButtonEnabled,
                onClick = {
                    onCancel(data)
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
    data: BtDownloadInfoBean
) {
    when (data.downloadState) {
        BtDownloadInfoBean.DownloadState.Downloading,
        BtDownloadInfoBean.DownloadState.StorageMovedFailed,
        BtDownloadInfoBean.DownloadState.ErrorPaused,
        BtDownloadInfoBean.DownloadState.Paused -> {
            LinearProgressIndicator(modifier = modifier, progress = { data.progress })
        }

        BtDownloadInfoBean.DownloadState.Init -> LinearProgressIndicator(modifier = modifier)
        BtDownloadInfoBean.DownloadState.Seeding,
        BtDownloadInfoBean.DownloadState.SeedingPaused,
        BtDownloadInfoBean.DownloadState.Completed -> LinearProgressIndicator(
            modifier = modifier,
            progress = { 1f },
        )
    }
}