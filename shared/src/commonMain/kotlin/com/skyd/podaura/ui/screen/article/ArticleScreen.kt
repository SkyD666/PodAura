package com.skyd.podaura.ui.screen.article

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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.LoadingIndicator
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
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
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.skyd.compone.component.BackIcon
import com.skyd.compone.component.ComponeFloatingActionButton
import com.skyd.compone.component.ComponeIconButton
import com.skyd.compone.component.ComponeScaffold
import com.skyd.compone.component.ComponeTopBar
import com.skyd.compone.component.DefaultBackClick
import com.skyd.compone.component.dialog.WaitingDialog
import com.skyd.compone.component.navigation.LocalNavBackStack
import com.skyd.compone.ext.onlyHorizontal
import com.skyd.compone.ext.plus
import com.skyd.compone.ext.setText
import com.skyd.compone.ext.withoutTop
import com.skyd.mvi.MviEventListener
import com.skyd.mvi.getDispatcher
import com.skyd.podaura.ext.getOrDefault
import com.skyd.podaura.ext.safeItemKey
import com.skyd.podaura.model.bean.article.ArticleWithFeed
import com.skyd.podaura.model.bean.feed.FeedBean
import com.skyd.podaura.model.preference.appearance.article.ArticleItemMinWidthPreference
import com.skyd.podaura.model.preference.appearance.article.ArticleListTonalElevationPreference
import com.skyd.podaura.model.preference.appearance.article.ArticleTopBarTonalElevationPreference
import com.skyd.podaura.model.preference.appearance.article.ShowArticlePullRefreshPreference
import com.skyd.podaura.model.preference.appearance.article.ShowArticleTopBarRefreshPreference
import com.skyd.podaura.model.preference.behavior.article.AlwaysShowArticleFilterPreference
import com.skyd.podaura.model.preference.dataStore
import com.skyd.podaura.ui.component.CircularProgressPlaceholder
import com.skyd.podaura.ui.component.ErrorPlaceholder
import com.skyd.podaura.ui.component.PagingRefreshStateIndicator
import com.skyd.podaura.ui.component.UuidList
import com.skyd.podaura.ui.component.UuidListType
import com.skyd.podaura.ui.component.listType
import com.skyd.podaura.ui.component.navigation.deeplink.DeepLinkPattern
import com.skyd.podaura.ui.component.uuidListType
import com.skyd.podaura.ui.screen.search.SearchRoute
import io.ktor.http.URLBuilder
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.article_screen_name
import podaura.shared.generated.resources.article_screen_search_article
import podaura.shared.generated.resources.copy
import podaura.shared.generated.resources.refresh
import podaura.shared.generated.resources.to_top
import kotlin.uuid.Uuid


@Serializable
data class ArticleRoute(
    @SerialName("feedUrls")
    val feedUrls: List<String>? = null,
    @SerialName("groupIds")
    val groupIds: List<String>? = null,
    @SerialName("articleIds")
    val articleIds: UuidList? = null,
) : NavKey {
    fun toDeeplink(): String {
        return URLBuilder(BASE_PATH).apply {
            feedUrls?.let { parameters.append("feedUrls", Json.encodeToString(feedUrls)) }
            groupIds?.let { parameters.append("groupIds", Json.encodeToString(groupIds)) }
            articleIds?.let {
                parameters.append(
                    "articleIds",
                    UuidListType.encodeUuidList(articleIds.uuids.map { Uuid.parse(it) })
                )
            }
        }.toString()
    }

    companion object {
        private const val BASE_PATH = "podaura://article.screen"

        val deepLinkPattern = DeepLinkPattern(
            serializer(),
            urlPattern = URLBuilder(BASE_PATH).apply {
                parameters.append("feedUrls", "{feedUrls}")
                parameters.append("groupIds", "{groupIds}")
                parameters.append("articleIds", "{articleIds}")
            }.build(),
            typeParsers = mapOf(
                UuidList.serializer().descriptor.kind to uuidListType(),
                ListSerializer(String.serializer()).descriptor.kind to listType<String>(),
            )
        )

        @Composable
        fun ArticleLauncher(
            route: ArticleRoute,
            onBack: (() -> Unit)? = DefaultBackClick,
            windowInsets: WindowInsets = WindowInsets.safeDrawing
        ) {
            ArticleScreen(
                feedUrls = route.feedUrls.orEmpty(),
                groupIds = route.groupIds.orEmpty(),
                articleIds = route.articleIds?.uuids.orEmpty(),
                onBackClick = onBack,
                windowInsets = windowInsets
            )
        }
    }
}

@Composable
fun ArticleScreen(
    feedUrls: List<String>,
    groupIds: List<String>,
    articleIds: List<String>,
    onBackClick: (() -> Unit)? = DefaultBackClick,
    viewModel: ArticleViewModel = koinViewModel(),
    windowInsets: WindowInsets = WindowInsets.safeDrawing
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }
    val navBackStack = LocalNavBackStack.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboard.current

    val listState: LazyGridState = rememberLazyGridState()
    var fabHeight by remember { mutableStateOf(0.dp) }
    var showFilterBar by rememberSaveable {
        mutableStateOf(dataStore.getOrDefault(AlwaysShowArticleFilterPreference))
    }

    val dispatch = viewModel.getDispatcher(
        feedUrls, groupIds, articleIds,
        startWith = ArticleIntent.Init(
            feedUrls = feedUrls,
            groupIds = groupIds,
            articleIds = articleIds,
        )
    )
    val uiState by viewModel.viewState.collectAsStateWithLifecycle()

    ComponeScaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            ComponeTopBar(
                title = { Text(text = stringResource(Res.string.article_screen_name)) },
                navigationIcon = {
                    if (onBackClick == DefaultBackClick) BackIcon()
                    else if (onBackClick != null) BackIcon(onClick = onBackClick)
                },
                colors = TopAppBarDefaults.topAppBarColors().copy(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(
                        ArticleTopBarTonalElevationPreference.current.dp
                    ),
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(
                        ArticleTopBarTonalElevationPreference.current.dp + 4.dp
                    ),
                ),
                actions = {
                    if (ShowArticleTopBarRefreshPreference.current) {
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
                        ComponeIconButton(
                            onClick = {
                                dispatch(
                                    ArticleIntent.Refresh(
                                        feedUrls = feedUrls,
                                        groupIds = groupIds,
                                        articleIds = articleIds,
                                    )
                                )
                            },
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = stringResource(Res.string.refresh),
                            rotate = angle,
                            enabled = !uiState.articleListState.loading,
                        )
                    }
                    FilterIcon(
                        hasFilter = uiState.articleFilterState != FeedBean.DEFAULT_FILTER_MASK,
                        showFilterBar = showFilterBar,
                        onFilterBarVisibilityChanged = { showFilterBar = it },
                        onFilterMaskChanged = {
                            dispatch(
                                ArticleIntent.UpdateFilter(
                                    feedUrls = feedUrls,
                                    groupIds = groupIds,
                                    articleIds = articleIds,
                                    filterMask = it,
                                )
                            )
                        },
                    )
                    ComponeIconButton(
                        onClick = {
                            navBackStack.add(
                                SearchRoute.Article(
                                    feedUrls = feedUrls,
                                    groupIds = groupIds,
                                    articleIds = articleIds,
                                ),
                            )
                        },
                        imageVector = Icons.Outlined.Search,
                        contentDescription = stringResource(Res.string.article_screen_search_article),
                    )
                },
                windowInsets = windowInsets.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = remember { derivedStateOf { listState.firstVisibleItemIndex > 2 } }.value,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                ComponeFloatingActionButton(
                    onClick = { scope.launch { listState.animateScrollToItem(0) } },
                    onSizeWithSinglePaddingChanged = { _, height -> fabHeight = height },
                    contentDescription = stringResource(Res.string.to_top),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ArrowUpward,
                        contentDescription = stringResource(Res.string.to_top),
                    )
                }
            }
        },
        contentWindowInsets = windowInsets,
        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(
            LocalAbsoluteTonalElevation.current +
                    ArticleListTonalElevationPreference.current.dp
        ),
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) { paddingValues ->
        Content(
            uiState = uiState,
            listState = listState,
            nestedScrollConnection = scrollBehavior.nestedScrollConnection,
            showFilterBar = showFilterBar,
            onRefresh = { dispatch(ArticleIntent.Refresh(feedUrls, groupIds, articleIds)) },
            onFilterMaskChanged = {
                dispatch(
                    ArticleIntent.UpdateFilter(
                        feedUrls = feedUrls,
                        groupIds = groupIds,
                        articleIds = articleIds,
                        filterMask = it,
                    )
                )
            },
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
            onDelete = { articleWithFeed ->
                dispatch(
                    ArticleIntent.Delete(
                        articleId = articleWithFeed.articleWithEnclosure.article.articleId,
                    )
                )
            },
            onMessage = { scope.launch { snackbarHostState.showSnackbar(it) } },
            contentPadding = paddingValues + PaddingValues(bottom = fabHeight),
        )

        WaitingDialog(visible = uiState.loadingDialog)

        MviEventListener(viewModel.singleEvent) { event ->
            when (event) {
                is ArticleEvent.InitArticleListResultEvent.Failed ->
                    snackbarHostState.showSnackbar(event.msg)

                is ArticleEvent.RefreshArticleListResultEvent.Failed -> {
                    val result = snackbarHostState.showSnackbar(
                        message = event.msg,
                        actionLabel = getString(Res.string.copy),
                        duration = SnackbarDuration.Long,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        clipboard.setText(event.msg)
                    }
                }

                is ArticleEvent.FavoriteArticleResultEvent.Failed ->
                    snackbarHostState.showSnackbar(event.msg)

                is ArticleEvent.ReadArticleResultEvent.Failed ->
                    snackbarHostState.showSnackbar(event.msg)

                is ArticleEvent.DeleteArticleResultEvent.Failed ->
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
    onFilterMaskChanged: (Int) -> Unit,
    onFavorite: (ArticleWithFeed, Boolean) -> Unit,
    onRead: (ArticleWithFeed, Boolean) -> Unit,
    onDelete: (ArticleWithFeed) -> Unit,
    onMessage: (String) -> Unit,
    contentPadding: PaddingValues,
) {
    val state = rememberPullToRefreshState()
    Box(
        modifier = Modifier
            .pullToRefresh(
                state = state,
                enabled = ShowArticlePullRefreshPreference.current,
                onRefresh = onRefresh,
                isRefreshing = uiState.articleListState.loading
            )
            .padding(top = contentPadding.calculateTopPadding())
    ) {
        Column {
            AnimatedVisibility(visible = showFilterBar) {
                Column(modifier = Modifier.padding(contentPadding.onlyHorizontal())) {
                    FilterRow(
                        articleFilterMask = uiState.articleFilterState,
                        onFilterMaskChanged = onFilterMaskChanged,
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
                    onDelete = onDelete,
                    contentPadding = currentContentPadding,
                    onMessage = onMessage,
                )
            }
        }

        if (ShowArticlePullRefreshPreference.current) {
            LoadingIndicator(
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
    onDelete: (ArticleWithFeed) -> Unit,
    onMessage: (String) -> Unit,
    contentPadding: PaddingValues,
) {
    PagingRefreshStateIndicator(
        lazyPagingItems = articles,
        placeholderPadding = contentPadding,
    ) {
        LazyVerticalGrid(
            modifier = modifier
                .fillMaxSize()
                .testTag("ArticleLazyVerticalGrid"),
            columns = GridCells.Adaptive(ArticleItemMinWidthPreference.current.dp),
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
                        onDelete = onDelete,
                        onMessage = onMessage,
                    )

                    null -> Article1ItemPlaceholder()
                }
            }
        }
    }
}
