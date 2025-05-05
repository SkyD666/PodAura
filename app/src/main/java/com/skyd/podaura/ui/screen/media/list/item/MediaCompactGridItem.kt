package com.skyd.podaura.ui.screen.media.list.item

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.skyd.podaura.model.bean.MediaBean
import com.skyd.podaura.model.preference.appearance.media.item.MediaItemGridTypeCoverRatioPreference

@Composable
fun MediaCompactGridItem(
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
                .gridTypeCoverRatio(MediaItemGridTypeCoverRatioPreference.current)
                .background(MaterialTheme.colorScheme.secondary.copy(0.1f))
                .itemClickable(),
        ) {
            MediaCover(
                data = data,
                modifier = Modifier.align(Alignment.Center),
                iconSize = 25.dp,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .basicMarquee(Int.MAX_VALUE)
                    .padding(horizontal = 8.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GridTag()
                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(6.dp))
                if (data.isDir) {
                    GridFolderNumberBadge()
                } else {
                    GridFileSize()
                }
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0f to Color.Transparent,
                                0.5f to Color.Black.copy(alpha = 0.3f),
                                1f to Color.Black.copy(alpha = 0.56f),
                            )
                        )
                    ),
            ) {
                Spacer(modifier = Modifier.height(20.dp))
                Title(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    color = Color.White,
                    maxLines = 2,
                    style = MaterialTheme.typography.titleSmall.copy(lineHeight = 1.1.em),
                )
            }
        }
    }
}