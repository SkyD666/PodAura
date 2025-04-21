package com.skyd.anivu.ui.screen.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.RssFeed
import androidx.compose.material.icons.outlined.SmartDisplay
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.layout.rememberPaneExpansionState
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import com.skyd.anivu.R
import com.skyd.anivu.ext.isSinglePane
import com.skyd.anivu.ui.component.BaseSettingsItem
import com.skyd.anivu.ui.component.PodAuraAnimatedPane
import com.skyd.anivu.ui.component.PodAuraTopBar
import com.skyd.anivu.ui.component.PodAuraTopBarStyle
import com.skyd.anivu.ui.component.SelectedItem
import com.skyd.anivu.ui.local.LocalNavController
import com.skyd.anivu.ui.screen.settings.appearance.AppearanceRoute
import com.skyd.anivu.ui.screen.settings.behavior.BehaviorRoute
import com.skyd.anivu.ui.screen.settings.data.DataRoute
import com.skyd.anivu.ui.screen.settings.playerconfig.PlayerConfigRoute
import com.skyd.anivu.ui.screen.settings.rssconfig.RssConfigRoute
import com.skyd.anivu.ui.screen.settings.transmission.TransmissionRoute
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable


@Serializable
data object SettingsRoute

@Composable
fun SettingsScreen() {
    val navigator = rememberListDetailPaneScaffoldNavigator<Any>(
        scaffoldDirective = calculatePaneScaffoldDirective(currentWindowAdaptiveInfo()).copy(
            horizontalPartitionSpacerSize = 0.dp,
        ),
    )
    val paneExpansionState = rememberPaneExpansionState()
    LaunchedEffect(Unit) {
        paneExpansionState.setFirstPaneProportion(0.36f)
    }
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    var currentRoute by remember {
        mutableStateOf(navigator.currentDestination?.contentKey ?: AppearanceRoute)
    }
    val onNavigate: (Any) -> Unit = {
        scope.launch { navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, it) }
        currentRoute = it
    }
    LaunchedEffect(navigator.isSinglePane) {
        if (!navigator.isSinglePane) onNavigate(currentRoute)
    }

    NavigableListDetailPaneScaffold(
        navigator = navigator,
        listPane = {
            PodAuraAnimatedPane {
                SettingsList(
                    currentItem = currentRoute.takeIf { !navigator.isSinglePane },
                    onItemSelected = onNavigate,
                )
            }
        },
        detailPane = {
            PodAuraAnimatedPane {
                CompositionLocalProvider(LocalNavController provides navController) {
                    SettingsDetailPaneNavHost(
                        navController = navController,
                        startDestination = currentRoute,
                        onPaneBack = if (navigator.isSinglePane) {
                            {
                                scope.launch { navigator.navigateBack() }
                            }
                        } else null,
                    )
                }
            }
        },
        paneExpansionState = paneExpansionState,
    )
}

@Composable
fun SettingsList(
    currentItem: Any?,
    onItemSelected: (Any) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        topBar = {
            PodAuraTopBar(
                style = PodAuraTopBarStyle.Large,
                scrollBehavior = scrollBehavior,
                title = { Text(text = stringResource(R.string.settings)) },
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = paddingValues,
        ) {
            item {
                SelectedItem(currentItem is AppearanceRoute) {
                    BaseSettingsItem(
                        icon = rememberVectorPainter(Icons.Outlined.Palette),
                        text = stringResource(id = R.string.appearance_screen_name),
                        descriptionText = stringResource(id = R.string.appearance_screen_description),
                        onClick = {
                            onItemSelected(AppearanceRoute)
                        }
                    )
                }
            }
            item {
                SelectedItem(currentItem is BehaviorRoute) {
                    BaseSettingsItem(
                        icon = rememberVectorPainter(Icons.Outlined.TouchApp),
                        text = stringResource(id = R.string.behavior_screen_name),
                        descriptionText = stringResource(id = R.string.behavior_screen_description),
                        onClick = {
                            onItemSelected(BehaviorRoute)
                        }
                    )
                }
            }
            item {
                SelectedItem(currentItem is RssConfigRoute) {
                    BaseSettingsItem(
                        icon = rememberVectorPainter(Icons.Outlined.RssFeed),
                        text = stringResource(id = R.string.rss_config_screen_name),
                        descriptionText = stringResource(id = R.string.rss_config_screen_description),
                        onClick = {
                            onItemSelected(RssConfigRoute)
                        }
                    )
                }
            }
            item {
                SelectedItem(currentItem is PlayerConfigRoute) {
                    BaseSettingsItem(
                        icon = rememberVectorPainter(Icons.Outlined.SmartDisplay),
                        text = stringResource(id = R.string.player_config_screen_name),
                        descriptionText = stringResource(id = R.string.player_config_screen_description),
                        onClick = {
                            onItemSelected(PlayerConfigRoute)
                        }
                    )
                }
            }
            item {
                SelectedItem(currentItem is DataRoute) {
                    BaseSettingsItem(
                        icon = painterResource(id = R.drawable.ic_database_24),
                        text = stringResource(id = R.string.data_screen_name),
                        descriptionText = stringResource(id = R.string.data_screen_description),
                        onClick = { onItemSelected(DataRoute) }
                    )
                }
            }
            item {
                SelectedItem(currentItem is TransmissionRoute) {
                    BaseSettingsItem(
                        icon = rememberVectorPainter(Icons.Outlined.SwapVert),
                        text = stringResource(id = R.string.transmission_screen_name),
                        descriptionText = stringResource(id = R.string.transmission_screen_description),
                        onClick = {
                            onItemSelected(TransmissionRoute)
                        }
                    )
                }
            }
        }
    }
}
