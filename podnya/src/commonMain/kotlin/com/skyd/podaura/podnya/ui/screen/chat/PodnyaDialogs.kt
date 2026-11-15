package com.skyd.podaura.podnya.ui.screen.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.skyd.podaura.podnya.core.AssistantFailure
import com.skyd.podaura.podnya.model.repository.conversation.AssistantConversationRepository.Confirmation
import com.skyd.podaura.podnya.ui.podnyaFailureText
import com.skyd.podnya.resources.Res
import com.skyd.podnya.resources.podnya_cancel
import com.skyd.podnya.resources.podnya_destination_conversation
import com.skyd.podnya.resources.podnya_destination_message
import com.skyd.podnya.resources.podnya_destination_title
import com.skyd.podnya.resources.podnya_new_conversation
import com.skyd.podnya.resources.podnya_ok
import com.skyd.podnya.resources.podnya_send
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun PodnyaDialogs(
    confirmation: Confirmation?,
    conversationTitle: String?,
    error: AssistantFailure?,
    onConfirm: (Confirmation) -> Unit,
    onDismissConfirmation: () -> Unit,
    onDismissError: () -> Unit,
) {
    confirmation?.let { target ->
        AlertDialog(
            onDismissRequest = { onDismissConfirmation() },
            title = { Text(stringResource(Res.string.podnya_destination_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(
                            Res.string.podnya_destination_conversation,
                            conversationTitle?.takeIf { it.isNotBlank() }
                                ?: stringResource(Res.string.podnya_new_conversation)),
                        style = MaterialTheme.typography.titleSmall)
                    Text(target.question, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    Text(stringResource(Res.string.podnya_destination_message, target.destination))
                }
            },
            confirmButton = {
                TextButton(
                    modifier = Modifier.testTag("podnya_destination_confirm"),
                    onClick = { onConfirm(target) }
                ) {
                    Text(stringResource(Res.string.podnya_send))
                }
            },
            dismissButton = {
                TextButton(onClick = { onDismissConfirmation() }) {
                    Text(stringResource(Res.string.podnya_cancel))
                }
            })
    }
    error?.let { failure ->
        AlertDialog(
            onDismissRequest = { onDismissError() },
            text = { Text(podnyaFailureText(failure)) },
            confirmButton = {
                TextButton(onClick = { onDismissError() }) {
                    Text(stringResource(Res.string.podnya_ok))
                }
            }
        )
    }
}
