package com.skyd.podaura.ui.screen

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.outlined.CalendarMonth
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.skyd.compone.component.navigation.multiplestacks.NavigationState
import com.skyd.compone.component.navigation.multiplestacks.Navigator
import com.skyd.compone.component.navigation.multiplestacks.rememberNavigationState
import com.skyd.podaura.ext.getOrDefault
import com.skyd.podaura.ext.isCompact
import com.skyd.podaura.model.preference.appearance.NavigationBarLabelPreference
import com.skyd.podaura.model.preference.behavior.DefaultHomeTabPreference
import com.skyd.podaura.model.preference.data.medialib.MediaLibLocationPreference
import com.skyd.podaura.model.preference.dataStore
import com.skyd.podaura.ui.component.navigation.PodAuraSerializersModule
import com.skyd.podaura.ui.local.LocalWindowSizeClass
import com.skyd.podaura.ui.player.LocalPlayerSession
import com.skyd.podaura.ui.player.mini.MiniPlayer
import com.skyd.podaura.ui.screen.calendar.CalendarRoute
import com.skyd.podaura.ui.screen.calendar.CalendarScreen
import com.skyd.podaura.ui.screen.feed.FeedRoute
import com.skyd.podaura.ui.screen.feed.FeedScreen
import com.skyd.podaura.ui.screen.media.MediaRoute
import com.skyd.podaura.ui.screen.media.MediaScreen
import com.skyd.podaura.ui.screen.more.MoreRoute
import com.skyd.podaura.ui.screen.more.MoreScreen
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.calendar_screen_name
import podaura.shared.generated.resources.feed_screen_name
import podaura.shared.generated.resources.media_screen_name
import podaura.shared.generated.resources.more_screen_name


@Serializable
data object MainRoute : NavKey

internal val mainTabRoutes: List<NavKey> = listOf(FeedRoute, CalendarRoute, MediaRoute, MoreRoute)

@Composable
fun MainScreen() {
    val windowSizeClass = LocalWindowSizeClass.current
    val playerSession = LocalPlayerSession.current
    val navigationState = rememberMainNavigationState(
        defaultHomeTab = remember { dataStore.getOrDefault(DefaultHomeTabPreference) },
    )
    val navigator = remember(navigationState) { Navigator(navigationState) }

    val navigationBarOrRail: @Composable () -> Unit = {
        NavigationBarOrRail(
            routes = mainTabRoutes,
            navigationState = navigationState,
            onSelectedChanged = { navigator.navigate(it) },
        )
    }
    val miniPlayer: @Composable (WindowInsets) -> Unit = { windowInsets ->
        MiniPlayer(
            coordinator = playerSession?.coordinator,
            onOpenPlayer = { playerSession?.openFullPlayer() },
            onClosePlayer = { playerSession?.destroySession() },
            windowInsets = windowInsets,
        )
    }

    Scaffold(
        bottomBar = {
            if (windowSizeClass.isCompact) {
                Column {
                    miniPlayer(
                        WindowInsets.systemBars.union(WindowInsets.displayCutout)
                            .only(WindowInsetsSides.Horizontal)
                    )
                    navigationBarOrRail()
                }
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

            Column(modifier = Modifier.weight(1f)) {
                val entryProvider: (NavKey) -> NavEntry<NavKey> = entryProvider {
                    entry<FeedRoute> { FeedScreen() }
                    entry<CalendarRoute> { CalendarScreen() }
                    entry<MediaRoute> { MediaScreen(path = MediaLibLocationPreference.current) }
                    entry<MoreRoute> { MoreScreen() }
                }
                MainTabNavDisplay(
                    navigationState = navigationState,
                    entryProvider = entryProvider,
                    modifier = Modifier.weight(1f),
                    onBack = { navigator.goBack() },
                )
                if (!windowSizeClass.isCompact) {
                    miniPlayer(
                        WindowInsets.systemBars.union(WindowInsets.displayCutout)
                            .only(WindowInsetsSides.Bottom + WindowInsetsSides.End)
                    )
                }
            }
        }
    }
}

@Composable
internal fun rememberMainNavigationState(defaultHomeTab: String): NavigationState {
    // Keep the startup choice stable when settings change or this entry is restored.
    val initialHomeTab = rememberSaveable { defaultHomeTab }
    val startRoute: NavKey = when (initialHomeTab) {
        DefaultHomeTabPreference.CALENDAR -> CalendarRoute
        DefaultHomeTabPreference.MEDIA -> MediaRoute
        DefaultHomeTabPreference.MORE -> MoreRoute
        else -> FeedRoute
    }
    return rememberNavigationState(
        startRoute = startRoute,
        topLevelRoutes = mainTabRoutes.toSet(),
        configuration = SavedStateConfiguration { serializersModule = PodAuraSerializersModule },
    )
}

@Composable
internal fun MainTabNavDisplay(
    navigationState: NavigationState,
    entryProvider: (NavKey) -> NavEntry<NavKey>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val entries = navigationState.toDecoratedEntries(entryProvider)
    val currentStack = navigationState.backStacks.getValue(navigationState.topLevelRoute)
    val contentTransform = fadeIn(animationSpec = tween(170)) togetherWith
            fadeOut(animationSpec = tween(170))
    NavDisplay(
        // Compone prepends the home stack. Tabs are peers: only the current stack handles Back.
        entries = entries.takeLast(currentStack.size),
        modifier = modifier,
        transitionSpec = { contentTransform },
        popTransitionSpec = { contentTransform },
        predictivePopTransitionSpec = { contentTransform },
        onBack = onBack,
    )
}

@Composable
private fun NavigationBarOrRail(
    routes: List<NavKey>,
    navigationState: NavigationState,
    onSelectedChanged: (NavKey) -> Unit,
) {
    val items = listOf(
        stringResource(Res.string.feed_screen_name),
        stringResource(Res.string.calendar_screen_name),
        stringResource(Res.string.media_screen_name),
        stringResource(Res.string.more_screen_name),
    )
    val icons = remember {
        mapOf(
            true to listOf(
                Icons.Filled.RssFeed,
                Icons.Filled.CalendarMonth,
                Icons.Filled.Movie,
                Icons.Filled.Widgets
            ),
            false to listOf(
                Icons.Outlined.RssFeed,
                Icons.Outlined.CalendarMonth,
                Icons.Outlined.Movie,
                Icons.Outlined.Widgets
            ),
        )
    }

    val navigationBarLabel = NavigationBarLabelPreference.current
    if (LocalWindowSizeClass.current.isCompact) {
        NavigationBar(
            windowInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout).only(
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
            windowInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout).only(
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
