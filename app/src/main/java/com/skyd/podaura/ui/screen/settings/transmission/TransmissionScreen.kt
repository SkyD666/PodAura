package com.skyd.podaura.ui.screen.settings.transmission

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.VpnKey
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
import com.skyd.podaura.model.preference.transmission.SeedingWhenCompletePreference
import com.skyd.podaura.model.preference.transmission.TorrentDhtBootstrapsPreference
import com.skyd.podaura.model.preference.transmission.TorrentTrackersPreference
import com.skyd.podaura.ui.component.BackIcon
import com.skyd.podaura.ui.component.DefaultBackClick
import com.skyd.podaura.ui.component.PodAuraTopBar
import com.skyd.podaura.ui.component.PodAuraTopBarStyle
import com.skyd.podaura.ui.component.dialog.TextFieldDialog
import com.skyd.podaura.ui.component.settings.BaseSettingsItem
import com.skyd.podaura.ui.component.settings.SettingsLazyColumn
import com.skyd.podaura.ui.component.settings.SwitchSettingsItem
import com.skyd.podaura.ui.local.LocalNavController
import com.skyd.podaura.ui.screen.settings.transmission.proxy.ProxyRoute
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.proxy_screen_description
import podaura.shared.generated.resources.proxy_screen_name
import podaura.shared.generated.resources.transmission_screen_config_category
import podaura.shared.generated.resources.transmission_screen_default_torrent_trackers
import podaura.shared.generated.resources.transmission_screen_default_torrent_trackers_description
import podaura.shared.generated.resources.transmission_screen_edit_torrent_dht_bootstraps_tips
import podaura.shared.generated.resources.transmission_screen_edit_torrent_trackers_tips
import podaura.shared.generated.resources.transmission_screen_name
import podaura.shared.generated.resources.transmission_screen_seeding_when_complete
import podaura.shared.generated.resources.transmission_screen_seeding_when_complete_description
import podaura.shared.generated.resources.transmission_screen_torrent_dht_bootstraps
import podaura.shared.generated.resources.transmission_screen_torrent_dht_bootstraps_default_description
import podaura.shared.generated.resources.transmission_screen_torrent_dht_bootstraps_description
import podaura.shared.generated.resources.transmission_screen_transmission_behavior_category


@Serializable
data object TransmissionRoute

@Composable
fun TransmissionScreen(onBack: (() -> Unit)? = DefaultBackClick) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scope = rememberCoroutineScope()
    val navController = LocalNavController.current

    val torrentTrackers = TorrentTrackersPreference.current
    var torrentTrackersDialogValue by rememberSaveable { mutableStateOf("") }
    var openTorrentTrackersEditDialog by rememberSaveable { mutableStateOf(false) }
    val torrentDhtBootstraps = TorrentDhtBootstrapsPreference.current
    var torrentDhtBootstrapsDialogValue by rememberSaveable { mutableStateOf("") }
    var openTorrentDhtBootstrapsEditDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            PodAuraTopBar(
                style = PodAuraTopBarStyle.LargeFlexible,
                scrollBehavior = scrollBehavior,
                title = { Text(text = stringResource(Res.string.transmission_screen_name)) },
                navigationIcon = { if (onBack != null) BackIcon(onClick = onBack) },
            )
        }
    ) { paddingValues ->
        SettingsLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = paddingValues,
        ) {
            group(
                text = { getString(Res.string.transmission_screen_transmission_behavior_category) },
            ) {
                item {
                    SwitchSettingsItem(
                        imageVector = Icons.Outlined.CloudUpload,
                        text = stringResource(Res.string.transmission_screen_seeding_when_complete),
                        description = stringResource(Res.string.transmission_screen_seeding_when_complete_description),
                        checked = SeedingWhenCompletePreference.current,
                        onCheckedChange = { SeedingWhenCompletePreference.put(scope, it) }
                    )
                }
            }
            group(text = { getString(Res.string.transmission_screen_config_category) }) {
                item {
                    BaseSettingsItem(
                        icon = rememberVectorPainter(Icons.Outlined.Dns),
                        text = stringResource(Res.string.transmission_screen_default_torrent_trackers),
                        descriptionText = pluralStringResource(
                            Res.plurals.transmission_screen_default_torrent_trackers_description,
                            quantity = torrentTrackers.size,
                            torrentTrackers.size,
                        ),
                        onClick = {
                            torrentTrackersDialogValue = torrentTrackers.joinToString("\n")
                            openTorrentTrackersEditDialog = true
                        }
                    )
                }
                item {
                    BaseSettingsItem(
                        icon = rememberVectorPainter(Icons.Outlined.Hub),
                        text = stringResource(Res.string.transmission_screen_torrent_dht_bootstraps),
                        descriptionText = if (torrentDhtBootstraps.isEmpty()) {
                            stringResource(Res.string.transmission_screen_torrent_dht_bootstraps_default_description)
                        } else {
                            pluralStringResource(
                                Res.plurals.transmission_screen_torrent_dht_bootstraps_description,
                                quantity = torrentDhtBootstraps.size,
                                torrentDhtBootstraps.size,
                            )
                        },
                        onClick = {
                            torrentDhtBootstrapsDialogValue =
                                torrentDhtBootstraps.joinToString("\n")
                            openTorrentDhtBootstrapsEditDialog = true
                        }
                    )
                }
                item {
                    BaseSettingsItem(
                        icon = rememberVectorPainter(image = Icons.Outlined.VpnKey),
                        text = stringResource(Res.string.proxy_screen_name),
                        descriptionText = stringResource(Res.string.proxy_screen_description),
                        onClick = { navController.navigate(ProxyRoute) },
                    )
                }
            }
        }
    }

    TextFieldDialog(
        visible = openTorrentTrackersEditDialog,
        value = torrentTrackersDialogValue,
        onValueChange = { torrentTrackersDialogValue = it },
        title = { Text(stringResource(Res.string.transmission_screen_default_torrent_trackers)) },
        placeholder = stringResource(Res.string.transmission_screen_edit_torrent_trackers_tips),
        onConfirm = {
            TorrentTrackersPreference.put(scope = scope, value = it.split("\n").toSet())
            openTorrentTrackersEditDialog = false
        },
        enableConfirm = { true },
        onDismissRequest = { openTorrentTrackersEditDialog = false },
    )
    TextFieldDialog(
        visible = openTorrentDhtBootstrapsEditDialog,
        value = torrentDhtBootstrapsDialogValue,
        onValueChange = { torrentDhtBootstrapsDialogValue = it },
        title = { Text(stringResource(Res.string.transmission_screen_torrent_dht_bootstraps)) },
        placeholder = stringResource(Res.string.transmission_screen_edit_torrent_dht_bootstraps_tips),
        onConfirm = { nodes ->
            TorrentDhtBootstrapsPreference.put(
                scope = scope,
                value = nodes.split("\n").map { it.trimEnd(',') }
                    .map { it.split(",") }.flatten().toSet(),
            )
            openTorrentDhtBootstrapsEditDialog = false
        },
        enableConfirm = { true },
        onDismissRequest = { openTorrentDhtBootstrapsEditDialog = false },
    )
}