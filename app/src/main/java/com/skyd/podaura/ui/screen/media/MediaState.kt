package com.skyd.podaura.ui.screen.media

import com.skyd.podaura.model.bean.MediaGroupBean
import com.skyd.podaura.ui.mvi.MviViewState

data class MediaState(
    val groups: List<Pair<MediaGroupBean, Long>>,
    val editGroupDialogBean: MediaGroupBean?,
    val loadingDialog: Boolean,
) : MviViewState {
    companion object {
        fun initial() = MediaState(
            groups = listOf(MediaGroupBean.DefaultMediaGroup to System.currentTimeMillis()),
            editGroupDialogBean = null,
            loadingDialog = false,
        )
    }
}