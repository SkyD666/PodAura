package com.skyd.podaura.ui.screen.feed

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.skyd.podaura.ui.component.PodAuraNavHost
import com.skyd.podaura.ui.screen.article.ArticleRoute
import com.skyd.podaura.ui.screen.article.ArticleRoute.Companion.ArticleLauncher
import com.skyd.podaura.ui.screen.search.SearchRoute
import com.skyd.podaura.ui.screen.search.SearchRoute.Article.Companion.SearchArticleLauncher

@Composable
internal fun FeedDetailPaneNavHost(
    navController: NavHostController,
    startDestination: Any,
    onPaneBack: (() -> Unit)?,
    articleRoute: ArticleRoute
) {
    val windowInsets = WindowInsets.safeDrawing.only(
        WindowInsetsSides.Vertical + WindowInsetsSides.End
    )
    PodAuraNavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        composable<ArticleRoute>(typeMap = ArticleRoute.typeMap) {
            ArticleLauncher(
                route = articleRoute,
                onBack = onPaneBack,
                windowInsets = windowInsets
            )
        }
        composable<SearchRoute.Article>(typeMap = SearchRoute.Article.typeMap) {
            SearchArticleLauncher(
                entry = it,
                windowInsets = windowInsets
            )
        }
    }
}
