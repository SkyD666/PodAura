package com.skyd.podaura.ui.component.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.skyd.podaura.ui.component.connectedButtonShapes
import com.skyd.podaura.ui.component.suspendString
import org.jetbrains.compose.resources.stringResource
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.ascending
import podaura.shared.generated.resources.descending
import podaura.shared.generated.resources.item_selected
import podaura.shared.generated.resources.sort

@Composable
fun SortDialog(
    visible: Boolean = true,
    onDismissRequest: () -> Unit,
    sortByValues: List<String>,
    sortBy: String,
    sortAsc: Boolean,
    enableSortAsc: Boolean = true,
    onSortBy: (String) -> Unit,
    onSortAsc: (Boolean) -> Unit,
    onSortByDisplayName: suspend (String) -> String,
    onSortByIcon: (String) -> ImageVector?,
) {
    PodAuraDialog(
        visible = visible,
        onDismissRequest = onDismissRequest,
        icon = {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Sort,
                contentDescription = null,
            )
        },
        title = { Text(stringResource(Res.string.sort)) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (enableSortAsc) {
                    val options = listOf(false, true)
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 12.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(
                            space = ButtonGroupDefaults.ConnectedSpaceBetween,
                            alignment = Alignment.CenterHorizontally,
                        ),
                    ) {
                        options.forEachIndexed { index, asc ->
                            ToggleButton(
                                checked = options[index] == sortAsc,
                                onCheckedChange = { if (it) onSortAsc(options[index]) },
                                modifier = Modifier
                                    .weight(1f)
                                    .semantics { role = Role.RadioButton },
                                shapes = ButtonGroupDefaults.connectedButtonShapes(options, index),
                            ) {
                                Text(stringResource(if (asc) Res.string.ascending else Res.string.descending))
                            }
                        }
                    }
                }
                sortByValues.forEach {
                    ListItem(
                        modifier = Modifier.selectable(
                            selected = sortBy == it,
                            onClick = { onSortBy(it) },
                            role = Role.RadioButton
                        ),
                        headlineContent = { Text(suspendString { onSortByDisplayName(it) }) },
                        leadingContent = {
                            onSortByIcon(it)?.let { icon ->
                                Icon(imageVector = icon, contentDescription = null)
                            }
                        },
                        trailingContent = {
                            if (sortBy == it) {
                                Icon(
                                    imageVector = Icons.Outlined.Done,
                                    contentDescription = stringResource(Res.string.item_selected),
                                )
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )
                }
            }
        },
        selectable = false,
        scrollable = false,
        confirmButton = {},
    )
}