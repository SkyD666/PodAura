package com.skyd.podaura.ui.screen.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalAbsoluteTonalElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
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
import androidx.compose.ui.unit.Dp
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
import com.skyd.podaura.ui.component.serializableType
import com.skyd.podaura.ui.screen.article.Article1Item
import com.skyd.podaura.ui.screen.article.Article1ItemPlaceholder
import com.skyd.podaura.ui.screen.feed.item.Feed1Item
import com.skyd.podaura.ui.screen.feed.item.Feed1ItemPlaceholder
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.clear_input_text
import podaura.shared.generated.resources.search_screen_hint
import podaura.shared.generated.resources.to_top
import kotlin.reflect.typeOf

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
            val typeMap = mapOf(typeOf<Article>() to serializableType<Article>())

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

    val searchResultListState = rememberLazyGridState()
    var fabHeight by remember { mutableStateOf(0.dp) }
    var fabWidth by remember { mutableStateOf(0.dp) }

    var searchFieldValueState by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(text = "", selection = TextRange(0)))
    }

    val dispatch = viewModel.getDispatcher(
        startWith = when (searchRoute) {
            SearchRoute.Feed -> SearchIntent.ListenSearchFeed
            is SearchRoute.Article -> SearchIntent.ListenSearchArticle(
                feedUrls = searchRoute.feedUrls,
                groupIds = searchRoute.groupIds,
                articleIds = searchRoute.articleIds,
            )
        }
    )

    ComponeScaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            AnimatedVisibility(
                visible = remember {
                    derivedStateOf { searchResultListState.firstVisibleItemIndex > 2 }
                }.value,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                ComponeFloatingActionButton(
                    onClick = { scope.launch { searchResultListState.animateScrollToItem(0) } },
                    onSizeWithSinglePaddingChanged = { width, height ->
                        fabWidth = width
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
                HorizontalDivider()
            }
        },
        contentWindowInsets = windowInsets,
        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(
            LocalAbsoluteTonalElevation.current +
                    SearchListTonalElevationPreference.current.dp
        ),
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) { innerPaddings ->
        when (val searchResultState = uiState.searchResultState) {
            is SearchResultState.Failed -> ErrorPlaceholder(
                modifier = Modifier.sizeIn(maxHeight = 200.dp),
                text = searchResultState.msg,
                contentPadding = innerPaddings
            )

            SearchResultState.Init,
            SearchResultState.Loading -> CircularProgressPlaceholder(contentPadding = innerPaddings)

            is SearchResultState.Success -> SuccessContent(
                searchResultState = searchResultState,
                searchResultListState = searchResultListState,
                searchRoute = searchRoute,
                dispatch = dispatch,
                innerPaddings = innerPaddings,
                fabHeight = fabHeight,
                onMessage = { scope.launch { snackbarHostState.showSnackbar(it) } },
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
            }
        }
    }
}

@Composable
private fun SuccessContent(
    searchResultState: SearchResultState.Success,
    searchResultListState: LazyGridState,
    searchRoute: SearchRoute,
    dispatch: (SearchIntent) -> Unit,
    innerPaddings: PaddingValues,
    fabHeight: Dp,
    onMessage: (String) -> Unit,
) {
    val result = searchResultState.result.collectAsLazyPagingItems()
    PagingRefreshStateIndicator(
        lazyPagingItems = result,
        placeholderPadding = innerPaddings,
    ) {
        SearchResultList(
            listState = searchResultListState,
            contentPadding = innerPaddings + PaddingValues(
                top = 4.dp,
                bottom = 4.dp + fabHeight
            ),
        ) {
            @Suppress("UNCHECKED_CAST")
            when (searchRoute) {
                SearchRoute.Feed -> feedItems(result as LazyPagingItems<FeedViewBean>)
                is SearchRoute.Article -> articleItems(
                    result = result as LazyPagingItems<ArticleWithFeed>,
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
                    onMessage = onMessage,
                )
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

private fun LazyGridScope.feedItems(result: LazyPagingItems<FeedViewBean>) {
    items(
        count = result.itemCount,
        key = result.safeItemKey { it.feed.url },
    ) { index ->
        when (val item = result[index]) {
            is FeedViewBean -> Feed1Item(item)
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
