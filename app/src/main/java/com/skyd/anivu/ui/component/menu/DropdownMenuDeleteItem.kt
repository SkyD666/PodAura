package com.skyd.anivu.ui.component.menu

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.skyd.anivu.R

@Composable
fun DropdownMenuDeleteItem(onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text = stringResource(id = R.string.delete)) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = null,
            )
        },
        onClick = onClick,
        colors = MenuDefaults.itemColors(
            textColor = MaterialTheme.colorScheme.error,
            leadingIconColor = MaterialTheme.colorScheme.error,
            trailingIconColor = MaterialTheme.colorScheme.error,
        ),
    )
}