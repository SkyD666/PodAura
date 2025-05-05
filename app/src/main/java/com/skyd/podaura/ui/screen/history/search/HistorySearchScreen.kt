package com.skyd.podaura.ui.screen.history.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerScope
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import com.skyd.podaura.ext.plus
import com.skyd.podaura.ext.withoutTop
import com.skyd.podaura.model.bean.history.MediaPlayHistoryWithArticle
import com.skyd.podaura.model.bean.history.ReadHistoryWithArticle
import com.skyd.podaura.ui.component.BackIcon
import com.skyd.podaura.ui.component.CircularProgressPlaceholder
import com.skyd.podaura.ui.component.ErrorPlaceholder
import com.skyd.podaura.ui.component.PodAuraFloatingActionButton
import com.skyd.podaura.ui.component.SearchBarInputField
import com.skyd.podaura.ui.component.dialog.WaitingDialog
import com.skyd.podaura.ui.mvi.MviEventListener
import com.skyd.podaura.ui.mvi.getDispatcher
import com.skyd.podaura.ui.screen.history.MediaPlayHistoryList
import com.skyd.podaura.ui.screen.history.ReadHistoryList
import com.skyd.podaura.ui.screen.search.TrailingIcon
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.history_screen_media_play
import podaura.shared.generated.resources.history_screen_read
import podaura.shared.generated.resources.history_search_screen_hint
import podaura.shared.generated.resources.to_top


@Serializable
data object HistorySearchRoute

@Composable
fun HistorySearchScreen(viewModel: HistorySearchViewModel = koinViewModel()) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val uiState by viewModel.viewState.collectAsStateWithLifecycle()
    val keyboardController = LocalSoftwareKeyboardController.current

    val searchResultListState = rememberLazyGridState()
    var fabHeight by remember { mutableStateOf(0.dp) }
    var fabWidth by remember { mutableStateOf(0.dp) }

    var searchFieldValueState by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(text = "", selection = TextRange(0)))
    }

    val dispatch = viewModel.getDispatcher(startWith = HistorySearchIntent.Query(""))

    Scaffold(
        modifier = Modifier.imePadding(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            AnimatedVisibility(
                visible = remember {
                    derivedStateOf { searchResultListState.firstVisibleItemIndex > 2 }
                }.value,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                PodAuraFloatingActionButton(
                    onClick = { scope.launch { searchResultListState.animateScrollToItem(0) } },
                    onSizeWithSinglePaddingChanged = { width, height ->
                        fabWidth = width
                        fabHeight = height
                    },
                    contentDescription = stringResource(Res.string.to_top),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ArrowUpward,
                        contentDescription = null,
                    )
                }
            }
        },
        topBar = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .windowInsetsPadding(
                        WindowInsets.systemBars
                            .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
                    )
            ) {
                SearchBarInputField(
                    onQueryChange = {
                        searchFieldValueState = it
                        dispatch(HistorySearchIntent.Query(it.text))
                    },
                    query = searchFieldValueState,
                    onSearch = { keyboardController?.hide() },
                    placeholder = { Text(text = stringResource(Res.string.history_search_screen_hint)) },
                    leadingIcon = { BackIcon() },
                    trailingIcon = {
                        TrailingIcon(showClearButton = searchFieldValueState.text.isNotEmpty()) {
                            searchFieldValueState = TextFieldValue(
                                text = "", selection = TextRange(0)
                            )
                            dispatch(HistorySearchIntent.Query(searchFieldValueState.text))
                        }
                    }
                )
                HorizontalDivider()
            }
        },
    ) { paddingValues ->
        val listContentPadding = paddingValues.withoutTop() +
                PaddingValues(horizontal = 12.dp, vertical = 12.dp)
        val pagerState = rememberPagerState(pageCount = { 2 })
        val tabs = listOf<Pair<String, @Composable PagerScope.() -> Unit>>(
            stringResource(Res.string.history_screen_read) to {
                ReadHistoryContent(
                    state = uiState.readHistorySearchResultState,
                    contentPadding = listContentPadding,
                    onDelete = { dispatch(HistorySearchIntent.DeleteReadHistory(it.readHistoryBean.articleId)) }
                )
            },
            stringResource(Res.string.history_screen_media_play) to {
                MediaPlayHistoryContent(
                    state = uiState.mediaPlayHistorySearchResultState,
                    contentPadding = listContentPadding,
                    onDelete = { dispatch(HistorySearchIntent.DeleteMediaPlayHistory(it.mediaPlayHistoryBean.path)) }
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



        WaitingDialog(visible = uiState.loadingDialog)

        MviEventListener(viewModel.singleEvent) { event ->
            when (event) {
                is HistorySearchEvent.DeleteReadHistoryResultEvent.Failed ->
                    snackbarHostState.showSnackbar(event.msg)

                is HistorySearchEvent.DeleteMediaPlayHistoryResultEvent.Failed ->
                    snackbarHostState.showSnackbar(event.msg)
            }
        }
    }
}

@Composable
private fun ReadHistoryContent(
    state: ReadHistorySearchResultState,
    contentPadding: PaddingValues,
    onDelete: (ReadHistoryWithArticle) -> Unit,
) {
    when (state) {
        is ReadHistorySearchResultState.Failed -> ErrorPlaceholder(
            modifier = Modifier.sizeIn(maxHeight = 200.dp),
            text = state.msg,
            contentPadding = contentPadding,
        )

        ReadHistorySearchResultState.Init,
        ReadHistorySearchResultState.Loading -> CircularProgressPlaceholder(contentPadding = contentPadding)

        is ReadHistorySearchResultState.Success -> ReadHistoryList(
            historyList = state.result.collectAsLazyPagingItems(),
            nestedScrollConnection = null,
            contentPadding = contentPadding,
            onDelete = onDelete,
        )
    }
}

@Composable
private fun MediaPlayHistoryContent(
    state: MediaPlayHistorySearchResultState,
    contentPadding: PaddingValues,
    onDelete: (MediaPlayHistoryWithArticle) -> Unit,
) {
    when (state) {
        is MediaPlayHistorySearchResultState.Failed -> ErrorPlaceholder(
            modifier = Modifier.sizeIn(maxHeight = 200.dp),
            text = state.msg,
            contentPadding = contentPadding,
        )

        MediaPlayHistorySearchResultState.Init,
        MediaPlayHistorySearchResultState.Loading -> CircularProgressPlaceholder(contentPadding = contentPadding)

        is MediaPlayHistorySearchResultState.Success -> MediaPlayHistoryList(
            historyList = state.result.collectAsLazyPagingItems(),
            nestedScrollConnection = null,
            contentPadding = contentPadding,
            onDelete = onDelete,
        )
    }
}