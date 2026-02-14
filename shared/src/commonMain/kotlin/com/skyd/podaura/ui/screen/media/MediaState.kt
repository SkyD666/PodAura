package com.skyd.podaura.ui.screen.media

import com.skyd.mvi.MviViewState
import com.skyd.podaura.model.bean.MediaGroupBean
import kotlin.time.Clock

data class MediaState(
    val groups: List<Pair<MediaGroupBean, Long>>,
    val editGroupDialogBean: MediaGroupBean?,
    val loadingDialog: Boolean,
) : MviViewState {
    companion object {
        fun initial() = MediaState(
            groups = listOf(MediaGroupBean.DefaultMediaGroup to Clock.System.now().toEpochMilliseconds()),
            editGroupDialogBean = null,
            loadingDialog = false,
        )
    }
}