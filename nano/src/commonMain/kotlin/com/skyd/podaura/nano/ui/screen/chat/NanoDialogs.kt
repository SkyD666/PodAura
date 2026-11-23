package com.skyd.podaura.nano.ui.screen.chat

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.skyd.nano.resources.Res
import com.skyd.nano.resources.nano_ok
import com.skyd.podaura.nano.core.AssistantFailure
import com.skyd.podaura.nano.ui.nanoFailureText
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun NanoDialogs(
    error: AssistantFailure?,
    onDismissError: () -> Unit,
) {
    error?.let { failure ->
        AlertDialog(
            onDismissRequest = { onDismissError() },
            text = { Text(nanoFailureText(failure)) },
            confirmButton = {
                TextButton(onClick = { onDismissError() }) {
                    Text(stringResource(Res.string.nano_ok))
                }
            }
        )
    }
}
