package com.skyd.anivu.ui.screen.article

import android.net.Uri
import android.os.Bundle
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalAbsoluteTonalElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
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
import com.skyd.anivu.ext.onlyHorizontal
import com.skyd.anivu.ext.plus
import com.skyd.anivu.ext.safeItemKey
import com.skyd.anivu.ext.toEncodedUrl
import com.skyd.anivu.ext.withoutTop
import com.skyd.anivu.model.bean.article.ArticleWithFeed
import com.skyd.anivu.model.repository.ArticleSort
import com.skyd.anivu.ui.component.BackIcon
import com.skyd.anivu.ui.component.CircularProgressPlaceholder
import com.skyd.anivu.ui.component.ErrorPlaceholder
import com.skyd.anivu.ui.component.PagingRefreshStateIndicator
import com.skyd.anivu.ui.component.PodAuraFloatingActionButton
import com.skyd.anivu.ui.component.PodAuraIconButton
import com.skyd.anivu.ui.component.PodAuraTopBar
import com.skyd.anivu.ui.component.dialog.WaitingDialog
import com.skyd.anivu.ui.local.LocalArticleItemMinWidth
import com.skyd.anivu.ui.local.LocalArticleListTonalElevation
import com.skyd.anivu.ui.local.LocalArticleTopBarTonalElevation
import com.skyd.anivu.ui.local.LocalNavController
import com.skyd.anivu.ui.local.LocalShowArticlePullRefresh
import com.skyd.anivu.ui.local.LocalShowArticleTopBarRefresh
import com.skyd.anivu.ui.screen.search.SearchDomain
import com.skyd.anivu.ui.screen.search.openSearchScreen
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json


const val ARTICLE_SCREEN_ROUTE = "articleScreen"
const val FEED_URLS_KEY = "feedUrls"
const val GROUP_IDS_KEY = "groupIds"
const val ARTICLE_IDS_KEY = "articleIds"
const val ARTICLE_SCREEN_DEEP_LINK = "anivu://article.screen"
const val FEED_URLS_JSON_KEY = "feedUrlsJson"
const val GROUP_IDS_JSON_KEY = "groupIdsJson"
const val ARTICLE_IDS_JSON_KEY = "articleIdsJson"

fun getArticleScreenDeepLink(
    feedUrls: List<String>,
    groupIds: List<String> = emptyList(),
    articleIds: List<String> = emptyList(),
): Uri {
    return ARTICLE_SCREEN_DEEP_LINK.toUri().buildUpon()
        .appendQueryParameter(
            FEED_URLS_JSON_KEY,
            Json.encodeToString(feedUrls).toEncodedUrl(allow = null)
        )
        .appendQueryParameter(
            GROUP_IDS_JSON_KEY,
            Json.encodeToString(groupIds).toEncodedUrl(allow = null)
        )
        .appendQueryParameter(
            ARTICLE_IDS_JSON_KEY,
            Json.encodeToString(articleIds).toEncodedUrl(allow = null)
        )
        .build()
}

fun openArticleScreen(
    navController: NavController,
    feedUrls: List<String>,
    groupIds: List<String> = emptyList(),
    articleIds: List<String> = emptyList(),
    navOptions: NavOptions? = null,
) {
    navController.navigate(
        ARTICLE_SCREEN_ROUTE,
        Bundle().apply {
            putStringArrayList(FEED_URLS_KEY, ArrayList(feedUrls))
            putStringArrayList(GROUP_IDS_KEY, ArrayList(groupIds))
            putStringArrayList(ARTICLE_IDS_KEY, ArrayList(articleIds))
        },
        navOptions = navOptions,
    )
}

private val DefaultBackClick = { }

@Composable
fun ArticleScreen(
    feedUrls: List<String>,
    groupIds: List<String>,
    articleIds: List<String>,
    onBackClick: () -> Unit = DefaultBackClick,
    viewModel: ArticleViewModel = hiltViewModel(),
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val snackbarHostState = remember { SnackbarHostState() }
    val navController = LocalNavController.current
    val scope = rememberCoroutineScope()

    val listState: LazyGridState = rememberLazyGridState()
    var fabHeight by remember { mutableStateOf(0.dp) }
    var showFilterBar by rememberSaveable { mutableStateOf(false) }

    val dispatch = viewModel.getDispatcher(
        feedUrls, startWith = ArticleIntent.Init(
            urls = feedUrls,
            groupIds = groupIds,
            articleIds = articleIds,
        )
    )
    val uiState by viewModel.viewState.collectAsStateWithLifecycle()

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            PodAuraTopBar(
                title = { Text(text = stringResource(R.string.article_screen_name)) },
                navigationIcon = {
                    if (onBackClick == DefaultBackClick) BackIcon()
                    else BackIcon(onClick = onBackClick)
                },
                colors = TopAppBarDefaults.topAppBarColors().copy(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(
                        LocalArticleTopBarTonalElevation.current.dp
                    ),
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(
                        LocalArticleTopBarTonalElevation.current.dp + 4.dp
                    ),
                ),
                actions = {
                    if (LocalShowArticleTopBarRefresh.current) {
                        val angle = if (uiState.articleListState.loading) {
                            val infiniteTransition =
                                rememberInfiniteTransition(label = "topBarRefreshTransition")
                            infiniteTransition.animateFloat(
                                initialValue = 0f,
                                targetValue = 360f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1000, easing = LinearEasing)
                                ),
                                label = "topBarRefreshAnimate",
                            ).value
                        } else 0f
                        PodAuraIconButton(
                            onClick = { dispatch(ArticleIntent.Refresh(feedUrls, groupIds)) },
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = stringResource(id = R.string.refresh),
                            rotate = angle,
                            enabled = !uiState.articleListState.loading,
                        )
                    }
                    FilterIcon(
                        filterCount = uiState.articleFilterState.filterCount,
                        showFilterBar = showFilterBar,
                        onFilterBarVisibilityChanged = { showFilterBar = it },
                        onFilterFavorite = { dispatch(ArticleIntent.FilterFavorite(it)) },
                        onFilterRead = { dispatch(ArticleIntent.FilterRead(it)) },
                        onSort = { dispatch(ArticleIntent.UpdateSort(it)) },
                    )
                    PodAuraIconButton(
                        onClick = {
                            openSearchScreen(
                                navController = navController,
                                searchDomain = SearchDomain.Article(
                                    feedUrls = feedUrls,
                                    articleIds = articleIds,
                                ),
                            )
                        },
                        imageVector = Icons.Outlined.Search,
                        contentDescription = stringResource(id = R.string.article_screen_search_article),
                    )
                },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = remember { derivedStateOf { listState.firstVisibleItemIndex > 2 } }.value,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                PodAuraFloatingActionButton(
                    onClick = { scope.launch { listState.animateScrollToItem(0) } },
                    onSizeWithSinglePaddingChanged = { _, height -> fabHeight = height },
                    contentDescription = stringResource(R.string.to_top),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ArrowUpward,
                        contentDescription = null,
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(
            LocalAbsoluteTonalElevation.current +
                    LocalArticleListTonalElevation.current.dp
        ),
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) { paddingValues ->
        Content(
            uiState = uiState,
            listState = listState,
            nestedScrollConnection = scrollBehavior.nestedScrollConnection,
            showFilterBar = showFilterBar,
            onRefresh = { dispatch(ArticleIntent.Refresh(feedUrls, groupIds)) },
            onFilterFavorite = { dispatch(ArticleIntent.FilterFavorite(it)) },
            onFilterRead = { dispatch(ArticleIntent.FilterRead(it)) },
            onSort = { dispatch(ArticleIntent.UpdateSort(it)) },
            onFavorite = { articleWithFeed, favorite ->
                dispatch(
                    ArticleIntent.Favorite(
                        articleId = articleWithFeed.articleWithEnclosure.article.articleId,
                        favorite = favorite,
                    )
                )
            },
            onRead = { articleWithFeed, read ->
                dispatch(
                    ArticleIntent.Read(
                        articleId = articleWithFeed.articleWithEnclosure.article.articleId,
                        read = read,
                    )
                )
            },
            contentPadding = paddingValues + PaddingValues(bottom = fabHeight),
        )

        WaitingDialog(visible = uiState.loadingDialog)

        MviEventListener(viewModel.singleEvent) { event ->
            when (event) {
                is ArticleEvent.InitArticleListResultEvent.Failed ->
                    snackbarHostState.showSnackbar(event.msg)

                is ArticleEvent.RefreshArticleListResultEvent.Failed ->
                    snackbarHostState.showSnackbar(event.msg)

                is ArticleEvent.FavoriteArticleResultEvent.Failed ->
                    snackbarHostState.showSnackbar(event.msg)

                is ArticleEvent.ReadArticleResultEvent.Failed ->
                    snackbarHostState.showSnackbar(event.msg)
            }
        }
    }
}

@Composable
private fun Content(
    uiState: ArticleState,
    listState: LazyGridState,
    nestedScrollConnection: NestedScrollConnection,
    showFilterBar: Boolean,
    onRefresh: () -> Unit,
    onFilterFavorite: (Boolean?) -> Unit,
    onFilterRead: (Boolean?) -> Unit,
    onSort: (ArticleSort) -> Unit,
    onFavorite: (ArticleWithFeed, Boolean) -> Unit,
    onRead: (ArticleWithFeed, Boolean) -> Unit,
    contentPadding: PaddingValues,
) {
    val state = rememberPullToRefreshState()
    Box(
        modifier = Modifier
            .pullToRefresh(
                state = state,
                enabled = LocalShowArticlePullRefresh.current,
                onRefresh = onRefresh,
                isRefreshing = uiState.articleListState.loading
            )
            .padding(top = contentPadding.calculateTopPadding())
    ) {
        Column {
            AnimatedVisibility(visible = showFilterBar) {
                Column(modifier = Modifier.padding(contentPadding.onlyHorizontal())) {
                    FilterRow(
                        articleFilterState = uiState.articleFilterState,
                        onFilterFavorite = onFilterFavorite,
                        onFilterRead = onFilterRead,
                        onSort = onSort,
                    )
                    HorizontalDivider()
                }
            }

            val currentContentPadding = contentPadding.withoutTop() + PaddingValues(vertical = 4.dp)
            when (val articleListState = uiState.articleListState) {
                is ArticleListState.Init -> CircularProgressPlaceholder(
                    contentPadding = currentContentPadding,
                )

                is ArticleListState.Failed -> ErrorPlaceholder(
                    modifier = Modifier.sizeIn(maxHeight = 200.dp),
                    text = articleListState.msg,
                    contentPadding = currentContentPadding,
                )

                is ArticleListState.Success -> ArticleList(
                    modifier = Modifier.nestedScroll(nestedScrollConnection),
                    articles = articleListState.articlePagingDataFlow.collectAsLazyPagingItems(),
                    listState = listState,
                    onFavorite = onFavorite,
                    onRead = onRead,
                    contentPadding = currentContentPadding,
                )
            }
        }

        if (LocalShowArticlePullRefresh.current) {
            Indicator(
                isRefreshing = uiState.articleListState.loading,
                state = state,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
    }
}

@Composable
private fun ArticleList(
    modifier: Modifier = Modifier,
    articles: LazyPagingItems<ArticleWithFeed>,
    listState: LazyGridState,
    onFavorite: (ArticleWithFeed, Boolean) -> Unit,
    onRead: (ArticleWithFeed, Boolean) -> Unit,
    contentPadding: PaddingValues,
) {
    PagingRefreshStateIndicator(
        lazyPagingItems = articles,
        placeholderPadding = contentPadding,
    ) {
        LazyVerticalGrid(
            modifier = modifier.fillMaxSize(),
            columns = GridCells.Adaptive(LocalArticleItemMinWidth.current.dp),
            state = listState,
            contentPadding = contentPadding + PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(
                count = articles.itemCount,
                key = articles.safeItemKey { it.articleWithEnclosure.article.articleId },
            ) { index ->
                when (val item = articles[index]) {
                    is ArticleWithFeed -> Article1Item(
                        data = item,
                        onFavorite = onFavorite,
                        onRead = onRead,
                    )

                    null -> Article1ItemPlaceholder()
                }
            }
        }
    }
}