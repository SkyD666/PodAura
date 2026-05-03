package com.skyd.podaura.ui.screen.feed

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import com.skyd.compone.component.navigation.LocalGlobalNavBackStack
import com.skyd.podaura.ext.isCompact
import com.skyd.podaura.ext.isSinglePane
import com.skyd.podaura.ui.component.PodAuraNavDisplay
import com.skyd.podaura.ui.component.navigation.ListDetailSceneStrategy
import com.skyd.podaura.ui.local.LocalWindowSizeClass
import com.skyd.podaura.ui.screen.article.ArticleRoute
import com.skyd.podaura.ui.screen.article.ArticleRoute.Companion.ArticleLauncher
import com.skyd.podaura.ui.screen.read.ReadRoute
import com.skyd.podaura.ui.screen.read.ReadRoute.Companion.ReadLauncher
import com.skyd.podaura.ui.screen.search.SearchRoute
import com.skyd.podaura.ui.screen.search.SearchRoute.Article.Companion.SearchArticleLauncher
import com.skyd.podaura.ui.screen.search.SearchRoute.Feed.SearchFeedLauncher

@Composable
internal fun FeedDetailPaneNavDisplay(
    navBackStack: MutableList<NavKey>,
    sceneStrategy: ListDetailSceneStrategy<NavKey>,
) {
    val windowSizeClass = LocalWindowSizeClass.current
    val safeDrawingInsets = WindowInsets.safeDrawing
    val listWindowInsets =
        if (windowSizeClass.isCompact && sceneStrategy.isSinglePane)
            safeDrawingInsets.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
        else if (sceneStrategy.isSinglePane)
            safeDrawingInsets.only(WindowInsetsSides.Vertical + WindowInsetsSides.End)
        else
            safeDrawingInsets.only(WindowInsetsSides.Vertical)
    val detailWindowInsets =
        if (windowSizeClass.isCompact && sceneStrategy.isSinglePane)
            safeDrawingInsets.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
        else
            safeDrawingInsets.only(WindowInsetsSides.Vertical + WindowInsetsSides.End)

    PodAuraNavDisplay(
        backStack = navBackStack,
        sceneStrategies = listOf(sceneStrategy),
        entryProvider = entryProvider {
            entry<FeedListRoute>(metadata = ListDetailSceneStrategy.listPane()) { _ ->
                val currentArticleRoute =
                    navBackStack.filterIsInstance<ArticleRoute>().firstOrNull()
                val globalNavBackStack = LocalGlobalNavBackStack.current
                FeedList(
                    listPaneSelectedFeedUrls = currentArticleRoute?.feedUrls,
                    listPaneSelectedGroupIds = currentArticleRoute?.groupIds,
                    onShowArticleListByFeedUrls = { feedUrls ->
                        val route = ArticleRoute(feedUrls = feedUrls)
                        if (sceneStrategy.isSinglePane) {
                            globalNavBackStack.add(route)
                        } else {
                            navBackStack.removeAll { it !is FeedListRoute }
                            navBackStack.add(route)
                        }
                    },
                    onShowArticleListByGroupId = { groupId ->
                        val route = ArticleRoute(groupIds = listOf(groupId))
                        if (sceneStrategy.isSinglePane) {
                            globalNavBackStack.add(route)
                        } else {
                            navBackStack.removeAll { it !is FeedListRoute }
                            navBackStack.add(route)
                        }
                    },
                    windowInsets = listWindowInsets,
                )
            }
            entry<ArticleRoute>(metadata = ListDetailSceneStrategy.detailPane()) { route ->
                ArticleLauncher(
                    route = route,
                    onBack = if (sceneStrategy.isSinglePane ||
                        navBackStack.count { it !is FeedListRoute } > 1
                    ) {
                        { navBackStack.removeLastOrNull() }
                    } else null,
                    windowInsets = detailWindowInsets,
                )
            }
            entry<SearchRoute.Feed>(metadata = ListDetailSceneStrategy.detailPane()) {
                SearchFeedLauncher(route = it, windowInsets = detailWindowInsets)
            }
            entry<SearchRoute.Article>(metadata = ListDetailSceneStrategy.detailPane()) {
                SearchArticleLauncher(route = it, windowInsets = detailWindowInsets)
            }
            entry<ReadRoute>(metadata = ListDetailSceneStrategy.detailPane()) {
                ReadLauncher(route = it, windowInsets = detailWindowInsets)
            }
        }
    )
}
