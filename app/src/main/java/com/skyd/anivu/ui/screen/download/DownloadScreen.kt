package com.skyd.anivu.ui.screen.download

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerScope
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skyd.anivu.R
import com.skyd.anivu.base.mvi.getDispatcher
import com.skyd.anivu.ext.onlyHorizontal
import com.skyd.anivu.ext.plus
import com.skyd.anivu.model.bean.download.DownloadInfoBean
import com.skyd.anivu.model.bean.download.bt.BtDownloadInfoBean
import com.skyd.anivu.model.repository.download.DownloadManager
import com.skyd.anivu.model.repository.download.DownloadStarter
import com.skyd.anivu.model.repository.download.bt.BtDownloadManager
import com.skyd.anivu.model.repository.download.bt.BtDownloadManager.rememberBtDownloadWorkStarter
import com.skyd.anivu.ui.component.CircularProgressPlaceholder
import com.skyd.anivu.ui.component.EmptyPlaceholder
import com.skyd.anivu.ui.component.PodAuraFloatingActionButton
import com.skyd.anivu.ui.component.PodAuraTopBar
import com.skyd.anivu.ui.component.PodAuraTopBarStyle
import com.skyd.anivu.ui.component.dialog.TextFieldDialog
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable


@Serializable
data class DownloadRoute(
    val downloadLink: String? = null,
    val mimetype: String? = null,
) {
    companion object {
        const val BASE_PATH = "podaura://download.screen"
    }
}

@Serializable
data object DownloadDeepLinkRoute

@Composable
fun DownloadScreen(
    downloadLink: String? = null,
    mimetype: String? = null,
    viewModel: DownloadViewModel = hiltViewModel()
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
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
            PodAuraTopBar(
                style = PodAuraTopBarStyle.CenterAligned,
                scrollBehavior = scrollBehavior,
                title = { Text(text = stringResource(R.string.download_screen_name)) },
            )
        },
        floatingActionButton = {
            PodAuraFloatingActionButton(
                onClick = { openLinkDialog = "" },
                contentDescription = stringResource(id = R.string.download_screen_add_download),
                onSizeWithSinglePaddingChanged = { _, height -> fabHeight = height },
            ) {
                Icon(imageVector = Icons.Outlined.Add, contentDescription = null)
            }
        }
    ) { paddingValues ->
        when (val downloadListState = uiState.downloadListState) {
            is DownloadListState.Failed -> Unit
            DownloadListState.Init,
            DownloadListState.Loading -> CircularProgressPlaceholder(contentPadding = paddingValues)

            is DownloadListState.Success -> {
                val listContentPadding = paddingValues.onlyHorizontal() +
                        PaddingValues(bottom = fabHeight + 16.dp)
                val nestedScrollConnection = scrollBehavior.nestedScrollConnection
                val pagerState = rememberPagerState(pageCount = { 2 })
                val tabs = listOf<Pair<String, @Composable PagerScope.() -> Unit>>(
                    stringResource(R.string.download_screen_download_tasks) to {
                        DownloadList(
                            downloadInfoBeanList = downloadListState.downloadInfoBeanList,
                            nestedScrollConnection = nestedScrollConnection,
                            contentPadding = listContentPadding,
                        )
                    },
                    stringResource(R.string.download_screen_bt_tasks) to {
                        BtDownloadList(
                            btDownloadInfoBeanList = downloadListState.btDownloadInfoBeanList,
                            nestedScrollConnection = nestedScrollConnection,
                            contentPadding = listContentPadding,
                        )
                    }
                )
                Column(modifier = Modifier.padding(top = paddingValues.calculateTopPadding())) {
                    PrimaryTabRow(selectedTabIndex = pagerState.currentPage) {
                        tabs.forEachIndexed { index, (title, _) ->
                            Tab(
                                selected = pagerState.currentPage == index,
                                onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                                text = {
                                    Text(
                                        text = title,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            )
                        }
                    }
                    HorizontalPager(state = pagerState) { index ->
                        tabs[index].second.invoke(this)
                    }
                }
            }
        }
    }

    TextFieldDialog(
        visible = openLinkDialog != null,
        icon = { Icon(imageVector = Icons.Outlined.Download, contentDescription = null) },
        titleText = stringResource(id = R.string.download),
        value = openLinkDialog.orEmpty(),
        onValueChange = { openLinkDialog = it },
        placeholder = stringResource(id = R.string.download_screen_add_download_hint),
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

@Composable
private fun BtDownloadList(
    btDownloadInfoBeanList: List<BtDownloadInfoBean>,
    nestedScrollConnection: NestedScrollConnection,
    contentPadding: PaddingValues,
) {
    if (btDownloadInfoBeanList.isNotEmpty()) {
        val context = LocalContext.current
        val btDownloadWorkStarter = rememberBtDownloadWorkStarter()
        LazyColumn(
            modifier = Modifier
                .fillMaxHeight()
                .nestedScroll(nestedScrollConnection),
            contentPadding = contentPadding,
        ) {
            itemsIndexed(
                items = btDownloadInfoBeanList,
                key = { _, item -> item.link },
            ) { index, item ->
                if (index > 0) HorizontalDivider()
                BtDownloadItem(
                    data = item,
                    onPause = {
                        BtDownloadManager.pause(
                            context = context,
                            requestId = it.downloadRequestId,
                            link = it.link,
                        )
                    },
                    onResume = { video ->
                        btDownloadWorkStarter.start(
                            torrentLink = video.link,
                            saveDir = video.path,
                            requestId = video.downloadRequestId,
                        )
                    },
                    onCancel = { video ->
                        BtDownloadManager.delete(
                            context = context,
                            requestId = video.downloadRequestId,
                            link = video.link,
                        )
                    },
                )
            }
        }
    } else {
        EmptyPlaceholder(contentPadding = contentPadding)
    }
}