package com.skyd.anivu.ui.screen

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.skyd.anivu.R
import com.skyd.anivu.ext.isCompact
import com.skyd.anivu.ext.thenIf
import com.skyd.anivu.model.preference.appearance.NavigationBarLabelPreference
import com.skyd.anivu.ui.local.LocalMediaLibLocation
import com.skyd.anivu.ui.local.LocalNavigationBarLabel
import com.skyd.anivu.ui.local.LocalWindowSizeClass
import com.skyd.anivu.ui.screen.feed.FEED_SCREEN_ROUTE
import com.skyd.anivu.ui.screen.feed.FeedScreen
import com.skyd.anivu.ui.screen.media.MEDIA_SCREEN_ROUTE
import com.skyd.anivu.ui.screen.media.MediaScreen
import com.skyd.anivu.ui.screen.more.MORE_SCREEN_ROUTE
import com.skyd.anivu.ui.screen.more.MoreScreen
import com.skyd.anivu.ui.screen.playlist.PLAYLIST_SCREEN_ROUTE
import com.skyd.anivu.ui.screen.playlist.PlaylistScreen

const val MAIN_SCREEN_ROUTE = "mainScreen"

@Composable
fun MainScreen() {
    val windowSizeClass = LocalWindowSizeClass.current
    val mainNavController = rememberNavController()

    val navigationBarOrRail: @Composable () -> Unit = @Composable {
        NavigationBarOrRail(navController = mainNavController)
    }

    Scaffold(
        bottomBar = {
            if (windowSizeClass.isCompact) {
                navigationBarOrRail()
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Row(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .thenIf(!windowSizeClass.isCompact) {
                    windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                },
        ) {
            if (!windowSizeClass.isCompact) {
                navigationBarOrRail()
            }
            NavHost(
                navController = mainNavController,
                startDestination = FEED_SCREEN_ROUTE,
                modifier = Modifier.weight(1f),
                enterTransition = { fadeIn(animationSpec = tween(170)) },
                exitTransition = { fadeOut(animationSpec = tween(170)) },
                popEnterTransition = { fadeIn(animationSpec = tween(170)) },
                popExitTransition = { fadeOut(animationSpec = tween(170)) },
            ) {
                composable(FEED_SCREEN_ROUTE) { FeedScreen() }
                composable(PLAYLIST_SCREEN_ROUTE) { PlaylistScreen() }
                composable(MEDIA_SCREEN_ROUTE) { MediaScreen(path = LocalMediaLibLocation.current) }
                composable(MORE_SCREEN_ROUTE) { MoreScreen() }
            }
        }
    }
}

@Composable
private fun NavigationBarOrRail(navController: NavController) {
    val items = listOf(
        stringResource(R.string.feed_screen_name) to FEED_SCREEN_ROUTE,
        stringResource(R.string.playlist) to PLAYLIST_SCREEN_ROUTE,
        stringResource(R.string.media_screen_name) to MEDIA_SCREEN_ROUTE,
        stringResource(R.string.more_screen_name) to MORE_SCREEN_ROUTE,
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
    val currentDest = navBackStackEntry?.destination

    val onClick: (Int) -> Unit = { index ->
        navController.navigate(items[index].second) {
            // Pop up to the previous (?: start) destination of the graph to
            // avoid building up a large stack of destinations on the back stack as users select items
            popUpTo(
                id = navController.currentDestination?.id
                    ?: navController.graph.findStartDestination().id
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

    val navigationBarLabel = LocalNavigationBarLabel.current
    if (LocalWindowSizeClass.current.isCompact) {
        NavigationBar {
            items.forEachIndexed { index, item ->
                val selected = currentDest?.hierarchy?.any { it.route == item.second } == true
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
        NavigationRail {
            items.forEachIndexed { index, item ->
                val selected = currentDest?.hierarchy?.any { it.route == item.second } == true
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