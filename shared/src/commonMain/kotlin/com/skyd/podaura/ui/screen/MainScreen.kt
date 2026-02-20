package com.skyd.podaura.ui.screen

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation3.runtime.NavKey
import com.skyd.podaura.ext.isCompact
import com.skyd.podaura.model.preference.appearance.NavigationBarLabelPreference
import com.skyd.podaura.model.preference.data.medialib.MediaLibLocationPreference
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
import kotlin.reflect.KClass


@Serializable
data object MainRoute : NavKey

@Composable
fun MainScreen() {
    val windowSizeClass = LocalWindowSizeClass.current
    val mainNavController = rememberNavController()

    val navigationBarOrRail: @Composable () -> Unit = {
        NavigationBarOrRail(navController = mainNavController)
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
            NavHost(
                navController = mainNavController,
                startDestination = FeedRoute,
                modifier = Modifier.weight(1f),
                enterTransition = { fadeIn(animationSpec = tween(170)) },
                exitTransition = { fadeOut(animationSpec = tween(170)) },
                popEnterTransition = { fadeIn(animationSpec = tween(170)) },
                popExitTransition = { fadeOut(animationSpec = tween(170)) },
            ) {
                composable<FeedRoute> { FeedScreen() }
                composable<PlaylistRoute> { PlaylistScreen() }
                composable<MediaRoute> { MediaScreen(path = MediaLibLocationPreference.current) }
                composable<MoreRoute> { MoreScreen() }
            }
        }
    }
}

private fun <T : Any> NavBackStackEntry?.selected(route: KClass<T>) =
    this?.destination?.hierarchy?.any { it.hasRoute(route) } == true

@Composable
private fun NavigationBarOrRail(navController: NavController) {
    val items = listOf(
        stringResource(Res.string.feed_screen_name) to FeedRoute,
        stringResource(Res.string.playlist) to PlaylistRoute,
        stringResource(Res.string.media_screen_name) to MediaRoute,
        stringResource(Res.string.more_screen_name) to MoreRoute,
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

    val navBackStackEntry by navController.currentBackStackEntryAsState()

    val onClick: (Int) -> Unit = { index ->
        navController.navigate(items[index].second) {
            // Pop up to the previous (?: start) destination of the graph to
            // avoid building up a large stack of destinations on the back stack as users select items
            popUpTo(
                route = navController.currentDestination?.route
                    ?: navController.graph.findStartDestination().route!!
            ) {
                saveState = true
                inclusive = true
            }
            // Avoid multiple copies of the same destination when reselecting the same item
            launchSingleTop = true
            // Restore state when reselecting a previously selected item
            restoreState = true
        }
    }

    val navigationBarLabel = NavigationBarLabelPreference.current
    if (LocalWindowSizeClass.current.isCompact) {
        NavigationBar(
            windowInsets = WindowInsets.safeDrawing.only(
                WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
            )
        ) {
            items.forEachIndexed { index, item ->
                val selected = navBackStackEntry.selected(item.second::class)
                NavigationBarItem(
                    icon = { Icon(icons[selected]!![index], contentDescription = item.first) },
                    label = if (navigationBarLabel == NavigationBarLabelPreference.NONE) null else {
                        { Text(item.first) }
                    },
                    alwaysShowLabel = navigationBarLabel == NavigationBarLabelPreference.SHOW,
                    selected = selected,
                    onClick = { onClick(index) }
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
                val selected = navBackStackEntry.selected(item.second::class)
                NavigationRailItem(
                    icon = { Icon(icons[selected]!![index], contentDescription = item.first) },
                    label = if (navigationBarLabel == NavigationBarLabelPreference.NONE) null else {
                        { Text(item.first) }
                    },
                    alwaysShowLabel = navigationBarLabel == NavigationBarLabelPreference.SHOW,
                    selected = selected,
                    onClick = { onClick(index) }
                )
            }
        }
    }
}
