package com.skyd.anivu.ui.screen.search

import android.os.Bundle
import androidx.annotation.Keep
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalAbsoluteTonalElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.skyd.anivu.R
import com.skyd.anivu.base.mvi.MviEventListener
import com.skyd.anivu.base.mvi.getDispatcher
import com.skyd.anivu.ext.navigate
import com.skyd.anivu.ext.plus
import com.skyd.anivu.ext.safeItemKey
import com.skyd.anivu.model.bean.article.ArticleWithFeed
import com.skyd.anivu.model.bean.feed.FeedViewBean
import com.skyd.anivu.ui.component.BackIcon
import com.skyd.anivu.ui.component.CircularProgressPlaceholder
import com.skyd.anivu.ui.component.ErrorPlaceholder
import com.skyd.anivu.ui.component.PagingRefreshStateIndicator
import com.skyd.anivu.ui.component.PodAuraFloatingActionButton
import com.skyd.anivu.ui.component.PodAuraIconButton
import com.skyd.anivu.ui.component.dialog.WaitingDialog
import com.skyd.anivu.ui.local.LocalSearchItemMinWidth
import com.skyd.anivu.ui.local.LocalSearchListTonalElevation
import com.skyd.anivu.ui.local.LocalSearchTopBarTonalElevation
import com.skyd.anivu.ui.screen.article.Article1Item
import com.skyd.anivu.ui.screen.article.Article1ItemPlaceholder
import com.skyd.anivu.ui.screen.feed.item.Feed1Item
import com.skyd.anivu.ui.screen.feed.item.Feed1ItemPlaceholder
import kotlinx.coroutines.launch
import java.io.Serializable


const val SEARCH_SCREEN_ROUTE = "searchScreen"
const val SEARCH_DOMAIN_KEY = "searchDomain"

fun openSearchScreen(
    navController: NavController,
    searchDomain: SearchDomain,
    navOptions: NavOptions? = null,
) {
    navController.navigate(
        SEARCH_SCREEN_ROUTE,
        Bundle().apply {
            putSerializable(SEARCH_DOMAIN_KEY, searchDomain)
        },
        navOptions = navOptions,
    )
}

@kotlinx.serialization.Serializable
sealed interface SearchDomain : Serializable {
    data object Feed : SearchDomain {
        @Keep
        private fun readResolve(): Any = Feed
    }

    data class Article(
        val feedUrls: List<String>,
        val groupIds: List<String>,
        val articleIds: List<String>,
    ) : SearchDomain
}

@Composable
fun SearchScreen(
    searchDomain: SearchDomain,
    viewModel: SearchViewModel = hiltViewModel(),
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
        startWith = when (searchDomain) {
            SearchDomain.Feed -> SearchIntent.ListenSearchFeed
            is SearchDomain.Article -> SearchIntent.ListenSearchArticle(
                feedUrls = searchDomain.feedUrls,
                groupIds = searchDomain.groupIds,
                articleIds = searchDomain.articleIds,
            )
        }
    )

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
                    contentDescription = stringResource(R.string.to_top),
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
                    .background(
                        MaterialTheme.colorScheme.surfaceColorAtElevation(
                            LocalSearchTopBarTonalElevation.current.dp
                        )
                    )
                    .windowInsetsPadding(
                        WindowInsets.systemBars
                            .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
                    )
            ) {
                SearchBarInputField(
                    onQueryChange = {
                        searchFieldValueState = it
                        dispatch(SearchIntent.UpdateQuery(it.text))
                    },
                    query = searchFieldValueState,
                    onSearch = { keyboardController?.hide() },
                    placeholder = { Text(text = stringResource(R.string.search_screen_hint)) },
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
        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(
            LocalAbsoluteTonalElevation.current +
                    LocalSearchListTonalElevation.current.dp
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
                searchDomain = searchDomain,
                dispatch = dispatch,
                innerPaddings = innerPaddings,
                fabHeight = fabHeight,
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
    searchDomain: SearchDomain,
    dispatch: (SearchIntent) -> Unit,
    innerPaddings: PaddingValues,
    fabHeight: Dp,
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
            when (searchDomain) {
                SearchDomain.Feed -> feedItems(result as LazyPagingItems<FeedViewBean>)
                is SearchDomain.Article -> articleItems(
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
        columns = GridCells.Adaptive(LocalSearchItemMinWidth.current.dp),
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
        PodAuraIconButton(
            imageVector = Icons.Outlined.Clear,
            contentDescription = stringResource(R.string.clear_input_text),
            onClick = { onClick?.invoke() }
        )
    }
}

@Composable
fun SearchBarInputField(
    query: TextFieldValue,
    onQueryChange: (TextFieldValue) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val focusRequester = remember { FocusRequester() }
    TextField(
        modifier = modifier
            .focusRequester(focusRequester)
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .height(72.dp),
        value = query,
        onValueChange = onQueryChange,
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            errorIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
        ),
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        placeholder = placeholder,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        interactionSource = interactionSource,
        singleLine = true,
        shape = RectangleShape,
    )

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}