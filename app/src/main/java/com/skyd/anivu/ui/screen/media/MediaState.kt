package com.skyd.anivu.ui.screen.media

import com.skyd.anivu.ui.mvi.MviViewState
import com.skyd.anivu.model.bean.MediaGroupBean

data class MediaState(
    val groups: List<Pair<MediaGroupBean, Long>>,
    val loadingDialog: Boolean,
) : MviViewState {
    companion object {
        fun initial() = MediaState(
            groups = listOf(MediaGroupBean.DefaultMediaGroup to System.currentTimeMillis()),
            loadingDialog = false,
        )
    }
}