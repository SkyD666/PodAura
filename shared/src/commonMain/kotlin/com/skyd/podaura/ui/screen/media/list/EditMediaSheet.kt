package com.skyd.podaura.ui.screen.media.list

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.skyd.compone.component.dialog.DeleteWarningDialog
import com.skyd.podaura.ext.fileSize
import com.skyd.podaura.model.bean.MediaBean
import com.skyd.podaura.model.bean.MediaGroupBean
import com.skyd.podaura.model.bean.MediaGroupBean.Companion.isDefaultGroup
import com.skyd.podaura.ui.component.SheetChip
import com.skyd.podaura.ui.component.dialog.TextFieldDialog
import com.skyd.podaura.ui.screen.media.list.item.MediaCover
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.openFileWithDefaultApplication
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.add_to_playlist
import podaura.shared.generated.resources.delete
import podaura.shared.generated.resources.feed_screen_name
import podaura.shared.generated.resources.folder
import podaura.shared.generated.resources.item_selected
import podaura.shared.generated.resources.media_group
import podaura.shared.generated.resources.media_options
import podaura.shared.generated.resources.media_screen_add_group
import podaura.shared.generated.resources.media_screen_delete_directory_warning
import podaura.shared.generated.resources.media_screen_delete_file_warning
import podaura.shared.generated.resources.nickname
import podaura.shared.generated.resources.open_with
import podaura.shared.generated.resources.read_screen_name
import podaura.shared.generated.resources.rename

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
    val scope = rememberCoroutineScope()

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
                    if (mediaBean.isFile) Res.string.media_screen_delete_file_warning
                    else Res.string.media_screen_delete_directory_warning
                ),
                onOpenWith = {
                    scope.launch {
                        FileKit.openFileWithDefaultApplication(PlatformFile(mediaBean.filePath))
                    }
                },
                onRenameClicked = { openRenameInputDialog = mediaBean.name },
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
                titleText = stringResource(Res.string.rename),
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
                titleText = stringResource(Res.string.nickname),
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
                text = mediaBean.displayName ?: mediaBean.name,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val isFile = remember(mediaBean) { mediaBean.isFile }
            val description = if (isFile) {
                mediaBean.size.fileSize()
            } else {
                stringResource(Res.string.folder)
            }
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
            text = stringResource(Res.string.media_options),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        FlowRow(
            modifier = Modifier.padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (onOpenWith != null) {
                SheetChip(
                    icon = Icons.AutoMirrored.Outlined.OpenInNew,
                    text = stringResource(Res.string.open_with),
                    onClick = onOpenWith,
                )
            }
            if (onRenameClicked != null) {
                SheetChip(
                    icon = Icons.Outlined.DriveFileRenameOutline,
                    text = stringResource(Res.string.rename),
                    onClick = onRenameClicked,
                )
            }
            if (onSetFileDisplayNameClicked != null) {
                SheetChip(
                    icon = Icons.Outlined.Badge,
                    text = stringResource(Res.string.nickname),
                    onClick = onSetFileDisplayNameClicked,
                )
            }
            if (onAddToPlaylistClicked != null) {
                SheetChip(
                    icon = Icons.AutoMirrored.Outlined.PlaylistAdd,
                    text = stringResource(Res.string.add_to_playlist),
                    onClick = onAddToPlaylistClicked,
                )
            }
            if (onDelete != null) {
                SheetChip(
                    icon = Icons.Outlined.Delete,
                    iconTint = MaterialTheme.colorScheme.onError,
                    iconBackgroundColor = MaterialTheme.colorScheme.error,
                    text = stringResource(Res.string.delete),
                    onClick = { openDeleteWarningDialog = true },
                )
            }
            if (onOpenFeed != null) {
                SheetChip(
                    icon = Icons.Outlined.RssFeed,
                    text = stringResource(Res.string.feed_screen_name),
                    onClick = onOpenFeed,
                )
            }
            if (onOpenArticle != null) {
                SheetChip(
                    icon = Icons.AutoMirrored.Outlined.Article,
                    text = stringResource(Res.string.read_screen_name),
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
    title: String = stringResource(Res.string.media_group),
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
    ) {
        SheetChip(
            icon = Icons.Outlined.Add,
            text = null,
            contentDescription = stringResource(Res.string.media_screen_add_group),
            onClick = openCreateGroupDialog,
        )
        groups.forEach { group ->
            val selected = currentGroup.name == group.name &&
                    currentGroup.isDefaultGroup() == group.isDefaultGroup()
            SheetChip(
                modifier = Modifier.animateContentSize(),
                icon = if (selected) Icons.Outlined.Check else null,
                text = group.name,
                contentDescription = if (selected) stringResource(Res.string.item_selected) else null,
                onClick = { onGroupChange(group) },
            )
        }
    }
}