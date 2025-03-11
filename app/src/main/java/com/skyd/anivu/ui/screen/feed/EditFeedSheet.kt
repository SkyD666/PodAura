package com.skyd.anivu.ui.screen.feed

import android.net.Uri
import android.webkit.URLUtil
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ContextualFlowRow
import androidx.compose.foundation.layout.ContextualFlowRowOverflow
import androidx.compose.foundation.layout.ContextualFlowRowOverflowScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowOverflow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ClearAll
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Http
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.ToggleOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.paging.compose.LazyPagingItems
import com.skyd.anivu.R
import com.skyd.anivu.ext.copy
import com.skyd.anivu.ext.openBrowser
import com.skyd.anivu.ext.readable
import com.skyd.anivu.model.bean.feed.FeedViewBean
import com.skyd.anivu.model.bean.group.GroupVo
import com.skyd.anivu.ui.component.PodAuraIconButton
import com.skyd.anivu.ui.component.dialog.DeleteWarningDialog
import com.skyd.anivu.ui.component.dialog.PodAuraDialog
import com.skyd.anivu.ui.component.dialog.TextFieldDialog
import com.skyd.anivu.ui.component.showToast
import com.skyd.anivu.ui.local.LocalNavController
import com.skyd.anivu.ui.screen.article.FeedIcon
import com.skyd.anivu.ui.screen.feed.requestheaders.openRequestHeadersScreen
import com.skyd.anivu.util.launchImagePicker
import com.skyd.anivu.util.rememberImagePicker

@Composable
fun EditFeedSheet(
    onDismissRequest: () -> Unit,
    feedView: FeedViewBean,
    groups: LazyPagingItems<GroupVo>,
    onReadAll: (String) -> Unit,
    onRefresh: (String) -> Unit,
    onMute: (String, Boolean) -> Unit,
    onClear: (String) -> Unit,
    onDelete: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onNicknameChange: (String?) -> Unit,
    onCustomDescriptionChange: (String?) -> Unit,
    onCustomIconChange: (Uri?) -> Unit,
    onSortXmlArticlesOnUpdateChanged: (Boolean) -> Unit,
    onGroupChange: (GroupVo) -> Unit,
    openCreateGroupDialog: () -> Unit,
) {
    val navController = LocalNavController.current
    val feed = feedView.feed
    var openUrlDialog by rememberSaveable { mutableStateOf(false) }
    var url by rememberSaveable(feed.url) { mutableStateOf(feed.url) }
    var openNicknameDialog by rememberSaveable { mutableStateOf(false) }
    var nickname by rememberSaveable(feed.nickname, feed.title) {
        mutableStateOf(feed.nickname ?: feed.title)
    }
    var openCustomDescriptionDialog by rememberSaveable { mutableStateOf(false) }
    var customDescription by rememberSaveable(feed.customDescription, feed.description) {
        mutableStateOf(feed.customDescription ?: feed.description)
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            InfoArea(
                feedView = feedView,
                onCustomIconChange = onCustomIconChange,
                onNicknameChanged = { openNicknameDialog = true },
                onCustomDescriptionChanged = { openCustomDescriptionDialog = true },
            )
            Spacer(modifier = Modifier.height(20.dp))

            // URL
            LinkArea(link = feed.url, onLinkClick = { openUrlDialog = true })
            Spacer(modifier = Modifier.height(12.dp))

            // Options
            OptionArea(
                sortXmlArticlesOnUpdate = feed.sortXmlArticlesOnUpdate,
                mute = feed.mute,
                onReadAll = { onReadAll(feed.url) },
                onRefresh = { onRefresh(feed.url) },
                onMuteChanged = { onMute(feed.url, it) },
                onClear = { onClear(feed.url) },
                onDelete = {
                    onDelete(feed.url)
                    onDismissRequest()
                },
                onSortXmlArticlesOnUpdateChanged = onSortXmlArticlesOnUpdateChanged,
                onEditRequestHeaders = {
                    openRequestHeadersScreen(navController = navController, feedUrl = feed.url)
                },
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Group
            GroupArea(
                currentGroupId = feed.groupId,
                groups = groups,
                onGroupChange = onGroupChange,
                openCreateGroupDialog = openCreateGroupDialog,
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    TextFieldDialog(
        onDismissRequest = {
            openUrlDialog = false
            url = feed.url
        },
        visible = openUrlDialog,
        titleText = stringResource(id = R.string.feed_screen_rss_url),
        value = url,
        onValueChange = { url = it },
        onConfirm = {
            onUrlChange(it)
            openUrlDialog = false
        }
    )

    TextFieldDialog(
        onDismissRequest = {
            openNicknameDialog = false
            nickname = feed.nickname
        },
        visible = openNicknameDialog,
        maxLines = 1,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = stringResource(id = R.string.feed_screen_rss_title))
                Spacer(modifier = Modifier.weight(1f))
                PodAuraIconButton(
                    onClick = {
                        onNicknameChange(null)
                        nickname = feed.title
                        openNicknameDialog = false
                    },
                    imageVector = Icons.Outlined.History,
                    contentDescription = stringResource(id = R.string.reset),
                )
            }
        },
        value = nickname.orEmpty(),
        onValueChange = { nickname = it },
        enableConfirm = { !nickname.isNullOrBlank() },
        onConfirm = {
            onNicknameChange(it)
            openNicknameDialog = false
        }
    )

    TextFieldDialog(
        onDismissRequest = {
            openCustomDescriptionDialog = false
            customDescription = feed.customDescription
        },
        visible = openCustomDescriptionDialog,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = stringResource(id = R.string.feed_screen_rss_description))
                Spacer(modifier = Modifier.weight(1f))
                PodAuraIconButton(
                    onClick = {
                        onCustomDescriptionChange(null)
                        customDescription = feed.description
                        openCustomDescriptionDialog = false
                    },
                    imageVector = Icons.Outlined.History,
                    contentDescription = stringResource(id = R.string.reset),
                )
            }
        },
        value = customDescription.orEmpty(),
        onValueChange = { customDescription = it },
        enableConfirm = { true },
        onConfirm = {
            onCustomDescriptionChange(it)
            openCustomDescriptionDialog = false
        }
    )
}

@Composable
private fun InfoArea(
    feedView: FeedViewBean,
    onCustomIconChange: (Uri?) -> Unit,
    onNicknameChanged: () -> Unit,
    onCustomDescriptionChanged: () -> Unit
) {
    val feed = feedView.feed
    Row {
        val pickStickerLauncher = rememberImagePicker(multiple = false) { result ->
            result.firstOrNull()?.let { uri -> onCustomIconChange(uri) }
        }
        var openEditIconDialog by rememberSaveable { mutableStateOf(false) }
        var openNetworkIconDialog by rememberSaveable { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable { openEditIconDialog = true },
        ) {
            FeedIcon(data = feed, size = 48.dp)
            Icon(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .size(20.dp)
                    .padding(4.dp),
                imageVector = Icons.Filled.Edit,
                contentDescription = stringResource(id = R.string.feed_screen_rss_edit_icon),
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.align(Alignment.CenterVertically)) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onNicknameChanged),
                text = feedView.feed.nickname ?: feed.title.orEmpty(),
                style = MaterialTheme.typography.titleLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val description = (feed.customDescription ?: feed.description.orEmpty())
                .readable().trim()
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onCustomDescriptionChanged),
                text = description.ifBlank {
                    stringResource(id = R.string.feed_screen_rss_description_hint)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (description.isBlank()) MaterialTheme.colorScheme.outline
                else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (openEditIconDialog) {
            EditIconDialog(
                onDismissRequest = { openEditIconDialog = false },
                onLocal = {
                    pickStickerLauncher.launchImagePicker()
                    openEditIconDialog = false
                },
                onNetwork = {
                    openNetworkIconDialog = true
                    openEditIconDialog = false
                },
                onRemove = {
                    onCustomIconChange(null)
                    openEditIconDialog = false
                },
            )
        }

        var networkIcon by rememberSaveable(feed.customIcon) {
            mutableStateOf(if (URLUtil.isNetworkUrl(feed.customIcon)) feed.customIcon!! else "")
        }
        if (openNetworkIconDialog) {
            TextFieldDialog(
                onDismissRequest = { openNetworkIconDialog = false },
                titleText = stringResource(id = R.string.feed_screen_rss_icon_source_network),
                value = networkIcon,
                onValueChange = { networkIcon = it },
                placeholder = stringResource(id = R.string.feed_screen_rss_icon_source_network_hint),
                enableConfirm = { URLUtil.isNetworkUrl(networkIcon) },
                onConfirm = {
                    runCatching {
                        onCustomIconChange(it.toUri())
                    }.onSuccess {
                        openNetworkIconDialog = false
                    }.onFailure {
                        it.printStackTrace()
                        it.message?.showToast()
                    }
                },
            )
        }
    }
}

@Composable
private fun LinkArea(link: String, onLinkClick: () -> Unit) {
    val context = LocalContext.current
    SheetChip(
        modifier = Modifier.fillMaxWidth(),
        icon = Icons.Outlined.Link,
        text = link,
        contentDescription = stringResource(id = R.string.feed_screen_rss_url),
        onClick = onLinkClick,
        onLongClick = { link.copy(context) },
        onIconClick = { link.openBrowser(context) },
    )
}

@Composable
internal fun OptionArea(
    deleteWarningText: String = stringResource(id = R.string.feed_screen_delete_feed_warning),
    sortXmlArticlesOnUpdate: Boolean? = null,
    mute: Boolean? = null,
    onReadAll: () -> Unit,
    onRefresh: () -> Unit,
    onMuteChanged: ((Boolean) -> Unit)? = null,
    onMuteAll: ((Boolean) -> Unit)? = null,
    onClear: () -> Unit,
    onDelete: (() -> Unit)?,
    onSortXmlArticlesOnUpdateChanged: ((Boolean) -> Unit)? = null,
    onEditRequestHeaders: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    var openClearWarningDialog by rememberSaveable { mutableStateOf(false) }
    var openDeleteWarningDialog by rememberSaveable { mutableStateOf(false) }

    Text(
        text = stringResource(id = R.string.feed_options),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
    )
    FlowRow(
        modifier = Modifier.padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        overflow = FlowRowOverflow.Visible
    ) {
        SheetChip(
            icon = Icons.Outlined.DoneAll,
            text = stringResource(id = R.string.read_all),
            onClick = onReadAll,
        )
        SheetChip(
            icon = Icons.Outlined.Refresh,
            text = stringResource(id = R.string.refresh),
            onClick = onRefresh,
        )
        if (onMuteChanged != null && mute != null) {
            SheetChip(
                icon = if (mute) Icons.AutoMirrored.Outlined.VolumeUp else Icons.AutoMirrored.Outlined.VolumeOff,
                text = stringResource(id = if (mute) R.string.feed_screen_unmute_feed else R.string.feed_screen_mute_feed),
                onClick = { onMuteChanged(!mute) },
            )
        }
        if (onMuteAll != null) {
            SheetChip(
                icon = Icons.AutoMirrored.Outlined.VolumeOff,
                text = stringResource(id = R.string.feed_screen_mute_all_feeds),
                onClick = { onMuteAll(true) },
            )
            SheetChip(
                icon = Icons.AutoMirrored.Outlined.VolumeUp,
                text = stringResource(id = R.string.feed_screen_unmute_all_feeds),
                onClick = { onMuteAll(false) },
            )
        }
        SheetChip(
            icon = Icons.Outlined.ClearAll,
            text = stringResource(id = R.string.clear),
            onClick = { openClearWarningDialog = true },
        )
        if (onDelete != null) {
            SheetChip(
                icon = Icons.Outlined.Delete,
                iconTint = MaterialTheme.colorScheme.onError,
                iconBackgroundColor = MaterialTheme.colorScheme.error,
                text = stringResource(id = R.string.delete),
                onClick = { openDeleteWarningDialog = true },
            )
        }
        if (sortXmlArticlesOnUpdate != null) {
            SheetChip(
                icon = if (sortXmlArticlesOnUpdate) Icons.Filled.ToggleOn
                else Icons.Outlined.ToggleOff,
                text = stringResource(id = R.string.feed_screen_sort_xml_articles_on_update),
                onClick = {
                    onSortXmlArticlesOnUpdateChanged?.invoke(!sortXmlArticlesOnUpdate)
                    if (!sortXmlArticlesOnUpdate) {
                        context.getString(R.string.feed_screen_sort_xml_articles_on_update_tip)
                            .showToast()
                    }
                },
            )
        }
        if (onEditRequestHeaders != null) {
            SheetChip(
                icon = Icons.Outlined.Http,
                text = stringResource(id = R.string.request_headers_screen_name),
                onClick = onEditRequestHeaders,
            )
        }
    }

    DeleteWarningDialog(
        visible = openClearWarningDialog,
        text = stringResource(id = R.string.feed_screen_clear_articles_warning),
        onDismissRequest = { openClearWarningDialog = false },
        onDismiss = { openClearWarningDialog = false },
        onConfirm = onClear,
    )

    DeleteWarningDialog(
        visible = openDeleteWarningDialog,
        text = deleteWarningText,
        onDismissRequest = { openDeleteWarningDialog = false },
        onDismiss = { openDeleteWarningDialog = false },
        onConfirm = { onDelete?.invoke() },
    )
}

@Composable
internal fun GroupArea(
    title: String = stringResource(id = R.string.feed_group),
    currentGroupId: String?,
    groups: LazyPagingItems<GroupVo>,
    onGroupChange: (GroupVo) -> Unit,
    openCreateGroupDialog: () -> Unit,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
    )
    var flowRowMaxLines by rememberSaveable { mutableIntStateOf(3) }
    val moreOrCollapseIndicator = @Composable { scope: ContextualFlowRowOverflowScope ->
        val remainingItems = groups.itemCount + 2 - scope.shownItemCount
        SheetChip(
            icon = if (remainingItems == 0) Icons.Outlined.ExpandLess
            else Icons.Outlined.ExpandMore,
            text = null,
            contentDescription = stringResource(
                if (remainingItems == 0) R.string.collapse else R.string.expend
            ),
            onClick = {
                if (remainingItems == 0) {
                    flowRowMaxLines = 3
                } else {
                    flowRowMaxLines += 2
                }
            },
        )
    }
    ContextualFlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .wrapContentHeight(align = Alignment.Top),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        maxLines = flowRowMaxLines,
        overflow = ContextualFlowRowOverflow.expandOrCollapseIndicator(
            minRowsToShowCollapse = 5,
            expandIndicator = moreOrCollapseIndicator,
            collapseIndicator = moreOrCollapseIndicator
        ),
        itemCount = groups.itemCount + 2
    ) { index ->
        when (index) {
            0 -> SheetChip(
                icon = Icons.Outlined.Add,
                text = null,
                contentDescription = stringResource(id = R.string.feed_screen_add_group),
                onClick = openCreateGroupDialog,
            )

            1 -> {
                val selected =
                    currentGroupId == null || currentGroupId == GroupVo.DefaultGroup.groupId
                SheetChip(
                    icon = if (selected) Icons.Outlined.Check else null,
                    text = GroupVo.DefaultGroup.name,
                    contentDescription = if (selected) stringResource(id = R.string.item_selected) else null,
                    onClick = {
                        if (GroupVo.DefaultGroup.groupId != currentGroupId) {
                            onGroupChange(GroupVo.DefaultGroup)
                        }
                    },
                )
            }

            else -> {
                val group = groups[index - 2]
                if (group != null) {
                    val selected = (currentGroupId ?: GroupVo.DefaultGroup.groupId) == group.groupId
                    SheetChip(
                        modifier = Modifier.animateContentSize(),
                        icon = if (selected) Icons.Outlined.Check else null,
                        text = group.name,
                        contentDescription = if (selected) stringResource(id = R.string.item_selected) else null,
                        onClick = { if (group.groupId != currentGroupId) onGroupChange(group) },
                    )
                }
            }
        }
    }
}

@Composable
internal fun SheetChip(
    modifier: Modifier = Modifier,
    icon: ImageVector?,
    iconTint: Color = MaterialTheme.colorScheme.onPrimary,
    iconBackgroundColor: Color = MaterialTheme.colorScheme.primary,
    text: String?,
    contentDescription: String? = null,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onIconClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(100))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            .height(35.dp)
            .combinedClickable(onLongClick = onLongClick, onClick = onClick)
            .padding(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(iconBackgroundColor)
                    .run { if (onIconClick == null) this else clickable(onClick = onIconClick) }
                    .padding(3.dp)
                    .fillMaxHeight()
                    .aspectRatio(1f),
                imageVector = icon,
                contentDescription = contentDescription,
                tint = iconTint,
            )
            if (text != null) {
                Spacer(modifier = Modifier.width(4.dp))
            }
        }
        if (text != null) {
            Text(
                modifier = Modifier.padding(horizontal = 6.dp),
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun EditIconDialog(
    onDismissRequest: () -> Unit,
    onLocal: () -> Unit,
    onNetwork: () -> Unit,
    onRemove: () -> Unit,
) {
    PodAuraDialog(
        onDismissRequest = onDismissRequest,
        icon = { Icon(imageVector = Icons.Outlined.Image, contentDescription = null) },
        title = { Text(text = stringResource(id = R.string.feed_screen_rss_edit_icon)) },
        text = {
            Column {
                ListItem(
                    modifier = Modifier.clickable(onClick = onLocal),
                    headlineContent = { Text(text = stringResource(R.string.feed_screen_rss_icon_source_local)) },
                    leadingContent = {
                        Icon(imageVector = Icons.Outlined.PhoneAndroid, contentDescription = null)
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
                HorizontalDivider()
                ListItem(
                    modifier = Modifier.clickable(onClick = onNetwork),
                    headlineContent = { Text(text = stringResource(R.string.feed_screen_rss_icon_source_network)) },
                    leadingContent = {
                        Icon(imageVector = Icons.Outlined.Public, contentDescription = null)
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
                HorizontalDivider()
                ListItem(
                    modifier = Modifier.clickable(onClick = onRemove),
                    headlineContent = { Text(text = stringResource(R.string.delete)) },
                    leadingContent = {
                        Icon(imageVector = Icons.Outlined.Delete, contentDescription = null)
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }
        },
        selectable = false,
        confirmButton = {},
    )
}