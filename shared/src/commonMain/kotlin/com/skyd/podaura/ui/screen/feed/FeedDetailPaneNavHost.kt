package com.skyd.podaura.ui.screen.feed

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import com.skyd.compone.component.navigation.LocalGlobalNavBackStack
import com.skyd.compone.ext.isSinglePane
import com.skyd.podaura.ui.component.PodAuraNavDisplay
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
    val windowInsets = WindowInsets.safeDrawing.only(
        WindowInsetsSides.Vertical + WindowInsetsSides.End
    )
    PodAuraNavDisplay(
        backStack = navBackStack,
        sceneStrategy = sceneStrategy,
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
                    windowInsets = windowInsets,
                )
            }
            entry<SearchRoute.Feed>(metadata = ListDetailSceneStrategy.detailPane()) {
                SearchFeedLauncher(it)
            }
            entry<SearchRoute.Article>(metadata = ListDetailSceneStrategy.detailPane()) {
                SearchArticleLauncher(route = it, windowInsets = windowInsets)
            }
            entry<ReadRoute>(metadata = ListDetailSceneStrategy.detailPane()) {
                ReadLauncher(it)
            }
        }
    )
}
