package com.skyd.podaura.podnya.ui.screen.chat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.skyd.podaura.podnya.core.UnreadableContext
import com.skyd.podaura.podnya.model.bean.conversation.ConversationStorageState
import com.skyd.podnya.resources.Res
import com.skyd.podnya.resources.podnya_reset_context
import com.skyd.podnya.resources.podnya_retry_open
import com.skyd.podnya.resources.podnya_retry_save
import com.skyd.podnya.resources.podnya_search_context_required
import com.skyd.podnya.resources.podnya_storage_loading
import com.skyd.podnya.resources.podnya_storage_newer
import com.skyd.podnya.resources.podnya_storage_occupied
import com.skyd.podnya.resources.podnya_storage_open_failed
import com.skyd.podnya.resources.podnya_storage_save_failed
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ConversationStorageNotice(state: PodnyaState, dispatch: (PodnyaIntent) -> Unit) {
    val storage = state.conversations.storage
    if (storage != ConversationStorageState.READY) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .testTag("podnya_storage_notice")
        ) {
            Text(
                text = stringResource(
                    when (storage) {
                        ConversationStorageState.LOADING -> Res.string.podnya_storage_loading
                        ConversationStorageState.SAVE_FAILED -> Res.string.podnya_storage_save_failed
                        ConversationStorageState.OCCUPIED -> Res.string.podnya_storage_occupied
                        ConversationStorageState.NEWER_VERSION -> Res.string.podnya_storage_newer
                        else -> Res.string.podnya_storage_open_failed
                    }
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (storage == ConversationStorageState.LOADING) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else {
                TextButton(onClick = { dispatch(PodnyaIntent.RetrySave) }) {
                    Text(stringResource(if (storage == ConversationStorageState.SAVE_FAILED) Res.string.podnya_retry_save else Res.string.podnya_retry_open))
                }
            }
        }
    }
    if (state.session.context is UnreadableContext) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text(stringResource(Res.string.podnya_search_context_required))
            TextButton(onClick = { dispatch(PodnyaIntent.ResetContext(state.session.id)) }) {
                Text(stringResource(Res.string.podnya_reset_context))
            }
        }
    }
}
