package com.skyd.podaura.ui.screen.media.list.item

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.skyd.podaura.model.bean.MediaBean
import com.skyd.podaura.model.preference.appearance.media.item.BaseMediaItemTypePreference

@Composable
fun MediaItem(
    itemType: String,
    data: MediaBean,
    onPlay: (MediaBean) -> Unit,
    onOpenDir: (MediaBean) -> Unit,
    onRemove: (MediaBean) -> Unit,
    onOpenFeed: ((MediaBean) -> Unit)?,
    onOpenArticle: ((MediaBean) -> Unit)?,
    onOpenAddToPlaylistSheet: ((MediaBean) -> Unit)?,
    onLongClick: ((MediaBean) -> Unit)? = null,
) {
    when (itemType) {
        BaseMediaItemTypePreference.LIST -> MediaListItem(
            data = data,
            onPlay = onPlay,
            onOpenDir = onOpenDir,
            onRemove = onRemove,
            onOpenFeed = onOpenFeed,
            onOpenArticle = onOpenArticle,
            onOpenAddToPlaylistSheet = onOpenAddToPlaylistSheet,
            onLongClick = onLongClick,
        )

        BaseMediaItemTypePreference.COMPACT_GRID -> MediaCompactGridItem(
            data = data,
            onPlay = onPlay,
            onOpenDir = onOpenDir,
            onRemove = onRemove,
            onOpenFeed = onOpenFeed,
            onOpenArticle = onOpenArticle,
            onOpenAddToPlaylistSheet = onOpenAddToPlaylistSheet,
            onLongClick = onLongClick,
        )

        BaseMediaItemTypePreference.COMFORTABLE_GRID -> MediaComfortableGridItem(
            data = data,
            onPlay = onPlay,
            onOpenDir = onOpenDir,
            onRemove = onRemove,
            onOpenFeed = onOpenFeed,
            onOpenArticle = onOpenArticle,
            onOpenAddToPlaylistSheet = onOpenAddToPlaylistSheet,
            onLongClick = onLongClick,
        )

        BaseMediaItemTypePreference.COVER_GRID -> MediaCoverGridItem(
            data = data,
            onPlay = onPlay,
            onOpenDir = onOpenDir,
            onRemove = onRemove,
            onOpenFeed = onOpenFeed,
            onOpenArticle = onOpenArticle,
            onOpenAddToPlaylistSheet = onOpenAddToPlaylistSheet,
            onLongClick = onLongClick,
        )

        else -> Text("Unknown item type: $itemType")
    }
}