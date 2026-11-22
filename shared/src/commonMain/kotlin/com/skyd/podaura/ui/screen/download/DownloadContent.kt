package com.skyd.podaura.ui.screen.download

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skyd.mvi.getDispatcher
import com.skyd.podaura.model.download.DownloadInfoBean
import com.skyd.podaura.model.repository.download.DownloadManager
import com.skyd.podaura.model.repository.download.DownloadStarter
import com.skyd.podaura.model.repository.download.rememberDownloadStarter
import com.skyd.podaura.ui.component.CircularProgressPlaceholder
import com.skyd.podaura.ui.component.EmptyPlaceholder
import com.skyd.podaura.ui.component.dialog.TextFieldDialog
import com.skyd.podaura.ui.player.jumper.PlayDataMode
import com.skyd.podaura.ui.player.jumper.rememberPlayerJumper
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.download
import podaura.shared.generated.resources.download_screen_add_download_hint
import podaura.shared.generated.resources.download_without_notifications_tip
import podaura.shared.generated.resources.media_not_exists
import podaura.shared.generated.resources.open_notification_settings
import kotlin.time.Duration.Companion.milliseconds

@Composable
internal fun DownloadContent(
    viewModel: DownloadViewModel,
    snackbarHostState: SnackbarHostState,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    focus: DownloadListFocus? = DownloadListFocus(),
) {
    val uiState by viewModel.viewState.collectAsStateWithLifecycle()
    viewModel.getDispatcher(startWith = DownloadIntent.Init)
    val scope = rememberCoroutineScope()
    val playerJumper = rememberPlayerJumper()
    val mediaNotExistsMessage = stringResource(Res.string.media_not_exists)
    val state = uiState.downloadListState

    when (state) {
        is DownloadListState.Failed -> Unit
        is DownloadListState.Success if focus != null -> {
            DownloadList(
                downloadInfoBeanList = state.downloadInfoBeanList,
                focus = focus,
                modifier = modifier,
                contentPadding = contentPadding,
                onPlay = { item ->
                    scope.launch {
                        val path = item.resolveExistingLocalPath()
                        if (path == null) {
                            snackbarHostState.showSnackbar(mediaNotExistsMessage)
                        } else {
                            playerJumper.jump(item.toPlayDataMode(path))
                        }
                    }
                },
            )
        }

        else -> CircularProgressPlaceholder(contentPadding = contentPadding)
    }
}

@Composable
internal fun AddDownloadDialog(
    link: String?,
    onLinkChange: (String?) -> Unit,
    snackbarHostState: SnackbarHostState,
    mimetype: String? = null,
) {
    val scope = rememberCoroutineScope()
    val silentDownloadMessage = stringResource(Res.string.download_without_notifications_tip)
    val settingsAction = stringResource(Res.string.open_notification_settings)
    val starterHolder = remember { mutableStateOf<DownloadStarter?>(null) }
    val downloadStarter = rememberDownloadStarter {
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = silentDownloadMessage,
                actionLabel = settingsAction,
            )
            if (result == SnackbarResult.ActionPerformed) {
                starterHolder.value?.openNotificationSettings()
            }
        }
    }
    SideEffect { starterHolder.value = downloadStarter }
    TextFieldDialog(
        visible = link != null,
        icon = { Icon(imageVector = Icons.Outlined.Download, contentDescription = null) },
        titleText = stringResource(Res.string.download),
        value = link.orEmpty(),
        onValueChange = onLinkChange,
        placeholder = stringResource(Res.string.download_screen_add_download_hint),
        onDismissRequest = { onLinkChange(null) },
        onConfirm = { text ->
            onLinkChange(null)
            scope.launch { downloadStarter.download(url = text, type = mimetype) }
        },
    )
}

@Composable
internal fun DownloadList(
    downloadInfoBeanList: List<DownloadInfoBean>,
    focus: DownloadListFocus,
    modifier: Modifier,
    contentPadding: PaddingValues,
    onPlay: ((DownloadInfoBean) -> Unit)?,
) {
    val scope = rememberCoroutineScope()
    val initialIndex = remember { focus.initialIndex(downloadInfoBeanList) }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    var highlight by remember { mutableStateOf(focus.queued.isNotEmpty() || focus.existing.isNotEmpty()) }
    if (highlight) {
        LaunchedEffect(Unit) {
            delay(1800.milliseconds)
            highlight = false
        }
    }
    if (downloadInfoBeanList.isNotEmpty()) {
        LazyColumn(
            modifier = modifier.fillMaxHeight(),
            state = listState,
            contentPadding = contentPadding,
        ) {
            itemsIndexed(
                items = downloadInfoBeanList,
                key = { _, item -> item.id },
            ) { index, item ->
                if (index > 0) HorizontalDivider()
                DownloadItem(
                    data = item,
                    highlighted = highlight && (item.id in focus.queued || item.id in focus.existing),
                    onPause = { scope.launch { DownloadManager.instance.pause(item.id) } },
                    onResume = { scope.launch { DownloadManager.instance.resume(item.id) } },
                    onRetry = { scope.launch { DownloadManager.instance.retry(item.id) } },
                    onDelete = { scope.launch { DownloadManager.instance.delete(item.id) } },
                    onPlay = onPlay,
                )
            }
        }
    } else {
        EmptyPlaceholder(contentPadding = contentPadding)
    }
}

private suspend fun DownloadInfoBean.resolveExistingLocalPath(): String? =
    withContext(Dispatchers.IO) {
        runCatching {
            val file = PlatformFile(PlatformFile(path), fileName)
            file.path.takeIf { file.exists() }
        }.getOrNull()
    }

internal fun DownloadInfoBean.toPlayDataMode(localPath: String) =
    PlayDataMode.MediaLibraryList(
        startMediaPath = localPath,
        mediaList = listOf(
            PlayDataMode.MediaLibraryList.PlayMediaListItem(
                path = localPath,
                articleId = articleDownloadSource?.articleId,
                title = displayTitle,
                thumbnail = articleDownloadInfo?.imageCandidates?.firstOrNull(),
                historyUrl = url,
            )
        ),
    )
