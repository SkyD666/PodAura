package com.skyd.podaura.ui.screen.download

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.skyd.compone.component.ComponeFloatingActionButton
import com.skyd.compone.component.ComponeScaffold
import com.skyd.compone.component.ComponeTopBar
import com.skyd.compone.component.ComponeTopBarStyle
import com.skyd.compone.ext.plus
import com.skyd.mvi.getDispatcher
import com.skyd.podaura.model.download.DownloadInfoBean
import com.skyd.podaura.model.repository.download.DownloadManager
import com.skyd.podaura.model.repository.download.rememberDownloadStarter
import com.skyd.podaura.ui.component.CircularProgressPlaceholder
import com.skyd.podaura.ui.component.EmptyPlaceholder
import com.skyd.podaura.ui.component.dialog.TextFieldDialog
import com.skyd.podaura.ui.component.navigation.ExternalUrlHandler
import com.skyd.podaura.ui.component.navigation.deeplink.DeepLinkPattern
import io.ktor.http.URLBuilder
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.download
import podaura.shared.generated.resources.download_screen_add_download
import podaura.shared.generated.resources.download_screen_add_download_hint
import podaura.shared.generated.resources.download_screen_name


@Serializable
data class DownloadRoute(
    val downloadLink: String? = null,
    val mimetype: String? = null,
) : NavKey {
    companion object {
        const val BASE_PATH = "podaura://download.screen"

        val deepLinkPattern = DeepLinkPattern(
            serializer(),
            urlPattern = URLBuilder(BASE_PATH).build()
        )

        @Composable
        fun DownloadLauncher(route: DownloadRoute) {
            DownloadScreen(downloadLink = route.downloadLink, mimetype = route.mimetype)
        }
    }
}

@Serializable
data class DownloadDeepLinkRoute(
    @SerialName(ExternalUrlHandler.UrlData.URL_NAME)
    val url: String? = null,
    @SerialName(ExternalUrlHandler.UrlData.MIMETYPE_NAME)
    val mimeType: String? = null,
) : NavKey {
    companion object {
        @Composable
        fun DownloadDeepLinkLauncher(route: DownloadDeepLinkRoute) {
            DownloadScreen(downloadLink = route.url, mimetype = route.mimeType)
        }
    }
}

expect val DownloadDeepLinkRoute.Companion.deepLinkPatterns: List<DeepLinkPattern<DownloadDeepLinkRoute>>

@Composable
fun DownloadScreen(
    downloadLink: String? = null,
    mimetype: String? = null,
    viewModel: DownloadViewModel = koinViewModel()
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var openLinkDialog by rememberSaveable { mutableStateOf(downloadLink) }

    LaunchedEffect(downloadLink) {
        openLinkDialog = downloadLink
    }

    var fabHeight by remember { mutableStateOf(0.dp) }

    val uiState by viewModel.viewState.collectAsStateWithLifecycle()
    viewModel.getDispatcher(startWith = DownloadIntent.Init)

    ComponeScaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            ComponeTopBar(
                style = ComponeTopBarStyle.CenterAligned,
                scrollBehavior = scrollBehavior,
                title = { Text(text = stringResource(Res.string.download_screen_name)) },
            )
        },
        floatingActionButton = {
            ComponeFloatingActionButton(
                onClick = { openLinkDialog = "" },
                contentDescription = stringResource(Res.string.download_screen_add_download),
                onSizeWithSinglePaddingChanged = { _, height -> fabHeight = height },
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = stringResource(Res.string.download_screen_add_download),
                )
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        when (val downloadListState = uiState.downloadListState) {
            is DownloadListState.Failed -> Unit
            DownloadListState.Init,
            DownloadListState.Loading -> CircularProgressPlaceholder(contentPadding = innerPadding)

            is DownloadListState.Success -> DownloadList(
                downloadInfoBeanList = downloadListState.downloadInfoBeanList,
                nestedScrollConnection = scrollBehavior.nestedScrollConnection,
                contentPadding = innerPadding + PaddingValues(bottom = fabHeight + 16.dp),
            )
        }
    }

    val downloadStarter = rememberDownloadStarter()
    TextFieldDialog(
        visible = openLinkDialog != null,
        icon = { Icon(imageVector = Icons.Outlined.Download, contentDescription = null) },
        titleText = stringResource(Res.string.download),
        value = openLinkDialog.orEmpty(),
        onValueChange = { openLinkDialog = it },
        placeholder = stringResource(Res.string.download_screen_add_download_hint),
        onDismissRequest = { openLinkDialog = null },
        onConfirm = { text ->
            openLinkDialog = null
            scope.launch { downloadStarter.download(url = text, type = mimetype) }
        },
    )
}

@Composable
private fun DownloadList(
    downloadInfoBeanList: List<DownloadInfoBean>,
    nestedScrollConnection: NestedScrollConnection,
    contentPadding: PaddingValues,
) {
    if (downloadInfoBeanList.isNotEmpty()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxHeight()
                .nestedScroll(nestedScrollConnection),
            contentPadding = contentPadding,
        ) {
            itemsIndexed(
                items = downloadInfoBeanList,
                key = { _, item -> item.id },
            ) { index, item ->
                val downloadManager = remember { DownloadManager.instance }
                if (index > 0) HorizontalDivider()
                DownloadItem(
                    data = item,
                    onPause = { downloadManager.pause(item.id) },
                    onResume = { downloadManager.resume(item.id) },
                    onRetry = { downloadManager.retry(item.id) },
                    onDelete = { downloadManager.delete(item.id) },
                )
            }
        }
    } else {
        EmptyPlaceholder(contentPadding = contentPadding)
    }
}
