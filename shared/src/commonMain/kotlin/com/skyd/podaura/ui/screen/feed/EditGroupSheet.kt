package com.skyd.podaura.ui.screen.feed

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import com.skyd.compone.component.pointerOnBack
import com.skyd.podaura.model.bean.group.GroupVo
import com.skyd.podaura.model.bean.group.GroupVo.Companion.isDefaultGroup
import com.skyd.podaura.ui.component.TopSnackbatHostBox
import com.skyd.podaura.ui.component.dialog.TextFieldDialog
import org.jetbrains.compose.resources.stringResource
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.feed_screen_delete_group_warning
import podaura.shared.generated.resources.feed_screen_group_feeds_move_to
import podaura.shared.generated.resources.feed_screen_rss_title

@Composable
fun EditGroupSheet(
    onDismissRequest: () -> Unit,
    snackbarHost: @Composable () -> Unit = {},
    group: GroupVo,
    groups: LazyPagingItems<GroupVo>,
    onReadAll: (String) -> Unit,
    onRefresh: (String, Boolean) -> Unit,
    onMuteAll: (String, Boolean) -> Unit,
    onClear: (String) -> Unit,
    onDelete: (String) -> Unit,
    onNameChange: (String) -> Unit,
    onMoveTo: (GroupVo) -> Unit,
    onReorderFeedsInGroup: (String?) -> Unit,
    openCreateGroupDialog: () -> Unit,
    onMessage: (String) -> Unit,
) {
    var openNameDialog by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable(group.name) { mutableStateOf(group.name) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = Modifier.pointerOnBack(onBack = onDismissRequest),
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            InfoArea(
                group = group,
                onNameChanged = {
                    // Default group cannot be renamed
                    if (!group.isDefaultGroup()) {
                        openNameDialog = true
                    }
                },
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Options
            OptionArea(
                deleteWarningText = stringResource(
                    Res.string.feed_screen_delete_group_warning,
                    group.name,
                ),
                onReadAll = { onReadAll(group.groupId) },
                onRefresh = { onRefresh(group.groupId, it) },
                onMuteAll = { onMuteAll(group.groupId, it) },
                onClear = { onClear(group.groupId) },
                // Default group cannot be deleted
                onDelete = if (group.isDefaultGroup()) null else {
                    {
                        onDelete(group.groupId)
                        onDismissRequest()
                    }
                },
                onReorderFeedsInGroup = { onReorderFeedsInGroup(group.groupId) },
                onMessage = onMessage,
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Group
            GroupArea(
                title = stringResource(Res.string.feed_screen_group_feeds_move_to),
                currentGroupId = group.groupId,
                groups = groups,
                onGroupChange = {
                    onMoveTo(it)
                    onDismissRequest()
                },
                openCreateGroupDialog = openCreateGroupDialog,
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        TopSnackbatHostBox(snackbarHost = snackbarHost)
    }

    TextFieldDialog(
        onDismissRequest = {
            openNameDialog = false
            name = group.name
        },
        visible = openNameDialog,
        maxLines = 1,
        titleText = stringResource(Res.string.feed_screen_rss_title),
        value = name,
        onValueChange = { name = it },
        enableConfirm = { name.isNotEmpty() },
        onConfirm = {
            onNameChange(it)
            openNameDialog = false
        }
    )
}

@Composable
private fun InfoArea(
    group: GroupVo,
    onNameChanged: () -> Unit,
) {
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onNameChanged)
            .padding(8.dp),
        text = group.name,
        style = MaterialTheme.typography.titleLarge,
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}