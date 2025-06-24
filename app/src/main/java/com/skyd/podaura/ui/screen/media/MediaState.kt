package com.skyd.podaura.ui.screen.media

import com.skyd.mvi.MviViewState
import com.skyd.podaura.model.bean.MediaGroupBean

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