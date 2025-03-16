package com.skyd.anivu.ui.component.dialog

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.DialogProperties

@Composable
fun PodAuraDialog(
    modifier: Modifier = Modifier,
    visible: Boolean = true,
    properties: DialogProperties = DialogProperties(),
    onDismissRequest: () -> Unit = {},
    icon: @Composable (() -> Unit)? = {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
        )
    },
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    selectable: Boolean = false,
    scrollable: Boolean = true,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable (() -> Unit)? = null,
) {
    if (visible) {
        AlertDialog(
            properties = properties,
            modifier = modifier,
            onDismissRequest = onDismissRequest,
            icon = icon,
            title = title,
            text = {
                if (selectable) {
                    SelectionContainer(
                        modifier = Modifier.run {
                            if (scrollable) verticalScroll(rememberScrollState()) else this
                        },
                    ) {
                        text?.invoke()
                    }
                } else {
                    if (scrollable) {
                        Box(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            text?.invoke()
                        }
                    } else {
                        text?.invoke()
                    }
                }
            },
            confirmButton = confirmButton,
            dismissButton = dismissButton,
        )
    }
}