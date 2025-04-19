package com.skyd.anivu.ui.screen.settings.rssconfig

import android.os.Parcelable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.skyd.anivu.R
import com.skyd.anivu.model.preference.rss.ParseLinkTagAsEnclosurePreference
import com.skyd.anivu.model.preference.rss.RssSyncBatteryNotLowConstraintPreference
import com.skyd.anivu.model.preference.rss.RssSyncChargingConstraintPreference
import com.skyd.anivu.model.preference.rss.RssSyncFrequencyPreference
import com.skyd.anivu.model.preference.rss.RssSyncWifiConstraintPreference
import com.skyd.anivu.ui.component.BackIcon
import com.skyd.anivu.ui.component.BaseSettingsItem
import com.skyd.anivu.ui.component.CategorySettingsItem
import com.skyd.anivu.ui.component.CheckableListMenu
import com.skyd.anivu.ui.component.DefaultBackClick
import com.skyd.anivu.ui.component.PodAuraTopBar
import com.skyd.anivu.ui.component.PodAuraTopBarStyle
import com.skyd.anivu.ui.component.SwitchSettingsItem
import com.skyd.anivu.ui.local.LocalNavController
import com.skyd.anivu.ui.screen.settings.rssconfig.updatenotification.UpdateNotificationRoute
import com.skyd.generated.preference.LocalParseLinkTagAsEnclosure
import com.skyd.generated.preference.LocalRssSyncBatteryNotLowConstraint
import com.skyd.generated.preference.LocalRssSyncChargingConstraint
import com.skyd.generated.preference.LocalRssSyncFrequency
import com.skyd.generated.preference.LocalRssSyncWifiConstraint
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable


@Serializable
@Parcelize
data object RssConfigRoute : Parcelable {
    @Composable
    fun RssConfigLauncher(onBack: (() -> Unit)? = DefaultBackClick) {
        RssConfigScreen(onBack = onBack)
    }
}

@Composable
fun RssConfigScreen(onBack: (() -> Unit)? = DefaultBackClick) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val context = LocalContext.current
    val navController = LocalNavController.current
    val scope = rememberCoroutineScope()
    var expandRssSyncFrequencyMenu by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            PodAuraTopBar(
                style = PodAuraTopBarStyle.Large,
                scrollBehavior = scrollBehavior,
                title = { Text(text = stringResource(R.string.rss_config_screen_name)) },
                navigationIcon = { if (onBack != null) BackIcon(onClick = onBack) },
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
                CategorySettingsItem(text = stringResource(id = R.string.rss_config_screen_sync_category))
            }
            item {
                BaseSettingsItem(
                    icon = rememberVectorPainter(image = Icons.Outlined.Timer),
                    text = stringResource(id = R.string.rss_config_screen_sync_frequency),
                    descriptionText = RssSyncFrequencyPreference.toDisplayName(
                        context, LocalRssSyncFrequency.current,
                    ),
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
                    text = stringResource(id = R.string.rss_config_screen_sync_wifi_constraint),
                    checked = LocalRssSyncWifiConstraint.current,
                    onCheckedChange = {
                        RssSyncWifiConstraintPreference.put(
                            context = context,
                            scope = scope,
                            value = it,
                        )
                    }
                )
            }
            item {
                SwitchSettingsItem(
                    imageVector = Icons.Outlined.Bolt,
                    text = stringResource(id = R.string.rss_config_screen_sync_charging_constraint),
                    checked = LocalRssSyncChargingConstraint.current,
                    onCheckedChange = {
                        RssSyncChargingConstraintPreference.put(
                            context = context,
                            scope = scope,
                            value = it,
                        )
                    }
                )
            }
            item {
                SwitchSettingsItem(
                    imageVector = Icons.Outlined.BatteryFull,
                    text = stringResource(id = R.string.rss_config_screen_sync_battery_not_low_constraint),
                    checked = LocalRssSyncBatteryNotLowConstraint.current,
                    onCheckedChange = {
                        RssSyncBatteryNotLowConstraintPreference.put(
                            context = context,
                            scope = scope,
                            value = it,
                        )
                    }
                )
            }
            item {
                CategorySettingsItem(text = stringResource(id = R.string.rss_config_screen_notification_category))
            }
            item {
                BaseSettingsItem(
                    icon = rememberVectorPainter(image = Icons.Outlined.Notifications),
                    text = stringResource(id = R.string.update_notification_screen_name),
                    descriptionText = stringResource(id = R.string.rss_config_screen_update_notification_description),
                    onClick = { navController.navigate(UpdateNotificationRoute) },
                )
            }
            item {
                CategorySettingsItem(text = stringResource(id = R.string.rss_config_screen_parse_category))
            }
            item {
                SwitchSettingsItem(
                    imageVector = Icons.Outlined.Link,
                    text = stringResource(id = R.string.rss_config_screen_parse_link_tag_as_enclosure),
                    description = stringResource(id = R.string.rss_config_screen_parse_link_tag_as_enclosure_description),
                    checked = LocalParseLinkTagAsEnclosure.current,
                    onCheckedChange = {
                        ParseLinkTagAsEnclosurePreference.put(
                            context = context,
                            scope = scope,
                            value = it,
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun RssSyncFrequencyMenu(expanded: Boolean, onDismissRequest: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val rssSyncFrequency = LocalRssSyncFrequency.current

    CheckableListMenu(
        expanded = expanded,
        current = rssSyncFrequency,
        values = RssSyncFrequencyPreference.frequencies,
        displayName = { RssSyncFrequencyPreference.toDisplayName(context, it) },
        onChecked = { RssSyncFrequencyPreference.put(context, scope, it) },
        onDismissRequest = onDismissRequest,
    )
}