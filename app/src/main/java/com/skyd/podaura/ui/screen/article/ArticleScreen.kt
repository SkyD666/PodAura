package com.skyd.podaura.ui.screen.article

import android.net.Uri
import android.os.Parcelable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.skyd.compone.component.BackIcon
import com.skyd.compone.component.ComponeFloatingActionButton
import com.skyd.compone.component.ComponeIconButton
import com.skyd.compone.component.ComponeTopBar
import com.skyd.compone.component.DefaultBackClick
import com.skyd.compone.component.dialog.WaitingDialog
import com.skyd.compone.ext.onlyHorizontal
import com.skyd.compone.ext.plus
import com.skyd.compone.ext.withoutTop
import com.skyd.compone.local.LocalNavController
import com.skyd.podaura.ext.safeItemKey
import com.skyd.podaura.model.bean.article.ArticleWithFeed
import com.skyd.podaura.model.preference.appearance.article.ArticleItemMinWidthPreference
import com.skyd.podaura.model.preference.appearance.article.ArticleListTonalElevationPreference
import com.skyd.podaura.model.preference.appearance.article.ArticleTopBarTonalElevationPreference
import com.skyd.podaura.model.preference.appearance.article.ShowArticlePullRefreshPreference
import com.skyd.podaura.model.preference.appearance.article.ShowArticleTopBarRefreshPreference
import com.skyd.podaura.model.repository.article.ArticleSort
import com.skyd.podaura.ui.component.CircularProgressPlaceholder
import com.skyd.podaura.ui.component.ErrorPlaceholder
import com.skyd.podaura.ui.component.PagingRefreshStateIndicator
import com.skyd.podaura.ui.component.UuidList
import com.skyd.podaura.ui.component.UuidListType
import com.skyd.podaura.ui.component.listType
import com.skyd.podaura.ui.component.uuidListType
import com.skyd.podaura.ui.mvi.MviEventListener
import com.skyd.podaura.ui.mvi.getDispatcher
import com.skyd.podaura.ui.screen.search.SearchRoute
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.article_screen_name
import podaura.shared.generated.resources.article_screen_search_article
import podaura.shared.generated.resources.refresh
import podaura.shared.generated.resources.to_top
import java.util.UUID
import kotlin.reflect.typeOf


@Serializable
@Parcelize
data class ArticleRoute(
    @SerialName("feedUrls")
    val feedUrls: List<String>? = null,
    @SerialName("groupIds")
    val groupIds: List<String>? = null,
    @SerialName("articleIds")
    val articleIds: UuidList? = null,
) : Parcelable {
    fun toDeeplink(): Uri {
        return DEEP_LINK.toUri().buildUpon().apply {
            feedUrls?.let { appendQueryParameter("feedUrls", Json.encodeToString(feedUrls)) }
            groupIds?.let { appendQueryParameter("groupIds", Json.encodeToString(groupIds)) }
            articleIds?.let {
                appendQueryParameter(
                    "articleIds",
                    UuidListType.encodeUuidList(articleIds.uuids.map { UUID.fromString(it) })
                )
            }
        }.build()
    }

    companion object {
        private const val DEEP_LINK = "podaura://article.screen"
        const val BASE_PATH = DEEP_LINK

        val typeMap = mapOf(
            typeOf<UuidList?>() to uuidListType(isNullableAllowed = true),
            typeOf<List<String>?>() to listType<String>(isNullableAllowed = true),
        )

        val deepLinks = listOf(
            navDeepLink<ArticleRoute>(basePath = BASE_PATH, typeMap = typeMap),
        )

        @Composable
        fun ArticleLauncher(entry: NavBackStackEntry, onBack: (() -> Unit)? = DefaultBackClick) {
            ArticleLauncher(entry.toRoute<ArticleRoute>(), onBack)
        }

        @Composable
        fun ArticleLauncher(route: ArticleRoute, onBack: (() -> Unit)? = DefaultBackClick) {
            ArticleScreen(
                feedUrls = route.feedUrls.orEmpty(),
                groupIds = route.groupIds.orEmpty(),
                articleIds = route.articleIds?.uuids.orEmpty(),
                onBackClick = onBack,
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
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val snackbarHostState = remember { SnackbarHostState() }
    val navController = LocalNavController.current
    val scope = rememberCoroutineScope()

    val listState: LazyGridState = rememberLazyGridState()
    var fabHeight by remember { mutableStateOf(0.dp) }
    var showFilterBar by rememberSaveable { mutableStateOf(false) }

    val dispatch = viewModel.getDispatcher(
        feedUrls, groupIds, articleIds,
        startWith = ArticleIntent.Init(
            urls = feedUrls,
            groupIds = groupIds,
            articleIds = articleIds,
        )
    )
    val uiState by viewModel.viewState.collectAsStateWithLifecycle()

    Scaffold(
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
                        filterCount = uiState.articleFilterState.filterCount,
                        showFilterBar = showFilterBar,
                        onFilterBarVisibilityChanged = { showFilterBar = it },
                        onFilterFavorite = { dispatch(ArticleIntent.FilterFavorite(it)) },
                        onFilterRead = { dispatch(ArticleIntent.FilterRead(it)) },
                        onSort = { dispatch(ArticleIntent.UpdateSort(it)) },
                    )
                    ComponeIconButton(
                        onClick = {
                            navController.navigate(
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
            onDelete = { articleWithFeed ->
                dispatch(
                    ArticleIntent.Delete(
                        articleId = articleWithFeed.articleWithEnclosure.article.articleId,
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
    onFilterFavorite: (Boolean?) -> Unit,
    onFilterRead: (Boolean?) -> Unit,
    onSort: (ArticleSort) -> Unit,
    onFavorite: (ArticleWithFeed, Boolean) -> Unit,
    onRead: (ArticleWithFeed, Boolean) -> Unit,
    onDelete: (ArticleWithFeed) -> Unit,
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
                    onDelete = onDelete,
                    contentPadding = currentContentPadding,
                )
            }
        }

        if (ShowArticlePullRefreshPreference.current) {
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
    onDelete: (ArticleWithFeed) -> Unit,
    contentPadding: PaddingValues,
) {
    PagingRefreshStateIndicator(
        lazyPagingItems = articles,
        placeholderPadding = contentPadding,
    ) {
        LazyVerticalGrid(
            modifier = modifier
                .fillMaxSize()
                .semantics { testTagsAsResourceId = true }
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
                    )

                    null -> Article1ItemPlaceholder()
                }
            }
        }
    }
}