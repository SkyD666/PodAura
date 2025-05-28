package com.skyd.podaura.ui.screen.settings.behavior

import android.os.Parcelable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.SwipeLeft
import androidx.compose.material.icons.outlined.SwipeRight
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.skyd.podaura.R
import com.skyd.podaura.model.preference.appearance.media.MediaFileFilterPreference
import com.skyd.podaura.model.preference.behavior.LoadNetImageOnWifiOnlyPreference
import com.skyd.podaura.model.preference.behavior.article.ArticleSwipeActionPreference
import com.skyd.podaura.model.preference.behavior.article.ArticleSwipeLeftActionPreference
import com.skyd.podaura.model.preference.behavior.article.ArticleSwipeRightActionPreference
import com.skyd.podaura.model.preference.behavior.article.ArticleTapActionPreference
import com.skyd.podaura.model.preference.behavior.article.DeduplicateTitleInDescPreference
import com.skyd.podaura.model.preference.behavior.feed.HideEmptyDefaultPreference
import com.skyd.podaura.model.preference.behavior.feed.HideMutedFeedPreference
import com.skyd.podaura.model.preference.behavior.playlist.ReverseLoadArticlePlaylistPreference
import com.skyd.podaura.ui.component.BackIcon
import com.skyd.podaura.ui.component.CheckableListMenu
import com.skyd.podaura.ui.component.ClipboardTextField
import com.skyd.podaura.ui.component.DefaultBackClick
import com.skyd.podaura.ui.component.PodAuraTopBar
import com.skyd.podaura.ui.component.PodAuraTopBarStyle
import com.skyd.podaura.ui.component.dialog.PodAuraDialog
import com.skyd.podaura.ui.component.settings.BaseSettingsItem
import com.skyd.podaura.ui.component.settings.SettingsLazyColumn
import com.skyd.podaura.ui.component.settings.SwitchSettingsItem
import com.skyd.podaura.ui.component.suspendString
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.behavior_screen_article_screen_category
import podaura.shared.generated.resources.behavior_screen_article_screen_deduplicate_title_in_desc
import podaura.shared.generated.resources.behavior_screen_article_screen_deduplicate_title_in_desc_description
import podaura.shared.generated.resources.behavior_screen_article_swipe_left_action
import podaura.shared.generated.resources.behavior_screen_article_swipe_right_action
import podaura.shared.generated.resources.behavior_screen_article_tap_action
import podaura.shared.generated.resources.behavior_screen_common_category
import podaura.shared.generated.resources.behavior_screen_feed_screen_category
import podaura.shared.generated.resources.behavior_screen_feed_screen_hide_empty_default
import podaura.shared.generated.resources.behavior_screen_feed_screen_hide_empty_default_description
import podaura.shared.generated.resources.behavior_screen_feed_screen_hide_muted_feed
import podaura.shared.generated.resources.behavior_screen_load_net_image_on_wifi_only
import podaura.shared.generated.resources.behavior_screen_media_file_filter
import podaura.shared.generated.resources.behavior_screen_media_file_filter_placeholder
import podaura.shared.generated.resources.behavior_screen_media_screen_category
import podaura.shared.generated.resources.behavior_screen_name
import podaura.shared.generated.resources.behavior_screen_reverse_load_article_playlist
import podaura.shared.generated.resources.cancel
import podaura.shared.generated.resources.ok
import podaura.shared.generated.resources.playlist_screen_name


@Serializable
@Parcelize
data object BehaviorRoute : Parcelable

@Composable
fun BehaviorScreen(onBack: (() -> Unit)? = DefaultBackClick) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scope = rememberCoroutineScope()
    var expandArticleTapActionMenu by rememberSaveable { mutableStateOf(false) }
    var expandArticleSwipeLeftActionMenu by rememberSaveable { mutableStateOf(false) }
    var expandArticleSwipeRightActionMenu by rememberSaveable { mutableStateOf(false) }
    var openMediaFileFilterDialog by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            PodAuraTopBar(
                style = PodAuraTopBarStyle.LargeFlexible,
                scrollBehavior = scrollBehavior,
                title = { Text(text = stringResource(Res.string.behavior_screen_name)) },
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
            group(text = { getString(Res.string.behavior_screen_common_category) }) {
                item {
                    SwitchSettingsItem(
                        imageVector = Icons.Outlined.Wifi,
                        text = stringResource(Res.string.behavior_screen_load_net_image_on_wifi_only),
                        description = null,
                        checked = LoadNetImageOnWifiOnlyPreference.current,
                        onCheckedChange = { LoadNetImageOnWifiOnlyPreference.put(scope, it) }
                    )
                }
            }
            group(
                text = { getString(Res.string.behavior_screen_feed_screen_category) },
            ) {
                item {
                    SwitchSettingsItem(
                        imageVector = if (HideEmptyDefaultPreference.current) {
                            Icons.Outlined.VisibilityOff
                        } else {
                            Icons.Outlined.Visibility
                        },
                        text = stringResource(Res.string.behavior_screen_feed_screen_hide_empty_default),
                        description = stringResource(Res.string.behavior_screen_feed_screen_hide_empty_default_description),
                        checked = HideEmptyDefaultPreference.current,
                        onCheckedChange = { HideEmptyDefaultPreference.put(scope, it) }
                    )
                }
                item {
                    SwitchSettingsItem(
                        imageVector = Icons.AutoMirrored.Outlined.VolumeOff,
                        text = stringResource(Res.string.behavior_screen_feed_screen_hide_muted_feed),
                        checked = HideMutedFeedPreference.current,
                        onCheckedChange = { HideMutedFeedPreference.put(scope, it) }
                    )
                }
            }
            group(text = { getString(Res.string.behavior_screen_article_screen_category) }) {
                item {
                    SwitchSettingsItem(
                        painter = painterResource(id = R.drawable.ic_ink_eraser_24),
                        text = stringResource(Res.string.behavior_screen_article_screen_deduplicate_title_in_desc),
                        description = stringResource(Res.string.behavior_screen_article_screen_deduplicate_title_in_desc_description),
                        checked = DeduplicateTitleInDescPreference.current,
                        onCheckedChange = { DeduplicateTitleInDescPreference.put(scope, it) }
                    )
                }
                item {
                    BaseSettingsItem(
                        icon = rememberVectorPainter(image = Icons.AutoMirrored.Outlined.Article),
                        text = stringResource(Res.string.behavior_screen_article_tap_action),
                        descriptionText = suspendString(ArticleTapActionPreference.current) {
                            ArticleTapActionPreference.toDisplayName(it)
                        },
                        extraContent = {
                            ArticleTapActionMenu(
                                expanded = expandArticleTapActionMenu,
                                onDismissRequest = { expandArticleTapActionMenu = false }
                            )
                        },
                        onClick = { expandArticleTapActionMenu = true },
                    )
                }
                item {
                    BaseSettingsItem(
                        icon = rememberVectorPainter(image = Icons.Outlined.SwipeLeft),
                        text = stringResource(Res.string.behavior_screen_article_swipe_left_action),
                        descriptionText = suspendString(ArticleSwipeLeftActionPreference.current) {
                            ArticleSwipeActionPreference.toDisplayName(it)
                        },
                        extraContent = {
                            ArticleSwipeActionMenu(
                                expanded = expandArticleSwipeLeftActionMenu,
                                onDismissRequest = { expandArticleSwipeLeftActionMenu = false },
                                articleSwipeAction = ArticleSwipeLeftActionPreference.current,
                                values = ArticleSwipeLeftActionPreference.values,
                                toDisplayName = {
                                    ArticleSwipeActionPreference.toDisplayName(it)
                                },
                                onClick = { ArticleSwipeLeftActionPreference.put(scope, it) },
                            )
                        },
                        onClick = { expandArticleSwipeLeftActionMenu = true },
                    )
                }
                item {
                    BaseSettingsItem(
                        icon = rememberVectorPainter(image = Icons.Outlined.SwipeRight),
                        text = stringResource(Res.string.behavior_screen_article_swipe_right_action),
                        descriptionText = suspendString(ArticleSwipeRightActionPreference.current) {
                            ArticleSwipeActionPreference.toDisplayName(it)
                        },
                        extraContent = {
                            ArticleSwipeActionMenu(
                                expanded = expandArticleSwipeRightActionMenu,
                                onDismissRequest = { expandArticleSwipeRightActionMenu = false },
                                articleSwipeAction = ArticleSwipeRightActionPreference.current,
                                values = ArticleSwipeRightActionPreference.values,
                                toDisplayName = {
                                    ArticleSwipeActionPreference.toDisplayName(it)
                                },
                                onClick = { ArticleSwipeRightActionPreference.put(scope, it) },
                            )
                        },
                        onClick = { expandArticleSwipeRightActionMenu = true },
                    )
                }
            }
            group(text = { getString(Res.string.playlist_screen_name) }) {
                item {
                    SwitchSettingsItem(
                        checked = ReverseLoadArticlePlaylistPreference.current,
                        text = stringResource(Res.string.behavior_screen_reverse_load_article_playlist),
                        imageVector = Icons.Outlined.SwapVert,
                        description = null,
                        onCheckedChange = { ReverseLoadArticlePlaylistPreference.put(scope, it) },
                    )
                }
            }
            group(text = { getString(Res.string.behavior_screen_media_screen_category) }) {
                item {
                    val mediaFileFilter = MediaFileFilterPreference.current
                    BaseSettingsItem(
                        icon = rememberVectorPainter(image = Icons.Outlined.FilterAlt),
                        text = stringResource(Res.string.behavior_screen_media_file_filter),
                        descriptionText = suspendString {
                            MediaFileFilterPreference.toDisplayName(mediaFileFilter)
                        },
                        onClick = { openMediaFileFilterDialog = mediaFileFilter },
                    )
                }
            }
        }
    }

    if (openMediaFileFilterDialog != null) {
        MediaFileFilterDialog(
            onDismissRequest = { openMediaFileFilterDialog = null },
            initValue = openMediaFileFilterDialog!!,
            onConfirm = { MediaFileFilterPreference.put(scope, it) }
        )
    }
}

@Composable
private fun ArticleTapActionMenu(expanded: Boolean, onDismissRequest: () -> Unit) {
    val scope = rememberCoroutineScope()
    val articleTapAction = ArticleTapActionPreference.current

    CheckableListMenu(
        expanded = expanded,
        current = articleTapAction,
        values = remember { ArticleTapActionPreference.values.toList() },
        displayName = { ArticleTapActionPreference.toDisplayName(it) },
        onChecked = { ArticleTapActionPreference.put(scope, it) },
        onDismissRequest = onDismissRequest,
    )
}

@Composable
private fun ArticleSwipeActionMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    articleSwipeAction: String,
    values: Array<String>,
    toDisplayName: suspend (String) -> String,
    onClick: (String) -> Unit,
) {
    CheckableListMenu(
        expanded = expanded,
        current = articleSwipeAction,
        values = remember { values.toList() },
        displayName = toDisplayName,
        onChecked = onClick,
        onDismissRequest = onDismissRequest,
    )
}

@Composable
internal fun MediaFileFilterDialog(
    onDismissRequest: () -> Unit,
    initValue: String,
    onConfirm: (String) -> Unit,
) {
    var value by rememberSaveable { mutableStateOf(initValue) }

    PodAuraDialog(
        onDismissRequest = onDismissRequest,
        icon = { Icon(Icons.Outlined.FilterAlt, contentDescription = null) },
        title = { Text(stringResource(Res.string.behavior_screen_media_file_filter)) },
        text = {
            Column {
                ClipboardTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = value,
                    singleLine = true,
                    onValueChange = { value = it },
                    onConfirm = onConfirm,
                    placeholder = stringResource(Res.string.behavior_screen_media_file_filter_placeholder)
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MediaFileFilterPreference.values.forEach { filter ->
                        SuggestionChip(
                            onClick = { value = filter },
                            label = {
                                Text(suspendString { MediaFileFilterPreference.toDisplayName(filter) })
                            }
                        )
                    }
                }
            }

        },
        confirmButton = {
            val enabled = value.isNotBlank() && runCatching { Regex(value) }.isSuccess
            TextButton(
                enabled = enabled,
                onClick = {
                    onConfirm(value)
                    onDismissRequest()
                }
            ) {
                Text(
                    text = stringResource(Res.string.ok),
                    color = if (enabled) {
                        Color.Unspecified
                    } else {
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
                    }
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(Res.string.cancel))
            }
        },
    )
}