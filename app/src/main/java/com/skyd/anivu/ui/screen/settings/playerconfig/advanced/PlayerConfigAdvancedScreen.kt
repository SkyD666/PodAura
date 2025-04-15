package com.skyd.anivu.ui.screen.settings.playerconfig.advanced

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.rounded.DeveloperBoard
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
import com.skyd.anivu.model.preference.player.HardwareDecodePreference
import com.skyd.anivu.model.preference.player.MpvCacheDirPreference
import com.skyd.anivu.model.preference.player.MpvConfigDirPreference
import com.skyd.anivu.model.preference.player.MpvConfigPreference
import com.skyd.anivu.model.preference.player.MpvInputConfigPreference
import com.skyd.anivu.ui.component.BaseSettingsItem
import com.skyd.anivu.ui.component.PodAuraIconButton
import com.skyd.anivu.ui.component.PodAuraTopBar
import com.skyd.anivu.ui.component.PodAuraTopBarStyle
import com.skyd.anivu.ui.component.SwitchSettingsItem
import com.skyd.anivu.ui.component.dialog.TextFieldDialog
import com.skyd.anivu.ui.local.LocalNavController
import com.skyd.anivu.ui.screen.filepicker.FilePickerRoute
import com.skyd.anivu.ui.screen.filepicker.ListenToFilePicker
import com.skyd.generated.preference.LocalHardwareDecode
import com.skyd.generated.preference.LocalMpvCacheDir
import com.skyd.generated.preference.LocalMpvConfigDir
import kotlinx.serialization.Serializable


@Serializable
data object PlayerConfigAdvancedRoute

@Composable
fun PlayerConfigAdvancedScreen() {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val context = LocalContext.current
    val navController = LocalNavController.current
    val scope = rememberCoroutineScope()
    var mpvConfEditDialogValue by rememberSaveable { mutableStateOf("") }
    var openMpvConfEditDialog by rememberSaveable { mutableStateOf(false) }
    var mpvInputConfEditDialogValue by rememberSaveable { mutableStateOf("") }
    var openMpvInputConfEditDialog by rememberSaveable { mutableStateOf(false) }

    ListenToFilePicker { result ->
        when (result.id) {
            "configDir" -> MpvConfigDirPreference.put(context, scope, result.result)
            "cacheDir" -> MpvCacheDirPreference.put(context, scope, result.result)
            else -> Unit
        }
    }

    Scaffold(
        topBar = {
            PodAuraTopBar(
                style = PodAuraTopBarStyle.Large,
                scrollBehavior = scrollBehavior,
                title = { Text(text = stringResource(R.string.player_config_advanced_screen_name)) },
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
                SwitchSettingsItem(
                    imageVector = Icons.Rounded.DeveloperBoard,
                    text = stringResource(id = R.string.player_config_advanced_screen_hardware_decode),
                    description = stringResource(id = R.string.player_config_advanced_screen_hardware_decode_description),
                    checked = LocalHardwareDecode.current,
                    onCheckedChange = {
                        HardwareDecodePreference.put(
                            context = context,
                            scope = scope,
                            value = it,
                        )
                    }
                )
            }
            item {
                BaseSettingsItem(
                    icon = rememberVectorPainter(Icons.Outlined.PlayCircle),
                    text = stringResource(id = R.string.player_config_advanced_screen_mpv_config),
                    descriptionText = null,
                    onClick = {
                        mpvConfEditDialogValue = MpvConfigPreference.getValue()
                        openMpvConfEditDialog = true
                    }
                )
            }
            item {
                BaseSettingsItem(
                    icon = rememberVectorPainter(Icons.Outlined.Keyboard),
                    text = stringResource(id = R.string.player_config_advanced_screen_mpv_input_config),
                    descriptionText = null,
                    onClick = {
                        mpvInputConfEditDialogValue = MpvInputConfigPreference.getValue()
                        openMpvInputConfEditDialog = true
                    }
                )
            }
            item {
                val configDir = LocalMpvConfigDir.current
                BaseSettingsItem(
                    icon = null,
                    text = stringResource(id = R.string.player_config_advanced_screen_mpv_config_dir),
                    descriptionText = configDir,
                    onClick = {
                        navController.navigate(FilePickerRoute(path = configDir, id = "configDir"))
                    },
                    content = {
                        PodAuraIconButton(
                            onClick = {
                                MpvConfigDirPreference.put(
                                    context, scope, MpvConfigDirPreference.default,
                                )
                            },
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.reset),
                        )
                    },
                )
            }
            item {
                val cacheDir = LocalMpvCacheDir.current
                BaseSettingsItem(
                    icon = null,
                    text = stringResource(id = R.string.player_config_advanced_screen_mpv_cache_dir),
                    descriptionText = cacheDir,
                    onClick = {
                        navController.navigate(FilePickerRoute(path = cacheDir, id = "cacheDir"))
                    },
                    content = {
                        PodAuraIconButton(
                            onClick = {
                                MpvCacheDirPreference.put(
                                    context, scope, MpvCacheDirPreference.default,
                                )
                            },
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.reset),
                        )
                    },
                )
            }
        }

        TextFieldDialog(
            visible = openMpvConfEditDialog,
            value = mpvConfEditDialogValue,
            onValueChange = { mpvConfEditDialogValue = it },
            title = null,
            onConfirm = {
                MpvConfigPreference.put(
                    scope = scope,
                    value = it,
                )
                openMpvConfEditDialog = false
            },
            enableConfirm = { true },
            onDismissRequest = { openMpvConfEditDialog = false },
        )

        TextFieldDialog(
            visible = openMpvInputConfEditDialog,
            value = mpvInputConfEditDialogValue,
            onValueChange = { mpvInputConfEditDialogValue = it },
            title = null,
            onConfirm = {
                MpvInputConfigPreference.put(
                    scope = scope,
                    value = it,
                )
                openMpvInputConfEditDialog = false
            },
            enableConfirm = { true },
            onDismissRequest = { openMpvInputConfEditDialog = false },
        )
    }
}
