package com.skyd.anivu.ui.screen.media

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.skyd.anivu.R
import com.skyd.anivu.model.preference.behavior.media.BaseMediaListSortByPreference
import com.skyd.anivu.ui.component.dialog.PodAuraDialog

@Composable
fun SortMediaDialog(
    visible: Boolean = true,
    onDismissRequest: () -> Unit,
    sortByValues: List<String>,
    sortBy: String,
    sortAsc: Boolean,
    onSortBy: (String) -> Unit,
    onSortAsc: (Boolean) -> Unit,
) {
    val context = LocalContext.current
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
                val options = listOf(false, true)
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
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
                Spacer(modifier = Modifier.height(12.dp))
                sortByValues.forEach {
                    ListItem(
                        modifier = Modifier.clickable { onSortBy(it) },
                        headlineContent = {
                            Text(BaseMediaListSortByPreference.toDisplayName(context, it))
                        },
                        leadingContent = {
                            BaseMediaListSortByPreference.toIcon(it)?.let { icon ->
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