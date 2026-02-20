package com.skyd.podaura.ui.screen.settings.playerconfig.advanced

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.rounded.DeveloperBoard
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
import androidx.navigation3.runtime.NavKey
import com.skyd.compone.component.ComponeIconButton
import com.skyd.compone.component.ComponeScaffold
import com.skyd.compone.component.ComponeTopBar
import com.skyd.compone.component.ComponeTopBarStyle
import com.skyd.compone.component.navigation.LocalNavBackStack
import com.skyd.podaura.model.preference.player.HardwareDecodePreference
import com.skyd.podaura.model.preference.player.MpvCacheDirPreference
import com.skyd.podaura.model.preference.player.MpvConfigDirPreference
import com.skyd.podaura.model.preference.player.MpvConfigPreference
import com.skyd.podaura.model.preference.player.MpvInputConfigPreference
import com.skyd.podaura.ui.component.dialog.TextFieldDialog
import com.skyd.podaura.ui.screen.filepicker.FilePickerRoute
import com.skyd.podaura.ui.screen.filepicker.ListenToFilePicker
import com.skyd.settings.BaseSettingsItem
import com.skyd.settings.SettingsLazyColumn
import com.skyd.settings.SwitchSettingsItem
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.player_config_advanced_screen_hardware_decode
import podaura.shared.generated.resources.player_config_advanced_screen_hardware_decode_description
import podaura.shared.generated.resources.player_config_advanced_screen_mpv_cache_dir
import podaura.shared.generated.resources.player_config_advanced_screen_mpv_config
import podaura.shared.generated.resources.player_config_advanced_screen_mpv_config_dir
import podaura.shared.generated.resources.player_config_advanced_screen_mpv_input_config
import podaura.shared.generated.resources.player_config_advanced_screen_name
import podaura.shared.generated.resources.reset


@Serializable
data object PlayerConfigAdvancedRoute : NavKey

@Composable
fun PlayerConfigAdvancedScreen(
    windowInsets: WindowInsets = WindowInsets.safeDrawing
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val navBackStack = LocalNavBackStack.current
    val scope = rememberCoroutineScope()
    var mpvConfEditDialogValue by rememberSaveable { mutableStateOf("") }
    var openMpvConfEditDialog by rememberSaveable { mutableStateOf(false) }
    var mpvInputConfEditDialogValue by rememberSaveable { mutableStateOf("") }
    var openMpvInputConfEditDialog by rememberSaveable { mutableStateOf(false) }

    ListenToFilePicker { result ->
        when (result.id) {
            "configDir" -> MpvConfigDirPreference.put(scope, result.result)
            "cacheDir" -> MpvCacheDirPreference.put(scope, result.result)
            else -> Unit
        }
    }

    ComponeScaffold(
        topBar = {
            ComponeTopBar(
                style = ComponeTopBarStyle.LargeFlexible,
                scrollBehavior = scrollBehavior,
                title = { Text(text = stringResource(Res.string.player_config_advanced_screen_name)) },
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
            group {
                item {
                    SwitchSettingsItem(
                        imageVector = Icons.Rounded.DeveloperBoard,
                        text = stringResource(Res.string.player_config_advanced_screen_hardware_decode),
                        description = stringResource(Res.string.player_config_advanced_screen_hardware_decode_description),
                        checked = HardwareDecodePreference.current,
                        onCheckedChange = { HardwareDecodePreference.put(scope, it) }
                    )
                }
                item {
                    BaseSettingsItem(
                        icon = rememberVectorPainter(Icons.Outlined.PlayCircle),
                        text = stringResource(Res.string.player_config_advanced_screen_mpv_config),
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
                        text = stringResource(Res.string.player_config_advanced_screen_mpv_input_config),
                        descriptionText = null,
                        onClick = {
                            mpvInputConfEditDialogValue = MpvInputConfigPreference.getValue()
                            openMpvInputConfEditDialog = true
                        }
                    )
                }
                item {
                    val configDir = MpvConfigDirPreference.current
                    BaseSettingsItem(
                        icon = null,
                        text = stringResource(Res.string.player_config_advanced_screen_mpv_config_dir),
                        descriptionText = configDir,
                        onClick = {
                            navBackStack.add(FilePickerRoute(path = configDir, id = "configDir"))
                        },
                        content = {
                            ComponeIconButton(
                                onClick = {
                                    MpvConfigDirPreference.put(
                                        scope,
                                        MpvConfigDirPreference.default
                                    )
                                },
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(Res.string.reset),
                            )
                        },
                    )
                }
                item {
                    val cacheDir = MpvCacheDirPreference.current
                    BaseSettingsItem(
                        icon = null,
                        text = stringResource(Res.string.player_config_advanced_screen_mpv_cache_dir),
                        descriptionText = cacheDir,
                        onClick = {
                            navBackStack.add(FilePickerRoute(path = cacheDir, id = "cacheDir"))
                        },
                        content = {
                            ComponeIconButton(
                                onClick = {
                                    MpvCacheDirPreference.put(scope, MpvCacheDirPreference.default)
                                },
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(Res.string.reset),
                            )
                        },
                    )
                }
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
