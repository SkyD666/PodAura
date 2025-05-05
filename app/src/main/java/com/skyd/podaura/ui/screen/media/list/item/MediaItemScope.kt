package com.skyd.podaura.ui.screen.media.list.item

import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.skyd.podaura.ext.aspectRatioIn
import com.skyd.podaura.model.bean.MediaBean

interface MediaItemScope {
    val data: MediaBean
    val onPlay: (MediaBean) -> Unit
    val onOpenDir: (MediaBean) -> Unit
    val onRemove: (MediaBean) -> Unit
    val onOpenFeed: ((MediaBean) -> Unit)?
    val onOpenArticle: ((MediaBean) -> Unit)?
    val onOpenAddToPlaylistSheet: ((MediaBean) -> Unit)?
    val onLongClick: ((MediaBean) -> Unit)?
    val fileNameWithoutExtension: String
    val fileExtension: String
    var expandMenu: Boolean

    fun Modifier.itemClickable(): Modifier

    fun Modifier.gridTypeCoverRatio(ratio: Float): Modifier
}

abstract class MediaItemScopeDefaultImpl : MediaItemScope {
    override fun Modifier.gridTypeCoverRatio(ratio: Float): Modifier = run {
        if (ratio > 0f) aspectRatioIn(ratio, minHeight = 60.dp)
        else heightIn(min = 60.dp)
    }
}