package com.skyd.anivu.ui.screen.settings.appearance.feed

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.skyd.anivu.R
import com.skyd.anivu.model.preference.appearance.feed.FeedListTonalElevationPreference
import com.skyd.anivu.model.preference.appearance.feed.FeedNumberBadgePreference
import com.skyd.anivu.model.preference.appearance.feed.FeedTopBarTonalElevationPreference
import com.skyd.anivu.model.preference.appearance.feed.TonalElevationPreferenceUtil
import com.skyd.anivu.ui.component.BaseSettingsItem
import com.skyd.anivu.ui.component.CategorySettingsItem
import com.skyd.anivu.ui.component.CheckableListMenu
import com.skyd.anivu.ui.component.PodAuraIconButton
import com.skyd.anivu.ui.component.PodAuraTopBar
import com.skyd.anivu.ui.component.PodAuraTopBarStyle
import com.skyd.anivu.ui.component.dialog.SliderDialog
import com.skyd.anivu.ui.local.LocalFeedListTonalElevation
import com.skyd.anivu.ui.local.LocalFeedNumberBadge
import com.skyd.anivu.ui.local.LocalFeedTopBarTonalElevation


const val FEED_STYLE_SCREEN_ROUTE = "feedStyleScreen"

@Composable
fun FeedStyleScreen() {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            PodAuraTopBar(
                style = PodAuraTopBarStyle.Large,
                scrollBehavior = scrollBehavior,
                title = { Text(text = stringResource(R.string.feed_style_screen_name)) },
            )
        }
    ) { paddingValues ->
        var openTopBarTonalElevationDialog by rememberSaveable { mutableStateOf(false) }
        var openGroupListTonalElevationDialog by rememberSaveable { mutableStateOf(false) }
        var expandFeedNumberBadgeMenu by rememberSaveable { mutableStateOf(false) }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = paddingValues,
        ) {
            item {
                CategorySettingsItem(text = stringResource(id = R.string.feed_style_screen_top_bar_category))
            }
            item {
                BaseSettingsItem(
                    icon = rememberVectorPainter(Icons.Outlined.Tonality),
                    text = stringResource(id = R.string.tonal_elevation),
                    descriptionText = TonalElevationPreferenceUtil.toDisplay(
                        LocalFeedTopBarTonalElevation.current
                    ),
                    onClick = { openTopBarTonalElevationDialog = true }
                )
            }
            item {
                CategorySettingsItem(text = stringResource(id = R.string.feed_style_screen_group_list_category))
            }
            item {
                BaseSettingsItem(
                    icon = rememberVectorPainter(Icons.Outlined.Tonality),
                    text = stringResource(id = R.string.tonal_elevation),
                    descriptionText = TonalElevationPreferenceUtil.toDisplay(
                        LocalFeedListTonalElevation.current
                    ),
                    onClick = { openGroupListTonalElevationDialog = true }
                )
            }
            item {
                BaseSettingsItem(
                    icon = rememberVectorPainter(Icons.Outlined.Pin),
                    text = stringResource(id = R.string.feed_style_screen_number_badge),
                    descriptionText = FeedNumberBadgePreference.toDisplayName(
                        context, LocalFeedNumberBadge.current,
                    ),
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

        if (openTopBarTonalElevationDialog) {
            TonalElevationDialog(
                onDismissRequest = { openTopBarTonalElevationDialog = false },
                initValue = LocalFeedTopBarTonalElevation.current,
                defaultValue = { FeedTopBarTonalElevationPreference.default },
                onConfirm = {
                    FeedTopBarTonalElevationPreference.put(
                        context = context,
                        scope = scope,
                        value = it,
                    )
                    openTopBarTonalElevationDialog = false
                }
            )
        }
        if (openGroupListTonalElevationDialog) {
            TonalElevationDialog(
                onDismissRequest = { openGroupListTonalElevationDialog = false },
                initValue = LocalFeedListTonalElevation.current,
                defaultValue = { FeedListTonalElevationPreference.default },
                onConfirm = {
                    FeedListTonalElevationPreference.put(
                        context = context,
                        scope = scope,
                        value = it,
                    )
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
                    contentDescription = stringResource(R.string.reset),
                )
            }
        },
        icon = { Icon(imageVector = Icons.Outlined.Tonality, contentDescription = null) },
        title = { Text(text = stringResource(id = R.string.tonal_elevation)) },
        confirmButton = {
            TextButton(onClick = { onConfirm(value) }) {
                Text(text = stringResource(id = R.string.ok))
            }
        }
    )
}

@Composable
private fun FeedNumberBadgeMenu(expanded: Boolean, onDismissRequest: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val feedNumberBadge = LocalFeedNumberBadge.current

    CheckableListMenu(
        expanded = expanded,
        current = feedNumberBadge,
        values = remember { FeedNumberBadgePreference.values.toList() },
        displayName = { FeedNumberBadgePreference.toDisplayName(context, it) },
        onChecked = { FeedNumberBadgePreference.put(context, scope, it) },
        onDismissRequest = onDismissRequest,
    )
}