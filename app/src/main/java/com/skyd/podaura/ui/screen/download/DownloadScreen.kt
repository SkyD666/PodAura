package com.skyd.podaura.ui.screen.download

import android.content.Intent
import android.os.Build
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import com.skyd.compone.component.ComponeFloatingActionButton
import com.skyd.compone.component.ComponeTopBar
import com.skyd.compone.component.ComponeTopBarStyle
import com.skyd.compone.ext.plus
import com.skyd.mvi.getDispatcher
import com.skyd.podaura.ext.type
import com.skyd.podaura.model.bean.download.DownloadInfoBean
import com.skyd.podaura.model.repository.download.DownloadManager
import com.skyd.podaura.model.repository.download.DownloadStarter
import com.skyd.podaura.ui.component.CircularProgressPlaceholder
import com.skyd.podaura.ui.component.EmptyPlaceholder
import com.skyd.podaura.ui.component.dialog.TextFieldDialog
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
) {
    companion object {
        const val BASE_PATH = "podaura://download.screen"

        val deepLinks = listOf(navDeepLink<DownloadRoute>(basePath = BASE_PATH))

        @Composable
        fun DownloadLauncher(entry: NavBackStackEntry) {
            val route = entry.toRoute<DownloadRoute>()
            DownloadScreen(downloadLink = route.downloadLink, mimetype = route.mimetype)
        }
    }
}

@Serializable
data object DownloadDeepLinkRoute {
    val deepLinks = listOf("magnet:.*", "http://.*", "https://.*", "file://.*").map {
        navDeepLink {
            action = Intent.ACTION_VIEW
            uriPattern = it
        }
    }

    @Composable
    fun DownloadDeepLinkLauncher(entry: NavBackStackEntry) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            entry.arguments?.getParcelable(NavController.KEY_DEEP_LINK_INTENT, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            entry.arguments?.getParcelable(NavController.KEY_DEEP_LINK_INTENT)
        }
        DownloadScreen(downloadLink = intent?.data?.toString(), mimetype = intent?.data?.type)
    }
}

@Composable
fun DownloadScreen(
    downloadLink: String? = null,
    mimetype: String? = null,
    viewModel: DownloadViewModel = koinViewModel()
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var openLinkDialog by rememberSaveable { mutableStateOf(downloadLink) }

    LaunchedEffect(downloadLink) {
        openLinkDialog = downloadLink
    }

    var fabHeight by remember { mutableStateOf(0.dp) }

    val uiState by viewModel.viewState.collectAsStateWithLifecycle()
    viewModel.getDispatcher(startWith = DownloadIntent.Init)

    Scaffold(
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
        }
    ) { paddingValues ->
        when (val downloadListState = uiState.downloadListState) {
            is DownloadListState.Failed -> Unit
            DownloadListState.Init,
            DownloadListState.Loading -> CircularProgressPlaceholder(contentPadding = paddingValues)

            is DownloadListState.Success -> DownloadList(
                downloadInfoBeanList = downloadListState.downloadInfoBeanList,
                nestedScrollConnection = scrollBehavior.nestedScrollConnection,
                contentPadding = paddingValues + PaddingValues(bottom = fabHeight + 16.dp),
            )
        }
    }

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
            DownloadStarter.download(context = context, url = text, type = mimetype)
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
        val context = LocalContext.current
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
                val downloadManager = remember { DownloadManager.getInstance(context) }
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