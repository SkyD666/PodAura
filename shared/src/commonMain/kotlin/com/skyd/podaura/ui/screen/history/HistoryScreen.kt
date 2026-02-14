package com.skyd.podaura.ui.screen.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerScope
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.skyd.compone.component.ComponeIconButton
import com.skyd.compone.component.ComponeTopBar
import com.skyd.compone.component.ComponeTopBarStyle
import com.skyd.compone.ext.onlyHorizontal
import com.skyd.compone.ext.plus
import com.skyd.compone.ext.thenIfNotNull
import com.skyd.compone.ext.withoutTop
import com.skyd.compone.local.LocalNavController
import com.skyd.mvi.getDispatcher
import com.skyd.podaura.ext.safeItemKey
import com.skyd.podaura.model.bean.history.MediaPlayHistoryWithArticle
import com.skyd.podaura.model.bean.history.ReadHistoryWithArticle
import com.skyd.podaura.ui.component.CircularProgressPlaceholder
import com.skyd.podaura.ui.component.ErrorPlaceholder
import com.skyd.podaura.ui.component.PagingRefreshStateIndicator
import com.skyd.podaura.ui.screen.history.item.MediaPlayHistoryItem
import com.skyd.podaura.ui.screen.history.item.MediaPlayItemPlaceholder
import com.skyd.podaura.ui.screen.history.item.ReadHistoryItem
import com.skyd.podaura.ui.screen.history.item.ReadHistoryItemPlaceholder
import com.skyd.podaura.ui.screen.history.search.HistorySearchRoute
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.history_screen_media_play
import podaura.shared.generated.resources.history_screen_name
import podaura.shared.generated.resources.history_screen_read
import podaura.shared.generated.resources.history_search_screen_hint


@Serializable
data object HistoryRoute

@Composable
fun HistoryScreen(viewModel: HistoryViewModel = koinViewModel()) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val navController = LocalNavController.current

    val uiState by viewModel.viewState.collectAsStateWithLifecycle()
    val dispatch = viewModel.getDispatcher(startWith = HistoryIntent.Init)

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            ComponeTopBar(
                style = ComponeTopBarStyle.CenterAligned,
                scrollBehavior = scrollBehavior,
                title = { Text(text = stringResource(Res.string.history_screen_name)) },
                actions = {
                    ComponeIconButton(
                        onClick = { navController.navigate(HistorySearchRoute) },
                        imageVector = Icons.Outlined.Search,
                        contentDescription = stringResource(Res.string.history_search_screen_hint),
                    )
                }
            )
        },
    ) { innerPadding ->
        when (val historyListState = uiState.historyListState) {
            is HistoryListState.Failed -> ErrorPlaceholder(text = historyListState.msg)
            HistoryListState.Init,
            HistoryListState.Loading -> CircularProgressPlaceholder(contentPadding = innerPadding)

            is HistoryListState.Success -> {
                val listContentPadding = innerPadding.withoutTop() +
                        PaddingValues(horizontal = 12.dp, vertical = 12.dp)
                val nestedScrollConnection = scrollBehavior.nestedScrollConnection
                val pagerState = rememberPagerState(pageCount = { 2 })
                val tabs = listOf<Pair<String, @Composable PagerScope.() -> Unit>>(
                    stringResource(Res.string.history_screen_read) to {
                        ReadHistoryList(
                            historyList = historyListState.readHistoryList.collectAsLazyPagingItems(),
                            nestedScrollConnection = nestedScrollConnection,
                            contentPadding = listContentPadding,
                            onDelete = { dispatch(HistoryIntent.DeleteReadHistory(it.readHistoryBean.articleId)) }
                        )
                    },
                    stringResource(Res.string.history_screen_media_play) to {
                        MediaPlayHistoryList(
                            historyList = historyListState.mediaPlayHistoryList.collectAsLazyPagingItems(),
                            nestedScrollConnection = nestedScrollConnection,
                            contentPadding = listContentPadding,
                            onDelete = { dispatch(HistoryIntent.DeleteMediaPlayHistory(it.mediaPlayHistoryBean.path)) }
                        )
                    }
                )
                Column(modifier = Modifier.padding(top = innerPadding.calculateTopPadding())) {
                    PrimaryTabRow(
                        modifier = Modifier.padding(innerPadding.onlyHorizontal()),
                        selectedTabIndex = pagerState.currentPage
                    ) {
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
}

@Composable
internal fun ReadHistoryList(
    historyList: LazyPagingItems<ReadHistoryWithArticle>,
    nestedScrollConnection: NestedScrollConnection?,
    contentPadding: PaddingValues,
    onDelete: (ReadHistoryWithArticle) -> Unit,
) {
    PagingRefreshStateIndicator(
        lazyPagingItems = historyList,
        placeholderPadding = contentPadding,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxHeight()
                .thenIfNotNull(nestedScrollConnection) { nestedScroll(it) },
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(
                count = historyList.itemCount,
                key = historyList.safeItemKey { it.readHistoryBean.articleId },
            ) { index ->
                val data = historyList[index]
                if (data == null) {
                    ReadHistoryItemPlaceholder()
                } else {
                    ReadHistoryItem(
                        data = data,
                        onDelete = onDelete,
                    )
                }
            }
        }
    }
}

@Composable
internal fun MediaPlayHistoryList(
    historyList: LazyPagingItems<MediaPlayHistoryWithArticle>,
    nestedScrollConnection: NestedScrollConnection?,
    contentPadding: PaddingValues,
    onDelete: (MediaPlayHistoryWithArticle) -> Unit,
) {
    PagingRefreshStateIndicator(
        lazyPagingItems = historyList,
        placeholderPadding = contentPadding,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxHeight()
                .thenIfNotNull(nestedScrollConnection) { nestedScroll(it) },
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(
                count = historyList.itemCount,
                key = historyList.safeItemKey { it.mediaPlayHistoryBean.path },
            ) { index ->
                val data = historyList[index]
                if (data == null) {
                    MediaPlayItemPlaceholder()
                } else {
                    MediaPlayHistoryItem(
                        data = data,
                        onDelete = onDelete,
                    )
                }
            }
        }
    }
}