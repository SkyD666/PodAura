package com.skyd.podaura.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun <T> ListMenu(
    expanded: Boolean,
    values: Collection<T>,
    displayName: (T) -> String,
    leadingIcon: @Composable ((T) -> Unit)? = null,
    onClick: (T) -> Unit,
    onDismissRequest: () -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
    ) {
        values.forEach { v ->
            DropdownMenuItem(
                text = { Text(text = displayName(v)) },
                leadingIcon = if (leadingIcon == null) null else {
                    { leadingIcon.invoke(v) }
                },
                onClick = {
                    onClick(v)
                    onDismissRequest()
                },
            )
        }
    }
}

@Composable
fun <T> CheckableListMenu(
    expanded: Boolean,
    current: T,
    values: Collection<T>,
    displayName: suspend (T) -> String,
    onChecked: (T) -> Unit,
    onDismissRequest: () -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
    ) {
        values.forEach { v ->
            DropdownMenuItem(
                text = { Text(text = suspendString { displayName(v) }) },
                leadingIcon = {
                    if (current == v) {
                        Icon(imageVector = Icons.Outlined.Done, contentDescription = null)
                    }
                },
                onClick = {
                    onChecked(v)
                    onDismissRequest()
                },
            )
        }
    }
}