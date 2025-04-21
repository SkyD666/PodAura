package com.skyd.anivu.ui.screen.settings.behavior

import android.os.Parcelable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.SwipeLeft
import androidx.compose.material.icons.outlined.SwipeRight
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.skyd.anivu.R
import com.skyd.anivu.model.preference.appearance.media.MediaFileFilterPreference
import com.skyd.anivu.model.preference.behavior.article.ArticleSwipeLeftActionPreference
import com.skyd.anivu.model.preference.behavior.article.ArticleSwipeRightActionPreference
import com.skyd.anivu.model.preference.behavior.article.ArticleTapActionPreference
import com.skyd.anivu.model.preference.behavior.article.DeduplicateTitleInDescPreference
import com.skyd.anivu.model.preference.behavior.feed.HideEmptyDefaultPreference
import com.skyd.anivu.model.preference.behavior.feed.HideMutedFeedPreference
import com.skyd.anivu.ui.component.BackIcon
import com.skyd.anivu.ui.component.BaseSettingsItem
import com.skyd.anivu.ui.component.CategorySettingsItem
import com.skyd.anivu.ui.component.CheckableListMenu
import com.skyd.anivu.ui.component.ClipboardTextField
import com.skyd.anivu.ui.component.DefaultBackClick
import com.skyd.anivu.ui.component.PodAuraTopBar
import com.skyd.anivu.ui.component.PodAuraTopBarStyle
import com.skyd.anivu.ui.component.SwitchSettingsItem
import com.skyd.anivu.ui.component.dialog.PodAuraDialog
import com.skyd.generated.preference.LocalArticleSwipeLeftAction
import com.skyd.generated.preference.LocalArticleSwipeRightAction
import com.skyd.generated.preference.LocalArticleTapAction
import com.skyd.generated.preference.LocalDeduplicateTitleInDesc
import com.skyd.generated.preference.LocalHideEmptyDefault
import com.skyd.generated.preference.LocalHideMutedFeed
import com.skyd.generated.preference.LocalMediaFileFilter
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable


@Serializable
@Parcelize
data object BehaviorRoute : Parcelable

@Composable
fun BehaviorScreen(onBack: (() -> Unit)? = DefaultBackClick) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var expandArticleTapActionMenu by rememberSaveable { mutableStateOf(false) }
    var expandArticleSwipeLeftActionMenu by rememberSaveable { mutableStateOf(false) }
    var expandArticleSwipeRightActionMenu by rememberSaveable { mutableStateOf(false) }
    var openMediaFileFilterDialog by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            PodAuraTopBar(
                style = PodAuraTopBarStyle.Large,
                scrollBehavior = scrollBehavior,
                title = { Text(text = stringResource(R.string.behavior_screen_name)) },
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
                CategorySettingsItem(text = stringResource(id = R.string.behavior_screen_feed_screen_category))
            }
            item {
                SwitchSettingsItem(
                    imageVector = if (LocalHideEmptyDefault.current) {
                        Icons.Outlined.VisibilityOff
                    } else {
                        Icons.Outlined.Visibility
                    },
                    text = stringResource(id = R.string.behavior_screen_feed_screen_hide_empty_default),
                    description = stringResource(id = R.string.behavior_screen_feed_screen_hide_empty_default_description),
                    checked = LocalHideEmptyDefault.current,
                    onCheckedChange = {
                        HideEmptyDefaultPreference.put(
                            context = context,
                            scope = scope,
                            value = it,
                        )
                    }
                )
            }

            item {
                SwitchSettingsItem(
                    imageVector = Icons.AutoMirrored.Outlined.VolumeOff,
                    text = stringResource(id = R.string.behavior_screen_feed_screen_hide_muted_feed),
                    checked = LocalHideMutedFeed.current,
                    onCheckedChange = {
                        HideMutedFeedPreference.put(
                            context = context,
                            scope = scope,
                            value = it,
                        )
                    }
                )
            }
            item {
                CategorySettingsItem(text = stringResource(id = R.string.behavior_screen_article_screen_category))
            }
            item {
                SwitchSettingsItem(
                    painter = painterResource(id = R.drawable.ic_ink_eraser_24),
                    text = stringResource(id = R.string.behavior_screen_article_screen_deduplicate_title_in_desc),
                    description = stringResource(id = R.string.behavior_screen_article_screen_deduplicate_title_in_desc_description),
                    checked = LocalDeduplicateTitleInDesc.current,
                    onCheckedChange = {
                        DeduplicateTitleInDescPreference.put(
                            context = context,
                            scope = scope,
                            value = it,
                        )
                    }
                )
            }
            item {
                BaseSettingsItem(
                    icon = rememberVectorPainter(image = Icons.AutoMirrored.Outlined.Article),
                    text = stringResource(id = R.string.behavior_screen_article_tap_action),
                    descriptionText = ArticleTapActionPreference.toDisplayName(
                        context = context,
                        value = LocalArticleTapAction.current,
                    ),
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
                    text = stringResource(id = R.string.behavior_screen_article_swipe_left_action),
                    descriptionText = ArticleSwipeLeftActionPreference.toDisplayName(
                        context = context,
                        value = LocalArticleSwipeLeftAction.current,
                    ),
                    extraContent = {
                        ArticleSwipeActionMenu(
                            expanded = expandArticleSwipeLeftActionMenu,
                            onDismissRequest = { expandArticleSwipeLeftActionMenu = false },
                            articleSwipeAction = LocalArticleSwipeLeftAction.current,
                            values = ArticleSwipeLeftActionPreference.values,
                            toDisplayName = {
                                ArticleSwipeLeftActionPreference.toDisplayName(context, it)
                            },
                            onClick = { ArticleSwipeLeftActionPreference.put(context, scope, it) },
                        )
                    },
                    onClick = { expandArticleSwipeLeftActionMenu = true },
                )
            }
            item {
                BaseSettingsItem(
                    icon = rememberVectorPainter(image = Icons.Outlined.SwipeRight),
                    text = stringResource(id = R.string.behavior_screen_article_swipe_right_action),
                    descriptionText = ArticleSwipeRightActionPreference.toDisplayName(
                        context = context,
                        value = LocalArticleSwipeRightAction.current,
                    ),
                    extraContent = {
                        ArticleSwipeActionMenu(
                            expanded = expandArticleSwipeRightActionMenu,
                            onDismissRequest = { expandArticleSwipeRightActionMenu = false },
                            articleSwipeAction = LocalArticleSwipeRightAction.current,
                            values = ArticleSwipeRightActionPreference.values,
                            toDisplayName = {
                                ArticleSwipeRightActionPreference.toDisplayName(context, it)
                            },
                            onClick = { ArticleSwipeRightActionPreference.put(context, scope, it) },
                        )
                    },
                    onClick = { expandArticleSwipeRightActionMenu = true },
                )
            }
            item {
                CategorySettingsItem(text = stringResource(id = R.string.behavior_screen_media_screen_category))
            }
            item {
                val mediaFileFilter = LocalMediaFileFilter.current
                BaseSettingsItem(
                    icon = rememberVectorPainter(image = Icons.Outlined.FilterAlt),
                    text = stringResource(id = R.string.behavior_screen_media_file_filter),
                    descriptionText = MediaFileFilterPreference.toDisplayName(
                        context = context,
                        value = mediaFileFilter,
                    ),
                    onClick = { openMediaFileFilterDialog = mediaFileFilter },
                )
            }
        }
    }

    if (openMediaFileFilterDialog != null) {
        MediaFileFilterDialog(
            onDismissRequest = { openMediaFileFilterDialog = null },
            initValue = openMediaFileFilterDialog!!,
            onConfirm = {
                MediaFileFilterPreference.put(
                    context = context, scope = scope, value = it,
                )
            }
        )
    }
}

@Composable
private fun ArticleTapActionMenu(expanded: Boolean, onDismissRequest: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val articleTapAction = LocalArticleTapAction.current

    CheckableListMenu(
        expanded = expanded,
        current = articleTapAction,
        values = remember { ArticleTapActionPreference.values.toList() },
        displayName = { ArticleTapActionPreference.toDisplayName(context, it) },
        onChecked = { ArticleTapActionPreference.put(context, scope, it) },
        onDismissRequest = onDismissRequest,
    )
}

@Composable
private fun ArticleSwipeActionMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    articleSwipeAction: String,
    values: Array<String>,
    toDisplayName: (String) -> String,
    onClick: (String) -> Unit,
) {
    CheckableListMenu(
        expanded = expanded,
        current = articleSwipeAction,
        values = remember { values.toList() },
        displayName = { toDisplayName(it) },
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
    val context = LocalContext.current
    var value by rememberSaveable { mutableStateOf(initValue) }

    PodAuraDialog(
        onDismissRequest = onDismissRequest,
        icon = { Icon(Icons.Outlined.FilterAlt, contentDescription = null) },
        title = { Text(stringResource(R.string.behavior_screen_media_file_filter)) },
        text = {
            Column {
                ClipboardTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = value,
                    singleLine = true,
                    onValueChange = { value = it },
                    onConfirm = onConfirm,
                    placeholder = stringResource(R.string.behavior_screen_media_file_filter_placeholder)
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MediaFileFilterPreference.values.forEach { filter ->
                        SuggestionChip(
                            onClick = { value = filter },
                            label = {
                                Text(MediaFileFilterPreference.toDisplayName(context, filter))
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
                    text = stringResource(R.string.ok),
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
                Text(text = stringResource(R.string.cancel))
            }
        },
    )
}