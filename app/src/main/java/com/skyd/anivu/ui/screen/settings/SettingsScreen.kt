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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.skyd.anivu.R
import com.skyd.anivu.ui.component.BaseSettingsItem
import com.skyd.anivu.ui.component.PodAuraTopBar
import com.skyd.anivu.ui.component.PodAuraTopBarStyle
import com.skyd.anivu.ui.local.LocalNavController
import com.skyd.anivu.ui.screen.settings.appearance.AppearanceRoute
import com.skyd.anivu.ui.screen.settings.behavior.BehaviorRoute
import com.skyd.anivu.ui.screen.settings.data.DataRoute
import com.skyd.anivu.ui.screen.settings.playerconfig.PlayerConfigRoute
import com.skyd.anivu.ui.screen.settings.rssconfig.RssConfigRoute
import com.skyd.anivu.ui.screen.settings.transmission.TransmissionRoute
import kotlinx.serialization.Serializable


@Serializable
data object SettingsRoute

@Composable
fun SettingsScreen() {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val navController = LocalNavController.current
    Scaffold(
        topBar = {
            PodAuraTopBar(
                style = PodAuraTopBarStyle.Large,
                scrollBehavior = scrollBehavior,
                title = { Text(text = stringResource(R.string.settings_screen_name)) },
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
                BaseSettingsItem(
                    icon = rememberVectorPainter(Icons.Outlined.Palette),
                    text = stringResource(id = R.string.appearance_screen_name),
                    descriptionText = stringResource(id = R.string.appearance_screen_description),
                    onClick = { navController.navigate(AppearanceRoute) }
                )
            }
            item {
                BaseSettingsItem(
                    icon = rememberVectorPainter(Icons.Outlined.TouchApp),
                    text = stringResource(id = R.string.behavior_screen_name),
                    descriptionText = stringResource(id = R.string.behavior_screen_description),
                    onClick = { navController.navigate(BehaviorRoute) }
                )
            }
            item {
                BaseSettingsItem(
                    icon = rememberVectorPainter(Icons.Outlined.RssFeed),
                    text = stringResource(id = R.string.rss_config_screen_name),
                    descriptionText = stringResource(id = R.string.rss_config_screen_description),
                    onClick = { navController.navigate(RssConfigRoute) }
                )
            }
            item {
                BaseSettingsItem(
                    icon = rememberVectorPainter(Icons.Outlined.SmartDisplay),
                    text = stringResource(id = R.string.player_config_screen_name),
                    descriptionText = stringResource(id = R.string.player_config_screen_description),
                    onClick = { navController.navigate(PlayerConfigRoute) }
                )
            }
            item {
                BaseSettingsItem(
                    icon = painterResource(id = R.drawable.ic_database_24),
                    text = stringResource(id = R.string.data_screen_name),
                    descriptionText = stringResource(id = R.string.data_screen_description),
                    onClick = { navController.navigate(DataRoute) }
                )
            }
            item {
                BaseSettingsItem(
                    icon = rememberVectorPainter(Icons.Outlined.SwapVert),
                    text = stringResource(id = R.string.transmission_screen_name),
                    descriptionText = stringResource(id = R.string.transmission_screen_description),
                    onClick = { navController.navigate(TransmissionRoute) }
                )
            }
        }
    }
}
