package com.skyd.anivu.ui.screen.media.list

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowOverflow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.RssFeed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.skyd.anivu.R
import com.skyd.anivu.ext.fileSize
import com.skyd.anivu.ext.openWith
import com.skyd.anivu.ext.toUri
import com.skyd.anivu.model.bean.MediaBean
import com.skyd.anivu.model.bean.MediaGroupBean
import com.skyd.anivu.model.bean.MediaGroupBean.Companion.isDefaultGroup
import com.skyd.anivu.ui.component.dialog.DeleteWarningDialog
import com.skyd.anivu.ui.component.dialog.TextFieldDialog
import com.skyd.anivu.ui.screen.feed.SheetChip

@Composable
fun EditMediaSheet(
    onDismissRequest: () -> Unit,
    mediaBean: MediaBean,
    currentGroup: MediaGroupBean,
    groups: List<MediaGroupBean>,
    onRename: (MediaBean, String) -> Unit,
    onSetFileDisplayName: (MediaBean, String?) -> Unit,
    onAddToPlaylistClicked: ((MediaBean) -> Unit)?,
    onDelete: (MediaBean) -> Unit,
    onGroupChange: (MediaGroupBean) -> Unit,
    openCreateGroupDialog: () -> Unit,
    onOpenFeed: ((MediaBean) -> Unit)?,
    onOpenArticle: ((MediaBean) -> Unit)?,
) {
    val context = LocalContext.current

    ModalBottomSheet(onDismissRequest = onDismissRequest) {
        var openRenameInputDialog by rememberSaveable { mutableStateOf<String?>(null) }
        var openSetFileDisplayNameInputDialog by rememberSaveable { mutableStateOf<String?>(null) }

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            InfoArea(mediaBean = mediaBean)
            Spacer(modifier = Modifier.height(20.dp))

            // Options
            OptionArea(
                deleteWarningText = stringResource(
                    if (mediaBean.isFile) R.string.media_screen_delete_file_warning
                    else R.string.media_screen_delete_directory_warning
                ),
                onOpenWith = { mediaBean.file.toUri(context).openWith(context) },
                onRenameClicked = { openRenameInputDialog = mediaBean.file.name },
                onSetFileDisplayNameClicked = {
                    openSetFileDisplayNameInputDialog = mediaBean.displayName.orEmpty()
                },
                onAddToPlaylistClicked = onAddToPlaylistClicked?.let {
                    {
                        it(mediaBean)
                        onDismissRequest()
                    }
                },
                onDelete = {
                    onDelete(mediaBean)
                    onDismissRequest()
                },
                onOpenFeed = onOpenFeed?.let {
                    {
                        it(mediaBean)
                        onDismissRequest()
                    }
                },
                onOpenArticle = onOpenArticle?.let {
                    {
                        it(mediaBean)
                        onDismissRequest()
                    }
                },
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Group
            GroupArea(
                currentGroup = currentGroup,
                groups = groups,
                onGroupChange = onGroupChange,
                openCreateGroupDialog = openCreateGroupDialog,
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (openRenameInputDialog != null) {
            TextFieldDialog(
                titleText = stringResource(R.string.rename),
                value = openRenameInputDialog.orEmpty(),
                onValueChange = { openRenameInputDialog = it },
                singleLine = false,
                onConfirm = {
                    onRename(mediaBean, it.replace(Regex("[\\n\\r]"), ""))
                    openRenameInputDialog = null
                    onDismissRequest()
                },
                imeAction = ImeAction.Done,
                onDismissRequest = { openRenameInputDialog = null }
            )
        }

        if (openSetFileDisplayNameInputDialog != null) {
            TextFieldDialog(
                titleText = stringResource(R.string.nickname),
                value = openSetFileDisplayNameInputDialog.orEmpty(),
                onValueChange = { openSetFileDisplayNameInputDialog = it },
                singleLine = false,
                enableConfirm = { true },
                onConfirm = {
                    onSetFileDisplayName(mediaBean, it.replace(Regex("[\\n\\r]"), ""))
                    openSetFileDisplayNameInputDialog = null
                    onDismissRequest()
                },
                imeAction = ImeAction.Done,
                onDismissRequest = { openSetFileDisplayNameInputDialog = null }
            )
        }
    }
}

@Composable
private fun InfoArea(mediaBean: MediaBean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        MediaCover(
            data = mediaBean,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .size(48.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.align(Alignment.CenterVertically)) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp)),
                text = mediaBean.displayName ?: mediaBean.file.name.orEmpty(),
                style = MaterialTheme.typography.titleLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val description = mediaBean.file.length().fileSize(LocalContext.current)
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp)),
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun OptionArea(
    deleteWarningText: String,
    onOpenWith: (() -> Unit)? = null,
    onRenameClicked: (() -> Unit)? = null,
    onSetFileDisplayNameClicked: (() -> Unit)? = null,
    onAddToPlaylistClicked: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onOpenFeed: (() -> Unit)? = null,
    onOpenArticle: (() -> Unit)? = null,
) {
    var openDeleteWarningDialog by rememberSaveable { mutableStateOf(false) }

    if (onOpenWith != null || onDelete != null) {
        Text(
            text = stringResource(id = R.string.media_options),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        FlowRow(
            modifier = Modifier.padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            overflow = FlowRowOverflow.Visible
        ) {
            if (onOpenWith != null) {
                SheetChip(
                    icon = Icons.AutoMirrored.Outlined.OpenInNew,
                    text = stringResource(id = R.string.open_with),
                    onClick = onOpenWith,
                )
            }
            if (onRenameClicked != null) {
                SheetChip(
                    icon = Icons.Outlined.DriveFileRenameOutline,
                    text = stringResource(id = R.string.rename),
                    onClick = onRenameClicked,
                )
            }
            if (onSetFileDisplayNameClicked != null) {
                SheetChip(
                    icon = Icons.Outlined.Badge,
                    text = stringResource(id = R.string.nickname),
                    onClick = onSetFileDisplayNameClicked,
                )
            }
            if (onAddToPlaylistClicked != null) {
                SheetChip(
                    icon = Icons.AutoMirrored.Outlined.PlaylistAdd,
                    text = stringResource(id = R.string.add_to_playlist),
                    onClick = onAddToPlaylistClicked,
                )
            }
            if (onDelete != null) {
                SheetChip(
                    icon = Icons.Outlined.Delete,
                    iconTint = MaterialTheme.colorScheme.onError,
                    iconBackgroundColor = MaterialTheme.colorScheme.error,
                    text = stringResource(id = R.string.delete),
                    onClick = { openDeleteWarningDialog = true },
                )
            }
            if (onOpenFeed != null) {
                SheetChip(
                    icon = Icons.Outlined.RssFeed,
                    text = stringResource(id = R.string.feed_screen_name),
                    onClick = onOpenFeed,
                )
            }
            if (onOpenArticle != null) {
                SheetChip(
                    icon = Icons.AutoMirrored.Outlined.Article,
                    text = stringResource(id = R.string.read_screen_name),
                    onClick = onOpenArticle,
                )
            }
        }

        DeleteWarningDialog(
            visible = openDeleteWarningDialog,
            text = deleteWarningText,
            onDismissRequest = { openDeleteWarningDialog = false },
            onDismiss = { openDeleteWarningDialog = false },
            onConfirm = { onDelete?.invoke() },
        )
    }
}

@Composable
internal fun GroupArea(
    title: String = stringResource(id = R.string.media_group),
    currentGroup: MediaGroupBean,
    groups: List<MediaGroupBean>,
    onGroupChange: (MediaGroupBean) -> Unit,
    openCreateGroupDialog: () -> Unit,
) {
    Text(
        text = title,
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
            icon = Icons.Outlined.Add,
            text = null,
            contentDescription = stringResource(id = R.string.media_screen_add_group),
            onClick = openCreateGroupDialog,
        )
        groups.forEach { group ->
            val selected = currentGroup.name == group.name &&
                    currentGroup.isDefaultGroup() == group.isDefaultGroup()
            SheetChip(
                modifier = Modifier.animateContentSize(),
                icon = if (selected) Icons.Outlined.Check else null,
                text = group.name,
                contentDescription = if (selected) stringResource(id = R.string.item_selected) else null,
                onClick = { onGroupChange(group) },
            )
        }
    }
}