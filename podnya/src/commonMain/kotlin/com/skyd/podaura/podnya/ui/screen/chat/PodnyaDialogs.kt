package com.skyd.podaura.podnya.ui.screen.chat

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.skyd.podaura.podnya.core.AssistantFailure
import com.skyd.podaura.podnya.ui.podnyaFailureText
import com.skyd.podnya.resources.Res
import com.skyd.podnya.resources.podnya_ok
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun PodnyaDialogs(
    error: AssistantFailure?,
    onDismissError: () -> Unit,
) {
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
