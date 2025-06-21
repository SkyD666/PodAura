package com.skyd.podaura.ui.screen.settings.appearance.feed

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Pin
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Tonality
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.skyd.podaura.model.preference.appearance.feed.FeedListTonalElevationPreference
import com.skyd.podaura.model.preference.appearance.feed.FeedNumberBadgePreference
import com.skyd.podaura.model.preference.appearance.feed.FeedTopBarTonalElevationPreference
import com.skyd.podaura.model.preference.appearance.feed.TonalElevationPreferenceUtil
import com.skyd.podaura.ui.component.CheckableListMenu
import com.skyd.podaura.ui.component.PodAuraIconButton
import com.skyd.podaura.ui.component.PodAuraTopBar
import com.skyd.podaura.ui.component.PodAuraTopBarStyle
import com.skyd.podaura.ui.component.dialog.SliderDialog
import com.skyd.podaura.ui.component.suspendString
import com.skyd.settings.BaseSettingsItem
import com.skyd.settings.SettingsLazyColumn
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.feed_style_screen_group_list_category
import podaura.shared.generated.resources.feed_style_screen_name
import podaura.shared.generated.resources.feed_style_screen_number_badge
import podaura.shared.generated.resources.feed_style_screen_top_bar_category
import podaura.shared.generated.resources.ok
import podaura.shared.generated.resources.reset
import podaura.shared.generated.resources.tonal_elevation


@Serializable
data object FeedStyleRoute

@Composable
fun FeedStyleScreen() {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            PodAuraTopBar(
                style = PodAuraTopBarStyle.LargeFlexible,
                scrollBehavior = scrollBehavior,
                title = { Text(text = stringResource(Res.string.feed_style_screen_name)) },
            )
        }
    ) { paddingValues ->
        var openTopBarTonalElevationDialog by rememberSaveable { mutableStateOf(false) }
        var openGroupListTonalElevationDialog by rememberSaveable { mutableStateOf(false) }
        var expandFeedNumberBadgeMenu by rememberSaveable { mutableStateOf(false) }

        SettingsLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = paddingValues,
        ) {
            group(text = { getString(Res.string.feed_style_screen_top_bar_category) }) {
                item {
                    BaseSettingsItem(
                        icon = rememberVectorPainter(Icons.Outlined.Tonality),
                        text = stringResource(Res.string.tonal_elevation),
                        descriptionText = TonalElevationPreferenceUtil.toDisplay(
                            FeedTopBarTonalElevationPreference.current
                        ),
                        onClick = { openTopBarTonalElevationDialog = true }
                    )
                }
            }
            group(text = { getString(Res.string.feed_style_screen_group_list_category) }) {
                item {
                    BaseSettingsItem(
                        icon = rememberVectorPainter(Icons.Outlined.Tonality),
                        text = stringResource(Res.string.tonal_elevation),
                        descriptionText = TonalElevationPreferenceUtil.toDisplay(
                            FeedListTonalElevationPreference.current
                        ),
                        onClick = { openGroupListTonalElevationDialog = true }
                    )
                }
                item {
                    BaseSettingsItem(
                        icon = rememberVectorPainter(Icons.Outlined.Pin),
                        text = stringResource(Res.string.feed_style_screen_number_badge),
                        descriptionText = suspendString(FeedNumberBadgePreference.current) {
                            FeedNumberBadgePreference.toDisplayName(it)
                        },
                        extraContent = {
                            FeedNumberBadgeMenu(
                                expanded = expandFeedNumberBadgeMenu,
                                onDismissRequest = { expandFeedNumberBadgeMenu = false },
                            )
                        },
                        onClick = { expandFeedNumberBadgeMenu = true }
                    )
                }
            }
        }

        if (openTopBarTonalElevationDialog) {
            TonalElevationDialog(
                onDismissRequest = { openTopBarTonalElevationDialog = false },
                initValue = FeedTopBarTonalElevationPreference.current,
                defaultValue = { FeedTopBarTonalElevationPreference.default },
                onConfirm = {
                    FeedTopBarTonalElevationPreference.put(scope, it)
                    openTopBarTonalElevationDialog = false
                }
            )
        }
        if (openGroupListTonalElevationDialog) {
            TonalElevationDialog(
                onDismissRequest = { openGroupListTonalElevationDialog = false },
                initValue = FeedListTonalElevationPreference.current,
                defaultValue = { FeedListTonalElevationPreference.default },
                onConfirm = {
                    FeedListTonalElevationPreference.put(scope, it)
                    openGroupListTonalElevationDialog = false
                }
            )
        }
    }
}

@Composable
internal fun TonalElevationDialog(
    onDismissRequest: () -> Unit,
    initValue: Float,
    defaultValue: () -> Float,
    onConfirm: (Float) -> Unit,
) {
    var value by rememberSaveable { mutableFloatStateOf(initValue) }

    SliderDialog(
        onDismissRequest = onDismissRequest,
        value = value,
        onValueChange = { value = it },
        valueRange = -6f..12f,
        valueLabel = {
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .animateContentSize(),
                    text = TonalElevationPreferenceUtil.toDisplay(value),
                    style = MaterialTheme.typography.titleMedium,
                )
                PodAuraIconButton(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    onClick = { value = defaultValue() },
                    imageVector = Icons.Outlined.Restore,
                    contentDescription = stringResource(Res.string.reset),
                )
            }
        },
        icon = { Icon(imageVector = Icons.Outlined.Tonality, contentDescription = null) },
        title = { Text(text = stringResource(Res.string.tonal_elevation)) },
        confirmButton = {
            TextButton(onClick = { onConfirm(value) }) {
                Text(text = stringResource(Res.string.ok))
            }
        }
    )
}

@Composable
private fun FeedNumberBadgeMenu(expanded: Boolean, onDismissRequest: () -> Unit) {
    val scope = rememberCoroutineScope()
    val feedNumberBadge = FeedNumberBadgePreference.current

    CheckableListMenu(
        expanded = expanded,
        current = feedNumberBadge,
        values = remember { FeedNumberBadgePreference.values.toList() },
        displayName = { FeedNumberBadgePreference.toDisplayName(it) },
        onChecked = { FeedNumberBadgePreference.put(scope, it) },
        onDismissRequest = onDismissRequest,
    )
}