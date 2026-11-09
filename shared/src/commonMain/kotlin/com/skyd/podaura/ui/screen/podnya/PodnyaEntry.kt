package com.skyd.podaura.ui.screen.podnya

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.skyd.compone.component.BackInvoker
import com.skyd.compone.component.navigation.LocalNavBackStack
import com.skyd.podaura.ext.toDateTimeString
import com.skyd.podaura.ui.player.mini.miniPlayerEntry
import com.skyd.podaura.ui.screen.article.ArticleRoute
import com.skyd.podaura.ui.screen.read.ReadRoute
import com.skyd.podaura.ui.screen.search.SearchRoute
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

// Preserve saved route identities when moving navigation back to the host.
@Serializable
@SerialName("com.skyd.podaura.feature.podnya.integration.PodnyaRoute")
data object PodnyaRoute : NavKey

@Serializable
@SerialName("com.skyd.podaura.feature.podnya.integration.AssistantSettingsRoute")
data object AssistantSettingsRoute : NavKey

internal fun EntryProviderScope<NavKey>.podnyaEntries() {
    miniPlayerEntry<PodnyaRoute> { PodnyaEntry() }
    entry<AssistantSettingsRoute> {
        val pages = koinInject<PodnyaPages>()
        pages.settings(BackInvoker())
    }
}

@Composable
private fun PodnyaEntry() {
    val pages = koinInject<PodnyaPages>()
    val backStack = LocalNavBackStack.current
    pages.chat(
        PodnyaChatCallbacks(
            onBack = BackInvoker(),
            onSettings = { backStack.add(AssistantSettingsRoute) },
            onSearch = { backStack.add(SearchRoute.Article(emptyList(), emptyList(), emptyList())) },
            onOpenArticle = { articleId -> backStack.add(ReadRoute(articleId)) },
            onOpenFeed = { feedUrl -> backStack.add(ArticleRoute(feedUrls = listOf(feedUrl))) },
            formatDate = { millis -> millis.toDateTimeString() },
        ),
    )
}
