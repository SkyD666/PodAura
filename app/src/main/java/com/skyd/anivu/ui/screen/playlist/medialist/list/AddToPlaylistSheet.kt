package com.skyd.anivu.ui.screen.playlist.medialist.list

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.paging.compose.LazyPagingItems
import com.skyd.anivu.ext.safeItemKey
import com.skyd.anivu.model.bean.playlist.PlaylistViewBean
import com.skyd.anivu.ui.component.PagingRefreshStateIndicator
import com.skyd.anivu.ui.screen.playlist.PlaylistItem
import com.skyd.anivu.ui.screen.playlist.PlaylistItemPlaceholder


@Composable
fun AddToPlaylistSheetContent(
    playlist: LazyPagingItems<PlaylistViewBean>,
    selected: (PlaylistViewBean) -> Boolean,
    onSelect: (PlaylistViewBean) -> Unit,
    onRemove: (PlaylistViewBean) -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
) {
    PagingRefreshStateIndicator(
        lazyPagingItems = playlist,
        placeholderPadding = contentPadding,
    ) {
        LazyColumn {
            items(
                count = playlist.itemCount,
                key = playlist.safeItemKey { it.playlist.playlistId },
            ) { index ->
                when (val item = playlist[index]) {
                    is PlaylistViewBean -> {
                        val s = selected(item)
                        PlaylistItem(
                            playlistViewBean = item,
                            selected = s,
                            onClick = { if (s) onRemove(item) else onSelect(item) },
                            onRename = {},
                            onDelete = { },
                        )
                    }

                    else -> PlaylistItemPlaceholder()
                }
            }
        }
    }
}