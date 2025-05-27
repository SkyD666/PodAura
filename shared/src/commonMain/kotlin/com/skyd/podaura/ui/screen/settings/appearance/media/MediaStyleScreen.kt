package com.skyd.podaura.ui.screen.settings.appearance.media

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Toc
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.HideImage
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.skyd.podaura.model.preference.appearance.media.MediaShowGroupTabPreference
import com.skyd.podaura.model.preference.appearance.media.MediaShowThumbnailPreference
import com.skyd.podaura.model.preference.appearance.media.item.BaseMediaItemTypePreference
import com.skyd.podaura.model.preference.appearance.media.item.MediaItemGridTypeCoverRatioPreference
import com.skyd.podaura.model.preference.appearance.media.item.MediaItemGridTypeMinWidthPreference
import com.skyd.podaura.model.preference.appearance.media.item.MediaItemListTypeMinWidthPreference
import com.skyd.podaura.model.preference.appearance.media.item.MediaListItemTypePreference
import com.skyd.podaura.model.preference.appearance.media.item.MediaSubListItemTypePreference
import com.skyd.podaura.ui.component.PodAuraTopBar
import com.skyd.podaura.ui.component.PodAuraTopBarStyle
import com.skyd.podaura.ui.component.dialog.ItemMinWidthDialog
import com.skyd.podaura.ui.component.dialog.PodAuraDialog
import com.skyd.podaura.ui.component.dialog.SliderWithLabelDialog
import com.skyd.podaura.ui.component.settings.BaseSettingsItem
import com.skyd.podaura.ui.component.settings.SettingsLazyColumn
import com.skyd.podaura.ui.component.settings.SwitchBaseSettingsItem
import com.skyd.podaura.ui.component.settings.SwitchSettingsItem
import com.skyd.podaura.ui.component.suspendString
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.media_style_screen_media_item_grid_type_cover_ratio
import podaura.shared.generated.resources.media_style_screen_media_item_grid_type_min_width_dp
import podaura.shared.generated.resources.media_style_screen_media_item_list_type_min_width_dp
import podaura.shared.generated.resources.media_style_screen_media_list_category
import podaura.shared.generated.resources.media_style_screen_media_list_item_category
import podaura.shared.generated.resources.media_style_screen_media_list_item_type
import podaura.shared.generated.resources.media_style_screen_media_list_show_group_tab
import podaura.shared.generated.resources.media_style_screen_media_list_show_thumbnail
import podaura.shared.generated.resources.media_style_screen_media_primary_list
import podaura.shared.generated.resources.media_style_screen_media_sub_list
import podaura.shared.generated.resources.media_style_screen_name
import podaura.shared.generated.resources.unlimited


@Serializable
data object MediaStyleRoute

@Composable
fun MediaStyleScreen() {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scope = rememberCoroutineScope()
    var openMediaListItemTypeDialog by rememberSaveable { mutableStateOf(false) }
    var openMediaItemListTypeMinWidthDialog by rememberSaveable { mutableStateOf(false) }
    var openMediaItemGridMinWidthDialog by rememberSaveable { mutableStateOf(false) }
    var openMediaItemGridCoverRatioDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            PodAuraTopBar(
                style = PodAuraTopBarStyle.LargeFlexible,
                scrollBehavior = scrollBehavior,
                title = { Text(text = stringResource(Res.string.media_style_screen_name)) },
            )
        }
    ) { paddingValues ->
        SettingsLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = paddingValues,
        ) {
            group(text = { getString(Res.string.media_style_screen_media_list_category) }) {
                item {
                    val mediaShowThumbnail = MediaShowThumbnailPreference.current
                    SwitchSettingsItem(
                        imageVector = if (mediaShowThumbnail) Icons.Outlined.Image else Icons.Outlined.HideImage,
                        text = stringResource(Res.string.media_style_screen_media_list_show_thumbnail),
                        checked = mediaShowThumbnail,
                        onCheckedChange = { MediaShowThumbnailPreference.put(scope, it) }
                    )
                }
                item {
                    SwitchSettingsItem(
                        imageVector = Icons.AutoMirrored.Outlined.Toc,
                        text = stringResource(Res.string.media_style_screen_media_list_show_group_tab),
                        checked = MediaShowGroupTabPreference.current,
                        onCheckedChange = { MediaShowGroupTabPreference.put(scope, it) }
                    )
                }
            }
            group(text = { getString(Res.string.media_style_screen_media_list_item_category) }) {
                item {
                    BaseSettingsItem(
                        icon = null,
                        text = stringResource(Res.string.media_style_screen_media_list_item_type),
                        descriptionText = null,
                        onClick = { openMediaListItemTypeDialog = true },
                    )
                }
                item {
                    BaseSettingsItem(
                        icon = null,
                        text = stringResource(Res.string.media_style_screen_media_item_list_type_min_width_dp),
                        descriptionText = "%.2f".format(MediaItemListTypeMinWidthPreference.current) + " dp",
                        onClick = { openMediaItemListTypeMinWidthDialog = true },
                    )
                }
                item {
                    BaseSettingsItem(
                        icon = null,
                        text = stringResource(Res.string.media_style_screen_media_item_grid_type_min_width_dp),
                        descriptionText = "%.2f".format(MediaItemGridTypeMinWidthPreference.current) + " dp",
                        onClick = { openMediaItemGridMinWidthDialog = true },
                    )
                }
                item {
                    val coverRatio = MediaItemGridTypeCoverRatioPreference.current
                    SwitchBaseSettingsItem(
                        checked = coverRatio > 0f,
                        onCheckedChange = {
                            val value =
                                if (it) MediaItemGridTypeCoverRatioPreference.default else 0f
                            MediaItemGridTypeCoverRatioPreference.put(scope, value)
                        },
                        imageVector = null,
                        text = stringResource(Res.string.media_style_screen_media_item_grid_type_cover_ratio),
                        description = if (coverRatio == 0f) {
                            stringResource(Res.string.unlimited)
                        } else {
                            "%.2f".format(coverRatio)
                        },
                        onClick = { openMediaItemGridCoverRatioDialog = true },
                    )
                }
            }
        }
        if (openMediaListItemTypeDialog) {
            MediaListItemTypeDialog(
                onDismissRequest = { openMediaListItemTypeDialog = false },
            )
        }
        if (openMediaItemListTypeMinWidthDialog) {
            ItemMinWidthDialog(
                onDismissRequest = { openMediaItemListTypeMinWidthDialog = false },
                initValue = MediaItemListTypeMinWidthPreference.current,
                defaultValue = { MediaItemListTypeMinWidthPreference.default },
                valueRange = MediaItemListTypeMinWidthPreference.range,
                onConfirm = {
                    MediaItemListTypeMinWidthPreference.put(scope, it)
                    openMediaItemListTypeMinWidthDialog = false
                }
            )
        }
        if (openMediaItemGridMinWidthDialog) {
            ItemMinWidthDialog(
                onDismissRequest = { openMediaItemGridMinWidthDialog = false },
                initValue = MediaItemGridTypeMinWidthPreference.current,
                defaultValue = { MediaItemGridTypeMinWidthPreference.default },
                valueRange = MediaItemGridTypeMinWidthPreference.range,
                onConfirm = {
                    MediaItemGridTypeMinWidthPreference.put(scope, it)
                    openMediaItemGridMinWidthDialog = false
                }
            )
        }
        if (openMediaItemGridCoverRatioDialog) {
            GridCoverRatioDialog(
                onDismissRequest = { openMediaItemGridCoverRatioDialog = false },
                initValue = MediaItemGridTypeCoverRatioPreference.current,
                defaultValue = { MediaItemGridTypeCoverRatioPreference.default },
                valueRange = MediaItemGridTypeCoverRatioPreference.range,
                onConfirm = {
                    MediaItemGridTypeCoverRatioPreference.put(scope, it)
                    openMediaItemGridCoverRatioDialog = false
                }
            )
        }
    }
}

@Composable
private fun MediaListItemTypeDialog(onDismissRequest: () -> Unit) {
    val scope = rememberCoroutineScope()

    @Composable
    fun Category(text: String) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }

    @Composable
    fun TypeRadio(selected: Boolean, onClick: () -> Unit, text: String) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 12.dp)
                .clip(RoundedCornerShape(6.dp))
                .selectable(
                    selected = selected,
                    onClick = onClick,
                    role = Role.RadioButton
                )
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(
                selected = selected,
                onClick = null // null recommended for accessibility with screen readers
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }

    PodAuraDialog(
        onDismissRequest = onDismissRequest,
        icon = { Icon(imageVector = Icons.Outlined.GridView, contentDescription = null) },
        title = { Text(stringResource(Res.string.media_style_screen_media_list_item_type)) },
        text = {
            Column {
                Category(stringResource(Res.string.media_style_screen_media_primary_list))
                MediaListItemTypePreference.values.forEach { type ->
                    TypeRadio(
                        selected = MediaListItemTypePreference.current == type,
                        onClick = { MediaListItemTypePreference.put(scope, type) },
                        text = suspendString { BaseMediaItemTypePreference.toDisplayName(type) },
                    )
                }
                Category(stringResource(Res.string.media_style_screen_media_sub_list))
                MediaSubListItemTypePreference.values.forEach { type ->
                    TypeRadio(
                        selected = MediaSubListItemTypePreference.current == type,
                        onClick = { MediaSubListItemTypePreference.put(scope, type) },
                        text = suspendString { BaseMediaItemTypePreference.toDisplayName(type) },
                    )
                }
            }
        },
        confirmButton = {},
    )
}

@Composable
fun GridCoverRatioDialog(
    onDismissRequest: () -> Unit,
    initValue: Float,
    defaultValue: () -> Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onConfirm: (Float) -> Unit,
) {
    SliderWithLabelDialog(
        onDismissRequest = onDismissRequest,
        initValue = initValue,
        defaultValue = defaultValue,
        valueRange = valueRange,
        icon = Icons.Outlined.AspectRatio,
        title = stringResource(Res.string.media_style_screen_media_item_grid_type_cover_ratio),
        label = { if (it == 0f) stringResource(Res.string.unlimited) else "%.2f".format(it) },
        onConfirm = onConfirm,
    )
}