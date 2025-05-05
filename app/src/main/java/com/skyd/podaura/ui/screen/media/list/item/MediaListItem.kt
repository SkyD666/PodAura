package com.skyd.podaura.ui.screen.media.list.item

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.skyd.podaura.model.bean.MediaBean

@Composable
fun MediaListItem(
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
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.secondary.copy(0.1f))
                .itemClickable()
                .padding(horizontal = 13.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MediaCover(
                data = data,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .size(50.dp),
            )
            Spacer(modifier = Modifier.width(11.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Title(modifier = Modifier.weight(1f))
                    FolderNumberBadge()
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (fileExtension.isNotBlank()) {
                        Tag()
                        Spacer(modifier = Modifier.width(6.dp))
                    } else if (data.isDir) {
                        Tag()
                    }
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .basicMarquee(),
                    ) {
                        if (!data.isDir) {
                            FileSize(modifier = Modifier.alignByBaseline())
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Date(
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .alignByBaseline()
                        )
                    }
                }
                Menu()
            }
        }
    }
}