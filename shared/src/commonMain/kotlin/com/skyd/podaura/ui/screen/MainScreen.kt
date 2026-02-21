package com.skyd.podaura.ui.screen

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.outlined.PlaylistPlay
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.RssFeed
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.skyd.compone.component.navigation.multiplestacks.NavigationState
import com.skyd.compone.component.navigation.multiplestacks.Navigator
import com.skyd.compone.component.navigation.multiplestacks.rememberNavigationState
import com.skyd.podaura.ext.isCompact
import com.skyd.podaura.model.preference.appearance.NavigationBarLabelPreference
import com.skyd.podaura.model.preference.data.medialib.MediaLibLocationPreference
import com.skyd.podaura.ui.component.navigation.PodAuraSerializersModule
import com.skyd.podaura.ui.local.LocalWindowSizeClass
import com.skyd.podaura.ui.screen.feed.FeedRoute
import com.skyd.podaura.ui.screen.feed.FeedScreen
import com.skyd.podaura.ui.screen.media.MediaRoute
import com.skyd.podaura.ui.screen.media.MediaScreen
import com.skyd.podaura.ui.screen.more.MoreRoute
import com.skyd.podaura.ui.screen.more.MoreScreen
import com.skyd.podaura.ui.screen.playlist.PlaylistRoute
import com.skyd.podaura.ui.screen.playlist.PlaylistScreen
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.feed_screen_name
import podaura.shared.generated.resources.media_screen_name
import podaura.shared.generated.resources.more_screen_name
import podaura.shared.generated.resources.playlist


@Serializable
data object MainRoute : NavKey

@Composable
fun MainScreen() {
    val windowSizeClass = LocalWindowSizeClass.current
    val routes = listOf(FeedRoute, PlaylistRoute, MediaRoute, MoreRoute)
    val navigationState = rememberNavigationState(
        startRoute = FeedRoute,
        topLevelRoutes = routes.toSet(),
        configuration = SavedStateConfiguration { serializersModule = PodAuraSerializersModule }
    )
    val navigator = remember { Navigator(navigationState) }

    val navigationBarOrRail: @Composable () -> Unit = {
        NavigationBarOrRail(
            routes = routes,
            navigationState = navigationState,
            onSelectedChanged = { navigator.navigate(it) },
        )
    }

    Scaffold(
        bottomBar = {
            if (windowSizeClass.isCompact) {
                navigationBarOrRail()
            }
        },
        contentWindowInsets = WindowInsets()
    ) { innerPadding ->
        Row(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (!windowSizeClass.isCompact) {
                navigationBarOrRail()
            }

            val contentTransform = fadeIn(animationSpec = tween(170)) togetherWith
                    fadeOut(animationSpec = tween(170))
            val entryProvider = entryProvider {
                entry<FeedRoute> { FeedScreen() }
                entry<PlaylistRoute> { PlaylistScreen() }
                entry<MediaRoute> { MediaScreen(path = MediaLibLocationPreference.current) }
                entry<MoreRoute> { MoreScreen() }
            }
            NavDisplay(
                entries = navigationState.toDecoratedEntries(entryProvider),
                modifier = Modifier.weight(1f),
                transitionSpec = { contentTransform },
                popTransitionSpec = { contentTransform },
                predictivePopTransitionSpec = { contentTransform },
                onBack = { navigator.goBack() },
            )
        }
    }
}

@Composable
private fun NavigationBarOrRail(
    routes: List<NavKey>,
    navigationState: NavigationState,
    onSelectedChanged: (NavKey) -> Unit,
) {
    val items = listOf(
        stringResource(Res.string.feed_screen_name),
        stringResource(Res.string.playlist),
        stringResource(Res.string.media_screen_name),
        stringResource(Res.string.more_screen_name),
    )
    val icons = remember {
        mapOf(
            true to listOf(
                Icons.Filled.RssFeed,
                Icons.AutoMirrored.Filled.PlaylistPlay,
                Icons.Filled.Movie,
                Icons.Filled.Widgets
            ),
            false to listOf(
                Icons.Outlined.RssFeed,
                Icons.AutoMirrored.Outlined.PlaylistPlay,
                Icons.Outlined.Movie,
                Icons.Outlined.Widgets
            ),
        )
    }

    val navigationBarLabel = NavigationBarLabelPreference.current
    if (LocalWindowSizeClass.current.isCompact) {
        NavigationBar(
            windowInsets = WindowInsets.safeDrawing.only(
                WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
            )
        ) {
            items.forEachIndexed { index, item ->
                val selected = routes[index] == navigationState.topLevelRoute
                NavigationBarItem(
                    icon = { Icon(icons[selected]!![index], contentDescription = item) },
                    label = if (navigationBarLabel == NavigationBarLabelPreference.NONE) null else {
                        { Text(item) }
                    },
                    alwaysShowLabel = navigationBarLabel == NavigationBarLabelPreference.SHOW,
                    selected = selected,
                    onClick = { onSelectedChanged(routes[index]) }
                )
            }
        }
    } else {
        NavigationRail(
            windowInsets = WindowInsets.safeDrawing.only(
                WindowInsetsSides.Vertical + WindowInsetsSides.Start
            )
        ) {
            items.forEachIndexed { index, item ->
                val selected = routes[index] == navigationState.topLevelRoute
                NavigationRailItem(
                    icon = { Icon(icons[selected]!![index], contentDescription = item) },
                    label = if (navigationBarLabel == NavigationBarLabelPreference.NONE) null else {
                        { Text(item) }
                    },
                    alwaysShowLabel = navigationBarLabel == NavigationBarLabelPreference.SHOW,
                    selected = selected,
                    onClick = { onSelectedChanged(routes[index]) }
                )
            }
        }
    }
}
