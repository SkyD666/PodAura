package com.skyd.podaura.ui.screen.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.skyd.compone.component.ComponeIconButton
import com.skyd.compone.ext.thenIf
import com.skyd.podaura.model.bean.playlist.PlaylistViewBean
import com.skyd.podaura.ui.component.PodAuraImage
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import podaura.shared.generated.resources.Res
import podaura.shared.generated.resources.delete
import podaura.shared.generated.resources.playlist_screen_item_count
import podaura.shared.generated.resources.rename
import kotlin.math.min

@Composable
fun PlaylistItem(
    playlistViewBean: PlaylistViewBean,
    enableMenu: Boolean = false,
    selected: Boolean = false,
    draggable: Boolean = false,
    dragIconModifier: Modifier = Modifier,
    onClick: (PlaylistViewBean) -> Unit,
    onRename: (PlaylistViewBean) -> Unit,
    onDelete: (PlaylistViewBean) -> Unit,
) {
    var showMenu by rememberSaveable { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .thenIf(selected) { background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)) }
            .combinedClickable(
                onLongClick = { if (enableMenu) showMenu = true },
                onClick = { onClick(playlistViewBean) },
            )
            .padding(vertical = 10.dp)
            .padding(start = 16.dp, end = if (draggable) 6.dp else 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val thumbnails = playlistViewBean.thumbnails
        PlaylistThumbnail(modifier = Modifier.size(52.dp), thumbnails = thumbnails)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlistViewBean.playlist.name,
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = pluralStringResource(
                    Res.plurals.playlist_screen_item_count,
                    playlistViewBean.itemCount,
                    playlistViewBean.itemCount,
                ),
                style = MaterialTheme.typography.titleMedium,
                color = LocalContentColor.current.copy(alpha = 0.6f)
            )
        }
        Menu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            onRename = { onRename(playlistViewBean) },
            onDelete = { onDelete(playlistViewBean) },
        )
        if (selected) {
            Spacer(modifier = Modifier.width(16.dp))
            Checkbox(
                checked = true,
                onCheckedChange = null,
            )
        }
        if (draggable) {
            ComponeIconButton(
                onClick = {},
                imageVector = Icons.Rounded.DragHandle,
                modifier = dragIconModifier,
                contentDescription = null,
            )
        }
    }
}

@Composable
fun PlaylistThumbnail(
    modifier: Modifier,
    thumbnails: List<String>,
    roundedCorner: Dp = 4.dp,
    spaceSize: Dp = 1.dp,
) {
    OutlinedCard(modifier = modifier, shape = RoundedCornerShape(roundedCorner)) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (thumbnails.isNotEmpty()) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(if (thumbnails.size == 1) 1 else 2),
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(spaceSize),
                    horizontalArrangement = Arrangement.spacedBy(
                        spaceSize,
                        Alignment.CenterHorizontally
                    ),
                    userScrollEnabled = false,
                ) {
                    items(thumbnails.subList(0, min(thumbnails.size, 4))) {
                        PodAuraImage(
                            model = it,
                            modifier = Modifier.aspectRatio(1f),
                            contentScale = ContentScale.Crop,
                        )
                    }
                }
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.PlaylistPlay,
                    contentDescription = null,
                )
            }
        }
    }
}

@Composable
private fun Menu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    DropdownMenuPopup(expanded = expanded, onDismissRequest = onDismissRequest) {
        val texts = listOf(
            stringResource(Res.string.rename),
            stringResource(Res.string.delete),
        )
        val leadingIcons = listOf(
            Icons.Outlined.DriveFileRenameOutline,
            Icons.Outlined.Delete,
        )
        val onClicks = listOf(
            {
                onRename()
                onDismissRequest()
            },
            {
                onDelete()
                onDismissRequest()
            },
        )
        DropdownMenuGroup(shapes = MenuDefaults.groupShape(0, 1)) {
            texts.forEachIndexed { itemIndex, text ->
                DropdownMenuItem(
                    text = { Text(text = text) },
                    shape = MenuDefaults.itemShape(itemIndex, texts.size).shape,
                    leadingIcon = {
                        Icon(imageVector = leadingIcons[itemIndex], contentDescription = null)
                    },
                    onClick = onClicks[itemIndex],
                )
            }
        }
    }
}

@Composable
fun PlaylistItemPlaceholder() {
    val color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(55.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.3f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
            )
        }
    }
}