package com.skyd.podaura.podnya.ui.screen.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddComment
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.skyd.podaura.podnya.core.AssistantLimits
import com.skyd.podaura.podnya.model.bean.AssistantConversationSummary
import com.skyd.podaura.podnya.model.bean.AssistantConversationsState
import com.skyd.podnya.resources.Res
import com.skyd.podnya.resources.podnya_cancel
import com.skyd.podnya.resources.podnya_conversation_title
import com.skyd.podnya.resources.podnya_delete
import com.skyd.podnya.resources.podnya_delete_message
import com.skyd.podnya.resources.podnya_delete_running
import com.skyd.podnya.resources.podnya_delete_stop
import com.skyd.podnya.resources.podnya_history
import com.skyd.podnya.resources.podnya_history_empty
import com.skyd.podnya.resources.podnya_memory_only
import com.skyd.podnya.resources.podnya_new_conversation
import com.skyd.podnya.resources.podnya_rename
import com.skyd.podnya.resources.podnya_save
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ConversationHistoryPane(
    state: AssistantConversationsState,
    dispatch: (PodnyaIntent) -> Unit,
    modifier: Modifier = Modifier,
    showTitle: Boolean = true,
    onSelected: () -> Unit = {},
) {
    var rename by remember { mutableStateOf<AssistantConversationSummary?>(null) }
    var delete by remember { mutableStateOf<AssistantConversationSummary?>(null) }
    Surface(
        modifier = modifier
            .fillMaxHeight()
            .testTag("podnya_history"),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column {
            if (showTitle) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 64.dp)
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = stringResource(Res.string.podnya_history),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            OutlinedButton(
                onClick = {
                    dispatch(PodnyaIntent.NewConversation)
                    onSelected()
                },
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.AddComment,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(Res.string.podnya_new_conversation),
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                )
            }
            if (state.summaries.isEmpty()) {
                Text(
                    text = stringResource(resource = Res.string.podnya_history_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                )
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(items = state.summaries, key = { it.id.toString() }) { summary ->
                    ConversationHistoryItem(
                        summary = summary,
                        selected = summary.id == state.selected.id,
                        running = summary.id in state.runningIds,
                        onSelect = {
                            dispatch(PodnyaIntent.Select(summary.id))
                            onSelected()
                        },
                        onRename = { rename = summary },
                        onDelete = { delete = summary }
                    )
                }
            }
            Text(
                text = stringResource(Res.string.podnya_memory_only),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )
        }
    }
    rename?.let { target ->
        var title by remember(target.id) { mutableStateOf(target.title) }
        AlertDialog(
            onDismissRequest = { rename = null },
            title = { Text(stringResource(Res.string.podnya_rename)) },
            text = {
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= AssistantLimits.TITLE_CHARS) title = it },
                    label = { Text(stringResource(Res.string.podnya_conversation_title)) },
                    singleLine = true,
                    modifier = Modifier.testTag("podnya_rename_input")
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    dispatch(PodnyaIntent.Rename(target.id, title))
                    rename = null
                }, enabled = title.isNotBlank()) {
                    Text(stringResource(Res.string.podnya_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { rename = null }) {
                    Text(stringResource(Res.string.podnya_cancel))
                }
            })
    }
    delete?.let { target ->
        val running = target.id in state.runningIds
        AlertDialog(
            onDismissRequest = { delete = null },
            title = { Text(stringResource(Res.string.podnya_delete)) },
            text = { Text(stringResource(if (running) Res.string.podnya_delete_running else Res.string.podnya_delete_message)) },
            confirmButton = {
                TextButton(
                    modifier = Modifier.testTag("podnya_delete_confirm"),
                    onClick = {
                        dispatch(PodnyaIntent.Delete(target.id, running))
                        delete = null
                    }
                ) {
                    Text(
                        stringResource(if (running) Res.string.podnya_delete_stop else Res.string.podnya_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { delete = null }) {
                    Text(stringResource(Res.string.podnya_cancel))
                }
            }
        )
    }
}
