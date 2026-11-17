package com.skyd.podaura.ui.screen.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalAbsoluteTonalElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.skyd.compone.component.BackIcon
import com.skyd.compone.component.ComponeFloatingActionButton
import com.skyd.compone.component.ComponeIconButton
import com.skyd.compone.component.ComponeScaffold
import com.skyd.compone.component.SearchBarInputField
import com.skyd.compone.component.dialog.WaitingDialog
import com.skyd.compone.ext.plus
import com.skyd.compone.ext.withoutTop
import com.skyd.mvi.MviEventListener
import com.skyd.mvi.getDispatcher
import com.skyd.podaura.ext.safeItemKey
import com.skyd.podaura.model.bean.article.ArticleWithFeed
import com.skyd.podaura.model.bean.feed.FeedViewBean
import com.skyd.podaura.model.preference.appearance.search.SearchItemMinWidthPreference
import com.skyd.podaura.model.preference.appearance.search.SearchListTonalElevationPreference
import com.skyd.podaura.model.preference.appearance.search.SearchTopBarTonalElevationPreference
import com.skyd.podaura.ui.component.CircularProgressPlaceholder
import com.skyd.podaura.ui.component.ErrorPlaceholder
import com.skyd.podaura.ui.component.PagingRefreshStateIndicator
import com.skyd.podaura.ui.screen.article.Article1Item
import com.skyd.podaura.ui.screen.article.Article1ItemPlaceholder
import com.skyd.podaura.ui.screen.feed.item.Feed1Item
import com.skyd.podaura.ui.screen.feed.item.Feed1ItemPlaceholder
import com.skyd.podaura.ui.screen.feed.sheet.EditFeedSheet
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.article_delete_protected_by_download
import podaura.shared.generated.resources.article_screen_name
import podaura.shared.generated.resources.clear_input_text
import podaura.shared.generated.resources.feed_screen_name
import podaura.shared.generated.resources.search_screen_hint
import podaura.shared.generated.resources.to_top

@Serializable
sealed interface SearchRoute {
    @Serializable
    data object Feed : SearchRoute, NavKey {
        @Composable
        fun SearchFeedLauncher(
            route: Feed,
            windowInsets: WindowInsets = WindowInsets.safeDrawing
        ) {
            SearchScreen(searchRoute = route, windowInsets = windowInsets)
        }
    }

    @Serializable
    data class Article(
        val feedUrls: List<String>,
        val groupIds: List<String>,
        val articleIds: List<String>,
    ) : SearchRoute, NavKey {
        companion object {
            @Composable
            fun SearchArticleLauncher(
                route: Article,
                windowInsets: WindowInsets = WindowInsets.safeDrawing
            ) {
                SearchScreen(searchRoute = route, windowInsets = windowInsets)
            }
        }
    }
}

@Composable
fun SearchScreen(
    searchRoute: SearchRoute,
    viewModel: SearchViewModel = koinViewModel(),
    windowInsets: WindowInsets = WindowInsets.safeDrawing
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val uiState by viewModel.viewState.collectAsStateWithLifecycle()
    val keyboardController = LocalSoftwareKeyboardController.current

    val pagerState = rememberPagerState(
        initialPage = if (searchRoute is SearchRoute.Article) 1 else 0,
        pageCount = { 2 },
    )
    val feedListState = rememberLazyGridState()
    val articleListState = rememberLazyGridState()
    val searchResultListState = if (pagerState.currentPage == 0) feedListState else articleListState
    var fabHeight by remember { mutableStateOf(0.dp) }

    var searchFieldValueState by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(text = "", selection = TextRange(0)))
    }

    val dispatch = viewModel.getDispatcher(startWith = null)
    LaunchedEffect(viewModel, searchRoute) {
        dispatch(SearchIntent.UpdateQuery(query = searchFieldValueState.text))
        dispatch(SearchIntent.ListenSearchFeed)
        val articleRoute = searchRoute as? SearchRoute.Article
        dispatch(
            SearchIntent.ListenSearchArticle(
                feedUrls = articleRoute?.feedUrls.orEmpty(),
                groupIds = articleRoute?.groupIds.orEmpty(),
                articleIds = articleRoute?.articleIds.orEmpty(),
            )
        )
    }

    ComponeScaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            AnimatedVisibility(
                visible = remember(searchResultListState) {
                    derivedStateOf { searchResultListState.firstVisibleItemIndex > 2 }
                }.value,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                ComponeFloatingActionButton(
                    onClick = { scope.launch { searchResultListState.animateScrollToItem(0) } },
                    onSizeWithSinglePaddingChanged = { _, height ->
                        fabHeight = height
                    },
                    contentDescription = stringResource(Res.string.to_top),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ArrowUpward,
                        contentDescription = stringResource(Res.string.to_top),
                    )
                }
            }
        },
        topBar = {
            Column(
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.surfaceColorAtElevation(
                            SearchTopBarTonalElevationPreference.current.dp
                        )
                    )
                    .windowInsetsPadding(
                        windowInsets.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
                    )
            ) {
                SearchBarInputField(
                    onQueryChange = {
                        searchFieldValueState = it
                        dispatch(SearchIntent.UpdateQuery(it.text))
                    },
                    query = searchFieldValueState,
                    onSearch = { keyboardController?.hide() },
                    placeholder = { Text(text = stringResource(Res.string.search_screen_hint)) },
                    leadingIcon = { BackIcon() },
                    trailingIcon = {
                        TrailingIcon(showClearButton = searchFieldValueState.text.isNotEmpty()) {
                            searchFieldValueState = TextFieldValue(
                                text = "", selection = TextRange(0)
                            )
                            dispatch(SearchIntent.UpdateQuery(searchFieldValueState.text))
                        }
                    }
                )
                SearchTabs(pagerState = pagerState)
            }
        },
        contentWindowInsets = windowInsets,
        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(
            LocalAbsoluteTonalElevation.current +
                    SearchListTonalElevationPreference.current.dp
        ),
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) { innerPaddings ->
        HorizontalPager(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPaddings.calculateTopPadding()),
            state = pagerState,
            verticalAlignment = Alignment.Top,
        ) { page ->
            if (page == 0) {
                SearchResultContent(
                    searchResultState = uiState.feedSearchResultState,
                    listState = feedListState,
                    contentPadding = innerPaddings.withoutTop(),
                    fabHeight = fabHeight,
                ) { result ->
                    feedItems(
                        result = result,
                        onEdit = { dispatch(SearchIntent.OnEditFeedDialog(it.feed.url)) },
                    )
                }
            } else {
                SearchResultContent(
                    searchResultState = uiState.articleSearchResultState,
                    listState = articleListState,
                    contentPadding = innerPaddings.withoutTop(),
                    fabHeight = fabHeight,
                ) { result ->
                    articleItems(
                        result = result,
                        onFavorite = { articleWithFeed, favorite ->
                            dispatch(
                                SearchIntent.Favorite(
                                    articleId = articleWithFeed.articleWithEnclosure.article.articleId,
                                    favorite = favorite,
                                )
                            )
                        },
                        onRead = { articleWithFeed, read ->
                            dispatch(
                                SearchIntent.Read(
                                    articleId = articleWithFeed.articleWithEnclosure.article.articleId,
                                    read = read,
                                )
                            )
                        },
                        onDelete = { articleWithFeed ->
                            dispatch(
                                SearchIntent.Delete(articleWithFeed.articleWithEnclosure.article.articleId)
                            )
                        },
                        onMessage = { scope.launch { snackbarHostState.showSnackbar(it) } },
                        onEditFeedSheet = { dispatch(SearchIntent.OnEditFeedDialog(it)) },
                    )
                }
            }
        }

        uiState.editFeedUrl?.let { editFeedUrl ->
            EditFeedSheet(
                feedUrl = editFeedUrl,
                onDismissRequest = { dispatch(SearchIntent.OnEditFeedDialog(null)) },
                snackbarHostState = snackbarHostState,
            )
        }

        WaitingDialog(visible = uiState.loadingDialog)

        MviEventListener(viewModel.singleEvent) { event ->
            when (event) {
                is SearchEvent.FavoriteArticleResultEvent.Failed ->
                    snackbarHostState.showSnackbar(event.msg)

                is SearchEvent.ReadArticleResultEvent.Failed ->
                    snackbarHostState.showSnackbar(event.msg)

                is SearchEvent.DeleteArticleResultEvent.Failed ->
                    snackbarHostState.showSnackbar(event.msg)

                is SearchEvent.DeleteArticleResultEvent.ProtectedByDownload ->
                    snackbarHostState.showSnackbar(
                        getString(Res.string.article_delete_protected_by_download)
                    )
            }
        }
    }
}

@Composable
internal fun SearchTabs(
    pagerState: PagerState,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    PrimaryTabRow(
        // Article cards keep their swipe actions; the tab row always offers page swiping.
        modifier = modifier.scrollable(
            state = pagerState,
            orientation = Orientation.Horizontal,
            reverseDirection = LocalLayoutDirection.current == LayoutDirection.Ltr,
            flingBehavior = PagerDefaults.flingBehavior(state = pagerState),
        ),
        selectedTabIndex = pagerState.currentPage,
        containerColor = Color.Transparent,
    ) {
        listOf(
            stringResource(Res.string.feed_screen_name),
            stringResource(Res.string.article_screen_name),
        ).forEachIndexed { index, title ->
            Tab(
                selected = pagerState.currentPage == index,
                onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                text = {
                    Text(
                        text = title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
            )
        }
    }
}

@Composable
private fun <T : Any> SearchResultContent(
    searchResultState: SearchResultState<T>,
    listState: LazyGridState,
    contentPadding: PaddingValues,
    fabHeight: Dp,
    items: LazyGridScope.(LazyPagingItems<T>) -> Unit,
) {
    when (searchResultState) {
        is SearchResultState.Failed -> ErrorPlaceholder(
            modifier = Modifier.sizeIn(maxHeight = 200.dp),
            text = searchResultState.msg,
            contentPadding = contentPadding,
        )

        SearchResultState.Init,
        SearchResultState.Loading -> CircularProgressPlaceholder(contentPadding = contentPadding)

        is SearchResultState.Success -> {
            val result = searchResultState.result.collectAsLazyPagingItems()
            PagingRefreshStateIndicator(
                lazyPagingItems = result,
                placeholderPadding = contentPadding,
            ) {
                SearchResultList(
                    listState = listState,
                    contentPadding = contentPadding + PaddingValues(
                        top = 4.dp,
                        bottom = 4.dp + fabHeight,
                    ),
                ) { items(result) }
            }
        }
    }
}

@Composable
private fun SearchResultList(
    modifier: Modifier = Modifier,
    listState: LazyGridState,
    contentPadding: PaddingValues,
    items: LazyGridScope.() -> Unit,
) {
    LazyVerticalGrid(
        modifier = modifier,
        columns = GridCells.Adaptive(SearchItemMinWidthPreference.current.dp),
        state = listState,
        contentPadding = contentPadding + PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) { items() }
}

private fun LazyGridScope.feedItems(
    result: LazyPagingItems<FeedViewBean>,
    onEdit: (FeedViewBean) -> Unit,
) {
    items(
        count = result.itemCount,
        key = result.safeItemKey { it.feed.url },
    ) { index ->
        when (val item = result[index]) {
            is FeedViewBean -> Feed1Item(item, onEdit = onEdit)
            null -> Feed1ItemPlaceholder()
        }
    }
}

private fun LazyGridScope.articleItems(
    result: LazyPagingItems<ArticleWithFeed>,
    onFavorite: (ArticleWithFeed, Boolean) -> Unit,
    onRead: (ArticleWithFeed, Boolean) -> Unit,
    onDelete: (ArticleWithFeed) -> Unit,
    onMessage: (String) -> Unit,
    onEditFeedSheet: ((String) -> Unit)? = null,
) {
    items(
        count = result.itemCount,
        key = result.safeItemKey { it.articleWithEnclosure.article.articleId },
    ) { index ->
        when (val item = result[index]) {
            is ArticleWithFeed -> Article1Item(
                item,
                onFavorite = onFavorite,
                onRead = onRead,
                onDelete = onDelete,
                onMessage = onMessage,
                onEditFeedSheet = onEditFeedSheet,
            )

            null -> Article1ItemPlaceholder()
        }
    }
}

@Composable
fun TrailingIcon(
    showClearButton: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    if (showClearButton) {
        ComponeIconButton(
            imageVector = Icons.Outlined.Clear,
            contentDescription = stringResource(Res.string.clear_input_text),
            onClick = { onClick?.invoke() }
        )
    }
}
