package com.skyd.podaura.ui.screen.settings

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.RssFeed
import androidx.compose.material.icons.outlined.SmartDisplay
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.navigation3.runtime.NavKey
import com.skyd.compone.component.BackIcon
import com.skyd.compone.component.ComponeTopBar
import com.skyd.compone.component.ComponeTopBarStyle
import com.skyd.compone.component.navigation.LocalNavBackStack
import com.skyd.podaura.ext.isCompact
import com.skyd.podaura.ui.local.LocalWindowSizeClass
import com.skyd.podaura.ui.screen.settings.appearance.AppearanceRoute
import com.skyd.podaura.ui.screen.settings.behavior.BehaviorRoute
import com.skyd.podaura.ui.screen.settings.data.DataRoute
import com.skyd.podaura.ui.screen.settings.playerconfig.PlayerConfigRoute
import com.skyd.podaura.ui.screen.settings.rssconfig.RssConfigRoute
import com.skyd.settings.BaseSettingsItem
import com.skyd.settings.SelectedItem
import com.skyd.settings.SettingsLazyColumn
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.appearance_screen_description
import podaura.shared.generated.resources.appearance_screen_name
import podaura.shared.generated.resources.behavior_screen_description
import podaura.shared.generated.resources.behavior_screen_name
import podaura.shared.generated.resources.data_screen_description
import podaura.shared.generated.resources.data_screen_name
import podaura.shared.generated.resources.ic_database_24
import podaura.shared.generated.resources.player_config_screen_description
import podaura.shared.generated.resources.player_config_screen_name
import podaura.shared.generated.resources.rss_config_screen_description
import podaura.shared.generated.resources.rss_config_screen_name
import podaura.shared.generated.resources.settings


@Serializable
data object SettingsListRoute : NavKey

@Composable
fun SettingsList(onItemSelected: (NavKey) -> Unit) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val windowSizeClass = LocalWindowSizeClass.current
    val navBackStack = LocalNavBackStack.current

    Scaffold(
        topBar = {
            ComponeTopBar(
                style = ComponeTopBarStyle.LargeFlexible,
                scrollBehavior = scrollBehavior,
                title = { Text(text = stringResource(Res.string.settings)) },
                navigationIcon = {
                    BackIcon {
                        navBackStack.parent?.removeLastOrNull()
                    }
                },
                windowInsets =
                    if (windowSizeClass.isCompact)
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
                    else
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Start)
            )
        },
        contentWindowInsets =
            if (windowSizeClass.isCompact)
                WindowInsets.safeDrawing
            else
                WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.Start)
    ) { paddingValues ->
        SettingsLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = paddingValues,
        ) {
            group {
                item {
                    SelectedItem(navBackStack.contains(AppearanceRoute)) {
                        BaseSettingsItem(
                            icon = rememberVectorPainter(Icons.Outlined.Palette),
                            text = stringResource(Res.string.appearance_screen_name),
                            descriptionText = stringResource(Res.string.appearance_screen_description),
                            onClick = {
                                navBackStack.add(AppearanceRoute)
                            }
                        )
                    }
                }
                item {
                    SelectedItem(navBackStack.contains(BehaviorRoute)) {
                        BaseSettingsItem(
                            icon = rememberVectorPainter(Icons.Outlined.TouchApp),
                            text = stringResource(Res.string.behavior_screen_name),
                            descriptionText = stringResource(Res.string.behavior_screen_description),
                            onClick = { onItemSelected(BehaviorRoute) }
                        )
                    }
                }
                item {
                    SelectedItem(navBackStack.contains(RssConfigRoute)) {
                        BaseSettingsItem(
                            icon = rememberVectorPainter(Icons.Outlined.RssFeed),
                            text = stringResource(Res.string.rss_config_screen_name),
                            descriptionText = stringResource(Res.string.rss_config_screen_description),
                            onClick = { onItemSelected(RssConfigRoute) }
                        )
                    }
                }
                item {
                    SelectedItem(navBackStack.contains(PlayerConfigRoute)) {
                        BaseSettingsItem(
                            icon = rememberVectorPainter(Icons.Outlined.SmartDisplay),
                            text = stringResource(Res.string.player_config_screen_name),
                            descriptionText = stringResource(Res.string.player_config_screen_description),
                            onClick = { onItemSelected(PlayerConfigRoute) }
                        )
                    }
                }
                item {
                    SelectedItem(navBackStack.contains(DataRoute)) {
                        BaseSettingsItem(
                            icon = painterResource(Res.drawable.ic_database_24),
                            text = stringResource(Res.string.data_screen_name),
                            descriptionText = stringResource(Res.string.data_screen_description),
                            onClick = { onItemSelected(DataRoute) }
                        )
                    }
                }
//                item {
//                    SelectedItem(avBackStack.contains(TransmissionRoute)) {
//                        BaseSettingsItem(
//                            icon = rememberVectorPainter(Icons.Outlined.SwapVert),
//                            text = stringResource(Res.string.transmission_screen_name),
//                            descriptionText = stringResource(Res.string.transmission_screen_description),
//                            onClick = {
//                                onItemSelected(TransmissionRoute)
//                            }
//                        )
//                    }
//                }
            }
        }
    }
}