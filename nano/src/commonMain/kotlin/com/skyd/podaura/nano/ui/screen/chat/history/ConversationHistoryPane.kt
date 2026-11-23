package com.skyd.podaura.nano.ui.screen.chat.history

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.skyd.nano.resources.Res
import com.skyd.nano.resources.nano_cancel
import com.skyd.nano.resources.nano_conversation_title
import com.skyd.nano.resources.nano_delete
import com.skyd.nano.resources.nano_delete_message
import com.skyd.nano.resources.nano_delete_running
import com.skyd.nano.resources.nano_delete_stop
import com.skyd.nano.resources.nano_history
import com.skyd.nano.resources.nano_history_empty
import com.skyd.nano.resources.nano_history_on_device
import com.skyd.nano.resources.nano_new_conversation
import com.skyd.nano.resources.nano_next_page
import com.skyd.nano.resources.nano_previous_page
import com.skyd.nano.resources.nano_rename
import com.skyd.nano.resources.nano_save
import com.skyd.podaura.nano.core.AssistantLimits
import com.skyd.podaura.nano.model.bean.conversation.AssistantConversationSummary
import com.skyd.podaura.nano.model.bean.conversation.AssistantConversationsState
import com.skyd.podaura.nano.ui.screen.chat.NanoIntent
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ConversationHistoryPane(
    state: AssistantConversationsState,
    dispatch: (NanoIntent) -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    showTitle: Boolean = true,
    onSelected: () -> Unit = {},
) {
    var rename by remember { mutableStateOf<AssistantConversationSummary?>(null) }
    var delete by remember { mutableStateOf<AssistantConversationSummary?>(null) }
    Surface(
        modifier = modifier
            .fillMaxHeight()
            .testTag("nano_history"),
        color = containerColor
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
                        text = stringResource(Res.string.nano_history),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            OutlinedButton(
                onClick = {
                    dispatch(NanoIntent.NewConversation)
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
                    text = stringResource(Res.string.nano_new_conversation),
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                )
            }
            if (state.summaries.isEmpty()) {
                Text(
                    text = stringResource(resource = Res.string.nano_history_empty),
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
                            dispatch(NanoIntent.Select(summary.id))
                            onSelected()
                        },
                        onRename = { rename = summary },
                        onDelete = { delete = summary }
                    )
                }
            }
            if (state.summaryOffset > 0) {
                TextButton(onClick = {
                    dispatch(
                        NanoIntent.LoadSummaries(
                            (state.summaryOffset - 50).coerceAtLeast(0)
                        )
                    )
                }) {
                    Text(stringResource(Res.string.nano_previous_page))
                }
            }
            if (state.hasMoreSummaries) {
                TextButton(onClick = { dispatch(NanoIntent.LoadSummaries(state.summaryOffset + 50)) }) {
                    Text(stringResource(Res.string.nano_next_page))
                }
            }
            Text(
                text = stringResource(Res.string.nano_history_on_device),
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
            title = { Text(stringResource(Res.string.nano_rename)) },
            text = {
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= AssistantLimits.TITLE_CHARS) title = it },
                    label = { Text(stringResource(Res.string.nano_conversation_title)) },
                    singleLine = true,
                    modifier = Modifier.testTag("nano_rename_input")
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    dispatch(NanoIntent.Rename(target.id, title))
                    rename = null
                }, enabled = title.isNotBlank()) {
                    Text(stringResource(Res.string.nano_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { rename = null }) {
                    Text(stringResource(Res.string.nano_cancel))
                }
            })
    }
    delete?.let { target ->
        val running = target.id in state.runningIds
        AlertDialog(
            onDismissRequest = { delete = null },
            title = { Text(stringResource(Res.string.nano_delete)) },
            text = { Text(stringResource(if (running) Res.string.nano_delete_running else Res.string.nano_delete_message)) },
            confirmButton = {
                TextButton(
                    modifier = Modifier.testTag("nano_delete_confirm"),
                    onClick = {
                        dispatch(NanoIntent.Delete(target.id, running))
                        delete = null
                    }
                ) {
                    Text(
                        stringResource(if (running) Res.string.nano_delete_stop else Res.string.nano_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { delete = null }) {
                    Text(stringResource(Res.string.nano_cancel))
                }
            }
        )
    }
}
