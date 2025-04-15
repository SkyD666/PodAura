package com.skyd.anivu.ui.mpv.component.dialog

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
import androidx.compose.ui.res.stringResource
import com.skyd.anivu.R
import com.skyd.anivu.ui.component.PodAuraIconButton
import com.skyd.anivu.ui.component.PodAuraTextField
import com.skyd.anivu.ui.component.dialog.PodAuraDialog

@Composable
fun DelayMillisDialog(
    title: String,
    delay: Long,
    onConform: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var currentDelay by remember { mutableStateOf(delay.toString()) }
    PodAuraDialog(
        onDismissRequest = onDismiss,
        icon = null,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = title, modifier = Modifier.weight(1f))
                PodAuraIconButton(
                    onClick = { currentDelay = "0" },
                    imageVector = Icons.Outlined.Restore,
                    contentDescription = stringResource(R.string.reset),
                )
            }
        },
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val pattern = remember { Regex("^[-+]?\\d+$") }
                PodAuraIconButton(
                    onClick = {
                        currentDelay = ((currentDelay.toLongOrNull() ?: 0) - 500).toString()
                    },
                    imageVector = Icons.Outlined.RemoveCircleOutline,
                    contentDescription = stringResource(R.string.minus),
                )
                PodAuraTextField(
                    modifier = Modifier.weight(1f),
                    value = currentDelay.toString(),
                    onValueChange = {
                        if (it.isEmpty() || pattern.matches(it)) currentDelay = it
                    },
                    trailingIcon = { Text("ms") },
                )
                PodAuraIconButton(
                    onClick = {
                        currentDelay = ((currentDelay.toLongOrNull() ?: 0) + 500).toString()
                    },
                    imageVector = Icons.Outlined.AddCircleOutline,
                    contentDescription = stringResource(R.string.plus),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConform(currentDelay.toLongOrNull() ?: 0)
                onDismiss()
            }) {
                Text(text = stringResource(id = R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.cancel))
            }
        },
    )
}