package com.skyd.podaura.ui.screen.feed

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.savedstate.serialization.SavedStateConfiguration
import com.skyd.compone.component.navigation.LocalNavBackStack
import com.skyd.compone.component.navigation.newNavBackStack
import com.skyd.compone.ext.isSinglePane
import com.skyd.podaura.ui.component.navigation.PodAuraSerializersModule
import com.skyd.podaura.ui.screen.article.ArticleRoute
import kotlinx.serialization.Serializable


@Serializable
data object FeedRoute

@Composable
fun FeedScreen() {
    val windowAdaptiveInfo = currentWindowAdaptiveInfo()
    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>(
        directive = remember(windowAdaptiveInfo) {
            calculatePaneScaffoldDirective(windowAdaptiveInfo)
                .copy(horizontalPartitionSpacerSize = 0.dp)
        }
    )
    val newBackStack = rememberNavBackStack(
        configuration = SavedStateConfiguration { serializersModule = PodAuraSerializersModule },
        *buildList {
            add(FeedListRoute())
            if (!listDetailStrategy.isSinglePane) {
                add(ArticleRoute())
            }
        }.toTypedArray()
    )

    CompositionLocalProvider(LocalNavBackStack provides newNavBackStack(newBackStack)) {
        val navBackStack = LocalNavBackStack.current
        FeedDetailPaneNavDisplay(
            navBackStack = navBackStack,
            sceneStrategy = listDetailStrategy,
        )
    }
}