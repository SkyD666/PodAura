package com.skyd.anivu.ui.screen.settings.appearance.media

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.skyd.anivu.R
import com.skyd.anivu.model.preference.appearance.media.MediaShowGroupTabPreference
import com.skyd.anivu.model.preference.appearance.media.MediaShowThumbnailPreference
import com.skyd.anivu.model.preference.appearance.media.item.BaseMediaItemTypePreference
import com.skyd.anivu.model.preference.appearance.media.item.MediaItemGridTypeCoverRatioPreference
import com.skyd.anivu.model.preference.appearance.media.item.MediaItemGridTypeMinWidthPreference
import com.skyd.anivu.model.preference.appearance.media.item.MediaItemListTypeMinWidthPreference
import com.skyd.anivu.model.preference.appearance.media.item.MediaListItemTypePreference
import com.skyd.anivu.model.preference.appearance.media.item.MediaSubListItemTypePreference
import com.skyd.anivu.ui.component.BaseSettingsItem
import com.skyd.anivu.ui.component.CategorySettingsItem
import com.skyd.anivu.ui.component.PodAuraTopBar
import com.skyd.anivu.ui.component.PodAuraTopBarStyle
import com.skyd.anivu.ui.component.SwitchBaseSettingsItem
import com.skyd.anivu.ui.component.SwitchSettingsItem
import com.skyd.anivu.ui.component.dialog.ItemMinWidthDialog
import com.skyd.anivu.ui.component.dialog.PodAuraDialog
import com.skyd.anivu.ui.component.dialog.SliderWithLabelDialog
import com.skyd.generated.preference.LocalMediaItemGridTypeCoverRatio
import com.skyd.generated.preference.LocalMediaItemGridTypeMinWidth
import com.skyd.generated.preference.LocalMediaItemListTypeMinWidth
import com.skyd.generated.preference.LocalMediaListItemType
import com.skyd.generated.preference.LocalMediaShowGroupTab
import com.skyd.generated.preference.LocalMediaShowThumbnail
import com.skyd.generated.preference.LocalMediaSubListItemType
import kotlinx.serialization.Serializable


@Serializable
data object MediaStyleRoute

@Composable
fun MediaStyleScreen() {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var openMediaListItemTypeDialog by rememberSaveable { mutableStateOf(false) }
    var openMediaItemListTypeMinWidthDialog by rememberSaveable { mutableStateOf(false) }
    var openMediaItemGridMinWidthDialog by rememberSaveable { mutableStateOf(false) }
    var openMediaItemGridCoverRatioDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            PodAuraTopBar(
                style = PodAuraTopBarStyle.Large,
                scrollBehavior = scrollBehavior,
                title = { Text(text = stringResource(R.string.media_style_screen_name)) },
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
                CategorySettingsItem(text = stringResource(id = R.string.media_style_screen_media_list_category))
            }
            item {
                val mediaShowThumbnail = LocalMediaShowThumbnail.current
                SwitchSettingsItem(
                    imageVector = if (mediaShowThumbnail) Icons.Outlined.Image else Icons.Outlined.HideImage,
                    text = stringResource(id = R.string.media_style_screen_media_list_show_thumbnail),
                    checked = mediaShowThumbnail,
                    onCheckedChange = {
                        MediaShowThumbnailPreference.put(
                            context = context,
                            scope = scope,
                            value = it,
                        )
                    }
                )
            }
            item {
                val mediaShowGroupTab = LocalMediaShowGroupTab.current
                SwitchSettingsItem(
                    imageVector = Icons.AutoMirrored.Outlined.Toc,
                    text = stringResource(id = R.string.media_style_screen_media_list_show_group_tab),
                    checked = mediaShowGroupTab,
                    onCheckedChange = {
                        MediaShowGroupTabPreference.put(
                            context = context,
                            scope = scope,
                            value = it,
                        )
                    }
                )
            }
            item {
                CategorySettingsItem(text = stringResource(id = R.string.media_style_screen_media_list_item_category))
            }
            item {
                BaseSettingsItem(
                    icon = null,
                    text = stringResource(id = R.string.media_style_screen_media_list_item_type),
                    descriptionText = null,
                    onClick = { openMediaListItemTypeDialog = true },
                )
            }
            item {
                BaseSettingsItem(
                    icon = null,
                    text = stringResource(id = R.string.media_style_screen_media_item_list_type_min_width_dp),
                    descriptionText = "%.2f".format(LocalMediaItemListTypeMinWidth.current) + " dp",
                    onClick = { openMediaItemListTypeMinWidthDialog = true },
                )
            }
            item {
                BaseSettingsItem(
                    icon = null,
                    text = stringResource(id = R.string.media_style_screen_media_item_grid_type_min_width_dp),
                    descriptionText = "%.2f".format(LocalMediaItemGridTypeMinWidth.current) + " dp",
                    onClick = { openMediaItemGridMinWidthDialog = true },
                )
            }
            item {
                val coverRatio = LocalMediaItemGridTypeCoverRatio.current
                SwitchBaseSettingsItem(
                    checked = coverRatio > 0f,
                    onCheckedChange = {
                        val value = if (it) MediaItemGridTypeCoverRatioPreference.default else 0f
                        MediaItemGridTypeCoverRatioPreference.put(context, scope, value)
                    },
                    imageVector = null,
                    text = stringResource(id = R.string.media_style_screen_media_item_grid_type_cover_ratio),
                    description = if (coverRatio == 0f) {
                        stringResource(R.string.unlimited)
                    } else {
                        "%.2f".format(coverRatio)
                    },
                    onClick = { openMediaItemGridCoverRatioDialog = true },
                )
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
                initValue = LocalMediaItemListTypeMinWidth.current,
                defaultValue = { MediaItemListTypeMinWidthPreference.default },
                valueRange = MediaItemListTypeMinWidthPreference.range,
                onConfirm = {
                    MediaItemListTypeMinWidthPreference.put(context, scope, it)
                    openMediaItemListTypeMinWidthDialog = false
                }
            )
        }
        if (openMediaItemGridMinWidthDialog) {
            ItemMinWidthDialog(
                onDismissRequest = { openMediaItemGridMinWidthDialog = false },
                initValue = LocalMediaItemGridTypeMinWidth.current,
                defaultValue = { MediaItemGridTypeMinWidthPreference.default },
                valueRange = MediaItemGridTypeMinWidthPreference.range,
                onConfirm = {
                    MediaItemGridTypeMinWidthPreference.put(context, scope, it)
                    openMediaItemGridMinWidthDialog = false
                }
            )
        }
        if (openMediaItemGridCoverRatioDialog) {
            GridCoverRatioDialog(
                onDismissRequest = { openMediaItemGridCoverRatioDialog = false },
                initValue = LocalMediaItemGridTypeCoverRatio.current,
                defaultValue = { MediaItemGridTypeCoverRatioPreference.default },
                valueRange = MediaItemGridTypeCoverRatioPreference.range,
                onConfirm = {
                    MediaItemGridTypeCoverRatioPreference.put(context, scope, it)
                    openMediaItemGridCoverRatioDialog = false
                }
            )
        }
    }
}

@Composable
private fun MediaListItemTypeDialog(onDismissRequest: () -> Unit) {
    val context = LocalContext.current
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
        title = { Text(stringResource(R.string.media_style_screen_media_list_item_type)) },
        text = {
            Column {
                Category(stringResource(R.string.media_style_screen_media_primary_list))
                MediaListItemTypePreference.values.forEach { type ->
                    TypeRadio(
                        selected = LocalMediaListItemType.current == type,
                        onClick = { MediaListItemTypePreference.put(context, scope, type) },
                        text = BaseMediaItemTypePreference.toDisplayName(context, type),
                    )
                }
                Category(stringResource(R.string.media_style_screen_media_sub_list))
                MediaSubListItemTypePreference.values.forEach { type ->
                    TypeRadio(
                        selected = LocalMediaSubListItemType.current == type,
                        onClick = { MediaSubListItemTypePreference.put(context, scope, type) },
                        text = BaseMediaItemTypePreference.toDisplayName(context, type),
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
    val context = LocalContext.current
    SliderWithLabelDialog(
        onDismissRequest = onDismissRequest,
        initValue = initValue,
        defaultValue = defaultValue,
        valueRange = valueRange,
        icon = Icons.Outlined.AspectRatio,
        title = stringResource(id = R.string.media_style_screen_media_item_grid_type_cover_ratio),
        label = { if (it == 0f) context.getString(R.string.unlimited) else "%.2f".format(it) },
        onConfirm = onConfirm,
    )
}