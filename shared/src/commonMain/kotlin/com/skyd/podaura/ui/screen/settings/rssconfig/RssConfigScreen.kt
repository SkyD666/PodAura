package com.skyd.podaura.ui.screen.settings.rssconfig

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryFull
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.skyd.compone.component.BackIcon
import com.skyd.compone.component.ComponeTopBar
import com.skyd.compone.component.ComponeTopBarStyle
import com.skyd.compone.component.DefaultBackClick
import com.skyd.compone.component.menu.CheckableListMenu
import com.skyd.compone.local.LocalNavController
import com.skyd.podaura.model.preference.rss.ParseLinkTagAsEnclosurePreference
import com.skyd.podaura.model.preference.rss.RssSyncBatteryNotLowConstraintPreference
import com.skyd.podaura.model.preference.rss.RssSyncChargingConstraintPreference
import com.skyd.podaura.model.preference.rss.RssSyncFrequencyPreference
import com.skyd.podaura.model.preference.rss.RssSyncWifiConstraintPreference
import com.skyd.podaura.ui.screen.settings.rssconfig.updatenotification.UpdateNotificationRoute
import com.skyd.settings.BaseSettingsItem
import com.skyd.settings.SettingsLazyColumn
import com.skyd.settings.SwitchSettingsItem
import com.skyd.settings.suspendString
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.rss_config_screen_name
import podaura.shared.generated.resources.rss_config_screen_notification_category
import podaura.shared.generated.resources.rss_config_screen_parse_category
import podaura.shared.generated.resources.rss_config_screen_parse_link_tag_as_enclosure
import podaura.shared.generated.resources.rss_config_screen_parse_link_tag_as_enclosure_description
import podaura.shared.generated.resources.rss_config_screen_sync_battery_not_low_constraint
import podaura.shared.generated.resources.rss_config_screen_sync_category
import podaura.shared.generated.resources.rss_config_screen_sync_charging_constraint
import podaura.shared.generated.resources.rss_config_screen_sync_frequency
import podaura.shared.generated.resources.rss_config_screen_sync_wifi_constraint
import podaura.shared.generated.resources.rss_config_screen_update_notification_description
import podaura.shared.generated.resources.update_notification_screen_name


@Serializable
data object RssConfigRoute

@Composable
fun RssConfigScreen(
    onBack: (() -> Unit)? = DefaultBackClick,
    windowInsets: WindowInsets = WindowInsets.safeDrawing
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val navController = LocalNavController.current
    val scope = rememberCoroutineScope()
    var expandRssSyncFrequencyMenu by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            ComponeTopBar(
                style = ComponeTopBarStyle.LargeFlexible,
                scrollBehavior = scrollBehavior,
                title = { Text(text = stringResource(Res.string.rss_config_screen_name)) },
                navigationIcon = { if (onBack != null) BackIcon(onClick = onBack) },
                windowInsets = windowInsets.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
            )
        },
        contentWindowInsets = windowInsets
    ) { innerPadding ->
        SettingsLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = innerPadding,
        ) {
            group(text = { getString(Res.string.rss_config_screen_sync_category) }) {
                item {
                    BaseSettingsItem(
                        icon = rememberVectorPainter(image = Icons.Outlined.Timer),
                        text = stringResource(Res.string.rss_config_screen_sync_frequency),
                        descriptionText = suspendString(RssSyncFrequencyPreference.current) {
                            RssSyncFrequencyPreference.toDisplayName(it)
                        },
                        extraContent = {
                            RssSyncFrequencyMenu(
                                expanded = expandRssSyncFrequencyMenu,
                                onDismissRequest = { expandRssSyncFrequencyMenu = false }
                            )
                        },
                        onClick = { expandRssSyncFrequencyMenu = true },
                    )
                }
                item {
                    SwitchSettingsItem(
                        imageVector = Icons.Outlined.Wifi,
                        text = stringResource(Res.string.rss_config_screen_sync_wifi_constraint),
                        checked = RssSyncWifiConstraintPreference.current,
                        onCheckedChange = { RssSyncWifiConstraintPreference.put(scope, it) }
                    )
                }
                item {
                    SwitchSettingsItem(
                        imageVector = Icons.Outlined.Bolt,
                        text = stringResource(Res.string.rss_config_screen_sync_charging_constraint),
                        checked = RssSyncChargingConstraintPreference.current,
                        onCheckedChange = { RssSyncChargingConstraintPreference.put(scope, it) }
                    )
                }
                item {
                    SwitchSettingsItem(
                        imageVector = Icons.Outlined.BatteryFull,
                        text = stringResource(Res.string.rss_config_screen_sync_battery_not_low_constraint),
                        checked = RssSyncBatteryNotLowConstraintPreference.current,
                        onCheckedChange = {
                            RssSyncBatteryNotLowConstraintPreference.put(
                                scope,
                                it
                            )
                        }
                    )
                }
            }
            group(text = { getString(Res.string.rss_config_screen_notification_category) }) {
                item {
                    BaseSettingsItem(
                        icon = rememberVectorPainter(image = Icons.Outlined.Notifications),
                        text = stringResource(Res.string.update_notification_screen_name),
                        descriptionText = stringResource(Res.string.rss_config_screen_update_notification_description),
                        onClick = { navController.navigate(UpdateNotificationRoute) },
                    )
                }
            }
            group(text = { getString(Res.string.rss_config_screen_parse_category) }) {
                item {
                    SwitchSettingsItem(
                        imageVector = Icons.Outlined.Link,
                        text = stringResource(Res.string.rss_config_screen_parse_link_tag_as_enclosure),
                        description = stringResource(Res.string.rss_config_screen_parse_link_tag_as_enclosure_description),
                        checked = ParseLinkTagAsEnclosurePreference.current,
                        onCheckedChange = { ParseLinkTagAsEnclosurePreference.put(scope, it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RssSyncFrequencyMenu(expanded: Boolean, onDismissRequest: () -> Unit) {
    val scope = rememberCoroutineScope()
    CheckableListMenu(
        expanded = expanded,
        current = RssSyncFrequencyPreference.current,
        values = RssSyncFrequencyPreference.frequencies,
        displayName = { RssSyncFrequencyPreference.toDisplayName(it) },
        onChecked = { RssSyncFrequencyPreference.put(scope, it) },
        onDismissRequest = onDismissRequest,
    )
}