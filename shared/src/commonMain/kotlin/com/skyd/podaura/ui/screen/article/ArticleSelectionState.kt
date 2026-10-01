package com.skyd.podaura.ui.screen.article

import com.skyd.podaura.model.bean.playlist.MediaUrlWithArticleIdBean
import com.skyd.podaura.model.repository.BatchProgress
import com.skyd.podaura.model.repository.download.SelectedDownloadPlan

data class ArticleSelectionState(
    val active: Boolean = false,
    val selectedIds: Set<String> = emptySet(),
    val busy: Boolean = false,
    val confirmation: SelectedDownloadPlan? = null,
    val progress: BatchProgress? = null,
    val playlistMedias: List<MediaUrlWithArticleIdBean>? = null,
)
