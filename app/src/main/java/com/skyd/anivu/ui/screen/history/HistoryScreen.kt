package com.skyd.anivu.ui.screen.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerScope
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.skyd.anivu.R
import com.skyd.anivu.base.mvi.getDispatcher
import com.skyd.anivu.ext.plus
import com.skyd.anivu.model.bean.history.MediaPlayHistoryWithArticle
import com.skyd.anivu.model.bean.history.ReadHistoryWithArticle
import com.skyd.anivu.ui.component.CircularProgressPlaceholder
import com.skyd.anivu.ui.component.ErrorPlaceholder
import com.skyd.anivu.ui.component.PagingRefreshStateIndicator
import com.skyd.anivu.ui.component.PodAuraTopBar
import com.skyd.anivu.ui.component.PodAuraTopBarStyle
import com.skyd.anivu.ui.screen.history.item.MediaPlayHistoryItem
import com.skyd.anivu.ui.screen.history.item.MediaPlayItemPlaceholder
import com.skyd.anivu.ui.screen.history.item.ReadHistoryItem
import com.skyd.anivu.ui.screen.history.item.ReadHistoryItemPlaceholder
import kotlinx.coroutines.launch


const val HISTORY_SCREEN_ROUTE = "historyScreen"

@Composable
fun HistoryScreen(viewModel: HistoryViewModel = hiltViewModel()) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val uiState by viewModel.viewState.collectAsStateWithLifecycle()
    val dispatch = viewModel.getDispatcher(startWith = HistoryIntent.Init)

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            PodAuraTopBar(
                style = PodAuraTopBarStyle.CenterAligned,
                scrollBehavior = scrollBehavior,
                title = { Text(text = stringResource(R.string.history_screen_name)) },
            )
        },
    ) { paddingValues ->
        when (val historyListState = uiState.historyListState) {
            is HistoryListState.Failed -> ErrorPlaceholder(text = historyListState.msg)
            HistoryListState.Init,
            HistoryListState.Loading -> CircularProgressPlaceholder(contentPadding = paddingValues)

            is HistoryListState.Success -> {
                val listContentPadding = PaddingValues(
                    start = paddingValues.calculateStartPadding(LocalLayoutDirection.current),
                    end = paddingValues.calculateEndPadding(LocalLayoutDirection.current),
                ) + PaddingValues(horizontal = 12.dp, vertical = 12.dp)
                val nestedScrollConnection = scrollBehavior.nestedScrollConnection
                val pagerState = rememberPagerState(pageCount = { 2 })
                val tabs = listOf<Pair<String, @Composable PagerScope.() -> Unit>>(
                    stringResource(R.string.history_screen_read) to {
                        ReadHistoryList(
                            historyList = historyListState.readHistoryList.collectAsLazyPagingItems(),
                            nestedScrollConnection = nestedScrollConnection,
                            contentPadding = listContentPadding,
                            onDelete = { dispatch(HistoryIntent.DeleteReadHistory(it.readHistoryBean.articleId)) }
                        )
                    },
                    stringResource(R.string.history_screen_media_play) to {
                        MediaPlayHistoryList(
                            historyList = historyListState.mediaPlayHistoryList.collectAsLazyPagingItems(),
                            nestedScrollConnection = nestedScrollConnection,
                            contentPadding = listContentPadding,
                            onDelete = { dispatch(HistoryIntent.DeleteMediaPlayHistory(it.mediaPlayHistoryBean.path)) }
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
}

@Composable
private fun ReadHistoryList(
    historyList: LazyPagingItems<ReadHistoryWithArticle>,
    nestedScrollConnection: NestedScrollConnection,
    contentPadding: PaddingValues,
    onDelete: (ReadHistoryWithArticle) -> Unit,
) {
    PagingRefreshStateIndicator(
        lazyPagingItems = historyList,
        abnormalContent = { Box(modifier = Modifier.padding(contentPadding)) { it() } },
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxHeight()
                .nestedScroll(nestedScrollConnection),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(
                count = historyList.itemCount,
                key = historyList.itemKey { it.readHistoryBean.articleId },
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

// todo: download file with article info
@Composable
private fun MediaPlayHistoryList(
    historyList: LazyPagingItems<MediaPlayHistoryWithArticle>,
    nestedScrollConnection: NestedScrollConnection,
    contentPadding: PaddingValues,
    onDelete: (MediaPlayHistoryWithArticle) -> Unit,
) {
    PagingRefreshStateIndicator(
        lazyPagingItems = historyList,
        abnormalContent = { Box(modifier = Modifier.padding(contentPadding)) { it() } },
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxHeight()
                .nestedScroll(nestedScrollConnection),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(
                count = historyList.itemCount,
                key = historyList.itemKey { it.mediaPlayHistoryBean.path },
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