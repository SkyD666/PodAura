package com.skyd.anivu.ui.screen.settings.playerconfig

import android.os.Build
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.outlined.FastForward
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PictureInPictureAlt
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Speaker
import androidx.compose.material.icons.outlined.Timelapse
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.skyd.anivu.R
import com.skyd.anivu.ext.fileSize
import com.skyd.anivu.model.preference.player.BackgroundPlayPreference
import com.skyd.anivu.model.preference.player.PlayerAutoPipPreference
import com.skyd.anivu.model.preference.player.PlayerDoubleTapPreference
import com.skyd.anivu.model.preference.player.PlayerMaxBackCacheSizePreference
import com.skyd.anivu.model.preference.player.PlayerMaxCacheSizePreference
import com.skyd.anivu.model.preference.player.PlayerSeekOptionPreference
import com.skyd.anivu.model.preference.player.PlayerShow85sButtonPreference
import com.skyd.anivu.model.preference.player.PlayerShowProgressIndicatorPreference
import com.skyd.anivu.model.preference.player.PlayerShowScreenshotButtonPreference
import com.skyd.anivu.ui.component.BaseSettingsItem
import com.skyd.anivu.ui.component.CategorySettingsItem
import com.skyd.anivu.ui.component.CheckableListMenu
import com.skyd.anivu.ui.component.PodAuraIconButton
import com.skyd.anivu.ui.component.PodAuraTopBar
import com.skyd.anivu.ui.component.PodAuraTopBarStyle
import com.skyd.anivu.ui.component.SwitchSettingsItem
import com.skyd.anivu.ui.component.dialog.SliderDialog
import com.skyd.anivu.ui.local.LocalBackgroundPlay
import com.skyd.anivu.ui.local.LocalNavController
import com.skyd.anivu.ui.local.LocalPlayerAutoPip
import com.skyd.anivu.ui.local.LocalPlayerDoubleTap
import com.skyd.anivu.ui.local.LocalPlayerMaxBackCacheSize
import com.skyd.anivu.ui.local.LocalPlayerMaxCacheSize
import com.skyd.anivu.ui.local.LocalPlayerSeekOption
import com.skyd.anivu.ui.local.LocalPlayerShow85sButton
import com.skyd.anivu.ui.local.LocalPlayerShowProgressIndicator
import com.skyd.anivu.ui.local.LocalPlayerShowScreenshotButton
import com.skyd.anivu.ui.screen.settings.playerconfig.advanced.PLAYER_CONFIG_ADVANCED_SCREEN_ROUTE


const val PLAYER_CONFIG_SCREEN_ROUTE = "playerConfigScreen"

@Composable
fun PlayerConfigScreen() {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val navController = LocalNavController.current
    var expandDoubleTapMenu by rememberSaveable { mutableStateOf(false) }
    var expandSeekOptionMenu by rememberSaveable { mutableStateOf(false) }
    var openMaxCacheSizeDialog by rememberSaveable { mutableStateOf(false) }
    var openMaxBackCacheSizeDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            PodAuraTopBar(
                style = PodAuraTopBarStyle.Large,
                scrollBehavior = scrollBehavior,
                title = { Text(text = stringResource(R.string.player_config_screen_name)) },
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
                CategorySettingsItem(text = stringResource(id = R.string.player_config_screen_behavior_category))
            }
            item {
                SwitchSettingsItem(
                    imageVector = Icons.Outlined.Speaker,
                    text = stringResource(id = R.string.player_config_screen_background_play),
                    description = stringResource(id = R.string.player_config_screen_background_play_description),
                    checked = LocalBackgroundPlay.current,
                    onCheckedChange = {
                        BackgroundPlayPreference.put(
                            context = context,
                            scope = scope,
                            value = it,
                        )
                    }
                )
            }
            item {
                BaseSettingsItem(
                    icon = rememberVectorPainter(image = Icons.Outlined.TouchApp),
                    text = stringResource(id = R.string.player_config_screen_double_tap),
                    descriptionText = PlayerDoubleTapPreference.toDisplayName(
                        context,
                        LocalPlayerDoubleTap.current,
                    ),
                    extraContent = {
                        DoubleTapMenu(
                            expanded = expandDoubleTapMenu,
                            onDismissRequest = { expandDoubleTapMenu = false }
                        )
                    },
                    onClick = { expandDoubleTapMenu = true },
                )
            }
            item {
                SwitchSettingsItem(
                    imageVector = Icons.Outlined.PictureInPictureAlt,
                    text = stringResource(id = R.string.player_config_screen_auto_pip),
                    description = stringResource(id = R.string.player_config_screen_auto_pip_description),
                    checked = LocalPlayerAutoPip.current,
                    onCheckedChange = {
                        PlayerAutoPipPreference.put(
                            context = context,
                            scope = scope,
                            value = it,
                        )
                    }
                )
            }
            item {
                BaseSettingsItem(
                    icon = rememberVectorPainter(Icons.AutoMirrored.Outlined.Redo),
                    text = stringResource(id = R.string.player_config_screen_seek_option),
                    descriptionText = PlayerSeekOptionPreference.toDisplayName(
                        context = context,
                        value = LocalPlayerSeekOption.current,
                    ),
                    extraContent = {
                        SeekOptionMenu(
                            expanded = expandSeekOptionMenu,
                            onDismissRequest = { expandSeekOptionMenu = false },
                        )
                    },
                    onClick = { expandSeekOptionMenu = true },
                )
            }
            item {
                CategorySettingsItem(text = stringResource(id = R.string.player_config_screen_appearance_category))
            }
            item {
                SwitchSettingsItem(
                    imageVector = Icons.Outlined.PhotoCamera,
                    text = stringResource(id = R.string.player_config_screen_show_screenshot_button),
                    description = stringResource(id = R.string.player_config_screen_show_screenshot_button_description),
                    checked = LocalPlayerShowScreenshotButton.current,
                    onCheckedChange = {
                        PlayerShowScreenshotButtonPreference.put(
                            context = context,
                            scope = scope,
                            value = it,
                        )
                    }
                )
            }
            item {
                SwitchSettingsItem(
                    imageVector = Icons.Outlined.Timelapse,
                    text = stringResource(id = R.string.player_config_screen_show_progress_indicator),
                    description = stringResource(id = R.string.player_config_screen_show_progress_indicator_description),
                    checked = LocalPlayerShowProgressIndicator.current,
                    onCheckedChange = {
                        PlayerShowProgressIndicatorPreference.put(
                            context = context,
                            scope = scope,
                            value = it,
                        )
                    }
                )
            }
            item {
                SwitchSettingsItem(
                    imageVector = Icons.Outlined.FastForward,
                    text = stringResource(id = R.string.player_config_screen_show_85s_button),
                    description = stringResource(id = R.string.player_config_screen_show_85s_button_description),
                    checked = LocalPlayerShow85sButton.current,
                    onCheckedChange = {
                        PlayerShow85sButtonPreference.put(
                            context = context,
                            scope = scope,
                            value = it,
                        )
                    }
                )
            }
            item {
                CategorySettingsItem(text = stringResource(id = R.string.player_config_screen_cache_category))
            }
            item {
                BaseSettingsItem(
                    icon = rememberVectorPainter(Icons.AutoMirrored.Outlined.KeyboardArrowRight),
                    text = stringResource(id = R.string.player_config_screen_max_cache_size),
                    descriptionText = LocalPlayerMaxCacheSize.current.fileSize(context),
                    onClick = { openMaxCacheSizeDialog = true }
                )
            }
            item {
                BaseSettingsItem(
                    icon = rememberVectorPainter(Icons.AutoMirrored.Outlined.KeyboardArrowLeft),
                    text = stringResource(id = R.string.player_config_screen_max_back_cache_size),
                    descriptionText = LocalPlayerMaxBackCacheSize.current.fileSize(context),
                    onClick = { openMaxBackCacheSizeDialog = true }
                )
            }
            item {
                CategorySettingsItem(text = stringResource(id = R.string.player_config_screen_advanced_category))
            }
            item {
                BaseSettingsItem(
                    icon = null,
                    text = stringResource(id = R.string.player_config_advanced_screen_name),
                    descriptionText = null,
                    onClick = { navController.navigate(PLAYER_CONFIG_ADVANCED_SCREEN_ROUTE) },
                )
            }
        }

        if (openMaxCacheSizeDialog) {
            MaxCacheSizeDialog(
                onDismissRequest = { openMaxCacheSizeDialog = false },
                title = stringResource(id = R.string.player_config_screen_max_cache_size),
                initValue = LocalPlayerMaxCacheSize.current,
                defaultValue = { PlayerMaxCacheSizePreference.default },
                onConfirm = {
                    PlayerMaxCacheSizePreference.put(
                        context = context,
                        scope = scope,
                        value = it,
                    )
                    openMaxCacheSizeDialog = false
                }
            )
        }
        if (openMaxBackCacheSizeDialog) {
            MaxCacheSizeDialog(
                onDismissRequest = { openMaxBackCacheSizeDialog = false },
                title = stringResource(id = R.string.player_config_screen_max_back_cache_size),
                initValue = LocalPlayerMaxBackCacheSize.current,
                defaultValue = { PlayerMaxBackCacheSizePreference.default },
                onConfirm = {
                    PlayerMaxBackCacheSizePreference.put(
                        context = context,
                        scope = scope,
                        value = it,
                    )
                    openMaxBackCacheSizeDialog = false
                }
            )
        }
    }
}

@Composable
private fun DoubleTapMenu(expanded: Boolean, onDismissRequest: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val playerDoubleTap = LocalPlayerDoubleTap.current

    CheckableListMenu(
        expanded = expanded,
        current = playerDoubleTap,
        values = remember { PlayerDoubleTapPreference.values.toList() },
        displayName = { PlayerDoubleTapPreference.toDisplayName(context, it) },
        onChecked = { PlayerDoubleTapPreference.put(context, scope, it) },
        onDismissRequest = onDismissRequest,
    )
}

@Composable
private fun SeekOptionMenu(expanded: Boolean, onDismissRequest: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val playerSeekOption = LocalPlayerSeekOption.current

    CheckableListMenu(
        expanded = expanded,
        current = playerSeekOption,
        values = remember { PlayerSeekOptionPreference.values.toList() },
        displayName = { PlayerSeekOptionPreference.toDisplayName(context, it) },
        onChecked = { PlayerSeekOptionPreference.put(context, scope, it) },
        onDismissRequest = onDismissRequest,
    )
}

@Composable
internal fun MaxCacheSizeDialog(
    onDismissRequest: () -> Unit,
    title: String,
    initValue: Long,
    defaultValue: () -> Long,
    onConfirm: (Long) -> Unit,
) {
    var value by rememberSaveable { mutableLongStateOf(initValue) }
    val context = LocalContext.current

    // 64 MB / 32 MB
    val maxSize = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) 64f else 32f

    SliderDialog(
        onDismissRequest = onDismissRequest,
        value = value / 1048576f,
        onValueChange = { value = it.toLong() * 1048576 },
        valueRange = 1f..maxSize,
        valueLabel = {
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .animateContentSize(),
                    text = value.fileSize(context),
                    style = MaterialTheme.typography.titleMedium,
                )
                PodAuraIconButton(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    onClick = { value = defaultValue() },
                    imageVector = Icons.Outlined.Restore,
                    contentDescription = stringResource(R.string.reset),
                )
            }
        },
        icon = { Icon(imageVector = Icons.Outlined.Save, contentDescription = null) },
        title = { Text(text = title) },
        confirmButton = {
            TextButton(onClick = { onConfirm(value) }) {
                Text(text = stringResource(id = R.string.ok))
            }
        }
    )
}