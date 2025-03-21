package com.skyd.anivu.ui.component.dialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.skyd.anivu.R

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
    onSortByDisplayName: (String) -> String,
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
        title = { Text(stringResource(R.string.sort)) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (enableSortAsc) {
                    val options = listOf(false, true)
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 12.dp)
                            .fillMaxWidth()
                    ) {
                        options.forEachIndexed { index, asc ->
                            SegmentedButton(
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index, count = options.size,
                                ),
                                onClick = { onSortAsc(options[index]) },
                                selected = options[index] == sortAsc,
                                label = {
                                    Text(
                                        stringResource(
                                            if (asc) R.string.ascending
                                            else R.string.descending
                                        )
                                    )
                                }
                            )
                        }
                    }
                }
                sortByValues.forEach {
                    ListItem(
                        modifier = Modifier.clickable { onSortBy(it) },
                        headlineContent = { Text(onSortByDisplayName(it)) },
                        leadingContent = {
                            onSortByIcon(it)?.let { icon ->
                                Icon(imageVector = icon, contentDescription = null)
                            }
                        },
                        trailingContent = {
                            if (sortBy == it) {
                                Icon(
                                    imageVector = Icons.Outlined.Done,
                                    contentDescription = stringResource(R.string.item_selected),
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