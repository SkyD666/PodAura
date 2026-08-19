package com.skyd.podaura.ui.screen.settings.playerconfig.advanced

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.rounded.DeveloperBoard
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.Role
import androidx.navigation3.runtime.NavKey
import com.skyd.compone.component.ComponeIconButton
import com.skyd.compone.component.ComponeScaffold
import com.skyd.compone.component.ComponeTopBar
import com.skyd.compone.component.ComponeTopBarStyle
import com.skyd.compone.component.dialog.ComponeDialog
import com.skyd.podaura.model.preference.player.HardwareDecodePreference
import com.skyd.podaura.model.preference.player.MpvCacheLocation
import com.skyd.podaura.model.preference.player.MpvCacheLocationKind
import com.skyd.podaura.model.preference.player.MpvCacheSelectionMode
import com.skyd.podaura.model.preference.player.MpvCacheDirPreference
import com.skyd.podaura.model.preference.player.MpvConfigDirPreference
import com.skyd.podaura.model.preference.player.availableMpvCacheLocations
import com.skyd.podaura.model.preference.player.mpvCacheSelectionMode
import com.skyd.podaura.model.preference.player.mpvDirectoryDisplayName
import com.skyd.podaura.model.preference.player.readMpvConfigFile
import com.skyd.podaura.model.preference.player.resetMpvCacheDirectory
import com.skyd.podaura.model.preference.player.resetMpvConfigDirectory
import com.skyd.podaura.model.preference.player.selectMpvCacheDirectory
import com.skyd.podaura.model.preference.player.selectMpvCacheLocation
import com.skyd.podaura.model.preference.player.selectMpvConfigDirectory
import com.skyd.podaura.model.preference.player.syncMpvConfigDirectory
import com.skyd.podaura.model.preference.player.writeMpvConfigFile
import com.skyd.podaura.ui.component.dialog.TextFieldDialog
import com.skyd.settings.BaseSettingsItem
import com.skyd.settings.SettingsLazyColumn
import com.skyd.settings.SwitchSettingsItem
import io.github.vinceglb.filekit.dialogs.compose.rememberDirectoryPickerLauncher
import kotlinx.coroutines.launch
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
import podaura.shared.generated.resources.mpv_cache_external_storage
import podaura.shared.generated.resources.mpv_cache_external_storage_named
import podaura.shared.generated.resources.mpv_cache_internal_storage
import podaura.shared.generated.resources.mpv_config_sync
import podaura.shared.generated.resources.mpv_storage_error
import podaura.shared.generated.resources.item_selected
import podaura.shared.generated.resources.reset


@Serializable
data object PlayerConfigAdvancedRoute : NavKey

@Composable
fun PlayerConfigAdvancedScreen(
    windowInsets: WindowInsets = WindowInsets.safeDrawing
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val storageError = stringResource(Res.string.mpv_storage_error)
    var mpvConfEditDialogValue by rememberSaveable { mutableStateOf("") }
    var openMpvConfEditDialog by rememberSaveable { mutableStateOf(false) }
    var mpvInputConfEditDialogValue by rememberSaveable { mutableStateOf("") }
    var openMpvInputConfEditDialog by rememberSaveable { mutableStateOf(false) }
    var openCacheLocationDialog by rememberSaveable { mutableStateOf(false) }
    val configDir = MpvConfigDirPreference.current
    val cacheDir = MpvCacheDirPreference.current
    val cacheLocations = availableMpvCacheLocations()

    val configDirectoryPicker = rememberDirectoryPickerLauncher(
        onError = { scope.launch { snackbarHostState.showSnackbar(storageError) } },
        onResult = { directory ->
            directory ?: return@rememberDirectoryPickerLauncher
            scope.launch {
                runCatching { selectMpvConfigDirectory(directory) }
                    .onFailure { snackbarHostState.showSnackbar(storageError) }
            }
        },
    )
    val cacheDirectoryPicker = rememberDirectoryPickerLauncher(
        onError = { scope.launch { snackbarHostState.showSnackbar(storageError) } },
        onResult = { directory ->
            directory ?: return@rememberDirectoryPickerLauncher
            scope.launch {
                runCatching { selectMpvCacheDirectory(directory) }
                    .onFailure { snackbarHostState.showSnackbar(storageError) }
            }
        },
    )

    ComponeScaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                            scope.launch {
                                runCatching { readMpvConfigFile("mpv.conf") }
                                    .onSuccess {
                                        mpvConfEditDialogValue = it
                                        openMpvConfEditDialog = true
                                    }
                                    .onFailure { snackbarHostState.showSnackbar(storageError) }
                            }
                        }
                    )
                }
                item {
                    BaseSettingsItem(
                        icon = rememberVectorPainter(Icons.Outlined.Keyboard),
                        text = stringResource(Res.string.player_config_advanced_screen_mpv_input_config),
                        descriptionText = null,
                        onClick = {
                            scope.launch {
                                runCatching { readMpvConfigFile("input.conf") }
                                    .onSuccess {
                                        mpvInputConfEditDialogValue = it
                                        openMpvInputConfEditDialog = true
                                    }
                                    .onFailure { snackbarHostState.showSnackbar(storageError) }
                            }
                        }
                    )
                }
                item {
                    BaseSettingsItem(
                        icon = null,
                        text = stringResource(Res.string.player_config_advanced_screen_mpv_config_dir),
                        descriptionText = mpvDirectoryDisplayName(configDir),
                        enabled = MpvConfigDirPreference.key != null,
                        onClick = configDirectoryPicker::launch,
                        content = {
                            Row {
                                ComponeIconButton(
                                    onClick = {
                                        scope.launch {
                                            runCatching { syncMpvConfigDirectory() }
                                                .onFailure {
                                                    snackbarHostState.showSnackbar(storageError)
                                                }
                                        }
                                    },
                                    imageVector = Icons.Outlined.Sync,
                                    contentDescription = stringResource(Res.string.mpv_config_sync),
                                )
                                ComponeIconButton(
                                    onClick = {
                                        scope.launch {
                                            runCatching { resetMpvConfigDirectory() }
                                                .onFailure {
                                                    snackbarHostState.showSnackbar(storageError)
                                                }
                                        }
                                    },
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = stringResource(Res.string.reset),
                                )
                            }
                        },
                    )
                }
                item {
                    BaseSettingsItem(
                        icon = null,
                        text = stringResource(Res.string.player_config_advanced_screen_mpv_cache_dir),
                        descriptionText = cacheLocations.firstOrNull { it.path == cacheDir }
                            ?.let { mpvCacheLocationName(it) }
                            ?: mpvDirectoryDisplayName(cacheDir),
                        enabled = MpvCacheDirPreference.key != null &&
                                mpvCacheSelectionMode != MpvCacheSelectionMode.Unsupported,
                        onClick = {
                            when (mpvCacheSelectionMode) {
                                MpvCacheSelectionMode.DirectoryPicker -> cacheDirectoryPicker.launch()
                                MpvCacheSelectionMode.ManagedLocations ->
                                    openCacheLocationDialog = true

                                MpvCacheSelectionMode.Unsupported -> Unit
                            }
                        },
                        content = {
                            ComponeIconButton(
                                onClick = {
                                    scope.launch {
                                        runCatching { resetMpvCacheDirectory() }
                                            .onFailure {
                                                snackbarHostState.showSnackbar(storageError)
                                            }
                                    }
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
                scope.launch {
                    runCatching { writeMpvConfigFile("mpv.conf", it) }
                        .onSuccess { openMpvConfEditDialog = false }
                        .onFailure { snackbarHostState.showSnackbar(storageError) }
                }
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
                scope.launch {
                    runCatching { writeMpvConfigFile("input.conf", it) }
                        .onSuccess { openMpvInputConfEditDialog = false }
                        .onFailure { snackbarHostState.showSnackbar(storageError) }
                }
            },
            enableConfirm = { true },
            onDismissRequest = { openMpvInputConfEditDialog = false },
        )

        MpvCacheLocationDialog(
            visible = openCacheLocationDialog,
            locations = cacheLocations,
            selectedPath = cacheDir,
            onSelect = { location ->
                openCacheLocationDialog = false
                scope.launch {
                    runCatching { selectMpvCacheLocation(location) }
                        .onFailure { snackbarHostState.showSnackbar(storageError) }
                }
            },
            onDismissRequest = { openCacheLocationDialog = false },
        )
    }
}

@Composable
private fun MpvCacheLocationDialog(
    visible: Boolean,
    locations: List<MpvCacheLocation>,
    selectedPath: String,
    onSelect: (MpvCacheLocation) -> Unit,
    onDismissRequest: () -> Unit,
) {
    ComponeDialog(
        visible = visible,
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(Res.string.player_config_advanced_screen_mpv_cache_dir)) },
        text = {
            Column {
                locations.forEach { location ->
                    val selected = location.path == selectedPath
                    ListItem(
                        modifier = Modifier.selectable(
                            selected = selected,
                            onClick = { onSelect(location) },
                            role = Role.RadioButton,
                        ),
                        headlineContent = { Text(mpvCacheLocationName(location)) },
                        trailingContent = {
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Outlined.Done,
                                    contentDescription = stringResource(Res.string.item_selected),
                                )
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )
                }
            }
        },
        selectable = false,
        scrollable = false,
        confirmButton = {},
    )
}

@Composable
private fun mpvCacheLocationName(location: MpvCacheLocation): String = when (location.kind) {
    MpvCacheLocationKind.Internal -> stringResource(Res.string.mpv_cache_internal_storage)
    MpvCacheLocationKind.External -> location.volumeName?.let {
        stringResource(Res.string.mpv_cache_external_storage_named, it)
    } ?: stringResource(Res.string.mpv_cache_external_storage)
}
