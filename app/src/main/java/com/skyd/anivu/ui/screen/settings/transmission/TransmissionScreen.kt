package com.skyd.anivu.ui.screen.settings.transmission

import android.os.Parcelable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.skyd.anivu.R
import com.skyd.anivu.model.preference.transmission.SeedingWhenCompletePreference
import com.skyd.anivu.model.preference.transmission.TorrentDhtBootstrapsPreference
import com.skyd.anivu.model.preference.transmission.TorrentTrackersPreference
import com.skyd.anivu.ui.component.BackIcon
import com.skyd.anivu.ui.component.BaseSettingsItem
import com.skyd.anivu.ui.component.CategorySettingsItem
import com.skyd.anivu.ui.component.DefaultBackClick
import com.skyd.anivu.ui.component.PodAuraTopBar
import com.skyd.anivu.ui.component.PodAuraTopBarStyle
import com.skyd.anivu.ui.component.SwitchSettingsItem
import com.skyd.anivu.ui.component.dialog.TextFieldDialog
import com.skyd.anivu.ui.local.LocalNavController
import com.skyd.anivu.ui.screen.settings.transmission.proxy.ProxyRoute
import com.skyd.generated.preference.LocalSeedingWhenComplete
import com.skyd.generated.preference.LocalTorrentDhtBootstraps
import com.skyd.generated.preference.LocalTorrentTrackers
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable


@Serializable
@Parcelize
data object TransmissionRoute : Parcelable

@Composable
fun TransmissionScreen(onBack: (() -> Unit)? = DefaultBackClick) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val navController = LocalNavController.current

    val torrentTrackers = LocalTorrentTrackers.current
    var torrentTrackersDialogValue by rememberSaveable { mutableStateOf("") }
    var openTorrentTrackersEditDialog by rememberSaveable { mutableStateOf(false) }
    val torrentDhtBootstraps = LocalTorrentDhtBootstraps.current
    var torrentDhtBootstrapsDialogValue by rememberSaveable { mutableStateOf("") }
    var openTorrentDhtBootstrapsEditDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            PodAuraTopBar(
                style = PodAuraTopBarStyle.Large,
                scrollBehavior = scrollBehavior,
                title = { Text(text = stringResource(R.string.transmission_screen_name)) },
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
                CategorySettingsItem(text = stringResource(id = R.string.transmission_screen_transmission_behavior_category))
            }
            item {
                SwitchSettingsItem(
                    imageVector = Icons.Outlined.CloudUpload,
                    text = stringResource(id = R.string.transmission_screen_seeding_when_complete),
                    description = stringResource(id = R.string.transmission_screen_seeding_when_complete_description),
                    checked = LocalSeedingWhenComplete.current,
                    onCheckedChange = {
                        SeedingWhenCompletePreference.put(
                            context = context,
                            scope = scope,
                            value = it,
                        )
                    }
                )
            }
            item {
                CategorySettingsItem(text = stringResource(id = R.string.transmission_screen_config_category))
            }
            item {
                BaseSettingsItem(
                    icon = rememberVectorPainter(Icons.Outlined.Dns),
                    text = stringResource(id = R.string.transmission_screen_default_torrent_trackers),
                    descriptionText = pluralStringResource(
                        id = R.plurals.transmission_screen_default_torrent_trackers_description,
                        count = torrentTrackers.size,
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
                    text = stringResource(id = R.string.transmission_screen_torrent_dht_bootstraps),
                    descriptionText = if (torrentDhtBootstraps.isEmpty()) {
                        stringResource(R.string.transmission_screen_torrent_dht_bootstraps_default_description)
                    } else {
                        pluralStringResource(
                            id = R.plurals.transmission_screen_torrent_dht_bootstraps_description,
                            count = torrentDhtBootstraps.size,
                            torrentDhtBootstraps.size,
                        )
                    },
                    onClick = {
                        torrentDhtBootstrapsDialogValue = torrentDhtBootstraps.joinToString("\n")
                        openTorrentDhtBootstrapsEditDialog = true
                    }
                )
            }
            item {
                BaseSettingsItem(
                    icon = rememberVectorPainter(image = Icons.Outlined.VpnKey),
                    text = stringResource(id = R.string.proxy_screen_name),
                    descriptionText = stringResource(id = R.string.proxy_screen_description),
                    onClick = { navController.navigate(ProxyRoute) },
                )
            }
        }
    }

    TextFieldDialog(
        visible = openTorrentTrackersEditDialog,
        value = torrentTrackersDialogValue,
        onValueChange = { torrentTrackersDialogValue = it },
        title = { Text(stringResource(R.string.transmission_screen_default_torrent_trackers)) },
        placeholder = stringResource(R.string.transmission_screen_edit_torrent_trackers_tips),
        onConfirm = {
            TorrentTrackersPreference.put(
                context = context,
                scope = scope,
                value = it.split("\n").toSet(),
            )
            openTorrentTrackersEditDialog = false
        },
        enableConfirm = { true },
        onDismissRequest = { openTorrentTrackersEditDialog = false },
    )
    TextFieldDialog(
        visible = openTorrentDhtBootstrapsEditDialog,
        value = torrentDhtBootstrapsDialogValue,
        onValueChange = { torrentDhtBootstrapsDialogValue = it },
        title = { Text(stringResource(R.string.transmission_screen_torrent_dht_bootstraps)) },
        placeholder = stringResource(R.string.transmission_screen_edit_torrent_dht_bootstraps_tips),
        onConfirm = { nodes ->
            TorrentDhtBootstrapsPreference.put(
                context = context,
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