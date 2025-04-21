package com.skyd.anivu.ui.screen.media.list.item

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.skyd.anivu.model.bean.MediaBean
import com.skyd.generated.preference.LocalMediaItemGridTypeCoverRatio

@Composable
fun MediaCoverGridItem(
    data: MediaBean,
    onPlay: (MediaBean) -> Unit,
    onOpenDir: (MediaBean) -> Unit,
    onRemove: (MediaBean) -> Unit,
    onOpenFeed: ((MediaBean) -> Unit)?,
    onOpenArticle: ((MediaBean) -> Unit)?,
    onOpenAddToPlaylistSheet: ((MediaBean) -> Unit)?,
    onLongClick: ((MediaBean) -> Unit)? = null,
) {
    MediaItemContainer(
        data = data,
        onPlay = onPlay,
        onOpenDir = onOpenDir,
        onRemove = onRemove,
        onOpenFeed = onOpenFeed,
        onOpenArticle = onOpenArticle,
        onOpenAddToPlaylistSheet = onOpenAddToPlaylistSheet,
        onLongClick = onLongClick,
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .gridTypeCoverRatio(LocalMediaItemGridTypeCoverRatio.current)
                .background(MaterialTheme.colorScheme.secondary.copy(0.1f))
                .itemClickable(),
        ) {
            MediaCover(
                data = data,
                modifier = Modifier.align(Alignment.Center),
                iconSize = 25.dp,
            )
        }
    }
}