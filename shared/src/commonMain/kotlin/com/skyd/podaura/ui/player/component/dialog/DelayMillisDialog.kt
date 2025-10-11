package com.skyd.podaura.ui.player.component.dialog

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.skyd.compone.component.ComponeIconButton
import com.skyd.compone.component.dialog.ComponeDialog
import com.skyd.podaura.ui.component.ComponeTextField
import org.jetbrains.compose.resources.stringResource
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.cancel
import podaura.shared.generated.resources.minus
import podaura.shared.generated.resources.ok
import podaura.shared.generated.resources.plus
import podaura.shared.generated.resources.reset

@Composable
internal fun DelayMillisDialog(
    title: String,
    delay: Long,
    onConform: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var currentDelay by remember { mutableStateOf(delay.toString()) }
    ComponeDialog(
        onDismissRequest = onDismiss,
        icon = null,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = title, modifier = Modifier.weight(1f))
                ComponeIconButton(
                    onClick = { currentDelay = "0" },
                    imageVector = Icons.Outlined.Restore,
                    contentDescription = stringResource(Res.string.reset),
                )
            }
        },
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val pattern = remember { Regex("^[-+]?\\d+$") }
                ComponeIconButton(
                    onClick = {
                        currentDelay = ((currentDelay.toLongOrNull() ?: 0) - 500).toString()
                    },
                    imageVector = Icons.Outlined.RemoveCircleOutline,
                    contentDescription = stringResource(Res.string.minus),
                )
                ComponeTextField(
                    modifier = Modifier.weight(1f),
                    value = currentDelay.toString(),
                    onValueChange = {
                        if (it.isEmpty() || pattern.matches(it)) currentDelay = it
                    },
                    trailingIcon = { Text("ms") },
                )
                ComponeIconButton(
                    onClick = {
                        currentDelay = ((currentDelay.toLongOrNull() ?: 0) + 500).toString()
                    },
                    imageVector = Icons.Outlined.AddCircleOutline,
                    contentDescription = stringResource(Res.string.plus),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConform(currentDelay.toLongOrNull() ?: 0)
                onDismiss()
            }) {
                Text(text = stringResource(Res.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(Res.string.cancel))
            }
        },
    )
}